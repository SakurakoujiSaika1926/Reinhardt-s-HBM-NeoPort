package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.PurexBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.recipe.PurexRecipe;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Optional;

public class PurexMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = PurexBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public PurexMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(PurexBlockEntity.DATA_COUNT));
    }

    public PurexMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.PUREX.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, PurexBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, PurexBlockEntity.BATTERY_SLOT, 152, 81));
        addSlot(new ValidatedSlot(container, PurexBlockEntity.BLUEPRINT_SLOT, 35, 126));
        addSlot(new ValidatedSlot(container, PurexBlockEntity.UPGRADE_START, 152, 108));
        addSlot(new ValidatedSlot(container, PurexBlockEntity.UPGRADE_START + 1, 152, 126));
        for (int index = 0; index < 3; index++) {
            addSlot(new ValidatedSlot(container, PurexBlockEntity.INPUT_START + index, 8, 90 + index * 18));
        }
        for (int index = 0; index < 6; index++) {
            addSlot(new OutputSlot(container, PurexBlockEntity.OUTPUT_START + index, 80 + (index % 3) * 18, 36 + (index / 3) * 18));
        }

        addPlayerInventory(playerInventory, 8, 174);
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

        if (index >= PurexBlockEntity.OUTPUT_START && index < PurexBlockEntity.OUTPUT_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, PurexBlockEntity.BATTERY_SLOT, PurexBlockEntity.BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (BlueprintItem.isBlueprint(stack)) {
            if (!moveItemStackTo(stack, PurexBlockEntity.BLUEPRINT_SLOT, PurexBlockEntity.BLUEPRINT_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, PurexBlockEntity.UPGRADE_START, PurexBlockEntity.UPGRADE_END, false)) return ItemStack.EMPTY;
        } else if (hasRecipe(stack)) {
            if (!moveItemStackTo(stack, PurexBlockEntity.INPUT_START, PurexBlockEntity.INPUT_END, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof PurexBlockEntity purex) || !stillValid(player)) {
            return false;
        }
        if (id <= 0) {
            purex.setSelectedRecipe(null);
            return true;
        }
        List<RecipeHolder<PurexRecipe>> recipes = purexRecipes();
        int recipeIndex = id - 1;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return false;
        }
        purex.setSelectedRecipe(recipes.get(recipeIndex).id());
        return true;
    }

    public int energy() { return data.get(0); }
    public int lastInput() { return data.get(1); }
    public int progress() { return data.get(2); }
    public int workTime() { return Math.max(1, data.get(3)); }
    public int demand() { return data.get(4); }
    public int capacity() { return Math.max(1, data.get(5)); }
    public boolean working() { return data.get(6) != 0; }
    public boolean hasRecipe() { return data.get(7) != 0; }
    public int selectedRecipeIndex() { return data.get(8) - 1; }
    public int tankFluidId(int tank) { return data.get(9 + tank * 3); }
    public int tankAmount(int tank) { return data.get(10 + tank * 3); }
    public int tankPressure(int tank) { return data.get(11 + tank * 3); }

    public int energyScaled(int pixels) {
        return Math.min(pixels, energy() * pixels / capacity());
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, progress() * pixels / workTime());
    }

    public int tankScaled(int tank, int pixels) {
        return Math.min(pixels, tankAmount(tank) * pixels / PurexBlockEntity.TANK_CAPACITY);
    }

    public List<RecipeHolder<PurexRecipe>> purexRecipes() {
        return this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.PUREX.get()).stream()
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    public Optional<RecipeHolder<PurexRecipe>> selectedRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<PurexRecipe>> recipes = purexRecipes();
        return index >= 0 && index < recipes.size() ? Optional.of(recipes.get(index)) : Optional.empty();
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return selectedRecipe().map(RecipeHolder::id);
    }

    public int buttonIdForRecipe(ResourceLocation recipeId) {
        List<RecipeHolder<PurexRecipe>> recipes = purexRecipes();
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipeId)) {
                return index + 1;
            }
        }
        return 0;
    }

    private boolean hasRecipe(ItemStack stack) {
        return !stack.isEmpty() && this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.PUREX.get())
                .stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.inputItems().stream().anyMatch(input -> input.ingredient().test(stack)));
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
        return blockEntity instanceof PurexBlockEntity purex ? purex : new SimpleContainer(MACHINE_SLOT_COUNT);
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
