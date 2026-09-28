package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WoodBurnerBlock extends PowerMachineBlock {
    /** Exact MachineWoodBurner dimensions {1,0,1,0,1,0}, authored for SOUTH. */
    private static final LargeMachineBlock.Footprint FOOTPRINT =
            LargeMachineBlock.Footprint.box(-1, 0, 0, 1, -1, 0);
    private final VoxelShape shape;

    public WoodBurnerBlock(Properties properties, VoxelShape shape) {
        super(properties, MachineType.WOOD_BURNER);
        this.shape = shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || !LargeMachineBlock.canPlaceFootprint(context, state.getValue(FACING), FOOTPRINT,
                WoodBurnerBlock::mapWoodBurnerOffset)) {
            return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            LargeMachineBlock.placeDummies(level, pos, state.getValue(FACING), FOOTPRINT,
                    WoodBurnerBlock::mapWoodBurnerOffset);
            LargeMachineBlock.pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), FOOTPRINT, placer,
                    WoodBurnerBlock::mapWoodBurnerOffset);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            LargeMachineBlock.removeDummies(level, pos, state.getValue(FACING), FOOTPRINT,
                    WoodBurnerBlock::mapWoodBurnerOffset);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    private static BlockPos mapWoodBurnerOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("Wood burner facing must be horizontal: " + facing);
        };
    }
}
