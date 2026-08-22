package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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

public class DungeonChainBlock extends Block {
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    public static final BooleanProperty END = BooleanProperty.create("end");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape HANGING_SHAPE = Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D);
    private static final VoxelShape HANGING_SHAPE_FLOATING = Shapes.box(0.375D, 0.25D, 0.375D, 0.625D, 1.0D, 0.625D);
    private static final VoxelShape WALL_NORTH = Shapes.box(0.375D, 0.0D, 0.875D, 0.625D, 1.0D, 1.0D);
    private static final VoxelShape WALL_SOUTH = Shapes.box(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 0.125D);
    private static final VoxelShape WALL_WEST = Shapes.box(0.875D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D);
    private static final VoxelShape WALL_EAST = Shapes.box(0.0D, 0.0D, 0.375D, 0.125D, 1.0D, 0.625D);
    private static final VoxelShape WALL_NORTH_FLOATING = Shapes.box(0.375D, 0.25D, 0.875D, 0.625D, 1.0D, 1.0D);
    private static final VoxelShape WALL_SOUTH_FLOATING = Shapes.box(0.375D, 0.25D, 0.0D, 0.625D, 1.0D, 0.125D);
    private static final VoxelShape WALL_WEST_FLOATING = Shapes.box(0.875D, 0.25D, 0.375D, 1.0D, 1.0D, 0.625D);
    private static final VoxelShape WALL_EAST_FLOATING = Shapes.box(0.0D, 0.25D, 0.375D, 0.125D, 1.0D, 0.625D);

    public DungeonChainBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WALL, false)
                .setValue(END, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return stateFor(context.getLevel(), context.getClickedPos(), context.getClickedFace());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(WALL)) {
            return level.getBlockState(pos.above()).is(this)
                    || level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
        }
        Direction facing = state.getValue(FACING);
        BlockState above = level.getBlockState(pos.above());
        return above.is(this) && above.getValue(WALL) == state.getValue(WALL)
                || level.getBlockState(pos.relative(facing.getOpposite()))
                .isFaceSturdy(level, pos.relative(facing.getOpposite()), facing);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (!state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return state.setValue(END, isEnd(level, pos, state));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean floating = !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        if (!state.getValue(WALL)) {
            return floating ? HANGING_SHAPE_FLOATING : HANGING_SHAPE;
        }
        Direction facing = state.getValue(FACING);
        if (floating) {
            return switch (facing) {
                case SOUTH -> WALL_SOUTH_FLOATING;
                case WEST -> WALL_WEST_FLOATING;
                case EAST -> WALL_EAST_FLOATING;
                default -> WALL_NORTH_FLOATING;
            };
        }
        return wallShape(facing);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    public boolean isLadder(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    private BlockState stateFor(LevelReader level, BlockPos pos, Direction clickedFace) {
        Direction wallFacing = wallFacing(level, pos, clickedFace);
        boolean wall = wallFacing != null;
        Direction facing = wallFacing == null ? Direction.NORTH : wallFacing;

        if (!wall) {
            BlockState above = level.getBlockState(pos.above());
            if (above.is(this)) {
                wall = above.getValue(WALL);
                facing = above.getValue(FACING);
            } else if (above.isFaceSturdy(level, pos.above(), Direction.DOWN)) {
                wall = false;
                facing = Direction.NORTH;
            }
        }

        if (!wall && !level.getBlockState(pos.above()).is(this)
                && !level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN)) {
            Direction fallback = firstWallFacing(level, pos);
            if (fallback != null) {
                wall = true;
                facing = fallback;
            }
        }

        BlockState state = this.defaultBlockState()
                .setValue(WALL, wall)
                .setValue(FACING, facing);
        return state.setValue(END, isEnd(level, pos, state));
    }

    private static Direction wallFacing(LevelReader level, BlockPos pos, Direction clickedFace) {
        if (clickedFace.getAxis().isHorizontal()) {
            BlockPos supportPos = pos.relative(clickedFace.getOpposite());
            if (level.getBlockState(supportPos).isFaceSturdy(level, supportPos, clickedFace)) {
                return clickedFace;
            }
        }
        return null;
    }

    private static Direction firstWallFacing(LevelReader level, BlockPos pos) {
        Direction[] oldOrder = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction direction : oldOrder) {
            BlockPos supportPos = pos.relative(direction.getOpposite());
            if (level.getBlockState(supportPos).isFaceSturdy(level, supportPos, direction)) {
                return direction;
            }
        }
        return null;
    }

    private static boolean isEnd(LevelReader level, BlockPos pos, BlockState state) {
        BlockState below = level.getBlockState(pos.below());
        return !(below.is(state.getBlock()) && below.getValue(WALL) == state.getValue(WALL))
                && !below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static VoxelShape wallShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> WALL_SOUTH;
            case WEST -> WALL_WEST;
            case EAST -> WALL_EAST;
            default -> WALL_NORTH;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WALL, END, FACING);
    }
}
