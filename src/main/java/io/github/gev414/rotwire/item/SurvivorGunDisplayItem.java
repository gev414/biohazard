package io.github.gev414.rotwire.item;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Harmless GeckoLib host used only to draw PointBlank's static gun geometry
 * on survivor models. It deliberately is not a PointBlank GunItem, preventing
 * PointBlank from registering an NPC weapon as player inventory state -1.
 */
public final class SurvivorGunDisplayItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public SurvivorGunDisplayItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        // Survivors use a static third-person gun pose.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
