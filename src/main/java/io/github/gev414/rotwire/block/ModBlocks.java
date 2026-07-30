package io.github.gev414.rotwire.block;

import io.github.gev414.rotwire.Rotwire;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Rotwire.MOD_ID);

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

    public static final Supplier<TarpBlock> DEPLOYED_TARP =
            BLOCKS.registerBlock(
                    "deployed_tarp",
                    TarpBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN)
                            .strength(0.4F)
                            .sound(SoundType.WOOL)
                            .noCollission()
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .ignitedByLava()
                            .noLootTable()
            );

    private ModBlocks() {
    }
}
