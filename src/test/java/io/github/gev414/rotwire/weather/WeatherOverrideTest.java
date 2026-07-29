package io.github.gev414.rotwire.weather;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherOverrideTest {

    @Test
    void expiresAgainstMonotonicGameTime() {
        WeatherOverride override = WeatherOverride.create(
                ScheduledWeather.CONTAMINATED_STORM,
                1_000L,
                200
        );

        assertEquals(200, override.remainingTicks(1_000L));
        assertTrue(override.active(1_199L));
        assertEquals(1, override.remainingTicks(1_199L));
        assertFalse(override.active(1_200L));
        assertEquals(0, override.remainingTicks(1_200L));
    }

    @Test
    void persistsConditionAndDeadline() {
        WeatherOverride original = WeatherOverride.create(
                ScheduledWeather.CONTAMINATED_RAIN,
                4_500L,
                12_000
        );
        CompoundTag tag = original.save();

        assertEquals(original, WeatherOverride.load(tag));
    }

    @Test
    void clampsNonPositiveDurationToOneTick() {
        WeatherOverride override = WeatherOverride.create(
                ScheduledWeather.STORM,
                20L,
                0
        );

        assertEquals(1, override.remainingTicks(20L));
        assertFalse(override.active(21L));
    }

    @Test
    void saturatesDeadlineInsteadOfOverflowing() {
        WeatherOverride override = WeatherOverride.create(
                ScheduledWeather.RAIN,
                Long.MAX_VALUE - 5L,
                20
        );

        assertEquals(Long.MAX_VALUE, override.expiresAtGameTime());
        assertTrue(override.active(Long.MAX_VALUE - 1L));
    }
}
