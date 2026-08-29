package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.IcfCoreBlock;
import com.reinhardt.hbm.block.IcfStructBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Exact 1.7.10 TileEntityICFStruct requirement scan. */
public final class IcfStructBlockEntity extends BlockEntity {
    private int age;

    public IcfStructBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ICF_STRUCT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IcfStructBlockEntity entity) {
        if (++entity.age < 20) {
            return;
        }
        entity.age = 0;
        Direction facing = state.getValue(IcfStructBlock.FACING);
        if (!matches(level, pos, facing)) {
            return;
        }
        BlockState core = HbmBlocks.ICF.get().defaultBlockState().setValue(LargeMachineBlock.FACING, facing);
        level.setBlock(pos, core, Block.UPDATE_ALL);
        LargeMachineBlock.placeDummies(level, pos, facing, IcfCoreBlock.FOOTPRINT, LargeMachineBlock.RotationBasis.HBM_LEGACY_SOUTH);
        IcfCoreBlock.refreshPorts(level, pos, facing);
    }

    private static boolean matches(Level level, BlockPos origin, Direction facing) {
        for (int length = -8; length <= 8; length++) {
            if (!component(level, origin, facing, 1, 0, length, 0)
                    || (length != 0 && !component(level, origin, facing, 0, 0, length, 0))
                    || !component(level, origin, facing, -1, 0, length, 0)
                    || !component(level, origin, facing, 0, 3, length, 2)) {
                return false;
            }
            int vessel = Math.abs(length) <= 2 ? 2 : 4;
            for (int width = -1; width <= 1; width++) {
                if (!component(level, origin, facing, width, 1, length, vessel)) {
                    return false;
                }
            }
            for (int width = -2; width <= 2; width++) {
                if (!component(level, origin, facing, width, 2, length, vessel)) {
                    return false;
                }
                if (width != 0 && !component(level, origin, facing, width, 3, length, vessel)) {
                    return false;
                }
                if (!component(level, origin, facing, width, 4, length, vessel)) {
                    return false;
                }
            }
            for (int width = -1; width <= 1; width++) {
                if (!component(level, origin, facing, width, 5, length, vessel)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean component(Level level, BlockPos origin, Direction facing, int width, int y, int length, int variant) {
        Direction side = LegacyMachineGeometry.forgeRotateUp(facing);
        BlockPos pos = origin.relative(side, length).relative(facing, width).above(y);
        BlockState state = level.getBlockState(pos);
        return state.is(HbmBlocks.ICF_COMPONENT.get())
                && state.hasProperty(LegacyVariantBlock.VARIANT)
                && state.getValue(LegacyVariantBlock.VARIANT) == variant;
    }
}
