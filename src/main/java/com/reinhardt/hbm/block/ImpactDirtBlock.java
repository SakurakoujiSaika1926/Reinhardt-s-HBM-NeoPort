package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The 1.7.10 impact dirt recovers into vanilla grass under sufficient light. */
public final class ImpactDirtBlock extends Block {
    public ImpactDirtBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        com.reinhardt.hbm.world.TomImpactSavedData data = com.reinhardt.hbm.world.TomImpactSavedData.get(level);
        int light = Math.max(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos.above()),
                (int) (level.getMaxLocalRawBrightness(pos.above()) * (1.0F - data.dust())));
        if (light >= 9 && data.fire() == 0.0F) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                   Block neighborBlock, BlockPos neighborPos,
                                   boolean movedByPiston) {
        if (level.isClientSide) return;
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(nearby).is(Blocks.GRASS_BLOCK)) {
                level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
        }
    }
}
