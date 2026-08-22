package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.AssemblyMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.AssemblyMachineMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class AssemblyMachineBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 4;
    public static final int INPUT_START = 4;
    public static final int INPUT_END = 16;
    public static final int OUTPUT_SLOT = 16;
    public static final int SLOT_COUNT = 17;
    public static final int TANK_CAPACITY = 4_000;
    public static final int DATA_COUNT = 15;
    public static final long BASE_ENERGY_CAPACITY = 100_000L;

    private static final int[] AUTOMATION_SLOTS = createAutomationSlots();

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(TANK_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(TANK_CAPACITY);
    private long energyStored;
    private long lastInput;
    private long energyCapacity = BASE_ENERGY_CAPACITY;
    private int progress;
    private int workTime = 100;
    private int currentDemand = 100;
    private int completedCycles;
    private boolean hasRecipe;
    private int motorSoundCycle;
    private int strikeSoundCycle;
    private boolean wasWorking;
    private final AssemblerArm[] clientArms = {new AssemblerArm(), new AssemblerArm()};
    private double clientPrevRing;
    private double clientRing;
    private double clientRingSpeed;
    private double clientRingTarget;
    private int clientRingDelay;
    private boolean clientFrame;
    private long lastClientAnimationTick = Long.MIN_VALUE;
    @Nullable
    private ResourceLocation selectedRecipeId;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 9 && index < DATA_COUNT) {
                int tankDataIndex = index - 9;
                HbmFluidTank tank = tankByFlatIndex(tankDataIndex / 3);
                return switch (tankDataIndex % 3) {
                    case 0 -> tank.type().oldId();
                    case 1 -> tank.amount();
                    case 2 -> tank.pressure();
                    default -> 0;
                };
            }

            return switch (index) {
                case 0 -> (int) AssemblyMachineBlockEntity.this.energyStored;
                case 1 -> (int) AssemblyMachineBlockEntity.this.lastInput;
                case 2 -> AssemblyMachineBlockEntity.this.progress;
                case 3 -> AssemblyMachineBlockEntity.this.workTime;
                case 4 -> AssemblyMachineBlockEntity.this.completedCycles;
                case 5 -> AssemblyMachineBlockEntity.this.currentDemand;
                case 6 -> (int) AssemblyMachineBlockEntity.this.energyCapacity;
                case 7 -> AssemblyMachineBlockEntity.this.hasRecipe ? 1 : 0;
                case 8 -> AssemblyMachineBlockEntity.this.selectedRecipeMenuValue();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 9 && index < DATA_COUNT) {
                int tankDataIndex = index - 9;
                HbmFluidTank tank = tankByFlatIndex(tankDataIndex / 3);
                switch (tankDataIndex % 3) {
                    case 0 -> tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                    case 1 -> tank.setAmount(value);
                    case 2 -> tank.setPressure(value);
                    default -> {
                    }
                }
                return;
            }

            switch (index) {
                case 0 -> AssemblyMachineBlockEntity.this.energyStored = value;
                case 1 -> AssemblyMachineBlockEntity.this.lastInput = value;
                case 2 -> AssemblyMachineBlockEntity.this.progress = value;
                case 3 -> AssemblyMachineBlockEntity.this.workTime = Math.max(1, value);
                case 4 -> AssemblyMachineBlockEntity.this.completedCycles = value;
                case 5 -> AssemblyMachineBlockEntity.this.currentDemand = Math.max(1, value);
                case 6 -> AssemblyMachineBlockEntity.this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, value);
                case 7 -> AssemblyMachineBlockEntity.this.hasRecipe = value != 0;
                case 8 -> AssemblyMachineBlockEntity.this.setSelectedRecipeByMenuValue(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AssemblyMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ASSEMBLY_MACHINE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AssemblyMachineBlockEntity blockEntity) {
        PowerNetworkManager.tickFromEndpoint(level, blockEntity);
        blockEntity.tickWork(level);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        BlockPos pos = this.worldPosition;
        return List.of(
                pos.offset(2, 0, -1).immutable(),
                pos.offset(2, 0, 0).immutable(),
                pos.offset(2, 0, 1).immutable(),
                pos.offset(-2, 0, -1).immutable(),
                pos.offset(-2, 0, 0).immutable(),
                pos.offset(-2, 0, 1).immutable(),
                pos.offset(-1, 0, 2).immutable(),
                pos.offset(0, 0, 2).immutable(),
                pos.offset(1, 0, 2).immutable(),
                pos.offset(-1, 0, -2).immutable(),
                pos.offset(0, 0, -2).immutable(),
                pos.offset(1, 0, -2).immutable()
        );
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new AssemblyMachineFluidHandler();
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (this.energyStored >= this.energyCapacity) {
            return 0L;
        }
        long requested = Math.max(100L, this.currentDemand);
        return Math.min(requested, this.energyCapacity - this.energyStored);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.energyStored = Math.min(this.energyCapacity, this.energyStored + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.workTime <= 0 ? 0 : this.progress * 100 / this.workTime;
        return Component.translatable(
                "message.reinhardtshbm.power.assembly_machine",
                this.lastInput,
                this.currentDemand,
                this.energyStored,
                this.energyCapacity,
                percent,
                this.completedCycles
        );
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            if (isInputSlot(slot)) {
                this.progress = 0;
            }
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        if (isInputSlot(slot)) {
            this.progress = 0;
        }
        if (slot == BLUEPRINT_SLOT) {
            this.progress = 0;
            clearSelectionIfBlueprintNoLongerAllowsIt();
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot == BLUEPRINT_SLOT) {
            return isBlueprint(stack);
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return isSupportedUpgrade(stack);
        }
        if (isInputSlot(slot)) {
            return canAcceptInput(slot, stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot != OUTPUT_SLOT && slot != BLUEPRINT_SLOT && (slot < UPGRADE_START || slot >= UPGRADE_END) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT || isSlotClogged(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.progress = 0;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.assembly_machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AssemblyMachineMenu(containerId, playerInventory, this, this.menuData);
    }

    public boolean canAcceptInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            if (canAcceptInput(slot, stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAcceptInput(int slot, ItemStack stack) {
        if (!isInputSlot(slot) || stack.isEmpty() || this.level == null) {
            return false;
        }
        Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = selectedRecipe(this.level);
        if (selectedRecipe.isEmpty()) {
            return false;
        }

        int inputIndex = slot - INPUT_START;
        List<AssemblyMachineRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().ingredients();
        if (inputIndex < 0 || inputIndex >= ingredients.size()) {
            return false;
        }
        return ingredients.get(inputIndex).ingredient().test(stack);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            drop(level, pos, this.items.get(slot));
            this.items.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastInput", this.lastInput);
        tag.putLong("EnergyCapacity", this.energyCapacity);
        tag.putInt("Progress", this.progress);
        tag.putInt("WorkTime", this.workTime);
        tag.putInt("CurrentDemand", this.currentDemand);
        tag.putInt("CompletedCycles", this.completedCycles);
        if (this.selectedRecipeId != null) {
            tag.putString("SelectedRecipe", this.selectedRecipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.energyStored = tag.getLong("EnergyStored");
        this.lastInput = tag.getLong("LastInput");
        this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, tag.getLong("EnergyCapacity"));
        this.progress = tag.getInt("Progress");
        this.workTime = Math.max(1, tag.getInt("WorkTime"));
        this.currentDemand = Math.max(1, tag.getInt("CurrentDemand"));
        this.completedCycles = tag.getInt("CompletedCycles");
        this.selectedRecipeId = tag.contains("SelectedRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tickWork(Level level) {
        Optional<RecipeHolder<AssemblyMachineRecipe>> recipeHolder = getSelectedRecipe(level);
        this.hasRecipe = recipeHolder.isPresent();
        this.energyStored = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.energyStored, this.energyCapacity);
        if (recipeHolder.isEmpty()) {
            this.progress = 0;
            this.workTime = 100;
            this.currentDemand = 100;
            this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, this.energyStored);
            stopWorkingSounds(level);
            setLit(false);
            return;
        }

        AssemblyMachineRecipe recipe = recipeHolder.get().value();
        setupTanks(recipe);
        this.workTime = currentWorkTime(recipe);
        this.currentDemand = currentDemand(recipe);
        this.energyCapacity = Math.max(Math.max(BASE_ENERGY_CAPACITY, recipe.power() * 100L), this.energyStored);

        if (!canProcess(recipe)) {
            stopWorkingSounds(level);
            setLit(false);
            return;
        }

        if (this.energyStored < this.currentDemand) {
            stopWorkingSounds(level);
            setLit(false);
            return;
        }

        this.energyStored -= this.currentDemand;
        this.progress++;
        playWorkingSounds(level);
        setLit(true);

        if (this.progress >= this.workTime) {
            finishRecipe(recipe);
            this.progress = 0;
            this.completedCycles++;
        }
        setChanged();
    }

    private Optional<RecipeHolder<AssemblyMachineRecipe>> getSelectedRecipe(Level level) {
        if (this.selectedRecipeId != null) {
            Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = findSelectedRecipe(level);
            if (selectedRecipe.isEmpty() || !selectedRecipe.get().value().isVisibleForPool(installedBlueprintPool())) {
                this.selectedRecipeId = null;
                setChanged();
                return Optional.empty();
            }
            return selectedRecipe;
        }
        return Optional.empty();
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return Optional.ofNullable(this.selectedRecipeId);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId != null && this.level != null && findRecipe(this.level, recipeId).isEmpty()) {
            recipeId = null;
        }
        if (recipeId != null && this.level != null) {
            Optional<RecipeHolder<AssemblyMachineRecipe>> recipe = findRecipe(this.level, recipeId);
            if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool())) {
                recipeId = null;
            }
        }
        if (this.selectedRecipeId == recipeId || (this.selectedRecipeId != null && this.selectedRecipeId.equals(recipeId))) {
            return;
        }
        this.selectedRecipeId = recipeId;
        this.progress = 0;
        setChangedAndSync();
    }

    public Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe(Level level) {
        if (this.selectedRecipeId == null) {
            return Optional.empty();
        }
        return findRecipe(level, this.selectedRecipeId)
                .filter(holder -> holder.value().isVisibleForPool(installedBlueprintPool()));
    }

    private Optional<RecipeHolder<AssemblyMachineRecipe>> findSelectedRecipe(Level level) {
        return this.selectedRecipeId == null
                ? Optional.empty()
                : findRecipe(level, this.selectedRecipeId).filter(holder -> holder.value().isVisibleForPool(installedBlueprintPool()));
    }

    private Optional<RecipeHolder<AssemblyMachineRecipe>> findRecipe(Level level, ResourceLocation recipeId) {
        for (RecipeHolder<AssemblyMachineRecipe> holder : availableRecipes(level)) {
            if (holder.id().equals(recipeId)) {
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }

    private int selectedRecipeMenuValue() {
        if (this.selectedRecipeId == null || this.level == null) {
            return 0;
        }
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = availableRecipes(this.level);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(this.selectedRecipeId)) {
                return index + 1;
            }
        }
        return 0;
    }

    private void setSelectedRecipeByMenuValue(int value) {
        if (value <= 0 || this.level == null) {
            this.selectedRecipeId = null;
            return;
        }
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = availableRecipes(this.level);
        int index = value - 1;
        this.selectedRecipeId = index >= 0 && index < recipes.size() ? recipes.get(index).id() : null;
    }

    public List<RecipeHolder<AssemblyMachineRecipe>> availableRecipes(Level level) {
        List<RecipeHolder<AssemblyMachineRecipe>> visibleRecipes = level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(installedBlueprintPool()))
                .toList();
        return AssemblyMachineRecipe.activeVariants(visibleRecipes);
    }

    private Optional<String> installedBlueprintPool() {
        return BlueprintItem.pool(this.items.get(BLUEPRINT_SLOT));
    }

    private List<ItemStack> inputStacks() {
        List<ItemStack> stacks = new ArrayList<>(INPUT_END - INPUT_START);
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            stacks.add(this.items.get(slot));
        }
        return stacks;
    }

    private List<HbmFluidStack> inputFluidStacks() {
        return List.of(new HbmFluidStack(this.inputTank.type(), this.inputTank.amount(), this.inputTank.pressure()));
    }

    private int currentWorkTime(AssemblyMachineRecipe recipe) {
        double speed = 1.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3) / 3.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3);
        return Math.max(1, (int) Math.ceil(recipe.duration() / speed));
    }

    private int currentDemand(AssemblyMachineRecipe recipe) {
        double multiplier = 1.0D;
        multiplier -= Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3) * 0.25D;
        multiplier += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        multiplier += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) * 10.0D / 3.0D;
        return Math.max(1, (int) Math.ceil(recipe.power() * Math.max(0.25D, multiplier)));
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return level;
    }

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void finishRecipe(AssemblyMachineRecipe recipe) {
        consumeInputs(recipe);
        ItemStack result = recipe.result().copy();
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }
        produceFluidOutputs(recipe);
    }

    private void consumeInputs(AssemblyMachineRecipe recipe) {
        List<AssemblyMachineRecipe.CountedIngredient> ingredients = recipe.ingredients();
        int limit = Math.min(ingredients.size(), INPUT_END - INPUT_START);
        for (int inputIndex = 0; inputIndex < limit; inputIndex++) {
            int slot = INPUT_START + inputIndex;
            AssemblyMachineRecipe.CountedIngredient ingredient = ingredients.get(inputIndex);
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                stack.shrink(ingredient.count());
                if (stack.isEmpty()) {
                    this.items.set(slot, ItemStack.EMPTY);
                }
            }
        }

        if (!recipe.inputFluids().isEmpty()) {
            AssemblyMachineRecipe.AssemblyFluidStack input = recipe.inputFluids().getFirst();
            this.inputTank.drain(input.type(), input.amount(), false);
        }
    }

    private void produceFluidOutputs(AssemblyMachineRecipe recipe) {
        if (recipe.outputFluids().isEmpty()) {
            return;
        }
        AssemblyMachineRecipe.AssemblyFluidStack output = recipe.outputFluids().getFirst();
        this.outputTank.fill(output.type(), output.amount(), output.pressure(), false);
    }

    private void setupTanks(AssemblyMachineRecipe recipe) {
        if (!recipe.inputFluids().isEmpty()) {
            AssemblyMachineRecipe.AssemblyFluidStack input = recipe.inputFluids().getFirst();
            this.inputTank.conform(input.type(), input.pressure());
        } else if (this.inputTank.amount() == 0) {
            this.inputTank.clear();
        }

        if (!recipe.outputFluids().isEmpty()) {
            AssemblyMachineRecipe.AssemblyFluidStack output = recipe.outputFluids().getFirst();
            this.outputTank.conform(output.type(), output.pressure());
        } else if (this.outputTank.amount() == 0) {
            this.outputTank.clear();
        }
    }

    private boolean canProcess(AssemblyMachineRecipe recipe) {
        if (!recipe.matches(new AssemblyMachineRecipe.Input(inputStacks(), inputFluidStacks()), this.level)) {
            return false;
        }
        return canOutput(recipe.result()) && canFitFluidOutput(recipe);
    }

    private boolean canFitFluidOutput(AssemblyMachineRecipe recipe) {
        if (recipe.outputFluids().size() > 1) {
            return false;
        }
        if (recipe.outputFluids().isEmpty()) {
            return true;
        }
        AssemblyMachineRecipe.AssemblyFluidStack output = recipe.outputFluids().getFirst();
        return this.outputTank.type() == output.type()
                && this.outputTank.pressure() == output.pressure()
                && this.outputTank.amount() + output.amount() <= this.outputTank.capacity();
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }

        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof AssemblyMachineBlock && state.getValue(AssemblyMachineBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(AssemblyMachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void playWorkingSounds(Level level) {
        if (!this.wasWorking) {
            level.playSound(
                    null,
                    this.worldPosition,
                    HbmSoundEvents.ASSEMBLER_START.get(),
                    SoundSource.BLOCKS,
                    0.25F,
                    1.25F + level.random.nextFloat() * 0.25F
            );
            this.wasWorking = true;
            this.motorSoundCycle = 0;
            this.strikeSoundCycle = 0;
        }

        if (this.motorSoundCycle <= 0) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.ASSEMBLER_MOTOR.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
            this.motorSoundCycle = 20;
        }
        this.motorSoundCycle--;

        if (this.strikeSoundCycle <= 0) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.ASSEMBLER_STRIKE.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            this.strikeSoundCycle = 8 + level.random.nextInt(10);
        }
        this.strikeSoundCycle--;
    }

    private void stopWorkingSounds(Level level) {
        if (!this.wasWorking) {
            return;
        }

        level.playSound(null, this.worldPosition, HbmSoundEvents.ASSEMBLER_STOP.get(), SoundSource.BLOCKS, 0.25F, 1.5F);
        this.wasWorking = false;
        this.motorSoundCycle = 0;
        this.strikeSoundCycle = 0;
    }

    public void updateClientAnimation() {
        if (this.level == null || !this.level.isClientSide) {
            return;
        }

        long gameTime = this.level.getGameTime();
        if (this.lastClientAnimationTick == gameTime) {
            return;
        }
        if (this.lastClientAnimationTick == Long.MIN_VALUE || gameTime % 20L == 0L) {
            this.clientFrame = !this.level.getBlockState(this.worldPosition.above(3)).isAir();
        }

        int steps = this.lastClientAnimationTick == Long.MIN_VALUE
                ? 1
                : (int) Math.min(5L, Math.max(1L, gameTime - this.lastClientAnimationTick));
        for (int step = 0; step < steps; step++) {
            stepClientAnimation();
        }
        this.lastClientAnimationTick = gameTime;
    }

    public boolean clientFrame() {
        return this.clientFrame;
    }

    public double clientRing(float partialTick) {
        return lerp(this.clientPrevRing, this.clientRing, partialTick);
    }

    public double[] clientArmPositions(int index, float partialTick) {
        return this.clientArms[index].positions(partialTick);
    }

    private void stepClientAnimation() {
        boolean working = this.getBlockState().hasProperty(AssemblyMachineBlock.LIT)
                && this.getBlockState().getValue(AssemblyMachineBlock.LIT);

        for (AssemblerArm arm : this.clientArms) {
            arm.updateInterp();
            if (working) {
                arm.updateArm();
            } else {
                arm.returnToNullPos();
            }
        }

        this.clientPrevRing = this.clientRing;
        if (!working || this.level == null) {
            return;
        }

        if (this.clientRing != this.clientRingTarget) {
            double ringDelta = Math.abs(this.clientRingTarget - this.clientRing);
            if (ringDelta <= this.clientRingSpeed) {
                this.clientRing = this.clientRingTarget;
            }
            if (this.clientRingTarget > this.clientRing) {
                this.clientRing += this.clientRingSpeed;
            }
            if (this.clientRingTarget < this.clientRing) {
                this.clientRing -= this.clientRingSpeed;
            }
            if (this.clientRingTarget == this.clientRing) {
                if (this.clientRingTarget >= 360.0D) {
                    this.clientRingTarget -= 360.0D;
                    this.clientRing -= 360.0D;
                    this.clientPrevRing -= 360.0D;
                }
                if (this.clientRingTarget <= -360.0D) {
                    this.clientRingTarget += 360.0D;
                    this.clientRing += 360.0D;
                    this.clientPrevRing += 360.0D;
                }
                this.clientRingDelay = 20 + this.level.random.nextInt(21);
            }
        } else {
            if (this.clientRingDelay > 0) {
                this.clientRingDelay--;
            }
            if (this.clientRingDelay <= 0) {
                this.clientRingTarget += (this.level.random.nextDouble() * 2.0D - 1.0D) * 135.0D;
                this.clientRingSpeed = 10.0D + this.level.random.nextDouble() * 5.0D;
            }
        }
    }

    public static boolean isBlueprint(ItemStack stack) {
        return BlueprintItem.isBlueprint(stack);
    }

    private void clearSelectionIfBlueprintNoLongerAllowsIt() {
        if (this.level == null || this.selectedRecipeId == null) {
            return;
        }
        Optional<RecipeHolder<AssemblyMachineRecipe>> recipe = findRecipe(this.level, this.selectedRecipeId);
        if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool())) {
            this.selectedRecipeId = null;
        }
    }

    private static boolean isSupportedUpgrade(ItemStack stack) {
        if (!MachineUpgradeItem.isMachineUpgrade(stack)) {
            return false;
        }
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED
                || type == MachineUpgradeItem.UpgradeType.POWER
                || type == MachineUpgradeItem.UpgradeType.OVERDRIVE;
    }

    private static boolean isInputSlot(int slot) {
        return slot >= INPUT_START && slot < INPUT_END;
    }

    private boolean isSlotClogged(int slot) {
        if (!isInputSlot(slot)) {
            return false;
        }
        ItemStack stack = this.items.get(slot);
        return !stack.isEmpty() && !canAcceptInput(slot, stack);
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private HbmFluidTank tankByFlatIndex(int tank) {
        return tank <= 0 ? this.inputTank : this.outputTank;
    }

    private static int[] createAutomationSlots() {
        int[] slots = new int[(INPUT_END - INPUT_START) + 1];
        int index = 0;
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            slots[index++] = slot;
        }
        slots[index] = OUTPUT_SLOT;
        return slots;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack.copy()
        ));
    }

    private static double lerp(double previous, double current, float partialTick) {
        return previous + (current - previous) * partialTick;
    }

    public static class AssemblerArm {
        private static final double[][] POSITIONS = {
                {45.0D, -15.0D, -5.0D},
                {15.0D, 15.0D, -15.0D},
                {25.0D, 10.0D, -15.0D},
                {30.0D, 0.0D, -10.0D},
                {70.0D, -10.0D, -25.0D}
        };

        private final double[] angles = new double[4];
        private final double[] prevAngles = new double[4];
        private final double[] targetAngles = new double[4];
        private final double[] speed = new double[4];
        private final Random random = new Random();
        private ArmActionState state = ArmActionState.ASSUME_POSITION;
        private int actionDelay;

        AssemblerArm() {
            resetSpeed();
        }

        private void updateInterp() {
            System.arraycopy(this.angles, 0, this.prevAngles, 0, this.angles.length);
        }

        private void returnToNullPos() {
            for (int index = 0; index < 4; index++) {
                this.targetAngles[index] = 0.0D;
            }
            for (int index = 0; index < 3; index++) {
                this.speed[index] = 3.0D;
            }
            this.speed[3] = 0.25D;
            this.state = ArmActionState.RETRACT_STRIKER;
            move();
        }

        private void resetSpeed() {
            this.speed[0] = 15.0D;
            this.speed[1] = 15.0D;
            this.speed[2] = 15.0D;
            this.speed[3] = 0.5D;
        }

        private void updateArm() {
            resetSpeed();
            if (this.actionDelay > 0) {
                this.actionDelay--;
                return;
            }

            switch (this.state) {
                case ASSUME_POSITION -> {
                    if (move()) {
                        this.actionDelay = 2;
                        this.state = ArmActionState.EXTEND_STRIKER;
                        this.targetAngles[3] = -0.75D;
                    }
                }
                case EXTEND_STRIKER -> {
                    if (move()) {
                        this.state = ArmActionState.RETRACT_STRIKER;
                        this.targetAngles[3] = 0.0D;
                    }
                }
                case RETRACT_STRIKER -> {
                    if (move()) {
                        this.actionDelay = 2 + this.random.nextInt(5);
                        chooseNewArmPosition();
                        this.state = ArmActionState.ASSUME_POSITION;
                    }
                }
            }
        }

        private void chooseNewArmPosition() {
            int chosen = this.random.nextInt(POSITIONS.length);
            this.targetAngles[0] = POSITIONS[chosen][0];
            this.targetAngles[1] = POSITIONS[chosen][1];
            this.targetAngles[2] = POSITIONS[chosen][2];
        }

        private boolean move() {
            boolean didMove = false;
            for (int index = 0; index < this.angles.length; index++) {
                if (this.angles[index] == this.targetAngles[index]) {
                    continue;
                }

                didMove = true;
                double delta = Math.abs(this.angles[index] - this.targetAngles[index]);
                if (delta <= this.speed[index]) {
                    this.angles[index] = this.targetAngles[index];
                    continue;
                }

                if (this.angles[index] < this.targetAngles[index]) {
                    this.angles[index] += this.speed[index];
                } else {
                    this.angles[index] -= this.speed[index];
                }
            }
            return !didMove;
        }

        private double[] positions(float partialTick) {
            return new double[] {
                    lerp(this.prevAngles[0], this.angles[0], partialTick),
                    lerp(this.prevAngles[1], this.angles[1], partialTick),
                    lerp(this.prevAngles[2], this.angles[2], partialTick),
                    lerp(this.prevAngles[3], this.angles[3], partialTick)
            };
        }

        private enum ArmActionState {
            ASSUME_POSITION,
            EXTEND_STRIKER,
            RETRACT_STRIKER
        }
    }

    private final class AssemblyMachineFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> inputTank.getFluidInTank(0);
                case 1 -> outputTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 || tank == 1 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty() || inputTank.type().isNone() || inputTank.pressure() != 0) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return !fluid.isNone() && inputTank.type() == fluid;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || inputTank.type().isNone() || inputTank.pressure() != 0) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone() || inputTank.type() != fluid) {
                return 0;
            }
            int filled = inputTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                setChangedAndSync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || outputTank.type().isNone() || outputTank.pressure() != 0) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone() || outputTank.type() != fluid) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = outputTank.drain(fluid, resource.getAmount(), action.simulate());
            if (drained.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                setChangedAndSync();
            }
            return HbmFluids.toNeoStack(fluid, drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || outputTank.type().isNone() || outputTank.pressure() != 0) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = outputTank.type();
            HbmFluidStack drained = outputTank.drain(fluid, maxDrain, action.simulate());
            if (drained.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                setChangedAndSync();
            }
            return HbmFluids.toNeoStack(fluid, drained.amount());
        }
    }
}
