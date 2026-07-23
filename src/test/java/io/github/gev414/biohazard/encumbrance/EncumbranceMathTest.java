package io.github.gev414.biohazard.encumbrance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncumbranceMathTest {

    @Test
    void stackWeightUsesQuarterMinimumAndFullStackMaximum() {
        assertEquals(
                0.5D,
                EncumbranceMath.scaledStackWeight(2.0D, 1, 64),
                0.0001D
        );
        assertEquals(
                1.0D,
                EncumbranceMath.scaledStackWeight(2.0D, 32, 64),
                0.0001D
        );
        assertEquals(
                2.0D,
                EncumbranceMath.scaledStackWeight(2.0D, 64, 64),
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
