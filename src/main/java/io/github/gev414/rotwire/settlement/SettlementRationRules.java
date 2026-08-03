package io.github.gev414.rotwire.settlement;

/**
 * Bounded arithmetic shared by settlement population and daily upkeep.
 */
public final class SettlementRationRules {

    public static int dailyCost(int population, int rationsPerSettler) {
        long cost = (long) Math.max(0, population)
                * Math.max(0, rationsPerSettler);
        return (int) Math.min(Integer.MAX_VALUE, cost);
    }

    private SettlementRationRules() {
    }
}
