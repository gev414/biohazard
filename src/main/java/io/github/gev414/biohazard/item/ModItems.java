package io.github.gev414.biohazard.item;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Biohazard.MOD_ID);

    public static final Supplier<BlockItem> RADIO_TRANSMITTER =
            ITEMS.register(
                    "radio_transmitter",
                    () -> new BlockItem(
                            ModBlocks.RADIO_TRANSMITTER.get(),
                            new Item.Properties()
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

    private ModItems() {
    }
}
