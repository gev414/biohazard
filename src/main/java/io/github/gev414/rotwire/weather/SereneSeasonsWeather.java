package io.github.gev414.rotwire.weather;

import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

final class SereneSeasonsWeather {

    static WeatherSeason current(ServerLevel level) {
        Season season = SeasonHelper.getSeasonState(level).getSeason();
        return switch (season) {
            case SPRING -> WeatherSeason.SPRING;
            case SUMMER -> WeatherSeason.SUMMER;
            case AUTUMN -> WeatherSeason.AUTUMN;
            case WINTER -> WeatherSeason.WINTER;
        };
    }

    private SereneSeasonsWeather() {
    }
}
