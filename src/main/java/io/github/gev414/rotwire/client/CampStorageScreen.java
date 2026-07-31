package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.menu.CampStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CampStorageScreen
        extends AbstractContainerScreen<CampStorageMenu> {

    private static final int PANEL_COLOR = 0xF0141A17;
    private static final int INNER_COLOR = 0xE51D2720;
    private static final int BORDER_COLOR = 0xFF536B4E;
    private static final int TEXT_COLOR = 0xFFD8E7D4;

    public CampStorageScreen(
            CampStorageMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 167;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 74;
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                PANEL_COLOR
        );
        outline(graphics);
        graphics.fill(
                leftPos + 7,
                topPos + 17,
                leftPos + 169,
                topPos + 72,
                INNER_COLOR
        );
        graphics.fill(
                leftPos + 7,
                topPos + 84,
                leftPos + 169,
                topPos + 162,
                INNER_COLOR
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOR);
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                TEXT_COLOR
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void outline(GuiGraphics graphics) {
        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + 1,
                BORDER_COLOR
        );
        graphics.fill(
                leftPos,
                topPos + imageHeight - 1,
                leftPos + imageWidth,
                topPos + imageHeight,
                BORDER_COLOR
        );
        graphics.fill(
                leftPos,
                topPos,
                leftPos + 1,
                topPos + imageHeight,
                BORDER_COLOR
        );
        graphics.fill(
                leftPos + imageWidth - 1,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                BORDER_COLOR
        );
    }
}
