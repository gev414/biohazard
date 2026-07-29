package io.github.gev414.rotwire.weather;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.config.WeatherConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongPredicate;

final class WeatherScheduleSavedData extends SavedData {

    private static final String FILE_NAME = "rotwire_weather_schedule";
    private static final Factory<WeatherScheduleSavedData> FACTORY =
            new Factory<>(
                    WeatherScheduleSavedData::new,
                    WeatherScheduleSavedData::load,
                    DataFixTypes.LEVEL
            );

    private final Map<Long, WeatherDayPlan> plans =
            new LinkedHashMap<>();
    private WeatherOverride forcedWeather;

    static WeatherScheduleSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, FILE_NAME);
    }

    Forecast forecast(
            long day,
            long worldSeed,
            WeatherSeason season,
            LongPredicate hordeDay
    ) {
        WeatherGenerationRules rules =
                WeatherConfig.generationRules();
        WeatherDayPlan previous = ensure(
                day - 1L,
                worldSeed,
                season,
                null,
                hordeDay.test(day - 1L),
                rules
        );
        WeatherDayPlan today = ensure(
                day,
                worldSeed,
                season,
                previous.weather(),
                hordeDay.test(day),
                rules
        );
        WeatherDayPlan tomorrow = ensure(
                day + 1L,
                worldSeed,
                season,
                today.weather(),
                hordeDay.test(day + 1L),
                rules
        );
        prune(day);
        return new Forecast(today, tomorrow, season);
    }

    WeatherOverride forcedWeather(long gameTime) {
        if (forcedWeather != null
                && !forcedWeather.active(gameTime)) {
            forcedWeather = null;
            setDirty();
        }
        return forcedWeather;
    }

    void forceWeather(
            ScheduledWeather weather,
            long gameTime,
            int durationTicks
    ) {
        forcedWeather = WeatherOverride.create(
                weather,
                gameTime,
                durationTicks
        );
        setDirty();
    }

    boolean clearForcedWeather() {
        if (forcedWeather == null) {
            return false;
        }
        forcedWeather = null;
        setDirty();
        return true;
    }

    private WeatherDayPlan ensure(
            long day,
            long worldSeed,
            WeatherSeason season,
            ScheduledWeather previous,
            boolean hordeDay,
            WeatherGenerationRules rules
    ) {
        WeatherDayPlan existing = plans.get(day);
        if (existing != null) {
            return existing;
        }
        WeatherDayPlan generated = WeatherScheduleGenerator.generate(
                worldSeed,
                day,
                season,
                previous,
                hordeDay,
                rules
        );
        plans.put(day, generated);
        setDirty();
        return generated;
    }

    private void prune(long currentDay) {
        boolean removed = plans.keySet().removeIf(
                day -> day < currentDay - 2L
                        || day > currentDay + 3L
        );
        if (removed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag entries = new ListTag();
        for (WeatherDayPlan plan : plans.values()) {
            entries.add(plan.save());
        }
        tag.put("plans", entries);
        if (forcedWeather != null) {
            tag.put("forced_weather", forcedWeather.save());
        }
        return tag;
    }

    private static WeatherScheduleSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        WeatherScheduleSavedData data =
                new WeatherScheduleSavedData();
        ListTag entries = tag.getList("plans", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            try {
                WeatherDayPlan plan = WeatherDayPlan.load(
                        entries.getCompound(index)
                );
                data.plans.put(plan.day(), plan);
            } catch (RuntimeException exception) {
                Rotwire.LOGGER.error(
                        "Skipping malformed weather plan at index {}",
                        index,
                        exception
                );
            }
        }
        if (tag.contains("forced_weather", Tag.TAG_COMPOUND)) {
            try {
                data.forcedWeather = WeatherOverride.load(
                        tag.getCompound("forced_weather")
                );
            } catch (RuntimeException exception) {
                Rotwire.LOGGER.error(
                        "Skipping malformed forced weather override",
                        exception
                );
            }
        }
        return data;
    }

    record Forecast(
            WeatherDayPlan today,
            WeatherDayPlan tomorrow,
            WeatherSeason season
    ) {
    }
}
