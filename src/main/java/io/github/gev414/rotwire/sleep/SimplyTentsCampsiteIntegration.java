package io.github.gev414.rotwire.sleep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry-only integration so SimplyTents remains an optional dependency.
 */
final class SimplyTentsCampsiteIntegration {

    private static final String MOD_ID = "simplytents";
    private static final int MAX_HORIZONTAL_SCAN = 4;
    private static final int MAX_CORE_HEIGHT = 4;

    private static final TentFootprint COMPACT =
            new TentFootprint(1, 1, 3, 3, false);
    private static final TentFootprint COMPACT_ZIP =
            new TentFootprint(1, 2, 3, 3, false);
    private static final TentFootprint DUO =
            new TentFootprint(2, 2, 3, 4, false);
    private static final TentFootprint DUO_ZIP =
            new TentFootprint(2, 3, 3, 4, false);
    private static final TentFootprint LARGE =
            new TentFootprint(3, 3, 3, 5, false);
    private static final TentFootprint LARGE_ZIP =
            new TentFootprint(3, 4, 3, 5, false);
    private static final TentFootprint TIPI =
            new TentFootprint(2, 2, 4, 4, false);
    private static final TentFootprint YURT =
            new TentFootprint(4, 4, 3, 6, true);

    private static final Map<ResourceLocation, TentFootprint> TENTS =
            Map.ofEntries(
                    tent("tent", COMPACT),
                    tent("wall_tent", COMPACT),
                    tent("roof_tent", COMPACT),
                    tent("zip_tent", COMPACT_ZIP),
                    tent("small_tipi_tent", COMPACT),
                    tent("duo_tent", DUO),
                    tent("duo_wall_tent", DUO),
                    tent("duo_roof_tent", DUO),
                    tent("duo_zip_tent", DUO_ZIP),
                    tent("large_tent", LARGE),
                    tent("large_wall_tent", LARGE),
                    tent("large_roof_tent", LARGE),
                    tent("large_zip_tent", LARGE_ZIP),
                    tent("tipi_tent", TIPI),
                    tent("yurt_tent", YURT)
            );
    private static final Map<ResourceLocation, CampStatus.ShelterType>
            SHELTER_TYPES = shelterTypes();

    static Optional<Shelter> findShelter(
            ServerLevel level,
            BlockPos sleepingBagPosition
    ) {
        ShelterCandidate best = null;

        for (int y = 1; y <= MAX_CORE_HEIGHT; y++) {
            for (int x = -MAX_HORIZONTAL_SCAN;
                    x <= MAX_HORIZONTAL_SCAN;
                    x++) {
                for (int z = -MAX_HORIZONTAL_SCAN;
                        z <= MAX_HORIZONTAL_SCAN;
                        z++) {
                    BlockPos anchor = sleepingBagPosition.offset(x, y, z);
                    BlockState state = level.getBlockState(anchor);
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(
                            state.getBlock()
                    );
                    TentFootprint footprint = TENTS.get(blockId);
                    if (footprint == null) {
                        continue;
                    }

                    Direction facing = state.hasProperty(
                            BlockStateProperties.HORIZONTAL_FACING
                    )
                            ? state.getValue(
                                    BlockStateProperties.HORIZONTAL_FACING
                            )
                            : Direction.NORTH;
                    if (!footprint.contains(
                            anchor,
                            sleepingBagPosition,
                            facing
                    )) {
                        continue;
                    }

                    ShelterCandidate candidate = new ShelterCandidate(
                            new Shelter(
                                    anchor.immutable(),
                                    footprint.campsiteCenter(anchor),
                                    footprint.minimumCampsiteRadius(),
                                    SHELTER_TYPES.getOrDefault(
                                            blockId,
                                            CampStatus.ShelterType.NONE
                                    )
                            ),
                            sleepingBagPosition.distSqr(anchor),
                            anchor.asLong()
                    );
                    if (best == null
                            || ShelterCandidate.ORDER.compare(
                                    candidate,
                                    best
                            ) < 0) {
                        best = candidate;
                    }
                }
            }
        }

        return best == null
                ? Optional.empty()
                : Optional.of(best.shelter());
    }

    static TentFootprint footprint(ResourceLocation blockId) {
        return TENTS.get(blockId);
    }

    private static Map.Entry<ResourceLocation, TentFootprint> tent(
            String path,
            TentFootprint footprint
    ) {
        return Map.entry(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, path),
                footprint
        );
    }

    private static Map<ResourceLocation, CampStatus.ShelterType>
            shelterTypes() {
        Map<ResourceLocation, CampStatus.ShelterType> types =
                new HashMap<>();
        putTypes(types, CampStatus.ShelterType.COMPACT_TENT,
                "tent", "wall_tent", "roof_tent", "zip_tent");
        putTypes(types, CampStatus.ShelterType.SMALL_TIPI,
                "small_tipi_tent");
        putTypes(types, CampStatus.ShelterType.DUO_TENT,
                "duo_tent", "duo_wall_tent", "duo_roof_tent",
                "duo_zip_tent");
        putTypes(types, CampStatus.ShelterType.LARGE_TENT,
                "large_tent", "large_wall_tent", "large_roof_tent",
                "large_zip_tent");
        putTypes(types, CampStatus.ShelterType.TIPI, "tipi_tent");
        putTypes(types, CampStatus.ShelterType.YURT, "yurt_tent");
        return Map.copyOf(types);
    }

    private static void putTypes(
            Map<ResourceLocation, CampStatus.ShelterType> types,
            CampStatus.ShelterType type,
            String... paths
    ) {
        for (String path : paths) {
            types.put(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, path),
                    type
            );
        }
    }

    record Shelter(
            BlockPos anchor,
            BlockPos center,
            int minimumCampsiteRadius,
            CampStatus.ShelterType type
    ) {
        int effectiveRadius(int configuredRadius) {
            return Math.max(configuredRadius, minimumCampsiteRadius);
        }
    }

    record TentFootprint(
            int halfWidth,
            int halfLength,
            int coreHeight,
            int minimumCampsiteRadius,
            boolean roundedYurt
    ) {

        boolean contains(
                BlockPos anchor,
                BlockPos sleepingBagPosition,
                Direction facing
        ) {
            int verticalOffset =
                    anchor.getY() - sleepingBagPosition.getY();
            if (verticalOffset < 1 || verticalOffset > coreHeight) {
                return false;
            }

            int worldX = sleepingBagPosition.getX() - anchor.getX();
            int worldZ = sleepingBagPosition.getZ() - anchor.getZ();
            LocalOffset local = toLocal(worldX, worldZ, facing);
            int x = Math.abs(local.x());
            int z = Math.abs(local.z());
            if (!roundedYurt) {
                return x <= halfWidth && z <= halfLength;
            }

            return switch (z) {
                case 0, 1, 2 -> x <= 4;
                case 3 -> x <= 3;
                case 4 -> x <= 2;
                default -> false;
            };
        }

        BlockPos campsiteCenter(BlockPos anchor) {
            return anchor.below(coreHeight);
        }

        private static LocalOffset toLocal(
                int worldX,
                int worldZ,
                Direction facing
        ) {
            return switch (facing) {
                case SOUTH -> new LocalOffset(-worldX, -worldZ);
                case EAST -> new LocalOffset(worldZ, -worldX);
                case WEST -> new LocalOffset(-worldZ, worldX);
                default -> new LocalOffset(worldX, worldZ);
            };
        }
    }

    private record LocalOffset(int x, int z) {
    }

    private record ShelterCandidate(
            Shelter shelter,
            double distanceSquared,
            long anchorPosition
    ) {

        private static final Comparator<ShelterCandidate> ORDER =
                Comparator.comparingDouble(
                                ShelterCandidate::distanceSquared
                        )
                        .thenComparingLong(
                                ShelterCandidate::anchorPosition
                        );
    }

    private SimplyTentsCampsiteIntegration() {
    }
}
