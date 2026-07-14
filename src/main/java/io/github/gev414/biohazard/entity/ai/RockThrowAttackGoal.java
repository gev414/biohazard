package io.github.gev414.biohazard.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;

import java.util.EnumSet;

public final class RockThrowAttackGoal<T extends Mob & RangedAttackMob>
        extends Goal {

    private final T mob;

    private final double minimumRangeSqr;
    private final double maximumRangeSqr;

    private final int windupTicks;
    private final int cooldownTicks;

    private LivingEntity target;
    private int remainingWindupTicks;
    private long nextAttackGameTime;

    public RockThrowAttackGoal(
            T mob,
            double minimumRange,
            double maximumRange,
            int windupTicks,
            int cooldownTicks
    ) {
        if (minimumRange < 0.0D) {
            throw new IllegalArgumentException(
                    "Minimum range cannot be negative"
            );
        }

        if (maximumRange <= minimumRange) {
            throw new IllegalArgumentException(
                    "Maximum range must be greater than minimum range"
            );
        }

        if (windupTicks <= 0) {
            throw new IllegalArgumentException(
                    "Windup ticks must be greater than zero"
            );
        }

        if (cooldownTicks < 0) {
            throw new IllegalArgumentException(
                    "Cooldown ticks cannot be negative"
            );
        }

        this.mob = mob;
        this.minimumRangeSqr = minimumRange * minimumRange;
        this.maximumRangeSqr = maximumRange * maximumRange;
        this.windupTicks = windupTicks;
        this.cooldownTicks = cooldownTicks;

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long currentGameTime = this.mob.level().getGameTime();

        if (currentGameTime < this.nextAttackGameTime) {
            return false;
        }

        LivingEntity currentTarget = this.mob.getTarget();

        if (!this.isValidTarget(currentTarget)) {
            return false;
        }

        this.target = currentTarget;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.remainingWindupTicks > 0
                && this.target == this.mob.getTarget()
                && this.isValidTarget(this.target);
    }

    @Override
    public void start() {
        this.remainingWindupTicks = this.windupTicks;
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }

        this.mob.getNavigation().stop();

        this.mob.getLookControl().setLookAt(
                this.target,
                30.0F,
                30.0F
        );

        long currentGameTime = this.mob.level().getGameTime();

        this.remainingWindupTicks--;

        if (this.remainingWindupTicks > 0) {
            return;
        }

        double distanceSqr = this.mob.distanceToSqr(this.target);

        float distanceFactor = (float) Math.sqrt(
                distanceSqr / this.maximumRangeSqr
        );

        distanceFactor = Math.min(distanceFactor, 1.0F);

        this.mob.performRangedAttack(
                this.target,
                distanceFactor
        );

        this.nextAttackGameTime =
                currentGameTime + this.cooldownTicks;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean isValidTarget(LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive()) {
            return false;
        }

        double distanceSqr = this.mob.distanceToSqr(candidate);

        if (distanceSqr < this.minimumRangeSqr
                || distanceSqr > this.maximumRangeSqr) {
            return false;
        }

        return this.mob.getSensing().hasLineOfSight(candidate);
    }
}