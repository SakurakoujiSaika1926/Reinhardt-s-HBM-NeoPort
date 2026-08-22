package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoatedEnergyCableBlock extends EnergyCableBlock {
    private static final VoxelShape FULL_BLOCK = Shapes.block();

    public CoatedEnergyCableBlock(Properties properties) {
        super(properties, 8.0D);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withNeighborCoatedConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
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
        return state.setValue(propertyFor(direction), neighborState.getBlock() instanceof CoatedEnergyCableBlock);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_BLOCK;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_BLOCK;
    }

    private BlockState withNeighborCoatedConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            updated = updated.setValue(
                    propertyFor(direction),
                    level.getBlockState(pos.relative(direction)).getBlock() instanceof CoatedEnergyCableBlock
            );
        }
        return updated;
    }

    public static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }
}
