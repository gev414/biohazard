package io.github.gev414.rotwire.settlement;

import io.github.gev414.rotwire.Rotwire;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Data-driven limits for the small amount of block damage a siege may cause. */
public final class SettlementBlockTags {

    public static final TagKey<Block> SIEGE_BREAKABLE = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(
                    Rotwire.MOD_ID,
                    "siege_breakable"
            )
    );
    public static final TagKey<Block> SIEGE_BREACH_FRAGILE = siegeTag(
            "siege_breach_fragile"
    );
    public static final TagKey<Block> SIEGE_BREACH_REINFORCED = siegeTag(
            "siege_breach_reinforced"
    );
    public static final TagKey<Block> SIEGE_UNBREAKABLE = siegeTag(
            "siege_unbreakable"
    );

    private static TagKey<Block> siegeTag(String path) {
        return TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(Rotwire.MOD_ID, path)
        );
    }

    private SettlementBlockTags() {
    }
}
