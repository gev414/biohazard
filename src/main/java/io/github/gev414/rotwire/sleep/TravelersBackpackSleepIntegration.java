package io.github.gev414.rotwire.sleep;

import com.tiviacz.travelersbackpack.blocks.SleepingBagBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Isolated so servers without Traveler's Backpack never load its classes.
 */
final class TravelersBackpackSleepIntegration {

    static boolean isSleepingBag(BlockState state) {
        return state.getBlock() instanceof SleepingBagBlock;
    }

    private TravelersBackpackSleepIntegration() {
    }
}
