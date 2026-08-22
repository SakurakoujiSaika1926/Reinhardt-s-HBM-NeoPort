package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.WasteDrumBlockEntity;
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

public class WasteDrumMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = WasteDrumBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;

    public WasteDrumMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()));
    }

    public WasteDrumMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.WASTE_DRUM.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        this.container = container;

        addSlot(new ValidatedSlot(container, 0, 71, 21));
        addSlot(new ValidatedSlot(container, 1, 89, 21));
        addSlot(new ValidatedSlot(container, 2, 53, 39));
        addSlot(new ValidatedSlot(container, 3, 71, 39));
        addSlot(new ValidatedSlot(container, 4, 89, 39));
        addSlot(new ValidatedSlot(container, 5, 107, 39));
        addSlot(new ValidatedSlot(container, 6, 53, 57));
        addSlot(new ValidatedSlot(container, 7, 71, 57));
        addSlot(new ValidatedSlot(container, 8, 89, 57));
        addSlot(new ValidatedSlot(container, 9, 107, 57));
        addSlot(new ValidatedSlot(container, 10, 71, 75));
        addSlot(new ValidatedSlot(container, 11, 89, 75));
        addPlayerInventory(playerInventory, 8, 107);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (this.container.canPlaceItem(0, stack)) {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof WasteDrumBlockEntity drum ? drum : new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }
}
