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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** The 1.7.10 australium patch: a small, fixed region around (-400, -400). */
public final class AustraliumRegionFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int MIN_X = -450;
    private static final int MAX_X = -350;
    private static final int MIN_Z = -450;
    private static final int MAX_Z = -350;
    private static final int VEIN_SIZE = 50;

    public AustraliumRegionFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public AustraliumRegionFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (context.level().getLevel() != null && context.level().getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        boolean placed = false;
        // Old code attempted 0..3 veins per chunk and checked each random
        // position against the 100x100 australium region.
        for (int i = 0, attempts = random.nextInt(4); i < attempts; i++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            if (x < MIN_X || x > MAX_X || z < MIN_Z || z > MAX_Z) {
                continue;
            }
            int y = 15 + random.nextInt(15);
            placed |= generateVein(context.level(), random, x, y, z);
        }
        return placed;
    }

    private static boolean generateVein(WorldGenLevel level, RandomSource random,
                                        int centerX, int centerY, int centerZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        float angle = random.nextFloat() * (float) Math.PI;
        double startX = centerX + 8.0D + Mth.sin(angle) * VEIN_SIZE / 8.0F;
        double endX = centerX + 8.0D - Mth.sin(angle) * VEIN_SIZE / 8.0F;
        double startZ = centerZ + 8.0D + Mth.cos(angle) * VEIN_SIZE / 8.0F;
        double endZ = centerZ + 8.0D - Mth.cos(angle) * VEIN_SIZE / 8.0F;
        double startY = centerY + random.nextInt(3) - 2;
        double endY = centerY + random.nextInt(3) - 2;
        for (int i = 0; i <= VEIN_SIZE; i++) {
            double x = startX + (endX - startX) * i / VEIN_SIZE;
            double y = startY + (endY - startY) * i / VEIN_SIZE;
            double z = startZ + (endZ - startZ) * i / VEIN_SIZE;
            double radius = random.nextDouble() * VEIN_SIZE / 16.0D;
            double horizontal = (Mth.sin(i * (float) Math.PI / VEIN_SIZE) + 1.0F) * radius + 1.0D;
            double vertical = horizontal;
            int minX = Mth.floor(x - horizontal / 2.0D);
            int minY = Mth.floor(y - vertical / 2.0D);
            int minZ = Mth.floor(z - horizontal / 2.0D);
            int maxX = Mth.floor(x + horizontal / 2.0D);
            int maxY = Mth.floor(y + vertical / 2.0D);
            int maxZ = Mth.floor(z + horizontal / 2.0D);
            for (int px = minX; px <= maxX; px++) {
                double dx = (px + 0.5D - x) / (horizontal / 2.0D);
                if (dx * dx >= 1.0D) continue;
                for (int py = minY; py <= maxY; py++) {
                    if (py < level.getMinBuildHeight() || py >= level.getMaxBuildHeight()) continue;
                    double dy = (py + 0.5D - y) / (vertical / 2.0D);
                    if (dx * dx + dy * dy >= 1.0D) continue;
                    for (int pz = minZ; pz <= maxZ; pz++) {
                        double dz = (pz + 0.5D - z) / (horizontal / 2.0D);
                        if (dx * dx + dy * dy + dz * dz >= 1.0D) continue;
                        pos.set(px, py, pz);
                        if (level.getBlockState(pos).is(Blocks.STONE)) {
                            level.setBlock(pos, HbmBlocks.ORE_AUSTRALIUM.get().defaultBlockState(), SET_FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
