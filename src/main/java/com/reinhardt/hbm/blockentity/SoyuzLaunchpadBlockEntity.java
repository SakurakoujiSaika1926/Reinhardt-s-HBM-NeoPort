package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.SoyuzLaunchpadBlock;
import com.reinhardt.hbm.entity.SoyuzEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.SoyuzItem;
import com.reinhardt.hbm.menu.SoyuzLaunchpadMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.satellite.SatelliteSavedData;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Literal high-version port of the latest TileEntityLaunchpadSoyuz state machine. */
public final class SoyuzLaunchpadBlockEntity extends BlockEntity
        implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_SATELLITE = 2;
    public static final int SLOT_ORBITAL_MODULE = 3;
    public static final int SLOT_KEROSENE_IN = 4;
    public static final int SLOT_KEROSENE_OUT = 5;
    public static final int SLOT_OXYGEN_IN = 6;
    public static final int SLOT_OXYGEN_OUT = 7;
    public static final int SLOT_BATTERY = 8;
    public static final int SLOT_CARGO_START = 9;
    public static final int SLOT_COUNT = 27;
    public static final int DATA_COUNT = 9;

    public static final int INDEX_STRUT1 = 0;
    public static final int INDEX_STRUT2 = 1;
    public static final int INDEX_STRUT3 = 2;
    public static final int INDEX_STRUT4 = 3;
    public static final int INDEX_STRUT5 = 4;
    public static final int INDEX_CARRIAGE = 5;
    public static final int INDEX_ROTOR = 6;
    public static final int INDEX_TILT = 7;
    public static final int ANIMATION_COUNT = 8;

    public static final long MAX_POWER = 1_000_000L;
    public static final long CONSUMPTION = 10_000L;
    public static final int TANK_CAPACITY = 128_000;
    public static final int FUEL_REQUIRED = 100_000;
    public static final int FUEL_DURATION = 15 * 20;
    public static final int COUNTDOWN_DURATION = 600;

    private static final int[] ACCESSIBLE_SLOTS = {
            SLOT_ROCKET, SLOT_SATELLITE, SLOT_ORBITAL_MODULE,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    /*
     * LaunchpadSoyuz gained a power/fluid/inventory proxy in upstream commit
     * 36ef9ea303 at the same time that this exact 2 x 8 x 5 service annex was
     * added to fillSpace.  The old implementation omitted every makeExtra
     * call, leaving the proxy factory unreachable.  Keep the authored
     * structure unchanged and expose only the ground-level exterior of that
     * already-authored annex.  Coordinates are in the model's EAST-authored
     * basis; rotation below remains machine-specific.
     */
    private static final List<AuthoredPort> AUTHORED_SERVICE_PORTS = authoredServicePorts();

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank keroseneTank = new HbmFluidTank(kerosene(), TANK_CAPACITY);
    private final HbmFluidTank oxygenTank = new HbmFluidTank(oxygen(), TANK_CAPACITY);
    private final float[] positions = new float[ANIMATION_COUNT];
    private final float[] previousPositions = new float[ANIMATION_COUNT];
    private final float[] speed = new float[ANIMATION_COUNT];
    private final float[] target = new float[ANIMATION_COUNT];
    private final float[] synchronizedPositions = new float[ANIMATION_COUNT];

    private long power;
    private int interpolationTicks;
    private SoyuzStatus soyuzStatus = SoyuzStatus.ABSENT;
    private ComponentStatus strutStatus = ComponentStatus.RETRACT;
    private ComponentStatus carriageStatus = ComponentStatus.RETRACT;
    private ComponentStatus rotorStatus = ComponentStatus.RETRACT;
    private boolean cargoMode;
    private int loadedType = -1;
    private int fuelCountdown;
    private int countdown;
    @Nullable
    private BlockPos servicePortCacheOrigin;
    @Nullable
    private Direction servicePortCacheFacing;
    private List<CapabilityPort> servicePorts = List.of();
    private List<BlockPos> servicePortHosts = List.of();
    private List<BlockPos> powerConnectorPositions = List.of();

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) SoyuzLaunchpadBlockEntity.this.power;
                case 1 -> SoyuzLaunchpadBlockEntity.this.loadedType;
                case 2 -> SoyuzLaunchpadBlockEntity.this.cargoMode ? 1 : 0;
                case 3 -> SoyuzLaunchpadBlockEntity.this.countdown;
                case 4 -> SoyuzLaunchpadBlockEntity.this.soyuzStatus.ordinal();
                case 5 -> SoyuzLaunchpadBlockEntity.this.keroseneTank.amount();
                case 6 -> SoyuzLaunchpadBlockEntity.this.keroseneTank.capacity();
                case 7 -> SoyuzLaunchpadBlockEntity.this.oxygenTank.amount();
                case 8 -> SoyuzLaunchpadBlockEntity.this.oxygenTank.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> SoyuzLaunchpadBlockEntity.this.power = value;
                case 1 -> SoyuzLaunchpadBlockEntity.this.loadedType = value;
                case 2 -> SoyuzLaunchpadBlockEntity.this.cargoMode = value != 0;
                case 3 -> SoyuzLaunchpadBlockEntity.this.countdown = value;
                case 4 -> SoyuzLaunchpadBlockEntity.this.soyuzStatus = safeStatus(value);
                case 5 -> SoyuzLaunchpadBlockEntity.this.keroseneTank.setAmount(value);
                case 7 -> SoyuzLaunchpadBlockEntity.this.oxygenTank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SoyuzLaunchpadBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SOYUZ_LAUNCHPAD.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            SoyuzLaunchpadBlockEntity launchpad) {
        if (level.isClientSide) {
            launchpad.clientTick();
            com.reinhardt.hbm.client.sound.SoyuzLaunchpadClientEffects.tick(launchpad);
            return;
        }

        boolean changed = launchpad.tickContainers();
        long oldPower = launchpad.power;
        launchpad.power = BatteryPackItem.dischargeIntoMachine(
                launchpad.items.get(SLOT_BATTERY), launchpad.power, MAX_POWER);
        changed |= oldPower != launchpad.power;
        PowerNetworkManager.tickFromEndpoint(level, launchpad);

        if (!launchpad.hasRocketLoaded()) {
            boolean moving = false;
            for (int i = 0; i < ANIMATION_COUNT; i++) {
                if (!launchpad.finishedMoving(i)) {
                    moving = true;
                    break;
                }
            }
            if (!moving && launchpad.soyuzStatus != SoyuzStatus.ABSENT) {
                launchpad.soyuzStatus = SoyuzStatus.ABSENT;
                changed = true;
            }
            if (launchpad.loadedType != -1) {
                launchpad.loadedType = -1;
                changed = true;
            }
        } else {
            int type = SoyuzItem.skin(launchpad.items.get(SLOT_ROCKET));
            if (launchpad.loadedType != type) {
                launchpad.loadedType = type;
                changed = true;
            }
        }

        if (launchpad.needsPoweredStateTick() && launchpad.power >= CONSUMPTION) {
            changed |= launchpad.updateStates();
            boolean[] wasMoving = new boolean[INDEX_STRUT5 + 1];
            for (int i = 0; i <= INDEX_STRUT5; i++) {
                wasMoving[i] = !launchpad.finishedMoving(i);
            }
            changed |= launchpad.move();
            launchpad.power -= CONSUMPTION;
            changed = true;
            for (int i = 0; i <= INDEX_STRUT5; i++) {
                if (wasMoving[i] && launchpad.finishedMoving(i)) {
                    BlockPos soundPos = launchpad.strutSoundOrigin();
                    level.playSound(null, soundPos, HbmSoundEvents.DOOR_SLIDING_SEAL_STOP.get(),
                            SoundSource.BLOCKS, 35.0F, 0.75F);
                }
            }
        }

        if (changed) {
            launchpad.sync();
        } else if (level.getGameTime() % 10L == 0L) {
            launchpad.syncClientOnly();
        }
    }

    private void clientTick() {
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            this.previousPositions[i] = this.positions[i];
            if (this.interpolationTicks > 0) {
                this.positions[i] += (this.synchronizedPositions[i] - this.positions[i])
                        / this.interpolationTicks;
            } else {
                this.positions[i] = this.synchronizedPositions[i];
            }
        }
        if (this.interpolationTicks > 0) {
            this.interpolationTicks--;
        }
    }

    /** Exact latest-old component order, durations and launch countdown. */
    private boolean updateStates() {
        boolean changed = false;
        if (this.soyuzStatus == SoyuzStatus.ABSENT) {
            if (this.strutStatus == ComponentStatus.DEPLOY) {
                this.strutStatus = ComponentStatus.RETRACT;
                for (int i = 0; i <= INDEX_STRUT5; i++) {
                    setTarget(i, false, 60 + randomDuration(21));
                }
                changed = true;
            }
            if (this.carriageStatus == ComponentStatus.DEPLOY) {
                this.carriageStatus = ComponentStatus.RETRACT;
                setTarget(INDEX_CARRIAGE, false, 100);
                changed = true;
            }
            if (this.carriageStatus == ComponentStatus.RETRACT && finishedMoving(INDEX_CARRIAGE)) {
                if (wasMoving(INDEX_CARRIAGE)) {
                    setTarget(INDEX_TILT, true, 3);
                    changed = true;
                }
                if (this.target[INDEX_TILT] == 0.0F && this.rotorStatus == ComponentStatus.DEPLOY) {
                    this.rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100);
                    changed = true;
                }
            }
            if (this.target[INDEX_TILT] > 0.0F && finishedMoving(INDEX_TILT)) {
                setTarget(INDEX_TILT, false, 3);
                changed = true;
            }
            if (hasRocketLoaded()
                    && this.carriageStatus == ComponentStatus.RETRACT && finishedMoving(INDEX_CARRIAGE)
                    && this.rotorStatus == ComponentStatus.RETRACT && finishedMoving(INDEX_ROTOR)) {
                this.carriageStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_CARRIAGE, true, 200);
                this.soyuzStatus = SoyuzStatus.LOADING;
                return true;
            }
        }

        if (this.soyuzStatus == SoyuzStatus.LOADING) {
            if (this.rotorStatus == ComponentStatus.RETRACT && finishedMoving(INDEX_CARRIAGE)) {
                this.rotorStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_ROTOR, true, 200);
                changed = true;
            }
            if (this.carriageStatus == ComponentStatus.DEPLOY && finishedMoving(INDEX_CARRIAGE)
                    && this.rotorStatus == ComponentStatus.DEPLOY && finishedMoving(INDEX_ROTOR)) {
                if (this.strutStatus == ComponentStatus.RETRACT) {
                    this.strutStatus = ComponentStatus.DEPLOY;
                    for (int i = 0; i <= INDEX_STRUT5; i++) {
                        setTarget(i, true, 60 + randomDuration(21));
                    }
                    changed = true;
                } else {
                    boolean deployed = true;
                    for (int i = 0; i <= INDEX_STRUT5; i++) {
                        if (!finishedMoving(i)) {
                            deployed = false;
                            break;
                        }
                    }
                    if (deployed) {
                        this.fuelCountdown = FUEL_DURATION;
                        this.soyuzStatus = SoyuzStatus.FUELING;
                        return true;
                    }
                }
            }
        }

        if (this.soyuzStatus == SoyuzStatus.FUELING && hasAllFuel()) {
            if (this.fuelCountdown > 0) {
                this.fuelCountdown--;
                changed = true;
            } else {
                this.soyuzStatus = SoyuzStatus.READY;
                return true;
            }
        }

        if (this.soyuzStatus == SoyuzStatus.READY && !hasAllFuel()) {
            this.fuelCountdown = FUEL_DURATION;
            this.soyuzStatus = SoyuzStatus.FUELING;
            return true;
        }

        if (this.soyuzStatus == SoyuzStatus.LAUNCHING) {
            if (this.carriageStatus == ComponentStatus.DEPLOY) {
                this.carriageStatus = ComponentStatus.RETRACT;
                setTarget(INDEX_CARRIAGE, false, 100);
                changed = true;
            }
            if (this.carriageStatus == ComponentStatus.RETRACT && finishedMoving(INDEX_CARRIAGE)) {
                if (wasMoving(INDEX_CARRIAGE)) {
                    setTarget(INDEX_TILT, true, 3);
                    changed = true;
                }
                if (this.target[INDEX_TILT] == 0.0F && this.rotorStatus == ComponentStatus.DEPLOY) {
                    this.rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100);
                    changed = true;
                }
            }
            if (this.target[INDEX_TILT] > 0.0F && finishedMoving(INDEX_TILT)) {
                setTarget(INDEX_TILT, false, 3);
                changed = true;
            }
            if (this.countdown > 0) {
                this.countdown--;
                changed = true;
                if (this.countdown % 100 == 0 && this.countdown > 0 && this.level != null) {
                    this.level.playSound(null, this.worldPosition, HbmSoundEvents.HATCH_ALARM.get(),
                            SoundSource.BLOCKS, 100.0F, 1.1F);
                }
                if (this.countdown == 20 && this.level != null) {
                    this.level.playSound(null, this.worldPosition, HbmSoundEvents.SOYUZ_TAKEOFF.get(),
                            SoundSource.BLOCKS, 100.0F, 1.1F);
                }
            } else {
                if (canLaunch()) {
                    this.soyuzStatus = SoyuzStatus.ABSENT;
                    liftOff();
                } else {
                    this.soyuzStatus = SoyuzStatus.READY;
                }
                for (int i = 0; i <= INDEX_STRUT5; i++) {
                    setTarget(i, false, 18 + randomDuration(8));
                }
                changed = true;
            }
        }
        return changed;
    }

    private int randomDuration(int bound) {
        return this.level == null ? 0 : this.level.random.nextInt(bound);
    }

    private boolean needsPoweredStateTick() {
        if (hasRocketLoaded() || this.soyuzStatus != SoyuzStatus.ABSENT) {
            return true;
        }
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            if (!finishedMoving(i) || this.positions[i] != 0.0F || this.target[i] != 0.0F) {
                return true;
            }
        }
        return false;
    }

    public void setCargoMode(boolean cargoMode) {
        if (this.cargoMode != cargoMode) {
            this.cargoMode = cargoMode;
            sync();
        }
    }

    public void startLaunch() {
        if (this.soyuzStatus == SoyuzStatus.READY && canLaunch()) {
            this.soyuzStatus = SoyuzStatus.LAUNCHING;
            this.countdown = COUNTDOWN_DURATION;
            sync();
        }
    }

    public boolean hasRocketLoaded() {
        return this.items.get(SLOT_ROCKET).is(HbmItems.MISSILE_SOYUZ.get());
    }

    public boolean hasAllFuel() {
        return hasJetFuel() && hasOxidizer();
    }

    public boolean hasJetFuel() {
        return this.keroseneTank.amount() >= FUEL_REQUIRED;
    }

    public boolean hasOxidizer() {
        return this.oxygenTank.amount() >= FUEL_REQUIRED;
    }

    public boolean canLaunch() {
        if (this.loadedType < 0 || !hasAllFuel() || this.power < CONSUMPTION) {
            return false;
        }
        if (this.cargoMode) {
            if (!isReadyDesignator(this.items.get(SLOT_DESIGNATOR))) {
                return false;
            }
            for (int i = SLOT_CARGO_START; i < SLOT_COUNT; i++) {
                if (!this.items.get(i).isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        return orbitalState() != 1 && !this.items.get(SLOT_SATELLITE).isEmpty();
    }

    /** 0: not required, 1: required/missing, 2: required/present. */
    public int orbitalState() {
        if (this.cargoMode) {
            return 0;
        }
        ItemStack satellite = this.items.get(SLOT_SATELLITE);
        if (satellite.is(HbmItems.SAT_GERALD.get()) || satellite.is(HbmItems.SAT_LUNAR_MINER.get())
                || satellite.is(HbmItems.SATELLITE_MINER_LUNAR.get())) {
            return this.items.get(SLOT_ORBITAL_MODULE).is(HbmItems.MISSILE_SOYUZ_LANDER.get()) ? 2 : 1;
        }
        return 0;
    }

    private void setTarget(int index, boolean deploy, int duration) {
        this.target[index] = deploy ? 1.0F : 0.0F;
        this.speed[index] = 1.0F / duration;
    }

    private boolean move() {
        boolean changed = false;
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            this.previousPositions[i] = this.positions[i];
            if (Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
                this.positions[i] = this.target[i];
            } else if (this.positions[i] < this.target[i]) {
                this.positions[i] += this.speed[i];
            } else {
                this.positions[i] -= this.speed[i];
            }
            changed |= this.positions[i] != this.previousPositions[i];
        }
        return changed;
    }

    private boolean finishedMoving(int index) {
        return this.positions[index] == this.target[index];
    }

    private boolean wasMoving(int index) {
        return this.positions[index] != this.previousPositions[index];
    }

    public float renderPosition(int index, float partialTick) {
        return this.previousPositions[index]
                + (this.positions[index] - this.previousPositions[index]) * partialTick;
    }

    public int renderLoadedType() {
        return this.loadedType;
    }

    public SoyuzStatus renderStatus() {
        return this.soyuzStatus;
    }

    public boolean cargoMode() {
        return this.cargoMode;
    }

    public int countdown() {
        return this.countdown;
    }

    public long renderPower() {
        return this.power;
    }

    public BlockPos launchOrigin() {
        Direction facing = getBlockState().getValue(LargeMachineBlock.FACING);
        Direction rotated = facing.getClockWise();
        return this.worldPosition.relative(facing, -4).relative(rotated, -4).above(4);
    }

    private BlockPos strutSoundOrigin() {
        Direction facing = getBlockState().getValue(LargeMachineBlock.FACING);
        Direction rotated = facing.getClockWise();
        return this.worldPosition.relative(facing, -4).relative(rotated, 2).above(25);
    }

    private void liftOff() {
        if (this.level == null || this.level.isClientSide || !canLaunch()) {
            return;
        }
        BlockPos origin = launchOrigin();
        int targetX = this.worldPosition.getX();
        int targetZ = this.worldPosition.getZ();
        if (this.cargoMode) {
            CompoundTag tag = customData(this.items.get(SLOT_DESIGNATOR));
            targetX = tag.getInt("xCoord");
            targetZ = tag.getInt("zCoord");
        }

        SoyuzEntity soyuz = new SoyuzEntity(this.level, this.cargoMode ? 1 : 0,
                targetX, targetZ, this.loadedType);
        soyuz.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);

        boolean consumeOrbiter = orbitalState() == 2;
        if (this.cargoMode) {
            for (int i = SLOT_CARGO_START; i < SLOT_COUNT; i++) {
                soyuz.setPayload(i - SLOT_CARGO_START, this.items.get(i));
                this.items.set(i, ItemStack.EMPTY);
            }
        } else {
            soyuz.setPayload(0, this.items.get(SLOT_SATELLITE));
            this.items.set(SLOT_SATELLITE, ItemStack.EMPTY);
            if (consumeOrbiter) {
                this.items.set(SLOT_ORBITAL_MODULE, ItemStack.EMPTY);
            }
        }
        this.level.addFreshEntity(soyuz);
        this.keroseneTank.drain(kerosene(), FUEL_REQUIRED, false);
        this.oxygenTank.drain(oxygen(), FUEL_REQUIRED, false);
        this.items.set(SLOT_ROCKET, ItemStack.EMPTY);
        sync();
    }

    public HbmFluidDefinition keroseneFluid() {
        return kerosene();
    }

    public HbmFluidDefinition oxygenFluid() {
        return oxygen();
    }

    public int keroseneAmount() {
        return this.keroseneTank.amount();
    }

    public int oxygenAmount() {
        return this.oxygenTank.amount();
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsAutomationPort(queriedPos, side)) {
            return null;
        }
        return new LaunchpadFluidHandler();
    }

    public boolean isAutomationPort(BlockPos queriedPos) {
        if (queriedPos.equals(this.worldPosition)) {
            return true;
        }
        ensureServicePortCache();
        return this.servicePortHosts.contains(queriedPos);
    }

    public boolean allowsAutomationPort(BlockPos queriedPos, @Nullable Direction side) {
        if (queriedPos.equals(this.worldPosition)) {
            return true;
        }
        ensureServicePortCache();
        if (side == null) {
            return this.servicePortHosts.contains(queriedPos);
        }
        for (CapabilityPort port : this.servicePorts) {
            if (port.host().equals(queriedPos) && port.face() == side) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        ensureServicePortCache();
        return this.powerConnectorPositions;
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        ensureServicePortCache();
        for (CapabilityPort port : this.servicePorts) {
            if (port.connector().equals(connectorPos) && port.face() == machineSide) {
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
        return needsPoweredStateTick() ? Math.max(0L, MAX_POWER - this.power) : 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        if (receivedInput > 0L) {
            this.power = Math.min(MAX_POWER, this.power + receivedInput);
            setChanged();
        }
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.power.launchpad_soyuz", this.power, MAX_POWER);
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
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0 || this.items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            sync();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot == SLOT_ROCKET) {
            return stack.is(HbmItems.MISSILE_SOYUZ.get());
        }
        if (slot == SLOT_DESIGNATOR) {
            return isPotentialDesignator(stack);
        }
        if (slot == SLOT_SATELLITE) {
            return !this.cargoMode && SatelliteSavedData.SatelliteKind.fromItem(stack) != null;
        }
        if (slot == SLOT_ORBITAL_MODULE) {
            return !this.cargoMode && stack.is(HbmItems.MISSILE_SOYUZ_LANDER.get());
        }
        if (slot == SLOT_KEROSENE_IN) {
            return canDrainInto(stack, this.keroseneTank, kerosene());
        }
        if (slot == SLOT_OXYGEN_IN) {
            return canDrainInto(stack, this.oxygenTank, oxygen());
        }
        if (slot >= SLOT_CARGO_START) {
            return this.cargoMode
                    && !canPlaceItem(SLOT_ROCKET, stack)
                    && !canPlaceItem(SLOT_DESIGNATOR, stack);
        }
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return containsSlot(ACCESSIBLE_SLOTS, slot) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // TileEntityMachineBase#canExtractItem returned false in the latest
        // 1.7.10 implementation.  Hoppers may load the declared accessible
        // slots, but they must not pull the rocket or payload back out.
        return false;
    }

    private void ensureServicePortCache() {
        Direction facing = getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.EAST;
        if (this.worldPosition.equals(this.servicePortCacheOrigin)
                && facing == this.servicePortCacheFacing) {
            return;
        }

        ArrayList<CapabilityPort> ports = new ArrayList<>(AUTHORED_SERVICE_PORTS.size());
        LinkedHashSet<BlockPos> hosts = new LinkedHashSet<>();
        LinkedHashSet<BlockPos> connectors = new LinkedHashSet<>();
        for (AuthoredPort authored : AUTHORED_SERVICE_PORTS) {
            BlockPos host = this.worldPosition.offset(
                    SoyuzLaunchpadBlock.mapAuthoredOffset(facing, authored.offset())).immutable();
            Direction face = mapAuthoredPortFace(facing, authored.face());
            BlockPos connector = host.relative(face).immutable();
            ports.add(new CapabilityPort(host, connector, face));
            hosts.add(host);
            connectors.add(connector);
        }
        this.servicePortCacheOrigin = this.worldPosition.immutable();
        this.servicePortCacheFacing = facing;
        this.servicePorts = List.copyOf(ports);
        this.servicePortHosts = List.copyOf(hosts);
        this.powerConnectorPositions = List.copyOf(connectors);
    }

    private static List<AuthoredPort> authoredServicePorts() {
        ArrayList<AuthoredPort> ports = new ArrayList<>(18);
        for (int z = 3; z <= 7; z++) {
            ports.add(new AuthoredPort(new BlockPos(2, 0, z), Direction.WEST));
            ports.add(new AuthoredPort(new BlockPos(3, 0, z), Direction.EAST));
        }
        for (int x = 2; x <= 3; x++) {
            ports.add(new AuthoredPort(new BlockPos(x, 0, 3), Direction.NORTH));
            ports.add(new AuthoredPort(new BlockPos(x, 0, 7), Direction.SOUTH));
        }
        return List.copyOf(ports);
    }

    /** Dedicated face transform paired with SoyuzLaunchpadBlock#mapAuthoredOffset. */
    private static Direction mapAuthoredPortFace(Direction facing, Direction authoredFace) {
        return switch (facing) {
            case EAST -> authoredFace;
            case WEST -> switch (authoredFace) {
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case UP, DOWN -> authoredFace;
            };
            case SOUTH -> switch (authoredFace) {
                case EAST -> Direction.SOUTH;
                case WEST -> Direction.NORTH;
                case NORTH -> Direction.EAST;
                case SOUTH -> Direction.WEST;
                case UP, DOWN -> authoredFace;
            };
            case NORTH -> switch (authoredFace) {
                case EAST -> Direction.NORTH;
                case WEST -> Direction.SOUTH;
                case NORTH -> Direction.WEST;
                case SOUTH -> Direction.EAST;
                case UP, DOWN -> authoredFace;
            };
            case UP, DOWN -> throw new IllegalStateException(
                    "Soyuz launch pad facing is vertical: " + facing);
        };
    }

    private record AuthoredPort(BlockPos offset, Direction face) {
    }

    private record CapabilityPort(BlockPos host, BlockPos connector, Direction face) {
    }

    @Override
    public boolean stillValid(Player player) {
        if (MachineDummyMenuAccess.validDummyAccess(this, player)) {
            return true;
        }
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 50.0D * 50.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        sync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.launchpad_soyuz");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SoyuzLaunchpadMenu(id, inventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < this.items.size(); i++) {
            tag.put("Slot" + i, this.items.get(i).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.put("Kerosene", this.keroseneTank.save());
        tag.put("Oxygen", this.oxygenTank.save());
        tag.putBoolean("CargoMode", this.cargoMode);
        tag.putInt("LoadedType", this.loadedType);
        tag.putInt("FuelCountdown", this.fuelCountdown);
        tag.putInt("Countdown", this.countdown);
        tag.putInt("SoyuzStatus", this.soyuzStatus.ordinal());
        tag.putInt("StrutStatus", this.strutStatus.ordinal());
        tag.putInt("CarriageStatus", this.carriageStatus.ordinal());
        tag.putInt("RotorStatus", this.rotorStatus.ordinal());
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            tag.putFloat("Position" + i, this.positions[i]);
            tag.putFloat("Target" + i, this.target[i]);
            tag.putFloat("Speed" + i, this.speed[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.parseOptional(registries, tag.getCompound("Slot" + i)));
        }
        this.power = tag.getLong("Power");
        this.keroseneTank.load(tag.getCompound("Kerosene"));
        this.oxygenTank.load(tag.getCompound("Oxygen"));
        this.cargoMode = tag.getBoolean("CargoMode");
        this.loadedType = tag.contains("LoadedType") ? tag.getInt("LoadedType") : -1;
        this.fuelCountdown = tag.getInt("FuelCountdown");
        this.countdown = tag.getInt("Countdown");
        this.soyuzStatus = safeStatus(tag.getInt("SoyuzStatus"));
        this.strutStatus = safeComponent(tag.getInt("StrutStatus"));
        this.carriageStatus = safeComponent(tag.getInt("CarriageStatus"));
        this.rotorStatus = safeComponent(tag.getInt("RotorStatus"));
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            this.positions[i] = tag.getFloat("Position" + i);
            this.previousPositions[i] = this.positions[i];
            this.synchronizedPositions[i] = this.positions[i];
            this.target[i] = tag.getFloat("Target" + i);
            this.speed[i] = tag.getFloat("Speed" + i);
        }
        ensureTankTypes();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        float[] oldRendered = this.positions.clone();
        float[] previousSync = this.synchronizedPositions.clone();
        super.handleUpdateTag(tag, registries);
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            this.synchronizedPositions[i] = tag.getFloat("Position" + i);
            if (previousSync[i] != this.synchronizedPositions[i]) {
                this.interpolationTicks = 3;
                this.positions[i] = oldRendered[i];
                this.previousPositions[i] = oldRendered[i];
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private boolean tickContainers() {
        boolean changed = drainContainerIntoTank(
                SLOT_KEROSENE_IN, SLOT_KEROSENE_OUT, this.keroseneTank, kerosene());
        changed |= drainContainerIntoTank(
                SLOT_OXYGEN_IN, SLOT_OXYGEN_OUT, this.oxygenTank, oxygen());
        ensureTankTypes();
        return changed;
    }

    private boolean drainContainerIntoTank(int inputSlot, int outputSlot,
                                           HbmFluidTank tank, HbmFluidDefinition fluid) {
        ItemStack input = this.items.get(inputSlot);
        if (input.isEmpty()) {
            return false;
        }
        boolean moved = HbmFluidContainerTransfer.drainIntoTank(
                input,
                tank,
                candidate -> candidate == fluid,
                output -> canPlaceOutput(outputSlot, output),
                output -> placeOutput(outputSlot, output));
        if (moved && input.isEmpty()) {
            this.items.set(inputSlot, ItemStack.EMPTY);
        }
        return moved;
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(slot);
        return current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack)
                && current.getCount() + stack.getCount() <= current.getMaxStackSize();
    }

    private void placeOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
    }

    private void ensureTankTypes() {
        if (this.keroseneTank.amount() == 0 && this.keroseneTank.type().isNone()) {
            this.keroseneTank.setType(kerosene());
        }
        if (this.oxygenTank.amount() == 0 && this.oxygenTank.type().isNone()) {
            this.oxygenTank.setType(oxygen());
        }
    }

    private void sync() {
        setChanged();
        syncClientOnly();
    }

    private void syncClientOnly() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isPotentialDesignator(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof com.reinhardt.hbm.item.LegacyCoordinateDesignatorItem
                || stack.getItem() instanceof com.reinhardt.hbm.item.LegacyRangeDesignatorItem);
    }

    private static boolean isReadyDesignator(ItemStack stack) {
        if (!isPotentialDesignator(stack)) {
            return false;
        }
        CompoundTag tag = customData(stack);
        return tag.contains("xCoord") && tag.contains("zCoord");
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static boolean canDrainInto(ItemStack stack, HbmFluidTank tank, HbmFluidDefinition fluid) {
        return HbmFluidContainerTransfer.canDrainIntoTank(
                stack, tank, candidate -> candidate == fluid, output -> true);
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static boolean containsSlot(int[] slots, int slot) {
        for (int value : slots) {
            if (value == slot) {
                return true;
            }
        }
        return false;
    }

    private static SoyuzStatus safeStatus(int ordinal) {
        SoyuzStatus[] values = SoyuzStatus.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SoyuzStatus.ABSENT;
    }

    private static ComponentStatus safeComponent(int ordinal) {
        ComponentStatus[] values = ComponentStatus.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ComponentStatus.RETRACT;
    }

    private static HbmFluidDefinition kerosene() {
        return HbmFluids.byName("kerosene").orElseThrow();
    }

    private static HbmFluidDefinition oxygen() {
        return HbmFluids.byName("oxygen").orElseThrow();
    }

    public enum SoyuzStatus {
        ABSENT,
        LOADING,
        FUELING,
        READY,
        LAUNCHING
    }

    private enum ComponentStatus {
        DEPLOY,
        RETRACT
    }

    private final class LaunchpadFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> keroseneTank.getFluidInTank(0);
                case 1 -> oxygenTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 || tank == 1 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return tank == 0 && fluid == kerosene() || tank == 1 && fluid == oxygen();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            int filled = fluid == kerosene()
                    ? keroseneTank.fill(fluid, resource.getAmount(), action.simulate())
                    : fluid == oxygen()
                    ? oxygenTank.fill(fluid, resource.getAmount(), action.simulate()) : 0;
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
