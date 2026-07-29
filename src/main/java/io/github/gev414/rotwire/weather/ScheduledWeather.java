package io.github.gev414.rotwire.weather;

import java.util.Locale;

public enum ScheduledWeather {
    CLEAR,
    RAIN,
    STORM,
    CONTAMINATED_RAIN,
    CONTAMINATED_STORM;

    public boolean precipitation() {
        return this != CLEAR;
    }

    public boolean storm() {
        return this == STORM || this == CONTAMINATED_STORM;
    }

    public boolean contaminated() {
        return this == CONTAMINATED_RAIN
                || this == CONTAMINATED_STORM;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ScheduledWeather ordinaryEquivalent() {
        return switch (this) {
            case CONTAMINATED_RAIN -> RAIN;
            case CONTAMINATED_STORM -> STORM;
            default -> this;
        };
    }

    public static ScheduledWeather fromName(String name) {
        if (name == null) {
            return CLEAR;
        }
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CLEAR;
        }
    }

    public static ScheduledWeather fromNetwork(int ordinal) {
        ScheduledWeather[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CLEAR;
        }
        return values[ordinal];
    }
}
