package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CrystallizerBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.CrystallizerMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.CrystallizerRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.FluidCopiable;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class CrystallizerBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int INPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int FLUID_INPUT_SLOT = 3;
    public static final int FLUID_OUTPUT_SLOT = 4;
    public static final int UPGRADE_START = 5;
    public static final int UPGRADE_END = 7;
    public static final int FLUID_IDENTIFIER_SLOT = 7;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 9;
    public static final long MAX_POWER = 1_000_000L;
    public static final int BASE_DEMAND = 1000;
    public static final int TANK_CAPACITY = 8000;

    private static final int[] AUTOMATION_SLOTS = {INPUT_SLOT, OUTPUT_SLOT, FLUID_OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank acidTank = new HbmFluidTank(HbmFluids.byName("peroxide").orElse(HbmFluids.none()), TANK_CAPACITY);
    private long power;
    private long lastInput;
    private int progress;
    private int workTime = 600;
    private int currentDemand = BASE_DEMAND;
    private int completedCycles;
    private boolean working;
    private float clientAngle;
    private float clientPrevAngle;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) CrystallizerBlockEntity.this.power;
                case 1 -> (int) CrystallizerBlockEntity.this.lastInput;
                case 2 -> CrystallizerBlockEntity.this.progress;
                case 3 -> CrystallizerBlockEntity.this.workTime;
                case 4 -> CrystallizerBlockEntity.this.currentDemand;
                case 5 -> CrystallizerBlockEntity.this.completedCycles;
                case 6 -> CrystallizerBlockEntity.this.working ? 1 : 0;
                case 7 -> CrystallizerBlockEntity.this.acidTank.type().oldId();
                case 8 -> CrystallizerBlockEntity.this.acidTank.amount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CrystallizerBlockEntity.this.power = value;
                case 1 -> CrystallizerBlockEntity.this.lastInput = value;
                case 2 -> CrystallizerBlockEntity.this.progress = value;
                case 3 -> CrystallizerBlockEntity.this.workTime = Math.max(1, value);
                case 4 -> CrystallizerBlockEntity.this.currentDemand = Math.max(1, value);
                case 5 -> CrystallizerBlockEntity.this.completedCycles = value;
                case 6 -> CrystallizerBlockEntity.this.working = value != 0;
                case 7 -> CrystallizerBlockEntity.this.acidTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 8 -> CrystallizerBlockEntity.this.acidTank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CrystallizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.CRYSTALLIZER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrystallizerBlockEntity crystallizer) {
        if (level.isClientSide) {
            crystallizer.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, crystallizer);
        crystallizer.tickServer(level);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public HbmFluidTank acidTank() {
        return this.acidTank;
    }

    public boolean isWorking() {
        return this.working;
    }

    public float clientAngle(float partialTick) {
        return this.clientPrevAngle + (this.clientAngle - this.clientPrevAngle) * partialTick;
    }

    public int clientFluidColor() {
        return 0x66000000 | this.acidTank.type().color();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return connectorPositions();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return connectorPositions().contains(connectorPos);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (this.power >= MAX_POWER) {
            return 0L;
        }
        return Math.min(Math.max(BASE_DEMAND, this.currentDemand), MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.workTime <= 0 ? 0 : this.progress * 100 / this.workTime;
        return Component.translatable(
                "message.reinhardtshbm.power.crystallizer",
                this.lastInput,
                this.currentDemand,
                this.power,
                MAX_POWER,
                percent,
                this.completedCycles
        );
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new AcidTankFluidHandler();
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
        if (slot == INPUT_SLOT) {
            this.progress = 0;
        }
        setChangedAndSync(false);
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
        if (slot == INPUT_SLOT) {
            this.progress = 0;
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> canAcceptInput(stack);
            case BATTERY_SLOT -> BatteryPackItem.isBattery(stack);
            case FLUID_INPUT_SLOT -> isDrainableContainer(stack);
            case UPGRADE_START, UPGRADE_START + 1 -> isSupportedUpgrade(stack);
            case FLUID_IDENTIFIER_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
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
        return slot == OUTPUT_SLOT || slot == FLUID_OUTPUT_SLOT || isSlotClogged(slot);
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
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.crystallizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrystallizerMenu(containerId, playerInventory, this, this.menuData);
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
        tag.put("AcidTank", this.acidTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("CompletedCycles", this.completedCycles);
        tag.putBoolean("Working", this.working);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.acidTank.load(tag.getCompound("AcidTank"));
        if (this.acidTank.type().isNone()) {
            this.acidTank.setType(HbmFluids.byName("peroxide").orElse(HbmFluids.none()));
        }
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.progress = tag.getInt("Progress");
        this.completedCycles = tag.getInt("CompletedCycles");
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

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.acidTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        this.acidTank.setType(fluid);
        setChangedAndSync(true);
        player.displayClientMessage(Component.translatable(
                "message.reinhardtshbm.fluid.changed_type",
                Component.translatable(fluid.translationKey()),
                1
        ), true);
    }

    public boolean canAcceptInput(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) {
            return false;
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CRYSTALLIZER.get())
                .stream()
                .anyMatch(holder -> holder.value().ingredient().test(stack));
    }

    private void tickServer(Level level) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        applyFluidIdentifierSlot();
        boolean containerChanged = drainContainerIntoTank();

        Optional<RecipeHolder<CrystallizerRecipe>> holder = recipeFor(this.items.get(INPUT_SLOT));
        if (holder.isEmpty()) {
            this.progress = 0;
            this.working = false;
            this.workTime = 600;
            this.currentDemand = currentDemand();
            setLit(false);
            if (containerChanged) {
                setChangedAndSync(true);
            }
            return;
        }

        CrystallizerRecipe recipe = holder.get().value();
        this.workTime = currentDuration(recipe);
        this.currentDemand = currentDemand();
        boolean processed = false;
        int cycles = currentCycleCount();
        for (int cycle = 0; cycle < cycles; cycle++) {
            if (!canProcess(recipe)) {
                if (!processed) {
                    this.progress = 0;
                    this.working = false;
                    setLit(false);
                }
                break;
            }

            this.power -= this.currentDemand;
            this.progress++;
            this.working = true;
            processed = true;
            setLit(true);

            if (this.progress > this.workTime) {
                finishRecipe(recipe, level);
                this.progress = 0;
                this.completedCycles++;
            }
        }

        if (!processed) {
            this.working = false;
            setLit(false);
        }
        setChanged();
        if (containerChanged || processed) {
            setChangedAndSync(processed);
        }
    }

    private void tickClient() {
        this.clientPrevAngle = this.clientAngle;
        if (this.working) {
            this.clientAngle += 5.0F * currentCycleCount();
            if (this.clientAngle >= 360.0F) {
                this.clientAngle -= 360.0F;
                this.clientPrevAngle -= 360.0F;
            }
        }
    }

    private Optional<RecipeHolder<CrystallizerRecipe>> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(
                HbmRecipeTypes.CRYSTALLIZER.get(),
                new CrystallizerRecipe.Input(stack, new HbmFluidStack(this.acidTank.type(), this.acidTank.amount(), this.acidTank.pressure())),
                this.level
        );
    }

    private boolean canProcess(CrystallizerRecipe recipe) {
        ItemStack input = this.items.get(INPUT_SLOT);
        if (input.getCount() < recipe.inputCount() || this.power < this.currentDemand) {
            return false;
        }
        int acidCost = requiredAcid(recipe.acid().amount());
        if (this.acidTank.type() != recipe.acid().type()
                || this.acidTank.pressure() != recipe.acid().pressure()
                || this.acidTank.amount() < acidCost) {
            return false;
        }
        ItemStack result = recipe.result();
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    private void finishRecipe(CrystallizerRecipe recipe, Level level) {
        ItemStack result = recipe.result().copy();
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }

        this.acidTank.drain(recipe.acid().type(), requiredAcid(recipe.acid().amount()), false);
        if (freeChance() <= 0.0F || freeChance() < level.random.nextFloat()) {
            this.items.get(INPUT_SLOT).shrink(recipe.inputCount());
            if (this.items.get(INPUT_SLOT).isEmpty()) {
                this.items.set(INPUT_SLOT, ItemStack.EMPTY);
            }
        }
    }

    private int currentDuration(CrystallizerRecipe recipe) {
        int speed = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        if (speed <= 0) {
            return Math.max(1, recipe.duration());
        }
        return Math.max(1, (int) Math.ceil(recipe.duration() * Math.max(1.0F - 0.25F * speed, 0.25F)));
    }

    private int currentDemand() {
        int speed = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        return BASE_DEMAND + Math.min(speed * 1000, 3000);
    }

    private int currentCycleCount() {
        int overdrive = upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE);
        return Math.min(1 + overdrive * 2, 7);
    }

    private int requiredAcid(int base) {
        int efficiency = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.EFFECT), 3);
        if (efficiency > 0) {
            return base * (efficiency + 2);
        }
        return base;
    }

    private float freeChance() {
        int efficiency = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.EFFECT), 3);
        if (efficiency > 0) {
            return Math.min(efficiency * 0.05F, 0.15F);
        }
        return 0.0F;
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return level;
    }

    private void applyFluidIdentifierSlot() {
        ItemStack stack = this.items.get(FLUID_IDENTIFIER_SLOT);
        if (stack.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition fluid = FluidIdentifierItem.primary(stack);
            if (!fluid.isNone() && fluid != this.acidTank.type()) {
                this.acidTank.setType(fluid);
                setChangedAndSync(true);
            }
        }
    }

    private boolean drainContainerIntoTank() {
        ItemStack input = this.items.get(FLUID_INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        boolean changed = HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.acidTank,
                fluid -> fluid == this.acidTank.type(),
                output -> canPlaceOutput(FLUID_OUTPUT_SLOT, output),
                output -> placeOutput(FLUID_OUTPUT_SLOT, output)
        );
        if (input.isEmpty()) {
            this.items.set(FLUID_INPUT_SLOT, ItemStack.EMPTY);
        }
        return changed;
    }

    private boolean isDrainableContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof InfiniteFluidContainerItem infinite) {
            return infinite.sourceFor(this.acidTank) == this.acidTank.type() && !this.acidTank.type().isNone();
        }
        if (stack.getItem() instanceof HbmFluidContainerItem item && item.isFilledContainer()) {
            return HbmFluidContainerItem.fluid(stack) == this.acidTank.type()
                    && canPlaceOutput(FLUID_OUTPUT_SLOT, item.getCraftingRemainingItem(stack));
        }
        return FluidUtil.getFluidContained(stack)
                .flatMap(contained -> HbmFluids.fromNeoFluid(contained.getFluid()))
                .filter(fluid -> fluid == this.acidTank.type())
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

    private boolean isSlotClogged(int slot) {
        if (slot != INPUT_SLOT) {
            return false;
        }
        ItemStack stack = this.items.get(slot);
        return !stack.isEmpty() && !canAcceptInput(stack);
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof CrystallizerBlock && state.getValue(CrystallizerBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(CrystallizerBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (sync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isSupportedUpgrade(ItemStack stack) {
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED
                || type == MachineUpgradeItem.UpgradeType.EFFECT
                || type == MachineUpgradeItem.UpgradeType.OVERDRIVE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private List<BlockPos> connectorPositions() {
        Direction facing = this.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? this.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.NORTH;
        return List.of(
                connectorPosition(2, 0, 1, facing),
                connectorPosition(2, 0, -1, facing),
                connectorPosition(-2, 0, 1, facing),
                connectorPosition(-2, 0, -1, facing),
                connectorPosition(1, 0, 2, facing),
                connectorPosition(-1, 0, 2, facing),
                connectorPosition(1, 0, -2, facing),
                connectorPosition(-1, 0, -2, facing)
        );
    }

    private BlockPos connectorPosition(int x, int y, int z, Direction facing) {
        BlockPos offset = switch (facing) {
            case EAST -> new BlockPos(-z, y, x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
        return this.worldPosition.offset(offset).immutable();
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

    private final class AcidTankFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? acidTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? acidTank.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return !fluid.isNone() && fluid == acidTank.type() && acidTank.pressure() == 0;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone() || fluid != acidTank.type()) {
                return 0;
            }
            int filled = acidTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
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
