package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.CrucibleBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.recipe.CrucibleRecipe;
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

public class CrucibleMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = CrucibleBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT - 1;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public CrucibleMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(CrucibleBlockEntity.DATA_COUNT));
    }

    public CrucibleMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.CRUCIBLE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CrucibleBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        int slot = CrucibleBlockEntity.INPUT_START;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new CrucibleInputSlot(container, slot++, 107 + column * 18, 18 + row * 18));
            }
        }
        addPlayerInventory(playerInventory, 8, 132);
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
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(CrucibleBlockEntity.INPUT_START, stack)) {
            if (!moveItemStackTo(stack, 0, PLAYER_INVENTORY_START, false)) {
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof CrucibleBlockEntity crucible) || !stillValid(player)) {
            return false;
        }
        if (id <= 0) {
            crucible.setSelectedRecipe(null);
            return true;
        }

        List<RecipeHolder<CrucibleRecipe>> recipes = crucibleRecipes();
        int recipeIndex = id - 1;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return false;
        }
        crucible.setSelectedRecipe(recipes.get(recipeIndex).id());
        return true;
    }

    public int heat() {
        return this.data.get(0);
    }

    public int progress() {
        return this.data.get(1);
    }

    public int recipeAmount() {
        return this.data.get(5);
    }

    public int wasteAmount() {
        return this.data.get(6);
    }

    public int heatScaled(int pixels) {
        return Math.min(pixels, this.heat() * pixels / CrucibleBlockEntity.MAX_HEAT);
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / CrucibleBlockEntity.PROCESS_TIME);
    }

    public int recipeScaled(int pixels) {
        return Math.min(pixels, this.recipeAmount() * pixels / CrucibleBlockEntity.RECIPE_CAPACITY);
    }

    public int wasteScaled(int pixels) {
        return Math.min(pixels, this.wasteAmount() * pixels / CrucibleBlockEntity.WASTE_CAPACITY);
    }

    public int selectedRecipeIndex() {
        return this.data.get(4) - 1;
    }

    public Optional<RecipeHolder<CrucibleRecipe>> selectedRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<CrucibleRecipe>> recipes = crucibleRecipes();
        if (index < 0 || index >= recipes.size()) {
            return Optional.empty();
        }
        return Optional.of(recipes.get(index));
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return selectedRecipe().map(RecipeHolder::id);
    }

    public List<FoundryMaterialStack> recipeStack() {
        if (this.container instanceof CrucibleBlockEntity crucible) {
            return crucible.recipeStack();
        }
        return List.of();
    }

    public List<FoundryMaterialStack> wasteStack() {
        if (this.container instanceof CrucibleBlockEntity crucible) {
            return crucible.wasteStack();
        }
        return List.of();
    }

    public List<RecipeHolder<CrucibleRecipe>> crucibleRecipes() {
        if (this.container instanceof CrucibleBlockEntity crucible) {
            return crucible.availableRecipes(this.playerInventory.player.level());
        }
        return this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CRUCIBLE.get())
                .stream()
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    public int buttonIdForRecipe(ResourceLocation recipeId) {
        List<RecipeHolder<CrucibleRecipe>> recipes = crucibleRecipes();
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipeId)) {
                return index + 1;
            }
        }
        return 0;
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
        if (blockEntity instanceof CrucibleBlockEntity crucible) {
            return crucible;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static final class CrucibleInputSlot extends Slot {
        private CrucibleInputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }
}
