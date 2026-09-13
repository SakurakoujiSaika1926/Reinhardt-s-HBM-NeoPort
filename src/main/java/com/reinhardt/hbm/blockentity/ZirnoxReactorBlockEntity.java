package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.ZirnoxReactorBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.event.LegacyMobSpawnEvents;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.ZirnoxRodItem;
import com.reinhardt.hbm.menu.ZirnoxReactorMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ZirnoxReactorBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int ROD_SLOT_COUNT = 24;
    public static final int CO2_INPUT_SLOT = 24;
    public static final int WATER_INPUT_SLOT = 25;
    public static final int CO2_OUTPUT_SLOT = 26;
    public static final int WATER_OUTPUT_SLOT = 27;
    public static final int SLOT_COUNT = 28;
    public static final int DATA_COUNT = 13;
    public static final int MAX_HEAT = 100_000;
    public static final int MAX_PRESSURE = 100_000;
    public static final int STEAM_CAPACITY = 8_000;
    public static final int CARBON_DIOXIDE_CAPACITY = 16_000;
    public static final int WATER_CAPACITY = 32_000;

    private static final int[] ROD_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };
    private static final int[] ALL_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
            24, 25, 26, 27
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank steam = new HbmFluidTank(superhotSteam(), STEAM_CAPACITY);
    private final HbmFluidTank carbonDioxide = new HbmFluidTank(carbonDioxide(), CARBON_DIOXIDE_CAPACITY);
    private final HbmFluidTank water = new HbmFluidTank(water(), WATER_CAPACITY);
    private int heat;
    private int pressure;
    private int output;
    private boolean active;
    private boolean redstonePowered;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ZirnoxReactorBlockEntity.this.heat;
                case 1 -> ZirnoxReactorBlockEntity.this.pressure;
                case 2 -> ZirnoxReactorBlockEntity.this.active ? 1 : 0;
                case 3 -> ZirnoxReactorBlockEntity.this.redstonePowered ? 1 : 0;
                case 4 -> ZirnoxReactorBlockEntity.this.output;
                case 5 -> ZirnoxReactorBlockEntity.this.steam.type().oldId();
                case 6 -> ZirnoxReactorBlockEntity.this.steam.amount();
                case 7 -> ZirnoxReactorBlockEntity.this.steam.capacity();
                case 8 -> ZirnoxReactorBlockEntity.this.carbonDioxide.type().oldId();
                case 9 -> ZirnoxReactorBlockEntity.this.carbonDioxide.amount();
                case 10 -> ZirnoxReactorBlockEntity.this.carbonDioxide.capacity();
                case 11 -> ZirnoxReactorBlockEntity.this.water.amount();
                case 12 -> ZirnoxReactorBlockEntity.this.water.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ZirnoxReactorBlockEntity.this.heat = value;
                case 1 -> ZirnoxReactorBlockEntity.this.pressure = value;
                case 2 -> ZirnoxReactorBlockEntity.this.active = value != 0;
                case 3 -> ZirnoxReactorBlockEntity.this.redstonePowered = value != 0;
                case 4 -> ZirnoxReactorBlockEntity.this.output = value;
                case 6 -> ZirnoxReactorBlockEntity.this.steam.setAmount(value);
                case 9 -> ZirnoxReactorBlockEntity.this.carbonDioxide.setAmount(value);
                case 11 -> ZirnoxReactorBlockEntity.this.water.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ZirnoxReactorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ZIRNOX_REACTOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ZirnoxReactorBlockEntity reactor) {
        reactor.tickServer(level);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    /** Entry point for the original ItemDyatlov, which set Zirnox heat to 200000. */
    public void forceOverheat() {
        this.heat = 200_000;
        setChanged();
    }

    public HbmFluidTank steamTank() {
        return this.steam;
    }

    public HbmFluidTank carbonDioxideTank() {
        return this.carbonDioxide;
    }

    public HbmFluidTank waterTank() {
        return this.water;
    }

    public int heat() {
        return this.heat;
    }

    public int pressure() {
        return this.pressure;
    }

    public boolean active() {
        return this.active;
    }

    public boolean redstonePowered() {
        return this.redstonePowered;
    }

    public void toggleActive() {
        if (this.redstonePowered) {
            return;
        }
        this.active = !this.active;
        sync();
    }

    public void ventCarbonDioxide() {
        this.carbonDioxide.drain(carbonDioxide(), 1_000, false);
        sync();
    }

    public void setRedstonePowered(boolean powered) {
        if (!powered && this.redstonePowered) {
            this.active = false;
        }
        this.redstonePowered = powered;
        if (powered) {
            this.active = true;
        }
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (queriedPos.equals(this.worldPosition)) {
            return side == null ? new ZirnoxFluidHandler() : null;
        }
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new ZirnoxFluidHandler();
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
        setChangedAndMaybeSync(false);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChangedAndMaybeSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < ROD_SLOT_COUNT) {
            return stack.getItem() instanceof ZirnoxRodItem;
        }
        if (slot == CO2_INPUT_SLOT) {
            return canDrainFluid(stack, carbonDioxide());
        }
        if (slot == WATER_INPUT_SLOT) {
            return canDrainFluid(stack, water());
        }
        if (slot == CO2_OUTPUT_SLOT || slot == WATER_OUTPUT_SLOT) {
            return canFillFluidContainer(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack) && slot != CO2_OUTPUT_SLOT && slot != WATER_OUTPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot >= 0 && slot < ROD_SLOT_COUNT && !(stack.getItem() instanceof ZirnoxRodItem))
                || slot == CO2_OUTPUT_SLOT
                || slot == WATER_OUTPUT_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndMaybeSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.zirnox");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ZirnoxReactorMenu(containerId, playerInventory, this, this.menuData, this.worldPosition);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            drop(level, pos, this.items.get(slot));
            this.items.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putInt("Heat", this.heat);
        tag.putInt("Pressure", this.pressure);
        tag.putInt("Output", this.output);
        tag.putBoolean("Active", this.active);
        tag.putBoolean("RedstonePowered", this.redstonePowered);
        tag.put("Steam", this.steam.save());
        tag.put("CarbonDioxide", this.carbonDioxide.save());
        tag.put("Water", this.water.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        setupTanks();
        this.heat = tag.getInt("Heat");
        this.pressure = tag.getInt("Pressure");
        this.output = tag.getInt("Output");
        this.active = tag.getBoolean("Active");
        this.redstonePowered = tag.getBoolean("RedstonePowered");
        this.steam.load(tag.getCompound("Steam"));
        this.carbonDioxide.load(tag.getCompound("CarbonDioxide"));
        this.water.load(tag.getCompound("Water"));
        setupTanks();
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
        setupTanks();
        if (this.redstonePowered) {
            this.active = true;
        }
        this.output = 0;
        transferContainers();

        if (this.active) {
            for (int slot = 0; slot < ROD_SLOT_COUNT; slot++) {
                ItemStack stack = this.items.get(slot);
                if (stack.getItem() instanceof ZirnoxRodItem) {
                    decay(slot, stack);
                } else if (stack.is(HbmItems.METEORITE_SWORD_BRED.get())) {
                    this.items.set(slot, new ItemStack(HbmItems.METEORITE_SWORD_IRRADIATED.get()));
                }
            }
        }

        this.pressure = this.carbonDioxide.amount() * 2
                + (int) (this.heat * (this.carbonDioxide.amount() / (float) Math.max(1, this.carbonDioxide.capacity())));

        if (this.heat > 0 && this.heat < MAX_HEAT) {
            if (this.water.amount() > 0 && this.carbonDioxide.amount() > 0 && this.steam.amount() < this.steam.capacity()) {
                generateSteam();
                this.heat -= (int) (this.heat * this.pressure / 1_000_000.0F);
            } else {
                this.heat -= 10;
            }
            if (this.heat < 0) {
                this.heat = 0;
            }
        }

        sendSteam(level);
        if (this.pressure > MAX_PRESSURE || this.heat > MAX_HEAT) {
            meltdown(level);
            return;
        }

        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void transferContainers() {
        boolean changed = false;
        changed |= HbmFluidContainerTransfer.drainIntoTank(
                this.items.get(CO2_INPUT_SLOT),
                this.carbonDioxide,
                fluid -> fluid == carbonDioxide(),
                output -> canMerge(CO2_OUTPUT_SLOT, output),
                output -> merge(CO2_OUTPUT_SLOT, output)
        );
        changed |= HbmFluidContainerTransfer.drainIntoTank(
                this.items.get(WATER_INPUT_SLOT),
                this.water,
                fluid -> fluid == water(),
                output -> canMerge(WATER_OUTPUT_SLOT, output),
                output -> merge(WATER_OUTPUT_SLOT, output)
        );
        if (changed) {
            setChanged();
        }
    }

    private void decay(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof ZirnoxRodItem rod)) {
            return;
        }
        ZirnoxRodItem.Fuel fuel = rod.fuel(stack);
        int decay = neighborFuelCount(slot);
        if (!fuel.breeding()) {
            decay++;
        }
        for (int i = 0; i < decay; i++) {
            this.heat += fuel.heat();
            ZirnoxRodItem.incrementLife(stack);
            if (ZirnoxRodItem.life(stack) > fuel.maxLife()) {
                this.items.set(slot, depletedResult(fuel.id()));
                break;
            }
        }
    }

    private int neighborFuelCount(int slot) {
        int count = 0;
        for (int neighbor : neighboringSlots(slot)) {
            ItemStack stack = this.items.get(neighbor);
            if (stack.getItem() instanceof ZirnoxRodItem rod && !rod.fuel(stack).breeding()) {
                count++;
            }
        }
        return count;
    }

    private void generateSteam() {
        if (this.heat <= 10_256) {
            return;
        }
        int cycle = (int) (((this.heat - 10_256F) / MAX_HEAT)
                * Math.min(this.carbonDioxide.amount() / 14_000F, 1.0F)
                * 25F
                * 5F);
        if (cycle <= 0) {
            return;
        }
        int availableWater = Math.min(this.water.amount(), cycle);
        int availableSteam = Math.min(this.steam.capacity() - this.steam.amount(), availableWater);
        if (availableSteam <= 0) {
            return;
        }
        this.water.drain(water(), availableSteam, false);
        this.steam.fill(superhotSteam(), availableSteam, false);
        this.output = availableSteam;
    }

    private void sendSteam(Level level) {
        if (this.steam.amount() <= 0) {
            return;
        }
        for (Port port : ports()) {
            if (this.steam.amount() <= 0) {
                break;
            }
            int amount = Math.min(16_000, this.steam.amount());
            FluidStack stack = HbmFluids.toNeoStack(this.steam.type(), amount);
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.steam.drain(this.steam.type(), accepted, false);
            }
        }
    }

    private void meltdown(Level level) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.steam.clear();
        this.carbonDioxide.clear();
        this.water.clear();
        this.active = false;

        BlockState oldState = getBlockState();
        Direction facing = oldState.hasProperty(LargeMachineBlock.FACING) ? oldState.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        LargeMachineBlock.removeDummies(level, this.worldPosition, facing, ZirnoxReactorBlock.FOOTPRINT);
        BlockState destroyed = HbmBlocks.ZIRNOX_DESTROYED.get().defaultBlockState().setValue(LargeMachineBlock.FACING, facing);
        level.setBlock(this.worldPosition, destroyed, 3);
        LargeMachineBlock.placeDummies(level, this.worldPosition, facing, ZirnoxReactorBlock.FOOTPRINT);
        level.explode(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 3.0D, this.worldPosition.getZ() + 0.5D, 12.0F, Level.ExplosionInteraction.BLOCK);
        if (level instanceof ServerLevel serverLevel) {
            AABB area = new AABB(
                    worldPosition.getX() - 100.0D, worldPosition.getY() - 100.0D, worldPosition.getZ() - 100.0D,
                    worldPosition.getX() + 101.0D, worldPosition.getY() + 101.0D, worldPosition.getZ() + 101.0D
            );
            HbmAdvancements.awardNearby(serverLevel, area, "zirnox_boom");
        }
        markNearbyRadiationBeastTargets(level);
    }

    private void markNearbyRadiationBeastTargets(Level level) {
        if (!HbmConfig.ENABLE_MELTDOWN_ELEMENTALS.get() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Old: AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1).expand(100, 100, 100).
        AABB area = new AABB(worldPosition.getX() - 100.0D, worldPosition.getY() - 100.0D,
                worldPosition.getZ() - 100.0D, worldPosition.getX() + 101.0D,
                worldPosition.getY() + 101.0D, worldPosition.getZ() + 101.0D);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area)) {
            LegacyMobSpawnEvents.markRadiationBeastTarget(player);
        }
    }

    private void setupTanks() {
        this.steam.setCapacity(STEAM_CAPACITY);
        this.carbonDioxide.setCapacity(CARBON_DIOXIDE_CAPACITY);
        this.water.setCapacity(WATER_CAPACITY);
        if (this.steam.amount() == 0 && this.steam.type() != superhotSteam()) {
            this.steam.setType(superhotSteam());
        }
        if (this.carbonDioxide.amount() == 0 && this.carbonDioxide.type() != carbonDioxide()) {
            this.carbonDioxide.setType(carbonDioxide());
        }
        if (this.water.amount() == 0 && this.water.type() != water()) {
            this.water.setType(water());
        }
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : ports()) {
            if (queriedPos.equals(port.dummyPos()) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    public boolean allowsAutomationPort(BlockPos queriedPos, @Nullable Direction side) {
        return allowsPort(queriedPos, side);
    }

    public List<Port> ports() {
        Direction facing = getBlockState().hasProperty(LargeMachineBlock.FACING) ? getBlockState().getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();
        return List.of(
                new Port(this.worldPosition.relative(right, 2).above(1), this.worldPosition.relative(right, 3).above(1), right),
                new Port(this.worldPosition.relative(right, 2).above(3), this.worldPosition.relative(right, 3).above(3), right),
                new Port(this.worldPosition.relative(left, 2).above(1), this.worldPosition.relative(left, 3).above(1), left),
                new Port(this.worldPosition.relative(left, 2).above(3), this.worldPosition.relative(left, 3).above(3), left)
        );
    }

    private ItemStack depletedResult(String fuelId) {
        return switch (fuelId) {
            case "th232_fuel" -> LegacyVariantItem.stackFor(HbmItems.ROD_ZIRNOX, "thorium_fuel");
            case "lithium_fuel" -> new ItemStack(HbmItems.ROD_ZIRNOX_TRITIUM.get());
            case "natural_uranium_fuel",
                 "uranium_fuel",
                 "thorium_fuel",
                 "mox_fuel",
                 "plutonium_fuel",
                 "u233_fuel",
                 "u235_fuel",
                 "les_fuel",
                 "zfb_mox_fuel" -> LegacyVariantItem.stackFor(HbmItems.ROD_ZIRNOX_DEPLETED, fuelId);
            default -> ItemStack.EMPTY;
        };
    }

    private boolean canDrainFluid(ItemStack stack, HbmFluidDefinition fluid) {
        HbmFluidTank target = fluid == water() ? this.water : this.carbonDioxide;
        return HbmFluidContainerTransfer.canDrainIntoTank(stack, target, candidate -> candidate == fluid, output -> true);
    }

    private static boolean canFillFluidContainer(ItemStack stack) {
        return !stack.isEmpty() && FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
    }

    private boolean canMerge(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, output) && current.getCount() + output.getCount() <= current.getMaxStackSize());
    }

    private void merge(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, output.copy());
        } else if (ItemStack.isSameItemSameComponents(current, output)) {
            current.grow(output.getCount());
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            for (Port port : ports()) {
                this.level.invalidateCapabilities(port.dummyPos());
            }
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void setChangedAndMaybeSync(boolean sync) {
        setChanged();
        if (sync) {
            sync();
        }
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

    private static int[] neighboringSlots(int id) {
        return switch (id) {
            case 0 -> new int[]{1, 7};
            case 1 -> new int[]{0, 2, 8};
            case 2 -> new int[]{1, 9};
            case 3 -> new int[]{4, 10};
            case 4 -> new int[]{3, 5, 11};
            case 5 -> new int[]{4, 6, 12};
            case 6 -> new int[]{5, 13};
            case 7 -> new int[]{0, 8, 14};
            case 8 -> new int[]{1, 7, 9, 15};
            case 9 -> new int[]{2, 8, 16};
            case 10 -> new int[]{3, 11, 17};
            case 11 -> new int[]{4, 10, 12, 18};
            case 12 -> new int[]{5, 11, 13, 19};
            case 13 -> new int[]{6, 12, 20};
            case 14 -> new int[]{7, 15, 21};
            case 15 -> new int[]{8, 14, 16, 22};
            case 16 -> new int[]{9, 15, 23};
            case 17 -> new int[]{10, 18};
            case 18 -> new int[]{11, 17, 19};
            case 19 -> new int[]{12, 18, 20};
            case 20 -> new int[]{13, 19};
            case 21 -> new int[]{14, 22};
            case 22 -> new int[]{15, 21, 23};
            case 23 -> new int[]{16, 22};
            default -> new int[0];
        };
    }

    private static HbmFluidDefinition superhotSteam() {
        return HbmFluids.byName("superhotsteam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition carbonDioxide() {
        return HbmFluids.byName("carbondioxide").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    public record Port(BlockPos dummyPos, BlockPos connectorPos, Direction face) {
    }

    private final class ZirnoxFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> steam.getFluidInTank(0);
                case 1 -> carbonDioxide.getFluidInTank(0);
                case 2 -> water.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> steam.capacity();
                case 1 -> carbonDioxide.capacity();
                case 2 -> water.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return (tank == 1 && fluid == carbonDioxide()) || (tank == 2 && fluid == water());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank tank = fluid == water() ? water : fluid == carbonDioxide() ? carbonDioxide : null;
            if (tank == null) {
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
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != steam.type()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = steam.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }
}
