package com.reinhardt.hbm.block;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7.10 high-energy field jammer. It refreshes its 100-tick FLEIJA exclusion field every 10 ticks. */
public final class FieldDisturberBlock extends Block {
    private static final int TICK_RATE = 10;
    private static final int FIELD_LIFETIME = 100;

    public FieldDisturberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, TICK_RATE);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        NukeExplosionManager.registerFieldDisturber(level, pos, FIELD_LIFETIME);
        level.scheduleTick(pos, this, TICK_RATE);
    }
}
