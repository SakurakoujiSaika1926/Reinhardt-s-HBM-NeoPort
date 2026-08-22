package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Direct 1.7.10 ContainerMachineRadGen layout: 12 inputs and 12 outputs. */
public final class RadGenMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 24;
    private static final int INPUT_SLOTS = 12;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int DATA_SIZE = 37;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public RadGenMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, getContainer(inventory, buffer.readBlockPos()));
    }

    public RadGenMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, new RadGenData(machine));
    }

    private RadGenMenu(int containerId, Inventory inventory, Container container) {
        this(containerId, inventory, container, new RadGenData(container));
    }

    private RadGenMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.RADGEN.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, DATA_SIZE);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof BlockEntity entity ? entity.getBlockPos().immutable() : BlockPos.ZERO;

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new ValidatedSlot(container, column + row * 3, 8 + column * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new OutputSlot(container, 12 + column + row * 3, 116 + column * 18, 17 + row * 18));
            }
        }
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = this.slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = source.getItem();
        ItemStack moved = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, INPUT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int energy() { return this.data.get(0); }
    public int energyCapacity() { return 1_000_000; }
    public int progress(int slot) { return this.data.get(1 + slot); }
    public int duration(int slot) { return this.data.get(13 + slot); }
    public int production(int slot) { return this.data.get(25 + slot); }
    public BlockPos blockPos() { return this.blockPos; }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 102 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 160));
        }
    }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_radgen")
                ? machine : new SimpleContainer(MACHINE_SLOTS);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }

    private static final class OutputSlot extends ValidatedSlot {
        private OutputSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    private static final class RadGenData implements ContainerData {
        private final Container container;
        private final int[] values = new int[DATA_SIZE];
        private RadGenData(Container container) { this.container = container; }

        @Override
        public int get(int index) {
            if (this.container instanceof LegacyMachineBlockEntity machine) {
                if (index == 0) return (int) Math.min(Integer.MAX_VALUE, machine.energyStored());
                if (index >= 1 && index < 13) return machine.radgenProgress(index - 1);
                if (index >= 13 && index < 25) return machine.radgenDuration(index - 13);
                if (index >= 25 && index < 37) return machine.radgenProduction(index - 25);
            }
            return index >= 0 && index < DATA_SIZE ? this.values[index] : 0;
        }

        @Override public void set(int index, int value) {
            if (index >= 0 && index < DATA_SIZE) this.values[index] = value;
        }
        @Override public int getCount() { return DATA_SIZE; }
    }
}
