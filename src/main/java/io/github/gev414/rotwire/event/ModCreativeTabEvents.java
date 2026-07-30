package io.github.gev414.rotwire.event;

import io.github.gev414.rotwire.item.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class ModCreativeTabEvents {

    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.RADIO_TRANSMITTER.get());
            event.accept(ModItems.TARP.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.DOCUMENTS.get());
            event.accept(ModItems.RESEARCH_DATA.get());
            event.accept(ModItems.ENCRYPTED_INTEL.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.ANTIVIRAL_SUPPRESSANT.get());
            event.accept(ModItems.INFECTION_CURE.get());
        }
    }

    private ModCreativeTabEvents() {
    }
}
