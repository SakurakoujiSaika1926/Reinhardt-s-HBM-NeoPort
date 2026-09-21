package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.BlastDoorDummyBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
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

public class BlastDoorDummyBlockEntity extends BlockEntity {
    private static final String CREATE_CORE_OFFSET_X = "HbmCoreOffsetX";
    private static final String CREATE_CORE_OFFSET_Y = "HbmCoreOffsetY";
    private static final String CREATE_CORE_OFFSET_Z = "HbmCoreOffsetZ";
    private BlockPos corePos = BlockPos.ZERO;
    // Transient, single-removal authorization set only by a real player break.
    private boolean dropCoreWhenRemoved;

    public BlastDoorDummyBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BLAST_DOOR_DUMMY.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlastDoorDummyBlockEntity dummy) {
        if (!level.isClientSide && !level.getBlockState(dummy.corePos).is(HbmBlocks.BLAST_DOOR.get())) {
            BlastDoorDummyBlock.runWithoutCoreDestroy(() -> level.destroyBlock(pos, false));
        }
    }

    public BlockPos corePos() {
        return this.corePos;
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos.immutable();
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Records whether a player harvest should drop the single blast-door core. */
    public void setDropCoreWhenRemoved(boolean drop) {
        this.dropCoreWhenRemoved = drop;
    }

    public boolean consumeDropCoreWhenRemoved() {
        boolean drop = this.dropCoreWhenRemoved;
        this.dropCoreWhenRemoved = false;
        return drop;
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
        tag.putInt("tx", this.corePos.getX());
        tag.putInt("ty", this.corePos.getY());
        tag.putInt("tz", this.corePos.getZ());
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
            this.corePos = new BlockPos(tag.getInt("tx"), tag.getInt("ty"), tag.getInt("tz"));
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
