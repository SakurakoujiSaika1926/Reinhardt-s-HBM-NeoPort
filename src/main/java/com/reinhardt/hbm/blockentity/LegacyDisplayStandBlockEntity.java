package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class LegacyDisplayStandBlockEntity extends BlockEntity {
    private ItemStack displayedItem = ItemStack.EMPTY;

    public LegacyDisplayStandBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LEGACY_DISPLAY_STAND.get(), pos, state);
    }

    public ItemStack displayedItem() {
        return this.displayedItem;
    }

    public void setDisplayedItem(ItemStack stack) {
        this.displayedItem = stack.copy();
        sync();
    }

    public ItemStack takeDisplayedItem() {
        ItemStack result = this.displayedItem;
        this.displayedItem = ItemStack.EMPTY;
        sync();
        return result;
    }

    void clearDisplayedItem() {
        this.displayedItem = ItemStack.EMPTY;
        sync();
    }

    public void dropDisplayedItem() {
        if (this.level == null || this.level.isClientSide || this.displayedItem.isEmpty()) {
            return;
        }
        this.level.addFreshEntity(new ItemEntity(this.level,
                this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D, this.displayedItem.copy()));
        this.displayedItem = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("item", this.displayedItem.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.displayedItem = ItemStack.parseOptional(registries, tag.getCompound("item"));
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
        if (this.level != null) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
