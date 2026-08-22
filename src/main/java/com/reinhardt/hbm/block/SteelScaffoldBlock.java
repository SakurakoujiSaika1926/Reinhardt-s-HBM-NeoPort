package com.reinhardt.hbm.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class SteelScaffoldBlock extends Block {
    public static final EnumProperty<Orientation> ORIENT = EnumProperty.create("orient", Orientation.class);

    private static final VoxelShape HORIZONTAL_NS = box(0.0D, 0.0D, 2.0D, 16.0D, 16.0D, 14.0D);
    private static final VoxelShape HORIZONTAL_EW = box(2.0D, 0.0D, 0.0D, 14.0D, 16.0D, 16.0D);
    private static final VoxelShape VERTICAL = box(0.0D, 2.0D, 0.0D, 16.0D, 14.0D, 16.0D);

    public SteelScaffoldBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ORIENT, Orientation.HORIZONTAL_NS));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Orientation orientation;
        if (clickedFace.getAxis().isVertical()) {
            orientation = context.getHorizontalDirection().getAxis() == Direction.Axis.Z
                    ? Orientation.HORIZONTAL_NS
                    : Orientation.HORIZONTAL_EW;
        } else {
            orientation = clickedFace.getAxis() == Direction.Axis.Z
                    ? Orientation.VERTICAL_NS
                    : Orientation.VERTICAL_EW;
        }
        return this.defaultBlockState().setValue(ORIENT, orientation);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ORIENT, state.getValue(ORIENT).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ORIENT, state.getValue(ORIENT).mirror(mirror));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENT);
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(ORIENT)) {
            case HORIZONTAL_EW -> HORIZONTAL_EW;
            case HORIZONTAL_NS -> HORIZONTAL_NS;
            case VERTICAL_NS, VERTICAL_EW -> VERTICAL;
        };
    }

    public enum Orientation implements StringRepresentable {
        HORIZONTAL_NS("horizontal_north_south"),
        HORIZONTAL_EW("horizontal_east_west"),
        VERTICAL_NS("vertical_north_south"),
        VERTICAL_EW("vertical_east_west");

        private final String serializedName;

        Orientation(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }

        private Orientation rotate(Rotation rotation) {
            return switch (rotation) {
                case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> swapAxis();
                default -> this;
            };
        }

        private Orientation mirror(Mirror mirror) {
            return mirror == Mirror.NONE ? this : this;
        }

        private Orientation swapAxis() {
            return switch (this) {
                case HORIZONTAL_NS -> HORIZONTAL_EW;
                case HORIZONTAL_EW -> HORIZONTAL_NS;
                case VERTICAL_NS -> VERTICAL_EW;
                case VERTICAL_EW -> VERTICAL_NS;
            };
        }
    }
}
