package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.integration.createdieselgenerators.HbmCreateDieselGeneratorsOilCompat;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.FrackingTowerMenu;
import com.reinhardt.hbm.oil.OilFieldSource;
import com.reinhardt.hbm.oil.ShallowOilTaskQueue;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import com.reinhardt.hbm.worldgen.OilFieldSurfaceEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.ChunkPos;
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
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class FrackingTowerBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int OIL_INPUT_SLOT = 1;
    public static final int OIL_OUTPUT_SLOT = 2;
    public static final int GAS_INPUT_SLOT = 3;
    public static final int GAS_OUTPUT_SLOT = 4;
    public static final int UPGRADE_START = 5;
    public static final int UPGRADE_END = 8;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 12;

    public static final long MAX_POWER = 5_000_000L;
    public static final int CONSUMPTION = 5_000;
    public static final int SOLUTION_REQUIRED = 10;
    public static final int DELAY = 20;
    public static final int OIL_PER_DEPOSIT = 1_000;
    public static final int GAS_PER_DEPOSIT_MIN = 100;
    public static final int GAS_PER_DEPOSIT_MAX = 500;
    public static final double DRAIN_CHANCE = 0.02D;
    public static final int OIL_PER_BEDROCK_DEPOSIT = 100;
    public static final int GAS_PER_BEDROCK_DEPOSIT_MIN = 10;
    public static final int GAS_PER_BEDROCK_DEPOSIT_MAX = 50;
    public static final int DESTRUCTION_RANGE = 75;
    public static final int TANK_CAPACITY = 64_000;

    private static final int PUSH_PER_PORT = 16_000;
    private static final int MAX_SUCK_NODES = 256;
    private static final long[] EMPTY_SHALLOW_OIL_TASKS = ShallowOilTaskQueue.EMPTY;
    private static final int FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS = 3;
    private static final long INFINITE_DEEP_OIL = Long.MAX_VALUE;
    private static final int UNSET_DRILL_CURSOR = Integer.MIN_VALUE;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] INPUT_SLOTS = {0, 1, 3, 5, 6, 7};
    private static final int[] OUTPUT_SLOTS = {2, 4};

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final HbmFluidTank oilTank = new HbmFluidTank(oil(), TANK_CAPACITY);
    private final HbmFluidTank gasTank = new HbmFluidTank(gas(), TANK_CAPACITY);
    private final HbmFluidTank fracksolTank = new HbmFluidTank(fracksol(), TANK_CAPACITY);
    private final Set<BlockPos> processed = new HashSet<>();
    private long power;
    private long lastInput;
    private int indicator;
    private int drillCursorY = UNSET_DRILL_CURSOR;
    private long[] shallowOilTasks = EMPTY_SHALLOW_OIL_TASKS;
    private int shallowOilTaskIndex;
    private OilFieldSource oilFieldSource = OilFieldSource.UNDETERMINED;
    private boolean createDieselGeneratorsPipeReady;
    private List<ChunkPos> createDieselGeneratorsOilChunks;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> FrackingTowerBlockEntity.this.oilTank.type().oldId();
                case 1 -> FrackingTowerBlockEntity.this.oilTank.amount();
                case 2 -> FrackingTowerBlockEntity.this.oilTank.capacity();
                case 3 -> FrackingTowerBlockEntity.this.gasTank.type().oldId();
                case 4 -> FrackingTowerBlockEntity.this.gasTank.amount();
                case 5 -> FrackingTowerBlockEntity.this.gasTank.capacity();
                case 6 -> FrackingTowerBlockEntity.this.fracksolTank.type().oldId();
                case 7 -> FrackingTowerBlockEntity.this.fracksolTank.amount();
                case 8 -> FrackingTowerBlockEntity.this.fracksolTank.capacity();
                case 9 -> (int) FrackingTowerBlockEntity.this.power;
                case 10 -> FrackingTowerBlockEntity.this.indicator;
                case 11 -> (int) FrackingTowerBlockEntity.this.maxPower();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 9 -> FrackingTowerBlockEntity.this.power = Math.max(0, Math.min(FrackingTowerBlockEntity.this.maxPower(), value));
                case 10 -> FrackingTowerBlockEntity.this.indicator = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FrackingTowerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FRACKING_TOWER.get(), pos, blockState);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FrackingTowerBlockEntity tower) {
        if (level.isClientSide) {
            return;
        }
        tower.tickServer(level);
    }

    public HbmFluidTank oilTank() {
        return this.oilTank;
    }

    public HbmFluidTank gasTank() {
        return this.gasTank;
    }

    public HbmFluidTank fracksolTank() {
        return this.fracksolTank;
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

    public long maxPower() {
        return MAX_POWER;
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new FrackingFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(this.worldPosition);
    }

    public static List<Port> portsFor(BlockPos pos) {
        return List.of(
                new Port(pos.relative(Direction.EAST), Direction.EAST),
                new Port(pos.relative(Direction.WEST), Direction.WEST),
                new Port(pos.relative(Direction.SOUTH), Direction.SOUTH),
                new Port(pos.relative(Direction.NORTH), Direction.NORTH)
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
        return this.power >= maxPower() ? 0L : maxPower() - this.power;
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
        return this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D
        ) <= 1024.0D;
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
        return Component.translatable("container.reinhardtshbm.machine_fracking_tower");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FrackingTowerMenu(containerId, playerInventory, this, this.menuData);
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
        tag.put("FracksolTank", this.fracksolTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Indicator", this.indicator);
        tag.putInt("DrillCursorY", this.drillCursorY);
        tag.putString("OilFieldSource", this.oilFieldSource.name());
        tag.putBoolean("CreateDieselGeneratorsPipeReady", this.createDieselGeneratorsPipeReady);
        if (hasShallowOilTasks()) {
            tag.putLongArray("ShallowOilTasks", this.shallowOilTasks);
            tag.putInt("ShallowOilTaskIndex", this.shallowOilTaskIndex);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.parseOptional(registries, tag.getCompound("Slot" + i));
        }
        this.oilTank.load(tag.getCompound("OilTank"));
        this.gasTank.load(tag.getCompound("GasTank"));
        this.fracksolTank.load(tag.getCompound("FracksolTank"));
        this.power = Math.max(0L, Math.min(maxPower(), tag.getLong("Power")));
        this.lastInput = tag.getLong("LastInput");
        this.indicator = tag.getInt("Indicator");
        this.drillCursorY = tag.contains("DrillCursorY") ? tag.getInt("DrillCursorY") : UNSET_DRILL_CURSOR;
        this.oilFieldSource = OilFieldSource.byName(tag.getString("OilFieldSource"));
        this.createDieselGeneratorsPipeReady = tag.getBoolean("CreateDieselGeneratorsPipeReady");
        loadShallowOilTasks(tag);
        ensureTankTypes();
    }

    private void loadShallowOilTasks(CompoundTag tag) {
        if (!tag.contains("ShallowOilTasks")) {
            clearShallowOilTasks();
            return;
        }
        this.shallowOilTasks = tag.getLongArray("ShallowOilTasks");
        this.shallowOilTaskIndex = Math.max(0, Math.min(this.shallowOilTasks.length, tag.getInt("ShallowOilTaskIndex")));
        if (!hasShallowOilTasks()) {
            clearShallowOilTasks();
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

    private void tickServer(Level level) {
        ensureTankTypes();
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.power = BatteryPackItem.dischargeIntoMachine(this.items[BATTERY_SLOT], this.power, maxPower());
        tickContainers();
        tickAfterburner();
        pushTank(level, this.oilTank);
        pushTank(level, this.gasTank);
        operate(level);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
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
            ensureOilFieldSource(level);
            this.power -= required;
            if (level.getGameTime() % delayEff() == 0L) {
                this.indicator = 0;
                if (this.oilFieldSource == OilFieldSource.CREATE_DIESEL_GENERATORS) {
                    if (!this.createDieselGeneratorsPipeReady && !advanceCreateDieselGeneratorsOilPipe(level)) {
                        return;
                    }
                    if (!pumpCreateDieselGeneratorsOil(level) && this.indicator == 0) {
                        if (switchToBedrockOilIfAvailable(level)) {
                            return;
                        }
                        this.indicator = 1;
                    }
                    return;
                }
                if (this.oilFieldSource == OilFieldSource.HBM_BEDROCK) {
                    pumpBedrockOil(level);
                    return;
                }
                int minY = Math.min(drillDepth(level), this.worldPosition.getY() - 1);
                int y = nextDrillY(level, minY);
                if (y < minY) {
                    if (switchToCreateDieselGeneratorsOilIfAvailable(level)
                            || switchToBedrockOilIfAvailable(level)) {
                        return;
                    }
                    this.indicator = 1;
                    return;
                }
                BlockPos drillPos = new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ());
                BlockState drillState = level.getBlockState(drillPos);
                if (isDeepDrillTarget(drillState)) {
                    if (switchToCreateDieselGeneratorsOilIfAvailable(level)
                            || switchToBedrockOilIfAvailable(level)) {
                        return;
                    }
                    this.indicator = 1;
                    return;
                }
                if (!trySuckShallow(level, drillPos) && tryDrill(level, y)) {
                    this.drillCursorY = y - 1;
                }
            }
        } else {
            this.indicator = 2;
        }
    }

    private void ensureOilFieldSource(Level level) {
        if (this.oilFieldSource != OilFieldSource.UNDETERMINED) {
            return;
        }
        this.oilFieldSource = detectOilFieldSource(level);
        if (this.oilFieldSource != OilFieldSource.CREATE_DIESEL_GENERATORS) {
            this.createDieselGeneratorsPipeReady = false;
        }
        setChanged();
        sync();
    }

    private OilFieldSource detectOilFieldSource(Level level) {
        if (hasShallowOilInDrillColumn(level)) {
            return OilFieldSource.HBM;
        }
        if (hasCreateDieselGeneratorsOil(level)) {
            return OilFieldSource.CREATE_DIESEL_GENERATORS;
        }
        if (hasBedrockOilInDrillColumn(level)) {
            return OilFieldSource.HBM_BEDROCK;
        }
        return OilFieldSource.HBM;
    }

    private boolean switchToCreateDieselGeneratorsOilIfAvailable(Level level) {
        if (!hasCreateDieselGeneratorsOil(level)) {
            return false;
        }
        this.oilFieldSource = OilFieldSource.CREATE_DIESEL_GENERATORS;
        this.createDieselGeneratorsPipeReady = false;
        clearShallowOilTasks();
        setChanged();
        sync();
        return true;
    }

    private boolean switchToBedrockOilIfAvailable(Level level) {
        if (!hasBedrockOilInDrillColumn(level)) {
            return false;
        }
        this.oilFieldSource = OilFieldSource.HBM_BEDROCK;
        this.createDieselGeneratorsPipeReady = false;
        clearShallowOilTasks();
        setChanged();
        sync();
        return true;
    }

    private boolean hasCreateDieselGeneratorsOil(Level level) {
        return HbmCreateDieselGeneratorsOilCompat.isLoaded()
                && level instanceof ServerLevel serverLevel
                && scanCreateDieselGeneratorsOil(serverLevel).hasOil();
    }

    private boolean advanceCreateDieselGeneratorsOilPipe(Level level) {
        int minY = Math.min(level.getMinBuildHeight(), this.worldPosition.getY() - 1);
        int y = nextDrillY(level, minY);
        if (y < minY) {
            this.indicator = 1;
            return false;
        }

        BlockPos pos = new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ());
        if (isDeepDrillTarget(level.getBlockState(pos))) {
            this.createDieselGeneratorsPipeReady = true;
            setChanged();
            sync();
            return true;
        }
        if (tryDrill(level, y)) {
            this.drillCursorY = y - 1;
        }
        return false;
    }

    private boolean hasShallowOilInDrillColumn(Level level) {
        int topY = this.worldPosition.getY() - 1;
        int minY = Math.min(drillDepth(level), topY);
        for (int y = topY; y >= minY; y--) {
            BlockPos pos = new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ());
            BlockState state = level.getBlockState(pos);
            if (isShallowOilDeposit(state) || (isShallowEmptyOilDeposit(state) && shallowNetworkHasOil(level, pos))) {
                return true;
            }
        }
        return false;
    }

    private boolean shallowNetworkHasOil(Level level, BlockPos startPos) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        this.processed.clear();
        queue.offer(startPos);
        this.processed.add(startPos);

        int nodesVisited = 0;
        while (!queue.isEmpty() && nodesVisited < MAX_SUCK_NODES) {
            BlockPos currentPos = queue.poll();
            nodesVisited++;
            BlockState currentState = level.getBlockState(currentPos);
            if (isShallowOilDeposit(currentState)) {
                return true;
            }
            if (!isShallowEmptyOilDeposit(currentState)) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(direction);
                if (!this.processed.contains(neighborPos) && isShallowSearchBlock(level.getBlockState(neighborPos))) {
                    this.processed.add(neighborPos);
                    queue.offer(neighborPos);
                }
            }
        }
        return false;
    }

    private boolean hasBedrockOilInDrillColumn(Level level) {
        int topY = this.worldPosition.getY() - 1;
        int minY = Math.min(drillDepth(level), topY);
        for (int y = topY; y >= minY; y--) {
            BlockState state = level.getBlockState(new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ()));
            if (isBedrockOilDeposit(state)) {
                return true;
            }
        }
        return false;
    }

    private boolean pumpCreateDieselGeneratorsOil(Level level) {
        if (!this.createDieselGeneratorsPipeReady || !(level instanceof ServerLevel serverLevel) || !canPump()) {
            return false;
        }

        int acceptedOil = this.oilTank.fill(oil(), OIL_PER_DEPOSIT, true);
        if (acceptedOil <= 0) {
            return false;
        }

        DeepOilSnapshot snapshot = scanCreateDieselGeneratorsOil(serverLevel);
        if (!snapshot.hasOil()) {
            return false;
        }
        if (!snapshot.infinite()) {
            acceptedOil = drainCreateDieselGeneratorsOil(
                    serverLevel,
                    snapshot,
                    (int) Math.min(acceptedOil, snapshot.totalOil())
            );
            if (acceptedOil <= 0) {
                return false;
            }
        }
        this.oilTank.fill(oil(), acceptedOil, false);
        int gasAmount = GAS_PER_DEPOSIT_MIN + level.random.nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1);
        this.gasTank.fill(gas(), gasAmount, false);
        this.fracksolTank.drain(fracksol(), SOLUTION_REQUIRED, false);
        OilFieldSurfaceEffects.generateRuntimeOilSpot(level, level.random, this.worldPosition.getX(), this.worldPosition.getZ(), DESTRUCTION_RANGE, 10, false);
        level.playSound(null, this.worldPosition, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 2.0F, 0.5F);
        sync();
        return true;
    }

    private DeepOilSnapshot scanCreateDieselGeneratorsOil(ServerLevel level) {
        List<DeepOilChunk> availableChunks = new ArrayList<>();
        long total = 0L;
        for (ChunkPos chunk : createDieselGeneratorsOilChunks()) {
            int amount = HbmCreateDieselGeneratorsOilCompat.materializeChunkOilAmount(level, chunk);
            if (amount == Integer.MAX_VALUE) {
                return new DeepOilSnapshot(List.of(), INFINITE_DEEP_OIL, true);
            }
            if (amount > 0) {
                availableChunks.add(new DeepOilChunk(chunk, amount));
                total += amount;
            }
        }
        return new DeepOilSnapshot(availableChunks, total, false);
    }

    private int drainCreateDieselGeneratorsOil(ServerLevel level, DeepOilSnapshot snapshot, int amount) {
        int remaining = amount;
        for (DeepOilChunk chunk : snapshot.chunks()) {
            int drained = Math.min(chunk.amount(), remaining);
            if (!HbmCreateDieselGeneratorsOilCompat.setChunkOilAmount(level, chunk.pos(), chunk.amount() - drained)) {
                continue;
            }
            remaining -= drained;
            if (remaining <= 0) {
                return amount;
            }
        }
        return amount - remaining;
    }

    private List<ChunkPos> createDieselGeneratorsOilChunks() {
        if (this.createDieselGeneratorsOilChunks != null) {
            return this.createDieselGeneratorsOilChunks;
        }
        ChunkPos center = new ChunkPos(this.worldPosition);
        List<ChunkPos> chunks = new ArrayList<>(1 + 2 * FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS * (FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS + 1));
        for (int radius = 0; radius <= FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS; radius++) {
            for (int chunkX = -radius; chunkX <= radius; chunkX++) {
                for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
                    if (Math.abs(chunkX) + Math.abs(chunkZ) != radius) {
                        continue;
                    }
                    chunks.add(new ChunkPos(center.x + chunkX, center.z + chunkZ));
                }
            }
        }
        this.createDieselGeneratorsOilChunks = List.copyOf(chunks);
        return this.createDieselGeneratorsOilChunks;
    }

    private record DeepOilChunk(ChunkPos pos, int amount) {
    }

    private record DeepOilSnapshot(List<DeepOilChunk> chunks, long totalOil, boolean infinite) {
        private boolean hasOil() {
            return this.infinite || this.totalOil > 0L;
        }
    }

    private int powerReqEff() {
        int speedLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int energyLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3);
        int overLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) + 1;
        return (CONSUMPTION + (CONSUMPTION / 4 * speedLevel) - (CONSUMPTION / 4 * energyLevel)) * overLevel;
    }

    private int delayEff() {
        int speedLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int energyLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3);
        int overLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) + 1;
        return Math.max((DELAY - (DELAY / 4 * speedLevel) + (DELAY / 10 * energyLevel)) / overLevel, 1);
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
        return level.getMinBuildHeight();
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
        }
        this.indicator = 2;
        return false;
    }

    private boolean pumpBedrockOil(Level level) {
        int minY = Math.min(drillDepth(level), this.worldPosition.getY() - 1);
        int y = nextDrillY(level, minY);
        if (y < minY) {
            this.indicator = 1;
            return false;
        }

        BlockPos pos = new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ());
        BlockState state = level.getBlockState(pos);
        if (isBedrockOilDeposit(state)) {
            if (!canPump()) {
                return false;
            }
            doSuck(level, pos, state);
            return true;
        }
        if (HbmCreateDieselGeneratorsOilCompat.isBedrock(state)) {
            this.indicator = 1;
            return false;
        }
        if (tryDrill(level, y)) {
            this.drillCursorY = y - 1;
        }
        return false;
    }

    private boolean trySuckShallow(Level level, BlockPos startPos) {
        if (!hasShallowOilTasks()) {
            BlockState startState = level.getBlockState(startPos);
            if (!isShallowSearchBlock(startState)) {
                return false;
            }
            rebuildShallowOilTasks(level, startPos);
            if (!hasShallowOilTasks()) {
                return false;
            }
        }
        if (!canPump()) {
            return true;
        }
        return consumeShallowOilTask(level);
    }

    private boolean canPump() {
        boolean hasSolution = this.fracksolTank.amount() >= SOLUTION_REQUIRED;
        if (!hasSolution) {
            this.indicator = 3;
        }
        return hasSolution;
    }

    private void rebuildShallowOilTasks(Level level, BlockPos startPos) {
        this.shallowOilTasks = ShallowOilTaskQueue.build(
                level,
                startPos,
                MAX_SUCK_NODES,
                this::isShallowSearchBlock,
                this::isShallowOilDeposit
        );
        this.shallowOilTaskIndex = 0;
        setChanged();
    }

    private boolean consumeShallowOilTask(Level level) {
        if (!hasShallowOilTasks()) {
            return false;
        }

        BlockPos pos = BlockPos.of(this.shallowOilTasks[this.shallowOilTaskIndex]);
        if (!level.isLoaded(pos)) {
            advanceShallowOilTask();
            return true;
        }

        BlockState state = level.getBlockState(pos);
        if (!isShallowOilDeposit(state)) {
            advanceShallowOilTask();
            return true;
        }

        doSuck(level, pos, state);
        if (!isShallowOilDeposit(level.getBlockState(pos))) {
            advanceShallowOilTask();
        }
        return true;
    }

    private boolean hasShallowOilTasks() {
        return this.shallowOilTaskIndex >= 0 && this.shallowOilTaskIndex < this.shallowOilTasks.length;
    }

    private void advanceShallowOilTask() {
        this.shallowOilTaskIndex++;
        if (!hasShallowOilTasks()) {
            clearShallowOilTasks();
        }
        setChanged();
    }

    private void clearShallowOilTasks() {
        this.shallowOilTasks = EMPTY_SHALLOW_OIL_TASKS;
        this.shallowOilTaskIndex = 0;
    }

    private void doSuck(Level level, BlockPos pos, BlockState state) {
        if (state.is(HbmBlocks.ORE_BEDROCK_OIL.get())) {
            this.oilTank.fill(oil(), OIL_PER_BEDROCK_DEPOSIT, false);
            int gasAmount = GAS_PER_BEDROCK_DEPOSIT_MIN + level.random.nextInt(GAS_PER_BEDROCK_DEPOSIT_MAX - GAS_PER_BEDROCK_DEPOSIT_MIN + 1);
            this.gasTank.fill(gas(), gasAmount, false);
        } else {
            this.oilTank.fill(oil(), OIL_PER_DEPOSIT, false);
            int gasAmount = GAS_PER_DEPOSIT_MIN + level.random.nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1);
            this.gasTank.fill(gas(), gasAmount, false);

            if (level.random.nextDouble() < DRAIN_CHANCE) {
                BlockState empty = state.is(HbmBlocks.ORE_DEEPSLATE_OIL.get())
                        ? HbmBlocks.ORE_DEEPSLATE_OIL_EMPTY.get().defaultBlockState()
                        : HbmBlocks.ORE_OIL_EMPTY.get().defaultBlockState();
                level.setBlock(pos, empty, Block.UPDATE_ALL);
            }
        }

        this.fracksolTank.drain(fracksol(), SOLUTION_REQUIRED, false);
        OilFieldSurfaceEffects.generateRuntimeOilSpot(level, level.random, this.worldPosition.getX(), this.worldPosition.getZ(), DESTRUCTION_RANGE, 10, false);
        level.playSound(null, this.worldPosition, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 2.0F, 0.5F);
        sync();
    }

    private boolean isDeepDrillTarget(BlockState state) {
        return HbmCreateDieselGeneratorsOilCompat.isBedrock(state) || isBedrockOilDeposit(state);
    }

    private boolean isShallowSearchBlock(BlockState state) {
        return isShallowOilDeposit(state) || isShallowEmptyOilDeposit(state);
    }

    private boolean isShallowOilDeposit(BlockState state) {
        return state.is(HbmBlocks.ORE_OIL.get()) || state.is(HbmBlocks.ORE_DEEPSLATE_OIL.get());
    }

    private boolean isShallowEmptyOilDeposit(BlockState state) {
        return state.is(HbmBlocks.ORE_OIL_EMPTY.get()) || state.is(HbmBlocks.ORE_DEEPSLATE_OIL_EMPTY.get());
    }

    private boolean isBedrockOilDeposit(BlockState state) {
        return state.is(HbmBlocks.ORE_BEDROCK_OIL.get());
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
        if (this.fracksolTank.amount() == 0 && this.fracksolTank.type() != fracksol()) {
            this.fracksolTank.setType(fracksol());
        }
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
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

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : ports(this.level)) {
                this.level.invalidateCapabilities(port.pos());
            }
        }
    }

    @SuppressWarnings("unused")
    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
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

    private static HbmFluidDefinition fracksol() {
        return HbmFluids.byName("fracksol").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
    }

    private final class FrackingFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private FrackingFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> oilTank.getFluidInTank(0);
                case 1 -> gasTank.getFluidInTank(0);
                case 2 -> fracksolTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> oilTank.capacity();
                case 1 -> gasTank.capacity();
                case 2 -> fracksolTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 2 || stack.isEmpty()) {
                return false;
            }
            return HbmFluids.fromNeoFluid(stack.getFluid()).filter(definition -> definition == fracksol()).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != fracksol()) {
                return 0;
            }
            int accepted = fracksolTank.fill(fracksol(), resource.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) {
                sync();
            }
            return accepted;
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
