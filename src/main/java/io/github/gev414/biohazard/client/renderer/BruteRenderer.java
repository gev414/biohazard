package io.github.gev414.biohazard.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gev414.biohazard.entity.BruteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public final class BruteRenderer extends ZombieRenderer {

    private static final ResourceLocation BRUTE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "biohazard",
                    "textures/entity/brute.png"
            );

    public BruteRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius *= BruteEntity.SCALE;
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return BRUTE_TEXTURE;
    }

    @Override
    protected void scale(
            Zombie entity,
            PoseStack poseStack,
            float partialTickTime
    ) {
        super.scale(entity, poseStack, partialTickTime);

        poseStack.scale(
                BruteEntity.SCALE,
                BruteEntity.SCALE,
                BruteEntity.SCALE
        );
    }
}