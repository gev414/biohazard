package io.github.gev414.rotwire.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurvivalMeterEffectTest {

    @Test
    void clampsDrainAtEmpty() {
        assertEquals(0, SurvivalMeterEffect.adjustMeter(1, -2));
    }

    @Test
    void clampsRecoveryAtFull() {
        assertEquals(20, SurvivalMeterEffect.adjustMeter(19, 2));
    }

    @Test
    void changesBothDirectionsWithinBounds() {
        assertEquals(8, SurvivalMeterEffect.adjustMeter(10, -2));
        assertEquals(12, SurvivalMeterEffect.adjustMeter(10, 2));
    }
}
