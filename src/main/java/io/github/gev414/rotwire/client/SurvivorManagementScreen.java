package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.menu.SurvivorManagementMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SurvivorManagementScreen
        extends AbstractContainerScreen<SurvivorManagementMenu> {

    private static final int PANEL_COLOR = 0xF0141A17;
    private static final int INNER_COLOR = 0xE51D2720;
    private static final int BORDER_COLOR = 0xFF536B4E;
    private static final int TEXT_COLOR = 0xFFD8E7D4;
    private static final int MUTED_COLOR = 0xFF8DA18B;
    private static final int READY_COLOR = 0xFF86C76F;
    private static final int MISSING_COLOR = 0xFFD17863;

    private Button callCivilianButton;
    private Button callRiflemanButton;
    private Button rallyButton;

    public SurvivorManagementScreen(
            SurvivorManagementMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        imageWidth = 276;
        imageHeight = 236;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        callCivilianButton = addRenderableWidget(Button.builder(
                Component.translatable(
                        "screen.rotwire.survivors.call_civilian"
                ),
                button -> clickMenuButton(
                        SurvivorManagementMenu.CALL_CIVILIAN_BUTTON
                )
        ).bounds(leftPos + 20, topPos + 137, 112, 20).build());
        callRiflemanButton = addRenderableWidget(Button.builder(
                Component.translatable(
                        "screen.rotwire.survivors.call_rifleman"
                ),
                button -> clickMenuButton(
                        SurvivorManagementMenu.CALL_RIFLEMAN_BUTTON
                )
        ).bounds(leftPos + 144, topPos + 137, 112, 20).build());
        rallyButton = addRenderableWidget(Button.builder(
                Component.translatable(
                        "screen.rotwire.survivors.rally"
                ),
                button -> clickMenuButton(
                        SurvivorManagementMenu.RALLY_SURVIVORS_BUTTON
                )
        ).bounds(leftPos + 20, topPos + 174, 236, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.rotwire.survivors.back"),
                button -> clickMenuButton(
                        SurvivorManagementMenu.BACK_BUTTON
                )
        ).bounds(leftPos + 82, topPos + 204, 112, 20).build());
        refreshButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
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
        outline(graphics);
        graphics.fill(
                leftPos + 14,
                topPos + 38,
                leftPos + 262,
                topPos + 95,
                INNER_COLOR
        );
        graphics.fill(
                leftPos + 14,
                topPos + 103,
                leftPos + 138,
                topPos + 163,
                INNER_COLOR
        );
        graphics.fill(
                leftPos + 138,
                topPos + 103,
                leftPos + 262,
                topPos + 163,
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
                Component.translatable("screen.rotwire.survivors.title"),
                imageWidth / 2,
                9,
                TEXT_COLOR
        );
        graphics.drawCenteredString(
                font,
                Component.literal(menu.settlementName()),
                imageWidth / 2,
                23,
                MUTED_COLOR
        );

        drawValue(
                graphics,
                22,
                47,
                "screen.rotwire.survivors.population",
                menu.population()
        );
        drawValue(
                graphics,
                22,
                62,
                "screen.rotwire.survivors.civilians",
                menu.civilians()
        );
        drawValue(
                graphics,
                22,
                77,
                "screen.rotwire.survivors.riflemen",
                menu.riflemen()
        );
        drawValue(
                graphics,
                148,
                47,
                "screen.rotwire.survivors.rations",
                menu.rations()
        );
        drawValue(
                graphics,
                148,
                62,
                "screen.rotwire.survivors.ammunition",
                menu.mosinAmmunition()
        );

        graphics.drawString(
                font,
                Component.translatable("screen.rotwire.survivors.civilian"),
                22,
                109,
                TEXT_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.rotwire.survivors.requires_rations",
                        menu.civilianRationsRequired()
                ),
                22,
                122,
                hasCivilianResources() ? READY_COLOR : MISSING_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("screen.rotwire.survivors.rifleman"),
                146,
                109,
                TEXT_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.rotwire.survivors.rifleman_requirements",
                        menu.riflemanRationsRequired(),
                        menu.riflemanAmmunitionRequired()
                ),
                146,
                122,
                hasRiflemanResources() ? READY_COLOR : MISSING_COLOR,
                false
        );
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.rotwire.survivors.rally_hint"
                ),
                imageWidth / 2,
                163,
                MUTED_COLOR
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshButtons() {
        boolean canManage = menu.canManageSurvivors();
        if (callCivilianButton != null) {
            callCivilianButton.active = canManage
                    && menu.civilians() < menu.maximumCivilians()
                    && hasCivilianResources();
        }
        if (callRiflemanButton != null) {
            callRiflemanButton.active = canManage
                    && menu.riflemen() < menu.maximumRiflemen()
                    && hasRiflemanResources();
        }
        if (rallyButton != null) {
            rallyButton.active = canManage && menu.population() > 0;
        }
    }

    private boolean hasCivilianResources() {
        return menu.rations() >= menu.civilianRationsRequired();
    }

    private boolean hasRiflemanResources() {
        return menu.rations() >= menu.riflemanRationsRequired()
                && menu.mosinAmmunition()
                >= menu.riflemanAmmunitionRequired();
    }

    private void drawValue(
            GuiGraphics graphics,
            int x,
            int y,
            String translationKey,
            int value
    ) {
        Component label = Component.translatable(translationKey);
        Component amount = Component.literal(Integer.toString(value));
        graphics.drawString(font, label, x, y, MUTED_COLOR, false);
        graphics.drawString(
                font,
                amount,
                x + 105 - font.width(amount),
                y,
                TEXT_COLOR,
                false
        );
    }

    private void clickMenuButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    buttonId
            );
        }
    }

    private void outline(GuiGraphics graphics) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1,
                BORDER_COLOR);
        graphics.fill(leftPos, topPos + imageHeight - 1,
                leftPos + imageWidth, topPos + imageHeight, BORDER_COLOR);
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight,
                BORDER_COLOR);
        graphics.fill(leftPos + imageWidth - 1, topPos,
                leftPos + imageWidth, topPos + imageHeight, BORDER_COLOR);
    }
}
