package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.IronFurnaceBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class IronFurnaceMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = IronFurnaceBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public IronFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(IronFurnaceBlockEntity.DATA_COUNT));
    }

    public IronFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.IRON_FURNACE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, IronFurnaceBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, IronFurnaceBlockEntity.INPUT_SLOT, 53, 17));
        this.addSlot(new ValidatedSlot(container, IronFurnaceBlockEntity.FUEL_A_SLOT, 53, 53));
        this.addSlot(new ValidatedSlot(container, IronFurnaceBlockEntity.FUEL_B_SLOT, 71, 53));
        this.addSlot(new OutputSlot(container, IronFurnaceBlockEntity.OUTPUT_SLOT, 125, 35));
        this.addSlot(new ValidatedSlot(container, IronFurnaceBlockEntity.UPGRADE_SLOT, 17, 35));
        addPlayerInventory(playerInventory, 8, 84);
        addDataSlots(data);
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

        if (index == IronFurnaceBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (IronFurnaceBlockEntity.isSpeedUpgrade(stack)) {
            if (!moveItemStackTo(stack, IronFurnaceBlockEntity.UPGRADE_SLOT, IronFurnaceBlockEntity.UPGRADE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (IronFurnaceBlockEntity.fuelDuration(stack) > 0) {
            if (!moveItemStackTo(stack, IronFurnaceBlockEntity.FUEL_A_SLOT, IronFurnaceBlockEntity.FUEL_B_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (IronFurnaceBlockEntity.hasSmeltingRecipe(level(), stack)) {
            if (!moveItemStackTo(stack, IronFurnaceBlockEntity.INPUT_SLOT, IronFurnaceBlockEntity.INPUT_SLOT + 1, false)) {
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

    public int maxBurnTime() {
        return Math.max(1, this.data.get(0));
    }

    public int burnTime() {
        return this.data.get(1);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int processingTime() {
        return Math.max(1, this.data.get(3));
    }

    public boolean wasOn() {
        return this.data.get(4) != 0;
    }

    public boolean canSmelt() {
        return this.data.get(5) != 0;
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / processingTime());
    }

    public int burnScaled(int pixels) {
        return Math.min(pixels, this.burnTime() * pixels / maxBurnTime());
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

    private Level level() {
        return this.playerInventory.player.level();
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof IronFurnaceBlockEntity furnace) {
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
