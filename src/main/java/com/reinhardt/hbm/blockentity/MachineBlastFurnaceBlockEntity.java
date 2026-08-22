package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.MachineBlastFurnaceBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.menu.MachineBlastFurnaceMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.recipe.BlastFurnaceFuelRecipe;
import com.reinhardt.hbm.recipe.BlastFurnaceRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
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

public class MachineBlastFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int FUEL_SLOT = 0;
    public static final int UPPER_INPUT_SLOT = 1;
    public static final int LOWER_INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int BYPRODUCT_SLOT = 4;
    public static final int INPUT_CONTAINER_SLOT = 5;
    public static final int INPUT_CONTAINER_RESULT_SLOT = 6;
    public static final int OUTPUT_CONTAINER_SLOT = 7;
    public static final int OUTPUT_CONTAINER_RESULT_SLOT = 8;
    public static final int SLOT_COUNT = 9;

    public static final int AIRBLAST_TANK = 0;
    public static final int FLUE_TANK = 1;
    public static final int TANK_CAPACITY_AIRBLAST = 4_000;
    public static final int TANK_CAPACITY_FLUE = 1_000;

    public static final int DATA_COUNT = 10;
    public static final int FUEL_COAL = 200 * 8;
    public static final int FUEL_RATE = 200 * 4;
    public static final int MAX_FUEL = FUEL_COAL * 16;
    public static final int FLUE_GAS_PER_OPERATION = 100;
    private static final int OUTPUT_PER_PORT = 1_000;

    private static final int[] ACCESSIBLE_SLOTS = {
            UPPER_INPUT_SLOT,
            LOWER_INPUT_SLOT,
            FUEL_SLOT,
            OUTPUT_SLOT,
            BYPRODUCT_SLOT,
            INPUT_CONTAINER_SLOT,
            INPUT_CONTAINER_RESULT_SLOT,
            OUTPUT_CONTAINER_SLOT,
            OUTPUT_CONTAINER_RESULT_SLOT
    };
    private static final int[] DOWN_SLOTS = {
            OUTPUT_SLOT,
            BYPRODUCT_SLOT,
            INPUT_CONTAINER_RESULT_SLOT,
            OUTPUT_CONTAINER_RESULT_SLOT
    };

    private ItemStack fuelStack = ItemStack.EMPTY;
    private ItemStack upperInputStack = ItemStack.EMPTY;
    private ItemStack lowerInputStack = ItemStack.EMPTY;
    private ItemStack outputStack = ItemStack.EMPTY;
    private ItemStack byproductStack = ItemStack.EMPTY;
    private ItemStack inputContainerStack = ItemStack.EMPTY;
    private ItemStack inputContainerResultStack = ItemStack.EMPTY;
    private ItemStack outputContainerStack = ItemStack.EMPTY;
    private ItemStack outputContainerResultStack = ItemStack.EMPTY;

    private final HbmFluidTank airblastTank = new HbmFluidTank(airblast(), TANK_CAPACITY_AIRBLAST);
    private final HbmFluidTank flueTank = new HbmFluidTank(flue(), TANK_CAPACITY_FLUE);

    private int fuel;
    private float progress;
    private float speed;
    private boolean progressing;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> MachineBlastFurnaceBlockEntity.this.fuel;
                case 1 -> Math.round(MachineBlastFurnaceBlockEntity.this.progress * 1000.0F);
                case 2 -> Math.round(MachineBlastFurnaceBlockEntity.this.speed * 100.0F);
                case 3 -> MachineBlastFurnaceBlockEntity.this.progressing ? 1 : 0;
                case 4 -> MachineBlastFurnaceBlockEntity.this.airblastTank.type().oldId();
                case 5 -> MachineBlastFurnaceBlockEntity.this.airblastTank.amount();
                case 6 -> MachineBlastFurnaceBlockEntity.this.flueTank.type().oldId();
                case 7 -> MachineBlastFurnaceBlockEntity.this.flueTank.amount();
                case 8 -> MachineBlastFurnaceBlockEntity.this.flueTank.capacity();
                case 9 -> 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> MachineBlastFurnaceBlockEntity.this.fuel = value;
                case 1 -> MachineBlastFurnaceBlockEntity.this.progress = value / 1000.0F;
                case 2 -> MachineBlastFurnaceBlockEntity.this.speed = value / 100.0F;
                case 3 -> MachineBlastFurnaceBlockEntity.this.progressing = value != 0;
                case 4 -> MachineBlastFurnaceBlockEntity.this.airblastTank.setType(HbmFluids.byOldId(value).orElse(airblast()));
                case 6 -> MachineBlastFurnaceBlockEntity.this.flueTank.setType(HbmFluids.byOldId(value).orElse(flue()));
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MachineBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.MACHINE_BLAST_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBlastFurnaceBlockEntity furnace) {
        if (!level.isClientSide) {
            furnace.tickServer(level);
        }
    }

    public static boolean hasExtension(@Nullable Level level, BlockPos pos) {
        return false;
    }

    public static boolean canAcceptRecipeInput(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<BlastFurnaceRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get())) {
            BlastFurnaceRecipe recipe = holder.value();
            if (recipe.inputA().test(stack) || recipe.inputB().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static int fuelPower(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        for (RecipeHolder<BlastFurnaceFuelRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE_FUEL.get())) {
            BlastFurnaceFuelRecipe recipe = holder.value();
            if (recipe.ingredient().test(stack)) {
                return recipe.power();
            }
        }
        return 0;
    }

    public HbmFluidTank airblastTank() {
        return this.airblastTank;
    }

    public HbmFluidTank flueTank() {
        return this.flueTank;
    }

    public float progressFraction() {
        return this.progress;
    }

    public float speedMultiplier() {
        return this.speed;
    }

    public boolean isProgressing() {
        return this.progressing;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new BlastFurnaceFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> fluidPorts(LevelAccessor level) {
        return fluidPortsFor(this.worldPosition, facing());
    }

    public List<Port> automationPorts(LevelAccessor level) {
        return automationPortsFor(this.worldPosition, facing());
    }

    public static List<Port> fluidPortsFor(BlockPos pos, Direction facing) {
        Direction dir = horizontal(facing);
        List<Port> ports = new ArrayList<>(6);
        ports.add(new Port(pos.east(), Direction.EAST));
        ports.add(new Port(pos.west(), Direction.WEST));
        ports.add(new Port(pos.south(), Direction.SOUTH));
        ports.add(new Port(pos.relative(dir).above(3), dir));
        ports.add(new Port(pos.relative(dir).above(5), dir));
        ports.add(new Port(pos.above(6), Direction.UP));
        return List.copyOf(ports);
    }

    public static List<Port> automationPortsFor(BlockPos pos, Direction facing) {
        Direction dir = horizontal(facing);
        List<Port> ports = new ArrayList<>(7);
        ports.add(new Port(pos.east(), Direction.EAST));
        ports.add(new Port(pos.west(), Direction.WEST));
        ports.add(new Port(pos.south(), Direction.SOUTH));
        ports.add(new Port(pos.north(), Direction.NORTH));
        ports.add(new Port(pos.relative(dir).above(3), dir));
        ports.add(new Port(pos.relative(dir).above(5), dir));
        ports.add(new Port(pos.above(6), Direction.UP));
        return List.copyOf(ports);
    }

    public boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : fluidPorts(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    public boolean allowsAutomationPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : automationPorts(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case FUEL_SLOT -> this.fuelStack;
            case UPPER_INPUT_SLOT -> this.upperInputStack;
            case LOWER_INPUT_SLOT -> this.lowerInputStack;
            case OUTPUT_SLOT -> this.outputStack;
            case BYPRODUCT_SLOT -> this.byproductStack;
            case INPUT_CONTAINER_SLOT -> this.inputContainerStack;
            case INPUT_CONTAINER_RESULT_SLOT -> this.inputContainerResultStack;
            case OUTPUT_CONTAINER_SLOT -> this.outputContainerStack;
            case OUTPUT_CONTAINER_RESULT_SLOT -> this.outputContainerResultStack;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            setSlot(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            if (slot == UPPER_INPUT_SLOT || slot == LOWER_INPUT_SLOT) {
                this.progress = 0.0F;
            }
            setChangedAndSync(false);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = getItem(slot);
        setSlot(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack previous = getItem(slot).copy();
        setSlot(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        if (shouldResetInputProgress(slot, previous, stack)) {
            this.progress = 0.0F;
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> this.level != null && fuelPower(this.level, stack) > 0;
            case UPPER_INPUT_SLOT, LOWER_INPUT_SLOT -> canAcceptInputSlot(slot, stack);
            case INPUT_CONTAINER_SLOT -> isDrainableAirblastContainer(stack);
            case OUTPUT_CONTAINER_SLOT -> isFillableFlueContainer(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? DOWN_SLOTS : ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT
                || slot == BYPRODUCT_SLOT
                || slot == INPUT_CONTAINER_RESULT_SLOT
                || slot == OUTPUT_CONTAINER_RESULT_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            setSlot(slot, ItemStack.EMPTY);
        }
        this.progress = 0.0F;
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_blast_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineBlastFurnaceMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            drop(level, pos, getItem(slot));
            setSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FuelStack", this.fuelStack.saveOptional(registries));
        tag.put("UpperInput", this.upperInputStack.saveOptional(registries));
        tag.put("LowerInput", this.lowerInputStack.saveOptional(registries));
        tag.put("Output", this.outputStack.saveOptional(registries));
        tag.put("Byproduct", this.byproductStack.saveOptional(registries));
        tag.put("InputContainer", this.inputContainerStack.saveOptional(registries));
        tag.put("InputContainerResult", this.inputContainerResultStack.saveOptional(registries));
        tag.put("OutputContainer", this.outputContainerStack.saveOptional(registries));
        tag.put("OutputContainerResult", this.outputContainerResultStack.saveOptional(registries));
        tag.putInt("Fuel", this.fuel);
        tag.putFloat("Progress", this.progress);
        tag.putFloat("Speed", this.speed);
        tag.putBoolean("Progressing", this.progressing);
        tag.put("AirblastTank", this.airblastTank.save());
        tag.put("FlueTank", this.flueTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.fuelStack = ItemStack.parseOptional(registries, tag.getCompound("FuelStack"));
        this.upperInputStack = ItemStack.parseOptional(registries, tag.getCompound("UpperInput"));
        this.lowerInputStack = ItemStack.parseOptional(registries, tag.getCompound("LowerInput"));
        this.outputStack = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        this.byproductStack = ItemStack.parseOptional(registries, tag.getCompound("Byproduct"));
        this.inputContainerStack = ItemStack.parseOptional(registries, tag.getCompound("InputContainer"));
        this.inputContainerResultStack = ItemStack.parseOptional(registries, tag.getCompound("InputContainerResult"));
        this.outputContainerStack = ItemStack.parseOptional(registries, tag.getCompound("OutputContainer"));
        this.outputContainerResultStack = ItemStack.parseOptional(registries, tag.getCompound("OutputContainerResult"));
        this.fuel = tag.getInt("Fuel");
        this.progress = tag.getFloat("Progress");
        this.speed = tag.getFloat("Speed");
        this.progressing = tag.getBoolean("Progressing");
        this.airblastTank.load(tag.getCompound("AirblastTank"));
        this.flueTank.load(tag.getCompound("FlueTank"));
        if (this.airblastTank.type().isNone()) {
            this.airblastTank.setType(airblast());
        }
        if (this.flueTank.type().isNone()) {
            this.flueTank.setType(flue());
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
        boolean changed = tickContainers();
        if (tryConsumeFuel(level)) {
            changed = true;
        }

        Optional<BlastFurnaceRecipe.Match> match = findMatch(level);
        this.speed = 0.0F;

        if (match.isPresent() && this.fuel >= FUEL_RATE) {
            this.speed = Math.max(0.5F, Math.min(5.0F, 0.5F + this.airblastTank.amount() * 8.0F / Math.max(1.0F, this.airblastTank.capacity())));
            this.progressing = true;
            this.progress += this.speed / progressDivisor(match.get());

            if (this.progress >= 1.0F) {
                finishProcessing(match.get());
                this.progress = 0.0F;
                this.fuel = Math.max(0, this.fuel - FUEL_RATE);
                int accepted = this.flueTank.fill(flue(), FLUE_GAS_PER_OPERATION, false);
                int overflow = FLUE_GAS_PER_OPERATION - accepted;
                if (overflow > 0) {
                    HbmPollution.polluteFluid(level, this.worldPosition, flue(), HbmPollution.ReleaseType.SPILL, overflow);
                }
                changed = true;
            }
        } else {
            if (this.progress != 0.0F) {
                this.progress = 0.0F;
                changed = true;
            }
            this.progressing = false;
        }

        if (this.airblastTank.amount() > 0) {
            int drained = Math.max(1, (int) Math.floor(this.airblastTank.amount() * 0.05D));
            this.airblastTank.drain(airblast(), drained, false);
            changed = true;
        }

        pushFlueGas(level);
        setLit(level, this.progressing);
        if (changed) {
            setChangedAndSync(false);
        }
    }

    private void pushFlueGas(Level level) {
        if (this.flueTank.amount() <= 0 || this.flueTank.type().isNone()) {
            return;
        }
        for (Port port : fluidPorts(level)) {
            if (this.flueTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(this.flueTank.type(), Math.min(OUTPUT_PER_PORT, this.flueTank.amount()));
            int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), stack, this.worldPosition, true);
            if (accepted > 0) {
                this.flueTank.drain(this.flueTank.type(), accepted, false);
                setChangedAndSync(true);
            }
        }
    }

    private boolean tickContainers() {
        boolean changed = false;
        if (!this.inputContainerStack.isEmpty()) {
            boolean moved = HbmFluidContainerTransfer.drainIntoTank(
                    this.inputContainerStack,
                    this.airblastTank,
                    fluid -> fluid == airblast(),
                    output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output),
                    output -> placeOutput(INPUT_CONTAINER_RESULT_SLOT, output)
            );
            if (moved) {
                if (this.inputContainerStack.isEmpty()) {
                    this.inputContainerStack = ItemStack.EMPTY;
                }
                changed = true;
            }
        }
        if (!this.outputContainerStack.isEmpty()) {
            boolean moved = HbmFluidContainerTransfer.fillFromTank(
                    this.outputContainerStack,
                    this.flueTank,
                    output -> canPlaceOutput(OUTPUT_CONTAINER_RESULT_SLOT, output),
                    output -> placeOutput(OUTPUT_CONTAINER_RESULT_SLOT, output)
            );
            if (moved) {
                if (this.outputContainerStack.isEmpty()) {
                    this.outputContainerStack = ItemStack.EMPTY;
                }
                changed = true;
            }
        }
        return changed;
    }

    private boolean tryConsumeFuel(Level level) {
        int fuelPower = fuelPower(level, this.fuelStack);
        if (this.fuelStack.isEmpty() || fuelPower <= 0 || this.fuel > MAX_FUEL - fuelPower) {
            return false;
        }

        ItemStack remainder = craftingRemaining(this.fuelStack, 1);
        if (!canStoreFuelRemainder(remainder)) {
            return false;
        }

        this.fuel += fuelPower;
        this.fuelStack.shrink(1);
        if (this.fuelStack.isEmpty()) {
            this.fuelStack = ItemStack.EMPTY;
        }
        storeFuelRemainder(remainder);
        return true;
    }

    private Optional<BlastFurnaceRecipe.Match> findMatch(Level level) {
        for (RecipeHolder<BlastFurnaceRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get())) {
            Optional<BlastFurnaceRecipe.Match> match = holder.value().match(this.upperInputStack, this.lowerInputStack);
            if (match.isPresent() && canOutputAll(match.get(), this.upperInputStack, this.lowerInputStack)) {
                return match;
            }
        }
        return Optional.empty();
    }

    private void finishProcessing(BlastFurnaceRecipe.Match match) {
        ItemStack usedUpper = this.upperInputStack.copy();
        usedUpper.setCount(match.upperCount());
        ItemStack usedLower = this.lowerInputStack.copy();
        usedLower.setCount(match.lowerCount());

        this.upperInputStack.shrink(match.upperCount());
        if (this.upperInputStack.isEmpty()) {
            this.upperInputStack = ItemStack.EMPTY;
        }
        this.lowerInputStack.shrink(match.lowerCount());
        if (this.lowerInputStack.isEmpty()) {
            this.lowerInputStack = ItemStack.EMPTY;
        }

        addToOutput(OUTPUT_SLOT, match.result().copy());
        addToOutput(OUTPUT_SLOT, craftingRemaining(usedUpper, match.upperCount()));
        addToOutput(OUTPUT_SLOT, craftingRemaining(usedLower, match.lowerCount()));
    }

    private boolean canOutputAll(BlastFurnaceRecipe.Match match, ItemStack upper, ItemStack lower) {
        ItemStack simulated = this.outputStack.copy();
        simulated = simulateAdd(simulated, match.result());
        if (simulated == null) {
            return false;
        }
        simulated = simulateAdd(simulated, craftingRemaining(upper, match.upperCount()));
        if (simulated == null) {
            return false;
        }
        return simulateAdd(simulated, craftingRemaining(lower, match.lowerCount())) != null;
    }

    @Nullable
    private static ItemStack simulateAdd(ItemStack base, ItemStack addition) {
        if (addition.isEmpty()) {
            return base;
        }
        if (base.isEmpty()) {
            ItemStack copy = addition.copy();
            return copy.getCount() <= copy.getMaxStackSize() ? copy : null;
        }
        if (!ItemStack.isSameItemSameComponents(base, addition)) {
            return null;
        }
        if (base.getCount() + addition.getCount() > base.getMaxStackSize()) {
            return null;
        }
        ItemStack copy = base.copy();
        copy.grow(addition.getCount());
        return copy;
    }

    private void addToOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            setSlot(slot, stack.copy());
        } else if (ItemStack.isSameItemSameComponents(current, stack)) {
            current.grow(stack.getCount());
        }
    }

    private boolean canStoreFuelRemainder(ItemStack remainder) {
        if (remainder.isEmpty()) {
            return true;
        }
        if (this.fuelStack.getCount() <= 1) {
            return true;
        }
        return simulateAdd(this.outputStack, remainder) != null;
    }

    private void storeFuelRemainder(ItemStack remainder) {
        if (remainder.isEmpty()) {
            return;
        }
        if (this.fuelStack.isEmpty()) {
            this.fuelStack = remainder.copy();
        } else {
            addToOutput(OUTPUT_SLOT, remainder);
        }
    }

    private static ItemStack craftingRemaining(ItemStack stack, int count) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack single = stack.copy();
        single.setCount(1);
        Item item = single.getItem();
        if (!item.hasCraftingRemainingItem(single)) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = item.getCraftingRemainingItem(single);
        if (!remainder.isEmpty()) {
            remainder.setCount(remainder.getCount() * count);
        }
        return remainder;
    }

    private boolean canAcceptInputSlot(int slot, ItemStack stack) {
        if (this.level == null || stack.isEmpty() || !canAcceptRecipeInput(this.level, stack)) {
            return false;
        }
        ItemStack other = slot == UPPER_INPUT_SLOT ? this.lowerInputStack : this.upperInputStack;
        return other.isEmpty() || !ItemStack.isSameItemSameComponents(other, stack);
    }

    private boolean shouldResetInputProgress(int slot, ItemStack previous, ItemStack next) {
        if (slot != UPPER_INPUT_SLOT && slot != LOWER_INPUT_SLOT) {
            return false;
        }
        if (this.progress <= 0.0F) {
            return false;
        }
        if (previous.isEmpty() || next.isEmpty()) {
            return true;
        }
        return !ItemStack.isSameItemSameComponents(previous, next);
    }

    private boolean isDrainableAirblastContainer(ItemStack stack) {
        return !stack.isEmpty()
                && HbmFluidContainerTransfer.canDrainIntoTank(
                stack,
                this.airblastTank,
                fluid -> fluid == airblast(),
                output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output)
        );
    }

    private boolean isFillableFlueContainer(ItemStack stack) {
        return !stack.isEmpty()
                && HbmFluidContainerTransfer.canFillFromTank(
                stack,
                this.flueTank,
                output -> canPlaceOutput(OUTPUT_CONTAINER_RESULT_SLOT, output)
        );
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = getItem(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            setSlot(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
    }

    private float progressDivisor(BlastFurnaceRecipe.Match match) {
        int totalInputs = Math.max(1, match.upperCount() + match.lowerCount());
        return 200.0F * totalInputs;
    }

    private void setSlot(int slot, ItemStack stack) {
        switch (slot) {
            case FUEL_SLOT -> this.fuelStack = stack;
            case UPPER_INPUT_SLOT -> this.upperInputStack = stack;
            case LOWER_INPUT_SLOT -> this.lowerInputStack = stack;
            case OUTPUT_SLOT -> this.outputStack = stack;
            case BYPRODUCT_SLOT -> this.byproductStack = stack;
            case INPUT_CONTAINER_SLOT -> this.inputContainerStack = stack;
            case INPUT_CONTAINER_RESULT_SLOT -> this.inputContainerResultStack = stack;
            case OUTPUT_CONTAINER_SLOT -> this.outputContainerStack = stack;
            case OUTPUT_CONTAINER_RESULT_SLOT -> this.outputContainerResultStack = stack;
            default -> {
            }
        }
    }

    private void setLit(Level level, boolean lit) {
        BlockState state = level.getBlockState(this.worldPosition);
        if (state.hasProperty(MachineBlastFurnaceBlock.LIT) && state.getValue(MachineBlastFurnaceBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(MachineBlastFurnaceBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void setChangedAndSync(boolean invalidateCapabilities) {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            if (invalidateCapabilities) {
                this.level.invalidateCapabilities(this.worldPosition);
                for (Port port : fluidPorts(this.level)) {
                    this.level.invalidateCapabilities(port.pos());
                    this.level.invalidateCapabilities(port.connectorPos());
                }
                for (Port port : automationPorts(this.level)) {
                    this.level.invalidateCapabilities(port.pos());
                }
            }
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(MachineBlastFurnaceBlock.FACING) ? state.getValue(MachineBlastFurnaceBlock.FACING) : Direction.NORTH;
    }

    private static Direction horizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
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

    private static HbmFluidDefinition airblast() {
        return HbmFluids.byName("airblast").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition flue() {
        return HbmFluids.byName("flue").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class BlastFurnaceFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private BlastFurnaceFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
                case AIRBLAST_TANK -> airblastTank.getFluidInTank(0);
                case FLUE_TANK -> flueTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case AIRBLAST_TANK -> airblastTank.capacity();
                case FLUE_TANK -> flueTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != AIRBLAST_TANK || stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == airblast();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != airblast()) {
                return 0;
            }
            int filled = airblastTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                setChangedAndSync(true);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != flueTank.type()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = flueTank.drain(fluid, resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                setChangedAndSync(true);
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = flueTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                setChangedAndSync(true);
            }
            return drained;
        }
    }
}
