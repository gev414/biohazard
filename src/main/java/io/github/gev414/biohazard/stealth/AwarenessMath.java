package io.github.gev414.biohazard.stealth;

import net.minecraft.util.Mth;

public final class AwarenessMath {

    public static boolean insideFieldOfView(
            double lookX,
            double lookY,
            double lookZ,
            double targetX,
            double targetY,
            double targetZ,
            double fieldOfViewDegrees
    ) {
        double lookLength = Math.sqrt(
                lookX * lookX + lookY * lookY + lookZ * lookZ
        );
        double targetLength = Math.sqrt(
                targetX * targetX
                        + targetY * targetY
                        + targetZ * targetZ
        );
        if (lookLength < 1.0E-6D || targetLength < 1.0E-6D) {
            return true;
        }
        double dot = (
                lookX * targetX
                        + lookY * targetY
                        + lookZ * targetZ
        ) / (lookLength * targetLength);
        double halfAngle = Math.toRadians(
                Mth.clamp(fieldOfViewDegrees, 1.0D, 360.0D) * 0.5D
        );
        return dot >= Math.cos(halfAngle);
    }

    public static double gain(
            double suspicion,
            double gainPerSecond,
            double elapsedSeconds,
            double distance,
            double maximumDistance,
            double multiplier
    ) {
        double distanceFraction = Mth.clamp(
                distance / Math.max(0.001D, maximumDistance),
                0.0D,
                1.0D
        );
        double distanceFactor = 1.0D - 0.65D * distanceFraction;
        return Mth.clamp(
                suspicion
                        + gainPerSecond
                        * Math.max(0.0D, elapsedSeconds)
                        * distanceFactor
                        * Math.max(0.0D, multiplier),
                0.0D,
                100.0D
        );
    }

    public static double decay(
            double suspicion,
            double decayPerSecond,
            double elapsedSeconds
    ) {
        return Mth.clamp(
                suspicion
                        - decayPerSecond
                        * Math.max(0.0D, elapsedSeconds),
                0.0D,
                100.0D
        );
    }

    private AwarenessMath() {
    }
}
