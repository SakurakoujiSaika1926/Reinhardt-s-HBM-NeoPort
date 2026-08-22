package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.SirenBlockEntity;
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

public final class SirenMenu extends AbstractContainerMenu {
    private final Container container;
    private final BlockPos blockPos;

    public SirenMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    private SirenMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, getContainer(playerInventory, pos), pos);
    }

    public SirenMenu(int containerId, Inventory playerInventory, SirenBlockEntity siren) {
        this(containerId, playerInventory, siren, siren.getBlockPos());
    }

    private SirenMenu(int containerId, Inventory playerInventory, Container container, BlockPos pos) {
        super(HbmMenus.SIREN.get(), containerId);
        checkContainerSize(container, 1);
        this.container = container;
        this.blockPos = pos.immutable();
        this.addSlot(new TrackSlot(container, 0, 8, 35));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    public BlockPos blockPos() { return this.blockPos; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return moved;
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) { return this.container.stillValid(player); }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof SirenBlockEntity siren ? siren : new SimpleContainer(1);
    }

    private static final class TrackSlot extends Slot {
        private TrackSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override
        public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }
}
