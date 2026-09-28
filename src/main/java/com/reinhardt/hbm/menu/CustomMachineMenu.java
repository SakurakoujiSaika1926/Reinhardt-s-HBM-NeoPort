package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.CustomMachineBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
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

public final class CustomMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = CustomMachineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public CustomMachineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    private CustomMachineMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, getContainer(playerInventory, pos),
                new SimpleContainerData(CustomMachineBlockEntity.DATA_COUNT), pos);
    }

    public CustomMachineMenu(int containerId, Inventory playerInventory, Container container,
                             ContainerData data, BlockPos pos) {
        super(HbmMenus.CUSTOM_MACHINE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, CustomMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = pos.immutable();

        addSlot(new ValidatedSlot(container, CustomMachineBlockEntity.INPUT_SLOT, 44, 42));
        addSlot(new OutputSlot(container, CustomMachineBlockEntity.OUTPUT_SLOT, 116, 42));
        addSlot(new ValidatedSlot(container, CustomMachineBlockEntity.BATTERY_SLOT, 152, 67));
        addPlayerInventory(playerInventory, 8, 100);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, CustomMachineBlockEntity.BATTERY_SLOT,
                    CustomMachineBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, CustomMachineBlockEntity.INPUT_SLOT,
                CustomMachineBlockEntity.INPUT_SLOT + 1, false)) {
            if (index < PLAYER_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
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

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public int energy() {
        return this.data.get(0);
    }

    public int progress() {
        return this.data.get(1);
    }

    public int processTime() {
        return Math.max(1, this.data.get(2));
    }

    public int usage() {
        return Math.max(1, this.data.get(3));
    }

    public HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(4)).orElse(HbmFluids.none());
    }

    public int fluidAmount() {
        return this.data.get(5);
    }

    public boolean structureValid() {
        return this.data.get(6) != 0;
    }

    public boolean working() {
        return this.data.get(7) != 0;
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / CustomMachineBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
    }

    public int fluidScaled(int pixels) {
        return Math.min(pixels, this.fluidAmount() * pixels / CustomMachineBlockEntity.TANK_CAPACITY);
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

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof CustomMachineBlockEntity machine
                ? machine
                : new SimpleContainer(MACHINE_SLOTS);
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
