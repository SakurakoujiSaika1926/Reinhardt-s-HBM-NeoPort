package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Port of HBM 1.7.10 {@code OreLayer3D} for the hematite,
 * bauxite and malachite deposits.  These are broad noise layers, not vanilla
 * ore veins; keep the per-layer constants and legacy construction-order noise
 * ids instead of folding them into the generic ore JSON pipeline.
 */
public final class ResourceOreLayer3DFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS;

    private final Supplier<? extends Block> resource;
    private final double scaleH;
    private final double scaleV;
    private final double threshold;
    private final BooleanSupplier enabled;
    private final IntSupplier legacyId;

    public static ResourceOreLayer3DFeature hematite() {
        return new ResourceOreLayer3DFeature(
                HbmBlocks.STONE_RESOURCE_HEMATITE,
                0.04D,
                0.25D,
                230.0D,
                () -> HbmConfig.ENABLE_HEMATITE_DEPOSITS.get(),
                () -> 0
        );
    }

    public static ResourceOreLayer3DFeature bauxite() {
        return new ResourceOreLayer3DFeature(
                HbmBlocks.STONE_RESOURCE_BAUXITE,
                0.03D,
                0.15D,
                300.0D,
                () -> HbmConfig.ENABLE_BAUXITE_DEPOSITS.get(),
                () -> HbmConfig.ENABLE_HEMATITE_DEPOSITS.get() ? 1 : 0
        );
    }

    public static ResourceOreLayer3DFeature malachite() {
        return new ResourceOreLayer3DFeature(
                HbmBlocks.STONE_RESOURCE_MALACHITE,
                0.1D,
                0.15D,
                275.0D,
                () -> HbmConfig.ENABLE_MALACHITE_DEPOSITS.get(),
                () -> (HbmConfig.ENABLE_HEMATITE_DEPOSITS.get() ? 1 : 0)
                        + (HbmConfig.ENABLE_BAUXITE_DEPOSITS.get() ? 1 : 0)
        );
    }

    private ResourceOreLayer3DFeature(Supplier<? extends Block> resource, double scaleH, double scaleV, double threshold,
                                      BooleanSupplier enabled, IntSupplier legacyId) {
        super(NoneFeatureConfiguration.CODEC);
        this.resource = resource;
        this.scaleH = scaleH;
        this.scaleV = scaleV;
        this.threshold = threshold;
        this.enabled = enabled;
        this.legacyId = legacyId;
    }

    public ResourceOreLayer3DFeature(Codec<NoneFeatureConfiguration> codec, Supplier<? extends Block> resource, double scaleH,
                                     double scaleV, double threshold, BooleanSupplier enabled,
                                     IntSupplier legacyId) {
        super(codec);
        this.resource = resource;
        this.scaleH = scaleH;
        this.scaleV = scaleV;
        this.threshold = threshold;
        this.enabled = enabled;
        this.legacyId = legacyId;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!this.enabled.getAsBoolean()) {
            return false;
        }
        if (context.level().getLevel() != null && context.level().getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        int id = this.legacyId.getAsInt();
        LegacyPerlin noiseX = new LegacyPerlin(new Random(level.getSeed() + 101L + id), 4);
        LegacyPerlin noiseY = new LegacyPerlin(new Random(level.getSeed() + 102L + id), 4);
        LegacyPerlin noiseZ = new LegacyPerlin(new Random(level.getSeed() + 103L + id), 4);
        double[][] cacheX = new double[16][65];
        double[][] cacheZ = new double[16][65];

        for (int o = 0; o < 16; o++) {
            for (int y = 64; y > 5; y--) {
                cacheX[o][y] = noiseX.noise(y * this.scaleV, (chunkZ + 8 + o) * this.scaleH);
                cacheZ[o][y] = noiseZ.noise((chunkX + 8 + o) * this.scaleH, y * this.scaleV);
            }
        }

        BlockState resource = this.resource.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int ox = 0; ox < 16; ox++) {
            int x = chunkX + 8 + ox;

            for (int oz = 0; oz < 16; oz++) {
                int z = chunkZ + 8 + oz;
                double ny = noiseY.noise(x * this.scaleH, z * this.scaleH);

                for (int y = 64; y > 5; y--) {
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    double nx = cacheX[oz][y];
                    // 1.7.10 computed cacheZ but used cacheX here; keep the effective old output.
                    double nz = cacheX[ox][y];

                    if (nx * ny * nz <= this.threshold) {
                        continue;
                    }

                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.STONE)) {
                        level.setBlock(pos, resource, SET_FLAGS);
                        placed = true;
                    }
                }
            }
        }

        return placed;
    }

    /**
     * Equivalent of Minecraft 1.7.10 NoiseGeneratorPerlin#func_151601_a.
     */
    private static final class LegacyPerlin {
        private final LegacyImprovedNoise[] generators;
        private final int octaves;

        private LegacyPerlin(Random random, int octaves) {
            this.octaves = octaves;
            this.generators = new LegacyImprovedNoise[octaves];
            for (int i = 0; i < octaves; i++) {
                this.generators[i] = new LegacyImprovedNoise(random);
            }
        }

        private double noise(double x, double z) {
            double result = 0.0D;
            double divisor = 1.0D;
            for (int i = 0; i < this.octaves; i++) {
                result += this.generators[i].noise(x * divisor, z * divisor) / divisor;
                divisor /= 2.0D;
            }
            return result;
        }
    }

    private static final class LegacyImprovedNoise {
        private final int[] permutations = new int[512];
        private final double xCoord;
        private final double yCoord;
        private final double zCoord;

        private LegacyImprovedNoise(Random random) {
            this.xCoord = random.nextDouble() * 256.0D;
            this.yCoord = random.nextDouble() * 256.0D;
            this.zCoord = random.nextDouble() * 256.0D;
            for (int i = 0; i < 256; this.permutations[i] = i++) {
            }
            for (int i = 0; i < 256; i++) {
                int j = random.nextInt(256 - i) + i;
                int value = this.permutations[i];
                this.permutations[i] = this.permutations[j];
                this.permutations[j] = value;
                this.permutations[i + 256] = this.permutations[i];
            }
        }

        private double noise(double x, double z) {
            double sampleX = x + this.xCoord;
            double sampleY = this.yCoord;
            double sampleZ = z + this.zCoord;
            int floorX = Mth.floor(sampleX);
            int floorY = Mth.floor(sampleY);
            int floorZ = Mth.floor(sampleZ);
            sampleX -= floorX;
            sampleY -= floorY;
            sampleZ -= floorZ;
            floorX &= 255;
            floorY &= 255;
            floorZ &= 255;
            double fadeX = fade(sampleX);
            double fadeY = fade(sampleY);
            double fadeZ = fade(sampleZ);
            int a = this.permutations[floorX] + floorY;
            int aa = this.permutations[a] + floorZ;
            int ab = this.permutations[a + 1] + floorZ;
            int b = this.permutations[floorX + 1] + floorY;
            int ba = this.permutations[b] + floorZ;
            int bb = this.permutations[b + 1] + floorZ;
            return lerp(fadeZ,
                    lerp(fadeY,
                            lerp(fadeX, grad(this.permutations[aa], sampleX, sampleY, sampleZ),
                                    grad(this.permutations[ba], sampleX - 1.0D, sampleY, sampleZ)),
                            lerp(fadeX, grad(this.permutations[ab], sampleX, sampleY - 1.0D, sampleZ),
                                    grad(this.permutations[bb], sampleX - 1.0D, sampleY - 1.0D, sampleZ))),
                    lerp(fadeY,
                            lerp(fadeX, grad(this.permutations[aa + 1], sampleX, sampleY, sampleZ - 1.0D),
                                    grad(this.permutations[ba + 1], sampleX - 1.0D, sampleY, sampleZ - 1.0D)),
                            lerp(fadeX, grad(this.permutations[ab + 1], sampleX, sampleY - 1.0D, sampleZ - 1.0D),
                                    grad(this.permutations[bb + 1], sampleX - 1.0D, sampleY - 1.0D, sampleZ - 1.0D))));
        }

        private static double fade(double value) {
            return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
        }

        private static double lerp(double delta, double start, double end) {
            return start + delta * (end - start);
        }

        private static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 4 ? y : h != 12 && h != 14 ? z : x;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }
}
