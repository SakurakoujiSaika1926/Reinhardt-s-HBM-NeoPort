package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ChemicalPlantBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
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
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.List;
import java.util.Optional;

public class ChemicalPlantMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ChemicalPlantBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public ChemicalPlantMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(ChemicalPlantBlockEntity.DATA_COUNT));
    }

    public ChemicalPlantMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.CHEMICAL_PLANT.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ChemicalPlantBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, ChemicalPlantBlockEntity.BATTERY_SLOT, 152, 81));
        this.addSlot(new ValidatedSlot(container, ChemicalPlantBlockEntity.BLUEPRINT_SLOT, 35, 126));
        this.addSlot(new ValidatedSlot(container, ChemicalPlantBlockEntity.UPGRADE_START, 152, 108));
        this.addSlot(new ValidatedSlot(container, ChemicalPlantBlockEntity.UPGRADE_START + 1, 152, 126));
        addSlots(container, ChemicalPlantBlockEntity.SOLID_INPUT_START, 8, 99, false);
        addSlots(container, ChemicalPlantBlockEntity.SOLID_OUTPUT_START, 80, 99, true);
        addSlots(container, ChemicalPlantBlockEntity.FLUID_INPUT_CONTAINER_START, 8, 54, false);
        addSlots(container, ChemicalPlantBlockEntity.FLUID_INPUT_CONTAINER_RESULT_START, 8, 72, true);
        addSlots(container, ChemicalPlantBlockEntity.FLUID_OUTPUT_CONTAINER_START, 80, 54, false);
        addSlots(container, ChemicalPlantBlockEntity.FLUID_OUTPUT_CONTAINER_RESULT_START, 80, 72, true);
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

        if (index >= ChemicalPlantBlockEntity.SOLID_OUTPUT_START && index < ChemicalPlantBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, ChemicalPlantBlockEntity.BATTERY_SLOT, ChemicalPlantBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ChemicalPlantBlockEntity.isBlueprint(stack)) {
            if (!moveItemStackTo(stack, ChemicalPlantBlockEntity.BLUEPRINT_SLOT, ChemicalPlantBlockEntity.BLUEPRINT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, ChemicalPlantBlockEntity.UPGRADE_START, ChemicalPlantBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isFilledFluidContainer(stack)) {
            if (!moveFullFluidContainer(stack)) {
                return ItemStack.EMPTY;
            }
        } else if (isEmptyFluidContainer(stack)) {
            if (!moveItemStackTo(stack, ChemicalPlantBlockEntity.FLUID_OUTPUT_CONTAINER_START, ChemicalPlantBlockEntity.FLUID_OUTPUT_CONTAINER_START + 3, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isChemicalIngredient(stack)) {
            if (!moveItemStackTo(stack, ChemicalPlantBlockEntity.SOLID_INPUT_START, ChemicalPlantBlockEntity.SOLID_INPUT_END, false)) {
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
        if (!(this.container instanceof ChemicalPlantBlockEntity chemicalPlant) || !stillValid(player)) {
            return false;
        }
        if (id <= 0) {
            chemicalPlant.setSelectedRecipe(null);
            return true;
        }

        List<RecipeHolder<ChemicalPlantRecipe>> recipes = chemicalPlantRecipes();
        int recipeIndex = id - 1;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return false;
        }
        chemicalPlant.setSelectedRecipe(recipes.get(recipeIndex).id());
        return true;
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

    public int workTime() {
        return Math.max(1, this.data.get(3));
    }

    public int completedCycles() {
        return this.data.get(4);
    }

    public int currentDemand() {
        return Math.max(1, this.data.get(5));
    }

    public int energyCapacity() {
        return Math.max((int) ChemicalPlantBlockEntity.BASE_ENERGY_CAPACITY, this.data.get(6));
    }

    public boolean hasRecipe() {
        return this.data.get(7) != 0;
    }

    public int selectedRecipeIndex() {
        return this.data.get(8) - 1;
    }

    public boolean isWorking() {
        return this.data.get(9) != 0;
    }

    public HbmFluidDefinition tankFluid(int tank) {
        return HbmFluids.byOldId(this.data.get(10 + tank * 3)).orElse(HbmFluids.none());
    }

    public int tankAmount(int tank) {
        return this.data.get(10 + tank * 3 + 1);
    }

    public int tankPressure(int tank) {
        return this.data.get(10 + tank * 3 + 2);
    }

    public int tankCapacity() {
        return ChemicalPlantBlockEntity.TANK_CAPACITY;
    }

    public Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<ChemicalPlantRecipe>> recipes = chemicalPlantRecipes();
        if (index < 0 || index >= recipes.size()) {
            return Optional.empty();
        }
        return Optional.of(recipes.get(index));
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return selectedRecipe().map(RecipeHolder::id);
    }

    public List<RecipeHolder<ChemicalPlantRecipe>> chemicalPlantRecipes() {
        if (this.container instanceof ChemicalPlantBlockEntity chemicalPlant) {
            return chemicalPlant.availableRecipes(this.playerInventory.player.level());
        }
        return this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(Optional.empty()))
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    public int buttonIdForRecipe(ResourceLocation recipeId) {
        List<RecipeHolder<ChemicalPlantRecipe>> recipes = chemicalPlantRecipes();
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipeId)) {
                return index + 1;
            }
        }
        return 0;
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, this.energy() * pixels / this.energyCapacity());
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.workTime());
    }

    public int tankScaled(int tank, int pixels) {
        return Math.min(pixels, this.tankAmount(tank) * pixels / this.tankCapacity());
    }

    private void addSlots(Container container, int startSlot, int left, int top, boolean output) {
        for (int column = 0; column < 3; column++) {
            int slot = startSlot + column;
            this.addSlot(output ? new OutputSlot(container, slot, left + column * 18, top) : new ValidatedSlot(container, slot, left + column * 18, top));
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

    private boolean moveFullFluidContainer(ItemStack stack) {
        HbmFluidDefinition fluid = fluidIn(stack);
        Optional<RecipeHolder<ChemicalPlantRecipe>> recipe = selectedRecipe();
        if (recipe.isPresent()) {
            List<ChemicalPlantRecipe.ChemicalFluidStack> fluids = recipe.get().value().inputFluids();
            for (int index = 0; index < fluids.size(); index++) {
                ChemicalPlantRecipe.ChemicalFluidStack input = fluids.get(index);
                if (input.type() == fluid && input.pressure() == 0) {
                    if (moveItemStackTo(stack, ChemicalPlantBlockEntity.FLUID_INPUT_CONTAINER_START + index, ChemicalPlantBlockEntity.FLUID_INPUT_CONTAINER_START + index + 1, false)) {
                        return true;
                    }
                }
            }
        }
        return moveItemStackTo(stack, ChemicalPlantBlockEntity.FLUID_INPUT_CONTAINER_START, ChemicalPlantBlockEntity.FLUID_INPUT_CONTAINER_START + 3, false);
    }

    private boolean isChemicalIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.container instanceof ChemicalPlantBlockEntity chemicalPlant) {
            return chemicalPlant.canAcceptInput(stack);
        }
        return this.playerInventory.player.level().getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get()).stream()
                .anyMatch(holder -> holder.value().inputItems().stream().anyMatch(ingredient -> ingredient.ingredient().test(stack)));
    }

    private static boolean isFilledFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof InfiniteFluidContainerItem
                || (stack.getItem() instanceof HbmFluidContainerItem item && item.isFilledContainer())
                || !fluidIn(stack).isNone();
    }

    private static boolean isEmptyFluidContainer(ItemStack stack) {
        if (stack.getItem() instanceof HbmFluidContainerItem item) {
            return !item.isFilledContainer();
        }
        return FluidUtil.getFluidHandler(stack.copyWithCount(1))
                .filter(handler -> handler.drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).isEmpty())
                .isPresent();
    }

    private static HbmFluidDefinition fluidIn(ItemStack stack) {
        if (stack.getItem() instanceof InfiniteFluidContainerItem infinite) {
            return infinite.fluid();
        }
        if (stack.getItem() instanceof HbmFluidContainerItem item && item.isFilledContainer()) {
            return HbmFluidContainerItem.fluid(stack);
        }
        return FluidUtil.getFluidContained(stack)
                .flatMap(contained -> HbmFluids.fromNeoFluid(contained.getFluid()))
                .orElse(HbmFluids.none());
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ChemicalPlantBlockEntity chemicalPlant) {
            return chemicalPlant;
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
