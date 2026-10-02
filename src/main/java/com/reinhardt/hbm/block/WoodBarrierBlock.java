package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Modern state/collision port of the 1.7.10 wooden {@code BlockBarrier}. */
public final class WoodBarrierBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;

    private static final VoxelShape NORTH_SHAPE = box(0, 0, 0, 16, 16, 2);
    private static final VoxelShape EAST_SHAPE = box(14, 0, 0, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape WEST_SHAPE = box(0, 0, 0, 2, 16, 16);

    public WoodBarrierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, true)
                .setValue(WEST, false)
                .setValue(UP, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction facing = clickedFace.getAxis().isHorizontal()
                ? clickedFace
                : context.getHorizontalDirection().getOpposite();
        return updateConnections(this.defaultBlockState().setValue(FACING, facing),
                context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return updateConnections(state, level, pos);
    }

    public static BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction forced = state.getValue(FACING).getOpposite();
        return state
                .setValue(NORTH, forced == Direction.NORTH || supported(level, pos, Direction.NORTH))
                .setValue(EAST, forced == Direction.EAST || supported(level, pos, Direction.EAST))
                .setValue(SOUTH, forced == Direction.SOUTH || supported(level, pos, Direction.SOUTH))
                .setValue(WEST, forced == Direction.WEST || supported(level, pos, Direction.WEST))
                .setValue(UP, supported(level, pos, Direction.UP));
    }

    private static boolean supported(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos supportPos = pos.relative(direction);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, direction.getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    private static VoxelShape shapeFor(BlockState state) {
        VoxelShape shape = Shapes.empty();
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        return shape;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = rotation.rotate(state.getValue(FACING));
        BlockState rotated = state.setValue(FACING, facing)
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (state.getValue(property(direction))) {
                rotated = rotated.setValue(property(rotation.rotate(direction)), true);
            }
        }
        return rotated;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Wood barrier direction must be horizontal: " + direction);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, EAST, SOUTH, WEST, UP);
    }
}
