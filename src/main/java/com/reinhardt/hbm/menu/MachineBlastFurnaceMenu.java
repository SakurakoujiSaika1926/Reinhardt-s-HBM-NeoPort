package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.MachineBlastFurnaceBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineBlastFurnaceMenu extends AbstractContainerMenu {
    // The 1.7.10 GUI exposes only the five furnace slots. Container transfer remains automation-only.
    private static final int MACHINE_SLOT_COUNT = 5;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public MachineBlastFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(MachineBlastFurnaceBlockEntity.DATA_COUNT));
    }

    public MachineBlastFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.MACHINE_BLAST_FURNACE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MachineBlastFurnaceBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, MachineBlastFurnaceBlockEntity.FUEL_SLOT, 80, 81));
        this.addSlot(new ValidatedSlot(container, MachineBlastFurnaceBlockEntity.UPPER_INPUT_SLOT, 80, 27));
        this.addSlot(new ValidatedSlot(container, MachineBlastFurnaceBlockEntity.LOWER_INPUT_SLOT, 80, 45));
        this.addSlot(new OutputSlot(container, MachineBlastFurnaceBlockEntity.OUTPUT_SLOT, 134, 72));
        this.addSlot(new OutputSlot(container, MachineBlastFurnaceBlockEntity.BYPRODUCT_SLOT, 134, 90));
        addPlayerInventory(playerInventory, 8, 140);
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

        if (index == MachineBlastFurnaceBlockEntity.OUTPUT_SLOT
                || index == MachineBlastFurnaceBlockEntity.BYPRODUCT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isFuel(stack)) {
            if (!moveItemStackTo(stack, MachineBlastFurnaceBlockEntity.FUEL_SLOT, MachineBlastFurnaceBlockEntity.FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isRecipeInput(stack)) {
            if (!moveItemStackTo(stack, MachineBlastFurnaceBlockEntity.UPPER_INPUT_SLOT, MachineBlastFurnaceBlockEntity.LOWER_INPUT_SLOT + 1, false)) {
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

    public int fuel() {
        return this.data.get(0);
    }

    public int fuelScaled(int pixels) {
        return Math.min(pixels, this.fuel() * pixels / MachineBlastFurnaceBlockEntity.MAX_FUEL);
    }

    public float progressFraction() {
        return this.data.get(1) / 1000.0F;
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, Math.round(progressFraction() * pixels));
    }

    public int speedPercent() {
        return this.data.get(2);
    }

    public boolean isProcessing() {
        return this.data.get(3) != 0;
    }

    public HbmFluidDefinition airblastFluid() {
        return HbmFluids.byOldId(this.data.get(4)).orElse(HbmFluids.none());
    }

    public int airblastAmount() {
        return this.data.get(5);
    }

    public int airblastCapacity() {
        return MachineBlastFurnaceBlockEntity.TANK_CAPACITY_AIRBLAST;
    }

    public HbmFluidDefinition flueFluid() {
        return HbmFluids.byOldId(this.data.get(6)).orElse(HbmFluids.none());
    }

    public int flueAmount() {
        return this.data.get(7);
    }

    public int flueCapacity() {
        return Math.max(1, this.data.get(8));
    }

    public boolean hasExtension() {
        return this.data.get(9) != 0;
    }

    public boolean hasFuel() {
        return this.fuel() > 0;
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

    private boolean isFuel(ItemStack stack) {
        return MachineBlastFurnaceBlockEntity.fuelPower(level(), stack) > 0;
    }

    private boolean isRecipeInput(ItemStack stack) {
        return MachineBlastFurnaceBlockEntity.canAcceptRecipeInput(level(), stack);
    }

    private Level level() {
        return this.playerInventory.player.level();
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineBlastFurnaceBlockEntity furnace) {
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
