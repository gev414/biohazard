package io.github.gev414.rotwire.weather;

import net.minecraft.nbt.CompoundTag;

record WeatherOverride(
        ScheduledWeather weather,
        long startedAtGameTime,
        long expiresAtGameTime
) {

    WeatherOverride {
        weather = weather == null ? ScheduledWeather.CLEAR : weather;
        long minimumExpiry = startedAtGameTime == Long.MAX_VALUE
                ? Long.MAX_VALUE
                : startedAtGameTime + 1L;
        expiresAtGameTime = Math.max(
                minimumExpiry,
                expiresAtGameTime
        );
    }

    static WeatherOverride create(
            ScheduledWeather weather,
            long gameTime,
            int durationTicks
    ) {
        long duration = Math.max(1L, durationTicks);
        long expiresAt = gameTime > Long.MAX_VALUE - duration
                ? Long.MAX_VALUE
                : gameTime + duration;
        return new WeatherOverride(
                weather,
                gameTime,
                expiresAt
        );
    }

    boolean active(long gameTime) {
        return gameTime < expiresAtGameTime;
    }

    int remainingTicks(long gameTime) {
        long remaining = Math.max(
                0L,
                expiresAtGameTime - gameTime
        );
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("weather", weather.serializedName());
        tag.putLong("started_at", startedAtGameTime);
        tag.putLong("expires_at", expiresAtGameTime);
        return tag;
    }

    static WeatherOverride load(CompoundTag tag) {
        return new WeatherOverride(
                ScheduledWeather.fromName(tag.getString("weather")),
                tag.getLong("started_at"),
                tag.getLong("expires_at")
        );
    }
}
