package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Rendering anchor for the legacy RadioTelex multiblock core. */
public final class RadioTelexBlockEntity extends BlockEntity {
    public RadioTelexBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.RADIO_TELEX.get(), pos, state);
    }
}
