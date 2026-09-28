package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.RockMillBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.menu.RockMillMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.MachineRecipeCache;
import com.reinhardt.hbm.recipe.RockMillRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Complete high-version port of TileEntityMachineRockMill. */
public final class RockMillBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int INPUT_START = 2;
    public static final int INPUT_END = 5;
    public static final int OUTPUT_START = 5;
    public static final int OUTPUT_END = 8;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 15;
    public static final long BASE_MAX_POWER = 2_500L;
    public static final int BASE_TANK_CAPACITY = 4_000;
    public static final float ACCELERATION = 0.1F;
    public static final float MAX_ROTATION_SPEED = 15.0F;

    private static final int[] AUTOMATION_SLOTS = {2, 3, 4, 5, 6, 7};
    private static final List<AuthoredPort> AUTHORED_PORTS = List.of(
            new AuthoredPort(new BlockPos(2, 0, 1), Direction.EAST),
            new AuthoredPort(new BlockPos(2, 0, -1), Direction.EAST),
            new AuthoredPort(new BlockPos(-2, 0, 1), Direction.WEST),
            new AuthoredPort(new BlockPos(-2, 0, -1), Direction.WEST),
            new AuthoredPort(new BlockPos(1, 0, 2), Direction.SOUTH),
            new AuthoredPort(new BlockPos(-1, 0, 2), Direction.SOUTH),
            new AuthoredPort(new BlockPos(1, 0, -2), Direction.NORTH),
            new AuthoredPort(new BlockPos(-1, 0, -2), Direction.NORTH)
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final NonNullList<ItemStack> observedRecipeItems = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(BASE_TANK_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(BASE_TANK_CAPACITY);

    private long power;
    private long maxPower = BASE_MAX_POWER;
    private long lastInput;
    private int progress;
    private boolean processing;
    private int completedCycles;

    private float rotation;
    private float previousRotation;
    private float rotationSpeed;
    private boolean frame;
    private boolean frameInitialized;

    private long observedRecipeCacheGeneration = Long.MIN_VALUE;
    private boolean recipeCatalogDirty = true;
    private boolean recipeMatchDirty = true;
    private boolean recipeStateInitialized;
    private boolean autoSwitchedThisTick;
    private List<RecipeHolder<RockMillRecipe>> cachedRecipeCatalog = List.of();
    @Nullable
    private ResourceLocation selectedRecipeId;
    @Nullable
    private RecipeHolder<RockMillRecipe> cachedRecipe;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            RockMillRecipe recipe = RockMillBlockEntity.this.cachedRecipe == null
                    ? null : RockMillBlockEntity.this.cachedRecipe.value();
            return switch (index) {
                case 0 -> (int) RockMillBlockEntity.this.power;
                case 1 -> (int) RockMillBlockEntity.this.maxPower;
                case 2 -> (int) RockMillBlockEntity.this.lastInput;
                case 3 -> RockMillBlockEntity.this.progress;
                case 4 -> recipe == null ? 1 : recipe.duration();
                case 5 -> recipe == null ? 0 : (int) recipe.power();
                case 6 -> RockMillBlockEntity.this.processing ? 1 : 0;
                case 7 -> RockMillBlockEntity.this.inputTank.type().oldId();
                case 8 -> RockMillBlockEntity.this.inputTank.amount();
                case 9 -> RockMillBlockEntity.this.inputTank.capacity();
                case 10 -> RockMillBlockEntity.this.outputTank.type().oldId();
                case 11 -> RockMillBlockEntity.this.outputTank.amount();
                case 12 -> RockMillBlockEntity.this.outputTank.capacity();
                case 13 -> selectedRecipeIndex() + 1;
                case 14 -> RockMillBlockEntity.this.completedCycles;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> RockMillBlockEntity.this.power = value;
                case 1 -> RockMillBlockEntity.this.maxPower = Math.max(BASE_MAX_POWER, value);
                case 2 -> RockMillBlockEntity.this.lastInput = value;
                case 3 -> RockMillBlockEntity.this.progress = Math.max(0, value);
                case 6 -> RockMillBlockEntity.this.processing = value != 0;
                case 7 -> RockMillBlockEntity.this.inputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 8 -> RockMillBlockEntity.this.inputTank.setAmount(value);
                case 9 -> RockMillBlockEntity.this.inputTank.setCapacity(value);
                case 10 -> RockMillBlockEntity.this.outputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 11 -> RockMillBlockEntity.this.outputTank.setAmount(value);
                case 12 -> RockMillBlockEntity.this.outputTank.setCapacity(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public RockMillBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ROCK_MILL.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RockMillBlockEntity mill) {
        if (level.isClientSide) {
            mill.tickClient(level);
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, mill);
        mill.tickServer(level);
    }

    public float rotation(float partialTick) {
        return this.previousRotation + (this.rotation - this.previousRotation) * partialTick;
    }

    public boolean hasFrame() {
        return this.frame;
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return capabilityPorts().stream().map(CapabilityPort::connector).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return capabilityPorts().stream().anyMatch(port ->
                port.connector().equals(connectorPos) && port.face() == machineSide);
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
        long consumption = this.cachedRecipe == null ? 25L : Math.max(1L, this.cachedRecipe.value().power());
        return Math.min(consumption, this.maxPower - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        long previousPower = this.power;
        long previousLastInput = this.lastInput;
        this.power = Math.min(this.maxPower, this.power + Math.max(0L, receivedInput));
        this.lastInput = Math.max(0L, receivedInput);
        if (this.power != previousPower || this.lastInput != previousLastInput) {
            markRuntimeChanged();
        }
    }

    @Override
    public Component getPowerStatus() {
        RockMillRecipe recipe = this.cachedRecipe == null ? null : this.cachedRecipe.value();
        return Component.translatable(
                "message.reinhardtshbm.power.rock_mill",
                this.lastInput,
                recipe == null ? 0L : recipe.power(),
                this.power,
                this.maxPower,
                recipe == null ? 0 : this.progress * 100 / Math.max(1, recipe.duration()),
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
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            inventoryChanged(slot);
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
        if (!removed.isEmpty()) {
            inventoryChanged(slot);
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        inventoryChanged(slot);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot == BLUEPRINT_SLOT) {
            return BlueprintItem.isBlueprint(stack);
        }
        if (slot == INPUT_START) {
            return canAcceptRecipeInput(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_START && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_END || isCloggedInput(slot);
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
        this.progress = 0;
        invalidateRecipeMatch();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.rock_mill");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RockMillMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(
                        level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    public List<RecipeHolder<RockMillRecipe>> recipes(Level level) {
        ensureRecipeCatalog(level);
        return this.cachedRecipeCatalog;
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return Optional.ofNullable(this.selectedRecipeId);
    }

    public Optional<RecipeHolder<RockMillRecipe>> selectedRecipe(Level level) {
        refreshRecipeState(level);
        return Optional.ofNullable(this.cachedRecipe);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        if (Objects.equals(this.selectedRecipeId, recipeId)) {
            return;
        }
        this.selectedRecipeId = recipeId;
        this.progress = 0;
        this.recipeStateInitialized = true;
        invalidateRecipeMatch();
        setChanged();
        syncClient();
    }

    public boolean canAcceptRecipeInput(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return false;
        }
        refreshRecipeState(this.level);
        if (this.cachedRecipe == null) {
            return false;
        }
        RockMillRecipe selected = this.cachedRecipe.value();
        if (selected.canAutoSwitchFrom(stack)) {
            return true;
        }
        Optional<String> group = selected.autoSwitchGroup();
        if (group.isEmpty()) {
            return false;
        }
        for (RecipeHolder<RockMillRecipe> holder : this.cachedRecipeCatalog) {
            RockMillRecipe recipe = holder.value();
            if (recipe.autoSwitchGroup().filter(group.get()::equals).isPresent()
                    && recipe.canAutoSwitchFrom(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAutomationPort(BlockPos queriedPos) {
        return queriedPos.equals(this.worldPosition) || portPositions().contains(queriedPos);
    }

    public boolean allowsAutomationPort(BlockPos queriedPos, @Nullable Direction side) {
        if (side == null) {
            return isAutomationPort(queriedPos);
        }
        return capabilityPorts().stream().anyMatch(port ->
                port.host().equals(queriedPos) && port.face() == side);
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        return allowsAutomationPort(queriedPos, side) ? new RockMillFluidHandler() : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.putLong("MaxPower", this.maxPower);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("CompletedCycles", this.completedCycles);
        tag.putBoolean("Processing", this.processing);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        if (this.selectedRecipeId != null) {
            tag.putString("SelectedRecipe", this.selectedRecipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
            this.observedRecipeItems.set(slot, this.items.get(slot).copy());
        }
        this.power = Math.max(0L, tag.getLong("Power"));
        this.maxPower = Math.max(BASE_MAX_POWER, tag.getLong("MaxPower"));
        this.lastInput = Math.max(0L, tag.getLong("LastInput"));
        this.progress = Math.max(0, tag.getInt("Progress"));
        this.completedCycles = Math.max(0, tag.getInt("CompletedCycles"));
        this.processing = tag.getBoolean("Processing");
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.selectedRecipeId = tag.contains("SelectedRecipe")
                ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
        this.recipeStateInitialized = false;
        invalidateRecipeCatalog();
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
        refreshRecipeState(level);
        RockMillRecipe recipe = this.cachedRecipe == null ? null : this.cachedRecipe.value();
        this.maxPower = Math.max(this.power, Math.max(BASE_MAX_POWER, recipe == null ? 0L : recipe.power() * 100L));
        boolean batteryChanged = dischargeBattery();

        boolean worked = false;
        if (recipe != null && !this.autoSwitchedThisTick
                && hasInput(recipe)
                && canFitItemOutput(recipe)
                && canFitFluidOutput(recipe)
                && this.power >= recipe.power()) {
            this.power -= recipe.power();
            this.progress++;
            worked = true;

            if (this.progress >= recipe.duration()) {
                this.progress = 0;
                consumeInput(recipe);
                produceOutput(level, recipe);
                this.completedCycles++;
                captureRecipeSlotSnapshot();
                invalidateRecipeMatch();
            }
        }
        this.autoSwitchedThisTick = false;

        if (worked && Math.floorMod(level.getGameTime() + this.worldPosition.asLong(), 3L) == 0L) {
            playGrindingSound(level);
        }
        if (this.processing != worked) {
            this.processing = worked;
            syncClient();
        }
        if (worked || batteryChanged) {
            markRuntimeChanged();
        }
    }

    private void tickClient(Level level) {
        this.previousRotation = this.rotation;
        this.rotationSpeed += ACCELERATION * (this.processing ? 1.0F : -1.0F);
        this.rotationSpeed = Math.max(0.0F, Math.min(MAX_ROTATION_SPEED, this.rotationSpeed));
        this.rotation += this.rotationSpeed;
        if (this.rotation >= 360.0F) {
            this.previousRotation -= 360.0F;
            this.rotation -= 360.0F;
        }
        if (!this.frameInitialized || level.getGameTime() % 20L == 0L) {
            this.frame = !level.getBlockState(this.worldPosition.above(3)).isAir();
            this.frameInitialized = true;
        }
        if (!this.processing) {
            return;
        }
        Player player = level.getNearestPlayer(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 1.5D,
                this.worldPosition.getZ() + 0.5D,
                35.0D,
                false
        );
        if (player == null) {
            return;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double radialX = Math.cos(angle);
        double radialZ = Math.sin(angle);
        level.addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, particleBlockState()),
                this.worldPosition.getX() + 0.5D + radialX * 2.25D,
                this.worldPosition.getY() + 1.5D,
                this.worldPosition.getZ() + 0.5D + radialZ * 2.25D,
                radialX * 0.125D,
                -0.1D,
                radialZ * 0.125D
        );
    }

    private boolean dischargeBattery() {
        long previous = this.power;
        this.power = BatteryPackItem.dischargeIntoMachine(
                this.items.get(BATTERY_SLOT), this.power, this.maxPower);
        return this.power != previous;
    }

    private boolean hasInput(RockMillRecipe recipe) {
        ItemStack input = this.items.get(INPUT_START);
        if (input.isEmpty() || input.getCount() < recipe.input().count()
                || !recipe.input().ingredient().test(input)) {
            return false;
        }
        if (recipe.inputFluid().isEmpty()) {
            return true;
        }
        RockMillRecipe.FluidStack fluid = recipe.inputFluid().get();
        return this.inputTank.type() == fluid.type() && this.inputTank.amount() >= fluid.amount();
    }

    private boolean canFitItemOutput(RockMillRecipe recipe) {
        ItemStack output = this.items.get(OUTPUT_START);
        if (recipe.outputs().size() > 1) {
            return output.isEmpty();
        }
        ItemStack result = recipe.outputs().getFirst().stack();
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private boolean canFitFluidOutput(RockMillRecipe recipe) {
        if (recipe.outputFluid().isEmpty()) {
            return true;
        }
        RockMillRecipe.FluidStack result = recipe.outputFluid().get();
        return (this.outputTank.type().isNone() || this.outputTank.type() == result.type())
                && this.outputTank.amount() + result.amount() <= this.outputTank.capacity();
    }

    private void consumeInput(RockMillRecipe recipe) {
        ItemStack input = this.items.get(INPUT_START);
        input.shrink(recipe.input().count());
        if (input.isEmpty()) {
            this.items.set(INPUT_START, ItemStack.EMPTY);
        }
        recipe.inputFluid().ifPresent(fluid -> this.inputTank.drain(fluid.type(), fluid.amount(), false));
    }

    private void produceOutput(Level level, RockMillRecipe recipe) {
        ItemStack result = recipe.selectOutput(level.random);
        ItemStack output = this.items.get(OUTPUT_START);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_START, result);
        } else {
            output.grow(result.getCount());
        }
        recipe.outputFluid().ifPresent(fluid -> this.outputTank.fill(fluid.type(), fluid.amount(), false));
    }

    private void playGrindingSound(Level level) {
        BlockState state = particleBlockState();
        SoundType sound = state.getSoundType(level, this.worldPosition, null);
        level.playSound(
                null,
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 1.5D,
                this.worldPosition.getZ() + 0.5D,
                sound.getStepSound(),
                SoundSource.BLOCKS,
                1.0F,
                0.75F
        );
    }

    private BlockState particleBlockState() {
        ItemStack input = this.items.get(INPUT_START);
        if (input.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState();
        }
        return Blocks.GRAVEL.defaultBlockState();
    }

    private void refreshRecipeState(Level level) {
        ensureRecipeCatalog(level);
        if (!this.recipeMatchDirty) {
            return;
        }
        this.recipeMatchDirty = false;
        ResourceLocation previousId = this.cachedRecipe == null ? null : this.cachedRecipe.id();
        this.cachedRecipe = findRecipe(this.selectedRecipeId);

        if (this.cachedRecipe != null) {
            Optional<String> autoSwitchGroup = this.cachedRecipe.value().autoSwitchGroup();
            ItemStack firstInput = this.items.get(INPUT_START);
            if (autoSwitchGroup.isPresent() && !firstInput.isEmpty()) {
                for (RecipeHolder<RockMillRecipe> holder : this.cachedRecipeCatalog) {
                    if (holder.id().equals(this.cachedRecipe.id())) {
                        continue;
                    }
                    RockMillRecipe candidate = holder.value();
                    if (candidate.autoSwitchGroup().filter(autoSwitchGroup.get()::equals).isPresent()
                            && candidate.canAutoSwitchFrom(firstInput)) {
                        this.cachedRecipe = holder;
                        this.selectedRecipeId = holder.id();
                        this.autoSwitchedThisTick = true;
                        break;
                    }
                }
            }
        }

        ResourceLocation nextId = this.cachedRecipe == null ? null : this.cachedRecipe.id();
        if (!this.recipeStateInitialized || !Objects.equals(previousId, nextId)) {
            if (this.recipeStateInitialized || nextId == null) {
                this.progress = 0;
            }
            this.recipeStateInitialized = true;
            conformTanks(this.cachedRecipe == null ? null : this.cachedRecipe.value());
            syncClient();
        }
    }

    @Nullable
    private RecipeHolder<RockMillRecipe> findRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId == null) {
            return null;
        }
        for (RecipeHolder<RockMillRecipe> holder : this.cachedRecipeCatalog) {
            if (holder.id().equals(recipeId)) {
                return holder;
            }
        }
        return null;
    }

    private void conformTanks(@Nullable RockMillRecipe recipe) {
        if (recipe == null) {
            this.inputTank.clear();
            this.outputTank.clear();
            return;
        }
        if (recipe.inputFluid().isPresent()) {
            RockMillRecipe.FluidStack input = recipe.inputFluid().get();
            this.inputTank.conform(input.type(), 0);
            this.inputTank.setCapacity(Math.max(BASE_TANK_CAPACITY,
                    Math.max(this.inputTank.amount(), input.amount() * 2)));
        } else {
            this.inputTank.clear();
        }
        if (recipe.outputFluid().isPresent()) {
            RockMillRecipe.FluidStack output = recipe.outputFluid().get();
            this.outputTank.conform(output.type(), 0);
            this.outputTank.setCapacity(Math.max(BASE_TANK_CAPACITY,
                    Math.max(this.outputTank.amount(), output.amount() * 2)));
        } else {
            this.outputTank.clear();
        }
    }

    private boolean isCloggedInput(int slot) {
        if (slot < INPUT_START || slot >= INPUT_END) {
            return false;
        }
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return false;
        }
        return slot != INPUT_START || !canAcceptRecipeInput(stack);
    }

    private int selectedRecipeIndex() {
        if (this.selectedRecipeId == null) {
            return -1;
        }
        for (int index = 0; index < this.cachedRecipeCatalog.size(); index++) {
            if (this.cachedRecipeCatalog.get(index).id().equals(this.selectedRecipeId)) {
                return index;
            }
        }
        return -1;
    }

    private List<BlockPos> portPositions() {
        return capabilityPorts().stream().map(CapabilityPort::host).toList();
    }

    private List<CapabilityPort> capabilityPorts() {
        return AUTHORED_PORTS.stream().map(port -> {
            BlockPos host = this.worldPosition.offset(port.offset()).immutable();
            return new CapabilityPort(host, host.relative(port.face()).immutable(), port.face());
        }).toList();
    }

    private record AuthoredPort(BlockPos offset, Direction face) {
    }

    private record CapabilityPort(BlockPos host, BlockPos connector, Direction face) {
    }

    private void ensureRecipeCatalog(Level level) {
        long generation = MachineRecipeCache.generation();
        if (this.observedRecipeCacheGeneration != generation) {
            this.observedRecipeCacheGeneration = generation;
            invalidateRecipeCatalog();
        }
        if (!this.recipeCatalogDirty) {
            return;
        }
        this.cachedRecipeCatalog = level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ROCK_MILL.get())
                .stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
        this.recipeCatalogDirty = false;
        this.recipeMatchDirty = true;
    }

    private void invalidateRecipeCatalog() {
        this.recipeCatalogDirty = true;
        this.recipeStateInitialized = false;
        this.cachedRecipeCatalog = List.of();
        invalidateRecipeMatch();
    }

    private void invalidateRecipeMatch() {
        this.recipeMatchDirty = true;
    }

    private void inventoryChanged(int slot) {
        if (slot >= BLUEPRINT_SLOT && slot < INPUT_END) {
            invalidateRecipeMatch();
        }
        if (slot == INPUT_START) {
            syncClient();
        }
    }

    private void captureRecipeSlotSnapshot() {
        for (int slot = BLUEPRINT_SLOT; slot < INPUT_END; slot++) {
            this.observedRecipeItems.set(slot, this.items.get(slot).copy());
        }
    }

    @Override
    public void setChanged() {
        for (int slot = BLUEPRINT_SLOT; slot < INPUT_END; slot++) {
            ItemStack current = this.items.get(slot);
            ItemStack observed = this.observedRecipeItems.get(slot);
            if (current.getCount() == observed.getCount()
                    && ItemStack.isSameItemSameComponents(current, observed)) {
                continue;
            }
            this.observedRecipeItems.set(slot, current.copy());
            inventoryChanged(slot);
        }
        super.setChanged();
    }

    private void markRuntimeChanged() {
        super.setChanged();
    }

    private void syncClient() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(
                    this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean acceptsSelectedFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || this.level == null) {
            return false;
        }
        refreshRecipeState(this.level);
        return this.cachedRecipe != null
                && this.cachedRecipe.value().inputFluid().filter(stack -> stack.type() == fluid).isPresent();
    }

    private void fluidChanged() {
        invalidateRecipeMatch();
        setChanged();
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private final class RockMillFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> RockMillBlockEntity.this.inputTank.getFluidInTank(0);
                case 1 -> RockMillBlockEntity.this.outputTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> RockMillBlockEntity.this.inputTank.capacity();
                case 1 -> RockMillBlockEntity.this.outputTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && !stack.isEmpty()
                    && HbmFluids.fromNeoFluid(stack.getFluid())
                    .filter(RockMillBlockEntity.this::acceptsSelectedFluid)
                    .isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            Optional<HbmFluidDefinition> fluid = HbmFluids.fromNeoFluid(resource.getFluid());
            if (fluid.isEmpty() || !acceptsSelectedFluid(fluid.get())) {
                return 0;
            }
            int accepted = RockMillBlockEntity.this.inputTank.fill(
                    fluid.get(), resource.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) {
                fluidChanged();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            Optional<HbmFluidDefinition> fluid = HbmFluids.fromNeoFluid(resource.getFluid());
            if (fluid.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = RockMillBlockEntity.this.outputTank.drain(
                    fluid.get(), resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                fluidChanged();
            }
            return drained.isEmpty() ? FluidStack.EMPTY
                    : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            HbmFluidStack drained = RockMillBlockEntity.this.outputTank.drain(
                    null, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                fluidChanged();
            }
            return drained.isEmpty() ? FluidStack.EMPTY
                    : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }
}
