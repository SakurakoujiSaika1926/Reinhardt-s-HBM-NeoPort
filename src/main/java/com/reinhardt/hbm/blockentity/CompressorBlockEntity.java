package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CompressorBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.CompressorMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.CompressorRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.FluidCopiable;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import java.util.Optional;

public class CompressorBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public enum Kind {
        NORMAL,
        COMPACT
    }

    public static final int FLUID_IDENTIFIER_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 4;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_COUNT = 12;
    public static final int TANK_CAPACITY = 16_000;
    public static final long MAX_POWER = 100_000L;
    public static final int BASE_USAGE = 2_500;
    public static final int BASE_PROCESS_TIME = 100;
    private static final int PUSH_PER_PORT = 8_000;
    private static final int[] AUTOMATION_SLOTS = {FLUID_IDENTIFIER_SLOT};

    private final Kind kind;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(TANK_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(TANK_CAPACITY);
    private HbmFluidDefinition configuredFluid = HbmFluids.none();
    private long power;
    private long lastInput;
    private int progress;
    private int usage = BASE_USAGE;
    private int processTime = BASE_PROCESS_TIME;
    private boolean working;
    private float fanSpin;
    private float prevFanSpin;
    private float piston;
    private float prevPiston;
    private boolean pistonDown;
    private float pistonDropSpeed = 0.1F;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) CompressorBlockEntity.this.power;
                case 1 -> (int) CompressorBlockEntity.this.lastInput;
                case 2 -> CompressorBlockEntity.this.progress;
                case 3 -> CompressorBlockEntity.this.processTime;
                case 4 -> CompressorBlockEntity.this.usage;
                case 5 -> CompressorBlockEntity.this.inputTank.type().oldId();
                case 6 -> CompressorBlockEntity.this.inputTank.amount();
                case 7 -> CompressorBlockEntity.this.inputTank.pressure();
                case 8 -> CompressorBlockEntity.this.outputTank.type().oldId();
                case 9 -> CompressorBlockEntity.this.outputTank.amount();
                case 10 -> CompressorBlockEntity.this.outputTank.pressure();
                case 11 -> CompressorBlockEntity.this.kind.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CompressorBlockEntity.this.power = value;
                case 1 -> CompressorBlockEntity.this.lastInput = value;
                case 2 -> CompressorBlockEntity.this.progress = value;
                case 3 -> CompressorBlockEntity.this.processTime = Math.max(1, value);
                case 4 -> CompressorBlockEntity.this.usage = Math.max(1, value);
                case 5 -> CompressorBlockEntity.this.inputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 6 -> CompressorBlockEntity.this.inputTank.setAmount(value);
                case 7 -> CompressorBlockEntity.this.inputTank.setPressure(value);
                case 8 -> CompressorBlockEntity.this.outputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 9 -> CompressorBlockEntity.this.outputTank.setAmount(value);
                case 10 -> CompressorBlockEntity.this.outputTank.setPressure(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CompressorBlockEntity(BlockPos pos, BlockState blockState, Kind kind) {
        super(HbmBlockEntities.COMPRESSOR.get(), pos, blockState);
        this.kind = kind;
        this.outputTank.setPressure(1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CompressorBlockEntity compressor) {
        if (level.isClientSide) {
            compressor.tickClient(level);
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, compressor);
        compressor.tickServer(level);
    }

    public Kind kind() {
        return this.kind;
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public boolean isWorking() {
        return this.working;
    }

    public int progress() {
        return this.progress;
    }

    public int processTime() {
        return this.processTime;
    }

    public float fanSpin(float partialTick) {
        return this.prevFanSpin + (this.fanSpin - this.prevFanSpin) * partialTick;
    }

    public float piston(float partialTick) {
        return this.prevPiston + (this.piston - this.prevPiston) * partialTick;
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return portsFor(this.worldPosition, getBlockState(), this.kind).stream().map(Port::connectorPos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        if (machineSide == null) {
            return true;
        }
        for (Port port : portsFor(this.worldPosition, getBlockState(), this.kind)) {
            if (port.connectorPos().equals(connectorPos) && port.face() == machineSide) {
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
                "message.reinhardtshbm.power.compressor",
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
        return new CompressorFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public static List<Port> portsFor(BlockPos pos, BlockState state, Kind kind) {
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
        List<LocalPort> local = kind == Kind.COMPACT ? compactLocalPorts() : normalLocalPorts();
        return local.stream()
                .map(port -> port.rotate(pos, facing))
                .toList();
    }

    @Override
    public int[] getFluidIdsToCopy() {
        HbmFluidDefinition copied = this.inputTank.amount() > 0 ? this.inputTank.type() : this.configuredFluid;
        return copied.isNone() ? new int[]{HbmFluids.none().oldId()} : new int[]{copied.oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        configureFluid(fluid);
    }

    public void setCompression(int pressure) {
        int clamped = Mth.clamp(pressure, 0, 4);
        if (clamped == this.inputTank.pressure()) {
            return;
        }
        this.inputTank.setPressure(clamped);
        setupOutputTank();
        this.progress = 0;
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
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            updateUpgrades();
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FLUID_IDENTIFIER_SLOT) {
            return stack.getItem() instanceof FluidIdentifierItem;
        }
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
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
        return slot == FLUID_IDENTIFIER_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
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
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.kind == Kind.COMPACT
                ? "container.reinhardtshbm.machine_compressor_compact"
                : "container.reinhardtshbm.machine_compressor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompressorMenu(containerId, playerInventory, this, this.menuData, this.worldPosition);
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
        tag.putString("Kind", this.kind.name());
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putString("ConfiguredFluid", this.configuredFluid.name());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("Usage", this.usage);
        tag.putInt("ProcessTime", this.processTime);
        tag.putBoolean("Working", this.working);
        tag.putFloat("FanSpin", this.fanSpin);
        tag.putFloat("Piston", this.piston);
        tag.putBoolean("PistonDown", this.pistonDown);
        tag.putFloat("PistonDropSpeed", this.pistonDropSpeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.configuredFluid = HbmFluids.byName(tag.getString("ConfiguredFluid")).orElse(HbmFluids.none());
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = tag.getLong("LastInput");
        this.progress = tag.getInt("Progress");
        this.usage = Math.max(1, tag.getInt("Usage"));
        this.processTime = Math.max(1, tag.getInt("ProcessTime"));
        this.working = tag.getBoolean("Working");
        this.fanSpin = tag.getFloat("FanSpin");
        this.prevFanSpin = this.fanSpin;
        this.piston = tag.getFloat("Piston");
        this.prevPiston = this.piston;
        this.pistonDown = tag.getBoolean("PistonDown");
        this.pistonDropSpeed = tag.contains("PistonDropSpeed") ? tag.getFloat("PistonDropSpeed") : 0.1F;
        if (this.inputTank.amount() == 0 && !this.configuredFluid.isNone()) {
            this.inputTank.setType(this.configuredFluid);
        }
        updateUpgrades();
        setupOutputTank();
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

    private void tickClient(Level level) {
        this.prevFanSpin = this.fanSpin;
        this.prevPiston = this.piston;
        if (!this.working) {
            return;
        }
        this.fanSpin += this.kind == Kind.COMPACT ? 45.0F : 15.0F;
        if (this.fanSpin >= 360.0F) {
            this.prevFanSpin -= 360.0F;
            this.fanSpin -= 360.0F;
        }
        if (this.kind == Kind.COMPACT) {
            return;
        }
        if (this.pistonDown) {
            this.piston -= this.pistonDropSpeed;
            if (this.piston <= 0.0F) {
                level.playLocalSound(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D,
                        SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 0.5F, 0.75F, false);
                this.pistonDown = false;
            }
        } else {
            this.piston += 0.05F;
            if (this.piston >= 1.0F) {
                this.pistonDropSpeed = 0.085F + level.random.nextFloat() * 0.03F;
                this.pistonDown = true;
            }
        }
        this.piston = Mth.clamp(this.piston, 0.0F, 1.0F);
    }

    private void tickServer(Level level) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        applyIdentifierSlot();
        updateUpgrades();
        setupOutputTank();
        process(level);
        pushOutput(level);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void process(Level level) {
        boolean wasWorking = this.working;
        this.working = false;
        Optional<RecipeHolder<CompressorRecipe>> holder = currentRecipe();
        int inputAmount = holder.map(recipe -> recipe.value().input().amount()).orElse(1000);
        int outputAmount = holder.map(recipe -> recipe.value().output().amount()).orElse(1000);

        if (!canProcess(holder.map(RecipeHolder::value).orElse(null), inputAmount, outputAmount)) {
            this.progress = 0;
            syncIfWorkingChanged(wasWorking);
            return;
        }

        if (this.power <= this.usage) {
            syncIfWorkingChanged(wasWorking);
            return;
        }

        this.power -= this.usage;
        this.progress++;
        this.working = true;

        if (this.progress >= this.processTime) {
            finishRecipe(holder.map(RecipeHolder::value).orElse(null), inputAmount, outputAmount);
            this.progress = 0;
            setChangedAndSync(true);
        } else {
            syncIfWorkingChanged(wasWorking);
        }
    }

    private Optional<RecipeHolder<CompressorRecipe>> currentRecipe() {
        if (this.level == null || this.inputTank.type().isNone()) {
            return Optional.empty();
        }
        CompressorRecipe.Input input = new CompressorRecipe.Input(this.inputTank.type(), this.inputTank.amount(), this.inputTank.pressure());
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.COMPRESSOR.get())
                .stream()
                // Legacy lookup is keyed by fluid type and pressure. Amount is checked by canProcess,
                // otherwise an underfilled recipe incorrectly falls through to generic compression.
                .filter(holder -> holder.value().input().fluid() == input.fluid()
                        && holder.value().input().pressure() == input.pressure())
                .findFirst();
    }

    private boolean canProcess(@Nullable CompressorRecipe recipe, int inputAmount, int outputAmount) {
        if (this.inputTank.type().isNone() || this.inputTank.amount() < inputAmount) {
            return false;
        }
        setupOutputTank(recipe);
        if (this.outputTank.type().isNone()) {
            return false;
        }
        if (this.outputTank.amount() > 0 && this.outputTank.type() != outputFluid(recipe)) {
            return false;
        }
        return this.outputTank.amount() + outputAmount <= this.outputTank.capacity();
    }

    private void finishRecipe(@Nullable CompressorRecipe recipe, int inputAmount, int outputAmount) {
        this.inputTank.drain(this.inputTank.type(), inputAmount, false);
        HbmFluidDefinition outputFluid = outputFluid(recipe);
        int outputPressure = outputPressure(recipe);
        this.outputTank.fill(outputFluid, outputAmount, outputPressure, false);
        if (this.inputTank.amount() == 0 && !this.configuredFluid.isNone()) {
            this.inputTank.setType(this.configuredFluid);
        }
        setupOutputTank();
    }

    private void pushOutput(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : portsFor(this.worldPosition, getBlockState(), this.kind)) {
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

    private void setupOutputTank() {
        setupOutputTank(currentRecipe().map(RecipeHolder::value).orElse(null));
    }

    private void setupOutputTank(@Nullable CompressorRecipe recipe) {
        HbmFluidDefinition outputFluid = outputFluid(recipe);
        int outputPressure = outputPressure(recipe);
        if (outputFluid.isNone()) {
            return;
        }
        if (this.outputTank.amount() == 0) {
            this.outputTank.conform(outputFluid, outputPressure);
        }
    }

    private HbmFluidDefinition outputFluid(@Nullable CompressorRecipe recipe) {
        if (recipe != null) {
            return recipe.output().fluid();
        }
        return this.inputTank.type();
    }

    private int outputPressure(@Nullable CompressorRecipe recipe) {
        if (recipe != null) {
            return recipe.output().pressure();
        }
        return this.inputTank.pressure() + 1;
    }

    private void applyIdentifierSlot() {
        ItemStack identifier = this.items.get(FLUID_IDENTIFIER_SLOT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        HbmFluidDefinition fluid = FluidIdentifierItem.primary(identifier);
        if (fluid.isNone()) {
            return;
        }
        configureFluid(fluid);
    }

    private void configureFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        this.configuredFluid = fluid;
        if (this.inputTank.amount() == 0 || this.inputTank.type() == fluid) {
            this.inputTank.setType(fluid);
            setChangedAndSync(true);
        }
    }

    private void updateUpgrades() {
        int speed = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int power = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3);
        int overdrive = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 9);
        Optional<RecipeHolder<CompressorRecipe>> holder = currentRecipe();
        int baseTime = holder.map(recipe -> recipe.value().duration()).orElse(BASE_PROCESS_TIME);
        this.processTime = holder.isEmpty()
                ? (speed == 3 ? 10 : speed == 2 ? 20 : speed == 1 ? 60 : baseTime)
                : baseTime / (speed + 1);
        this.processTime = Math.max(1, this.processTime / (overdrive + 1));
        this.usage = Math.max(1, BASE_USAGE / (power + 1) * ((overdrive * 2) + 1));
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

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : portsFor(this.worldPosition, getBlockState(), this.kind)) {
            if (port.proxyPos().equals(queriedPos) && (side == null || side == port.face())) {
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
            for (Port port : portsFor(this.worldPosition, getBlockState(), this.kind)) {
                this.level.invalidateCapabilities(port.proxyPos());
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

    private static List<LocalPort> normalLocalPorts() {
        return List.of(
                new LocalPort(new BlockPos(2, 0, 0), Direction.EAST),
                new LocalPort(new BlockPos(-2, 0, 0), Direction.WEST),
                new LocalPort(new BlockPos(0, 0, -2), Direction.NORTH)
        );
    }

    private static List<LocalPort> compactLocalPorts() {
        return List.of(
                new LocalPort(new BlockPos(4, 1, 0), Direction.EAST),
                new LocalPort(new BlockPos(-4, 1, 0), Direction.WEST),
                new LocalPort(new BlockPos(-1, 1, 2), Direction.SOUTH),
                new LocalPort(new BlockPos(1, 1, 2), Direction.SOUTH),
                new LocalPort(new BlockPos(-1, 1, -2), Direction.NORTH),
                new LocalPort(new BlockPos(1, 1, -2), Direction.NORTH)
        );
    }

    public record Port(BlockPos proxyPos, BlockPos connectorPos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(
                    connectorPos.relative(face.getOpposite()).immutable(),
                    connectorPos.immutable(),
                    face
            );
        }
    }

    private record LocalPort(BlockPos offset, Direction face) {
        private Port rotate(BlockPos corePos, Direction facing) {
            BlockPos rotatedOffset = LegacyMachineGeometry.rotate(this.offset, facing, LargeMachineBlock.RotationBasis.HBM_LEGACY_SOUTH);
            Direction rotatedFace = LegacyMachineGeometry.rotateDirection(this.face, facing, LargeMachineBlock.RotationBasis.HBM_LEGACY_SOUTH);
            return Port.fromConnector(corePos.offset(rotatedOffset), rotatedFace);
        }
    }

    private final class CompressorFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private CompressorFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            if (tankIndex == 0) {
                return inputTank.getFluidInTank(0);
            }
            if (tankIndex == 1) {
                return outputTank.getFluidInTank(0);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tankIndex == 0 || tankIndex == 1 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return tankIndex == 0 && !stack.isEmpty() && allowsFluidPort(this.queriedPos, this.side);
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
            if (inputTank.amount() > 0 && inputTank.type() != fluid) {
                return 0;
            }
            int filled = inputTank.fill(fluid, resource.getAmount(), inputTank.pressure(), action.simulate());
            if (filled > 0 && action.execute()) {
                configuredFluid = fluid;
                setupOutputTank();
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
    }
}
