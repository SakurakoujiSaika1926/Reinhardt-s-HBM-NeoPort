package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.SatelliteDockBlockEntity;
import com.reinhardt.hbm.item.SatelliteChipItem;
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

/** Original ContainerSatDock layout: 15 take-only cargo slots and one chip slot. */
public final class SatelliteDockMenu extends AbstractContainerMenu {
    private final Container container;

    public SatelliteDockMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, findDock(playerInventory, buffer.readBlockPos()));
    }

    public SatelliteDockMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.SAT_DOCK.get(), containerId);
        this.container = container;
        checkContainerSize(container, SatelliteDockBlockEntity.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 5; column++) {
                int slot = column + row * 5;
                addSlot(new TakeOnlySlot(container, slot, 62 + column * 18, 17 + row * 18));
            }
        }
        addSlot(new SatelliteSlot(container, SatelliteDockBlockEntity.SLOT_SATELLITE, 26, 35));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index <= SatelliteDockBlockEntity.SLOT_SATELLITE) {
            if (!moveItemStackTo(stack, SatelliteDockBlockEntity.SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SatelliteDockBlockEntity.OUTPUT_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    private static Container findDock(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof SatelliteDockBlockEntity dock ? dock : new SimpleContainer(SatelliteDockBlockEntity.SLOT_COUNT);
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    private static final class SatelliteSlot extends Slot {
        private SatelliteSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof SatelliteChipItem; }
    }
}
