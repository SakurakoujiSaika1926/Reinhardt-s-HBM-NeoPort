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

/** Direct 1.7.10 ContainerMachineRTG layout: fifteen pellet slots. */
public final class RtgMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 15;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public RtgMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, getContainer(inventory, buffer.readBlockPos()),
                new net.minecraft.world.inventory.SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public RtgMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private RtgMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.RTG_GREY.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof BlockEntity entity ? entity.getBlockPos().immutable() : BlockPos.ZERO;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 5; column++) {
                addSlot(new ValidatedSlot(container, column + row * 5, 16 + column * 18, 18 + row * 18));
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
        } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) { return this.container.stillValid(player); }

    public int energy() { return this.data.get(0); }
    public int energyCapacity() { return 100_000; }
    public int heat() { return this.data.get(4); }
    public BlockPos blockPos() { return this.blockPos; }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 106 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 164));
        }
    }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_rtg_grey")
                ? machine : new SimpleContainer(MACHINE_SLOTS);
    }

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }
}
