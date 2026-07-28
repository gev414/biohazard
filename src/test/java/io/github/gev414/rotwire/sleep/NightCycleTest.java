package io.github.gev414.rotwire.sleep;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NightCycleTest {

    @Test
    void recognizesNaturalNightStart() {
        assertTrue(NightCycle.crossedNightStart(12_999L, 13_000L));
        assertTrue(NightCycle.isNight(13_000L));
    }

    @Test
    void rejectsTimeCommandsAndMidNightInitialization() {
        assertFalse(NightCycle.crossedNightStart(12_000L, 13_000L));
        assertFalse(NightCycle.crossedNightStart(13_000L, 13_001L));
    }

    @Test
    void recognizesNaturalDawnOnly() {
        assertTrue(NightCycle.crossedDawn(23_999L, 24_000L));
        assertFalse(NightCycle.crossedDawn(13_000L, 24_000L));
        assertFalse(NightCycle.crossedDawn(24_000L, 23_999L));
    }
}
