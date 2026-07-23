package io.github.gev414.biohazard.event;

import io.github.gev414.biohazard.config.SurvivalSystemsConfig;
import io.github.gev414.biohazard.encumbrance.EncumbranceManager;
import io.github.gev414.biohazard.encumbrance.EncumbranceSnapshot;
import io.github.gev414.biohazard.encumbrance.EncumbranceMath;
import io.github.gev414.biohazard.network.SurvivalStatusPayload;
import io.github.gev414.biohazard.stealth.AwarenessManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SurvivalStatusSync {

    private static final int SYNC_INTERVAL_TICKS = 5;
    private static final Map<UUID, SurvivalStatusPayload> LAST_SENT =
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
        EncumbranceSnapshot snapshot =
                EncumbranceManager.snapshot(player);
        SurvivalStatusPayload payload = new SurvivalStatusPayload(
                EncumbranceMath.displayTenths(snapshot.weight()),
                snapshot.tier().ordinal(),
                SurvivalSystemsConfig.ENABLED.get(),
                SurvivalSystemsConfig.ENABLED.get()
                        && AwarenessManager.isQuiet(player),
                AwarenessManager.suspicionPercent(player),
                EncumbranceMath.displayTenths(
                        SurvivalSystemsConfig.LIGHT_MAX_WEIGHT.get()
                ),
                EncumbranceMath.displayTenths(
                        SurvivalSystemsConfig.BURDENED_MAX_WEIGHT.get()
                ),
                EncumbranceMath.displayTenths(
                        SurvivalSystemsConfig.HEAVY_MAX_WEIGHT.get()
                ),
                penaltyPercent(
                        SurvivalSystemsConfig
                                .BURDENED_SPEED_PENALTY
                                .get()
                ),
                penaltyPercent(
                        SurvivalSystemsConfig.HEAVY_SPEED_PENALTY.get()
                ),
                penaltyPercent(
                        SurvivalSystemsConfig
                                .OVERLOADED_SPEED_PENALTY
                                .get()
                )
        );
        SurvivalStatusPayload previous = LAST_SENT.get(player.getUUID());
        if (!force && payload.equals(previous)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
        LAST_SENT.put(player.getUUID(), payload);
    }

    private static int penaltyPercent(double penalty) {
        return (int) Math.round(
                Math.max(0.0D, Math.min(penalty, 0.95D)) * 100.0D
        );
    }

    private SurvivalStatusSync() {
    }
}
