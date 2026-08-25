package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PowerMachineBlock;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.ElectricFurnaceMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

public class ElectricFurnaceBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int UPGRADE_SLOT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_COUNT = 6;
    public static final long DEMAND_PER_TICK = 50L;
    public static final long ENERGY_CAPACITY = 100_000L;

    private static final int BASE_WORK_TIME = 100;
    private static final int[] AUTOMATION_SLOTS = {BATTERY_SLOT, INPUT_SLOT, OUTPUT_SLOT};

    private ItemStack batteryStack = ItemStack.EMPTY;
    private ItemStack inputStack = ItemStack.EMPTY;
    private ItemStack outputStack = ItemStack.EMPTY;
    private ItemStack upgradeStack = ItemStack.EMPTY;
    private long energyStored;
    private long lastInput;
    private int workProgress;
    private int workTime = 100;
    private int completedCycles;
    private int cooldown;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) ElectricFurnaceBlockEntity.this.energyStored;
                case 1 -> (int) ElectricFurnaceBlockEntity.this.lastInput;
                case 2 -> ElectricFurnaceBlockEntity.this.workProgress;
                case 3 -> ElectricFurnaceBlockEntity.this.workTime;
                case 4 -> ElectricFurnaceBlockEntity.this.completedCycles;
                case 5 -> (int) ElectricFurnaceBlockEntity.this.currentConsumption();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ElectricFurnaceBlockEntity.this.energyStored = value;
                case 1 -> ElectricFurnaceBlockEntity.this.lastInput = value;
                case 2 -> ElectricFurnaceBlockEntity.this.workProgress = value;
                case 3 -> ElectricFurnaceBlockEntity.this.workTime = Math.max(1, value);
                case 4 -> ElectricFurnaceBlockEntity.this.completedCycles = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ELECTRIC_FURNACE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity blockEntity) {
        PowerNetworkManager.tickFromEndpoint(level, blockEntity);
        blockEntity.tickWork(level);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (this.energyStored >= ENERGY_CAPACITY) {
            return 0L;
        }
        return ENERGY_CAPACITY - this.energyStored;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.energyStored = Math.min(ENERGY_CAPACITY, this.energyStored + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.workTime <= 0 ? 0 : (this.workProgress * 100) / this.workTime;
        return Component.translatable(
                "message.reinhardtshbm.power.electric_furnace",
                this.lastInput,
                ENERGY_CAPACITY - this.energyStored,
                this.energyStored,
                ENERGY_CAPACITY,
                percent,
                this.completedCycles,
                this.inputStack.isEmpty() ? 0 : this.inputStack.getCount(),
                this.inputStack.isEmpty() ? Component.translatable("message.reinhardtshbm.power.empty_slot") : this.inputStack.getHoverName(),
                this.outputStack.isEmpty() ? 0 : this.outputStack.getCount(),
                this.outputStack.isEmpty() ? Component.translatable("message.reinhardtshbm.power.empty_slot") : this.outputStack.getHoverName()
        );
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.batteryStack.isEmpty()
                && this.inputStack.isEmpty()
                && this.outputStack.isEmpty()
                && this.upgradeStack.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case BATTERY_SLOT -> this.batteryStack;
            case INPUT_SLOT -> this.inputStack;
            case OUTPUT_SLOT -> this.outputStack;
            case UPGRADE_SLOT -> this.upgradeStack;
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
            if (slot == INPUT_SLOT) {
                this.workProgress = 0;
            }
            setChanged();
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
        setSlot(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        if (slot == INPUT_SLOT) {
            this.workProgress = 0;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
            case INPUT_SLOT -> canAcceptInput(stack);
            case UPGRADE_SLOT -> MachineUpgradeItem.isMachineUpgrade(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot != OUTPUT_SLOT && slot != UPGRADE_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT || slot == BATTERY_SLOT && BatteryPackItem.charge(stack) <= 0L;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.batteryStack = ItemStack.EMPTY;
        this.inputStack = ItemStack.EMPTY;
        this.outputStack = ItemStack.EMPTY;
        this.upgradeStack = ItemStack.EMPTY;
        this.workProgress = 0;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.electric_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricFurnaceMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public boolean canAcceptInput(ItemStack stack) {
        return this.level == null || hasSmeltingRecipe(stack, this.level);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        drop(level, pos, this.batteryStack);
        drop(level, pos, this.inputStack);
        drop(level, pos, this.outputStack);
        drop(level, pos, this.upgradeStack);
        this.batteryStack = ItemStack.EMPTY;
        this.inputStack = ItemStack.EMPTY;
        this.outputStack = ItemStack.EMPTY;
        this.upgradeStack = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Battery", this.batteryStack.saveOptional(registries));
        tag.put("Input", this.inputStack.saveOptional(registries));
        tag.put("Output", this.outputStack.saveOptional(registries));
        tag.put("Upgrade", this.upgradeStack.saveOptional(registries));
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("WorkProgress", this.workProgress);
        tag.putInt("WorkTime", this.workTime);
        tag.putInt("CompletedCycles", this.completedCycles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.batteryStack = ItemStack.parseOptional(registries, tag.getCompound("Battery"));
        this.inputStack = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        this.outputStack = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        this.upgradeStack = ItemStack.parseOptional(registries, tag.getCompound("Upgrade"));
        this.energyStored = tag.getLong("EnergyStored");
        this.lastInput = tag.getLong("LastInput");
        this.workProgress = tag.getInt("WorkProgress");
        this.workTime = Math.max(1, tag.getInt("WorkTime"));
        this.completedCycles = tag.getInt("CompletedCycles");
    }

    private void tickWork(Level level) {
        this.energyStored = BatteryPackItem.dischargeIntoMachine(this.batteryStack, this.energyStored, ENERGY_CAPACITY);

        if (this.cooldown > 0) {
            this.cooldown--;
        }

        Optional<RecipeHolder<SmeltingRecipe>> recipe = canSmelt(level);
        if (recipe.isEmpty()) {
            this.workProgress = 0;
            setLit(false);
            return;
        }

        this.workTime = currentWorkTime();
        long consumption = currentConsumption();
        if (this.energyStored < consumption) {
            this.workProgress = 0;
            this.cooldown = 20;
            setLit(false);
            setChanged();
            return;
        }

        if (this.cooldown > 0) {
            this.workProgress = 0;
            setLit(false);
            setChanged();
            return;
        }

        this.energyStored -= consumption;
        this.workProgress++;
        setLit(true);
        if (level.getGameTime() % 20L == 0L) {
            HbmPollution.increment(level, this.worldPosition, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND);
        }

        if (this.workProgress >= this.workTime) {
            finishSmelting(level, recipe.get());
        }
        setChanged();
    }

    private int currentWorkTime() {
        int speedLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
        int powerLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        return Math.max(1, BASE_WORK_TIME - speedLevel * 25 + powerLevel * 10);
    }

    private long currentConsumption() {
        int speedLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
        int powerLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        return Math.max(1L, DEMAND_PER_TICK + speedLevel * 50L - powerLevel * 15L);
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        if (MachineUpgradeItem.upgradeType(this.upgradeStack) != type) {
            return 0;
        }
        return Math.min(3, Math.max(0, MachineUpgradeItem.upgradeTier(this.upgradeStack)));
    }

    private Optional<RecipeHolder<SmeltingRecipe>> canSmelt(Level level) {
        if (this.inputStack.isEmpty()) {
            return Optional.empty();
        }

        SingleRecipeInput input = new SingleRecipeInput(this.inputStack);
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (recipe.isEmpty()) {
            return Optional.empty();
        }

        ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            return Optional.empty();
        }

        if (this.outputStack.isEmpty()) {
            return recipe;
        }

        if (!ItemStack.isSameItemSameComponents(this.outputStack, result)) {
            return Optional.empty();
        }

        return this.outputStack.getCount() + result.getCount() <= this.outputStack.getMaxStackSize()
                ? recipe
                : Optional.empty();
    }

    private boolean hasSmeltingRecipe(ItemStack stack, Level level) {
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .isPresent();
    }

    private void setSlot(int slot, ItemStack stack) {
        switch (slot) {
            case BATTERY_SLOT -> this.batteryStack = stack;
            case INPUT_SLOT -> this.inputStack = stack;
            case OUTPUT_SLOT -> this.outputStack = stack;
            case UPGRADE_SLOT -> this.upgradeStack = stack;
            default -> {
            }
        }
    }

    private void finishSmelting(Level level, RecipeHolder<SmeltingRecipe> recipe) {
        SingleRecipeInput input = new SingleRecipeInput(this.inputStack);
        ItemStack result = recipe.value().assemble(input, level.registryAccess());
        this.inputStack.shrink(1);

        if (this.outputStack.isEmpty()) {
            this.outputStack = result.copy();
        } else {
            this.outputStack.grow(result.getCount());
        }

        this.workProgress = 0;
        this.completedCycles++;
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

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }

        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof PowerMachineBlock && state.getValue(PowerMachineBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(PowerMachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }
}
