package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class SchistStratumFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final double SCALE = 0.01D;
    private static final int THRESHOLD = 5;

    private static final OreEntry[] ORES = {
            new OreEntry(25, 6, HbmBlocks.ORE_GNEISS_IRON),
            new OreEntry(10, 6, HbmBlocks.ORE_GNEISS_GOLD),
            new OreEntry(21, 6, HbmBlocks.ORE_GNEISS_URANIUM),
            new OreEntry(6, 6, HbmBlocks.ORE_GNEISS_ASBESTOS),
            new OreEntry(6, 6, HbmBlocks.ORE_GNEISS_LITHIUM),
            new OreEntry(6, 6, HbmBlocks.ORE_GNEISS_RARE),
            new OreEntry(15, 10, HbmBlocks.ORE_GNEISS_GAS)
    };

    public SchistStratumFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public SchistStratumFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (context.level().getLevel() != null && context.level().getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        LegacyPerlin noise = new LegacyPerlin(new Random(level.getSeed()), 4);
        boolean placed = generateStratum(level, noise, chunkX, chunkZ);
        for (OreEntry ore : ORES) {
            placed |= generateOre(level, random, chunkX, chunkZ, ore);
        }
        return placed;
    }

    private static boolean generateStratum(WorldGenLevel level, LegacyPerlin noise, int chunkX, int chunkZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = chunkX + 8; x < chunkX + 24; x++) {
            for (int z = chunkZ + 8; z < chunkZ + 24; z++) {
                double n = noise.noise(x * SCALE, z * SCALE);
                if (n <= THRESHOLD) {
                    continue;
                }
                int range = (int) ((n - THRESHOLD) * 3.0D);
                if (range > 4) {
                    range = 8 - range;
                }
                if (range < 0) {
                    continue;
                }
                for (int y = 30 - range; y <= 30 + range; y++) {
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    pos.set(x, y, z);
                    BlockState target = level.getBlockState(pos);
                    if (target.is(Blocks.STONE)) {
                        level.setBlock(pos, HbmBlocks.STONE_GNEISS.get().defaultBlockState(), SET_FLAGS);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    private static boolean generateOre(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, OreEntry ore) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int i = 0; i < ore.veinCount; i++) {
            int centerX = chunkX + random.nextInt(16);
            int centerY = 30 + random.nextInt(10);
            int centerZ = chunkZ + random.nextInt(16);
            placed |= generateMinable(level, random, pos, centerX, centerY, centerZ, ore.amount, ore.block.get());
        }
        return placed;
    }

    private static boolean generateMinable(WorldGenLevel level, RandomSource random, BlockPos.MutableBlockPos pos,
                                           int centerX, int centerY, int centerZ, int amount, Block ore) {
        float angle = random.nextFloat() * (float) Math.PI;
        double startX = centerX + 8.0D + Mth.sin(angle) * amount / 8.0F;
        double endX = centerX + 8.0D - Mth.sin(angle) * amount / 8.0F;
        double startZ = centerZ + 8.0D + Mth.cos(angle) * amount / 8.0F;
        double endZ = centerZ + 8.0D - Mth.cos(angle) * amount / 8.0F;
        double startY = centerY + random.nextInt(3) - 2;
        double endY = centerY + random.nextInt(3) - 2;
        boolean placed = false;

        for (int i = 0; i <= amount; i++) {
            double x = startX + (endX - startX) * i / amount;
            double y = startY + (endY - startY) * i / amount;
            double z = startZ + (endZ - startZ) * i / amount;
            double radiusRand = random.nextDouble() * amount / 16.0D;
            double horizontal = (Mth.sin(i * (float) Math.PI / amount) + 1.0F) * radiusRand + 1.0D;
            double vertical = (Mth.sin(i * (float) Math.PI / amount) + 1.0F) * radiusRand + 1.0D;
            int minX = Mth.floor(x - horizontal / 2.0D);
            int minY = Mth.floor(y - vertical / 2.0D);
            int minZ = Mth.floor(z - horizontal / 2.0D);
            int maxX = Mth.floor(x + horizontal / 2.0D);
            int maxY = Mth.floor(y + vertical / 2.0D);
            int maxZ = Mth.floor(z + horizontal / 2.0D);

            for (int px = minX; px <= maxX; px++) {
                double dx = (px + 0.5D - x) / (horizontal / 2.0D);
                if (dx * dx >= 1.0D) {
                    continue;
                }
                for (int py = minY; py <= maxY; py++) {
                    if (py < level.getMinBuildHeight() || py >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    double dy = (py + 0.5D - y) / (vertical / 2.0D);
                    if (dx * dx + dy * dy >= 1.0D) {
                        continue;
                    }
                    for (int pz = minZ; pz <= maxZ; pz++) {
                        double dz = (pz + 0.5D - z) / (horizontal / 2.0D);
                        if (dx * dx + dy * dy + dz * dz >= 1.0D) {
                            continue;
                        }
                        pos.set(px, py, pz);
                        if (level.getBlockState(pos).is(HbmBlocks.STONE_GNEISS.get())) {
                            level.setBlock(pos, ore.defaultBlockState(), SET_FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    private record OreEntry(int veinCount, int amount, net.neoforged.neoforge.registries.DeferredBlock<Block> block) {
    }

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
