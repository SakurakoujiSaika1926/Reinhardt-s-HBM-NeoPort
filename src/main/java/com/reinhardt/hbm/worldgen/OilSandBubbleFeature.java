package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
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
    private static final int MIN_Y = 56;
    private static final int Y_RANGE = 16;

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
        WorldgenFeatureBounds targetBounds = WorldgenFeatureBounds.forCurrentChunk(level, context.origin());
        int targetChunkX = targetBounds.minBlockX() >> 4;
        int targetChunkZ = targetBounds.minBlockZ() >> 4;
        int maximumRadius = ChunkSlicedOilWorldgen.maxConfiguredRadius(
                HbmConfig.OIL_SAND_DEPOSIT_MIN_RADIUS.get(),
                HbmConfig.OIL_SAND_DEPOSIT_MAX_RADIUS.get()
        );
        int searchRadius = ChunkSlicedOilWorldgen.sourceChunkSearchRadius(maximumRadius);
        boolean placed = false;

        for (int sourceChunkX = targetChunkX - searchRadius; sourceChunkX <= targetChunkX + searchRadius; sourceChunkX++) {
            for (int sourceChunkZ = targetChunkZ - searchRadius; sourceChunkZ <= targetChunkZ + searchRadius; sourceChunkZ++) {
                ChunkSlicedOilWorldgen.Deposit deposit = depositForSourceChunk(level, sourceChunkX, sourceChunkZ);
                if (deposit != null && ChunkSlicedOilWorldgen.horizontallyIntersects(
                        targetBounds, deposit.center().getX(), deposit.center().getZ(), deposit.radius())) {
                    placed |= spawnOilSandSlice(level, deposit, targetBounds);
                }
            }
        }
        return placed;
    }

    private static ChunkSlicedOilWorldgen.Deposit depositForSourceChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        RandomSource random = ChunkSlicedOilWorldgen.sourceRandom(
                level.getSeed(), chunkX, chunkZ, ChunkSlicedOilWorldgen.OIL_SAND_DEPOSIT_SALT
        );
        int centerX = (chunkX << 4) + random.nextInt(16);
        int centerZ = (chunkZ << 4) + random.nextInt(16);
        int centerY = MIN_Y + random.nextInt(Y_RANGE);
        int spawnRate = Math.max(1, HbmConfig.OIL_SAND_DEPOSIT_SPAWN_RATE.get());
        if (random.nextInt(spawnRate) != 0) {
            return null;
        }

        Biome biome = level.getUncachedNoiseBiome(
                QuartPos.fromBlock(centerX), QuartPos.fromBlock(centerY), QuartPos.fromBlock(centerZ)
        ).value();
        if (biome.hasPrecipitation() || biome.getBaseTemperature() < 1.5F) {
            return null;
        }

        int minRadius = ChunkSlicedOilWorldgen.minConfiguredRadius(
                HbmConfig.OIL_SAND_DEPOSIT_MIN_RADIUS.get(), HbmConfig.OIL_SAND_DEPOSIT_MAX_RADIUS.get()
        );
        int maxRadius = ChunkSlicedOilWorldgen.maxConfiguredRadius(
                HbmConfig.OIL_SAND_DEPOSIT_MIN_RADIUS.get(), HbmConfig.OIL_SAND_DEPOSIT_MAX_RADIUS.get()
        );
        int radius = minRadius + random.nextInt(maxRadius - minRadius + 1);
        return new ChunkSlicedOilWorldgen.Deposit(
                new BlockPos(centerX, centerY, centerZ), radius, random.nextLong()
        );
    }

    private static boolean spawnOilSandSlice(WorldGenLevel level, ChunkSlicedOilWorldgen.Deposit deposit,
                                             WorldgenFeatureBounds targetBounds) {
        BlockPos origin = deposit.center();
        int radius = deposit.radius();
        int r2 = radius * radius;
        int r22 = r2 / 2;
        int randomBound = Math.max(1, r22 / 3);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = Math.max(origin.getX() - radius, targetBounds.minBlockX());
        int maxX = Math.min(origin.getX() + radius, targetBounds.maxBlockXExclusive());
        int minZ = Math.max(origin.getZ() - radius, targetBounds.minBlockZ());
        int maxZ = Math.min(origin.getZ() + radius, targetBounds.maxBlockZExclusive());
        int minY = Math.max(origin.getY() - radius, level.getMinBuildHeight());
        int maxY = Math.min(origin.getY() + radius, level.getMaxBuildHeight());

        for (int x = minX; x < maxX; x++) {
            int xx = x - origin.getX();
            int xxSq = xx * xx;
            for (int y = minY; y < maxY; y++) {
                int yy = y - origin.getY();
                int yySq = xxSq + yy * yy * 3;
                for (int z = minZ; z < maxZ; z++) {
                    int zz = z - origin.getZ();
                    int dist = yySq + zz * zz;
                    int fuzz = ChunkSlicedOilWorldgen.boundedCoordinateRandom(
                            deposit.detailSeed() ^ ChunkSlicedOilWorldgen.OIL_SAND_FUZZ_SALT,
                            x, y, z, randomBound
                    );
                    if (dist < r22 + fuzz) {
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
