package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.Supplier;

/**
 * Preserves the bottom-layer cluster distribution while using only the
 * canonical deepslate cluster blocks and vanilla deepslate host rock.
 */
public final class BedrockDeepslateClusterFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int SIZE = 5;
    private static final double FILL = 0.6D;

    private final Supplier<? extends Block> ore;
    private final int chance;

    public BedrockDeepslateClusterFeature(Supplier<? extends Block> ore, int chance) {
        this(NoneFeatureConfiguration.CODEC, ore, chance);
    }

    public BedrockDeepslateClusterFeature(
            Codec<NoneFeatureConfiguration> codec,
            Supplier<? extends Block> ore,
            int chance
    ) {
        super(codec);
        this.ore = ore;
        this.chance = chance;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        if (this.chance <= 0 || random.nextInt(this.chance) != 0) {
            return false;
        }

        BlockPos origin = context.origin();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        int x = chunkX + random.nextInt(16) + 8;
        int y = context.level().getMinBuildHeight() + random.nextInt(3);
        int z = chunkZ + random.nextInt(16) + 8;
        return generate(context.level(), x, y, z, this.ore.get(), random);
    }

    private static boolean generate(
            WorldGenLevel level,
            int centerX,
            int centerY,
            int centerZ,
            Block ore,
            RandomSource random
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = centerX - SIZE; x <= centerX + SIZE; x++) {
            int dx = centerX - x;
            for (int y = centerY - SIZE; y <= centerY + SIZE; y++) {
                if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                    continue;
                }
                int dy = centerY - y;
                for (int z = centerZ - SIZE; z <= centerZ + SIZE; z++) {
                    int dz = centerZ - z;
                    pos.set(x, y, z);
                    BlockState current = level.getBlockState(pos);
                    if (!current.is(Blocks.DEEPSLATE) && !current.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    double length = Math.sqrt(dx * (double) dx + dy * (double) dy + dz * (double) dz);
                    if (length + random.nextInt(2) < SIZE * FILL) {
                        level.setBlock(pos, ore.defaultBlockState(), SET_FLAGS);
                        placed = true;
                    } else if (length + random.nextInt(2) <= SIZE) {
                        level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), SET_FLAGS);
                    }
                }
            }
        }
        return placed;
    }
}
