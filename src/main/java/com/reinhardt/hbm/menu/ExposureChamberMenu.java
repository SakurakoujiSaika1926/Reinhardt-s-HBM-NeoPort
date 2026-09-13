package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ExposureChamberBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
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

public class ExposureChamberMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ExposureChamberBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = 7;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public ExposureChamberMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(ExposureChamberBlockEntity.DATA_COUNT));
    }

    public ExposureChamberMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.EXPOSURE_CHAMBER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ExposureChamberBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, ExposureChamberBlockEntity.PARTICLE_SLOT, 8, 18));
        addSlot(new OutputSlot(container, ExposureChamberBlockEntity.PARTICLE_CONTAINER_OUTPUT_SLOT, 8, 54));
        addSlot(new ValidatedSlot(container, ExposureChamberBlockEntity.INGREDIENT_SLOT, 80, 36));
        addSlot(new OutputSlot(container, ExposureChamberBlockEntity.OUTPUT_SLOT, 116, 36));
        addSlot(new ValidatedSlot(container, ExposureChamberBlockEntity.BATTERY_SLOT, 152, 54));
        addSlot(new ValidatedSlot(container, ExposureChamberBlockEntity.UPGRADE_A_SLOT, 44, 54));
        addSlot(new ValidatedSlot(container, ExposureChamberBlockEntity.UPGRADE_B_SLOT, 62, 54));

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

        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, 5, 7, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 3, false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
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

    public int power() { return data.get(0); }
    public int progress() { return data.get(1); }
    public int processTime() { return Math.max(1, data.get(2)); }
    public int consumption() { return data.get(3); }
    public int savedParticles() { return data.get(4); }
    public boolean on() { return data.get(5) != 0; }
    public int maxPower() { return Math.max(1, data.get(6)); }
    public int maxParticles() { return Math.max(1, data.get(7)); }

    public int powerScaled(int pixels) {
        return Math.min(pixels, power() * pixels / maxPower());
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, progress() * pixels / processTime());
    }

    public int particlesScaled(int pixels) {
        return Math.min(pixels, savedParticles() * pixels / maxParticles());
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

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof ExposureChamberBlockEntity chamber ? chamber : new SimpleContainer(MACHINE_SLOT_COUNT);
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

    private static final class OutputSlot extends LegacyAchievementOutputSlot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
