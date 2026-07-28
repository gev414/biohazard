package io.github.gev414.rotwire.block.entity;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Rotwire.MOD_ID);

    public static final Supplier<BlockEntityType<RadioTransmitterBlockEntity>>
            RADIO_TRANSMITTER = BLOCK_ENTITY_TYPES.register(
                    "radio_transmitter",
                    () -> new BlockEntityType<>(
                            RadioTransmitterBlockEntity::new,
                            Set.of(ModBlocks.RADIO_TRANSMITTER.get()),
                            null
                    )
            );

    private ModBlockEntities() {
    }
}
