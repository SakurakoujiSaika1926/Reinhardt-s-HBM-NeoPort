package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
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

/** Slot layout and button behavior copied from 1.7.10 ContainerForceField. */
public final class ForcefieldMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 3;
    private static final int PLAYER_START = SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public ForcefieldMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, findMachine(inventory, buffer.readBlockPos()), new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public ForcefieldMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private ForcefieldMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.FORCEFIELD.get(), containerId);
        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof BlockEntity entity ? entity.getBlockPos().immutable() : BlockPos.ZERO;

        this.addSlot(new ValidatedSlot(container, 0, 26, 53));
        this.addSlot(new ValidatedSlot(container, 1, 89, 35));
        this.addSlot(new ValidatedSlot(container, 2, 107, 35));
        addPlayerInventory(inventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.container instanceof LegacyMachineBlockEntity machine && stillValid(player)) {
            machine.toggleForcefield();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size() || !this.slots.get(index).hasItem()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public long power() { return Integer.toUnsignedLong(this.data.get(0)); }
    public int health() { return this.data.get(1); }
    public int maxHealth() { return Math.max(1, this.data.get(2)); }
    public boolean enabled() { return this.data.get(4) != 0; }
    public BlockPos blockPos() { return this.blockPos; }

    private static LegacyMachineBlockEntity findMachine(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine ? machine : null;
    }

    private static Container unwrap(LegacyMachineBlockEntity machine) {
        return machine == null ? new SimpleContainer(SLOT_COUNT) : machine;
    }

    private ForcefieldMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine, ContainerData data) {
        this(containerId, inventory, unwrap(machine), data);
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

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }
}
