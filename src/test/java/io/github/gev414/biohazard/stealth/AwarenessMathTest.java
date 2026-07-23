package io.github.gev414.biohazard.stealth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwarenessMathTest {

    @Test
    void fieldOfViewAcceptsFrontAndRejectsRear() {
        assertTrue(AwarenessMath.insideFieldOfView(
                0.0D,
                0.0D,
                1.0D,
                0.0D,
                0.0D,
                5.0D,
                140.0D
        ));
        assertFalse(AwarenessMath.insideFieldOfView(
                0.0D,
                0.0D,
                1.0D,
                0.0D,
                0.0D,
                -5.0D,
                140.0D
        ));
    }

    @Test
    void bruteMultiplierAcceleratesSuspicionAndValuesStayBounded() {
        double normal = AwarenessMath.gain(
                0.0D,
                35.0D,
                1.0D,
                0.0D,
                24.0D,
                1.0D
        );
        double brute = AwarenessMath.gain(
                0.0D,
                35.0D,
                1.0D,
                0.0D,
                24.0D,
                2.5D
        );
        assertEquals(35.0D, normal, 0.0001D);
        assertEquals(87.5D, brute, 0.0001D);
        assertEquals(
                100.0D,
                AwarenessMath.gain(
                        90.0D,
                        35.0D,
                        1.0D,
                        0.0D,
                        24.0D,
                        2.5D
                ),
                0.0001D
        );
    }

    @Test
    void suspicionDecaysToZero() {
        assertEquals(
                0.0D,
                AwarenessMath.decay(10.0D, 20.0D, 1.0D),
                0.0001D
        );
    }
}
