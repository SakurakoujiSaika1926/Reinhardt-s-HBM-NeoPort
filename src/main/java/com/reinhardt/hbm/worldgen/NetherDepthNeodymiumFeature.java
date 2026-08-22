package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class NetherDepthNeodymiumFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int SIZE = 7;
    private static final double FILL = 0.6D;
    private static final int CHANCE = 16;

    public NetherDepthNeodymiumFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public NetherDepthNeodymiumFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (context.level().getLevel() != null && context.level().getLevel().dimension() != Level.NETHER) {
            return false;
        }
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int chunkX = context.origin().getX() & ~15;
        int chunkZ = context.origin().getZ() & ~15;
        boolean placed = false;
        placed |= generateCondition(level, chunkX, 0, 3, chunkZ, random);
        placed |= generateCondition(level, chunkX, 125, 3, chunkZ, random);
        return placed;
    }

    private static boolean generateCondition(WorldGenLevel level, int chunkX, int yMin, int yDev, int chunkZ, RandomSource random) {
        if (random.nextInt(CHANCE) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16) + 8;
        int y = yMin + random.nextInt(yDev);
        int z = chunkZ + random.nextInt(16) + 8;
        return generate(level, x, y, z, random);
    }

    private static boolean generate(WorldGenLevel level, int centerX, int centerY, int centerZ, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = centerX - SIZE; x <= centerX + SIZE; x++) {
            int dx = centerX - x;
            for (int y = centerY - SIZE; y <= centerY + SIZE; y++) {
                if (y < 1 || y > 126 || y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                    continue;
                }
                int dy = centerY - y;
                for (int z = centerZ - SIZE; z <= centerZ + SIZE; z++) {
                    int dz = centerZ - z;
                    pos.set(x, y, z);
                    BlockState target = level.getBlockState(pos);
                    if (!target.is(Blocks.NETHERRACK) && !target.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    double len = Math.sqrt(dx * (double) dx + dy * (double) dy + dz * (double) dz);
                    if (len + random.nextInt(2) < SIZE * FILL) {
                        level.setBlock(pos, HbmBlocks.ORE_DEPTH_NETHER_NEODYMIUM.get().defaultBlockState(), SET_FLAGS);
                        placed = true;
                    } else if (len + random.nextInt(2) <= SIZE) {
                        level.setBlock(pos, HbmBlocks.STONE_DEPTH_NETHER.get().defaultBlockState(), SET_FLAGS);
                    }
                }
            }
        }
        return placed;
    }
}
