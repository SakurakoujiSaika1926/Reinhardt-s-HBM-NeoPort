package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
import com.reinhardt.hbm.registry.HbmFluids;
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

/** Exact 1.7.10 ContainerMachinePrecAss layout and shift-click contract. */
public final class PrecisionAssemblerMenu extends AbstractContainerMenu {
    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 4;
    public static final int INPUT_START = 4;
    public static final int INPUT_END = 13;
    public static final int OUTPUT_START = 13;
    public static final int OUTPUT_END = 22;
    public static final int SLOT_COUNT = 22;

    private static final int PLAYER_START = SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public PrecisionAssemblerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()),
                new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public PrecisionAssemblerMenu(int containerId, Inventory playerInventory, LegacyMachineBlockEntity machine) {
        this(containerId, playerInventory, machine, machine.menuData());
    }

    private PrecisionAssemblerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.PRECISION_ASSEMBLER.get(), containerId);
        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof BlockEntity blockEntity ? blockEntity.getBlockPos().immutable() : BlockPos.ZERO;

        addSlot(new ValidatedSlot(container, BATTERY_SLOT, 152, 81));
        addSlot(new ValidatedSlot(container, BLUEPRINT_SLOT, 35, 126));
        addSlot(new ValidatedSlot(container, UPGRADE_START, 152, 108));
        addSlot(new ValidatedSlot(container, UPGRADE_START + 1, 152, 126));
        addGrid(container, INPUT_START, 8, 27, false);
        addGrid(container, OUTPUT_START, 80, 27, true);
        addPlayerInventory(playerInventory, 8, 174);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return moved;

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (LegacyMachineBlockEntity.isBatteryStack(stack)) {
            if (!moveItemStackTo(stack, BATTERY_SLOT, BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (BlueprintItem.isBlueprint(stack)) {
            if (!moveItemStackTo(stack, BLUEPRINT_SLOT, BLUEPRINT_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (isSupportedUpgrade(stack)) {
            if (!moveItemStackTo(stack, UPGRADE_START, UPGRADE_END, false)) return ItemStack.EMPTY;
        } else if (isPrecisionIngredient(stack)) {
            if (!moveItemStackTo(stack, INPUT_START, INPUT_END, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof LegacyMachineBlockEntity machine) || !stillValid(player)) return false;
        if (id <= 0) {
            machine.setPrecisionAssemblerRecipe(null);
            return true;
        }
        List<RecipeHolder<PrecisionAssemblerRecipe>> recipes = recipes();
        int index = id - 1;
        if (index < 0 || index >= recipes.size()) return false;
        machine.setPrecisionAssemblerRecipe(recipes.get(index).id());
        return true;
    }

    public int energy() { return this.data.get(0); }
    public int progress() { return this.data.get(2); }
    public int capacity() { return Math.max(100_000, this.data.get(12)); }
    public int workTime() { return Math.max(1, this.data.get(13)); }
    public int selectedRecipeIndex() { return this.data.get(14) - 1; }
    public int recipeCount() { return Math.max(0, this.data.get(15)); }
    public boolean isWorking() { return this.data.get(16) != 0; }
    public BlockPos blockPos() { return this.blockPos; }

    public int energyScaled(int pixels) { return Math.min(pixels, this.energy() * pixels / Math.max(1, capacity())); }
    public int progressScaled(int pixels) { return Math.min(pixels, this.progress() * pixels / Math.max(1, workTime())); }
    public int tankScaled(int tank, int pixels) { return Math.min(pixels, tankAmount(tank) * pixels / Math.max(1, tankCapacity(tank))); }
    public HbmFluidDefinition tankFluid(int tank) { return HbmFluids.byOldId(this.data.get(tank == 0 ? 5 : 7)).orElse(HbmFluids.none()); }
    public int tankAmount(int tank) { return Math.max(0, this.data.get(tank == 0 ? 6 : 8)); }
    public int tankCapacity(int tank) { return this.container instanceof LegacyMachineBlockEntity machine ? machine.precisionAssemblerTankCapacity(tank) : 4_000; }

    public List<RecipeHolder<PrecisionAssemblerRecipe>> recipes() {
        if (this.container instanceof LegacyMachineBlockEntity machine) return machine.availablePrecisionAssemblerRecipes(this.playerInventory.player.level());
        return List.of();
    }

    public Optional<RecipeHolder<PrecisionAssemblerRecipe>> selectedRecipe() {
        int index = selectedRecipeIndex();
        List<RecipeHolder<PrecisionAssemblerRecipe>> recipes = recipes();
        return index >= 0 && index < recipes.size() ? Optional.of(recipes.get(index)) : Optional.empty();
    }

    public Optional<ResourceLocation> selectedRecipeId() { return selectedRecipe().map(RecipeHolder::id); }

    public int buttonIdForRecipe(ResourceLocation id) {
        List<RecipeHolder<PrecisionAssemblerRecipe>> recipes = recipes();
        for (int index = 0; index < recipes.size(); index++) if (recipes.get(index).id().equals(id)) return index + 1;
        return 0;
    }

    private void addGrid(Container container, int firstSlot, int x, int y, boolean output) {
        int slot = firstSlot;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(output ? new OutputSlot(container, slot++, x + column * 18, y + row * 18)
                        : new ValidatedSlot(container, slot++, x + column * 18, y + row * 18));
            }
        }
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
        for (int column = 0; column < 9; column++) this.addSlot(new Slot(inventory, column, left + column * 18, top + 58));
    }

    private boolean isPrecisionIngredient(ItemStack stack) {
        if (this.container instanceof LegacyMachineBlockEntity machine) return machine.canAcceptPrecisionAssemblerInput(stack);
        return false;
    }

    private static boolean isSupportedUpgrade(ItemStack stack) {
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return MachineUpgradeItem.isMachineUpgrade(stack)
                && (type == MachineUpgradeItem.UpgradeType.SPEED || type == MachineUpgradeItem.UpgradeType.POWER || type == MachineUpgradeItem.UpgradeType.OVERDRIVE);
    }

    private static Container getContainer(Inventory inventory, BlockPos position) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(position);
        return blockEntity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_precass")
                ? machine : new SimpleContainer(SLOT_COUNT);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }

    private static final class OutputSlot extends LegacyAchievementOutputSlot {
        private OutputSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
