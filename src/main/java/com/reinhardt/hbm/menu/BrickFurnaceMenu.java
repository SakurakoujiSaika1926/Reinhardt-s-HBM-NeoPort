package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.BrickFurnaceBlockEntity;
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

public final class BrickFurnaceMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = BrickFurnaceBlockEntity.SLOT_COUNT;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private final Container container;
    private final ContainerData data;

    public BrickFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(BrickFurnaceBlockEntity.DATA_COUNT));
    }

    public BrickFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.BRICK_FURNACE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, BrickFurnaceBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        addSlot(new InputSlot(container, BrickFurnaceBlockEntity.INPUT_SLOT, 62, 35));
        addSlot(new FuelSlot(container, BrickFurnaceBlockEntity.FUEL_SLOT, 35, 17));
        addSlot(new OutputSlot(container, BrickFurnaceBlockEntity.OUTPUT_SLOT, 116, 35));
        addSlot(new OutputSlot(container, BrickFurnaceBlockEntity.ASH_SLOT, 35, 53));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (BrickFurnaceBlockEntity.fuelDuration(stack) > 0) {
            // 1.7.10 tries the fuel slot first, then falls back to the input slot
            // when the fuel slot cannot accept the stack.
            if (!moveItemStackTo(stack, BrickFurnaceBlockEntity.FUEL_SLOT, BrickFurnaceBlockEntity.FUEL_SLOT + 1, false)
                    && !moveItemStackTo(stack, BrickFurnaceBlockEntity.INPUT_SLOT, BrickFurnaceBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, BrickFurnaceBlockEntity.INPUT_SLOT, BrickFurnaceBlockEntity.INPUT_SLOT + 1, false)) {
            if (index < PLAYER_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    public int maxBurnTime() { return Math.max(1, data.get(0)); }
    public int burnTime() { return data.get(1); }
    public int progress() { return data.get(2); }
    public boolean working() { return data.get(3) != 0; }
    public int processTime() { return Math.max(1, data.get(4)); }
    public int burnScaled(int pixels) { return Math.min(pixels, burnTime() * pixels / maxBurnTime()); }
    public int progressScaled(int pixels) { return Math.min(pixels, progress() * pixels / processTime()); }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof BrickFurnaceBlockEntity furnace ? furnace : new SimpleContainer(MACHINE_SLOTS);
    }

    private static final class InputSlot extends Slot {
        private InputSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return true; }
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return BrickFurnaceBlockEntity.fuelDuration(stack) > 0; }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
