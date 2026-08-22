package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.FrackingTowerBlockEntity;
import com.reinhardt.hbm.registry.HbmFluids;
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

public class FrackingTowerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = FrackingTowerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public FrackingTowerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(FrackingTowerBlockEntity.DATA_COUNT));
    }

    public FrackingTowerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.FRACKING_TOWER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, FrackingTowerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, FrackingTowerBlockEntity.BATTERY_SLOT, 8, 53));
        this.addSlot(new ValidatedSlot(container, FrackingTowerBlockEntity.OIL_INPUT_SLOT, 80, 17));
        this.addSlot(new TakeOnlySlot(container, FrackingTowerBlockEntity.OIL_OUTPUT_SLOT, 80, 53));
        this.addSlot(new ValidatedSlot(container, FrackingTowerBlockEntity.GAS_INPUT_SLOT, 125, 17));
        this.addSlot(new TakeOnlySlot(container, FrackingTowerBlockEntity.GAS_OUTPUT_SLOT, 125, 53));
        this.addSlot(new ValidatedSlot(container, FrackingTowerBlockEntity.UPGRADE_START, 152, 17));
        this.addSlot(new ValidatedSlot(container, FrackingTowerBlockEntity.UPGRADE_START + 1, 152, 35));
        this.addSlot(new ValidatedSlot(container, FrackingTowerBlockEntity.UPGRADE_START + 2, 152, 53));
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(FrackingTowerBlockEntity.BATTERY_SLOT, stack)) {
            if (!moveItemStackTo(stack, FrackingTowerBlockEntity.BATTERY_SLOT, FrackingTowerBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(FrackingTowerBlockEntity.OIL_INPUT_SLOT, stack)) {
            if (!moveItemStackTo(stack, FrackingTowerBlockEntity.OIL_INPUT_SLOT, FrackingTowerBlockEntity.OIL_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(FrackingTowerBlockEntity.GAS_INPUT_SLOT, stack)) {
            if (!moveItemStackTo(stack, FrackingTowerBlockEntity.GAS_INPUT_SLOT, FrackingTowerBlockEntity.GAS_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (moveUpgrade(stack)) {
            return moved;
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

    public com.reinhardt.hbm.fluid.HbmFluidDefinition oilFluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int oilAmount() {
        return this.data.get(1);
    }

    public int oilCapacity() {
        return Math.max(1, this.data.get(2));
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition gasFluid() {
        return HbmFluids.byOldId(this.data.get(3)).orElse(HbmFluids.none());
    }

    public int gasAmount() {
        return this.data.get(4);
    }

    public int gasCapacity() {
        return Math.max(1, this.data.get(5));
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition fracksolFluid() {
        return HbmFluids.byOldId(this.data.get(6)).orElse(HbmFluids.none());
    }

    public int fracksolAmount() {
        return this.data.get(7);
    }

    public int fracksolCapacity() {
        return Math.max(1, this.data.get(8));
    }

    public int power() {
        return this.data.get(9);
    }

    public int indicator() {
        return this.data.get(10);
    }

    public int maxPower() {
        return Math.max(1, this.data.get(11));
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / maxPower()));
    }

    public int oilScaled(int pixels) {
        return Math.min(pixels, this.oilAmount() * pixels / Math.max(1, oilCapacity()));
    }

    public int gasScaled(int pixels) {
        return Math.min(pixels, this.gasAmount() * pixels / Math.max(1, gasCapacity()));
    }

    public int fracksolScaled(int pixels) {
        return Math.min(pixels, this.fracksolAmount() * pixels / Math.max(1, fracksolCapacity()));
    }

    private boolean moveUpgrade(ItemStack stack) {
        for (int slot = FrackingTowerBlockEntity.UPGRADE_START; slot < FrackingTowerBlockEntity.UPGRADE_END; slot++) {
            if (!this.container.canPlaceItem(slot, stack)) {
                continue;
            }
            if (moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return false;
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
        if (blockEntity instanceof FrackingTowerBlockEntity tower) {
            return tower;
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

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
