package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DroneWaypointBlock;
import com.reinhardt.hbm.drone.DroneRequestNetwork;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The request-network waypoint uses the same face attachment and height rules as the transport waypoint. */
public final class DroneRequestWaypointBlockEntity extends DroneRequestNetworkBlockEntity {
    private int height = 5;

    public DroneRequestWaypointBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DRONE_WAYPOINT_REQUEST.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DroneRequestWaypointBlockEntity waypoint) {
        if (!level.isClientSide) {
            waypoint.tickNetwork(level);
        }
    }

    public void addHeight(int amount) {
        int nextHeight = Math.clamp(height + amount, 1, 15);
        if (nextHeight == height) {
            return;
        }
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            DroneRequestNetwork.remove(serverLevel, droneNodePosition());
            height = nextHeight;
            DroneRequestNetwork.refresh(serverLevel, this, !level.hasNeighborSignal(worldPosition));
        } else {
            height = nextHeight;
        }
        syncNetworkState();
    }

    public int height() {
        return height;
    }

    @Override
    public DroneRequestNetwork.NodeKind droneNodeKind() {
        return DroneRequestNetwork.NodeKind.WAYPOINT;
    }

    @Override
    public BlockPos droneNodePosition() {
        Direction direction = getBlockState().getValue(DroneWaypointBlock.FACING);
        return worldPosition.relative(direction, height);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("height", height);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        height = Math.clamp(tag.contains("height") ? tag.getInt("height") : 5, 1, 15);
    }
}
