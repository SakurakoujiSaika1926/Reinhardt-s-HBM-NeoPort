package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.entity.SoyuzEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.SoyuzItem;
import com.reinhardt.hbm.menu.SoyuzLauncherMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SoyuzLauncherBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
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

    public static final long MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 128_000;
    public static final int MAX_COUNTDOWN = 600;

    private static final int[] AUTOMATION_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank keroseneTank = new HbmFluidTank(kerosene(), TANK_CAPACITY);
    private final HbmFluidTank oxygenTank = new HbmFluidTank(oxygen(), TANK_CAPACITY);
    private long power;
    private byte mode;
    private boolean starting;
    private int countdown = MAX_COUNTDOWN;
    private byte rocketType = -1;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) SoyuzLauncherBlockEntity.this.power;
                case 1 -> SoyuzLauncherBlockEntity.this.mode;
                case 2 -> SoyuzLauncherBlockEntity.this.starting ? 1 : 0;
                case 3 -> SoyuzLauncherBlockEntity.this.countdown;
                case 4 -> SoyuzLauncherBlockEntity.this.rocketType;
                case 5 -> SoyuzLauncherBlockEntity.this.keroseneTank.amount();
                case 6 -> SoyuzLauncherBlockEntity.this.keroseneTank.capacity();
                case 7 -> SoyuzLauncherBlockEntity.this.oxygenTank.amount();
                case 8 -> SoyuzLauncherBlockEntity.this.oxygenTank.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> SoyuzLauncherBlockEntity.this.power = value;
                case 1 -> SoyuzLauncherBlockEntity.this.mode = (byte) value;
                case 2 -> SoyuzLauncherBlockEntity.this.starting = value != 0;
                case 3 -> SoyuzLauncherBlockEntity.this.countdown = value;
                case 4 -> SoyuzLauncherBlockEntity.this.rocketType = (byte) value;
                case 5 -> SoyuzLauncherBlockEntity.this.keroseneTank.setAmount(value);
                case 7 -> SoyuzLauncherBlockEntity.this.oxygenTank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SoyuzLauncherBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SOYUZ_LAUNCHER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SoyuzLauncherBlockEntity blockEntity) {
        if (level.isClientSide) {
            com.reinhardt.hbm.client.sound.SoyuzLauncherClientEffects.tick(blockEntity);
            return;
        }

        boolean changed = blockEntity.tickContainers();
        long oldPower = blockEntity.power;
        blockEntity.power = BatteryPackItem.dischargeIntoMachine(blockEntity.items.get(SLOT_BATTERY), blockEntity.power, MAX_POWER);
        changed |= blockEntity.power != oldPower;
        PowerNetworkManager.tickFromEndpoint(level, blockEntity);

        blockEntity.rocketType = blockEntity.getRocketType();
        if (!blockEntity.starting || !blockEntity.canLaunch()) {
            if (blockEntity.starting || blockEntity.countdown != MAX_COUNTDOWN) {
                changed = true;
            }
            blockEntity.countdown = MAX_COUNTDOWN;
            blockEntity.starting = false;
        } else if (blockEntity.countdown > 0) {
            blockEntity.countdown--;
            changed = true;
            if (blockEntity.countdown % 100 == 0 && blockEntity.countdown > 0) {
                level.playSound(null, pos, HbmSoundEvents.HATCH_ALARM.get(), SoundSource.BLOCKS, 100.0F, 1.1F);
            }
        } else {
            blockEntity.liftOff();
            changed = true;
        }

        if (changed || level.getGameTime() % 10L == 0L) {
            blockEntity.sync();
        }
    }

    public void setMode(int mode) {
        this.mode = (byte) (mode == 1 ? 1 : 0);
        sync();
    }

    public void startCountdown() {
        if (canLaunch()) {
            this.starting = true;
            sync();
        }
    }

    public boolean canLaunch() {
        // TileEntitySoyuzLauncher.canLaunch checks hasRocket twice and omits hasOxy.
        // Keep that legacy launch predicate; the oxygen gauge remains independent.
        return hasRocket() && hasFuel() && hasPower() && designatorState() != 1 && orbitalState() != 1 && satelliteState() != 1;
    }

    public boolean hasFuel() {
        return this.keroseneTank.amount() >= getFuelRequired();
    }

    public boolean hasOxygen() {
        return this.oxygenTank.amount() >= getFuelRequired();
    }

    public boolean hasPower() {
        return this.power >= getPowerRequired();
    }

    public boolean hasRocket() {
        return this.items.get(SLOT_ROCKET).is(HbmItems.MISSILE_SOYUZ.get());
    }

    public int designatorState() {
        if (this.mode == 0) {
            return 0;
        }
        return isReadyDesignator(this.items.get(SLOT_DESIGNATOR)) ? 2 : 1;
    }

    public int satelliteState() {
        if (this.mode == 1) {
            return 0;
        }
        return this.items.get(SLOT_SATELLITE).isEmpty() ? 1 : 2;
    }

    public int orbitalState() {
        if (this.mode == 1) {
            return 0;
        }
        ItemStack satellite = this.items.get(SLOT_SATELLITE);
        if (isLegacyItem(satellite, "sat_gerald") || isLegacyItem(satellite, "sat_lunar_miner")) {
            return this.items.get(SLOT_ORBITAL_MODULE).is(HbmItems.MISSILE_SOYUZ_LANDER.get()) ? 2 : 1;
        }
        return 0;
    }

    public int getFuelRequired() {
        if (this.mode == 1) {
            return Math.min(5_000 + getTargetDistance(), TANK_CAPACITY);
        }
        return TANK_CAPACITY;
    }

    public int getPowerRequired() {
        return (int) (MAX_POWER * 0.75D);
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.power * pixels / MAX_POWER));
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

    public int renderRocketType() {
        return this.rocketType;
    }

    public boolean renderStarting() {
        return this.starting;
    }

    public int renderCountdown() {
        return this.countdown;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new SoyuzFluidHandler(queriedPos.immutable(), side);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return getConnectorPorts().stream().map(Port::pos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : getConnectorPorts()) {
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
        return Component.translatable("message.reinhardtshbm.power.soyuz_launcher", this.power, MAX_POWER);
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
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
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
        // TileEntityMachineBase.isItemValidForSlot was false; the GUI used plain Slots.
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
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
        return Container.stillValidBlockEntity(this, player);
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
        return Component.translatable("container.reinhardtshbm.soyuz_launcher");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SoyuzLauncherMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            drop(level, pos, stack);
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
        tag.putByte("Mode", this.mode);
        tag.putByte("RocketType", this.rocketType);
        tag.put("Kerosene", this.keroseneTank.save());
        tag.put("Oxygen", this.oxygenTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.parseOptional(registries, tag.getCompound("Slot" + i)));
        }
        this.power = tag.getLong("Power");
        this.mode = tag.getByte("Mode");
        this.starting = tag.getBoolean("Starting");
        this.countdown = tag.contains("Countdown") ? tag.getInt("Countdown") : MAX_COUNTDOWN;
        this.rocketType = tag.getByte("RocketType");
        this.keroseneTank.load(tag.getCompound("Kerosene"));
        this.oxygenTank.load(tag.getCompound("Oxygen"));
        ensureTankTypes();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        // Countdown is synchronized to clients, but restarting the world cancels it in 1.7.10.
        tag.putBoolean("Starting", this.starting);
        tag.putInt("Countdown", this.countdown);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void liftOff() {
        if (this.level == null || this.level.isClientSide || !canLaunch()) return;
        boolean consumeModule = orbitalState() == 2;
        this.starting = false;
        int requirement = getFuelRequired();
        this.keroseneTank.drain(kerosene(), requirement, false);
        this.oxygenTank.drain(oxygen(), requirement, false);
        this.power = Math.max(0L, this.power - getPowerRequired());
        if (this.level != null) {
            SoyuzEntity soyuz = new SoyuzEntity(this.level, this.mode, targetX(), targetZ(), Math.max(0, this.rocketType));
            soyuz.setPos(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D);
            if (this.mode == 0) {
                soyuz.setPayload(0, this.items.get(SLOT_SATELLITE));
            } else {
                for (int i = SLOT_CARGO_START; i < SLOT_COUNT; i++) {
                    soyuz.setPayload(i - SLOT_CARGO_START, this.items.get(i));
                }
            }
            this.level.addFreshEntity(soyuz);
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.SOYUZ_TAKEOFF.get(), SoundSource.BLOCKS, 100.0F, 1.1F);
        }
        if (this.mode == 0) {
            if (consumeModule) {
                this.items.set(SLOT_ORBITAL_MODULE, ItemStack.EMPTY);
            }
            this.items.set(SLOT_SATELLITE, ItemStack.EMPTY);
        } else {
            for (int i = SLOT_CARGO_START; i < SLOT_COUNT; i++) {
                this.items.set(i, ItemStack.EMPTY);
            }
        }
        this.items.set(SLOT_ROCKET, ItemStack.EMPTY);
        this.countdown = MAX_COUNTDOWN;
        sync();
    }

    private boolean tickContainers() {
        boolean changed = drainContainerIntoTank(SLOT_KEROSENE_IN, SLOT_KEROSENE_OUT, this.keroseneTank, kerosene());
        changed |= drainContainerIntoTank(SLOT_OXYGEN_IN, SLOT_OXYGEN_OUT, this.oxygenTank, oxygen());
        ensureTankTypes();
        return changed;
    }

    private boolean drainContainerIntoTank(int inputSlot, int outputSlot, HbmFluidTank tank, HbmFluidDefinition fluid) {
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
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
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

    private int getTargetDistance() {
        if (designatorState() != 2) return 0;
        ItemStack designator = this.items.get(SLOT_DESIGNATOR);
        CompoundTag tag = designator.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (!tag.contains("xCoord") || !tag.contains("zCoord")) {
            return 0;
        }
        int x = tag.getInt("xCoord");
        int z = tag.getInt("zCoord");
        double dx = this.worldPosition.getX() - x;
        double dz = this.worldPosition.getZ() - z;
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    private int targetX() {
        CompoundTag tag = this.items.get(SLOT_DESIGNATOR).getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.contains("xCoord") ? tag.getInt("xCoord") : this.worldPosition.getX();
    }

    private int targetZ() {
        CompoundTag tag = this.items.get(SLOT_DESIGNATOR).getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.contains("zCoord") ? tag.getInt("zCoord") : this.worldPosition.getZ();
    }

    private byte getRocketType() {
        if (!hasRocket()) {
            return -1;
        }
        ItemStack rocket = this.items.get(SLOT_ROCKET);
        return (byte) SoyuzItem.skin(rocket);
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        if (side == null) {
            return queriedPos.equals(this.worldPosition);
        }
        for (Port port : getConnectorPorts()) {
            if (port.pos().relative(side.getOpposite()).equals(queriedPos) && port.face() == side) {
                return true;
            }
        }
        return false;
    }

    private List<Port> getConnectorPorts() {
        List<Port> ports = new ArrayList<>();
        for (Direction dir : List.of(Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH)) {
            Direction rot = dir.getClockWise();
            for (int i = -6; i <= 6; i++) {
                BlockPos edge = this.worldPosition
                        .relative(dir, 7)
                        .relative(rot, i);
                ports.add(new Port(edge.immutable(), dir));
                ports.add(new Port(edge.below().immutable(), dir));
            }
        }
        return ports;
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
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : getConnectorPorts()) {
                this.level.invalidateCapabilities(port.pos());
            }
        }
    }

    private static boolean canDrainInto(ItemStack stack, HbmFluidTank tank, HbmFluidDefinition fluid) {
        return HbmFluidContainerTransfer.canDrainIntoTank(stack, tank, candidate -> candidate == fluid, output -> true);
    }

    private static boolean isPotentialDesignator(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof com.reinhardt.hbm.item.LegacyCoordinateDesignatorItem
                || stack.getItem() instanceof com.reinhardt.hbm.item.LegacyRangeDesignatorItem;
    }

    private static boolean isReadyDesignator(ItemStack stack) {
        if (!isPotentialDesignator(stack)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.contains("xCoord") && tag.contains("zCoord");
    }

    private static boolean isLegacyItem(ItemStack stack, String id) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem())
                .equals(com.reinhardt.hbm.ReinhardtsHBM.id(id));
    }

    private static HbmFluidDefinition kerosene() {
        return HbmFluids.byName("kerosene").orElseThrow();
    }

    private static HbmFluidDefinition oxygen() {
        return HbmFluids.byName("oxygen").orElseThrow();
    }

    private static int[] createSlotRange(int start, int endExclusive) {
        int[] slots = new int[endExclusive - start];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = start + i;
        }
        return slots;
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
    }

    private record Port(BlockPos pos, Direction face) {
    }

    private final class SoyuzFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private SoyuzFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
            if (stack.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return tank == 0 && fluid == kerosene() || tank == 1 && fluid == oxygen();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            int filled = 0;
            if (fluid == kerosene()) {
                filled = keroseneTank.fill(fluid, resource.getAmount(), action.simulate());
            } else if (fluid == oxygen()) {
                filled = oxygenTank.fill(fluid, resource.getAmount(), action.simulate());
            }
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
