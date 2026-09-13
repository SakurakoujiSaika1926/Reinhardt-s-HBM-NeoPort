package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.menu.SafeMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.CaveSpider;
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

import java.util.Random;

public final class SafeBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, MachineInventory, LockableBlockEntity {
    public static final int SLOT_COUNT = 15;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
    private static final String ITEM_DATA_KEY = "safe_data";

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int pins;
    private boolean locked;
    private double lockMod = 0.1D;
    private boolean cheesable = true;
    private boolean hasSpiders;

    public SafeBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SAFE.get(), pos, state);
    }

    @Override
    public int getContainerSize() { return SLOT_COUNT; }

    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getItem(int slot) { return valid(slot) ? items.get(slot) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!valid(slot)) return ItemStack.EMPTY;
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return valid(slot) ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!valid(slot)) return;
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) stack.setCount(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return valid(slot) && !locked; }

    @Override
    public int[] getSlotsForFace(Direction side) { return SLOTS; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return valid(slot) && !locked; }

    @Override
    public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() { items.clear(); setChanged(); }

    @Override
    public Component getDisplayName() { return Component.translatable("container.reinhardtshbm.safe"); }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SafeMenu(containerId, inventory, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            items.set(slot, ItemStack.EMPTY);
        }
    }

    /** TileEntityCrateBase#fillWithSpiders. */
    public void fillWithSpiders() {
        hasSpiders = true;
        sync();
    }

    /** Exact placed-crate branch of TileEntityCrateBase#spawnSpiders. */
    public void releaseSpiders(Player player) {
        if (!hasSpiders || level == null || level.isClientSide) {
            return;
        }
        Random random = new Random();
        for (int index = 0; index < 3; index++) {
            CaveSpider spider = new CaveSpider(EntityType.CAVE_SPIDER, level);
            spider.moveTo(worldPosition.getX() + random.nextGaussian() * 2.0D,
                    worldPosition.getY() + 1.0D,
                    worldPosition.getZ() + random.nextGaussian() * 2.0D,
                    random.nextFloat(), 0.0F);
            spider.setTarget(player);
            level.addFreshEntity(spider);
        }
        hasSpiders = false;
        sync();
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
        if (root.contains(ITEM_DATA_KEY)) {
            readData(root.getCompound(ITEM_DATA_KEY), registries);
            sync();
        }
    }

    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag data = new CompoundTag();
        ContainerHelper.saveAllItems(data, items, registries);
        data.putInt("lock", pins);
        data.putBoolean("isLocked", locked);
        data.putDouble("lockMod", lockMod);
        data.putBoolean("cheesable", cheesable);
        if (hasSpiders) {
            data.putBoolean("spiders", true);
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(ITEM_DATA_KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("lock", pins);
        tag.putBoolean("isLocked", locked);
        tag.putDouble("lockMod", lockMod);
        tag.putBoolean("cheesable", cheesable);
        tag.putBoolean("spiders", hasSpiders);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        readLockData(tag);
        hasSpiders = tag.getBoolean("spiders");
    }

    private void readData(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
        readLockData(tag);
    }

    private void readLockData(CompoundTag tag) {
        pins = tag.getInt("lock");
        locked = tag.getBoolean("isLocked");
        lockMod = tag.contains("lockMod") ? tag.getDouble("lockMod") : 0.1D;
        cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
        hasSpiders = tag.getBoolean("spiders");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private boolean valid(int slot) { return slot >= 0 && slot < SLOT_COUNT; }
}
