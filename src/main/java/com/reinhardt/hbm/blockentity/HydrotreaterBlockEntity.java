package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.HydrotreaterMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.HydrotreatingRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HydrotreaterBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int INPUT_CONTAINER_SLOT = 1;
    public static final int INPUT_CONTAINER_RESULT_SLOT = 2;
    public static final int HYDROGEN_CONTAINER_SLOT = 3;
    public static final int HYDROGEN_CONTAINER_RESULT_SLOT = 4;
    public static final int OUTPUT1_CONTAINER_SLOT = 5;
    public static final int OUTPUT1_CONTAINER_RESULT_SLOT = 6;
    public static final int OUTPUT2_CONTAINER_SLOT = 7;
    public static final int OUTPUT2_CONTAINER_RESULT_SLOT = 8;
    public static final int FLUID_IDENTIFIER_SLOT = 9;
    public static final int CATALYST_SLOT = 10;
    public static final int SLOT_COUNT = 11;

    public static final int INPUT_TANK = 0;
    public static final int HYDROGEN_TANK = 1;
    public static final int OUTPUT1_TANK = 2;
    public static final int OUTPUT2_TANK = 3;
    public static final int TANK_COUNT = 4;
    public static final int DATA_COUNT = 17;
    public static final int INPUT_CAPACITY = 64_000;
    public static final int OUTPUT_CAPACITY = 24_000;
    public static final long MAX_POWER = 1_000_000L;

    private static final int PUSH_PER_PORT = 16_000;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] INPUT_SLOTS = {0, 1, 5, 7, 9, 10};
    private static final int[] OUTPUT_SLOTS = {2, 4, 6, 8};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank[] tanks = new HbmFluidTank[TANK_COUNT];
    private long power;
    private long lastInput;
    private boolean working;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < 12) {
                HbmFluidTank tank = tanks[index / 3];
                return switch (index % 3) {
                    case 0 -> tank.type().oldId();
                    case 1 -> tank.amount();
                    default -> tank.capacity();
                };
            }
            return switch (index) {
                case 12 -> (int) power;
                case 13 -> (int) lastInput;
                case 14 -> (int) MAX_POWER;
                case 15 -> working ? 1 : 0;
                case 16 -> currentRecipe().map(recipe -> recipe.value().power()).orElse(HydrotreatingRecipe.DEFAULT_POWER);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index < 12) {
                HbmFluidTank tank = tanks[index / 3];
                switch (index % 3) {
                    case 0 -> tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                    case 1 -> tank.setAmount(value);
                    default -> {
                    }
                }
                return;
            }
            switch (index) {
                case 12 -> power = Math.max(0L, Math.min(MAX_POWER, value));
                case 13 -> lastInput = value;
                case 15 -> working = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public HydrotreaterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.HYDROTREATER.get(), pos, state);
        tanks[INPUT_TANK] = new HbmFluidTank(oil(), INPUT_CAPACITY);
        tanks[HYDROGEN_TANK] = new HbmFluidTank(hydrogen(), INPUT_CAPACITY);
        tanks[HYDROGEN_TANK].setPressure(1);
        tanks[OUTPUT1_TANK] = new HbmFluidTank(OUTPUT_CAPACITY);
        tanks[OUTPUT2_TANK] = new HbmFluidTank(OUTPUT_CAPACITY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HydrotreaterBlockEntity machine) {
        if (!level.isClientSide) {
            machine.tickServer(level);
        }
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public HbmFluidTank tank(int index) {
        return index >= 0 && index < TANK_COUNT ? tanks[index] : tanks[0];
    }

    public boolean isWorking() {
        return working;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        return allowsFluidPort(queriedPos, side) ? new HydroFluidHandler(queriedPos.immutable(), side) : null;
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(worldPosition, facing());
    }

    public static List<Port> portsFor(BlockPos pos, Direction facing) {
        List<Port> ports = new ArrayList<>(8);
        addPort(ports, pos, 2, 1, Direction.EAST);
        addPort(ports, pos, 2, -1, Direction.EAST);
        addPort(ports, pos, -2, 1, Direction.WEST);
        addPort(ports, pos, -2, -1, Direction.WEST);
        addPort(ports, pos, 1, 2, Direction.SOUTH);
        addPort(ports, pos, -1, 2, Direction.SOUTH);
        addPort(ports, pos, 1, -2, Direction.NORTH);
        addPort(ports, pos, -1, -2, Direction.NORTH);
        return List.copyOf(ports);
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
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
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return power >= MAX_POWER ? 0L : MAX_POWER - power;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        power = Math.min(MAX_POWER, power + receivedInput);
        lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(String.format("%,d / %,d HE", power, MAX_POWER));
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return validSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync(false);
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot == INPUT_CONTAINER_SLOT) {
            return isDrainableForInput(stack);
        }
        if (slot == FLUID_IDENTIFIER_SLOT) {
            return stack.getItem() instanceof FluidIdentifierItem;
        }
        if (slot == CATALYST_SLOT) {
            return stack.is(HbmItems.CATALYTIC_CONVERTER.get());
        }
        int output = outputIndex(slot);
        return output >= 0 && isFillableForOutput(output, stack);
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
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int index = 0; index < items.size(); index++) {
            items.set(index, ItemStack.EMPTY);
        }
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_hydrotreater");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new HydrotreaterMenu(id, inventory, this, menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int index = 0; index < items.size(); index++) {
            if (!items.get(index).isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, items.get(index).copy()));
                items.set(index, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int index = 0; index < items.size(); index++) {
            tag.put("Slot" + index, items.get(index).saveOptional(registries));
        }
        for (int index = 0; index < tanks.length; index++) {
            tag.put("Tank" + index, tanks[index].save());
        }
        tag.putLong("Power", power);
        tag.putLong("LastInput", lastInput);
        tag.putBoolean("Working", working);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int index = 0; index < items.size(); index++) {
            items.set(index, ItemStack.parseOptional(registries, tag.getCompound("Slot" + index)));
        }
        for (int index = 0; index < tanks.length; index++) {
            tanks[index].load(tag.getCompound("Tank" + index));
        }
        power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        lastInput = tag.getLong("LastInput");
        working = tag.getBoolean("Working");
        tanks[HYDROGEN_TANK].conform(hydrogen(), 1);
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
        PowerNetworkManager.tickFromEndpoint(level, this);
        power = BatteryPackItem.dischargeIntoMachine(items.get(BATTERY_SLOT), power, MAX_POWER);
        applyIdentifierSlot();
        setupTanks();
        tickContainers();
        if (level.getGameTime() % 2L == 0L) {
            process();
        }
        pushOutputs(level);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void applyIdentifierSlot() {
        if (!(items.get(FLUID_IDENTIFIER_SLOT).getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        HbmFluidDefinition fluid = FluidIdentifierItem.primary(items.get(FLUID_IDENTIFIER_SLOT));
        if (!fluid.isNone() && (tanks[INPUT_TANK].amount() == 0 || tanks[INPUT_TANK].type() == fluid)) {
            tanks[INPUT_TANK].setType(fluid);
        }
    }

    private void setupTanks() {
        tanks[HYDROGEN_TANK].conform(hydrogen(), 1);
        Optional<net.minecraft.world.item.crafting.RecipeHolder<HydrotreatingRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            for (int index = OUTPUT1_TANK; index < TANK_COUNT; index++) {
                if (tanks[index].amount() == 0) {
                    tanks[index].clear();
                }
            }
            return;
        }
        HydrotreatingRecipe recipe = holder.get().value();
        tanks[OUTPUT1_TANK].setType(recipe.output1().fluid());
        tanks[OUTPUT2_TANK].setType(recipe.output2().fluid());
    }

    private void tickContainers() {
        boolean changed = drainContainerIntoInputTank();
        changed |= fillContainerFromOutputTank(0);
        changed |= fillContainerFromOutputTank(1);
        if (changed) {
            sync();
        }
    }

    private boolean drainContainerIntoInputTank() {
        ItemStack input = items.get(INPUT_CONTAINER_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        boolean changed = HbmFluidContainerTransfer.drainIntoTank(
                input, tanks[INPUT_TANK],
                fluid -> acceptsInputFluid(fluid) && (tanks[INPUT_TANK].type().isNone() || tanks[INPUT_TANK].type() == fluid),
                output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output),
                output -> placeOutput(INPUT_CONTAINER_RESULT_SLOT, output));
        if (input.isEmpty()) {
            items.set(INPUT_CONTAINER_SLOT, ItemStack.EMPTY);
        }
        return changed;
    }

    private boolean fillContainerFromOutputTank(int outputIndex) {
        int inputSlot = outputIndex == 0 ? OUTPUT1_CONTAINER_SLOT : OUTPUT2_CONTAINER_SLOT;
        int resultSlot = outputIndex == 0 ? OUTPUT1_CONTAINER_RESULT_SLOT : OUTPUT2_CONTAINER_RESULT_SLOT;
        ItemStack input = items.get(inputSlot);
        boolean changed = HbmFluidContainerTransfer.fillFromTank(
                input, tanks[OUTPUT1_TANK + outputIndex],
                output -> canPlaceOutput(resultSlot, output),
                output -> placeOutput(resultSlot, output));
        if (input.isEmpty()) {
            items.set(inputSlot, ItemStack.EMPTY);
        }
        return changed;
    }

    private void process() {
        working = false;
        Optional<net.minecraft.world.item.crafting.RecipeHolder<HydrotreatingRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            return;
        }
        HydrotreatingRecipe recipe = holder.get().value();
        if (power < recipe.power()
                || tanks[INPUT_TANK].amount() < recipe.input().amount()
                || tanks[HYDROGEN_TANK].amount() < recipe.hydrogen().amount()
                || tanks[HYDROGEN_TANK].pressure() != recipe.hydrogen().pressure()
                || !items.get(CATALYST_SLOT).is(HbmItems.CATALYTIC_CONVERTER.get())
                || !hasSpace(OUTPUT1_TANK, recipe.output1())
                || !hasSpace(OUTPUT2_TANK, recipe.output2())) {
            return;
        }
        power -= recipe.power();
        tanks[INPUT_TANK].drain(recipe.input().fluid(), recipe.input().amount(), false);
        tanks[HYDROGEN_TANK].drain(recipe.hydrogen().fluid(), recipe.hydrogen().amount(), false);
        fillOutput(OUTPUT1_TANK, recipe.output1());
        fillOutput(OUTPUT2_TANK, recipe.output2());
        working = true;
    }

    private boolean hasSpace(int tankIndex, HydrotreatingRecipe.FluidOutput output) {
        if (output.isEmpty()) {
            return true;
        }
        HbmFluidTank tank = tanks[tankIndex];
        return tank.type() == output.fluid() && tank.amount() + output.amount() <= tank.capacity();
    }

    private void fillOutput(int tankIndex, HydrotreatingRecipe.FluidOutput output) {
        if (!output.isEmpty()) {
            tanks[tankIndex].fill(output.fluid(), output.amount(), 0, false);
        }
    }

    private void pushOutputs(Level level) {
        for (int index = OUTPUT1_TANK; index < TANK_COUNT; index++) {
            HbmFluidTank tank = tanks[index];
            if (tank.amount() <= 0 || tank.type().isNone()) {
                continue;
            }
            for (Port port : ports(level)) {
                if (tank.amount() <= 0) {
                    break;
                }
                FluidStack offered = HbmFluids.toNeoStack(tank.type(), Math.min(PUSH_PER_PORT, tank.amount()));
                int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), offered, worldPosition, true);
                if (accepted > 0) {
                    tank.drain(tank.type(), accepted, false);
                }
            }
        }
    }

    private Optional<net.minecraft.world.item.crafting.RecipeHolder<HydrotreatingRecipe>> currentRecipe() {
        if (level == null || tanks[INPUT_TANK].type().isNone()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.HYDROTREATING.get()).stream()
                .filter(holder -> holder.value().input().fluid() == tanks[INPUT_TANK].type())
                .findFirst();
    }

    private boolean acceptsInputFluid(HbmFluidDefinition fluid) {
        return fluid != null && !fluid.isNone() && level != null
                && level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.HYDROTREATING.get()).stream()
                .anyMatch(holder -> holder.value().input().fluid() == fluid);
    }

    private boolean isDrainableForInput(ItemStack stack) {
        return !stack.isEmpty() && HbmFluidContainerTransfer.canDrainIntoTank(
                stack, tanks[INPUT_TANK],
                fluid -> acceptsInputFluid(fluid) && (tanks[INPUT_TANK].type().isNone() || tanks[INPUT_TANK].type() == fluid),
                output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output));
    }

    private boolean isFillableForOutput(int outputIndex, ItemStack stack) {
        int inputSlot = outputIndex == 0 ? OUTPUT1_CONTAINER_SLOT : OUTPUT2_CONTAINER_SLOT;
        int resultSlot = outputIndex == 0 ? OUTPUT1_CONTAINER_RESULT_SLOT : OUTPUT2_CONTAINER_RESULT_SLOT;
        return outputIndex >= 0 && outputIndex < 2 && !stack.isEmpty()
                && HbmFluidContainerTransfer.canFillFromTank(stack, tanks[OUTPUT1_TANK + outputIndex], output -> canPlaceOutput(resultSlot, output));
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = items.get(slot);
        return current.isEmpty() || (ItemStack.isSameItemSameComponents(current, stack)
                && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = items.get(slot);
        if (current.isEmpty()) {
            items.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : ports(level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void applySync() {
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void setChangedAndSync(boolean sync) {
        if (sync) {
            applySync();
        } else {
            setChanged();
        }
    }

    private void sync() {
        applySync();
        if (level != null && !level.isClientSide) {
            for (Port port : ports(level)) {
                level.invalidateCapabilities(port.pos());
                level.invalidateCapabilities(port.connectorPos());
            }
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    private static void addPort(List<Port> ports, BlockPos core, int x, int z, Direction face) {
        BlockPos connector = core.offset(x, 0, z);
        ports.add(new Port(connector.relative(face.getOpposite()).immutable(), face));
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static boolean contains(int[] values, int slot) {
        for (int value : values) {
            if (value == slot) {
                return true;
            }
        }
        return false;
    }

    private static int outputIndex(int slot) {
        return switch (slot) {
            case OUTPUT1_CONTAINER_SLOT -> 0;
            case OUTPUT2_CONTAINER_SLOT -> 1;
            default -> -1;
        };
    }

    private static HbmFluidDefinition oil() {
        return HbmFluids.byName("oil").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition hydrogen() {
        return HbmFluids.byName("hydrogen").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        public BlockPos connectorPos() {
            return pos.relative(face).immutable();
        }
    }

    private final class HydroFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private HydroFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return TANK_COUNT;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank >= 0 && tank < TANK_COUNT ? tanks[tank].getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < TANK_COUNT ? tanks[tank].capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (stack.isEmpty() || !allowsFluidPort(queriedPos, side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return tank == INPUT_TANK ? acceptsInputFluid(fluid) : tank == HYDROGEN_TANK && fluid == hydrogen();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(queriedPos, side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            int tankIndex;
            int pressure;
            if (fluid == hydrogen()) {
                tankIndex = HYDROGEN_TANK;
                pressure = 1;
            } else if (acceptsInputFluid(fluid)) {
                tankIndex = INPUT_TANK;
                pressure = 0;
            } else {
                return 0;
            }
            HbmFluidTank tank = tanks[tankIndex];
            if (!tank.type().isNone() && tank.type() != fluid) {
                return 0;
            }
            int accepted = tank.fill(fluid, resource.getAmount(), pressure, action.simulate());
            if (accepted > 0 && action.execute()) {
                setupTanks();
                sync();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(queriedPos, side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            for (int index = OUTPUT1_TANK; index < TANK_COUNT; index++) {
                if (tanks[index].type() != fluid) {
                    continue;
                }
                HbmFluidStack drained = tanks[index].drain(fluid, resource.getAmount(), action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        sync();
                    }
                    return HbmFluids.toNeoStack(fluid, drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsFluidPort(queriedPos, side)) {
                return FluidStack.EMPTY;
            }
            for (int index = OUTPUT1_TANK; index < TANK_COUNT; index++) {
                HbmFluidTank tank = tanks[index];
                if (tank.amount() <= 0 || tank.type().isNone()) {
                    continue;
                }
                HbmFluidStack drained = tank.drain(tank.type(), maxDrain, action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        sync();
                    }
                    return HbmFluids.toNeoStack(drained.type(), drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
