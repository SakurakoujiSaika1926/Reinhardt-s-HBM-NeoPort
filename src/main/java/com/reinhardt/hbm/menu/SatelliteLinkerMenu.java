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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Direct port of the three manually operated slots in ContainerMachineSatLinker. */
public final class SatelliteLinkerMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 3;
    private static final int PLAYER_START = SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 36;
    private final Container container;
    private final BlockPos blockPos;

    public SatelliteLinkerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, findMachine(inventory, buffer.readBlockPos()));
    }

    public SatelliteLinkerMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        super(HbmMenus.SATELLITE_LINKER.get(), containerId);
        this.container = machine == null ? new SimpleContainer(SLOT_COUNT) : machine;
        checkContainerSize(this.container, SLOT_COUNT);
        this.blockPos = machine == null ? BlockPos.ZERO : machine.getBlockPos().immutable();
        this.addSlot(new ManualSlot(this.container, 0, 44, 35));
        this.addSlot(new ManualSlot(this.container, 1, 80, 35));
        this.addSlot(new ManualSlot(this.container, 2, 116, 35));
        addPlayerInventory(inventory, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size() || !this.slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        // ContainerMachineSatLinker only shift-transferred the copy-input slot.
        if (index == 0) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (index >= PLAYER_START) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public BlockPos blockPos() { return this.blockPos; }

    private static LegacyMachineBlockEntity findMachine(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine ? machine : null;
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

    private static final class ManualSlot extends Slot {
        private ManualSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }
}
