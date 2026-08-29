package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.menu.FilingCabinetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Eight-slot, manually operated filing cabinet with the 1.7.10 drawer animation. */
public final class FilingCabinetBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, MachineInventory, LockableBlockEntity {
    public static final int SLOT_COUNT = 8;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int timer;
    private int playersUsing;
    private int pins;
    private boolean locked;
    private double lockMod = 0.1D;
    private boolean cheesable = true;
    public float lowerExtent;
    public float prevLowerExtent;
    public float upperExtent;
    public float prevUpperExtent;

    public FilingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FILING_CABINET.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FilingCabinetBlockEntity cabinet) {
        if (!level.isClientSide) {
            if (cabinet.playersUsing > 0) {
                cabinet.timer = Math.min(10, cabinet.timer + 1);
            } else {
                cabinet.timer = 0;
            }
            cabinet.sync();
        } else {
            cabinet.prevLowerExtent = cabinet.lowerExtent;
            cabinet.prevUpperExtent = cabinet.upperExtent;
        }

        float speed = cabinet.playersUsing > 0 ? 1.0F / 16.0F : 1.0F / 25.0F;
        float max = 0.8F;
        if (cabinet.playersUsing > 0) {
            cabinet.lowerExtent += speed;
            if (cabinet.timer >= 10) {
                cabinet.upperExtent += speed;
            }
        } else if (cabinet.lowerExtent > 0.0F) {
            cabinet.lowerExtent -= speed;
            cabinet.upperExtent -= speed;
        }
        cabinet.lowerExtent = clamp(cabinet.lowerExtent, 0.0F, max);
        cabinet.upperExtent = clamp(cabinet.upperExtent, 0.0F, max);
    }

    public void openInventory() {
        playersUsing++;
        playDrawerSound(true);
    }

    public void closeInventory() {
        playersUsing = Math.max(0, playersUsing - 1);
        playDrawerSound(false);
    }

    private void playDrawerSound(boolean open) {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, open ? HbmSoundEvents.CRATE_OPEN.get() : HbmSoundEvents.CRATE_CLOSE.get(),
                    SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public float lower(float partialTick) {
        return prevLowerExtent + (lowerExtent - prevLowerExtent) * partialTick;
    }

    public float upper(float partialTick) {
        return prevUpperExtent + (upperExtent - prevUpperExtent) * partialTick;
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return valid(slot) ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (!valid(slot)) return ItemStack.EMPTY;
        ItemStack result = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return valid(slot) ? net.minecraft.world.ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) { if (valid(slot)) { items.set(slot, stack); setChanged(); } }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return valid(slot); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public Component getDisplayName() { return Component.translatable("container.reinhardtshbm.filing_cabinet"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new FilingCabinetMenu(id, inventory, this); }

    @Override public boolean isLocked() { return locked; }
    @Override public void lock() { locked = true; lockChanged(); }
    @Override public void unlock() { locked = false; lockChanged(); }
    @Override public int pins() { return pins; }
    @Override public void setPins(int value) { pins = value; lockChanged(); }
    @Override public double lockMod() { return lockMod; }
    @Override public void setLockMod(double value) { lockMod = value; lockChanged(); }
    @Override public boolean cheesable() { return cheesable; }
    @Override public void setCheesable(boolean value) { cheesable = value; lockChanged(); }
    @Override public void lockChanged() { sync(); }

    @Override public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, stack.copy()));
        }
        clearContent();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("lock", pins);
        tag.putBoolean("isLocked", locked);
        tag.putDouble("lockMod", lockMod);
        tag.putBoolean("cheesable", cheesable);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items, registries);
        pins = tag.getInt("lock");
        locked = tag.getBoolean("isLocked");
        lockMod = tag.contains("lockMod") ? tag.getDouble("lockMod") : .1D;
        cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag tag = super.getUpdateTag(registries); saveAdditional(tag, registries); return tag; }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private boolean valid(int slot) { return slot >= 0 && slot < SLOT_COUNT; }
}
