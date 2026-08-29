package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** The four metadata variants of 1.7.10 BlockAbsorber. */
public final class RadiationAbsorberBlock extends LegacyVariantBlock {
    private static final float[] ABSORB_AMOUNT = {2.5F, 10.0F, 100.0F, 10_000.0F};

    public RadiationAbsorberBlock(Properties properties) {
        super(properties.randomTicks(), 3);
    }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, 10);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int tier = Math.max(0, Math.min(ABSORB_AMOUNT.length - 1, state.getValue(VARIANT)));
        com.reinhardt.hbm.radiation.ChunkRadiationData data =
                com.reinhardt.hbm.radiation.ChunkRadiationData.get(level);
        data.decrementRadiation(pos, ABSORB_AMOUNT[tier]);
        level.scheduleTick(pos, this, 10);
    }
}
