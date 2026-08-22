package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.BatteryReddBlock;
import com.reinhardt.hbm.block.EnergyCableBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.menu.BatteryReddMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;

public class BatteryReddBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;
    public static final long TRANSFER_LIMIT = Long.MAX_VALUE / 100L;
    private static final BigInteger BIG_TRANSFER_LIMIT = BigInteger.valueOf(TRANSFER_LIMIT);
    private static final int MODE_INPUT = 0;
    private static final int MODE_BUFFER = 1;
    private static final int MODE_OUTPUT = 2;
    private static final int MODE_NONE = 3;
    private static final int[] SLOTS = {SLOT_INPUT, SLOT_OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private BigInteger power = BigInteger.ZERO;
    private BigInteger delta = BigInteger.ZERO;
    private final BigInteger[] log = new BigInteger[20];
    private int redLow = MODE_INPUT;
    private int redHigh = MODE_OUTPUT;
    private PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.LOW;
    private long lastOutput;
    private long lastInput;
    public float prevRotation;
    public float rotation;

    public BatteryReddBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BATTERY_REDD.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        PowerNetworkManager.markDirty(this.level);
        for (Port port : ports(this.level)) {
            EnergyCableBlock.refreshConnections(this.level, port.connectorPos());
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatteryReddBlockEntity battery) {
        if (level.isClientSide) {
            battery.prevRotation = battery.rotation;
            battery.rotation += battery.speed();
            if (battery.rotation >= 360.0F) {
                battery.rotation -= 360.0F;
                battery.prevRotation -= 360.0F;
            }
            return;
        }

        BigInteger previous = battery.power;
        battery.dischargeBatterySlot();
        PowerNetworkManager.tickFromEndpoint(level, battery);
        battery.chargeBatterySlot();
        BigInteger average = battery.power.add(previous).divide(BigInteger.valueOf(2L));
        battery.delta = average.subtract(battery.log[0] == null ? BigInteger.ZERO : battery.log[0]);
        System.arraycopy(battery.log, 1, battery.log, 0, battery.log.length - 1);
        battery.log[battery.log.length - 1] = average;
        if (level.getGameTime() % 10L == 0L || !battery.delta.equals(BigInteger.ZERO)) {
            battery.sync();
        }
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return ports(level).stream().map(Port::connectorPos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return ports(level).stream().anyMatch(port -> port.connectorPos().equals(connectorPos) && port.face() == machineSide);
    }

    @Override
    public long getAvailableOutput() {
        int mode = relevantMode();
        if (mode != MODE_OUTPUT && mode != MODE_BUFFER) {
            return 0L;
        }
        // TileEntityBatteryREDD#getPower in 1.7.10 exposes at most half of its
        // connection speed to the provider side.
        return this.power.min(BIG_TRANSFER_LIMIT.divide(BigInteger.TWO)).longValue();
    }

    @Override
    public long getRequestedInput() {
        int mode = relevantMode();
        if (mode != MODE_INPUT && mode != MODE_BUFFER) {
            return 0L;
        }
        return TRANSFER_LIMIT;
    }

    @Override
    public PowerEndpoint.ConnectionPriority getPowerPriority() {
        return this.priority;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.lastOutput = usedOutput;
        this.lastInput = receivedInput;
        if (usedOutput > 0L) {
            this.power = this.power.subtract(BigInteger.valueOf(usedOutput)).max(BigInteger.ZERO);
        }
        if (receivedInput > 0L) {
            this.power = this.power.add(BigInteger.valueOf(receivedInput));
        }
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.battery_redd",
                this.power.toString(),
                this.delta.toString(),
                this.lastInput,
                this.lastOutput
        );
    }

    public BigInteger power() {
        return this.power;
    }

    public BigInteger delta() {
        return this.delta;
    }

    public int redLow() {
        return this.redLow;
    }

    public int redHigh() {
        return this.redHigh;
    }

    public PowerEndpoint.ConnectionPriority priority() {
        return this.priority;
    }

    public int relevantMode() {
        return hasPortRedstoneSignal() ? this.redHigh : this.redLow;
    }

    public void cycleLowMode() {
        this.redLow = (this.redLow + 1) & 3;
        markPowerTopologyDirty();
        sync();
    }

    public void cycleHighMode() {
        this.redHigh = (this.redHigh + 1) & 3;
        markPowerTopologyDirty();
        sync();
    }

    public void cyclePriority() {
        this.priority = switch (this.priority) {
            case LOW, LOWEST -> PowerEndpoint.ConnectionPriority.NORMAL;
            case NORMAL -> PowerEndpoint.ConnectionPriority.HIGH;
            case HIGH, HIGHEST -> PowerEndpoint.ConnectionPriority.LOW;
        };
        markPowerTopologyDirty();
        sync();
    }

    public float speed() {
        return (float) Math.min(Math.pow(Math.log(this.power.doubleValue() * 0.05D + 1.0D) * 0.05D, 5.0D), 15.0D);
    }

    private boolean hasPortRedstoneSignal() {
        if (this.level == null) {
            return false;
        }
        // TileEntityBatteryBase#getRelevantMode() in 1.7.10 reads the six
        // internal port blocks. The modern multiblock uses the same dummy block
        // for every internal part, so those parts must not act as redstone
        // neighbours of one another.
        for (Port port : ports(this.level)) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = port.proxyPos().relative(direction);
                if (isOwnMachinePart(neighbor)) {
                    continue;
                }
                if (this.level.getSignal(neighbor, direction.getOpposite()) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOwnMachinePart(BlockPos pos) {
        if (pos.equals(this.worldPosition)) {
            return true;
        }
        return this.level != null
                && this.level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && dummy.getCorePos().equals(this.worldPosition);
    }

    private void markPowerTopologyDirty() {
        if (this.level != null && !this.level.isClientSide) {
            PowerNetworkManager.markDirty(this.level);
        }
    }

    private void dischargeBatterySlot() {
        ItemStack stack = this.items.get(SLOT_INPUT);
        if (!BatteryPackItem.isBattery(stack)) {
            return;
        }
        long before = BatteryPackItem.charge(stack);
        long scratch = BatteryPackItem.dischargeIntoMachine(stack, 0L, TRANSFER_LIMIT);
        long moved = Math.max(0L, scratch);
        if (moved > 0L || BatteryPackItem.charge(stack) != before) {
            this.power = this.power.add(BigInteger.valueOf(moved));
            setChanged();
        }
    }

    private void chargeBatterySlot() {
        ItemStack stack = this.items.get(SLOT_OUTPUT);
        if (!BatteryPackItem.isBattery(stack) || this.power.signum() <= 0) {
            return;
        }
        long transferable = this.power.min(BIG_TRANSFER_LIMIT).longValue();
        long remaining = BatteryPackItem.chargeFromMachine(stack, transferable);
        long moved = transferable - remaining;
        if (moved > 0L) {
            this.power = this.power.subtract(BigInteger.valueOf(moved)).max(BigInteger.ZERO);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putByteArray("power", this.power.toByteArray());
        tag.putByteArray("delta", this.delta.toByteArray());
        tag.putInt("redLow", this.redLow);
        tag.putInt("redHigh", this.redHigh);
        tag.putInt("priority", this.priority.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.power = readBigInt(tag, "power");
        this.delta = readBigInt(tag, "delta");
        this.redLow = tag.contains("redLow") ? tag.getInt("redLow") : MODE_INPUT;
        this.redHigh = tag.contains("redHigh") ? tag.getInt("redHigh") : MODE_OUTPUT;
        int priorityIndex = tag.contains("priority") ? tag.getInt("priority") : PowerEndpoint.ConnectionPriority.LOW.ordinal();
        PowerEndpoint.ConnectionPriority[] values = PowerEndpoint.ConnectionPriority.VALUES;
        this.priority = values[Math.max(0, Math.min(priorityIndex, values.length - 1))];
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

    public void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == SLOT_OUTPUT && BatteryPackItem.isBattery(stack) && BatteryPackItem.charge(stack) >= BatteryPackItem.capacity(stack);
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
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }

        Direction facing = getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.NORTH;
        int radiusX = facing.getAxis() == Direction.Axis.Z ? 2 : 4;
        int radiusZ = facing.getAxis() == Direction.Axis.Z ? 4 : 2;
        AABB machineBounds = new AABB(
                this.worldPosition.getX() - radiusX,
                this.worldPosition.getY(),
                this.worldPosition.getZ() - radiusZ,
                this.worldPosition.getX() + radiusX + 1,
                this.worldPosition.getY() + 10,
                this.worldPosition.getZ() + radiusZ + 1
        );
        return machineBounds.inflate(8.0D).contains(player.position());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return BatteryPackItem.isBattery(stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.battery_redd");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BatteryReddMenu(containerId, playerInventory, this);
    }

    private static BigInteger readBigInt(CompoundTag tag, String key) {
        byte[] bytes = tag.getByteArray(key);
        return bytes.length == 0 ? BigInteger.ZERO : new BigInteger(bytes);
    }

    private List<Port> ports(LevelAccessor level) {
        Direction facing = this.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? this.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.NORTH;
        return portsFor(this.worldPosition, facing);
    }

    /**
     * Exact 1.7.10 TileEntityBatteryREDD#getConPos() layout, transformed to
     * this port's modern-NORTH model basis. The old port blocks are one cell
     * inside these connector positions.
     */
    private static List<Port> portsFor(BlockPos corePos, Direction facing) {
        return List.of(
                Port.fromLocal(corePos, facing, new BlockPos(3, 0, 2), Direction.EAST),
                Port.fromLocal(corePos, facing, new BlockPos(3, 0, -2), Direction.EAST),
                Port.fromLocal(corePos, facing, new BlockPos(-3, 0, 2), Direction.WEST),
                Port.fromLocal(corePos, facing, new BlockPos(-3, 0, -2), Direction.WEST),
                Port.fromLocal(corePos, facing, new BlockPos(0, 0, 5), Direction.SOUTH),
                Port.fromLocal(corePos, facing, new BlockPos(0, 0, -5), Direction.NORTH)
        );
    }

    private record Port(BlockPos proxyPos, BlockPos connectorPos, Direction face) {
        private static Port fromLocal(BlockPos corePos, Direction facing, BlockPos localConnector, Direction localFace) {
            BlockPos connector = corePos.offset(LegacyMachineGeometry.rotateModernNorth(localConnector, facing)).immutable();
            Direction face = LegacyMachineGeometry.rotateDirection(localFace, facing, LargeMachineBlock.RotationBasis.MODERN_NORTH);
            return new Port(connector.relative(face.getOpposite()).immutable(), connector, face);
        }
    }
}
