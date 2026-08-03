package io.github.gev414.rotwire.settlement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivorSafetyRulesTest {

    @Test
    void activeSiegeAlwaysOrdersARetreat() {
        assertTrue(SurvivorSafetyRules.mustReturnToCamp(
                new SurvivorSafetyRules.SafetyContext(
                        SettlementSiegeState.ACTIVE,
                        false,
                        0,
                        false,
                        0,
                        0
                )
        ));
    }

    @Test
    void emptyRangedLoadoutAndOutnumberedMeleeBothRetreat() {
        assertTrue(SurvivorSafetyRules.mustReturnToCamp(
                new SurvivorSafetyRules.SafetyContext(
                        SettlementSiegeState.CALM,
                        true,
                        0,
                        false,
                        0,
                        0
                )
        ));
        assertTrue(SurvivorSafetyRules.mustReturnToCamp(
                new SurvivorSafetyRules.SafetyContext(
                        SettlementSiegeState.CALM,
                        false,
                        0,
                        true,
                        3,
                        1
                )
        ));
    }

    @Test
    void calmArmedSurvivorDoesNotRetreatWithoutAMeleeDisadvantage() {
        assertFalse(SurvivorSafetyRules.mustReturnToCamp(
                new SurvivorSafetyRules.SafetyContext(
                        SettlementSiegeState.CALM,
                        true,
                        8,
                        true,
                        1,
                        1
                )
        ));
    }
}
