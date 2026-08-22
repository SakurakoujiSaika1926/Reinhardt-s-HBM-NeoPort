package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CentrifugeBlock;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.CentrifugeMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.CentrifugeRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CentrifugeBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int OUTPUT_START = 2;
    public static final int OUTPUT_END = 6;
    public static final int UPGRADE_START = 6;
    public static final int UPGRADE_END = 8;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 8;
    public static final long MAX_POWER = 100_000L;
    public static final int BASE_PROCESSING_SPEED = 200;
    public static final int BASE_CONSUMPTION = 200;
    private static final int TOWER_HEIGHT = 4;

    private static final int[] AUTOMATION_SLOTS = {INPUT_SLOT, 2, 3, 4, 5};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private long power;
    private long lastInput;
    private int progress;
    private int completedCycles;
    private int currentConsumption = BASE_CONSUMPTION;
    private int currentSpeed = 1;
    private int soundCycle;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) CentrifugeBlockEntity.this.power;
                case 1 -> (int) CentrifugeBlockEntity.this.lastInput;
                case 2 -> CentrifugeBlockEntity.this.progress;
                case 3 -> BASE_PROCESSING_SPEED;
                case 4 -> CentrifugeBlockEntity.this.currentConsumption;
                case 5 -> CentrifugeBlockEntity.this.currentSpeed;
                case 6 -> CentrifugeBlockEntity.this.completedCycles;
                case 7 -> CentrifugeBlockEntity.this.isWorking() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CentrifugeBlockEntity.this.power = value;
                case 1 -> CentrifugeBlockEntity.this.lastInput = value;
                case 2 -> CentrifugeBlockEntity.this.progress = value;
                case 4 -> CentrifugeBlockEntity.this.currentConsumption = value;
                case 5 -> CentrifugeBlockEntity.this.currentSpeed = value;
                case 6 -> CentrifugeBlockEntity.this.completedCycles = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CentrifugeBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.CENTRIFUGE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity centrifuge) {
        PowerNetworkManager.tickFromEndpoint(level, centrifuge);
        centrifuge.tickServer(level);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public boolean isWorking() {
        return this.progress > 0;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return towerPowerConnectorPositions(this.worldPosition);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return isTowerPowerConnector(this.worldPosition, connectorPos, machineSide);
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
        return Math.min(Math.max(BASE_CONSUMPTION, this.currentConsumption), MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.progress * 100 / BASE_PROCESSING_SPEED;
        return Component.translatable(
                "message.reinhardtshbm.power.centrifuge",
                this.lastInput,
                this.currentConsumption,
                this.power,
                MAX_POWER,
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
        if (slot == INPUT_SLOT) {
            this.progress = 0;
        }
        setChanged();
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
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> canAcceptInput(stack);
            case BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
            case 6, 7 -> isSupportedUpgrade(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && canAcceptInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_END;
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
        return Component.translatable("container.reinhardtshbm.centrifuge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CentrifugeMenu(containerId, playerInventory, this, this.menuData);
    }

    public boolean canAcceptInput(ItemStack stack) {
        return !stack.isEmpty() && recipeFor(stack).isPresent();
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
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("CompletedCycles", this.completedCycles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.progress = tag.getInt("Progress");
        this.completedCycles = tag.getInt("CompletedCycles");
    }

    private void tickServer(Level level) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        updateUpgrades();

        Optional<RecipeHolder<CentrifugeRecipe>> holder = recipeFor(this.items.get(INPUT_SLOT));
        if (holder.isEmpty() || !canProcess(holder.get().value())) {
            this.progress = 0;
            setLit(false);
            return;
        }

        if (this.power <= 0) {
            setLit(false);
            return;
        }

        this.power = Math.max(0L, this.power - this.currentConsumption);
        this.progress += this.currentSpeed;
        playWorkingSound(level);
        setLit(true);

        if (this.progress >= BASE_PROCESSING_SPEED) {
            finishRecipe(holder.get().value());
            this.progress = 0;
            this.completedCycles++;
        }
        setChanged();
    }

    private Optional<RecipeHolder<CentrifugeRecipe>> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(HbmRecipeTypes.CENTRIFUGE.get(), new CentrifugeRecipe.Input(stack), this.level);
    }

    private boolean canProcess(CentrifugeRecipe recipe) {
        ItemStack input = this.items.get(INPUT_SLOT);
        return input.getCount() >= recipe.inputCount() && canFitOutputs(recipe.results());
    }

    private boolean canFitOutputs(List<ItemStack> results) {
        for (ItemStack result : results) {
            if (!canFitOutput(result)) {
                return false;
            }
        }
        return true;
    }

    private boolean canFitOutput(ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }
        int remaining = result.getCount();
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack output = this.items.get(slot);
            if (output.isEmpty()) {
                remaining -= result.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(output, result)) {
                remaining -= output.getMaxStackSize() - output.getCount();
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private void finishRecipe(CentrifugeRecipe recipe) {
        this.items.get(INPUT_SLOT).shrink(recipe.inputCount());
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        for (ItemStack result : recipe.results()) {
            insertOutput(result.copy());
        }
    }

    private void insertOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !stack.isEmpty(); slot++) {
            ItemStack output = this.items.get(slot);
            if (ItemStack.isSameItemSameComponents(output, stack)) {
                int moved = Math.min(stack.getCount(), output.getMaxStackSize() - output.getCount());
                if (moved > 0) {
                    output.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !stack.isEmpty(); slot++) {
            if (this.items.get(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack inserted = stack.copyWithCount(moved);
                this.items.set(slot, inserted);
                stack.shrink(moved);
            }
        }
    }

    private void updateUpgrades() {
        int speedLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
        int powerLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        int overdriveLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE);

        int speed = 1 + speedLevel;
        int consumption = BASE_CONSUMPTION + speedLevel * BASE_CONSUMPTION;
        speed *= 1 + overdriveLevel * 5;
        consumption += overdriveLevel * BASE_CONSUMPTION * 50;
        consumption /= 1 + powerLevel;

        this.currentSpeed = Math.max(1, speed);
        this.currentConsumption = Math.max(1, consumption);
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

    private void playWorkingSound(Level level) {
        if (this.soundCycle <= 0) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.CENTRIFUGE_OPERATE.get(), SoundSource.BLOCKS, 0.65F, 0.65F);
            this.soundCycle = 36;
        }
        this.soundCycle--;
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof CentrifugeBlock && state.getValue(CentrifugeBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(CentrifugeBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isSupportedUpgrade(ItemStack stack) {
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED
                || type == MachineUpgradeItem.UpgradeType.POWER
                || type == MachineUpgradeItem.UpgradeType.OVERDRIVE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static List<BlockPos> towerPowerConnectorPositions(BlockPos core) {
        ArrayList<BlockPos> connectors = new ArrayList<>(2 + TOWER_HEIGHT * 4);
        connectors.add(core.below().immutable());
        connectors.add(core.above(TOWER_HEIGHT).immutable());
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            BlockPos column = core.above(y);
            connectors.add(column.east().immutable());
            connectors.add(column.west().immutable());
            connectors.add(column.south().immutable());
            connectors.add(column.north().immutable());
        }
        return List.copyOf(connectors);
    }

    private static boolean isTowerPowerConnector(BlockPos core, BlockPos connectorPos, Direction machineSide) {
        if (connectorPos.equals(core.below())) {
            return machineSide == Direction.DOWN;
        }
        if (connectorPos.equals(core.above(TOWER_HEIGHT))) {
            return machineSide == Direction.UP;
        }
        if (!machineSide.getAxis().isHorizontal()) {
            return false;
        }
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            if (connectorPos.equals(core.above(y).relative(machineSide))) {
                return true;
            }
        }
        return false;
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
