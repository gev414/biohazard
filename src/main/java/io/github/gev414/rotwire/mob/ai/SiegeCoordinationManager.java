package io.github.gev414.rotwire.mob.ai;

import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.settlement.SiegeBreachRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Objective-scoped tactical memory for infected pursuing a protected target.
 * It shares failed-route evidence and one guarded structural breach lane while
 * leaving each mob's short local path to Minecraft navigation.
 */
final class SiegeCoordinationManager {

    private static final int GROUP_EXPIRY_TICKS = 600;
    private static final int TARGET_MEMORY_TICKS = 120;
    private static final int ROUTE_FAILURE_MEMORY_TICKS = 200;
    private static final int OPENING_VALIDATION_TICKS = 200;
    private static final int OPENING_MEMORY_TICKS = 1_200;
    private static final double PROGRESS_DECAY_PER_TICK = 0.25D;
    private static final int CANDIDATE_RADIUS = 2;
    private static final int MAX_TERRAIN_ESCAPE_DROP = 8;
    private static final Map<GroupKey, SiegeGroup> GROUPS = new HashMap<>();

    static void publishTarget(
            ServerLevel level,
            BlockPos campCenter,
            LivingEntity target
    ) {
        if (!target.isAlive()) {
            return;
        }
        SiegeGroup group = group(level, campCenter);
        group.sharedTarget = target;
        group.targetExpiresAt = level.getGameTime() + TARGET_MEMORY_TICKS;
    }

    static @Nullable LivingEntity sharedTarget(
            ServerLevel level,
            BlockPos campCenter
    ) {
        SiegeGroup group = group(level, campCenter);
        LivingEntity target = group.sharedTarget;
        if (target == null
                || !target.isAlive()
                || target.level() != level
                || group.targetExpiresAt < level.getGameTime()) {
            group.sharedTarget = null;
            group.targetExpiresAt = -1L;
            return null;
        }
        return target;
    }

    static void recordRouteFailure(
            ServerLevel level,
            BlockPos campCenter,
            Mob mob
    ) {
        SiegeGroup group = group(level, campCenter);
        group.routeFailures.put(mob.getUUID(), level.getGameTime());
    }

    static void recordRouteSuccess(
            ServerLevel level,
            BlockPos campCenter,
            Mob mob,
            boolean validatesOpening
    ) {
        SiegeGroup group = group(level, campCenter);
        group.routeFailures.remove(mob.getUUID());
        if (validatesOpening
                && group.plan != null
                && group.plan.awaitingRouteValidation
                && group.plan.validators.contains(mob.getUUID())) {
            finishPlan(group);
        }
    }

    static long openingRevision(
            ServerLevel level,
            BlockPos campCenter
    ) {
        return group(level, campCenter).openingRevision;
    }

    static long recordLocalOpening(
            ServerLevel level,
            BlockPos campCenter,
            BlockPos position
    ) {
        SiegeGroup group = group(level, campCenter);
        finishPlan(group);
        int[] step = horizontalStep(position, campCenter);
        publishOpening(group, position, step[0], step[1], false);
        return group.openingRevision;
    }

    static @Nullable OpeningView opening(
            ServerLevel level,
            BlockPos campCenter,
            Mob mob
    ) {
        return openingView(group(level, campCenter), mob);
    }

    static void recordOpeningTraversed(
            ServerLevel level,
            BlockPos campCenter,
            Mob mob,
            long revision
    ) {
        SiegeGroup group = group(level, campCenter);
        if (group.opening == null
                || group.opening.revision != revision) {
            return;
        }
        group.routeFailures.remove(mob.getUUID());
        if (group.plan != null
                && group.plan.awaitingRouteValidation) {
            finishPlan(group);
        }
    }

    static @Nullable BreachView plan(
            ServerLevel level,
            BlockPos campCenter,
            Mob mob
    ) {
        SiegeGroup group = group(level, campCenter);
        validatePlan(group);
        if (group.plan != null
                && group.plan.terrainEscape
                && group.plan.origin.distToCenterSqr(mob.position())
                > 64.0D) {
            return null;
        }
        return activeView(group.plan);
    }

    static @Nullable BreachView requestPlan(
            ServerLevel level,
            BlockPos campCenter,
            Mob scout,
            int individualFailures
    ) {
        SiegeGroup group = group(level, campCenter);
        validatePlan(group);
        if (group.plan != null) {
            if (group.plan.terrainEscape
                    && group.plan.origin.distToCenterSqr(scout.position())
                    > 64.0D) {
                return null;
            }
            if (!group.plan.awaitingRouteValidation) {
                return view(group.plan);
            }
            if (!hasStructuralEvidence(group, individualFailures)
                    || !group.routeFailures.containsKey(scout.getUUID())
                    || !group.plan.validators.contains(scout.getUUID())) {
                return null;
            }
            advancePlan(group);
            return activeView(group.plan);
        }
        if (!SettlementConfig.SIEGE_STRUCTURAL_BREACH_ENABLED.get()
                || !hasStructuralEvidence(group, individualFailures)
                || !group.routeFailures.containsKey(scout.getUUID())) {
            return null;
        }
        group.plan = selectPlan(group, scout);
        return view(group.plan);
    }

    static Contribution contribute(
            ServerLevel level,
            BlockPos campCenter,
            Mob mob
    ) {
        SiegeGroup group = group(level, campCenter);
        validatePlan(group);
        BreachPlan plan = group.plan;
        if (plan == null || plan.awaitingRouteValidation) {
            return Contribution.NONE;
        }
        if (plan.target.distToCenterSqr(mob.position()) > 16.0D) {
            return new Contribution(false, plan.target, true, false);
        }
        plan.validators.add(mob.getUUID());
        long now = level.getGameTime();
        if (plan.contributorTick != now) {
            plan.contributorTick = now;
            plan.contributors = 0;
        }
        if (plan.contributors >= SettlementConfig
                .SIEGE_STRUCTURAL_MAX_CONTRIBUTORS.get()) {
            return new Contribution(false, plan.target, true, false);
        }
        plan.contributors++;
        plan.progress++;
        plan.lastContributionAt = now;
        updateCracks(plan);
        if (plan.progress >= plan.duration) {
            BlockPos destroyed = plan.target;
            level.destroyBlockProgress(plan.crackId, destroyed, -1);
            if (level.destroyBlock(destroyed, false, mob)) {
                suspendForRouteValidation(group);
                return new Contribution(true, destroyed, false, true);
            }
            finishPlan(group);
            return new Contribution(true, destroyed, false, false);
        }
        return new Contribution(
                true,
                plan.target,
                true,
                false
        );
    }

    static GroupSnapshot snapshot(
            ServerLevel level,
            @Nullable BlockPos campCenter,
            Mob mob
    ) {
        if (campCenter == null) {
            return GroupSnapshot.EMPTY;
        }
        SiegeGroup group = GROUPS.get(new GroupKey(
                level.dimension(),
                campCenter.asLong()
        ));
        if (group == null) {
            return GroupSnapshot.EMPTY;
        }
        validatePlan(group);
        BreachPlan plan = group.plan;
        OpeningView opening = openingView(group, mob);
        return new GroupSnapshot(
                group.routeFailures.size(),
                plan == null
                        ? "none"
                        : (plan.awaitingRouteValidation
                                ? "validating opening "
                                : "")
                                + (plan.terrainEscape
                                        ? "terrain escape "
                                        : "")
                                + plan.target.toShortString()
                                + " depth " + (plan.depth + 1)
                                + " progress " + (int) plan.progress
                                + "/" + plan.duration,
                opening == null
                        ? "none"
                        : opening.block().toShortString()
                                + (opening.traversable()
                                ? " traversable"
                                : " obstructed")
        );
    }

    static void onServerTick() {
        Iterator<SiegeGroup> iterator = GROUPS.values().iterator();
        while (iterator.hasNext()) {
            SiegeGroup group = iterator.next();
            long now = group.level.getGameTime();
            pruneRouteFailures(group, now);
            if (now - group.lastAccessAt > GROUP_EXPIRY_TICKS) {
                clearCracks(group.plan);
                iterator.remove();
                continue;
            }
            BreachPlan plan = group.plan;
            if (group.opening != null
                    && now > group.opening.expiresAt) {
                group.opening = null;
                group.openingGeometries.clear();
            }
            if (plan != null
                    && plan.awaitingRouteValidation
                    && now - plan.validationStartedAt
                    > OPENING_VALIDATION_TICKS) {
                finishPlan(group);
                continue;
            }
            if (plan != null
                    && !plan.awaitingRouteValidation
                    && plan.progress > 0.0D
                    && now - plan.lastContributionAt
                    > SettlementConfig
                            .SIEGE_STRUCTURAL_PROGRESS_DECAY_DELAY_TICKS
                            .get()) {
                plan.progress = Math.max(
                        0.0D,
                        plan.progress - PROGRESS_DECAY_PER_TICK
                );
                if (now % 10L == 0L) {
                    updateCracks(plan);
                }
            }
        }
    }

    static void clear() {
        for (SiegeGroup group : GROUPS.values()) {
            clearCracks(group.plan);
        }
        GROUPS.clear();
    }

    private static SiegeGroup group(
            ServerLevel level,
            BlockPos campCenter
    ) {
        GroupKey key = new GroupKey(level.dimension(), campCenter.asLong());
        SiegeGroup group = GROUPS.computeIfAbsent(
                key,
                ignored -> new SiegeGroup(level, campCenter.immutable())
        );
        group.lastAccessAt = level.getGameTime();
        pruneRouteFailures(group, group.lastAccessAt);
        return group;
    }

    private static boolean hasStructuralEvidence(
            SiegeGroup group,
            int individualFailures
    ) {
        return group.routeFailures.size() >= SettlementConfig
                .SIEGE_STRUCTURAL_FAILURE_THRESHOLD.get()
                || individualFailures >= SettlementConfig
                .SIEGE_STRUCTURAL_INDIVIDUAL_FAILURE_THRESHOLD.get();
    }

    private static @Nullable BreachPlan selectPlan(
            SiegeGroup group,
            Mob scout
    ) {
        ServerLevel level = group.level;
        BlockPos origin = scout.blockPosition();
        BlockPos best = null;
        int bestStepX = 0;
        int bestStepZ = 0;
        boolean bestTerrainEscape = false;
        double bestScore = Double.MAX_VALUE;
        double scoutCampDistance = horizontalDistanceSqr(
                scout.blockPosition(),
                group.campCenter
        );
        for (int x = -CANDIDATE_RADIUS; x <= CANDIDATE_RADIUS; x++) {
            for (int y = -CANDIDATE_RADIUS; y <= CANDIDATE_RADIUS; y++) {
                for (int z = -CANDIDATE_RADIUS; z <= CANDIDATE_RADIUS; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    boolean structural = eligibleStructuralBlock(
                            group,
                            candidate
                    );
                    boolean terrainEscape = !structural
                            && eligibleTerrainEscapeBlock(
                                    group,
                                    scout,
                                    candidate
                            );
                    if (!structural && !terrainEscape) {
                        continue;
                    }
                    double candidateCampDistance = horizontalDistanceSqr(
                            candidate,
                            group.campCenter
                    );
                    if (candidateCampDistance >= scoutCampDistance) {
                        continue;
                    }
                    int[] step = horizontalStep(candidate, group.campCenter);
                    if (step[0] == 0 && step[1] == 0) {
                        continue;
                    }
                    BlockPos exterior = candidate.offset(-step[0], 0, -step[1]);
                    if (!passable(level, exterior)
                            || !passable(level, exterior.above())
                            || exterior.distToCenterSqr(scout.position())
                            > 9.0D
                            || (candidate.getY()
                                    == scout.blockPosition().getY()
                                    && isWalkableStep(level, candidate))) {
                        continue;
                    }
                    double score = candidate.distToCenterSqr(scout.position())
                            + horizontalDistanceSqr(
                                    candidate,
                                    group.campCenter
                            ) * 0.01D;
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate.immutable();
                        bestStepX = step[0];
                        bestStepZ = step[1];
                        bestTerrainEscape = terrainEscape;
                    }
                }
            }
        }
        if (best == null) {
            return null;
        }
        BlockPos approach = best.offset(-bestStepX, 0, -bestStepZ);
        return new BreachPlan(
                best,
                best,
                approach,
                bestStepX,
                bestStepZ,
                0,
                bestTerrainEscape,
                level,
                scout.getId()
        );
    }

    /**
     * Allows a zombie stranded below the settlement grade to start an upward
     * escape ramp. Later plan steps must rise one block for every block they
     * advance, so this permission cannot become a level or downward tunnel.
     */
    private static boolean eligibleTerrainEscapeBlock(
            SiegeGroup group,
            Mob scout,
            BlockPos position
    ) {
        int breachFloor = group.campCenter.getY() - 1;
        if (position.getY() >= breachFloor
                || breachFloor - position.getY()
                > MAX_TERRAIN_ESCAPE_DROP
                || position.getY() < scout.blockPosition().getY()
                || position.getY() > scout.blockPosition().getY() + 2) {
            return false;
        }
        int maximumDistance = SettlementConfig
                .SIEGE_STRUCTURAL_MAX_CAMP_DISTANCE
                .get();
        if (horizontalDistanceSqr(position, group.campCenter)
                > (double) maximumDistance * maximumDistance
                || !SiegeBreachRules.canStructurallyBreach(
                        group.level,
                        position
                )) {
            return false;
        }
        return true;
    }

    private static boolean eligibleStructuralBlock(
            SiegeGroup group,
            BlockPos position
    ) {
        int breachFloor = group.campCenter.getY() - 1;
        if (position.getY() < breachFloor
                || position.getY() > breachFloor + 3) {
            return false;
        }
        int maximumDistance = SettlementConfig
                .SIEGE_STRUCTURAL_MAX_CAMP_DISTANCE.get();
        if (horizontalDistanceSqr(position, group.campCenter)
                > (double) maximumDistance * maximumDistance) {
            return false;
        }
        return SiegeBreachRules.canStructurallyBreach(
                group.level,
                position
        );
    }

    private static void validatePlan(SiegeGroup group) {
        BreachPlan plan = group.plan;
        if (plan == null) {
            return;
        }
        if (plan.awaitingRouteValidation) {
            return;
        }
        boolean eligible = plan.terrainEscape
                ? eligibleTerrainEscapePlanTarget(group, plan)
                : eligibleStructuralBlock(group, plan.target);
        if (!eligible) {
            if (passable(group.level, plan.target)) {
                suspendForRouteValidation(group);
            } else {
                finishPlan(group);
            }
        }
    }

    private static boolean eligibleTerrainEscapePlanTarget(
            SiegeGroup group,
            BreachPlan plan
    ) {
        int breachFloor = group.campCenter.getY() - 1;
        BlockPos lower = terrainEscapeLower(plan, plan.depth);
        boolean rampTarget = plan.target.equals(lower)
                || plan.target.equals(lower.above());
        int maximumDistance = SettlementConfig
                .SIEGE_STRUCTURAL_MAX_CAMP_DISTANCE
                .get();
        return rampTarget
                && plan.origin.getY() < breachFloor
                && breachFloor - plan.origin.getY()
                <= MAX_TERRAIN_ESCAPE_DROP
                && horizontalDistanceSqr(plan.target, group.campCenter)
                <= (double) maximumDistance * maximumDistance
                && SiegeBreachRules.canStructurallyBreach(
                        group.level,
                        plan.target
                );
    }

    private static void advancePlan(SiegeGroup group) {
        BreachPlan plan = group.plan;
        if (plan == null) {
            return;
        }
        clearCracks(plan);

        if (plan.terrainEscape) {
            BlockPos lowerAtDepth = terrainEscapeLower(plan, plan.depth);
            if (plan.target.equals(lowerAtDepth)) {
                BlockPos upper = lowerAtDepth.above();
                if (SiegeBreachRules.canStructurallyBreach(
                        group.level,
                        upper
                )) {
                    setTarget(plan, upper);
                    return;
                }
            }
            int nextDepth = plan.depth + 1;
            int maximumDepth = Math.min(
                    MAX_TERRAIN_ESCAPE_DROP,
                    SettlementConfig.SIEGE_STRUCTURAL_MAX_DEPTH.get()
            );
            if (nextDepth >= maximumDepth) {
                finishPlan(group);
                return;
            }
            BlockPos nextLower = terrainEscapeLower(plan, nextDepth);
            BlockPos nextUpper = nextLower.above();
            if (nextLower.getY() > group.campCenter.getY() + 1) {
                finishPlan(group);
                return;
            }
            plan.depth = nextDepth;
            plan.approach = terrainEscapeLower(plan, nextDepth - 1);
            if (SiegeBreachRules.canStructurallyBreach(
                    group.level,
                    nextLower
            )) {
                setTarget(plan, nextLower);
            } else if (SiegeBreachRules.canStructurallyBreach(
                    group.level,
                    nextUpper
            )) {
                setTarget(plan, nextUpper);
            } else {
                finishPlan(group);
            }
            return;
        }
        BlockPos lowerAtDepth = plan.origin.offset(
                plan.stepX * plan.depth,
                0,
                plan.stepZ * plan.depth
        );
        if (plan.target.equals(lowerAtDepth)) {
            BlockPos upper = lowerAtDepth.above();
            if (eligibleStructuralBlock(group, upper)) {
                setTarget(plan, upper);
                return;
            }
        }

        int nextDepth = plan.depth + 1;
        if (nextDepth >= SettlementConfig.SIEGE_STRUCTURAL_MAX_DEPTH.get()) {
            finishPlan(group);
            return;
        }
        BlockPos nextLower = plan.origin.offset(
                plan.stepX * nextDepth,
                0,
                plan.stepZ * nextDepth
        );
        BlockPos nextUpper = nextLower.above();
        plan.depth = nextDepth;
        plan.approach = nextLower.offset(-plan.stepX, 0, -plan.stepZ);
        if (eligibleStructuralBlock(group, nextLower)
                && !isWalkableStep(group.level, nextLower)) {
            setTarget(plan, nextLower);
        } else if (eligibleStructuralBlock(group, nextUpper)) {
            setTarget(plan, nextUpper);
        } else {
            finishPlan(group);
        }
    }

    private static void setTarget(BreachPlan plan, BlockPos target) {
        plan.target = target.immutable();
        plan.progress = 0.0D;
        plan.duration = breachDuration(plan.level, plan.target);
        plan.lastContributionAt = plan.level.getGameTime();
        plan.awaitingRouteValidation = false;
        plan.validationStartedAt = -1L;
        plan.validators.clear();
    }

    private static BlockPos terrainEscapeLower(
            BreachPlan plan,
            int depth
    ) {
        return plan.origin.offset(
                plan.stepX * depth,
                depth,
                plan.stepZ * depth
        );
    }

    private static void suspendForRouteValidation(SiegeGroup group) {
        BreachPlan plan = group.plan;
        if (plan == null) {
            return;
        }
        clearCracks(plan);
        plan.progress = 0.0D;
        plan.awaitingRouteValidation = true;
        plan.validationStartedAt = group.level.getGameTime();
        group.routeFailures.clear();
        publishOpening(
                group,
                plan.target,
                plan.stepX,
                plan.stepZ,
                plan.terrainEscape
        );
    }

    private static void finishPlan(SiegeGroup group) {
        clearCracks(group.plan);
        group.plan = null;
        group.routeFailures.clear();
    }

    private static void updateCracks(BreachPlan plan) {
        if (plan.progress <= 0.0D) {
            plan.level.destroyBlockProgress(plan.crackId, plan.target, -1);
            return;
        }
        int stage = Math.min(
                9,
                (int) (plan.progress * 10.0D / Math.max(1, plan.duration))
        );
        plan.level.destroyBlockProgress(plan.crackId, plan.target, stage);
    }

    private static void clearCracks(@Nullable BreachPlan plan) {
        if (plan != null) {
            plan.level.destroyBlockProgress(plan.crackId, plan.target, -1);
        }
    }

    private static boolean passable(ServerLevel level, BlockPos position) {
        return level.getBlockState(position)
                .getCollisionShape(level, position)
                .isEmpty()
                && level.getFluidState(position).isEmpty();
    }

    private static void publishOpening(
            SiegeGroup group,
            BlockPos position,
            int stepX,
            int stepZ,
            boolean localOnly
    ) {
        if (stepX == 0 && stepZ == 0) {
            return;
        }
        group.openingRevision++;
        group.opening = new Opening(
                position.immutable(),
                stepX,
                stepZ,
                localOnly,
                group.openingRevision,
                group.level.getGameTime() + OPENING_MEMORY_TICKS
        );
        group.openingGeometries.clear();
    }

    private static @Nullable OpeningView openingView(
            SiegeGroup group,
            Mob mob
    ) {
        Opening opening = group.opening;
        if (opening == null
                || group.level.getGameTime() > opening.expiresAt
                || !passable(group.level, opening.block)) {
            group.opening = null;
            group.openingGeometries.clear();
            return null;
        }
        if (opening.localOnly
                && opening.block.distToCenterSqr(mob.position()) > 64.0D) {
            return null;
        }
        long dimensions = Integer.toUnsignedLong(Float.floatToIntBits(
                mob.getBbWidth()
        )) << 32 | Integer.toUnsignedLong(Float.floatToIntBits(
                mob.getBbHeight()
        ));
        OpeningGeometry geometry = group.openingGeometries.get(dimensions);
        if (geometry == null) {
            geometry = openingGeometry(group, opening, mob);
            group.openingGeometries.put(dimensions, geometry);
        }
        return new OpeningView(
                opening.block,
                geometry.approach,
                geometry.passage,
                geometry.exit,
                geometry.stepX,
                geometry.stepZ,
                opening.revision,
                geometry.traversable
        );
    }

    private static OpeningGeometry openingGeometry(
            SiegeGroup group,
            Opening opening,
            Mob mob
    ) {
        Vec3 passage = standingPosition(group.level, opening.block, mob);
        Vec3 approach = null;
        Vec3 exit = null;
        int selectedStepX = opening.stepX;
        int selectedStepZ = opening.stepZ;
        double bestScore = Double.MAX_VALUE;
        int[][] directions = {
                {opening.stepX, opening.stepZ},
                {-opening.stepX, -opening.stepZ},
                {-opening.stepZ, opening.stepX},
                {opening.stepZ, -opening.stepX}
        };
        if (passage != null) {
            for (int[] direction : directions) {
                BlockPos approachBlock = opening.block.offset(
                        -direction[0],
                        0,
                        -direction[1]
                );
                BlockPos exitBlock = opening.block.offset(
                        direction[0],
                        0,
                        direction[1]
                );
                Vec3 candidateApproach = standingPosition(
                        group.level,
                        approachBlock,
                        mob
                );
                Vec3 candidateExit = standingPosition(
                        group.level,
                        exitBlock,
                        mob
                );
                if (candidateApproach == null || candidateExit == null) {
                    continue;
                }
                double approachCampDistance = horizontalDistanceSqr(
                        BlockPos.containing(candidateApproach),
                        group.campCenter
                );
                double exitCampDistance = horizontalDistanceSqr(
                        BlockPos.containing(candidateExit),
                        group.campCenter
                );
                if (exitCampDistance >= approachCampDistance) {
                    continue;
                }
                double score = exitCampDistance
                        + (direction[0] == opening.stepX
                                && direction[1] == opening.stepZ
                                ? 0.0D
                                : 0.25D);
                if (score < bestScore) {
                    bestScore = score;
                    approach = candidateApproach;
                    exit = candidateExit;
                    selectedStepX = direction[0];
                    selectedStepZ = direction[1];
                }
            }
        }
        boolean traversable = passage != null
                && approach != null
                && exit != null;
        return new OpeningGeometry(
                approach == null
                        ? opening.block.offset(
                                -selectedStepX,
                                0,
                                -selectedStepZ
                        ).getCenter()
                        : approach,
                passage == null ? opening.block.getCenter() : passage,
                exit == null
                        ? opening.block.offset(
                                selectedStepX,
                                0,
                                selectedStepZ
                        ).getCenter()
                        : exit,
                selectedStepX,
                selectedStepZ,
                traversable
        );
    }

    private static @Nullable Vec3 standingPosition(
            ServerLevel level,
            BlockPos base,
            Mob mob
    ) {
        int[] verticalOffsets = {0, 1, -1};
        double[] horizontalOffsets = {0.0D, -0.15D, 0.15D};
        for (int vertical : verticalOffsets) {
            double y = base.getY() + vertical;
            for (double xOffset : horizontalOffsets) {
                for (double zOffset : horizontalOffsets) {
                    double x = base.getX() + 0.5D + xOffset;
                    double z = base.getZ() + 0.5D + zOffset;
                    BlockPos support = BlockPos.containing(
                            x,
                            y - 0.01D,
                            z
                    );
                    if (!level.getBlockState(support).isFaceSturdy(
                            level,
                            support,
                            Direction.UP
                    )) {
                        continue;
                    }
                    Vec3 candidate = new Vec3(x, y, z);
                    AABB standingBox = mob.getBoundingBox().move(
                            candidate.x - mob.getX(),
                            candidate.y - mob.getY(),
                            candidate.z - mob.getZ()
                    );
                    // Opening geometry is long-lived tactical memory. Test
                    // only blocks here: another zombie occupying the opening
                    // for one tick must not cache it as obstructed for the
                    // entire horde.
                    if (!level.getBlockCollisions(mob, standingBox)
                            .iterator()
                            .hasNext()) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isWalkableStep(
            ServerLevel level,
            BlockPos position
    ) {
        VoxelShape shape = level.getBlockState(position)
                .getCollisionShape(level, position);
        return !shape.isEmpty()
                && shape.max(Direction.Axis.Y) <= 1.0D
                && passable(level, position.above())
                && passable(level, position.above(2));
    }

    private static int breachDuration(ServerLevel level, BlockPos position) {
        return SiegeBreachRules.canBreach(level, position)
                ? SiegeBreachRules.durationTicks(level.getBlockState(position))
                : SiegeBreachRules.structuralDurationTicks(level, position);
    }

    private static int[] horizontalStep(BlockPos from, BlockPos toward) {
        int deltaX = toward.getX() - from.getX();
        int deltaZ = toward.getZ() - from.getZ();
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return new int[]{Integer.signum(deltaX), 0};
        }
        return new int[]{0, Integer.signum(deltaZ)};
    }

    private static double horizontalDistanceSqr(BlockPos left, BlockPos right) {
        double x = left.getX() - right.getX();
        double z = left.getZ() - right.getZ();
        return x * x + z * z;
    }

    private static void pruneRouteFailures(SiegeGroup group, long now) {
        if (group.lastFailurePruneAt == now) {
            return;
        }
        group.lastFailurePruneAt = now;
        group.routeFailures.entrySet().removeIf(
                entry -> now - entry.getValue() > ROUTE_FAILURE_MEMORY_TICKS
        );
    }

    private static @Nullable BreachView activeView(
            @Nullable BreachPlan plan
    ) {
        return plan == null || plan.awaitingRouteValidation
                ? null
                : view(plan);
    }

    private static @Nullable BreachView view(@Nullable BreachPlan plan) {
        return plan == null
                ? null
                : new BreachView(
                        plan.target,
                        plan.approach,
                        plan.depth,
                        plan.progress,
                        plan.duration
                );
    }

    record BreachView(
            BlockPos target,
            BlockPos approach,
            int depth,
            double progress,
            int duration
    ) {
    }

    record OpeningView(
            BlockPos block,
            Vec3 approach,
            Vec3 passage,
            Vec3 exit,
            int stepX,
            int stepZ,
            long revision,
            boolean traversable
    ) {
        boolean isOutside(Vec3 position) {
            double deltaX = position.x - (block.getX() + 0.5D);
            double deltaZ = position.z - (block.getZ() + 0.5D);
            return deltaX * stepX + deltaZ * stepZ < 0.45D;
        }
    }

    record Contribution(
            boolean contributed,
            @Nullable BlockPos target,
            boolean planActive,
            boolean openingCreated
    ) {
        static final Contribution NONE = new Contribution(
                false,
                null,
                false,
                false
        );
    }

    record GroupSnapshot(
            int routeFailures,
            String breachPlan,
            String opening
    ) {
        static final GroupSnapshot EMPTY = new GroupSnapshot(
                0,
                "none",
                "none"
        );
    }

    private record GroupKey(ResourceKey<Level> dimension, long campCenter) {
    }

    private static final class SiegeGroup {
        final ServerLevel level;
        final BlockPos campCenter;
        long lastAccessAt;
        LivingEntity sharedTarget;
        long targetExpiresAt = -1L;
        final Map<UUID, Long> routeFailures = new HashMap<>();
        long lastFailurePruneAt = Long.MIN_VALUE;
        long openingRevision;
        Opening opening;
        final Map<Long, OpeningGeometry> openingGeometries = new HashMap<>();
        BreachPlan plan;

        SiegeGroup(ServerLevel level, BlockPos campCenter) {
            this.level = level;
            this.campCenter = campCenter;
            this.lastAccessAt = level.getGameTime();
        }
    }

    private static final class BreachPlan {
        final BlockPos origin;
        final int stepX;
        final int stepZ;
        final boolean terrainEscape;
        final ServerLevel level;
        final int crackId;
        BlockPos target;
        BlockPos approach;
        int depth;
        double progress;
        int duration;
        long lastContributionAt;
        long contributorTick = Long.MIN_VALUE;
        int contributors;
        final Set<UUID> validators = new HashSet<>();
        boolean awaitingRouteValidation;
        long validationStartedAt = -1L;

        BreachPlan(
                BlockPos origin,
                BlockPos target,
                BlockPos approach,
                int stepX,
                int stepZ,
                int depth,
                boolean terrainEscape,
                ServerLevel level,
                int crackId
        ) {
            this.origin = origin;
            this.target = target;
            this.approach = approach;
            this.stepX = stepX;
            this.stepZ = stepZ;
            this.depth = depth;
            this.terrainEscape = terrainEscape;
            this.level = level;
            this.crackId = crackId;
            this.duration = breachDuration(level, target);
            this.lastContributionAt = level.getGameTime();
        }
    }

    private record Opening(
            BlockPos block,
            int stepX,
            int stepZ,
            boolean localOnly,
            long revision,
            long expiresAt
    ) {
    }

    private record OpeningGeometry(
            Vec3 approach,
            Vec3 passage,
            Vec3 exit,
            int stepX,
            int stepZ,
            boolean traversable
    ) {
    }

    private SiegeCoordinationManager() {
    }
}
