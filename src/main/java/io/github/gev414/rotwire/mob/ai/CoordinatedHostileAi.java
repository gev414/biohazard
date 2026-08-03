package io.github.gev414.rotwire.mob.ai;

import io.github.gev414.rotwire.city.ModEntityTypeTags;
import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import io.github.gev414.rotwire.settlement.SiegeBreachRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Registry and shared path budget for Rotwire-coordinated hostile AI. */
public final class CoordinatedHostileAi {

    public static final String ASSAULT_TARGET_TAG = "rotwireSiegeTarget";
    private static final int GOAL_PRIORITY = -4;
    private static final int MEDIUM_PATH_LOAD = 24;
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<ResourceKey<Level>, PathBudget> PATH_BUDGETS =
            new HashMap<>();
    private static final Map<ResourceKey<Level>, Integer> ACTIVE_COUNTS =
            new HashMap<>();
    private static int cleanupTicks;

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Mob mob)
                || !mob.getType().is(ModEntityTypeTags.COORDINATED_HOSTILES)
                || !SurvivalSystemsConfig
                .COORDINATED_INFECTED_AI_ENABLED
                .get()) {
            return;
        }
        ZombieTacticsCompatibility.suppressCompetingGoals(mob);
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof CoordinatedHostileGoal existing) {
                STATES.put(mob.getUUID(), existing.state());
                return;
            }
        }
        State state = new State(mob);
        STATES.put(mob.getUUID(), state);
        mob.goalSelector.addGoal(
                GOAL_PRIORITY,
                new CoordinatedHostileGoal(mob, state)
        );
    }

    public static boolean investigate(
            Mob mob,
            Vec3 position,
            long expiresAt
    ) {
        State state = STATES.get(mob.getUUID());
        if (state == null || expiresAt <= mob.level().getGameTime()) {
            return false;
        }
        state.investigationPosition = position;
        state.investigationExpiresAt = Math.max(
                state.investigationExpiresAt,
                expiresAt
        );
        return true;
    }

    static boolean requestPath(ServerLevel level, Mob mob) {
        PathBudget budget = PATH_BUDGETS.computeIfAbsent(
                level.dimension(),
                ignored -> new PathBudget()
        );
        return budget.tryAcquire(
                level.getGameTime(),
                SurvivalSystemsConfig.COORDINATED_AI_PATHS_PER_TICK.get(),
                mob.getUUID()
        );
    }

    static void recordPathCalculation(ServerLevel level, long elapsedNanos) {
        PATH_BUDGETS.computeIfAbsent(
                level.dimension(),
                ignored -> new PathBudget()
        ).recordCalculation(level.getGameTime(), elapsedNanos);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        SiegeCoordinationManager.onServerTick();
        ACTIVE_COUNTS.clear();
        for (State state : STATES.values()) {
            Mob mob = state.mob;
            if (state.intent != HostileIntent.IDLE
                    && mob.isAlive()
                    && !mob.isRemoved()
                    && mob.level() instanceof ServerLevel level) {
                ACTIVE_COUNTS.merge(level.dimension(), 1, Integer::sum);
            }
        }
        for (Map.Entry<ResourceKey<Level>, PathBudget> entry
                : PATH_BUDGETS.entrySet()) {
            entry.getValue().activeCount = ACTIVE_COUNTS.getOrDefault(
                    entry.getKey(),
                    0
            );
        }
        if (++cleanupTicks < 200) {
            return;
        }
        cleanupTicks = 0;
        Iterator<State> iterator = STATES.values().iterator();
        while (iterator.hasNext()) {
            Mob mob = iterator.next().mob;
            if (!mob.isAlive() || mob.isRemoved()) {
                iterator.remove();
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
        PATH_BUDGETS.clear();
        ACTIVE_COUNTS.clear();
        SiegeCoordinationManager.clear();
        cleanupTicks = 0;
    }

    public static Report report(
            ServerLevel level,
            Vec3 origin,
            double radius
    ) {
        double radiusSqr = radius * radius;
        EnumMap<HostileIntent, Integer> intents = new EnumMap<>(
                HostileIntent.class
        );
        EnumMap<HostileAction, Integer> actions = new EnumMap<>(
                HostileAction.class
        );
        Snapshot nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int total = 0;
        for (State state : STATES.values()) {
            Mob mob = state.mob;
            if (!mob.isAlive() || mob.level() != level) {
                continue;
            }
            double distance = mob.distanceToSqr(origin);
            if (distance > radiusSqr) {
                continue;
            }
            total++;
            intents.merge(state.intent, 1, Integer::sum);
            actions.merge(state.action, 1, Integer::sum);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = snapshot(state);
            }
        }
        PathBudget budget = PATH_BUDGETS.get(level.dimension());
        PathBudgetSnapshot pathBudget = budget == null
                ? PathBudgetSnapshot.EMPTY
                : budget.snapshot(level.getGameTime());
        return new Report(total, intents, actions, nearest, pathBudget);
    }

    private static Snapshot snapshot(State state) {
        Mob mob = state.mob;
        LivingEntity target = mob.getTarget();
        BlockPos coordinationCenter = mob.getPersistentData().contains(
                ASSAULT_TARGET_TAG
        ) ? BlockPos.of(mob.getPersistentData().getLong(
                ASSAULT_TARGET_TAG
        )) : state.cachedCoordinationCenter;
        SiegeCoordinationManager.GroupSnapshot siegeGroup =
                mob.level() instanceof ServerLevel level
                        ? SiegeCoordinationManager.snapshot(
                                level,
                                coordinationCenter,
                                mob
                        )
                        : SiegeCoordinationManager.GroupSnapshot.EMPTY;
        BlockPos obstacle = state.lastBlockingObstacle;
        boolean obstacleBreachable = obstacle != null
                && mob.level() instanceof ServerLevel level
                && SiegeBreachRules.canBreach(level, obstacle);
        return new Snapshot(
                mob.getId(),
                mob.getDisplayName().getString(),
                state.intent,
                state.action,
                target == null ? "none" : target.getDisplayName().getString(),
                state.pathCooldown,
                mob.getNavigation().isDone(),
                state.pathAttempts,
                state.lastPathSucceeded,
                state.activePathPartial,
                state.breachTarget == null
                        ? state.sharedBreachTarget
                        : state.breachTarget,
                target == null ? -1.0D : mob.distanceTo(target),
                target != null && mob.getSensing().hasLineOfSight(target),
                obstacle == null
                        ? "none"
                        : BuiltInRegistries.BLOCK.getKey(
                                mob.level().getBlockState(obstacle).getBlock()
                        ) + " " + obstacle.toShortString(),
                obstacleBreachable,
                state.breachAllowed,
                state.consecutivePathFailures,
                siegeGroup.routeFailures(),
                siegeGroup.breachPlan(),
                siegeGroup.opening()
        );
    }

    static final class State {
        final Mob mob;
        HostileIntent intent = HostileIntent.IDLE;
        HostileAction action = HostileAction.WAIT;
        Vec3 investigationPosition;
        long investigationExpiresAt = -1L;
        LivingEntity rememberedTarget;
        long targetMemoryExpiresAt = -1L;
        int pathCooldown;
        int survivorScanCooldown;
        int obstacleScanCooldown;
        int attackCooldown;
        int campDamageCooldown;
        Vec3 lastObjective;
        BlockPos breachTarget;
        BlockPos sharedBreachTarget;
        BlockPos cachedCoordinationCenter;
        UUID coordinationTargetId;
        long nextCoordinationCheckAt;
        long observedOpeningRevision = -1L;
        long activeOpeningRevision = -1L;
        BlockPos lastBlockingObstacle;
        int breachProgress;
        int breachDuration;
        int breachRouteCheckCooldown;
        long pathAttempts;
        int consecutivePathFailures;
        boolean lastPathSucceeded;
        boolean activePathPartial;
        boolean breachAllowed;
        boolean sharedBreachAssigned;

        State(Mob mob) {
            this.mob = mob;
            int jitter = Math.max(
                    1,
                    SurvivalSystemsConfig
                            .COORDINATED_AI_PATH_RETRY_JITTER_TICKS
                            .get() + 1
            );
            pathCooldown = mob.getRandom().nextInt(jitter);
            survivorScanCooldown = mob.getRandom().nextInt(
                    Math.max(
                            1,
                            SurvivalSystemsConfig
                                    .COORDINATED_AI_SURVIVOR_SCAN_TICKS
                                    .get()
                    )
            );
        }
    }

    private static final class PathBudget {
        private long tick = Long.MIN_VALUE;
        private int used;
        private int activeCount;
        private long window = Long.MIN_VALUE;
        private int calculated;
        private int deferred;
        private long totalNanos;
        private long maximumNanos;
        private int configuredLimit;
        private int effectiveLimit;
        private boolean hasCompletedWindow;
        private int lastCalculated;
        private int lastDeferred;
        private long lastTotalNanos;
        private long lastMaximumNanos;
        private final LinkedHashMap<UUID, Long> waiting =
                new LinkedHashMap<>();

        boolean tryAcquire(long gameTime, int maximum, UUID requester) {
            rotateWindow(gameTime);
            if (tick != gameTime) {
                tick = gameTime;
                used = 0;
                waiting.entrySet().removeIf(
                        entry -> entry.getValue() < gameTime - 1L
                );
            }
            configuredLimit = maximum;
            effectiveLimit = maximum;
            if (activeCount >= MEDIUM_PATH_LOAD) {
                effectiveLimit = Math.min(effectiveLimit, 2);
            }
            waiting.putIfAbsent(requester, gameTime);
            waiting.replace(requester, gameTime);
            UUID next = waiting.keySet().stream().findFirst().orElse(null);
            if (used >= effectiveLimit || !requester.equals(next)) {
                deferred++;
                return false;
            }
            waiting.remove(requester);
            used++;
            calculated++;
            return true;
        }

        void recordCalculation(long gameTime, long elapsedNanos) {
            rotateWindow(gameTime);
            long duration = Math.max(0L, elapsedNanos);
            totalNanos += duration;
            maximumNanos = Math.max(maximumNanos, duration);
        }

        PathBudgetSnapshot snapshot(long gameTime) {
            rotateWindow(gameTime);
            int sampleCalculated = hasCompletedWindow
                    ? lastCalculated
                    : calculated;
            int sampleDeferred = hasCompletedWindow
                    ? lastDeferred
                    : deferred;
            long sampleTotalNanos = hasCompletedWindow
                    ? lastTotalNanos
                    : totalNanos;
            long sampleMaximumNanos = hasCompletedWindow
                    ? lastMaximumNanos
                    : maximumNanos;
            return new PathBudgetSnapshot(
                    activeCount,
                    configuredLimit,
                    effectiveLimit,
                    waiting.size(),
                    sampleCalculated,
                    sampleDeferred,
                    sampleCalculated == 0
                            ? 0.0D
                            : sampleTotalNanos / 1_000_000.0D
                                    / sampleCalculated,
                    sampleMaximumNanos / 1_000_000.0D
            );
        }

        private void rotateWindow(long gameTime) {
            long currentWindow = gameTime / 20L;
            if (window == currentWindow) {
                return;
            }
            if (window != Long.MIN_VALUE) {
                hasCompletedWindow = true;
                lastCalculated = calculated;
                lastDeferred = deferred;
                lastTotalNanos = totalNanos;
                lastMaximumNanos = maximumNanos;
            }
            window = currentWindow;
            calculated = 0;
            deferred = 0;
            totalNanos = 0L;
            maximumNanos = 0L;
        }
    }

    public record Snapshot(
            int entityId,
            String name,
            HostileIntent intent,
            HostileAction action,
            String target,
            int pathCooldown,
            boolean navigationDone,
            long pathAttempts,
            boolean lastPathSucceeded,
            boolean activePathPartial,
            BlockPos breachTarget,
            double targetDistance,
            boolean targetVisible,
            String lastObstacle,
            boolean lastObstacleBreachable,
            boolean breachAllowed,
            int consecutivePathFailures,
            int sharedRouteFailures,
            String sharedBreachPlan,
            String sharedOpening
    ) {
    }

    public record Report(
            int total,
            Map<HostileIntent, Integer> intents,
            Map<HostileAction, Integer> actions,
            Snapshot nearest,
            PathBudgetSnapshot pathBudget
    ) {
    }

    public record PathBudgetSnapshot(
            int active,
            int configuredLimit,
            int effectiveLimit,
            int queued,
            int calculated,
            int deferred,
            double averageMilliseconds,
            double maximumMilliseconds
    ) {
        static final PathBudgetSnapshot EMPTY = new PathBudgetSnapshot(
                0,
                0,
                0,
                0,
                0,
                0,
                0.0D,
                0.0D
        );
    }

    private CoordinatedHostileAi() {
    }
}
