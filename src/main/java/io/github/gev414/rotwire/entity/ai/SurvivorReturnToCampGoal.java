package io.github.gev414.rotwire.entity.ai;

import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Gives urgent retreat orders a higher priority than ordinary city roaming.
 */
public final class SurvivorReturnToCampGoal extends Goal {

    private static final int PATH_REFRESH_TICKS = 20;

    private final SurvivorEntity survivor;
    private final double speedModifier;
    private int pathRefreshTicks;

    public SurvivorReturnToCampGoal(
            SurvivorEntity survivor,
            double speedModifier
    ) {
        this.survivor = survivor;
        this.speedModifier = speedModifier;
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
        if (--pathRefreshTicks <= 0
                || survivor.getNavigation().isDone()) {
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
        if (home == null) {
            return;
        }
        pathRefreshTicks = PATH_REFRESH_TICKS;
        survivor.getNavigation().moveTo(
                home.getX() + 0.5D,
                home.getY(),
                home.getZ() + 0.5D,
                speedModifier
        );
    }
}
