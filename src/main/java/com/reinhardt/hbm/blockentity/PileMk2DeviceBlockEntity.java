package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PileMk2Block;
import com.reinhardt.hbm.block.PileMk2DeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

abstract class PileMk2DeviceBlockEntity extends BlockEntity {
    protected int channelNumber = -1;
    @Nullable private BlockPos cachedCorePos;

    protected PileMk2DeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected final Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(PileMk2DeviceBlock.FACING)
                ? state.getValue(PileMk2DeviceBlock.FACING) : Direction.NORTH;
    }

    @Nullable
    protected final PileMk2CoreBlockEntity resolveCore(BlockPos entry) {
        if (level == null || !level.getBlockState(entry).hasProperty(PileMk2Block.KIND)) {
            cachedCorePos = null;
            return null;
        }
        if (cachedCorePos != null && level.hasChunkAt(cachedCorePos)
                && level.getBlockEntity(cachedCorePos) instanceof PileMk2CoreBlockEntity cached
                && cached.contains(entry)) {
            return cached;
        }
        PileMk2CoreBlockEntity core = PileMk2Block.findCore(level, entry);
        BlockPos next = core == null ? null : core.getBlockPos().immutable();
        if (!java.util.Objects.equals(cachedCorePos, next)) {
            cachedCorePos = next;
            setChanged();
        }
        return core;
    }

    public final int channelNumber() {
        return channelNumber;
    }

    protected final void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ChannelNumber", channelNumber);
        if (cachedCorePos != null) tag.putLong("CorePos", cachedCorePos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        channelNumber = tag.getInt("ChannelNumber");
        cachedCorePos = tag.contains("CorePos") ? BlockPos.of(tag.getLong("CorePos")) : null;
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
}
