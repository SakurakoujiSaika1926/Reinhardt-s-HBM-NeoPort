package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7.10 FragileBrick: stepping on it or its scheduled tick removes it without drops. */
public final class FragileBrickBlock extends Block {
    public FragileBrickBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level instanceof ServerLevel serverLevel) {
            breakFragile(serverLevel, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        breakFragile(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && oldState.getBlock() != this) {
            level.scheduleTick(pos, this, 8 + level.random.nextInt(4));
        }
    }

    private static void breakFragile(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof FragileBrickBlock) {
            level.destroyBlock(pos, false);
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
    }
}
