package io.github.gev414.biohazard.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RadioClockTest {

    @Test
    void convertsMinecraftDayTimeToTwentyFourHourClock() {
        assertEquals("06:00", RadioClock.format(0L));
        assertEquals("12:00", RadioClock.format(6_000L));
        assertEquals("18:00", RadioClock.format(12_000L));
        assertEquals("00:00", RadioClock.format(18_000L));
        assertEquals("05:59", RadioClock.format(23_999L));
    }

    @Test
    void wrapsAcrossDays() {
        assertEquals("06:00", RadioClock.format(24_000L));
        assertEquals("00:00", RadioClock.format(-6_000L));
    }
}
