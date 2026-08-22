package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WasteLeavesBlock extends LeavesBlock {
    public WasteLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(30) == 0) {
            level.removeBlock(pos, false);
            if (level.isEmptyBlock(pos.below())) {
                level.setBlock(pos.below(), HbmBlocks.LEAVES_LAYER.get().defaultBlockState(), 3);
            }
            return;
        }
        super.randomTick(state, level, pos, random);
    }
}
