package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Persistent core link for one of the old cargo-pad dummy cells. */
public final class SatelliteDockDummyBlockEntity extends BlockEntity {
    private static final String CREATE_CORE_OFFSET_X = "HbmCoreOffsetX";
    private static final String CREATE_CORE_OFFSET_Y = "HbmCoreOffsetY";
    private static final String CREATE_CORE_OFFSET_Z = "HbmCoreOffsetZ";
    private BlockPos corePos = BlockPos.ZERO;

    public SatelliteDockDummyBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SAT_DOCK_DUMMY.get(), pos, state);
    }

    public BlockPos corePos() {
        return this.corePos;
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos.immutable();
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public BlockPos coreOffset() {
        return this.corePos.subtract(this.worldPosition);
    }

    public void setCoreOffset(BlockPos coreOffset) {
        setCorePos(this.worldPosition.offset(coreOffset));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CoreX", corePos.getX());
        tag.putInt("CoreY", corePos.getY());
        tag.putInt("CoreZ", corePos.getZ());
        BlockPos coreOffset = coreOffset();
        tag.putInt(CREATE_CORE_OFFSET_X, coreOffset.getX());
        tag.putInt(CREATE_CORE_OFFSET_Y, coreOffset.getY());
        tag.putInt(CREATE_CORE_OFFSET_Z, coreOffset.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(CREATE_CORE_OFFSET_X)
                && tag.contains(CREATE_CORE_OFFSET_Y)
                && tag.contains(CREATE_CORE_OFFSET_Z)) {
            this.corePos = this.worldPosition.offset(
                    tag.getInt(CREATE_CORE_OFFSET_X),
                    tag.getInt(CREATE_CORE_OFFSET_Y),
                    tag.getInt(CREATE_CORE_OFFSET_Z)
            ).immutable();
        } else {
            this.corePos = new BlockPos(tag.getInt("CoreX"), tag.getInt("CoreY"), tag.getInt("CoreZ"));
        }
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
