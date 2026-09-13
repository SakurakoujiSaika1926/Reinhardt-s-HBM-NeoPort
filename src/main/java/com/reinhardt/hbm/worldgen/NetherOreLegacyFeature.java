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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class NetherOreLegacyFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final OreEntry[] ORES = {
            new OreEntry(8, 6, 0, 127, HbmBlocks.ORE_NETHER_URANIUM),
            new OreEntry(10, 10, 0, 127, HbmBlocks.ORE_NETHER_TUNGSTEN),
            new OreEntry(26, 12, 0, 127, HbmBlocks.ORE_NETHER_SULFUR),
            new OreEntry(24, 6, 0, 127, HbmBlocks.ORE_NETHER_FIRE),
            new OreEntry(8, 32, 16, 96, HbmBlocks.ORE_NETHER_COAL),
            new OreEntry(2, 6, 100, 26, HbmBlocks.ORE_NETHER_COBALT)
    };
    private static final OreEntry PLUTONIUM = new OreEntry(8, 4, 0, 127, HbmBlocks.ORE_NETHER_PLUTONIUM);

    public NetherOreLegacyFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public NetherOreLegacyFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (context.level().getLevel() != null && context.level().getLevel().dimension() != Level.NETHER) {
            return false;
        }
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        boolean placed = false;
        if (HbmConfig.ENABLE_NETHER_ORES.get()) {
            for (OreEntry ore : ORES) {
                placed |= generateOre(level, random, chunkX, chunkZ, ore);
            }
            if (HbmConfig.ENABLE_NETHER_PLUTONIUM_ORE.get()) {
                placed |= generateOre(level, random, chunkX, chunkZ, PLUTONIUM);
            }
        }
        placed |= generateSmoldering(level, random, chunkX, chunkZ);
        placed |= generateGeyser(level, random, chunkX, chunkZ);
        return placed;
    }

    private static boolean generateOre(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, OreEntry ore) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int i = 0; i < ore.veinCount; i++) {
            int x = chunkX + random.nextInt(16);
            int y = ore.minHeight + (ore.variance > 0 ? random.nextInt(ore.variance) : 0);
            int z = chunkZ + random.nextInt(16);
            placed |= generateMinable(level, random, pos, x, y, z, ore.amount, ore.block.get());
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
                        if (level.getBlockState(pos).is(Blocks.NETHERRACK)) {
                            level.setBlock(pos, ore.defaultBlockState(), SET_FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static boolean generateSmoldering(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int k = 0; k < 30; k++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            int d = 16 + random.nextInt(96);
            for (int y = d - 5; y <= d; y++) {
                if (y < level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) {
                    continue;
                }
                pos.set(x, y, z);
                if (level.getBlockState(pos).is(Blocks.NETHERRACK) && level.isEmptyBlock(pos.above())) {
                    level.setBlock(pos, HbmBlocks.ORE_NETHER_SMOLDERING.get().defaultBlockState(), SET_FLAGS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean generateGeyser(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int top = 16 + random.nextInt(96);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int y = top - 5; y <= top; y++) {
            pos.set(x, y, z);
            if (level.getBlockState(pos).is(Blocks.NETHERRACK) && level.isEmptyBlock(pos.above())) {
                level.setBlock(pos, HbmBlocks.GEYSIR_NETHER.get().defaultBlockState(), SET_FLAGS);
                placed = true;
            }
        }
        return placed;
    }

    private record OreEntry(int veinCount, int amount, int minHeight, int variance,
                            net.neoforged.neoforge.registries.DeferredBlock<Block> block) {
    }
}
