package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.CentrifugeBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
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

public class CentrifugeMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = CentrifugeBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public CentrifugeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(CentrifugeBlockEntity.DATA_COUNT));
    }

    public CentrifugeMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.CENTRIFUGE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CentrifugeBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, CentrifugeBlockEntity.INPUT_SLOT, 36, 50));
        this.addSlot(new ValidatedSlot(container, CentrifugeBlockEntity.BATTERY_SLOT, 9, 50));
        for (int slot = CentrifugeBlockEntity.OUTPUT_START; slot < CentrifugeBlockEntity.OUTPUT_END; slot++) {
            this.addSlot(new OutputSlot(container, slot, 63 + (slot - CentrifugeBlockEntity.OUTPUT_START) * 20, 50));
        }
        this.addSlot(new UpgradeSlot(container, CentrifugeBlockEntity.UPGRADE_START, 149, 22));
        this.addSlot(new UpgradeSlot(container, CentrifugeBlockEntity.UPGRADE_START + 1, 149, 40));
        addPlayerInventory(playerInventory, 8, 104);
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

        if (index >= CentrifugeBlockEntity.OUTPUT_START && index < CentrifugeBlockEntity.OUTPUT_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, CentrifugeBlockEntity.BATTERY_SLOT, CentrifugeBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, CentrifugeBlockEntity.UPGRADE_START, CentrifugeBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (hasRecipe(stack)) {
            if (!moveItemStackTo(stack, CentrifugeBlockEntity.INPUT_SLOT, CentrifugeBlockEntity.INPUT_SLOT + 1, false)) {
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

    public int energy() {
        return this.data.get(0);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int processTime() {
        return Math.max(1, this.data.get(3));
    }

    public int consumption() {
        return Math.max(1, this.data.get(4));
    }

    public int speed() {
        return Math.max(1, this.data.get(5));
    }

    public int completedCycles() {
        return this.data.get(6);
    }

    public boolean isWorking() {
        return this.data.get(7) != 0;
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / CentrifugeBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
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

    private boolean hasRecipe(ItemStack stack) {
        return !stack.isEmpty() && this.playerInventory.player.level().getRecipeManager()
                .getRecipeFor(HbmRecipeTypes.CENTRIFUGE.get(), new com.reinhardt.hbm.recipe.CentrifugeRecipe.Input(stack), this.playerInventory.player.level())
                .isPresent();
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CentrifugeBlockEntity centrifuge) {
            return centrifuge;
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

    private static final class UpgradeSlot extends ValidatedSlot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
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
