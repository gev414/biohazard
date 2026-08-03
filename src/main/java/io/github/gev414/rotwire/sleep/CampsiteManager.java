package io.github.gev414.rotwire.sleep;

import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

final class CampsiteManager {

    private static final int MAX_SHELTER_CENTER_OFFSET = 5;

    static boolean tryPayForRest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos sleepingBagPosition
    ) {
        Optional<CampsiteArea> campsite = findCampsite(
                level,
                sleepingBagPosition
        );
        if (campsite.isEmpty()) {
            return false;
        }

        CampsiteArea area = campsite.get();
        if (!CampInspector.hasLitCampfire(
                level,
                area.center(),
                area.radius()
        )) {
            return false;
        }

        return CampContainerSupplies.tryConsumeCampRations(
                level,
                player,
                area.center(),
                area.radius(),
                SurvivalSystemsConfig
                        .SLEEP_CAMPSITE_FOOD_NUTRITION_THRESHOLD
                        .get()
        );
    }

    static boolean isPlayerInReadyCampsite(
            ServerLevel level,
            ServerPlayer player
    ) {
        int searchRadius = readinessSearchRadius(
                SurvivalSystemsConfig.SLEEP_CAMPSITE_RADIUS.get()
        );
        BlockPos playerPosition = player.blockPosition();
        BlockPos minimum = playerPosition.offset(
                -searchRadius,
                -searchRadius,
                -searchRadius
        );
        BlockPos maximum = playerPosition.offset(
                searchRadius,
                searchRadius,
                searchRadius
        );

        for (BlockPos mutable : BlockPos.betweenClosed(minimum, maximum)) {
            if (!TravelersBackpackSleepIntegration.isSleepingBag(
                    level.getBlockState(mutable)
            )) {
                continue;
            }

            Optional<CampsiteArea> campsite = findCampsite(
                    level,
                    mutable
            );
            if (campsite.isEmpty()) {
                continue;
            }

            CampsiteArea area = campsite.get();
            if (area.contains(playerPosition)
                    && isReady(level, player, area)) {
                return true;
            }
        }
        return false;
    }

    static int readinessSearchRadius(int configuredRadius) {
        return Math.max(configuredRadius, 6)
                + MAX_SHELTER_CENTER_OFFSET;
    }

    private static boolean isReady(
            ServerLevel level,
            ServerPlayer player,
            CampsiteArea area
    ) {
        return CampInspector.hasLitCampfire(
                level,
                area.center(),
                area.radius()
        )
                && CampContainerSupplies.hasCampRations(
                        level,
                        player,
                        area.center(),
                        area.radius(),
                        SurvivalSystemsConfig
                                .SLEEP_CAMPSITE_FOOD_NUTRITION_THRESHOLD
                                .get()
                );
    }

    private static Optional<CampsiteArea> findCampsite(
            ServerLevel level,
            BlockPos sleepingBagPosition
    ) {
        return CampInspector.findShelter(
                level,
                sleepingBagPosition
        ).map(area -> new CampsiteArea(
                area.center(),
                area.radius()
        ));
    }

    private record CampsiteArea(BlockPos center, int radius) {

        private boolean contains(BlockPos position) {
            return center.distSqr(position)
                    <= (double) radius * radius;
        }
    }

    private CampsiteManager() {
    }
}
