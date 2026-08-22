package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.RtgPelletItem;
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

public final class RadiolysisMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 15;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int HOTBAR_START = PLAYER_START + 27;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public RadiolysisMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, getContainer(inventory, buffer.readBlockPos()),
                new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public RadiolysisMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private RadiolysisMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.RADIOLYSIS.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof BlockEntity be ? be.getBlockPos().immutable() : BlockPos.ZERO;

        // Ten RTG positions, matching ContainerRadiolysis from 1.7.10.
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 5; row++) {
                addSlot(new ValidatedSlot(container, row + column * 5, 188 + column * 18, 8 + row * 18));
            }
        }
        addSlot(new ValidatedSlot(container, 10, 34, 17));
        addSlot(new TakeOnlySlot(container, 11, 34, 53));
        addSlot(new ValidatedSlot(container, 12, 148, 17));
        addSlot(new TakeOnlySlot(container, 13, 148, 53));
        addSlot(new ValidatedSlot(container, 14, 8, 53));
        addPlayerInventory(inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = this.slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack moved = source.getItem().copy();
        ItemStack stack = source.getItem();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean placed = false;
            if (stack.getItem() instanceof RtgPelletItem) placed = moveItemStackTo(stack, 0, 10, false);
            else if (stack.getItem() instanceof FluidIdentifierItem) placed = moveItemStackTo(stack, 10, 11, false);
            else if (container.canPlaceItem(12, stack)) placed = moveItemStackTo(stack, 12, 13, false);
            if (!placed && BatteryPackItem.isBattery(stack)) placed = moveItemStackTo(stack, 14, 15, false);
            if (!placed) return ItemStack.EMPTY;
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
    public int heat() { return this.data.get(4); }
    public int energyCapacity() { return 1_000_000; }
    public int fluidId(int tank) { return this.data.get(5 + tank * 2); }
    public int fluidAmount(int tank) { return this.data.get(6 + tank * 2); }
    public int fluidCapacity(int tank) { return tank == 0 ? 2_000 : 2_000; }
    public com.reinhardt.hbm.fluid.HbmFluidDefinition fluid(int tank) {
        return HbmFluids.byOldId(fluidId(tank)).orElse(HbmFluids.none());
    }
    public BlockPos blockPos() { return this.blockPos; }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_radiolysis")
                ? machine : new SimpleContainer(MACHINE_SLOTS);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }

    private static final class TakeOnlySlot extends ValidatedSlot {
        private TakeOnlySlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
