package io.github.gev414.biohazard.client;

public final class HordeAtmosphereState {

    private static final Snapshot INACTIVE = new Snapshot(
            false,
            false,
            24_000,
            18_000
    );

    private static volatile Snapshot current = INACTIVE;

    public static Snapshot current() {
        return current;
    }

    public static void update(
            boolean hordeDay,
            boolean active,
            int dayLength,
            int hordeStartTime
    ) {
        current = new Snapshot(
                hordeDay,
                active,
                dayLength,
                hordeStartTime
        );
    }

    public static void reset() {
        current = INACTIVE;
    }

    public record Snapshot(
            boolean hordeDay,
            boolean active,
            int dayLength,
            int hordeStartTime
    ) {

        public Snapshot {
            dayLength = Math.max(1, dayLength);
            hordeStartTime = Math.max(
                    0,
                    Math.min(hordeStartTime, dayLength)
            );
        }
    }

    private HordeAtmosphereState() {
    }
}
