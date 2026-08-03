package io.github.gev414.rotwire.client.renderer;

import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * Uses Minecraft's own player geometry and default skin rotation so civilians
 * read as ordinary people without bundling or impersonating player profiles.
 */
public final class SurvivorRenderer extends
        HumanoidMobRenderer<SurvivorEntity, PlayerModel<SurvivorEntity>> {

    public SurvivorRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new SurvivorModel(context.bakeLayer(ModelLayers.PLAYER)),
                0.5F
        );
        addLayer(new SurvivorItemInHandLayer(
                this,
                context.getItemInHandRenderer()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(SurvivorEntity survivor) {
        return DefaultPlayerSkin.get(survivor.getUUID()).texture();
    }
}
