package com.reinhardt.hbm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface MachineInventory {
    void dropContents(Level level, BlockPos pos);
}
