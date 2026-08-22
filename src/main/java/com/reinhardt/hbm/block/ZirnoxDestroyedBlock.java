package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ZirnoxDestroyedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ZirnoxDestroyedBlock extends LargeMachineBlock implements EntityBlock {
    public ZirnoxDestroyedBlock(Properties properties, VoxelShape shape) {
        super(properties, ZirnoxReactorBlock.FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new ZirnoxDestroyedBlockEntity(pos, state);
    }
}

