package io.github.gev414.biohazard.client;

import io.github.gev414.biohazard.network.CourierChoiceOpenPayload;
import io.github.gev414.biohazard.network.CourierChoiceSelectPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

final class CourierChoiceScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int OPTION_WIDTH = 78;
    private static final int OPTION_HEIGHT = 48;
    private static final int OPTION_SPACING = 8;

    private final String deliveryId;
    private final List<ItemStack> options;

    CourierChoiceScreen(CourierChoiceOpenPayload payload) {
        super(Component.translatable("screen.biohazard.courier_choice.title"));
        deliveryId = payload.deliveryId();
        options = payload.options().stream()
                .map(CourierChoiceScreen::stackFor)
                .toList();
    }

    @Override
    protected void init() {
        int columns = Math.min(3, options.size());
        int totalWidth = columns * OPTION_WIDTH
                + Math.max(0, columns - 1) * OPTION_SPACING;
        int originX = (width - totalWidth) / 2;
        int originY = height / 2 - 12;

        for (int index = 0; index < options.size(); index++) {
            int column = index % 3;
            int row = index / 3;
            int optionIndex = index;
            addRenderableWidget(Button.builder(
                    Component.empty(),
                    button -> choose(optionIndex)
            ).bounds(
                    originX + column * (OPTION_WIDTH + OPTION_SPACING),
                    originY + row * (OPTION_HEIGHT + OPTION_SPACING),
                    OPTION_WIDTH,
                    OPTION_HEIGHT
            ).build());
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int panelLeft = (width - PANEL_WIDTH) / 2;
        int panelTop = height / 2 - 70;
        int panelBottom = Math.min(height - 12, panelTop + 150);
        graphics.fill(0, 0, width, height, 0xB0000000);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelBottom,
                0xE5151B18);
        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                panelTop + 12,
                0xD3E9D2
        );
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.biohazard.courier_choice.prompt"),
                width / 2,
                panelTop + 30,
                0xA8C5A5
        );

        super.render(graphics, mouseX, mouseY, partialTick);

        for (int index = 0; index < options.size(); index++) {
            ItemStack stack = options.get(index);
            Button button = (Button) children().get(index);
            int itemX = button.getX() + (button.getWidth() - 16) / 2;
            int itemY = button.getY() + 8;
            graphics.renderItem(stack, itemX, itemY);
            graphics.drawCenteredString(
                    font,
                    stack.getHoverName(),
                    button.getX() + button.getWidth() / 2,
                    button.getY() + 28,
                    0xFFFFFF
            );
            if (button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void choose(int optionIndex) {
        PacketDistributor.sendToServer(new CourierChoiceSelectPayload(
                deliveryId,
                optionIndex
        ));
        onClose();
    }

    private static ItemStack stackFor(String itemId) {
        return new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse(itemId)
        ));
    }
}
