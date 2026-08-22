package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HbmMushroomBlock extends BushBlock {
    private static final MapCodec<HbmMushroomBlock> CODEC = simpleCodec(HbmMushroomBlock::new);

    public HbmMushroomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.MYCELIUM)
                || state.is(BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("waste_mycelium")))
                || state.is(BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("waste_earth")))
                || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0 && level.getBlockState(pos.below()).is(Blocks.DIRT)) {
            Block wasteMycelium = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("waste_mycelium"));
            if (wasteMycelium != Blocks.AIR) {
                level.setBlock(pos.below(), wasteMycelium.defaultBlockState(), 3);
            }
        }
    }
}
