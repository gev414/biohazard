package io.github.gev414.rotwire.entity.ai;

import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Settlement guard combat driven by the actual PointBlank Mosin held in the
 * survivor's main hand. Armed survivors maintain a weapon-specific minimum
 * distance, use bounded lateral repositioning when sight is blocked, and do
 * not follow an unseen target directly into melee range.
 */
public final class SurvivorMosinAttackGoal extends Goal {

    private static final int PATH_REFRESH_TICKS = 20;
    private static final int EMPTY_MAGAZINE_RELOAD_TICKS = 40;
    private static final double APPROACH_SPEED = 0.95D;
    private static final double RETREAT_SPEED = 1.10D;
    private static final int RETREAT_HORIZONTAL_DISTANCE = 10;
    private static final int RETREAT_VERTICAL_DISTANCE = 5;

    private final SurvivorEntity survivor;
    private int pathRefreshTicks;
    private int repositionCooldown;
    private double shotCooldownTicks;
    private int reloadTicks;
    private boolean tacticalPath;

    public SurvivorMosinAttackGoal(SurvivorEntity survivor) {
        this.survivor = survivor;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = survivor.getTarget();
        return target != null
                && target.isAlive()
                && survivor.canFightWithFirearm();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = survivor.getTarget();
        return target != null
                && target.isAlive()
                && survivor.canFightWithFirearm();
    }

    @Override
    public void start() {
        pathRefreshTicks = 0;
        int interval = SettlementConfig
                .SURVIVOR_REPOSITION_INTERVAL_TICKS.get();
        repositionCooldown = survivor.getRandom().nextInt(
                Math.max(1, interval)
        );
        shotCooldownTicks = 0;
        reloadTicks = 0;
        tacticalPath = false;
    }

    @Override
    public void stop() {
        survivor.getNavigation().stop();
        reloadTicks = 0;
        tacticalPath = false;
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
        if (pathRefreshTicks > 0) {
            pathRefreshTicks--;
        }
        if (repositionCooldown > 0) {
            repositionCooldown--;
        }

        double distanceSqr = survivor.distanceToSqr(target);
        int maximumDistance = survivor.firearmMaximumShootingDistance();
        double minimumDistance = survivor
                .firearmMinimumEngagementDistance();
        boolean hasLineOfSight = survivor.getSensing()
                .hasLineOfSight(target);
        if (maximumDistance <= 0) {
            survivor.getNavigation().stop();
            return;
        }

        if (survivor.firearmMagazineRounds() <= 0) {
            if (distanceSqr < minimumDistance * minimumDistance) {
                retreatFromTarget(target);
            } else {
                tacticalPath = false;
                survivor.getNavigation().stop();
            }
            reloadMosin();
            return;
        }

        if (distanceSqr < minimumDistance * minimumDistance) {
            retreatFromTarget(target);
            if (hasLineOfSight) {
                tryFire(target);
            }
            return;
        }

        if (hasLineOfSight
                && distanceSqr <= (double) maximumDistance * maximumDistance) {
            tacticalPath = false;
            survivor.getNavigation().stop();
            tryFire(target);
            return;
        }

        if (distanceSqr > (double) maximumDistance * maximumDistance) {
            tacticalPath = false;
            survivor.getNavigation().stop();
            return;
        }

        repositionOrHold(target, minimumDistance, maximumDistance);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void reloadMosin() {
        if (reloadTicks == 0) {
            reloadTicks = EMPTY_MAGAZINE_RELOAD_TICKS;
            return;
        }
        if (--reloadTicks == 0) {
            survivor.reloadFirearmFromSettlement();
        }
    }

    private void retreatFromTarget(LivingEntity target) {
        if (tacticalPath && !survivor.getNavigation().isDone()) {
            return;
        }
        if (pathRefreshTicks > 0) {
            survivor.getNavigation().stop();
            return;
        }
        Vec3 candidate = DefaultRandomPos.getPosAway(
                survivor,
                RETREAT_HORIZONTAL_DISTANCE,
                RETREAT_VERTICAL_DISTANCE,
                target.position()
        );
        pathRefreshTicks = PATH_REFRESH_TICKS;
        if (candidate == null
                || candidate.distanceToSqr(target.position())
                <= survivor.distanceToSqr(target)
                || !survivor.isTacticalPositionAllowed(
                        BlockPos.containing(candidate)
                )) {
            tacticalPath = false;
            survivor.getNavigation().stop();
            return;
        }
        if (!(survivor.level() instanceof ServerLevel level)
                || !SurvivorNavigationBudget.requestPath(level)) {
            tacticalPath = false;
            survivor.getNavigation().stop();
            return;
        }
        long pathStartedAt = System.nanoTime();
        tacticalPath = survivor.getNavigation().moveTo(
                candidate.x,
                candidate.y,
                candidate.z,
                RETREAT_SPEED
        );
        SurvivorNavigationBudget.recordPathCalculation(
                level,
                System.nanoTime() - pathStartedAt
        );
    }

    private void repositionOrHold(
            LivingEntity target,
            double minimumDistance,
            int maximumDistance
    ) {
        if (tacticalPath && !survivor.getNavigation().isDone()) {
            return;
        }
        tacticalPath = false;
        survivor.getNavigation().stop();
        if (repositionCooldown > 0) {
            return;
        }
        repositionCooldown = SettlementConfig
                .SURVIVOR_REPOSITION_INTERVAL_TICKS.get();

        int attempts = SettlementConfig.SURVIVOR_REPOSITION_ATTEMPTS.get();
        int radius = SettlementConfig.SURVIVOR_REPOSITION_RADIUS.get();
        double currentTargetDistance = survivor.distanceToSqr(target);
        double preferredDistance = Math.min(
                maximumDistance * 0.75D,
                Math.max(minimumDistance + 3.0D, minimumDistance * 1.5D)
        );
        Vec3 best = null;
        double bestScore = Double.MAX_VALUE;
        for (int attempt = 0; attempt < attempts; attempt++) {
            Vec3 candidate = LandRandomPos.getPos(survivor, radius, 4);
            if (candidate == null
                    || !survivor.isTacticalPositionAllowed(
                            BlockPos.containing(candidate)
                    )) {
                continue;
            }
            double candidateTargetDistance = candidate.distanceToSqr(
                    target.position()
            );
            if (candidateTargetDistance + 1.0D < currentTargetDistance
                    || !hasClearShotFrom(candidate, target)) {
                continue;
            }
            double score = Math.abs(
                    Math.sqrt(candidateTargetDistance) - preferredDistance
            ) + candidate.distanceTo(survivor.position()) * 0.15D;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null) {
            if (!(survivor.level() instanceof ServerLevel level)
                    || !SurvivorNavigationBudget.requestPath(level)) {
                return;
            }
            long pathStartedAt = System.nanoTime();
            tacticalPath = survivor.getNavigation().moveTo(
                    best.x,
                    best.y,
                    best.z,
                    APPROACH_SPEED
            );
            SurvivorNavigationBudget.recordPathCalculation(
                    level,
                    System.nanoTime() - pathStartedAt
            );
        }
    }

    private boolean hasClearShotFrom(Vec3 candidate, LivingEntity target) {
        Vec3 origin = candidate.add(0.0D, survivor.getEyeHeight(), 0.0D);
        BlockHitResult hit = survivor.level().clip(new ClipContext(
                origin,
                target.getEyePosition(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                survivor
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void tryFire(LivingEntity target) {
        if (shotCooldownTicks <= 0
                && survivor.fireFirearmAt(target)) {
            // Preserve fractional shot intervals: 800 RPM is 1.5 game ticks,
            // so carrying the remainder alternates one- and two-tick gaps.
            shotCooldownTicks += survivor.firearmShotCooldownTicks();
        }
    }
}
