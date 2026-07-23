package io.github.gev414.biohazard.client;

import io.github.gev414.biohazard.network.SurvivalStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class SurvivalStatusClient {

    private static final int WIDTH = 116;
    private static final int BASE_HEIGHT = 27;
    private static final int SUSPICION_HEIGHT = 7;
    private static final int RIGHT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 8;
    private static final int CHAT_BOTTOM_MARGIN = 20;

    private static volatile SurvivalStatusPayload status;

    public static void update(SurvivalStatusPayload payload) {
        status = payload;
    }

    public static void reset() {
        status = null;
    }

    static SurvivalStatusPayload current() {
        return status;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        SurvivalStatusPayload current = status;
        if (current == null
                || !current.enabled()
                || minecraft.options.hideGui
                || minecraft.player == null) {
            return;
        }

        int suspicion = current.suspicionPercent();
        int height = suspicion > 0
                ? BASE_HEIGHT + SUSPICION_HEIGHT
                : BASE_HEIGHT;
        int bottomMargin = minecraft.screen instanceof ChatScreen
                ? CHAT_BOTTOM_MARGIN
                : BOTTOM_MARGIN;
        int right = graphics.guiWidth() - RIGHT_MARGIN;
        int bottom = graphics.guiHeight() - bottomMargin;
        int left = right - WIDTH;
        int top = bottom - height;
        graphics.fill(
                left - 1,
                top - 1,
                right + 1,
                bottom + 1,
                0xA04B684E
        );
        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xB80B1210
        );
        graphics.drawString(
                minecraft.font,
                Component.translatable(
                        "hud.biohazard.encumbrance",
                        String.format(
                                Locale.ROOT,
                                "%.1f",
                                current.weightTenths() / 10.0D
                        ),
                        tierName(current.tier())
                ),
                left + 5,
                top + 4,
                tierColor(current.tier()),
                false
        );
        graphics.drawString(
                minecraft.font,
                Component.translatable(
                        current.quiet()
                                ? "hud.biohazard.stealth.quiet"
                                : "hud.biohazard.stealth.exposed"
                ),
                left + 5,
                top + 15,
                current.quiet() ? 0x95D69A : 0xD6B78A,
                false
        );
        if (suspicion > 0) {
            int barLeft = left + 5;
            int barTop = top + 29;
            int barWidth = WIDTH - 10;
            graphics.fill(
                    barLeft,
                    barTop,
                    barLeft + barWidth,
                    barTop + 3,
                    0xFF2A302B
            );
            graphics.fill(
                    barLeft,
                    barTop,
                    barLeft + Math.round(barWidth * suspicion / 100.0F),
                    barTop + 3,
                    suspicion >= 75 ? 0xFFD65E54 : 0xFFD6B85E
            );
        }
    }

    private static Component tierName(int tier) {
        String key = switch (tier) {
            case 1 -> "hud.biohazard.encumbrance.burdened";
            case 2 -> "hud.biohazard.encumbrance.heavy";
            case 3 -> "hud.biohazard.encumbrance.overloaded";
            default -> "hud.biohazard.encumbrance.light";
        };
        return Component.translatable(key);
    }

    private static int tierColor(int tier) {
        return switch (tier) {
            case 1 -> 0xE8D39B;
            case 2 -> 0xE89E73;
            case 3 -> 0xE8685F;
            default -> 0xB9DFB7;
        };
    }

    private SurvivalStatusClient() {
    }
}
