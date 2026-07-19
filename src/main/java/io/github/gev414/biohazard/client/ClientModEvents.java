package io.github.gev414.biohazard.client;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.entity.ModEntities;
import io.github.gev414.biohazard.client.renderer.BruteRenderer;
import io.github.gev414.biohazard.item.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

@EventBusSubscriber(
        modid = Biohazard.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientModEvents {

    private static final int REGENERATION_PINK = 0xCD5CAB;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            NeoForge.EVENT_BUS.addListener(
                    HordeAtmosphereClientEvents::onRenderFog
            );
            NeoForge.EVENT_BUS.addListener(
                    HordeAtmosphereClientEvents::onLoggingOut
            );
        });
    }

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

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? REGENERATION_PINK : 0xFFFFFFFF,
                ModItems.ANTIVIRAL_SUPPRESSANT.get()
        );
    }

    private ClientModEvents() {
    }
}
