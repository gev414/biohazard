package io.github.gev414.rotwire.weather;

import net.minecraft.nbt.CompoundTag;

public record WeatherDayPlan(
        long day,
        ScheduledWeather weather,
        int startTick,
        int endTick
) {

    public static final int DAY_LENGTH = 24_000;

    public WeatherDayPlan {
        weather = weather == null ? ScheduledWeather.CLEAR : weather;
        startTick = clampTick(startTick);
        endTick = Math.max(startTick, clampTick(endTick));
        if (weather == ScheduledWeather.CLEAR) {
            startTick = 0;
            endTick = 0;
        }
    }

    public ScheduledWeather weatherAt(long dayTime) {
        long currentDay = Math.floorDiv(dayTime, DAY_LENGTH);
        int tick = (int) Math.floorMod(dayTime, DAY_LENGTH);
        if (currentDay != day
                || weather == ScheduledWeather.CLEAR
                || tick < startTick
                || tick >= endTick) {
            return ScheduledWeather.CLEAR;
        }
        return weather;
    }

    public int ticksUntilTransition(long dayTime) {
        int tick = (int) Math.floorMod(dayTime, DAY_LENGTH);
        if (weather == ScheduledWeather.CLEAR) {
            return Math.max(1, DAY_LENGTH - tick);
        }
        if (tick < startTick) {
            return Math.max(1, startTick - tick);
        }
        if (tick < endTick) {
            return Math.max(1, endTick - tick);
        }
        return Math.max(1, DAY_LENGTH - tick);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("day", day);
        tag.putString("weather", weather.serializedName());
        tag.putInt("start", startTick);
        tag.putInt("end", endTick);
        return tag;
    }

    public static WeatherDayPlan load(CompoundTag tag) {
        return new WeatherDayPlan(
                tag.getLong("day"),
                ScheduledWeather.fromName(tag.getString("weather")),
                tag.getInt("start"),
                tag.getInt("end")
        );
    }

    private static int clampTick(int value) {
        return Math.max(0, Math.min(value, DAY_LENGTH));
    }
}
