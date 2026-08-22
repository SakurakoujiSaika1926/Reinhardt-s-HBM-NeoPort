package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.PressBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.item.MachineUpgradeItem;
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

public class MachineEPressMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = PressBlockEntity.ELECTRIC_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public MachineEPressMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(PressBlockEntity.DATA_COUNT));
    }

    public MachineEPressMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.MACHINE_EPRESS.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, PressBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, PressBlockEntity.ELECTRIC_BATTERY_SLOT, 44, 53));
        this.addSlot(new StampSlot(container, PressBlockEntity.STAMP_SLOT, 80, 17));
        this.addSlot(new ValidatedSlot(container, PressBlockEntity.INPUT_SLOT, 80, 53));
        this.addSlot(new OutputSlot(container, PressBlockEntity.OUTPUT_SLOT, 140, 35));
        this.addSlot(new UpgradeSlot(container, PressBlockEntity.ELECTRIC_UPGRADE_SLOT, 44, 21));
        addPlayerInventory(playerInventory, 8, 84);
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
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, PressBlockEntity.ELECTRIC_BATTERY_SLOT, PressBlockEntity.ELECTRIC_BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, PressBlockEntity.ELECTRIC_UPGRADE_SLOT, PressBlockEntity.ELECTRIC_UPGRADE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (StampItem.isStamp(stack)) {
            if (!moveItemStackTo(stack, PressBlockEntity.STAMP_SLOT, PressBlockEntity.STAMP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(PressBlockEntity.INPUT_SLOT, stack)) {
            if (!moveItemStackTo(stack, PressBlockEntity.INPUT_SLOT, PressBlockEntity.INPUT_SLOT + 1, false)) {
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

    public int energy() {
        return this.data.get(0);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int completedCycles() {
        return this.data.get(4);
    }

    public int currentConsumption() {
        return Math.max(1, this.data.get(5));
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / PressBlockEntity.ELECTRIC_ENERGY_CAPACITY));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / PressBlockEntity.MAX_PROGRESS);
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
        if (blockEntity instanceof PressBlockEntity press && press.kind() == PressBlockEntity.Kind.ELECTRIC) {
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

    private static final class UpgradeSlot extends ValidatedSlot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
