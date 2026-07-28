package io.github.gev414.rotwire.encumbrance;

public final class EncumbranceMath {

    public static double itemCountWeight(
            double itemWeight,
            int count
    ) {
        if (itemWeight <= 0.0D || count <= 0) {
            return 0.0D;
        }
        return itemWeight * count;
    }

    public static int displayTenths(double weight) {
        return (int) Math.round(Math.max(0.0D, weight) * 10.0D);
    }

    private EncumbranceMath() {
    }
}
