package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PowerMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.WoodBurnerMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
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
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WoodBurnerBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int FUEL_SLOT = 0;
    public static final int ASH_SLOT = 1;
    public static final int FLUID_ID_SLOT = 2;
    public static final int FLUID_INPUT_SLOT = 3;
    public static final int FLUID_OUTPUT_SLOT = 4;
    public static final int BATTERY_SLOT = 5;
    public static final int SLOT_COUNT = 6;
    public static final int DATA_COUNT = 9;
    public static final long OUTPUT_PER_TICK = 100L;
    public static final long ENERGY_CAPACITY = 100_000L;
    public static final int TANK_CAPACITY = 16_000;
    private static final int SOLID_ASH_THRESHOLD = 2_000;
    private static final int LIQUID_BURN_PER_TICK = 2;
    private static final int[] AUTOMATION_SLOTS = {FUEL_SLOT, ASH_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank tank = new HbmFluidTank(defaultFluid(), TANK_CAPACITY);
    private HbmFluidDefinition configuredFluid = defaultFluid();
    private long energyStored;
    private long lastOutput;
    private int burnTime;
    private int burnTimeTotal;
    private int powerGen;
    private boolean isOn;
    private boolean liquidBurn;
    private int ashLevelWood;
    private int ashLevelCoal;
    private int ashLevelMisc;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) WoodBurnerBlockEntity.this.energyStored;
                case 1 -> WoodBurnerBlockEntity.this.burnTime;
                case 2 -> WoodBurnerBlockEntity.this.burnTimeTotal;
                case 3 -> WoodBurnerBlockEntity.this.powerGen;
                case 4 -> WoodBurnerBlockEntity.this.isOn ? 1 : 0;
                case 5 -> WoodBurnerBlockEntity.this.liquidBurn ? 1 : 0;
                case 6 -> WoodBurnerBlockEntity.this.displayFluid().oldId();
                case 7 -> WoodBurnerBlockEntity.this.tank.amount();
                case 8 -> WoodBurnerBlockEntity.this.tank.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> WoodBurnerBlockEntity.this.energyStored = value;
                case 1 -> WoodBurnerBlockEntity.this.burnTime = value;
                case 2 -> WoodBurnerBlockEntity.this.burnTimeTotal = value;
                case 3 -> WoodBurnerBlockEntity.this.powerGen = value;
                case 4 -> WoodBurnerBlockEntity.this.isOn = value != 0;
                case 5 -> WoodBurnerBlockEntity.this.liquidBurn = value != 0;
                case 6 -> WoodBurnerBlockEntity.this.configuredFluid = HbmFluids.byOldId(value).orElse(HbmFluids.none());
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public WoodBurnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.WOOD_BURNER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WoodBurnerBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.tickClient(state);
            return;
        }

        blockEntity.powerGen = 0;
        boolean changed = blockEntity.tickContainers();
        blockEntity.energyStored = BatteryPackItem.chargeFromMachine(blockEntity.items.get(BATTERY_SLOT), blockEntity.energyStored);
        PowerNetworkManager.tickFromEndpoint(level, blockEntity);
        if (blockEntity.liquidBurn) {
            changed |= blockEntity.tickLiquidFuel(level, pos);
        } else {
            changed |= blockEntity.tickSolidFuel(level, pos);
        }
        if (blockEntity.powerGen > 0) {
            blockEntity.energyStored = Math.min(ENERGY_CAPACITY, blockEntity.energyStored + blockEntity.powerGen);
            changed = true;
        }
        blockEntity.ensureConfiguredTankType();
        blockEntity.setLit(blockEntity.powerGen > 0 || (!blockEntity.liquidBurn && blockEntity.burnTime > 0));
        if (changed || level.getGameTime() % 10L == 0L) {
            blockEntity.sync();
        }
    }

    public HbmFluidDefinition fluid() {
        return displayFluid();
    }

    public int fluidAmount() {
        return this.tank.amount();
    }

    public boolean isOn() {
        return this.isOn;
    }

    public boolean liquidBurn() {
        return this.liquidBurn;
    }

    public void toggleEnabled() {
        this.isOn = !this.isOn;
        sync();
    }

    public void toggleLiquidMode() {
        this.liquidBurn = !this.liquidBurn;
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new WoodBurnerFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return powerConnectorPorts(level).stream().map(Port::pos).toList();
    }

    @Override
    public long getAvailableOutput() {
        return this.energyStored;
    }

    @Override
    public long getRequestedInput() {
        return 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.energyStored = Math.max(0L, this.energyStored - usedOutput);
        this.lastOutput = usedOutput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.wood_burner",
                this.lastOutput,
                Math.max(OUTPUT_PER_TICK, this.powerGen),
                this.energyStored,
                ENERGY_CAPACITY
        );
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
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
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
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> fuelDuration(stack) > 0;
            case FLUID_ID_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
            case FLUID_INPUT_SLOT -> isDrainableFluidContainer(stack);
            case BATTERY_SLOT -> BatteryPackItem.isBattery(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == FUEL_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == ASH_SLOT;
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
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.wood_burner");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WoodBurnerMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.items.size(); i++) {
            drop(level, pos, this.items.get(i));
            this.items.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < this.items.size(); i++) {
            tag.put("Slot" + i, this.items.get(i).saveOptional(registries));
        }
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastOutput", this.lastOutput);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("BurnTimeTotal", this.burnTimeTotal);
        tag.putInt("PowerGen", this.powerGen);
        tag.putBoolean("IsOn", this.isOn);
        tag.putBoolean("LiquidBurn", this.liquidBurn);
        tag.putInt("AshWood", this.ashLevelWood);
        tag.putInt("AshCoal", this.ashLevelCoal);
        tag.putInt("AshMisc", this.ashLevelMisc);
        tag.put("Tank", this.tank.save());
        tag.putString("ConfiguredFluid", this.configuredFluid.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.parseOptional(registries, tag.getCompound("Slot" + i)));
        }
        if (this.items.get(FUEL_SLOT).isEmpty() && tag.contains("Fuel")) {
            this.items.set(FUEL_SLOT, ItemStack.parseOptional(registries, tag.getCompound("Fuel")));
        }
        this.energyStored = tag.getLong("EnergyStored");
        this.lastOutput = tag.getLong("LastOutput");
        this.burnTime = tag.getInt("BurnTime");
        this.burnTimeTotal = tag.getInt("BurnTimeTotal");
        this.powerGen = tag.getInt("PowerGen");
        this.isOn = tag.getBoolean("IsOn");
        this.liquidBurn = tag.getBoolean("LiquidBurn");
        this.ashLevelWood = tag.getInt("AshWood");
        this.ashLevelCoal = tag.getInt("AshCoal");
        this.ashLevelMisc = tag.getInt("AshMisc");
        this.tank.load(tag.getCompound("Tank"));
        this.configuredFluid = HbmFluids.byName(tag.getString("ConfiguredFluid")).orElse(defaultFluid());
        if (this.configuredFluid.isNone() && !this.tank.type().isNone()) {
            this.configuredFluid = this.tank.type();
        }
        ensureConfiguredTankType();
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
        boolean changed = applyFluidIdentifier();
        changed |= drainContainerIntoTank();
        return changed;
    }

    private boolean applyFluidIdentifier() {
        ItemStack identifier = this.items.get(FLUID_ID_SLOT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        HbmFluidDefinition next = FluidIdentifierItem.primary(identifier);
        if (next == null || next == this.configuredFluid) {
            return false;
        }
        this.configuredFluid = next;
        this.tank.setType(next);
        return true;
    }

    private boolean drainContainerIntoTank() {
        ItemStack input = this.items.get(FLUID_INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        boolean moved = HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.tank,
                this::acceptsConfiguredFluid,
                output -> canPlaceOutput(FLUID_OUTPUT_SLOT, output),
                output -> placeOutput(FLUID_OUTPUT_SLOT, output)
        );
        if (!moved) {
            return false;
        }
        if (input.isEmpty()) {
            this.items.set(FLUID_INPUT_SLOT, ItemStack.EMPTY);
        }
        return true;
    }

    private boolean tickSolidFuel(Level level, BlockPos pos) {
        boolean changed = false;
        if (this.burnTime <= 0) {
            changed |= startBurningFuel();
        } else if (this.energyStored < ENERGY_CAPACITY && this.isOn) {
            this.burnTime--;
            this.powerGen += OUTPUT_PER_TICK;
            if (level.getGameTime() % 20L == 0L) {
                HbmPollution.increment(level, pos, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND);
            }
            changed = true;
        }
        return changed;
    }

    private boolean tickLiquidFuel(Level level, BlockPos pos) {
        if (!this.isOn || this.energyStored >= ENERGY_CAPACITY || this.tank.amount() <= 0) {
            return false;
        }
        HbmFluidDefinition fluid = this.tank.type();
        long heatEnergy = fluid.combustibleHeatEnergy();
        if (heatEnergy <= 0L) {
            return false;
        }
        int toBurn = Math.min(this.tank.amount(), LIQUID_BURN_PER_TICK);
        if (toBurn <= 0) {
            return false;
        }
        this.powerGen += (int) (heatEnergy * toBurn / 2_000L);
        this.tank.drain(fluid, toBurn, false);
        if (level.getGameTime() % 20L == 0L) {
            HbmPollution.increment(level, pos, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND * toBurn / 2.0D);
        }
        return true;
    }

    private boolean startBurningFuel() {
        ItemStack fuel = this.items.get(FUEL_SLOT);
        int duration = fuelDuration(fuel);
        if (duration <= 0) {
            return false;
        }

        AshType ashType = ashType(fuel);
        if (ashType == AshType.WOOD) {
            this.ashLevelWood += duration;
        } else if (ashType == AshType.COAL) {
            this.ashLevelCoal += duration;
        } else {
            this.ashLevelMisc += duration;
        }
        while (this.ashLevelWood >= SOLID_ASH_THRESHOLD && processAsh()) {
            this.ashLevelWood -= SOLID_ASH_THRESHOLD;
        }
        while (this.ashLevelCoal >= SOLID_ASH_THRESHOLD && processAsh()) {
            this.ashLevelCoal -= SOLID_ASH_THRESHOLD;
        }
        while (this.ashLevelMisc >= SOLID_ASH_THRESHOLD && processAsh()) {
            this.ashLevelMisc -= SOLID_ASH_THRESHOLD;
        }

        Item consumedItem = fuel.getItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            this.items.set(FUEL_SLOT, consumedItem == Items.LAVA_BUCKET ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY);
        }
        this.burnTime = duration;
        this.burnTimeTotal = duration;
        return true;
    }

    private boolean processAsh() {
        ItemStack ash = new ItemStack(HbmItems.POWDER_ASH.get());
        if (!canPlaceOutput(ASH_SLOT, ash)) {
            return false;
        }
        placeOutput(ASH_SLOT, ash);
        return true;
    }

    public static int fuelDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int base = stack.getBurnTime(null);
        if (base <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.round(base * fuelTimeMultiplier(stack)));
    }

    private static double fuelTimeMultiplier(ItemStack stack) {
        if (stack.is(ItemTags.LOGS)) {
            return 4.0D;
        }
        if (isWoodFuel(stack)) {
            return 2.0D;
        }
        return 1.0D;
    }

    private static boolean isWoodFuel(ItemStack stack) {
        if (stack.is(ItemTags.PLANKS)
                || stack.is(ItemTags.WOODEN_BUTTONS)
                || stack.is(ItemTags.WOODEN_DOORS)
                || stack.is(ItemTags.WOODEN_FENCES)
                || stack.is(ItemTags.WOODEN_PRESSURE_PLATES)
                || stack.is(ItemTags.WOODEN_SLABS)
                || stack.is(ItemTags.WOODEN_STAIRS)
                || stack.is(ItemTags.WOODEN_TRAPDOORS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id == null ? "" : id.getPath();
        return path.endsWith("_wood")
                || path.endsWith("_hyphae")
                || path.contains("_wooden_")
                || path.startsWith("wooden_");
    }

    private static AshType ashType(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id == null ? "" : id.getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("coke") || path.contains("coal") || path.contains("lignite")) {
            return AshType.COAL;
        }
        if (stack.is(ItemTags.LOGS)
                || isWoodFuel(stack)
                || path.contains("sapling")) {
            return AshType.WOOD;
        }
        return AshType.MISC;
    }

    private boolean isDrainableFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return FluidUtil.getFluidHandler(stack.copyWithCount(1))
                .map(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE))
                .filter(fluidStack -> !fluidStack.isEmpty())
                .flatMap(fluidStack -> HbmFluids.fromNeoFluid(fluidStack.getFluid()))
                .filter(this::acceptsConfiguredFluid)
                .isPresent();
    }

    private boolean acceptsConfiguredFluid(HbmFluidDefinition fluid) {
        HbmFluidDefinition configured = displayFluid();
        return fluid != null && !fluid.isNone() && (configured.isNone() || fluid == configured);
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

    private HbmFluidDefinition displayFluid() {
        if (this.tank.amount() > 0 && !this.tank.type().isNone()) {
            return this.tank.type();
        }
        return this.configuredFluid == null ? HbmFluids.none() : this.configuredFluid;
    }

    private void ensureConfiguredTankType() {
        if (this.tank.amount() == 0 && this.tank.type().isNone() && this.configuredFluid != null && !this.configuredFluid.isNone()) {
            this.tank.setType(this.configuredFluid);
        }
    }

    private List<Port> powerConnectorPorts(LevelAccessor level) {
        Direction facing = facing(level);
        Direction right = facing.getClockWise();
        BlockPos first = this.worldPosition.relative(facing.getOpposite(), 2);
        return List.of(
                new Port(first.immutable(), right.getOpposite()),
                new Port(first.relative(right).immutable(), facing.getOpposite())
        );
    }

    private List<Port> automationPorts(LevelAccessor level) {
        Direction facing = facing(level);
        Direction right = facing.getClockWise();
        BlockPos first = this.worldPosition.relative(facing.getOpposite());
        Direction face = facing.getOpposite();
        return List.of(
                new Port(first.immutable(), face),
                new Port(first.relative(right).immutable(), face)
        );
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        return allowsAutomationPort(queriedPos, side);
    }

    public boolean allowsAutomationPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        if (queriedPos.equals(this.worldPosition)) {
            return side == null;
        }
        for (Port port : automationPorts(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAutomationPort(BlockPos queriedPos) {
        if (this.level == null) {
            return false;
        }
        if (queriedPos.equals(this.worldPosition)) {
            return true;
        }
        for (Port port : automationPorts(this.level)) {
            if (port.pos().equals(queriedPos)) {
                return true;
            }
        }
        return false;
    }

    private Direction facing(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        return state.hasProperty(PowerMachineBlock.FACING) ? state.getValue(PowerMachineBlock.FACING) : Direction.NORTH;
    }

    private void tickClient(BlockState state) {
        if (this.level == null || this.powerGen <= 0 || this.level.getGameTime() % 5L != 0L) {
            return;
        }
        Direction facing = state.hasProperty(PowerMachineBlock.FACING) ? state.getValue(PowerMachineBlock.FACING) : Direction.NORTH;
        Direction right = facing.getClockWise();
        this.level.addParticle(
                ParticleTypes.SMOKE,
                this.worldPosition.getX() + 0.5D - facing.getStepX() + right.getStepX(),
                this.worldPosition.getY() + 4.0D,
                this.worldPosition.getZ() + 0.5D - facing.getStepZ() + right.getStepZ(),
                0.0D,
                0.05D,
                0.0D
        );
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof PowerMachineBlock && state.getValue(PowerMachineBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(PowerMachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : automationPorts(this.level)) {
                this.level.invalidateCapabilities(port.pos());
            }
            for (Port port : powerConnectorPorts(this.level)) {
                this.level.invalidateCapabilities(port.pos());
            }
        }
    }

    private static HbmFluidDefinition defaultFluid() {
        return HbmFluids.byName("woodoil").orElse(HbmFluids.none());
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack.copy()
        ));
    }

    private enum AshType {
        WOOD,
        COAL,
        MISC
    }

    private record Port(BlockPos pos, Direction face) {
    }

    private final class WoodBurnerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private WoodBurnerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tankIndex == 0 ? tank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tankIndex == 0 ? tank.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            if (tankIndex != 0 || stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return acceptsConfiguredFluid(fluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!acceptsConfiguredFluid(fluid)) {
                return 0;
            }
            int filled = tank.fill(fluid, resource.getAmount(), action.simulate());
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
