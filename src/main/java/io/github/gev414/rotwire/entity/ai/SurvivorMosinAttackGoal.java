package io.github.gev414.rotwire.entity.ai;

import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Settlement guard combat driven by the actual PointBlank Mosin held in the
 * survivor's main hand. Navigation is only used to gain line of sight or to
 * close to the rifle's configured shooting distance; firing remains hitscan
 * so the guard is not coupled to a fake player input path.
 */
public final class SurvivorMosinAttackGoal extends Goal {

    private static final int PATH_REFRESH_TICKS = 10;
    private static final int EMPTY_MAGAZINE_RELOAD_TICKS = 40;
    private static final double APPROACH_SPEED = 0.95D;

    private final SurvivorEntity survivor;
    private int pathRefreshTicks;
    private int shotCooldownTicks;
    private int reloadTicks;

    public SurvivorMosinAttackGoal(SurvivorEntity survivor) {
        this.survivor = survivor;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = survivor.getTarget();
        return target != null
                && target.isAlive()
                && survivor.canFightWithMosin();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = survivor.getTarget();
        return target != null
                && target.isAlive()
                && survivor.canFightWithMosin();
    }

    @Override
    public void start() {
        pathRefreshTicks = 0;
        shotCooldownTicks = 0;
        reloadTicks = 0;
    }

    @Override
    public void stop() {
        survivor.getNavigation().stop();
        reloadTicks = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = survivor.getTarget();
        if (target == null) {
            return;
        }
        survivor.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (shotCooldownTicks > 0) {
            shotCooldownTicks--;
        }

        if (survivor.mosinMagazineRounds() <= 0) {
            reloadMosin();
            return;
        }

        double distanceSqr = survivor.distanceToSqr(target);
        int maximumDistance = survivor.mosinMaximumShootingDistance();
        if (maximumDistance <= 0
                || distanceSqr > (double) maximumDistance * maximumDistance
                || !survivor.getSensing().hasLineOfSight(target)) {
            approachTarget(target);
            return;
        }

        survivor.getNavigation().stop();
        if (shotCooldownTicks <= 0
                && survivor.fireMosinAt(target)) {
            shotCooldownTicks = survivor.mosinShotCooldownTicks();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void reloadMosin() {
        survivor.getNavigation().stop();
        if (reloadTicks == 0) {
            reloadTicks = EMPTY_MAGAZINE_RELOAD_TICKS;
            return;
        }
        if (--reloadTicks == 0) {
            survivor.reloadMosinFromSettlement();
        }
    }

    private void approachTarget(LivingEntity target) {
        if (--pathRefreshTicks <= 0
                || survivor.getNavigation().isDone()) {
            pathRefreshTicks = PATH_REFRESH_TICKS;
            survivor.getNavigation().moveTo(target, APPROACH_SPEED);
        }
    }
}
