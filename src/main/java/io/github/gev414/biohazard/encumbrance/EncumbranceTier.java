package io.github.gev414.biohazard.encumbrance;

public enum EncumbranceTier {
    LIGHT,
    BURDENED,
    HEAVY,
    OVERLOADED;

    public static EncumbranceTier forWeight(
            double weight,
            double lightMaximum,
            double burdenedMaximum,
            double heavyMaximum
    ) {
        double safeWeight = Math.max(0.0D, weight);
        double safeLight = Math.max(0.0D, lightMaximum);
        double safeBurdened = Math.max(safeLight, burdenedMaximum);
        double safeHeavy = Math.max(safeBurdened, heavyMaximum);

        if (safeWeight <= safeLight) {
            return LIGHT;
        }
        if (safeWeight <= safeBurdened) {
            return BURDENED;
        }
        if (safeWeight <= safeHeavy) {
            return HEAVY;
        }
        return OVERLOADED;
    }

    public boolean permitsQuietMovement() {
        return this == LIGHT;
    }
}
