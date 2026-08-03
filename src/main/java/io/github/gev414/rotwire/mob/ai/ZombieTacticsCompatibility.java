package io.github.gev414.rotwire.mob.ai;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.util.List;
import java.util.Set;

/** Suppresses ZombieTactics behavior that competes with coordinated movement. */
final class ZombieTacticsCompatibility {

    private static final Set<String> COMPETING_GOALS = Set.of(
            "n643064.zombie_tactics.ZombieMineGoal",
            "n643064.zombie_tactics.MoveTowardsMarkerGoal",
            "n643064.zombie_tactics.RemoveMarkerGoal"
    );
    private static boolean climbingHookChecked;

    static void suppressCompetingGoals(Mob mob) {
        suppressCollisionClimbing();
        if (!SurvivalSystemsConfig.SUPPRESS_ZOMBIE_TACTICS_AI_GOALS.get()) {
            return;
        }
        for (WrappedGoal wrapped : List.copyOf(
                mob.goalSelector.getAvailableGoals()
        )) {
            Goal goal = wrapped.getGoal();
            if (COMPETING_GOALS.contains(goal.getClass().getName())) {
                mob.goalSelector.removeGoal(goal);
            }
        }
    }

    /**
     * ZombieTactics implements group climbing as a global collision injection,
     * not as a removable goal. Rotwire therefore disables that hook once when
     * it owns coordinated movement. Vanilla step and jump navigation remain
     * available; only the repeated zombie-on-zombie vertical boost is removed.
     */
    private static void suppressCollisionClimbing() {
        if (climbingHookChecked
                || !SurvivalSystemsConfig
                        .SUPPRESS_ZOMBIE_TACTICS_CLIMBING
                        .get()) {
            return;
        }
        climbingHookChecked = true;
        try {
            Class<?> config = Class.forName(
                    "n643064.zombie_tactics.Config"
            );
            config.getField("zombiesClimbing").setBoolean(null, false);
            Rotwire.LOGGER.info(
                    "Disabled ZombieTactics collision climbing for coordinated infected AI"
            );
        } catch (ClassNotFoundException ignored) {
            // ZombieTactics is optional.
        } catch (ReflectiveOperationException | LinkageError exception) {
            Rotwire.LOGGER.warn(
                    "Could not disable ZombieTactics collision climbing",
                    exception
            );
        }
    }

    private ZombieTacticsCompatibility() {
    }
}
