package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.GasTurbineBlock;
import com.reinhardt.hbm.client.sound.GasTurbineClientSounds;
import com.reinhardt.hbm.fluid.CombustibleFuelGrade;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.GasTurbineMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.FluidCopiable;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class GasTurbineBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int BATTERY_SLOT = 0;
    public static final int ID_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    public static final int DATA_COUNT = 17;
    public static final long ENERGY_CAPACITY = 1_000_000L;
    public static final int FUEL_CAPACITY = 100_000;
    public static final int LUBRICANT_CAPACITY = 16_000;
    public static final int WATER_CAPACITY = 16_000;
    public static final int STEAM_CAPACITY = 160_000;
    private static final int RPM_IDLE = 10;
    private static final int TEMP_IDLE = 300;
    private static final int[] ALL_SLOTS = {BATTERY_SLOT, ID_SLOT};
    private static final Map<String, Double> FUEL_MAX_CONSUMPTION = Map.of(
            "gas", 50.0D,
            "syngas", 10.0D,
            "oxyhydrogen", 100.0D,
            "reformgas", 5.0D
    );

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final HbmFluidTank fuelTank = new HbmFluidTank(fuel("gas"), FUEL_CAPACITY);
    private final HbmFluidTank lubricantTank = new HbmFluidTank(fluid("lubricant"), LUBRICANT_CAPACITY);
    private final HbmFluidTank waterTank = new HbmFluidTank(fluid("water"), WATER_CAPACITY);
    private final HbmFluidTank steamTank = new HbmFluidTank(fluid("hotsteam"), STEAM_CAPACITY);
    private long power;
    private long powerBeforeNet;
    private int rpm;
    private int temp = 20;
    private int powerSliderPos;
    private int throttle;
    private boolean autoMode;
    private int state;
    private int counter;
    private int instantPowerOutput;
    private double waterToBoil;
    private double fuelToConsume;
    private int rpmLast;
    private int tempLast;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, GasTurbineBlockEntity.this.powerBeforeNet);
                case 1 -> GasTurbineBlockEntity.this.rpm;
                case 2 -> GasTurbineBlockEntity.this.temp;
                case 3 -> GasTurbineBlockEntity.this.state;
                case 4 -> GasTurbineBlockEntity.this.autoMode ? 1 : 0;
                case 5 -> GasTurbineBlockEntity.this.throttle;
                case 6 -> GasTurbineBlockEntity.this.powerSliderPos;
                case 7 -> GasTurbineBlockEntity.this.state == 1 ? GasTurbineBlockEntity.this.instantPowerOutput : GasTurbineBlockEntity.this.counter;
                case 8 -> GasTurbineBlockEntity.this.fuelTank.type().oldId();
                case 9 -> GasTurbineBlockEntity.this.fuelTank.amount();
                case 10 -> GasTurbineBlockEntity.this.lubricantTank.type().oldId();
                case 11 -> GasTurbineBlockEntity.this.lubricantTank.amount();
                case 12 -> GasTurbineBlockEntity.this.waterTank.type().oldId();
                case 13 -> GasTurbineBlockEntity.this.waterTank.amount();
                case 14 -> GasTurbineBlockEntity.this.steamTank.type().oldId();
                case 15 -> GasTurbineBlockEntity.this.steamTank.amount();
                case 16 -> GasTurbineBlockEntity.this.instantPowerOutput;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> GasTurbineBlockEntity.this.powerBeforeNet = value;
                case 1 -> GasTurbineBlockEntity.this.rpm = value;
                case 2 -> GasTurbineBlockEntity.this.temp = value;
                case 3 -> GasTurbineBlockEntity.this.state = value;
                case 4 -> GasTurbineBlockEntity.this.autoMode = value != 0;
                case 5 -> GasTurbineBlockEntity.this.throttle = value;
                case 6 -> GasTurbineBlockEntity.this.powerSliderPos = value;
                case 7 -> {
                    if (GasTurbineBlockEntity.this.state == 1) {
                        GasTurbineBlockEntity.this.instantPowerOutput = value;
                    } else {
                        GasTurbineBlockEntity.this.counter = value;
                    }
                }
                case 16 -> GasTurbineBlockEntity.this.instantPowerOutput = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public GasTurbineBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.GAS_TURBINE.get(), pos, blockState);
        for (int slot = 0; slot < this.items.length; slot++) {
            this.items[slot] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GasTurbineBlockEntity turbine) {
        if (level.isClientSide) {
            GasTurbineClientSounds.tick(turbine);
            return;
        }
        turbine.tickServer(level);
    }

    public HbmFluidTank fuelTank() {
        return this.fuelTank;
    }

    public HbmFluidTank lubricantTank() {
        return this.lubricantTank;
    }

    public HbmFluidTank waterTank() {
        return this.waterTank;
    }

    public HbmFluidTank steamTank() {
        return this.steamTank;
    }

    public long power() {
        return this.powerBeforeNet;
    }

    public int rpm() {
        return this.rpm;
    }

    public int temp() {
        return this.temp;
    }

    public int state() {
        return this.state;
    }

    public boolean autoMode() {
        return this.autoMode;
    }

    public int throttle() {
        return this.throttle;
    }

    public int powerSliderPos() {
        return this.powerSliderPos;
    }

    public int counterOrOutput() {
        return this.state == 1 ? this.instantPowerOutput : this.counter;
    }

    public int instantPowerOutput() {
        return this.instantPowerOutput;
    }

    public double fuelConsumptionPerSecond() {
        double max = fuelMaxConsumption(this.fuelTank.type());
        return 20.0D * (max * 0.05D + max * (this.powerSliderPos / 60.0D));
    }

    public static boolean isGasFuel(HbmFluidDefinition fluid) {
        return fluid != null && fluid.combustibleFuelGrade() == CombustibleFuelGrade.GAS;
    }

    public static long combustionEnergy(HbmFluidDefinition fluid) {
        return fluid == null ? 0L : fluid.combustibleHeatEnergy();
    }

    public static double fuelMaxConsumption(HbmFluidDefinition fluid) {
        return fluid == null ? 5.0D : FUEL_MAX_CONSUMPTION.getOrDefault(fluid.name(), 5.0D);
    }

    public void setSlider(int slider) {
        this.powerSliderPos = Math.max(0, Math.min(60, slider));
        setChanged();
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
        setChanged();
    }

    public void requestState(int requestedState) {
        if (this.counter == 0 || this.counter == 579) {
            this.state = Math.max(-1, Math.min(1, requestedState));
            setChanged();
        }
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{
                this.fuelTank.type().oldId(),
                this.lubricantTank.type().oldId(),
                this.waterTank.type().oldId(),
                this.steamTank.type().oldId()
        };
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (setFuelType(fluid)) {
            sync();
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        PortKind kind = portKind(queriedPos, side);
        if (kind == null) {
            return null;
        }
        return new GasTurbineFluidHandler(kind);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        Direction facing = facing(level);
        return portsFor(this.worldPosition, facing);
    }

    public static List<Port> portsFor(BlockPos corePos, Direction facing) {
        Direction rot = LegacyMachineGeometry.forgeRotateUp(facing);
        return List.of(
                connectorPort(corePos, facing, -2, rot, 1, 0, facing.getOpposite(), PortKind.FUEL_LUBE),
                connectorPort(corePos, facing, 2, rot, 1, 0, facing, PortKind.FUEL_LUBE),
                connectorPort(corePos, facing, -2, rot, -4, 0, facing.getOpposite(), PortKind.WATER),
                connectorPort(corePos, facing, 2, rot, -4, 0, facing, PortKind.WATER),
                connectorPort(corePos, facing, 0, rot.getOpposite(), 6, 1, rot.getOpposite(), PortKind.STEAM)
        );
    }

    private static Port connectorPort(BlockPos corePos, Direction facing, int exDir, Direction rot, int exRot, int exY, Direction face, PortKind kind) {
        BlockPos connector = corePos.offset(LegacyMachineGeometry.legacyOffset(facing, exDir, rot, exRot, exY));
        return Port.fromConnector(connector, face, kind);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        Direction facing = facing(level);
        return powerConnectors(this.worldPosition, facing);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        Direction facing = facing(level);
        Direction rot = LegacyMachineGeometry.forgeRotateUp(facing);
        return connectorPos.equals(this.worldPosition.relative(rot, 5).above()) && machineSide == rot;
    }

    public static List<BlockPos> powerConnectors(BlockPos corePos, Direction facing) {
        Direction rot = LegacyMachineGeometry.forgeRotateUp(facing);
        return List.of(corePos.relative(rot, 5).above().immutable());
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
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.gas_turbine",
                this.instantPowerOutput,
                this.power,
                ENERGY_CAPACITY,
                this.rpm,
                this.throttle
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
        if (slot == ID_SLOT && applyIdentifier()) {
            sync();
        } else {
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
            case ID_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
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
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_turbinegas");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasTurbineMenu(containerId, playerInventory, this, this.menuData);
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
        for (int slot = 0; slot < this.items.length; slot++) {
            tag.put("Slot" + slot, this.items[slot].saveOptional(registries));
        }
        tag.put("FuelTank", this.fuelTank.save());
        tag.put("LubricantTank", this.lubricantTank.save());
        tag.put("WaterTank", this.waterTank.save());
        tag.put("SteamTank", this.steamTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("PowerBeforeNet", this.powerBeforeNet);
        tag.putInt("Rpm", this.rpm);
        tag.putInt("Temp", this.temp);
        tag.putInt("PowerSliderPos", this.powerSliderPos);
        tag.putInt("Throttle", this.throttle);
        tag.putBoolean("AutoMode", this.autoMode);
        tag.putInt("State", this.state);
        tag.putInt("Counter", this.counter);
        tag.putInt("InstantPowerOutput", this.instantPowerOutput);
        tag.putDouble("WaterToBoil", this.waterToBoil);
        tag.putDouble("FuelToConsume", this.fuelToConsume);
        tag.putInt("RpmLast", this.rpmLast);
        tag.putInt("TempLast", this.tempLast);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.length; slot++) {
            this.items[slot] = ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot));
        }
        this.fuelTank.load(tag.getCompound("FuelTank"));
        this.lubricantTank.load(tag.getCompound("LubricantTank"));
        this.waterTank.load(tag.getCompound("WaterTank"));
        this.steamTank.load(tag.getCompound("SteamTank"));
        this.power = tag.getLong("Power");
        this.powerBeforeNet = tag.getLong("PowerBeforeNet");
        this.rpm = tag.getInt("Rpm");
        this.temp = tag.contains("Temp") ? tag.getInt("Temp") : 20;
        this.powerSliderPos = tag.getInt("PowerSliderPos");
        this.throttle = tag.getInt("Throttle");
        this.autoMode = tag.getBoolean("AutoMode");
        this.state = tag.getInt("State");
        this.counter = tag.getInt("Counter");
        this.instantPowerOutput = tag.getInt("InstantPowerOutput");
        this.waterToBoil = tag.getDouble("WaterToBoil");
        this.fuelToConsume = tag.getDouble("FuelToConsume");
        this.rpmLast = tag.getInt("RpmLast");
        this.tempLast = tag.getInt("TempLast");
        setupDefaultTankTypes();
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
        setupDefaultTankTypes();
        this.waterToBoil = 0.0D;
        this.throttle = this.powerSliderPos * 100 / 60;
        if (applyIdentifier()) {
            sync();
        }

        if (this.autoMode) {
            int target;
            if (this.fuelTank.amount() * 10 > this.fuelTank.capacity()) {
                target = 60 - (int) (60.0D * this.power / ENERGY_CAPACITY);
            } else {
                target = (int) (this.fuelTank.amount() * 0.0001D
                        * (60 - (int) (60.0D * this.power / ENERGY_CAPACITY)));
            }
            if (target > this.powerSliderPos) {
                this.powerSliderPos++;
            } else if (target < this.powerSliderPos) {
                this.powerSliderPos--;
            }
        }

        switch (this.state) {
            case 0 -> shutdown(level);
            case -1 -> {
                stopIfNotReady();
                startup(level);
            }
            case 1 -> {
                stopIfNotReady();
                run(level);
            }
            default -> {
            }
        }

        this.powerBeforeNet = Math.min(this.power, ENERGY_CAPACITY);
        this.power = BatteryPackItem.chargeFromMachine(this.items[BATTERY_SLOT], this.power);
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.power = Math.min(this.power, ENERGY_CAPACITY);
        pullFluids(level);
        sendSteam(level);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    /**
     * HBM 1.7.10 reads the fluid identifier slot every server tick. Keep the
     * selected gas type and the exposed port capabilities in lockstep.
     */
    private boolean applyIdentifier() {
        ItemStack identifier = this.items[ID_SLOT];
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            return setFuelType(FluidIdentifierItem.primary(identifier));
        }
        return false;
    }

    private boolean setFuelType(HbmFluidDefinition next) {
        if (!isGasFuel(next) || this.fuelTank.type() == next) {
            return false;
        }
        this.fuelTank.setType(next);
        return true;
    }

    private void setupDefaultTankTypes() {
        if (this.fuelTank.type().isNone() && this.fuelTank.amount() == 0) {
            this.fuelTank.setType(fuel("gas"));
        }
        if (this.lubricantTank.type().isNone() && this.lubricantTank.amount() == 0) {
            this.lubricantTank.setType(fluid("lubricant"));
        }
        if (this.waterTank.type().isNone() && this.waterTank.amount() == 0) {
            this.waterTank.setType(fluid("water"));
        }
        if (this.steamTank.type().isNone() && this.steamTank.amount() == 0) {
            this.steamTank.setType(fluid("hotsteam"));
        }
    }

    private void stopIfNotReady() {
        if (this.fuelTank.amount() == 0 || this.lubricantTank.amount() == 0 || !isGasFuel(this.fuelTank.type())) {
            this.state = 0;
        }
    }

    private void startup(Level level) {
        this.counter++;
        if (this.counter <= 20) {
            this.rpm = 5 * this.counter;
        } else if (this.counter <= 40) {
            this.rpm = 100 - 5 * (this.counter - 20);
        } else if (this.counter > 50) {
            this.rpm = RPM_IDLE * (this.counter - 50) / 530;
            this.temp = TEMP_IDLE * (this.counter - 50) / 530;
        }

        if (this.counter == 50) {
            level.playSound(null, this.worldPosition.getX(), this.worldPosition.getY() + 2.0D, this.worldPosition.getZ(), HbmSoundEvents.TURBINE_GAS_STARTUP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (this.counter == 580) {
            this.counter = 225;
            this.state = 1;
        }
    }

    private void shutdown(Level level) {
        this.autoMode = false;
        this.instantPowerOutput = 0;
        if (this.powerSliderPos > 0) {
            this.powerSliderPos--;
        }

        if (this.rpm <= 10 && this.counter > 0) {
            if (this.counter == 225) {
                level.playSound(null, this.worldPosition.getX(), this.worldPosition.getY() + 2.0D, this.worldPosition.getZ(), HbmSoundEvents.TURBINE_GAS_SHUTDOWN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                this.rpmLast = this.rpm;
                this.tempLast = this.temp;
            }
            this.counter--;
            this.rpm = this.rpmLast * this.counter / 225;
            this.temp = this.tempLast * this.counter / 225;
        } else if (this.rpm > 11) {
            this.counter = 42069;
            this.rpm--;
        } else if (this.rpm == 11) {
            this.counter = 225;
            this.rpm--;
        }
    }

    private void run(Level level) {
        long gameTime = level.getGameTime();
        if ((int) (this.throttle * 0.9D) > this.rpm - RPM_IDLE) {
            if (gameTime % 5L == 0L) {
                this.rpm++;
            }
        } else if ((int) (this.throttle * 0.9D) < this.rpm - RPM_IDLE) {
            if (gameTime % 2L == 0L) {
                this.rpm--;
            }
        }

        int maxTemp = fluidBurnTemp(this.fuelTank.type());
        int targetTemp = this.throttle * 5 * (maxTemp - TEMP_IDLE) / 500;
        if (targetTemp > this.temp - TEMP_IDLE) {
            if (gameTime % 2L == 0L) {
                this.temp++;
            }
        } else if (targetTemp < this.temp - TEMP_IDLE) {
            if (gameTime % 2L == 0L) {
                this.temp--;
            }
        }

        makePower(level, fuelMaxConsumption(this.fuelTank.type()), this.throttle);
    }

    private int fluidBurnTemp(HbmFluidDefinition type) {
        double energy = combustionEnergy(type);
        return (int) Math.floor(800.0D - Math.pow(Math.E, -energy / 100_000.0D) * 300.0D);
    }

    private void makePower(Level level, double consMax, int throttle) {
        double idleConsumption = consMax * 0.05D;
        double consumption = idleConsumption + consMax * throttle / 100.0D;
        this.fuelToConsume += consumption;

        int fuelConsumed = (int) Math.floor(this.fuelToConsume);
        if (fuelConsumed > 0) {
            HbmFluidDefinition burnedFuel = this.fuelTank.type();
            this.fuelToConsume -= fuelConsumed;
            int fuelBeforeBurn = this.fuelTank.amount();
            this.fuelTank.setAmount(Math.max(0, fuelBeforeBurn - fuelConsumed));
            if (fuelConsumed > fuelBeforeBurn) {
                this.fuelTank.setAmount(0);
                this.state = 0;
            }
            HbmPollution.polluteFluid(level, this.worldPosition, burnedFuel, HbmPollution.ReleaseType.BURN, fuelConsumed * 5.0D);
            if (!burnedFuel.name().equals("oxyhydrogen") && level.getGameTime() % 20L == 0L) {
                HbmPollution.increment(level, this.worldPosition, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND * 3.0D);
            }
        }

        if (level.getGameTime() % 10L == 0L) {
            if (this.lubricantTank.amount() <= 0) {
                this.lubricantTank.setAmount(0);
                this.state = 0;
            } else {
                this.lubricantTank.setAmount(this.lubricantTank.amount() - 1);
            }
        }

        long energy = combustionEnergy(this.fuelTank.type()) / 1000L;
        int rpmEff = this.rpm - RPM_IDLE;
        double targetOutput = consMax * energy * rpmEff / 90.0D;
        if (this.instantPowerOutput < targetOutput) {
            this.instantPowerOutput += (int) (Math.random() * 0.005D * consMax * energy);
            if (this.instantPowerOutput > targetOutput) {
                this.instantPowerOutput = (int) targetOutput;
            }
        } else if (this.instantPowerOutput > targetOutput) {
            this.instantPowerOutput -= (int) (Math.random() * 0.011D * consMax * energy);
            if (this.instantPowerOutput < targetOutput) {
                this.instantPowerOutput = (int) targetOutput;
            }
        }
        this.power += this.instantPowerOutput;

        double waterPerTick = consMax * energy * (this.temp - TEMP_IDLE) / 220_000.0D;
        this.waterToBoil = waterPerTick;
        int heatCycles = (int) Math.floor(this.waterToBoil);
        int waterCycles = this.waterTank.amount();
        int steamCycles = (this.steamTank.capacity() - this.steamTank.amount()) / 10;
        int cycles = Math.min(heatCycles, Math.min(waterCycles, steamCycles));
        if (cycles > 0) {
            this.waterTank.setAmount(this.waterTank.amount() - cycles);
            this.steamTank.fill(fluid("hotsteam"), cycles * 10, false);
        }
    }

    private void pullFluids(Level level) {
        for (Port port : ports(level)) {
            if (port.kind() == PortKind.FUEL_LUBE) {
                pullInto(level, port, this.fuelTank, this.fuelTank.type(), true);
                pullInto(level, port, this.lubricantTank, fluid("lubricant"), false);
            } else if (port.kind() == PortKind.WATER) {
                pullInto(level, port, this.waterTank, fluid("water"), false);
            }
        }
    }

    private void pullInto(Level level, Port port, HbmFluidTank tank, HbmFluidDefinition type, boolean requireGasFuel) {
        int space = tank.capacity() - tank.amount();
        if (space <= 0 || type.isNone() || (requireGasFuel && !isGasFuel(type))) {
            return;
        }
        FluidStack drained = HbmFluidNetworks.drainFrom(
                level,
                port.connectorPos(),
                port.face().getOpposite(),
                type,
                space,
                this.worldPosition,
                true
        );
        if (!drained.isEmpty()) {
            tank.fill(type, drained.getAmount(), false);
        }
    }

    private void sendSteam(Level level) {
        if (this.steamTank.amount() <= 0 || this.steamTank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (port.kind() != PortKind.STEAM) {
                continue;
            }
            FluidStack stack = HbmFluids.toNeoStack(this.steamTank.type(), this.steamTank.amount());
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.steamTank.drain(this.steamTank.type(), accepted, false);
            }
        }
    }

    @Nullable
    private PortKind portKind(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return null;
        }
        for (Port port : ports(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return port.kind();
            }
        }
        return null;
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
        return state.hasProperty(GasTurbineBlock.FACING) ? state.getValue(GasTurbineBlock.FACING) : Direction.NORTH;
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition fuel(String name) {
        return HbmFluids.byName(name).filter(GasTurbineBlockEntity::isGasFuel).orElse(HbmFluids.none());
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

    public enum PortKind {
        FUEL_LUBE,
        WATER,
        STEAM
    }

    public record Port(BlockPos pos, Direction face, PortKind kind) {
        private static Port fromConnector(BlockPos connectorPos, Direction face, PortKind kind) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face, kind);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face);
        }
    }

    private final class GasTurbineFluidHandler implements IFluidHandler {
        private final PortKind kind;

        private GasTurbineFluidHandler(PortKind kind) {
            this.kind = kind;
        }

        @Override
        public int getTanks() {
            return 4;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> fuelTank.getFluidInTank(0);
                case 1 -> lubricantTank.getFluidInTank(0);
                case 2 -> waterTank.getFluidInTank(0);
                case 3 -> steamTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> fuelTank.capacity();
                case 1 -> lubricantTank.capacity();
                case 2 -> waterTank.capacity();
                case 3 -> steamTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return switch (this.kind) {
                case FUEL_LUBE -> (tank == 0 && isGasFuel(fluid)) || (tank == 1 && fluid.name().equals("lubricant"));
                case WATER -> tank == 2 && fluid.name().equals("water");
                case STEAM -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            int filled = switch (this.kind) {
                case FUEL_LUBE -> {
                    if (isGasFuel(fluid)) {
                        yield fuelTank.fill(fluid, resource.getAmount(), action.simulate());
                    }
                    if (fluid.name().equals("lubricant")) {
                        yield lubricantTank.fill(fluid, resource.getAmount(), action.simulate());
                    }
                    yield 0;
                }
                case WATER -> fluid.name().equals("water") ? waterTank.fill(fluid, resource.getAmount(), action.simulate()) : 0;
                case STEAM -> 0;
            };
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || this.kind != PortKind.STEAM) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != steamTank.type()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || this.kind != PortKind.STEAM) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = steamTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }
}
