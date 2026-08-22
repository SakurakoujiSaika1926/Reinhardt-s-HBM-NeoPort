package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.CokerMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.recipe.CokerRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.FluidCopiable;
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

import java.util.List;
import java.util.Optional;

public class CokerBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int FLUID_IDENTIFIER_SLOT = 0;
    public static final int ITEM_OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    public static final int DATA_COUNT = 11;
    public static final int INPUT_CAPACITY = 16_000;
    public static final int OUTPUT_CAPACITY = 8_000;
    public static final int PROCESS_TIME = 20_000;
    public static final int MAX_HEAT = 100_000;
    private static final double DIFFUSION = 0.25D;
    private static final int PUSH_PER_PORT = 8_000;
    private static final int[] ALL_SLOTS = {FLUID_IDENTIFIER_SLOT, ITEM_OUTPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {ITEM_OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(heavyOil(), INPUT_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(cokerOil(), OUTPUT_CAPACITY);
    private HbmFluidDefinition configuredInput = heavyOil();
    private int progress;
    private int heat;
    private boolean working;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CokerBlockEntity.this.inputTank.type().oldId();
                case 1 -> CokerBlockEntity.this.inputTank.amount();
                case 2 -> CokerBlockEntity.this.inputTank.capacity();
                case 3 -> CokerBlockEntity.this.outputTank.type().oldId();
                case 4 -> CokerBlockEntity.this.outputTank.amount();
                case 5 -> CokerBlockEntity.this.outputTank.capacity();
                case 6 -> CokerBlockEntity.this.progress;
                case 7 -> PROCESS_TIME;
                case 8 -> CokerBlockEntity.this.heat;
                case 9 -> MAX_HEAT;
                case 10 -> CokerBlockEntity.this.working ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CokerBlockEntity.this.inputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> CokerBlockEntity.this.inputTank.setAmount(value);
                case 3 -> CokerBlockEntity.this.outputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 4 -> CokerBlockEntity.this.outputTank.setAmount(value);
                case 6 -> CokerBlockEntity.this.progress = value;
                case 8 -> CokerBlockEntity.this.heat = value;
                case 10 -> CokerBlockEntity.this.working = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CokerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.COKER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CokerBlockEntity coker) {
        if (level.isClientSide) {
            coker.tickClient(level);
        } else {
            coker.tickServer(level);
        }
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public int heat() {
        return this.heat;
    }

    public int progress() {
        return this.progress;
    }

    public boolean isWorking() {
        return this.working;
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new CokerFluidHandler(queriedPos.immutable(), side);
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
                Port.fromConnector(pos.offset(2, 0, 1), Direction.EAST),
                Port.fromConnector(pos.offset(2, 0, -1), Direction.EAST),
                Port.fromConnector(pos.offset(-2, 0, 1), Direction.WEST),
                Port.fromConnector(pos.offset(-2, 0, -1), Direction.WEST),
                Port.fromConnector(pos.offset(1, 0, 2), Direction.SOUTH),
                Port.fromConnector(pos.offset(-1, 0, 2), Direction.SOUTH),
                Port.fromConnector(pos.offset(1, 0, -2), Direction.NORTH),
                Port.fromConnector(pos.offset(-1, 0, -2), Direction.NORTH)
        );
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.configuredInput.oldId(), this.outputTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (fluid == null || fluid.isNone() || !acceptsInputFluid(fluid)) {
            return;
        }
        setConfiguredInput(fluid);
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
        return slot == FLUID_IDENTIFIER_SLOT && stack.getItem() instanceof FluidIdentifierItem;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
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
        return Component.translatable("container.reinhardtshbm.machine_coker");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CokerMenu(containerId, playerInventory, this, this.menuData);
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
        tag.put("OutputTank", this.outputTank.save());
        tag.putString("ConfiguredInput", this.configuredInput.name());
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
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput")).orElse(heavyOil());
        this.progress = tag.getInt("Progress");
        this.heat = Math.max(0, Math.min(MAX_HEAT, tag.getInt("Heat")));
        this.working = tag.getBoolean("Working");
        if (this.inputTank.amount() == 0) {
            this.inputTank.setType(this.configuredInput);
        }
        if (this.outputTank.amount() == 0 && this.outputTank.type().isNone()) {
            this.outputTank.setType(cokerOil());
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
        tryPullHeat(level);
        applyIdentifierSlot();
        setupTanks();
        process();
        pushOutput(level);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level) {
        if (!this.working || level.getGameTime() % 2L != 0L) {
            return;
        }
        level.addParticle(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 22.0D,
                this.worldPosition.getZ() + 0.5D,
                (level.random.nextDouble() - 0.5D) * 0.08D,
                0.08D + level.random.nextDouble() * 0.04D,
                (level.random.nextDouble() - 0.5D) * 0.08D
        );
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

    private void applyIdentifierSlot() {
        ItemStack identifier = this.items.get(FLUID_IDENTIFIER_SLOT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        HbmFluidDefinition fluid = FluidIdentifierItem.primary(identifier);
        if (fluid.isNone() || !acceptsInputFluid(fluid)) {
            return;
        }
        if (this.inputTank.amount() == 0 || this.inputTank.type() == fluid) {
            setConfiguredInput(fluid);
        }
    }

    private void setConfiguredInput(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        if (this.configuredInput == fluid && (this.inputTank.amount() > 0 || this.inputTank.type() == fluid)) {
            return;
        }
        this.configuredInput = fluid;
        if (this.inputTank.amount() == 0) {
            this.inputTank.setType(fluid);
        }
        sync();
    }

    private void setupTanks() {
        if (this.inputTank.amount() == 0 && !this.configuredInput.isNone() && this.inputTank.type() != this.configuredInput) {
            this.inputTank.setType(this.configuredInput);
        }
        Optional<net.minecraft.world.item.crafting.RecipeHolder<CokerRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            if (this.outputTank.amount() == 0) {
                this.outputTank.setType(cokerOil());
            }
            return;
        }
        CokerRecipe recipe = holder.get().value();
        if (recipe.hasByproduct()) {
            if (this.outputTank.amount() == 0 || this.outputTank.type() == recipe.byproduct().fluid()) {
                this.outputTank.setType(recipe.byproduct().fluid());
            }
        } else if (this.outputTank.amount() == 0) {
            this.outputTank.clear();
        }
    }

    private void process() {
        this.working = false;
        Optional<net.minecraft.world.item.crafting.RecipeHolder<CokerRecipe>> holder = currentRecipe();
        if (holder.isEmpty() || !canProcess(holder.get().value())) {
            return;
        }

        int burn = this.heat / 100;
        if (burn <= 0) {
            return;
        }

        this.working = true;
        this.progress += burn;
        this.heat -= burn;
        if (this.level != null && this.level.getGameTime() % 5L == 0L) {
            HbmPollution.increment(this.level, this.worldPosition, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND * 5.0D);
        }

        if (this.progress < PROCESS_TIME) {
            return;
        }

        this.progress -= PROCESS_TIME;
        CokerRecipe recipe = holder.get().value();
        this.inputTank.drain(recipe.input().fluid(), recipe.input().amount(), false);
        if (this.inputTank.amount() == 0) {
            this.inputTank.setType(this.configuredInput);
        }
        placeOutput(recipe.output());
        if (recipe.hasByproduct()) {
            this.outputTank.fill(recipe.byproduct().fluid(), recipe.byproduct().amount(), false);
        }
        sync();
    }

    private boolean canProcess(CokerRecipe recipe) {
        if (this.inputTank.type() != recipe.input().fluid() || this.inputTank.amount() < recipe.input().amount()) {
            return false;
        }
        if (!canFitItemOutput(recipe.output())) {
            return false;
        }
        if (!recipe.hasByproduct()) {
            return true;
        }
        if (this.outputTank.type() != recipe.byproduct().fluid()) {
            return this.outputTank.amount() == 0;
        }
        return this.outputTank.amount() + recipe.byproduct().amount() <= this.outputTank.capacity();
    }

    private boolean canFitItemOutput(ItemStack output) {
        if (output.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(ITEM_OUTPUT_SLOT);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, output) && current.getCount() + output.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(ITEM_OUTPUT_SLOT);
        if (current.isEmpty()) {
            this.items.set(ITEM_OUTPUT_SLOT, output.copy());
        } else {
            current.grow(output.getCount());
        }
    }

    private void pushOutput(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(this.outputTank.type(), Math.min(PUSH_PER_PORT, this.outputTank.amount()));
            int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), stack, this.worldPosition, true);
            if (accepted > 0) {
                this.outputTank.drain(this.outputTank.type(), accepted, false);
                sync();
            }
        }
    }

    private Optional<net.minecraft.world.item.crafting.RecipeHolder<CokerRecipe>> currentRecipe() {
        if (this.level == null) {
            return Optional.empty();
        }
        HbmFluidDefinition input = this.inputTank.amount() > 0 ? this.inputTank.type() : this.configuredInput;
        if (input.isNone()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.COKER.get())
                .stream()
                .filter(holder -> holder.value().input().fluid() == input)
                .findFirst();
    }

    private boolean acceptsInputFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || this.level == null) {
            return false;
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.COKER.get())
                .stream()
                .anyMatch(holder -> holder.value().input().fluid() == fluid);
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

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static HbmFluidDefinition heavyOil() {
        return HbmFluids.byName("heavyoil").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition cokerOil() {
        return HbmFluids.byName("oil_coker").orElse(HbmFluids.none());
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
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class CokerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private CokerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return inputTank.getFluidInTank(0);
            }
            if (tank == 1) {
                return outputTank.getFluidInTank(0);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> inputTank.capacity();
                case 1 -> outputTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
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
            if (!acceptsInputFluid(fluid)) {
                return 0;
            }
            if (inputTank.amount() > 0 && inputTank.type() != fluid) {
                return 0;
            }
            if (inputTank.amount() == 0 && action.execute()) {
                configuredInput = fluid;
                inputTank.setType(fluid);
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
            if (fluid != outputTank.type()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = outputTank.drain(fluid, resource.getAmount(), action.simulate());
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
            HbmFluidDefinition fluid = outputTank.type();
            HbmFluidStack drained = outputTank.drain(fluid, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }
    }
}
