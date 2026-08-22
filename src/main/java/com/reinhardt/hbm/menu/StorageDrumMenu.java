package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.StorageDrumBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The 6x6 ring layout and 234 px height match GUIStorageDrum in 1.7.10. */
public final class StorageDrumMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = StorageDrumBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public StorageDrumMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolve(playerInventory, buffer.readBlockPos()));
    }

    private StorageDrumMenu(int containerId, Inventory playerInventory, Resolved resolved) {
        this(containerId, playerInventory, resolved.container(), resolved.data());
    }

    public StorageDrumMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.STORAGE_DRUM.get(), containerId);
        checkContainerSize(container, StorageDrumBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;

        int index = 0;
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 6; column++) {
                if (column + row > 1 && column + row < 9 && 5 - column + row > 1 && column + 5 - row > 1) {
                    addSlot(new ValidatedSlot(container, index++, 35 + column * 18, 24 + row * 18));
                }
            }
        }
        addPlayerInventory(playerInventory, 8, 152);
        addDataSlots(data);
    }

    public int liquidAmount() {
        return this.data.get(0);
    }

    public int liquidCapacity() {
        return this.data.get(1);
    }

    public int gasAmount() {
        return this.data.get(2);
    }

    public int gasCapacity() {
        return this.data.get(3);
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
        if (index < StorageDrumBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(0, stack)) {
            if (!moveItemStackTo(stack, 0, StorageDrumBlockEntity.SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
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

    private static Resolved resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof StorageDrumBlockEntity drum) {
            return new Resolved(drum, drum.menuData());
        }
        return new Resolved(new SimpleContainer(StorageDrumBlockEntity.SLOT_COUNT), new SimpleContainerData(4));
    }

    private record Resolved(Container container, ContainerData data) {
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
