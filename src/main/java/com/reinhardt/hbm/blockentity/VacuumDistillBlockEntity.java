package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.client.sound.VacuumDistillClientSounds;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.VacuumDistillMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.VacuumDistillRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
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

public class VacuumDistillBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int OUTPUT_CONTAINER_START = 1;
    public static final int OUTPUT_CONTAINER_RESULT_START = 2;
    public static final int FLUID_IDENTIFIER_SLOT = 9;
    public static final int SLOT_COUNT = 10;
    public static final int TANK_COUNT = 5;
    public static final int DATA_COUNT = 21;
    public static final int INPUT_CAPACITY = 64_000;
    public static final int OUTPUT_CAPACITY = 24_000;
    public static final long MAX_POWER = 1_000_000L;

    private static final int PUSH_PER_PORT = 16_000;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] INPUT_SLOTS = {0, 1, 3, 5, 7, 9};
    private static final int[] OUTPUT_SLOTS = {2, 4, 6, 8};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(oil(), INPUT_CAPACITY);
    private final HbmFluidTank[] outputTanks = new HbmFluidTank[4];
    private long power;
    private long lastInput;
    private boolean working;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 5 && index < 17) {
                int flat = index - 5;
                HbmFluidTank tank = outputTanks[flat / 3];
                return switch (flat % 3) {
                    case 0 -> tank.type().oldId();
                    case 1 -> tank.amount();
                    case 2 -> tank.capacity();
                    default -> 0;
                };
            }
            return switch (index) {
                case 0 -> inputTank.type().oldId();
                case 1 -> inputTank.amount();
                case 2 -> inputTank.capacity();
                case 3 -> (int) VacuumDistillBlockEntity.this.power;
                case 4 -> 0;
                case 17 -> (int) VacuumDistillBlockEntity.this.lastInput;
                case 18 -> (int) MAX_POWER;
                case 19 -> VacuumDistillBlockEntity.this.working ? 1 : 0;
                case 20 -> currentRecipe().map(holder -> holder.value().power()).orElse(10_000);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 5 && index < 17) {
                int flat = index - 5;
                HbmFluidTank tank = outputTanks[flat / 3];
                switch (flat % 3) {
                    case 0 -> tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                    case 1 -> tank.setAmount(value);
                    default -> {
                    }
                }
                return;
            }
            switch (index) {
                case 1 -> inputTank.setAmount(value);
                case 3 -> VacuumDistillBlockEntity.this.power = Math.max(0, Math.min(MAX_POWER, value));
                case 17 -> VacuumDistillBlockEntity.this.lastInput = value;
                case 19 -> VacuumDistillBlockEntity.this.working = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public VacuumDistillBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.VACUUM_DISTILL.get(), pos, blockState);
        for (int index = 0; index < this.outputTanks.length; index++) {
            this.outputTanks[index] = new HbmFluidTank(OUTPUT_CAPACITY);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VacuumDistillBlockEntity refinery) {
        if (level.isClientSide) {
            VacuumDistillClientSounds.tick(refinery);
        } else {
            refinery.tickServer(level);
        }
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank(int index) {
        return index >= 0 && index < this.outputTanks.length ? this.outputTanks[index] : this.outputTanks[0];
    }

    public boolean isWorking() {
        return this.working;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new RefineryFluidHandler(queriedPos.immutable(), side);
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
                new Port(pos.offset(1, 0, 1), Direction.EAST),
                new Port(pos.offset(1, 0, -1), Direction.EAST),
                new Port(pos.offset(-1, 0, 1), Direction.WEST),
                new Port(pos.offset(-1, 0, -1), Direction.WEST),
                new Port(pos.offset(1, 0, 1), Direction.SOUTH),
                new Port(pos.offset(-1, 0, 1), Direction.SOUTH),
                new Port(pos.offset(1, 0, -1), Direction.NORTH),
                new Port(pos.offset(-1, 0, -1), Direction.NORTH)
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(8);
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
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
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
        if (slot == FLUID_IDENTIFIER_SLOT) {
            return stack.getItem() instanceof FluidIdentifierItem;
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
        return Component.translatable("container.reinhardtshbm.machine_vacuum_distill");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VacuumDistillMenu(containerId, playerInventory, this, this.menuData);
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
        tag.put("InputTank", this.inputTank.save());
        for (int index = 0; index < this.outputTanks.length; index++) {
            tag.put("OutputTank" + index, this.outputTanks[index].save());
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
        this.inputTank.load(tag.getCompound("InputTank"));
        for (int index = 0; index < this.outputTanks.length; index++) {
            this.outputTanks[index].load(tag.getCompound("OutputTank" + index));
        }
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = tag.getLong("LastInput");
        this.working = tag.getBoolean("Working");
        if (this.inputTank.amount() == 0 && this.inputTank.type().isNone()) {
            this.inputTank.setType(oil());
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
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        applyIdentifierSlot();
        setupTanks();
        tickContainers();
        process();
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
        if (this.inputTank.amount() == 0 || this.inputTank.type() == fluid) {
            this.inputTank.setType(fluid);
        }
    }

    private void setupTanks() {
        Optional<net.minecraft.world.item.crafting.RecipeHolder<VacuumDistillRecipe>> recipe = currentRecipe();
        if (recipe.isEmpty()) {
            for (HbmFluidTank tank : this.outputTanks) {
                if (tank.amount() == 0) {
                    tank.clear();
                }
            }
            return;
        }
        List<VacuumDistillRecipe.FluidOutput> outputs = recipe.get().value().outputs();
        for (int index = 0; index < this.outputTanks.length; index++) {
            if (index < outputs.size()) {
                this.outputTanks[index].setType(outputs.get(index).fluid());
            } else if (this.outputTanks[index].amount() == 0) {
                this.outputTanks[index].clear();
            }
        }
    }

    private void tickContainers() {
        boolean changed = false;
        for (int index = 0; index < this.outputTanks.length; index++) {
            changed |= fillContainerFromOutputTank(index);
        }
        if (changed) {
            sync();
        }
    }

    private boolean fillContainerFromOutputTank(int index) {
        int inputSlot = OUTPUT_CONTAINER_START + index * 2;
        int resultSlot = OUTPUT_CONTAINER_RESULT_START + index * 2;
        ItemStack input = this.items.get(inputSlot);
        HbmFluidTank tank = this.outputTanks[index];
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

    private void process() {
        this.working = false;
        Optional<net.minecraft.world.item.crafting.RecipeHolder<VacuumDistillRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            return;
        }

        VacuumDistillRecipe recipe = holder.get().value();
        if (this.power < recipe.power() || this.inputTank.amount() < recipe.input().amount() || !canFitFluidOutputs(recipe)) {
            return;
        }

        this.power -= recipe.power();
        this.inputTank.drain(recipe.input().fluid(), recipe.input().amount(), false);
        for (int index = 0; index < recipe.outputs().size(); index++) {
            VacuumDistillRecipe.FluidOutput output = recipe.outputs().get(index);
            this.outputTanks[index].fill(output.fluid(), output.amount(), false);
        }
        this.working = true;
    }

    private boolean canFitFluidOutputs(VacuumDistillRecipe recipe) {
        if (recipe.outputs().size() > this.outputTanks.length) {
            return false;
        }
        for (int index = 0; index < recipe.outputs().size(); index++) {
            VacuumDistillRecipe.FluidOutput output = recipe.outputs().get(index);
            HbmFluidTank tank = this.outputTanks[index];
            if (tank.type() != output.fluid() || tank.amount() + output.amount() > tank.capacity()) {
                return false;
            }
        }
        return true;
    }

    private void pushOutputs(Level level) {
        for (HbmFluidTank tank : this.outputTanks) {
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

    private Optional<net.minecraft.world.item.crafting.RecipeHolder<VacuumDistillRecipe>> currentRecipe() {
        if (this.level == null || this.inputTank.type().isNone()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.VACUUM_DISTILL.get())
                .stream()
                .filter(holder -> holder.value().input().fluid() == this.inputTank.type())
                .findFirst();
    }

    private boolean isFillableForOutput(int index, ItemStack stack) {
        if (index < 0 || index >= this.outputTanks.length || stack.isEmpty()) {
            return false;
        }
        HbmFluidTank tank = this.outputTanks[index];
        int resultSlot = OUTPUT_CONTAINER_RESULT_START + index * 2;
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

    private static int outputContainerIndex(int slot) {
        if (slot < OUTPUT_CONTAINER_START || slot > 9 || (slot - OUTPUT_CONTAINER_START) % 2 != 0) {
            return -1;
        }
        return (slot - OUTPUT_CONTAINER_START) / 2;
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

    private static HbmFluidDefinition oil() {
        return HbmFluids.byName("oil").orElse(HbmFluids.none());
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

    public record Port(BlockPos pos, Direction face) {
        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class RefineryFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private RefineryFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return TANK_COUNT;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return inputTank.getFluidInTank(0);
            }
            int output = tank - 1;
            return output >= 0 && output < outputTanks.length ? outputTanks[output].getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) {
                return inputTank.capacity();
            }
            int output = tank - 1;
            return output >= 0 && output < outputTanks.length ? outputTanks[output].capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return !fluid.isNone() && (inputTank.type().isNone() || inputTank.type() == fluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone() || (!inputTank.type().isNone() && inputTank.type() != fluid)) {
                return 0;
            }
            int accepted = inputTank.fill(fluid, resource.getAmount(), action.simulate());
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
            if (fluid.isNone()) {
                return FluidStack.EMPTY;
            }
            for (HbmFluidTank tank : outputTanks) {
                if (tank.type() != fluid) {
                    continue;
                }
                HbmFluidStack drained = tank.drain(fluid, resource.getAmount(), action.simulate());
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
            for (HbmFluidTank tank : outputTanks) {
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

