package io.github.gev414.rotwire.settlement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementRationRulesTest {

    @Test
    void dailyCostIsNonNegativeAndOverflowSafe() {
        assertEquals(0, SettlementRationRules.dailyCost(-1, 5));
        assertEquals(0, SettlementRationRules.dailyCost(5, -1));
        assertEquals(
                Integer.MAX_VALUE,
                SettlementRationRules.dailyCost(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )
        );
    }
}
