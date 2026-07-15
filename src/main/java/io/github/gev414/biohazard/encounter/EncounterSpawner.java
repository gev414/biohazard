package io.github.gev414.biohazard.encounter;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.config.EncounterConfig;
import io.github.gev414.biohazard.entity.BruteEntity;
import io.github.gev414.biohazard.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Zombie;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class EncounterSpawner {

    private static final int[] VERTICAL_OFFSETS = {
            0, 1, -1, 2, -2, 3, -3
    };
    private static final Set<String> WARNED_INVALID_MOBS =
            new HashSet<>();

    public static int countLoadedRegulars(
            ServerLevel level,
            BuildingDescriptor building
    ) {
        return level.getEntities(
                (Entity) null,
                building.bounds(),
                entity -> entity.isAlive()
                        && EncounterEntityData.matches(
                        entity,
                        building.key(),
                        EncounterEntityData.Role.REGULAR
                )
        ).size();
    }

    public static Optional<BruteEntity> findLoadedBoss(
            ServerLevel level,
            BuildingDescriptor building
    ) {
        return level.getEntities(
                        (Entity) null,
                        building.bounds(),
                        entity -> entity.isAlive()
                                && entity instanceof BruteEntity
                                && EncounterEntityData.matches(
                                entity,
                                building.key(),
                                EncounterEntityData.Role.BOSS
                        )
                ).stream()
                .map(BruteEntity.class::cast)
                .findFirst();
    }

    public static boolean spawnRegular(
            ServerLevel level,
            BuildingDescriptor building,
            List<ServerPlayer> occupants
    ) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }

        List<EntityType<?>> pool = regularMobPool();
        if (pool.isEmpty()) {
            return false;
        }

        RandomSource random = level.getRandom();
        EntityType<?> type = pool.get(
                random.nextInt(pool.size())
        );
        Entity createdEntity = type.create(level);
        if (!(createdEntity instanceof Mob mob)) {
            if (createdEntity != null) {
                createdEntity.discard();
            }
            warnInvalidMob(EntityType.getKey(type).toString());
            return false;
        }

        BlockPos spawnPosition = findSpawnPosition(
                level,
                building,
                occupants,
                mob,
                random
        );
        if (spawnPosition == null) {
            mob.discard();
            return false;
        }

        EncounterEntityData.mark(
                mob,
                building.key(),
                EncounterEntityData.Role.REGULAR
        );

        boolean spawned = level.addFreshEntity(mob);

        if (spawned && mob instanceof Zombie) {
            level.playSound(
                    null,
                    mob.blockPosition(),
                    SoundEvents.ZOMBIE_AMBIENT,
                    SoundSource.HOSTILE,
                    1.5F,
                    0.85F + level.getRandom().nextFloat() * 0.30F
            );
        }

        return spawned;
    }

    public static Optional<BruteEntity> spawnBoss(
            ServerLevel level,
            BuildingDescriptor building,
            List<ServerPlayer> occupants
    ) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return Optional.empty();
        }

        BruteEntity brute = ModEntities.BRUTE.get().create(level);
        if (brute == null) {
            return Optional.empty();
        }

        BlockPos spawnPosition = findSpawnPosition(
                level,
                building,
                occupants,
                brute,
                level.getRandom()
        );
        if (spawnPosition == null) {
            brute.discard();
            return Optional.empty();
        }

        brute.setHealth(brute.getMaxHealth());
        brute.setPersistenceRequired();
        EncounterEntityData.mark(
                brute,
                building.key(),
                EncounterEntityData.Role.BOSS
        );

        if (!level.addFreshEntity(brute)) {
            brute.discard();
            return Optional.empty();
        }
        return Optional.of(brute);
    }

    @Nullable
    private static BlockPos findSpawnPosition(
            ServerLevel level,
            BuildingDescriptor building,
            List<ServerPlayer> occupants,
            Mob mob,
            RandomSource random
    ) {
        if (occupants.isEmpty()) {
            return null;
        }

        double minimumDistance =
                EncounterConfig.minimumSpawnDistance();
        double maximumDistance =
                EncounterConfig.maximumSpawnDistance();

        for (int attempt = 0;
             attempt < EncounterConfig.SPAWN_POSITION_ATTEMPTS.get();
             attempt++) {
            ServerPlayer anchor = occupants.get(
                    random.nextInt(occupants.size())
            );
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

            for (int verticalOffset : VERTICAL_OFFSETS) {
                BlockPos candidate = new BlockPos(
                        x,
                        anchor.blockPosition().getY() + verticalOffset,
                        z
                );
                if (!building.contains(candidate)
                        || !isDistanceValid(
                        candidate,
                        occupants,
                        minimumDistance,
                        maximumDistance
                )
                        || !canSpawnAt(
                        level,
                        building.bounds(),
                        candidate,
                        mob
                )) {
                    continue;
                }

                mob.moveTo(
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

    private static boolean isDistanceValid(
            BlockPos position,
            List<ServerPlayer> occupants,
            double minimumDistance,
            double maximumDistance
    ) {
        double minimumSquared = minimumDistance * minimumDistance;
        double maximumSquared = maximumDistance * maximumDistance;
        boolean withinMaximumOfAnOccupant = false;

        for (ServerPlayer occupant : occupants) {
            double distanceSquared = occupant.distanceToSqr(
                    position.getX() + 0.5D,
                    position.getY(),
                    position.getZ() + 0.5D
            );
            if (distanceSquared < minimumSquared) {
                return false;
            }
            if (distanceSquared <= maximumSquared) {
                withinMaximumOfAnOccupant = true;
            }
        }
        return withinMaximumOfAnOccupant;
    }

    private static boolean canSpawnAt(
            ServerLevel level,
            AABB buildingBounds,
            BlockPos position,
            Mob mob
    ) {
        if (!level.isAreaLoaded(position, 0)
                || !level.getWorldBorder().isWithinBounds(position)
                || !level.getFluidState(position).isEmpty()
                || !level.getBlockState(position.below())
                .isFaceSturdy(level, position.below(), Direction.UP)) {
            return false;
        }

        mob.moveTo(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        AABB mobBounds = mob.getBoundingBox();
        if (mobBounds.minX < buildingBounds.minX
                || mobBounds.maxX > buildingBounds.maxX
                || mobBounds.minY < buildingBounds.minY
                || mobBounds.maxY > buildingBounds.maxY
                || mobBounds.minZ < buildingBounds.minZ
                || mobBounds.maxZ > buildingBounds.maxZ) {
            return false;
        }

        return level.noCollision(mob)
                && mob.checkSpawnObstruction(level);
    }

    private static List<EntityType<?>> regularMobPool() {
        List<EntityType<?>> pool = new ArrayList<>();
        for (String configuredId : EncounterConfig.REGULAR_MOBS.get()) {
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            EntityType<?> type = id == null
                    ? null
                    : BuiltInRegistries.ENTITY_TYPE
                    .getOptional(id)
                    .orElse(null);

            if (type == null
                    || type == ModEntities.BRUTE.get()
                    || type.getCategory() != MobCategory.MONSTER) {
                warnInvalidMob(configuredId);
                continue;
            }
            pool.add(type);
        }
        return pool;
    }

    private static void warnInvalidMob(String id) {
        if (WARNED_INVALID_MOBS.add(id)) {
            Biohazard.LOGGER.warn(
                    "Ignoring invalid regular encounter mob '{}'; "
                            + "entries must be non-Brute hostile mobs",
                    id
            );
        }
    }

    private EncounterSpawner() {
    }
}
