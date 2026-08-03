package io.github.gev414.rotwire.mob.ai;

import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import io.github.gev414.rotwire.encounter.EncounterEntityData;
import io.github.gev414.rotwire.entity.BruteEntity;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import io.github.gev414.rotwire.settlement.SettlementManager;
import io.github.gev414.rotwire.settlement.SettlementSiegeState;
import io.github.gev414.rotwire.settlement.SiegeBreachRules;
import io.github.gev414.rotwire.stealth.AwarenessManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Single movement owner for coordinated infected. It resolves intent first,
 * then selects exactly one action without allowing attention, assault, and
 * breaching systems to replace one another's navigation.
 */
final class CoordinatedHostileGoal extends Goal {

    private static final int ATTACK_INTERVAL_TICKS = 20;
    private static final int OBSTACLE_SCAN_TICKS = 15;
    private static final int BREACH_ROUTE_CHECK_TICKS = 20;
    private static final int CAMP_DAMAGE_INTERVAL_TICKS = 200;
    private static final double SURVIVOR_SCAN_RADIUS = 32.0D;
    private static final double[] BREACH_RAY_HEIGHTS = {
            0.45D,
            0.9D,
            1.35D
    };
    private static final double BREACH_SCAN_RANGE = 4.5D;
    private static final double BREACH_SCAN_STEP = 0.5D;
    private static final double LOCAL_BREACH_TRIGGER_DISTANCE_SQR = 64.0D;
    private static final double GATEWAY_DIRECT_DISTANCE_SQR = 9.0D;
    private static final double GATEWAY_PASSAGE_DISTANCE_SQR = 0.64D;
    private static final double[] BREACH_LATERAL_OFFSETS = {
            0.0D,
            -0.55D,
            0.55D,
            -1.1D,
            1.1D
    };
    private static final int[] BREACH_VERTICAL_OFFSETS = {0, 1, -1};

    private final Mob mob;
    private final CoordinatedHostileAi.State state;

    CoordinatedHostileGoal(
            Mob mob,
            CoordinatedHostileAi.State state
    ) {
        this.mob = mob;
        this.state = state;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    CoordinatedHostileAi.State state() {
        return state;
    }

    @Override
    public boolean canUse() {
        return enabledAndActive() && !shouldYieldSpecialCombat();
    }

    @Override
    public boolean canContinueToUse() {
        return enabledAndActive() && !shouldYieldSpecialCombat();
    }

    @Override
    public void start() {
        state.action = HostileAction.WAIT;
    }

    @Override
    public void stop() {
        stopNavigation();
        clearBreachProgress();
        state.action = HostileAction.WAIT;
    }

    @Override
    public void tick() {
        tickCooldowns();
        scanForAssaultTarget();
        HostileIntent intent = refreshIntent();
        Objective objective = objective(intent);
        if (objective == null) {
            state.action = HostileAction.WAIT;
            return;
        }

        LivingEntity target = objective.target();
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (mob.level() instanceof ServerLevel level) {
                BlockPos center = coordinationCenter();
                if (center != null) {
                    SiegeCoordinationManager.publishTarget(
                            level,
                            center,
                            target
                    );
                }
            }
        }

        reactToNewOpening();
        objective = routeThroughOpening(objective);
        if (reconsiderActiveBreach(objective)) {
            return;
        }
        if (tickBreachProgress()) {
            return;
        }
        if (target != null && canAttack(target)) {
            attack(target);
            return;
        }

        recordCompletedPartialPath();
        if (steerThroughOpening(objective)) {
            return;
        }
        if (handleSharedBreachPlan(objective)) {
            return;
        }

        boolean closeBreachFailure = target != null
                && state.consecutivePathFailures >= 1
                && mob.distanceToSqr(target)
                <= LOCAL_BREACH_TRIGGER_DISTANCE_SQR
                && canBreach();
        boolean navigationFailed = closeBreachFailure
                || state.consecutivePathFailures >= 2
                || mob.getNavigation().isStuck();
        if (navigationFailed
                && state.obstacleScanCooldown == 0) {
            state.obstacleScanCooldown = OBSTACLE_SCAN_TICKS;
            if (mob.getNavigation().isStuck()
                    && mob.level() instanceof ServerLevel level) {
                state.consecutivePathFailures = Math.max(
                        1,
                        state.consecutivePathFailures
                );
                BlockPos center = coordinationCenter();
                if (center != null) {
                    SiegeCoordinationManager.recordRouteFailure(
                            level,
                            center,
                            mob
                    );
                }
            }
            state.breachAllowed = canBreach();
            if (state.breachAllowed) {
                RouteCheck routeCheck = tryReachableRoute(objective);
                if (routeCheck != RouteCheck.BLOCKED) {
                    return;
                }
                BlockPos center = coordinationCenter();
                if (center != null
                        && mob.level() instanceof ServerLevel level) {
                    SiegeCoordinationManager.BreachView shared =
                            SiegeCoordinationManager.requestPlan(
                                    level,
                                    center,
                                    mob,
                                    state.consecutivePathFailures
                            );
                    if (shared != null) {
                        state.sharedBreachAssigned = true;
                        state.breachRouteCheckCooldown =
                                BREACH_ROUTE_CHECK_TICKS;
                    }
                    if (shared != null
                            && handleSharedBreachPlan(objective)) {
                        return;
                    }
                }
                BlockPos obstacle = findBreachableObstacle(
                        objective.position()
                );
                if (obstacle != null) {
                    startBreach(obstacle);
                    tickBreachProgress();
                    return;
                }
            }
        }

        if (intent == HostileIntent.ASSAULT
                && target == null
                && state.campDamageCooldown == 0
                && mob.distanceToSqr(objective.position()) <= 64.0D) {
            state.campDamageCooldown = CAMP_DAMAGE_INTERVAL_TICKS;
            BlockPos damageTarget = findCampDamageTarget(
                    BlockPos.containing(objective.position())
            );
            if (damageTarget != null) {
                startBreach(damageTarget);
                tickBreachProgress();
                return;
            }
        }

        if (intent == HostileIntent.INVESTIGATE
                && mob.distanceToSqr(objective.position()) <= 2.25D) {
            state.investigationPosition = null;
            state.investigationExpiresAt = -1L;
            state.intent = HostileIntent.IDLE;
            state.action = HostileAction.WAIT;
            stopNavigation();
            return;
        }

        navigate(objective);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean enabledAndActive() {
        if (!SurvivalSystemsConfig.COORDINATED_INFECTED_AI_ENABLED.get()
                || !mob.isAlive()
                || mob.isNoAi()) {
            state.intent = HostileIntent.IDLE;
            return false;
        }
        return refreshIntent() != HostileIntent.IDLE;
    }

    private HostileIntent refreshIntent() {
        LivingEntity target = mob.getTarget();
        if (!validTarget(target)) {
            if (target != null) {
                mob.setTarget(null);
            }
            target = null;
        }
        long gameTime = mob.level().getGameTime();
        if (target != null && hasFreshContact(target)) {
            state.rememberedTarget = target;
            state.targetMemoryExpiresAt = gameTime + SurvivalSystemsConfig
                    .COORDINATED_AI_CONTACT_MEMORY_TICKS.get();
        } else if (target == null
                && validTarget(state.rememberedTarget)
                && state.targetMemoryExpiresAt >= gameTime) {
            target = state.rememberedTarget;
            mob.setTarget(target);
        } else if (state.targetMemoryExpiresAt < gameTime
                || !validTarget(state.rememberedTarget)) {
            state.rememberedTarget = null;
            state.targetMemoryExpiresAt = -1L;
        }
        boolean hasAssault = assaultTarget() != null;
        boolean hasInvestigation = state.investigationPosition != null
                && state.investigationExpiresAt > mob.level().getGameTime();
        if (!hasInvestigation) {
            state.investigationPosition = null;
            state.investigationExpiresAt = -1L;
        }
        state.intent = HostileIntentResolver.resolve(
                target != null,
                hasAssault,
                hasInvestigation
        );
        return state.intent;
    }

    private boolean hasFreshContact(LivingEntity target) {
        int radius = SurvivalSystemsConfig.COORDINATED_AI_CONTACT_RADIUS.get();
        return mob.distanceToSqr(target) <= (double) radius * radius
                || mob.getSensing().hasLineOfSight(target);
    }

    private Objective objective(HostileIntent intent) {
        return switch (intent) {
            case HUNT -> {
                LivingEntity target = mob.getTarget();
                yield target == null
                        ? null
                        : new Objective(target.position(), target);
            }
            case ASSAULT -> {
                BlockPos target = assaultTarget();
                yield target == null
                        ? null
                        : new Objective(target.getCenter(), null);
            }
            case INVESTIGATE -> state.investigationPosition == null
                    ? null
                    : new Objective(state.investigationPosition, null);
            case IDLE -> null;
        };
    }

    private void tickCooldowns() {
        if (state.pathCooldown > 0) {
            state.pathCooldown--;
        }
        if (state.survivorScanCooldown > 0) {
            state.survivorScanCooldown--;
        }
        if (state.obstacleScanCooldown > 0) {
            state.obstacleScanCooldown--;
        }
        if (state.attackCooldown > 0) {
            state.attackCooldown--;
        }
        if (state.campDamageCooldown > 0) {
            state.campDamageCooldown--;
        }
        if (state.breachRouteCheckCooldown > 0) {
            state.breachRouteCheckCooldown--;
        }
    }

    private void scanForAssaultTarget() {
        BlockPos center = assaultTarget();
        if (center == null
                || validTarget(mob.getTarget())
                || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (state.survivorScanCooldown > 0) {
            return;
        }
        LivingEntity shared = SiegeCoordinationManager.sharedTarget(
                level,
                center
        );
        if (validTarget(shared)
                && mob.distanceToSqr(shared)
                <= SURVIVOR_SCAN_RADIUS * SURVIVOR_SCAN_RADIUS) {
            mob.setTarget(shared);
            return;
        }
        int interval = SurvivalSystemsConfig
                .COORDINATED_AI_SURVIVOR_SCAN_TICKS
                .get();
        state.survivorScanCooldown = interval
                + mob.getRandom().nextInt(Math.max(1, interval / 2));
        SurvivorEntity nearest = level.getEntitiesOfClass(
                        SurvivorEntity.class,
                        mob.getBoundingBox().inflate(SURVIVOR_SCAN_RADIUS),
                        LivingEntity::isAlive
                ).stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private boolean validTarget(LivingEntity target) {
        if (target == null
                || !target.isAlive()
                || target.level() != mob.level()) {
            return false;
        }
        if (target instanceof ServerPlayer player) {
            return !player.isSpectator() && !player.isCreative();
        }
        return true;
    }

    private boolean shouldYieldSpecialCombat() {
        LivingEntity target = mob.getTarget();
        if (!validTarget(target) || !mob.getSensing().hasLineOfSight(target)) {
            return false;
        }
        double distance = mob.distanceToSqr(target);
        if (mob instanceof BruteEntity) {
            return distance >= 36.0D && distance <= 324.0D;
        }
        return mob instanceof Drowned
                && mob.getMainHandItem().is(Items.TRIDENT)
                && distance > attackReachSqr(target)
                && distance <= 400.0D;
    }

    private boolean canAttack(LivingEntity target) {
        return mob.distanceToSqr(target) <= attackReachSqr(target)
                && mob.getSensing().hasLineOfSight(target);
    }

    private void attack(LivingEntity target) {
        state.action = HostileAction.ATTACK;
        stopNavigation();
        if (state.attackCooldown == 0) {
            state.attackCooldown = ATTACK_INTERVAL_TICKS;
            mob.swing(InteractionHand.MAIN_HAND);
            mob.doHurtTarget(target);
        }
    }

    private double attackReachSqr(LivingEntity target) {
        double reach = mob.getBbWidth() * 2.0D;
        return reach * reach + target.getBbWidth();
    }

    private void navigate(Objective objective) {
        Vec3 destination = objective.position();
        boolean objectiveMoved = state.lastObjective == null
                || state.lastObjective.distanceToSqr(destination) >= 4.0D;
        boolean needsPath = mob.getNavigation().isDone()
                || mob.getNavigation().isStuck()
                || objectiveMoved;
        if (!needsPath) {
            state.action = HostileAction.MOVE;
            return;
        }
        if (state.pathCooldown > 0
                || !(mob.level() instanceof ServerLevel level)
                || !CoordinatedHostileAi.requestPath(level, mob)) {
            state.action = mob.getNavigation().isDone()
                    ? HostileAction.WAIT
                    : HostileAction.MOVE;
            return;
        }

        Vec3 segment = segmentDestination(destination);
        long pathStartedAt = System.nanoTime();
        Path path = mob.getNavigation().createPath(
                BlockPos.containing(segment),
                1
        );
        CoordinatedHostileAi.recordPathCalculation(
                level,
                System.nanoTime() - pathStartedAt
        );
        boolean hasPath = path != null
                && mob.getNavigation().moveTo(path, movementSpeed());
        boolean reachesSegment = hasPath && path.canReach();
        state.pathAttempts++;
        state.lastPathSucceeded = reachesSegment;
        state.activePathPartial = hasPath && !reachesSegment;
        BlockPos center = coordinationCenter();
        if (reachesSegment) {
            state.consecutivePathFailures = 0;
            if (center != null) {
                SiegeCoordinationManager.recordRouteSuccess(
                        level,
                        center,
                        mob,
                        objective.opening() == null
                );
            }
        } else if (!hasPath) {
            state.consecutivePathFailures++;
            if (center != null) {
                SiegeCoordinationManager.recordRouteFailure(
                        level,
                        center,
                        mob
                );
            }
        }
        state.lastObjective = destination;
        int base = SurvivalSystemsConfig
                .COORDINATED_AI_PATH_RETRY_TICKS
                .get();
        int jitter = SurvivalSystemsConfig
                .COORDINATED_AI_PATH_RETRY_JITTER_TICKS
                .get();
        boolean closeFailure = !hasPath
                && mob.distanceToSqr(destination) <= 256.0D;
        int retryBase = hasPath
                ? base
                : closeFailure ? Math.max(5, base / 2) : base * 2;
        int retryJitter = closeFailure ? Math.min(10, jitter) : jitter;
        state.pathCooldown = retryBase
                + (retryJitter == 0
                        ? 0
                        : mob.getRandom().nextInt(retryJitter + 1));
        state.action = hasPath ? HostileAction.MOVE : HostileAction.WAIT;
    }

    /**
     * A breach is only justified when a fresh, full route cannot reach the
     * current objective. This deliberately uses the same shared path budget as
     * ordinary navigation so a large horde cannot turn route validation into a
     * new tick spike.
     */
    private RouteCheck tryReachableRoute(Objective objective) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return RouteCheck.BLOCKED;
        }
        if (!CoordinatedHostileAi.requestPath(level, mob)) {
            state.action = HostileAction.WAIT;
            return RouteCheck.DEFERRED;
        }

        long pathStartedAt = System.nanoTime();
        Path path = mob.getNavigation().createPath(
                BlockPos.containing(objective.position()),
                1
        );
        CoordinatedHostileAi.recordPathCalculation(
                level,
                System.nanoTime() - pathStartedAt
        );
        boolean reachable = path != null
                && path.canReach()
                && mob.getNavigation().moveTo(path, movementSpeed());
        state.pathAttempts++;
        state.lastPathSucceeded = reachable;
        state.activePathPartial = false;
        state.lastObjective = objective.position();
        BlockPos center = coordinationCenter();
        if (!reachable) {
            mob.getNavigation().stop();
            state.consecutivePathFailures++;
            if (center != null) {
                SiegeCoordinationManager.recordRouteFailure(
                        level,
                        center,
                        mob
                );
            }
            return RouteCheck.BLOCKED;
        }

        clearBreachProgress();
        state.sharedBreachTarget = null;
        state.sharedBreachAssigned = false;
        state.lastBlockingObstacle = null;
        state.consecutivePathFailures = 0;
        int base = SurvivalSystemsConfig
                .COORDINATED_AI_PATH_RETRY_TICKS
                .get();
        int jitter = SurvivalSystemsConfig
                .COORDINATED_AI_PATH_RETRY_JITTER_TICKS
                .get();
        state.pathCooldown = base + (jitter == 0
                ? 0
                : mob.getRandom().nextInt(jitter + 1));
        state.action = HostileAction.MOVE;
        if (center != null) {
            SiegeCoordinationManager.recordRouteSuccess(
                    level,
                    center,
                    mob,
                    objective.opening() == null
            );
        }
        return RouteCheck.REACHABLE;
    }

    private boolean reconsiderActiveBreach(Objective objective) {
        if (state.breachTarget == null
                || objective.target() == null
                || state.breachRouteCheckCooldown > 0) {
            return false;
        }
        RouteCheck routeCheck = tryReachableRoute(objective);
        if (routeCheck == RouteCheck.REACHABLE) {
            return true;
        }
        state.breachRouteCheckCooldown = routeCheck == RouteCheck.DEFERRED
                ? 1
                : BREACH_ROUTE_CHECK_TICKS;
        return false;
    }

    private void recordCompletedPartialPath() {
        if (!state.activePathPartial || !mob.getNavigation().isDone()) {
            return;
        }
        state.activePathPartial = false;
        state.lastPathSucceeded = false;
        state.consecutivePathFailures++;
        state.pathCooldown = 0;
        if (mob.level() instanceof ServerLevel level) {
            BlockPos center = coordinationCenter();
            if (center != null) {
                SiegeCoordinationManager.recordRouteFailure(
                        level,
                        center,
                        mob
                );
            }
        }
    }

    private Objective routeThroughOpening(Objective objective) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return objective;
        }
        BlockPos center = coordinationCenter();
        if (center == null) {
            state.activeOpeningRevision = -1L;
            return objective;
        }
        SiegeCoordinationManager.OpeningView opening =
                SiegeCoordinationManager.opening(level, center, mob);
        if (opening == null || !opening.traversable()) {
            state.activeOpeningRevision = -1L;
            return objective;
        }
        if (objective.target() != null
                && opening.isOutside(objective.target().position())) {
            state.activeOpeningRevision = -1L;
            return objective;
        }
        if (!opening.isOutside(mob.position())) {
            if (state.activeOpeningRevision == opening.revision()) {
                SiegeCoordinationManager.recordOpeningTraversed(
                        level,
                        center,
                        mob,
                        opening.revision()
                );
            }
            state.activeOpeningRevision = -1L;
            return objective;
        }
        state.activeOpeningRevision = opening.revision();
        return new Objective(
                opening.approach(),
                objective.target(),
                opening
        );
    }

    private boolean steerThroughOpening(Objective objective) {
        SiegeCoordinationManager.OpeningView opening = objective.opening();
        if (opening == null
                || mob.position().distanceToSqr(opening.approach())
                > GATEWAY_DIRECT_DISTANCE_SQR) {
            return false;
        }
        Vec3 destination = mob.position().distanceToSqr(opening.passage())
                > GATEWAY_PASSAGE_DISTANCE_SQR
                ? opening.passage()
                : opening.exit();
        stopNavigation();
        mob.getMoveControl().setWantedPosition(
                destination.x,
                destination.y,
                destination.z,
                movementSpeed()
        );
        mob.getLookControl().setLookAt(
                destination.x,
                destination.y + 0.75D,
                destination.z
        );
        state.pathCooldown = 0;
        state.action = HostileAction.MOVE;
        return true;
    }

    private boolean handleSharedBreachPlan(Objective objective) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos center = coordinationCenter();
        if (center == null) {
            return false;
        }
        SiegeCoordinationManager.BreachView plan =
                SiegeCoordinationManager.plan(level, center, mob);
        if (plan == null) {
            state.sharedBreachTarget = null;
            state.sharedBreachAssigned = false;
            return false;
        }
        if (!state.sharedBreachAssigned) {
            if (state.consecutivePathFailures < 2
                    && !mob.getNavigation().isStuck()) {
                return false;
            }
            RouteCheck routeCheck = tryReachableRoute(objective);
            if (routeCheck != RouteCheck.BLOCKED) {
                return true;
            }
            state.sharedBreachAssigned = true;
            state.breachRouteCheckCooldown = BREACH_ROUTE_CHECK_TICKS;
        } else if (objective.target() != null
                && state.breachRouteCheckCooldown == 0) {
            RouteCheck routeCheck = tryReachableRoute(objective);
            state.breachRouteCheckCooldown =
                    routeCheck == RouteCheck.DEFERRED
                            ? 1
                            : BREACH_ROUTE_CHECK_TICKS;
            if (routeCheck != RouteCheck.BLOCKED) {
                return true;
            }
        }
        state.sharedBreachTarget = plan.target();
        if (plan.target().distToCenterSqr(mob.position()) > 16.0D) {
            navigate(new Objective(plan.approach().getCenter(), null));
            return true;
        }

        stopNavigation();
        mob.getLookControl().setLookAt(
                plan.target().getX() + 0.5D,
                plan.target().getY() + 0.5D,
                plan.target().getZ() + 0.5D
        );
        SiegeCoordinationManager.Contribution contribution =
                SiegeCoordinationManager.contribute(
                        level,
                        center,
                        mob
                );
        state.action = contribution.contributed()
                ? HostileAction.BREACH
                : HostileAction.WAIT;
        if (contribution.contributed()
                && level.getGameTime() % ATTACK_INTERVAL_TICKS == 0L) {
            mob.swing(InteractionHand.MAIN_HAND);
        }
        state.sharedBreachTarget = contribution.target();
        if (contribution.openingCreated()) {
            state.observedOpeningRevision = SiegeCoordinationManager
                    .openingRevision(level, center);
            resetNavigationAfterOpening();
            return false;
        }
        return true;
    }

    private void reactToNewOpening() {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = coordinationCenter();
        if (center == null) {
            return;
        }
        long revision = SiegeCoordinationManager.openingRevision(
                level,
                center
        );
        if (state.observedOpeningRevision < 0L) {
            state.observedOpeningRevision = revision;
            return;
        }
        if (revision == state.observedOpeningRevision) {
            return;
        }
        state.observedOpeningRevision = revision;
        if (SiegeCoordinationManager.opening(level, center, mob) == null) {
            return;
        }
        if (state.breachTarget != null
                || state.sharedBreachAssigned
                || state.consecutivePathFailures > 0
                || mob.getNavigation().isStuck()) {
            resetNavigationAfterOpening();
        }
    }

    private void resetNavigationAfterOpening() {
        clearBreachProgress();
        stopNavigation();
        state.sharedBreachTarget = null;
        state.sharedBreachAssigned = false;
        state.lastBlockingObstacle = null;
        state.consecutivePathFailures = 0;
        state.pathCooldown = 0;
        state.obstacleScanCooldown = OBSTACLE_SCAN_TICKS;
        state.campDamageCooldown = Math.max(
                state.campDamageCooldown,
                OBSTACLE_SCAN_TICKS
        );
        state.lastObjective = null;
        state.action = HostileAction.WAIT;
    }

    private Vec3 segmentDestination(Vec3 destination) {
        double segmentDistance = SurvivalSystemsConfig
                .COORDINATED_AI_PATH_SEGMENT_DISTANCE
                .get();
        double deltaX = destination.x - mob.getX();
        double deltaZ = destination.z - mob.getZ();
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontal <= segmentDistance) {
            return destination;
        }
        double ratio = segmentDistance / horizontal;
        return new Vec3(
                mob.getX() + deltaX * ratio,
                mob.getY() + (destination.y - mob.getY()) * ratio,
                mob.getZ() + deltaZ * ratio
        );
    }

    private double movementSpeed() {
        return switch (state.intent) {
            case HUNT -> 1.15D;
            case ASSAULT -> 1.10D;
            case INVESTIGATE -> 1.05D;
            case IDLE -> 1.0D;
        };
    }

    private boolean canBreach() {
        if (assaultTarget() != null) {
            return true;
        }
        // A fresh pursuit of a survivor or non-creative player may escalate
        // from tagged local defenses to the guarded structural lane after
        // enough failed routes. Target-less ambient zombies still cannot mine.
        if (isStructuralBreachTarget(mob.getTarget())) {
            return true;
        }
        if (EncounterEntityData.read(mob).isPresent()) {
            return SurvivalSystemsConfig
                    .COORDINATED_AI_BUILDING_BREACH
                    .get();
        }
        if (AwarenessManager.isHordeSpawn(mob)) {
            return SurvivalSystemsConfig
                    .COORDINATED_AI_HORDE_BREACH
                    .get();
        }
        return SurvivalSystemsConfig.COORDINATED_AI_AMBIENT_BREACH.get();
    }

    private boolean targetsActiveSiegeSurvivor(SurvivorEntity survivor) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        SurvivorEntity.SettlementBinding binding = survivor
                .settlementBinding()
                .orElse(null);
        if (binding == null) {
            return false;
        }
        boolean active = SettlementManager.status(level, binding.cityZone())
                .filter(settlement -> settlement.id().equals(
                        binding.settlementId()
                )).map(settlement ->
                        settlement.siegeState()
                                == SettlementSiegeState.ACTIVE
                ).orElse(false);
        return active;
    }

    private BlockPos findBreachableObstacle(Vec3 destination) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        state.lastBlockingObstacle = null;
        BlockPos physicalObstacle = findPhysicalBreachableObstacle(
                level,
                destination
        );
        if (physicalObstacle != null) {
            state.lastBlockingObstacle = physicalObstacle;
            return physicalObstacle;
        }
        LivingEntity target = mob.getTarget();
        if (target != null) {
            BlockHitResult sightHit = level.clip(new ClipContext(
                    mob.getEyePosition(),
                    target.getEyePosition(),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mob
            ));
            if (sightHit.getType() != HitResult.Type.MISS) {
                state.lastBlockingObstacle = sightHit.getBlockPos().immutable();
                if (state.lastBlockingObstacle.distToCenterSqr(mob.position())
                        <= 25.0D
                        && SiegeBreachRules.canBreach(
                                level,
                                state.lastBlockingObstacle
                        )) {
                    return state.lastBlockingObstacle;
                }
            }
        }
        for (double height : BREACH_RAY_HEIGHTS) {
            Vec3 start = new Vec3(
                    mob.getX(),
                    mob.getY() + height,
                    mob.getZ()
            );
            Vec3 end = new Vec3(
                    destination.x,
                    destination.y + height,
                    destination.z
            );
            BlockHitResult hit = level.clip(new ClipContext(
                    start,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mob
            ));
            if (hit.getType() != HitResult.Type.MISS) {
                BlockPos hitPosition = hit.getBlockPos().immutable();
                if (state.lastBlockingObstacle == null) {
                    state.lastBlockingObstacle = hitPosition;
                }
                if (hitPosition.distToCenterSqr(mob.position()) <= 25.0D
                        && SiegeBreachRules.canBreach(level, hitPosition)) {
                    return hitPosition;
                }
            }
        }

        // Modded fences often expose multipart or unusually narrow collision
        // shapes. When pathing is already blocked, sample a tiny corridor in
        // front of the mob rather than depending on one ray hitting the exact
        // tagged part of that shape.
        double deltaX = destination.x - mob.getX();
        double deltaZ = destination.z - mob.getZ();
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontal < 1.0E-4D) {
            return null;
        }
        double directionX = deltaX / horizontal;
        double directionZ = deltaZ / horizontal;
        double sideX = -directionZ;
        double sideZ = directionX;
        Set<BlockPos> visited = new HashSet<>();
        for (double forward = BREACH_SCAN_STEP;
                forward <= BREACH_SCAN_RANGE;
                forward += BREACH_SCAN_STEP) {
            for (double lateral : BREACH_LATERAL_OFFSETS) {
                double sampleX = mob.getX() + directionX * forward
                        + sideX * lateral;
                double sampleZ = mob.getZ() + directionZ * forward
                        + sideZ * lateral;
                for (int vertical : BREACH_VERTICAL_OFFSETS) {
                    BlockPos candidate = BlockPos.containing(
                            sampleX,
                            mob.getY() + vertical,
                            sampleZ
                    );
                    if (!visited.add(candidate)
                            || !SiegeBreachRules.canBreach(level, candidate)) {
                        continue;
                    }
                    state.lastBlockingObstacle = candidate.immutable();
                    return state.lastBlockingObstacle;
                }
            }
        }
        return null;
    }

    /**
     * Fences can leave a clear eye-level ray while still blocking the mob's
     * body. Sweep the body-sized collision box forward before falling back to
     * sight rays and point samples, so multipart modded fences are recognized
     * as the immediate local breach target.
     */
    private BlockPos findPhysicalBreachableObstacle(
            ServerLevel level,
            Vec3 destination
    ) {
        double deltaX = destination.x - mob.getX();
        double deltaZ = destination.z - mob.getZ();
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontal < 1.0E-4D) {
            return null;
        }
        double directionX = deltaX / horizontal;
        double directionZ = deltaZ / horizontal;
        AABB body = mob.getBoundingBox();
        for (double forward = 0.15D;
                forward <= BREACH_SCAN_RANGE;
                forward += 0.25D) {
            AABB swept = body.move(
                    directionX * forward,
                    0.0D,
                    directionZ * forward
            );
            int minimumX = (int) Math.floor(swept.minX + 1.0E-4D);
            int maximumX = (int) Math.floor(swept.maxX - 1.0E-4D);
            int minimumY = (int) Math.floor(swept.minY + 0.05D);
            int maximumY = (int) Math.floor(swept.maxY - 1.0E-4D);
            int minimumZ = (int) Math.floor(swept.minZ + 1.0E-4D);
            int maximumZ = (int) Math.floor(swept.maxZ - 1.0E-4D);
            for (int x = minimumX; x <= maximumX; x++) {
                for (int y = minimumY; y <= maximumY; y++) {
                    for (int z = minimumZ; z <= maximumZ; z++) {
                        BlockPos candidate = new BlockPos(x, y, z);
                        if (!SiegeBreachRules.canBreach(level, candidate)) {
                            continue;
                        }
                        VoxelShape shape = level.getBlockState(candidate)
                                .getCollisionShape(level, candidate);
                        if (shape.isEmpty()) {
                            continue;
                        }
                        for (AABB collision : shape.toAabbs()) {
                            if (collision.move(
                                    candidate.getX(),
                                    candidate.getY(),
                                    candidate.getZ()
                            ).intersects(swept)) {
                                return candidate.immutable();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findCampDamageTarget(BlockPos center) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos candidate = center.offset(
                    mob.getRandom().nextInt(9) - 4,
                    mob.getRandom().nextInt(5) - 2,
                    mob.getRandom().nextInt(9) - 4
            );
            if (candidate.distToCenterSqr(mob.position()) <= 16.0D
                    && SiegeBreachRules.canBreach(level, candidate)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    private void startBreach(BlockPos target) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        state.breachTarget = target;
        state.breachProgress = 0;
        state.breachDuration = SiegeBreachRules.durationTicks(
                level.getBlockState(target)
        );
        state.breachRouteCheckCooldown = BREACH_ROUTE_CHECK_TICKS;
        state.action = HostileAction.BREACH;
        stopNavigation();
    }

    private boolean tickBreachProgress() {
        BlockPos target = state.breachTarget;
        if (target == null || !(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        if (target.distToCenterSqr(mob.position()) > 16.0D
                || !SiegeBreachRules.canBreach(level, target)) {
            clearBreachProgress();
            return false;
        }
        state.action = HostileAction.BREACH;
        stopNavigation();
        mob.getLookControl().setLookAt(
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D
        );
        state.breachProgress++;
        if (state.breachProgress == 1
                || state.breachProgress % ATTACK_INTERVAL_TICKS == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
        }
        int stage = Math.min(
                9,
                state.breachProgress * 10
                        / Math.max(1, state.breachDuration)
        );
        level.destroyBlockProgress(mob.getId(), target, stage);
        if (state.breachProgress >= state.breachDuration) {
            clearBreachProgress();
            if (level.destroyBlock(target, true, mob)) {
                BlockPos center = coordinationCenter();
                if (center != null) {
                    state.observedOpeningRevision = SiegeCoordinationManager
                            .recordLocalOpening(level, center, target);
                }
                resetNavigationAfterOpening();
            }
        }
        return true;
    }

    private void clearBreachProgress() {
        if (state.breachTarget != null
                && mob.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(mob.getId(), state.breachTarget, -1);
        }
        state.breachTarget = null;
        state.breachProgress = 0;
        state.breachDuration = 0;
        state.breachRouteCheckCooldown = 0;
    }

    private void stopNavigation() {
        mob.getNavigation().stop();
        state.activePathPartial = false;
    }

    private BlockPos assaultTarget() {
        return mob.getPersistentData().contains(
                CoordinatedHostileAi.ASSAULT_TARGET_TAG
        ) ? BlockPos.of(mob.getPersistentData().getLong(
                CoordinatedHostileAi.ASSAULT_TARGET_TAG
        )) : null;
    }

    private BlockPos coordinationCenter() {
        BlockPos assault = assaultTarget();
        if (assault != null) {
            return assault;
        }
        LivingEntity target = mob.getTarget();
        if (!validTarget(target) || !isStructuralBreachTarget(target)) {
            state.cachedCoordinationCenter = null;
            state.coordinationTargetId = null;
            return null;
        }
        long now = mob.level().getGameTime();
        UUID targetId = target.getUUID();
        if (targetId.equals(state.coordinationTargetId)
                && now < state.nextCoordinationCheckAt) {
            return state.cachedCoordinationCenter;
        }
        state.nextCoordinationCheckAt = now + 20L;
        state.cachedCoordinationCenter = null;
        state.coordinationTargetId = targetId;
        if (target instanceof SurvivorEntity survivor
                && targetsActiveSiegeSurvivor(survivor)) {
            state.cachedCoordinationCenter = survivor.homePosition();
        } else {
            state.cachedCoordinationCenter = structuralBreachAnchor(target);
        }
        return state.cachedCoordinationCenter;
    }

    private boolean isStructuralBreachTarget(LivingEntity target) {
        return target instanceof SurvivorEntity || target instanceof ServerPlayer;
    }

    private BlockPos structuralBreachAnchor(LivingEntity target) {
        BlockPos position = target.blockPosition();
        return new BlockPos(
                Math.floorDiv(position.getX(), 8) * 8 + 4,
                position.getY(),
                Math.floorDiv(position.getZ(), 8) * 8 + 4
        );
    }

    private record Objective(
            Vec3 position,
            LivingEntity target,
            SiegeCoordinationManager.OpeningView opening
    ) {
        private Objective(Vec3 position, LivingEntity target) {
            this(position, target, null);
        }
    }

    private enum RouteCheck {
        REACHABLE,
        BLOCKED,
        DEFERRED
    }
}
