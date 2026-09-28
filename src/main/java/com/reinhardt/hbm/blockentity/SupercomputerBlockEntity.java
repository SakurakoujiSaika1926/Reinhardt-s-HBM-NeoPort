package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.SupercomputerBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.menu.SupercomputerMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.MachineRecipeCache;
import com.reinhardt.hbm.recipe.SupercomputerRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
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
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Event-driven high-version implementation of TileEntityMachineSuperComputer. */
public final class SupercomputerBlockEntity extends BlockEntity
        implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int INPUT_START = 2;
    public static final int INPUT_END = 5;
    public static final int OUTPUT_START = 5;
    public static final int OUTPUT_END = 8;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 17;
    public static final long BASE_MAX_POWER = 100_000L;
    public static final int BASE_TANK_CAPACITY = 4_000;

    private static final int[] AUTOMATION_SLOTS = {2, 3, 4, 5, 6, 7};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final NonNullList<ItemStack> observedRecipeItems = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(BASE_TANK_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(BASE_TANK_CAPACITY);

    private long power;
    private long maxPower = BASE_MAX_POWER;
    private long lastInput;
    private int progress;
    private boolean processing;
    private int completedCycles;

    private long observedRecipeCacheGeneration = Long.MIN_VALUE;
    private boolean recipeCatalogDirty = true;
    private boolean recipeStateDirty = true;
    private List<RecipeHolder<SupercomputerRecipe>> cachedRecipeCatalog = List.of();
    @Nullable
    private ResourceLocation selectedRecipeId;
    @Nullable
    private RecipeHolder<SupercomputerRecipe> cachedRecipe;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            SupercomputerRecipe recipe = SupercomputerBlockEntity.this.cachedRecipe == null
                    ? null : SupercomputerBlockEntity.this.cachedRecipe.value();
            return switch (index) {
                case 0 -> (int) SupercomputerBlockEntity.this.power;
                case 1 -> (int) SupercomputerBlockEntity.this.maxPower;
                case 2 -> (int) SupercomputerBlockEntity.this.lastInput;
                case 3 -> SupercomputerBlockEntity.this.progress;
                case 4 -> recipe == null ? 1 : recipe.duration();
                case 5 -> recipe == null ? 0 : recipe.power();
                case 6 -> SupercomputerBlockEntity.this.processing ? 1 : 0;
                case 7 -> SupercomputerBlockEntity.this.inputTank.type().oldId();
                case 8 -> SupercomputerBlockEntity.this.inputTank.amount();
                case 9 -> SupercomputerBlockEntity.this.inputTank.capacity();
                case 10 -> SupercomputerBlockEntity.this.inputTank.pressure();
                case 11 -> SupercomputerBlockEntity.this.outputTank.type().oldId();
                case 12 -> SupercomputerBlockEntity.this.outputTank.amount();
                case 13 -> SupercomputerBlockEntity.this.outputTank.capacity();
                case 14 -> SupercomputerBlockEntity.this.outputTank.pressure();
                case 15 -> selectedRecipeIndex() + 1;
                case 16 -> SupercomputerBlockEntity.this.completedCycles;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> SupercomputerBlockEntity.this.power = Math.max(0, value);
                case 1 -> SupercomputerBlockEntity.this.maxPower = Math.max(BASE_MAX_POWER, value);
                case 2 -> SupercomputerBlockEntity.this.lastInput = Math.max(0, value);
                case 3 -> SupercomputerBlockEntity.this.progress = Math.max(0, value);
                case 6 -> SupercomputerBlockEntity.this.processing = value != 0;
                case 7 -> SupercomputerBlockEntity.this.inputTank.setType(
                        HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 8 -> SupercomputerBlockEntity.this.inputTank.setAmount(value);
                case 9 -> SupercomputerBlockEntity.this.inputTank.setCapacity(value);
                case 10 -> SupercomputerBlockEntity.this.inputTank.setPressure(value);
                case 11 -> SupercomputerBlockEntity.this.outputTank.setType(
                        HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 12 -> SupercomputerBlockEntity.this.outputTank.setAmount(value);
                case 13 -> SupercomputerBlockEntity.this.outputTank.setCapacity(value);
                case 14 -> SupercomputerBlockEntity.this.outputTank.setPressure(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SupercomputerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SUPERCOMPUTER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SupercomputerBlockEntity computer) {
        if (level.isClientSide) {
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, computer);
        computer.tickServer(level);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public boolean processing() {
        return this.processing;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return capabilityPorts().stream().map(SupercomputerBlock.CapabilityPort::connector).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return capabilityPorts().stream().anyMatch(port ->
                port.connector().equals(connectorPos) && port.face() == machineSide);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (this.power >= this.maxPower) {
            return 0L;
        }
        long demand = this.cachedRecipe == null ? 100L : Math.max(1L, this.cachedRecipe.value().power());
        return Math.min(demand, this.maxPower - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        long previousPower = this.power;
        long previousLastInput = this.lastInput;
        this.power = Math.min(this.maxPower, this.power + Math.max(0L, receivedInput));
        this.lastInput = Math.max(0L, receivedInput);
        if (this.power != previousPower || this.lastInput != previousLastInput) {
            markRuntimeChanged();
        }
    }

    @Override
    public Component getPowerStatus() {
        SupercomputerRecipe recipe = this.cachedRecipe == null ? null : this.cachedRecipe.value();
        return Component.translatable(
                "message.reinhardtshbm.power.supercomputer",
                this.lastInput,
                recipe == null ? 0 : recipe.power(),
                this.power,
                this.maxPower,
                recipe == null ? 0 : this.progress * 100 / Math.max(1, recipe.duration()),
                this.completedCycles
        );
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            inventoryChanged(slot);
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        if (!removed.isEmpty()) {
            inventoryChanged(slot);
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        ItemStack stored = stack;
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize(stored)) {
            stored = stored.copyWithCount(getMaxStackSize(stored));
        }
        this.items.set(slot, stored);
        inventoryChanged(slot);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot == BLUEPRINT_SLOT) {
            return BlueprintItem.isBlueprint(stack);
        }
        return slot >= INPUT_START && slot < INPUT_END && acceptsInputAt(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= INPUT_START && slot < INPUT_END && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_END || isCloggedInput(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.progress = 0;
        invalidateRecipeState();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.supercomputer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SupercomputerMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    public List<RecipeHolder<SupercomputerRecipe>> recipes(Level level) {
        ensureRecipeCatalog(level);
        return this.cachedRecipeCatalog;
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return Optional.ofNullable(this.selectedRecipeId);
    }

    public Optional<RecipeHolder<SupercomputerRecipe>> selectedRecipe(Level level) {
        refreshRecipeState(level);
        return Optional.ofNullable(this.cachedRecipe);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        if (Objects.equals(this.selectedRecipeId, recipeId)) {
            return;
        }
        this.selectedRecipeId = recipeId;
        this.progress = 0;
        invalidateRecipeState();
        setChanged();
        syncClient();
    }

    public boolean acceptsInputAt(int slot, ItemStack stack) {
        if (this.level == null || stack.isEmpty() || slot < INPUT_START || slot >= INPUT_END) {
            return false;
        }
        refreshRecipeState(this.level);
        if (this.cachedRecipe == null) {
            return false;
        }
        int ingredientIndex = slot - INPUT_START;
        List<SupercomputerRecipe.CountedIngredient> ingredients = this.cachedRecipe.value().ingredients();
        return ingredientIndex < ingredients.size() && ingredients.get(ingredientIndex).ingredient().test(stack);
    }

    public boolean isAutomationPort(BlockPos queriedPos) {
        return queriedPos.equals(this.worldPosition) || portPositions().contains(queriedPos);
    }

    public boolean allowsAutomationPort(BlockPos queriedPos, @Nullable Direction side) {
        if (side == null) {
            return isAutomationPort(queriedPos);
        }
        return capabilityPorts().stream().anyMatch(port ->
                port.host().equals(queriedPos) && port.face() == side);
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        return allowsAutomationPort(queriedPos, side) ? new SupercomputerFluidHandler() : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.putLong("MaxPower", this.maxPower);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putBoolean("Processing", this.processing);
        tag.putInt("CompletedCycles", this.completedCycles);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        if (this.selectedRecipeId != null) {
            tag.putString("SelectedRecipe", this.selectedRecipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
            this.observedRecipeItems.set(slot, this.items.get(slot).copy());
        }
        this.power = Math.max(0L, tag.getLong("Power"));
        this.maxPower = Math.max(BASE_MAX_POWER, tag.getLong("MaxPower"));
        this.lastInput = Math.max(0L, tag.getLong("LastInput"));
        this.progress = Math.max(0, tag.getInt("Progress"));
        this.processing = tag.getBoolean("Processing");
        this.completedCycles = Math.max(0, tag.getInt("CompletedCycles"));
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.selectedRecipeId = tag.contains("SelectedRecipe")
                ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
        invalidateRecipeCatalog();
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

    private void tickServer(Level level) {
        refreshRecipeState(level);
        SupercomputerRecipe recipe = this.cachedRecipe == null ? null : this.cachedRecipe.value();
        this.maxPower = Math.max(this.power,
                Math.max(BASE_MAX_POWER, recipe == null ? 0L : (long) recipe.power() * 100L));
        boolean batteryChanged = dischargeBattery();

        boolean worked = false;
        if (recipe != null && hasInput(recipe) && canFitOutput(recipe) && this.power >= recipe.power()) {
            this.power -= recipe.power();
            this.progress++;
            worked = true;
            if (this.progress >= recipe.duration()) {
                this.progress = 0;
                consumeInput(recipe);
                produceOutput(level, recipe);
                this.completedCycles++;
                captureRecipeSlotSnapshot();
                invalidateRecipeState();
            }
        } else if (this.progress != 0) {
            // ModuleMachineBase resets progress whenever any process condition fails.
            this.progress = 0;
        }

        if (this.processing != worked) {
            this.processing = worked;
            syncClient();
        }
        if (worked || batteryChanged) {
            markRuntimeChanged();
        }
    }

    private boolean dischargeBattery() {
        long previous = this.power;
        this.power = BatteryPackItem.dischargeIntoMachine(
                this.items.get(BATTERY_SLOT), this.power, this.maxPower);
        return this.power != previous;
    }

    private boolean hasInput(SupercomputerRecipe recipe) {
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            SupercomputerRecipe.CountedIngredient required = recipe.ingredients().get(index);
            ItemStack held = this.items.get(INPUT_START + index);
            if (held.isEmpty() || held.getCount() < required.count() || !required.ingredient().test(held)) {
                return false;
            }
        }
        if (recipe.inputFluids().isEmpty()) {
            return true;
        }
        SupercomputerRecipe.FluidStack required = recipe.inputFluids().getFirst();
        return this.inputTank.type() == required.type()
                && this.inputTank.pressure() == required.pressure()
                && this.inputTank.amount() >= required.amount();
    }

    private boolean canFitOutput(SupercomputerRecipe recipe) {
        ItemStack output = this.items.get(OUTPUT_START);
        if (recipe.outputs().size() > 1) {
            if (!output.isEmpty()) {
                return false;
            }
        } else {
            ItemStack result = recipe.outputs().getFirst().stack();
            if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                    || output.getCount() + result.getCount() > output.getMaxStackSize())) {
                return false;
            }
        }
        if (recipe.outputFluids().isEmpty()) {
            return true;
        }
        SupercomputerRecipe.FluidStack result = recipe.outputFluids().getFirst();
        return (this.outputTank.type().isNone() || this.outputTank.type() == result.type())
                && this.outputTank.pressure() == result.pressure()
                && this.outputTank.amount() + result.amount() <= this.outputTank.capacity();
    }

    private void consumeInput(SupercomputerRecipe recipe) {
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            ItemStack input = this.items.get(INPUT_START + index);
            input.shrink(recipe.ingredients().get(index).count());
            if (input.isEmpty()) {
                this.items.set(INPUT_START + index, ItemStack.EMPTY);
            }
        }
        if (!recipe.inputFluids().isEmpty()) {
            SupercomputerRecipe.FluidStack fluid = recipe.inputFluids().getFirst();
            this.inputTank.drain(fluid.type(), fluid.amount(), false);
        }
    }

    private void produceOutput(Level level, SupercomputerRecipe recipe) {
        ItemStack result = recipe.rollOutput(level.random);
        ItemStack output = this.items.get(OUTPUT_START);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_START, result);
        } else {
            output.grow(result.getCount());
        }
        if (!recipe.outputFluids().isEmpty()) {
            SupercomputerRecipe.FluidStack fluid = recipe.outputFluids().getFirst();
            this.outputTank.fill(fluid.type(), fluid.amount(), false);
        }
    }

    private void refreshRecipeState(Level level) {
        ensureRecipeCatalog(level);
        if (!this.recipeStateDirty) {
            return;
        }
        this.recipeStateDirty = false;
        ResourceLocation previous = this.cachedRecipe == null ? null : this.cachedRecipe.id();
        this.cachedRecipe = findRecipe(this.selectedRecipeId);
        ResourceLocation current = this.cachedRecipe == null ? null : this.cachedRecipe.id();
        if (!Objects.equals(previous, current)) {
            this.progress = 0;
        }
        conformTanks(this.cachedRecipe == null ? null : this.cachedRecipe.value());
        syncClient();
    }

    @Nullable
    private RecipeHolder<SupercomputerRecipe> findRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId == null) {
            return null;
        }
        for (RecipeHolder<SupercomputerRecipe> holder : this.cachedRecipeCatalog) {
            if (holder.id().equals(recipeId)) {
                return holder;
            }
        }
        return null;
    }

    private void conformTanks(@Nullable SupercomputerRecipe recipe) {
        if (recipe == null || recipe.inputFluids().isEmpty()) {
            this.inputTank.clear();
        } else {
            SupercomputerRecipe.FluidStack input = recipe.inputFluids().getFirst();
            this.inputTank.conform(input.type(), input.pressure());
            this.inputTank.setCapacity(Math.max(BASE_TANK_CAPACITY,
                    Math.max(this.inputTank.amount(), input.amount() * 2)));
        }
        if (recipe == null || recipe.outputFluids().isEmpty()) {
            this.outputTank.clear();
        } else {
            SupercomputerRecipe.FluidStack output = recipe.outputFluids().getFirst();
            this.outputTank.conform(output.type(), output.pressure());
            this.outputTank.setCapacity(Math.max(BASE_TANK_CAPACITY,
                    Math.max(this.outputTank.amount(), output.amount() * 2)));
        }
    }

    private boolean acceptsInputFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || this.level == null) {
            return false;
        }
        refreshRecipeState(this.level);
        return this.cachedRecipe != null && !this.cachedRecipe.value().inputFluids().isEmpty()
                && this.cachedRecipe.value().inputFluids().getFirst().type() == fluid;
    }

    private boolean isCloggedInput(int slot) {
        return slot >= INPUT_START && slot < INPUT_END
                && !this.items.get(slot).isEmpty()
                && !acceptsInputAt(slot, this.items.get(slot));
    }

    private int selectedRecipeIndex() {
        if (this.selectedRecipeId == null) {
            return -1;
        }
        for (int index = 0; index < this.cachedRecipeCatalog.size(); index++) {
            if (this.cachedRecipeCatalog.get(index).id().equals(this.selectedRecipeId)) {
                return index;
            }
        }
        return -1;
    }

    private List<BlockPos> portPositions() {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
        return SupercomputerBlock.portPositions(this.worldPosition, facing);
    }

    private List<SupercomputerBlock.CapabilityPort> capabilityPorts() {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
        return SupercomputerBlock.capabilityPorts(this.worldPosition, facing);
    }

    private void ensureRecipeCatalog(Level level) {
        long generation = MachineRecipeCache.generation();
        if (this.observedRecipeCacheGeneration != generation) {
            this.observedRecipeCacheGeneration = generation;
            invalidateRecipeCatalog();
        }
        if (!this.recipeCatalogDirty) {
            return;
        }
        this.cachedRecipeCatalog = level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.SUPERCOMPUTER.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
        this.recipeCatalogDirty = false;
        this.recipeStateDirty = true;
    }

    private void invalidateRecipeCatalog() {
        this.recipeCatalogDirty = true;
        this.cachedRecipeCatalog = List.of();
        invalidateRecipeState();
    }

    private void invalidateRecipeState() {
        this.recipeStateDirty = true;
    }

    private void inventoryChanged(int slot) {
        if (slot >= BLUEPRINT_SLOT && slot < OUTPUT_END) {
            invalidateRecipeState();
        }
    }

    private void captureRecipeSlotSnapshot() {
        for (int slot = BLUEPRINT_SLOT; slot < OUTPUT_END; slot++) {
            this.observedRecipeItems.set(slot, this.items.get(slot).copy());
        }
    }

    @Override
    public void setChanged() {
        for (int slot = BLUEPRINT_SLOT; slot < OUTPUT_END; slot++) {
            ItemStack current = this.items.get(slot);
            ItemStack observed = this.observedRecipeItems.get(slot);
            if (current.getCount() == observed.getCount()
                    && ItemStack.isSameItemSameComponents(current, observed)) {
                continue;
            }
            this.observedRecipeItems.set(slot, current.copy());
            inventoryChanged(slot);
        }
        super.setChanged();
    }

    private void markRuntimeChanged() {
        super.setChanged();
    }

    private void fluidChanged() {
        invalidateRecipeState();
        setChanged();
        syncClient();
    }

    private void syncClient() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(
                    this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private final class SupercomputerFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> SupercomputerBlockEntity.this.inputTank.getFluidInTank(0);
                case 1 -> SupercomputerBlockEntity.this.outputTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> SupercomputerBlockEntity.this.inputTank.capacity();
                case 1 -> SupercomputerBlockEntity.this.outputTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && !stack.isEmpty()
                    && HbmFluids.fromNeoFluid(stack.getFluid())
                    .filter(SupercomputerBlockEntity.this::acceptsInputFluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            Optional<HbmFluidDefinition> fluid = HbmFluids.fromNeoFluid(resource.getFluid());
            if (fluid.isEmpty() || !acceptsInputFluid(fluid.get())) {
                return 0;
            }
            int accepted = SupercomputerBlockEntity.this.inputTank.fill(
                    fluid.get(), resource.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) {
                fluidChanged();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            Optional<HbmFluidDefinition> fluid = HbmFluids.fromNeoFluid(resource.getFluid());
            if (fluid.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = SupercomputerBlockEntity.this.outputTank.drain(
                    fluid.get(), resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                fluidChanged();
            }
            return drained.isEmpty() ? FluidStack.EMPTY
                    : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            HbmFluidStack drained = SupercomputerBlockEntity.this.outputTank.drain(
                    null, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                fluidChanged();
            }
            return drained.isEmpty() ? FluidStack.EMPTY
                    : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }
}
