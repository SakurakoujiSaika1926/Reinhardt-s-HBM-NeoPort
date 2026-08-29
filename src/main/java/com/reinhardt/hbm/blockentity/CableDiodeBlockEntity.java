package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CableDiodeBlock;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerGraphNode;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Directional bridge for the 1.7.10 power network. */
public final class CableDiodeBlockEntity extends BlockEntity implements PowerGraphNode {
    private int diodeLevel = 1;
    private PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.NORMAL;

    public CableDiodeBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    public int diodeLevel() {
        return diodeLevel;
    }

    public PowerEndpoint.ConnectionPriority priority() {
        return priority;
    }

    public void increaseLevel() {
        if (diodeLevel < 11) {
            diodeLevel++;
            changedAndDirty();
        }
    }

    public void decreaseLevel() {
        if (diodeLevel > 1) {
            diodeLevel--;
            changedAndDirty();
        }
    }

    public void cyclePriority() {
        PowerEndpoint.ConnectionPriority[] values = PowerEndpoint.ConnectionPriority.VALUES;
        priority = values[(priority.ordinal() + 1) % values.length];
        changedAndDirty();
    }

    public Direction outputDirection() {
        return getBlockState().getValue(CableDiodeBlock.FACING).getOpposite();
    }

    public long maxPower() {
        return (long) Math.pow(10L, diodeLevel);
    }

    @Override
    public BlockPos getGraphPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(
                worldPosition.relative(Direction.NORTH), worldPosition.relative(Direction.SOUTH),
                worldPosition.relative(Direction.EAST), worldPosition.relative(Direction.WEST),
                worldPosition.relative(Direction.UP), worldPosition.relative(Direction.DOWN)
        );
    }

    /** The old diode transmits toward the opposite side of its placement face. */
    @Override
    public List<BlockPos> getPowerFlowPositions(LevelAccessor level) {
        return List.of(worldPosition.relative(outputDirection()));
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    @Override
    public boolean canAcceptPowerFrom(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return connectorPos.equals(worldPosition.relative(getBlockState().getValue(CableDiodeBlock.FACING)));
    }

    @Override
    public long getPowerFlowLimit(LevelAccessor level) {
        return maxPower();
    }

    @Override
    public List<BlockPos> getRemotePowerLinks(Level level) {
        return List.of();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CableDiodeBlockEntity diode) {
        if (!level.isClientSide) {
            PowerNetworkManager.markDirty(level);
        }
    }

    private void changedAndDirty() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            PowerNetworkManager.markDirty(this.level);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Level", diodeLevel);
        tag.putInt("Priority", priority.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        diodeLevel = Math.max(1, Math.min(11, tag.getInt("Level")));
        int ordinal = Math.max(0, Math.min(PowerEndpoint.ConnectionPriority.VALUES.length - 1, tag.getInt("Priority")));
        priority = PowerEndpoint.ConnectionPriority.VALUES[ordinal];
    }
}
