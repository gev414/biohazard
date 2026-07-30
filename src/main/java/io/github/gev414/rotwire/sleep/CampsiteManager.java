package io.github.gev414.rotwire.sleep;

import io.github.gev414.rotwire.block.TarpBlock;
import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

final class CampsiteManager {

    static boolean tryPayForRest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos sleepingBagPosition
    ) {
        if (!hasCompleteTarp(level, sleepingBagPosition)) {
            return false;
        }

        int radius = SurvivalSystemsConfig.SLEEP_CAMPSITE_RADIUS.get();
        if (!hasLitCampfire(level, sleepingBagPosition, radius)) {
            return false;
        }

        return TravelersBackpackSleepIntegration.tryConsumeCampRations(
                level,
                player,
                sleepingBagPosition,
                radius,
                SurvivalSystemsConfig
                        .SLEEP_CAMPSITE_FOOD_NUTRITION_THRESHOLD
                        .get()
        );
    }

    private static boolean hasCompleteTarp(
            ServerLevel level,
            BlockPos sleepingBagPosition
    ) {
        BlockPos tarpPosition = sleepingBagPosition.above();
        BlockState tarpState = level.getBlockState(tarpPosition);
        return tarpState.getBlock() instanceof TarpBlock
                && TarpBlock.isComplete(
                        level,
                        tarpPosition,
                        tarpState
                );
    }

    private static boolean hasLitCampfire(
            ServerLevel level,
            BlockPos origin,
            int radius
    ) {
        BlockPos minimum = origin.offset(-radius, -radius, -radius);
        BlockPos maximum = origin.offset(radius, radius, radius);
        double maximumDistanceSquared = (double) radius * radius;

        for (BlockPos position : BlockPos.betweenClosed(minimum, maximum)) {
            if (origin.distSqr(position) > maximumDistanceSquared) {
                continue;
            }
            BlockState state = level.getBlockState(position);
            if (state.getBlock() instanceof CampfireBlock
                    && state.getValue(CampfireBlock.LIT)) {
                return true;
            }
        }
        return false;
    }

    private CampsiteManager() {
    }
}
