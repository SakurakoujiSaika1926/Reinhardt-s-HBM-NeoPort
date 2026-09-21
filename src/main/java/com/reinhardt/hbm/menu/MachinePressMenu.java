package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.PressBlockEntity;
import com.reinhardt.hbm.item.StampItem;
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

public class MachinePressMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = PressBlockEntity.FIRE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public MachinePressMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(PressBlockEntity.DATA_COUNT));
    }

    public MachinePressMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.MACHINE_PRESS.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, PressBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, PressBlockEntity.FIRE_FUEL_SLOT, 26, 53));
        this.addSlot(new StampSlot(container, PressBlockEntity.STAMP_SLOT, 80, 17));
        this.addSlot(new ValidatedSlot(container, PressBlockEntity.INPUT_SLOT, 80, 53));
        this.addSlot(new OutputSlot(container, PressBlockEntity.OUTPUT_SLOT, 140, 35));
        for (int index = 0; index < 9; index++) {
            this.addSlot(new Slot(container, PressBlockEntity.FIRE_STORAGE_START + index, 8 + index * 18, 84));
        }
        addPlayerInventory(playerInventory, 8, 120);
        addDataSlots(data);
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
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            if (index == PressBlockEntity.OUTPUT_SLOT) {
                slot.onTake(player, stack);
            }
        } else if (StampItem.isStamp(stack)) {
            if (!moveItemStackTo(stack, PressBlockEntity.STAMP_SLOT, PressBlockEntity.STAMP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(PressBlockEntity.INPUT_SLOT, stack)) {
            if (!moveItemStackTo(stack, PressBlockEntity.INPUT_SLOT, PressBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(PressBlockEntity.FIRE_FUEL_SLOT, stack)) {
            if (!moveItemStackTo(stack, PressBlockEntity.FIRE_FUEL_SLOT, PressBlockEntity.FIRE_FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PressBlockEntity.FIRE_STORAGE_START, PressBlockEntity.FIRE_STORAGE_END, false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
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

    public int burnTime() {
        return this.data.get(1);
    }

    public int speed() {
        return this.data.get(3);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int completedCycles() {
        return this.data.get(4);
    }

    public int operationsLeft() {
        return this.burnTime() / PressBlockEntity.FIRE_BURN_PER_OPERATION;
    }

    public int speedPercent() {
        return this.speed() * 100 / PressBlockEntity.MAX_SPEED;
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / PressBlockEntity.MAX_PROGRESS);
    }

    public int speedScaled(int pixels) {
        return Math.min(pixels, this.speed() * pixels / PressBlockEntity.MAX_SPEED);
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof PressBlockEntity press && press.kind() == PressBlockEntity.Kind.FIRE) {
            return press;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }

    private static final class StampSlot extends ValidatedSlot {
        private StampSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class OutputSlot extends LegacyAchievementOutputSlot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
