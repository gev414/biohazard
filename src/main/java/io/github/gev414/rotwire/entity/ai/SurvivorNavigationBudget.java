package io.github.gev414.rotwire.entity.ai;

import io.github.gev414.rotwire.config.SettlementConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Shared per-level budget and diagnostics for survivor path creation. */
public final class SurvivorNavigationBudget {

    private static final Map<ResourceKey<Level>, Budget> BUDGETS =
            new HashMap<>();

    public static boolean requestPath(ServerLevel level) {
        return budget(level).tryAcquire(
                level.getGameTime(),
                SettlementConfig.SURVIVOR_PATHS_PER_TICK.get()
        );
    }

    public static void recordPathCalculation(
            ServerLevel level,
            long elapsedNanos
    ) {
        budget(level).recordCalculation(
                level.getGameTime(),
                elapsedNanos
        );
    }

    public static Snapshot snapshot(ServerLevel level) {
        Budget budget = BUDGETS.get(level.dimension());
        return budget == null
                ? Snapshot.EMPTY
                : budget.snapshot(level.getGameTime());
    }

    public static void clear() {
        BUDGETS.clear();
    }

    private static Budget budget(ServerLevel level) {
        return BUDGETS.computeIfAbsent(
                level.dimension(),
                ignored -> new Budget()
        );
    }

    private static final class Budget {
        private long tick = Long.MIN_VALUE;
        private int used;
        private int limit;
        private long window = Long.MIN_VALUE;
        private int calculated;
        private int deferred;
        private long totalNanos;
        private long maximumNanos;
        private boolean hasCompletedWindow;
        private int lastCalculated;
        private int lastDeferred;
        private long lastTotalNanos;
        private long lastMaximumNanos;

        boolean tryAcquire(long gameTime, int configuredLimit) {
            rotateWindow(gameTime);
            if (tick != gameTime) {
                tick = gameTime;
                used = 0;
            }
            limit = configuredLimit;
            if (used >= limit) {
                deferred++;
                return false;
            }
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

        Snapshot snapshot(long gameTime) {
            rotateWindow(gameTime);
            int sampleCalculated = hasCompletedWindow
                    ? lastCalculated
                    : calculated;
            int sampleDeferred = hasCompletedWindow
                    ? lastDeferred
                    : deferred;
            long sampleTotal = hasCompletedWindow
                    ? lastTotalNanos
                    : totalNanos;
            long sampleMaximum = hasCompletedWindow
                    ? lastMaximumNanos
                    : maximumNanos;
            return new Snapshot(
                    limit,
                    sampleCalculated,
                    sampleDeferred,
                    sampleCalculated == 0
                            ? 0.0D
                            : sampleTotal / 1_000_000.0D
                                    / sampleCalculated,
                    sampleMaximum / 1_000_000.0D
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
            int limit,
            int calculated,
            int deferred,
            double averageMilliseconds,
            double maximumMilliseconds
    ) {
        static final Snapshot EMPTY = new Snapshot(
                0,
                0,
                0,
                0.0D,
                0.0D
        );
    }

    private SurvivorNavigationBudget() {
    }
}
