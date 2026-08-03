package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.network.SettlementNamePayload;
import io.github.gev414.rotwire.settlement.SettlementNameRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class SettlementNameScreen extends Screen {

    private final BlockPos radioPosition;
    private EditBox nameField;

    SettlementNameScreen(BlockPos radioPosition) {
        super(Component.translatable("screen.rotwire.settlement.name.title"));
        this.radioPosition = radioPosition.immutable();
    }

    @Override
    protected void init() {
        int left = width / 2 - 110;
        int top = height / 2 - 42;
        nameField = addRenderableWidget(new EditBox(
                font,
                left,
                top + 34,
                220,
                20,
                Component.translatable("screen.rotwire.settlement.name.input")
        ));
        nameField.setMaxLength(SettlementNameRules.MAX_LENGTH);
        nameField.setFocused(true);
        setFocused(nameField);
        addRenderableWidget(Button.builder(
                Component.translatable("screen.rotwire.settlement.name.confirm"),
                button -> submit()
        ).bounds(left + 56, top + 62, 108, 20).build());
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.fill(0, 0, width, height, 0xB0000000);
        int left = width / 2 - 124;
        int top = height / 2 - 56;
        graphics.fill(left, top, left + 248, top + 132, 0xF0141A17);
        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                top + 14,
                0xFFD8E7D4
        );
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.rotwire.settlement.name.prompt"),
                width / 2,
                top + 28,
                0xFF8DA18B
        );
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void submit() {
        if (nameField == null
                || SettlementNameRules.normalize(nameField.getValue())
                .isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(new SettlementNamePayload(
                radioPosition,
                nameField.getValue()
        ));
        onClose();
    }
}
