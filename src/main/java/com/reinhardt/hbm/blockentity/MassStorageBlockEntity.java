package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.MassStorageBlock;
import com.reinhardt.hbm.menu.MassStorageMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class MassStorageBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, MachineInventory, LockableBlockEntity {
    public static final int SLOT_COUNT = 3;
    private static final int[] IO_SLOTS = {0, 2};
    private static final String ITEM_DATA_KEY = "mass_storage_data";

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int stockpile;
    private boolean output;
    private int redstone;
    private int pins;
    private boolean locked;
    private double lockMod = 0.1D;
    private boolean cheesable = true;

    public MassStorageBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.MASS_STORAGE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MassStorageBlockEntity storage) {
        if (level.isClientSide) return;
        storage.tickServer(level, pos);
    }

    public int capacity() {
        return getBlockState().getBlock() instanceof MassStorageBlock block ? block.kind().capacity() : MassStorageBlock.Kind.IRON.capacity();
    }

    public int stockpile() { return stockpile; }
    public boolean output() { return output; }
    public int redstone() { return redstone; }
    public ItemStack type() { return items.get(1).isEmpty() ? ItemStack.EMPTY : items.get(1).copy(); }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty) && stockpile == 0; }
    @Override public ItemStack getItem(int slot) { return valid(slot) ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (!valid(slot)) return ItemStack.EMPTY;
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return valid(slot) ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY; }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!valid(slot)) return;
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) stack.setCount(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (locked || stack.isEmpty()) return false;
        if (slot == 1) return stockpile == 0;
        return slot == 0 && (type().isEmpty() || same(stack, type())) && stockpile < capacity();
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return slot == 0 && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return !locked && slot == 2; }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); stockpile = 0; setChanged(); }
    @Override public Component getDisplayName() { return Component.translatable("container.reinhardtshbm.mass_storage"); }
    @Override @Nullable public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MassStorageMenu(id, inventory, this); }

    public boolean quickInsert(ItemStack stack) {
        if (!canInsert(stack)) return false;
        int amount = stack.getCount();
        if (amount > capacity() - stockpile) return false;
        stockpile += amount;
        stack.setCount(0);
        setChanged();
        return true;
    }

    public ItemStack quickExtract(boolean fullStack) {
        if (!output || type().isEmpty() || stockpile <= 0) return ItemStack.EMPTY;
        int amount = Math.min(stockpile, fullStack ? type().getMaxStackSize() : 1);
        stockpile -= amount;
        setChanged();
        return type().copyWithCount(amount);
    }

    public boolean canInsert(ItemStack stack) {
        return !locked && !type().isEmpty() && stockpile < capacity() && same(stack, type());
    }

    public void toggleOutput() { output = !output; sync(); }

    @Override public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
        if (!type().isEmpty() && stockpile > 0) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, type().copyWithCount(Math.min(stockpile, type().getMaxStackSize()))));
        items.clear();
        stockpile = 0;
    }

    @Override public boolean isLocked() { return locked; }
    @Override public void lock() { locked = true; lockChanged(); }
    @Override public void unlock() { locked = false; lockChanged(); }
    @Override public int pins() { return pins; }
    @Override public void setPins(int pins) { this.pins = pins; lockChanged(); }
    @Override public double lockMod() { return lockMod; }
    @Override public void setLockMod(double mod) { lockMod = mod; lockChanged(); }
    @Override public boolean cheesable() { return cheesable; }
    @Override public void setCheesable(boolean value) { cheesable = value; lockChanged(); }
    @Override public void lockChanged() { sync(); }

    public void loadFromItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains(ITEM_DATA_KEY)) { readData(root.getCompound(ITEM_DATA_KEY), registries); sync(); }
    }

    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag data = new CompoundTag();
        ContainerHelper.saveAllItems(data, items, registries);
        data.putInt("stockpile", stockpile);
        data.putBoolean("output", output);
        data.putInt("lock", pins);
        data.putBoolean("isLocked", locked);
        data.putDouble("lockMod", lockMod);
        data.putBoolean("cheesable", cheesable);
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(ITEM_DATA_KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("stockpile", stockpile); tag.putBoolean("output", output); tag.putInt("redstone", redstone);
        tag.putInt("lock", pins); tag.putBoolean("isLocked", locked); tag.putDouble("lockMod", lockMod); tag.putBoolean("cheesable", cheesable);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); readData(tag, registries);
        redstone = Math.max(0, Math.min(15, tag.getInt("redstone")));
    }

    private void readData(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
        stockpile = Math.max(0, Math.min(capacity(), tag.getInt("stockpile")));
        output = tag.getBoolean("output"); pins = tag.getInt("lock"); locked = tag.getBoolean("isLocked");
        lockMod = tag.contains("lockMod") ? tag.getDouble("lockMod") : 0.1D;
        cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag tag = super.getUpdateTag(registries); saveAdditional(tag, registries); return tag; }
    @Override @Nullable public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private void tickServer(Level level, BlockPos pos) {
        boolean changed = false;
        int oldRedstone = redstone;
        redstone = capacity() <= 0 ? 0 : Math.min(15, stockpile * 15 / capacity());
        if (!items.get(0).isEmpty() && items.get(0).is(HbmItems.FLUID_BARREL_INFINITE.get()) && stockpile != capacity()) {
            stockpile = capacity();
            changed = true;
        }
        if (type().isEmpty() && stockpile != 0) {
            stockpile = 0;
            changed = true;
        }
        ItemStack input = items.get(0);
        if (canInsert(input)) {
            int amount = Math.min(capacity() - stockpile, input.getCount());
            if (amount > 0) {
                input.shrink(amount);
                stockpile += amount;
                changed = true;
            }
        }
        if (output && !type().isEmpty() && (items.get(2).isEmpty() || same(items.get(2), type()))) {
            int amount = Math.min(stockpile, type().getMaxStackSize());
            if (amount > 0) {
                int room = items.get(2).isEmpty() ? type().getMaxStackSize() : items.get(2).getMaxStackSize() - items.get(2).getCount();
                amount = Math.min(amount, room);
                if (amount > 0) {
                    items.set(2, items.get(2).isEmpty() ? type().copyWithCount(amount) : items.get(2).copyWithCount(items.get(2).getCount() + amount));
                    stockpile -= amount;
                    changed = true;
                }
            }
        }
        redstone = capacity() <= 0 ? 0 : Math.min(15, stockpile * 15 / capacity());
        if (oldRedstone != redstone) level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        if (oldRedstone != redstone || changed) sync();
    }

    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
    private boolean valid(int slot) { return slot >= 0 && slot < SLOT_COUNT; }
    private static boolean same(ItemStack left, ItemStack right) { return !left.isEmpty() && !right.isEmpty() && ItemStack.isSameItemSameComponents(left, right); }
}
