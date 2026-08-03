package io.github.gev414.rotwire.settlement;

import java.util.Objects;

/**
 * Pure retreat policy shared by civilian and future guard survivors.
 *
 * <p>The civilian introduced in the first vertical slice has no ranged or
 * melee loadout, so only the active-siege branch applies to it directly. The
 * loadout branches are deliberately implemented here now so the planned
 * Rotwire hitscan guards use exactly the same safety contract rather than
 * duplicating the decision later.</p>
 */
public final class SurvivorSafetyRules {

    public static boolean mustReturnToCamp(SafetyContext context) {
        Objects.requireNonNull(context, "context");
        return context.siegeState() == SettlementSiegeState.ACTIVE
                || (context.hasRangedLoadout()
                && context.ammunition() <= 0)
                || (context.inMeleeFight()
                && context.hostileCount()
                > Math.max(1, context.meleeAllyCount()));
    }

    public record SafetyContext(
            SettlementSiegeState siegeState,
            boolean hasRangedLoadout,
            int ammunition,
            boolean inMeleeFight,
            int hostileCount,
            int meleeAllyCount
    ) {

        public SafetyContext {
            Objects.requireNonNull(siegeState, "siegeState");
        }
    }

    private SurvivorSafetyRules() {
    }
}
