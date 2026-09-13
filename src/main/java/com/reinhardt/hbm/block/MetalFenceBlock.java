package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.MetalFenceBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class MetalFenceBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty FORCE_POST = BooleanProperty.create("force_post");
    public static final BooleanProperty PILLAR = BooleanProperty.create("pillar");

    private static final VoxelShape PILLAR_SHAPE = Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D);
    private static final VoxelShape NORTH_SHAPE = Shapes.box(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 0.375D);
    private static final VoxelShape EAST_SHAPE = Shapes.box(0.625D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D);
    private static final VoxelShape SOUTH_SHAPE = Shapes.box(0.375D, 0.0D, 0.625D, 0.625D, 1.0D, 1.0D);
    private static final VoxelShape WEST_SHAPE = Shapes.box(0.0D, 0.0D, 0.375D, 0.375D, 1.0D, 0.625D);
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);

    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST, EAST);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.WEST, WEST);
    }

    public MetalFenceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(FORCE_POST, false)
                .setValue(PILLAR, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        boolean forcePost = MetalFenceBlockItem.isPost(stack);
        return updateConnections(this.defaultBlockState().setValue(FORCE_POST, forcePost), context.getLevel(), context.getClickedPos());
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
        if (direction.getAxis().isHorizontal()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighborState, level, neighborPos));
            return state.setValue(PILLAR, shouldShowPillar(state));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
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

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (state.getValue(FORCE_POST) && this.asItem() instanceof MetalFenceBlockItem item) {
            return MetalFenceBlockItem.postStack(item);
        }
        return new ItemStack(this);
    }

    private static BlockState updateConnections(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(
                    PROPERTY_BY_DIRECTION.get(direction),
                    connectsTo(level.getBlockState(pos.relative(direction)), level, pos.relative(direction))
            );
        }
        return state.setValue(PILLAR, shouldShowPillar(state));
    }

    private static boolean connectsTo(BlockState neighbor, LevelReader level, BlockPos neighborPos) {
        // Match 1.7.10 BlockFence#canConnectFenceTo exactly: the custom
        // fence connects to another fence of the same block, to a fence gate,
        // or to an opaque/full-cube block.  Iron bars are deliberately not a
        // connection target in the old implementation.
        if (neighbor.getBlock() instanceof MetalFenceBlock || neighbor.getBlock() instanceof FenceGateBlock) {
            return true;
        }
        return neighbor.isSolidRender(level, neighborPos);
    }

    /**
     * Recalculate the four connection properties after a bulk structure write.
     * Structure placement uses update flag 2 (as did 1.7.10 NBTStructure), so
     * neighbour updates are intentionally suppressed during the pass.
     */
    public static void refreshConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MetalFenceBlock)) {
            return;
        }
        BlockState connected = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            connected = connected.updateShape(direction, level.getBlockState(neighborPos), level, pos, neighborPos);
        }
        if (connected != state) {
            level.setBlock(pos, connected, 2);
        }
    }

    private static boolean shouldShowPillar(BlockState state) {
        boolean west = state.getValue(WEST);
        boolean east = state.getValue(EAST);
        boolean north = state.getValue(NORTH);
        boolean south = state.getValue(SOUTH);
        boolean hasX = west || east;
        boolean hasZ = north || south;
        boolean straightX = !hasZ && west && east;
        boolean straightZ = !hasX && north && south;
        return state.getValue(FORCE_POST) || (!straightX && !straightZ);
    }

    private static VoxelShape shapeFor(BlockState state) {
        VoxelShape shape = state.getValue(PILLAR) ? PILLAR_SHAPE : Shapes.empty();
        if (state.getValue(NORTH)) {
            shape = Shapes.joinUnoptimized(shape, NORTH_SHAPE, BooleanOp.OR);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.joinUnoptimized(shape, EAST_SHAPE, BooleanOp.OR);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.joinUnoptimized(shape, SOUTH_SHAPE, BooleanOp.OR);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.joinUnoptimized(shape, WEST_SHAPE, BooleanOp.OR);
        }
        return shape.isEmpty() ? PILLAR_SHAPE : shape.optimize();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = switch (rotation) {
            case CLOCKWISE_180 -> state
                    .setValue(NORTH, state.getValue(SOUTH))
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH))
                    .setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state
                    .setValue(NORTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state
                    .setValue(NORTH, state.getValue(WEST))
                    .setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST))
                    .setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
        return rotated.setValue(PILLAR, shouldShowPillar(rotated));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = switch (mirror) {
            case LEFT_RIGHT -> state
                    .setValue(NORTH, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(WEST, state.getValue(EAST));
            default -> state;
        };
        return mirrored.setValue(PILLAR, shouldShowPillar(mirrored));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, FORCE_POST, PILLAR);
    }
}
