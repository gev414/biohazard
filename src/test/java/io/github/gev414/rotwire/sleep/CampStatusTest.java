package io.github.gev414.rotwire.sleep;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampStatusTest {

    @Test
    void campIsActiveOnlyWhenEveryRequirementIsReady() {
        CampStatus ready = new CampStatus(
                CampStatus.ShelterType.LARGE_TENT,
                BlockPos.ZERO,
                12,
                true,
                true,
                true,
                true,
                16
        );
        assertTrue(ready.active());

        assertFalse(new CampStatus(
                ready.shelter(),
                ready.center(),
                ready.radius(),
                ready.sleepingBagPresent(),
                false,
                ready.backpackPresent(),
                ready.rationReady(),
                ready.availableNutrition()
        ).active());
        assertFalse(new CampStatus(
                CampStatus.ShelterType.NONE,
                ready.center(),
                ready.radius(),
                true,
                true,
                true,
                true,
                ready.availableNutrition()
        ).active());
    }
}
