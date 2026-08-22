package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WoodBurnerBlock extends PowerMachineBlock {
    private final LargeMachineBlock.Footprint footprint;
    private final VoxelShape shape;

    public WoodBurnerBlock(Properties properties, LargeMachineBlock.Footprint footprint, VoxelShape shape) {
        super(properties, MachineType.WOOD_BURNER);
        this.footprint = footprint;
        this.shape = shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || !LargeMachineBlock.canPlaceFootprint(context, state.getValue(FACING), this.footprint)) {
            return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            LargeMachineBlock.placeDummies(level, pos, state.getValue(FACING), this.footprint);
            LargeMachineBlock.pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), this.footprint, placer);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            LargeMachineBlock.removeDummies(level, pos, state.getValue(FACING), this.footprint);
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
}
