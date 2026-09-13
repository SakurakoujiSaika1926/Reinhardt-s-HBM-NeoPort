package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.MicrowaveBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MicrowaveMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = MicrowaveBlockEntity.SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public MicrowaveMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    private MicrowaveMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        this(containerId, playerInventory, getContainer(playerInventory, blockPos),
                new SimpleContainerData(MicrowaveBlockEntity.DATA_COUNT), blockPos);
    }

    public MicrowaveMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(containerId, playerInventory, container, data, null);
    }

    public MicrowaveMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos suppliedPos) {
        super(HbmMenus.MICROWAVE.get(), containerId);
        checkContainerSize(container, MicrowaveBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, MicrowaveBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;
        this.blockPos = suppliedPos;
        this.addSlot(new InputSlot(container, MicrowaveBlockEntity.INPUT_SLOT, 80, 35));
        this.addSlot(new OutputSlot(container, MicrowaveBlockEntity.OUTPUT_SLOT, 140, 35));
        this.addSlot(new BatterySlot(container, MicrowaveBlockEntity.BATTERY_SLOT, 8, 53));
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
        if (index == MicrowaveBlockEntity.OUTPUT_SLOT || index < MicrowaveBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, MicrowaveBlockEntity.BATTERY_SLOT, MicrowaveBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (hasSmeltingRecipe(stack)) {
            if (!moveItemStackTo(stack, MicrowaveBlockEntity.INPUT_SLOT, MicrowaveBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
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

    public int energy() {
        return this.data.get(0);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int speed() {
        return this.data.get(3);
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / MicrowaveBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / MicrowaveBlockEntity.MAX_TIME);
    }

    public int speedScaled(int pixels) {
        return Math.min(pixels, this.speed() * pixels / MicrowaveBlockEntity.MAX_SPEED);
    }

    private boolean hasSmeltingRecipe(ItemStack stack) {
        return !stack.isEmpty() && this.playerInventory.player.level().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), this.playerInventory.player.level())
                .isPresent();
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
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof MicrowaveBlockEntity microwave ? microwave : new SimpleContainer(MicrowaveBlockEntity.SLOT_COUNT);
    }

    private static class InputSlot extends Slot {
        private InputSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }

    private static class OutputSlot extends LegacyAchievementOutputSlot {
        private OutputSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    private static class BatterySlot extends Slot {
        private BatterySlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }
}
