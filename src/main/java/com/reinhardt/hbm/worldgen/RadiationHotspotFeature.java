package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/** HBM 1.7.10 MapGenCrater entry used for desert Sellafield radiation hotspots. */
public final class RadiationHotspotFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int MIN_SIZE = 8;
    private static final int MAX_SIZE = 64;
    private static final int RANGE = (MAX_SIZE / 8) + 1;

    public RadiationHotspotFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public RadiationHotspotFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int frequency = HbmConfig.RADIATION_HOTSPOT_SPAWN_RATE.get();
        if (!HbmConfig.ENABLE_RADIATION_HOTSPOTS.get()
                || frequency <= 0
                || level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        BlockPos origin = context.origin();
        int currentChunkX = Math.floorDiv(origin.getX(), 16);
        int currentChunkZ = Math.floorDiv(origin.getZ(), 16);
        Random seedRandom = new Random(level.getSeed());
        long xSeed = seedRandom.nextLong();
        long zSeed = seedRandom.nextLong();
        boolean placed = false;

        for (int offsetChunkX = currentChunkX - RANGE; offsetChunkX <= currentChunkX + RANGE; offsetChunkX++) {
            for (int offsetChunkZ = currentChunkZ - RANGE; offsetChunkZ <= currentChunkZ + RANGE; offsetChunkZ++) {
                long craterSeedX = (long) offsetChunkX * xSeed;
                long craterSeedZ = (long) offsetChunkZ * zSeed;
                Random random = new Random(craterSeedX ^ craterSeedZ ^ level.getSeed());
                if (random.nextInt(frequency) != 0 || !isLegacyDesert(level, offsetChunkX, offsetChunkZ)) {
                    continue;
                }
                placed |= carveCurrentChunkSlice(level, random, currentChunkX, currentChunkZ, offsetChunkX, offsetChunkZ);
            }
        }
        return placed;
    }

    private static boolean carveCurrentChunkSlice(WorldGenLevel level, Random random,
                                                  int currentChunkX, int currentChunkZ,
                                                  int offsetChunkX, int offsetChunkZ) {
        int chunkMinX = currentChunkX << 4;
        int chunkMinZ = currentChunkZ << 4;
        int relativeChunkX = -offsetChunkX + currentChunkX;
        int relativeChunkZ = -offsetChunkZ + currentChunkZ;
        double radius = random.nextInt(MAX_SIZE - MIN_SIZE) + MIN_SIZE;
        double depth = radius * 0.35D;
        BlockState fill = HbmBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int bx = 15; bx >= 0; bx--) {
            for (int bz = 15; bz >= 0; bz--) {
                int worldX = chunkMinX + bx;
                int worldZ = chunkMinZ + bz;
                for (int y = Math.min(127, level.getMaxBuildHeight() - 1); y >= Math.max(0, level.getMinBuildHeight()); y--) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !(state.isCollisionShapeFullBlock(level, pos) || !state.getFluidState().isEmpty())) {
                        continue;
                    }

                    int relativeX = relativeChunkX * 16 + bx;
                    int relativeZ = relativeChunkZ * 16 + bz;
                    double distance = Math.sqrt(relativeX * (double) relativeX + relativeZ * (double) relativeZ);
                    if (distance - random.nextInt(3) <= radius) {
                        int carveDepth = Mth.clamp((int) depthFunc(distance, radius, depth), 0, y - 1);
                        for (int i = 0; i < carveDepth; i++) {
                            pos.set(worldX, y - i, worldZ);
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                            placed = true;
                        }

                        int fillStartY = y - carveDepth;
                        int fillDepth = Math.min(3, fillStartY - 1);
                        if (distance + random.nextInt(3) <= radius / 3.0D) {
                            for (int i = 0; i < fillDepth; i++) {
                                pos.set(worldX, fillStartY - i, worldZ);
                                level.setBlock(pos, fill, FLAGS);
                                placed = true;
                            }
                        } else {
                            for (int i = 0; i < fillDepth; i++) {
                                pos.set(worldX, fillStartY - i, worldZ);
                                level.setBlock(pos, fill, FLAGS);
                                placed = true;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return placed;
    }

    private static boolean isLegacyDesert(WorldGenLevel level, int chunkX, int chunkZ) {
        Holder<Biome> biome = level.getLevel().getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(chunkX << 4),
                QuartPos.fromBlock(64),
                QuartPos.fromBlock(chunkZ << 4),
                level.getLevel().getChunkSource().randomState().sampler()
        );
        return biome
                .unwrapKey()
                .map(key -> key.equals(Biomes.DESERT))
                .orElse(false);
    }

    private static double depthFunc(double x, double radius, double depth) {
        return -Math.pow(x, 2) / Math.pow(radius, 2) * depth + depth;
    }
}
