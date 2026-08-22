package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FusionTorusStructBlockEntity extends BlockEntity {
    private int age;

    public FusionTorusStructBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FUSION_TORUS_STRUCT.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionTorusStructBlockEntity entity) {
        entity.age++;
        if (entity.age < 20) {
            return;
        }
        entity.age = 0;
        if (matchesTorus(level, pos)) {
            buildTorus(level, pos);
        }
    }

    private static boolean matchesTorus(Level level, BlockPos corePos) {
        for (int y = 0; y < 5; y++) {
            int layerIndex = y > 2 ? 4 - y : y;
            int[][] layer = FusionMachineBlock.TORUS_LAYOUT[layerIndex];
            for (int x = 0; x < layer.length; x++) {
                for (int z = 0; z < layer[x].length; z++) {
                    int required = layer[x][z];
                    if (required == 0 || (x == 7 && y == 0 && z == 7)) {
                        continue;
                    }
                    if (!matchesComponent(level, corePos.offset(x - 7, y, z - 7), required)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean matchesComponent(Level level, BlockPos pos, int variant) {
        BlockState state = level.getBlockState(pos);
        return state.is(HbmBlocks.FUSION_COMPONENT.get())
                && state.hasProperty(LegacyVariantBlock.VARIANT)
                && state.getValue(LegacyVariantBlock.VARIANT) == variant;
    }

    private static void buildTorus(Level level, BlockPos corePos) {
        Direction facing = Direction.SOUTH;
        BlockState torusState = HbmBlocks.FUSION_TORUS.get()
                .defaultBlockState()
                .setValue(LargeMachineBlock.FACING, facing);
        level.setBlock(corePos, torusState, Block.UPDATE_ALL);
        LargeMachineBlock.placeDummies(
                level,
                corePos,
                facing,
                FusionMachineBlock.Kind.TORUS.footprint(),
                LargeMachineBlock.RotationBasis.HBM_LEGACY_SOUTH
        );
        FusionMachineBlock.markPortDummies(level, corePos, facing, FusionMachineBlock.Kind.TORUS);
    }
}
