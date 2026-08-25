package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.ChemicalPlantBlock;
import com.reinhardt.hbm.client.sound.ChemicalPlantClientSounds;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.ChemicalPlantMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
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

public class ChemicalPlantBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 4;
    public static final int SOLID_INPUT_START = 4;
    public static final int SOLID_INPUT_END = 7;
    public static final int SOLID_OUTPUT_START = 7;
    public static final int SOLID_OUTPUT_END = 10;
    public static final int FLUID_INPUT_CONTAINER_START = 10;
    public static final int FLUID_INPUT_CONTAINER_RESULT_START = 13;
    public static final int FLUID_OUTPUT_CONTAINER_START = 16;
    public static final int FLUID_OUTPUT_CONTAINER_RESULT_START = 19;
    public static final int SLOT_COUNT = 22;
    public static final int TANK_COUNT = 3;
    public static final int TANK_CAPACITY = 24_000;
    public static final int DATA_COUNT = 28;
    public static final long BASE_ENERGY_CAPACITY = 100_000L;

    private static final int[] AUTOMATION_SLOTS = {4, 5, 6, 7, 8, 9};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank[] inputTanks = new HbmFluidTank[TANK_COUNT];
    private final HbmFluidTank[] outputTanks = new HbmFluidTank[TANK_COUNT];
    private long energyStored;
    private long lastInput;
    private long energyCapacity = BASE_ENERGY_CAPACITY;
    private int progress;
    private int workTime = 100;
    private int currentDemand = 100;
    private int completedCycles;
    private boolean hasRecipe;
    private boolean didProcess;
    private double clientPrevAnim;
    private double clientAnim;
    private boolean clientFrame;
    private long lastClientAnimationTick = Long.MIN_VALUE;
    @Nullable
    private ResourceLocation selectedRecipeId;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 10 && index < DATA_COUNT) {
                int tankDataIndex = index - 10;
                HbmFluidTank tank = tankByFlatIndex(tankDataIndex / 3);
                return switch (tankDataIndex % 3) {
                    case 0 -> tank.type().oldId();
                    case 1 -> tank.amount();
                    case 2 -> tank.pressure();
                    default -> 0;
                };
            }

            return switch (index) {
                case 0 -> (int) ChemicalPlantBlockEntity.this.energyStored;
                case 1 -> (int) ChemicalPlantBlockEntity.this.lastInput;
                case 2 -> ChemicalPlantBlockEntity.this.progress;
                case 3 -> ChemicalPlantBlockEntity.this.workTime;
                case 4 -> ChemicalPlantBlockEntity.this.completedCycles;
                case 5 -> ChemicalPlantBlockEntity.this.currentDemand;
                case 6 -> (int) ChemicalPlantBlockEntity.this.energyCapacity;
                case 7 -> ChemicalPlantBlockEntity.this.hasRecipe ? 1 : 0;
                case 8 -> ChemicalPlantBlockEntity.this.selectedRecipeMenuValue();
                case 9 -> ChemicalPlantBlockEntity.this.didProcess ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 10 && index < DATA_COUNT) {
                int tankDataIndex = index - 10;
                HbmFluidTank tank = tankByFlatIndex(tankDataIndex / 3);
                switch (tankDataIndex % 3) {
                    case 0 -> tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                    case 2 -> tank.setPressure(value);
                    default -> {
                    }
                }
                return;
            }

            switch (index) {
                case 0 -> ChemicalPlantBlockEntity.this.energyStored = value;
                case 1 -> ChemicalPlantBlockEntity.this.lastInput = value;
                case 2 -> ChemicalPlantBlockEntity.this.progress = value;
                case 3 -> ChemicalPlantBlockEntity.this.workTime = Math.max(1, value);
                case 4 -> ChemicalPlantBlockEntity.this.completedCycles = value;
                case 5 -> ChemicalPlantBlockEntity.this.currentDemand = Math.max(1, value);
                case 6 -> ChemicalPlantBlockEntity.this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, value);
                case 7 -> ChemicalPlantBlockEntity.this.hasRecipe = value != 0;
                case 8 -> ChemicalPlantBlockEntity.this.setSelectedRecipeByMenuValue(value);
                case 9 -> ChemicalPlantBlockEntity.this.didProcess = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ChemicalPlantBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.CHEMICAL_PLANT.get(), pos, blockState);
        for (int index = 0; index < TANK_COUNT; index++) {
            this.inputTanks[index] = new HbmFluidTank(TANK_CAPACITY);
            this.outputTanks[index] = new HbmFluidTank(TANK_CAPACITY);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChemicalPlantBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, blockEntity);
        blockEntity.tickServer(level);
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
                "message.reinhardtshbm.power.chemical_plant",
                this.lastInput,
                this.currentDemand,
                this.energyStored,
                this.energyCapacity,
                percent,
                this.completedCycles
        );
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new ChemicalPlantFluidHandler();
    }

    public HbmFluidTank inputTank(int index) {
        return index >= 0 && index < TANK_COUNT ? this.inputTanks[index] : this.inputTanks[0];
    }

    public HbmFluidTank outputTank(int index) {
        return index >= 0 && index < TANK_COUNT ? this.outputTanks[index] : this.outputTanks[0];
    }

    public ContainerData getMenuData() {
        return this.menuData;
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
            if (isSolidInputSlot(slot)) {
                this.progress = 0;
            }
            setChangedAndSync(false);
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
        if (isSolidInputSlot(slot)) {
            this.progress = 0;
        }
        if (slot == BLUEPRINT_SLOT) {
            this.progress = 0;
            clearSelectionIfBlueprintNoLongerAllowsIt();
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack) || isMachinedMeteoriteSword(stack);
        }
        if (slot == BLUEPRINT_SLOT) {
            return isBlueprint(stack);
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return isSupportedUpgrade(stack);
        }
        if (isSolidInputSlot(slot)) {
            return canAcceptInput(slot, stack);
        }
        if (slot >= FLUID_INPUT_CONTAINER_START && slot < FLUID_INPUT_CONTAINER_START + TANK_COUNT) {
            return isDrainableContainerForInput(slot - FLUID_INPUT_CONTAINER_START, stack);
        }
        if (slot >= FLUID_OUTPUT_CONTAINER_START && slot < FLUID_OUTPUT_CONTAINER_START + TANK_COUNT) {
            return isFillableContainerForOutput(slot - FLUID_OUTPUT_CONTAINER_START, stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isSolidInputSlot(slot) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot >= SOLID_OUTPUT_START && slot < SOLID_OUTPUT_END) || isSlotClogged(slot);
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
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.chemical_plant");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChemicalPlantMenu(containerId, playerInventory, this, this.menuData);
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
        for (int index = 0; index < TANK_COUNT; index++) {
            tag.put("InputTank" + index, this.inputTanks[index].save());
            tag.put("OutputTank" + index, this.outputTanks[index].save());
        }
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastInput", this.lastInput);
        tag.putLong("EnergyCapacity", this.energyCapacity);
        tag.putInt("Progress", this.progress);
        tag.putInt("WorkTime", this.workTime);
        tag.putInt("CurrentDemand", this.currentDemand);
        tag.putInt("CompletedCycles", this.completedCycles);
        tag.putBoolean("DidProcess", this.didProcess);
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
        for (int index = 0; index < TANK_COUNT; index++) {
            this.inputTanks[index].load(tag.getCompound("InputTank" + index));
            this.outputTanks[index].load(tag.getCompound("OutputTank" + index));
        }
        this.energyStored = tag.getLong("EnergyStored");
        this.lastInput = tag.getLong("LastInput");
        this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, tag.getLong("EnergyCapacity"));
        this.progress = tag.getInt("Progress");
        this.workTime = Math.max(1, tag.getInt("WorkTime"));
        this.currentDemand = Math.max(1, tag.getInt("CurrentDemand"));
        this.completedCycles = tag.getInt("CompletedCycles");
        this.didProcess = tag.getBoolean("DidProcess");
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

    public boolean canAcceptInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int slot = SOLID_INPUT_START; slot < SOLID_INPUT_END; slot++) {
            if (canAcceptInput(slot, stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAcceptInput(int slot, ItemStack stack) {
        if (!isSolidInputSlot(slot) || stack.isEmpty() || this.level == null) {
            return false;
        }
        Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = selectedRecipe(this.level);
        if (selectedRecipe.isEmpty()) {
            return false;
        }
        int inputIndex = slot - SOLID_INPUT_START;
        List<ChemicalPlantRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().inputItems();
        if (inputIndex < 0 || inputIndex >= ingredients.size()) {
            return false;
        }
        return ingredients.get(inputIndex).ingredient().test(stack);
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return Optional.ofNullable(this.selectedRecipeId);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId != null && this.level != null && findRecipe(this.level, recipeId).isEmpty()) {
            recipeId = null;
        }
        if (recipeId != null && this.level != null) {
            Optional<RecipeHolder<ChemicalPlantRecipe>> recipe = findRecipe(this.level, recipeId);
            if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool())) {
                recipeId = null;
            }
        }
        if (this.selectedRecipeId == recipeId || (this.selectedRecipeId != null && this.selectedRecipeId.equals(recipeId))) {
            return;
        }
        this.selectedRecipeId = recipeId;
        this.progress = 0;
        setChangedAndSync(true);
    }

    public Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe(Level level) {
        if (this.selectedRecipeId == null) {
            return Optional.empty();
        }
        return findRecipe(level, this.selectedRecipeId)
                .filter(holder -> holder.value().isVisibleForPool(installedBlueprintPool()));
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
            this.clientPrevAnim = this.clientAnim;
            if (isClientWorking()) {
                this.clientAnim++;
            }
        }
        this.lastClientAnimationTick = gameTime;
    }

    public boolean clientFrame() {
        return this.clientFrame;
    }

    public double clientAnim(float partialTick) {
        return this.clientPrevAnim + (this.clientAnim - this.clientPrevAnim) * partialTick;
    }

    public int clientFluidColor() {
        if (this.level == null) {
            return 0xFFFFFFFF;
        }
        Optional<RecipeHolder<ChemicalPlantRecipe>> holder = selectedRecipe(this.level);
        if (holder.isEmpty()) {
            return 0xFFFFFFFF;
        }
        List<ChemicalPlantRecipe.ChemicalFluidStack> fluids = !holder.get().value().outputFluids().isEmpty()
                ? holder.get().value().outputFluids()
                : holder.get().value().inputFluids();
        if (fluids.isEmpty()) {
            return 0xFFFFFFFF;
        }

        int red = 0;
        int green = 0;
        int blue = 0;
        int colors = 0;
        for (ChemicalPlantRecipe.ChemicalFluidStack stack : fluids) {
            if (stack.isEmpty()) {
                continue;
            }
            red += (stack.type().color() >> 16) & 0xFF;
            green += (stack.type().color() >> 8) & 0xFF;
            blue += stack.type().color() & 0xFF;
            colors++;
        }
        if (colors <= 0) {
            return 0xFFFFFFFF;
        }
        return 0x80000000 | ((red / colors) << 16) | ((green / colors) << 8) | (blue / colors);
    }

    private void tickServer(Level level) {
        Optional<RecipeHolder<ChemicalPlantRecipe>> recipeHolder = getSelectedRecipe(level);
        this.hasRecipe = recipeHolder.isPresent();
        this.didProcess = false;
        this.energyStored = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.energyStored, this.energyCapacity);

        if (recipeHolder.isEmpty()) {
            this.progress = 0;
            this.workTime = 100;
            this.currentDemand = 100;
            this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, this.energyStored);
            setLit(false);
            setChangedAndSync(level.getGameTime() % 20L == 0L);
            return;
        }

        ChemicalPlantRecipe recipe = recipeHolder.get().value();
        setupTanks(recipe);
        tickContainers(recipe);
        this.workTime = currentWorkTime(recipe);
        this.currentDemand = currentDemand(recipe);
        this.energyCapacity = Math.max(Math.max(BASE_ENERGY_CAPACITY, recipe.power() * 100L), this.energyStored);

        if (!canProcess(recipe) || this.energyStored < this.currentDemand) {
            this.progress = 0;
            setLit(false);
            setChangedAndSync(level.getGameTime() % 20L == 0L);
            return;
        }

        this.energyStored -= this.currentDemand;
        this.progress++;
        this.didProcess = true;
        setLit(true);

        if (this.progress >= this.workTime) {
            finishRecipe(recipe);
            this.progress = 0;
            this.completedCycles++;
            treatMeteoriteSword();
        }

        setChangedAndSync(level.getGameTime() % 10L == 0L);
    }

    private void tickClient() {
        ChemicalPlantClientSounds.tick(this);
    }

    private Optional<RecipeHolder<ChemicalPlantRecipe>> getSelectedRecipe(Level level) {
        if (this.selectedRecipeId == null) {
            return Optional.empty();
        }
        Optional<RecipeHolder<ChemicalPlantRecipe>> recipe = findRecipe(level, this.selectedRecipeId);
        if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool())) {
            this.selectedRecipeId = null;
            return Optional.empty();
        }
        return recipe;
    }

    private Optional<RecipeHolder<ChemicalPlantRecipe>> findRecipe(Level level, ResourceLocation recipeId) {
        for (RecipeHolder<ChemicalPlantRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get())) {
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
        List<RecipeHolder<ChemicalPlantRecipe>> recipes = availableRecipes(this.level);
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
        List<RecipeHolder<ChemicalPlantRecipe>> recipes = availableRecipes(this.level);
        int index = value - 1;
        this.selectedRecipeId = index >= 0 && index < recipes.size() ? recipes.get(index).id() : null;
    }

    public List<RecipeHolder<ChemicalPlantRecipe>> availableRecipes(Level level) {
        return level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(installedBlueprintPool()))
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    private Optional<String> installedBlueprintPool() {
        return BlueprintItem.pool(this.items.get(BLUEPRINT_SLOT));
    }

    private void setupTanks(ChemicalPlantRecipe recipe) {
        for (int index = 0; index < TANK_COUNT; index++) {
            if (index < recipe.inputFluids().size()) {
                ChemicalPlantRecipe.ChemicalFluidStack stack = recipe.inputFluids().get(index);
                this.inputTanks[index].conform(stack.type(), stack.pressure());
            } else if (this.inputTanks[index].amount() == 0) {
                this.inputTanks[index].clear();
            }

            if (index < recipe.outputFluids().size()) {
                ChemicalPlantRecipe.ChemicalFluidStack stack = recipe.outputFluids().get(index);
                this.outputTanks[index].conform(stack.type(), stack.pressure());
            } else if (this.outputTanks[index].amount() == 0) {
                this.outputTanks[index].clear();
            }
        }
    }

    private void tickContainers(ChemicalPlantRecipe recipe) {
        boolean changed = false;
        for (int index = 0; index < Math.min(TANK_COUNT, recipe.inputFluids().size()); index++) {
            changed |= drainContainerIntoInputTank(index);
        }
        for (int index = 0; index < TANK_COUNT; index++) {
            changed |= fillContainerFromOutputTank(index);
        }
        if (changed) {
            setChangedAndSync(true);
        }
    }

    private boolean drainContainerIntoInputTank(int index) {
        int inputSlot = FLUID_INPUT_CONTAINER_START + index;
        int resultSlot = FLUID_INPUT_CONTAINER_RESULT_START + index;
        ItemStack input = this.items.get(inputSlot);
        HbmFluidTank tank = this.inputTanks[index];
        if (input.isEmpty() || tank.type().isNone()) {
            return false;
        }
        boolean changed = HbmFluidContainerTransfer.drainIntoTank(
                input,
                tank,
                fluid -> fluid == tank.type(),
                output -> canPlaceOutput(resultSlot, output),
                output -> placeOutput(resultSlot, output)
        );
        if (input.isEmpty()) {
            this.items.set(inputSlot, ItemStack.EMPTY);
        }
        return changed;
    }

    private boolean fillContainerFromOutputTank(int index) {
        int inputSlot = FLUID_OUTPUT_CONTAINER_START + index;
        int resultSlot = FLUID_OUTPUT_CONTAINER_RESULT_START + index;
        ItemStack input = this.items.get(inputSlot);
        HbmFluidTank tank = this.outputTanks[index];
        boolean changed = HbmFluidContainerTransfer.fillFromTank(
                input,
                tank,
                output -> canPlaceOutput(resultSlot, output),
                output -> placeOutput(resultSlot, output)
        );
        if (input.isEmpty()) {
            this.items.set(inputSlot, ItemStack.EMPTY);
        }
        return changed;
    }

    private boolean canProcess(ChemicalPlantRecipe recipe) {
        if (!recipe.matches(new ChemicalPlantRecipe.Input(inputStacks(), inputFluidStacks()), this.level)) {
            return false;
        }
        return canFitItemOutputs(recipe) && canFitFluidOutputs(recipe);
    }

    private boolean canFitItemOutputs(ChemicalPlantRecipe recipe) {
        if (recipe.outputItems().size() > TANK_COUNT) {
            return false;
        }
        for (int index = 0; index < recipe.outputItems().size(); index++) {
            ItemStack result = recipe.outputItems().get(index);
            if (result.isEmpty()) {
                continue;
            }
            ItemStack current = this.items.get(SOLID_OUTPUT_START + index);
            if (current.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(current, result)
                    || current.getCount() + result.getCount() > current.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private boolean canFitFluidOutputs(ChemicalPlantRecipe recipe) {
        if (recipe.outputFluids().size() > TANK_COUNT) {
            return false;
        }
        for (int index = 0; index < recipe.outputFluids().size(); index++) {
            ChemicalPlantRecipe.ChemicalFluidStack output = recipe.outputFluids().get(index);
            HbmFluidTank tank = this.outputTanks[index];
            if (tank.type() != output.type() || tank.pressure() != output.pressure() || tank.amount() + output.amount() > tank.capacity()) {
                return false;
            }
        }
        return true;
    }

    private void finishRecipe(ChemicalPlantRecipe recipe) {
        consumeInputs(recipe);
        produceOutputs(recipe);
    }

    private void consumeInputs(ChemicalPlantRecipe recipe) {
        for (int index = 0; index < Math.min(recipe.inputItems().size(), TANK_COUNT); index++) {
            int slot = SOLID_INPUT_START + index;
            ChemicalPlantRecipe.CountedIngredient ingredient = recipe.inputItems().get(index);
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                stack.shrink(ingredient.count());
                if (stack.isEmpty()) {
                    this.items.set(slot, ItemStack.EMPTY);
                }
            }
        }

        for (int index = 0; index < Math.min(recipe.inputFluids().size(), TANK_COUNT); index++) {
            ChemicalPlantRecipe.ChemicalFluidStack input = recipe.inputFluids().get(index);
            this.inputTanks[index].drain(input.type(), input.amount(), false);
        }
    }

    private void produceOutputs(ChemicalPlantRecipe recipe) {
        for (int index = 0; index < Math.min(recipe.outputItems().size(), TANK_COUNT); index++) {
            ItemStack result = recipe.outputItems().get(index).copy();
            if (result.isEmpty()) {
                continue;
            }
            int slot = SOLID_OUTPUT_START + index;
            ItemStack current = this.items.get(slot);
            if (current.isEmpty()) {
                this.items.set(slot, result);
            } else {
                current.grow(result.getCount());
            }
        }

        for (int index = 0; index < Math.min(recipe.outputFluids().size(), TANK_COUNT); index++) {
            ChemicalPlantRecipe.ChemicalFluidStack output = recipe.outputFluids().get(index);
            this.outputTanks[index].fill(output.type(), output.amount(), output.pressure(), false);
        }
    }

    private int currentWorkTime(ChemicalPlantRecipe recipe) {
        double speed = 1.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3) / 3.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3);
        return Math.max(1, (int) Math.ceil(recipe.duration() / speed));
    }

    private int currentDemand(ChemicalPlantRecipe recipe) {
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

    public boolean isClientWorking() {
        return this.getBlockState().hasProperty(ChemicalPlantBlock.LIT)
                && this.getBlockState().getValue(ChemicalPlantBlock.LIT);
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }

        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof ChemicalPlantBlock && state.getValue(ChemicalPlantBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(ChemicalPlantBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private List<ItemStack> inputStacks() {
        List<ItemStack> stacks = new ArrayList<>(TANK_COUNT);
        for (int slot = SOLID_INPUT_START; slot < SOLID_INPUT_END; slot++) {
            stacks.add(this.items.get(slot));
        }
        return stacks;
    }

    private List<HbmFluidStack> inputFluidStacks() {
        List<HbmFluidStack> stacks = new ArrayList<>(TANK_COUNT);
        for (HbmFluidTank tank : this.inputTanks) {
            stacks.add(new HbmFluidStack(tank.type(), tank.amount(), tank.pressure()));
        }
        return stacks;
    }

    private boolean isDrainableContainerForInput(int index, ItemStack stack) {
        if (stack.isEmpty() || index < 0 || index >= TANK_COUNT) {
            return false;
        }
        HbmFluidTank tank = this.inputTanks[index];
        if (tank.type().isNone() || tank.pressure() != 0) {
            return false;
        }
        int resultSlot = FLUID_INPUT_CONTAINER_RESULT_START + index;
        return HbmFluidContainerTransfer.canDrainIntoTank(
                stack,
                tank,
                fluid -> fluid == tank.type(),
                output -> canPlaceOutput(resultSlot, output)
        );
    }

    private boolean isFillableContainerForOutput(int index, ItemStack stack) {
        if (stack.isEmpty() || index < 0 || index >= TANK_COUNT) {
            return false;
        }
        HbmFluidTank tank = this.outputTanks[index];
        int resultSlot = FLUID_OUTPUT_CONTAINER_RESULT_START + index;
        return HbmFluidContainerTransfer.canFillFromTank(
                stack,
                tank,
                output -> canPlaceOutput(resultSlot, output)
        );
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        ItemStack current = this.items.get(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
    }

    private boolean isSlotClogged(int slot) {
        if (!isSolidInputSlot(slot)) {
            return false;
        }
        ItemStack stack = this.items.get(slot);
        return !stack.isEmpty() && !canAcceptInput(slot, stack);
    }

    private HbmFluidTank tankByFlatIndex(int tank) {
        if (tank < TANK_COUNT) {
            return this.inputTanks[Math.max(0, tank)];
        }
        return this.outputTanks[Math.max(0, Math.min(TANK_COUNT - 1, tank - TANK_COUNT))];
    }

    private void treatMeteoriteSword() {
        ItemStack batterySlot = this.items.get(BATTERY_SLOT);
        if (isMachinedMeteoriteSword(batterySlot)) {
            this.items.set(BATTERY_SLOT, new ItemStack(HbmItems.METEORITE_SWORD_TREATED.get()));
        }
    }

    public static boolean isBlueprint(ItemStack stack) {
        return BlueprintItem.isBlueprint(stack);
    }

    private void clearSelectionIfBlueprintNoLongerAllowsIt() {
        if (this.level == null || this.selectedRecipeId == null) {
            return;
        }
        Optional<RecipeHolder<ChemicalPlantRecipe>> recipe = findRecipe(this.level, this.selectedRecipeId);
        if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool())) {
            this.selectedRecipeId = null;
        }
    }

    private static boolean isMachinedMeteoriteSword(ItemStack stack) {
        return !stack.isEmpty() && stack.is(HbmItems.METEORITE_SWORD_MACHINED.get());
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

    private static boolean isSolidInputSlot(int slot) {
        return slot >= SOLID_INPUT_START && slot < SOLID_INPUT_END;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (sync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
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

    private final class ChemicalPlantFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return TANK_COUNT * 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank >= 0 && tank < TANK_COUNT) {
                return inputTanks[tank].getFluidInTank(0);
            }
            int output = tank - TANK_COUNT;
            return output >= 0 && output < TANK_COUNT ? outputTanks[output].getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < TANK_COUNT * 2 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank < 0 || tank >= TANK_COUNT || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank input = inputTanks[tank];
            return !fluid.isNone() && input.type() == fluid;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                return 0;
            }
            int remaining = resource.getAmount();
            int filled = 0;
            for (HbmFluidTank tank : inputTanks) {
                if (remaining <= 0) {
                    break;
                }
                if (tank.type() != fluid) {
                    continue;
                }
                // The selected recipe configures the legacy tank pressure.
                // NeoForge FluidStack has no equivalent pressure field, so a
                // pipe must retain that configured value while filling it.
                int accepted = tank.fill(fluid, remaining, tank.pressure(), action.simulate());
                filled += accepted;
                remaining -= accepted;
            }
            if (filled > 0 && action.execute()) {
                setChangedAndSync(true);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                return FluidStack.EMPTY;
            }
            int remaining = resource.getAmount();
            int drained = 0;
            for (HbmFluidTank tank : outputTanks) {
                if (remaining <= 0) {
                    break;
                }
                if (tank.type() != fluid) {
                    continue;
                }
                HbmFluidStack stack = tank.drain(fluid, remaining, action.simulate());
                drained += stack.amount();
                remaining -= stack.amount();
            }
            if (drained > 0 && action.execute()) {
                setChangedAndSync(true);
            }
            return drained <= 0 ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            for (HbmFluidTank tank : outputTanks) {
                if (tank.amount() <= 0 || tank.type().isNone()) {
                    continue;
                }
                HbmFluidDefinition fluid = tank.type();
                HbmFluidStack drained = tank.drain(fluid, maxDrain, action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        setChangedAndSync(true);
                    }
                    return HbmFluids.toNeoStack(fluid, drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
