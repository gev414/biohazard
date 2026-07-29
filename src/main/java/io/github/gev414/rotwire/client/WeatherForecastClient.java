package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.network.WeatherForecastPayload;
import io.github.gev414.rotwire.weather.ScheduledWeather;
import io.github.gev414.rotwire.weather.WeatherDayPlan;
import io.github.gev414.rotwire.weather.WeatherSeason;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class WeatherForecastClient {

    private static final int PANEL_WIDTH = 208;
    private static final int PANEL_HEIGHT = 144;
    private static final int PANEL_TOP = 72;
    private static final int TAB_OFFSET = 24;
    private static final int TAB_WIDTH = 34;
    private static final int TAB_HEIGHT = 20;
    private static final int RIGHT_MARGIN = 24;

    private static WeatherForecastPayload forecast;
    private static boolean expanded;

    public static void update(WeatherForecastPayload payload) {
        forecast = payload;
        expanded = false;
    }

    public static void clear() {
        forecast = null;
        expanded = false;
    }

    public static void collapse() {
        expanded = false;
    }

    public static void render(
            Screen screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        if (forecast == null
                || !CityStatusClient.isQuestScreen(screen)) {
            return;
        }

        int panelTop = panelTop(screen);
        int tabTop = panelTop + TAB_OFFSET;
        int tabRight = screen.width - RIGHT_MARGIN;
        int tabLeft = tabRight - TAB_WIDTH;
        boolean hovered = contains(
                mouseX,
                mouseY,
                tabLeft,
                tabTop,
                tabRight,
                tabTop + TAB_HEIGHT
        );
        drawTab(
                graphics,
                tabLeft,
                tabTop,
                tabRight,
                hovered
        );
        if (!expanded) {
            return;
        }

        int panelRight = tabLeft;
        int panelLeft = panelRight - PANEL_WIDTH;
        int panelBottom = panelTop + PANEL_HEIGHT;
        graphics.fill(
                panelLeft - 1,
                panelTop - 1,
                panelRight + 1,
                panelBottom + 1,
                0xB04B684E
        );
        graphics.fill(
                panelLeft,
                panelTop,
                panelRight,
                panelBottom,
                0xE50B1210
        );
        graphics.fill(
                panelLeft,
                panelTop,
                panelRight,
                panelTop + 20,
                0xE51B3021
        );
        graphics.drawCenteredString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        "screen.rotwire.weather.title"
                ),
                (panelLeft + panelRight) / 2,
                panelTop + 6,
                0xD8EED3
        );

        if (!forecast.available()) {
            graphics.drawString(
                    MinecraftFonts.font(graphics),
                    Component.translatable(
                            "screen.rotwire.weather.unavailable"
                    ),
                    panelLeft + 10,
                    panelTop + 31,
                    0xD6BDA6,
                    false
            );
            return;
        }
        drawForecast(graphics, panelLeft, panelRight, panelTop);
    }

    public static boolean handleMouseClick(
            Screen screen,
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0
                || forecast == null
                || !CityStatusClient.isQuestScreen(screen)) {
            return false;
        }
        int panelTop = panelTop(screen);
        int tabTop = panelTop + TAB_OFFSET;
        int tabRight = screen.width - RIGHT_MARGIN;
        int tabLeft = tabRight - TAB_WIDTH;
        if (!contains(
                mouseX,
                mouseY,
                tabLeft,
                tabTop,
                tabRight,
                tabTop + TAB_HEIGHT
        )) {
            return false;
        }

        expanded = !expanded;
        if (expanded) {
            CityStatusClient.collapse();
        }
        return true;
    }

    private static void drawForecast(
            GuiGraphics graphics,
            int panelLeft,
            int panelRight,
            int panelTop
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int tick = minecraft.level == null
                ? 0
                : (int) Math.floorMod(
                        minecraft.level.getDayTime(),
                        WeatherDayPlan.DAY_LENGTH
                );
        ScheduledWeather today = ScheduledWeather.fromNetwork(
                forecast.todayWeather()
        );
        boolean forced = forcedActive(minecraft);
        ScheduledWeather current = forced
                ? ScheduledWeather.fromNetwork(
                        forecast.forcedWeather()
                )
                : conditionAt(
                        today,
                        forecast.todayStart(),
                        forecast.todayEnd(),
                        tick
                );
        int textLeft = panelLeft + 10;

        graphics.drawString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        "screen.rotwire.weather.now",
                        weatherName(current)
                ),
                textLeft,
                panelTop + 28,
                weatherColor(current),
                false
        );
        graphics.drawString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        "screen.rotwire.weather.time",
                        RadioClock.format(tick)
                ),
                panelRight - 65,
                panelTop + 28,
                0xD8EED3,
                false
        );
        Component transition = forced
                ? Component.translatable(
                        "screen.rotwire.weather.override",
                        formatDuration(
                                forcedRemainingTicks(minecraft)
                        )
                )
                : transition(today, tick);
        if (transition != null) {
            graphics.drawString(
                    MinecraftFonts.font(graphics),
                    transition,
                    textLeft,
                    panelTop + 43,
                    0xB8C9B5,
                    false
            );
        }

        graphics.fill(
                textLeft,
                panelTop + 57,
                panelRight - 10,
                panelTop + 58,
                0x604B684E
        );
        drawDay(
                graphics,
                textLeft,
                panelTop + 65,
                "screen.rotwire.weather.today",
                today,
                forecast.todayStart(),
                forecast.todayEnd()
        );
        drawDay(
                graphics,
                textLeft,
                panelTop + 101,
                "screen.rotwire.weather.tomorrow",
                ScheduledWeather.fromNetwork(
                        forecast.tomorrowWeather()
                ),
                forecast.tomorrowStart(),
                forecast.tomorrowEnd()
        );

        WeatherSeason season = season();
        graphics.drawString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        "screen.rotwire.weather.season",
                        Component.translatable(
                                "screen.rotwire.weather.season."
                                        + season.name().toLowerCase()
                        )
                ),
                panelRight - 78,
                panelTop + 130,
                0x899C88,
                false
        );
    }

    private static void drawDay(
            GuiGraphics graphics,
            int left,
            int top,
            String headingKey,
            ScheduledWeather weather,
            int start,
            int end
    ) {
        graphics.drawString(
                MinecraftFonts.font(graphics),
                Component.translatable(headingKey),
                left,
                top,
                0x899C88,
                false
        );
        graphics.drawString(
                MinecraftFonts.font(graphics),
                weatherName(weather),
                left,
                top + 12,
                weatherColor(weather),
                false
        );
        if (weather != ScheduledWeather.CLEAR) {
            graphics.drawString(
                    MinecraftFonts.font(graphics),
                    Component.translatable(
                            "screen.rotwire.weather.window",
                            RadioClock.format(start),
                            RadioClock.format(end)
                    ),
                    left + 104,
                    top + 12,
                    0xB8C9B5,
                    false
            );
        }
    }

    private static Component transition(
            ScheduledWeather weather,
            int tick
    ) {
        if (weather == ScheduledWeather.CLEAR) {
            return null;
        }
        if (tick < forecast.todayStart()) {
            return Component.translatable(
                    "screen.rotwire.weather.begins",
                    RadioClock.format(forecast.todayStart())
            );
        }
        if (tick < forecast.todayEnd()) {
            return Component.translatable(
                    "screen.rotwire.weather.clears",
                    RadioClock.format(forecast.todayEnd())
            );
        }
        return null;
    }

    private static ScheduledWeather conditionAt(
            ScheduledWeather weather,
            int start,
            int end,
            int tick
    ) {
        return weather != ScheduledWeather.CLEAR
                && tick >= start
                && tick < end
                ? weather
                : ScheduledWeather.CLEAR;
    }

    private static Component weatherName(ScheduledWeather weather) {
        return Component.translatable(
                "screen.rotwire.weather.type."
                        + weather.serializedName()
        );
    }

    private static int weatherColor(ScheduledWeather weather) {
        if (weather.contaminated()) {
            return weather.storm() ? 0xE8685F : 0xD6B85E;
        }
        return switch (weather) {
            case STORM -> 0xA9B3C6;
            case RAIN -> 0x9CB8C7;
            default -> 0xB9DFB7;
        };
    }

    private static WeatherSeason season() {
        WeatherSeason[] seasons = WeatherSeason.values();
        int ordinal = forecast.season();
        return ordinal >= 0 && ordinal < seasons.length
                ? seasons[ordinal]
                : WeatherSeason.TEMPERATE;
    }

    private static boolean forcedActive(Minecraft minecraft) {
        return forecast.forced()
                && minecraft.level != null
                && forecast.forcedExpiresAt()
                > minecraft.level.getGameTime();
    }

    private static long forcedRemainingTicks(Minecraft minecraft) {
        if (minecraft.level == null) {
            return 0L;
        }
        return Math.max(
                0L,
                forecast.forcedExpiresAt()
                        - minecraft.level.getGameTime()
        );
    }

    private static String formatDuration(long ticks) {
        long totalSeconds = Math.max(0L, (ticks + 19L) / 20L);
        long hours = totalSeconds / 3_600L;
        long minutes = totalSeconds % 3_600L / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(
                    Locale.ROOT,
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
            );
        }
        return String.format(
                Locale.ROOT,
                "%d:%02d",
                minutes,
                seconds
        );
    }

    private static void drawTab(
            GuiGraphics graphics,
            int left,
            int top,
            int right,
            boolean hovered
    ) {
        graphics.fill(
                left - 1,
                top - 1,
                right + 1,
                top + TAB_HEIGHT + 1,
                0xD04B684E
        );
        graphics.fill(
                left,
                top,
                right,
                top + TAB_HEIGHT,
                hovered ? 0xF02A4931 : 0xE51B3021
        );
        graphics.drawCenteredString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        expanded
                                ? "screen.rotwire.weather.tab.close"
                                : "screen.rotwire.weather.tab.open"
                ),
                (left + right) / 2,
                top + 6,
                0xD8EED3
        );
    }

    private static int panelTop(Screen screen) {
        return Math.max(
                12,
                Math.min(
                        PANEL_TOP,
                        screen.height - PANEL_HEIGHT - 12
                )
        );
    }

    private static boolean contains(
            double x,
            double y,
            int left,
            int top,
            int right,
            int bottom
    ) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    private WeatherForecastClient() {
    }
}
