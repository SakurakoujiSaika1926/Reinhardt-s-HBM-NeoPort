package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.EnergyConverterBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class EnergyConverterBlockEntity extends BlockEntity implements PowerEndpoint {
    private static final long HE_CAPACITY = 5_000_000L;
    private static final int FE_CAPACITY = 1_000_000;
    private long he;
    private int fe;

    public EnergyConverterBlockEntity(BlockPos pos, BlockState state, EnergyConverterBlock.Kind kind) {
        super(HbmBlockEntities.ENERGY_CONVERTER.get(), pos, state);
    }

    public EnergyConverterBlock.Kind kind() {
        return ((EnergyConverterBlock) getBlockState().getBlock()).kind();
    }

    public long he() { return he; }
    public int fe() { return fe; }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyConverterBlockEntity converter) {
        if (level.isClientSide) return;
        PowerNetworkManager.tickFromEndpoint(level, converter);
        if (converter.kind() == EnergyConverterBlock.Kind.HE_TO_FE) {
            long heUsed = HbmConfig.HE_TO_FE_HE_USED.get();
            long feCreated = HbmConfig.HE_TO_FE_FE_CREATED.get();
            long created = Math.min(converter.feCapacity() - converter.fe,
                    multiplySaturated(converter.he / heUsed, feCreated));
            converter.he -= multiplySaturated(created, heUsed) / feCreated;
            converter.fe += (int) created;
            if (converter.he > 0L) {
                converter.he = (long) (converter.he * (1.0D - HbmConfig.HE_TO_FE_INPUT_DECAY.get()));
            }
            converter.pushFe(level);
        } else {
            long feUsed = HbmConfig.FE_TO_HE_FE_USED.get();
            long heCreated = HbmConfig.FE_TO_HE_HE_CREATED.get();
            long feConverted = Math.min(converter.fe,
                    multiplySaturated(converter.heCapacity() - converter.he, feUsed) / heCreated);
            converter.fe -= (int) feConverted;
            converter.he += multiplySaturated(feConverted, heCreated) / feUsed;
            if (converter.fe > 0) {
                int decay = (int) Math.ceil(converter.fe * HbmConfig.FE_TO_HE_INPUT_DECAY.get());
                converter.fe -= Math.min(converter.fe, decay);
            }
        }
        converter.setChanged();
        if (level.getGameTime() % 5L == 0L) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void pushFe(Level level) {
        int remaining = fe;
        for (Direction direction : Direction.values()) {
            if (remaining <= 0) break;
            IEnergyStorage target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (target == null) continue;
            int sent = target.receiveEnergy(Math.min(remaining, 1_000_000), false);
            remaining -= sent;
            fe -= sent;
        }
    }

    private static long multiplySaturated(long left, long right) {
        if (left == 0L || right == 0L) return 0L;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }

    public IEnergyStorage energyStorage() {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                if (kind() == EnergyConverterBlock.Kind.HE_TO_FE) return 0;
                int accepted = Math.min(maxReceive, feCapacity() - fe);
                if (!simulate) { fe += accepted; setChanged(); }
                return accepted;
            }
            @Override public int extractEnergy(int maxExtract, boolean simulate) {
                if (kind() == EnergyConverterBlock.Kind.FE_TO_HE) return 0;
                int extracted = Math.min(maxExtract, fe);
                if (!simulate) { fe -= extracted; setChanged(); }
                return extracted;
            }
            @Override public int getEnergyStored() { return fe; }
            @Override public int getMaxEnergyStored() { return feCapacity(); }
            @Override public boolean canExtract() { return kind() == EnergyConverterBlock.Kind.HE_TO_FE; }
            @Override public boolean canReceive() { return kind() == EnergyConverterBlock.Kind.FE_TO_HE; }
        };
    }

    private long heCapacity() { return HE_CAPACITY; }
    private int feCapacity() { return FE_CAPACITY; }

    @Override public BlockPos getPowerPos() { return worldPosition; }
    @Override public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(Direction.values()).stream().map(worldPosition::relative).toList();
    }
    @Override public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }
    @Override public long getAvailableOutput() { return kind() == EnergyConverterBlock.Kind.FE_TO_HE ? he : 0L; }
    @Override public long getRequestedInput() { return kind() == EnergyConverterBlock.Kind.HE_TO_FE ? heCapacity() - he : 0L; }
    @Override public void applyPower(long usedOutput, long receivedInput) {
        he = Math.max(0L, Math.min(heCapacity(), he + receivedInput - usedOutput));
        setChanged();
    }
    @Override public PowerEndpoint.ConnectionPriority getPowerPriority() {
        return kind() == EnergyConverterBlock.Kind.HE_TO_FE
                ? PowerEndpoint.ConnectionPriority.LOW
                : PowerEndpoint.ConnectionPriority.NORMAL;
    }
    @Override public Component getPowerStatus() {
        return Component.literal(String.format("%,d HE / %,d FE", he, fe));
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putLong("HE", he); tag.putInt("FE", fe);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); he = Math.max(0L, Math.min(heCapacity(), tag.getLong("HE"))); fe = Math.max(0, Math.min(feCapacity(), tag.getInt("FE")));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag tag = super.getUpdateTag(registries); saveAdditional(tag, registries); return tag; }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
