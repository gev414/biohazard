package io.github.gev414.rotwire.mob.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HostileIntentResolverTest {

    @Test
    void huntOverridesAssaultAndInvestigation() {
        assertEquals(
                HostileIntent.HUNT,
                HostileIntentResolver.resolve(true, true, true)
        );
    }

    @Test
    void assaultOverridesInvestigation() {
        assertEquals(
                HostileIntent.ASSAULT,
                HostileIntentResolver.resolve(false, true, true)
        );
    }

    @Test
    void investigationRunsOnlyWithoutCombatObjective() {
        assertEquals(
                HostileIntent.INVESTIGATE,
                HostileIntentResolver.resolve(false, false, true)
        );
        assertEquals(
                HostileIntent.IDLE,
                HostileIntentResolver.resolve(false, false, false)
        );
    }
}
