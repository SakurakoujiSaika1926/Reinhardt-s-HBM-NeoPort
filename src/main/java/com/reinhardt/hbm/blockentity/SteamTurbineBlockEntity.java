package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.menu.SteamTurbineMenu;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class SteamTurbineBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int ID_SLOT = 0;
    public static final int ID_RESULT_SLOT = 1;
    public static final int INPUT_CONTAINER_SLOT = 2;
    public static final int INPUT_CONTAINER_RESULT_SLOT = 3;
    public static final int BATTERY_SLOT = 4;
    public static final int OUTPUT_CONTAINER_SLOT = 5;
    public static final int OUTPUT_CONTAINER_RESULT_SLOT = 6;
    public static final int SLOT_COUNT = 7;
    public static final int DATA_COUNT = 7;
    public static final int INPUT_CAPACITY = 64_000;
    public static final int OUTPUT_CAPACITY = 128_000;
    public static final long ENERGY_CAPACITY = 1_000_000L;
    private static final int INPUT_PER_TICK = 6_000;
    private static final double SMALL_TURBINE_EFFICIENCY = 0.85D;
    private static final int[] AUTOMATION_SLOTS = {
            INPUT_CONTAINER_SLOT,
            INPUT_CONTAINER_RESULT_SLOT,
            OUTPUT_CONTAINER_SLOT,
            OUTPUT_CONTAINER_RESULT_SLOT
    };
    private static final int[] NO_SLOTS = {};

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final HbmFluidTank inputTank = new HbmFluidTank(HbmFluids.byName("steam").orElse(HbmFluids.none()), INPUT_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(HbmFluids.byName("spentsteam").orElse(HbmFluids.none()), OUTPUT_CAPACITY);
    private HbmFluidDefinition configuredInput = HbmFluids.byName("steam").orElse(HbmFluids.none());
    private long power;
    private long lastOutput;
    private boolean active;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SteamTurbineBlockEntity.this.inputTank.type().oldId();
                case 1 -> SteamTurbineBlockEntity.this.inputTank.amount();
                case 2 -> SteamTurbineBlockEntity.this.outputTank.type().oldId();
                case 3 -> SteamTurbineBlockEntity.this.outputTank.amount();
                case 4 -> SteamTurbineBlockEntity.this.outputTank.capacity();
                case 5 -> (int) SteamTurbineBlockEntity.this.power;
                case 6 -> (int) SteamTurbineBlockEntity.this.lastOutput;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> SteamTurbineBlockEntity.this.configuredInput = HbmFluids.byOldId(value).orElse(HbmFluids.none());
                case 5 -> SteamTurbineBlockEntity.this.power = value;
                case 6 -> SteamTurbineBlockEntity.this.lastOutput = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SteamTurbineBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.STEAM_TURBINE.get(), pos, blockState);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamTurbineBlockEntity turbine) {
        // TileEntityMachineTurbine#updateEntity only mutates tanks and power on the server.
        if (level.isClientSide) {
            return;
        }
        turbine.tickContainers();
        turbine.applyIdentifier();
        turbine.power = BatteryPackItem.chargeFromMachine(turbine.items[BATTERY_SLOT], turbine.power);
        turbine.power = (long) (turbine.power * 0.95D);
        turbine.tryConvert();
        turbine.sendOutputFluid(level);
        PowerNetworkManager.tickFromEndpoint(level, turbine);
        turbine.power = Math.min(ENERGY_CAPACITY, turbine.power);
        turbine.setChanged();
        if (level.getGameTime() % 10L == 0L) {
            turbine.sync();
        }
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

    public boolean active() {
        return this.active;
    }

    public HbmFluidDefinition configuredInput() {
        return this.configuredInput;
    }

    public void setConfiguredInput(HbmFluidDefinition fluid) {
        HbmFluidDefinition next = fluid == null ? HbmFluids.none() : fluid;
        if (HbmThermalConversions.turbineStep(next).isEmpty()) {
            return;
        }
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
        return new TurbineFluidHandler();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
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
                "message.reinhardtshbm.power.steam_turbine",
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
        return Component.translatable("container.reinhardtshbm.machine_turbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SteamTurbineMenu(containerId, playerInventory, this, this.menuData);
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
        this.configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput"))
                .orElse(HbmFluids.byName("steam").orElse(HbmFluids.none()));
        this.power = tag.getLong("Power");
        this.lastOutput = tag.getLong("LastOutput");
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

    private void applyIdentifier() {
        ItemStack identifier = this.items[ID_SLOT];
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition next = FluidIdentifierItem.primary(identifier);
            if (HbmThermalConversions.turbineStep(next).isPresent()
                    && this.configuredInput != next
                    && this.items[ID_RESULT_SLOT].isEmpty()) {
                this.configuredInput = next;
                this.inputTank.clear();
                this.outputTank.clear();
                this.items[ID_RESULT_SLOT] = identifier.copy();
                this.items[ID_SLOT] = ItemStack.EMPTY;
                setupTanks();
                sync();
                return;
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
            this.configuredInput = HbmFluids.byName("steam").orElse(HbmFluids.none());
            if (this.inputTank.amount() == 0) {
                this.inputTank.setType(this.configuredInput);
            }
        });
    }

    private void tryConvert() {
        this.active = false;
        HbmThermalConversions.turbineStep(this.configuredInput).ifPresent(step -> {
            int inputOps = this.inputTank.amount() / step.amountReq();
            int outputOps = (this.outputTank.capacity() - this.outputTank.amount()) / step.amountProduced();
            int cap = INPUT_PER_TICK / step.amountReq();
            int ops = Math.min(inputOps, Math.min(outputOps, cap));
            if (ops <= 0) {
                return;
            }
            this.inputTank.drain(step.input(), ops * step.amountReq(), false);
            this.outputTank.fill(step.output(), ops * step.amountProduced(), false);
            this.power = Math.min(ENERGY_CAPACITY, this.power + (long) (ops * step.heatEnergy() * step.turbineEfficiency() * SMALL_TURBINE_EFFICIENCY));
            this.active = true;
        });
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

    private void sendOutputFluid(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            int amount = Math.min(16_000, this.outputTank.amount());
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

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
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

    private final class TurbineFluidHandler implements IFluidHandler {
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
            return fluid == configuredInput && HbmThermalConversions.turbineStep(fluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
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
