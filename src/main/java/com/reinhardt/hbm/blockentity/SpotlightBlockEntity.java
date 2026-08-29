package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stateless block entity used by the OBJ renderer for a directional spotlight. */
public final class SpotlightBlockEntity extends BlockEntity {
    public SpotlightBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SPOTLIGHT.get(), pos, state);
    }
}
