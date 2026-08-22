package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.LiquefactorMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.LiquefactionRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
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

public class LiquefactorBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 4;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_COUNT = 8;
    public static final int TANK_CAPACITY = 24_000;
    public static final long MAX_POWER = 100_000L;
    public static final int BASE_USAGE = 500;
    public static final int BASE_PROCESS_TIME = 100;
    private static final int PUSH_PER_PORT = 8_000;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank tank = new HbmFluidTank(TANK_CAPACITY);
    private long power;
    private long lastInput;
    private int progress;
    private int usage = BASE_USAGE;
    private int processTime = BASE_PROCESS_TIME;
    private boolean working;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) LiquefactorBlockEntity.this.power;
                case 1 -> (int) LiquefactorBlockEntity.this.lastInput;
                case 2 -> LiquefactorBlockEntity.this.progress;
                case 3 -> LiquefactorBlockEntity.this.processTime;
                case 4 -> LiquefactorBlockEntity.this.usage;
                case 5 -> LiquefactorBlockEntity.this.tank.type().oldId();
                case 6 -> LiquefactorBlockEntity.this.tank.amount();
                case 7 -> LiquefactorBlockEntity.this.tank.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> LiquefactorBlockEntity.this.power = value;
                case 1 -> LiquefactorBlockEntity.this.lastInput = value;
                case 2 -> LiquefactorBlockEntity.this.progress = value;
                case 3 -> LiquefactorBlockEntity.this.processTime = Math.max(1, value);
                case 4 -> LiquefactorBlockEntity.this.usage = Math.max(1, value);
                case 5 -> LiquefactorBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 6 -> LiquefactorBlockEntity.this.tank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public LiquefactorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.LIQUEFACTOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LiquefactorBlockEntity liquefactor) {
        if (level.isClientSide) {
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, liquefactor);
        liquefactor.tickServer(level);
    }

    public HbmFluidTank tank() {
        return this.tank;
    }

    public int progress() {
        return this.progress;
    }

    public int processTime() {
        return this.processTime;
    }

    public boolean isWorking() {
        return this.working;
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        BlockPos pos = this.worldPosition;
        return List.of(
                pos.offset(0, 4, 0).immutable(),
                pos.offset(0, -1, 0).immutable(),
                pos.offset(2, 1, 0).immutable(),
                pos.offset(-2, 1, 0).immutable(),
                pos.offset(0, 1, 2).immutable(),
                pos.offset(0, 1, -2).immutable()
        );
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
        long requested = Math.max(BASE_USAGE, this.usage);
        return Math.min(requested, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.processTime <= 0 ? 0 : this.progress * 100 / this.processTime;
        return Component.translatable(
                "message.reinhardtshbm.power.liquefactor",
                this.lastInput,
                this.usage,
                this.power,
                MAX_POWER,
                percent
        );
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new LiquefactorFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public static List<Port> portsFor(BlockPos pos) {
        return List.of(
                Port.fromConnector(pos.offset(0, 4, 0), Direction.UP),
                Port.fromConnector(pos.offset(0, -1, 0), Direction.DOWN),
                Port.fromConnector(pos.offset(2, 1, 0), Direction.EAST),
                Port.fromConnector(pos.offset(-2, 1, 0), Direction.WEST),
                Port.fromConnector(pos.offset(0, 1, 2), Direction.SOUTH),
                Port.fromConnector(pos.offset(0, 1, -2), Direction.NORTH)
        );
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
        if (slot == INPUT_SLOT) {
            this.progress = 0;
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            updateUpgrades();
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            return recipeFor(stack).isPresent();
        }
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return isSupportedUpgrade(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
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
        return Component.translatable("container.reinhardtshbm.machine_liquefactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LiquefactorMenu(containerId, playerInventory, this, this.menuData);
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
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("Usage", this.usage);
        tag.putInt("ProcessTime", this.processTime);
        tag.putBoolean("Working", this.working);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.tank.load(tag.getCompound("Tank"));
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = tag.getLong("LastInput");
        this.progress = tag.getInt("Progress");
        this.usage = Math.max(1, tag.getInt("Usage"));
        this.processTime = Math.max(1, tag.getInt("ProcessTime"));
        this.working = tag.getBoolean("Working");
        updateUpgrades();
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
        updateUpgrades();
        process();
        pushOutput(level);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void process() {
        boolean wasWorking = this.working;
        this.working = false;
        Optional<RecipeHolder<LiquefactionRecipe>> holder = currentRecipe();
        if (holder.isEmpty() || !canProcess(holder.get().value())) {
            this.progress = 0;
            syncIfWorkingChanged(wasWorking);
            return;
        }

        if (this.power < this.usage) {
            syncIfWorkingChanged(wasWorking);
            return;
        }

        this.power -= this.usage;
        this.progress++;
        this.working = true;

        if (this.progress >= this.processTime) {
            finishRecipe(holder.get().value());
            this.progress = 0;
            setChangedAndSync(true);
        } else {
            syncIfWorkingChanged(wasWorking);
        }
    }

    private void pushOutput(Level level) {
        if (this.tank.amount() <= 0 || this.tank.type().isNone()) {
            return;
        }
        for (Port port : portsFor(this.worldPosition)) {
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

    private Optional<RecipeHolder<LiquefactionRecipe>> currentRecipe() {
        return recipeFor(this.items.get(INPUT_SLOT));
    }

    private Optional<RecipeHolder<LiquefactionRecipe>> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.LIQUEFACTION.get())
                .stream()
                .filter(holder -> holder.value().matches(input, this.level))
                .findFirst();
    }

    private boolean canProcess(LiquefactionRecipe recipe) {
        if (recipe.output().fluid().isNone() || recipe.output().amount() <= 0) {
            return false;
        }
        if (this.tank.amount() > 0 && this.tank.type() != recipe.output().fluid()) {
            return false;
        }
        return this.tank.amount() + recipe.output().amount() <= this.tank.capacity();
    }

    private void finishRecipe(LiquefactionRecipe recipe) {
        this.tank.fill(recipe.output().fluid(), recipe.output().amount(), false);
        this.items.get(INPUT_SLOT).shrink(1);
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private void updateUpgrades() {
        int speed = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int power = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3);
        this.processTime = Math.max(1, BASE_PROCESS_TIME - (BASE_PROCESS_TIME / 4) * speed);
        this.usage = Math.max(1, (BASE_USAGE + BASE_USAGE * speed) / (power + 1));
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

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : portsFor(this.worldPosition)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void syncIfWorkingChanged(boolean wasWorking) {
        if (wasWorking != this.working) {
            setChangedAndSync(true);
        }
    }

    private void sync() {
        setChangedAndSync(true);
        if (this.level != null && !this.level.isClientSide) {
            for (Port port : portsFor(this.worldPosition)) {
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

    private static boolean isSupportedUpgrade(ItemStack stack) {
        if (!MachineUpgradeItem.isMachineUpgrade(stack)) {
            return false;
        }
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED || type == MachineUpgradeItem.UpgradeType.POWER;
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

    public record Port(BlockPos pos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class LiquefactorFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private LiquefactorFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
