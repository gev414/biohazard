package io.github.gev414.rotwire.weather;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.config.WeatherConfig;
import io.github.gev414.rotwire.network.WeatherForecastPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.smileycorp.hordes.config.HordeEventConfig;

public final class WeatherManager {

    private static final int WEATHER_REFRESH_TICKS = 5;

    private static int ticksUntilRefresh;

    public static void onServerTick(ServerTickEvent.Post event) {
        WeatherExposureManager.onServerTick(event);
        if (!WeatherConfig.ENABLED.get()) {
            return;
        }
        if (ticksUntilRefresh > 0) {
            ticksUntilRefresh--;
            return;
        }
        ticksUntilRefresh = WEATHER_REFRESH_TICKS - 1;

        ServerLevel level = event.getServer().overworld();
        WeatherScheduleSavedData.Forecast forecast = forecast(level);
        apply(level, forecast.today(), forced(level));
    }

    public static ScheduledWeather current(ServerLevel level) {
        if (!WeatherConfig.ENABLED.get()
                || level.dimension() != Level.OVERWORLD) {
            if (level.isThundering()) {
                return ScheduledWeather.STORM;
            }
            if (level.isRaining()) {
                return ScheduledWeather.RAIN;
            }
            return ScheduledWeather.CLEAR;
        }
        WeatherOverride forced = forced(level);
        if (forced != null) {
            return forced.weather();
        }
        return forecast(level).today().weatherAt(level.getDayTime());
    }

    public static void sendForecast(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD
                || !WeatherConfig.ENABLED.get()) {
            PacketDistributor.sendToPlayer(
                    player,
                    WeatherForecastPayload.unavailable()
            );
            return;
        }
        WeatherScheduleSavedData.Forecast forecast =
                forecast(player.serverLevel());
        WeatherOverride forced = forced(player.serverLevel());
        PacketDistributor.sendToPlayer(
                player,
                WeatherForecastPayload.from(
                        forecast.today(),
                        forecast.tomorrow(),
                        forecast.season(),
                        forced == null ? null : forced.weather(),
                        forced == null
                                ? 0L
                                : forced.expiresAtGameTime()
                )
        );
    }

    static void force(
            ServerLevel level,
            ScheduledWeather weather,
            int durationTicks
    ) {
        WeatherScheduleSavedData data =
                WeatherScheduleSavedData.get(level.getServer());
        data.forceWeather(
                weather,
                level.getGameTime(),
                durationTicks
        );
        ticksUntilRefresh = 0;
        apply(level, forecast(level).today(), forced(level));
    }

    static boolean clearForced(ServerLevel level) {
        boolean cleared = WeatherScheduleSavedData
                .get(level.getServer())
                .clearForcedWeather();
        if (cleared) {
            ticksUntilRefresh = 0;
            apply(level, forecast(level).today(), null);
        }
        return cleared;
    }

    static WeatherOverride forced(ServerLevel level) {
        return WeatherScheduleSavedData
                .get(level.getServer())
                .forcedWeather(level.getGameTime());
    }

    public static double suspicionMultiplier(ServerLevel level) {
        if (!WeatherConfig.ENABLED.get()) {
            return 1.0D;
        }
        if (level.isThundering()) {
            return WeatherConfig.STORM_SUSPICION_MULTIPLIER.get();
        }
        if (level.isRaining()) {
            return WeatherConfig.RAIN_SUSPICION_MULTIPLIER.get();
        }
        return 1.0D;
    }

    public static double attentionMultiplier(ServerLevel level) {
        if (!WeatherConfig.ENABLED.get()) {
            return 1.0D;
        }
        if (level.isThundering()) {
            return WeatherConfig.STORM_ATTENTION_MULTIPLIER.get();
        }
        if (level.isRaining()) {
            return WeatherConfig.RAIN_ATTENTION_MULTIPLIER.get();
        }
        return 1.0D;
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        ticksUntilRefresh = 0;
        WeatherExposureManager.clear();
    }

    private static WeatherScheduleSavedData.Forecast forecast(
            ServerLevel level
    ) {
        long day = Math.floorDiv(
                level.getDayTime(),
                WeatherDayPlan.DAY_LENGTH
        );
        return WeatherScheduleSavedData
                .get(level.getServer())
                .forecast(
                        day,
                        level.getSeed(),
                        season(level),
                        WeatherManager::isHordeDay
                );
    }

    private static WeatherSeason season(ServerLevel level) {
        if (!WeatherConfig.SEASONAL_WEIGHTING.get()
                || !ModList.get().isLoaded("sereneseasons")) {
            return WeatherSeason.TEMPERATE;
        }
        try {
            return SereneSeasonsWeather.current(level);
        } catch (RuntimeException | LinkageError exception) {
            Rotwire.LOGGER.warn(
                    "Unable to read Serene Seasons; using temperate weather",
                    exception
            );
            return WeatherSeason.TEMPERATE;
        }
    }

    private static boolean isHordeDay(long day) {
        int interval = Math.max(
                1,
                HordeEventConfig.hordeSpawnDays.get()
        );
        if (HordeEventConfig.hordeSpawnVariation.get() != 0) {
            return false;
        }
        if (day == 0L) {
            return HordeEventConfig.spawnFirstDay.get();
        }
        return day > 0L && Math.floorMod(day, interval) == 0L;
    }

    private static void apply(
            ServerLevel level,
            WeatherDayPlan plan,
            WeatherOverride forced
    ) {
        ScheduledWeather expected = forced == null
                ? plan.weatherAt(level.getDayTime())
                : forced.weather();
        boolean shouldRain = expected.precipitation();
        boolean shouldThunder = expected.storm();
        if (level.isRaining() == shouldRain
                && level.isThundering() == shouldThunder) {
            return;
        }

        int remaining = forced == null
                ? plan.ticksUntilTransition(level.getDayTime())
                : Math.max(
                        1,
                        forced.remainingTicks(level.getGameTime())
                );
        level.setWeatherParameters(
                shouldRain ? 0 : remaining,
                shouldRain ? remaining : 0,
                shouldRain,
                shouldThunder
        );
    }

    private WeatherManager() {
    }
}
