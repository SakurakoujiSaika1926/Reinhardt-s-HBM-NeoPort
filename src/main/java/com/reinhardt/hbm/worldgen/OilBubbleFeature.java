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

public class OilBubbleFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public OilBubbleFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public OilBubbleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!HbmConfig.GENERATE_OIL_DEPOSITS.get()) {
            return false;
        }

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int spawnRate = adjustedSpawnRate(level, origin);
        if (spawnRate <= 0 || random.nextInt(spawnRate) != 0) {
            return false;
        }

        int radius = HbmConfig.radius(HbmConfig.OIL_DEPOSIT_MIN_RADIUS, HbmConfig.OIL_DEPOSIT_MAX_RADIUS, random);
        boolean placed = spawnOil(level, origin, radius);
        OilFieldSurfaceEffects.addOilDepositSurfaceSpot(level, random, origin.getX(), origin.getZ());
        return placed;
    }

    private static int adjustedSpawnRate(WorldGenLevel level, BlockPos origin) {
        int spawnRate = HbmConfig.OIL_DEPOSIT_SPAWN_RATE.get();
        Biome biome = level.getBiome(origin).value();
        if (biome.getBaseTemperature() >= 2.0F && biome.getModifiedClimateSettings().downfall() < 0.1F) {
            spawnRate /= HbmConfig.OIL_DEPOSIT_DRY_BIOME_DIVISOR.get();
        }
        return Math.max(1, spawnRate);
    }

    private static boolean spawnOil(WorldGenLevel level, BlockPos origin, int radius) {
        int r2 = radius * radius;
        int r22 = r2 / 2;
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
                    if (dist < r22) {
                        pos.set(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.is(Blocks.STONE)) {
                            level.setBlock(pos, HbmBlocks.ORE_OIL.get().defaultBlockState(), SET_FLAGS);
                            placed = true;
                        } else if (state.is(Blocks.DEEPSLATE)) {
                            level.setBlock(pos, HbmBlocks.ORE_DEEPSLATE_OIL.get().defaultBlockState(), SET_FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
