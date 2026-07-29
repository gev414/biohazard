package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.network.WeatherExposurePayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class WeatherExposureClient {

    private static final int VIGNETTE_LAYERS = 14;
    private static final int LAYER_WIDTH = 3;
    private static final float FADE_PER_SECOND = 4.5F;

    private static volatile WeatherExposurePayload state =
            WeatherExposurePayload.clear();
    private static float opacity;
    private static long lastRenderTime;

    public static void update(WeatherExposurePayload payload) {
        state = payload;
    }

    public static void reset() {
        state = WeatherExposurePayload.clear();
        opacity = 0.0F;
        lastRenderTime = 0L;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            reset();
            return;
        }

        long now = Util.getMillis();
        float elapsed = lastRenderTime == 0L
                ? 0.0F
                : Math.min(0.1F, (now - lastRenderTime) / 1_000.0F);
        lastRenderTime = now;
        float target = state.exposed() ? 1.0F : 0.0F;
        opacity = Mth.approach(
                opacity,
                target,
                FADE_PER_SECOND * elapsed
        );
        if (opacity <= 0.001F) {
            return;
        }

        float pulse = state.harmful()
                ? 0.82F + 0.18F * (float) Math.sin(now / 110.0D)
                : 0.92F + 0.08F * (float) Math.sin(now / 240.0D);
        int red = state.harmful() ? 139 : 83;
        int green = state.harmful() ? 54 : 104;
        int blue = state.harmful() ? 31 : 48;
        drawVignette(
                graphics,
                red,
                green,
                blue,
                opacity * pulse
        );

        if (!minecraft.options.hideGui && state.exposed()) {
            Component warning = Component.translatable(
                    state.storm()
                            ? "hud.rotwire.weather.exposed_storm"
                            : "hud.rotwire.weather.exposed_rain"
            );
            int color = state.harmful() ? 0xF0A06A : 0xD8CF86;
            graphics.drawCenteredString(
                    minecraft.font,
                    warning,
                    graphics.guiWidth() / 2,
                    graphics.guiHeight() - 72,
                    color
            );
        }
    }

    private static void drawVignette(
            GuiGraphics graphics,
            int red,
            int green,
            int blue,
            float strength
    ) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (int layer = 0; layer < VIGNETTE_LAYERS; layer++) {
            float fraction = 1.0F
                    - layer / (float) VIGNETTE_LAYERS;
            int alpha = Math.round(
                    110.0F * strength * fraction * fraction
            );
            int color = alpha << 24
                    | red << 16
                    | green << 8
                    | blue;
            int inset = layer * LAYER_WIDTH;
            int thickness = LAYER_WIDTH + 1;
            graphics.fill(
                    inset,
                    inset,
                    width - inset,
                    inset + thickness,
                    color
            );
            graphics.fill(
                    inset,
                    height - inset - thickness,
                    width - inset,
                    height - inset,
                    color
            );
            graphics.fill(
                    inset,
                    inset + thickness,
                    inset + thickness,
                    height - inset - thickness,
                    color
            );
            graphics.fill(
                    width - inset - thickness,
                    inset + thickness,
                    width - inset,
                    height - inset - thickness,
                    color
            );
        }
    }

    private WeatherExposureClient() {
    }
}
