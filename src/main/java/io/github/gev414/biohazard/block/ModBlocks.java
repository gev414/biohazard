package io.github.gev414.biohazard.block;

import io.github.gev414.biohazard.Biohazard;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Biohazard.MOD_ID);

    public static final Supplier<RadioTransmitterBlock> RADIO_TRANSMITTER =
            BLOCKS.registerBlock(
                    "radio_transmitter",
                    RadioTransmitterBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .requiresCorrectToolForDrops()
            );

    private ModBlocks() {
    }
}
