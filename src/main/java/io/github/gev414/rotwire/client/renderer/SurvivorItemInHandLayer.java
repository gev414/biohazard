package io.github.gev414.rotwire.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import io.github.gev414.rotwire.item.ModItems;
import io.github.gev414.rotwire.item.SurvivorGunDisplayItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Held-item layer with a PointBlank NPC compatibility path. PointBlank's
 * normal renderer is bound to a real player's inventory and render state, so
 * NPCs render the mod's existing Mosin geometry and texture through a small,
 * static GeckoLib renderer instead.
 */
public final class SurvivorItemInHandLayer extends ItemInHandLayer<
        SurvivorEntity,
        PlayerModel<SurvivorEntity>> {

    private static final float MODEL_SCALE = 0.25F;
    private static final float MODEL_Y_OFFSET = -4.0F / 16.0F;
    private static final float MODEL_Z_OFFSET = 1.5F / 16.0F;
    private static final float RIFLE_AIM_ELEVATION_DEGREES = -12.0F;

    private final NpcGunRenderer mosinRenderer = new NpcGunRenderer("mosin");
    private final NpcGunRenderer pistolRenderer = new NpcGunRenderer("m1911a1");
    private final NpcGunRenderer shotgunRenderer = new NpcGunRenderer("m870");
    private ItemStack firearmDisplayStack = ItemStack.EMPTY;

    public SurvivorItemInHandLayer(
            RenderLayerParent<
                    SurvivorEntity,
                    PlayerModel<SurvivorEntity>> parent,
            ItemInHandRenderer itemInHandRenderer
    ) {
        super(parent, itemInHandRenderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            SurvivorEntity survivor,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        ItemStack displayStack = displayStackFor(survivor);
        if (displayStack.isEmpty()) {
            return;
        }
        renderArmWithItem(
                survivor,
                displayStack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                survivor.getMainArm(),
                poseStack,
                buffer,
                packedLight
        );
    }

    @Override
    protected void renderArmWithItem(
            LivingEntity livingEntity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (!(livingEntity instanceof SurvivorEntity survivor)
                || !survivor.hasFirearm()) {
            super.renderArmWithItem(
                    livingEntity,
                    itemStack,
                    displayContext,
                    arm,
                    poseStack,
                    buffer,
                    packedLight
            );
            return;
        }

        poseStack.pushPose();
        getParentModel().translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // PointBlank's third-person Mosin rests slightly low by default.
        // Raise its muzzle toward a torso-height neutral firing line.
        poseStack.mulPose(Axis.XP.rotationDegrees(
                RIFLE_AIM_ELEVATION_DEGREES
        ));
        boolean leftHand = arm == HumanoidArm.LEFT;
        poseStack.translate(
                (float) (leftHand ? -1 : 1) / 16.0F,
                0.125F,
                -0.625F
        );

        // Reproduce PointBlank's third-person-right-hand item transform. The
        // direct renderer intentionally bypasses its player-only state path.
        poseStack.translate(
                0.0F,
                MODEL_Y_OFFSET,
                MODEL_Z_OFFSET
        );
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        rendererFor(survivor).renderByItem(
                itemStack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
    }

    /**
     * Renders only the physical rifle from PointBlank's own resources. Bones
     * used for first-person arms, attachment markers, and reload animations
     * must not appear on the survivor model.
     */
    private NpcGunRenderer rendererFor(SurvivorEntity survivor) {
        ResourceLocation itemId = survivor.firearmItemId();
        if (itemId == null) {
            return mosinRenderer;
        }
        if (itemId.getPath().equals("m1911a1")) {
            return pistolRenderer;
        }
        if (itemId.getPath().equals("m870")) {
            return shotgunRenderer;
        }
        return mosinRenderer;
    }

    private ItemStack displayStackFor(SurvivorEntity survivor) {
        if (!survivor.hasFirearm()) {
            return ItemStack.EMPTY;
        }
        if (firearmDisplayStack.isEmpty()) {
            firearmDisplayStack = new ItemStack(
                    ModItems.SURVIVOR_FIREARM_DISPLAY.get()
            );
        }
        return firearmDisplayStack;
    }

    private static final class NpcGunRenderer
            extends GeoItemRenderer<SurvivorGunDisplayItem> {

        private static final String[] HIDDEN_BONES = {
                "rightarm",
                "leftarm",
                "_camera_",
                "_cb_scope",
                "scope",
                "sniper",
                "muzzleflash",
                "muzzleflash2",
                "muzzleflash3",
                "clip",
                "bullets",
                "bullet",
                "bullet2",
                "bullet3"
        };

        private NpcGunRenderer(String itemPath) {
            super(new DefaultedItemGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "pointblank",
                            itemPath
                    )
            ));
        }

        @Override
        public void preRender(
                PoseStack poseStack,
                SurvivorGunDisplayItem displayItem,
                BakedGeoModel model,
                MultiBufferSource buffer,
                VertexConsumer vertexConsumer,
                boolean isReRender,
                float partialTick,
                int packedLight,
                int packedOverlay,
                int renderColor
        ) {
            for (String boneName : HIDDEN_BONES) {
                model.getBone(boneName).ifPresent(bone -> {
                    bone.setHidden(true);
                    bone.setChildrenHidden(true);
                });
            }
            super.preRender(
                    poseStack,
                    displayItem,
                    model,
                    buffer,
                    vertexConsumer,
                    isReRender,
                    partialTick,
                    packedLight,
                    packedOverlay,
                    renderColor
            );
        }
    }
}
