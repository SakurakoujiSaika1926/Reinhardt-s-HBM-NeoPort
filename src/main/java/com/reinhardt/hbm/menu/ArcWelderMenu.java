package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.blockentity.ArcWelderBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.recipe.ArcWelderRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class ArcWelderMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ArcWelderBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public ArcWelderMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(ArcWelderBlockEntity.DATA_COUNT));
    }

    public ArcWelderMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.ARC_WELDER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ArcWelderBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        addRecipeInputSlots(container);
        this.addSlot(new OutputSlot(container, ArcWelderBlockEntity.OUTPUT_SLOT, 107, 36));
        this.addSlot(new BatterySlot(container, ArcWelderBlockEntity.BATTERY_SLOT, 152, 72));
        this.addSlot(new FluidIdentifierSlot(container, ArcWelderBlockEntity.FLUID_IDENTIFIER_SLOT, 17, 63));
        this.addSlot(new UpgradeSlot(container, ArcWelderBlockEntity.UPGRADE_START, 89, 63));
        this.addSlot(new UpgradeSlot(container, ArcWelderBlockEntity.UPGRADE_START + 1, 107, 63));
        addPlayerInventory(playerInventory, 8, 122);
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
            if (index == ArcWelderBlockEntity.OUTPUT_SLOT) {
                slot.onTake(player, stack);
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, ArcWelderBlockEntity.BATTERY_SLOT, ArcWelderBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, ArcWelderBlockEntity.FLUID_IDENTIFIER_SLOT, ArcWelderBlockEntity.FLUID_IDENTIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, ArcWelderBlockEntity.UPGRADE_START, ArcWelderBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isArcWelderIngredient(stack)) {
            if (!moveItemStackTo(stack, ArcWelderBlockEntity.INPUT_START, ArcWelderBlockEntity.INPUT_END, false)) {
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

    public int maxPower() {
        return Math.max((int) ArcWelderBlockEntity.BASE_MAX_POWER, this.data.get(5));
    }

    public boolean hasRecipe() {
        return this.data.get(6) != 0;
    }

    public int completedCycles() {
        return this.data.get(7);
    }

    public HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(8)).orElse(HbmFluids.none());
    }

    public int tankAmount() {
        return Math.max(0, this.data.get(9));
    }

    public int tankCapacity() {
        return Math.max(1, this.data.get(10));
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, this.energy() * pixels / this.maxPower());
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
    }

    public int fluidScaled(int pixels) {
        return Math.min(pixels, this.tankAmount() * pixels / this.tankCapacity());
    }

    public boolean isWorking() {
        return this.progress() > 0 && this.energy() >= this.consumption();
    }

    private void addRecipeInputSlots(Container container) {
        int slot = ArcWelderBlockEntity.INPUT_START;
        for (int column = 0; column < 3; column++) {
            this.addSlot(new ValidatedSlot(container, slot++, 17 + column * 18, 36));
        }
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

    private boolean isArcWelderIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.container instanceof ArcWelderBlockEntity arcWelder) {
            return arcWelder.canAcceptInput(stack);
        }
        return this.playerInventory.player.level().getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ARC_WELDER.get()).stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> containsIngredient(recipe.inputs(), stack));
    }

    private static boolean containsIngredient(List<ArcWelderRecipe.CountedIngredient> ingredients, ItemStack stack) {
        for (ArcWelderRecipe.CountedIngredient ingredient : ingredients) {
            if (ingredient.ingredient().test(stack)) {
                return true;
            }
        }
        return false;
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ArcWelderBlockEntity ArcWelder) {
            return ArcWelder;
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

    private static final class BatterySlot extends ValidatedSlot {
        private BatterySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class FluidIdentifierSlot extends ValidatedSlot {
        private FluidIdentifierSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class UpgradeSlot extends ValidatedSlot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }
}
