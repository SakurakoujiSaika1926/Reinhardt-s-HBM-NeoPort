package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent state of the old TileEntityBobble. */
public final class BobbleheadBlockEntity extends BlockEntity {
    private BobbleheadType type = BobbleheadType.NONE;

    public BobbleheadBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.BOBBLEHEAD.get(), pos, state);
    }

    public BobbleheadType type() {
        return type;
    }

    public void setType(BobbleheadType type) {
        this.type = type == null ? BobbleheadType.NONE : type;
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        type = BobbleheadType.byOrdinal(Math.abs(tag.getByte("type")));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
}
