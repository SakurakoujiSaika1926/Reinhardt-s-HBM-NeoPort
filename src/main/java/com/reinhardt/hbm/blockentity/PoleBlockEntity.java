package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Static client render anchor for the two legacy antenna pole blocks. */
public final class PoleBlockEntity extends BlockEntity {
    public PoleBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.POLE.get(), pos, state);
    }
}
