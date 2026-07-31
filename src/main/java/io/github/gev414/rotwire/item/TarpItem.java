package io.github.gev414.rotwire.item;

import io.github.gev414.rotwire.block.TarpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class TarpItem extends BlockItem {

    public TarpItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @Nullable BlockPlaceContext updatePlacementContext(
            BlockPlaceContext context
    ) {
        BlockPos anchor = context.getClickedPos().above();
        if (anchor.getY() >= context.getLevel().getMaxBuildHeight()) {
            return null;
        }
        return BlockPlaceContext.at(context, anchor, Direction.UP);
    }

    @Override
    protected boolean placeBlock(
            BlockPlaceContext context,
            BlockState state
    ) {
        if (!(state.getBlock() instanceof TarpBlock tarp)) {
            return false;
        }
        return tarp.placeStructure(
                context.getLevel(),
                context.getClickedPos(),
                state
        );
    }

    @Override
    public String getDescriptionId() {
        return "item.rotwire.tarp";
    }
}
