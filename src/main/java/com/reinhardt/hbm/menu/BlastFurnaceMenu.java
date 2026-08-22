package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.BlastFurnaceBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BlastFurnaceMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = BlastFurnaceBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public BlastFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(BlastFurnaceBlockEntity.DATA_COUNT));
    }

    public BlastFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.BLAST_FURNACE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, BlastFurnaceBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, BlastFurnaceBlockEntity.UPPER_INPUT_SLOT, 80, 18));
        this.addSlot(new ValidatedSlot(container, BlastFurnaceBlockEntity.LOWER_INPUT_SLOT, 80, 54));
        this.addSlot(new ValidatedSlot(container, BlastFurnaceBlockEntity.FUEL_SLOT, 8, 36));
        this.addSlot(new OutputSlot(container, BlastFurnaceBlockEntity.OUTPUT_SLOT, 134, 36));
        addPlayerInventory(playerInventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 3 && button == 1 && clickType == ClickType.PICKUP && getCarried().isEmpty()) {
            Slot slot = this.slots.get(slotId);
            if (!slot.hasItem() && this.container instanceof BlastFurnaceBlockEntity furnace) {
                furnace.cycleSideMode(slotId);
                broadcastChanges();
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            if (index == BlastFurnaceBlockEntity.OUTPUT_SLOT) {
                slot.onTake(player, stack);
            }
        } else if (!moveItemStackTo(stack, 0, 3, false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
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

    public int fuel() {
        return this.data.get(0);
    }

    public int fuelScaled(int pixels) {
        return Math.min(pixels, this.fuel() * pixels / BlastFurnaceBlockEntity.MAX_FUEL);
    }

    public int progress() {
        return this.data.get(1);
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / BlastFurnaceBlockEntity.PROCESSING_SPEED);
    }

    public boolean canProcess() {
        return this.data.get(2) != 0;
    }

    public boolean hasFuel() {
        return this.fuel() > 0;
    }

    public int sideMode(int slot) {
        return switch (slot) {
            case BlastFurnaceBlockEntity.UPPER_INPUT_SLOT -> this.data.get(3);
            case BlastFurnaceBlockEntity.LOWER_INPUT_SLOT -> this.data.get(4);
            case BlastFurnaceBlockEntity.FUEL_SLOT -> this.data.get(5);
            default -> 0;
        };
    }

    public static String legacyDirectionName(int mode) {
        return switch (mode) {
            case 0 -> "DOWN";
            case 1 -> "UP";
            case 2 -> "NORTH";
            case 3 -> "SOUTH";
            case 4 -> "WEST";
            case 5 -> "EAST";
            default -> "UNKNOWN";
        };
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

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof BlastFurnaceBlockEntity furnace) {
            return furnace;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
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
