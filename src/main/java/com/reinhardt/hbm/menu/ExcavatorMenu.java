package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ExcavatorBlockEntity;
import com.reinhardt.hbm.item.DrillbitItem;
import com.reinhardt.hbm.registry.HbmFluids;
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

public class ExcavatorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ExcavatorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public ExcavatorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(ExcavatorBlockEntity.DATA_COUNT));
    }

    public ExcavatorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.EXCAVATOR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ExcavatorBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, ExcavatorBlockEntity.BATTERY_SLOT, 220, 72));
        this.addSlot(new ValidatedSlot(container, ExcavatorBlockEntity.FLUID_ID_SLOT, 202, 72));
        this.addSlot(new ValidatedSlot(container, ExcavatorBlockEntity.UPGRADE_START, 136, 75));
        this.addSlot(new ValidatedSlot(container, ExcavatorBlockEntity.UPGRADE_START + 1, 154, 75));
        this.addSlot(new ValidatedSlot(container, ExcavatorBlockEntity.DRILLBIT_SLOT, 172, 75));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new TakeOnlySlot(container, ExcavatorBlockEntity.OUTPUT_START + row * 3 + column, 136 + column * 18, 5 + row * 18));
            }
        }
        addPlayerInventory(playerInventory, 41, 122);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < 5 && this.container instanceof ExcavatorBlockEntity excavator && stillValid(player)) {
            excavator.toggle(id);
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
        } else if (this.container.canPlaceItem(ExcavatorBlockEntity.BATTERY_SLOT, stack)) {
            if (!moveItemStackTo(stack, ExcavatorBlockEntity.BATTERY_SLOT, ExcavatorBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(ExcavatorBlockEntity.FLUID_ID_SLOT, stack)) {
            if (!moveItemStackTo(stack, ExcavatorBlockEntity.FLUID_ID_SLOT, ExcavatorBlockEntity.FLUID_ID_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(ExcavatorBlockEntity.DRILLBIT_SLOT, stack)) {
            if (!moveItemStackTo(stack, ExcavatorBlockEntity.DRILLBIT_SLOT, ExcavatorBlockEntity.DRILLBIT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (moveUpgrade(stack)) {
            return moved;
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

    public int power() {
        return this.data.get(0);
    }

    public int maxPower() {
        return (int) ExcavatorBlockEntity.MAX_POWER;
    }

    public boolean toggleState(int id) {
        return this.data.get(2 + id) != 0;
    }

    public boolean operational() {
        return this.data.get(7) != 0;
    }

    public boolean hasInstalledDrill() {
        return DrillbitItem.typeOf(this.container.getItem(ExcavatorBlockEntity.DRILLBIT_SLOT)) != null;
    }

    public boolean canVeinMine() {
        DrillbitItem.DrillType type = DrillbitItem.typeOf(this.container.getItem(ExcavatorBlockEntity.DRILLBIT_SLOT));
        return toggleState(3) && type != null && type.vein();
    }

    public boolean canSilkTouch() {
        DrillbitItem.DrillType type = DrillbitItem.typeOf(this.container.getItem(ExcavatorBlockEntity.DRILLBIT_SLOT));
        return toggleState(4) && type != null && type.silk();
    }

    public int targetDepth() {
        return this.data.get(8);
    }

    public int consumption() {
        return this.data.get(10);
    }

    public int fluidAmount() {
        return this.data.get(11);
    }

    public int fluidCapacity() {
        return ExcavatorBlockEntity.TANK_CAPACITY;
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(12)).orElse(HbmFluids.none());
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / Math.max(1, this.maxPower())));
    }

    public int fluidScaled(int pixels) {
        return Math.min(pixels, this.fluidAmount() * pixels / Math.max(1, this.fluidCapacity()));
    }

    private boolean moveUpgrade(ItemStack stack) {
        for (int slot = ExcavatorBlockEntity.UPGRADE_START; slot < ExcavatorBlockEntity.UPGRADE_END; slot++) {
            if (slot == ExcavatorBlockEntity.DRILLBIT_SLOT || !this.container.canPlaceItem(slot, stack)) {
                continue;
            }
            if (moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return false;
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
        return blockEntity instanceof ExcavatorBlockEntity excavator ? excavator : new SimpleContainer(MACHINE_SLOT_COUNT);
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

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
