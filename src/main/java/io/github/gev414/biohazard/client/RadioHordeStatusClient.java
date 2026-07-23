package io.github.gev414.biohazard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RadioHordeStatusClient {

    private static final int WIDTH = 192;
    private static final int HEIGHT = 62;
    private static final int MARGIN = 18;

    public static void render(Screen screen, GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!CityStatusClient.hasRadioSnapshot()
                || !CityStatusClient.isQuestScreen(screen)
                || minecraft.level == null) {
            return;
        }

        int left = Math.max(8, screen.width - WIDTH - MARGIN);
        int top = Math.max(8, screen.height - HEIGHT - MARGIN);
        int right = left + WIDTH;
        int bottom = top + HEIGHT;
        HordeAtmosphereState.Snapshot state =
                HordeAtmosphereState.current();

        graphics.fill(
                left - 1,
                top - 1,
                right + 1,
                bottom + 1,
                0xB04B684E
        );
        graphics.fill(left, top, right, bottom, 0xE50B1210);
        graphics.fill(left, top, right, top + 18, 0xE51B3021);
        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable(
                        "screen.biohazard.radio_horde.title"
                ),
                (left + right) / 2,
                top + 5,
                0xD8EED3
        );
        graphics.drawString(
                minecraft.font,
                Component.translatable(
                        "screen.biohazard.radio_horde.day",
                        Component.translatable(
                                state.hordeDay()
                                        ? "screen.biohazard.radio_horde.yes"
                                        : "screen.biohazard.radio_horde.no"
                        )
                ),
                left + 8,
                top + 25,
                state.hordeDay() ? 0xE3B060 : 0xAFC8AD,
                false
        );
        graphics.drawString(
                minecraft.font,
                Component.translatable(
                        state.active()
                                ? "screen.biohazard.radio_horde.active"
                                : "screen.biohazard.radio_horde.inactive"
                ),
                left + 8,
                top + 39,
                state.active() ? 0xE8685F : 0xAFC8AD,
                false
        );
        graphics.drawString(
                minecraft.font,
                Component.translatable(
                        "screen.biohazard.radio_horde.time",
                        RadioClock.format(minecraft.level.getDayTime())
                ),
                right - 57,
                top + 39,
                0xD8EED3,
                false
        );
    }

    private RadioHordeStatusClient() {
    }
}
