package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.FunnelBlockEntity;
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

public class FunnelMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = FunnelBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public FunnelMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    private FunnelMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        this(containerId, playerInventory, getContainer(playerInventory, blockPos),
                new SimpleContainerData(FunnelBlockEntity.DATA_COUNT), blockPos);
    }

    private FunnelMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos blockPos) {
        super(HbmMenus.FUNNEL.get(), containerId);
        checkContainerSize(container, FunnelBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, FunnelBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos.immutable();

        for (int slot = FunnelBlockEntity.INPUT_START; slot < FunnelBlockEntity.INPUT_END; slot++) {
            this.addSlot(new InputSlot(container, slot, 8 + slot * 18, 18));
        }
        for (int slot = FunnelBlockEntity.OUTPUT_START; slot < FunnelBlockEntity.OUTPUT_END; slot++) {
            this.addSlot(new OutputSlot(container, slot, 8 + (slot - FunnelBlockEntity.OUTPUT_START) * 18, 54));
        }
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public FunnelMenu(int containerId, Inventory playerInventory, FunnelBlockEntity funnel) {
        this(containerId, playerInventory, funnel, funnel.menuData(), funnel.getBlockPos());
    }

    public int mode() {
        return this.data.get(0);
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < FunnelBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(FunnelBlockEntity.INPUT_START, stack)) {
            if (!moveItemStackTo(stack, FunnelBlockEntity.INPUT_START, FunnelBlockEntity.INPUT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 86 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 144));
        }
    }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof FunnelBlockEntity funnel ? funnel : new SimpleContainer(FunnelBlockEntity.SLOT_COUNT);
    }

    private static final class InputSlot extends Slot {
        private InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
