package io.github.gev414.biohazard.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurvivalStatusPayloadTest {

    @Test
    void normalizesThresholdOrderAndPercentages() {
        SurvivalStatusPayload payload = new SurvivalStatusPayload(
                -10,
                8,
                true,
                false,
                140,
                -5,
                100,
                50,
                -10,
                20,
                120
        );

        assertEquals(0, payload.weightTenths());
        assertEquals(3, payload.tier());
        assertEquals(100, payload.suspicionPercent());
        assertEquals(0, payload.lightMaximumTenths());
        assertEquals(100, payload.burdenedMaximumTenths());
        assertEquals(100, payload.heavyMaximumTenths());
        assertEquals(0, payload.burdenedPenaltyPercent());
        assertEquals(20, payload.heavyPenaltyPercent());
        assertEquals(95, payload.overloadedPenaltyPercent());
    }
}
