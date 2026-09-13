package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
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

/** The 1.7.10 seed-derived 528 coltan deposit used by the coltan compass. */
public final class ColtanDepositFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int VEIN_COUNT = 2;
    private static final int VEIN_SIZE = 4;
    private static final int MIN_Y = 15;
    private static final int Y_VARIANCE = 25;
    private static final int BASE_RANGE = 750;

    public ColtanDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public ColtanDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!HbmConfig.ENABLE_528_COLTAN_DEPOSIT.get()) {
            return false;
        }
        if (context.level().getLevel() != null && context.level().getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;

        Random depositRandom = new Random(level.getSeed() + 5L);
        int depositX = (int) (depositRandom.nextGaussian() * 1500.0D);
        int depositZ = (int) (depositRandom.nextGaussian() * 1500.0D);

        boolean placed = false;
        for (int k = 0; k < VEIN_COUNT; k++) {
            for (int r = 1; r <= 5; r++) {
                int x = chunkX + random.nextInt(16);
                int y = MIN_Y + random.nextInt(Y_VARIANCE);
                int z = chunkZ + random.nextInt(16);
                int range = BASE_RANGE / r;
                if (x <= depositX + range && x >= depositX - range && z <= depositZ + range && z >= depositZ - range) {
                    placed |= generateMinable(level, random, x, y, z);
                }
            }
        }
        return placed;
    }

    private static boolean generateMinable(WorldGenLevel level, RandomSource random, int centerX, int centerY, int centerZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        float angle = random.nextFloat() * (float) Math.PI;
        double startX = centerX + 8.0D + Mth.sin(angle) * VEIN_SIZE / 8.0F;
        double endX = centerX + 8.0D - Mth.sin(angle) * VEIN_SIZE / 8.0F;
        double startZ = centerZ + 8.0D + Mth.cos(angle) * VEIN_SIZE / 8.0F;
        double endZ = centerZ + 8.0D - Mth.cos(angle) * VEIN_SIZE / 8.0F;
        double startY = centerY + random.nextInt(3) - 2;
        double endY = centerY + random.nextInt(3) - 2;
        boolean placed = false;

        for (int i = 0; i <= VEIN_SIZE; i++) {
            double x = startX + (endX - startX) * i / VEIN_SIZE;
            double y = startY + (endY - startY) * i / VEIN_SIZE;
            double z = startZ + (endZ - startZ) * i / VEIN_SIZE;
            double radiusRand = random.nextDouble() * VEIN_SIZE / 16.0D;
            double horizontal = (Mth.sin(i * (float) Math.PI / VEIN_SIZE) + 1.0F) * radiusRand + 1.0D;
            double vertical = (Mth.sin(i * (float) Math.PI / VEIN_SIZE) + 1.0F) * radiusRand + 1.0D;
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
                        BlockState target = level.getBlockState(pos);
                        if (target.is(Blocks.STONE)) {
                            level.setBlock(pos, HbmBlocks.ORE_COLTAN.get().defaultBlockState(), SET_FLAGS);
                            placed = true;
                        } else if (target.is(Blocks.DEEPSLATE)) {
                            level.setBlock(pos, HbmBlocks.ORE_DEEPSLATE_COLTAN.get().defaultBlockState(), SET_FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
