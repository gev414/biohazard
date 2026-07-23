package io.github.gev414.biohazard.client;

import java.util.Locale;

public final class RadioClock {

    public static String format(long dayTime) {
        long ticks = Math.floorMod(dayTime, 24_000L);
        int totalMinutes = (int) (
                (ticks * 1_440L / 24_000L + 360L) % 1_440L
        );
        return String.format(
                Locale.ROOT,
                "%02d:%02d",
                totalMinutes / 60,
                totalMinutes % 60
        );
    }

    private RadioClock() {
    }
}
