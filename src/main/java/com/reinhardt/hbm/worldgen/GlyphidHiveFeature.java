package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class GlyphidHiveFeature extends Feature<NoneFeatureConfiguration> {
    public GlyphidHiveFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public GlyphidHiveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!HbmConfig.GLYPHID_ENABLE_HIVES.get()
                || level.getLevel().dimension() != Level.OVERWORLD
                || !level.getLevel().getServer().getWorldData().worldGenOptions().generateStructures()) {
            return false;
        }

        RandomSource random = context.random();
        if (random.nextInt(Math.max(1, HbmConfig.GLYPHID_HIVE_SPAWN.get())) != 0) {
            return false;
        }

        BlockPos origin = context.origin();
        int x = origin.getX() + random.nextInt(16) + 8;
        int z = origin.getZ() + random.nextInt(16) + 8;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        for (int k = 3; k >= -1; k--) {
            BlockPos support = new BlockPos(x, y - 1 + k, z);
            BlockState state = level.getBlockState(support);
            if (state.isCollisionShapeFullBlock(level, support)) {
                GlyphidHiveGenerator.generateSmall(level, x, y + k, z,
                        random, random.nextInt(10) == 0, true);
                return true;
            }
        }
        return false;
    }
}
