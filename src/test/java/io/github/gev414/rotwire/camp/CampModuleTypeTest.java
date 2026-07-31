package io.github.gev414.rotwire.camp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CampModuleTypeTest {

    @Test
    void everyModuleUsesADistinctMask() {
        assertNotEquals(
                CampModuleType.STORAGE.mask(),
                CampModuleType.CRAFTING.mask()
        );
        assertNotEquals(
                CampModuleType.CRAFTING.mask(),
                CampModuleType.OPERATIONS.mask()
        );
        assertNotEquals(
                CampModuleType.STORAGE.mask(),
                CampModuleType.OPERATIONS.mask()
        );
    }

    @Test
    void persistedMaskRejectsUnknownFutureBits() {
        int installed = CampModuleType.STORAGE.mask()
                | CampModuleType.OPERATIONS.mask()
                | (1 << 20);

        assertEquals(
                CampModuleType.STORAGE.mask()
                        | CampModuleType.OPERATIONS.mask(),
                CampModuleType.sanitizeMask(installed)
        );
    }
}
