package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FractionSpacerBlockEntity extends BlockEntity {
    public FractionSpacerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FRACTION_SPACER.get(), pos, blockState);
    }
}
