package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Per-network HE/t and HE/s readout from the legacy power-gauge cable. */
public final class PowerGaugeBlockEntity extends BlockEntity {
    private long deltaTick;
    private long secondAccumulator;
    private long deltaLastSecond;

    public PowerGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.POWER_GAUGE.get(), pos, state);
    }

    public long deltaTick() {
        return deltaTick;
    }

    public long deltaLastSecond() {
        return deltaLastSecond;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PowerGaugeBlockEntity gauge) {
        if (level.isClientSide) {
            return;
        }
        gauge.deltaTick = PowerNetworkManager.transferredPowerAt(level, pos);
        gauge.secondAccumulator += gauge.deltaTick;
        if (level.getGameTime() % 20L == 0L) {
            gauge.deltaLastSecond = gauge.secondAccumulator;
            gauge.secondAccumulator = 0L;
        }
        if (level.getGameTime() % 5L == 0L) {
            gauge.sync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("DeltaTick", deltaTick);
        tag.putLong("SecondAccumulator", secondAccumulator);
        tag.putLong("DeltaLastSecond", deltaLastSecond);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        deltaTick = Math.max(0L, tag.getLong("DeltaTick"));
        secondAccumulator = Math.max(0L, tag.getLong("SecondAccumulator"));
        deltaLastSecond = Math.max(0L, tag.getLong("DeltaLastSecond"));
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
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
