package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HbmMushroomBlock extends BushBlock implements BonemealableBlock {
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
                || state.is(HbmBlocks.WASTE_TRINITITE.get())
                || state.is(HbmBlocks.WASTE_TRINITITE_RED.get())
                || state.is(HbmBlocks.BLOCK_WASTE.get())
                || state.is(HbmBlocks.BLOCK_WASTE_PAINTED.get())
                || state.is(HbmBlocks.BLOCK_WASTE_VITRIFIED.get())
                || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (HbmConfig.ENABLE_MYCELIUM_SPREAD.get()
                && random.nextInt(5) == 0
                && level.getBlockState(pos.below()).is(HbmBlocks.WASTE_EARTH.get())) {
            level.setBlock(pos.below(), HbmBlocks.WASTE_MYCELIUM.get().defaultBlockState(), Block.UPDATE_ALL);
        }

        if (random.nextInt(25) != 0) {
            return;
        }
        int remaining = 3;
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(nearby).is(this) && --remaining <= 0) {
                return;
            }
        }

        BlockPos candidate = pos.offset(random.nextInt(5) - 2,
                random.nextInt(2) - random.nextInt(2), random.nextInt(5) - 2);
        for (int attempt = 0; attempt < 4; attempt++) {
            if (level.isEmptyBlock(candidate)
                    && mayPlaceOn(level.getBlockState(candidate.below()), level, candidate.below())) {
                pos = candidate;
            }
            candidate = pos.offset(random.nextInt(5) - 2,
                    random.nextInt(2) - random.nextInt(2), random.nextInt(5) - 2);
        }
        if (level.isEmptyBlock(candidate)
                && mayPlaceOn(level.getBlockState(candidate.below()), level, candidate.below())) {
            level.setBlock(candidate, defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return canSurvive(state, level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.4F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false);
        generateHugeMushroom(level, pos);
    }

    private static void generateHugeMushroom(ServerLevel level, BlockPos origin) {
        fillLayer(level, origin, 0, 1, HbmBlocks.MUSH_BLOCK.get());
        fillLayer(level, origin, 3, 1, HbmBlocks.MUSH_BLOCK.get());
        fillLayer(level, origin, 5, 2, HbmBlocks.MUSH_BLOCK.get());
        for (int y = 6; y <= 8; y++) {
            fillLayer(level, origin, y, 4, HbmBlocks.MUSH_BLOCK.get());
        }
        fillLayer(level, origin, 9, 3, HbmBlocks.MUSH_BLOCK.get());
        fillLayer(level, origin, 10, 1, HbmBlocks.MUSH_BLOCK.get());
        for (int y = 0; y < 8; y++) {
            BlockPos stem = origin.above(y);
            if (level.isInWorldBounds(stem)) {
                level.setBlock(stem, HbmBlocks.MUSH_BLOCK_STEM.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void fillLayer(ServerLevel level, BlockPos origin, int yOffset, int radius, Block block) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos target = origin.offset(x, yOffset, z);
                if (level.isInWorldBounds(target)) {
                    level.setBlock(target, block.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
}
