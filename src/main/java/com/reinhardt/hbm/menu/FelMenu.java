package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.FelBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FelCrystalItem;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.util.Wavelength;
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

public class FelMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = FelBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public FelMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(FelBlockEntity.DATA_COUNT));
    }

    public FelMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.FEL.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, FelBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof FelBlockEntity fel ? fel.getBlockPos() : BlockPos.ZERO;

        this.addSlot(new ValidatedSlot(container, FelBlockEntity.BATTERY_SLOT, 182, 144));
        this.addSlot(new ValidatedSlot(container, FelBlockEntity.CRYSTAL_SLOT, 141, 23));

        addPlayerInventory(playerInventory, 8, 83);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.container instanceof FelBlockEntity fel && stillValid(player)) {
            fel.toggleEnabled();
            return true;
        }
        return false;
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
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, FelBlockEntity.BATTERY_SLOT, FelBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FelCrystalItem) {
            if (!moveItemStackTo(stack, FelBlockEntity.CRYSTAL_SLOT, FelBlockEntity.CRYSTAL_SLOT + 1, false)) {
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

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public long power() {
        return Integer.toUnsignedLong(this.data.get(0));
    }

    public long maxPower() {
        return Integer.toUnsignedLong(this.data.get(1));
    }

    public long lastInput() {
        return Integer.toUnsignedLong(this.data.get(2));
    }

    public Wavelength mode() {
        Wavelength[] values = Wavelength.values();
        int ordinal = this.data.get(3);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Wavelength.NULL;
    }

    public boolean enabled() {
        return this.data.get(4) != 0;
    }

    public boolean missingValidSilex() {
        return this.data.get(5) != 0;
    }

    public int distance() {
        return this.data.get(6);
    }

    public int activeCost() {
        return this.data.get(7);
    }

    public int powerScaled(int pixels) {
        long maxPower = Math.max(1L, maxPower());
        return (int) Math.min(pixels, power() * pixels / maxPower);
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
        return blockEntity instanceof FelBlockEntity fel ? fel : new SimpleContainer(MACHINE_SLOT_COUNT);
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
}
