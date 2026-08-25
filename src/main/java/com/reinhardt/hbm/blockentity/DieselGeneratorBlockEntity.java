package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DieselGeneratorBlock;
import com.reinhardt.hbm.client.sound.DieselGeneratorClientSounds;
import com.reinhardt.hbm.fluid.CombustibleFuelGrade;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.DieselGeneratorMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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
import java.util.Map;

public class DieselGeneratorBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final long BASE_MAX_POWER = 50_000L;
    public static final int FUEL_CAPACITY = 4_000;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_IDENTIFIER_INPUT = 3;
    public static final int SLOT_IDENTIFIER_OUTPUT = 4;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 7;
    private static final int SMOKE_BUFFER_CAPACITY = 100;
    private static final int[] TOP_SLOTS = {SLOT_INPUT};
    private static final int[] BOTTOM_SLOTS = {SLOT_OUTPUT, SLOT_BATTERY};
    private static final int[] SIDE_SLOTS = {SLOT_BATTERY};
    private static final Map<CombustibleFuelGrade, Double> FUEL_EFFICIENCY = Map.of(
            CombustibleFuelGrade.MEDIUM, 0.5D,
            CombustibleFuelGrade.HIGH, 0.75D,
            CombustibleFuelGrade.AERO, 0.1D
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank fuelTank = new HbmFluidTank(defaultFuel(), FUEL_CAPACITY);
    private final HbmFluidTank smokeTank = new HbmFluidTank(HbmPollution.smokeFluid(com.reinhardt.hbm.pollution.HbmPollutionType.SOOT), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokeLeadedTank = new HbmFluidTank(HbmPollution.smokeFluid(com.reinhardt.hbm.pollution.HbmPollutionType.HEAVYMETAL), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokePoisonTank = new HbmFluidTank(HbmPollution.smokeFluid(com.reinhardt.hbm.pollution.HbmPollutionType.POISON), SMOKE_BUFFER_CAPACITY);
    private long power;
    private long powerCap = BASE_MAX_POWER;
    private boolean running;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) DieselGeneratorBlockEntity.this.power;
                case 1 -> (int) DieselGeneratorBlockEntity.this.powerCap;
                case 2 -> DieselGeneratorBlockEntity.this.running ? 1 : 0;
                case 3 -> DieselGeneratorBlockEntity.this.fuelTank.type().oldId();
                case 4 -> DieselGeneratorBlockEntity.this.fuelTank.amount();
                case 5 -> DieselGeneratorBlockEntity.this.fuelTank.capacity();
                case 6 -> (int) DieselGeneratorBlockEntity.this.hePerTick();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DieselGeneratorBlockEntity.this.power = value;
                case 1 -> DieselGeneratorBlockEntity.this.powerCap = value;
                case 2 -> DieselGeneratorBlockEntity.this.running = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DieselGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DIESEL_GENERATOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DieselGeneratorBlockEntity diesel) {
        if (level.isClientSide) {
            DieselGeneratorClientSounds.tick(diesel);
            return;
        }
        diesel.tickServer(level);
    }

    public static boolean isBattery(ItemStack stack) {
        return BatteryPackItem.isBattery(stack);
    }

    public static boolean acceptsFuel(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return false;
        }
        CombustibleFuelGrade grade = fluid.combustibleFuelGrade();
        return grade != null && grade != CombustibleFuelGrade.LOW && fluid.combustibleHeatEnergy() > 0;
    }

    public static long heFromFuel(HbmFluidDefinition fluid) {
        if (!acceptsFuel(fluid)) {
            return 0L;
        }
        double efficiency = fuelEfficiency(fluid.combustibleFuelGrade());
        return (long) (fluid.combustibleHeatEnergy() / 1000.0D * efficiency);
    }

    public static double fuelEfficiency(CombustibleFuelGrade grade) {
        return FUEL_EFFICIENCY.getOrDefault(grade, 0.0D);
    }

    public HbmFluidTank fuelTank() {
        return this.fuelTank;
    }

    public boolean running() {
        return this.running;
    }

    public long power() {
        return this.power;
    }

    public long powerCap() {
        return this.powerCap;
    }

    public long hePerTick() {
        return heFromFuel(this.fuelTank.type());
    }

    public boolean hasAcceptableFuel() {
        return acceptsFuel(this.fuelTank.type());
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new DieselFluidHandler();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(
                this.worldPosition.north(),
                this.worldPosition.south(),
                this.worldPosition.east(),
                this.worldPosition.west(),
                this.worldPosition.above(),
                this.worldPosition.below()
        );
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
        return Component.translatable(
                "message.reinhardtshbm.power.diesel_generator",
                hePerTick(),
                this.power,
                this.powerCap
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
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> canDrainIntoFuel(stack);
            case SLOT_BATTERY -> isBattery(stack);
            case SLOT_IDENTIFIER_INPUT -> stack.getItem() instanceof FluidIdentifierItem;
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
        return slot == SLOT_BATTERY && isBattery(stack) && BatteryPackItem.charge(stack) >= BatteryPackItem.capacity(stack);
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
        return Component.translatable("container.reinhardtshbm.machine_diesel");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DieselGeneratorMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData menuData() {
        return this.menuData;
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
        tag.putLong("PowerCap", this.powerCap);
        tag.putBoolean("Running", this.running);
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
        this.powerCap = tag.contains("PowerCap") ? tag.getLong("PowerCap") : BASE_MAX_POWER;
        this.running = tag.getBoolean("Running");
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
        this.powerCap = this.fuelTank.type().name().equals("nitan") ? BASE_MAX_POWER * 10L : BASE_MAX_POWER;
        this.power = BatteryPackItem.chargeFromMachine(this.items.get(SLOT_BATTERY), this.power);
        this.running = false;
        changed |= sendSmoke(level);

        if (!level.hasNeighborSignal(this.worldPosition) && acceptsFuel(this.fuelTank.type()) && this.fuelTank.amount() > 0) {
            this.running = true;
            HbmFluidDefinition burnedFuel = this.fuelTank.type();
            this.fuelTank.drain(this.fuelTank.type(), 1, false);
            // The legacy FluidTank retains its configured type when it reaches
            // zero, so the generator never switches fuel types implicitly.
            if (this.fuelTank.amount() == 0) {
                this.fuelTank.setType(burnedFuel);
            }
            if (level.getGameTime() % 5L == 0L) {
                HbmPollution.bufferedLegacyPolluteFluid(level, this.worldPosition, burnedFuel, HbmPollution.ReleaseType.BURN, 5.0D, this::smokeTank);
            }
            this.power = Math.min(this.powerCap, this.power + hePerTick());
            changed = true;
        }

        PowerNetworkManager.tickFromEndpoint(level, this);
        setLit(this.running);
        if (changed || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private boolean transferFluidInput() {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }
        boolean moved = HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.fuelTank,
                DieselGeneratorBlockEntity::acceptsFuel,
                output -> canPlaceOutput(SLOT_OUTPUT, output),
                output -> placeOutput(SLOT_OUTPUT, output)
        );
        if (moved && input.isEmpty()) {
            this.items.set(SLOT_INPUT, ItemStack.EMPTY);
        }
        return moved;
    }

    /**
     * Mirrors FluidTank#setType(3, 4, slots) from 1.7.10: changing the
     * selected fuel clears the tank and moves the identifier to its output.
     */
    private boolean applyFluidIdentifierSlot() {
        ItemStack identifier = this.items.get(SLOT_IDENTIFIER_INPUT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
        if (this.fuelTank.type() == selected || !this.items.get(SLOT_IDENTIFIER_OUTPUT).isEmpty()) {
            return false;
        }
        this.fuelTank.setType(selected);
        this.items.set(SLOT_IDENTIFIER_OUTPUT, identifier.copy());
        this.items.set(SLOT_IDENTIFIER_INPUT, ItemStack.EMPTY);
        return true;
    }

    private boolean canDrainIntoFuel(ItemStack stack) {
        return FluidUtil.getFluidHandler(stack.copyWithCount(1))
                .map(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE))
                .flatMap(fluidStack -> HbmFluids.fromNeoFluid(fluidStack.getFluid()))
                .filter(DieselGeneratorBlockEntity::acceptsFuel)
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
        if (state.getBlock() instanceof DieselGeneratorBlock && state.getValue(DieselGeneratorBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(DieselGeneratorBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean sendSmoke(Level level) {
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            changed |= HbmPollution.sendSmoke(level, this.worldPosition, this.worldPosition.relative(direction), direction.getOpposite(), this.smokeTank, this.smokeLeadedTank, this.smokePoisonTank);
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

    private static HbmFluidDefinition defaultFuel() {
        return HbmFluids.byName("diesel").orElse(HbmFluids.none());
    }

    private final class DieselFluidHandler implements IFluidHandler {
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
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            return HbmFluids.fromNeoFluid(stack.getFluid()).filter(DieselGeneratorBlockEntity::acceptsFuel).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
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
