package io.github.gev414.biohazard.event;

import io.github.gev414.biohazard.network.HordeAtmospherePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.smileycorp.hordes.config.HordeEventConfig;
import net.smileycorp.hordes.hordeevent.capability.HordeEvent;
import net.smileycorp.hordes.hordeevent.capability.HordeSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HordeAtmosphereSyncEvents {

    private static final int SYNC_INTERVAL_TICKS = 20;

    private static final Map<UUID, HordeAtmospherePayload> LAST_SENT =
            new HashMap<>();

    private static int ticksUntilSync;

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ticksUntilSync > 0) {
            ticksUntilSync--;
            return;
        }
        ticksUntilSync = SYNC_INTERVAL_TICKS - 1;

        for (ServerPlayer player
                : event.getServer().getPlayerList().getPlayers()) {
            sync(player, false);
        }
    }

    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player, true);
        }
    }

    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        LAST_SENT.clear();
        ticksUntilSync = 0;
    }

    private static void sync(ServerPlayer player, boolean force) {
        HordeAtmospherePayload payload = statusFor(player);
        HordeAtmospherePayload previous = LAST_SENT.get(player.getUUID());
        if (!force && payload.equals(previous)) {
            return;
        }

        PacketDistributor.sendToPlayer(player, payload);
        LAST_SENT.put(player.getUUID(), payload);
    }

    private static HordeAtmospherePayload statusFor(ServerPlayer player) {
        int dayLength = Math.max(1, HordeEventConfig.dayLength.get());
        int hordeStartTime = Math.max(
                0,
                Math.min(HordeEventConfig.hordeStartTime.get(), dayLength)
        );

        if (player.serverLevel().dimension() != Level.OVERWORLD
                || !HordeEventConfig.enableHordeEvent.get()) {
            return new HordeAtmospherePayload(
                    false,
                    false,
                    dayLength,
                    hordeStartTime
            );
        }

        HordeEvent hordeEvent = HordeSavedData
                .getData(player.serverLevel())
                .getEvent(player);
        boolean active = hordeEvent.isActive();
        boolean hordeDay = HordeEventConfig.hordesCommandOnly.get()
                ? active
                : hordeEvent.isHordeDay(player);

        return new HordeAtmospherePayload(
                hordeDay,
                active,
                dayLength,
                hordeStartTime
        );
    }

    private HordeAtmosphereSyncEvents() {
    }
}
