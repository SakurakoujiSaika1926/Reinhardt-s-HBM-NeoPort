package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.drone.DroneRequestNetwork;
import com.reinhardt.hbm.drone.DroneRequestNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;

/** Shared 20-tick lease refresh used by 1.7.10 request-network endpoints. */
abstract class DroneRequestNetworkBlockEntity extends BlockEntity implements DroneRequestNetworkNode {
    protected DroneRequestNetworkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected final void tickNetwork(Level level) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && level.getGameTime() % 20L == 0L) {
            DroneRequestNetwork.refresh(serverLevel, this, !level.hasNeighborSignal(worldPosition));
        }
    }

    protected final void syncNetworkState() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
