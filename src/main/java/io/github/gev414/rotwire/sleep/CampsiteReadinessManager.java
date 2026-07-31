package io.github.gev414.rotwire.sleep;

import io.github.gev414.rotwire.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

final class CampsiteReadinessManager {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final boolean TRAVELERS_BACKPACK_LOADED =
            ModList.get().isLoaded("travelersbackpack");

    static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player
                : event.getServer().getPlayerList().getPlayers()) {
            boolean ready = TRAVELERS_BACKPACK_LOADED
                    && player.isAlive()
                    && !player.isSpectator()
                    && CampsiteManager.isPlayerInReadyCampsite(
                            player.serverLevel(),
                            player
                    );
            if (ready) {
                apply(player);
            } else {
                player.removeEffect(ModEffects.PREPARED_SHELTER);
            }
        }
    }

    private static void apply(ServerPlayer player) {
        if (player.hasEffect(ModEffects.PREPARED_SHELTER)) {
            return;
        }
        player.addEffect(new MobEffectInstance(
                ModEffects.PREPARED_SHELTER,
                MobEffectInstance.INFINITE_DURATION,
                0,
                false,
                false,
                true
        ));
    }

    private CampsiteReadinessManager() {
    }
}
