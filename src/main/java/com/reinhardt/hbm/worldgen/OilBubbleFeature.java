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

public class OilBubbleFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int MIN_Y = 15;
    private static final int Y_RANGE = 25;

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
        WorldgenFeatureBounds targetBounds = WorldgenFeatureBounds.forCurrentChunk(level, context.origin());
        int targetChunkX = targetBounds.minBlockX() >> 4;
        int targetChunkZ = targetBounds.minBlockZ() >> 4;
        int configuredMaxRadius = ChunkSlicedOilWorldgen.maxConfiguredRadius(
                HbmConfig.OIL_DEPOSIT_MIN_RADIUS.get(),
                HbmConfig.OIL_DEPOSIT_MAX_RADIUS.get()
        );
        int maximumReach = Math.max(configuredMaxRadius, OilFieldSurfaceEffects.OIL_DEPOSIT_SURFACE_MAX_OFFSET);
        int searchRadius = ChunkSlicedOilWorldgen.sourceChunkSearchRadius(maximumReach);
        boolean placed = false;

        for (int sourceChunkX = targetChunkX - searchRadius; sourceChunkX <= targetChunkX + searchRadius; sourceChunkX++) {
            for (int sourceChunkZ = targetChunkZ - searchRadius; sourceChunkZ <= targetChunkZ + searchRadius; sourceChunkZ++) {
                ChunkSlicedOilWorldgen.Deposit deposit = depositForSourceChunk(level, sourceChunkX, sourceChunkZ);
                if (deposit == null) {
                    continue;
                }

                if (ChunkSlicedOilWorldgen.horizontallyIntersects(
                        targetBounds, deposit.center().getX(), deposit.center().getZ(), deposit.radius())) {
                    placed |= spawnOilSlice(level, deposit, targetBounds);
                }
                if (ChunkSlicedOilWorldgen.horizontallyIntersects(
                        targetBounds,
                        deposit.center().getX(),
                        deposit.center().getZ(),
                        OilFieldSurfaceEffects.OIL_DEPOSIT_SURFACE_MAX_OFFSET)) {
                    placed |= OilFieldSurfaceEffects.addOilDepositSurfaceSpotSlice(
                            level,
                            deposit.detailSeed(),
                            deposit.center().getX(),
                            deposit.center().getZ(),
                            targetBounds
                    );
                }
            }
        }
        return placed;
    }

    private static ChunkSlicedOilWorldgen.Deposit depositForSourceChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        RandomSource random = ChunkSlicedOilWorldgen.sourceRandom(
                level.getSeed(), chunkX, chunkZ, ChunkSlicedOilWorldgen.OIL_DEPOSIT_SALT
        );
        int centerX = (chunkX << 4) + random.nextInt(16);
        int centerZ = (chunkZ << 4) + random.nextInt(16);
        int centerY = MIN_Y + random.nextInt(Y_RANGE);
        int normalSpawnRate = Math.max(1, HbmConfig.OIL_DEPOSIT_SPAWN_RATE.get());
        int drySpawnRate = Math.max(1,
                normalSpawnRate / HbmConfig.OIL_DEPOSIT_DRY_BIOME_DIVISOR.get());
        boolean normalSelected = random.nextInt(normalSpawnRate) == 0;
        boolean drySelected = random.nextInt(drySpawnRate) == 0;
        if (!normalSelected && !drySelected) {
            return null;
        }

        Biome biome = level.getUncachedNoiseBiome(
                QuartPos.fromBlock(centerX), QuartPos.fromBlock(centerY), QuartPos.fromBlock(centerZ)
        ).value();
        boolean hotAndDry = biome.getBaseTemperature() >= 2.0F
                && biome.getModifiedClimateSettings().downfall() < 0.1F;
        if (hotAndDry ? !drySelected : !normalSelected) {
            return null;
        }

        int minRadius = ChunkSlicedOilWorldgen.minConfiguredRadius(
                HbmConfig.OIL_DEPOSIT_MIN_RADIUS.get(), HbmConfig.OIL_DEPOSIT_MAX_RADIUS.get()
        );
        int maxRadius = ChunkSlicedOilWorldgen.maxConfiguredRadius(
                HbmConfig.OIL_DEPOSIT_MIN_RADIUS.get(), HbmConfig.OIL_DEPOSIT_MAX_RADIUS.get()
        );
        int radius = minRadius + random.nextInt(maxRadius - minRadius + 1);
        return new ChunkSlicedOilWorldgen.Deposit(
                new BlockPos(centerX, centerY, centerZ), radius, random.nextLong()
        );
    }

    private static boolean spawnOilSlice(WorldGenLevel level, ChunkSlicedOilWorldgen.Deposit deposit,
                                         WorldgenFeatureBounds targetBounds) {
        BlockPos origin = deposit.center();
        int radius = deposit.radius();
        int r2 = radius * radius;
        int r22 = r2 / 2;
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
