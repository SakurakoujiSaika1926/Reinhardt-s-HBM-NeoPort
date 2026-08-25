package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DroneWaypointBlock;
import com.reinhardt.hbm.entity.LegacyDeliveryDroneEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** State and route dispatch for a transport-drone waypoint. */
public final class DroneWaypointBlockEntity extends BlockEntity implements DroneLinkable {
    private int height = 5;
    @Nullable
    private BlockPos nextTarget;

    public DroneWaypointBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DRONE_WAYPOINT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DroneWaypointBlockEntity waypoint) {
        if (level.isClientSide) {
            waypoint.clientTick();
        } else {
            waypoint.serverTick();
        }
    }

    @Override
    public BlockPos dronePoint() {
        Direction direction = getBlockState().getValue(DroneWaypointBlock.FACING);
        return worldPosition.relative(direction, height);
    }

    @Override
    public void setDroneTarget(BlockPos target) {
        nextTarget = target.immutable();
        sync();
    }

    public void addHeight(int amount) {
        height = Math.clamp(height + amount, 1, 15);
        sync();
    }

    public int height() {
        return height;
    }

    @Nullable
    public BlockPos nextTarget() {
        return nextTarget;
    }

    private void serverTick() {
        if (level == null || nextTarget == null) {
            return;
        }
        AABB trigger = new AABB(dronePoint());
        for (LegacyDeliveryDroneEntity drone : level.getEntitiesOfClass(LegacyDeliveryDroneEntity.class, trigger)) {
            if (drone.getDeltaMovement().lengthSqr() < 0.0025D) {
                drone.setTarget(nextTarget.getX() + 0.5D, nextTarget.getY(), nextTarget.getZ() + 0.5D);
            }
        }
    }

    private void clientTick() {
        if (level != null && nextTarget != null && level.getGameTime() % 2L == 0L) {
            BlockPos point = dronePoint();
            level.addParticle(net.minecraft.core.particles.DustParticleOptions.REDSTONE,
                    point.getX() + 0.5D, point.getY() + 0.5D, point.getZ() + 0.5D,
                    0.0D, 0.0D, 1.0D);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("height", height);
        if (nextTarget != null) {
            tag.putLong("next", nextTarget.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        height = Math.clamp(tag.contains("height") ? tag.getInt("height") : 5, 1, 15);
        nextTarget = tag.contains("next") ? BlockPos.of(tag.getLong("next")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
