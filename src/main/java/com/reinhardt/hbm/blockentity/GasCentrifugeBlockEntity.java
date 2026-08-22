package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.CentrifugeBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.GasCentrifugeMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.GasCentrifugeRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import java.util.Set;

public class GasCentrifugeBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int OUTPUT_START = 0;
    public static final int OUTPUT_END = 4;
    public static final int BATTERY_SLOT = 4;
    public static final int FLUID_ID_SLOT = 5;
    public static final int UPGRADE_SLOT = 6;
    public static final int SLOT_COUNT = 7;
    public static final int DATA_COUNT = 11;
    public static final int TANK_CAPACITY = 8_000;
    public static final long MAX_POWER = 100_000L;
    public static final int BASE_PROCESSING_SPEED = 150;
    public static final int FAST_PROCESSING_SPEED = 80;
    public static final int BASE_CONSUMPTION = 200;
    public static final int FAST_CONSUMPTION = 300;
    private static final int TOWER_HEIGHT = 4;
    private static final Set<String> KNOWN_INPUT_FLUIDS = Set.of("uf6", "leuf6", "meuf6", "heuf6", "puf6", "watz", "watz_heavy");

    private static final int[] OUTPUT_SLOTS = {0, 1, 2, 3};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(TANK_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(TANK_CAPACITY);
    private long power;
    private long lastInput;
    private int progress;
    private int completedCycles;
    private int currentConsumption = BASE_CONSUMPTION;
    private int currentProcessTime = BASE_PROCESSING_SPEED;
    private int soundCycle;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) GasCentrifugeBlockEntity.this.power;
                case 1 -> (int) GasCentrifugeBlockEntity.this.lastInput;
                case 2 -> GasCentrifugeBlockEntity.this.progress;
                case 3 -> GasCentrifugeBlockEntity.this.currentProcessTime;
                case 4 -> GasCentrifugeBlockEntity.this.currentConsumption;
                case 5 -> GasCentrifugeBlockEntity.this.completedCycles;
                case 6 -> GasCentrifugeBlockEntity.this.inputTank.type().oldId();
                case 7 -> GasCentrifugeBlockEntity.this.inputTank.amount();
                case 8 -> GasCentrifugeBlockEntity.this.outputTank.type().oldId();
                case 9 -> GasCentrifugeBlockEntity.this.outputTank.amount();
                case 10 -> GasCentrifugeBlockEntity.this.isWorking() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> GasCentrifugeBlockEntity.this.power = value;
                case 1 -> GasCentrifugeBlockEntity.this.lastInput = value;
                case 2 -> GasCentrifugeBlockEntity.this.progress = value;
                case 3 -> GasCentrifugeBlockEntity.this.currentProcessTime = Math.max(1, value);
                case 4 -> GasCentrifugeBlockEntity.this.currentConsumption = Math.max(1, value);
                case 5 -> GasCentrifugeBlockEntity.this.completedCycles = value;
                case 6 -> GasCentrifugeBlockEntity.this.inputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 7 -> GasCentrifugeBlockEntity.this.inputTank.setAmount(value);
                case 8 -> GasCentrifugeBlockEntity.this.outputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 9 -> GasCentrifugeBlockEntity.this.outputTank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public GasCentrifugeBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.GAS_CENTRIFUGE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GasCentrifugeBlockEntity centrifuge) {
        PowerNetworkManager.tickFromEndpoint(level, centrifuge);
        centrifuge.tickServer(level);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public boolean applyFluidIdentifier(ItemStack stack) {
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        return setConfiguredInputFluid(FluidIdentifierItem.primary(stack));
    }

    public boolean isWorking() {
        return this.progress > 0;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return towerPowerConnectorPositions(this.worldPosition);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return isTowerPowerConnector(this.worldPosition, connectorPos, machineSide);
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
        return Math.min(Math.max(BASE_CONSUMPTION, this.currentConsumption), MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.currentProcessTime <= 0 ? 0 : this.progress * 100 / this.currentProcessTime;
        return Component.translatable(
                "message.reinhardtshbm.power.gas_centrifuge",
                this.lastInput,
                this.currentConsumption,
                this.power,
                MAX_POWER,
                percent,
                this.completedCycles
        );
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new GasCentrifugeFluidHandler();
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (queriedPos.equals(this.worldPosition)) {
            return side == null || isConnectionPort(this.worldPosition.relative(side), side);
        }
        if (side == null) {
            return isConnectionPosition(queriedPos);
        }
        return isConnectionPort(queriedPos, side);
    }

    private boolean isConnectionPosition(BlockPos pos) {
        BlockPos core = this.worldPosition;
        return pos.equals(core.below())
                || pos.equals(core.east())
                || pos.equals(core.west())
                || pos.equals(core.south())
                || pos.equals(core.north());
    }

    private boolean isConnectionPort(BlockPos pos, Direction side) {
        BlockPos core = this.worldPosition;
        return (pos.equals(core.below()) && side == Direction.DOWN)
                || (pos.equals(core.east()) && side == Direction.EAST)
                || (pos.equals(core.west()) && side == Direction.WEST)
                || (pos.equals(core.south()) && side == Direction.SOUTH)
                || (pos.equals(core.north()) && side == Direction.NORTH);
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
        setChanged();
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
        if (slot == FLUID_ID_SLOT) {
            updateTankTypeFromIdentifier();
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
            case FLUID_ID_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
            case UPGRADE_SLOT -> isGasSpeedUpgrade(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_END;
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
        this.inputTank.clear();
        this.outputTank.clear();
        this.progress = 0;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.gas_centrifuge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasCentrifugeMenu(containerId, playerInventory, this, this.menuData);
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
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("CompletedCycles", this.completedCycles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.progress = tag.getInt("Progress");
        this.completedCycles = tag.getInt("CompletedCycles");
    }

    private void tickServer(Level level) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        updateTankTypeFromIdentifier();

        Optional<GasCentrifugeProcess> process = processFor(level);
        updateWorkStats();
        if (process.isEmpty() || !canProcess(process.get())) {
            this.progress = 0;
            setLit(false);
        } else if (this.power > 0) {
            this.power = Math.max(0L, this.power - this.currentConsumption);
            this.progress++;
            playWorkingSound(level);
            setLit(true);
            if (this.progress >= this.currentProcessTime) {
                finishProcess(process.get());
                this.progress = 0;
                this.completedCycles++;
            }
            setChanged();
        } else {
            setLit(false);
        }
    }

    private void updateWorkStats() {
        boolean fast = isGasSpeedUpgrade(this.items.get(UPGRADE_SLOT));
        this.currentProcessTime = fast ? FAST_PROCESSING_SPEED : BASE_PROCESSING_SPEED;
        this.currentConsumption = fast ? FAST_CONSUMPTION : BASE_CONSUMPTION;
    }

    private Optional<GasCentrifugeProcess> processFor(Level level) {
        HbmFluidDefinition fluid = this.inputTank.type();
        if (fluid.isNone() || this.inputTank.amount() <= 0) {
            return Optional.empty();
        }
        GasCentrifugeRecipe.Input input = new GasCentrifugeRecipe.Input(fluid, this.inputTank.amount());
        return level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.GAS_CENTRIFUGE.get())
                .stream()
                .filter(holder -> holder.value().matches(input, level))
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .findFirst()
                .map(RecipeHolder::value)
                .map(recipe -> new GasCentrifugeProcess(recipe.input(), recipe.output(), recipe.highSpeed(), recipe.results()));
    }

    private boolean canProcess(GasCentrifugeProcess process) {
        if (process.highSpeed() && !isGasSpeedUpgrade(this.items.get(UPGRADE_SLOT))) {
            return false;
        }
        if (this.inputTank.type() != process.input().fluid() || this.inputTank.amount() < process.input().amount()) {
            return false;
        }
        if (!canFitFluidOutput(process.output())) {
            return false;
        }
        return canFitOutputs(process.results());
    }

    private boolean canFitFluidOutput(GasCentrifugeRecipe.FluidOutput output) {
        if (output == null || output.isEmpty()) {
            return true;
        }
        if (this.outputTank.amount() > 0 && this.outputTank.type() != output.fluid()) {
            return false;
        }
        return this.outputTank.amount() + output.amount() <= this.outputTank.capacity();
    }

    private boolean canFitOutputs(List<ItemStack> results) {
        for (ItemStack result : results) {
            if (!canFitOutput(result)) {
                return false;
            }
        }
        return true;
    }

    private boolean canFitOutput(ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }
        int remaining = result.getCount();
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack output = this.items.get(slot);
            if (output.isEmpty()) {
                remaining -= result.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(output, result)) {
                remaining -= output.getMaxStackSize() - output.getCount();
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private void finishProcess(GasCentrifugeProcess process) {
        this.inputTank.drain(process.input().fluid(), process.input().amount(), false);
        if (process.output() != null && !process.output().isEmpty()) {
            this.outputTank.fill(process.output().fluid(), process.output().amount(), false);
        }
        for (ItemStack result : process.results()) {
            insertOutput(result.copy());
        }
        syncFluidChange();
    }

    private void insertOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !stack.isEmpty(); slot++) {
            ItemStack output = this.items.get(slot);
            if (ItemStack.isSameItemSameComponents(output, stack)) {
                int moved = Math.min(stack.getCount(), output.getMaxStackSize() - output.getCount());
                if (moved > 0) {
                    output.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !stack.isEmpty(); slot++) {
            if (this.items.get(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                this.items.set(slot, stack.copyWithCount(moved));
                stack.shrink(moved);
            }
        }
    }

    private void updateTankTypeFromIdentifier() {
        ItemStack identifier = this.items.get(FLUID_ID_SLOT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        setConfiguredInputFluid(FluidIdentifierItem.primary(identifier));
    }

    private boolean setConfiguredInputFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            if (this.inputTank.amount() > 0 || this.inputTank.type().isNone()) {
                return false;
            }
            this.inputTank.clear();
            this.progress = 0;
            syncFluidChange();
            return true;
        }
        if (!isValidInputFluid(fluid)) {
            return false;
        }
        if (this.inputTank.amount() > 0 && this.inputTank.type() != fluid) {
            return false;
        }
        if (this.inputTank.type() == fluid) {
            return false;
        }
        this.inputTank.setType(fluid);
        this.progress = 0;
        syncFluidChange();
        return true;
    }

    private boolean canAcceptInputFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || !isValidInputFluid(fluid)) {
            return false;
        }
        if (this.inputTank.amount() > 0) {
            return this.inputTank.type() == fluid;
        }
        return this.inputTank.type().isNone() || this.inputTank.type() == fluid;
    }

    private boolean isValidInputFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return false;
        }
        if (this.level == null) {
            return KNOWN_INPUT_FLUIDS.contains(fluid.name());
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.GAS_CENTRIFUGE.get())
                .stream()
                .anyMatch(holder -> holder.value().input().fluid() == fluid);
    }

    private void playWorkingSound(Level level) {
        if (this.soundCycle <= 0) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.CENTRIFUGE_OPERATE.get(), SoundSource.BLOCKS, 0.65F, 0.65F);
            this.soundCycle = 36;
        }
        this.soundCycle--;
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof CentrifugeBlock && state.getValue(CentrifugeBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(CentrifugeBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void syncFluidChange() {
        setChanged();
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private static boolean isGasSpeedUpgrade(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ReinhardtsHBM.id("upgrade_gc_speed"));
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static List<BlockPos> towerPowerConnectorPositions(BlockPos core) {
        ArrayList<BlockPos> connectors = new ArrayList<>(2 + TOWER_HEIGHT * 4);
        connectors.add(core.below().immutable());
        connectors.add(core.above(TOWER_HEIGHT).immutable());
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            BlockPos column = core.above(y);
            connectors.add(column.east().immutable());
            connectors.add(column.west().immutable());
            connectors.add(column.south().immutable());
            connectors.add(column.north().immutable());
        }
        return List.copyOf(connectors);
    }

    private static boolean isTowerPowerConnector(BlockPos core, BlockPos connectorPos, Direction machineSide) {
        if (connectorPos.equals(core.below())) {
            return machineSide == Direction.DOWN;
        }
        if (connectorPos.equals(core.above(TOWER_HEIGHT))) {
            return machineSide == Direction.UP;
        }
        if (!machineSide.getAxis().isHorizontal()) {
            return false;
        }
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            if (connectorPos.equals(core.above(y).relative(machineSide))) {
                return true;
            }
        }
        return false;
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

    private record GasCentrifugeProcess(
            GasCentrifugeRecipe.FluidIngredient input,
            GasCentrifugeRecipe.FluidOutput output,
            boolean highSpeed,
            List<ItemStack> results
    ) {
    }

    private final class GasCentrifugeFluidHandler implements IFluidHandler {
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
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return canAcceptInputFluid(fluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!canAcceptInputFluid(fluid)) {
                return 0;
            }
            int filled = inputTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                syncFluidChange();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || outputTank.type().isNone()) {
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
                syncFluidChange();
            }
            return HbmFluids.toNeoStack(fluid, drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || outputTank.type().isNone()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = outputTank.type();
            HbmFluidStack drained = outputTank.drain(fluid, maxDrain, action.simulate());
            if (drained.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                syncFluidChange();
            }
            return HbmFluids.toNeoStack(fluid, drained.amount());
        }
    }
}
