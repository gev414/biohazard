package io.github.gev414.biohazard.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfectionMedicineItemTest {

    @Test
    void suppressantAddsFiveMinutesToCurrentStage() {
        int oneMinute = 60 * InfectionMedicineItem.TICKS_PER_SECOND;

        assertEquals(
                6 * 60 * InfectionMedicineItem.TICKS_PER_SECOND,
                InfectionMedicineItem.extendedDuration(oneMinute)
        );
    }

    @Test
    void suppressantCapsStageAtTenMinutes() {
        int nineMinutes = 9 * 60 * InfectionMedicineItem.TICKS_PER_SECOND;

        assertEquals(
                InfectionMedicineItem.SUPPRESSANT_MAX_REMAINING_TICKS,
                InfectionMedicineItem.extendedDuration(nineMinutes)
        );
    }
}
