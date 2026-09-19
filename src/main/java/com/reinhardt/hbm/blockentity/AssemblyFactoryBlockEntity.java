package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.AssemblyFactoryBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.AssemblyFactoryMenu;
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

public class AssemblyFactoryBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int MODULE_COUNT = 4;
    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_START = 1;
    public static final int UPGRADE_END = 4;
    public static final int MODULE_START = 4;
    public static final int MODULE_STRIDE = 14;
    public static final int MODULE_BLUEPRINT_OFFSET = 0;
    public static final int MODULE_INPUT_OFFSET = 1;
    public static final int MODULE_INPUT_COUNT = 12;
    public static final int MODULE_OUTPUT_OFFSET = 13;
    public static final int SLOT_COUNT = 60;
    public static final int RECIPE_TANK_CAPACITY = 4_000;
    public static final int COOLANT_TANK_CAPACITY = 4_000;
    public static final long BASE_ENERGY_CAPACITY = 1_000_000L;
    public static final int DATA_COUNT = 68;

    private static final int DATA_MODULE_START = 4;
    private static final int DATA_PER_MODULE = 6;
    private static final int DATA_TANK_START = DATA_MODULE_START + MODULE_COUNT * DATA_PER_MODULE;
    private static final int TANK_COUNT = 10;
    private static final int[] AUTOMATION_SLOTS = createAutomationSlots();

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank[] inputTanks = new HbmFluidTank[MODULE_COUNT];
    private final HbmFluidTank[] outputTanks = new HbmFluidTank[MODULE_COUNT];
    private final HbmFluidTank waterTank = new HbmFluidTank(water(), COOLANT_TANK_CAPACITY);
    private final HbmFluidTank spentSteamTank = new HbmFluidTank(spentSteam(), COOLANT_TANK_CAPACITY);
    private final int[] progress = new int[MODULE_COUNT];
    private final int[] workTime = new int[MODULE_COUNT];
    private final int[] currentDemand = new int[MODULE_COUNT];
    private final int[] completedCycles = new int[MODULE_COUNT];
    private final boolean[] hasRecipe = new boolean[MODULE_COUNT];
    private final boolean[] didProcess = new boolean[MODULE_COUNT];
    private final int[] motorSoundCycle = new int[MODULE_COUNT];
    private final int[] strikeSoundCycle = new int[MODULE_COUNT];
    private final boolean[] wasWorking = new boolean[MODULE_COUNT];
    private final AssemfacArm[] clientAnimations = {new AssemfacArm(0), new AssemfacArm(1)};
    private long energyStored;
    private long lastInput;
    private long energyCapacity = BASE_ENERGY_CAPACITY;
    private boolean clientFrame;
    private long lastClientAnimationTick = Long.MIN_VALUE;
    @Nullable
    private ResourceLocation[] selectedRecipeIds = new ResourceLocation[MODULE_COUNT];

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_TANK_START && index < DATA_COUNT) {
                int tankData = index - DATA_TANK_START;
                HbmFluidTank tank = tankByFlatIndex(tankData / 4);
                return switch (tankData % 4) {
                    case 0 -> tank.type().oldId();
                    case 1 -> tank.capacity();
                    case 2 -> tank.amount();
                    case 3 -> tank.pressure();
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
                HbmFluidTank tank = tankByFlatIndex(tankData / 4);
                switch (tankData % 4) {
                    case 0 -> tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                    case 1 -> tank.setCapacity(value);
                    case 2 -> tank.setAmount(value);
                    case 3 -> tank.setPressure(value);
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

    public AssemblyFactoryBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ASSEMBLY_FACTORY.get(), pos, blockState);
        for (int module = 0; module < MODULE_COUNT; module++) {
            this.inputTanks[module] = new HbmFluidTank(RECIPE_TANK_CAPACITY);
            this.outputTanks[module] = new HbmFluidTank(RECIPE_TANK_CAPACITY);
            this.workTime[module] = 100;
            this.currentDemand[module] = 100;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AssemblyFactoryBlockEntity factory) {
        if (level.isClientSide) {
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
                "message.reinhardtshbm.power.assembly_factory",
                this.lastInput,
                totalDemand(),
                this.energyStored,
                this.energyCapacity,
                totalCompletedCycles()
        );
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new AssemblyFactoryFluidHandler(-1);
    }

    public IFluidHandler fluidHandler(BlockPos accessorPos, @Nullable Direction side) {
        if (isCoolantAccessor(accessorPos, side)) {
            return new AssemblyFactoryFluidHandler(-2);
        }
        int module = moduleForAccessor(accessorPos, side);
        return new AssemblyFactoryFluidHandler(module >= 0 ? module : -3);
    }

    public boolean isAutomationPort(BlockPos accessorPos) {
        return allowsAutomationPort(accessorPos, null);
    }

    public boolean allowsAutomationPort(BlockPos accessorPos, @Nullable Direction side) {
        return isCoolantAccessor(accessorPos, side) || moduleForAccessor(accessorPos, side) >= 0;
    }

    public HbmFluidTank inputTank(int module) {
        return this.inputTanks[Math.max(0, Math.min(MODULE_COUNT - 1, module))];
    }

    public HbmFluidTank outputTank(int module) {
        return this.outputTanks[Math.max(0, Math.min(MODULE_COUNT - 1, module))];
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
            int module = moduleForBlueprintSlot(slot);
            clearSelectionIfBlueprintNoLongerAllowsIt(module);
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

    public int[] getSlotsForAccessor(BlockPos accessorPos, @Nullable Direction side) {
        int module = moduleForAccessor(accessorPos, side);
        if (module < 0) {
            return AUTOMATION_SLOTS;
        }
        int[] slots = new int[MODULE_INPUT_COUNT + MODULE_COUNT];
        int index = 0;
        int inputStart = inputSlotStart(module);
        for (int slot = inputStart; slot < inputStart + MODULE_INPUT_COUNT; slot++) {
            slots[index++] = slot;
        }
        for (int outputModule = 0; outputModule < MODULE_COUNT; outputModule++) {
            slots[index++] = outputSlot(outputModule);
        }
        return slots;
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
        return Component.translatable("container.reinhardtshbm.assembly_factory");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AssemblyFactoryMenu(containerId, playerInventory, this, this.menuData);
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
        for (int module = 0; module < MODULE_COUNT; module++) {
            tag.put("InputTank" + module, this.inputTanks[module].save());
            tag.put("OutputTank" + module, this.outputTanks[module].save());
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
        for (int module = 0; module < MODULE_COUNT; module++) {
            this.inputTanks[module].load(tag.getCompound("InputTank" + module));
            this.outputTanks[module].load(tag.getCompound("OutputTank" + module));
            this.progress[module] = tag.getInt("Progress" + module);
            this.completedCycles[module] = tag.getInt("CompletedCycles" + module);
            this.selectedRecipeIds[module] = tag.contains("SelectedRecipe" + module)
                    ? ResourceLocation.tryParse(tag.getString("SelectedRecipe" + module))
                    : null;
        }
        if (tag.contains("DidProcessMask")) {
            int mask = tag.getInt("DidProcessMask");
            for (int module = 0; module < MODULE_COUNT; module++) {
                this.didProcess[module] = (mask & (1 << module)) != 0;
            }
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
        tag.putInt("DidProcessMask", workingMask());
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
        int start = inputSlotStart(module);
        for (int slot = start; slot < start + MODULE_INPUT_COUNT; slot++) {
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
        Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = selectedRecipe(module, this.level);
        if (selectedRecipe.isEmpty()) {
            return false;
        }
        int inputIndex = slot - inputSlotStart(module);
        for (RecipeHolder<AssemblyMachineRecipe> holder : selectedChoiceVariants(this.level, module, selectedRecipe.get())) {
            List<AssemblyMachineRecipe.CountedIngredient> ingredients = holder.value().ingredients();
            if (inputIndex >= 0 && inputIndex < ingredients.size() && ingredients.get(inputIndex).ingredient().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public Optional<ResourceLocation> selectedRecipeId(int module) {
        return isValidModule(module) ? Optional.ofNullable(this.selectedRecipeIds[module]) : Optional.empty();
    }

    public void setSelectedRecipe(int module, @Nullable ResourceLocation recipeId) {
        if (!isValidModule(module)) {
            return;
        }
        if (recipeId != null && this.level != null) {
            Optional<RecipeHolder<AssemblyMachineRecipe>> recipe = findRecipe(this.level, module, recipeId);
            recipeId = recipe.map(RecipeHolder::id).orElse(null);
        }
        if (this.selectedRecipeIds[module] == recipeId || (this.selectedRecipeIds[module] != null && this.selectedRecipeIds[module].equals(recipeId))) {
            return;
        }
        this.selectedRecipeIds[module] = recipeId;
        this.progress[module] = 0;
        setChangedAndSync(true);
    }

    public Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe(int module, Level level) {
        if (!isValidModule(module) || this.selectedRecipeIds[module] == null) {
            return Optional.empty();
        }
        return findRecipe(level, module, this.selectedRecipeIds[module])
                .filter(holder -> holder.value().isVisibleForPool(installedBlueprintPool(module)));
    }

    public List<RecipeHolder<AssemblyMachineRecipe>> availableRecipes(Level level, int module) {
        return AssemblyMachineRecipe.collapseDisplayChoices(activeVisibleRecipes(level, module));
    }

    private List<RecipeHolder<AssemblyMachineRecipe>> activeVisibleRecipes(Level level, int module) {
        if (!isValidModule(module)) {
            return List.of();
        }
        Optional<String> pool = installedBlueprintPool(module);
        List<RecipeHolder<AssemblyMachineRecipe>> visibleRecipes = level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(pool))
                .toList();
        return AssemblyMachineRecipe.activeVariants(visibleRecipes);
    }

    private List<RecipeHolder<AssemblyMachineRecipe>> selectedChoiceVariants(Level level, int module, RecipeHolder<AssemblyMachineRecipe> selectedRecipe) {
        return activeVisibleRecipes(level, module).stream()
                .filter(holder -> AssemblyMachineRecipe.sameDisplayChoice(selectedRecipe, holder))
                .toList();
    }

    private Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipeForInputs(Level level, int module, RecipeHolder<AssemblyMachineRecipe> selectedRecipe) {
        AssemblyMachineRecipe.Input input = new AssemblyMachineRecipe.Input(inputStacks(module), inputFluidStacks(module));
        return selectedChoiceVariants(level, module, selectedRecipe).stream()
                .filter(holder -> holder.value().matches(input, level))
                .filter(holder -> canOutput(module, holder.value().result()))
                .filter(holder -> canFitFluidOutput(module, holder.value()))
                .findFirst();
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
            boolean working = this.getBlockState().hasProperty(AssemblyFactoryBlock.LIT)
                    && this.getBlockState().getValue(AssemblyFactoryBlock.LIT);
            for (AssemfacArm arm : this.clientAnimations) {
                arm.update(working ? this.didProcess : null);
            }
        }
        this.lastClientAnimationTick = gameTime;
    }

    public boolean clientFrame() {
        return this.clientFrame;
    }

    public double clientSlider(int group, float partialTick) {
        return group >= 0 && group < this.clientAnimations.length ? this.clientAnimations[group].slider(partialTick) : 0.0D;
    }

    public double[] clientArmPositions(int group, boolean saw, float partialTick) {
        if (group < 0 || group >= this.clientAnimations.length) {
            return new double[] {0.0D, 0.0D, 0.0D, 0.0D, 0.0D};
        }
        return (saw ? this.clientAnimations[group].saw : this.clientAnimations[group].striker).positions(partialTick);
    }

    private void tickServer(Level level) {
        this.energyStored = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.energyStored, this.energyCapacity);

        long nextCapacity = 0L;
        boolean anyWorking = false;
        int previousWorkingMask = workingMask();
        boolean sync = level.getGameTime() % 10L == 0L;
        for (int module = 0; module < MODULE_COUNT; module++) {
            this.didProcess[module] = false;
            Optional<RecipeHolder<AssemblyMachineRecipe>> recipeHolder = getSelectedRecipe(module, level);
            this.hasRecipe[module] = recipeHolder.isPresent();
            if (recipeHolder.isEmpty()) {
                this.progress[module] = 0;
                this.workTime[module] = 100;
                this.currentDemand[module] = 100;
                stopWorkingSound(level, module);
                continue;
            }

            RecipeHolder<AssemblyMachineRecipe> selectedHolder = recipeHolder.get();
            AssemblyMachineRecipe recipe = selectedRecipeForInputs(level, module, selectedHolder)
                    .orElse(selectedHolder)
                    .value();
            nextCapacity += recipe.power() * 100L;
            setupTanks(module, recipe);
            this.workTime[module] = currentWorkTime(recipe);
            this.currentDemand[module] = currentDemand(recipe);

            if (!canProcess(module, recipe) || this.energyStored < this.currentDemand[module] || !canCool()) {
                stopWorkingSound(level, module);
                continue;
            }

            this.energyStored -= this.currentDemand[module];
            this.progress[module]++;
            this.didProcess[module] = true;
            anyWorking = true;
            this.waterTank.drain(water(), 100, false);
            this.spentSteamTank.fill(spentSteam(), 100, false);
            playWorkingSound(level, module);

            if (this.progress[module] >= this.workTime[module]) {
                finishRecipe(module, recipe);
                this.progress[module] = 0;
                this.completedCycles[module]++;
            }
        }

        this.energyCapacity = Math.max(Math.max(BASE_ENERGY_CAPACITY, nextCapacity), this.energyStored);
        setLit(anyWorking);
        boolean workingChanged = previousWorkingMask != workingMask();
        if (anyWorking || sync || workingChanged) {
            setChangedAndSync(sync || workingChanged);
        }
    }

    private Optional<RecipeHolder<AssemblyMachineRecipe>> getSelectedRecipe(int module, Level level) {
        if (!isValidModule(module) || this.selectedRecipeIds[module] == null) {
            return Optional.empty();
        }
        Optional<RecipeHolder<AssemblyMachineRecipe>> recipe = findRecipe(level, module, this.selectedRecipeIds[module]);
        if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool(module))) {
            this.selectedRecipeIds[module] = null;
            return Optional.empty();
        }
        if (!recipe.get().id().equals(this.selectedRecipeIds[module])) {
            this.selectedRecipeIds[module] = recipe.get().id();
        }
        return recipe;
    }

    private Optional<RecipeHolder<AssemblyMachineRecipe>> findRecipe(Level level, int module, ResourceLocation recipeId) {
        List<RecipeHolder<AssemblyMachineRecipe>> displayRecipes = availableRecipes(level, module);
        for (RecipeHolder<AssemblyMachineRecipe> holder : displayRecipes) {
            if (holder.id().equals(recipeId)) {
                return Optional.of(holder);
            }
        }
        Optional<RecipeHolder<AssemblyMachineRecipe>> rawRecipe = activeVisibleRecipes(level, module).stream()
                .filter(holder -> holder.id().equals(recipeId))
                .findFirst();
        if (rawRecipe.isPresent()) {
            for (RecipeHolder<AssemblyMachineRecipe> holder : displayRecipes) {
                if (AssemblyMachineRecipe.sameDisplayChoice(holder, rawRecipe.get())) {
                    return Optional.of(holder);
                }
            }
        }
        return Optional.empty();
    }

    private int selectedRecipeMenuValue(int module) {
        if (!isValidModule(module) || this.selectedRecipeIds[module] == null || this.level == null) {
            return 0;
        }
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = availableRecipes(this.level, module);
        ResourceLocation displayRecipeId = findRecipe(this.level, module, this.selectedRecipeIds[module])
                .map(RecipeHolder::id)
                .orElse(this.selectedRecipeIds[module]);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(displayRecipeId)) {
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
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = availableRecipes(this.level, module);
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
        Optional<RecipeHolder<AssemblyMachineRecipe>> recipe = findRecipe(this.level, module, this.selectedRecipeIds[module]);
        if (recipe.isEmpty() || !recipe.get().value().isVisibleForPool(installedBlueprintPool(module))) {
            this.selectedRecipeIds[module] = null;
        }
    }

    private void setupTanks(int module, AssemblyMachineRecipe recipe) {
        if (!recipe.inputFluids().isEmpty()) {
            AssemblyMachineRecipe.AssemblyFluidStack input = recipe.inputFluids().getFirst();
            this.inputTanks[module].setCapacity(Math.max(Math.max(this.inputTanks[module].amount(), input.amount() * 2), RECIPE_TANK_CAPACITY));
            this.inputTanks[module].conform(input.type(), input.pressure());
        } else if (this.inputTanks[module].amount() == 0) {
            this.inputTanks[module].setCapacity(RECIPE_TANK_CAPACITY);
            this.inputTanks[module].clear();
        }

        if (!recipe.outputFluids().isEmpty()) {
            AssemblyMachineRecipe.AssemblyFluidStack output = recipe.outputFluids().getFirst();
            this.outputTanks[module].setCapacity(Math.max(Math.max(this.outputTanks[module].amount(), output.amount() * 2), RECIPE_TANK_CAPACITY));
            this.outputTanks[module].conform(output.type(), output.pressure());
        } else if (this.outputTanks[module].amount() == 0) {
            this.outputTanks[module].setCapacity(RECIPE_TANK_CAPACITY);
            this.outputTanks[module].clear();
        }
    }

    private boolean canProcess(int module, AssemblyMachineRecipe recipe) {
        if (!recipe.matches(new AssemblyMachineRecipe.Input(inputStacks(module), inputFluidStacks(module)), this.level)) {
            return false;
        }
        return canOutput(module, recipe.result()) && canFitFluidOutput(module, recipe);
    }

    private boolean canOutput(int module, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(outputSlot(module));
        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    private boolean canFitFluidOutput(int module, AssemblyMachineRecipe recipe) {
        if (recipe.outputFluids().size() > 1) {
            return false;
        }
        if (recipe.outputFluids().isEmpty()) {
            return true;
        }
        AssemblyMachineRecipe.AssemblyFluidStack output = recipe.outputFluids().getFirst();
        HbmFluidTank tank = this.outputTanks[module];
        return tank.type() == output.type()
                && tank.pressure() == output.pressure()
                && tank.amount() + output.amount() <= tank.capacity();
    }

    private void finishRecipe(int module, AssemblyMachineRecipe recipe) {
        consumeInputs(module, recipe);
        ItemStack result = recipe.result().copy();
        ItemStack output = this.items.get(outputSlot(module));
        if (output.isEmpty()) {
            this.items.set(outputSlot(module), result);
        } else {
            output.grow(result.getCount());
        }
        if (!recipe.outputFluids().isEmpty()) {
            AssemblyMachineRecipe.AssemblyFluidStack fluid = recipe.outputFluids().getFirst();
            this.outputTanks[module].fill(fluid.type(), fluid.amount(), fluid.pressure(), false);
        }
    }

    private void consumeInputs(int module, AssemblyMachineRecipe recipe) {
        List<AssemblyMachineRecipe.CountedIngredient> ingredients = recipe.ingredients();
        int start = inputSlotStart(module);
        for (int input = 0; input < Math.min(ingredients.size(), MODULE_INPUT_COUNT); input++) {
            int slot = start + input;
            ItemStack stack = this.items.get(slot);
            AssemblyMachineRecipe.CountedIngredient ingredient = ingredients.get(input);
            if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                stack.shrink(ingredient.count());
                if (stack.isEmpty()) {
                    this.items.set(slot, ItemStack.EMPTY);
                }
            }
        }
        if (!recipe.inputFluids().isEmpty()) {
            AssemblyMachineRecipe.AssemblyFluidStack fluid = recipe.inputFluids().getFirst();
            this.inputTanks[module].drain(fluid.type(), fluid.amount(), false);
        }
    }

    private List<ItemStack> inputStacks(int module) {
        ArrayList<ItemStack> stacks = new ArrayList<>(MODULE_INPUT_COUNT);
        int start = inputSlotStart(module);
        for (int slot = start; slot < start + MODULE_INPUT_COUNT; slot++) {
            stacks.add(this.items.get(slot));
        }
        return stacks;
    }

    private List<HbmFluidStack> inputFluidStacks(int module) {
        HbmFluidTank tank = this.inputTanks[module];
        return List.of(new HbmFluidStack(tank.type(), tank.amount(), tank.pressure()));
    }

    private int currentWorkTime(AssemblyMachineRecipe recipe) {
        double speed = 1.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3) / 3.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3);
        speed *= 2.0D;
        return Math.max(1, (int) Math.ceil(recipe.duration() / speed));
    }

    private int currentDemand(AssemblyMachineRecipe recipe) {
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

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof AssemblyFactoryBlock && state.getValue(AssemblyFactoryBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(AssemblyFactoryBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void playWorkingSound(Level level, int module) {
        if (!this.wasWorking[module]) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.25F, 1.25F + level.random.nextFloat() * 0.25F);
            this.wasWorking[module] = true;
            this.motorSoundCycle[module] = 0;
            this.strikeSoundCycle[module] = 0;
        }
        if (this.motorSoundCycle[module] <= 0) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.ASSEMBLER_MOTOR.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
            this.motorSoundCycle[module] = 20;
        }
        this.motorSoundCycle[module]--;
        if (this.strikeSoundCycle[module] <= 0) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.ASSEMBLER_STRIKE.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
            this.strikeSoundCycle[module] = 8 + level.random.nextInt(10);
        }
        this.strikeSoundCycle[module]--;
    }

    private void stopWorkingSound(Level level, int module) {
        if (!this.wasWorking[module]) {
            return;
        }
        level.playSound(null, this.worldPosition, HbmSoundEvents.ASSEMBLER_STOP.get(), SoundSource.BLOCKS, 0.25F, 1.5F);
        this.wasWorking[module] = false;
        this.motorSoundCycle[module] = 0;
        this.strikeSoundCycle[module] = 0;
    }

    private int moduleForAccessor(BlockPos accessorPos, @Nullable Direction side) {
        List<Port> ports = ioAccessorPorts();
        for (int module = 0; module < ports.size(); module++) {
            Port port = ports.get(module);
            if (port.pos().equals(accessorPos) && (side == null || port.face() == side)) {
                return module;
            }
        }
        return -1;
    }

    private boolean isCoolantAccessor(BlockPos accessorPos, @Nullable Direction side) {
        for (Port port : coolantAccessorPorts()) {
            if (port.pos().equals(accessorPos) && (side == null || port.face() == side)) {
                return true;
            }
        }
        return false;
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
        if (tank < MODULE_COUNT) {
            return this.inputTanks[Math.max(0, tank)];
        }
        if (tank < MODULE_COUNT * 2) {
            return this.outputTanks[Math.max(0, tank - MODULE_COUNT)];
        }
        return tank == MODULE_COUNT * 2 ? this.waterTank : this.spentSteamTank;
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

    private int workingMask() {
        int mask = 0;
        for (int module = 0; module < MODULE_COUNT; module++) {
            if (this.didProcess[module]) {
                mask |= 1 << module;
            }
        }
        return mask;
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

    public static int outputSlot(int module) {
        return MODULE_START + module * MODULE_STRIDE + MODULE_OUTPUT_OFFSET;
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
        return local >= MODULE_INPUT_OFFSET && local < MODULE_INPUT_OFFSET + MODULE_INPUT_COUNT;
    }

    private static boolean isOutputSlot(int slot) {
        if (slot < MODULE_START || slot >= SLOT_COUNT) {
            return false;
        }
        return (slot - MODULE_START) % MODULE_STRIDE == MODULE_OUTPUT_OFFSET;
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
        int[] slots = new int[MODULE_COUNT * (MODULE_INPUT_COUNT + 1)];
        int index = 0;
        for (int module = 0; module < MODULE_COUNT; module++) {
            for (int slot = inputSlotStart(module); slot < inputSlotStart(module) + MODULE_INPUT_COUNT; slot++) {
                slots[index++] = slot;
            }
            slots[index++] = outputSlot(module);
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

    private final class AssemblyFactoryFluidHandler implements IFluidHandler {
        private final int module;

        private AssemblyFactoryFluidHandler(int module) {
            this.module = module;
        }

        @Override
        public int getTanks() {
            if (this.module == -3) {
                return 0;
            }
            return this.module == -2 ? 2 : this.module >= 0 ? 2 : TANK_COUNT;
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
            if (fluid.isNone()) {
                return false;
            }
            HbmFluidTank hbmTank = exposedTank(tank);
            return hbmTank != null && canFillTank(hbmTank, fluid);
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
                int accepted = tank.fill(fluid, remaining, action.simulate());
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
                if (remaining <= 0 || tank.type() != fluid || tank.pressure() != 0) {
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
                if (tank.amount() <= 0 || tank.type().isNone() || tank.pressure() != 0) {
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
                return tank == 0 ? inputTanks[this.module] : tank == 1 ? outputTanks[this.module] : null;
            }
            return tank >= 0 && tank < TANK_COUNT ? tankByFlatIndex(tank) : null;
        }

        private List<HbmFluidTank> fillableTanks() {
            if (this.module == -3) {
                return List.of();
            }
            if (this.module == -2) {
                return List.of(waterTank);
            }
            if (this.module >= 0) {
                return List.of(inputTanks[this.module]);
            }
            ArrayList<HbmFluidTank> tanks = new ArrayList<>();
            tanks.addAll(List.of(inputTanks));
            tanks.add(waterTank);
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
                return List.of(outputTanks[this.module]);
            }
            ArrayList<HbmFluidTank> tanks = new ArrayList<>();
            tanks.addAll(List.of(outputTanks));
            tanks.add(spentSteamTank);
            return tanks;
        }

        private boolean canFillTank(HbmFluidTank tank, HbmFluidDefinition fluid) {
            return !fluid.isNone() && (tank.type().isNone() || tank.type() == fluid) && tank.pressure() == 0;
        }
    }

    private record Port(BlockPos pos, Direction face) {
    }

    public static class AssemfacArm {
        private final Arm striker;
        private final Arm saw;
        private final Random random = new Random();
        private double slider;
        private double prevSlider;
        private boolean direction;
        private int slideDelay;

        private AssemfacArm(int group) {
            this.striker = new Arm(false, group == 0 ? 0 : 3);
            this.saw = new Arm(true, group == 0 ? 1 : 2);
            this.slideDelay = 140 + this.random.nextInt(161);
        }

        private void update(@Nullable boolean[] workingModules) {
            this.prevSlider = this.slider;
            boolean strikerWorking = this.striker.isServicedModuleWorking(workingModules, this.slider);
            boolean sawWorking = this.saw.isServicedModuleWorking(workingModules, this.slider);
            this.striker.update(strikerWorking);
            this.saw.update(sawWorking);
            if (!strikerWorking && !sawWorking) {
                return;
            }
            if (this.slideDelay > 0) {
                this.slideDelay--;
                return;
            }
            double speed = 0.1D;
            this.slider += this.direction ? speed : -speed;
            if (this.slider >= 1.0D || this.slider <= 0.0D) {
                this.slider = Math.max(0.0D, Math.min(1.0D, this.slider));
                this.direction = !this.direction;
                this.slideDelay = 140 + this.random.nextInt(161);
            }
        }

        private double slider(float partialTick) {
            return this.prevSlider + (this.slider - this.prevSlider) * partialTick;
        }
    }

    public static class Arm {
        private static final double[][] STRIKER_POSITIONS = {
                {10.0D, 10.0D, -10.0D},
                {15.0D, 15.0D, -15.0D},
                {25.0D, 10.0D, -15.0D},
                {30.0D, 0.0D, -10.0D},
                {-10.0D, 10.0D, 0.0D},
                {-20.0D, 30.0D, -15.0D}
        };
        private static final double[][] SAW_POSITIONS = {
                {-15.0D, 15.0D, -10.0D},
                {-15.0D, 15.0D, -15.0D},
                {-15.0D, 15.0D, 10.0D},
                {-15.0D, 15.0D, 15.0D},
                {-15.0D, 15.0D, 2.0D},
                {-15.0D, 15.0D, -2.0D}
        };

        private final boolean saw;
        private final double[] angles = new double[4];
        private final double[] previous = new double[4];
        private final double[] target = new double[4];
        private final Random random = new Random();
        private final int recipeIndex;
        private double sawAngle;
        private double previousSawAngle;
        private int delay;
        private int state;

        private Arm(boolean saw, int recipeIndex) {
            this.saw = saw;
            this.recipeIndex = recipeIndex;
            choosePosition();
        }

        private boolean isServicedModuleWorking(@Nullable boolean[] workingModules, double slider) {
            if (workingModules == null || workingModules.length < MODULE_COUNT) {
                return false;
            }
            int module = this.recipeIndex;
            if (slider > 0.5D) {
                module += module % 2 == 0 ? 1 : -1;
            }
            return module >= 0 && module < workingModules.length && workingModules[module];
        }

        private void update(boolean working) {
            System.arraycopy(this.angles, 0, this.previous, 0, this.angles.length);
            this.previousSawAngle = this.sawAngle;
            if (!working) {
                for (int index = 0; index < this.target.length; index++) {
                    this.target[index] = 0.0D;
                }
                move();
                return;
            }
            if (this.saw && this.state == 2) {
                this.sawAngle += 45.0D;
            }
            if (this.delay > 0) {
                this.delay--;
                return;
            }
            switch (this.state) {
                case 0 -> {
                    if (move()) {
                        this.delay = 2;
                        this.target[3] = this.saw ? -0.375D : -0.75D;
                        this.state = 1;
                    }
                }
                case 1 -> {
                    if (move()) {
                        if (this.saw) {
                            this.target[2] = -this.target[2];
                            this.state = 2;
                        } else {
                            this.target[3] = 0.0D;
                            this.state = 3;
                        }
                    }
                }
                case 2 -> {
                    if (move()) {
                        this.target[3] = 0.0D;
                        this.state = 3;
                    }
                }
                case 3 -> {
                    if (move()) {
                        this.delay = 2 + this.random.nextInt(5);
                        choosePosition();
                        this.state = 0;
                    }
                }
                default -> this.state = 0;
            }
        }

        private void choosePosition() {
            double[][] positions = this.saw ? SAW_POSITIONS : STRIKER_POSITIONS;
            double[] chosen = positions[this.random.nextInt(positions.length)];
            this.target[0] = chosen[0];
            this.target[1] = chosen[1];
            this.target[2] = chosen[2];
        }

        private boolean move() {
            boolean didMove = false;
            for (int index = 0; index < this.angles.length; index++) {
                if (this.angles[index] == this.target[index]) {
                    continue;
                }
                didMove = true;
                double speed = index == 3 ? (this.saw ? 0.125D : 0.5D) : 15.0D;
                double delta = Math.abs(this.angles[index] - this.target[index]);
                if (delta <= speed) {
                    this.angles[index] = this.target[index];
                } else if (this.angles[index] < this.target[index]) {
                    this.angles[index] += speed;
                } else {
                    this.angles[index] -= speed;
                }
            }
            return !didMove;
        }

        private double[] positions(float partialTick) {
            return new double[] {
                    lerp(this.previous[0], this.angles[0], partialTick),
                    lerp(this.previous[1], this.angles[1], partialTick),
                    lerp(this.previous[2], this.angles[2], partialTick),
                    lerp(this.previous[3], this.angles[3], partialTick),
                    lerp(this.previousSawAngle, this.sawAngle, partialTick)
            };
        }

        private static double lerp(double previous, double current, float partialTick) {
            return previous + (current - previous) * partialTick;
        }
    }
}
