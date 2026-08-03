package io.github.gev414.rotwire.item;

import io.github.gev414.rotwire.settlement.SettlementUpgrade;
import net.minecraft.world.item.Item;

/**
 * A physical installation kit for a city-wide settlement upgrade.
 */
public final class SettlementUpgradeItem extends Item {

    private final SettlementUpgrade upgrade;

    public SettlementUpgradeItem(
            SettlementUpgrade upgrade,
            Properties properties
    ) {
        super(properties);
        this.upgrade = upgrade;
    }

    public SettlementUpgrade upgrade() {
        return upgrade;
    }
}
