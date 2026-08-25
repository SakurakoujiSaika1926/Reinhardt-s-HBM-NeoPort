package com.reinhardt.hbm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Shared inventory persistence and sided access for old drone-network blocks. */
public abstract class DroneInventoryBlockEntity extends BlockEntity implements WorldlyContainer, MachineInventory {
    protected final NonNullList<ItemStack> items;
    private final int[] allSlots;

    protected DroneInventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.allSlots = new int[size];
        for (int index = 0; index < size; index++) {
            allSlots[index] = index;
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return valid(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!valid(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            sync();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!valid(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!valid(slot)) {
            return;
        }
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return valid(slot);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return allSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return valid(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 128.0D;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
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

    protected void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    protected boolean valid(int slot) {
        return slot >= 0 && slot < items.size();
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        sync();
    }
}
