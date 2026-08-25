package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.ChemicalFactoryBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.client.sound.ChemicalFactoryClientSounds;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.ChemicalFactoryMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
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

public class ChemicalFactoryBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int MODULE_COUNT = 4;
    public static final int TANKS_PER_MODULE = 3;
    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_START = 1;
    public static final int UPGRADE_END = 4;
    public static final int MODULE_START = 4;
    public static final int MODULE_STRIDE = 7;
    public static final int MODULE_BLUEPRINT_OFFSET = 0;
    public static final int MODULE_INPUT_OFFSET = 1;
    public static final int MODULE_OUTPUT_OFFSET = 4;
    public static final int MODULE_ITEM_COUNT = 3;
    public static final int SLOT_COUNT = 32;
    public static final int TANK_CAPACITY = 24_000;
    public static final int COOLANT_TANK_CAPACITY = 4_000;
    public static final long BASE_ENERGY_CAPACITY = 1_000_000L;
    public static final int DATA_COUNT = 106;

    private static final int DATA_MODULE_START = 4;
    private static final int DATA_PER_MODULE = 6;
    private static final int DATA_TANK_START = DATA_MODULE_START + MODULE_COUNT * DATA_PER_MODULE;
    private static final int TANK_COUNT = MODULE_COUNT * TANKS_PER_MODULE * 2 + 2;
    private static final int[] AUTOMATION_SLOTS = createAutomationSlots();

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank[] inputTanks = new HbmFluidTank[MODULE_COUNT * TANKS_PER_MODULE];
    private final HbmFluidTank[] outputTanks = new HbmFluidTank[MODULE_COUNT * TANKS_PER_MODULE];
    private final HbmFluidTank waterTank = new HbmFluidTank(water(), COOLANT_TANK_CAPACITY);
    private final HbmFluidTank spentSteamTank = new HbmFluidTank(spentSteam(), COOLANT_TANK_CAPACITY);
    private final int[] progress = new int[MODULE_COUNT];
    private final int[] workTime = new int[MODULE_COUNT];
    private final int[] currentDemand = new int[MODULE_COUNT];
    private final int[] completedCycles = new int[MODULE_COUNT];
    private final boolean[] hasRecipe = new boolean[MODULE_COUNT];
    private final boolean[] didProcess = new boolean[MODULE_COUNT];
    private long energyStored;
    private long lastInput;
    private long energyCapacity = BASE_ENERGY_CAPACITY;
    private double clientPrevAnim;
    private double clientAnim;
    private boolean clientFrame;
    private long lastClientAnimationTick = Long.MIN_VALUE;
    @Nullable
    private ResourceLocation[] selectedRecipeIds = new ResourceLocation[MODULE_COUNT];

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_TANK_START && index < DATA_COUNT) {
                int tankData = index - DATA_TANK_START;
                HbmFluidTank tank = tankByFlatIndex(tankData / 3);
                return switch (tankData % 3) {
                    case 0 -> tank.type().oldId();
                    case 1 -> tank.amount();
                    case 2 -> tank.pressure();
                    default -> 0;
                };
            }
            if (index >= DATA_MODULE_START && index < DATA_TANK_START) {
                int module = (index - DATA_MODULE_START) / DATA_PER_MODULE;
                int field = (index - DATA_MODULE_START) % DATA_PER_MODULE;
                return switch (field) {
                    case 0 -> progress[module];
                    case 1 -> Math.max(1, workTime[module]);
                    case 2 -> Math.max(1, currentDemand[module]);
                    case 3 -> hasRecipe[module] ? 1 : 0;
                    case 4 -> didProcess[module] ? 1 : 0;
                    case 5 -> selectedRecipeMenuValue(module);
                    default -> 0;
                };
            }
            return switch (index) {
                case 0 -> (int) energyStored;
                case 1 -> (int) lastInput;
                case 2 -> (int) energyCapacity;
                case 3 -> totalCompletedCycles();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= DATA_TANK_START && index < DATA_COUNT) {
                int tankData = index - DATA_TANK_START;
                HbmFluidTank tank = tankByFlatIndex(tankData / 3);
                switch (tankData % 3) {
                    case 0 -> tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                    case 1 -> tank.setAmount(value);
                    case 2 -> tank.setPressure(value);
                    default -> {
                    }
                }
                return;
            }
            if (index >= DATA_MODULE_START && index < DATA_TANK_START) {
                int module = (index - DATA_MODULE_START) / DATA_PER_MODULE;
                int field = (index - DATA_MODULE_START) % DATA_PER_MODULE;
                switch (field) {
                    case 0 -> progress[module] = value;
                    case 1 -> workTime[module] = Math.max(1, value);
                    case 2 -> currentDemand[module] = Math.max(1, value);
                    case 3 -> hasRecipe[module] = value != 0;
                    case 4 -> didProcess[module] = value != 0;
                    case 5 -> setSelectedRecipeByMenuValue(module, value);
                    default -> {
                    }
                }
                return;
            }
            switch (index) {
                case 0 -> energyStored = value;
                case 1 -> lastInput = value;
                case 2 -> energyCapacity = Math.max(BASE_ENERGY_CAPACITY, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ChemicalFactoryBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.CHEMICAL_FACTORY.get(), pos, blockState);
        for (int index = 0; index < inputTanks.length; index++) {
            this.inputTanks[index] = new HbmFluidTank(TANK_CAPACITY);
            this.outputTanks[index] = new HbmFluidTank(TANK_CAPACITY);
        }
        for (int module = 0; module < MODULE_COUNT; module++) {
            this.workTime[module] = 100;
            this.currentDemand[module] = 100;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChemicalFactoryBlockEntity factory) {
        if (level.isClientSide) {
            factory.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, factory);
        factory.tickServer(level);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        ArrayList<BlockPos> connectors = new ArrayList<>(30);
        for (Port port : powerPorts()) {
            connectors.add(port.pos().immutable());
        }
        return List.copyOf(connectors);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : powerPorts()) {
            if (port.pos().equals(connectorPos) && port.face() == machineSide) {
                return true;
            }
        }
        return false;
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
        return Math.min(Math.max(100L, totalDemand()), this.energyCapacity - this.energyStored);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.energyStored = Math.min(this.energyCapacity, this.energyStored + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.chemical_factory",
                this.lastInput,
                totalDemand(),
                this.energyStored,
                this.energyCapacity,
                totalCompletedCycles()
        );
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new ChemicalFactoryFluidHandler(-1);
    }

    public IFluidHandler fluidHandler(BlockPos accessorPos, @Nullable Direction side) {
        if (isCoolantAccessor(accessorPos, side)) {
            return new ChemicalFactoryFluidHandler(-2);
        }
        // TileEntityMachineChemicalFactory exposes its twelve input and
        // twelve output tanks through every normal connection point. The
        // four recipe-field faces are visual labels, not per-module pipes.
        return isProcessAccessor(accessorPos, side)
                ? new ChemicalFactoryFluidHandler(-1)
                : new ChemicalFactoryFluidHandler(-3);
    }

    public boolean isAutomationPort(BlockPos accessorPos) {
        return allowsAutomationPort(accessorPos, null);
    }

    public boolean allowsAutomationPort(BlockPos accessorPos, @Nullable Direction side) {
        return isCoolantAccessor(accessorPos, side) || isProcessAccessor(accessorPos, side);
    }

    public HbmFluidTank inputTank(int index) {
        return this.inputTanks[Math.max(0, Math.min(this.inputTanks.length - 1, index))];
    }

    public HbmFluidTank outputTank(int index) {
        return this.outputTanks[Math.max(0, Math.min(this.outputTanks.length - 1, index))];
    }

    public HbmFluidTank waterTank() {
        return this.waterTank;
    }

    public HbmFluidTank spentSteamTank() {
        return this.spentSteamTank;
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
            moduleForSlot(slot).ifPresent(module -> this.progress[module] = 0);
            setChangedAndSync(false);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
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
        moduleForSlot(slot).ifPresent(module -> this.progress[module] = 0);
        if (isBlueprintSlot(slot)) {
            clearSelectionIfBlueprintNoLongerAllowsIt(moduleForBlueprintSlot(slot));
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return isSupportedUpgrade(stack);
        }
        if (isBlueprintSlot(slot)) {
            return BlueprintItem.isBlueprint(stack);
        }
        Optional<Integer> module = moduleForSlot(slot);
        return module.isPresent() && isInputSlot(slot) && canAcceptInput(module.get(), slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isInputSlot(slot) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isOutputSlot(slot) || isSlotClogged(slot);
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
        for (int module = 0; module < MODULE_COUNT; module++) {
            this.progress[module] = 0;
        }
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.chemical_factory");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChemicalFactoryMenu(containerId, playerInventory, this, this.menuData);
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
        for (int index = 0; index < inputTanks.length; index++) {
            tag.put("InputTank" + index, this.inputTanks[index].save());
            tag.put("OutputTank" + index, this.outputTanks[index].save());
        }
        for (int module = 0; module < MODULE_COUNT; module++) {
            tag.putInt("Progress" + module, this.progress[module]);
            tag.putInt("CompletedCycles" + module, this.completedCycles[module]);
            if (this.selectedRecipeIds[module] != null) {
                tag.putString("SelectedRecipe" + module, this.selectedRecipeIds[module].toString());
            }
        }
        tag.put("WaterTank", this.waterTank.save());
        tag.put("SpentSteamTank", this.spentSteamTank.save());
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastInput", this.lastInput);
        tag.putLong("EnergyCapacity", this.energyCapacity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        for (int index = 0; index < inputTanks.length; index++) {
            this.inputTanks[index].load(tag.getCompound("InputTank" + index));
            this.outputTanks[index].load(tag.getCompound("OutputTank" + index));
        }
        for (int module = 0; module < MODULE_COUNT; module++) {
            this.progress[module] = tag.getInt("Progress" + module);
            this.completedCycles[module] = tag.getInt("CompletedCycles" + module);
            this.selectedRecipeIds[module] = tag.contains("SelectedRecipe" + module)
                    ? ResourceLocation.tryParse(tag.getString("SelectedRecipe" + module))
                    : null;
        }
        this.waterTank.load(tag.getCompound("WaterTank"));
        this.spentSteamTank.load(tag.getCompound("SpentSteamTank"));
        this.energyStored = tag.getLong("EnergyStored");
        this.lastInput = tag.getLong("LastInput");
        this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, tag.getLong("EnergyCapacity"));
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

    public boolean canAcceptInput(int module, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int slot = inputSlotStart(module); slot < inputSlotStart(module) + MODULE_ITEM_COUNT; slot++) {
            if (canAcceptInput(module, slot, stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAcceptInput(int module, int slot, ItemStack stack) {
        if (!isValidModule(module) || !isInputSlot(slot) || stack.isEmpty() || this.level == null) {
            return false;
        }
        Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = selectedRecipe(module, this.level);
        if (selectedRecipe.isEmpty()) {
            return false;
        }
        int inputIndex = slot - inputSlotStart(module);
        List<ChemicalPlantRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().inputItems();
        if (inputIndex < 0 || inputIndex >= ingredients.size()) {
            return false;
        }
        return ingredients.get(inputIndex).ingredient().test(stack);
    }

    public Optional<ResourceLocation> selectedRecipeId(int module) {
        return isValidModule(module) ? Optional.ofNullable(this.selectedRecipeIds[module]) : Optional.empty();
    }

    public void setSelectedRecipe(int module, @Nullable ResourceLocation recipeId) {
        if (!isValidModule(module)) {
            return;
        }
        if (recipeId != null && this.level != null) {
            Optional<RecipeHolder<ChemicalPlantRecipe>> recipe = findRecipe(this.level, recipeId);
            if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool(module))) {
                recipeId = null;
            }
        }
        if (this.selectedRecipeIds[module] == recipeId || (this.selectedRecipeIds[module] != null && this.selectedRecipeIds[module].equals(recipeId))) {
            return;
        }
        this.selectedRecipeIds[module] = recipeId;
        this.progress[module] = 0;
        setChangedAndSync(true);
    }

    public Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe(int module, Level level) {
        if (!isValidModule(module) || this.selectedRecipeIds[module] == null) {
            return Optional.empty();
        }
        return findRecipe(level, this.selectedRecipeIds[module])
                .filter(holder -> holder.value().isVisibleForPool(installedBlueprintPool(module)));
    }

    public List<RecipeHolder<ChemicalPlantRecipe>> availableRecipes(Level level, int module) {
        Optional<String> pool = isValidModule(module) ? installedBlueprintPool(module) : Optional.empty();
        return level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(pool))
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
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
        this.clientPrevAnim = this.clientAnim;
        if (this.getBlockState().hasProperty(ChemicalFactoryBlock.LIT) && this.getBlockState().getValue(ChemicalFactoryBlock.LIT)) {
            this.clientAnim++;
        }
        this.lastClientAnimationTick = gameTime;
    }

    public double clientAnim(float partialTick) {
        return this.clientPrevAnim + (this.clientAnim - this.clientPrevAnim) * partialTick;
    }

    public boolean clientFrame() {
        return this.clientFrame;
    }

    private void tickClient() {
        updateClientAnimation();
        ChemicalFactoryClientSounds.tick(this);
    }

    private void tickServer(Level level) {
        this.energyStored = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.energyStored, this.energyCapacity);
        long nextCapacity = 0L;
        boolean anyWorking = false;
        boolean sync = level.getGameTime() % 10L == 0L;

        for (int module = 0; module < MODULE_COUNT; module++) {
            this.didProcess[module] = false;
            Optional<RecipeHolder<ChemicalPlantRecipe>> recipeHolder = getSelectedRecipe(module, level);
            this.hasRecipe[module] = recipeHolder.isPresent();
            if (recipeHolder.isEmpty()) {
                this.progress[module] = 0;
                this.workTime[module] = 100;
                this.currentDemand[module] = 100;
                continue;
            }

            ChemicalPlantRecipe recipe = recipeHolder.get().value();
            nextCapacity += recipe.power() * 100L;
            setupTanks(module, recipe);
            tickContainers(module, recipe);
            this.workTime[module] = currentWorkTime(recipe);
            this.currentDemand[module] = currentDemand(recipe);

            if (!canProcess(module, recipe) || this.energyStored < this.currentDemand[module] || !canCool()) {
                continue;
            }

            this.energyStored -= this.currentDemand[module];
            this.progress[module]++;
            this.didProcess[module] = true;
            anyWorking = true;
            this.waterTank.drain(water(), 100, false);
            this.spentSteamTank.fill(spentSteam(), 100, false);

            if (this.progress[module] >= this.workTime[module]) {
                finishRecipe(module, recipe);
                this.progress[module] = 0;
                this.completedCycles[module]++;
            }
        }

        recycleOutputsIntoInputs();
        this.energyCapacity = Math.max(Math.max(BASE_ENERGY_CAPACITY, nextCapacity), this.energyStored);
        setLit(anyWorking);
        if (anyWorking || sync) {
            setChangedAndSync(sync);
        }
    }

    private Optional<RecipeHolder<ChemicalPlantRecipe>> getSelectedRecipe(int module, Level level) {
        if (!isValidModule(module) || this.selectedRecipeIds[module] == null) {
            return Optional.empty();
        }
        Optional<RecipeHolder<ChemicalPlantRecipe>> recipe = findRecipe(level, this.selectedRecipeIds[module]);
        if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool(module))) {
            this.selectedRecipeIds[module] = null;
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

    private int selectedRecipeMenuValue(int module) {
        if (!isValidModule(module) || this.selectedRecipeIds[module] == null || this.level == null) {
            return 0;
        }
        List<RecipeHolder<ChemicalPlantRecipe>> recipes = availableRecipes(this.level, module);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(this.selectedRecipeIds[module])) {
                return index + 1;
            }
        }
        return 0;
    }

    private void setSelectedRecipeByMenuValue(int module, int value) {
        if (!isValidModule(module)) {
            return;
        }
        if (value <= 0 || this.level == null) {
            this.selectedRecipeIds[module] = null;
            return;
        }
        List<RecipeHolder<ChemicalPlantRecipe>> recipes = availableRecipes(this.level, module);
        int index = value - 1;
        this.selectedRecipeIds[module] = index >= 0 && index < recipes.size() ? recipes.get(index).id() : null;
    }

    private Optional<String> installedBlueprintPool(int module) {
        return BlueprintItem.pool(this.items.get(blueprintSlot(module)));
    }

    private void clearSelectionIfBlueprintNoLongerAllowsIt(int module) {
        if (this.level == null || !isValidModule(module) || this.selectedRecipeIds[module] == null) {
            return;
        }
        Optional<RecipeHolder<ChemicalPlantRecipe>> recipe = findRecipe(this.level, this.selectedRecipeIds[module]);
        if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool(module))) {
            this.selectedRecipeIds[module] = null;
        }
    }

    private void setupTanks(int module, ChemicalPlantRecipe recipe) {
        for (int index = 0; index < TANKS_PER_MODULE; index++) {
            int tankIndex = tankIndex(module, index);
            if (index < recipe.inputFluids().size()) {
                ChemicalPlantRecipe.ChemicalFluidStack input = recipe.inputFluids().get(index);
                this.inputTanks[tankIndex].conform(input.type(), input.pressure());
            } else if (this.inputTanks[tankIndex].amount() == 0) {
                this.inputTanks[tankIndex].clear();
            }

            if (index < recipe.outputFluids().size()) {
                ChemicalPlantRecipe.ChemicalFluidStack output = recipe.outputFluids().get(index);
                this.outputTanks[tankIndex].conform(output.type(), output.pressure());
            } else if (this.outputTanks[tankIndex].amount() == 0) {
                this.outputTanks[tankIndex].clear();
            }
        }
    }

    private void tickContainers(int module, ChemicalPlantRecipe recipe) {
        // The large factory has no separate bucket slots in 1.12; external automation handles fluid containers.
    }

    private boolean canProcess(int module, ChemicalPlantRecipe recipe) {
        if (!recipe.matches(new ChemicalPlantRecipe.Input(inputStacks(module), inputFluidStacks(module)), this.level)) {
            return false;
        }
        return canFitItemOutputs(module, recipe) && canFitFluidOutputs(module, recipe);
    }

    private boolean canFitItemOutputs(int module, ChemicalPlantRecipe recipe) {
        if (recipe.outputItems().size() > MODULE_ITEM_COUNT) {
            return false;
        }
        for (int index = 0; index < recipe.outputItems().size(); index++) {
            ItemStack result = recipe.outputItems().get(index);
            if (result.isEmpty()) {
                continue;
            }
            ItemStack current = this.items.get(outputSlotStart(module) + index);
            if (current.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(current, result) || current.getCount() + result.getCount() > current.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private boolean canFitFluidOutputs(int module, ChemicalPlantRecipe recipe) {
        if (recipe.outputFluids().size() > TANKS_PER_MODULE) {
            return false;
        }
        for (int index = 0; index < recipe.outputFluids().size(); index++) {
            ChemicalPlantRecipe.ChemicalFluidStack output = recipe.outputFluids().get(index);
            HbmFluidTank tank = this.outputTanks[tankIndex(module, index)];
            if (tank.type() != output.type() || tank.pressure() != output.pressure() || tank.amount() + output.amount() > tank.capacity()) {
                return false;
            }
        }
        return true;
    }

    private void finishRecipe(int module, ChemicalPlantRecipe recipe) {
        consumeInputs(module, recipe);
        produceOutputs(module, recipe);
    }

    private void consumeInputs(int module, ChemicalPlantRecipe recipe) {
        for (int index = 0; index < Math.min(recipe.inputItems().size(), MODULE_ITEM_COUNT); index++) {
            int slot = inputSlotStart(module) + index;
            ChemicalPlantRecipe.CountedIngredient ingredient = recipe.inputItems().get(index);
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                stack.shrink(ingredient.count());
                if (stack.isEmpty()) {
                    this.items.set(slot, ItemStack.EMPTY);
                }
            }
        }
        for (int index = 0; index < Math.min(recipe.inputFluids().size(), TANKS_PER_MODULE); index++) {
            ChemicalPlantRecipe.ChemicalFluidStack fluid = recipe.inputFluids().get(index);
            this.inputTanks[tankIndex(module, index)].drain(fluid.type(), fluid.amount(), false);
        }
    }

    private void produceOutputs(int module, ChemicalPlantRecipe recipe) {
        for (int index = 0; index < Math.min(recipe.outputItems().size(), MODULE_ITEM_COUNT); index++) {
            ItemStack result = recipe.outputItems().get(index).copy();
            if (result.isEmpty()) {
                continue;
            }
            int slot = outputSlotStart(module) + index;
            ItemStack current = this.items.get(slot);
            if (current.isEmpty()) {
                this.items.set(slot, result);
            } else {
                current.grow(result.getCount());
            }
        }
        for (int index = 0; index < Math.min(recipe.outputFluids().size(), TANKS_PER_MODULE); index++) {
            ChemicalPlantRecipe.ChemicalFluidStack fluid = recipe.outputFluids().get(index);
            this.outputTanks[tankIndex(module, index)].fill(fluid.type(), fluid.amount(), fluid.pressure(), false);
        }
    }

    private void recycleOutputsIntoInputs() {
        for (HbmFluidTank in : this.inputTanks) {
            if (in.type().isNone()) {
                continue;
            }
            for (HbmFluidTank out : this.outputTanks) {
                if (out.type() != in.type() || out.pressure() != in.pressure()) {
                    continue;
                }
                int moved = Math.min(Math.min(in.capacity() - in.amount(), out.amount()), 50);
                if (moved > 0) {
                    in.fill(in.type(), moved, in.pressure(), false);
                    out.drain(out.type(), moved, false);
                }
            }
        }
    }

    private List<ItemStack> inputStacks(int module) {
        ArrayList<ItemStack> stacks = new ArrayList<>(MODULE_ITEM_COUNT);
        for (int slot = inputSlotStart(module); slot < inputSlotStart(module) + MODULE_ITEM_COUNT; slot++) {
            stacks.add(this.items.get(slot));
        }
        return stacks;
    }

    private List<HbmFluidStack> inputFluidStacks(int module) {
        ArrayList<HbmFluidStack> fluids = new ArrayList<>(TANKS_PER_MODULE);
        for (int index = 0; index < TANKS_PER_MODULE; index++) {
            HbmFluidTank tank = this.inputTanks[tankIndex(module, index)];
            fluids.add(new HbmFluidStack(tank.type(), tank.amount(), tank.pressure()));
        }
        return fluids;
    }

    private int currentWorkTime(ChemicalPlantRecipe recipe) {
        double speed = 1.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3) / 3.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3);
        speed *= 2.0D;
        return Math.max(1, (int) Math.ceil(recipe.duration() / speed));
    }

    private int currentDemand(ChemicalPlantRecipe recipe) {
        double multiplier = 1.0D;
        multiplier -= Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3) * 0.25D;
        multiplier += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        multiplier += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) * 10.0D / 3.0D;
        multiplier *= 2.0D;
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

    private boolean canCool() {
        return this.waterTank.amount() >= 100 && this.spentSteamTank.amount() <= this.spentSteamTank.capacity() - 100;
    }

    public boolean isClientWorking() {
        return this.getBlockState().hasProperty(ChemicalFactoryBlock.LIT)
                && this.getBlockState().getValue(ChemicalFactoryBlock.LIT);
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof ChemicalFactoryBlock && state.getValue(ChemicalFactoryBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(ChemicalFactoryBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private boolean isProcessAccessor(BlockPos accessorPos, @Nullable Direction side) {
        for (Port port : processConnectorPorts()) {
            BlockPos innerPort = port.pos().relative(port.face().getOpposite());
            if (innerPort.equals(accessorPos) && (side == null || port.face() == side)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCoolantAccessor(BlockPos accessorPos, @Nullable Direction side) {
        for (Port port : coolantAccessorPorts()) {
            if (port.pos().equals(accessorPos) && (side == null || port.face() == side)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The 1.7.10 factory ran its input/output transfer loop over the 12 base
     * and 10 top positions from getConPos(), plus the four displayed recipe
     * field faces. All of them operate on the same process-tank arrays.
     */
    private List<Port> processConnectorPorts() {
        ArrayList<Port> ports = new ArrayList<>(26);
        BlockPos pos = this.worldPosition;
        ports.add(new Port(pos.offset(3, 0, -2), Direction.EAST));
        ports.add(new Port(pos.offset(3, 0, 0), Direction.EAST));
        ports.add(new Port(pos.offset(3, 0, 2), Direction.EAST));
        ports.add(new Port(pos.offset(-3, 0, -2), Direction.WEST));
        ports.add(new Port(pos.offset(-3, 0, 0), Direction.WEST));
        ports.add(new Port(pos.offset(-3, 0, 2), Direction.WEST));
        ports.add(new Port(pos.offset(-2, 0, 3), Direction.SOUTH));
        ports.add(new Port(pos.offset(0, 0, 3), Direction.SOUTH));
        ports.add(new Port(pos.offset(2, 0, 3), Direction.SOUTH));
        ports.add(new Port(pos.offset(-2, 0, -3), Direction.NORTH));
        ports.add(new Port(pos.offset(0, 0, -3), Direction.NORTH));
        ports.add(new Port(pos.offset(2, 0, -3), Direction.NORTH));

        Direction facing = facing();
        Direction rot = facing.getClockWise();
        for (int i = -2; i <= 2; i++) {
            ports.add(new Port(pos.offset(offset(facing, i, rot, 2)).above(3), Direction.UP));
            ports.add(new Port(pos.offset(offset(facing, i, rot, -2)).above(3), Direction.UP));
        }
        ports.addAll(ioConnectorPorts());
        return List.copyOf(ports);
    }

    private List<Port> powerPorts() {
        ArrayList<Port> ports = new ArrayList<>(30);
        BlockPos pos = this.worldPosition;
        ports.add(new Port(pos.offset(3, 0, -2), Direction.EAST));
        ports.add(new Port(pos.offset(3, 0, 0), Direction.EAST));
        ports.add(new Port(pos.offset(3, 0, 2), Direction.EAST));
        ports.add(new Port(pos.offset(-3, 0, -2), Direction.WEST));
        ports.add(new Port(pos.offset(-3, 0, 0), Direction.WEST));
        ports.add(new Port(pos.offset(-3, 0, 2), Direction.WEST));
        ports.add(new Port(pos.offset(-2, 0, 3), Direction.SOUTH));
        ports.add(new Port(pos.offset(0, 0, 3), Direction.SOUTH));
        ports.add(new Port(pos.offset(2, 0, 3), Direction.SOUTH));
        ports.add(new Port(pos.offset(-2, 0, -3), Direction.NORTH));
        ports.add(new Port(pos.offset(0, 0, -3), Direction.NORTH));
        ports.add(new Port(pos.offset(2, 0, -3), Direction.NORTH));

        Direction facing = facing();
        Direction rot = facing.getClockWise();
        for (int i = -2; i <= 2; i++) {
            ports.add(new Port(pos.offset(offset(facing, i, rot, 2)).above(3), Direction.UP));
            ports.add(new Port(pos.offset(offset(facing, i, rot, -2)).above(3), Direction.UP));
        }
        ports.addAll(ioConnectorPorts());
        ports.addAll(coolantConnectorPorts());
        return List.copyOf(ports);
    }

    private List<Port> ioConnectorPorts() {
        Direction facing = facing();
        Direction rot = facing.getClockWise();
        return List.of(
                new Port(this.worldPosition.offset(offset(facing, 1, rot, 3)), rot),
                new Port(this.worldPosition.offset(offset(facing, -1, rot, 3)), rot),
                new Port(this.worldPosition.offset(offset(facing, 1, rot, -3)), rot.getOpposite()),
                new Port(this.worldPosition.offset(offset(facing, -1, rot, -3)), rot.getOpposite())
        );
    }

    private List<Port> ioAccessorPorts() {
        Direction facing = facing();
        Direction rot = facing.getClockWise();
        return List.of(
                new Port(this.worldPosition.offset(offset(facing, 1, rot, 2)), rot),
                new Port(this.worldPosition.offset(offset(facing, -1, rot, 2)), rot),
                new Port(this.worldPosition.offset(offset(facing, 1, rot, -2)), rot.getOpposite()),
                new Port(this.worldPosition.offset(offset(facing, -1, rot, -2)), rot.getOpposite())
        );
    }

    private List<Port> coolantConnectorPorts() {
        Direction facing = facing();
        Direction rot = facing.getClockWise();
        return List.of(
                new Port(this.worldPosition.offset(offset(facing, 3, rot, 1)), facing),
                new Port(this.worldPosition.offset(offset(facing, 3, rot, -1)), facing),
                new Port(this.worldPosition.offset(offset(facing, -3, rot, 1)), facing.getOpposite()),
                new Port(this.worldPosition.offset(offset(facing, -3, rot, -1)), facing.getOpposite())
        );
    }

    private List<Port> coolantAccessorPorts() {
        Direction facing = facing();
        Direction rot = facing.getClockWise();
        return List.of(
                new Port(this.worldPosition.offset(offset(facing, 2, rot, 1)), facing),
                new Port(this.worldPosition.offset(offset(facing, 2, rot, -1)), facing),
                new Port(this.worldPosition.offset(offset(facing, -2, rot, 1)), facing.getOpposite()),
                new Port(this.worldPosition.offset(offset(facing, -2, rot, -1)), facing.getOpposite())
        );
    }

    private Direction facing() {
        BlockState state = this.getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    private HbmFluidTank tankByFlatIndex(int tank) {
        if (tank < inputTanks.length) {
            return this.inputTanks[Math.max(0, tank)];
        }
        if (tank < inputTanks.length + outputTanks.length) {
            return this.outputTanks[Math.max(0, tank - inputTanks.length)];
        }
        return tank == inputTanks.length + outputTanks.length ? this.waterTank : this.spentSteamTank;
    }

    private int totalDemand() {
        int total = 0;
        for (int demand : this.currentDemand) {
            total += Math.max(0, demand);
        }
        return total;
    }

    private int totalCompletedCycles() {
        int total = 0;
        for (int cycles : this.completedCycles) {
            total += cycles;
        }
        return total;
    }

    private Optional<Integer> moduleForSlot(int slot) {
        if (slot < MODULE_START || slot >= SLOT_COUNT) {
            return Optional.empty();
        }
        int module = (slot - MODULE_START) / MODULE_STRIDE;
        return isValidModule(module) ? Optional.of(module) : Optional.empty();
    }

    private boolean isSlotClogged(int slot) {
        Optional<Integer> module = moduleForSlot(slot);
        return module.isPresent() && isInputSlot(slot) && !this.items.get(slot).isEmpty() && !canAcceptInput(module.get(), slot, this.items.get(slot));
    }

    public static int blueprintSlot(int module) {
        return MODULE_START + module * MODULE_STRIDE + MODULE_BLUEPRINT_OFFSET;
    }

    public static int inputSlotStart(int module) {
        return MODULE_START + module * MODULE_STRIDE + MODULE_INPUT_OFFSET;
    }

    public static int outputSlotStart(int module) {
        return MODULE_START + module * MODULE_STRIDE + MODULE_OUTPUT_OFFSET;
    }

    public static int tankIndex(int module, int index) {
        return module * TANKS_PER_MODULE + index;
    }

    private static int moduleForBlueprintSlot(int slot) {
        return (slot - MODULE_START) / MODULE_STRIDE;
    }

    private static boolean isBlueprintSlot(int slot) {
        if (slot < MODULE_START || slot >= SLOT_COUNT) {
            return false;
        }
        return (slot - MODULE_START) % MODULE_STRIDE == MODULE_BLUEPRINT_OFFSET;
    }

    private static boolean isInputSlot(int slot) {
        if (slot < MODULE_START || slot >= SLOT_COUNT) {
            return false;
        }
        int local = (slot - MODULE_START) % MODULE_STRIDE;
        return local >= MODULE_INPUT_OFFSET && local < MODULE_INPUT_OFFSET + MODULE_ITEM_COUNT;
    }

    private static boolean isOutputSlot(int slot) {
        if (slot < MODULE_START || slot >= SLOT_COUNT) {
            return false;
        }
        int local = (slot - MODULE_START) % MODULE_STRIDE;
        return local >= MODULE_OUTPUT_OFFSET && local < MODULE_OUTPUT_OFFSET + MODULE_ITEM_COUNT;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static boolean isValidModule(int module) {
        return module >= 0 && module < MODULE_COUNT;
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

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (sync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static int[] createAutomationSlots() {
        int[] slots = new int[MODULE_COUNT * MODULE_ITEM_COUNT * 2];
        int index = 0;
        for (int module = 0; module < MODULE_COUNT; module++) {
            for (int slot = inputSlotStart(module); slot < inputSlotStart(module) + MODULE_ITEM_COUNT; slot++) {
                slots[index++] = slot;
            }
            for (int slot = outputSlotStart(module); slot < outputSlotStart(module) + MODULE_ITEM_COUNT; slot++) {
                slots[index++] = slot;
            }
        }
        return slots;
    }

    private static BlockPos offset(Direction facing, int facingScale, Direction rot, int rotScale) {
        return new BlockPos(
                facing.getStepX() * facingScale + rot.getStepX() * rotScale,
                0,
                facing.getStepZ() * facingScale + rot.getStepZ() * rotScale
        );
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
    }

    private final class ChemicalFactoryFluidHandler implements IFluidHandler {
        private final int module;

        private ChemicalFactoryFluidHandler(int module) {
            this.module = module;
        }

        @Override
        public int getTanks() {
            if (this.module == -3) {
                return 0;
            }
            return this.module == -2 ? 2 : this.module >= 0 ? TANKS_PER_MODULE * 2 : inputTanks.length + outputTanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            HbmFluidTank hbmTank = exposedTank(tank);
            return hbmTank == null ? FluidStack.EMPTY : hbmTank.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            HbmFluidTank hbmTank = exposedTank(tank);
            return hbmTank == null ? 0 : hbmTank.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank hbmTank = exposedTank(tank);
            return hbmTank != null && !fluid.isNone() && canFillTank(hbmTank, fluid);
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
            for (HbmFluidTank tank : fillableTanks()) {
                if (remaining <= 0 || !canFillTank(tank, fluid)) {
                    continue;
                }
                // Process tanks are conformed to their selected recipe before
                // pipe transfer. NeoForge fluid stacks do not carry HBM's
                // legacy pressure field, so retain the configured tank
                // pressure instead of replacing it with the stack default.
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
            for (HbmFluidTank tank : drainableTanks()) {
                if (remaining <= 0 || tank.type() != fluid) {
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
            for (HbmFluidTank tank : drainableTanks()) {
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

        @Nullable
        private HbmFluidTank exposedTank(int tank) {
            if (this.module == -2) {
                return tank == 0 ? waterTank : tank == 1 ? spentSteamTank : null;
            }
            if (this.module >= 0) {
                if (tank >= 0 && tank < TANKS_PER_MODULE) {
                    return inputTanks[tankIndex(this.module, tank)];
                }
                int output = tank - TANKS_PER_MODULE;
                return output >= 0 && output < TANKS_PER_MODULE ? outputTanks[tankIndex(this.module, output)] : null;
            }
            if (tank >= 0 && tank < inputTanks.length) {
                return inputTanks[tank];
            }
            int output = tank - inputTanks.length;
            return output >= 0 && output < outputTanks.length ? outputTanks[output] : null;
        }

        private List<HbmFluidTank> fillableTanks() {
            if (this.module == -3) {
                return List.of();
            }
            if (this.module == -2) {
                return List.of(waterTank);
            }
            if (this.module >= 0) {
                ArrayList<HbmFluidTank> tanks = new ArrayList<>(TANKS_PER_MODULE);
                for (int index = 0; index < TANKS_PER_MODULE; index++) {
                    tanks.add(inputTanks[tankIndex(this.module, index)]);
                }
                return tanks;
            }
            ArrayList<HbmFluidTank> tanks = new ArrayList<>(inputTanks.length);
            tanks.addAll(List.of(inputTanks));
            return tanks;
        }

        private List<HbmFluidTank> drainableTanks() {
            if (this.module == -3) {
                return List.of();
            }
            if (this.module == -2) {
                return List.of(spentSteamTank);
            }
            if (this.module >= 0) {
                ArrayList<HbmFluidTank> tanks = new ArrayList<>(TANKS_PER_MODULE);
                for (int index = 0; index < TANKS_PER_MODULE; index++) {
                    tanks.add(outputTanks[tankIndex(this.module, index)]);
                }
                return tanks;
            }
            ArrayList<HbmFluidTank> tanks = new ArrayList<>(outputTanks.length);
            tanks.addAll(List.of(outputTanks));
            return tanks;
        }

        private boolean canFillTank(HbmFluidTank tank, HbmFluidDefinition fluid) {
            return !fluid.isNone() && (tank.type().isNone() || tank.type() == fluid);
        }
    }

    private record Port(BlockPos pos, Direction face) {
    }
}
