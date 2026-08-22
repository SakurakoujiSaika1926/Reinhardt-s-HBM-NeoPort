package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.menu.BatterySocketMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BatterySocketBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COUNT = 1;
    private static final int MODE_INPUT = 0;
    private static final int MODE_BUFFER = 1;
    private static final int MODE_OUTPUT = 2;
    private static final int MODE_NONE = 3;
    private static final int[] SLOTS = {SLOT_BATTERY};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final long[] log = new long[20];
    private long delta;
    private int redLow = MODE_INPUT;
    private int redHigh = MODE_OUTPUT;
    private PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.LOW;
    private long lastOutput;
    private long lastInput;

    public BatterySocketBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BATTERY_SOCKET.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatterySocketBlockEntity socket) {
        if (level.isClientSide) {
            return;
        }
        long previous = socket.power();
        socket.lastInput = 0L;
        socket.lastOutput = 0L;
        PowerNetworkManager.tickFromEndpoint(level, socket);
        long average = (socket.power() + previous) / 2L;
        socket.delta = average - socket.log[0];
        System.arraycopy(socket.log, 1, socket.log, 0, socket.log.length - 1);
        socket.log[socket.log.length - 1] = average;
        if (level.getGameTime() % 10L == 0L || socket.delta != 0L) {
            socket.sync();
        }
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        Direction dir = facing();
        Direction rot = dir.getClockWise();
        return List.of(
                this.worldPosition.relative(dir),
                this.worldPosition.relative(dir).relative(rot),
                this.worldPosition.relative(dir.getOpposite(), 2),
                this.worldPosition.relative(dir.getOpposite(), 2).relative(rot),
                this.worldPosition.relative(rot, 2),
                this.worldPosition.relative(rot, 2).relative(dir.getOpposite()),
                this.worldPosition.relative(rot.getOpposite()),
                this.worldPosition.relative(rot.getOpposite()).relative(dir.getOpposite())
        );
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    @Override
    public long getAvailableOutput() {
        int mode = relevantMode();
        ItemStack stack = batteryStack();
        if (mode != MODE_OUTPUT && mode != MODE_BUFFER || !BatteryPackItem.isBattery(stack)) {
            return 0L;
        }
        return Math.min(BatteryPackItem.charge(stack), BatteryPackItem.dischargeRate(stack));
    }

    @Override
    public long getRequestedInput() {
        int mode = relevantMode();
        ItemStack stack = batteryStack();
        if (mode != MODE_INPUT && mode != MODE_BUFFER || !BatteryPackItem.isBattery(stack)) {
            return 0L;
        }
        return Math.min(BatteryPackItem.capacity(stack) - BatteryPackItem.charge(stack), BatteryPackItem.chargeRate(stack));
    }

    @Override
    public PowerEndpoint.ConnectionPriority getPowerPriority() {
        return this.priority;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        ItemStack stack = batteryStack();
        if (!BatteryPackItem.isBattery(stack)) {
            return;
        }
        long power = BatteryPackItem.charge(stack);
        if (usedOutput > 0L) {
            power = Math.max(0L, power - usedOutput);
        }
        if (receivedInput > 0L) {
            power = Math.min(BatteryPackItem.capacity(stack), power + receivedInput);
        }
        this.lastOutput = usedOutput;
        this.lastInput = receivedInput;
        BatteryPackItem.setStoredCharge(stack, power);
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        ItemStack stack = batteryStack();
        return Component.translatable("message.reinhardtshbm.power.battery_socket",
                BatteryPackItem.charge(stack),
                BatteryPackItem.capacity(stack),
                this.delta,
                this.lastInput,
                this.lastOutput);
    }

    public long power() {
        return BatteryPackItem.charge(batteryStack());
    }

    public long capacity() {
        return BatteryPackItem.capacity(batteryStack());
    }

    public long delta() {
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

    public ItemStack batteryStack() {
        return this.items.get(SLOT_BATTERY);
    }

    public int relevantMode() {
        return hasPortRedstoneSignal() ? this.redHigh : this.redLow;
    }

    public void cycleLowMode() {
        this.redLow = (this.redLow + 1) & 3;
        sync();
    }

    public void cycleHighMode() {
        this.redHigh = (this.redHigh + 1) & 3;
        sync();
    }

    public void cyclePriority() {
        this.priority = switch (this.priority) {
            case LOW, LOWEST -> PowerEndpoint.ConnectionPriority.NORMAL;
            case NORMAL -> PowerEndpoint.ConnectionPriority.HIGH;
            case HIGH, HIGHEST -> PowerEndpoint.ConnectionPriority.LOW;
        };
        sync();
    }

    private boolean hasPortRedstoneSignal() {
        if (this.level == null) {
            return false;
        }
        for (BlockPos pos : getPowerConnectorPositions(this.level)) {
            if (this.level.hasNeighborSignal(pos)) {
                return true;
            }
        }
        return false;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putLong("delta", this.delta);
        tag.putInt("redLow", this.redLow);
        tag.putInt("redHigh", this.redHigh);
        tag.putInt("priority", this.priority.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.delta = tag.getLong("delta");
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
        return BatteryPackItem.isBattery(stack)
                && (BatteryPackItem.charge(stack) == 0L || BatteryPackItem.charge(stack) >= BatteryPackItem.capacity(stack));
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return this.items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return this.items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(this.items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (this.level != null && !this.level.isClientSide) {
            PowerNetworkManager.markDirty(this.level);
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return BatteryPackItem.isBattery(stack); }
    @Override public void clearContent() { this.items.set(SLOT_BATTERY, ItemStack.EMPTY); }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        ItemStack stack = batteryStack();
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
        clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.battery_socket");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BatterySocketMenu(containerId, playerInventory, this);
    }
}
