package io.github.gev414.rotwire.sleep;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampRationPlannerTest {

    @Test
    void requiresNutritionStrictlyAboveThreshold() {
        assertTrue(CampRationPlanner.select(
                List.of(new CampRationPlanner.FoodStack(0, 5, 1)),
                5
        ).isEmpty());
    }

    @Test
    void choosesSmallestQualifyingNutritionTotal() {
        CampRationPlanner.Selection selection =
                CampRationPlanner.select(
                        List.of(
                                new CampRationPlanner.FoodStack(0, 8, 1),
                                new CampRationPlanner.FoodStack(1, 3, 2)
                        ),
                        5
                ).orElseThrow();

        assertEquals(6, selection.nutrition());
        assertEquals(
                List.of(new CampRationPlanner.SlotAmount(1, 2)),
                selection.amounts()
        );
    }

    @Test
    void prefersFewerItemsWhenNutritionIsEqual() {
        CampRationPlanner.Selection selection =
                CampRationPlanner.select(
                        List.of(
                                new CampRationPlanner.FoodStack(0, 8, 1),
                                new CampRationPlanner.FoodStack(1, 3, 1),
                                new CampRationPlanner.FoodStack(2, 5, 1)
                        ),
                        5
                ).orElseThrow();

        assertEquals(
                List.of(new CampRationPlanner.SlotAmount(0, 1)),
                selection.amounts()
        );
    }

    @Test
    void canUseMultipleSlotsAndStackedItems() {
        CampRationPlanner.Selection selection =
                CampRationPlanner.select(
                        List.of(
                                new CampRationPlanner.FoodStack(2, 2, 2),
                                new CampRationPlanner.FoodStack(7, 1, 4)
                        ),
                        5
                ).orElseThrow();

        assertEquals(6, selection.nutrition());
        assertEquals(4, selection.amounts().stream()
                .mapToInt(CampRationPlanner.SlotAmount::amount)
                .sum());
        assertEquals(
                List.of(
                        new CampRationPlanner.SlotAmount(2, 2),
                        new CampRationPlanner.SlotAmount(7, 2)
                ),
                selection.amounts()
        );
    }

    @Test
    void emptyInventoryCannotPay() {
        assertTrue(CampRationPlanner.select(List.of(), 5).isEmpty());
    }
}
