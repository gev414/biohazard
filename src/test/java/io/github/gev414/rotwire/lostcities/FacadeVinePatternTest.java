package io.github.gev414.rotwire.lostcities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacadeVinePatternTest {

    private static final int SAMPLE_COUNT = 1_000;
    private static final int FACADE_HEIGHT = 64;

    @Test
    void zeroChanceLeavesFacadeClear() {
        assertEquals(0.0D, averageCoverage(0.0F));
    }

    @Test
    void profileChanceProducesBrokenCoverageInsteadOfGreenWalls() {
        double coverage = averageCoverage(0.70F);
        assertTrue(
                coverage >= 0.24D && coverage <= 0.38D,
                () -> "70% profile chance produced " + coverage
        );
    }

    @Test
    void maximumChanceStillPreservesBuildingTexture() {
        double coverage = averageCoverage(1.0F);
        assertTrue(
                coverage < 0.55D,
                () -> "Maximum chance covered too much facade: " + coverage
        );
    }

    @Test
    void defaultLostCitiesChanceRemainsSubtle() {
        double coverage = averageCoverage(0.009F);
        assertTrue(
                coverage < 0.04D,
                () -> "Default chance was unexpectedly dense: " + coverage
        );
    }

    @Test
    void higherProfileChanceStillMeansMoreVines() {
        assertTrue(averageCoverage(0.30F) < averageCoverage(0.70F));
        assertTrue(averageCoverage(0.70F) < averageCoverage(1.0F));
    }

    private static double averageCoverage(float vineChance) {
        long vines = 0L;
        long cells = 0L;
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            boolean[][] pattern = FacadeVinePattern.create(
                    0x524F54574952454CL + sample * 31L,
                    vineChance,
                    FACADE_HEIGHT
            );
            for (boolean[] column : pattern) {
                for (boolean present : column) {
                    if (present) {
                        vines++;
                    }
                    cells++;
                }
            }
        }
        return cells == 0L ? 0.0D : (double) vines / cells;
    }
}
