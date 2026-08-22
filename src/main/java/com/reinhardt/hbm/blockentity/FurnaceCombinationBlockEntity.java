package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.menu.FurnaceCombinationMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.recipe.CombinationOvenRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FurnaceCombinationBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int ITEM_OUTPUT_SLOT = 1;
    public static final int CONTAINER_INPUT_SLOT = 2;
    public static final int CONTAINER_OUTPUT_SLOT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_COUNT = 7;
    public static final int TANK_CAPACITY = 24_000;
    public static final int PROCESS_TIME = 20_000;
    public static final int MAX_HEAT = 100_000;
    private static final int SMOKE_BUFFER_CAPACITY = 50;
    private static final double DIFFUSION = 0.25D;
    private static final int PUSH_PER_PORT = 8_000;
    private static final int[] ACCESSIBLE_SLOTS = {INPUT_SLOT, ITEM_OUTPUT_SLOT};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank tank = new HbmFluidTank(HbmFluids.none(), TANK_CAPACITY);
    private final HbmFluidTank smokeTank = new HbmFluidTank(smokeFluid(), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokeLeadedTank = new HbmFluidTank(smokeLeadedFluid(), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokePoisonTank = new HbmFluidTank(smokePoisonFluid(), SMOKE_BUFFER_CAPACITY);
    private int progress;
    private int heat;
    private boolean working;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> FurnaceCombinationBlockEntity.this.tank.type().oldId();
                case 1 -> FurnaceCombinationBlockEntity.this.tank.amount();
                case 2 -> FurnaceCombinationBlockEntity.this.tank.capacity();
                case 3 -> FurnaceCombinationBlockEntity.this.progress;
                case 4 -> PROCESS_TIME;
                case 5 -> FurnaceCombinationBlockEntity.this.heat;
                case 6 -> MAX_HEAT;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> FurnaceCombinationBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> FurnaceCombinationBlockEntity.this.tank.setAmount(value);
                case 3 -> FurnaceCombinationBlockEntity.this.progress = value;
                case 5 -> FurnaceCombinationBlockEntity.this.heat = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FurnaceCombinationBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FURNACE_COMBINATION.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FurnaceCombinationBlockEntity furnace) {
        if (level.isClientSide) {
            furnace.tickClient(level);
        } else {
            furnace.tickServer(level);
        }
    }

    public HbmFluidTank tank() {
        return this.tank;
    }

    public int progress() {
        return this.progress;
    }

    public int heat() {
        return this.heat;
    }

    public boolean isWorking() {
        return this.working;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new OutputFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(this.worldPosition);
    }

    public static List<Port> portsFor(BlockPos pos) {
        ArrayList<Port> ports = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Direction tangent = direction.getClockWise();
            for (int y = 0; y <= 1; y++) {
                for (int offset = -1; offset <= 1; offset++) {
                    BlockPos connector = pos.relative(direction, 2)
                            .relative(tangent, offset)
                            .above(y);
                    ports.add(Port.fromConnector(connector, direction));
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                ports.add(Port.fromConnector(pos.offset(x, 2, z), Direction.UP));
            }
        }
        return List.copyOf(ports);
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
        return switch (slot) {
            case INPUT_SLOT -> recipeFor(stack).isPresent();
            case CONTAINER_INPUT_SLOT -> isFillableContainer(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == ITEM_OUTPUT_SLOT;
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
        return Component.translatable("container.reinhardtshbm.furnace_combination");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FurnaceCombinationMenu(containerId, playerInventory, this, this.menuData);
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
        tag.put("Tank", this.tank.save());
        tag.put("Smoke", this.smokeTank.save());
        tag.put("SmokeLeaded", this.smokeLeadedTank.save());
        tag.put("SmokePoison", this.smokePoisonTank.save());
        tag.putInt("Progress", this.progress);
        tag.putInt("Heat", this.heat);
        tag.putBoolean("Working", this.working);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.tank.load(tag.getCompound("Tank"));
        this.smokeTank.load(tag.getCompound("Smoke"));
        this.smokeLeadedTank.load(tag.getCompound("SmokeLeaded"));
        this.smokePoisonTank.load(tag.getCompound("SmokePoison"));
        this.progress = tag.getInt("Progress");
        this.heat = Math.max(0, Math.min(MAX_HEAT, tag.getInt("Heat")));
        this.working = tag.getBoolean("Working");
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
        tryPullHeat(level);
        tickContainerSlot();
        setupTank();
        process(level);
        if (level.getGameTime() % 20L == 0L) {
            pushOutput(level);
            pushSmoke(level);
        }
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level) {
        if (this.working && level.random.nextInt(15) == 0) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    this.worldPosition.getX() + 0.5D + level.random.nextGaussian() * 0.5D,
                    this.worldPosition.getY() + 2.0D,
                    this.worldPosition.getZ() + 0.5D + level.random.nextGaussian() * 0.5D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private void tryPullHeat(Level level) {
        if (this.heat >= MAX_HEAT) {
            return;
        }

        HeatSourceBlockEntity source = heatSourceBelow(level);
        if (source != null) {
            int diff = source.getHeatStored() - this.heat;
            if (diff == 0) {
                return;
            }
            if (diff > 0) {
                int pulled = (int) Math.ceil(diff * DIFFUSION);
                source.useHeat(pulled);
                this.heat = Math.min(MAX_HEAT, this.heat + pulled);
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    @Nullable
    private HeatSourceBlockEntity heatSourceBelow(Level level) {
        BlockEntity below = level.getBlockEntity(this.worldPosition.below());
        if (below instanceof HeatSourceBlockEntity source) {
            return source;
        }
        if (below instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof HeatSourceBlockEntity source) {
            return source;
        }
        return null;
    }

    private void tickContainerSlot() {
        ItemStack input = this.items.get(CONTAINER_INPUT_SLOT);
        boolean changed = HbmFluidContainerTransfer.fillFromTank(
                input,
                this.tank,
                output -> canPlaceOutput(CONTAINER_OUTPUT_SLOT, output),
                output -> placeOutput(CONTAINER_OUTPUT_SLOT, output)
        );
        if (input.isEmpty()) {
            this.items.set(CONTAINER_INPUT_SLOT, ItemStack.EMPTY);
        }
        if (changed) {
            sync();
        }
    }

    private void setupTank() {
        Optional<net.minecraft.world.item.crafting.RecipeHolder<CombinationOvenRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            if (this.tank.amount() == 0) {
                this.tank.clear();
            }
            return;
        }
        CombinationOvenRecipe recipe = holder.get().value();
        if (recipe.hasFluid() && (this.tank.amount() == 0 || this.tank.type() == recipe.fluid().fluid())) {
            this.tank.setType(recipe.fluid().fluid());
        } else if (!recipe.hasFluid() && this.tank.amount() == 0) {
            this.tank.clear();
        }
    }

    private void process(Level level) {
        this.working = false;
        Optional<net.minecraft.world.item.crafting.RecipeHolder<CombinationOvenRecipe>> holder = currentRecipe();
        if (holder.isEmpty() || !canProcess(holder.get().value())) {
            this.progress = 0;
            return;
        }

        int burn = this.heat / 100;
        if (burn <= 0) {
            return;
        }

        this.working = true;
        this.progress += burn;
        this.heat -= burn;
        applyWorkingEffects(level);
        if (level.getGameTime() % 20L == 0L) {
            bufferedPollute(level, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND * 3.0D);
        }

        if (this.progress < PROCESS_TIME) {
            return;
        }

        this.progress -= PROCESS_TIME;
        CombinationOvenRecipe recipe = holder.get().value();
        this.items.get(INPUT_SLOT).shrink(1);
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        placeOutput(ITEM_OUTPUT_SLOT, recipe.output());
        if (recipe.hasFluid()) {
            if (this.tank.type() != recipe.fluid().fluid()) {
                this.tank.setType(recipe.fluid().fluid());
            }
            this.tank.fill(recipe.fluid().fluid(), recipe.fluid().amount(), false);
        }
        sync();
    }

    private boolean canProcess(CombinationOvenRecipe recipe) {
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            return false;
        }
        if (!canPlaceOutput(ITEM_OUTPUT_SLOT, recipe.output())) {
            return false;
        }
        if (!recipe.hasFluid()) {
            return true;
        }
        HbmFluidDefinition fluid = recipe.fluid().fluid();
        if (this.tank.type() != fluid && this.tank.amount() > 0) {
            return false;
        }
        return this.tank.amount() + recipe.fluid().amount() <= this.tank.capacity();
    }

    private void applyWorkingEffects(Level level) {
        AABB fireBox = new AABB(
                this.worldPosition.getX() - 0.5D,
                this.worldPosition.getY() + 2.0D,
                this.worldPosition.getZ() - 0.5D,
                this.worldPosition.getX() + 1.5D,
                this.worldPosition.getY() + 4.0D,
                this.worldPosition.getZ() + 1.5D
        );
        for (Entity entity : level.getEntities(null, fireBox)) {
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 100));
        }
        if (level.getGameTime() % 10L == 0L) {
            level.playSound(
                    null,
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 1.0D,
                    this.worldPosition.getZ() + 0.5D,
                    HbmSoundEvents.FLAMETHROWER_SHOOT.get(),
                    SoundSource.BLOCKS,
                    0.25F,
                    0.5F
            );
        }
    }

    private void pushOutput(Level level) {
        if (this.tank.amount() <= 0 || this.tank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (this.tank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(this.tank.type(), Math.min(PUSH_PER_PORT, this.tank.amount()));
            int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), stack, this.worldPosition, true);
            if (accepted > 0) {
                this.tank.drain(this.tank.type(), accepted, false);
                sync();
            }
        }
    }

    private void pushSmoke(Level level) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Direction tangent = direction.getClockWise();
            for (int y = 0; y <= 1; y++) {
                for (int offset = -1; offset <= 1; offset++) {
                    BlockPos connector = this.worldPosition.relative(direction, 2).relative(tangent, offset).above(y);
                    sendSmokeTo(level, connector, direction.getOpposite(), this.smokeTank);
                    sendSmokeTo(level, connector, direction.getOpposite(), this.smokeLeadedTank);
                    sendSmokeTo(level, connector, direction.getOpposite(), this.smokePoisonTank);
                }
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos connector = this.worldPosition.offset(x, 2, z);
                sendSmokeTo(level, connector, Direction.DOWN, this.smokeTank);
                sendSmokeTo(level, connector, Direction.DOWN, this.smokeLeadedTank);
                sendSmokeTo(level, connector, Direction.DOWN, this.smokePoisonTank);
            }
        }
    }

    private void sendSmokeTo(Level level, BlockPos connector, Direction side, HbmFluidTank smoke) {
        if (smoke.amount() <= 0 || smoke.type().isNone()) {
            return;
        }
        FluidStack stack = HbmFluids.toNeoStack(smoke.type(), smoke.amount());
        int accepted = HbmFluidNetworks.fillInto(level, connector, side, stack, this.worldPosition, true);
        if (accepted > 0) {
            smoke.drain(smoke.type(), accepted, false);
            sync();
        }
    }

    private void bufferedPollute(Level level, HbmPollutionType type, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        HbmFluidTank smoke = smokeTank(type);
        int fluidAmount = (int) Math.ceil(amount * 100.0D);
        int accepted = smoke.fill(smokeFluid(type), fluidAmount, false);
        int overflow = fluidAmount - accepted;
        if (overflow > 0) {
            HbmPollution.increment(level, this.worldPosition, type, overflow / 100.0D);
        }
    }

    private Optional<net.minecraft.world.item.crafting.RecipeHolder<CombinationOvenRecipe>> currentRecipe() {
        return recipeFor(this.items.get(INPUT_SLOT));
    }

    private Optional<net.minecraft.world.item.crafting.RecipeHolder<CombinationOvenRecipe>> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.COMBINATION_OVEN.get())
                .stream()
                .filter(holder -> holder.value().matches(input, this.level))
                .findFirst();
    }

    private boolean isFillableContainer(ItemStack stack) {
        return !stack.isEmpty()
                && HbmFluidContainerTransfer.canFillFromTank(
                stack,
                this.tank,
                output -> canPlaceOutput(CONTAINER_OUTPUT_SLOT, output)
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

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
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

    private static HbmFluidDefinition smokeFluid() {
        return HbmFluids.byName("smoke").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition smokeLeadedFluid() {
        return HbmFluids.byName("smoke_leaded").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition smokePoisonFluid() {
        return HbmFluids.byName("smoke_poison").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition smokeFluid(HbmPollutionType type) {
        return switch (type) {
            case SOOT -> smokeFluid();
            case HEAVYMETAL -> smokeLeadedFluid();
            case POISON -> smokePoisonFluid();
            case FALLOUT -> HbmFluids.none();
        };
    }

    private HbmFluidTank smokeTank(HbmPollutionType type) {
        return switch (type) {
            case SOOT -> this.smokeTank;
            case HEAVYMETAL -> this.smokeLeadedTank;
            case POISON -> this.smokePoisonTank;
            case FALLOUT -> this.smokeTank;
        };
    }

    public record Port(BlockPos pos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class OutputFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private OutputFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != tank.type()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = tank.drain(fluid, resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = tank.type();
            HbmFluidStack drained = tank.drain(fluid, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }
    }
}
