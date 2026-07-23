package io.github.gev414.biohazard.client;

import io.github.gev414.biohazard.network.SurvivalStatusPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InventoryEncumbranceClient {

    private static final int X_OFFSET = 126;
    private static final int Y_OFFSET = 59;
    private static final int WIDTH = 47;
    private static final int HEIGHT = 22;
    private static final int BAR_LEFT_OFFSET = 3;
    private static final int BAR_TOP_OFFSET = 16;
    private static final int BAR_WIDTH = WIDTH - 6;

    public static void render(
            InventoryScreen screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        SurvivalStatusPayload status = SurvivalStatusClient.current();
        boolean recipeBookCoversInventory =
                screen.getRecipeBookComponent().isVisible()
                        && graphics.guiWidth() < 379;
        if (status == null
                || !status.enabled()
                || Minecraft.getInstance().player == null
                || recipeBookCoversInventory) {
            return;
        }

        int left = screen.getGuiLeft() + X_OFFSET;
        int top = screen.getGuiTop() + Y_OFFSET;
        int right = left + WIDTH;
        int bottom = top + HEIGHT;
        int color = tierColor(status.tier());

        graphics.fill(
                left - 1,
                top - 1,
                right + 1,
                bottom + 1,
                0xFF373737
        );
        graphics.fill(left, top, right, bottom, 0xE5101512);
        drawWeightGlyph(graphics, left + 3, top + 3, color);

        Font font = Minecraft.getInstance().font;
        String weight = formatTenths(status.weightTenths());
        graphics.drawString(
                font,
                weight,
                right - 3 - font.width(weight),
                top + 4,
                color,
                false
        );
        drawTierBar(graphics, status, left, top, color);

        if (contains(mouseX, mouseY, left, top, right, bottom)) {
            graphics.renderComponentTooltip(
                    font,
                    tooltip(status),
                    mouseX,
                    mouseY
            );
        }
    }

    private static void drawWeightGlyph(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x + 3, y, x + 7, y + 2, color);
        graphics.fill(x + 2, y + 2, x + 8, y + 4, color);
        graphics.fill(x + 1, y + 4, x + 9, y + 9, color);
        graphics.fill(x, y + 8, x + 10, y + 10, color);
        graphics.fill(x + 3, y + 2, x + 7, y + 4, 0xE5101512);
    }

    private static void drawTierBar(
            GuiGraphics graphics,
            SurvivalStatusPayload status,
            int left,
            int top,
            int color
    ) {
        int barLeft = left + BAR_LEFT_OFFSET;
        int barTop = top + BAR_TOP_OFFSET;
        int heavyMaximum = Math.max(1, status.heavyMaximumTenths());
        int filled = Math.round(
                BAR_WIDTH
                        * Math.min(status.weightTenths(), heavyMaximum)
                        / (float) heavyMaximum
        );

        graphics.fill(
                barLeft,
                barTop,
                barLeft + BAR_WIDTH,
                barTop + 3,
                0xFF292E2A
        );
        graphics.fill(
                barLeft,
                barTop,
                barLeft + filled,
                barTop + 3,
                color
        );
        drawDivider(
                graphics,
                barLeft,
                barTop,
                status.lightMaximumTenths(),
                heavyMaximum
        );
        drawDivider(
                graphics,
                barLeft,
                barTop,
                status.burdenedMaximumTenths(),
                heavyMaximum
        );
    }

    private static void drawDivider(
            GuiGraphics graphics,
            int barLeft,
            int barTop,
            int threshold,
            int heavyMaximum
    ) {
        int x = barLeft + Math.round(
                BAR_WIDTH
                        * Math.min(threshold, heavyMaximum)
                        / (float) heavyMaximum
        );
        graphics.fill(x, barTop - 1, x + 1, barTop + 4, 0xFFD6D6D6);
    }

    private static List<Component> tooltip(
            SurvivalStatusPayload status
    ) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "tooltip.biohazard.encumbrance.title"
        ).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable(
                "tooltip.biohazard.encumbrance.current",
                formatTenths(status.weightTenths()),
                tierName(status.tier())
        ).withStyle(style -> style.withColor(tierColor(status.tier()))));
        lines.add(Component.empty());
        lines.add(tierLine(
                "tooltip.biohazard.encumbrance.light",
                0,
                status.tier(),
                formatTenths(status.lightMaximumTenths()),
                "0%"
        ));
        lines.add(tierLine(
                "tooltip.biohazard.encumbrance.burdened",
                1,
                status.tier(),
                formatTenths(status.lightMaximumTenths()),
                formatTenths(status.burdenedMaximumTenths()),
                status.burdenedPenaltyPercent() + "%"
        ));
        lines.add(tierLine(
                "tooltip.biohazard.encumbrance.heavy",
                2,
                status.tier(),
                formatTenths(status.burdenedMaximumTenths()),
                formatTenths(status.heavyMaximumTenths()),
                status.heavyPenaltyPercent() + "%"
        ));
        lines.add(tierLine(
                "tooltip.biohazard.encumbrance.overloaded",
                3,
                status.tier(),
                formatTenths(status.heavyMaximumTenths()),
                status.overloadedPenaltyPercent() + "%"
        ));
        lines.add(Component.empty());
        lines.add(Component.translatable(
                "tooltip.biohazard.encumbrance.stealth"
        ).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static Component tierLine(
            String key,
            int lineTier,
            int currentTier,
            Object... arguments
    ) {
        Component line = Component.translatable(key, arguments);
        return lineTier == currentTier
                ? line.copy().withStyle(
                        style -> style.withColor(tierColor(lineTier))
                )
                : line.copy().withStyle(ChatFormatting.GRAY);
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

    private static String formatTenths(int value) {
        return String.format(
                Locale.ROOT,
                "%.1f",
                Math.max(0, value) / 10.0D
        );
    }

    private static int tierColor(int tier) {
        return switch (tier) {
            case 1 -> 0xE8D39B;
            case 2 -> 0xE89E73;
            case 3 -> 0xE8685F;
            default -> 0xB9DFB7;
        };
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

    private InventoryEncumbranceClient() {
    }
}
