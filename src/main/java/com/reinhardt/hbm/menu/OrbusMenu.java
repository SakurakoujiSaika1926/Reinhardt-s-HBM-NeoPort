package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
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

/** Direct port of ContainerBarrel for the 512,000 mB heavy magnetic storage tank. */
public final class OrbusMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 6;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private final Container container;
    private final ContainerData data;

    public OrbusMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, findMachine(inventory, buffer.readBlockPos()), new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public OrbusMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private OrbusMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.ORBUS.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, 0, 8, 17));
        addSlot(new TakeOnlySlot(container, 1, 8, 53));
        addSlot(new ValidatedSlot(container, 2, 35, 17));
        addSlot(new TakeOnlySlot(container, 3, 35, 53));
        addSlot(new ValidatedSlot(container, 4, 125, 17));
        addSlot(new TakeOnlySlot(container, 5, 125, 53));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.container instanceof LegacyMachineBlockEntity machine && stillValid(player)) {
            machine.cycleOrbusMode();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size() || !this.slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot source = this.slots.get(index);
        ItemStack stack = source.getItem();
        ItemStack moved = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (this.container.canPlaceItem(2, stack)) {
            if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
        } else if (this.container.canPlaceItem(4, stack)) {
            if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_START + 27) {
            if (!moveItemStackTo(stack, PLAYER_START + 27, this.slots.size(), false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_START + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY); else source.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int fluidId() { return this.data.get(5); }
    public int fluidAmount() { return this.data.get(6); }
    public int mode() { return this.data.get(9); }

    private static Container findMachine(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_orbus")
                ? machine : new SimpleContainer(MACHINE_SLOTS);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }

    private static final class TakeOnlySlot extends ValidatedSlot {
        private TakeOnlySlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
