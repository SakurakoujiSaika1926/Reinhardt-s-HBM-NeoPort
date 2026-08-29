package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small, connectable pneumatic transport tube. The legacy tube used a tile entity
 * for routing; this 1.21 block keeps the visible network geometry and connectivity
 * in block state so isolated tubes remain useful as decorative infrastructure. */
public class PneumaticTubeBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape CENTER = Shapes.box(0.3125D, 0.3125D, 0.3125D, 0.6875D, 0.6875D, 0.6875D);
    private static final VoxelShape NORTH_ARM = Shapes.box(0.3125D, 0.3125D, 0.0D, 0.6875D, 0.6875D, 0.3125D);
    private static final VoxelShape SOUTH_ARM = Shapes.box(0.3125D, 0.3125D, 0.6875D, 0.6875D, 0.6875D, 1.0D);
    private static final VoxelShape WEST_ARM = Shapes.box(0.0D, 0.3125D, 0.3125D, 0.3125D, 0.6875D, 0.6875D);
    private static final VoxelShape EAST_ARM = Shapes.box(0.6875D, 0.3125D, 0.3125D, 1.0D, 0.6875D, 0.6875D);
    private static final VoxelShape UP_ARM = Shapes.box(0.3125D, 0.6875D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
    private static final VoxelShape DOWN_ARM = Shapes.box(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.3125D, 0.6875D);

    private final boolean paintable;

    public PneumaticTubeBlock(Properties properties, boolean paintable) {
        super(properties);
        this.paintable = paintable;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    public boolean isPaintable() {
        return paintable;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), canConnect(context.getLevel().getBlockState(pos.relative(direction))));
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_ARM);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_ARM);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_ARM);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_ARM);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_ARM);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_ARM);
        return shape;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(property(direction), canConnect(neighborState));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    private static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private static boolean canConnect(BlockState state) {
        return state.getBlock() instanceof PneumaticTubeBlock
                || state.getBlock() instanceof PneumaticStorageBlock;
    }
}
