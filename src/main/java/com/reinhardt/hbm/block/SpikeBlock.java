package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.7.10 Spikes: visual-only collision and falling-contact damage. */
public final class SpikeBlock extends Block {
    private static final VoxelShape EMPTY = Shapes.empty();

    public SpikeBlock(Properties properties) {
        super(properties.noOcclusion().noCollission());
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return EMPTY;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!level.isClientSide && entity instanceof LivingEntity living && entity.getDeltaMovement().y < -0.1D) {
            living.hurt(level.damageSources().generic(), 100.0F);
        }
    }
}
