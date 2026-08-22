package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.SmallBoilerBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.SmallBoilerMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.FluidCopiable;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SmallBoilerBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int ID_SLOT = 0;
    public static final int ID_RESULT_SLOT = 1;
    public static final int INPUT_CONTAINER_SLOT = 2;
    public static final int INPUT_CONTAINER_RESULT_SLOT = 3;
    public static final int FUEL_OR_BATTERY_SLOT = 4;
    public static final int OUTPUT_CONTAINER_SLOT = 5;
    public static final int OUTPUT_CONTAINER_RESULT_SLOT = 6;
    public static final int SLOT_COUNT = 7;
    public static final int DATA_COUNT = 12;
    public static final int INPUT_CAPACITY = 16_000;
    public static final int OUTPUT_CAPACITY = 16_000;
    public static final long ENERGY_CAPACITY = 100_000L;
    public static final long INPUT_RATE = 200L;
    public static final long DEMAND_PER_TICK = 50L;
    private static final int TRANSFER_PER_TICK = 16_000;
    private static final int MAX_INPUT_PER_TICK = 5;
    private static final int[] AUTOMATION_SLOTS = {
            ID_SLOT,
            ID_RESULT_SLOT,
            INPUT_CONTAINER_SLOT,
            INPUT_CONTAINER_RESULT_SLOT,
            FUEL_OR_BATTERY_SLOT,
            OUTPUT_CONTAINER_SLOT,
            OUTPUT_CONTAINER_RESULT_SLOT
    };

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final HbmFluidTank inputTank = new HbmFluidTank(defaultInput(), INPUT_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(defaultOutput(), OUTPUT_CAPACITY);
    private HbmFluidDefinition configuredInput = defaultInput();
    private long energyStored;
    private long lastInput;
    private int burnTime;
    private int burnTimeTotal;
    private int lastConverted;
    private boolean active;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SmallBoilerBlockEntity.this.inputTank.type().oldId();
                case 1 -> SmallBoilerBlockEntity.this.inputTank.amount();
                case 2 -> SmallBoilerBlockEntity.this.inputTank.capacity();
                case 3 -> SmallBoilerBlockEntity.this.outputTank.type().oldId();
                case 4 -> SmallBoilerBlockEntity.this.outputTank.amount();
                case 5 -> SmallBoilerBlockEntity.this.outputTank.capacity();
                case 6 -> (int) SmallBoilerBlockEntity.this.energyStored;
                case 7 -> (int) SmallBoilerBlockEntity.this.lastInput;
                case 8 -> SmallBoilerBlockEntity.this.burnTime;
                case 9 -> SmallBoilerBlockEntity.this.burnTimeTotal;
                case 10 -> SmallBoilerBlockEntity.this.lastConverted;
                case 11 -> SmallBoilerBlockEntity.this.isElectric() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> SmallBoilerBlockEntity.this.configuredInput = HbmFluids.byOldId(value).orElse(HbmFluids.none());
                case 6 -> SmallBoilerBlockEntity.this.energyStored = value;
                case 7 -> SmallBoilerBlockEntity.this.lastInput = value;
                case 8 -> SmallBoilerBlockEntity.this.burnTime = value;
                case 9 -> SmallBoilerBlockEntity.this.burnTimeTotal = value;
                case 10 -> SmallBoilerBlockEntity.this.lastConverted = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SmallBoilerBlockEntity(BlockPos pos, BlockState blockState) {
        this(HbmBlockEntities.SMALL_BOILER.get(), pos, blockState);
    }

    protected SmallBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SmallBoilerBlockEntity boiler) {
        boiler.setupTanks();
        boiler.tickContainers();
        if (boiler.isElectric()) {
            if (boiler instanceof PowerEndpoint endpoint) {
                PowerNetworkManager.tickFromEndpoint(level, endpoint);
            }
            boiler.energyStored = BatteryPackItem.dischargeIntoMachine(boiler.items[FUEL_OR_BATTERY_SLOT], boiler.energyStored, ENERGY_CAPACITY);
        } else {
            boiler.energyStored = 0L;
            boiler.lastInput = 0L;
            boiler.tickFuel();
        }
        boiler.tryConvert();
        boiler.sendOutputFluid(level);
        boiler.setLit(boiler.active || (!boiler.isElectric() && boiler.burnTime > 0));
        boiler.setChanged();
        if (level.getGameTime() % 10L == 0L) {
            boiler.sync();
        }
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public HbmFluidDefinition configuredInput() {
        return this.configuredInput;
    }

    public boolean active() {
        return this.active;
    }

    public long power() {
        return this.energyStored;
    }

    public long lastInput() {
        return this.lastInput;
    }

    public boolean isElectric() {
        return false;
    }

    public void setConfiguredInput(HbmFluidDefinition fluid) {
        HbmFluidDefinition next = validInput(fluid) ? fluid : HbmFluids.none();
        if (this.configuredInput == next) {
            return;
        }
        this.configuredInput = next;
        this.inputTank.clear();
        this.outputTank.clear();
        setupTanks();
        sync();
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.configuredInput.oldId(), this.outputTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        setConfiguredInput(fluid);
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new BoilerFluidHandler();
    }

    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        if (!isElectric()) {
            return List.of();
        }
        return List.of(
                this.worldPosition.relative(Direction.UP).immutable(),
                this.worldPosition.relative(Direction.DOWN).immutable(),
                this.worldPosition.relative(Direction.NORTH).immutable(),
                this.worldPosition.relative(Direction.SOUTH).immutable(),
                this.worldPosition.relative(Direction.WEST).immutable(),
                this.worldPosition.relative(Direction.EAST).immutable()
        );
    }

    public long getAvailableOutput() {
        return 0L;
    }

    public long getRequestedInput() {
        if (!isElectric() || this.energyStored >= ENERGY_CAPACITY) {
            return 0L;
        }
        return Math.min(Math.max(INPUT_RATE, DEMAND_PER_TICK), ENERGY_CAPACITY - this.energyStored);
    }

    public void applyPower(long usedOutput, long receivedInput) {
        if (!isElectric()) {
            return;
        }
        this.energyStored = Math.min(ENERGY_CAPACITY, this.energyStored + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.small_boiler",
                this.lastInput,
                INPUT_RATE,
                this.energyStored,
                ENERGY_CAPACITY,
                this.inputTank.amount(),
                this.outputTank.amount(),
                this.lastConverted
        );
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
        return isValidSlot(slot) ? this.items[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        if (isValidSlot(slot)) {
            this.items[slot] = ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items[slot] = stack;
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case ID_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
            case INPUT_CONTAINER_SLOT -> isDrainableInputContainer(stack);
            case FUEL_OR_BATTERY_SLOT -> isElectric() ? ShredderBlockEntity.isBattery(stack) : WoodBurnerBlockEntity.fuelDuration(stack) > 0;
            case OUTPUT_CONTAINER_SLOT -> isEmptyFluidContainer(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return (slot == ID_SLOT || slot == INPUT_CONTAINER_SLOT || slot == FUEL_OR_BATTERY_SLOT || slot == OUTPUT_CONTAINER_SLOT)
                && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == ID_RESULT_SLOT || slot == INPUT_CONTAINER_RESULT_SLOT || slot == OUTPUT_CONTAINER_RESULT_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(isElectric()
                ? "container.reinhardtshbm.machine_boiler_electric"
                : "container.reinhardtshbm.machine_boiler");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SmallBoilerMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public void copyMachineStateTo(SmallBoilerBlockEntity target, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        target.loadAdditional(tag, registries);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.items.length; i++) {
            drop(level, pos, this.items[i]);
            this.items[i] = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < this.items.length; i++) {
            tag.put("Slot" + i, this.items[i].saveOptional(registries));
        }
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putString("ConfiguredInput", this.configuredInput.name());
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("BurnTimeTotal", this.burnTimeTotal);
        tag.putInt("LastConverted", this.lastConverted);
        tag.putBoolean("Active", this.active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.parseOptional(registries, tag.getCompound("Slot" + i));
        }
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput")).orElse(defaultInput());
        if (!validInput(this.configuredInput)) {
            this.configuredInput = defaultInput();
        }
        this.energyStored = tag.getLong("EnergyStored");
        this.lastInput = tag.getLong("LastInput");
        this.burnTime = tag.getInt("BurnTime");
        this.burnTimeTotal = tag.getInt("BurnTimeTotal");
        this.lastConverted = tag.getInt("LastConverted");
        this.active = tag.getBoolean("Active");
        setupTanks();
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

    private void setupTanks() {
        HbmThermalConversions.firstBoilerStep(this.configuredInput).filter(step -> validInput(step.input())).ifPresentOrElse(step -> {
            if (this.inputTank.amount() == 0 && this.inputTank.type() != step.input()) {
                this.inputTank.setType(step.input());
            }
            if (this.outputTank.amount() == 0 && this.outputTank.type() != step.output()) {
                this.outputTank.setType(step.output());
            }
        }, () -> {
            if (!validInput(this.configuredInput)) {
                this.configuredInput = defaultInput();
            }
            if (this.inputTank.amount() == 0) {
                this.inputTank.setType(this.configuredInput);
            }
        });
    }

    private void tickContainers() {
        boolean changed = false;
        changed |= applyIdentifier();
        changed |= drainContainerIntoInput();
        changed |= fillContainerFromOutput();
        if (changed) {
            sync();
        }
    }

    private boolean applyIdentifier() {
        ItemStack input = this.items[ID_SLOT];
        if (!(input.getItem() instanceof FluidIdentifierItem) || !this.items[ID_RESULT_SLOT].isEmpty()) {
            return false;
        }
        HbmFluidDefinition next = FluidIdentifierItem.primary(input);
        if (!validInput(next) || next == this.configuredInput) {
            return false;
        }
        this.configuredInput = next;
        this.inputTank.clear();
        this.outputTank.clear();
        setupTanks();
        ItemStack result = input.copy();
        result.setCount(1);
        this.items[ID_RESULT_SLOT] = result;
        input.shrink(1);
        if (input.isEmpty()) {
            this.items[ID_SLOT] = ItemStack.EMPTY;
        }
        return true;
    }

    private boolean drainContainerIntoInput() {
        ItemStack input = this.items[INPUT_CONTAINER_SLOT];
        if (input.isEmpty()) {
            return false;
        }

        boolean moved = HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.inputTank,
                fluid -> fluid == this.configuredInput && validInput(fluid),
                output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output),
                output -> placeOutput(INPUT_CONTAINER_RESULT_SLOT, output)
        );
        if (!moved) {
            return false;
        }
        if (input.isEmpty()) {
            this.items[INPUT_CONTAINER_SLOT] = ItemStack.EMPTY;
        }
        return true;
    }

    private boolean fillContainerFromOutput() {
        ItemStack input = this.items[OUTPUT_CONTAINER_SLOT];
        if (input.isEmpty()) {
            return false;
        }

        boolean moved = HbmFluidContainerTransfer.fillFromTank(
                input,
                this.outputTank,
                output -> canPlaceOutput(OUTPUT_CONTAINER_RESULT_SLOT, output),
                output -> placeOutput(OUTPUT_CONTAINER_RESULT_SLOT, output)
        );
        if (!moved) {
            return false;
        }
        if (input.isEmpty()) {
            this.items[OUTPUT_CONTAINER_SLOT] = ItemStack.EMPTY;
        }
        return true;
    }

    private void tickFuel() {
        if (this.burnTime <= 0 && canConvert()) {
            startBurningFuel();
        }
        if (this.burnTime > 0) {
            this.burnTime--;
            setChanged();
        }
    }

    private void startBurningFuel() {
        ItemStack fuel = this.items[FUEL_OR_BATTERY_SLOT];
        int duration = WoodBurnerBlockEntity.fuelDuration(fuel);
        if (duration <= 0) {
            return;
        }
        Item consumedItem = fuel.getItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            this.items[FUEL_OR_BATTERY_SLOT] = consumedItem == Items.LAVA_BUCKET ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY;
        }
        this.burnTime = duration;
        this.burnTimeTotal = duration;
        setChanged();
    }

    private boolean canConvert() {
        return HbmThermalConversions.firstBoilerStep(this.configuredInput)
                .filter(step -> validInput(step.input()))
                .filter(step -> this.inputTank.amount() >= step.amountReq())
                .filter(step -> this.outputTank.capacity() - this.outputTank.amount() >= step.amountProduced())
                .isPresent();
    }

    private void tryConvert() {
        this.active = false;
        this.lastConverted = 0;
        HbmThermalConversions.firstBoilerStep(this.configuredInput).filter(step -> validInput(step.input())).ifPresent(step -> {
            int inputOps = this.inputTank.amount() / step.amountReq();
            int outputOps = (this.outputTank.capacity() - this.outputTank.amount()) / step.amountProduced();
            int tickOps = Math.max(1, MAX_INPUT_PER_TICK / step.amountReq());
            int ops = Math.min(inputOps, Math.min(outputOps, tickOps));
            if (ops <= 0) {
                return;
            }
            if (isElectric()) {
                int cost = Math.max(1, step.heatReq());
                ops = Math.min(ops, (int) (this.energyStored / cost));
                if (ops <= 0) {
                    return;
                }
                this.energyStored -= (long) ops * cost;
            } else if (this.burnTime <= 0) {
                return;
            }
            this.inputTank.drain(step.input(), ops * step.amountReq(), false);
            this.outputTank.fill(step.output(), ops * step.amountProduced(), false);
            this.lastConverted = ops * step.amountProduced();
            this.active = true;
        });
    }

    private void sendOutputFluid(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            int amount = Math.min(TRANSFER_PER_TICK, this.outputTank.amount());
            FluidStack stack = HbmFluids.toNeoStack(this.outputTank.type(), amount);
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    this.worldPosition.relative(direction),
                    direction.getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.outputTank.drain(this.outputTank.type(), accepted, false);
                sync();
            }
        }
    }

    private boolean isDrainableInputContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return FluidUtil.getFluidHandler(stack.copyWithCount(1))
                .map(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE))
                .filter(fluidStack -> !fluidStack.isEmpty())
                .flatMap(fluidStack -> HbmFluids.fromNeoFluid(fluidStack.getFluid()))
                .filter(fluid -> fluid == this.configuredInput && validInput(fluid))
                .isPresent();
    }

    private boolean isEmptyFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        HbmFluidDefinition fluid = this.outputTank.type().isNone() ? defaultOutput() : this.outputTank.type();
        if (fluid.isNone()) {
            return false;
        }
        return FluidUtil.getFluidHandler(stack.copyWithCount(1))
                .map(handler -> handler.fill(HbmFluids.toNeoStack(fluid, Math.max(1, this.outputTank.capacity())),
                        IFluidHandler.FluidAction.SIMULATE) > 0)
                .orElse(false);
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        ItemStack current = this.items[slot];
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        ItemStack current = this.items[slot];
        if (current.isEmpty()) {
            this.items[slot] = stack.copy();
        } else {
            current.grow(stack.getCount());
        }
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof SmallBoilerBlock && state.getValue(SmallBoilerBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(SmallBoilerBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static HbmFluidDefinition defaultInput() {
        return HbmFluids.byName("oil").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition defaultOutput() {
        return HbmFluids.byName("hotoil").orElse(HbmFluids.none());
    }

    public static boolean validInput(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return false;
        }
        return switch (fluid.name()) {
            case "oil", "oil_ds", "crackoil", "crackoil_ds" -> HbmThermalConversions.firstBoilerStep(fluid).isPresent();
            default -> false;
        };
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

    private final class BoilerFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return inputTank.getFluidInTank(0);
            }
            if (tank == 1) {
                return outputTank.getFluidInTank(0);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> inputTank.capacity();
                case 1 -> outputTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == configuredInput && validInput(fluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != configuredInput || !validInput(fluid)) {
                return 0;
            }
            int filled = inputTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != outputTank.type()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = outputTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }
}
