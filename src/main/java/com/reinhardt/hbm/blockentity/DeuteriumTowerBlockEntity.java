package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DeuteriumTowerBlockEntity extends DeuteriumExtractorBlockEntity {
    public DeuteriumTowerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DEUTERIUM_TOWER.get(), pos, blockState);
    }
}
