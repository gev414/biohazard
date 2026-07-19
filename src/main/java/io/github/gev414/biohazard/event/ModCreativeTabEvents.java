package io.github.gev414.biohazard.event;

import io.github.gev414.biohazard.item.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class ModCreativeTabEvents {

    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.RADIO_TRANSMITTER.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.DOCUMENTS.get());
            event.accept(ModItems.RESEARCH_DATA.get());
            event.accept(ModItems.ENCRYPTED_INTEL.get());
        }
    }

    private ModCreativeTabEvents() {
    }
}
