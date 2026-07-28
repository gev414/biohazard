package io.github.gev414.rotwire.city;

import io.github.gev414.rotwire.config.CityOperationsConfig;
import io.github.gev414.rotwire.lostcities.LostCitiesCityResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;

public final class CityStreetZombieSpawner {

    private static final String STREET_SPAWN_TAG =
            "rotwire_city_street_spawn";

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!CityOperationsConfig.ENABLED.get()
                || !CityOperationsConfig.STREET_SPAWNS_ENABLED.get()
                || CityOperationsConfig.STREET_ZOMBIE_CAP.get() <= 0) {
            return;
        }

        int interval =
                CityOperationsConfig.STREET_SPAWN_INTERVAL_TICKS.get();
        if (server.overworld().getGameTime() % interval != 0L) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            trySpawnFor(player);
        }
    }

    private static void trySpawnFor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!player.isAlive()
                || player.isSpectator()
                || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().getBoolean(
                GameRules.RULE_DOMOBSPAWNING
        )) {
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextDouble()
                >= CityOperationsConfig.STREET_SPAWN_CHANCE.get()) {
            return;
        }

        int capRadius =
                CityOperationsConfig.STREET_ZOMBIE_CAP_RADIUS.get();
        AABB nearbyArea = new AABB(
                player.getX() - capRadius,
                level.getMinBuildHeight(),
                player.getZ() - capRadius,
                player.getX() + capRadius,
                level.getMaxBuildHeight(),
                player.getZ() + capRadius
        );
        int nearbyStreetZombies = level.getEntitiesOfClass(
                Zombie.class,
                nearbyArea,
                CityStreetZombieSpawner::isStreetSpawn
        ).size();
        if (nearbyStreetZombies
                >= CityOperationsConfig.STREET_ZOMBIE_CAP.get()) {
            return;
        }

        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            return;
        }

        BlockPos spawnPosition = findSpawnPosition(
                level,
                player,
                zombie,
                random
        );
        if (spawnPosition == null) {
            zombie.discard();
            return;
        }

        EventHooks.finalizeMobSpawn(
                zombie,
                level,
                level.getCurrentDifficultyAt(spawnPosition),
                MobSpawnType.NATURAL,
                null
        );
        zombie.getPersistentData().putBoolean(STREET_SPAWN_TAG, true);
        if (!level.tryAddFreshEntityWithPassengers(zombie)) {
            zombie.discard();
        }
    }

    @Nullable
    private static BlockPos findSpawnPosition(
            ServerLevel level,
            ServerPlayer anchor,
            Zombie zombie,
            RandomSource random
    ) {
        int minimumDistance =
                CityOperationsConfig.minimumStreetSpawnDistance();
        int maximumDistance =
                CityOperationsConfig.maximumStreetSpawnDistance();

        for (int attempt = 0;
             attempt
                     < CityOperationsConfig
                     .STREET_SPAWN_POSITION_ATTEMPTS
                     .get();
             attempt++) {
            double angle = random.nextDouble() * Math.TAU;
            double distance = minimumDistance
                    + random.nextDouble()
                    * (maximumDistance - minimumDistance);
            int x = (int) Math.floor(
                    anchor.getX() + Math.cos(angle) * distance
            );
            int z = (int) Math.floor(
                    anchor.getZ() + Math.sin(angle) * distance
            );
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos candidate = new BlockPos(
                    x,
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    ),
                    z
            );

            if (isValidPosition(
                    level,
                    candidate,
                    zombie,
                    minimumDistance
            )) {
                zombie.moveTo(
                        candidate.getX() + 0.5D,
                        candidate.getY(),
                        candidate.getZ() + 0.5D,
                        random.nextFloat() * 360.0F,
                        0.0F
                );
                return candidate;
            }
        }
        return null;
    }

    private static boolean isValidPosition(
            ServerLevel level,
            BlockPos position,
            Zombie zombie,
            int minimumDistance
    ) {
        if (!level.isAreaLoaded(position, 1)
                || !level.getWorldBorder().isWithinBounds(position)
                || !LostCitiesCityResolver.isStreetChunk(
                level,
                position.getX() >> 4,
                position.getZ() >> 4
        )
                || !level.canSeeSky(position)
                || !level.getFluidState(position).isEmpty()
                || !level.getBlockState(position.below())
                .isFaceSturdy(level, position.below(), Direction.UP)
                || isTooCloseToAnyPlayer(
                level,
                position,
                minimumDistance
        )) {
            return false;
        }

        zombie.moveTo(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        return level.noCollision(zombie)
                && zombie.checkSpawnObstruction(level);
    }

    private static boolean isTooCloseToAnyPlayer(
            ServerLevel level,
            BlockPos position,
            int minimumDistance
    ) {
        double minimumDistanceSquared =
                (double) minimumDistance * minimumDistance;
        for (ServerPlayer player : level.players()) {
            double deltaX =
                    player.getX() - (position.getX() + 0.5D);
            double deltaZ =
                    player.getZ() - (position.getZ() + 0.5D);
            if (!player.isSpectator()
                    && deltaX * deltaX + deltaZ * deltaZ
                    < minimumDistanceSquared) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStreetSpawn(Zombie zombie) {
        return zombie.isAlive()
                && zombie.getPersistentData()
                .getBoolean(STREET_SPAWN_TAG);
    }

    private CityStreetZombieSpawner() {
    }
}
