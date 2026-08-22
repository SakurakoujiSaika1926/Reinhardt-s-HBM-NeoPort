package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Empty by design: TileEntityMachineUF6Tank and TileEntityMachinePuF6Tank have no stored data in 1.7.10. */
public final class HexafluorideTankBlockEntity extends BlockEntity {
    public HexafluorideTankBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.HEXAFLUORIDE_TANK.get(), pos, state);
    }
}
