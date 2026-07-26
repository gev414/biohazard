package io.github.gev414.biohazard.sleep;

final class NightCycle {

    static final long DAY_LENGTH = 24_000L;
    static final long NIGHT_START = 13_000L;
    static final int FULL_NIGHT_TICKS =
            (int) (DAY_LENGTH - NIGHT_START);

    static boolean crossedNightStart(long previous, long current) {
        return current == previous + 1L
                && day(previous) == day(current)
                && timeOfDay(previous) < NIGHT_START
                && timeOfDay(current) >= NIGHT_START;
    }

    static boolean crossedDawn(long previous, long current) {
        return current == previous + 1L
                && day(current) == day(previous) + 1L
                && timeOfDay(current) == 0L;
    }

    static boolean isNight(long dayTime) {
        return timeOfDay(dayTime) >= NIGHT_START;
    }

    static long day(long dayTime) {
        return Math.floorDiv(dayTime, DAY_LENGTH);
    }

    private static long timeOfDay(long dayTime) {
        return Math.floorMod(dayTime, DAY_LENGTH);
    }

    private NightCycle() {
    }
}
