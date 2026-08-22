package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.RadarLinkerItem;
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

/** Exact ten-slot layout from ContainerMachineRadarNT. */
public final class RadarSlotsMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 10;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public RadarSlotsMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, findMachine(inventory, buffer.readBlockPos()), new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public RadarSlotsMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private RadarSlotsMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine, ContainerData data) {
        super(HbmMenus.RADAR_SLOTS.get(), containerId);
        this.container = machine == null ? new SimpleContainer(MACHINE_SLOTS) : machine;
        this.data = data;
        this.blockPos = machine == null ? BlockPos.ZERO : machine.getBlockPos().immutable();
        checkContainerSize(this.container, MACHINE_SLOTS);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        for (int index = 0; index < 8; index++) addSlot(new ValidatedSlot(this.container, index, 26 + index * 18, 17));
        addSlot(new ValidatedSlot(this.container, 8, 26, 44));
        addSlot(new ValidatedSlot(this.container, 9, 152, 44));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size() || !this.slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot source = this.slots.get(index);
        ItemStack stack = source.getItem();
        ItemStack moved = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, 9, 10, false)) return ItemStack.EMPTY;
        } else if (stack.is(HbmItems.SAT_RELAY.get())) {
            if (!moveItemStackTo(stack, 0, 8, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof RadarLinkerItem) {
            if (!moveItemStackTo(stack, 8, 9, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY); else source.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) { return RadarMenu.isBoundRadar(player, this.blockPos, this.container); }

    public BlockPos blockPos() { return this.blockPos; }
    public long power() { return Integer.toUnsignedLong(this.data.get(0)); }
    public long powerCapacity() { return this.container instanceof LegacyMachineBlockEntity machine ? machine.energyCapacity() : 100_000L; }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 161));
    }

    private static LegacyMachineBlockEntity findMachine(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine
                && (machine.machineId().equals("machine_radar") || machine.machineId().equals("machine_radar_large"))
                ? machine : null;
    }

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }
}
