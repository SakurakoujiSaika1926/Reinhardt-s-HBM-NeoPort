package com.reinhardt.hbm.blockentity;

import net.minecraft.core.BlockPos;

/** Shared 1.7.10 IDroneLinkable contract for transport-drone route nodes. */
public interface DroneLinkable {
    BlockPos dronePoint();

    void setDroneTarget(BlockPos target);
}
