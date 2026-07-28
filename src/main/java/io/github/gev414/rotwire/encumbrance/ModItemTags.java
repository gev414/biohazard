package io.github.gev414.rotwire.encumbrance;

import io.github.gev414.rotwire.Rotwire;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {

    public static final TagKey<Item> WEIGHTLESS = tag("encumbrance/weightless");
    public static final TagKey<Item> TINY = tag("encumbrance/tiny");
    public static final TagKey<Item> LIGHT = tag("encumbrance/light");
    public static final TagKey<Item> DENSE = tag("encumbrance/dense");
    public static final TagKey<Item> VERY_DENSE = tag(
            "encumbrance/very_dense"
    );
    public static final TagKey<Item> LIGHT_EQUIPMENT = tag(
            "encumbrance/equipment/light"
    );
    public static final TagKey<Item> HEAVY_EQUIPMENT = tag(
            "encumbrance/equipment/heavy"
    );

    private static TagKey<Item> tag(String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(
                        Rotwire.MOD_ID,
                        path
                )
        );
    }

    private ModItemTags() {
    }
}
