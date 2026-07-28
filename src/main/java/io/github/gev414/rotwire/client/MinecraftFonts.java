package io.github.gev414.rotwire.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

final class MinecraftFonts {

    static Font font(GuiGraphics graphics) {
        return Minecraft.getInstance().font;
    }

    private MinecraftFonts() {
    }
}
