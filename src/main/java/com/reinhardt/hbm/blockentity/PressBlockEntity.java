package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PressMachineBlock;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.item.StampItem;
import com.reinhardt.hbm.menu.MachineEPressMenu;
import com.reinhardt.hbm.menu.MachinePressMenu;
import com.reinhardt.hbm.network.PressAnimationPayload;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.PressRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class PressBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int FIRE_FUEL_SLOT = 0;
    public static final int STAMP_SLOT = 1;
    public static final int INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int FIRE_STORAGE_START = 4;
    public static final int FIRE_STORAGE_END = 13;
    public static final int ELECTRIC_BATTERY_SLOT = 0;
    public static final int ELECTRIC_UPGRADE_SLOT = 4;
    public static final int FIRE_SLOT_COUNT = 13;
    public static final int ELECTRIC_SLOT_COUNT = 5;
    public static final int DATA_COUNT = 7;
    public static final int MAX_PROGRESS = 200;
    public static final int MAX_SPEED = 400;
    public static final int PROGRESS_AT_MAX_SPEED = 25;
    public static final int FIRE_BURN_PER_OPERATION = 200;
    public static final long ELECTRIC_ENERGY_CAPACITY = 50_000L;
    public static final long ELECTRIC_DEMAND_PER_TICK = 100L;

    private static final int[] NO_AUTOMATION_SLOTS = {};
    private static final int[] FIRE_FUEL_AUTOMATION_SLOTS = {FIRE_FUEL_SLOT};
    private static final int[] FIRE_PROCESS_AUTOMATION_SLOTS = {INPUT_SLOT, OUTPUT_SLOT};
    private static final int[] FIRE_STAMP_AUTOMATION_SLOTS = {STAMP_SLOT};
    private static final int[] ELECTRIC_AUTOMATION_SLOTS = {STAMP_SLOT, INPUT_SLOT, OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(FIRE_SLOT_COUNT, ItemStack.EMPTY);
    private long energyStored;
    private long lastInput;
    private int burnTime;
    private int speed;
    private int progress;
    private int completedCycles;
    private int delay;
    private boolean retracting;
    private boolean wasWorking;
    private int clientProgress;
    private int clientPrevProgress;
    private boolean clientAnimationInitialized;
    private int lastSyncedProgress = Integer.MIN_VALUE;
    private int lastSyncedSpeed = Integer.MIN_VALUE;
    private int lastSyncedDelay = Integer.MIN_VALUE;
    private boolean lastSyncedRetracting;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) PressBlockEntity.this.energyStored;
                case 1 -> PressBlockEntity.this.kind() == Kind.FIRE ? PressBlockEntity.this.burnTime : (int) PressBlockEntity.this.lastInput;
                case 2 -> PressBlockEntity.this.progress;
                case 3 -> PressBlockEntity.this.speed;
                case 4 -> PressBlockEntity.this.completedCycles;
                case 5 -> (int) PressBlockEntity.this.currentElectricDemand();
                case 6 -> PressBlockEntity.this.retracting ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> PressBlockEntity.this.energyStored = value;
                case 1 -> {
                    if (PressBlockEntity.this.kind() == Kind.FIRE) {
                        PressBlockEntity.this.burnTime = value;
                    } else {
                        PressBlockEntity.this.lastInput = value;
                    }
                }
                case 2 -> PressBlockEntity.this.progress = value;
                case 3 -> PressBlockEntity.this.speed = value;
                case 4 -> PressBlockEntity.this.completedCycles = value;
                case 6 -> PressBlockEntity.this.retracting = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PressBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.PRESS.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PressBlockEntity blockEntity) {
        if (blockEntity.kind() == Kind.ELECTRIC) {
            PowerNetworkManager.tickFromEndpoint(level, blockEntity);
            blockEntity.tickElectric(level);
        } else {
            blockEntity.tickFire(level);
        }
    }

    public Kind kind() {
        return this.getBlockState().is(HbmBlocks.MACHINE_EPRESS.get()) ? Kind.ELECTRIC : Kind.FIRE;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        if (kind() != Kind.ELECTRIC) {
            return List.of();
        }
        return List.of(
                this.worldPosition.north().immutable(),
                this.worldPosition.east().immutable(),
                this.worldPosition.south().immutable(),
                this.worldPosition.west().immutable(),
                this.worldPosition.above().immutable(),
                this.worldPosition.below().immutable()
        );
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (kind() != Kind.ELECTRIC || this.energyStored >= ELECTRIC_ENERGY_CAPACITY) {
            return 0L;
        }
        return ELECTRIC_ENERGY_CAPACITY - this.energyStored;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        if (kind() != Kind.ELECTRIC) {
            return;
        }
        this.energyStored = Math.min(ELECTRIC_ENERGY_CAPACITY, this.energyStored + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        if (kind() == Kind.FIRE) {
            return Component.translatable(
                    "message.reinhardtshbm.power.press",
                    this.speed * 100 / MAX_SPEED,
                    this.burnTime / FIRE_BURN_PER_OPERATION,
                    this.progress * 100 / MAX_PROGRESS,
                    this.completedCycles
            );
        }
        return Component.translatable(
                "message.reinhardtshbm.power.epress",
                this.lastInput,
                currentElectricDemand(),
                this.energyStored,
                ELECTRIC_ENERGY_CAPACITY,
                this.progress * 100 / MAX_PROGRESS,
                this.completedCycles
        );
    }

    @Override
    public int getContainerSize() {
        return kind() == Kind.ELECTRIC ? ELECTRIC_SLOT_COUNT : FIRE_SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        int size = getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            if (!this.items.get(slot).isEmpty()) {
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

        ItemStack previous = stack.copy();
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            resetProgressIfProcessIdentityChanged(slot, previous, this.items.get(slot));
            setChangedAndSync(false);
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
        resetProgressIfProcessIdentityChanged(slot, removed, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        ItemStack previous = this.items.get(slot).copy();
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        resetProgressIfProcessIdentityChanged(slot, previous, stack);
        setChangedAndSync(false);
    }

    private void resetProgressIfProcessIdentityChanged(int slot, ItemStack previous, ItemStack current) {
        if ((slot != INPUT_SLOT && slot != STAMP_SLOT) || this.retracting) {
            return;
        }
        if (!sameProcessIngredient(previous, current)) {
            this.progress = 0;
        }
    }

    private static boolean sameProcessIngredient(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }
        // Stack size is deliberately excluded: one-at-a-time hopper/funnel
        // transfers do not change the press recipe being worked.
        return ItemStack.isSameItemSameComponents(first, second);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (kind() == Kind.ELECTRIC) {
            return switch (slot) {
                case ELECTRIC_BATTERY_SLOT -> ShredderBlockEntity.isBattery(stack);
                case STAMP_SLOT -> StampItem.isStamp(stack);
                case INPUT_SLOT -> canAcceptInput(stack);
                case ELECTRIC_UPGRADE_SLOT -> isSupportedUpgrade(stack);
                default -> false;
            };
        }

        return switch (slot) {
            case FIRE_FUEL_SLOT -> WoodBurnerBlockEntity.fuelDuration(stack) > 0;
            case STAMP_SLOT -> StampItem.isStamp(stack);
            // TileEntityMachinePress accepted any non-fuel, non-stamp item in the
            // input slot. A valid press ingredient takes priority when an item
            // is also fuel (notably coal coke for the graphite-ingot recipe).
            case INPUT_SLOT -> canAcceptInput(stack)
                    || !StampItem.isRegisteredStamp(stack) && WoodBurnerBlockEntity.fuelDuration(stack) <= 0;
            case OUTPUT_SLOT -> false;
            default -> slot >= FIRE_STORAGE_START && slot < FIRE_STORAGE_END;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (kind() == Kind.ELECTRIC) {
            return ELECTRIC_AUTOMATION_SLOTS;
        }
        return getSlotsForAccessor(this.worldPosition, side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItemThroughAccessor(this.worldPosition, slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canTakeItemThroughAccessor(this.worldPosition, slot, stack, side);
    }

    /**
     * Exposes the modern three-tier fire press automation layout. The core is
     * the bottom fuel tier, with the process tier and stamp tier directly above
     * it. The queried face deliberately does not change a tier's role, so
     * hoppers, Create funnels/chutes and capability-based buses agree.
     */
    public int[] getSlotsForAccessor(BlockPos accessorPos, @Nullable Direction side) {
        if (kind() == Kind.ELECTRIC) {
            return ELECTRIC_AUTOMATION_SLOTS;
        }
        int layer = accessorPos.getY() - this.worldPosition.getY();
        return switch (layer) {
            case 0 -> FIRE_FUEL_AUTOMATION_SLOTS;
            case 1 -> FIRE_PROCESS_AUTOMATION_SLOTS;
            case 2 -> FIRE_STAMP_AUTOMATION_SLOTS;
            default -> NO_AUTOMATION_SLOTS;
        };
    }

    public boolean canPlaceItemThroughAccessor(
            BlockPos accessorPos,
            int slot,
            ItemStack stack,
            @Nullable Direction side
    ) {
        return slot != OUTPUT_SLOT
                && containsSlot(getSlotsForAccessor(accessorPos, side), slot)
                && canPlaceItem(slot, stack);
    }

    public boolean canTakeItemThroughAccessor(
            BlockPos accessorPos,
            int slot,
            ItemStack stack,
            @Nullable Direction side
    ) {
        return slot == OUTPUT_SLOT && containsSlot(getSlotsForAccessor(accessorPos, side), slot);
    }

    private static boolean containsSlot(int[] slots, int slot) {
        for (int accessibleSlot : slots) {
            if (accessibleSlot == slot) {
                return true;
            }
        }
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
        this.progress = 0;
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(kind() == Kind.ELECTRIC ? "container.reinhardtshbm.epress" : "container.reinhardtshbm.press");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return kind() == Kind.ELECTRIC
                ? new MachineEPressMenu(containerId, playerInventory, this, this.menuData)
                : new MachinePressMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public boolean canAcceptInput(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) {
            return false;
        }
        for (RecipeHolder<PressRecipe> holder : this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRESS.get())) {
            if (holder.value().ingredient().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public int energy() {
        return (int) this.energyStored;
    }

    public int burnTime() {
        return this.burnTime;
    }

    public int speed() {
        return this.speed;
    }

    public int progress() {
        return this.progress;
    }

    public ItemStack visualInput() {
        return this.items.get(INPUT_SLOT);
    }

    public void acceptAnimationSync(int progress, int speed, int delay, boolean retracting) {
        int clampedProgress = Math.max(0, Math.min(MAX_PROGRESS, progress));
        if (!this.clientAnimationInitialized) {
            this.clientProgress = clampedProgress;
            this.clientPrevProgress = clampedProgress;
            this.clientAnimationInitialized = true;
        } else {
            this.clientPrevProgress = this.clientProgress;
            this.clientProgress = clampedProgress;
        }
        this.progress = clampedProgress;
        this.speed = Math.max(0, Math.min(MAX_SPEED, speed));
        this.delay = Math.max(0, delay);
        this.retracting = retracting;
    }

    public float clientHeadTravel(float partialTick) {
        return (float) lerp(this.clientPrevProgress, this.clientProgress, partialTick) * 0.875F / MAX_PROGRESS;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        int size = getContainerSize();
        for (int slot = 0; slot < size; slot++) {
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
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("Speed", this.speed);
        tag.putInt("Progress", this.progress);
        tag.putInt("CompletedCycles", this.completedCycles);
        tag.putInt("Delay", this.delay);
        tag.putBoolean("Retracting", this.retracting);
        tag.putBoolean("WasWorking", this.wasWorking);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.energyStored = tag.getLong("EnergyStored");
        this.lastInput = tag.getLong("LastInput");
        this.burnTime = tag.getInt("BurnTime");
        this.speed = tag.getInt("Speed");
        this.progress = tag.getInt("Progress");
        this.completedCycles = tag.getInt("CompletedCycles");
        this.delay = tag.getInt("Delay");
        this.retracting = tag.getBoolean("Retracting");
        this.wasWorking = tag.getBoolean("WasWorking");
        if (!this.clientAnimationInitialized) {
            this.clientProgress = this.progress;
            this.clientPrevProgress = this.progress;
            this.clientAnimationInitialized = true;
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

    private void tickFire(Level level) {
        boolean canProcess = canProcess(level);
        boolean preheated = isPreheated(level);
        boolean impact = false;

        if ((canProcess || this.retracting) && this.burnTime >= FIRE_BURN_PER_OPERATION) {
            this.speed = Math.min(MAX_SPEED, this.speed + (preheated ? 4 : 1));
        } else {
            this.speed = Math.max(0, this.speed - 1);
        }

        if (this.delay <= 0) {
            int stampSpeed = this.speed * PROGRESS_AT_MAX_SPEED / MAX_SPEED;
            if (this.retracting) {
                this.progress -= stampSpeed;
                if (this.progress <= 0) {
                    this.progress = 0;
                    this.retracting = false;
                    this.delay = 5;
                }
            } else if (canProcess) {
                this.progress += stampSpeed;
                if (this.progress >= MAX_PROGRESS) {
                    this.progress = MAX_PROGRESS;
                    impact = craft(level);
                    this.retracting = true;
                    this.delay = 5;
                    if (this.burnTime >= FIRE_BURN_PER_OPERATION) {
                        this.burnTime -= FIRE_BURN_PER_OPERATION;
                    }
                }
            }
        } else {
            this.delay--;
        }

        if (!canProcess && !this.retracting && this.progress > 0) {
            this.retracting = true;
        }

        if (!this.items.get(FIRE_FUEL_SLOT).isEmpty() && this.burnTime < FIRE_BURN_PER_OPERATION) {
            startBurningFuel();
        }

        setLit(this.progress > 0 || this.speed > 0);
        syncActiveVisuals(level);
        syncAnimation(level, impact);
    }

    private void tickElectric(Level level) {
        boolean canProcess = canProcess(level);
        boolean active = false;
        boolean impact = false;
        this.energyStored = BatteryPackItem.dischargeIntoMachine(this.items.get(ELECTRIC_BATTERY_SLOT), this.energyStored, ELECTRIC_ENERGY_CAPACITY);

        if ((canProcess || this.retracting || this.delay > 0) && this.energyStored >= ELECTRIC_DEMAND_PER_TICK) {
            this.energyStored -= ELECTRIC_DEMAND_PER_TICK;
            active = true;

            if (this.delay <= 0) {
                int speedLevel = 1 + Math.min(3, upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED));
                double processSpeed = this.retracting ? 20.0D : 45.0D;
                processSpeed *= 1.0D + speedLevel / 4.0D;
                // The legacy Java assignment truncated the upgraded movement
                // step instead of rounding it.
                int step = Math.max(1, (int) processSpeed);

                if (this.retracting) {
                    this.progress -= step;
                    if (this.progress <= 0) {
                        this.progress = 0;
                        this.retracting = false;
                        this.delay = Math.max(0, 6 - speedLevel);
                    }
                } else if (canProcess) {
                    this.progress += step;
                    if (this.progress >= MAX_PROGRESS) {
                        this.progress = MAX_PROGRESS;
                        impact = craft(level);
                        this.retracting = true;
                        this.delay = Math.max(0, 6 - speedLevel);
                    }
                }
            } else {
                this.delay--;
            }
        }

        setLit(active || this.progress > 0);
        syncActiveVisuals(level);
        syncAnimation(level, impact);
    }

    private Optional<RecipeHolder<PressRecipe>> getRecipe(Level level) {
        return level.getRecipeManager().getRecipeFor(
                HbmRecipeTypes.PRESS.get(),
                new PressRecipe.Input(this.items.get(INPUT_SLOT), this.items.get(STAMP_SLOT)),
                level
        );
    }

    private boolean canProcess(Level level) {
        if (kind() == Kind.FIRE && this.burnTime < FIRE_BURN_PER_OPERATION) {
            return false;
        }
        if (this.items.get(STAMP_SLOT).isEmpty() || this.items.get(INPUT_SLOT).isEmpty()) {
            return false;
        }
        Optional<RecipeHolder<PressRecipe>> recipe = getRecipe(level);
        return recipe.isPresent() && canOutput(recipe.get().value().result());
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

    private boolean craft(Level level) {
        Optional<RecipeHolder<PressRecipe>> recipeHolder = getRecipe(level);
        if (recipeHolder.isEmpty()) {
            return false;
        }

        PressRecipe recipe = recipeHolder.get().value();
        ItemStack result = recipe.result().copy();
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }

        this.items.get(INPUT_SLOT).shrink(recipe.inputCount());
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        ItemStack stamp = this.items.get(STAMP_SLOT);
        StampItem.damageStamp(stamp);
        if (stamp.isEmpty()) {
            this.items.set(STAMP_SLOT, ItemStack.EMPTY);
        }

        this.completedCycles++;
        setChangedAndSync(true);
        return true;
    }

    private void startBurningFuel() {
        ItemStack fuel = this.items.get(FIRE_FUEL_SLOT);
        int duration = WoodBurnerBlockEntity.fuelDuration(fuel);
        if (duration <= 0) {
            return;
        }

        boolean lavaBucket = fuel.is(Items.LAVA_BUCKET);
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            this.items.set(FIRE_FUEL_SLOT, lavaBucket ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY);
        }
        this.burnTime += duration;
        setChangedAndSync(false);
    }

    private boolean isPreheated(Level level) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(this.worldPosition.relative(direction)).is(HbmBlocks.PRESS_PREHEATER.get())) {
                return true;
            }
        }
        return false;
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        ItemStack stack = this.items.get(ELECTRIC_UPGRADE_SLOT);
        if (MachineUpgradeItem.upgradeType(stack) != type) {
            return 0;
        }
        return Math.max(0, MachineUpgradeItem.upgradeTier(stack));
    }

    private long currentElectricDemand() {
        return kind() == Kind.ELECTRIC ? ELECTRIC_DEMAND_PER_TICK : 0L;
    }

    private static boolean isSupportedUpgrade(ItemStack stack) {
        return MachineUpgradeItem.isMachineUpgrade(stack)
                && MachineUpgradeItem.upgradeType(stack) == MachineUpgradeItem.UpgradeType.SPEED;
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getContainerSize();
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }

        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof PressMachineBlock && state.getValue(PressMachineBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(PressMachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void syncActiveVisuals(Level level) {
        boolean working = this.progress > 0 || this.retracting || this.speed > 0;
        if (working != this.wasWorking) {
            setChangedAndSync(true);
            this.wasWorking = working;
        } else {
            setChanged();
        }
    }

    private void syncAnimation(Level level, boolean impact) {
        if (level.isClientSide) {
            return;
        }
        boolean changed = this.progress != this.lastSyncedProgress
                || this.speed != this.lastSyncedSpeed
                || this.delay != this.lastSyncedDelay
                || this.retracting != this.lastSyncedRetracting;
        if (!changed && !impact) {
            return;
        }

        PressAnimationPayload.sendToTracking(
                level,
                this.worldPosition,
                this.progress,
                this.speed,
                this.delay,
                this.retracting,
                impact
        );
        this.lastSyncedProgress = this.progress;
        this.lastSyncedSpeed = this.speed;
        this.lastSyncedDelay = this.delay;
        this.lastSyncedRetracting = this.retracting;
    }

    private void setChangedAndSync(boolean forceUpdate) {
        setChanged();
        if (forceUpdate && this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
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

    private static double lerp(double previous, double current, float partialTick) {
        return previous + (current - previous) * partialTick;
    }

    public enum Kind {
        FIRE,
        ELECTRIC
    }
}
