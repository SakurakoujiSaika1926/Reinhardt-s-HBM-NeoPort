package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LambdaLaunchpadBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.entity.LambdaRocketEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.menu.LambdaLaunchpadMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Literal modern implementation of TileEntityLaunchpadLambda. */
public final class LambdaLaunchpadBlockEntity extends BlockEntity
        implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_SATELLITE = 1;
    public static final int SLOT_GASOLINE_IN = 2;
    public static final int SLOT_GASOLINE_OUT = 3;
    public static final int SLOT_PEROXIDE_IN = 4;
    public static final int SLOT_PEROXIDE_OUT = 5;
    public static final int SLOT_BATTERY = 6;
    public static final int SLOT_COUNT = 7;
    public static final int DATA_COUNT = 9;

    public static final int INDEX_DOORS = 0;
    public static final int INDEX_ERECTOR = 1;
    public static final int INDEX_ROTOR = 2;
    public static final int INDEX_CLAMPS = 3;
    public static final int INDEX_PISTONS = 4;
    public static final int ANIMATION_COUNT = 5;

    public static final long MAX_POWER = 1_000_000L;
    public static final long CONSUMPTION = 10_000L;
    public static final int TANK_CAPACITY = 64_000;
    public static final int FUEL_REQUIRED = 24_000;
    public static final int COUNTDOWN_DURATION = 200;
    private static final int[] NO_AUTOMATION_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank gasolineTank = new HbmFluidTank(gasoline(), TANK_CAPACITY);
    private final HbmFluidTank peroxideTank = new HbmFluidTank(peroxide(), TANK_CAPACITY);
    private final float[] positions = new float[ANIMATION_COUNT];
    private final float[] previousPositions = new float[ANIMATION_COUNT];
    private final float[] target = new float[ANIMATION_COUNT];
    private final float[] speed = new float[ANIMATION_COUNT];
    private final float[] synchronizedPositions = new float[ANIMATION_COUNT];

    private long power;
    private boolean erected;
    private boolean erecting;
    private boolean autolaunch;
    private int animationDelay;
    private int countdown;
    private int interpolationTicks;
    @Nullable
    private BlockPos connectorCacheOrigin;
    @Nullable
    private Direction connectorCacheFacing;
    private List<Port> connectorPorts = List.of();
    private List<BlockPos> powerConnectorPositions = List.of();
    private List<BlockPos> capabilityHosts = List.of();

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) LambdaLaunchpadBlockEntity.this.power;
                case 1 -> LambdaLaunchpadBlockEntity.this.erected ? 1 : 0;
                case 2 -> LambdaLaunchpadBlockEntity.this.erecting ? 1 : 0;
                case 3 -> LambdaLaunchpadBlockEntity.this.autolaunch ? 1 : 0;
                case 4 -> LambdaLaunchpadBlockEntity.this.countdown;
                case 5 -> LambdaLaunchpadBlockEntity.this.gasolineTank.amount();
                case 6 -> LambdaLaunchpadBlockEntity.this.gasolineTank.capacity();
                case 7 -> LambdaLaunchpadBlockEntity.this.peroxideTank.amount();
                case 8 -> LambdaLaunchpadBlockEntity.this.peroxideTank.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> LambdaLaunchpadBlockEntity.this.power = value;
                case 1 -> LambdaLaunchpadBlockEntity.this.erected = value != 0;
                case 2 -> LambdaLaunchpadBlockEntity.this.erecting = value != 0;
                case 3 -> LambdaLaunchpadBlockEntity.this.autolaunch = value != 0;
                case 4 -> LambdaLaunchpadBlockEntity.this.countdown = value;
                case 5 -> LambdaLaunchpadBlockEntity.this.gasolineTank.setAmount(value);
                case 7 -> LambdaLaunchpadBlockEntity.this.peroxideTank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public LambdaLaunchpadBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LAMBDA_LAUNCHPAD.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            LambdaLaunchpadBlockEntity launchpad) {
        if (level.isClientSide) {
            launchpad.clientTick();
            com.reinhardt.hbm.client.sound.LambdaLaunchpadClientEffects.tick(launchpad);
            return;
        }

        boolean changed = launchpad.tickContainers();
        long oldPower = launchpad.power;
        launchpad.power = BatteryPackItem.dischargeIntoMachine(
                launchpad.items.get(SLOT_BATTERY), launchpad.power, MAX_POWER);
        changed |= launchpad.power != oldPower;
        PowerNetworkManager.tickFromEndpoint(level, launchpad);

        if (!launchpad.hasRocketLoaded()) {
            if (launchpad.erected || launchpad.erecting || launchpad.countdown != 0) {
                launchpad.erected = false;
                launchpad.erecting = false;
                launchpad.countdown = 0;
                changed = true;
            }
        }

        if (launchpad.power >= CONSUMPTION) {
            changed |= launchpad.updateStates();
            changed |= launchpad.move();
            launchpad.power -= CONSUMPTION;
            changed = true;
            if (launchpad.autolaunch && launchpad.erected
                    && launchpad.canLaunch() && launchpad.countdown <= 0) {
                launchpad.countdown = COUNTDOWN_DURATION;
                changed = true;
            }
        }

        if (changed) {
            launchpad.setChanged();
            if ((level.getGameTime() & 1L) == 0L || launchpad.countdown == 0) {
                launchpad.sync();
            }
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

    /** Exact state sequence from TileEntityLaunchpadLambda#updateStates. */
    private boolean updateStates() {
        boolean changed = false;
        if (finishedAllMoving() && this.erecting && !this.erected) {
            if (erectorDown() && doorsClosed()) {
                setTarget(INDEX_DOORS, 3.0F, 3.0F, 60);
                setTarget(INDEX_PISTONS, 0.0F, 1.0F, 1);
                setTarget(INDEX_CLAMPS, 0.0F, 1.0F, 1);
                setTarget(INDEX_ROTOR, 180.0F, 180.0F, 1);
            } else if (erectorDown() && doorsOpen()) {
                this.animationDelay = 20;
                setTarget(INDEX_ERECTOR, 27.0F, 27.0F, 100);
            } else if (erectorUp() && rotorDown()) {
                this.animationDelay = 20;
                setTarget(INDEX_ROTOR, 0.0F, 180.0F, 60);
                setTarget(INDEX_DOORS, 0.0F, 3.0F, 60);
            } else if (erectorUp() && rotorUp() && doorsClosed()) {
                this.animationDelay = 20;
                setTarget(INDEX_ERECTOR, 25.0F, 2.0F, 60);
            } else if (erectorCenter() && rotorUp() && doorsClosed()) {
                this.erected = true;
            } else {
                this.erecting = false;
            }
            changed = true;
        }

        if (this.erected && this.countdown <= 20 && this.countdown > 0) {
            if (this.erecting) {
                this.erecting = false;
                changed = true;
            }
        }

        if (finishedAllMoving() && !this.erecting) {
            setTarget(INDEX_DOORS, 0.0F, 3.0F, 60);
            if (clampsOn()) {
                setTarget(INDEX_PISTONS, 0.75F, 0.75F, 20);
            } else if (clampsOff() && clampsDown()) {
                this.animationDelay = 10;
                setTarget(INDEX_CLAMPS, 90.0F, 90.0F, 40);
            } else if (clampsUp() && !erectorDown()) {
                this.animationDelay = 20;
                setTarget(INDEX_ERECTOR, 0.0F, 25.0F, 100);
            } else if (erectorDown() && doorsClosed() && hasRocketLoaded()) {
                this.erecting = true;
            }
            changed = true;
        }

        if (this.erected && this.countdown > 0) {
            this.countdown--;
            changed = true;
            if (this.countdown <= 0) {
                if (this.level != null) {
                    this.level.playSound(null, this.worldPosition, HbmSoundEvents.SOYUZ_TAKEOFF.get(),
                            SoundSource.BLOCKS, 100.0F, 1.1F);
                }
                liftOff();
            }
        }
        return changed;
    }

    private boolean move() {
        boolean changed = false;
        if (this.animationDelay > 0) {
            this.animationDelay--;
            changed = true;
        }
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            this.previousPositions[i] = this.positions[i];
            if (this.animationDelay > 0) {
                continue;
            }
            float before = this.positions[i];
            if (Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
                this.positions[i] = this.target[i];
            } else if (this.positions[i] < this.target[i]) {
                this.positions[i] += this.speed[i];
            } else {
                this.positions[i] -= this.speed[i];
            }
            this.synchronizedPositions[i] = this.positions[i];
            changed |= Float.compare(before, this.positions[i]) != 0;
        }
        return changed;
    }

    private void setTarget(int index, float targetValue, float span, int duration) {
        this.target[index] = targetValue;
        float safeSpan = span <= 0.0F ? 1.0F : span;
        this.speed[index] = safeSpan / Math.max(1, duration);
    }

    public void setAutolaunch(boolean autolaunch) {
        if (this.autolaunch != autolaunch) {
            this.autolaunch = autolaunch;
            sync();
        }
    }

    public void startCountdown() {
        if (this.erected && canLaunch()) {
            this.countdown = COUNTDOWN_DURATION;
            sync();
        }
    }

    public boolean canLaunch() {
        if (!hasRocketLoaded() || !hasAllFuel() || this.power < CONSUMPTION) {
            return false;
        }
        ItemStack satellite = this.items.get(SLOT_SATELLITE);
        SatelliteSavedData.SatelliteKind kind = SatelliteSavedData.SatelliteKind.fromItem(satellite);
        return kind != null
                && kind != SatelliteSavedData.SatelliteKind.HORIZONS
                && kind != SatelliteSavedData.SatelliteKind.LUNAR_MINER;
    }

    public boolean hasRocketLoaded() {
        return this.items.get(SLOT_ROCKET).is(HbmItems.MISSILE_LAMBDA.get());
    }

    public boolean hasAllFuel() {
        return hasJetFuel() && hasOxidizer();
    }

    public boolean hasJetFuel() {
        return this.gasolineTank.amount() >= FUEL_REQUIRED;
    }

    public boolean hasOxidizer() {
        return this.peroxideTank.amount() >= FUEL_REQUIRED;
    }

    public boolean erected() {
        return this.erected;
    }

    public boolean erecting() {
        return this.erecting;
    }

    public boolean autolaunch() {
        return this.autolaunch;
    }

    public int countdown() {
        return this.countdown;
    }

    public long power() {
        return this.power;
    }

    public HbmFluidDefinition gasolineFluid() {
        return gasoline();
    }

    public HbmFluidDefinition peroxideFluid() {
        return peroxide();
    }

    public int gasolineAmount() {
        return this.gasolineTank.amount();
    }

    public int peroxideAmount() {
        return this.peroxideTank.amount();
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public float renderPosition(int index, float partialTick) {
        if (index < 0 || index >= ANIMATION_COUNT) {
            return 0.0F;
        }
        return this.previousPositions[index]
                + (this.positions[index] - this.previousPositions[index]) * partialTick;
    }

    private boolean doorsClosed() { return this.positions[INDEX_DOORS] == 0.0F; }
    private boolean doorsOpen() { return this.positions[INDEX_DOORS] == 3.0F; }
    private boolean erectorDown() { return this.positions[INDEX_ERECTOR] == 0.0F; }
    private boolean erectorCenter() { return this.positions[INDEX_ERECTOR] == 25.0F; }
    private boolean erectorUp() { return this.positions[INDEX_ERECTOR] == 27.0F; }
    private boolean rotorUp() { return this.positions[INDEX_ROTOR] == 0.0F; }
    private boolean rotorDown() { return this.positions[INDEX_ROTOR] == 180.0F; }
    private boolean clampsOn() { return this.positions[INDEX_PISTONS] == 0.0F; }
    private boolean clampsOff() { return this.positions[INDEX_PISTONS] == 0.75F; }
    private boolean clampsUp() { return this.positions[INDEX_CLAMPS] == 90.0F; }
    private boolean clampsDown() { return this.positions[INDEX_CLAMPS] == 0.0F; }

    private boolean finishedAllMoving() {
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            if (Float.compare(this.positions[i], this.target[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    private void liftOff() {
        if (this.level == null || this.level.isClientSide || !canLaunch()) {
            return;
        }
        LambdaRocketEntity rocket = new LambdaRocketEntity(this.level);
        rocket.setPos(this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 2.0D,
                this.worldPosition.getZ() + 0.5D);
        rocket.setPayload(this.items.get(SLOT_SATELLITE));
        this.level.addFreshEntity(rocket);
        this.gasolineTank.drain(gasoline(), FUEL_REQUIRED, false);
        this.peroxideTank.drain(peroxide(), FUEL_REQUIRED, false);
        this.items.set(SLOT_ROCKET, ItemStack.EMPTY);
        this.items.set(SLOT_SATELLITE, ItemStack.EMPTY);
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new LambdaFluidHandler(queriedPos.immutable(), side);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        ensureConnectorCache();
        return this.powerConnectorPositions;
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        ensureConnectorCache();
        for (Port port : this.connectorPorts) {
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
        return Math.max(0L, MAX_POWER - this.power);
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
        return Component.translatable(
                "message.reinhardtshbm.power.lambda_launchpad", this.power, MAX_POWER);
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
        return switch (slot) {
            case SLOT_ROCKET -> stack.is(HbmItems.MISSILE_LAMBDA.get());
            case SLOT_SATELLITE -> SatelliteSavedData.SatelliteKind.fromItem(stack) != null;
            case SLOT_GASOLINE_IN -> canDrainInto(stack, this.gasolineTank, gasoline());
            case SLOT_GASOLINE_OUT, SLOT_PEROXIDE_OUT -> false;
            case SLOT_PEROXIDE_IN -> canDrainInto(stack, this.peroxideTank, peroxide());
            case SLOT_BATTERY -> BatteryPackItem.isBattery(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
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
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        sync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.launchpad_lambda");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LambdaLaunchpadMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        stack.copy()));
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
        tag.putBoolean("Erected", this.erected);
        tag.putBoolean("Erecting", this.erecting);
        tag.putBoolean("Autolaunch", this.autolaunch);
        tag.putInt("AnimationDelay", this.animationDelay);
        tag.putInt("Countdown", this.countdown);
        tag.put("Gasoline", this.gasolineTank.save());
        tag.put("Peroxide", this.peroxideTank.save());
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
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.erected = tag.getBoolean("Erected");
        this.erecting = tag.getBoolean("Erecting");
        this.autolaunch = tag.getBoolean("Autolaunch");
        this.animationDelay = Math.max(0, tag.getInt("AnimationDelay"));
        this.countdown = Math.max(0, tag.getInt("Countdown"));
        this.gasolineTank.load(tag.getCompound("Gasoline"));
        this.peroxideTank.load(tag.getCompound("Peroxide"));
        ensureTankTypes();
        boolean clientUpdate = this.level != null && this.level.isClientSide;
        for (int i = 0; i < ANIMATION_COUNT; i++) {
            float next = tag.getFloat("Position" + i);
            this.target[i] = tag.getFloat("Target" + i);
            this.speed[i] = tag.getFloat("Speed" + i);
            this.synchronizedPositions[i] = next;
            if (!clientUpdate) {
                this.positions[i] = next;
                this.previousPositions[i] = next;
            }
        }
        if (clientUpdate) {
            this.interpolationTicks = 3;
        }
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

    private boolean tickContainers() {
        boolean changed = drainContainerIntoTank(
                SLOT_GASOLINE_IN, SLOT_GASOLINE_OUT, this.gasolineTank, gasoline());
        changed |= drainContainerIntoTank(
                SLOT_PEROXIDE_IN, SLOT_PEROXIDE_OUT, this.peroxideTank, peroxide());
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
                output -> placeOutput(outputSlot, output)
        );
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
        return current.isEmpty()
                || ItemStack.isSameItemSameComponents(current, stack)
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

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        if (side == null) {
            return queriedPos.equals(this.worldPosition);
        }
        ensureConnectorCache();
        for (Port port : this.connectorPorts) {
            if (port.host().equals(queriedPos) && port.face() == side) {
                return true;
            }
        }
        return false;
    }

    private void ensureConnectorCache() {
        Direction facing = getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
        if (this.worldPosition.equals(this.connectorCacheOrigin) && facing == this.connectorCacheFacing) {
            return;
        }
        ArrayList<Port> ports = new ArrayList<>(5);
        for (BlockPos host : LambdaLaunchpadBlock.portHosts(this.worldPosition, facing)) {
            ports.add(new Port(host, host.relative(facing).immutable(), facing));
        }
        this.connectorCacheOrigin = this.worldPosition.immutable();
        this.connectorCacheFacing = facing;
        this.connectorPorts = List.copyOf(ports);
        this.powerConnectorPositions = ports.stream().map(Port::connector).toList();
        this.capabilityHosts = ports.stream().map(Port::host).toList();
    }

    private void ensureTankTypes() {
        if (this.gasolineTank.amount() == 0 && this.gasolineTank.type().isNone()) {
            this.gasolineTank.setType(gasoline());
        }
        if (this.peroxideTank.amount() == 0 && this.peroxideTank.type().isNone()) {
            this.peroxideTank.setType(peroxide());
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(
                    this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            ensureConnectorCache();
            for (BlockPos host : this.capabilityHosts) {
                this.level.invalidateCapabilities(host);
            }
        }
    }

    private static boolean canDrainInto(
            ItemStack stack, HbmFluidTank tank, HbmFluidDefinition fluid) {
        return HbmFluidContainerTransfer.canDrainIntoTank(
                stack, tank, candidate -> candidate == fluid, output -> true);
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static HbmFluidDefinition gasoline() {
        return HbmFluids.byName("gasoline_leaded").orElseThrow();
    }

    private static HbmFluidDefinition peroxide() {
        return HbmFluids.byName("peroxide").orElseThrow();
    }

    private record Port(BlockPos host, BlockPos connector, Direction face) {
    }

    private final class LambdaFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private LambdaFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> gasolineTank.getFluidInTank(0);
                case 1 -> peroxideTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 || tank == 1 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (stack.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid())
                    .orElse(HbmFluids.none());
            return tank == 0 && fluid == gasoline() || tank == 1 && fluid == peroxide();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid())
                    .orElse(HbmFluids.none());
            int filled = fluid == gasoline()
                    ? gasolineTank.fill(fluid, resource.getAmount(), action.simulate())
                    : fluid == peroxide()
                    ? peroxideTank.fill(fluid, resource.getAmount(), action.simulate())
                    : 0;
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
