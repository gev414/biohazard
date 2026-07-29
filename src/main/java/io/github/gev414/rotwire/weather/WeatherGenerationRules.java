package io.github.gev414.rotwire.weather;

public record WeatherGenerationRules(
        int hazardousStartDay,
        int clearWeight,
        int rainWeight,
        int stormWeight,
        int contaminatedRainWeight,
        int contaminatedStormWeight,
        int normalMinimumDuration,
        int normalMaximumDuration,
        int hazardousMinimumDuration,
        int hazardousMaximumDuration
) {

    public WeatherGenerationRules {
        hazardousStartDay = Math.max(0, hazardousStartDay);
        clearWeight = Math.max(0, clearWeight);
        rainWeight = Math.max(0, rainWeight);
        stormWeight = Math.max(0, stormWeight);
        contaminatedRainWeight = Math.max(
                0,
                contaminatedRainWeight
        );
        contaminatedStormWeight = Math.max(
                0,
                contaminatedStormWeight
        );
        normalMinimumDuration = boundedDuration(
                normalMinimumDuration
        );
        normalMaximumDuration = Math.max(
                normalMinimumDuration,
                boundedDuration(normalMaximumDuration)
        );
        hazardousMinimumDuration = boundedDuration(
                hazardousMinimumDuration
        );
        hazardousMaximumDuration = Math.max(
                hazardousMinimumDuration,
                boundedDuration(hazardousMaximumDuration)
        );
    }

    public static WeatherGenerationRules defaults() {
        return new WeatherGenerationRules(
                5,
                40,
                30,
                15,
                10,
                5,
                6_000,
                12_000,
                4_000,
                7_000
        );
    }

    private static int boundedDuration(int value) {
        return Math.max(200, Math.min(value, 23_000));
    }
}
