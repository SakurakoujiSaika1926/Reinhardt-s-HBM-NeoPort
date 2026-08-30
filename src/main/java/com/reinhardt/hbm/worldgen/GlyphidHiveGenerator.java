package com.reinhardt.hbm.worldgen;

import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.blockentity.DecoLootBlockEntity;
import com.reinhardt.hbm.blockentity.HbmStructureLoot;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Exact small-hive schematic used by GlyphidScout in HBM 1.7.10. */
public final class GlyphidHiveGenerator {
    private static final String[][] SCHEMATIC_SMALL = {
            {
                    "00000000000", "00000000000", "00000000000", "00000000000",
                    "00001110000", "00001110000", "00001110000", "00000000000",
                    "00000000000", "00000000000", "00000000000"
            },
            {
                    "00000000000", "00000000000", "00001110000", "00011111000",
                    "00111111100", "00111111100", "00111111100", "00011111000",
                    "00001110000", "00000000000", "00000000000"
            },
            {
                    "00000000000", "00001110000", "00111111100", "00111111100",
                    "01113331110", "01113331110", "01113331110", "00111111100",
                    "00111111100", "00001110000", "00000000000"
            },
            {
                    "00000000000", "00001110000", "00111111100", "00111222100",
                    "01122222210", "01122222210", "01122222210", "00111222100",
                    "00111111100", "00001110000", "00000000000"
            },
            {
                    "00000000000", "00001110000", "00111111100", "00111111100",
                    "01111111110", "01111111110", "01111111110", "00111111100",
                    "00111111100", "00001110000", "00000000000"
            }
    };

    private GlyphidHiveGenerator() {
    }

    public static void generateSmall(LevelAccessor level, int x, int y, int z,
                                     RandomSource random, boolean infected, boolean loot) {
        int variant = infected ? 1 : 0;
        BlockState base = HbmBlocks.GLYPHID_BASE.get().defaultBlockState()
                .setValue(LegacyVariantBlock.VARIANT, variant);
        BlockState spawner = HbmBlocks.GLYPHID_SPAWNER.get().defaultBlockState()
                .setValue(LegacyVariantBlock.VARIANT, variant);

        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 11; k++) {
                    int marker = SCHEMATIC_SMALL[4 - j][i].charAt(k) - '0';
                    BlockPos pos = new BlockPos(x + i - 5, y + j - 2, z + k - 5);
                    switch (marker) {
                        case 1 -> level.setBlock(pos, base, 2);
                        case 2 -> level.setBlock(pos, random.nextInt(3) == 0 ? spawner : base, 2);
                        case 3 -> placeCenter(level, pos, random, variant, loot);
                        default -> {
                        }
                    }
                }
            }
        }
    }

    private static void placeCenter(LevelAccessor level, BlockPos pos, RandomSource random,
                                    int variant, boolean loot) {
        int choice = random.nextInt(3);
        if (choice == 0) {
            BlockState skull = Blocks.SKELETON_SKULL.defaultBlockState()
                    .setValue(SkullBlock.ROTATION, random.nextInt(16));
            level.setBlock(pos, skull, 3);
        } else if (choice == 1) {
            level.setBlock(pos, HbmBlocks.DECO_LOOT.get().defaultBlockState(), 2);
            if (level.getBlockEntity(pos) instanceof DecoLootBlockEntity decoLoot) {
                HbmStructureLoot.applyPileLoot(decoLoot, "LOOT_BONES", random);
            }
        } else if (loot) {
            level.setBlock(pos, HbmBlocks.DECO_LOOT.get().defaultBlockState(), 2);
            if (level.getBlockEntity(pos) instanceof DecoLootBlockEntity decoLoot) {
                HbmStructureLoot.applyPileLoot(decoLoot, "LOOT_GLYPHID_HIVE", random);
            }
        } else {
            level.setBlock(pos, HbmBlocks.GLYPHID_BASE.get().defaultBlockState()
                    .setValue(LegacyVariantBlock.VARIANT, variant), 2);
        }
    }
}
