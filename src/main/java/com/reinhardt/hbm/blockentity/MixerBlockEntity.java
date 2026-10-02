package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.MixerMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.MixerRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

import java.util.List;

public class MixerBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int BATTERY_SLOT = 0;
    public static final int SOLID_INPUT_SLOT = 1;
    public static final int FLUID_IDENTIFIER_SLOT = 2;
    public static final int UPGRADE_START = 3;
    public static final int UPGRADE_END = 5;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 16;
    public static final int INPUT_TANK_CAPACITY = 16_000;
    public static final int OUTPUT_TANK_CAPACITY = 24_000;
    public static final long MAX_POWER = 10_000L;
    public static final int BASE_USAGE = 50;
    private static final int PUSH_PER_PORT = 8_000;
    private static final int[] AUTOMATION_SLOTS = {SOLID_INPUT_SLOT};

    private enum InputLayout {
        DIRECT(0, 1),
        SWAPPED(1, 0),
        SINGLE_TANK_1(0, -1),
        SINGLE_TANK_2(1, -1),
        NONE(-1, -1),
        INVALID(-1, -1);

        private final int input1Tank;
        private final int input2Tank;

        InputLayout(int input1Tank, int input2Tank) {
            this.input1Tank = input1Tank;
            this.input2Tank = input2Tank;
        }

        private int input1Tank() {
            return this.input1Tank;
        }

        private int input2Tank() {
            return this.input2Tank;
        }

        private boolean isValid() {
            return this != INVALID;
        }
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank1 = new HbmFluidTank(INPUT_TANK_CAPACITY);
    private final HbmFluidTank inputTank2 = new HbmFluidTank(INPUT_TANK_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(OUTPUT_TANK_CAPACITY);
    private HbmFluidDefinition configuredOutput = HbmFluids.none();
    private long power;
    private long lastInput;
    private int progress;
    private int processTime = 1;
    private int recipeIndex;
    private int usage = BASE_USAGE;
    private boolean working;
    private float rotation;
    private float prevRotation;
    @Nullable
    private MixerRecipe cachedInputLayoutRecipe;
    private InputLayout cachedInputLayout = InputLayout.INVALID;
    private boolean inputLayoutDirty = true;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) MixerBlockEntity.this.power;
                case 1 -> (int) MixerBlockEntity.this.lastInput;
                case 2 -> MixerBlockEntity.this.progress;
                case 3 -> MixerBlockEntity.this.processTime;
                case 4 -> MixerBlockEntity.this.usage;
                case 5 -> MixerBlockEntity.this.recipeIndex;
                case 6 -> MixerBlockEntity.this.inputTank1.type().oldId();
                case 7 -> MixerBlockEntity.this.inputTank1.amount();
                case 8 -> MixerBlockEntity.this.inputTank1.capacity();
                case 9 -> MixerBlockEntity.this.inputTank2.type().oldId();
                case 10 -> MixerBlockEntity.this.inputTank2.amount();
                case 11 -> MixerBlockEntity.this.inputTank2.capacity();
                case 12 -> MixerBlockEntity.this.outputTank.type().oldId();
                case 13 -> MixerBlockEntity.this.outputTank.amount();
                case 14 -> MixerBlockEntity.this.outputTank.capacity();
                case 15 -> MixerBlockEntity.this.configuredOutput.oldId();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> MixerBlockEntity.this.power = value;
                case 1 -> MixerBlockEntity.this.lastInput = value;
                case 2 -> MixerBlockEntity.this.progress = value;
                case 3 -> MixerBlockEntity.this.processTime = Math.max(1, value);
                case 4 -> MixerBlockEntity.this.usage = Math.max(1, value);
                case 5 -> MixerBlockEntity.this.recipeIndex = Math.max(0, value);
                case 6 -> MixerBlockEntity.this.inputTank1.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 7 -> MixerBlockEntity.this.inputTank1.setAmount(value);
                case 9 -> MixerBlockEntity.this.inputTank2.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 10 -> MixerBlockEntity.this.inputTank2.setAmount(value);
                case 12 -> MixerBlockEntity.this.outputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 13 -> MixerBlockEntity.this.outputTank.setAmount(value);
                case 15 -> MixerBlockEntity.this.configuredOutput = HbmFluids.byOldId(value).orElse(HbmFluids.none());
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MixerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.MIXER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MixerBlockEntity mixer) {
        if (level.isClientSide) {
            mixer.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, mixer);
        mixer.tickServer(level);
    }

    public HbmFluidTank inputTank1() {
        return this.inputTank1;
    }

    public HbmFluidTank inputTank2() {
        return this.inputTank2;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public HbmFluidDefinition configuredOutput() {
        return this.configuredOutput;
    }

    public boolean isWorking() {
        return this.working;
    }

    public float rotation(float partialTick) {
        return this.prevRotation + (this.rotation - this.prevRotation) * partialTick;
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public BlockPos menuBlockPos() {
        return this.worldPosition;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return portsFor(this.worldPosition).stream().map(Port::connectorPos).toList();
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (this.power >= MAX_POWER) {
            return 0L;
        }
        long requested = Math.max(BASE_USAGE, this.usage);
        return Math.min(requested, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.processTime <= 0 ? 0 : this.progress * 100 / this.processTime;
        return Component.translatable(
                "message.reinhardtshbm.power.mixer",
                this.lastInput,
                this.usage,
                this.power,
                MAX_POWER,
                percent
        );
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new MixerFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public static List<Port> portsFor(BlockPos pos) {
        return List.of(
                Port.fromConnector(pos.offset(0, -1, 0), Direction.DOWN),
                Port.fromConnector(pos.offset(1, 0, 0), Direction.EAST),
                Port.fromConnector(pos.offset(-1, 0, 0), Direction.WEST),
                Port.fromConnector(pos.offset(0, 0, 1), Direction.SOUTH),
                Port.fromConnector(pos.offset(0, 0, -1), Direction.NORTH)
        );
    }

    @Override
    public int[] getFluidIdsToCopy() {
        HbmFluidDefinition copied = this.outputTank.amount() > 0 ? this.outputTank.type() : this.configuredOutput;
        return copied.isNone() ? new int[0] : new int[]{copied.oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (fluid == null || fluid.isNone() || !hasOutputRecipes(fluid)) {
            return;
        }
        configureOutput(fluid);
    }

    public void cycleRecipe() {
        List<RecipeHolder<MixerRecipe>> recipes = outputRecipes();
        if (recipes.isEmpty()) {
            if (this.recipeIndex != 0) {
                markInputLayoutDirty();
            }
            this.recipeIndex = 0;
            return;
        }
        this.recipeIndex = (this.recipeIndex + 1) % recipes.size();
        this.progress = 0;
        markInputLayoutDirty();
        setChangedAndSync(true);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
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
        if (slot == SOLID_INPUT_SLOT) {
            this.progress = 0;
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            updateUpgrades();
        }
        setChangedAndSync(false);
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
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        if (slot == FLUID_IDENTIFIER_SLOT) {
            applyIdentifierSlot();
        }
        if (slot == SOLID_INPUT_SLOT) {
            this.progress = 0;
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            updateUpgrades();
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot == SOLID_INPUT_SLOT) {
            return acceptsSolidInput(stack);
        }
        if (slot == FLUID_IDENTIFIER_SLOT) {
            return stack.getItem() instanceof FluidIdentifierItem;
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return isSupportedUpgrade(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SOLID_INPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_mixer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MixerMenu(containerId, playerInventory, this, this.menuData, this.worldPosition);
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
        tag.put("InputTank1", this.inputTank1.save());
        tag.put("InputTank2", this.inputTank2.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putString("ConfiguredOutput", this.configuredOutput.name());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("ProcessTime", this.processTime);
        tag.putInt("RecipeIndex", this.recipeIndex);
        tag.putInt("Usage", this.usage);
        tag.putBoolean("Working", this.working);
        tag.putFloat("Rotation", this.rotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.inputTank1.load(tag.getCompound("InputTank1"));
        this.inputTank2.load(tag.getCompound("InputTank2"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.configuredOutput = HbmFluids.byName(tag.getString("ConfiguredOutput")).orElse(HbmFluids.none());
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = tag.getLong("LastInput");
        this.progress = tag.getInt("Progress");
        this.processTime = Math.max(1, tag.getInt("ProcessTime"));
        this.recipeIndex = Math.max(0, tag.getInt("RecipeIndex"));
        this.usage = Math.max(1, tag.getInt("Usage"));
        this.working = tag.getBoolean("Working");
        this.rotation = tag.getFloat("Rotation");
        this.prevRotation = this.rotation;
        if (this.outputTank.amount() == 0 && !this.configuredOutput.isNone()) {
            this.outputTank.setType(this.configuredOutput);
        }
        this.cachedInputLayoutRecipe = null;
        this.cachedInputLayout = InputLayout.INVALID;
        this.inputLayoutDirty = true;
        updateUpgrades();
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

    private void tickClient() {
        this.prevRotation = this.rotation;
        if (this.working) {
            this.rotation += 20.0F;
        }
        if (this.rotation >= 360.0F) {
            this.rotation -= 360.0F;
            this.prevRotation -= 360.0F;
        }
    }

    private void tickServer(Level level) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        applyIdentifierSlot();
        updateUpgrades();

        boolean wasWorking = this.working;
        this.working = false;
        RecipeHolder<MixerRecipe> holder = selectedRecipe();
        if (holder == null || !canProcess(holder.value())) {
            this.progress = 0;
            syncIfWorkingChanged(wasWorking);
            pushOutput(level);
            return;
        }

        if (this.power < this.usage) {
            syncIfWorkingChanged(wasWorking);
            pushOutput(level);
            return;
        }

        this.power -= this.usage;
        this.progress++;
        this.working = true;
        int effectiveTime = effectiveProcessTime(holder.value());
        this.processTime = effectiveTime;
        if (this.progress >= effectiveTime) {
            finishRecipe(holder.value());
            this.progress = 0;
            setChangedAndSync(true);
        } else {
            syncIfWorkingChanged(wasWorking);
        }

        pushOutput(level);
    }

    private void pushOutput(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : portsFor(this.worldPosition)) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(this.outputTank.type(), Math.min(PUSH_PER_PORT, this.outputTank.amount()));
            int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), stack, this.worldPosition, true);
            if (accepted > 0) {
                this.outputTank.drain(this.outputTank.type(), accepted, false);
                sync();
            }
        }
    }

    @Nullable
    private RecipeHolder<MixerRecipe> selectedRecipe() {
        List<RecipeHolder<MixerRecipe>> recipes = outputRecipes();
        if (recipes.isEmpty()) {
            this.recipeIndex = 0;
            return null;
        }
        this.recipeIndex = Math.floorMod(this.recipeIndex, recipes.size());
        return recipes.get(this.recipeIndex);
    }

    private List<RecipeHolder<MixerRecipe>> outputRecipes() {
        if (this.level == null) {
            return List.of();
        }
        HbmFluidDefinition output = this.outputTank.amount() > 0 ? this.outputTank.type() : this.configuredOutput;
        if (output.isNone()) {
            return List.of();
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.MIXER.get())
                .stream()
                .filter(holder -> holder.value().output().fluid() == output)
                .toList();
    }

    private boolean hasOutputRecipes(HbmFluidDefinition output) {
        if (this.level == null || output == null || output.isNone()) {
            return false;
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.MIXER.get())
                .stream()
                .anyMatch(holder -> holder.value().output().fluid() == output);
    }

    private boolean canProcess(MixerRecipe recipe) {
        InputLayout layout = inputLayout(recipe);
        if (!layout.isValid()) {
            return false;
        }
        if (recipe.input1().isPresent()
                && !hasFluid(inputTank(layout.input1Tank()), recipe.input1().get())) {
            return false;
        }
        if (recipe.input2().isPresent()
                && !hasFluid(inputTank(layout.input2Tank()), recipe.input2().get())) {
            return false;
        }
        if (recipe.output().fluid().isNone()) {
            return false;
        }
        if (this.outputTank.amount() > 0 && this.outputTank.type() != recipe.output().fluid()) {
            return false;
        }
        if (this.outputTank.amount() + recipe.output().amount() > this.outputTank.capacity()) {
            return false;
        }
        if (recipe.solidInput().isPresent() && !recipe.solidInput().get().matches(this.items.get(SOLID_INPUT_SLOT))) {
            return false;
        }
        this.processTime = recipe.duration();
        return true;
    }

    private boolean hasFluid(HbmFluidTank tank, MixerRecipe.FluidStackDef required) {
        return tank != null
                && tank.type() == required.fluid()
                && tank.pressure() == required.pressure()
                && tank.amount() >= required.amount();
    }

    private void finishRecipe(MixerRecipe recipe) {
        InputLayout layout = inputLayout(recipe);
        if (!layout.isValid()) {
            return;
        }
        recipe.input1().ifPresent(input -> drainRecipeInput(layout.input1Tank(), input));
        recipe.input2().ifPresent(input -> drainRecipeInput(layout.input2Tank(), input));
        recipe.solidInput().ifPresent(input -> {
            ItemStack solid = this.items.get(SOLID_INPUT_SLOT);
            solid.shrink(input.count());
            if (solid.isEmpty()) {
                this.items.set(SOLID_INPUT_SLOT, ItemStack.EMPTY);
            }
        });
        this.outputTank.fill(recipe.output().fluid(), recipe.output().amount(), recipe.output().pressure(), false);
        this.configuredOutput = recipe.output().fluid();
    }

    private void drainRecipeInput(int tankIndex, MixerRecipe.FluidStackDef input) {
        HbmFluidTank tank = inputTank(tankIndex);
        if (tank != null) {
            tank.drain(input.fluid(), input.amount(), false);
        }
    }

    @Nullable
    private HbmFluidTank inputTank(int tankIndex) {
        return switch (tankIndex) {
            case 0 -> this.inputTank1;
            case 1 -> this.inputTank2;
            default -> null;
        };
    }

    private InputLayout inputLayout(MixerRecipe recipe) {
        if (this.inputLayoutDirty || this.cachedInputLayoutRecipe != recipe) {
            this.cachedInputLayoutRecipe = recipe;
            this.cachedInputLayout = resolveInputLayout(recipe, -1, null);
            this.inputLayoutDirty = false;
        }
        return this.cachedInputLayout;
    }

    private void cacheInputLayout(MixerRecipe recipe, InputLayout layout) {
        this.cachedInputLayoutRecipe = recipe;
        this.cachedInputLayout = layout;
        this.inputLayoutDirty = false;
    }

    private void markInputLayoutDirty() {
        this.inputLayoutDirty = true;
    }

    private InputLayout resolveInputLayout(
            MixerRecipe recipe,
            int pendingTankIndex,
            @Nullable HbmFluidDefinition pendingFluid
    ) {
        MixerRecipe.FluidStackDef input1 = recipe.input1().orElse(null);
        MixerRecipe.FluidStackDef input2 = recipe.input2().orElse(null);
        if (input1 == null && input2 == null) {
            return InputLayout.NONE;
        }
        if (input1 == null) {
            return InputLayout.INVALID;
        }
        if (input2 == null) {
            boolean tank1Possible = canTankServeInput(0, input1, pendingTankIndex, pendingFluid);
            boolean tank2Possible = canTankServeInput(1, input1, pendingTankIndex, pendingFluid);
            if (!tank1Possible && !tank2Possible) {
                return InputLayout.INVALID;
            }
            if (tank1Possible && tank2Possible) {
                int tank1Preference = inputAssignmentPreference(0, input1, pendingTankIndex, pendingFluid);
                int tank2Preference = inputAssignmentPreference(1, input1, pendingTankIndex, pendingFluid);
                return tank2Preference > tank1Preference ? InputLayout.SINGLE_TANK_2 : InputLayout.SINGLE_TANK_1;
            }
            return tank1Possible ? InputLayout.SINGLE_TANK_1 : InputLayout.SINGLE_TANK_2;
        }

        boolean directPossible = canTankServeInput(0, input1, pendingTankIndex, pendingFluid)
                && canTankServeInput(1, input2, pendingTankIndex, pendingFluid);
        boolean swappedPossible = canTankServeInput(0, input2, pendingTankIndex, pendingFluid)
                && canTankServeInput(1, input1, pendingTankIndex, pendingFluid);
        if (!directPossible && !swappedPossible) {
            return InputLayout.INVALID;
        }
        if (directPossible && swappedPossible) {
            int directPreference = inputAssignmentPreference(0, input1, pendingTankIndex, pendingFluid)
                    + inputAssignmentPreference(1, input2, pendingTankIndex, pendingFluid);
            int swappedPreference = inputAssignmentPreference(0, input2, pendingTankIndex, pendingFluid)
                    + inputAssignmentPreference(1, input1, pendingTankIndex, pendingFluid);
            return swappedPreference > directPreference ? InputLayout.SWAPPED : InputLayout.DIRECT;
        }
        return directPossible ? InputLayout.DIRECT : InputLayout.SWAPPED;
    }

    private boolean canTankServeInput(
            int tankIndex,
            MixerRecipe.FluidStackDef input,
            int pendingTankIndex,
            @Nullable HbmFluidDefinition pendingFluid
    ) {
        HbmFluidTank tank = inputTank(tankIndex);
        if (tank == null) {
            return false;
        }
        if (tankIndex == pendingTankIndex) {
            if (pendingFluid != input.fluid()) {
                return false;
            }
            return tank.amount() == 0
                    || (tank.type() == input.fluid() && tank.pressure() == input.pressure());
        }
        return tank.amount() == 0
                || (tank.type() == input.fluid() && tank.pressure() == input.pressure());
    }

    private int inputAssignmentPreference(
            int tankIndex,
            MixerRecipe.FluidStackDef input,
            int pendingTankIndex,
            @Nullable HbmFluidDefinition pendingFluid
    ) {
        if (tankIndex == pendingTankIndex) {
            return pendingFluid == input.fluid() ? 4 : 0;
        }
        HbmFluidTank tank = inputTank(tankIndex);
        if (tank == null || tank.type() != input.fluid()) {
            return 0;
        }
        if (tank.amount() > 0 && tank.pressure() != input.pressure()) {
            return 0;
        }
        return tank.amount() > 0 ? 2 : 1;
    }

    private int effectiveProcessTime(MixerRecipe recipe) {
        int speed = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int overdrive = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 6);
        int time = recipe.duration();
        time -= time * speed / 4;
        time /= (overdrive + 1);
        return Math.max(1, time);
    }

    private void updateUpgrades() {
        int speed = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int power = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3);
        int overdrive = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 6);
        double consumption = BASE_USAGE + speed * 150.0D;
        consumption -= consumption * power * 0.25D;
        consumption *= overdrive * 3 + 1;
        this.usage = Math.max(1, (int) consumption);
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

    private void applyIdentifierSlot() {
        ItemStack identifier = this.items.get(FLUID_IDENTIFIER_SLOT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        HbmFluidDefinition fluid = FluidIdentifierItem.primary(identifier);
        if (fluid.isNone() || !hasOutputRecipes(fluid)) {
            return;
        }
        configureOutput(fluid);
    }

    private void configureOutput(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        if (this.outputTank.amount() > 0 && this.outputTank.type() != fluid) {
            return;
        }
        boolean outputChanged = this.configuredOutput != fluid;
        this.configuredOutput = fluid;
        if (this.outputTank.amount() == 0) {
            this.outputTank.setType(fluid);
        }
        int normalizedRecipeIndex = Math.floorMod(this.recipeIndex, Math.max(1, outputRecipes().size()));
        if (outputChanged || this.recipeIndex != normalizedRecipeIndex) {
            markInputLayoutDirty();
        }
        this.recipeIndex = normalizedRecipeIndex;
        setChangedAndSync(true);
    }

    private boolean acceptsSolidInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        RecipeHolder<MixerRecipe> selected = selectedRecipe();
        if (selected != null && selected.value().solidInput().map(input -> input.matches(stack)).orElse(false)) {
            return true;
        }
        return outputRecipes().stream().anyMatch(holder -> holder.value().solidInput().map(input -> input.matches(stack)).orElse(false));
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : portsFor(this.worldPosition)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void syncIfWorkingChanged(boolean wasWorking) {
        if (wasWorking != this.working) {
            setChangedAndSync(true);
        }
    }

    private void sync() {
        setChangedAndSync(true);
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            for (Port port : portsFor(this.worldPosition)) {
                this.level.invalidateCapabilities(port.pos());
                this.level.invalidateCapabilities(port.connectorPos());
            }
            if (sync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
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

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    stack.copy()
            ));
        }
    }

    public record Port(BlockPos pos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class MixerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private MixerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return switch (tankIndex) {
                case 0 -> inputTank1.getFluidInTank(0);
                case 1 -> inputTank2.getFluidInTank(0);
                case 2 -> outputTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return switch (tankIndex) {
                case 0, 1 -> INPUT_TANK_CAPACITY;
                case 2 -> OUTPUT_TANK_CAPACITY;
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            if (tankIndex < 0 || tankIndex > 1 || stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                return false;
            }
            RecipeHolder<MixerRecipe> selected = selectedRecipe();
            if (selected == null) {
                return false;
            }
            MixerRecipe recipe = selected.value();
            InputLayout layout = resolveInputLayout(recipe, tankIndex, fluid);
            MixerRecipe.FluidStackDef expected = inputForTank(recipe, layout, tankIndex);
            return layout.isValid() && expected != null && expected.fluid() == fluid;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                return 0;
            }
            RecipeHolder<MixerRecipe> selected = selectedRecipe();
            if (selected == null) {
                return 0;
            }
            MixerRecipe recipe = selected.value();
            MixerRecipe.FluidStackDef input1 = recipe.input1().orElse(null);
            MixerRecipe.FluidStackDef input2 = recipe.input2().orElse(null);
            int filled;
            InputLayout filledLayout;
            if (input1 != null && input2 != null
                    && input1.fluid() == fluid && input2.fluid() == fluid) {
                filledLayout = inputLayout(recipe);
                filled = fillSameFluidInputs(
                        input1,
                        input2,
                        filledLayout,
                        resource.getAmount(),
                        action.simulate()
                );
            } else {
                InputLayout tank1Layout = resolveInputLayout(recipe, 0, fluid);
                InputLayout tank2Layout = resolveInputLayout(recipe, 1, fluid);
                MixerRecipe.FluidStackDef tank1Input = inputForTank(recipe, tank1Layout, 0);
                MixerRecipe.FluidStackDef tank2Input = inputForTank(recipe, tank2Layout, 1);
                boolean tank1Valid = tank1Layout.isValid()
                        && tank1Input != null && tank1Input.fluid() == fluid;
                boolean tank2Valid = tank2Layout.isValid()
                        && tank2Input != null && tank2Input.fluid() == fluid;
                if (!tank1Valid && !tank2Valid) {
                    return 0;
                }

                boolean preferTank1 = tank1Valid && isTankTypedFor(inputTank1, tank1Input);
                boolean preferTank2 = tank2Valid && isTankTypedFor(inputTank2, tank2Input);
                int targetTankIndex = tank1Valid && (!preferTank2 || preferTank1) ? 0 : 1;
                filledLayout = targetTankIndex == 0 ? tank1Layout : tank2Layout;
                MixerRecipe.FluidStackDef expected = targetTankIndex == 0 ? tank1Input : tank2Input;
                filled = fillInputTank(inputTank(targetTankIndex), expected, resource.getAmount(), action.simulate());
            }
            if (filled > 0 && action.execute()) {
                cacheInputLayout(recipe, filledLayout);
                setChangedAndSync(true);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != outputTank.type()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = outputTank.drain(fluid, resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                setChangedAndSync(true);
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = outputTank.type();
            HbmFluidStack drained = outputTank.drain(fluid, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                setChangedAndSync(true);
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }

        @Nullable
        private MixerRecipe.FluidStackDef inputForTank(MixerRecipe recipe, InputLayout layout, int tankIndex) {
            if (layout.input1Tank() == tankIndex) {
                return recipe.input1().orElse(null);
            }
            if (layout.input2Tank() == tankIndex) {
                return recipe.input2().orElse(null);
            }
            return null;
        }

        private int fillSameFluidInputs(
                MixerRecipe.FluidStackDef input1,
                MixerRecipe.FluidStackDef input2,
                InputLayout layout,
                int amount,
                boolean simulate
        ) {
            if (!layout.isValid()) {
                return 0;
            }
            HbmFluidTank input1Tank = inputTank(layout.input1Tank());
            HbmFluidTank input2Tank = inputTank(layout.input2Tank());
            int input1Capacity = availableInputCapacity(input1Tank, input1);
            int input2Capacity = availableInputCapacity(input2Tank, input2);
            int firstRound = amount / 2;
            int input1Amount = Math.min(firstRound, input1Capacity);
            int input2Amount = Math.min(firstRound, input2Capacity);
            int remaining = amount - input1Amount - input2Amount;
            int extraInput1 = Math.min(remaining, input1Capacity - input1Amount);
            input1Amount += extraInput1;
            remaining -= extraInput1;
            input2Amount += Math.min(remaining, input2Capacity - input2Amount);

            if (!simulate) {
                fillInputTank(input1Tank, input1, input1Amount, false);
                fillInputTank(input2Tank, input2, input2Amount, false);
            }
            return input1Amount + input2Amount;
        }

        private boolean isTankTypedFor(HbmFluidTank tank, MixerRecipe.FluidStackDef expected) {
            return tank.type() == expected.fluid()
                    && (tank.amount() == 0 || tank.pressure() == expected.pressure());
        }

        private int availableInputCapacity(
                @Nullable HbmFluidTank tank,
                @Nullable MixerRecipe.FluidStackDef expected
        ) {
            if (tank == null || expected == null
                    || (tank.amount() > 0
                    && (tank.type() != expected.fluid() || tank.pressure() != expected.pressure()))) {
                return 0;
            }
            return Math.max(0, tank.capacity() - tank.amount());
        }

        private int fillInputTank(
                @Nullable HbmFluidTank tank,
                @Nullable MixerRecipe.FluidStackDef expected,
                int amount,
                boolean simulate
        ) {
            if (tank == null || expected == null || amount <= 0) {
                return 0;
            }
            if (tank.amount() == 0) {
                int accepted = Math.min(amount, tank.capacity());
                if (!simulate && accepted > 0) {
                    tank.conform(expected.fluid(), expected.pressure());
                    return tank.fill(expected.fluid(), accepted, expected.pressure(), false);
                }
                return accepted;
            }
            return tank.fill(expected.fluid(), amount, expected.pressure(), simulate);
        }
    }
}
