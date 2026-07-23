package io.github.gev414.biohazard.encumbrance;

import com.tiviacz.travelersbackpack.capability.AttachmentUtils;
import com.tiviacz.travelersbackpack.components.BackpackContainerContents;
import com.tiviacz.travelersbackpack.components.Fluids;
import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.items.TravelersBackpackItem;
import io.github.gev414.biohazard.config.SurvivalSystemsConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.ToDoubleFunction;

/**
 * Kept in its own class so a server without Traveler's Backpack never has to
 * load any of the optional mod's classes.
 */
final class TravelersBackpackIntegration {

    static boolean isBackpack(ItemStack stack) {
        return stack.getItem() instanceof TravelersBackpackItem;
    }

    static double equippedWeight(
            Player player,
            ToDoubleFunction<ItemStack> itemWeight
    ) {
        return AttachmentUtils.getAttachment(player)
                .filter(attachment -> attachment.hasBackpack())
                .map(attachment -> backpackWeight(
                        attachment.getBackpack(),
                        itemWeight
                ))
                .orElse(0.0D);
    }

    static double backpackWeight(
            ItemStack backpack,
            ToDoubleFunction<ItemStack> itemWeight
    ) {
        if (backpack.isEmpty()) {
            return 0.0D;
        }

        double weight = SurvivalSystemsConfig.BACKPACK_BASE_WEIGHT.get();
        weight += contentsWeight(
                backpack.getOrDefault(
                        ModDataComponents.BACKPACK_CONTAINER.get(),
                        BackpackContainerContents.EMPTY
                ),
                itemWeight
        );
        weight += contentsWeight(
                backpack.getOrDefault(
                        ModDataComponents.TOOLS_CONTAINER.get(),
                        BackpackContainerContents.EMPTY
                ),
                itemWeight
        );
        weight += contentsWeight(
                backpack.getOrDefault(
                        ModDataComponents.UPGRADES.get(),
                        BackpackContainerContents.EMPTY
                ),
                itemWeight
        );

        Fluids fluids = backpack.getOrDefault(
                ModDataComponents.FLUIDS.get(),
                Fluids.empty()
        );
        long fluidAmount = (long) fluids.leftFluidStack().getAmount()
                + fluids.rightFluidStack().getAmount();
        weight += ((double) fluidAmount / FluidType.BUCKET_VOLUME)
                * SurvivalSystemsConfig
                .BACKPACK_FLUID_WEIGHT_PER_BUCKET
                .get();
        return weight;
    }

    private static double contentsWeight(
            BackpackContainerContents contents,
            ToDoubleFunction<ItemStack> itemWeight
    ) {
        double weight = 0.0D;
        for (ItemStack stack : contents.getItems()) {
            weight += itemWeight.applyAsDouble(stack);
        }
        return weight;
    }

    private TravelersBackpackIntegration() {
    }
}
