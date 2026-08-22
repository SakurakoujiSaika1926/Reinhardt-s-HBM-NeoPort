package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.menu.NukeBoyMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class NukeBoyBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_SHIELDING = 0;
    public static final int SLOT_TARGET = 1;
    public static final int SLOT_BULLET = 2;
    public static final int SLOT_PROPELLANT = 3;
    public static final int SLOT_IGNITER = 4;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 1;

    private static final int[] NO_AUTOMATION_SLOTS = {};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 && NukeBoyBlockEntity.this.isReady() ? 1 : 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public NukeBoyBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.NUKE_BOY.get(), pos, blockState);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChangedAndSync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValidSlot(slot) && itemForSlot(slot) == stack.getItem();
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
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync();
    }

    public boolean isReady() {
        return isSlotReady(SLOT_SHIELDING)
                && isSlotReady(SLOT_TARGET)
                && isSlotReady(SLOT_BULLET)
                && isSlotReady(SLOT_PROPELLANT)
                && isSlotReady(SLOT_IGNITER);
    }

    public boolean slotHasExpectedItem(int slot) {
        return isSlotReady(slot);
    }

    public void detonate() {
        Level level = this.level;
        if (!(level instanceof ServerLevel serverLevel) || !isReady()) {
            return;
        }

        clearSlotsNoSync();
        BlockPos pos = this.worldPosition.immutable();
        level.removeBlock(pos, false);
        NukeExplosionManager.scheduleLittleBoy(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.nuke_boy");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeBoyMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            drop(level, pos, this.items.get(slot));
            this.items.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
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

    private boolean isSlotReady(int slot) {
        return isValidSlot(slot) && this.items.get(slot).is(itemForSlot(slot));
    }

    private static Item itemForSlot(int slot) {
        return switch (slot) {
            case SLOT_SHIELDING -> HbmItems.BOY_SHIELDING.get();
            case SLOT_TARGET -> HbmItems.BOY_TARGET.get();
            case SLOT_BULLET -> HbmItems.BOY_BULLET.get();
            case SLOT_PROPELLANT -> HbmItems.BOY_PROPELLANT.get();
            case SLOT_IGNITER -> HbmItems.BOY_IGNITER.get();
            default -> HbmItems.BOY_SHIELDING.get();
        };
    }

    private void clearSlotsNoSync() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.is(HbmBlocks.NUKE_BOY.get())) {
                this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack.copy()
        ));
    }
}
