package io.github.gev414.rotwire.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gev414.rotwire.Rotwire;
import mod.pbj.Config;
import mod.pbj.client.BiDirectionalInterpolator;
import mod.pbj.client.GunClientState;
import mod.pbj.client.render.RenderUtil;
import mod.pbj.feature.AimingFeature;
import mod.pbj.feature.PipFeature;
import mod.pbj.item.GunItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Supplies Point Blank's missing non-PIP fallback for attachment scopes.
 *
 * <p>Point Blank 2.2.0 falls back to its normal camera and scope overlay when
 * a gun declares {@code pipScopeZoom} directly. Magnified attachment scopes
 * declare a {@link PipFeature} instead, so disabling PIP leaves their scope
 * model in front of the camera and uses only their low ADS zoom. This handler
 * applies the same fallback behavior to those attachment scopes.</p>
 */
@EventBusSubscriber(
        modid = Rotwire.MOD_ID,
        value = Dist.CLIENT
)
public final class PointBlankScopeCompat {

    private static final ResourceLocation SCOPE_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(
                    "pointblank",
                    "textures/gui/scope.png"
            );
    private static final float SCOPE_VIEW_START_PROGRESS = 0.70F;
    private static final float HAND_HIDE_PROGRESS = 0.92F;
    private static final double OVERLAY_SCALE = 3.3D;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void usePipMagnificationOnMainCamera(
            ViewportEvent.ComputeFov event
    ) {
        ScopeContext context = activeScope();
        if (context == null || context.aimingProgress() <= 0.0F) {
            return;
        }

        float scopeProgress = scopeViewProgress(context.aimingProgress());
        if (scopeProgress <= 0.0F) {
            return;
        }

        float aimingZoom = AimingFeature.getZoom(context.itemStack());
        event.setFOV(ScopeFovMath.remap(
                event.getFOV(),
                aimingZoom,
                context.pipFeature().getZoom(),
                context.aimingProgress(),
                scopeProgress
        ));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideObstructingScopeModel(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ScopeContext context = activeScope();
        if (context != null
                && context.aimingProgress() > HAND_HIDE_PROGRESS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderScopeOverlay(RenderGuiEvent.Pre event) {
        ScopeContext context = activeScope();
        if (context == null || context.aimingProgress() <= 0.0F) {
            return;
        }

        float scopeProgress = scopeViewProgress(context.aimingProgress());
        if (scopeProgress <= 0.0F) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int renderWidth = (int) (screenWidth * OVERLAY_SCALE);
        int renderHeight = renderWidth;
        if (renderHeight > screenHeight) {
            renderHeight = (int) (screenHeight * OVERLAY_SCALE);
            renderWidth = renderHeight;
        }

        float x = (screenWidth - renderWidth) / 2.0F;
        float y = (screenHeight - renderHeight) / 2.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        graphics.setColor(1.0F, 1.0F, 1.0F, scopeProgress);
        RenderUtil.blit(
                graphics,
                SCOPE_OVERLAY,
                x,
                y,
                -90,
                0.0F,
                0.0F,
                renderWidth,
                renderHeight,
                renderWidth,
                renderHeight
        );
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static float scopeViewProgress(float aimingProgress) {
        return ScopeFovMath.delayedProgress(
                aimingProgress,
                SCOPE_VIEW_START_PROGRESS
        );
    }

    private static ScopeContext activeScope() {
        if (Config.pipScopesEnabled) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return null;
        }

        ItemStack itemStack = minecraft.player.getMainHandItem();
        if (!(itemStack.getItem() instanceof GunItem)) {
            return null;
        }

        PipFeature pipFeature = PipFeature.getSelected(itemStack);
        if (pipFeature == null) {
            return null;
        }

        GunClientState state = GunClientState.getMainHeldState(minecraft.player);
        if (state == null
                || !(state.getAnimationController("aiming")
                instanceof BiDirectionalInterpolator aiming)) {
            return null;
        }

        float progress = Mth.clamp((float) aiming.getValue(), 0.0F, 1.0F);
        return new ScopeContext(itemStack, pipFeature, progress);
    }

    private PointBlankScopeCompat() {
    }

    private record ScopeContext(
            ItemStack itemStack,
            PipFeature pipFeature,
            float aimingProgress
    ) {
    }
}
