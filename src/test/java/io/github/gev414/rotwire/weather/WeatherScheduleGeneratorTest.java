package io.github.gev414.rotwire.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WeatherScheduleGeneratorTest {

    @Test
    void hazardousWeatherIsDowngradedDuringOpeningDays() {
        WeatherDayPlan plan = WeatherScheduleGenerator.generate(
                42L,
                4L,
                WeatherSeason.TEMPERATE,
                ScheduledWeather.CLEAR,
                false,
                only(ScheduledWeather.CONTAMINATED_RAIN)
        );

        assertEquals(ScheduledWeather.RAIN, plan.weather());
    }

    @Test
    void hazardousWeatherCannotRepeatOnConsecutiveDays() {
        WeatherDayPlan plan = WeatherScheduleGenerator.generate(
                42L,
                12L,
                WeatherSeason.TEMPERATE,
                ScheduledWeather.CONTAMINATED_STORM,
                false,
                only(ScheduledWeather.CONTAMINATED_RAIN)
        );

        assertEquals(ScheduledWeather.RAIN, plan.weather());
    }

    @Test
    void contaminatedStormIsDowngradedOnHordeDay() {
        WeatherDayPlan plan = WeatherScheduleGenerator.generate(
                42L,
                15L,
                WeatherSeason.SUMMER,
                ScheduledWeather.CLEAR,
                true,
                only(ScheduledWeather.CONTAMINATED_STORM)
        );

        assertEquals(ScheduledWeather.STORM, plan.weather());
    }

    @Test
    void winterSuppressesThunderstorms() {
        WeatherDayPlan plan = WeatherScheduleGenerator.generate(
                42L,
                20L,
                WeatherSeason.WINTER,
                ScheduledWeather.CLEAR,
                false,
                only(ScheduledWeather.STORM)
        );

        assertEquals(ScheduledWeather.CLEAR, plan.weather());
    }

    @Test
    void precipitationWindowFitsInsideTheDay() {
        WeatherDayPlan plan = WeatherScheduleGenerator.generate(
                91L,
                10L,
                WeatherSeason.SPRING,
                ScheduledWeather.CLEAR,
                false,
                only(ScheduledWeather.RAIN)
        );

        assertTrue(plan.startTick() >= 2_000);
        assertTrue(plan.endTick() <= 23_000);
        assertTrue(plan.endTick() > plan.startTick());
    }

    @Test
    void clearPlanHasNoEventWindow() {
        WeatherDayPlan plan = WeatherScheduleGenerator.generate(
                91L,
                10L,
                WeatherSeason.TEMPERATE,
                ScheduledWeather.CLEAR,
                false,
                only(ScheduledWeather.CLEAR)
        );

        assertEquals(ScheduledWeather.CLEAR, plan.weather());
        assertEquals(0, plan.startTick());
        assertEquals(0, plan.endTick());
        assertFalse(plan.weather().precipitation());
    }

    private static WeatherGenerationRules only(
            ScheduledWeather weather
    ) {
        return new WeatherGenerationRules(
                5,
                weather == ScheduledWeather.CLEAR ? 1 : 0,
                weather == ScheduledWeather.RAIN ? 1 : 0,
                weather == ScheduledWeather.STORM ? 1 : 0,
                weather == ScheduledWeather.CONTAMINATED_RAIN
                        ? 1
                        : 0,
                weather == ScheduledWeather.CONTAMINATED_STORM
                        ? 1
                        : 0,
                6_000,
                12_000,
                4_000,
                7_000
        );
    }
}
