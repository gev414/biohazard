package io.github.gev414.biohazard.encumbrance;

import io.github.gev414.biohazard.Biohazard;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {

    public static final TagKey<Item> WEIGHTLESS = tag("encumbrance/weightless");
    public static final TagKey<Item> LIGHT = tag("encumbrance/light");
    public static final TagKey<Item> HEAVY = tag("encumbrance/heavy");
    public static final TagKey<Item> VERY_HEAVY = tag(
            "encumbrance/very_heavy"
    );

    private static TagKey<Item> tag(String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(
                        Biohazard.MOD_ID,
                        path
                )
        );
    }

    private ModItemTags() {
    }
}
