package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;

public class MeteoriteFeature extends Feature<NoneFeatureConfiguration> {
    public MeteoriteFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public MeteoriteFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int spawnRate = Math.max(1, HbmConfig.METEORITE_SPAWN.get());
        if (context.random().nextInt(spawnRate) != 0) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int x = origin.getX() + context.random().nextInt(16) + 8;
        int z = origin.getZ() + context.random().nextInt(16) + 8;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - context.random().nextInt(10);
        if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockPos supportPos = new BlockPos(x, y - 2, z);
        BlockState support = level.getBlockState(supportPos);
        FluidState fluid = support.getFluidState();
        if (support.isAir() || !fluid.isEmpty()) {
            return false;
        }

        MeteoriteGenerator.generate(level, context.random(), new BlockPos(x, y, z), false, false, false);
        return true;
    }
}
