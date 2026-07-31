package io.github.gev414.rotwire.menu;

import io.github.gev414.rotwire.block.ModBlocks;
import io.github.gev414.rotwire.block.entity.RadioTransmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CampStorageMenu extends AbstractContainerMenu {

    private static final int CACHE_SLOTS = 27;
    private static final int PLAYER_INVENTORY_SLOTS = 36;

    private final BlockPos radioPosition;

    public CampStorageMenu(
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf buffer
    ) {
        this(
                containerId,
                playerInventory,
                buffer.readBlockPos(),
                new ItemStackHandler(CACHE_SLOTS)
        );
    }

    public CampStorageMenu(
            int containerId,
            Inventory playerInventory,
            RadioTransmitterBlockEntity radio
    ) {
        this(
                containerId,
                playerInventory,
                radio.getBlockPos(),
                radio.cache()
        );
    }

    private CampStorageMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos radioPosition,
            ItemStackHandler cache
    ) {
        super(ModMenus.CAMP_STORAGE.get(), containerId);
        this.radioPosition = radioPosition.immutable();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = column + row * 9;
                addSlot(new SlotItemHandler(
                        cache,
                        slot,
                        8 + column * 18,
                        18 + row * 18
                ));
            }
        }
        addPlayerInventory(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(radioPosition).is(
                ModBlocks.RADIO_TRANSMITTER.get()
        ) && player.distanceToSqr(
                radioPosition.getX() + 0.5D,
                radioPosition.getY() + 0.5D,
                radioPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CACHE_SLOTS) {
            if (!moveItemStackTo(
                    stack,
                    CACHE_SLOTS,
                    CACHE_SLOTS + PLAYER_INVENTORY_SLOTS,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(
                stack,
                0,
                CACHE_SLOTS,
                false
        )) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        85 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(
                    inventory,
                    column,
                    8 + column * 18,
                    143
            ));
        }
    }
}
