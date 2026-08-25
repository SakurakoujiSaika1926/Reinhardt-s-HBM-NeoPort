package com.reinhardt.hbm.drone;

import net.minecraft.core.BlockPos;

/** A live endpoint in the 1.7.10 logistics-drone radio network. */
public interface DroneRequestNetworkNode {
    DroneRequestNetwork.NodeKind droneNodeKind();

    BlockPos droneNodePosition();
}
