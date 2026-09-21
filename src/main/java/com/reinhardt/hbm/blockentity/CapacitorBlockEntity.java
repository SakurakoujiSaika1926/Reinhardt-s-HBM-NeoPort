package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CapacitorBlock;
import com.reinhardt.hbm.block.CapacitorBusBlock;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerGraphNode;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Direct power port of TileEntityCapacitor from HBM 1.7.10. */
public final class CapacitorBlockEntity extends BlockEntity implements PowerEndpoint, PowerGraphNode {
    public static final long MAX_POWER = 1_000_000L;
    public static final long INPUT_RATE = MAX_POWER / 100L;
    public static final long OUTPUT_RATE = MAX_POWER / 300L;
    private static final String ITEM_DATA = "capacitor_data";

    private long power;
    private long lastReceived;
    private long lastSent;

    public CapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CAPACITOR_COPPER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CapacitorBlockEntity capacitor) {
        if (level.isClientSide) {
            return;
        }
        capacitor.lastReceived = 0L;
        capacitor.lastSent = 0L;
        PowerNetworkManager.tickFromEndpoint(level, capacitor);
        if (level.getGameTime() % 5L == 0L) {
            capacitor.sync();
        }
    }

    public long power() {
        return power;
    }

    public long lastReceived() {
        return lastReceived;
    }

    public long lastSent() {
        return lastSent;
    }

    public long capacity() {
        return MAX_POWER;
    }

    private Direction facing() {
        return getBlockState().getValue(CapacitorBlock.FACING);
    }

    private BlockPos inputPos() {
        return worldPosition.relative(facing());
    }

    private BlockPos outputPos() {
        Direction output = facing().getOpposite();
        BlockPos pos = worldPosition.relative(output);
        Direction chainDirection = null;
        while (level != null && level.getBlockState(pos).getBlock() instanceof CapacitorBusBlock) {
            Direction busDirection = level.getBlockState(pos).getValue(CapacitorBusBlock.FACING);
            if (chainDirection != null && chainDirection != busDirection) {
                return null;
            }
            chainDirection = busDirection;
            pos = pos.relative(busDirection);
        }
        return chainDirection == null ? null : pos;
    }

    private BlockPos firstBus() {
        BlockPos pos = worldPosition.relative(facing().getOpposite());
        return level != null && level.getBlockState(pos).getBlock() instanceof CapacitorBusBlock ? pos : null;
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    @Override
    public BlockPos getGraphPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        BlockPos bus = firstBus();
        return bus == null ? List.of(inputPos()) : List.of(inputPos(), bus);
    }

    @Override
    public List<BlockPos> getPowerFlowPositions(LevelAccessor level) {
        BlockPos bus = firstBus();
        return bus == null ? List.of() : List.of(bus);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    @Override
    public boolean canAcceptPowerFrom(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return connectorPos.equals(inputPos());
    }

    @Override
    public long getPowerFlowLimit(LevelAccessor level) {
        return OUTPUT_RATE;
    }

    @Override
    public boolean requiresDirectedPowerRouting(LevelAccessor level) {
        return true;
    }

    @Override
    public List<BlockPos> getRemotePowerLinks(Level level) {
        return List.of();
    }

    @Override
    public long getAvailableOutput() {
        return Math.min(power, OUTPUT_RATE);
    }

    @Override
    public long getRequestedInput() {
        return Math.min(MAX_POWER - power, INPUT_RATE);
    }

    @Override
    public PowerEndpoint.ConnectionPriority getPowerPriority() {
        return PowerEndpoint.ConnectionPriority.LOW;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        long sent = Math.min(power, Math.max(0L, usedOutput));
        power = Math.max(0L, power - sent);
        long received = Math.min(MAX_POWER - power, Math.max(0L, receivedInput));
        power += received;
        lastSent = sent;
        lastReceived = received;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.power.capacitor",
                power, MAX_POWER, lastReceived, lastSent);
    }

    public void loadFromItem(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains(ITEM_DATA)) {
            this.power = Math.max(0L, Math.min(MAX_POWER, root.getCompound(ITEM_DATA).getLong("Power")));
        }
        setChanged();
    }

    public void saveToItem(ItemStack stack) {
        if (power <= 0L) {
            return;
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag data = new CompoundTag();
        data.putLong("Power", power);
        root.put(ITEM_DATA, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Power", power);
        tag.putLong("LastReceived", lastReceived);
        tag.putLong("LastSent", lastSent);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        lastReceived = Math.max(0L, tag.getLong("LastReceived"));
        lastSent = Math.max(0L, tag.getLong("LastSent"));
    }
}
