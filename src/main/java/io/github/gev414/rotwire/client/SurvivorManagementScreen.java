package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.menu.SurvivorManagementMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SurvivorManagementScreen
        extends AbstractContainerScreen<SurvivorManagementMenu> {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 310;
    private static final int CONTENT_LEFT = 14;
    private static final int LEFT_COLUMN_RIGHT = 167;
    private static final int RIGHT_COLUMN_LEFT = 173;
    private static final int CONTENT_RIGHT = 326;
    private static final int ROLE_CARD_HEIGHT = 52;

    private static final int PANEL_COLOR = 0xF0141A17;
    private static final int INNER_COLOR = 0xE51D2720;
    private static final int BORDER_COLOR = 0xFF536B4E;
    private static final int TEXT_COLOR = 0xFFD8E7D4;
    private static final int MUTED_COLOR = 0xFF8DA18B;
    private static final int READY_COLOR = 0xFF86C76F;
    private static final int MISSING_COLOR = 0xFFD17863;

    private Button callCivilianButton;
    private Button callRiflemanButton;
    private Button callPistolmanButton;
    private Button callShotgunnerButton;
    private Button rallyButton;

    public SurvivorManagementScreen(
            SurvivorManagementMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
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
        ).bounds(leftPos + 20, topPos + 153, 141, 20).build());
        callRiflemanButton = addRenderableWidget(Button.builder(
                Component.translatable(
                        "screen.rotwire.survivors.call_rifleman"
                ),
                button -> clickMenuButton(
                        SurvivorManagementMenu.CALL_RIFLEMAN_BUTTON
                )
        ).bounds(leftPos + 179, topPos + 153, 141, 20).build());
        callPistolmanButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.rotwire.survivors.call_pistolman"),
                button -> clickMenuButton(
                        SurvivorManagementMenu.CALL_PISTOLMAN_BUTTON
                )
        ).bounds(leftPos + 20, topPos + 210, 141, 20).build());
        callShotgunnerButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.rotwire.survivors.call_shotgunner"),
                button -> clickMenuButton(
                        SurvivorManagementMenu.CALL_SHOTGUNNER_BUTTON
                )
        ).bounds(leftPos + 179, topPos + 210, 141, 20).build());
        rallyButton = addRenderableWidget(Button.builder(
                Component.translatable(
                        "screen.rotwire.survivors.rally"
                ),
                button -> clickMenuButton(
                        SurvivorManagementMenu.RALLY_SURVIVORS_BUTTON
                )
        ).bounds(leftPos + 20, topPos + 250, 300, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.rotwire.survivors.back"),
                button -> clickMenuButton(
                        SurvivorManagementMenu.BACK_BUTTON
                )
        ).bounds(leftPos + 114, topPos + 280, 112, 20).build());
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
                leftPos + CONTENT_LEFT,
                topPos + 38,
                leftPos + LEFT_COLUMN_RIGHT,
                topPos + 116,
                INNER_COLOR
        );
        graphics.fill(
                leftPos + RIGHT_COLUMN_LEFT,
                topPos + 38,
                leftPos + CONTENT_RIGHT,
                topPos + 116,
                INNER_COLOR
        );
        fillRoleCard(graphics, CONTENT_LEFT, 124);
        fillRoleCard(graphics, RIGHT_COLUMN_LEFT, 124);
        fillRoleCard(graphics, CONTENT_LEFT, 181);
        fillRoleCard(graphics, RIGHT_COLUMN_LEFT, 181);
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
                Component.literal(font.plainSubstrByWidth(
                        menu.settlementName(), imageWidth - 40
                )),
                imageWidth / 2,
                23,
                MUTED_COLOR
        );

        drawValue(
                graphics,
                22,
                47,
                "screen.rotwire.survivors.population",
                menu.population(),
                157
        );
        drawValue(
                graphics,
                22,
                62,
                "screen.rotwire.survivors.civilians",
                menu.civilians(),
                157
        );
        drawValue(
                graphics,
                22,
                77,
                "screen.rotwire.survivors.riflemen",
                menu.riflemen(),
                157
        );
        drawValue(
                graphics,
                22,
                92,
                "screen.rotwire.survivors.pistolmen",
                menu.pistolmen(),
                157
        );
        drawValue(
                graphics,
                22,
                107,
                "screen.rotwire.survivors.shotgunners",
                menu.shotgunners(),
                157
        );
        drawValue(
                graphics,
                181,
                47,
                "screen.rotwire.survivors.rations",
                menu.rations(),
                318
        );
        drawValue(
                graphics,
                181,
                62,
                "screen.rotwire.survivors.ammunition",
                menu.mosinAmmunition(),
                318
        );
        drawValue(graphics, 181, 77,
                "screen.rotwire.survivors.pistol_ammunition",
                menu.pistolAmmunition(), 318);
        drawValue(graphics, 181, 92,
                "screen.rotwire.survivors.shotgun_ammunition",
                menu.shotgunAmmunition(), 318);

        drawRoleCard(
                graphics,
                CONTENT_LEFT,
                124,
                "screen.rotwire.survivors.civilian",
                Component.translatable(
                        "screen.rotwire.survivors.requires_rations",
                        menu.civilianRationsRequired()
                ),
                hasCivilianResources()
        );
        drawRoleCard(
                graphics,
                RIGHT_COLUMN_LEFT,
                124,
                "screen.rotwire.survivors.rifleman",
                armedRequirements(
                        menu.riflemanRationsRequired(),
                        menu.riflemanAmmunitionRequired()
                ),
                hasRiflemanResources()
        );
        drawRoleCard(
                graphics,
                CONTENT_LEFT,
                181,
                "screen.rotwire.survivors.pistolman",
                armedRequirements(
                        menu.pistolmanRationsRequired(),
                        menu.pistolmanAmmunitionRequired()
                ),
                hasPistolmanResources()
        );
        drawRoleCard(
                graphics,
                RIGHT_COLUMN_LEFT,
                181,
                "screen.rotwire.survivors.shotgunner",
                armedRequirements(
                        menu.shotgunnerRationsRequired(),
                        menu.shotgunnerAmmunitionRequired()
                ),
                hasShotgunnerResources()
        );
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.rotwire.survivors.rally_hint"
                ),
                imageWidth / 2,
                238,
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
        if (callPistolmanButton != null) {
            callPistolmanButton.active = canManage
                    && menu.pistolmen() < menu.maximumPistolmen()
                    && hasPistolmanResources();
        }
        if (callShotgunnerButton != null) {
            callShotgunnerButton.active = canManage
                    && menu.shotgunners() < menu.maximumShotgunners()
                    && hasShotgunnerResources();
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

    private boolean hasPistolmanResources() {
        return menu.rations() >= menu.pistolmanRationsRequired()
                && menu.pistolAmmunition()
                >= menu.pistolmanAmmunitionRequired();
    }

    private boolean hasShotgunnerResources() {
        return menu.rations() >= menu.shotgunnerRationsRequired()
                && menu.shotgunAmmunition()
                >= menu.shotgunnerAmmunitionRequired();
    }

    private void drawValue(
            GuiGraphics graphics,
            int x,
            int y,
            String translationKey,
            int value,
            int valueRight
    ) {
        Component label = Component.translatable(translationKey);
        Component amount = Component.literal(Integer.toString(value));
        graphics.drawString(font, label, x, y, MUTED_COLOR, false);
        graphics.drawString(
                font,
                amount,
                valueRight - font.width(amount),
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

    private Component armedRequirements(int rations, int ammunition) {
        return Component.translatable(
                "screen.rotwire.survivors.rifleman_requirements",
                rations,
                ammunition
        );
    }

    private void fillRoleCard(GuiGraphics graphics, int x, int y) {
        graphics.fill(
                leftPos + x,
                topPos + y,
                leftPos + x + (LEFT_COLUMN_RIGHT - CONTENT_LEFT),
                topPos + y + ROLE_CARD_HEIGHT,
                INNER_COLOR
        );
    }

    private void drawRoleCard(
            GuiGraphics graphics,
            int x,
            int y,
            String titleKey,
            Component requirements,
            boolean hasResources
    ) {
        graphics.drawString(
                font,
                Component.translatable(titleKey),
                x + 8,
                y + 6,
                TEXT_COLOR,
                false
        );
        graphics.drawString(
                font,
                requirements,
                x + 8,
                y + 19,
                hasResources ? READY_COLOR : MISSING_COLOR,
                false
        );
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
