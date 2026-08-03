package io.github.gev414.rotwire.stealth;

import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import io.github.gev414.rotwire.mob.ai.CoordinatedHostileAi;
import io.github.gev414.rotwire.weather.WeatherManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class AttentionManager {

    private static final ResourceLocation ZOMBIE_TACTICS_MARKER =
            ResourceLocation.fromNamespaceAndPath(
                    "zombie_tactics",
                    "marker"
            );
    private static final boolean ZOMBIE_TACTICS_LOADED =
            ModList.get().isLoaded("zombie_tactics");

    public static void emit(
            ServerLevel level,
            Vec3 position,
            ServerPlayer source,
            double range
    ) {
        if (!SurvivalSystemsConfig.ENABLED.get() || range <= 0.0D) {
            return;
        }
        double effectiveRange = range
                * WeatherManager.attentionMultiplier(level);
        if (effectiveRange <= 0.0D) {
            return;
        }
        AwarenessManager.markNoisy(source);
        long expiry = level.getGameTime() + Math.max(
                60L,
                Math.round(effectiveRange * 2.0D)
        );
        AABB area = new AABB(position, position).inflate(effectiveRange);
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class,
                area,
                candidate -> AwarenessManager.isAffected(candidate)
                        && candidate.isAlive()
        )) {
            CoordinatedHostileAi.investigate(mob, position, expiry);
        }
    }

    public static void alertDirectlyAttacked(
            Mob mob,
            ServerPlayer player
    ) {
        AwarenessManager.markNoisy(player);
        AwarenessManager.alert(mob, player);
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!ZOMBIE_TACTICS_LOADED
                || !SurvivalSystemsConfig.ENABLED.get()
                || !SurvivalSystemsConfig
                .REPLACE_ZOMBIE_TACTICS_MARKERS
                .get()) {
            return;
        }
        Entity entity = event.getEntity();
        if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                .equals(ZOMBIE_TACTICS_MARKER)) {
            event.setCanceled(true);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
    }

    private AttentionManager() {
    }
}
