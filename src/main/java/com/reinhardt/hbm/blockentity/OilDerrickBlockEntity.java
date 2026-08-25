package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.OilDerrickBlock;
import com.reinhardt.hbm.block.OilPumpjackBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.OilDerrickMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class OilDerrickBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int OIL_INPUT_SLOT = 1;
    public static final int OIL_OUTPUT_SLOT = 2;
    public static final int GAS_INPUT_SLOT = 3;
    public static final int GAS_OUTPUT_SLOT = 4;
    public static final int UPGRADE_START = 5;
    public static final int UPGRADE_END = 8;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 9;

    public static final long MAX_POWER = Kind.DERRICK.maxPower;
    public static final int CONSUMPTION = Kind.DERRICK.consumption;
    public static final int DELAY = Kind.DERRICK.delay;
    public static final int OIL_PER_DEPOSIT = Kind.DERRICK.oilPerDeposit;
    public static final int GAS_PER_DEPOSIT_MIN = Kind.DERRICK.gasPerDepositMin;
    public static final int GAS_PER_DEPOSIT_MAX = Kind.DERRICK.gasPerDepositMax;
    public static final double DRAIN_CHANCE = Kind.DERRICK.drainChance;
    public static final int TANK_CAPACITY = 64_000;

    private static final int PUSH_PER_PORT = 16_000;
    private static final int MAX_SUCK_NODES = 256;
    private static final int UNSET_DRILL_CURSOR = Integer.MIN_VALUE;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] INPUT_SLOTS = {0, 1, 3, 5, 6, 7};
    private static final int[] OUTPUT_SLOTS = {2, 4};

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final HbmFluidTank oilTank = new HbmFluidTank(oil(), TANK_CAPACITY);
    private final HbmFluidTank gasTank = new HbmFluidTank(gas(), TANK_CAPACITY);
    private final Set<BlockPos> processed = new HashSet<>();
    private long power;
    private long lastInput;
    private int indicator;
    private int drillCursorY = UNSET_DRILL_CURSOR;
    private float pumpjackRot;
    private float pumpjackPrevRot;
    private float pumpjackSpeed;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> OilDerrickBlockEntity.this.oilTank.type().oldId();
                case 1 -> OilDerrickBlockEntity.this.oilTank.amount();
                case 2 -> OilDerrickBlockEntity.this.oilTank.capacity();
                case 3 -> OilDerrickBlockEntity.this.gasTank.type().oldId();
                case 4 -> OilDerrickBlockEntity.this.gasTank.amount();
                case 5 -> OilDerrickBlockEntity.this.gasTank.capacity();
                case 6 -> (int) OilDerrickBlockEntity.this.power;
                case 7 -> OilDerrickBlockEntity.this.indicator;
                case 8 -> (int) OilDerrickBlockEntity.this.maxPower();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 6 -> OilDerrickBlockEntity.this.power = Math.max(0, Math.min(OilDerrickBlockEntity.this.maxPower(), value));
                case 7 -> OilDerrickBlockEntity.this.indicator = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OilDerrickBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.OIL_DERRICK.get(), pos, blockState);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OilDerrickBlockEntity derrick) {
        if (level.isClientSide) {
            derrick.tickClient();
            return;
        }
        derrick.tickServer(level);
    }

    public HbmFluidTank oilTank() {
        return this.oilTank;
    }

    public HbmFluidTank gasTank() {
        return this.gasTank;
    }

    public long power() {
        return this.power;
    }

    public long lastInput() {
        return this.lastInput;
    }

    public int indicator() {
        return this.indicator;
    }

    public Kind kind() {
        return this.getBlockState().is(HbmBlocks.MACHINE_PUMPJACK.get()) ? Kind.PUMPJACK : Kind.DERRICK;
    }

    public long maxPower() {
        return kind().maxPower();
    }

    public float pumpjackRotation(float partialTick) {
        return this.pumpjackPrevRot + (this.pumpjackRot - this.pumpjackPrevRot) * partialTick;
    }

    public void invalidateDrillCursor() {
        this.drillCursorY = UNSET_DRILL_CURSOR;
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new DerrickFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(kind(), this.worldPosition, facing());
    }

    public static List<Port> portsFor(Kind kind, BlockPos pos, Direction facing) {
        if (kind == Kind.PUMPJACK) {
            return pumpjackPorts(pos, facing);
        }
        return List.of(
                new Port(pos.relative(Direction.EAST), Direction.EAST),
                new Port(pos.relative(Direction.WEST), Direction.WEST),
                new Port(pos.relative(Direction.SOUTH), Direction.SOUTH),
                new Port(pos.relative(Direction.NORTH), Direction.NORTH)
        );
    }

    public static List<Port> pumpjackPorts(BlockPos pos, Direction facing) {
        Direction dir = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        Direction rot = dir.getCounterClockWise();
        return List.of(
                new Port(offset(pos, rot, 2, dir, 2), dir),
                new Port(offset(pos, rot, 2, dir, -2), dir.getOpposite()),
                new Port(offset(pos, rot, 4, dir, 2), dir),
                new Port(offset(pos, rot, 4, dir, -2), dir.getOpposite())
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(4);
        for (Port port : ports(level)) {
            connectors.add(port.pos().immutable());
        }
        return List.copyOf(connectors);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : ports(level)) {
            if (port.pos().equals(connectorPos) && port.face() == machineSide) {
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
        long maxPower = maxPower();
        return this.power >= maxPower ? 0L : maxPower - this.power;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(maxPower(), this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(String.format("%,d / %,d HE", this.power, maxPower()));
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
            case BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
            case OIL_INPUT_SLOT -> isFillableContainerFor(stack, oil());
            case GAS_INPUT_SLOT -> isFillableContainerFor(stack, gas());
            case UPGRADE_START, UPGRADE_START + 1, UPGRADE_START + 2 -> isValidUpgrade(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return contains(INPUT_SLOTS, slot) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return contains(OUTPUT_SLOTS, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        LargeMachineBlock.Footprint footprint = kind() == Kind.PUMPJACK
                ? OilPumpjackBlock.FOOTPRINT
                : OilDerrickBlock.FOOTPRINT;
        Direction facing = facing();
        for (BlockPos offset : footprint.offsets()) {
            BlockPos part = this.worldPosition.offset(LegacyMachineGeometry.rotate(
                    offset, facing, LargeMachineBlock.RotationBasis.MODERN_NORTH));
            if (player.distanceToSqr(
                    part.getX() + 0.5D,
                    part.getY() + 0.5D,
                    part.getZ() + 0.5D
            ) <= 64.0D) {
                return true;
            }
        }
        return false;
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
        return Component.translatable("container.reinhardtshbm." + kind().translationKey());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OilDerrickMenu(containerId, playerInventory, this, this.menuData);
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
        tag.put("OilTank", this.oilTank.save());
        tag.put("GasTank", this.gasTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Indicator", this.indicator);
        tag.putFloat("PumpjackSpeed", this.pumpjackSpeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.parseOptional(registries, tag.getCompound("Slot" + i));
        }
        this.oilTank.load(tag.getCompound("OilTank"));
        this.gasTank.load(tag.getCompound("GasTank"));
        this.power = Math.max(0L, Math.min(maxPower(), tag.getLong("Power")));
        this.lastInput = tag.getLong("LastInput");
        this.indicator = tag.getInt("Indicator");
        this.pumpjackSpeed = tag.getFloat("PumpjackSpeed");
        ensureTankTypes();
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
        ensureTankTypes();
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.power = BatteryPackItem.dischargeIntoMachine(this.items[BATTERY_SLOT], this.power, maxPower());
        tickContainers();
        tickAfterburner();
        pushTank(level, this.oilTank);
        pushTank(level, this.gasTank);
        operate(level);
        float previousSpeed = this.pumpjackSpeed;
        this.pumpjackSpeed = animationSpeed();
        setChanged();
        if (level.getGameTime() % 10L == 0L || Float.compare(previousSpeed, this.pumpjackSpeed) != 0) {
            sync();
        }
    }

    private void tickClient() {
        this.pumpjackPrevRot = this.pumpjackRot;
        if (kind() == Kind.PUMPJACK && this.pumpjackSpeed > 0.0F) {
            this.pumpjackRot += this.pumpjackSpeed;
        }
        if (this.pumpjackRot >= 360.0F) {
            this.pumpjackPrevRot -= 360.0F;
            this.pumpjackRot -= 360.0F;
        }
    }

    private void tickContainers() {
        boolean changed = false;
        changed |= fillContainerFromTank(OIL_INPUT_SLOT, OIL_OUTPUT_SLOT, this.oilTank, oil());
        changed |= fillContainerFromTank(GAS_INPUT_SLOT, GAS_OUTPUT_SLOT, this.gasTank, gas());
        if (changed) {
            sync();
        }
    }

    private boolean fillContainerFromTank(int inputSlot, int outputSlot, HbmFluidTank tank, HbmFluidDefinition defaultFluid) {
        ItemStack input = this.items[inputSlot];
        if (input.isEmpty()) {
            return false;
        }
        if (tank.amount() <= 0 && tank.type().isNone()) {
            tank.setType(defaultFluid);
        }
        boolean moved = HbmFluidContainerTransfer.fillFromTank(
                input,
                tank,
                output -> canPlaceOutput(outputSlot, output),
                output -> placeOutput(outputSlot, output)
        );
        if (!moved) {
            return false;
        }
        if (input.isEmpty()) {
            this.items[inputSlot] = ItemStack.EMPTY;
        }
        return true;
    }

    private void tickAfterburner() {
        int afterburnLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.AFTERBURN), 3);
        int toBurn = Math.min(this.gasTank.amount(), afterburnLevel * 10);
        if (toBurn <= 0) {
            return;
        }
        this.gasTank.drain(gas(), toBurn, false);
        this.power = Math.min(maxPower(), this.power + toBurn * 5L);
    }

    private void pushTank(Level level, HbmFluidTank tank) {
        if (tank.amount() <= 0 || tank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (tank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(tank.type(), Math.min(PUSH_PER_PORT, tank.amount()));
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.pos(),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                tank.drain(tank.type(), accepted, false);
                sync();
            }
        }
    }

    private void operate(Level level) {
        int required = powerReqEff();
        if (this.power >= required && this.oilTank.amount() < this.oilTank.capacity() && this.gasTank.amount() < this.gasTank.capacity()) {
            this.power -= required;
            if (level.getGameTime() % delayEff() == 0L) {
                this.indicator = 0;
                int minY = Math.min(drillDepth(level), this.worldPosition.getY() - 1);
                int y = nextDrillY(level, minY);
                if (y < minY) {
                    this.indicator = 1;
                    return;
                }
                if (!trySuck(level, y) && tryDrill(level, y)) {
                    this.drillCursorY = y - 1;
                }
            }
        } else {
            this.indicator = 2;
        }
    }

    private int powerReqEff() {
        int speedLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int energyLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3);
        int overLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) + 1;
        int consumption = kind().consumption();
        return (consumption + (consumption / 4 * speedLevel) - (consumption / 4 * energyLevel)) * overLevel;
    }

    private int delayEff() {
        int speedLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int energyLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3);
        int overLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) + 1;
        int delay = kind().delay();
        return Math.max((delay - (delay / 4 * speedLevel) + (delay / 10 * energyLevel)) / overLevel, 1);
    }

    private float animationSpeed() {
        if (kind() != Kind.PUMPJACK || this.indicator != 0) {
            return 0.0F;
        }
        int speedLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int overLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) + 1;
        return (5.0F + 2.0F * speedLevel) + (overLevel - 1.0F) * 10.0F;
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items[slot];
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return level;
    }

    private int drillDepth(Level level) {
        return level.getMinBuildHeight() + 5;
    }

    private int nextDrillY(Level level, int minY) {
        int topY = this.worldPosition.getY() - 1;
        if (this.drillCursorY == UNSET_DRILL_CURSOR || this.drillCursorY > topY || this.drillCursorY < minY - 1) {
            return scanNextDrillY(level, topY, minY);
        }
        if (this.drillCursorY == minY - 1) {
            return this.drillCursorY;
        }

        int y = this.drillCursorY;
        while (y >= minY && level.getBlockState(new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ())).is(HbmBlocks.OIL_PIPE.get())) {
            y--;
        }
        this.drillCursorY = y;
        return y;
    }

    private int scanNextDrillY(Level level, int topY, int minY) {
        for (int y = topY; y >= minY; y--) {
            BlockPos drillPos = new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ());
            if (!level.getBlockState(drillPos).is(HbmBlocks.OIL_PIPE.get())) {
                this.drillCursorY = y;
                return y;
            }
        }
        this.drillCursorY = minY - 1;
        return this.drillCursorY;
    }

    private boolean tryDrill(Level level, int y) {
        BlockPos pos = new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ());
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            level.setBlock(pos, HbmBlocks.OIL_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        if (state.getBlock().getExplosionResistance() < 1000.0F) {
            level.setBlock(pos, HbmBlocks.OIL_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
            return true;
        } else {
            this.indicator = 2;
            return false;
        }
    }

    private boolean trySuck(Level level, int y) {
        return trySuck(level, new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ()));
    }

    private boolean trySuck(Level level, BlockPos startPos) {
        BlockState startState = level.getBlockState(startPos);
        if (!canSuckBlock(startState)) {
            return false;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        this.processed.clear();
        queue.offer(startPos);
        this.processed.add(startPos);

        int nodesVisited = 0;
        while (!queue.isEmpty() && nodesVisited < MAX_SUCK_NODES) {
            BlockPos currentPos = queue.poll();
            nodesVisited++;
            BlockState currentState = level.getBlockState(currentPos);
            if (isOilDeposit(currentState)) {
                doSuck(level, currentPos, currentState);
                return true;
            }
            if (!isEmptyOilDeposit(currentState)) {
                continue;
            }

            List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
            Collections.shuffle(directions, new Random(level.random.nextLong()));
            for (Direction direction : directions) {
                BlockPos neighborPos = currentPos.relative(direction);
                if (!this.processed.contains(neighborPos) && canSuckBlock(level.getBlockState(neighborPos))) {
                    this.processed.add(neighborPos);
                    queue.offer(neighborPos);
                }
            }
        }
        return false;
    }

    private void doSuck(Level level, BlockPos pos, BlockState state) {
        Kind kind = kind();
        this.oilTank.fill(oil(), kind.oilPerDeposit(), false);
        int gasAmount = kind.gasPerDepositMin() + level.random.nextInt(kind.gasPerDepositMax() - kind.gasPerDepositMin() + 1);
        this.gasTank.fill(gas(), gasAmount, false);

        level.playSound(null, this.worldPosition, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 2.0F, 0.5F);

        if (level.random.nextDouble() < kind.drainChance()) {
            BlockState empty = state.is(HbmBlocks.ORE_DEEPSLATE_OIL.get())
                    ? HbmBlocks.ORE_DEEPSLATE_OIL_EMPTY.get().defaultBlockState()
                    : HbmBlocks.ORE_OIL_EMPTY.get().defaultBlockState();
            level.setBlock(pos, empty, Block.UPDATE_ALL);
        }
    }

    private boolean canSuckBlock(BlockState state) {
        return isOilDeposit(state) || isEmptyOilDeposit(state);
    }

    private boolean isOilDeposit(BlockState state) {
        return state.is(HbmBlocks.ORE_OIL.get()) || state.is(HbmBlocks.ORE_DEEPSLATE_OIL.get());
    }

    private boolean isEmptyOilDeposit(BlockState state) {
        return state.is(HbmBlocks.ORE_OIL_EMPTY.get()) || state.is(HbmBlocks.ORE_DEEPSLATE_OIL_EMPTY.get());
    }

    private boolean isFillableContainerFor(ItemStack stack, HbmFluidDefinition fluid) {
        if (stack.isEmpty() || fluid == null || fluid.isNone()) {
            return false;
        }
        return FluidUtil.getFluidHandler(stack.copyWithCount(1))
                .map(handler -> handler.fill(HbmFluids.toNeoStack(fluid, 1), IFluidHandler.FluidAction.SIMULATE) > 0)
                .orElse(false);
    }

    private boolean isValidUpgrade(ItemStack stack) {
        if (!MachineUpgradeItem.isMachineUpgrade(stack)) {
            return false;
        }
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED
                || type == MachineUpgradeItem.UpgradeType.POWER
                || type == MachineUpgradeItem.UpgradeType.AFTERBURN
                || type == MachineUpgradeItem.UpgradeType.OVERDRIVE;
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = this.items[slot];
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = this.items[slot];
        if (current.isEmpty()) {
            this.items[slot] = stack.copy();
        } else {
            current.grow(stack.getCount());
        }
    }

    private void ensureTankTypes() {
        if (this.oilTank.amount() == 0 && this.oilTank.type() != oil()) {
            this.oilTank.setType(oil());
        }
        if (this.gasTank.amount() == 0 && this.gasTank.type() != gas()) {
            this.gasTank.setType(gas());
        }
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        if (kind() == Kind.PUMPJACK) {
            return allowsPumpjackPort(queriedPos, side);
        }
        if (queriedPos.equals(this.worldPosition) && (side == null || side.getAxis().isHorizontal())) {
            return true;
        }
        for (Port port : ports(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private boolean allowsPumpjackPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : ports(this.level)) {
            if ((port.accessPos().equals(queriedPos) || port.pos().equals(queriedPos))
                    && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : ports(this.level)) {
                this.level.invalidateCapabilities(port.pos());
                this.level.invalidateCapabilities(port.accessPos());
            }
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    private static BlockPos offset(BlockPos pos, Direction first, int firstDistance, Direction second, int secondDistance) {
        return pos.offset(
                first.getStepX() * firstDistance + second.getStepX() * secondDistance,
                first.getStepY() * firstDistance + second.getStepY() * secondDistance,
                first.getStepZ() * firstDistance + second.getStepZ() * secondDistance
        );
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static boolean contains(int[] slots, int slot) {
        for (int value : slots) {
            if (value == slot) {
                return true;
            }
        }
        return false;
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

    private static HbmFluidDefinition oil() {
        return HbmFluids.byName("oil").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition gas() {
        return HbmFluids.byName("gas").orElse(HbmFluids.none());
    }

    public enum Kind {
        DERRICK(100_000L, 100, 50, 500, 100, 500, 0.05D, "machine_well"),
        PUMPJACK(250_000L, 200, 25, 750, 50, 250, 0.025D, "machine_pumpjack");

        private final long maxPower;
        private final int consumption;
        private final int delay;
        private final int oilPerDeposit;
        private final int gasPerDepositMin;
        private final int gasPerDepositMax;
        private final double drainChance;
        private final String translationKey;

        Kind(long maxPower, int consumption, int delay, int oilPerDeposit, int gasPerDepositMin, int gasPerDepositMax, double drainChance, String translationKey) {
            this.maxPower = maxPower;
            this.consumption = consumption;
            this.delay = delay;
            this.oilPerDeposit = oilPerDeposit;
            this.gasPerDepositMin = gasPerDepositMin;
            this.gasPerDepositMax = gasPerDepositMax;
            this.drainChance = drainChance;
            this.translationKey = translationKey;
        }

        public long maxPower() {
            return this.maxPower;
        }

        public int consumption() {
            return this.consumption;
        }

        public int delay() {
            return this.delay;
        }

        public int oilPerDeposit() {
            return this.oilPerDeposit;
        }

        public int gasPerDepositMin() {
            return this.gasPerDepositMin;
        }

        public int gasPerDepositMax() {
            return this.gasPerDepositMax;
        }

        public double drainChance() {
            return this.drainChance;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    public record Port(BlockPos pos, Direction face) {
        public BlockPos accessPos() {
            return this.pos.relative(this.face.getOpposite()).immutable();
        }
    }

    private final class DerrickFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private DerrickFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
                return oilTank.getFluidInTank(0);
            }
            if (tank == 1) {
                return gasTank.getFluidInTank(0);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> oilTank.capacity();
                case 1 -> gasTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid == oil()) {
                return drainTank(oilTank, oil(), resource.getAmount(), action);
            }
            if (fluid == gas()) {
                return drainTank(gasTank, gas(), resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            if (oilTank.amount() > 0) {
                return drainTank(oilTank, oil(), maxDrain, action);
            }
            if (gasTank.amount() > 0) {
                return drainTank(gasTank, gas(), maxDrain, action);
            }
            return FluidStack.EMPTY;
        }

        private FluidStack drainTank(HbmFluidTank tank, HbmFluidDefinition fluid, int amount, FluidAction action) {
            FluidStack drained = tank.drain(HbmFluids.toNeoStack(fluid, amount), action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }
}
