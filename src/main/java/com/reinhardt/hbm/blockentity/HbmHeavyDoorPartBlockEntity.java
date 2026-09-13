package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HbmHeavyDoorPartBlockEntity extends BlockEntity {
    private static final String CREATE_CORE_OFFSET_X = "HbmCoreOffsetX";
    private static final String CREATE_CORE_OFFSET_Y = "HbmCoreOffsetY";
    private static final String CREATE_CORE_OFFSET_Z = "HbmCoreOffsetZ";
    private BlockPos corePos = BlockPos.ZERO;
    private BlockPos localOffset = BlockPos.ZERO;
    private boolean dropCoreWhenRemoved = true;

    public HbmHeavyDoorPartBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.HEAVY_DOOR_PART.get(), pos, blockState);
    }

    public BlockPos corePos() {
        return this.corePos;
    }

    public BlockPos localOffset() {
        return this.localOffset;
    }

    /**
     * Records the player's harvest mode before Block.onRemove is called.  The
     * legacy dummy chain dropped the single core item for survival harvests,
     * but creative harvests removed the whole assembly without a drop.
     */
    public void setDropCoreWhenRemoved(boolean drop) {
        this.dropCoreWhenRemoved = drop;
    }

    public boolean consumeDropCoreWhenRemoved() {
        boolean drop = this.dropCoreWhenRemoved;
        this.dropCoreWhenRemoved = true;
        return drop;
    }

    public void configure(BlockPos corePos, BlockPos localOffset) {
        this.corePos = corePos.immutable();
        this.localOffset = localOffset.immutable();
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public BlockPos coreOffset() {
        return this.corePos.subtract(this.worldPosition);
    }

    public void setCoreOffset(BlockPos coreOffset) {
        this.corePos = this.worldPosition.offset(coreOffset).immutable();
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CoreX", this.corePos.getX());
        tag.putInt("CoreY", this.corePos.getY());
        tag.putInt("CoreZ", this.corePos.getZ());
        tag.putInt("LocalX", this.localOffset.getX());
        tag.putInt("LocalY", this.localOffset.getY());
        tag.putInt("LocalZ", this.localOffset.getZ());
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
        this.localOffset = new BlockPos(tag.getInt("LocalX"), tag.getInt("LocalY"), tag.getInt("LocalZ"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
