package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7.10 lithium storage block: direct water contact causes a violent reaction. */
public final class LithiumBlock extends Block {
    public LithiumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        reactToWater(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        reactToWater(level, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos.above())) {
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + random.nextFloat(), pos.getY() + 1.0D, pos.getZ() + random.nextFloat(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private void reactToWater(Level level, BlockPos pos) {
        if (level.isClientSide || !level.getBlockState(pos).is(this)) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                level.removeBlock(pos, false);
                level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        15.0F, false, Level.ExplosionInteraction.BLOCK);
                return;
            }
        }
    }
}
