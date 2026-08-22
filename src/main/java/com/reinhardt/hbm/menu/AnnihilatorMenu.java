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

/** Exact dedicated menu for the 1.7.10 annihilator. */
public final class AnnihilatorMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public AnnihilatorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, getContainer(inventory, buffer.readBlockPos()),
                new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public AnnihilatorMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private AnnihilatorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.ANNIHILATOR.get(), containerId);
        checkContainerSize(container, 11);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof BlockEntity entity ? entity.getBlockPos().immutable() : BlockPos.ZERO;

        addSlot(new ValidatedSlot(container, 0, 17, 45));
        addSlot(new ValidatedSlot(container, 1, 35, 45));
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                addSlot(new ValidatedSlot(container, 2 + column * 3 + row, 80 + column * 18, 36 + row * 18));
            }
        }
        addSlot(new ValidatedSlot(container, 8, 152, 18));
        addSlot(new ValidatedSlot(container, 9, 152, 62));
        addSlot(new ValidatedSlot(container, 10, 152, 80));
        addPlayerInventory(inventory, 8, 126);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = this.slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = source.getItem();
        ItemStack moved = stack.copy();
        if (index < 11) {
            if (!moveItemStackTo(stack, 11, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) { return this.container.stillValid(player); }

    public int energy() { return this.data.get(0); }
    public int energyCapacity() { return this.container instanceof LegacyMachineBlockEntity machine ? (int) machine.energyCapacity() : 0; }
    public BlockPos blockPos() { return this.blockPos; }
    public boolean isAnnihilator() { return true; }
    public String annihilatorPool() { return this.container instanceof LegacyMachineBlockEntity machine ? machine.annihilatorPool() : "Recycling"; }
    public java.math.BigInteger annihilatorMonitor() { return this.container instanceof LegacyMachineBlockEntity machine ? machine.annihilatorMonitor() : java.math.BigInteger.ZERO; }
    public ItemStack annihilatorMonitorStack() { return this.container.getContainerSize() > 8 ? this.container.getItem(8) : ItemStack.EMPTY; }

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
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_annihilator")
                ? machine : new SimpleContainer(11);
    }

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }
}
