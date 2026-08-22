package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.ArcWelderBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.ArcWelderMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ArcWelderRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArcWelderBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_START = 0;
    public static final int INPUT_END = 3;
    public static final int OUTPUT_SLOT = 3;
    public static final int BATTERY_SLOT = 4;
    public static final int FLUID_IDENTIFIER_SLOT = 5;
    public static final int UPGRADE_START = 6;
    public static final int UPGRADE_END = 8;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 11;
    public static final int TANK_CAPACITY = 24_000;
    public static final long BASE_MAX_POWER = 2_000L;

    private static final int[] AUTOMATION_SLOTS = {0, 1, 2, 3};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank tank = new HbmFluidTank(TANK_CAPACITY);
    private long power;
    private long maxPower = BASE_MAX_POWER;
    private long lastInput;
    private long consumption = 100L;
    private int progress;
    private int processTime = 1;
    private int completedCycles;
    private boolean hasRecipe;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) ArcWelderBlockEntity.this.power;
                case 1 -> (int) ArcWelderBlockEntity.this.lastInput;
                case 2 -> ArcWelderBlockEntity.this.progress;
                case 3 -> ArcWelderBlockEntity.this.processTime;
                case 4 -> (int) ArcWelderBlockEntity.this.consumption;
                case 5 -> (int) ArcWelderBlockEntity.this.maxPower;
                case 6 -> ArcWelderBlockEntity.this.hasRecipe ? 1 : 0;
                case 7 -> ArcWelderBlockEntity.this.completedCycles;
                case 8 -> ArcWelderBlockEntity.this.tank.type().oldId();
                case 9 -> ArcWelderBlockEntity.this.tank.amount();
                case 10 -> ArcWelderBlockEntity.this.tank.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ArcWelderBlockEntity.this.power = value;
                case 1 -> ArcWelderBlockEntity.this.lastInput = value;
                case 2 -> ArcWelderBlockEntity.this.progress = value;
                case 3 -> ArcWelderBlockEntity.this.processTime = Math.max(1, value);
                case 4 -> ArcWelderBlockEntity.this.consumption = Math.max(1, value);
                case 5 -> ArcWelderBlockEntity.this.maxPower = Math.max(BASE_MAX_POWER, value);
                case 6 -> ArcWelderBlockEntity.this.hasRecipe = value != 0;
                case 7 -> ArcWelderBlockEntity.this.completedCycles = value;
                case 8 -> ArcWelderBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 9 -> ArcWelderBlockEntity.this.tank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ArcWelderBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ARC_WELDER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ArcWelderBlockEntity blockEntity) {
        PowerNetworkManager.tickFromEndpoint(level, blockEntity);
        blockEntity.tickWork(level);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return connectorPositions().stream().map(Port::pos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : connectorPositions()) {
            if (port.pos().equals(connectorPos)) {
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
        if (this.power >= this.maxPower) {
            return 0L;
        }
        long requested = Math.max(100L, this.consumption);
        return Math.min(requested, this.maxPower - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(this.maxPower, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.processTime <= 0 ? 0 : this.progress * 100 / this.processTime;
        return Component.translatable(
                "message.reinhardtshbm.power.arc_welder",
                this.lastInput,
                this.consumption,
                this.power,
                this.maxPower,
                percent,
                this.completedCycles
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
            if (isRecipeSlot(slot)) {
                this.progress = 0;
            }
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
        if (isRecipeSlot(slot)) {
            this.progress = 0;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot >= INPUT_START && slot < INPUT_END) {
            return isIngredient(stack);
        }
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot == FLUID_IDENTIFIER_SLOT) {
            return stack.getItem() instanceof FluidIdentifierItem;
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return isSupportedUpgrade(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= INPUT_START && slot < INPUT_END && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
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
        this.progress = 0;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.arc_welder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArcWelderMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new ArcWelderFluidHandler();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queryPos, @Nullable Direction side) {
        return isAutomationPort(queryPos) ? new ArcWelderFluidHandler() : null;
    }

    public boolean isAutomationPort(BlockPos queryPos) {
        for (Port port : connectorPositions()) {
            if (port.pos().equals(queryPos) || manhattanDistance(port.pos(), queryPos) == 1) {
                return true;
            }
        }
        return false;
    }

    public boolean canAcceptInput(ItemStack stack) {
        return !stack.isEmpty() && isIngredient(stack);
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
        tag.putLong("Power", this.power);
        tag.putLong("MaxPower", this.maxPower);
        tag.putLong("LastInput", this.lastInput);
        tag.putLong("Consumption", this.consumption);
        tag.putInt("Progress", this.progress);
        tag.putInt("ProcessTime", this.processTime);
        tag.putInt("CompletedCycles", this.completedCycles);
        tag.put("Tank", this.tank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.power = tag.getLong("Power");
        this.maxPower = Math.max(BASE_MAX_POWER, tag.getLong("MaxPower"));
        this.lastInput = tag.getLong("LastInput");
        this.consumption = Math.max(1L, tag.getLong("Consumption"));
        this.progress = tag.getInt("Progress");
        this.processTime = Math.max(1, tag.getInt("ProcessTime"));
        this.completedCycles = tag.getInt("CompletedCycles");
        this.tank.load(tag.getCompound("Tank"));
    }

    private void tickWork(Level level) {
        applyIdentifierSlot();
        Optional<RecipeHolder<ArcWelderRecipe>> recipeHolder = getRecipe(level);
        this.hasRecipe = recipeHolder.isPresent();
        if (recipeHolder.isEmpty()) {
            this.progress = 0;
            this.processTime = 1;
            this.consumption = 100L;
            this.maxPower = Math.max(BASE_MAX_POWER, this.power);
            this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, this.maxPower);
            setLit(false);
            setChanged();
            return;
        }

        ArcWelderRecipe recipe = recipeHolder.get().value();
        updateUpgradeAdjustedStats(recipe);
        this.maxPower = Math.max(this.consumption * 20L, this.power);
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, this.maxPower);

        if (!canOutput(recipe.result()) || this.power < this.consumption) {
            this.progress = 0;
            setLit(false);
            setChanged();
            return;
        }

        this.power -= this.consumption;
        this.progress += 1 + overdriveLevel();
        setLit(true);

        if (this.progress >= this.processTime) {
            this.progress = 0;
            consumeInputs(recipe);
            insertOutput(recipe.result());
            this.completedCycles++;
        }
        setChanged();
    }

    private Optional<RecipeHolder<ArcWelderRecipe>> getRecipe(Level level) {
        return level.getRecipeManager().getRecipeFor(HbmRecipeTypes.ARC_WELDER.get(), recipeInput(), level);
    }

    private ArcWelderRecipe.Input recipeInput() {
        return new ArcWelderRecipe.Input(
                stacks(INPUT_START, INPUT_END),
                this.tank.type(),
                this.tank.amount()
        );
    }

    private List<ItemStack> stacks(int start, int end) {
        List<ItemStack> stacks = new ArrayList<>(end - start);
        for (int slot = start; slot < end; slot++) {
            stacks.add(this.items.get(slot));
        }
        return stacks;
    }

    private void updateUpgradeAdjustedStats(ArcWelderRecipe recipe) {
        int redLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
        int blueLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        int blackLevel = overdriveLevel();

        this.processTime = Math.max(1, recipe.duration() - (recipe.duration() * redLevel / 6) + (recipe.duration() * blueLevel / 3));
        this.consumption = Math.max(1L, recipe.consumption() + (recipe.consumption() * redLevel) - (recipe.consumption() * blueLevel / 6));
        this.consumption *= 1L << blackLevel;
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return Math.min(3, level);
    }

    private int overdriveLevel() {
        return upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE);
    }

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void insertOutput(ItemStack result) {
        ItemStack copy = result.copy();
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, copy);
        } else {
            output.grow(copy.getCount());
        }
    }

    private void consumeInputs(ArcWelderRecipe recipe) {
        consumeGroup(recipe.inputs(), INPUT_START, INPUT_END);
        recipe.fluid().ifPresent(fluid -> this.tank.drain(fluid.fluid(), fluid.amount(), false));
    }

    private void consumeGroup(List<ArcWelderRecipe.CountedIngredient> ingredients, int start, int end) {
        for (ArcWelderRecipe.CountedIngredient ingredient : ingredients) {
            for (int slot = start; slot < end; slot++) {
                ItemStack stack = this.items.get(slot);
                if (!stack.isEmpty() && stack.getCount() >= ingredient.count() && ingredient.ingredient().test(stack)) {
                    stack.shrink(ingredient.count());
                    if (stack.isEmpty()) {
                        this.items.set(slot, ItemStack.EMPTY);
                    }
                    break;
                }
            }
        }
    }

    private boolean isIngredient(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return false;
        }
        return this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ARC_WELDER.get()).stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> containsIngredient(recipe.inputs(), stack));
    }

    private static boolean containsIngredient(List<ArcWelderRecipe.CountedIngredient> ingredients, ItemStack stack) {
        for (ArcWelderRecipe.CountedIngredient ingredient : ingredients) {
            if (ingredient.ingredient().test(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDuplicateInGroup(int slot, ItemStack stack, int start, int end) {
        for (int otherSlot = start; otherSlot < end; otherSlot++) {
            if (otherSlot == slot) {
                continue;
            }
            ItemStack other = this.items.get(otherSlot);
            if (!other.isEmpty() && ItemStack.isSameItemSameComponents(other, stack)) {
                return true;
            }
        }
        return false;
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }

        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof ArcWelderBlock && state.getValue(ArcWelderBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(ArcWelderBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isRecipeSlot(int slot) {
        return slot >= INPUT_START && slot < INPUT_END;
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
        if (this.tank.amount() == 0 || this.tank.type() == fluid) {
            this.tank.setType(fluid);
        }
    }

    private boolean acceptsRecipeFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || this.level == null) {
            return false;
        }

        return this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ARC_WELDER.get()).stream()
                .map(RecipeHolder::value)
                .map(ArcWelderRecipe::fluid)
                .flatMap(Optional::stream)
                .anyMatch(ingredient -> ingredient.fluid() == fluid);
    }

    private void syncFluidCapability() {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
        }
    }

    private List<Port> connectorPositions() {
        Direction dir = this.getBlockState().hasProperty(ArcWelderBlock.FACING) ? this.getBlockState().getValue(ArcWelderBlock.FACING) : Direction.NORTH;
        Direction rot = dir.getClockWise();
        return List.of(
                new Port(this.worldPosition.relative(dir), dir),
                new Port(this.worldPosition.relative(dir).relative(rot), dir),
                new Port(this.worldPosition.relative(dir).relative(rot.getOpposite()), dir),
                new Port(this.worldPosition.relative(dir.getOpposite(), 2), dir.getOpposite()),
                new Port(this.worldPosition.relative(dir.getOpposite(), 2).relative(rot), dir.getOpposite()),
                new Port(this.worldPosition.relative(dir.getOpposite(), 2).relative(rot.getOpposite()), dir.getOpposite()),
                new Port(this.worldPosition.relative(rot, 2), rot),
                new Port(this.worldPosition.relative(dir.getOpposite()).relative(rot, 2), rot),
                new Port(this.worldPosition.relative(rot.getOpposite(), 2), rot.getOpposite()),
                new Port(this.worldPosition.relative(dir.getOpposite()).relative(rot.getOpposite(), 2), rot.getOpposite())
        );
    }

    private static int manhattanDistance(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX())
                + Math.abs(first.getY() - second.getY())
                + Math.abs(first.getZ() - second.getZ());
    }

    private record Port(BlockPos pos, Direction direction) {
    }

    private final class ArcWelderFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return ArcWelderBlockEntity.this.tank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return ArcWelderBlockEntity.this.tank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            return HbmFluids.fromNeoFluid(stack.getFluid())
                    .filter(ArcWelderBlockEntity.this::acceptsRecipeFluid)
                    .isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            Optional<HbmFluidDefinition> fluid = HbmFluids.fromNeoFluid(resource.getFluid());
            if (fluid.isEmpty() || !acceptsRecipeFluid(fluid.get())) {
                return 0;
            }
            if (!ArcWelderBlockEntity.this.tank.type().isNone()
                    && ArcWelderBlockEntity.this.tank.type() != fluid.get()) {
                return 0;
            }

            int accepted = ArcWelderBlockEntity.this.tank.fill(fluid.get(), resource.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) {
                syncFluidCapability();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            Optional<HbmFluidDefinition> requested = HbmFluids.fromNeoFluid(resource.getFluid());
            if (requested.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = ArcWelderBlockEntity.this.tank.drain(requested.get(), resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                syncFluidCapability();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            HbmFluidStack drained = ArcWelderBlockEntity.this.tank.drain(null, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                syncFluidCapability();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static boolean isSupportedUpgrade(ItemStack stack) {
        if (!MachineUpgradeItem.isMachineUpgrade(stack)) {
            return false;
        }
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED
                || type == MachineUpgradeItem.UpgradeType.POWER
                || type == MachineUpgradeItem.UpgradeType.OVERDRIVE;
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

}
