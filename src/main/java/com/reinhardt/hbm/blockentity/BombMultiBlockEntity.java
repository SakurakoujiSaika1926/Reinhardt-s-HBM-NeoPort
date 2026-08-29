package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.BombMultiExplosions;
import com.reinhardt.hbm.menu.BombMultiMenu;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class BombMultiBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 6;
    public static final int SLOT_TNT_TOP_LEFT = 0;
    public static final int SLOT_EFFECT_TOP = 2;
    public static final int SLOT_TNT_BOTTOM_LEFT = 3;
    public static final int SLOT_EFFECT_BOTTOM = 5;
    private static final int[] NO_AUTOMATION_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public BombMultiBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.BOMB_MULTI.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (!removed.isEmpty()) {
            setChangedAndSync();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        ItemStack stored = stack.copy();
        stored.setCount(Math.min(1, stored.getCount()));
        this.items.set(slot, stored);
        setChangedAndSync();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // 1.7.10 exposed no sides for automation. GUI slots explicitly allow any item.
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        clearSlots();
        setChangedAndSync();
    }

    public boolean isLoaded() {
        return this.items.get(0).is(Items.TNT)
                && this.items.get(1).is(Items.TNT)
                && this.items.get(3).is(Items.TNT)
                && this.items.get(4).is(Items.TNT);
    }

    public int typeForSlot(int slot) {
        if (!validSlot(slot)) {
            return 0;
        }
        return typeFor(this.items.get(slot));
    }

    public static int typeFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Items.GUNPOWDER)) {
            return 1;
        }
        if (stack.is(Items.TNT)) {
            return 2;
        }
        if (isHbmItem(stack, "pellet_cluster")) {
            return 3;
        }
        if (isHbmItem(stack, "powder_fire")) {
            return 4;
        }
        if (isHbmItem(stack, "powder_poison")) {
            return 5;
        }
        if (isHbmItem(stack, "pellet_gas")) {
            return 6;
        }
        return 0;
    }

    private static boolean isHbmItem(ItemStack stack, String id) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ReinhardtsHBM.id(id));
    }

    public void detonate() {
        if (!(this.level instanceof ServerLevel serverLevel) || !isLoaded()) {
            return;
        }
        int topEffect = typeForSlot(SLOT_EFFECT_TOP);
        int bottomEffect = typeForSlot(SLOT_EFFECT_BOTTOM);
        clearSlots();
        setChanged();
        serverLevel.removeBlock(this.worldPosition, false);
        BombMultiExplosions.detonate(serverLevel, this.worldPosition, topEffect, bottomEffect);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.bomb_multi");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BombMultiMenu(containerId, playerInventory, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearSlots();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
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

    private void clearSlots() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.is(HbmBlocks.BOMB_MULTI.get())) {
                this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }
}
