package io.github.gev414.biohazard.client;

import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import io.github.gev414.biohazard.network.CityStatusPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CityStatusClient {

    private static final int PANEL_WIDTH = 178;
    private static final int PANEL_HEIGHT = 104;
    private static final int PANEL_TOP = 72;
    private static final int TAB_WIDTH = 34;
    private static final int TAB_HEIGHT = 20;
    private static final int RIGHT_MARGIN = 24;

    private static CityStatusPayload status;
    private static boolean expanded;

    public static void update(CityStatusPayload payload) {
        status = payload;
        expanded = false;
    }

    public static void clear() {
        status = null;
        expanded = false;
    }

    public static boolean hasRadioSnapshot() {
        return status != null;
    }

    public static void render(
            Screen screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        if (status == null || !isQuestScreen(screen)) {
            return;
        }

        int panelTop = panelTop(screen);
        int tabRight = screen.width - RIGHT_MARGIN;
        int tabLeft = tabRight - TAB_WIDTH;
        int tabBottom = panelTop + TAB_HEIGHT;
        boolean tabHovered = contains(
                mouseX,
                mouseY,
                tabLeft,
                panelTop,
                tabRight,
                tabBottom
        );

        graphics.fill(
                tabLeft - 1,
                panelTop - 1,
                tabRight + 1,
                tabBottom + 1,
                0xD04B684E
        );
        graphics.fill(
                tabLeft,
                panelTop,
                tabRight,
                tabBottom,
                tabHovered ? 0xF02A4931 : 0xE51B3021
        );
        graphics.drawCenteredString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        expanded
                                ? "screen.biohazard.city_status.tab.close"
                                : "screen.biohazard.city_status.tab.open"
                ),
                (tabLeft + tabRight) / 2,
                panelTop + 6,
                0xD8EED3
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
                        "screen.biohazard.city_status.compact_title"
                ),
                (panelLeft + panelRight) / 2,
                panelTop + 6,
                0xD8EED3
        );

        int textLeft = panelLeft + 10;
        int line = panelTop + 29;
        if (!status.mapped()) {
            graphics.drawString(
                    MinecraftFonts.font(graphics),
                    Component.translatable(
                            "screen.biohazard.city_status.none.line1"
                    ),
                    textLeft,
                    line,
                    0xD6BDA6,
                    false
            );
            graphics.drawString(
                    MinecraftFonts.font(graphics),
                    Component.translatable(
                            "screen.biohazard.city_status.none.line2"
                    ),
                    textLeft,
                    line + 16,
                    0xD6BDA6,
                    false
            );
            return;
        }

        graphics.drawString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        "screen.biohazard.city_status.cleared",
                        status.clearedBuildings()
                ),
                textLeft,
                line,
                0xD8EED3,
                false
        );
        graphics.drawString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        "screen.biohazard.city_status.danger",
                        status.dangerLevel(),
                        status.maximumDangerLevel()
                ),
                textLeft,
                line + 17,
                0xE8D39B,
                false
        );
        graphics.drawString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        "screen.biohazard.city_status.health",
                        status.healthPercent()
                ),
                textLeft,
                line + 34,
                0xD99C9C,
                false
        );
        graphics.drawString(
                MinecraftFonts.font(graphics),
                Component.translatable(
                        "screen.biohazard.city_status.next",
                        status.remainingUntilNextLevel()
                ),
                textLeft,
                line + 51,
                0xB8C9B5,
                false
        );
    }

    public static boolean handleMouseClick(
            Screen screen,
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0 || status == null || !isQuestScreen(screen)) {
            return false;
        }

        int tabRight = screen.width - RIGHT_MARGIN;
        int tabLeft = tabRight - TAB_WIDTH;
        int panelTop = panelTop(screen);
        if (!contains(
                mouseX,
                mouseY,
                tabLeft,
                panelTop,
                tabRight,
                panelTop + TAB_HEIGHT
        )) {
            return false;
        }

        expanded = !expanded;
        return true;
    }

    public static boolean isQuestScreen(Screen screen) {
        return screen instanceof ScreenWrapper wrapper
                && wrapper.getGui() instanceof QuestScreen;
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

    private CityStatusClient() {
    }
}
