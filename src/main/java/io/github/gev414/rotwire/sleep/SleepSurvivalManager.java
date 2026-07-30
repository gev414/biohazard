package io.github.gev414.rotwire.sleep;

import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import io.github.gev414.rotwire.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SleepSurvivalManager {

    private static final long NO_NIGHT = Long.MIN_VALUE;
    private static final boolean TRAVELERS_BACKPACK_LOADED =
            ModList.get().isLoaded("travelersbackpack");
    private static final Map<ResourceKey<Level>, NightState> NIGHT_STATES =
            new HashMap<>();
    private static final Set<UUID> RESTLESS_ON_WAKE = new HashSet<>();
    private static final Set<UUID> RESTED_ON_WAKE = new HashSet<>();

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!enabled()) {
            clear(event);
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            updateLevel(level);
        }
    }

    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (!enabled() || event.getProblem() != null) {
            return;
        }
        disqualify(event.getEntity());
    }

    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!enabled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        NightState state = NIGHT_STATES.get(level.dimension());
        if (state != null) {
            state.cancelNight();
        }

        if (!TRAVELERS_BACKPACK_LOADED) {
            return;
        }
        level.players().stream()
                .filter(ServerPlayer::isSleeping)
                .sorted(Comparator.comparing(ServerPlayer::getUUID))
                .forEach(player -> player.getSleepingPos()
                        .ifPresent(pos -> recordSleepOutcome(
                                level,
                                player,
                                pos
                        )));
    }

    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        boolean rested = RESTED_ON_WAKE.remove(playerId);
        boolean restless = RESTLESS_ON_WAKE.remove(playerId);
        if (!enabled()) {
            return;
        }

        if (rested) {
            player.removeEffect(ModEffects.RESTLESS_SLEEP);
            player.displayClientMessage(
                    Component.translatable(
                            "message.rotwire.rested_campsite"
                    ),
                    true
            );
            return;
        }
        if (!restless) {
            return;
        }
        applyEffect(
                player,
                ModEffects.RESTLESS_SLEEP,
                ModEffects.NEW_DAWN,
                "message.rotwire.restless_sleep"
        );
    }

    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        UUID playerId = event.getEntity().getUUID();
        RESTLESS_ON_WAKE.remove(playerId);
        RESTED_ON_WAKE.remove(playerId);
        NIGHT_STATES.values().forEach(
                state -> state.eligiblePlayers.remove(playerId)
        );
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        NIGHT_STATES.clear();
        RESTLESS_ON_WAKE.clear();
        RESTED_ON_WAKE.clear();
    }

    private static void recordSleepOutcome(
            ServerLevel level,
            ServerPlayer player,
            BlockPos sleepingPosition
    ) {
        if (!TravelersBackpackSleepIntegration.isSleepingBag(
                level.getBlockState(sleepingPosition)
        )) {
            return;
        }

        UUID playerId = player.getUUID();
        if (CampsiteManager.tryPayForRest(
                level,
                player,
                sleepingPosition
        )) {
            RESTLESS_ON_WAKE.remove(playerId);
            RESTED_ON_WAKE.add(playerId);
        } else {
            RESTED_ON_WAKE.remove(playerId);
            RESTLESS_ON_WAKE.add(playerId);
        }
    }

    private static void updateLevel(ServerLevel level) {
        NightState state = NIGHT_STATES.computeIfAbsent(
                level.dimension(),
                ignored -> new NightState()
        );
        long current = level.getDayTime();
        if (state.previousDayTime == Long.MIN_VALUE) {
            state.previousDayTime = current;
            return;
        }

        pruneIneligible(level, state);
        boolean dawn = NightCycle.crossedDawn(
                state.previousDayTime,
                current
        );

        if (state.hasActiveNight()) {
            if (dawn) {
                if (state.observedNightTicks
                        >= NightCycle.FULL_NIGHT_TICKS) {
                    rewardEligible(level, state);
                }
                state.cancelNight();
            } else if (NightCycle.isNight(current)
                    && NightCycle.day(current) == state.activeNightDay) {
                state.observedNightTicks++;
            } else {
                state.cancelNight();
            }
        }

        if (!state.hasActiveNight()
                && NightCycle.crossedNightStart(
                        state.previousDayTime,
                        current
                )) {
            startNight(level, state, current);
        }
        state.previousDayTime = current;
    }

    private static void startNight(
            ServerLevel level,
            NightState state,
            long dayTime
    ) {
        state.activeNightDay = NightCycle.day(dayTime);
        state.observedNightTicks = 1;
        state.eligiblePlayers.clear();
        for (ServerPlayer player : level.players()) {
            if (eligible(player) && !player.isSleeping()) {
                state.eligiblePlayers.add(player.getUUID());
            }
        }
    }

    private static void pruneIneligible(
            ServerLevel level,
            NightState state
    ) {
        state.eligiblePlayers.removeIf(playerId -> {
            ServerPlayer player = level.getServer()
                    .getPlayerList()
                    .getPlayer(playerId);
            return player == null
                    || player.level() != level
                    || player.isSleeping()
                    || !eligible(player);
        });
    }

    private static void rewardEligible(
            ServerLevel level,
            NightState state
    ) {
        for (UUID playerId : state.eligiblePlayers) {
            ServerPlayer player = level.getServer()
                    .getPlayerList()
                    .getPlayer(playerId);
            if (player != null && player.level() == level
                    && eligible(player) && !player.isSleeping()) {
                applyEffect(
                        player,
                        ModEffects.NEW_DAWN,
                        ModEffects.RESTLESS_SLEEP,
                        "message.rotwire.new_dawn"
                );
            }
        }
    }

    private static void disqualify(ServerPlayer player) {
        NightState state = NIGHT_STATES.get(player.level().dimension());
        if (state != null) {
            state.eligiblePlayers.remove(player.getUUID());
        }
    }

    private static boolean eligible(ServerPlayer player) {
        return player.isAlive()
                && !player.isCreative()
                && !player.isSpectator();
    }

    private static void applyEffect(
            ServerPlayer player,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>
                    effect,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>
                    opposite,
            String messageKey
    ) {
        player.removeEffect(opposite);
        player.addEffect(new MobEffectInstance(
                effect,
                SurvivalSystemsConfig.SLEEP_EFFECT_DURATION_TICKS.get(),
                0,
                false,
                false,
                true
        ));
        player.displayClientMessage(
                Component.translatable(messageKey),
                true
        );
    }

    private static boolean enabled() {
        return SurvivalSystemsConfig.ENABLED.get()
                && SurvivalSystemsConfig.SLEEP_SURVIVAL_ENABLED.get();
    }

    private static void clear(ServerTickEvent.Post event) {
        NIGHT_STATES.clear();
        RESTLESS_ON_WAKE.clear();
        RESTED_ON_WAKE.clear();
        for (ServerPlayer player
                : event.getServer().getPlayerList().getPlayers()) {
            player.removeEffect(ModEffects.RESTLESS_SLEEP);
            player.removeEffect(ModEffects.NEW_DAWN);
        }
    }

    private static final class NightState {

        private long previousDayTime = Long.MIN_VALUE;
        private long activeNightDay = NO_NIGHT;
        private int observedNightTicks;
        private final Set<UUID> eligiblePlayers = new HashSet<>();

        private boolean hasActiveNight() {
            return activeNightDay != NO_NIGHT;
        }

        private void cancelNight() {
            activeNightDay = NO_NIGHT;
            observedNightTicks = 0;
            eligiblePlayers.clear();
        }
    }

    private SleepSurvivalManager() {
    }
}
