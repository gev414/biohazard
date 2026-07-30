package io.github.gev414.rotwire.client;

final class ScopeFovMath {

    private static final double MIN_FACTOR = 0.01D;

    static double remap(
            double currentFov,
            float currentZoom,
            float targetZoom,
            float aimingProgress,
            float targetBlend
    ) {
        double progress = clamp(aimingProgress, 0.0D, 1.0D);
        double blend = clamp(targetBlend, 0.0D, 1.0D);
        double clampedCurrentZoom = clamp(currentZoom, 0.0D, 0.99D);
        double clampedTargetZoom = clamp(targetZoom, 0.0D, 0.99D);
        double effectiveTargetZoom = clampedCurrentZoom
                + (clampedTargetZoom - clampedCurrentZoom) * blend;
        double currentFactor = Math.max(
                MIN_FACTOR,
                1.0D - clampedCurrentZoom * progress
        );
        double targetFactor = Math.max(
                MIN_FACTOR,
                1.0D - effectiveTargetZoom * progress
        );
        return currentFov * targetFactor / currentFactor;
    }

    static float delayedProgress(float progress, float start) {
        double clampedStart = clamp(start, 0.0D, 0.99D);
        double linear = clamp(
                (progress - clampedStart) / (1.0D - clampedStart),
                0.0D,
                1.0D
        );
        return (float) (linear * linear * (3.0D - 2.0D * linear));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private ScopeFovMath() {
    }
}
