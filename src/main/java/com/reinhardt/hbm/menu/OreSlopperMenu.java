package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.OreSlopperBlockEntity;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
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

public class OreSlopperMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = OreSlopperBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public OreSlopperMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(OreSlopperBlockEntity.DATA_COUNT));
    }

    public OreSlopperMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.ORE_SLOPPER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, OreSlopperBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, OreSlopperBlockEntity.BATTERY_SLOT, 8, 72));
        this.addSlot(new ValidatedSlot(container, OreSlopperBlockEntity.FLUID_ID_SLOT, 26, 72));
        this.addSlot(new ValidatedSlot(container, OreSlopperBlockEntity.INPUT_SLOT, 71, 27));
        this.addSlot(new TakeOnlySlot(container, OreSlopperBlockEntity.OUTPUT_START, 134, 18));
        this.addSlot(new TakeOnlySlot(container, OreSlopperBlockEntity.OUTPUT_START + 1, 152, 18));
        this.addSlot(new TakeOnlySlot(container, OreSlopperBlockEntity.OUTPUT_START + 2, 134, 36));
        this.addSlot(new TakeOnlySlot(container, OreSlopperBlockEntity.OUTPUT_START + 3, 152, 36));
        this.addSlot(new TakeOnlySlot(container, OreSlopperBlockEntity.OUTPUT_START + 4, 134, 54));
        this.addSlot(new TakeOnlySlot(container, OreSlopperBlockEntity.OUTPUT_START + 5, 152, 54));
        this.addSlot(new ValidatedSlot(container, OreSlopperBlockEntity.UPGRADE_START, 62, 72));
        this.addSlot(new ValidatedSlot(container, OreSlopperBlockEntity.UPGRADE_START + 1, 80, 72));

        addPlayerInventory(playerInventory, 8, 122);
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
        } else if (stack.is(HbmItems.BEDROCK_ORE_BASE.get())) {
            if (!moveItemStackTo(stack, OreSlopperBlockEntity.INPUT_SLOT, OreSlopperBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, OreSlopperBlockEntity.UPGRADE_START, OreSlopperBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(OreSlopperBlockEntity.BATTERY_SLOT, stack)) {
            if (!moveItemStackTo(stack, OreSlopperBlockEntity.BATTERY_SLOT, OreSlopperBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(OreSlopperBlockEntity.FLUID_ID_SLOT, stack)) {
            if (!moveItemStackTo(stack, OreSlopperBlockEntity.FLUID_ID_SLOT, OreSlopperBlockEntity.FLUID_ID_SLOT + 1, false)) {
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

    public int power() {
        return this.data.get(0);
    }

    public int maxPower() {
        return (int) OreSlopperBlockEntity.MAX_POWER;
    }

    public int progress() {
        return this.data.get(2);
    }

    public int processTime() {
        return Math.max(1, this.data.get(3));
    }

    public int consumption() {
        return Math.max(1, this.data.get(4));
    }

    public boolean processing() {
        return this.data.get(5) != 0;
    }

    public int waterAmount() {
        return this.data.get(6);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition waterFluid() {
        return HbmFluids.byOldId(this.data.get(7)).orElse(HbmFluids.none());
    }

    public int slopAmount() {
        return this.data.get(8);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition slopFluid() {
        return HbmFluids.byOldId(this.data.get(9)).orElse(HbmFluids.none());
    }

    public int tankCapacity() {
        return OreSlopperBlockEntity.TANK_CAPACITY;
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / Math.max(1, this.maxPower())));
    }

    public int waterScaled(int pixels) {
        return Math.min(pixels, this.waterAmount() * pixels / this.tankCapacity());
    }

    public int slopScaled(int pixels) {
        return Math.min(pixels, this.slopAmount() * pixels / this.tankCapacity());
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
        if (blockEntity instanceof OreSlopperBlockEntity slopper) {
            return slopper;
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
