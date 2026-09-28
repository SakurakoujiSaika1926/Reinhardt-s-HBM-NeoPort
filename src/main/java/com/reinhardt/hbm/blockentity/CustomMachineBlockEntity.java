package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CustomMachineBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.menu.CustomMachineMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.CustomMachineRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Functional port of the latest legacy custom-machine default: the paper press. */
public final class CustomMachineBlockEntity extends BlockEntity
        implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final String MACHINE_KEY = "paper_press";
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_COUNT = 8;
    public static final int TANK_CAPACITY = 1_000;
    public static final long MAX_POWER = 10_000L;
    private static final int STRUCTURE_CHECK_INTERVAL = 300;
    private static final int[] AUTOMATION_SLOTS = {INPUT_SLOT, OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(water(), TANK_CAPACITY);
    private long power;
    private long lastInput;
    private int progress;
    private int processTime = 60;
    private int usage = 10;
    private int structureCheckDelay;
    private boolean structureValid;
    private boolean working;
    private boolean recipeDirty = true;
    @Nullable
    private RecipeHolder<CustomMachineRecipe> cachedRecipe;
    @Nullable
    private ResourceLocation cachedRecipeId;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) CustomMachineBlockEntity.this.power;
                case 1 -> CustomMachineBlockEntity.this.progress;
                case 2 -> CustomMachineBlockEntity.this.processTime;
                case 3 -> CustomMachineBlockEntity.this.usage;
                case 4 -> CustomMachineBlockEntity.this.inputTank.type().oldId();
                case 5 -> CustomMachineBlockEntity.this.inputTank.amount();
                case 6 -> CustomMachineBlockEntity.this.structureValid ? 1 : 0;
                case 7 -> CustomMachineBlockEntity.this.working ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CustomMachineBlockEntity.this.power = value;
                case 1 -> CustomMachineBlockEntity.this.progress = value;
                case 2 -> CustomMachineBlockEntity.this.processTime = Math.max(1, value);
                case 3 -> CustomMachineBlockEntity.this.usage = Math.max(1, value);
                case 4 -> CustomMachineBlockEntity.this.inputTank.setType(
                        HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 5 -> CustomMachineBlockEntity.this.inputTank.setAmount(value);
                case 6 -> CustomMachineBlockEntity.this.structureValid = value != 0;
                case 7 -> CustomMachineBlockEntity.this.working = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CustomMachineBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CUSTOM_MACHINE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CustomMachineBlockEntity machine) {
        if (level.isClientSide) {
            return;
        }
        if (machine.structureCheckDelay-- <= 0) {
            machine.checkStructure();
        }
        machine.refreshRecipeIfDirty();
        PowerNetworkManager.tickFromEndpoint(level, machine);
        machine.tickServer(level);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public boolean structureValid() {
        return this.structureValid;
    }

    public boolean checkStructure() {
        this.structureCheckDelay = STRUCTURE_CHECK_INTERVAL;
        if (this.level == null) {
            return false;
        }
        Direction facing = facing();
        boolean valid = true;
        for (int x = -1; x <= 1 && valid; x++) {
            for (int y = -1; y <= 1 && valid; y++) {
                for (int z = 0; z <= 2; z++) {
                    if ((x == 0 && y == 0 && z == 1) || (x == 0 && z == 0)) {
                        continue;
                    }
                    BlockPos target = this.worldPosition.offset(mapPaperPressOffset(facing, x, y, z));
                    Block expected = y == 0 ? HbmBlocks.CM_SHEET.get() : HbmBlocks.CM_BLOCK.get();
                    if (!isSteelVariant(this.level.getBlockState(target), expected)) {
                        valid = false;
                        break;
                    }
                }
            }
        }
        valid &= isSteelVariant(this.level.getBlockState(this.worldPosition.below()), HbmBlocks.CM_PORT.get());
        valid &= isSteelVariant(this.level.getBlockState(this.worldPosition.above()), HbmBlocks.CM_PORT.get());
        setStructureValid(valid);
        return valid;
    }

    public boolean ownsPort(BlockPos pos) {
        return this.structureValid && (pos.equals(this.worldPosition.above()) || pos.equals(this.worldPosition.below()));
    }

    @Nullable
    public static CustomMachineBlockEntity findForPort(LevelAccessor level, BlockPos portPos) {
        BlockEntity below = level.getBlockEntity(portPos.below());
        if (below instanceof CustomMachineBlockEntity machine && machine.ownsPort(portPos)) {
            return machine;
        }
        BlockEntity above = level.getBlockEntity(portPos.above());
        if (above instanceof CustomMachineBlockEntity machine && machine.ownsPort(portPos)) {
            return machine;
        }
        return null;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return this.structureValid
                ? List.of(this.worldPosition.below(), this.worldPosition.above())
                : List.of();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return this.structureValid
                && (connectorPos.equals(this.worldPosition.below()) || connectorPos.equals(this.worldPosition.above()));
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        refreshRecipeIfDirty();
        CustomMachineRecipe recipe = this.cachedRecipe == null ? null : this.cachedRecipe.value();
        if (recipe == null || !canProcess(recipe) || this.power >= MAX_POWER) {
            return 0L;
        }
        return Math.min(Math.max(100L, recipe.power()), MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + Math.max(0L, receivedInput));
        this.lastInput = Math.max(0L, receivedInput);
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.processTime <= 0 ? 0 : this.progress * 100 / this.processTime;
        return Component.translatable("message.reinhardtshbm.power.custom_machine",
                this.lastInput, this.usage, this.power, MAX_POWER, percent);
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new InputFluidHandler();
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0 || this.items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        contentsChanged(slot);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        this.recipeDirty = true;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        ItemStack inserted = stack;
        if (!inserted.isEmpty() && inserted.getCount() > getMaxStackSize(inserted)) {
            inserted = inserted.copyWithCount(getMaxStackSize(inserted));
        }
        this.items.set(slot, inserted);
        contentsChanged(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> !stack.isEmpty();
            case BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.recipeDirty = true;
        setChangedAndSync(true);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.custom_machine.paper_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CustomMachineMenu(containerId, playerInventory, this, this.menuData, this.worldPosition);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, stack.copy()));
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.put("InputTank", this.inputTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("ProcessTime", this.processTime);
        tag.putInt("Usage", this.usage);
        tag.putInt("StructureCheckDelay", this.structureCheckDelay);
        tag.putBoolean("StructureValid", this.structureValid);
        tag.putBoolean("Working", this.working);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.inputTank.load(tag.getCompound("InputTank"));
        if (this.inputTank.amount() == 0) {
            this.inputTank.setType(water());
            this.inputTank.setPressure(0);
        }
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = Math.max(0L, tag.getLong("LastInput"));
        this.progress = Math.max(0, tag.getInt("Progress"));
        this.processTime = Math.max(1, tag.getInt("ProcessTime"));
        this.usage = Math.max(1, tag.getInt("Usage"));
        this.structureCheckDelay = Math.max(0, tag.getInt("StructureCheckDelay"));
        this.structureValid = tag.getBoolean("StructureValid");
        this.working = tag.getBoolean("Working");
        this.recipeDirty = true;
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
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        CustomMachineRecipe recipe = this.cachedRecipe == null ? null : this.cachedRecipe.value();
        boolean wasWorking = this.working;
        this.working = false;

        if (!this.structureValid || recipe == null) {
            this.progress = 0;
            setWorkingState(false, wasWorking);
            return;
        }

        this.processTime = recipe.duration();
        this.usage = recipe.power();
        if (canProcess(recipe) && this.power >= recipe.power()) {
            this.power -= recipe.power();
            this.progress++;
            this.working = true;
            if (level.getGameTime() % 20L == 0L) {
                emitPollution(level, recipe);
            }
            if (this.progress >= recipe.duration()) {
                finishRecipe(recipe);
                this.progress = 0;
                this.recipeDirty = true;
                setChangedAndSync(true);
            }
        }
        setWorkingState(this.working, wasWorking);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void refreshRecipeIfDirty() {
        if (!this.recipeDirty || this.level == null) {
            return;
        }
        this.recipeDirty = false;
        ResourceLocation previous = this.cachedRecipeId;
        this.cachedRecipe = this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CUSTOM_MACHINE.get()).stream()
                .filter(holder -> MACHINE_KEY.equals(holder.value().machine()))
                .filter(holder -> holder.value().ingredient().test(this.items.get(INPUT_SLOT)))
                .filter(holder -> this.inputTank.type() == holder.value().inputFluid().fluid())
                .filter(holder -> this.inputTank.pressure() == holder.value().inputFluid().pressure())
                .findFirst()
                .orElse(null);
        this.cachedRecipeId = this.cachedRecipe == null ? null : this.cachedRecipe.id();
        if (previous != null && !previous.equals(this.cachedRecipeId)) {
            this.progress = 0;
        }
    }

    private boolean canProcess(CustomMachineRecipe recipe) {
        ItemStack input = this.items.get(INPUT_SLOT);
        if (input.isEmpty() || input.getCount() < recipe.inputCount() || !recipe.ingredient().test(input)) {
            return false;
        }
        if (this.inputTank.type() != recipe.inputFluid().fluid()
                || this.inputTank.pressure() != recipe.inputFluid().pressure()
                || this.inputTank.amount() < recipe.inputFluid().amount()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, recipe.result())
                && output.getCount() + recipe.result().getCount() <= output.getMaxStackSize();
    }

    private void finishRecipe(CustomMachineRecipe recipe) {
        this.items.get(INPUT_SLOT).shrink(recipe.inputCount());
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        this.inputTank.drain(recipe.inputFluid().fluid(), recipe.inputFluid().amount(), false);
        if (this.inputTank.amount() == 0) {
            this.inputTank.setType(water());
            this.inputTank.setPressure(0);
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, recipe.result().copy());
        } else {
            output.grow(recipe.result().getCount());
        }
    }

    private void emitPollution(Level level, CustomMachineRecipe recipe) {
        if (recipe.pollutionAmount() <= 0.0D || recipe.pollutionType().isBlank()) {
            return;
        }
        try {
            HbmPollutionType type = HbmPollutionType.valueOf(recipe.pollutionType().toUpperCase(Locale.ROOT));
            HbmPollution.increment(level, this.worldPosition, type, recipe.pollutionAmount());
        } catch (IllegalArgumentException ignored) {
            // Invalid datapack pollution identifiers disable only that recipe's pollution output.
        }
    }

    private void contentsChanged(int slot) {
        if (slot == INPUT_SLOT) {
            this.recipeDirty = true;
        }
        setChangedAndSync(false);
    }

    private void setStructureValid(boolean valid) {
        if (this.structureValid == valid) {
            return;
        }
        this.structureValid = valid;
        this.recipeDirty = true;
        if (!valid) {
            this.progress = 0;
            this.working = false;
        }
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.invalidateCapabilities(this.worldPosition.above());
            this.level.invalidateCapabilities(this.worldPosition.below());
            PowerNetworkManager.markDirty(this.level);
        }
        setChangedAndSync(true);
    }

    private void setWorkingState(boolean active, boolean previous) {
        if (active == previous || this.level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(CustomMachineBlock.ACTIVE) && state.getValue(CustomMachineBlock.ACTIVE) != active) {
            this.level.setBlock(this.worldPosition, state.setValue(CustomMachineBlock.ACTIVE, active),
                    Block.UPDATE_CLIENTS);
        }
        sync();
    }

    private void sync() {
        setChangedAndSync(true);
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null && sync && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(CustomMachineBlock.FACING)
                ? state.getValue(CustomMachineBlock.FACING)
                : Direction.NORTH;
    }

    /** Explicit per-facing mapping; do not replace with a shared generic rotation helper. */
    private static BlockPos mapPaperPressOffset(Direction facing, int x, int y, int z) {
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(-z, y, x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            default -> throw new IllegalArgumentException("Paper press requires a horizontal facing: " + facing);
        };
    }

    private static boolean isSteelVariant(BlockState state, Block expected) {
        return state.is(expected)
                && state.hasProperty(LegacyVariantBlock.VARIANT)
                && state.getValue(LegacyVariantBlock.VARIANT) == 0;
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private final class InputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inputTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid()).filter(fluid -> fluid == water()).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            HbmFluidDefinition type = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (resource.isEmpty() || type != water()) {
                return 0;
            }
            int filled = inputTank.fill(type, resource.getAmount(), 0, action.simulate());
            if (filled > 0 && action.execute()) {
                recipeDirty = true;
                setChangedAndSync(true);
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
