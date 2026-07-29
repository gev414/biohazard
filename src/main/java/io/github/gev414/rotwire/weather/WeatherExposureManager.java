package io.github.gev414.rotwire.weather;

import io.github.gev414.rotwire.config.WeatherConfig;
import io.github.gev414.rotwire.damage.ModDamageTypes;
import io.github.gev414.rotwire.network.WeatherExposurePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class WeatherExposureManager {

    private static final int CHECK_INTERVAL_TICKS = 5;

    private static final Map<UUID, Exposure> EXPOSURES =
            new HashMap<>();
    private static final Map<UUID, WeatherExposurePayload> LAST_SENT =
            new HashMap<>();

    private static int ticksUntilCheck;

    static void onServerTick(ServerTickEvent.Post event) {
        if (ticksUntilCheck > 0) {
            ticksUntilCheck--;
            return;
        }
        ticksUntilCheck = CHECK_INTERVAL_TICKS - 1;

        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player
                : event.getServer().getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            tickPlayer(player);
        }
        EXPOSURES.keySet().removeIf(id -> !online.contains(id));
        LAST_SENT.keySet().removeIf(id -> !online.contains(id));
    }

    static void clear() {
        EXPOSURES.clear();
        LAST_SENT.clear();
        ticksUntilCheck = 0;
    }

    private static void tickPlayer(ServerPlayer player) {
        if (!WeatherConfig.ENABLED.get()
                || player.serverLevel().dimension() != Level.OVERWORLD
                || !player.isAlive()
                || player.isCreative()
                || player.isSpectator()) {
            EXPOSURES.remove(player.getUUID());
            sync(player, WeatherExposurePayload.clear());
            return;
        }

        ServerLevel level = player.serverLevel();
        ScheduledWeather weather = WeatherManager.current(level);
        boolean contaminated = weather.contaminated();
        boolean exposed = contaminated && precipitationReaches(
                level,
                player.blockPosition()
        );
        if (!exposed) {
            EXPOSURES.remove(player.getUUID());
            sync(player, new WeatherExposurePayload(
                    contaminated,
                    weather.storm(),
                    false,
                    false
            ));
            return;
        }

        long now = level.getGameTime();
        int grace = weather.storm()
                ? WeatherConfig.STORM_GRACE_TICKS.get()
                : WeatherConfig.RAIN_GRACE_TICKS.get();
        Exposure exposure = EXPOSURES.computeIfAbsent(
                player.getUUID(),
                ignored -> new Exposure(now)
        );
        boolean harmful = now - exposure.startedAt >= grace;
        if (harmful && now >= exposure.nextDamageAt) {
            hurt(player);
            int interval = weather.storm()
                    ? WeatherConfig
                    .STORM_DAMAGE_INTERVAL_TICKS
                    .get()
                    : WeatherConfig
                    .RAIN_DAMAGE_INTERVAL_TICKS
                    .get();
            exposure.nextDamageAt = now + interval;
        }
        sync(player, new WeatherExposurePayload(
                true,
                weather.storm(),
                true,
                harmful
        ));
    }

    private static boolean precipitationReaches(
            ServerLevel level,
            BlockPos position
    ) {
        return level.isRainingAt(position)
                || level.isRainingAt(position.above());
    }

    private static void hurt(ServerPlayer player) {
        float amount = WeatherConfig.DAMAGE_AMOUNT.get().floatValue();
        if (amount <= 0.0F) {
            return;
        }
        DamageSource source = new DamageSource(
                player.serverLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(
                                ModDamageTypes.CONTAMINATED_RAIN
                        )
        );
        player.hurt(source, amount);
    }

    private static void sync(
            ServerPlayer player,
            WeatherExposurePayload payload
    ) {
        WeatherExposurePayload previous =
                LAST_SENT.get(player.getUUID());
        if (payload.equals(previous)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
        LAST_SENT.put(player.getUUID(), payload);
    }

    private static final class Exposure {

        private final long startedAt;
        private long nextDamageAt;

        private Exposure(long startedAt) {
            this.startedAt = startedAt;
            this.nextDamageAt = startedAt;
        }
    }

    private WeatherExposureManager() {
    }
}
