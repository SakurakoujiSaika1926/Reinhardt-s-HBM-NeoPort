package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.RockMillBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.recipe.RockMillRecipe;
import com.reinhardt.hbm.registry.HbmFluids;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class RockMillMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = RockMillBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public RockMillMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()),
                new SimpleContainerData(RockMillBlockEntity.DATA_COUNT));
    }

    public RockMillMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.ROCK_MILL.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, RockMillBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, RockMillBlockEntity.BATTERY_SLOT, 152, 91));
        addSlot(new ValidatedSlot(container, RockMillBlockEntity.BLUEPRINT_SLOT, 35, 90));
        addSlot(new ValidatedSlot(container, RockMillBlockEntity.INPUT_START, 8, 27));
        addSlot(new ValidatedSlot(container, RockMillBlockEntity.INPUT_START + 1, 26, 27));
        addSlot(new ValidatedSlot(container, RockMillBlockEntity.INPUT_START + 2, 44, 27));
        addSlot(new OutputSlot(container, RockMillBlockEntity.OUTPUT_START, 80, 27));
        addSlot(new OutputSlot(container, RockMillBlockEntity.OUTPUT_START + 1, 98, 27));
        addSlot(new OutputSlot(container, RockMillBlockEntity.OUTPUT_START + 2, 116, 27));
        addPlayerInventory(playerInventory, 8, 138);
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, RockMillBlockEntity.BATTERY_SLOT,
                    RockMillBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BlueprintItem.isBlueprint(stack)) {
            if (!moveItemStackTo(stack, RockMillBlockEntity.BLUEPRINT_SLOT,
                    RockMillBlockEntity.BLUEPRINT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isRecipeInput(stack)) {
            if (!moveItemStackTo(stack, RockMillBlockEntity.INPUT_START,
                    RockMillBlockEntity.INPUT_START + 1, false)) {
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
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof RockMillBlockEntity mill) || !stillValid(player)) {
            return false;
        }
        if (id == 0) {
            mill.setSelectedRecipe(null);
            return true;
        }
        List<RecipeHolder<RockMillRecipe>> recipes = rockMillRecipes();
        int index = id - 1;
        if (index < 0 || index >= recipes.size()) {
            return false;
        }
        mill.setSelectedRecipe(recipes.get(index).id());
        return true;
    }

    public List<RecipeHolder<RockMillRecipe>> rockMillRecipes() {
        if (this.container instanceof RockMillBlockEntity mill) {
            return mill.recipes(this.playerInventory.player.level());
        }
        return this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ROCK_MILL.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    public Optional<RecipeHolder<RockMillRecipe>> selectedRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<RockMillRecipe>> recipes = rockMillRecipes();
        return index >= 0 && index < recipes.size() ? Optional.of(recipes.get(index)) : Optional.empty();
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return selectedRecipe().map(RecipeHolder::id);
    }

    public int buttonIdForRecipe(ResourceLocation recipeId) {
        List<RecipeHolder<RockMillRecipe>> recipes = rockMillRecipes();
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipeId)) {
                return index + 1;
            }
        }
        return 0;
    }

    public int power() {
        return this.data.get(0);
    }

    public int maxPower() {
        return Math.max((int) RockMillBlockEntity.BASE_MAX_POWER, this.data.get(1));
    }

    public int lastInput() {
        return this.data.get(2);
    }

    public int progress() {
        return this.data.get(3);
    }

    public int duration() {
        return Math.max(1, this.data.get(4));
    }

    public int consumption() {
        return Math.max(0, this.data.get(5));
    }

    public boolean processing() {
        return this.data.get(6) != 0;
    }

    public HbmFluidDefinition inputFluid() {
        return HbmFluids.byOldId(this.data.get(7)).orElse(HbmFluids.none());
    }

    public int inputAmount() {
        return this.data.get(8);
    }

    public int inputCapacity() {
        return Math.max(1, this.data.get(9));
    }

    public HbmFluidDefinition outputFluid() {
        return HbmFluids.byOldId(this.data.get(10)).orElse(HbmFluids.none());
    }

    public int outputAmount() {
        return this.data.get(11);
    }

    public int outputCapacity() {
        return Math.max(1, this.data.get(12));
    }

    public int selectedRecipeIndex() {
        return this.data.get(13) - 1;
    }

    public int completedCycles() {
        return this.data.get(14);
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / Math.max(1, this.maxPower())));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.duration());
    }

    public int inputScaled(int pixels) {
        return Math.min(pixels, this.inputAmount() * pixels / this.inputCapacity());
    }

    public int outputScaled(int pixels) {
        return Math.min(pixels, this.outputAmount() * pixels / this.outputCapacity());
    }

    private boolean isRecipeInput(ItemStack stack) {
        if (this.container instanceof RockMillBlockEntity mill) {
            return mill.canAcceptRecipeInput(stack);
        }
        return rockMillRecipes().stream().anyMatch(holder -> holder.value().input().ingredient().test(stack));
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
        return blockEntity instanceof RockMillBlockEntity mill ? mill : new SimpleContainer(MACHINE_SLOT_COUNT);
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
