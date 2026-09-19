package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WatzStructBlockEntity extends BlockEntity {
    public WatzStructBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WATZ_STRUCT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WatzStructBlockEntity struct) {
        if (level.isClientSide || level.getGameTime() % 20L != 0L) {
            return;
        }
        if (!struct.isValidStructure(level, pos)) {
            return;
        }
        level.setBlock(pos, HbmBlocks.WATZ.get().defaultBlockState(), Block.UPDATE_ALL);
        WatzBlockEntity.fillDummies(level, pos);
    }

    private boolean isValidStructure(Level level, BlockPos core) {
        if (!has(level, core, HbmBlocks.WATZ_COOLER.get(), 0, 1, 0)) {
            return false;
        }
        if (!has(level, core, HbmBlocks.WATZ_COOLER.get(), 0, 2, 0)) {
            return false;
        }
        for (int y = 0; y < 3; y++) {
            for (int[] offset : WatzBlockEntity.ELEMENT_OFFSETS) {
                if (!has(level, core, HbmBlocks.WATZ_ELEMENT.get(), offset[0], y, offset[1])) {
                    return false;
                }
            }
            for (int[] offset : WatzBlockEntity.COOLER_OFFSETS) {
                if (!has(level, core, HbmBlocks.WATZ_COOLER.get(), offset[0], y, offset[1])) {
                    return false;
                }
            }
            for (int[] offset : WatzBlockEntity.CASING_OFFSETS) {
                if (!hasVariant(level, core, HbmBlocks.WATZ_END.get(), 1, offset[0], y, offset[1])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean has(Level level, BlockPos core, Block block, int x, int y, int z) {
        return level.getBlockState(core.offset(x, y, z)).is(block);
    }

    private static boolean hasVariant(Level level, BlockPos core, Block block, int variant, int x, int y, int z) {
        BlockState state = level.getBlockState(core.offset(x, y, z));
        return state.is(block)
                && state.hasProperty(LegacyVariantBlock.VARIANT)
                && state.getValue(LegacyVariantBlock.VARIANT) == variant;
    }

}
