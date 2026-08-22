package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.AssemblyFactoryBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.BlueprintItem;
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

public class AssemblyFactoryMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = AssemblyFactoryBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private static final int MODULE_BUTTON_STRIDE = 10_000;
    private static final int DATA_MODULE_START = 4;
    private static final int DATA_PER_MODULE = 6;
    private static final int DATA_TANK_START = DATA_MODULE_START + AssemblyFactoryBlockEntity.MODULE_COUNT * DATA_PER_MODULE;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public AssemblyFactoryMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(AssemblyFactoryBlockEntity.DATA_COUNT));
    }

    public AssemblyFactoryMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.ASSEMBLY_FACTORY.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, AssemblyFactoryBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, AssemblyFactoryBlockEntity.BATTERY_SLOT, 234, 112));
        this.addSlot(new ValidatedSlot(container, AssemblyFactoryBlockEntity.UPGRADE_START, 214, 149));
        this.addSlot(new ValidatedSlot(container, AssemblyFactoryBlockEntity.UPGRADE_START + 1, 214, 167));
        this.addSlot(new ValidatedSlot(container, AssemblyFactoryBlockEntity.UPGRADE_START + 2, 214, 185));
        addModuleSlots(container);
        addPlayerInventory(playerInventory, 33, 158);
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
            slot.onTake(player, stack);
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, AssemblyFactoryBlockEntity.BATTERY_SLOT, AssemblyFactoryBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BlueprintItem.isBlueprint(stack)) {
            if (!moveToBlueprintSlots(stack)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, AssemblyFactoryBlockEntity.UPGRADE_START, AssemblyFactoryBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isAssemblyIngredient(stack)) {
            if (!moveToInputSlots(stack)) {
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
        if (!(this.container instanceof AssemblyFactoryBlockEntity factory) || !stillValid(player)) {
            return false;
        }
        int module = id / MODULE_BUTTON_STRIDE - 1;
        int localId = id % MODULE_BUTTON_STRIDE;
        if (module < 0) {
            module = 0;
            localId = id;
        }
        if (module < 0 || module >= AssemblyFactoryBlockEntity.MODULE_COUNT) {
            return false;
        }
        if (localId <= 0) {
            factory.setSelectedRecipe(module, null);
            return true;
        }

        List<RecipeHolder<AssemblyMachineRecipe>> recipes = assemblyRecipes(module);
        int recipeIndex = localId - 1;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return false;
        }
        factory.setSelectedRecipe(module, recipes.get(recipeIndex).id());
        return true;
    }

    public int energy() {
        return this.data.get(0);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public int energyCapacity() {
        return Math.max((int) AssemblyFactoryBlockEntity.BASE_ENERGY_CAPACITY, this.data.get(2));
    }

    public int completedCycles() {
        return this.data.get(3);
    }

    public int progress(int module) {
        return moduleData(module, 0);
    }

    public int workTime(int module) {
        return Math.max(1, moduleData(module, 1));
    }

    public int currentDemand(int module) {
        return Math.max(1, moduleData(module, 2));
    }

    public boolean hasRecipe(int module) {
        return moduleData(module, 3) != 0;
    }

    public boolean isWorking(int module) {
        return moduleData(module, 4) != 0;
    }

    public int selectedRecipeIndex(int module) {
        return moduleData(module, 5) - 1;
    }

    public HbmFluidDefinition tankFluid(int tank) {
        return HbmFluids.byOldId(this.data.get(DATA_TANK_START + tank * 3)).orElse(HbmFluids.none());
    }

    public int tankAmount(int tank) {
        return this.data.get(DATA_TANK_START + tank * 3 + 1);
    }

    public int tankPressure(int tank) {
        return this.data.get(DATA_TANK_START + tank * 3 + 2);
    }

    public int tankCapacity(int tank) {
        return tank < AssemblyFactoryBlockEntity.MODULE_COUNT * 2
                ? AssemblyFactoryBlockEntity.RECIPE_TANK_CAPACITY
                : AssemblyFactoryBlockEntity.COOLANT_TANK_CAPACITY;
    }

    public Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe(int module) {
        int index = selectedRecipeIndex(module);
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = assemblyRecipes(module);
        if (index < 0 || index >= recipes.size()) {
            return Optional.empty();
        }
        return Optional.of(recipes.get(index));
    }

    public Optional<ResourceLocation> selectedRecipeId(int module) {
        return selectedRecipe(module).map(RecipeHolder::id);
    }

    public List<RecipeHolder<AssemblyMachineRecipe>> assemblyRecipes(int module) {
        if (this.container instanceof AssemblyFactoryBlockEntity factory) {
            return factory.availableRecipes(this.playerInventory.player.level(), module);
        }
        List<RecipeHolder<AssemblyMachineRecipe>> visibleRecipes = this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(Optional.empty()))
                .toList();
        return AssemblyMachineRecipe.activeVariants(visibleRecipes);
    }

    public int buttonIdForRecipe(int module, ResourceLocation recipeId) {
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = assemblyRecipes(module);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipeId)) {
                return buttonId(module, index + 1);
            }
        }
        return buttonId(module, 0);
    }

    public int buttonIdForClear(int module) {
        return buttonId(module, 0);
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, this.energy() * pixels / this.energyCapacity());
    }

    public int progressScaled(int module, int pixels) {
        return Math.min(pixels, this.progress(module) * pixels / this.workTime(module));
    }

    public int tankScaled(int tank, int pixels) {
        return Math.min(pixels, this.tankAmount(tank) * pixels / this.tankCapacity(tank));
    }

    private void addModuleSlots(Container container) {
        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int left = (module % 2) * 109;
            int top = (module / 2) * 56;
            this.addSlot(new ValidatedSlot(container, AssemblyFactoryBlockEntity.blueprintSlot(module), 25 + left, 54 + top));
            addInputSlots(container, module, 7 + left, 20 + top);
            this.addSlot(new OutputSlot(container, AssemblyFactoryBlockEntity.outputSlot(module), 87 + left, 54 + top));
        }
    }

    private void addInputSlots(Container container, int module, int left, int top) {
        int slot = AssemblyFactoryBlockEntity.inputSlotStart(module);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 6; column++) {
                this.addSlot(new ValidatedSlot(container, slot++, left + column * 16, top + row * 16));
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

    private boolean moveToBlueprintSlots(ItemStack stack) {
        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int slot = AssemblyFactoryBlockEntity.blueprintSlot(module);
            if (moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean moveToInputSlots(ItemStack stack) {
        boolean moved = false;
        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int start = AssemblyFactoryBlockEntity.inputSlotStart(module);
            moved |= moveItemStackTo(stack, start, start + AssemblyFactoryBlockEntity.MODULE_INPUT_COUNT, false);
            if (stack.isEmpty()) {
                return true;
            }
        }
        return moved;
    }

    private boolean isAssemblyIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.container instanceof AssemblyFactoryBlockEntity factory) {
            for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
                if (factory.canAcceptInput(module, stack)) {
                    return true;
                }
            }
            return false;
        }
        return this.playerInventory.player.level().getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get()).stream()
                .anyMatch(holder -> holder.value().ingredients().stream().anyMatch(ingredient -> ingredient.ingredient().test(stack)));
    }

    private int moduleData(int module, int field) {
        if (module < 0 || module >= AssemblyFactoryBlockEntity.MODULE_COUNT) {
            return 0;
        }
        return this.data.get(DATA_MODULE_START + module * DATA_PER_MODULE + field);
    }

    private static int buttonId(int module, int localId) {
        return (module + 1) * MODULE_BUTTON_STRIDE + localId;
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AssemblyFactoryBlockEntity factory) {
            return factory;
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
}
