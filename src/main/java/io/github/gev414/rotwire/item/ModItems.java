package io.github.gev414.rotwire.item;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.block.ModBlocks;
import io.github.gev414.rotwire.camp.CampModuleType;
import io.github.gev414.rotwire.settlement.SettlementUpgrade;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Rotwire.MOD_ID);

    public static final Supplier<BlockItem> RADIO_TRANSMITTER =
            ITEMS.register(
                    "radio_transmitter",
                    () -> new BlockItem(
                            ModBlocks.RADIO_TRANSMITTER.get(),
                            new Item.Properties()
                    )
            );
    public static final Supplier<TarpItem> TARP = ITEMS.register(
            "tarp",
            () -> new TarpItem(
                    ModBlocks.DEPLOYED_TARP.get(),
                    new Item.Properties().stacksTo(1)
            )
    );

    public static final Supplier<Item> DOCUMENTS = ITEMS.registerSimpleItem(
            "documents",
            new Item.Properties().stacksTo(64)
    );
    public static final Supplier<Item> RESEARCH_DATA = ITEMS.registerSimpleItem(
            "research_data",
            new Item.Properties().stacksTo(32)
    );
    public static final Supplier<Item> ENCRYPTED_INTEL = ITEMS.registerSimpleItem(
            "encrypted_intel",
            new Item.Properties().stacksTo(16)
    );
    public static final Supplier<SurvivorGunDisplayItem>
            SURVIVOR_FIREARM_DISPLAY = ITEMS.register(
                    "survivor_firearm_display",
                    () -> new SurvivorGunDisplayItem(
                            new Item.Properties().stacksTo(1)
                    )
            );
    public static final Supplier<CampModuleItem> QUARTERMASTER_CACHE_MODULE =
            module(
                    "quartermaster_cache_module",
                    CampModuleType.STORAGE
            );
    public static final Supplier<CampModuleItem> FIELD_WORKSHOP_MODULE =
            module(
                    "field_workshop_module",
                    CampModuleType.CRAFTING
            );
    public static final Supplier<CampModuleItem> OPERATIONS_RELAY_MODULE =
            module(
                    "operations_relay_module",
                    CampModuleType.OPERATIONS
            );
    public static final Supplier<SettlementUpgradeItem> CAMP_HUB_MODULE =
            settlementUpgrade("camp_hub_module", SettlementUpgrade.CAMP_HUB);
    public static final Supplier<Item> FIELD_REPAIR_KIT =
            ITEMS.registerSimpleItem(
                    "field_repair_kit",
                    new Item.Properties().stacksTo(16)
            );
    public static final Supplier<Item> INFECTION_CURE = ITEMS.register(
            "infection_cure",
            () -> new InfectionMedicineItem(
                    InfectionMedicineItem.Kind.FULL_CURE,
                    new Item.Properties().stacksTo(4).rarity(Rarity.EPIC)
            )
    );
    public static final Supplier<Item> ANTIVIRAL_SUPPRESSANT = ITEMS.register(
            "antiviral_suppressant",
            () -> new InfectionMedicineItem(
                    InfectionMedicineItem.Kind.SUPPRESSANT,
                    new Item.Properties().stacksTo(8).rarity(Rarity.RARE)
            )
    );

    public static Item moduleItem(CampModuleType type) {
        return switch (type) {
            case STORAGE -> QUARTERMASTER_CACHE_MODULE.get();
            case CRAFTING -> FIELD_WORKSHOP_MODULE.get();
            case OPERATIONS -> OPERATIONS_RELAY_MODULE.get();
        };
    }

    private static Supplier<CampModuleItem> module(
            String name,
            CampModuleType type
    ) {
        return ITEMS.register(
                name,
                () -> new CampModuleItem(
                        type,
                        new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                )
        );
    }

    private static Supplier<SettlementUpgradeItem> settlementUpgrade(
            String name,
            SettlementUpgrade upgrade
    ) {
        return ITEMS.register(
                name,
                () -> new SettlementUpgradeItem(
                        upgrade,
                        new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                )
        );
    }

    private ModItems() {
    }
}
