package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.CatalyticReformerMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ReformingRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CatalyticReformerBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int INPUT_CONTAINER_SLOT = 1;
    public static final int INPUT_CONTAINER_RESULT_SLOT = 2;
    public static final int OUTPUT1_CONTAINER_SLOT = 3;
    public static final int OUTPUT1_CONTAINER_RESULT_SLOT = 4;
    public static final int OUTPUT2_CONTAINER_SLOT = 5;
    public static final int OUTPUT2_CONTAINER_RESULT_SLOT = 6;
    public static final int OUTPUT3_CONTAINER_SLOT = 7;
    public static final int OUTPUT3_CONTAINER_RESULT_SLOT = 8;
    public static final int FLUID_IDENTIFIER_SLOT = 9;
    public static final int CATALYST_SLOT = 10;
    public static final int SLOT_COUNT = 11;

    public static final int TANK_COUNT = 4;
    public static final int INPUT_TANK = 0;
    public static final int OUTPUT1_TANK = 1;
    public static final int OUTPUT2_TANK = 2;
    public static final int OUTPUT3_TANK = 3;
    public static final int DATA_COUNT = 17;
    public static final int INPUT_CAPACITY = 64_000;
    public static final int OUTPUT_CAPACITY = 24_000;
    public static final long MAX_POWER = 1_000_000L;

    private static final int PUSH_PER_PORT = 16_000;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] INPUT_SLOTS = {0, 1, 3, 5, 7, 9, 10};
    private static final int[] OUTPUT_SLOTS = {2, 4, 6, 8};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank[] tanks = new HbmFluidTank[TANK_COUNT];
    private long power;
    private long lastInput;
    private boolean working;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < 12) {
                HbmFluidTank tank = CatalyticReformerBlockEntity.this.tanks[index / 3];
                return switch (index % 3) {
                    case 0 -> tank.type().oldId();
                    case 1 -> tank.amount();
                    case 2 -> tank.capacity();
                    default -> 0;
                };
            }
            return switch (index) {
                case 12 -> (int) CatalyticReformerBlockEntity.this.power;
                case 13 -> (int) CatalyticReformerBlockEntity.this.lastInput;
                case 14 -> (int) MAX_POWER;
                case 15 -> CatalyticReformerBlockEntity.this.working ? 1 : 0;
                case 16 -> currentRecipe().map(holder -> holder.value().power()).orElse(ReformingRecipe.DEFAULT_POWER);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < 12) {
                HbmFluidTank tank = CatalyticReformerBlockEntity.this.tanks[index / 3];
                switch (index % 3) {
                    case 0 -> tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                    case 1 -> tank.setAmount(value);
                    default -> {
                    }
                }
                return;
            }
            switch (index) {
                case 12 -> CatalyticReformerBlockEntity.this.power = Math.max(0L, Math.min(MAX_POWER, value));
                case 13 -> CatalyticReformerBlockEntity.this.lastInput = value;
                case 15 -> CatalyticReformerBlockEntity.this.working = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CatalyticReformerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.CATALYTIC_REFORMER.get(), pos, blockState);
        this.tanks[INPUT_TANK] = new HbmFluidTank(naphtha(), INPUT_CAPACITY);
        this.tanks[OUTPUT1_TANK] = new HbmFluidTank(reformate(), OUTPUT_CAPACITY);
        this.tanks[OUTPUT2_TANK] = new HbmFluidTank(petroleum(), OUTPUT_CAPACITY);
        this.tanks[OUTPUT3_TANK] = new HbmFluidTank(hydrogen(), OUTPUT_CAPACITY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CatalyticReformerBlockEntity reformer) {
        if (!level.isClientSide) {
            reformer.tickServer(level);
        }
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public HbmFluidTank tank(int index) {
        return index >= 0 && index < this.tanks.length ? this.tanks[index] : this.tanks[0];
    }

    public boolean isWorking() {
        return this.working;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new ReformerFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(this.worldPosition, facing());
    }

    public static List<Port> portsFor(BlockPos pos, Direction facing) {
        Direction dir = horizontal(facing);
        Direction rot = dir.getClockWise();
        List<Port> ports = new ArrayList<>(6);
        addPort(ports, pos, dir, 2, rot, 1, dir);
        addPort(ports, pos, dir, 2, rot, -1, dir);
        addPort(ports, pos, dir, -2, rot, 1, dir.getOpposite());
        addPort(ports, pos, dir, -2, rot, -1, dir.getOpposite());
        addPort(ports, pos, dir, 0, rot, 3, rot);
        addPort(ports, pos, dir, 0, rot, -3, rot.getOpposite());
        return List.copyOf(ports);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(6);
        for (Port port : ports(level)) {
            connectors.add(port.connectorPos());
        }
        return List.copyOf(connectors);
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
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return this.power >= MAX_POWER ? 0L : MAX_POWER - this.power;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(String.format("%,d / %,d HE", this.power, MAX_POWER));
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
        setChangedAndSync(false);
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
        int outputIndex = outputContainerIndex(slot);
        if (outputIndex >= 0) {
            return isFillableForOutput(outputIndex, stack);
        }
        return false;
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
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_catalytic_reformer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CatalyticReformerMenu(containerId, playerInventory, this, this.menuData);
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
        for (int index = 0; index < this.tanks.length; index++) {
            tag.put("Tank" + index, this.tanks[index].save());
        }
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putBoolean("Working", this.working);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        for (int index = 0; index < this.tanks.length; index++) {
            this.tanks[index].load(tag.getCompound("Tank" + index));
        }
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = tag.getLong("LastInput");
        this.working = tag.getBoolean("Working");
        if (this.tanks[INPUT_TANK].amount() == 0 && this.tanks[INPUT_TANK].type().isNone()) {
            this.tanks[INPUT_TANK].setType(naphtha());
        }
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
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        applyIdentifierSlot();
        setupTanks();
        tickContainers();
        reform();
        pushOutputs(level);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void applyIdentifierSlot() {
        ItemStack identifier = this.items.get(FLUID_IDENTIFIER_SLOT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        HbmFluidDefinition fluid = FluidIdentifierItem.primary(identifier);
        if (fluid.isNone()) {
            return;
        }
        if (this.tanks[INPUT_TANK].amount() == 0 || this.tanks[INPUT_TANK].type() == fluid) {
            this.tanks[INPUT_TANK].setType(fluid);
        }
    }

    private void setupTanks() {
        Optional<net.minecraft.world.item.crafting.RecipeHolder<ReformingRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            for (int index = OUTPUT1_TANK; index < TANK_COUNT; index++) {
                if (this.tanks[index].amount() == 0) {
                    this.tanks[index].clear();
                }
            }
            return;
        }

        ReformingRecipe recipe = holder.get().value();
        this.tanks[OUTPUT1_TANK].setType(recipe.output1().fluid());
        this.tanks[OUTPUT2_TANK].setType(recipe.output2().fluid());
        this.tanks[OUTPUT3_TANK].setType(recipe.output3().fluid());
    }

    private void tickContainers() {
        boolean changed = false;
        changed |= drainContainerIntoInputTank();
        for (int output = 0; output < 3; output++) {
            changed |= fillContainerFromOutputTank(output);
        }
        if (changed) {
            sync();
        }
    }

    private boolean drainContainerIntoInputTank() {
        ItemStack input = this.items.get(INPUT_CONTAINER_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        boolean changed = HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.tanks[INPUT_TANK],
                fluid -> acceptsInputFluid(fluid) && (this.tanks[INPUT_TANK].type().isNone() || this.tanks[INPUT_TANK].type() == fluid),
                output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output),
                output -> placeOutput(INPUT_CONTAINER_RESULT_SLOT, output)
        );
        if (input.isEmpty()) {
            this.items.set(INPUT_CONTAINER_SLOT, ItemStack.EMPTY);
        }
        return changed;
    }

    private boolean fillContainerFromOutputTank(int outputIndex) {
        int inputSlot = outputContainerSlot(outputIndex);
        int resultSlot = outputContainerResultSlot(outputIndex);
        ItemStack input = this.items.get(inputSlot);
        HbmFluidTank tank = this.tanks[OUTPUT1_TANK + outputIndex];
        boolean changed = HbmFluidContainerTransfer.fillFromTank(
                input,
                tank,
                output -> canPlaceOutput(resultSlot, output),
                output -> placeOutput(resultSlot, output)
        );
        if (input.isEmpty()) {
            this.items.set(inputSlot, ItemStack.EMPTY);
        }
        return changed;
    }

    private void reform() {
        this.working = false;
        Optional<net.minecraft.world.item.crafting.RecipeHolder<ReformingRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            return;
        }

        ReformingRecipe recipe = holder.get().value();
        if (this.power < recipe.power()
                || this.tanks[INPUT_TANK].amount() < recipe.input().amount()
                || !this.items.get(CATALYST_SLOT).is(HbmItems.CATALYTIC_CONVERTER.get())
                || !canFitFluidOutputs(recipe)) {
            return;
        }

        this.power -= recipe.power();
        this.tanks[INPUT_TANK].drain(recipe.input().fluid(), recipe.input().amount(), false);
        fillOutput(OUTPUT1_TANK, recipe.output1());
        fillOutput(OUTPUT2_TANK, recipe.output2());
        fillOutput(OUTPUT3_TANK, recipe.output3());
        this.working = true;
    }

    private boolean canFitFluidOutputs(ReformingRecipe recipe) {
        return hasSpace(OUTPUT1_TANK, recipe.output1())
                && hasSpace(OUTPUT2_TANK, recipe.output2())
                && hasSpace(OUTPUT3_TANK, recipe.output3());
    }

    private boolean hasSpace(int tankIndex, ReformingRecipe.FluidOutput output) {
        if (output.isEmpty()) {
            return true;
        }
        HbmFluidTank tank = this.tanks[tankIndex];
        return tank.type() == output.fluid() && tank.amount() + output.amount() <= tank.capacity();
    }

    private void fillOutput(int tankIndex, ReformingRecipe.FluidOutput output) {
        if (!output.isEmpty()) {
            this.tanks[tankIndex].fill(output.fluid(), output.amount(), false);
        }
    }

    private void pushOutputs(Level level) {
        for (int index = OUTPUT1_TANK; index < TANK_COUNT; index++) {
            HbmFluidTank tank = this.tanks[index];
            if (tank.amount() <= 0 || tank.type().isNone()) {
                continue;
            }
            for (Port port : ports(level)) {
                if (tank.amount() <= 0) {
                    break;
                }
                FluidStack stack = HbmFluids.toNeoStack(tank.type(), Math.min(PUSH_PER_PORT, tank.amount()));
                int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), stack, this.worldPosition, true);
                if (accepted > 0) {
                    tank.drain(tank.type(), accepted, false);
                    sync();
                }
            }
        }
    }

    private Optional<net.minecraft.world.item.crafting.RecipeHolder<ReformingRecipe>> currentRecipe() {
        if (this.level == null || this.tanks[INPUT_TANK].type().isNone()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.REFORMING.get())
                .stream()
                .filter(holder -> holder.value().input().fluid() == this.tanks[INPUT_TANK].type())
                .findFirst();
    }

    private boolean acceptsInputFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || this.level == null) {
            return false;
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.REFORMING.get())
                .stream()
                .anyMatch(holder -> holder.value().input().fluid() == fluid);
    }

    private boolean isDrainableForInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return HbmFluidContainerTransfer.canDrainIntoTank(
                stack,
                this.tanks[INPUT_TANK],
                fluid -> acceptsInputFluid(fluid) && (this.tanks[INPUT_TANK].type().isNone() || this.tanks[INPUT_TANK].type() == fluid),
                output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output)
        );
    }

    private boolean isFillableForOutput(int outputIndex, ItemStack stack) {
        if (outputIndex < 0 || outputIndex >= 3 || stack.isEmpty()) {
            return false;
        }
        HbmFluidTank tank = this.tanks[OUTPUT1_TANK + outputIndex];
        int resultSlot = outputContainerResultSlot(outputIndex);
        return HbmFluidContainerTransfer.canFillFromTank(
                stack,
                tank,
                output -> canPlaceOutput(resultSlot, output)
        );
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

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : ports(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void sync() {
        setChangedAndSync(true);
        if (this.level != null && !this.level.isClientSide) {
            for (Port port : ports(this.level)) {
                this.level.invalidateCapabilities(port.pos());
                this.level.invalidateCapabilities(port.connectorPos());
            }
        }
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (sync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    private static int outputContainerIndex(int slot) {
        return switch (slot) {
            case OUTPUT1_CONTAINER_SLOT -> 0;
            case OUTPUT2_CONTAINER_SLOT -> 1;
            case OUTPUT3_CONTAINER_SLOT -> 2;
            default -> -1;
        };
    }

    private static int outputContainerSlot(int outputIndex) {
        return OUTPUT1_CONTAINER_SLOT + outputIndex * 2;
    }

    private static int outputContainerResultSlot(int outputIndex) {
        return OUTPUT1_CONTAINER_RESULT_SLOT + outputIndex * 2;
    }

    private static void addPort(List<Port> ports, BlockPos core, Direction first, int firstDistance, Direction second, int secondDistance, Direction face) {
        BlockPos connector = core.offset(
                first.getStepX() * firstDistance + second.getStepX() * secondDistance,
                0,
                first.getStepZ() * firstDistance + second.getStepZ() * secondDistance
        );
        ports.add(Port.fromConnector(connector, face));
    }

    private static Direction horizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
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

    private static HbmFluidDefinition naphtha() {
        return HbmFluids.byName("naphtha").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition reformate() {
        return HbmFluids.byName("reformate").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition petroleum() {
        return HbmFluids.byName("petroleum").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition hydrogen() {
        return HbmFluids.byName("hydrogen").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class ReformerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private ReformerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
            if (tank != INPUT_TANK || stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return acceptsInputFluid(fluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!acceptsInputFluid(fluid)
                    || (!tanks[INPUT_TANK].type().isNone() && tanks[INPUT_TANK].type() != fluid)) {
                return 0;
            }
            int accepted = tanks[INPUT_TANK].fill(fluid, resource.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) {
                setupTanks();
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
            if (fluid.isNone()) {
                return FluidStack.EMPTY;
            }
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
            if (maxDrain <= 0 || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            for (int index = OUTPUT1_TANK; index < TANK_COUNT; index++) {
                HbmFluidTank tank = tanks[index];
                if (tank.amount() <= 0 || tank.type().isNone()) {
                    continue;
                }
                HbmFluidDefinition fluid = tank.type();
                HbmFluidStack drained = tank.drain(fluid, maxDrain, action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        sync();
                    }
                    return HbmFluids.toNeoStack(fluid, drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
