package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.ReactorSensorItem;
import com.reinhardt.hbm.menu.ReactorControlMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Direct port of TileEntityReactorControl, excluding only the old OpenComputers bridge. */
public final class ReactorControlBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_SENSOR = 0;
    public static final int SLOT_COUNT = 1;
    public static final int DATA_COUNT = 9;
    private static final int[] SENSOR_SLOT = {SLOT_SENSOR};
    private static final int MAX_HEAT = 50_000;
    private static final int LEVEL_SCALE = 10_000;

    public enum RodFunction {
        LINEAR,
        QUAD,
        LOG
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean linked;
    private int flux;
    private double level;
    private int heat;
    private int levelLower;
    private int levelUpper;
    private int heatLower;
    private int heatUpper;
    private RodFunction function = RodFunction.LINEAR;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ReactorControlBlockEntity.this.heat;
                case 1 -> ReactorControlBlockEntity.this.flux;
                case 2 -> (int) Math.round(ReactorControlBlockEntity.this.level * LEVEL_SCALE);
                case 3 -> ReactorControlBlockEntity.this.linked ? 1 : 0;
                case 4 -> ReactorControlBlockEntity.this.levelUpper;
                case 5 -> ReactorControlBlockEntity.this.levelLower;
                case 6 -> ReactorControlBlockEntity.this.heatUpper;
                case 7 -> ReactorControlBlockEntity.this.heatLower;
                case 8 -> ReactorControlBlockEntity.this.function.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ReactorControlBlockEntity.this.heat = value;
                case 1 -> ReactorControlBlockEntity.this.flux = value;
                case 2 -> ReactorControlBlockEntity.this.level = value / (double) LEVEL_SCALE;
                case 3 -> ReactorControlBlockEntity.this.linked = value != 0;
                case 4 -> ReactorControlBlockEntity.this.levelUpper = value;
                case 5 -> ReactorControlBlockEntity.this.levelLower = value;
                case 6 -> ReactorControlBlockEntity.this.heatUpper = value;
                case 7 -> ReactorControlBlockEntity.this.heatLower = value;
                case 8 -> ReactorControlBlockEntity.this.function = function(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ReactorControlBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.REACTOR_CONTROL.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReactorControlBlockEntity controller) {
        controller.tickServer(level);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public int comparatorOutput() {
        return Mth.clamp((int) Math.ceil(this.heat * 15.0D / MAX_HEAT), 0, 15);
    }

    public int levelUpper() {
        return this.levelUpper;
    }

    public int levelLower() {
        return this.levelLower;
    }

    public int heatUpper() {
        return this.heatUpper;
    }

    public int heatLower() {
        return this.heatLower;
    }

    public RodFunction function() {
        return this.function;
    }

    public void setFunction(int value) {
        this.function = function(value);
        setChanged();
    }

    public void setParameters(int levelUpper, int levelLower, int heatUpper, int heatLower) {
        this.levelUpper = Mth.clamp(levelUpper, 0, 100);
        this.levelLower = Mth.clamp(levelLower, 0, 100);
        this.heatUpper = Mth.clamp(heatUpper, 0, 50_000);
        this.heatLower = Mth.clamp(heatLower, 0, 50_000);
        setChanged();
    }

    /** The exact TileEntityReactorControl curve formulas, with invalid zero-span input left inert. */
    public double getTargetLevel(RodFunction curve, int currentHeat) {
        return switch (curve) {
            case LINEAR -> interpolate(currentHeat, this.heatLower, this.heatUpper, this.levelLower, this.levelUpper);
            case LOG -> logarithmic(currentHeat);
            case QUAD -> quadratic(currentHeat);
        };
    }

    public int displayTemperature() {
        return this.linked ? ResearchReactorBlockEntity.temperatureForHeat(this.heat) : 0;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(SLOT_SENSOR).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_SENSOR && stack.is(HbmItems.REACTOR_SENSOR.get());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SENSOR_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
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
        this.items.set(SLOT_SENSOR, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorControlMenu(containerId, playerInventory, this, this.menuData, this.worldPosition);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        ItemStack sensor = this.items.get(SLOT_SENSOR);
        if (!sensor.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, sensor.copy()));
            this.items.set(SLOT_SENSOR, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Sensor", this.items.get(SLOT_SENSOR).saveOptional(registries));
        tag.putBoolean("Linked", this.linked);
        tag.putInt("Flux", this.flux);
        tag.putDouble("Level", this.level);
        tag.putInt("Heat", this.heat);
        tag.putInt("LevelLower", this.levelLower);
        tag.putInt("LevelUpper", this.levelUpper);
        tag.putInt("HeatLower", this.heatLower);
        tag.putInt("HeatUpper", this.heatUpper);
        tag.putInt("Function", this.function.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.set(SLOT_SENSOR, ItemStack.parseOptional(registries, tag.getCompound("Sensor")));
        this.linked = tag.getBoolean("Linked");
        this.flux = tag.getInt("Flux");
        this.level = tag.getDouble("Level");
        this.heat = tag.getInt("Heat");
        this.levelLower = tag.getInt("LevelLower");
        this.levelUpper = tag.getInt("LevelUpper");
        this.heatLower = tag.getInt("HeatLower");
        this.heatUpper = tag.getInt("HeatUpper");
        this.function = function(tag.getInt("Function"));
    }

    private void tickServer(Level level) {
        this.linked = false;
        ItemStack sensor = this.items.get(SLOT_SENSOR);
        BlockPos target = sensor.is(HbmItems.REACTOR_SENSOR.get()) ? ReactorSensorItem.target(sensor) : null;
        if (target != null && level.getBlockEntity(target) instanceof ResearchReactorBlockEntity reactor) {
            this.linked = true;
            this.flux = reactor.totalFlux();
            this.level = reactor.controlLevel();
            this.heat = reactor.heat();

            double lowerBound = Math.min(this.heatLower, this.heatUpper);
            double upperBound = Math.max(this.heatLower, this.heatUpper);
            double fauxLevel;
            if (this.heat < lowerBound) {
                fauxLevel = this.levelLower;
            } else if (this.heat > upperBound) {
                fauxLevel = this.levelUpper;
            } else {
                fauxLevel = getTargetLevel(this.function, this.heat);
            }
            if (Double.isFinite(fauxLevel)) {
                reactor.setTargetLevel(Mth.clamp(fauxLevel * 0.01D, 0.0D, 1.0D));
            }
        } else {
            this.flux = 0;
            this.level = 0.0D;
            this.heat = 0;
        }
        setChanged();
    }

    private double interpolate(int current, int inputLow, int inputHigh, int outputLow, int outputHigh) {
        if (inputHigh == inputLow) {
            return Double.NaN;
        }
        return (current - inputLow) * ((outputHigh - outputLow) / (double) (inputHigh - inputLow)) + outputLow;
    }

    private double quadratic(int currentHeat) {
        if (this.heatUpper == this.heatLower) {
            return Double.NaN;
        }
        return Math.pow((currentHeat - this.heatLower) / (double) (this.heatUpper - this.heatLower), 2.0D)
                * (this.levelUpper - this.levelLower) + this.levelLower;
    }

    private double logarithmic(int currentHeat) {
        if (this.heatLower == this.heatUpper) {
            return Double.NaN;
        }
        return Math.pow((currentHeat - this.heatUpper) / (double) (this.heatLower - this.heatUpper), 2.0D)
                * (this.levelLower - this.levelUpper) + this.levelUpper;
    }

    private static RodFunction function(int value) {
        RodFunction[] values = RodFunction.values();
        return values[Mth.clamp(value, 0, values.length - 1)];
    }

    private static boolean validSlot(int slot) {
        return slot == SLOT_SENSOR;
    }
}
