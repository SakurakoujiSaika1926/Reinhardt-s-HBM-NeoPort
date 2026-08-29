package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.BombMultiBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BombMultiMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = BombMultiBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOTS;
    private final Container container;

    public BombMultiMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, containerAt(playerInventory, buffer.readBlockPos()));
    }

    public BombMultiMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.BOMB_MULTI.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        this.container = container;

        addSlot(new ManualSlot(container, 0, 44, 26));
        addSlot(new ManualSlot(container, 1, 62, 26));
        addSlot(new ManualSlot(container, 2, 80, 26));
        addSlot(new ManualSlot(container, 3, 44, 44));
        addSlot(new ManualSlot(container, 4, 62, 44));
        addSlot(new ManualSlot(container, 5, 80, 44));

        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // This follows ContainerBombMulti exactly: shift-click may only remove its six loaded components.
        if (index >= MACHINE_SLOTS || !moveItemStackTo(stack, PLAYER_INVENTORY_START, this.slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int typeForSlot(int slot) {
        if (this.container instanceof BombMultiBlockEntity bomb) {
            return bomb.typeForSlot(slot);
        }
        return BombMultiBlockEntity.typeFor(this.container.getItem(slot));
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    private static Container containerAt(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof BombMultiBlockEntity bomb ? bomb : new SimpleContainer(MACHINE_SLOTS);
    }

    private static final class ManualSlot extends Slot {
        private ManualSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
