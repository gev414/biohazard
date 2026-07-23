package io.github.gev414.biohazard.stealth;

import io.github.gev414.biohazard.city.ModEntityTypeTags;
import io.github.gev414.biohazard.config.SurvivalSystemsConfig;
import io.github.gev414.biohazard.encumbrance.EncumbranceManager;
import io.github.gev414.biohazard.entity.BruteEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.smileycorp.hordes.common.capability.HordesCapabilities;
import net.smileycorp.hordes.hordeevent.capability.HordeSpawn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AwarenessManager {

    private static final Map<AwarenessKey, Suspicion> SUSPICION =
            new HashMap<>();
    private static final Map<AwarenessKey, Long> ALERTED_UNTIL =
            new HashMap<>();
    private static final Map<UUID, Long> NOISY_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> PLAYER_SUSPICION =
            new HashMap<>();

    private static int ticksUntilScan;

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!SurvivalSystemsConfig.ENABLED.get()) {
            clear();
            return;
        }
        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }

        int scanTicks = SurvivalSystemsConfig
                .AWARENESS_SCAN_INTERVAL_TICKS
                .get();
        ticksUntilScan = scanTicks - 1;
        double elapsedSeconds = scanTicks / 20.0D;
        Set<AwarenessKey> observed = new HashSet<>();
        PLAYER_SUSPICION.clear();

        for (ServerPlayer player
                : event.getServer().getPlayerList().getPlayers()) {
            scanPlayer(player, elapsedSeconds, observed);
        }
        decayUnobserved(elapsedSeconds, observed);
        cleanExpired(event);
    }

    public static void onLivingChangeTarget(
            LivingChangeTargetEvent event
    ) {
        if (!SurvivalSystemsConfig.ENABLED.get()
                || !(event.getEntity() instanceof Mob mob)
                || !(event.getNewAboutToBeSetTarget()
                instanceof ServerPlayer player)
                || !isAffected(mob)
                || isHordeSpawn(mob)) {
            return;
        }
        if (isQuiet(player) && !isAlerted(mob, player)) {
            event.setCanceled(true);
        } else {
            rememberAlert(mob, player);
        }
    }

    public static boolean isQuiet(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        return !player.isCreative()
                && !player.isSpectator()
                && player.isCrouching()
                && EncumbranceManager.isLight(player)
                && NOISY_UNTIL.getOrDefault(player.getUUID(), 0L) <= now;
    }

    public static int suspicionPercent(ServerPlayer player) {
        return PLAYER_SUSPICION.getOrDefault(player.getUUID(), 0);
    }

    public static void markNoisy(ServerPlayer player) {
        long until = player.serverLevel().getGameTime()
                + SurvivalSystemsConfig.LOUD_ACTION_GRACE_TICKS.get();
        NOISY_UNTIL.merge(player.getUUID(), until, Math::max);
    }

    public static void alert(Mob mob, ServerPlayer player) {
        if (!isAffected(mob)) {
            return;
        }
        rememberAlert(mob, player);
        mob.setTarget(player);
    }

    private static void rememberAlert(Mob mob, ServerPlayer player) {
        long until = mob.level().getGameTime()
                + SurvivalSystemsConfig.ALERT_MEMORY_TICKS.get();
        AwarenessKey key = key(mob, player);
        ALERTED_UNTIL.put(key, until);
        SUSPICION.put(key, new Suspicion(100.0D, until));
    }

    public static boolean isAffected(Mob mob) {
        return mob.getType().is(
                ModEntityTypeTags.STEALTH_AFFECTED_INFECTED
        );
    }

    public static boolean isHordeSpawn(Mob mob) {
        HordeSpawn spawn = mob.getCapability(
                HordesCapabilities.HORDESPAWN
        );
        return spawn != null && spawn.isHordeSpawned();
    }

    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        UUID playerId = event.getEntity().getUUID();
        NOISY_UNTIL.remove(playerId);
        PLAYER_SUSPICION.remove(playerId);
        SUSPICION.keySet().removeIf(key -> key.playerId.equals(playerId));
        ALERTED_UNTIL.keySet().removeIf(
                key -> key.playerId.equals(playerId)
        );
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    private static void scanPlayer(
            ServerPlayer player,
            double elapsedSeconds,
            Set<AwarenessKey> observed
    ) {
        if (!isQuiet(player)) {
            return;
        }

        double range = SurvivalSystemsConfig.DETECTION_RANGE.get();
        AABB search = player.getBoundingBox().inflate(range);
        for (Mob mob : player.serverLevel().getEntitiesOfClass(
                Mob.class,
                search,
                candidate -> isAffected(candidate)
                        && candidate.isAlive()
        )) {
            if (isHordeSpawn(mob)) {
                linkHordeTarget(mob);
                continue;
            }

            double distance = mob.distanceTo(player);
            AwarenessKey key = key(mob, player);
            observed.add(key);
            Suspicion previous = SUSPICION.getOrDefault(
                    key,
                    Suspicion.EMPTY
            );
            double value = previous.value;
            boolean visible = distance <= range
                    && mob.getSensing().hasLineOfSight(player)
                    && insideView(mob, player);

            if (visible
                    && distance <= SurvivalSystemsConfig
                    .CLOSE_DETECTION_RANGE
                    .get()) {
                value = 100.0D;
            } else if (visible) {
                value = AwarenessMath.gain(
                        value,
                        SurvivalSystemsConfig.SUSPICION_PER_SECOND.get(),
                        elapsedSeconds,
                        distance,
                        range,
                        mob instanceof BruteEntity
                                ? SurvivalSystemsConfig
                                .BRUTE_DETECTION_MULTIPLIER
                                .get()
                                : 1.0D
                );
            } else {
                value = AwarenessMath.decay(
                        value,
                        SurvivalSystemsConfig
                        .SUSPICION_DECAY_PER_SECOND
                        .get(),
                        elapsedSeconds
                );
            }

            long now = mob.level().getGameTime();
            if (value >= 100.0D) {
                alert(mob, player);
                value = 100.0D;
            } else {
                SUSPICION.put(key, new Suspicion(value, now));
            }
            PLAYER_SUSPICION.merge(
                    player.getUUID(),
                    (int) Math.round(value),
                    Math::max
            );

            if (mob.getTarget() == player
                    && !isAlerted(mob, player)
                    && value < 100.0D) {
                mob.setTarget(null);
            }
        }
    }

    private static void decayUnobserved(
            double elapsedSeconds,
            Set<AwarenessKey> observed
    ) {
        Iterator<Map.Entry<AwarenessKey, Suspicion>> iterator =
                SUSPICION.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AwarenessKey, Suspicion> entry = iterator.next();
            if (observed.contains(entry.getKey())) {
                continue;
            }
            double value = AwarenessMath.decay(
                    entry.getValue().value,
                    SurvivalSystemsConfig.SUSPICION_DECAY_PER_SECOND.get(),
                    elapsedSeconds
            );
            if (value <= 0.0D) {
                iterator.remove();
            } else {
                entry.setValue(new Suspicion(
                        value,
                        entry.getValue().lastSeen
                ));
                PLAYER_SUSPICION.merge(
                        entry.getKey().playerId,
                        (int) Math.round(value),
                        Math::max
                );
            }
        }
    }

    private static boolean insideView(Mob mob, ServerPlayer player) {
        Vec3 look = mob.getLookAngle();
        Vec3 target = player.getEyePosition().subtract(
                mob.getEyePosition()
        );
        return AwarenessMath.insideFieldOfView(
                look.x,
                look.y,
                look.z,
                target.x,
                target.y,
                target.z,
                SurvivalSystemsConfig.FIELD_OF_VIEW_DEGREES.get()
        );
    }

    private static boolean isAlerted(Mob mob, ServerPlayer player) {
        return ALERTED_UNTIL.getOrDefault(key(mob, player), 0L)
                > mob.level().getGameTime();
    }

    private static void linkHordeTarget(Mob mob) {
        ServerPlayer hordePlayer = HordeSpawn.getHordePlayer(mob);
        if (hordePlayer != null && hordePlayer.isAlive()) {
            mob.setTarget(hordePlayer);
        }
    }

    private static void cleanExpired(ServerTickEvent.Post event) {
        long maximumTime = 0L;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            maximumTime = Math.max(maximumTime, level.getGameTime());
        }
        long now = maximumTime;
        ALERTED_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
        NOISY_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private static AwarenessKey key(Mob mob, ServerPlayer player) {
        return new AwarenessKey(mob.getUUID(), player.getUUID());
    }

    private static void clear() {
        SUSPICION.clear();
        ALERTED_UNTIL.clear();
        NOISY_UNTIL.clear();
        PLAYER_SUSPICION.clear();
        ticksUntilScan = 0;
    }

    private record AwarenessKey(UUID mobId, UUID playerId) {
    }

    private record Suspicion(double value, long lastSeen) {

        private static final Suspicion EMPTY = new Suspicion(0.0D, 0L);
    }

    private AwarenessManager() {
    }
}
