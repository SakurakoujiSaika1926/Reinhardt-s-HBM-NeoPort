package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class ChlorineGeyserFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final String[] SOLID_FOOTPRINT = {
            " XXX ",
            "XXXXX",
            "XXXXX",
            "XXXXX",
            " XXX "
    };
    private static final String[] RESERVOIR = {
            " XXX ",
            "XWYWX",
            "XYWWX",
            "XWYYX",
            " XXX "
    };
    private static final String[] CAVITY = {
            " XXX ",
            "X...X",
            "X...X",
            "X...X",
            " XXX "
    };
    private static final String[] NECK = {
            " XXX ",
            "XX.XX",
            "X...X",
            "XX.XX",
            " XXX "
    };
    private static final String[] SURFACE = {
            " GGG ",
            "GgSGS",
            "SGEGg",
            "GSGgG",
            " GGG "
    };

    public ChlorineGeyserFeature() {
        this(NoneFeatureConfiguration.CODEC);
    }

    public ChlorineGeyserFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int rate = HbmConfig.CHLORINE_GEYSER_SPAWN_RATE.get();
        if (rate <= 0 || context.random().nextInt(rate) != 0) {
            return false;
        }

        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD
                || !HbmConfig.legacyDungeonGenerationEnabled(level.getLevel().getServer().getWorldData().worldGenOptions().generateStructures())) {
            return false;
        }
        BlockPos origin = context.origin();
        int x = origin.getX();
        int z = origin.getZ();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos center = new BlockPos(x, surfaceY - 1, z);
        if (!level.getBlockState(center).is(Blocks.GRASS_BLOCK)) {
            return false;
        }

        placeLayer(level, center, -5, SOLID_FOOTPRINT);
        placeLayer(level, center, -4, SOLID_FOOTPRINT);
        placeLayer(level, center, -3, RESERVOIR);
        placeLayer(level, center, -2, CAVITY);
        placeLayer(level, center, -1, NECK);
        placeLayer(level, center, 0, SURFACE);
        return true;
    }

    private static void placeLayer(WorldGenLevel level, BlockPos center, int yOffset, String[] pattern) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int z = 0; z < pattern.length; z++) {
            String row = pattern[z];
            for (int x = 0; x < row.length(); x++) {
                BlockState state = stateFor(row.charAt(x));
                if (state == null) {
                    continue;
                }
                cursor.set(center.getX() + x - 2, center.getY() + yOffset, center.getZ() + z - 2);
                level.setBlock(cursor, state, FLAGS);
            }
        }
    }

    private static BlockState stateFor(char marker) {
        return switch (marker) {
            case 'X', 'S' -> Blocks.STONE.defaultBlockState();
            case 'W' -> Blocks.WATER.defaultBlockState();
            case 'Y' -> HbmBlocks.BLOCK_YELLOWCAKE.get().defaultBlockState();
            case '.' -> Blocks.AIR.defaultBlockState();
            case 'G' -> Blocks.GRASS_BLOCK.defaultBlockState();
            case 'g' -> Blocks.GRAVEL.defaultBlockState();
            case 'E' -> HbmBlocks.GEYSIR_CHLORINE.get().defaultBlockState();
            default -> null;
        };
    }
}
