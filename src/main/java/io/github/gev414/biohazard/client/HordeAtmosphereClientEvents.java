package io.github.gev414.biohazard.client;

import io.github.gev414.biohazard.config.HordeAtmosphereConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class HordeAtmosphereClientEvents {

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!HordeAtmosphereConfig.ENABLED.get()
                || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getType() != FogType.NONE) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }

        HordeAtmosphereState.Snapshot state =
                HordeAtmosphereState.current();
        float strength = HordeAtmosphereFog.strength(
                state.hordeDay(),
                state.active(),
                level.getDayTime(),
                state.dayLength(),
                state.hordeStartTime(),
                HordeAtmosphereConfig.FADE_DURATION_TICKS.get()
        );
        if (strength <= 0.0F) {
            return;
        }

        float targetFar = HordeAtmosphereConfig.TARGET_FAR_PLANE
                .get()
                .floatValue();
        float targetNear = Math.min(
                HordeAtmosphereConfig.TARGET_NEAR_PLANE
                        .get()
                        .floatValue(),
                targetFar
        );
        float farPlane = HordeAtmosphereFog.blendTowardCloserPlane(
                event.getFarPlaneDistance(),
                targetFar,
                strength
        );
        float nearPlane = HordeAtmosphereFog.blendTowardCloserPlane(
                event.getNearPlaneDistance(),
                targetNear,
                strength
        );

        if (farPlane == event.getFarPlaneDistance()
                && nearPlane == event.getNearPlaneDistance()) {
            return;
        }

        event.setFarPlaneDistance(farPlane);
        event.setNearPlaneDistance(nearPlane);
        event.setCanceled(true);
    }

    public static void onLoggingOut(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        HordeAtmosphereState.reset();
    }

    private HordeAtmosphereClientEvents() {
    }
}
