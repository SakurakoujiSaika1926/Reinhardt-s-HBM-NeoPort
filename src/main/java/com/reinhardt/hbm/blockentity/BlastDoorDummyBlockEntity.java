package com.reinhardt.hbm.blockentity;

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
    private BlockPos corePos = BlockPos.ZERO;

    public BlastDoorDummyBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BLAST_DOOR_DUMMY.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlastDoorDummyBlockEntity dummy) {
        if (!level.isClientSide && !level.getBlockState(dummy.corePos).is(HbmBlocks.BLAST_DOOR.get())) {
            level.destroyBlock(pos, false);
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("tx", this.corePos.getX());
        tag.putInt("ty", this.corePos.getY());
        tag.putInt("tz", this.corePos.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.corePos = new BlockPos(tag.getInt("tx"), tag.getInt("ty"), tag.getInt("tz"));
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
