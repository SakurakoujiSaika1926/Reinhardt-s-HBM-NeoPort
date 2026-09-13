package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.AssemblyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
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

import java.util.List;
import java.util.Optional;

public class AssemblyMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = AssemblyMachineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public AssemblyMachineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(AssemblyMachineBlockEntity.DATA_COUNT));
    }

    public AssemblyMachineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.ASSEMBLY_MACHINE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, AssemblyMachineBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        this.addSlot(new BatterySlot(container, AssemblyMachineBlockEntity.BATTERY_SLOT, 152, 81));
        this.addSlot(new BlueprintSlot(container, AssemblyMachineBlockEntity.BLUEPRINT_SLOT, 35, 126));
        this.addSlot(new UpgradeSlot(container, AssemblyMachineBlockEntity.UPGRADE_START, 152, 108));
        this.addSlot(new UpgradeSlot(container, AssemblyMachineBlockEntity.UPGRADE_START + 1, 152, 126));
        addInputSlots(container);
        this.addSlot(new OutputSlot(container, AssemblyMachineBlockEntity.OUTPUT_SLOT, 98, 45));
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

        if (index == AssemblyMachineBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, AssemblyMachineBlockEntity.BATTERY_SLOT, AssemblyMachineBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (AssemblyMachineBlockEntity.isBlueprint(stack)) {
            if (!moveItemStackTo(stack, AssemblyMachineBlockEntity.BLUEPRINT_SLOT, AssemblyMachineBlockEntity.BLUEPRINT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, AssemblyMachineBlockEntity.UPGRADE_START, AssemblyMachineBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isAssemblyIngredient(stack)) {
            if (!moveItemStackTo(stack, AssemblyMachineBlockEntity.INPUT_START, AssemblyMachineBlockEntity.INPUT_END, false)) {
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
        if (!(this.container instanceof AssemblyMachineBlockEntity assemblyMachine) || !stillValid(player)) {
            return false;
        }
        if (id <= 0) {
            assemblyMachine.setSelectedRecipe(null);
            return true;
        }

        List<RecipeHolder<AssemblyMachineRecipe>> recipes = assemblyRecipes();
        int recipeIndex = id - 1;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return false;
        }
        assemblyMachine.setSelectedRecipe(recipes.get(recipeIndex).id());
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
        return Math.max((int) AssemblyMachineBlockEntity.BASE_ENERGY_CAPACITY, this.data.get(6));
    }

    public boolean hasRecipe() {
        return this.data.get(7) != 0;
    }

    public int selectedRecipeIndex() {
        return this.data.get(8) - 1;
    }

    public boolean hasSelectedRecipe() {
        return selectedRecipeIndex() >= 0;
    }

    public HbmFluidDefinition tankFluid(int tank) {
        return HbmFluids.byOldId(this.data.get(9 + tank * 3)).orElse(HbmFluids.none());
    }

    public int tankAmount(int tank) {
        return this.data.get(9 + tank * 3 + 1);
    }

    public int tankPressure(int tank) {
        return this.data.get(9 + tank * 3 + 2);
    }

    public int tankCapacity() {
        return AssemblyMachineBlockEntity.TANK_CAPACITY;
    }

    public Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = assemblyRecipes();
        if (index < 0 || index >= recipes.size()) {
            return Optional.empty();
        }
        return Optional.of(recipes.get(index));
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return selectedRecipe().map(RecipeHolder::id);
    }

    public List<RecipeHolder<AssemblyMachineRecipe>> assemblyRecipes() {
        if (this.container instanceof AssemblyMachineBlockEntity assemblyMachine) {
            return assemblyMachine.availableRecipes(this.playerInventory.player.level());
        }
        List<RecipeHolder<AssemblyMachineRecipe>> visibleRecipes = this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(Optional.empty()))
                .toList();
        return AssemblyMachineRecipe.activeVariants(visibleRecipes);
    }

    public int buttonIdForRecipe(ResourceLocation recipeId) {
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = assemblyRecipes();
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

    public boolean isWorking() {
        return this.progress() > 0 && this.energy() >= this.currentDemand();
    }

    private void addInputSlots(Container container) {
        int slot = AssemblyMachineBlockEntity.INPUT_START;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new InputSlot(container, slot++, 8 + column * 18, 18 + row * 18));
            }
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

    private boolean isAssemblyIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.container instanceof AssemblyMachineBlockEntity assemblyMachine) {
            return assemblyMachine.canAcceptInput(stack);
        }
        return this.playerInventory.player.level().getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get()).stream()
                .anyMatch(holder -> holder.value().ingredients().stream().anyMatch(ingredient -> ingredient.ingredient().test(stack)));
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AssemblyMachineBlockEntity assemblyMachine) {
            return assemblyMachine;
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

    private static final class BatterySlot extends ValidatedSlot {
        private BatterySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class BlueprintSlot extends ValidatedSlot {
        private BlueprintSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class UpgradeSlot extends ValidatedSlot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class InputSlot extends ValidatedSlot {
        private InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
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
