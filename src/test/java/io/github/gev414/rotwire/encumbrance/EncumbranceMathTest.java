package io.github.gev414.rotwire.encumbrance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncumbranceMathTest {

    @Test
    void itemWeightScalesLinearlyFromTheFirstItem() {
        assertEquals(
                0.12D,
                EncumbranceMath.itemCountWeight(0.12D, 1),
                0.0001D
        );
        assertEquals(
                1.92D,
                EncumbranceMath.itemCountWeight(0.12D, 16),
                0.0001D
        );
        assertEquals(
                7.68D,
                EncumbranceMath.itemCountWeight(0.12D, 64),
                0.0001D
        );
        assertEquals(
                0.0D,
                EncumbranceMath.itemCountWeight(0.12D, 0),
                0.0001D
        );
    }

    @Test
    void tierBoundariesAreInclusiveAndOrdered() {
        assertEquals(
                EncumbranceTier.LIGHT,
                EncumbranceTier.forWeight(10.0D, 10.0D, 20.0D, 35.0D)
        );
        assertEquals(
                EncumbranceTier.BURDENED,
                EncumbranceTier.forWeight(10.1D, 10.0D, 20.0D, 35.0D)
        );
        assertEquals(
                EncumbranceTier.HEAVY,
                EncumbranceTier.forWeight(20.1D, 10.0D, 20.0D, 35.0D)
        );
        assertEquals(
                EncumbranceTier.OVERLOADED,
                EncumbranceTier.forWeight(35.1D, 10.0D, 20.0D, 35.0D)
        );
    }
}
