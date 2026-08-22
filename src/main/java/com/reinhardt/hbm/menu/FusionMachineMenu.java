package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.recipe.FusionRecipe;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.registry.HbmItems;
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

public class FusionMachineMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_SIZE = 27;
    private static final int HOTBAR_SIZE = 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;
    private final FusionMachineBlock.Kind kind;
    private final BlockPos blockPos;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    public FusionMachineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(FusionMachineBlockEntity.DATA_COUNT));
    }

    public FusionMachineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.FUSION_MACHINE.get(), containerId);
        checkContainerSize(container, FusionMachineBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, FusionMachineBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;
        this.kind = container instanceof FusionMachineBlockEntity fusion ? fusion.kind() : FusionMachineBlock.Kind.TORUS;
        this.blockPos = container instanceof FusionMachineBlockEntity fusion ? fusion.getBlockPos() : BlockPos.ZERO;

        addMachineSlots();
        this.playerInventoryStart = this.slots.size();
        addPlayerInventory(playerInventory, inventoryLeft(), inventoryTop());
        this.playerInventoryEnd = this.playerInventoryStart + PLAYER_INVENTORY_SIZE;
        this.hotbarStart = this.playerInventoryEnd;
        this.hotbarEnd = this.hotbarStart + HOTBAR_SIZE;
        addDataSlots(data);
    }

    private void addMachineSlots() {
        switch (this.kind) {
            case TORUS -> {
                addSlot(new ValidatedSlot(this.container, FusionMachineBlockEntity.BATTERY_SLOT, 8, 82));
                addSlot(new ValidatedSlot(this.container, FusionMachineBlockEntity.BLUEPRINT_SLOT, 71, 81));
                addSlot(new OutputSlot(this.container, FusionMachineBlockEntity.OUTPUT_SLOT, 130, 36));
            }
            case KLYSTRON -> addSlot(new ValidatedSlot(this.container, FusionMachineBlockEntity.BATTERY_SLOT, 8, 72));
            case BREEDER -> {
                addSlot(new ValidatedSlot(this.container, FusionMachineBlockEntity.BREEDER_FLUID_ID_SLOT, 26, 72));
                addSlot(new ValidatedSlot(this.container, FusionMachineBlockEntity.BREEDER_INPUT_SLOT, 48, 45));
                addSlot(new OutputSlot(this.container, FusionMachineBlockEntity.BREEDER_OUTPUT_SLOT, 112, 45));
            }
            case PLASMA_FORGE -> {
                addSlot(new ValidatedSlot(this.container, FusionMachineBlockEntity.BATTERY_SLOT, 152, 82));
                addSlot(new ValidatedSlot(this.container, FusionMachineBlockEntity.BLUEPRINT_SLOT, 35, 81));
                addSlot(new ValidatedSlot(this.container, FusionMachineBlockEntity.PLASMA_BOOSTER_SLOT, 98, 116));
                int slot = FusionMachineBlockEntity.PLASMA_INPUT_START;
                for (int row = 0; row < 4; row++) {
                    for (int column = 0; column < 3; column++) {
                        addSlot(new ValidatedSlot(this.container, slot++, 8 + column * 18, 18 + row * 18));
                    }
                }
                addSlot(new OutputSlot(this.container, FusionMachineBlockEntity.PLASMA_OUTPUT_SLOT, 116, 36));
            }
            default -> {
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof FusionMachineBlockEntity fusion) || !stillValid(player)) {
            return false;
        }
        if (id == 0 && this.kind == FusionMachineBlock.Kind.TORUS) {
            fusion.setSelectedFusionRecipe(null);
            return true;
        }
        if (id > 0 && this.kind == FusionMachineBlock.Kind.TORUS) {
            List<RecipeHolder<FusionRecipe>> recipes = fusionRecipes();
            int recipeIndex = id - 1;
            if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
                return false;
            }
            fusion.setSelectedFusionRecipe(recipes.get(recipeIndex).id());
            return true;
        }
        if (this.kind == FusionMachineBlock.Kind.PLASMA_FORGE) {
            if (id <= 0) {
                fusion.setSelectedPlasmaForgeRecipe(null);
                return true;
            }
            List<RecipeHolder<PlasmaForgeRecipe>> recipes = plasmaForgeRecipes();
            int recipeIndex = id - 1;
            if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
                return false;
            }
            fusion.setSelectedPlasmaForgeRecipe(recipes.get(recipeIndex).id());
            return true;
        }
        return false;
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

        if (index < this.playerInventoryStart) {
            if (!moveItemStackTo(stack, this.playerInventoryStart, this.hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (!moveItemStackTo(stack, 0, this.playerInventoryStart, false)) {
            if (index < this.playerInventoryEnd) {
                if (!moveItemStackTo(stack, this.hotbarStart, this.hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, this.playerInventoryStart, this.playerInventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
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

    public FusionMachineBlock.Kind kind() {
        return this.kind;
    }

    public long power() {
        return Integer.toUnsignedLong(this.data.get(1));
    }

    public long maxPower() {
        return Integer.toUnsignedLong(this.data.get(2));
    }

    public long lastInput() {
        return Integer.toUnsignedLong(this.data.get(3));
    }

    public long lastOutput() {
        return Integer.toUnsignedLong(this.data.get(4));
    }

    public long outputTarget() {
        return Integer.toUnsignedLong(this.data.get(5));
    }

    public long output() {
        return Integer.toUnsignedLong(this.data.get(6));
    }

    public long klystronEnergy() {
        return Integer.toUnsignedLong(this.data.get(7));
    }

    public long plasmaEnergy() {
        return Integer.toUnsignedLong(this.data.get(8));
    }

    public int neutronEnergy() {
        return this.data.get(9);
    }

    public int progress() {
        return this.data.get(10);
    }

    public int fuelConsumption() {
        return this.data.get(11);
    }

    public int temperature() {
        return this.data.get(12);
    }

    public boolean didProcess() {
        return this.data.get(13) != 0;
    }

    public boolean connected() {
        return this.data.get(14) != 0;
    }

    public int selectedRecipeIndex() {
        return this.data.get(15);
    }

    public int booster() {
        return this.data.get(16);
    }

    public int maxBooster() {
        return Math.max(1, this.data.get(17));
    }

    public int tankFluidId(int tank) {
        return this.data.get(18 + tank * 3);
    }

    public int tankAmount(int tank) {
        return this.data.get(19 + tank * 3);
    }

    public int tankCapacity(int tank) {
        return Math.max(1, this.data.get(20 + tank * 3));
    }

    public int tankScaled(int tank, int pixels) {
        return Math.min(pixels, this.tankAmount(tank) * pixels / this.tankCapacity(tank));
    }

    public int powerScaled(int pixels) {
        return (int) Math.min(pixels, this.power() * pixels / Math.max(1L, this.maxPower()));
    }

    public int outputScaled(int pixels) {
        long target = Math.max(1L, this.outputTarget());
        return (int) Math.min(pixels, this.output() * pixels / target);
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / 10_000);
    }

    public int boosterScaled(int pixels) {
        return Math.min(pixels, this.booster() * pixels / this.maxBooster());
    }

    public ItemStack recipeIcon() {
        if (this.kind == FusionMachineBlock.Kind.PLASMA_FORGE) {
            return selectedPlasmaForgeRecipe()
                    .map(holder -> holder.value().displayIcon())
                    .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        }
        if (this.kind != FusionMachineBlock.Kind.TORUS || this.selectedRecipeIndex() < 0) {
            return new ItemStack(HbmItems.TEMPLATE_FOLDER.get());
        }
        FusionRecipe recipe = selectedFusionRecipe();
        if (recipe == null) {
            return new ItemStack(HbmItems.TEMPLATE_FOLDER.get());
        }
        return recipe.displayIcon();
    }

    public long selectedFusionIgnitionTemp() {
        FusionRecipe recipe = selectedFusionRecipe();
        return recipe == null ? 0L : recipe.ignitionTemp();
    }

    public long selectedPlasmaForgeIgnitionTemp() {
        return selectedPlasmaForgeRecipe()
                .map(holder -> holder.value().ignitionTemp())
                .orElse(0L);
    }

    public long selectedFusionOutputTemp() {
        FusionRecipe recipe = selectedFusionRecipe();
        return recipe == null ? 0L : recipe.outputTemp();
    }

    private FusionRecipe selectedFusionRecipe() {
        if (this.kind != FusionMachineBlock.Kind.TORUS || this.selectedRecipeIndex() < 0) {
            return null;
        }
        List<RecipeHolder<FusionRecipe>> recipes = this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.FUSION.get())
                .stream()
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .toList();
        int index = this.selectedRecipeIndex();
        if (index < 0 || index >= recipes.size()) {
            return null;
        }
        return recipes.get(index).value();
    }

    public Optional<RecipeHolder<PlasmaForgeRecipe>> selectedPlasmaForgeRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<PlasmaForgeRecipe>> recipes = plasmaForgeRecipes();
        if (index < 0 || index >= recipes.size()) {
            return Optional.empty();
        }
        return Optional.of(recipes.get(index));
    }

    public Optional<ResourceLocation> selectedPlasmaForgeRecipeId() {
        return selectedPlasmaForgeRecipe().map(RecipeHolder::id);
    }

    public Optional<ResourceLocation> selectedFusionRecipeId() {
        FusionRecipe recipe = selectedFusionRecipe();
        if (recipe == null || this.selectedRecipeIndex() < 0) {
            return Optional.empty();
        }
        List<RecipeHolder<FusionRecipe>> recipes = fusionRecipes();
        int index = this.selectedRecipeIndex();
        return index >= 0 && index < recipes.size() ? Optional.of(recipes.get(index).id()) : Optional.empty();
    }

    public List<RecipeHolder<FusionRecipe>> fusionRecipes() {
        if (this.container instanceof FusionMachineBlockEntity fusion) {
            return fusion.availableFusionRecipes(this.playerInventory.player.level());
        }
        return this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.FUSION.get())
                .stream()
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    public int buttonIdForFusionRecipe(ResourceLocation recipeId) {
        List<RecipeHolder<FusionRecipe>> recipes = fusionRecipes();
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipeId)) {
                return index + 1;
            }
        }
        return 0;
    }

    public List<RecipeHolder<PlasmaForgeRecipe>> plasmaForgeRecipes() {
        if (this.container instanceof FusionMachineBlockEntity fusion) {
            return fusion.availablePlasmaForgeRecipes(this.playerInventory.player.level());
        }
        return this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.PLASMA_FORGE.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(Optional.empty()))
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    public int buttonIdForPlasmaForgeRecipe(ResourceLocation recipeId) {
        List<RecipeHolder<PlasmaForgeRecipe>> recipes = plasmaForgeRecipes();
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(recipeId)) {
                return index + 1;
            }
        }
        return 0;
    }

    private int inventoryLeft() {
        return switch (this.kind) {
            case TORUS -> 35;
            case KLYSTRON -> 17;
            case PLASMA_FORGE -> 8;
            default -> 8;
        };
    }

    private int inventoryTop() {
        return switch (this.kind) {
            case TORUS, PLASMA_FORGE -> 162;
            case KLYSTRON, BREEDER -> 118;
            default -> 118;
        };
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
        return blockEntity instanceof FusionMachineBlockEntity fusion ? fusion : new SimpleContainer(FusionMachineBlockEntity.SLOT_COUNT);
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
