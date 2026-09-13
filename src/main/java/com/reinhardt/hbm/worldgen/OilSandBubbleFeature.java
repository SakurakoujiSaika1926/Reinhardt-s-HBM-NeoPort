package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class OilSandBubbleFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public OilSandBubbleFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public OilSandBubbleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!HbmConfig.GENERATE_OIL_SAND_DEPOSITS.get()) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Biome biome = level.getBiome(origin).value();
        if (biome.hasPrecipitation() || biome.getBaseTemperature() < 1.5F) {
            return false;
        }

        RandomSource random = context.random();
        int spawnRate = Math.max(1, HbmConfig.OIL_SAND_DEPOSIT_SPAWN_RATE.get());
        if (random.nextInt(spawnRate) != 0) {
            return false;
        }

        int radius = HbmConfig.radius(HbmConfig.OIL_SAND_DEPOSIT_MIN_RADIUS, HbmConfig.OIL_SAND_DEPOSIT_MAX_RADIUS, random);
        return spawnOilSand(level, random, origin, radius);
    }

    private static boolean spawnOilSand(WorldGenLevel level, RandomSource random, BlockPos origin, int radius) {
        int r2 = radius * radius;
        int r22 = r2 / 2;
        int randomBound = Math.max(1, r22 / 3);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int xx = -radius; xx < radius; xx++) {
            int x = xx + origin.getX();
            int xxSq = xx * xx;
            for (int yy = -radius; yy < radius; yy++) {
                int y = yy + origin.getY();
                int yySq = xxSq + yy * yy * 3;
                if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                    continue;
                }
                for (int zz = -radius; zz < radius; zz++) {
                    int z = zz + origin.getZ();
                    int dist = yySq + zz * zz;
                    if (dist < r22 + random.nextInt(randomBound)) {
                        pos.set(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
                            level.setBlock(pos, HbmBlocks.ORE_OIL_SAND.get().defaultBlockState(), SET_FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
