package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ZirnoxDestroyedBlockEntity extends BlockEntity {
    public ZirnoxDestroyedBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ZIRNOX_DESTROYED.get(), pos, blockState);
    }
}
