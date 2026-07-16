package io.github.gev414.biohazard.client;

public final class HordeAtmosphereFog {

    public static float strength(
            boolean hordeDay,
            boolean active,
            long dayTime,
            int dayLength,
            int hordeStartTime,
            int fadeDurationTicks
    ) {
        if (!hordeDay) {
            return 0.0F;
        }
        if (active) {
            return 1.0F;
        }

        int safeDayLength = Math.max(1, dayLength);
        int eventStart = Math.max(
                0,
                Math.min(hordeStartTime, safeDayLength)
        );
        int fadeDuration = Math.max(
                0,
                Math.min(fadeDurationTicks, eventStart)
        );
        int fadeStart = eventStart - fadeDuration;
        long timeOfDay = Math.floorMod(dayTime, (long) safeDayLength);

        if (timeOfDay < fadeStart || timeOfDay >= eventStart) {
            return 0.0F;
        }
        if (fadeDuration == 0) {
            return 0.0F;
        }

        float progress = (float) (timeOfDay - fadeStart)
                / (float) fadeDuration;
        return progress * progress * (3.0F - 2.0F * progress);
    }

    public static float blendTowardCloserPlane(
            float currentDistance,
            float targetDistance,
            float strength
    ) {
        float boundedStrength = Math.max(0.0F, Math.min(strength, 1.0F));
        float closerTarget = Math.min(currentDistance, targetDistance);
        return currentDistance
                + (closerTarget - currentDistance) * boundedStrength;
    }

    private HordeAtmosphereFog() {
    }
}
