package io.github.gev414.rotwire.item;

import io.github.gev414.rotwire.camp.CampModuleType;
import net.minecraft.world.item.Item;

public final class CampModuleItem extends Item {

    private final CampModuleType moduleType;

    public CampModuleItem(
            CampModuleType moduleType,
            Properties properties
    ) {
        super(properties);
        this.moduleType = moduleType;
    }

    public CampModuleType moduleType() {
        return moduleType;
    }
}
