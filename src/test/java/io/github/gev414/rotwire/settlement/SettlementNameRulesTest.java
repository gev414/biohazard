package io.github.gev414.rotwire.settlement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementNameRulesTest {

    @Test
    void trimsCollapsesAndBoundsSettlementNames() {
        assertEquals(
                "Northbridge Sector",
                SettlementNameRules.normalize("  Northbridge\n\tSector  ")
                        .orElseThrow()
        );
        assertTrue(SettlementNameRules.normalize(" \t\n ").isEmpty());

        String longName = "A".repeat(SettlementNameRules.MAX_LENGTH + 10);
        assertEquals(
                SettlementNameRules.MAX_LENGTH,
                SettlementNameRules.normalize(longName).orElseThrow().length()
        );
    }
}
