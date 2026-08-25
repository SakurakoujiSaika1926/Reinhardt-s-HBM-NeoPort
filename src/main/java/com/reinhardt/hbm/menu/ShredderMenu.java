package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.item.BladesItem;
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

public class ShredderMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ShredderBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public ShredderMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(ShredderBlockEntity.DATA_COUNT));
    }

    public ShredderMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.SHREDDER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ShredderBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        addInputSlots(container);
        addOutputSlots(container);
        this.addSlot(new BladeSlot(container, ShredderBlockEntity.LEFT_BLADE_SLOT, 44, 108));
        this.addSlot(new BladeSlot(container, ShredderBlockEntity.RIGHT_BLADE_SLOT, 80, 108));
        this.addSlot(new BatterySlot(container, ShredderBlockEntity.BATTERY_SLOT, 8, 108));
        addPlayerInventory(playerInventory, 8, 151);
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
        } else if (stack.getItem() instanceof BladesItem) {
            if (!moveItemStackTo(stack, ShredderBlockEntity.LEFT_BLADE_SLOT, ShredderBlockEntity.RIGHT_BLADE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, ShredderBlockEntity.BATTERY_SLOT, ShredderBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ShredderBlockEntity.canAcceptInput(stack)) {
            if (!moveItemStackTo(stack, ShredderBlockEntity.INPUT_START, ShredderBlockEntity.INPUT_END, false)) {
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

    public int leftGear() {
        return this.data.get(3);
    }

    public int rightGear() {
        return this.data.get(4);
    }

    public int completedCycles() {
        return this.data.get(5);
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / ShredderBlockEntity.ENERGY_CAPACITY));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / ShredderBlockEntity.PROCESSING_SPEED);
    }

    public boolean isWorking() {
        return this.progress() > 0 && this.energy() >= ShredderBlockEntity.DEMAND_PER_TICK;
    }

    public boolean hasBladeError() {
        return this.leftGear() == 0 || this.leftGear() == 3 || this.rightGear() == 0 || this.rightGear() == 3;
    }

    private void addInputSlots(Container container) {
        int slot = ShredderBlockEntity.INPUT_START;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new InputSlot(container, slot++, 44 + column * 18, 18 + row * 18));
            }
        }
    }

    private void addOutputSlots(Container container) {
        int slot = ShredderBlockEntity.OUTPUT_START;
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new OutputSlot(container, slot++, 116 + column * 18, 18 + row * 18));
            }
        }
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
        if (blockEntity instanceof ShredderBlockEntity shredder) {
            return shredder;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static final class InputSlot extends Slot {
        private InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
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

    private static final class BladeSlot extends Slot {
        private BladeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof BladesItem;
        }
    }

    private static final class BatterySlot extends Slot {
        private BatterySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ShredderBlockEntity.isBattery(stack);
        }
    }
}
