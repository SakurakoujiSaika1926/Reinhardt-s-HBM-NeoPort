package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.drone.DroneRequestNetwork;
import com.reinhardt.hbm.drone.DroneRequestNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Base for the three 3x3 logistics inventories and the logistics drone dock. */
abstract class DroneNetworkContainerBlockEntity extends DroneInventoryBlockEntity implements DroneRequestNetworkNode {
    protected DroneNetworkContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    protected final void tickNetwork(Level level) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && level.getGameTime() % 20L == 0L) {
            DroneRequestNetwork.refresh(serverLevel, this, !level.hasNeighborSignal(worldPosition));
        }
    }

    @Override
    public BlockPos droneNodePosition() {
        return worldPosition.above();
    }

    @Override
    public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            DroneRequestNetwork.remove(serverLevel, droneNodePosition());
        }
        super.setRemoved();
    }
}
