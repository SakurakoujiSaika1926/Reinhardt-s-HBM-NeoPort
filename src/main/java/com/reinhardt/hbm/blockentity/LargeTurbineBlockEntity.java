package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.client.sound.LargeTurbineClientSounds;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.menu.LargeTurbineMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.FluidCopiable;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class LargeTurbineBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int ID_SLOT = 0;
    public static final int ID_RESULT_SLOT = 1;
    public static final int INPUT_CONTAINER_SLOT = 2;
    public static final int INPUT_CONTAINER_RESULT_SLOT = 3;
    public static final int BATTERY_SLOT = 4;
    public static final int OUTPUT_CONTAINER_SLOT = 5;
    public static final int OUTPUT_CONTAINER_RESULT_SLOT = 6;
    public static final int SLOT_COUNT = 7;
    public static final int DATA_COUNT = 8;
    public static final int INPUT_CAPACITY = 512_000;
    public static final int OUTPUT_CAPACITY = 10_240_000;
    public static final long ENERGY_CAPACITY = 100_000_000L;
    private static final int[] AUTOMATION_SLOTS = {
            INPUT_CONTAINER_SLOT,
            INPUT_CONTAINER_RESULT_SLOT,
            OUTPUT_CONTAINER_SLOT,
            OUTPUT_CONTAINER_RESULT_SLOT
    };
    private static final int[] NO_SLOTS = {};

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final HbmFluidTank inputTank = new HbmFluidTank(steam(), INPUT_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(spentSteam(), OUTPUT_CAPACITY);
    private HbmFluidDefinition configuredInput = steam();
    private long power;
    private long lastOutput;
    private boolean shouldTurn;
    private float rotor;
    private float lastRotor;
    private float fanAcceleration;
    private final float audioDesync = new Random().nextFloat() * 0.05F;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> LargeTurbineBlockEntity.this.inputTank.type().oldId();
                case 1 -> LargeTurbineBlockEntity.this.inputTank.amount();
                case 2 -> LargeTurbineBlockEntity.this.outputTank.type().oldId();
                case 3 -> LargeTurbineBlockEntity.this.outputTank.amount();
                case 4 -> LargeTurbineBlockEntity.this.outputTank.capacity();
                case 5 -> (int) Math.min(Integer.MAX_VALUE, LargeTurbineBlockEntity.this.power);
                case 6 -> (int) Math.min(Integer.MAX_VALUE, LargeTurbineBlockEntity.this.lastOutput);
                case 7 -> LargeTurbineBlockEntity.this.shouldTurn ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> LargeTurbineBlockEntity.this.configuredInput = HbmFluids.byOldId(value).orElse(steam());
                case 5 -> LargeTurbineBlockEntity.this.power = value;
                case 6 -> LargeTurbineBlockEntity.this.lastOutput = value;
                case 7 -> LargeTurbineBlockEntity.this.shouldTurn = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public LargeTurbineBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.LARGE_TURBINE.get(), pos, blockState);
        for (int slot = 0; slot < this.items.length; slot++) {
            this.items[slot] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LargeTurbineBlockEntity turbine) {
        if (level.isClientSide) {
            turbine.tickClient();
            LargeTurbineClientSounds.tick(turbine);
            return;
        }
        turbine.tickServer(level);
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public long power() {
        return this.power;
    }

    public long lastOutput() {
        return this.lastOutput;
    }

    public boolean shouldTurn() {
        return this.shouldTurn;
    }

    public float fanAcceleration() {
        return this.fanAcceleration;
    }

    public float audioDesync() {
        return this.audioDesync;
    }

    public float rotor(float partialTick) {
        return this.lastRotor + (this.rotor - this.lastRotor) * partialTick;
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.configuredInput.oldId(), this.outputTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        setConfiguredInput(fluid);
    }

    public void setConfiguredInput(HbmFluidDefinition fluid) {
        if (fluid == null || HbmThermalConversions.turbineStep(fluid).isEmpty()) {
            return;
        }
        if (this.configuredInput == fluid) {
            return;
        }
        this.configuredInput = fluid;
        this.inputTank.clear();
        this.outputTank.clear();
        setupTanks();
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new LargeTurbineFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        Direction facing = facing(level);
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();
        return List.of(
                new Port(this.worldPosition.relative(right), right),
                new Port(this.worldPosition.relative(left), left),
                new Port(this.worldPosition.relative(facing), facing)
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        Direction facing = facing(level);
        return List.of(this.worldPosition.relative(facing.getOpposite(), 4).immutable());
    }

    @Override
    public long getAvailableOutput() {
        return this.power;
    }

    @Override
    public long getRequestedInput() {
        return 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.max(0L, this.power - usedOutput);
        this.lastOutput = usedOutput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.large_turbine",
                this.lastOutput,
                this.power,
                ENERGY_CAPACITY,
                this.inputTank.amount(),
                this.outputTank.amount()
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
        return slot >= 0 && slot < SLOT_COUNT ? this.items[slot] : ItemStack.EMPTY;
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
        if (slot >= 0 && slot < SLOT_COUNT) {
            this.items[slot] = ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
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
            case INPUT_CONTAINER_SLOT -> isFilledInputContainer(stack);
            case BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
            case OUTPUT_CONTAINER_SLOT -> isEmptyFluidContainer(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == null ? NO_SLOTS : AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return (slot == INPUT_CONTAINER_SLOT || slot == OUTPUT_CONTAINER_SLOT) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == INPUT_CONTAINER_RESULT_SLOT || slot == OUTPUT_CONTAINER_RESULT_SLOT;
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
        return Component.translatable("container.reinhardtshbm.machine_large_turbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LargeTurbineMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData getMenuData() {
        return this.menuData;
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
        tag.putLong("Power", this.power);
        tag.putLong("LastOutput", this.lastOutput);
        tag.putBoolean("ShouldTurn", this.shouldTurn);
        tag.putFloat("Rotor", this.rotor);
        tag.putFloat("LastRotor", this.lastRotor);
        tag.putFloat("FanAcceleration", this.fanAcceleration);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.parseOptional(registries, tag.getCompound("Slot" + i));
        }
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput")).orElse(steam());
        this.power = tag.getLong("Power");
        this.lastOutput = tag.getLong("LastOutput");
        this.shouldTurn = tag.getBoolean("ShouldTurn");
        this.rotor = tag.getFloat("Rotor");
        this.lastRotor = tag.getFloat("LastRotor");
        this.fanAcceleration = tag.getFloat("FanAcceleration");
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

    private void tickServer(Level level) {
        tickContainers();
        applyIdentifier();
        pullInput(level);
        this.power = BatteryPackItem.chargeFromMachine(this.items[BATTERY_SLOT], this.power);
        this.power = (long) (this.power * 0.95D);
        tryConvert();
        sendOutputFluid(level);
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.power = Math.min(ENERGY_CAPACITY, this.power);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient() {
        this.lastRotor = this.rotor;
        this.rotor += this.fanAcceleration;
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.lastRotor -= 360.0F;
        }

        if (this.shouldTurn) {
            this.fanAcceleration = Math.max(0.0F, Math.min(15.0F, this.fanAcceleration + 0.075F + this.audioDesync));
        } else {
            this.fanAcceleration = Math.max(0.0F, Math.min(15.0F, this.fanAcceleration - 0.1F));
        }
    }

    private void applyIdentifier() {
        ItemStack identifier = this.items[ID_SLOT];
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition next = FluidIdentifierItem.primary(identifier);
            if (HbmThermalConversions.turbineStep(next).isPresent() && this.configuredInput != next) {
                this.configuredInput = next;
                this.inputTank.clear();
                this.outputTank.clear();
            }
        }
        setupTanks();
    }

    private void setupTanks() {
        HbmThermalConversions.turbineStep(this.configuredInput).ifPresentOrElse(step -> {
            if (this.inputTank.amount() == 0 && this.inputTank.type() != step.input()) {
                this.inputTank.setType(step.input());
            }
            if (this.outputTank.amount() == 0 && this.outputTank.type() != step.output()) {
                this.outputTank.setType(step.output());
            }
        }, () -> {
            this.configuredInput = steam();
            if (this.inputTank.amount() == 0) {
                this.inputTank.setType(this.configuredInput);
            }
            if (this.outputTank.amount() == 0) {
                this.outputTank.setType(HbmFluids.none());
            }
        });
    }

    private void tryConvert() {
        this.shouldTurn = false;
        HbmThermalConversions.turbineStep(this.configuredInput).ifPresentOrElse(step -> {
            this.outputTank.setType(step.output());
            int inputOps = this.inputTank.amount() / step.amountReq();
            int outputOps = (this.outputTank.capacity() - this.outputTank.amount()) / step.amountProduced();
            int cap = (int) Math.ceil((double) this.inputTank.amount() / step.amountReq() / 5.0D);
            int ops = Math.min(inputOps, Math.min(outputOps, cap));
            if (ops <= 0) {
                return;
            }
            this.inputTank.drain(step.input(), ops * step.amountReq(), false);
            this.outputTank.fill(step.output(), ops * step.amountProduced(), false);
            this.power = Math.min(ENERGY_CAPACITY, this.power + (long) (ops * step.heatEnergy() * step.turbineEfficiency()));
            this.shouldTurn = true;
        }, () -> this.outputTank.setType(HbmFluids.none()));
    }

    private void tickContainers() {
        ItemStack input = this.items[INPUT_CONTAINER_SLOT];
        if (isFilledInputContainer(input) && this.items[INPUT_CONTAINER_RESULT_SLOT].isEmpty()) {
            HbmFluidDefinition fluid = HbmFluidContainerItem.fluid(input);
            HbmFluidContainerItem item = (HbmFluidContainerItem) input.getItem();
            int accepted = this.inputTank.fill(fluid, item.kind().capacity(), false);
            if (accepted == item.kind().capacity()) {
                input.shrink(1);
                this.items[INPUT_CONTAINER_RESULT_SLOT] = item.kind().emptyStack();
                if (input.isEmpty()) {
                    this.items[INPUT_CONTAINER_SLOT] = ItemStack.EMPTY;
                }
            }
        }

        ItemStack output = this.items[OUTPUT_CONTAINER_SLOT];
        if (isEmptyFluidContainer(output) && this.items[OUTPUT_CONTAINER_RESULT_SLOT].isEmpty()) {
            HbmFluidContainerItem item = (HbmFluidContainerItem) output.getItem();
            HbmFluidDefinition fluid = this.outputTank.type();
            if (!fluid.isNone() && item.kind().allows(fluid) && this.outputTank.amount() >= item.kind().capacity()) {
                this.outputTank.drain(fluid, item.kind().capacity(), false);
                output.shrink(1);
                this.items[OUTPUT_CONTAINER_RESULT_SLOT] = fullContainerFor(item.kind(), fluid);
                if (output.isEmpty()) {
                    this.items[OUTPUT_CONTAINER_SLOT] = ItemStack.EMPTY;
                }
            }
        }
    }

    private void pullInput(Level level) {
        int space = this.inputTank.capacity() - this.inputTank.amount();
        if (space <= 0 || this.configuredInput.isNone()) {
            return;
        }

        for (Port port : ports(level)) {
            space = this.inputTank.capacity() - this.inputTank.amount();
            if (space <= 0) {
                break;
            }
            FluidStack drained = HbmFluidNetworks.drainFrom(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    this.configuredInput,
                    space,
                    this.worldPosition,
                    true
            );
            if (!drained.isEmpty()) {
                this.inputTank.fill(this.configuredInput, drained.getAmount(), false);
            }
        }
    }

    private void sendOutputFluid(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(this.outputTank.type(), this.outputTank.amount());
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.outputTank.drain(this.outputTank.type(), accepted, false);
            }
        }
    }

    private boolean isFilledInputContainer(ItemStack stack) {
        if (!(stack.getItem() instanceof HbmFluidContainerItem item) || !item.isFilledContainer()) {
            return false;
        }
        HbmFluidDefinition fluid = HbmFluidContainerItem.fluid(stack);
        return fluid == this.configuredInput && HbmThermalConversions.turbineStep(fluid).isPresent();
    }

    private boolean isEmptyFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof HbmFluidContainerItem item && !item.isFilledContainer();
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : ports(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : ports(this.level)) {
                this.level.invalidateCapabilities(port.pos());
            }
        }
    }

    private Direction facing(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    private static ItemStack fullContainerFor(HbmFluidContainerItem.Kind kind, HbmFluidDefinition fluid) {
        return switch (kind) {
            case CANISTER -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.CANISTER_FULL::get, fluid);
            case GAS_TANK -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.GAS_FULL::get, fluid);
            case FLUID_TANK -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.FLUID_TANK_FULL::get, fluid);
            case LEAD_TANK -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.FLUID_TANK_LEAD_FULL::get, fluid);
            case FLUID_BARREL -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.FLUID_BARREL_FULL::get, fluid);
            case FLUID_PACK -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.FLUID_PACK_FULL::get, fluid);
            case DISPERSER -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.DISPERSER_CANISTER::get, fluid);
            case GLYPHID_GLAND -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.GLYPHID_GLAND::get, fluid);
            case CELL -> HbmFluidContainerItem.makeFull(com.reinhardt.hbm.registry.HbmItems.CELL_TRITIUM::get, fluid);
        };
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

    private static HbmFluidDefinition steam() {
        return HbmFluids.byName("steam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        public BlockPos connectorPos() {
            return this.pos.relative(this.face);
        }
    }

    private final class LargeTurbineFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private LargeTurbineFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

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
            if (tank != 0 || stack.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == configuredInput && HbmThermalConversions.turbineStep(fluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != configuredInput || HbmThermalConversions.turbineStep(fluid).isEmpty()) {
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
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
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
            if (maxDrain <= 0 || !allowsPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = outputTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }
}
