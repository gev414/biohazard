package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.menu.CampRadioMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CampRadioScreen
        extends AbstractContainerScreen<CampRadioMenu> {

    private static final int PANEL_COLOR = 0xF0141A17;
    private static final int INNER_COLOR = 0xE51D2720;
    private static final int BORDER_COLOR = 0xFF536B4E;
    private static final int TEXT_COLOR = 0xFFD8E7D4;
    private static final int MUTED_COLOR = 0xFF8DA18B;
    private static final int READY_COLOR = 0xFF86C76F;
    private static final int MISSING_COLOR = 0xFFD17863;

    private Button contractsButton;

    public CampRadioScreen(
            CampRadioMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        imageWidth = 292;
        imageHeight = 210;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        contractsButton = addRenderableWidget(Button.builder(
                Component.translatable(
                        "screen.rotwire.camp_radio.contracts"
                ),
                button -> openContracts()
        ).bounds(
                leftPos + 164,
                topPos + 174,
                110,
                20
        ).build());
        contractsButton.active = menu.connected();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (contractsButton != null) {
            contractsButton.active = menu.connected();
        }
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
        outline(
                graphics,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                BORDER_COLOR
        );
        graphics.fill(
                leftPos + 14,
                topPos + 42,
                leftPos + 155,
                topPos + 196,
                INNER_COLOR
        );
        graphics.fill(
                leftPos + 163,
                topPos + 42,
                leftPos + 278,
                topPos + 164,
                INNER_COLOR
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawCenteredString(
                font,
                title,
                imageWidth / 2,
                10,
                TEXT_COLOR
        );
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        menu.active()
                                ? "screen.rotwire.camp_radio.state.active"
                                : "screen.rotwire.camp_radio.state.inactive"
                ),
                imageWidth / 2,
                25,
                menu.active() ? READY_COLOR : MISSING_COLOR
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.rotwire.camp_radio.section.status"
                ),
                22,
                50,
                MUTED_COLOR,
                false
        );
        drawValue(
                graphics,
                22,
                65,
                "screen.rotwire.camp_radio.shelter",
                Component.translatable(
                        menu.shelter().translationKey()
                )
        );
        drawValue(
                graphics,
                22,
                78,
                "screen.rotwire.camp_radio.radius",
                Component.literal(Integer.toString(menu.radius()))
        );
        drawRequirement(
                graphics,
                22,
                96,
                "screen.rotwire.camp_radio.sleeping_bag",
                menu.sleepingBagPresent()
        );
        drawRequirement(
                graphics,
                22,
                109,
                "screen.rotwire.camp_radio.campfire",
                menu.campfirePresent()
        );
        drawRequirement(
                graphics,
                22,
                122,
                "screen.rotwire.camp_radio.backpack",
                menu.backpackPresent()
        );
        drawRequirement(
                graphics,
                22,
                135,
                "screen.rotwire.camp_radio.ration",
                menu.rationReady()
        );
        drawValue(
                graphics,
                22,
                153,
                "screen.rotwire.camp_radio.nutrition",
                Component.literal(
                        Integer.toString(menu.availableNutrition())
                )
        );
        drawRequirement(
                graphics,
                22,
                174,
                "screen.rotwire.camp_radio.network",
                menu.connected()
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.rotwire.camp_radio.section.modules"
                ),
                171,
                50,
                MUTED_COLOR,
                false
        );
        drawModule(
                graphics,
                171,
                68,
                "screen.rotwire.camp_radio.module.storage"
        );
        drawModule(
                graphics,
                171,
                96,
                "screen.rotwire.camp_radio.module.crafting"
        );
        drawModule(
                graphics,
                171,
                124,
                "screen.rotwire.camp_radio.module.operations"
        );

        if (!menu.connected()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.rotwire.camp_radio.calibrating",
                            menu.connectionSeconds()
                    ),
                    220,
                    165,
                    MUTED_COLOR
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawRequirement(
            GuiGraphics graphics,
            int x,
            int y,
            String translationKey,
            boolean ready
    ) {
        Component marker = Component.literal(ready ? "[+]" : "[-]")
                .withStyle(
                        ready
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                );
        graphics.drawString(font, marker, x, y, 0xFFFFFFFF, false);
        graphics.drawString(
                font,
                Component.translatable(translationKey),
                x + 20,
                y,
                ready ? READY_COLOR : MISSING_COLOR,
                false
        );
    }

    private void drawValue(
            GuiGraphics graphics,
            int x,
            int y,
            String translationKey,
            Component value
    ) {
        graphics.drawString(
                font,
                Component.translatable(translationKey),
                x,
                y,
                MUTED_COLOR,
                false
        );
        graphics.drawString(
                font,
                value,
                x + 50,
                y,
                TEXT_COLOR,
                false
        );
    }

    private void drawModule(
            GuiGraphics graphics,
            int x,
            int y,
            String translationKey
    ) {
        graphics.fill(
                x,
                y,
                x + 108,
                y + 20,
                0xFF28332B
        );
        graphics.drawString(
                font,
                Component.translatable(translationKey),
                x + 5,
                y + 6,
                MUTED_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.rotwire.camp_radio.module.locked"
                ),
                x + 70,
                y + 6,
                MISSING_COLOR,
                false
        );
    }

    private void openContracts() {
        if (minecraft != null
                && minecraft.gameMode != null
                && menu.connected()) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    CampRadioMenu.CONTRACTS_BUTTON
            );
        }
    }

    private static void outline(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
