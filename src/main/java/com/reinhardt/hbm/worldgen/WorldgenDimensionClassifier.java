package com.reinhardt.hbm.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;

final class WorldgenDimensionClassifier {
    private WorldgenDimensionClassifier() {
    }

    static boolean isNetherLike(WorldGenLevel level, BlockPos samplePos) {
        if (level.getBiome(samplePos).is(BiomeTags.IS_NETHER)) {
            return true;
        }
        Level realLevel = level.getLevel();
        return realLevel != null && realLevel.dimensionType().ultraWarm();
    }
}
