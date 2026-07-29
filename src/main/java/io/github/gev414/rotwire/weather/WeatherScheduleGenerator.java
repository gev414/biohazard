package io.github.gev414.rotwire.weather;

import java.util.Random;

public final class WeatherScheduleGenerator {

    private static final int EARLIEST_EVENT_TICK = 2_000;
    private static final int LATEST_EVENT_END_TICK = 23_000;

    public static WeatherDayPlan generate(
            long worldSeed,
            long day,
            WeatherSeason season,
            ScheduledWeather previousWeather,
            boolean hordeDay,
            WeatherGenerationRules rules
    ) {
        Random random = new Random(mixSeed(worldSeed, day));
        int[] weights = seasonalWeights(
                season == null ? WeatherSeason.TEMPERATE : season,
                rules
        );
        ScheduledWeather weather = select(random, weights);

        if (day < rules.hazardousStartDay()
                || previousWeather != null
                && previousWeather.contaminated()) {
            weather = weather.ordinaryEquivalent();
        }
        if (hordeDay && weather == ScheduledWeather.CONTAMINATED_STORM) {
            weather = ScheduledWeather.STORM;
        }
        if (weather == ScheduledWeather.CLEAR) {
            return new WeatherDayPlan(day, weather, 0, 0);
        }

        int minimum = weather.contaminated()
                ? rules.hazardousMinimumDuration()
                : rules.normalMinimumDuration();
        int maximum = weather.contaminated()
                ? rules.hazardousMaximumDuration()
                : rules.normalMaximumDuration();
        int duration = between(random, minimum, maximum);
        int latestStart = Math.max(
                EARLIEST_EVENT_TICK,
                LATEST_EVENT_END_TICK - duration
        );
        int start = between(
                random,
                EARLIEST_EVENT_TICK,
                latestStart
        );
        return new WeatherDayPlan(
                day,
                weather,
                start,
                Math.min(LATEST_EVENT_END_TICK, start + duration)
        );
    }

    private static int[] seasonalWeights(
            WeatherSeason season,
            WeatherGenerationRules rules
    ) {
        int clear = rules.clearWeight();
        int rain = rules.rainWeight();
        int storm = rules.stormWeight();
        int contaminatedRain = rules.contaminatedRainWeight();
        int contaminatedStorm = rules.contaminatedStormWeight();

        return switch (season) {
            case SPRING -> new int[]{
                    scale(clear, 0.88D),
                    scale(rain, 1.20D),
                    storm,
                    contaminatedRain,
                    contaminatedStorm
            };
            case SUMMER -> new int[]{
                    scale(clear, 1.12D),
                    scale(rain, 0.80D),
                    scale(storm, 1.25D),
                    scale(contaminatedRain, 0.80D),
                    scale(contaminatedStorm, 1.20D)
            };
            case AUTUMN -> new int[]{
                    scale(clear, 0.88D),
                    scale(rain, 1.30D),
                    scale(storm, 0.80D),
                    scale(contaminatedRain, 1.20D),
                    scale(contaminatedStorm, 0.60D)
            };
            case WINTER -> new int[]{
                    clear,
                    scale(rain, 1.50D),
                    0,
                    contaminatedRain,
                    0
            };
            default -> new int[]{
                    clear,
                    rain,
                    storm,
                    contaminatedRain,
                    contaminatedStorm
            };
        };
    }

    private static ScheduledWeather select(
            Random random,
            int[] weights
    ) {
        int total = 0;
        for (int weight : weights) {
            total += Math.max(0, weight);
        }
        if (total <= 0) {
            return ScheduledWeather.CLEAR;
        }

        int roll = random.nextInt(total);
        ScheduledWeather[] values = ScheduledWeather.values();
        for (int index = 0; index < values.length; index++) {
            roll -= Math.max(0, weights[index]);
            if (roll < 0) {
                return values[index];
            }
        }
        return ScheduledWeather.CLEAR;
    }

    private static int between(Random random, int minimum, int maximum) {
        int low = Math.min(minimum, maximum);
        int high = Math.max(minimum, maximum);
        return low + (high == low ? 0 : random.nextInt(high - low + 1));
    }

    private static int scale(int value, double multiplier) {
        return Math.max(0, (int) Math.round(value * multiplier));
    }

    private static long mixSeed(long seed, long day) {
        long value = seed ^ (day * 0x9E3779B97F4A7C15L);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private WeatherScheduleGenerator() {
    }
}
