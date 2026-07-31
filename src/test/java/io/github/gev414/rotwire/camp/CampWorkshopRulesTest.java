package io.github.gev414.rotwire.camp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CampWorkshopRulesTest {

    @Test
    void repairRestoresOneQuarterOfMaximumDurability() {
        assertEquals(50, CampWorkshopRules.repairedDamage(100, 200));
    }

    @Test
    void repairNeverCreatesNegativeDamage() {
        assertEquals(0, CampWorkshopRules.repairedDamage(20, 200));
    }

    @Test
    void lowDurabilityItemsStillRepairAtLeastOnePoint() {
        assertEquals(1, CampWorkshopRules.repairedDamage(2, 3));
    }
}
