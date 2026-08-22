package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.AmmoPressBlockEntity;
import com.reinhardt.hbm.recipe.AmmoPressRecipe;
import com.reinhardt.hbm.registry.HbmMenus;
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

public class AmmoPressMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = AmmoPressBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public AmmoPressMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(AmmoPressBlockEntity.DATA_COUNT));
    }

    public AmmoPressMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.AMMO_PRESS.get(), containerId);
        checkContainerSize(container, AmmoPressBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, AmmoPressBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = row * 3 + column;
                this.addSlot(new InputSlot(container, slot, 116 + column * 18, 18 + row * 18));
            }
        }
        this.addSlot(new OutputSlot(container, AmmoPressBlockEntity.OUTPUT_SLOT, 134, 72));
        addPlayerInventory(playerInventory, 8, 118);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == AmmoPressBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index < AmmoPressBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveIntoMatchingInput(stack)) {
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
        return original;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof AmmoPressBlockEntity ammoPress) || !stillValid(player)) {
            return false;
        }
        if (id <= 0) {
            ammoPress.setSelectedRecipe(null);
            return true;
        }

        List<RecipeHolder<AmmoPressRecipe>> recipes = ammoPress.recipes(player.level());
        int recipeIndex = id - 1;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return false;
        }
        ammoPress.setSelectedRecipe(recipes.get(recipeIndex).id());
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int selectedRecipeIndex() {
        return this.data.get(0) - 1;
    }

    public Optional<RecipeHolder<AmmoPressRecipe>> selectedRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<AmmoPressRecipe>> recipes = recipes();
        return index >= 0 && index < recipes.size() ? Optional.of(recipes.get(index)) : Optional.empty();
    }

    public List<RecipeHolder<AmmoPressRecipe>> recipes() {
        if (this.container instanceof AmmoPressBlockEntity ammoPress) {
            return ammoPress.recipes(this.playerInventory.player.level());
        }
        return this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(com.reinhardt.hbm.registry.HbmRecipeTypes.AMMO_PRESS.get()).stream()
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    public int buttonIdForRecipe(ResourceLocation id) {
        List<RecipeHolder<AmmoPressRecipe>> recipes = recipes();
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(id)) {
                return index + 1;
            }
        }
        return 0;
    }

    private boolean moveIntoMatchingInput(ItemStack stack) {
        if (!(this.container instanceof AmmoPressBlockEntity ammoPress)) {
            return false;
        }
        Optional<RecipeHolder<AmmoPressRecipe>> selected = selectedRecipe();
        if (selected.isEmpty()) {
            return false;
        }

        for (int slot = AmmoPressBlockEntity.INPUT_START; slot < AmmoPressBlockEntity.INPUT_END; slot++) {
            AmmoPressRecipe.SlotIngredient required = selected.get().value().input().get(slot);
            if (!required.isEmpty() && required.ingredient().orElseThrow().test(stack)
                    && moveItemStackTo(stack, slot, slot + 1, false)) {
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

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof AmmoPressBlockEntity ammoPress
                ? ammoPress
                : new SimpleContainer(AmmoPressBlockEntity.SLOT_COUNT);
    }

    private final class InputSlot extends Slot {
        private InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            Optional<RecipeHolder<AmmoPressRecipe>> selected = AmmoPressMenu.this.selectedRecipe();
            if (selected.isEmpty()) {
                return false;
            }
            AmmoPressRecipe.SlotIngredient required = selected.get().value().input().get(this.getSlotIndex());
            return !required.isEmpty()
                    && required.ingredient().isPresent()
                    && required.ingredient().orElseThrow().test(stack);
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
