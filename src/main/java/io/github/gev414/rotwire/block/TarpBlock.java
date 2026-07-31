package io.github.gev414.rotwire.block;

import com.mojang.serialization.MapCodec;
import io.github.gev414.rotwire.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TarpBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<TarpBlock> CODEC =
            simpleCodec(TarpBlock::new);
    public static final EnumProperty<Part> PART =
            EnumProperty.create("part", Part.class);

    private static final ThreadLocal<Boolean> REMOVING_STRUCTURE =
            ThreadLocal.withInitial(() -> false);

    public TarpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.CENTER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos anchor = context.getClickedPos();
        if (!canDeploy(context, anchor, facing)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, Part.CENTER);
    }

    public boolean placeStructure(
            Level level,
            BlockPos anchor,
            BlockState anchorState
    ) {
        Direction facing = anchorState.getValue(FACING);
        for (Part part : Part.values()) {
            BlockPos partPos = positionFor(anchor, facing, part);
            BlockState partState = anchorState.setValue(PART, part);
            if (!level.setBlock(partPos, partState, 11)) {
                removePlacedParts(level, anchor, facing);
                return false;
            }
        }
        return true;
    }

    public static BlockPos anchorPosition(
            BlockPos position,
            BlockState state
    ) {
        BlockPos offset = state.getValue(PART)
                .offset(state.getValue(FACING));
        return position.offset(
                -offset.getX(),
                -offset.getY(),
                -offset.getZ()
        );
    }

    public static BlockPos positionFor(
            BlockPos anchor,
            Direction facing,
            Part part
    ) {
        BlockPos offset = part.offset(facing);
        return anchor.offset(offset);
    }

    public static boolean isComplete(
            BlockGetter level,
            BlockPos position,
            BlockState state
    ) {
        if (!(state.getBlock() instanceof TarpBlock)) {
            return false;
        }
        Direction facing = state.getValue(FACING);
        BlockPos anchor = anchorPosition(position, state);
        for (Part part : Part.values()) {
            BlockState partState = level.getBlockState(
                    positionFor(anchor, facing, part)
            );
            if (!(partState.getBlock() instanceof TarpBlock)
                    || partState.getValue(FACING) != facing
                    || partState.getValue(PART) != part) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos position,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide()) {
            BlockPos anchor = anchorPosition(position, state);
            if (!player.isCreative()) {
                popResource(
                        level,
                        anchor.below(),
                        new ItemStack(ModItems.TARP.get())
                );
            }
            removeOtherParts(level, position, state);
        }
        return super.playerWillDestroy(level, position, state, player);
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos position,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide()
                && !newState.is(this)
                && !REMOVING_STRUCTURE.get()) {
            removeOtherParts(level, position, state);
        }
        super.onRemove(state, level, position, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == Part.CENTER
                ? RenderShape.MODEL
                : RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return Shapes.empty();
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
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, PART);
    }

    private boolean canDeploy(
            BlockPlaceContext context,
            BlockPos anchor,
            Direction facing
    ) {
        Level level = context.getLevel();
        if (anchor.getY() < level.getMinBuildHeight() + 2
                || anchor.getY() >= level.getMaxBuildHeight()) {
            return false;
        }

        for (Part part : Part.values()) {
            BlockPos partPos = positionFor(anchor, facing, part);
            if (!level.getWorldBorder().isWithinBounds(partPos)
                    || !level.getBlockState(partPos).canBeReplaced()
                    || context.getPlayer() != null
                    && !context.getPlayer().mayUseItemAt(
                            partPos,
                            Direction.UP,
                            context.getItemInHand()
                    )) {
                return false;
            }
        }

        return hasPoleSupport(level, anchor, facing, Part.NORTH_WEST)
                && hasPoleSupport(level, anchor, facing, Part.SOUTH_WEST);
    }

    private static boolean hasPoleSupport(
            Level level,
            BlockPos anchor,
            Direction facing,
            Part pole
    ) {
        BlockPos ground = positionFor(anchor, facing, pole).below(2);
        return Block.canSupportCenter(level, ground, Direction.UP);
    }

    private void removeOtherParts(
            Level level,
            BlockPos removedPosition,
            BlockState removedState
    ) {
        BlockPos anchor = anchorPosition(removedPosition, removedState);
        Direction facing = removedState.getValue(FACING);
        REMOVING_STRUCTURE.set(true);
        try {
            for (Part part : Part.values()) {
                BlockPos partPos = positionFor(anchor, facing, part);
                if (partPos.equals(removedPosition)) {
                    continue;
                }
                BlockState partState = level.getBlockState(partPos);
                if (belongsToStructure(
                        partState,
                        part,
                        facing
                )) {
                    level.setBlock(
                            partPos,
                            Blocks.AIR.defaultBlockState(),
                            35
                    );
                }
            }
        } finally {
            REMOVING_STRUCTURE.set(false);
        }
    }

    private static boolean belongsToStructure(
            BlockState state,
            Part part,
            Direction facing
    ) {
        return state.getBlock() instanceof TarpBlock
                && state.getValue(PART) == part
                && state.getValue(FACING) == facing;
    }

    private void removePlacedParts(
            Level level,
            BlockPos anchor,
            Direction facing
    ) {
        REMOVING_STRUCTURE.set(true);
        try {
            for (Part part : Part.values()) {
                BlockPos partPos = positionFor(anchor, facing, part);
                BlockState state = level.getBlockState(partPos);
                if (belongsToStructure(state, part, facing)) {
                    level.setBlock(
                            partPos,
                            Blocks.AIR.defaultBlockState(),
                            35
                    );
                }
            }
        } finally {
            REMOVING_STRUCTURE.set(false);
        }
    }

    private static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public enum Part implements StringRepresentable {
        CENTER("center", 0, 0),
        NORTH_WEST("north_west", -1, -1),
        NORTH("north", 0, -1),
        WEST("west", -1, 0),
        SOUTH_WEST("south_west", -1, 1),
        SOUTH("south", 0, 1);

        private final String serializedName;
        private final int x;
        private final int z;

        Part(String serializedName, int x, int z) {
            this.serializedName = serializedName;
            this.x = x;
            this.z = z;
        }

        public BlockPos offset(Direction facing) {
            return new BlockPos(x, 0, z).rotate(rotationFor(facing));
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
