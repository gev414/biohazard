package io.github.gev414.rotwire.mob;

import io.github.gev414.rotwire.config.CityOperationsConfig;
import io.github.gev414.rotwire.config.MobSpawnConfig;
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

public final class SurfaceZombieSpawner {

    private static final String STREET_SPAWN_TAG =
            "rotwire_city_street_spawn";
    private static final String WILDERNESS_SPAWN_TAG =
            "rotwire_wilderness_surface_spawn";

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long gameTime = server.overworld().getGameTime();
        SpawnSettings citySettings = citySettings(gameTime);
        SpawnSettings wildernessSettings = wildernessSettings(gameTime);
        if (citySettings == null && wildernessSettings == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            boolean inCity = LostCitiesCityResolver.isCityChunk(
                    level,
                    player.chunkPosition().x,
                    player.chunkPosition().z
            );
            if (inCity && citySettings != null) {
                trySpawnFor(player, citySettings);
            } else if (!inCity
                    && wildernessSettings != null
                    && MobSpawnConfig.wildernessZombiesApplyTo(
                            level.dimension().location()
                    )) {
                trySpawnFor(player, wildernessSettings);
            }
        }
    }

    @Nullable
    private static SpawnSettings citySettings(long gameTime) {
        int interval =
                CityOperationsConfig.STREET_SPAWN_INTERVAL_TICKS.get();
        if (!CityOperationsConfig.ENABLED.get()
                || !CityOperationsConfig.STREET_SPAWNS_ENABLED.get()
                || CityOperationsConfig.STREET_ZOMBIE_CAP.get() <= 0
                || !isSpawnTick(gameTime, interval)) {
            return null;
        }
        return new SpawnSettings(
                SpawnRegion.CITY_STREET,
                CityOperationsConfig.STREET_SPAWN_CHANCE.get(),
                CityOperationsConfig
                        .STREET_NIGHTTIME_CHANCE_MULTIPLIER.get(),
                CityOperationsConfig.STREET_ZOMBIE_CAP.get(),
                CityOperationsConfig.STREET_ZOMBIE_CAP_RADIUS.get(),
                CityOperationsConfig.minimumStreetSpawnDistance(),
                CityOperationsConfig.maximumStreetSpawnDistance(),
                CityOperationsConfig.STREET_SPAWN_POSITION_ATTEMPTS.get()
        );
    }

    @Nullable
    private static SpawnSettings wildernessSettings(long gameTime) {
        int interval =
                MobSpawnConfig.WILDERNESS_SPAWN_INTERVAL_TICKS.get();
        if (!MobSpawnConfig.WILDERNESS_ZOMBIES_ENABLED.get()
                || MobSpawnConfig.WILDERNESS_ZOMBIE_CAP.get() <= 0
                || !isSpawnTick(gameTime, interval)) {
            return null;
        }
        return new SpawnSettings(
                SpawnRegion.WILDERNESS,
                MobSpawnConfig.WILDERNESS_SPAWN_CHANCE.get(),
                MobSpawnConfig
                        .WILDERNESS_NIGHTTIME_CHANCE_MULTIPLIER.get(),
                MobSpawnConfig.WILDERNESS_ZOMBIE_CAP.get(),
                MobSpawnConfig.WILDERNESS_ZOMBIE_CAP_RADIUS.get(),
                MobSpawnConfig.minimumWildernessSpawnDistance(),
                MobSpawnConfig.maximumWildernessSpawnDistance(),
                MobSpawnConfig.WILDERNESS_SPAWN_POSITION_ATTEMPTS.get()
        );
    }

    static boolean isSpawnTick(long gameTime, int intervalTicks) {
        return intervalTicks > 0 && gameTime % intervalTicks == 0L;
    }

    static boolean passesSpawnRoll(double roll, double chance) {
        return roll < Math.clamp(chance, 0.0D, 1.0D);
    }

    static double effectiveSpawnChance(
            double baseChance,
            double nighttimeMultiplier,
            boolean isNight
    ) {
        double multiplier = isNight
                ? Math.max(1.0D, nighttimeMultiplier)
                : 1.0D;
        return Math.clamp(baseChance * multiplier, 0.0D, 1.0D);
    }

    private static void trySpawnFor(
            ServerPlayer player,
            SpawnSettings settings
    ) {
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
        double chance = effectiveSpawnChance(
                settings.chance(),
                settings.nighttimeChanceMultiplier(),
                level.isNight()
        );
        if (!passesSpawnRoll(random.nextDouble(), chance)) {
            return;
        }

        int capRadius = settings.capRadius();
        AABB nearbyArea = new AABB(
                player.getX() - capRadius,
                level.getMinBuildHeight(),
                player.getZ() - capRadius,
                player.getX() + capRadius,
                level.getMaxBuildHeight(),
                player.getZ() + capRadius
        );
        int nearbySurfaceZombies = level.getEntitiesOfClass(
                Zombie.class,
                nearbyArea,
                zombie -> isSpawnFrom(zombie, settings.region())
        ).size();
        if (nearbySurfaceZombies >= settings.cap()) {
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
                random,
                settings
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
        zombie.getPersistentData().putBoolean(
                settings.region().spawnTag(),
                true
        );
        if (!level.tryAddFreshEntityWithPassengers(zombie)) {
            zombie.discard();
        }
    }

    @Nullable
    private static BlockPos findSpawnPosition(
            ServerLevel level,
            ServerPlayer anchor,
            Zombie zombie,
            RandomSource random,
            SpawnSettings settings
    ) {
        int minimumDistance = settings.minimumDistance();
        int maximumDistance = settings.maximumDistance();

        for (int attempt = 0;
             attempt < settings.positionAttempts();
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
                    minimumDistance,
                    settings.region()
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
            int minimumDistance,
            SpawnRegion region
    ) {
        if (!level.isAreaLoaded(position, 1)
                || !level.getWorldBorder().isWithinBounds(position)
                || !isRequiredRegion(level, position, region)
                || (region == SpawnRegion.CITY_STREET
                        && !level.canSeeSky(position))
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

    private static boolean isRequiredRegion(
            ServerLevel level,
            BlockPos position,
            SpawnRegion region
    ) {
        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        if (region == SpawnRegion.CITY_STREET) {
            return LostCitiesCityResolver.isStreetChunk(
                    level,
                    chunkX,
                    chunkZ
            );
        }
        return !LostCitiesCityResolver.isCityChunk(
                level,
                chunkX,
                chunkZ
        );
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

    private static boolean isSpawnFrom(
            Zombie zombie,
            SpawnRegion region
    ) {
        return zombie.isAlive()
                && zombie.getPersistentData()
                .getBoolean(region.spawnTag());
    }

    private record SpawnSettings(
            SpawnRegion region,
            double chance,
            double nighttimeChanceMultiplier,
            int cap,
            int capRadius,
            int minimumDistance,
            int maximumDistance,
            int positionAttempts
    ) {
    }

    private enum SpawnRegion {
        CITY_STREET(STREET_SPAWN_TAG),
        WILDERNESS(WILDERNESS_SPAWN_TAG);

        private final String spawnTag;

        SpawnRegion(String spawnTag) {
            this.spawnTag = spawnTag;
        }

        private String spawnTag() {
            return spawnTag;
        }
    }

    private SurfaceZombieSpawner() {
    }
}
