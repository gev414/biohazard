package io.github.gev414.rotwire.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WeatherDayPlanTest {

    @Test
    void weatherIsClearBeforeAndAfterScheduledWindow() {
        WeatherDayPlan plan = new WeatherDayPlan(
                3L,
                ScheduledWeather.CONTAMINATED_RAIN,
                4_000,
                9_000
        );
        long dayStart = 3L * WeatherDayPlan.DAY_LENGTH;

        assertEquals(
                ScheduledWeather.CLEAR,
                plan.weatherAt(dayStart + 3_999)
        );
        assertEquals(
                ScheduledWeather.CONTAMINATED_RAIN,
                plan.weatherAt(dayStart + 4_000)
        );
        assertEquals(
                ScheduledWeather.CLEAR,
                plan.weatherAt(dayStart + 9_000)
        );
    }

    @Test
    void planDoesNotLeakIntoAnotherDay() {
        WeatherDayPlan plan = new WeatherDayPlan(
                3L,
                ScheduledWeather.STORM,
                4_000,
                9_000
        );

        assertEquals(
                ScheduledWeather.CLEAR,
                plan.weatherAt(4L * WeatherDayPlan.DAY_LENGTH + 5_000)
        );
    }
}
