package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.MachineKeyForgeBlockEntity;
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

public class MachineKeyForgeMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = MachineKeyForgeBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;

    public MachineKeyForgeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()));
    }

    public MachineKeyForgeMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.MACHINE_KEYFORGE.get(), containerId);
        checkContainerSize(container, MachineKeyForgeBlockEntity.SLOT_COUNT);
        this.container = container;

        addSlot(new KeyForgeSlot(container, MachineKeyForgeBlockEntity.SLOT_SOURCE, 44, 35));
        addSlot(new KeyForgeSlot(container, MachineKeyForgeBlockEntity.SLOT_TARGET, 80, 35));
        addSlot(new KeyForgeSlot(container, MachineKeyForgeBlockEntity.SLOT_RANDOMIZE, 116, 35));
        addPlayerInventory(playerInventory);
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
        if (index < MachineKeyForgeBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(MachineKeyForgeBlockEntity.SLOT_SOURCE, stack)) {
            if (!moveItemStackTo(stack, 0, MachineKeyForgeBlockEntity.SLOT_COUNT, false)) {
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

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof MachineKeyForgeBlockEntity keyForge
                ? keyForge
                : new SimpleContainer(MachineKeyForgeBlockEntity.SLOT_COUNT);
    }

    private static final class KeyForgeSlot extends Slot {
        private KeyForgeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }
}
