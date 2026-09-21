package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.block.CaveSpikeBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Direct port of HBM 1.7.10's OreCave decorator for sulfur and asbestos caves. */
public final class OreCaveFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final double SCALE = 0.01D;

    private final Supplier<? extends Block> ore;
    private final int spikeMaterial;
    private final double threshold;
    private final int rangeMult;
    private final int yLevel;
    private final int maxRange;
    private final BlockState fluid;
    private final BooleanSupplier enabled;

    public static OreCaveFeature sulfur() {
        return new OreCaveFeature(HbmBlocks.STONE_RESOURCE_SULFUR, 0, 1.5D, 20, 30, 20,
                HbmBlocks.SULFURIC_ACID_BLOCK.get().defaultBlockState(),
                () -> HbmConfig.ENABLE_SULFUR_CAVES.get());
    }

    public static OreCaveFeature asbestos() {
        return new OreCaveFeature(HbmBlocks.STONE_RESOURCE_ASBESTOS, 1, 1.75D, 20, 25, 20,
                null,
                () -> HbmConfig.ENABLE_ASBESTOS_CAVES.get());
    }

    public OreCaveFeature(Supplier<? extends Block> ore, int spikeMaterial, double threshold, int rangeMult, int yLevel, int maxRange,
                          BlockState fluid, BooleanSupplier enabled) {
        super(NoneFeatureConfiguration.CODEC);
        this.ore = ore;
        this.spikeMaterial = spikeMaterial;
        this.threshold = threshold;
        this.rangeMult = rangeMult;
        this.yLevel = yLevel;
        this.maxRange = maxRange;
        this.fluid = fluid;
        this.enabled = enabled;
    }

    public OreCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
        this.ore = HbmBlocks.STONE_RESOURCE_SULFUR;
        this.spikeMaterial = 0;
        this.threshold = 1.5D;
        this.rangeMult = 20;
        this.yLevel = 30;
        this.maxRange = 20;
        this.fluid = HbmBlocks.SULFURIC_ACID_BLOCK.get().defaultBlockState();
        this.enabled = () -> true;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!this.enabled.getAsBoolean()
                || level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        BlockPos origin = context.origin();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        LegacyPerlin noise = new LegacyPerlin(new Random(level.getSeed() + this.yLevel), 2);
        RandomSource random = context.random();
        BlockState ore = this.ore.get().defaultBlockState();
        BlockState stalactite = HbmBlocks.STALACTITE.get().defaultBlockState()
                .setValue(CaveSpikeBlock.MATERIAL, this.spikeMaterial);
        BlockState stalagmite = HbmBlocks.STALAGMITE.get().defaultBlockState()
                .setValue(CaveSpikeBlock.MATERIAL, this.spikeMaterial);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int x = chunkX + 8; x < chunkX + 24; x++) {
            for (int z = chunkZ + 8; z < chunkZ + 24; z++) {
                double sample = noise.noise(x * SCALE, z * SCALE);
                if (sample <= this.threshold) {
                    continue;
                }

                int range = (int) ((sample - this.threshold) * this.rangeMult);
                if (range > this.maxRange) {
                    range = (this.maxRange * 2) - range;
                }
                if (range < 0) {
                    continue;
                }

                for (int y = this.yLevel - range; y <= this.yLevel + range; y++) {
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    pos.set(x, y, z);
                    BlockState target = level.getBlockState(pos);
                    if (isReplaceableStone(target)) {
                        placed |= tryPlaceOreOrFluid(level, random, pos, ore);
                    } else if (canTrySpike(level, random, pos, target)) {
                        if (stalactite.canSurvive(level, pos)) {
                            level.setBlock(pos, stalactite, FLAGS);
                            placed = true;
                        } else if (stalagmite.canSurvive(level, pos)) {
                            level.setBlock(pos, stalagmite, FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    private boolean tryPlaceOreOrFluid(WorldGenLevel level, RandomSource random, BlockPos pos, BlockState ore) {
        boolean shouldGen = false;
        boolean canGenFluid = this.fluid != null && random.nextBoolean();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.values()) {
            neighborPos.setWithOffset(pos, direction);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.isAir() || isSpike(neighbor)) {
                shouldGen = true;
            }

            if (shouldGen && (this.fluid == null || !canGenFluid)) {
                break;
            }

            if (this.fluid != null) {
                switch (direction) {
                    case UP -> {
                        if (!neighbor.isAir() && !isSpike(neighbor)) {
                            canGenFluid = false;
                        }
                    }
                    case DOWN -> {
                        if (!neighbor.isCollisionShapeFullBlock(level, neighborPos)) {
                            canGenFluid = false;
                        }
                    }
                    case NORTH, SOUTH, EAST, WEST -> {
                        if (!neighbor.isCollisionShapeFullBlock(level, neighborPos) && !neighbor.is(this.fluid.getBlock())) {
                            canGenFluid = false;
                        }
                    }
                }
            }
        }

        if (this.fluid != null && canGenFluid) {
            level.setBlock(pos, this.fluid, FLAGS);
            BlockPos below = pos.below();
            if (below.getY() >= level.getMinBuildHeight()) {
                level.setBlock(below, ore, FLAGS);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos side = pos.relative(direction);
                BlockState neighbor = level.getBlockState(side);
                if (neighbor.isCollisionShapeFullBlock(level, side)) {
                    level.setBlock(side, ore, FLAGS);
                }
            }
            return true;
        }

        if (shouldGen) {
            level.setBlock(pos, ore, FLAGS);
            return true;
        }
        return false;
    }

    private static boolean isReplaceableStone(BlockState state) {
        return state.is(Blocks.STONE);
    }

    private static boolean canTrySpike(WorldGenLevel level, RandomSource random, BlockPos pos, BlockState target) {
        return (target.isAir() || !target.isCollisionShapeFullBlock(level, pos))
                && target.getFluidState().isEmpty()
                && random.nextInt(5) == 0;
    }

    private static boolean isSpike(BlockState state) {
        return state.is(HbmBlocks.STALACTITE.get()) || state.is(HbmBlocks.STALAGMITE.get());
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
