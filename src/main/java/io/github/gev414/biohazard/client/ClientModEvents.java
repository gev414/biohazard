package io.github.gev414.biohazard.client;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.entity.ModEntities;
import io.github.gev414.biohazard.client.renderer.BruteRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

@EventBusSubscriber(
        modid = Biohazard.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                ModEntities.BRUTE.get(),
                BruteRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.BRUTE_ROCK.get(),
                context -> new ThrownItemRenderer<>(
                        context,
                        0.5F,
                        false
                )
        );
    }

    private ClientModEvents() {
    }
}