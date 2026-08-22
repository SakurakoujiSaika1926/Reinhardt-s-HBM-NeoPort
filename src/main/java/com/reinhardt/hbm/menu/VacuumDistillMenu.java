package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.VacuumDistillBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
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

public class VacuumDistillMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = VacuumDistillBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public VacuumDistillMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(VacuumDistillBlockEntity.DATA_COUNT));
    }

    public VacuumDistillMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.VACUUM_DISTILL.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, VacuumDistillBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, VacuumDistillBlockEntity.BATTERY_SLOT, 26, 90));
        this.addSlot(new ValidatedSlot(container, 1, 80, 90));
        this.addSlot(new TakeOnlySlot(container, 2, 80, 108));
        this.addSlot(new ValidatedSlot(container, 3, 98, 90));
        this.addSlot(new TakeOnlySlot(container, 4, 98, 108));
        this.addSlot(new ValidatedSlot(container, 5, 116, 90));
        this.addSlot(new TakeOnlySlot(container, 6, 116, 108));
        this.addSlot(new ValidatedSlot(container, 7, 134, 90));
        this.addSlot(new TakeOnlySlot(container, 8, 134, 108));
        this.addSlot(new ValidatedSlot(container, VacuumDistillBlockEntity.FLUID_IDENTIFIER_SLOT, 26, 108));

        addPlayerInventory(playerInventory, 8, 156);
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
        } else if (moveIntoFirstValidMachineSlot(stack)) {
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

    public HbmFluidDefinition inputFluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int inputAmount() {
        return this.data.get(1);
    }

    public int inputCapacity() {
        return Math.max(1, this.data.get(2));
    }

    public HbmFluidDefinition outputFluid(int index) {
        return HbmFluids.byOldId(this.data.get(5 + index * 3)).orElse(HbmFluids.none());
    }

    public int outputAmount(int index) {
        return this.data.get(6 + index * 3);
    }

    public int outputCapacity(int index) {
        return Math.max(1, this.data.get(7 + index * 3));
    }

    public int power() {
        return this.data.get(3);
    }

    public int byproductProgress() {
        return this.data.get(4);
    }

    public int lastInput() {
        return this.data.get(17);
    }

    public int maxPower() {
        return Math.max(1, this.data.get(18));
    }

    public boolean working() {
        return this.data.get(19) != 0;
    }

    public int currentPowerCost() {
        return Math.max(1, this.data.get(20));
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / maxPower()));
    }

    public int inputScaled(int pixels) {
        return Math.min(pixels, this.inputAmount() * pixels / inputCapacity());
    }

    public int outputScaled(int index, int pixels) {
        return Math.min(pixels, this.outputAmount(index) * pixels / outputCapacity(index));
    }

    private boolean moveIntoFirstValidMachineSlot(ItemStack stack) {
        for (int slot = 0; slot < MACHINE_SLOT_COUNT; slot++) {
            if (!this.container.canPlaceItem(slot, stack)) {
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
        if (blockEntity instanceof VacuumDistillBlockEntity refinery) {
            return refinery;
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

