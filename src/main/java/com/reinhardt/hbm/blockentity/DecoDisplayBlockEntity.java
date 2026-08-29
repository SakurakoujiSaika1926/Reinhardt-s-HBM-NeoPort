package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Client-rendered static display entity used by the legacy OBJ decorations. */
public final class DecoDisplayBlockEntity extends BlockEntity {
    public DecoDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DECO_DISPLAY.get(), pos, state);
    }
}
