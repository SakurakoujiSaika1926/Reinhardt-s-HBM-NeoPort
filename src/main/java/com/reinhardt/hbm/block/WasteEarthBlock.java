package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WasteEarthBlock extends Block {
    public WasteEarthBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (level.getRawBrightness(above, 0) < 4 && aboveState.getLightBlock(level, above) > 2) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            return;
        }
        if (aboveState.getBlock() instanceof MushroomBlock) {
            Block mush = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("mush"));
            if (mush != Blocks.AIR) {
                level.setBlock(above, mush.defaultBlockState(), 3);
            }
        }
    }

}
