package io.github.gev414.biohazard.encumbrance;

public final class EncumbranceMath {

    private static final double MINIMUM_STACK_FRACTION = 0.25D;

    public static double scaledStackWeight(
            double categoryWeight,
            int count,
            int maximumStackSize
    ) {
        if (categoryWeight <= 0.0D || count <= 0) {
            return 0.0D;
        }
        int safeMaximum = Math.max(1, maximumStackSize);
        double fullness = Math.min(
                1.0D,
                (double) count / safeMaximum
        );
        return categoryWeight * Math.max(
                MINIMUM_STACK_FRACTION,
                fullness
        );
    }

    public static int displayTenths(double weight) {
        return (int) Math.round(Math.max(0.0D, weight) * 10.0D);
    }

    private EncumbranceMath() {
    }
}
