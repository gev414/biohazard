package io.github.gev414.rotwire.settlement;

import net.minecraft.resources.ResourceLocation;

/** Ammunition types that physical camp stockpiles can supply to survivors. */
public enum SettlementAmmunition {

    MOSIN_762X51("ammo762x51"),
    PISTOL_45_ACP("ammo45acp"),
    SHOTGUN_12_GAUGE("ammo12gauge");

    private final ResourceLocation itemId;

    SettlementAmmunition(String itemPath) {
        itemId = ResourceLocation.fromNamespaceAndPath(
                "pointblank",
                itemPath
        );
    }

    ResourceLocation itemId() {
        return itemId;
    }
}
