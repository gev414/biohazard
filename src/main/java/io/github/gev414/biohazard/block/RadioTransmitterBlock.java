package io.github.gev414.biohazard.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RadioTransmitterBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<RadioTransmitterBlock> CODEC =
            simpleCodec(RadioTransmitterBlock::new);

    private static final VoxelShape TABLE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 2.0D),
            Block.box(14.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D),
            Block.box(14.0D, 0.0D, 14.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 0.0D, 14.0D, 2.0D, 2.0D, 16.0D),
            Block.box(0.0D, 2.0D, 0.0D, 16.0D, 4.0D, 16.0D)
    );
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            TABLE,
            Block.box(5.0D, 3.0D, 10.0D, 14.0D, 16.0D, 14.0D)
    );
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            TABLE,
            Block.box(2.0D, 3.0D, 5.0D, 6.0D, 16.0D, 14.0D)
    );
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            TABLE,
            Block.box(2.0D, 3.0D, 2.0D, 11.0D, 16.0D, 6.0D)
    );
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            TABLE,
            Block.box(10.0D, 3.0D, 2.0D, 14.0D, 16.0D, 11.0D)
    );

    public RadioTransmitterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(
                FACING,
                Direction.NORTH
        ));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }
}
