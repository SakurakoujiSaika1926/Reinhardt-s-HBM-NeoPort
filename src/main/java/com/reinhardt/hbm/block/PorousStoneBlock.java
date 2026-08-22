package com.reinhardt.hbm.block;

import com.reinhardt.hbm.radiation.ChunkRadiationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PorousStoneBlock extends Block {
    public PorousStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, nextTickDelay(level.random));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChunkRadiationData data = ChunkRadiationData.get(level);
        double radiation = data.getRadiation(pos);
        if (radiation > 0.0D) {
            data.setRadiation(pos, Math.max(0.0D, radiation - 10.0D));
        }
        level.scheduleTick(pos, this, nextTickDelay(random));
    }

    private static int nextTickDelay(RandomSource random) {
        return 90 + random.nextInt(20);
    }
}
