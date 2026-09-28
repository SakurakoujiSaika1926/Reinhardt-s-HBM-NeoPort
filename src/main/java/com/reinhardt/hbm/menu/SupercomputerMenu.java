package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.blockentity.SupercomputerBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.recipe.SupercomputerRecipe;
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

/** Exact eight-slot layout of ContainerMachineSuperComputer. */
public final class SupercomputerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = SupercomputerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public SupercomputerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()),
                new SimpleContainerData(SupercomputerBlockEntity.DATA_COUNT));
    }

    public SupercomputerMenu(int containerId, Inventory playerInventory,
                             Container container, ContainerData data) {
        super(HbmMenus.SUPERCOMPUTER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, SupercomputerBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, SupercomputerBlockEntity.BATTERY_SLOT, 152, 81));
        addSlot(new ValidatedSlot(container, SupercomputerBlockEntity.BLUEPRINT_SLOT, 35, 80));
        for (int index = 0; index < 3; index++) {
            addSlot(new ValidatedSlot(container, SupercomputerBlockEntity.INPUT_START + index,
                    8 + index * 18, 27));
        }
        for (int index = 0; index < 3; index++) {
            addSlot(new OutputSlot(container, SupercomputerBlockEntity.OUTPUT_START + index,
                    80 + index * 18, 27));
        }
        addPlayerInventory(playerInventory, 8, 129);
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
            if (!moveItemStackTo(stack, SupercomputerBlockEntity.BATTERY_SLOT,
                    SupercomputerBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BlueprintItem.isBlueprint(stack)) {
            if (!moveItemStackTo(stack, SupercomputerBlockEntity.BLUEPRINT_SLOT,
                    SupercomputerBlockEntity.BLUEPRINT_SLOT + 1, false)) {
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
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof SupercomputerBlockEntity computer) || !stillValid(player)) {
            return false;
        }
        if (id == 0) {
            computer.setSelectedRecipe(null);
            return true;
        }
        List<RecipeHolder<SupercomputerRecipe>> recipes = recipes();
        int index = id - 1;
        if (index < 0 || index >= recipes.size()) {
            return false;
        }
        computer.setSelectedRecipe(recipes.get(index).id());
        return true;
    }

    public List<RecipeHolder<SupercomputerRecipe>> recipes() {
        if (this.container instanceof SupercomputerBlockEntity computer) {
            return computer.recipes(this.playerInventory.player.level());
        }
        return this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.SUPERCOMPUTER.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    public Optional<RecipeHolder<SupercomputerRecipe>> selectedRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<SupercomputerRecipe>> recipes = recipes();
        return index >= 0 && index < recipes.size() ? Optional.of(recipes.get(index)) : Optional.empty();
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return selectedRecipe().map(RecipeHolder::id);
    }

    public int buttonIdForRecipe(ResourceLocation recipeId) {
        List<RecipeHolder<SupercomputerRecipe>> recipes = recipes();
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipeId)) {
                return index + 1;
            }
        }
        return 0;
    }

    public int power() { return this.data.get(0); }
    public int maxPower() { return Math.max((int) SupercomputerBlockEntity.BASE_MAX_POWER, this.data.get(1)); }
    public int lastInput() { return this.data.get(2); }
    public int progress() { return this.data.get(3); }
    public int duration() { return Math.max(1, this.data.get(4)); }
    public int consumption() { return Math.max(0, this.data.get(5)); }
    public boolean processing() { return this.data.get(6) != 0; }
    public HbmFluidDefinition inputFluid() { return HbmFluids.byOldId(this.data.get(7)).orElse(HbmFluids.none()); }
    public int inputAmount() { return this.data.get(8); }
    public int inputCapacity() { return Math.max(1, this.data.get(9)); }
    public int inputPressure() { return this.data.get(10); }
    public HbmFluidDefinition outputFluid() { return HbmFluids.byOldId(this.data.get(11)).orElse(HbmFluids.none()); }
    public int outputAmount() { return this.data.get(12); }
    public int outputCapacity() { return Math.max(1, this.data.get(13)); }
    public int outputPressure() { return this.data.get(14); }
    public int selectedRecipeIndex() { return this.data.get(15) - 1; }
    public int completedCycles() { return this.data.get(16); }

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

    private boolean moveIntoMatchingInput(ItemStack stack) {
        if (this.container instanceof SupercomputerBlockEntity computer) {
            for (int slot = SupercomputerBlockEntity.INPUT_START;
                 slot < SupercomputerBlockEntity.INPUT_END; slot++) {
                if (computer.acceptsInputAt(slot, stack)
                        && moveItemStackTo(stack, slot, slot + 1, false)) {
                    return true;
                }
            }
            return false;
        }
        for (RecipeHolder<SupercomputerRecipe> holder : recipes()) {
            for (int index = 0; index < holder.value().ingredients().size(); index++) {
                if (holder.value().ingredients().get(index).ingredient().test(stack)
                        && moveItemStackTo(stack, SupercomputerBlockEntity.INPUT_START + index,
                        SupercomputerBlockEntity.INPUT_START + index + 1, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof SupercomputerBlockEntity computer
                ? computer : new SimpleContainer(MACHINE_SLOT_COUNT);
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
