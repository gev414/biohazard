package io.github.gev414.rotwire.settlement;

/**
 * City-wide upgrades. Radio modules remain local infrastructure and are
 * recorded in each radio status; these flags are reserved for shared rules.
 */
public enum SettlementUpgrade {
    CAMP_HUB,
    FAST_TRAVEL,
    DEFENSES;

    public int mask() {
        return 1 << ordinal();
    }

    public static int sanitizeMask(int mask) {
        int valid = 0;
        for (SettlementUpgrade upgrade : values()) {
            valid |= upgrade.mask();
        }
        return mask & valid;
    }
}
