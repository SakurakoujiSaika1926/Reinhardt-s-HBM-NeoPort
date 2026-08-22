package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ReactorControlBlockEntity;
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

/** Direct port of ContainerReactorControl's one sensor slot and 176x166 layout. */
public final class ReactorControlMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = ReactorControlBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public ReactorControlMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, context(playerInventory, buffer.readBlockPos()));
    }

    private ReactorControlMenu(int containerId, Inventory playerInventory, Context context) {
        this(containerId, playerInventory, context.container(), new SimpleContainerData(ReactorControlBlockEntity.DATA_COUNT), context.blockPos());
    }

    public ReactorControlMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos blockPos) {
        super(HbmMenus.REACTOR_CONTROL.get(), containerId);
        checkContainerSize(container, ReactorControlBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, ReactorControlBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos.immutable();

        addSlot(new SensorSlot(container, ReactorControlBlockEntity.SLOT_SENSOR, 92, 38));
        addPlayerInventory(playerInventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < ReactorControlBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(ReactorControlBlockEntity.SLOT_SENSOR, stack)) {
            if (!moveItemStackTo(stack, ReactorControlBlockEntity.SLOT_SENSOR, ReactorControlBlockEntity.SLOT_SENSOR + 1, false)) {
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

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public int heat() {
        return this.data.get(0);
    }

    public int flux() {
        return this.data.get(1);
    }

    public int levelPercent() {
        return Math.round(this.data.get(2) * 100.0F / 10_000.0F);
    }

    public boolean linked() {
        return this.data.get(3) != 0;
    }

    public int levelUpper() {
        return this.data.get(4);
    }

    public int levelLower() {
        return this.data.get(5);
    }

    public int heatUpper() {
        return this.data.get(6);
    }

    public int heatLower() {
        return this.data.get(7);
    }

    public int function() {
        return this.data.get(8);
    }

    public int temperature() {
        return this.linked() ? com.reinhardt.hbm.blockentity.ResearchReactorBlockEntity.temperatureForHeat(this.heat()) : 0;
    }

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

    private static Context context(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ReactorControlBlockEntity controller) {
            return new Context(controller, pos);
        }
        return new Context(new SimpleContainer(ReactorControlBlockEntity.SLOT_COUNT), pos);
    }

    private record Context(Container container, BlockPos blockPos) {
    }

    private static final class SensorSlot extends Slot {
        private SensorSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }
}
