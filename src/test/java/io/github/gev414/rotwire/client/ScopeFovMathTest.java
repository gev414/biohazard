package io.github.gev414.rotwire.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScopeFovMathTest {

    @Test
    void leavesFovUnchangedBeforeAiming() {
        assertEquals(
                70.0D,
                ScopeFovMath.remap(
                        70.0D,
                        0.55F,
                        0.95F,
                        0.0F,
                        0.0F
                ),
                0.0001D
        );
    }

    @Test
    void replacesAttachmentAdsZoomWithPipMagnification() {
        double pointBlankFov = 70.0D * (1.0D - 0.55D);

        assertEquals(
                70.0D * (1.0D - 0.95D),
                ScopeFovMath.remap(
                        pointBlankFov,
                        0.55F,
                        0.95F,
                        1.0F,
                        1.0F
                ),
                0.0001D
        );
    }

    @Test
    void leavesPointBlankZoomAloneBeforeDelayedTransition() {
        double pointBlankFov = 70.0D * (1.0D - 0.55D * 0.7D);

        assertEquals(
                pointBlankFov,
                ScopeFovMath.remap(
                        pointBlankFov,
                        0.55F,
                        0.95F,
                        0.7F,
                        0.0F
                ),
                0.0001D
        );
    }

    @Test
    void delayedProgressStartsAtConfiguredPoint() {
        assertEquals(
                0.0F,
                ScopeFovMath.delayedProgress(0.7F, 0.7F),
                0.0001F
        );
        assertEquals(
                0.5F,
                ScopeFovMath.delayedProgress(0.85F, 0.7F),
                0.0001F
        );
        assertEquals(
                1.0F,
                ScopeFovMath.delayedProgress(1.0F, 0.7F),
                0.0001F
        );
    }
}
