package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.entity.ModEntities;
import io.github.gev414.rotwire.client.renderer.BruteRenderer;
import io.github.gev414.rotwire.item.ModItems;
import io.github.gev414.rotwire.effect.ModEffects;
import io.github.gev414.rotwire.menu.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

@EventBusSubscriber(
        modid = Rotwire.MOD_ID,
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
            NeoForge.EVENT_BUS.addListener(
                    ClientModEvents::onLoggingOut
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

    @SubscribeEvent
    public static void registerEffectIcons(
            RegisterClientExtensionsEvent event
    ) {
        event.registerMobEffect(
                new VanillaEffectIcon(MobEffects.HUNGER),
                ModEffects.RESTLESS_SLEEP
        );
        event.registerMobEffect(
                new ItemEffectIcon(Items.COOKED_BEEF),
                ModEffects.NEW_DAWN
        );
        event.registerMobEffect(
                new ItemEffectIcon(Items.CAMPFIRE),
                ModEffects.PREPARED_SHELTER
        );
    }

    @SubscribeEvent
    public static void registerMenuScreens(
            RegisterMenuScreensEvent event
    ) {
        event.register(
                ModMenus.CAMP_RADIO.get(),
                CampRadioScreen::new
        );
        event.register(
                ModMenus.CAMP_STORAGE.get(),
                CampStorageScreen::new
        );
    }

    @SubscribeEvent
    public static void addEffectLore(
            GatherEffectScreenTooltipsEvent event
    ) {
        if (event.getEffectInstance().getEffect().value()
                == ModEffects.PREPARED_SHELTER.get()) {
            event.getTooltip().add(
                    net.minecraft.network.chat.Component.translatable(
                            "effect.rotwire.prepared_shelter.description"
                    ).withStyle(net.minecraft.ChatFormatting.GRAY)
            );
        }
    }

    @SubscribeEvent
    public static void renderCityStatus(ScreenEvent.Render.Post event) {
        CityStatusClient.render(
                event.getScreen(),
                event.getGuiGraphics(),
                event.getMouseX(),
                event.getMouseY()
        );
        RadioHordeStatusClient.render(
                event.getScreen(),
                event.getGuiGraphics()
        );
        WeatherForecastClient.render(
                event.getScreen(),
                event.getGuiGraphics(),
                event.getMouseX(),
                event.getMouseY()
        );
        if (event.getScreen() instanceof InventoryScreen inventory) {
            InventoryEncumbranceClient.render(
                    inventory,
                    event.getGuiGraphics(),
                    event.getMouseX(),
                    event.getMouseY()
            );
        }
    }

    @SubscribeEvent
    public static void renderSurvivalStatus(RenderGuiEvent.Post event) {
        SurvivalStatusClient.render(event.getGuiGraphics());
        WeatherExposureClient.render(event.getGuiGraphics());
    }

    private static void onLoggingOut(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        SurvivalStatusClient.reset();
        CityStatusClient.clear();
        WeatherForecastClient.clear();
        WeatherExposureClient.reset();
    }

    @SubscribeEvent
    public static void toggleCityStatus(
            ScreenEvent.MouseButtonPressed.Pre event
    ) {
        if (CityStatusClient.handleMouseClick(
                event.getScreen(),
                event.getMouseX(),
                event.getMouseY(),
                event.getButton()
        )) {
            event.setCanceled(true);
            return;
        }
        if (WeatherForecastClient.handleMouseClick(
                event.getScreen(),
                event.getMouseX(),
                event.getMouseY(),
                event.getButton()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void closeCityStatus(ScreenEvent.Closing event) {
        if (CityStatusClient.isQuestScreen(event.getScreen())) {
            CityStatusClient.clear();
            WeatherForecastClient.clear();
        }
    }

    private ClientModEvents() {
    }

    private record VanillaEffectIcon(
            Holder<MobEffect> source
    ) implements IClientMobEffectExtensions {

        @Override
        public boolean renderInventoryIcon(
                MobEffectInstance instance,
                EffectRenderingInventoryScreen<?> screen,
                GuiGraphics graphics,
                int x,
                int y,
                int blitOffset
        ) {
            graphics.blit(
                    x,
                    y + 7,
                    blitOffset,
                    18,
                    18,
                    sprite()
            );
            return true;
        }

        @Override
        public boolean renderGuiIcon(
                MobEffectInstance instance,
                Gui gui,
                GuiGraphics graphics,
                int x,
                int y,
                float z,
                float alpha
        ) {
            graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            graphics.blit(x + 3, y + 3, (int) z, 18, 18, sprite());
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            return true;
        }

        private TextureAtlasSprite sprite() {
            return Minecraft.getInstance()
                    .getMobEffectTextures()
                    .get(source);
        }
    }

    private record ItemEffectIcon(
            Item item
    ) implements IClientMobEffectExtensions {

        @Override
        public boolean renderInventoryIcon(
                MobEffectInstance instance,
                EffectRenderingInventoryScreen<?> screen,
                GuiGraphics graphics,
                int x,
                int y,
                int blitOffset
        ) {
            graphics.renderItem(new ItemStack(item), x + 1, y + 8);
            return true;
        }

        @Override
        public boolean renderGuiIcon(
                MobEffectInstance instance,
                Gui gui,
                GuiGraphics graphics,
                int x,
                int y,
                float z,
                float alpha
        ) {
            graphics.renderItem(new ItemStack(item), x + 4, y + 4);
            return true;
        }
    }
}
