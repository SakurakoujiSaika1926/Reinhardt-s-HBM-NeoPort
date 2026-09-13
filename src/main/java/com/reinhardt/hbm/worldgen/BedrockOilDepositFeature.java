package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BedrockOilDepositFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public BedrockOilDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public BedrockOilDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!HbmConfig.GENERATE_BEDROCK_OIL_DEPOSITS.get()) {
            return false;
        }
        RandomSource random = context.random();
        int spawnRate = Math.max(1, HbmConfig.BEDROCK_OIL_DEPOSIT_SPAWN_RATE.get());
        if (random.nextInt(spawnRate) != 0) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        boolean placed = generateBedrockOil(level, origin.getX(), origin.getZ());
        OilFieldSurfaceEffects.generateBedrockOilSpot(
                level,
                random,
                origin.getX(),
                origin.getZ(),
                HbmConfig.BEDROCK_OIL_SURFACE_RADIUS.get(),
                HbmConfig.BEDROCK_OIL_SURFACE_ATTEMPTS.get(),
                true
        );
        return placed;
    }

    private static boolean generateBedrockOil(WorldGenLevel level, int centerX, int centerZ) {
        int dxzLimit = HbmConfig.BEDROCK_OIL_DXZ_LIMIT.get();
        int maxY = HbmConfig.BEDROCK_OIL_MAX_Y_OFFSET.get();
        int l1Max = HbmConfig.BEDROCK_OIL_L1_MAX.get();
        int baseY = level.getMinBuildHeight();
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -dxzLimit; dx <= dxzLimit; dx++) {
            for (int y = 0; y <= maxY; y++) {
                int worldY = baseY + y;
                if (worldY < level.getMinBuildHeight() || worldY >= level.getMaxBuildHeight()) {
                    continue;
                }
                for (int dz = -dxzLimit; dz <= dxzLimit; dz++) {
                    if (Math.abs(dx) + Math.abs(y) + Math.abs(dz) <= l1Max) {
                        pos.set(centerX + dx, worldY, centerZ + dz);
                        if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                            level.setBlock(pos, HbmBlocks.ORE_BEDROCK_OIL.get().defaultBlockState(), SET_FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static void generatePorousStone(WorldGenLevel level, RandomSource random, int centerX, int centerZ) {
        int veinCount = HbmConfig.BEDROCK_OIL_POROUS_VEIN_COUNT.get();
        int veinSize = HbmConfig.BEDROCK_OIL_POROUS_VEIN_SIZE.get();
        int minY = HbmConfig.BEDROCK_OIL_POROUS_MIN_Y.get();
        int variance = HbmConfig.BEDROCK_OIL_POROUS_Y_VARIANCE.get();
        int chunkMinX = (centerX >> 4) << 4;
        int chunkMinZ = (centerZ >> 4) << 4;

        for (int i = 0; i < veinCount; i++) {
            int x = chunkMinX + random.nextInt(16);
            int y = minY + (variance > 0 ? random.nextInt(variance) : 0);
            int z = chunkMinZ + random.nextInt(16);
            generateMinableNonCascade(level, random, new BlockPos(x, y, z), HbmBlocks.STONE_POROUS.get().defaultBlockState(), veinSize);
        }
    }

    private static void generateMinableNonCascade(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState oreState, int blockCount) {
        float angle = random.nextFloat() * (float) Math.PI;
        double x0 = origin.getX() + 8.0F + Mth.sin(angle) * blockCount / 8.0F;
        double x1 = origin.getX() + 8.0F - Mth.sin(angle) * blockCount / 8.0F;
        double z0 = origin.getZ() + 8.0F + Mth.cos(angle) * blockCount / 8.0F;
        double z1 = origin.getZ() + 8.0F - Mth.cos(angle) * blockCount / 8.0F;
        double y0 = origin.getY() + random.nextInt(3) - 2;
        double y1 = origin.getY() + random.nextInt(3) - 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < blockCount; i++) {
            float progress = (float) i / (float) blockCount;
            double x = x0 + (x1 - x0) * progress;
            double y = y0 + (y1 - y0) * progress;
            double z = z0 + (z1 - z0) * progress;
            double radiusRandom = random.nextDouble() * blockCount / 16.0D;
            double horizontalSize = (Math.sin(Math.PI * progress) + 1.0D) * radiusRandom + 1.0D;
            double verticalSize = (Math.sin(Math.PI * progress) + 1.0D) * radiusRandom + 1.0D;
            int minX = Mth.floor(x - horizontalSize / 2.0D);
            int minY = Mth.floor(y - verticalSize / 2.0D);
            int minZ = Mth.floor(z - horizontalSize / 2.0D);
            int maxX = Mth.floor(x + horizontalSize / 2.0D);
            int maxY = Mth.floor(y + verticalSize / 2.0D);
            int maxZ = Mth.floor(z + horizontalSize / 2.0D);

            for (int blockX = minX; blockX <= maxX; blockX++) {
                double xNorm = ((double) blockX + 0.5D - x) / (horizontalSize / 2.0D);
                if (xNorm * xNorm >= 1.0D) {
                    continue;
                }
                for (int blockY = minY; blockY <= maxY; blockY++) {
                    double yNorm = ((double) blockY + 0.5D - y) / (verticalSize / 2.0D);
                    if (xNorm * xNorm + yNorm * yNorm >= 1.0D || blockY < level.getMinBuildHeight() || blockY >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                        double zNorm = ((double) blockZ + 0.5D - z) / (horizontalSize / 2.0D);
                        if (xNorm * xNorm + yNorm * yNorm + zNorm * zNorm < 1.0D) {
                            pos.set(blockX, blockY, blockZ);
                            if (level.getBlockState(pos).is(Blocks.STONE)) {
                                level.setBlock(pos, oreState, SET_FLAGS);
                            }
                        }
                    }
                }
            }
        }
    }
}
