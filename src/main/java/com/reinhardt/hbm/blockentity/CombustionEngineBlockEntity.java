package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CombustionEngineBlock;
import com.reinhardt.hbm.client.sound.CombustionEngineClientSounds;
import com.reinhardt.hbm.fluid.CombustibleFuelGrade;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.CombustionPistonSetItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.CombustionEngineMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CombustionEngineBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final long MAX_POWER = 2_500_000L;
    public static final int FUEL_CAPACITY = 24_000;
    public static final int MAX_THROTTLE = 30;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_PISTON = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_IDENTIFIER = 4;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 10;
    private static final int SMOKE_BUFFER_CAPACITY = 50;
    private static final int[] TOP_SLOTS = {SLOT_INPUT};
    private static final int[] BOTTOM_SLOTS = {SLOT_OUTPUT, SLOT_BATTERY};
    private static final int[] SIDE_SLOTS = {SLOT_PISTON, SLOT_BATTERY, SLOT_IDENTIFIER};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank fuelTank = new HbmFluidTank(defaultFuel(), FUEL_CAPACITY);
    private final HbmFluidTank smokeTank = new HbmFluidTank(HbmPollution.smokeFluid(com.reinhardt.hbm.pollution.HbmPollutionType.SOOT), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokeLeadedTank = new HbmFluidTank(HbmPollution.smokeFluid(com.reinhardt.hbm.pollution.HbmPollutionType.HEAVYMETAL), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokePoisonTank = new HbmFluidTank(HbmPollution.smokeFluid(com.reinhardt.hbm.pollution.HbmPollutionType.POISON), SMOKE_BUFFER_CAPACITY);
    private long power;
    private boolean enabled;
    private boolean running;
    private int throttle;
    private int tenthMilliBucket;
    private int playersUsing;
    private float doorAngle;
    private float prevDoorAngle;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) CombustionEngineBlockEntity.this.power;
                case 1 -> (int) MAX_POWER;
                case 2 -> CombustionEngineBlockEntity.this.enabled ? 1 : 0;
                case 3 -> CombustionEngineBlockEntity.this.running ? 1 : 0;
                case 4 -> CombustionEngineBlockEntity.this.throttle;
                case 5 -> CombustionEngineBlockEntity.this.fuelTank.type().oldId();
                case 6 -> CombustionEngineBlockEntity.this.fuelTank.amount();
                case 7 -> CombustionEngineBlockEntity.this.fuelTank.capacity();
                case 8 -> (int) CombustionEngineBlockEntity.this.hePerTick();
                case 9 -> (int) Math.round(CombustionEngineBlockEntity.this.currentEfficiency() * 100.0D);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CombustionEngineBlockEntity.this.power = value;
                case 2 -> CombustionEngineBlockEntity.this.enabled = value != 0;
                case 3 -> CombustionEngineBlockEntity.this.running = value != 0;
                case 4 -> CombustionEngineBlockEntity.this.throttle = clampThrottle(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CombustionEngineBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.COMBUSTION_ENGINE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionEngineBlockEntity engine) {
        if (level.isClientSide) {
            engine.tickClient();
            CombustionEngineClientSounds.tick(engine);
            return;
        }
        engine.tickServer(level);
    }

    public static boolean acceptsFuel(HbmFluidDefinition fluid) {
        return fluid != null && !fluid.isNone() && fluid.combustibleFuelGrade() != null && fluid.combustibleHeatEnergy() > 0L;
    }

    public HbmFluidTank fuelTank() {
        return this.fuelTank;
    }

    public boolean running() {
        return this.running;
    }

    public float doorAngle(float partialTick) {
        return this.prevDoorAngle + (this.doorAngle - this.prevDoorAngle) * partialTick;
    }

    public long power() {
        return this.power;
    }

    public long hePerTick() {
        // Old compound-assignment semantics truncate the calculated double
        // toward zero; they do not round to nearest.
        return (long) ((this.throttle * 0.2D) * this.fuelTank.type().combustibleHeatEnergy() / 1_000.0D * currentEfficiency());
    }

    public double currentEfficiency() {
        ItemStack piston = this.items.get(SLOT_PISTON);
        if (!(piston.getItem() instanceof CombustionPistonSetItem pistonSet) || !acceptsFuel(this.fuelTank.type())) {
            return 0.0D;
        }
        CombustibleFuelGrade grade = this.fuelTank.type().combustibleFuelGrade();
        return pistonSet.pistonType(piston).efficiency(grade);
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
        sync();
    }

    public void setThrottle(int throttle) {
        this.throttle = clampThrottle(throttle);
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new FuelFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public boolean allowsAutomationPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        if (queriedPos.equals(this.worldPosition)) {
            return side == null;
        }
        for (Port port : ports(this.level)) {
            if (port.hostPos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    public boolean pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos soundPos) {
        if (fluid == null || !acceptsFuel(fluid)) {
            return false;
        }
        if (this.fuelTank.type() != fluid) {
            this.fuelTank.setType(fluid);
            this.tenthMilliBucket = 0;
            sync();
            level.playSound(null, soundPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
        }
        return true;
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
        for (Port port : ports(level)) {
            if (port.connectorPos().equals(connectorPos) && port.face() == machineSide) {
                return true;
            }
        }
        return false;
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
        return Component.translatable("message.reinhardtshbm.power.combustion_engine", hePerTick(), this.power, MAX_POWER);
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
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) {
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
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        this.items.set(slot, stack.copy());
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }
        if (slot == SLOT_IDENTIFIER) {
            // 1.7.10 applies the identifier when the slot is written. Do the
            // same for GUI, hopper, and menu writes instead of waiting a tick.
            applyFluidIdentifierSlot();
            sync();
            return;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> canDrainIntoFuel(stack);
            case SLOT_PISTON -> stack.getItem() instanceof CombustionPistonSetItem;
            case SLOT_BATTERY -> BatteryPackItem.isBattery(stack);
            case SLOT_IDENTIFIER -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case DOWN -> BOTTOM_SLOTS;
            case UP -> TOP_SLOTS;
            default -> SIDE_SLOTS;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == SLOT_OUTPUT) {
            return true;
        }
        return slot == SLOT_BATTERY && BatteryPackItem.isBattery(stack) && BatteryPackItem.charge(stack) >= BatteryPackItem.capacity(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void startOpen(Player player) {
        if (!player.isSpectator()) {
            this.playersUsing++;
            sync();
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!player.isSpectator()) {
            this.playersUsing = Math.max(0, this.playersUsing - 1);
            sync();
        }
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.combustion_engine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CombustionEngineMenu(containerId, playerInventory, this, this.menuData);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.putBoolean("Enabled", this.enabled);
        tag.putBoolean("Running", this.running);
        tag.putInt("Throttle", this.throttle);
        tag.putInt("TenthMilliBucket", this.tenthMilliBucket);
        tag.putInt("PlayersUsing", this.playersUsing);
        tag.put("FuelTank", this.fuelTank.save());
        tag.put("Smoke", this.smokeTank.save());
        tag.put("SmokeLeaded", this.smokeLeadedTank.save());
        tag.put("SmokePoison", this.smokePoisonTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.power = tag.getLong("Power");
        this.enabled = tag.getBoolean("Enabled");
        this.running = tag.getBoolean("Running");
        this.throttle = clampThrottle(tag.getInt("Throttle"));
        this.tenthMilliBucket = Math.max(0, Math.min(9, tag.getInt("TenthMilliBucket")));
        this.playersUsing = Math.max(0, tag.getInt("PlayersUsing"));
        this.fuelTank.load(tag.getCompound("FuelTank"));
        this.smokeTank.load(tag.getCompound("Smoke"));
        this.smokeLeadedTank.load(tag.getCompound("SmokeLeaded"));
        this.smokePoisonTank.load(tag.getCompound("SmokePoison"));
        if (this.fuelTank.type().isNone() && this.fuelTank.amount() == 0) {
            this.fuelTank.setType(defaultFuel());
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
        boolean changed = applyFluidIdentifierSlot();
        changed |= transferFluidInput();
        this.running = false;
        changed |= sendSmoke(level);

        int fillTenths = this.fuelTank.amount() * 10 + this.tenthMilliBucket;
        if (this.enabled && this.throttle > 0 && fillTenths > 0 && acceptsFuel(this.fuelTank.type()) && currentEfficiency() > 0.0D) {
            int toBurn = Math.min(fillTenths, this.throttle * 2);
            HbmFluidDefinition burnedFuel = this.fuelTank.type();
            // TileEntityMachineCombustionEngine used a long += double.  Keep
            // the exact truncation order instead of Math.round().
            this.power = Math.min(MAX_POWER, this.power + (long) (toBurn * (this.fuelTank.type().combustibleHeatEnergy() / 10_000.0D) * currentEfficiency()));
            fillTenths -= toBurn;
            this.fuelTank.setAmount(fillTenths / 10);
            this.tenthMilliBucket = fillTenths % 10;
            this.running = toBurn > 0;
            if (toBurn > 0 && level.getGameTime() % 5L == 0L) {
                HbmPollution.bufferedLegacyPolluteFluid(level, this.worldPosition, burnedFuel, HbmPollution.ReleaseType.BURN, toBurn * 0.5D, this::smokeTank);
            }
            changed = true;
        }

        this.power = BatteryPackItem.chargeFromMachine(this.items.get(SLOT_BATTERY), this.power);
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.power = Math.min(MAX_POWER, this.power);
        setLit(this.running);
        if (changed || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient() {
        this.prevDoorAngle = this.doorAngle;
        float swingSpeed = this.doorAngle / 10.0F + 3.0F;
        if (this.playersUsing > 0) {
            this.doorAngle += swingSpeed;
        } else {
            this.doorAngle -= swingSpeed;
        }
        this.doorAngle = Math.max(0.0F, Math.min(135.0F, this.doorAngle));
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        return allowsAutomationPort(queriedPos, side);
    }

    private List<Port> ports(LevelAccessor level) {
        Direction facing = facing(level);
        Direction rotation = LegacyMachineGeometry.forgeRotateUp(facing);
        return List.of(
                port(this.worldPosition.relative(facing).relative(rotation), facing),
                port(this.worldPosition.relative(facing).relative(rotation.getOpposite()), facing),
                port(this.worldPosition.relative(facing.getOpposite(), 2).relative(rotation), facing.getOpposite()),
                port(this.worldPosition.relative(facing.getOpposite(), 2).relative(rotation.getOpposite()), facing.getOpposite())
        );
    }

    /**
     * The positions in TileEntityMachineCombustionEngine#getConPos are already
     * the exposed cable/pipe positions. The adjacent block toward the machine
     * is the dummy that forwards NeoForge capabilities to this core.
     */
    private static Port port(BlockPos connectorPos, Direction face) {
        BlockPos connector = connectorPos.immutable();
        return new Port(connector, connector.relative(face.getOpposite()).immutable(), face);
    }

    private Direction facing(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        return state.hasProperty(CombustionEngineBlock.FACING) ? state.getValue(CombustionEngineBlock.FACING) : Direction.NORTH;
    }

    private boolean sendSmoke(Level level) {
        boolean changed = false;
        for (Port port : ports(level)) {
            changed |= HbmPollution.sendSmoke(level, this.worldPosition, port.connectorPos(), port.face().getOpposite(), this.smokeTank, this.smokeLeadedTank, this.smokePoisonTank);
        }
        return changed;
    }

    private HbmFluidTank smokeTank(com.reinhardt.hbm.pollution.HbmPollutionType type) {
        return switch (type) {
            case HEAVYMETAL -> this.smokeLeadedTank;
            case POISON -> this.smokePoisonTank;
            case SOOT, FALLOUT -> this.smokeTank;
        };
    }

    private boolean transferFluidInput() {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }
        boolean moved = HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.fuelTank,
                CombustionEngineBlockEntity::acceptsFuel,
                output -> canPlaceOutput(SLOT_OUTPUT, output),
                output -> placeOutput(SLOT_OUTPUT, output)
        );
        if (moved && input.isEmpty()) {
            this.items.set(SLOT_INPUT, ItemStack.EMPTY);
        }
        return moved;
    }

    private boolean applyFluidIdentifierSlot() {
        ItemStack identifier = this.items.get(SLOT_IDENTIFIER);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        HbmFluidDefinition fluid = FluidIdentifierItem.primary(identifier);
        if (!acceptsFuel(fluid)) {
            return false;
        }
        if (this.fuelTank.type() == fluid) {
            return true;
        }
        this.fuelTank.setType(fluid);
        this.tenthMilliBucket = 0;
        return true;
    }

    private boolean canDrainIntoFuel(ItemStack stack) {
        return FluidUtil.getFluidHandler(stack.copyWithCount(1))
                .map(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE))
                .flatMap(fluidStack -> HbmFluids.fromNeoFluid(fluidStack.getFluid()))
                .filter(CombustionEngineBlockEntity::acceptsFuel)
                .isPresent();
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

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof CombustionEngineBlock && state.getValue(CombustionEngineBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(CombustionEngineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static int clampThrottle(int throttle) {
        return Math.max(0, Math.min(MAX_THROTTLE, throttle));
    }

    private static HbmFluidDefinition defaultFuel() {
        return HbmFluids.byName("diesel").orElse(HbmFluids.none());
    }

    private record Port(BlockPos connectorPos, BlockPos hostPos, Direction face) {
    }

    private final class FuelFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private FuelFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? fuelTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? fuelTank.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return false;
            }
            return HbmFluids.fromNeoFluid(stack.getFluid()).filter(CombustionEngineBlockEntity::acceptsFuel).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!acceptsFuel(fluid)) {
                return 0;
            }
            int filled = fuelTank.fill(fluid, resource.getAmount(), action.simulate());
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
