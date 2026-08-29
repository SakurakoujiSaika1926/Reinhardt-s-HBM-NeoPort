package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PlushieType;
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

public final class PlushieBlockEntity extends BlockEntity {
    private PlushieType type = PlushieType.NONE;
    private int squishTimer;

    public PlushieBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PLUSHIE.get(), pos, state);
    }

    public PlushieType type() {
        return type;
    }

    public void setType(PlushieType type) {
        this.type = type == null ? PlushieType.NONE : type;
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void squish() {
        squishTimer = 11;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PlushieBlockEntity plushie) {
        if (plushie.squishTimer > 0) plushie.squishTimer--;
    }

    public float squishTimer(float partialTick) {
        return squishTimer > 0 ? Math.max(0.0F, squishTimer - partialTick) : 0.0F;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        type = PlushieType.byOrdinal(Math.abs(tag.getByte("type")));
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
