package io.github.gev414.rotwire.entity.ai;

import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Gives urgent retreat orders a higher priority than ordinary city roaming.
 */
public final class SurvivorReturnToCampGoal extends Goal {

    private static final int BUDGET_RETRY_TICKS = 2;
    private static final double MAX_RETURN_PATH_SEGMENT = 16.0D;
    private static final int[][] CAMP_APPROACH_OFFSETS = {
            {3, 0},
            {-3, 0},
            {0, 3},
            {0, -3},
            {2, 2},
            {2, -2},
            {-2, 2},
            {-2, -2}
    };
    private static final int[] APPROACH_VERTICAL_OFFSETS = {
            0,
            1,
            -1,
            2,
            -2,
            3,
            -3,
            4,
            -4
    };

    private final SurvivorEntity survivor;
    private final double speedModifier;
    private int pathRefreshTicks;
    private int nextApproachIndex;

    public SurvivorReturnToCampGoal(
            SurvivorEntity survivor,
            double speedModifier
    ) {
        this.survivor = survivor;
        this.speedModifier = speedModifier;
        nextApproachIndex = survivor.getRandom().nextInt(
                CAMP_APPROACH_OFFSETS.length
        );
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return survivor.shouldReturnToCamp()
                && !survivor.isAtCampRetreatPoint();
    }

    @Override
    public boolean canContinueToUse() {
        return survivor.shouldReturnToCamp()
                && !survivor.isAtCampRetreatPoint();
    }

    @Override
    public void start() {
        pathRefreshTicks = 0;
        moveHome();
    }

    @Override
    public void tick() {
        if (pathRefreshTicks > 0) {
            pathRefreshTicks--;
        }
        if (survivor.getNavigation().isDone()
                && pathRefreshTicks == 0) {
            moveHome();
        }
    }

    @Override
    public void stop() {
        if (survivor.isAtCampRetreatPoint()) {
            survivor.getNavigation().stop();
            survivor.completeReturnToCampOrder();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void moveHome() {
        BlockPos home = survivor.homePosition();
        if (home == null
                || !(survivor.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos approach = nextCampApproach(home);
        if (approach == null) {
            scheduleFailedRetry();
            return;
        }
        if (!SurvivorNavigationBudget.requestPath(level)) {
            pathRefreshTicks = BUDGET_RETRY_TICKS;
            return;
        }
        long pathStartedAt = System.nanoTime();
        Path path = survivor.getNavigation().createPath(
                segmentDestination(approach),
                0
        );
        SurvivorNavigationBudget.recordPathCalculation(
                level,
                System.nanoTime() - pathStartedAt
        );
        boolean moving = path != null
                && survivor.getNavigation().moveTo(path, speedModifier);
        if (!moving || !path.canReach()) {
            scheduleFailedRetry();
        }
    }

    /**
     * The radio may be enclosed by a tent, and therefore is not a valid mob
     * destination. Rally and retreat orders instead rotate through feet-level
     * points on the accepted 3-4 block shelter ring.
     */
    private BlockPos nextCampApproach(BlockPos home) {
        double retreatDistance = SettlementConfig
                .SURVIVOR_CAMP_RETREAT_DISTANCE.get();
        for (int checked = 0;
                checked < CAMP_APPROACH_OFFSETS.length;
                checked++) {
            int index = (nextApproachIndex + checked)
                    % CAMP_APPROACH_OFFSETS.length;
            int[] offset = CAMP_APPROACH_OFFSETS[index];
            double horizontalDistance = Math.sqrt(
                    offset[0] * offset[0] + offset[1] * offset[1]
            );
            if (horizontalDistance > retreatDistance) {
                continue;
            }
            BlockPos approach = standingPositionNear(
                    home.offset(offset[0], 0, offset[1])
            );
            if (approach != null) {
                nextApproachIndex = (index + 1)
                        % CAMP_APPROACH_OFFSETS.length;
                return approach;
            }
        }
        nextApproachIndex = (nextApproachIndex + 1)
                % CAMP_APPROACH_OFFSETS.length;
        return null;
    }

    /** Finds feet-level camp ground instead of a tent or building roof. */
    private BlockPos standingPositionNear(BlockPos base) {
        for (int vertical : APPROACH_VERTICAL_OFFSETS) {
            BlockPos feet = base.offset(0, vertical, 0);
            BlockPos head = feet.above();
            BlockPos support = feet.below();
            if (!survivor.level().getBlockState(feet)
                            .getCollisionShape(survivor.level(), feet)
                            .isEmpty()
                    || !survivor.level().getBlockState(head)
                            .getCollisionShape(survivor.level(), head)
                            .isEmpty()
                    || !survivor.level().getFluidState(feet).isEmpty()
                    || !survivor.level().getFluidState(head).isEmpty()
                    || !survivor.level().getBlockState(support).isFaceSturdy(
                            survivor.level(),
                            support,
                            Direction.UP
                    )) {
                continue;
            }
            return feet.immutable();
        }
        return null;
    }

    private void scheduleFailedRetry() {
        int base = SettlementConfig.SURVIVOR_RETURN_PATH_RETRY_TICKS.get();
        pathRefreshTicks = base + survivor.getRandom().nextInt(
                Math.max(1, base / 2)
        );
    }

    private BlockPos segmentDestination(BlockPos destination) {
        double deltaX = destination.getX() + 0.5D - survivor.getX();
        double deltaY = destination.getY() - survivor.getY();
        double deltaZ = destination.getZ() + 0.5D - survivor.getZ();
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontal <= MAX_RETURN_PATH_SEGMENT) {
            return destination;
        }
        double ratio = MAX_RETURN_PATH_SEGMENT / horizontal;
        return BlockPos.containing(
                survivor.getX() + deltaX * ratio,
                survivor.getY() + deltaY * ratio,
                survivor.getZ() + deltaZ * ratio
        );
    }
}
