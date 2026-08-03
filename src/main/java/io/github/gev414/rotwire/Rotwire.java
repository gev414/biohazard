package io.github.gev414.rotwire;

import io.github.gev414.rotwire.attachment.ModAttachments;
import io.github.gev414.rotwire.block.ModBlocks;
import io.github.gev414.rotwire.block.entity.ModBlockEntities;
import io.github.gev414.rotwire.city.InfectedCityScaling;
import io.github.gev414.rotwire.combat.KnockbackManager;
import io.github.gev414.rotwire.config.CityOperationsConfig;
import io.github.gev414.rotwire.config.EncounterConfig;
import io.github.gev414.rotwire.config.HordeAtmosphereConfig;
import io.github.gev414.rotwire.config.MobSpawnConfig;
import io.github.gev414.rotwire.config.RadioQuestConfig;
import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import io.github.gev414.rotwire.config.WeatherConfig;
import io.github.gev414.rotwire.entity.ModEntities;
import io.github.gev414.rotwire.effect.ModEffects;
import io.github.gev414.rotwire.event.EncounterEvents;
import io.github.gev414.rotwire.event.HordeAtmosphereSyncEvents;
import io.github.gev414.rotwire.event.ModEntityEvents;
import io.github.gev414.rotwire.event.ModCreativeTabEvents;
import io.github.gev414.rotwire.event.SurvivalSystemsEvents;
import io.github.gev414.rotwire.event.SurvivorEvents;
import io.github.gev414.rotwire.item.ModItems;
import io.github.gev414.rotwire.lostcities.LostCitiesIntegration;
import io.github.gev414.rotwire.lostcities.LostCitiesOvergrowth;
import io.github.gev414.rotwire.loot.HandcraftedStorageLoot;
import io.github.gev414.rotwire.menu.ModMenus;
import io.github.gev414.rotwire.mob.MobSpawnRestrictions;
import io.github.gev414.rotwire.mob.SurfaceZombieSpawner;
import io.github.gev414.rotwire.network.ModPayloads;
import io.github.gev414.rotwire.quest.FTBQuestsIntegration;
import io.github.gev414.rotwire.quest.QuestDefaultsInstaller;
import io.github.gev414.rotwire.quest.delivery.DeliveryManager;
import io.github.gev414.rotwire.settlement.SettlementManager;
import io.github.gev414.rotwire.weather.WeatherCommands;
import io.github.gev414.rotwire.weather.WeatherManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Rotwire.MOD_ID)
public final class Rotwire {

    public static final String MOD_ID = "rotwire";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Rotwire(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModMenus.MENU_TYPES.register(modEventBus);
        modEventBus.addListener(ModCreativeTabEvents::buildContents);
        modEventBus.addListener(ModEntityEvents::registerAttributes);
        modEventBus.addListener(ModPayloads::register);

        EncounterConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                EncounterConfig.SPEC,
                "rotwire-encounters.toml"
        );
        CityOperationsConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                CityOperationsConfig.SPEC,
                "rotwire-city-operations.toml"
        );
        MobSpawnConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                MobSpawnConfig.SPEC,
                "rotwire-mobs.toml"
        );
        HordeAtmosphereConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.CLIENT,
                HordeAtmosphereConfig.SPEC,
                "rotwire-client.toml"
        );
        RadioQuestConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                RadioQuestConfig.SPEC,
                "rotwire-radio-quests.toml"
        );
        SettlementConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                SettlementConfig.SPEC,
                "rotwire-settlements.toml"
        );
        SurvivalSystemsConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                SurvivalSystemsConfig.SPEC,
                "rotwire-survival.toml"
        );
        WeatherConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                WeatherConfig.SPEC,
                "rotwire-weather.toml"
        );

        QuestDefaultsInstaller.installIfMissing();
        FTBQuestsIntegration.initialize();

        LostCitiesIntegration.initialize(modEventBus);
        LostCitiesOvergrowth.initialize();
        NeoForge.EVENT_BUS.addListener(EncounterEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                EncounterEvents::onLivingDeath
        );
        NeoForge.EVENT_BUS.addListener(SurvivorEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EncounterEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(
                InfectedCityScaling::onEntityJoinLevel
        );
        NeoForge.EVENT_BUS.addListener(
                SurfaceZombieSpawner::onServerTick
        );
        NeoForge.EVENT_BUS.addListener(
                MobSpawnRestrictions::onSpawnPlacementCheck
        );
        NeoForge.EVENT_BUS.addListener(
                HandcraftedStorageLoot::onBlockPlaced
        );
        NeoForge.EVENT_BUS.addListener(
                HordeAtmosphereSyncEvents::onServerTick
        );
        NeoForge.EVENT_BUS.addListener(
                HordeAtmosphereSyncEvents::onPlayerLoggedIn
        );
        NeoForge.EVENT_BUS.addListener(
                HordeAtmosphereSyncEvents::onPlayerLoggedOut
        );
        NeoForge.EVENT_BUS.addListener(
                HordeAtmosphereSyncEvents::onServerStopped
        );
        NeoForge.EVENT_BUS.addListener(DeliveryManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::onServerStopped);
        NeoForge.EVENT_BUS.addListener(SettlementManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(WeatherCommands::register);
        NeoForge.EVENT_BUS.addListener(WeatherManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(WeatherManager::onServerStopped);
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onServerTick
        );
        NeoForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                SurvivalSystemsEvents::onLivingChangeTarget
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onIncomingDamage
        );
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                KnockbackManager::onLivingKnockBack
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onBlockBreak
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onSoundAtPosition
        );
        NeoForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                SurvivalSystemsEvents::onEntityJoinLevel
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onPlayerLoggedIn
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onPlayerLoggedOut
        );
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                SurvivalSystemsEvents::onCanPlayerSleep
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onSleepFinished
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onPlayerWakeUp
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onServerStopped
        );
    }
}
