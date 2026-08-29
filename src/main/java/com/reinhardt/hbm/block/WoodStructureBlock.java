package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Direct modern state/collision port of BlockWoodStructure. */
public final class WoodStructureBlock extends Block {
    public enum Type implements StringRepresentable {
        ROOF("roof"), SCAFFOLD("scaffold"), CEILING("ceiling");

        private final String id;

        Type(String id) { this.id = id; }

        @Override
        public String getSerializedName() { return this.id; }
    }

    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty ABOVE = BooleanProperty.create("above");

    public WoodStructureBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(TYPE, Type.ROOF)
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false)
                .setValue(ABOVE, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return updateConnections(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionShape(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, NORTH, EAST, SOUTH, WEST, ABOVE);
    }

    private static BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(NORTH, same(level, pos.north(), state))
                .setValue(EAST, same(level, pos.east(), state))
                .setValue(SOUTH, same(level, pos.south(), state))
                .setValue(WEST, same(level, pos.west(), state))
                .setValue(ABOVE, same(level, pos.above(), state));
    }

    private static boolean same(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState neighbor = level.getBlockState(pos);
        return neighbor.getBlock() == state.getBlock() && neighbor.hasProperty(TYPE)
                && neighbor.getValue(TYPE) == state.getValue(TYPE);
    }

    private static VoxelShape collisionShape(BlockState state) {
        return switch (state.getValue(TYPE)) {
            case ROOF -> Block.box(0, 0, 0, 16, 3, 16);
            case CEILING -> Block.box(0, 14, 0, 16, 16, 16);
            case SCAFFOLD -> Shapes.block();
        };
    }
}
