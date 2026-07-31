package io.github.gev414.rotwire.mob;

import io.github.gev414.rotwire.config.MobSpawnConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

public final class MobSpawnRestrictions {

    public static void onSpawnPlacementCheck(
            MobSpawnEvent.SpawnPlacementCheck event
    ) {
        if (!MobSpawnConfig.UNDERGROUND_RESTRICTIONS_ENABLED.get()
                || !isRestricted(
                        event.getEntityType(),
                        MobSpawnConfig.RESTRICT_SKELETONS.get(),
                        MobSpawnConfig.RESTRICT_CREEPERS.get()
                )) {
            return;
        }

        ServerLevel level = event.getLevel().getLevel();
        if (!MobSpawnConfig.restrictionsApplyTo(
                level.dimension().location()
        )) {
            return;
        }

        int maximumY = maximumNaturalSpawnY(
                level.getSeaLevel(),
                MobSpawnConfig.MINIMUM_DEPTH_BELOW_SEA_LEVEL.get()
        );
        if (rejectsSpawn(event.getSpawnType(), event.getPos().getY(), maximumY)) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    static boolean isRestricted(
            EntityType<?> entityType,
            boolean restrictSkeletons,
            boolean restrictCreepers
    ) {
        return (restrictSkeletons && isSkeletonFamily(entityType))
                || (restrictCreepers && entityType == EntityType.CREEPER);
    }

    private static boolean isSkeletonFamily(EntityType<?> entityType) {
        return entityType == EntityType.SKELETON
                || entityType == EntityType.STRAY
                || entityType == EntityType.BOGGED
                || entityType == EntityType.WITHER_SKELETON;
    }

    static int maximumNaturalSpawnY(
            int seaLevel,
            int minimumDepthBelowSeaLevel
    ) {
        return seaLevel - Math.max(0, minimumDepthBelowSeaLevel);
    }

    static boolean rejectsSpawn(
            MobSpawnType spawnType,
            int spawnY,
            int maximumNaturalSpawnY
    ) {
        return spawnType == MobSpawnType.NATURAL
                && spawnY > maximumNaturalSpawnY;
    }

    private MobSpawnRestrictions() {
    }
}
