package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.PurexMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.PurexRecipe;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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

public class PurexBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 4;
    public static final int INPUT_START = 4;
    public static final int INPUT_END = 7;
    public static final int OUTPUT_START = 7;
    public static final int OUTPUT_END = 13;
    public static final int SLOT_COUNT = 13;
    public static final int INPUT_TANK_COUNT = 3;
    public static final int TOTAL_TANK_COUNT = 4;
    public static final int TANK_CAPACITY = 24_000;
    public static final int DATA_COUNT = 21;
    public static final long BASE_ENERGY_CAPACITY = 1_000_000L;

    private static final int[] AUTOMATION_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank[] inputTanks = new HbmFluidTank[INPUT_TANK_COUNT];
    private final HbmFluidTank outputTank = new HbmFluidTank(TANK_CAPACITY);
    private long energyStored;
    private long lastInput;
    private long energyCapacity = BASE_ENERGY_CAPACITY;
    private double progress;
    private int currentDemand = 10_000;
    private boolean didProcess;
    private double clientPrevAnim;
    private double clientAnim;
    private boolean clientFrame;
    private long lastClientAnimationTick = Long.MIN_VALUE;
    @Nullable
    private ResourceLocation selectedRecipeId;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 9 && index < DATA_COUNT) {
                int tankDataIndex = index - 9;
                HbmFluidTank tank = tankByFlatIndex(tankDataIndex / 3);
                return switch (tankDataIndex % 3) {
                    case 0 -> tank.type().oldId();
                    case 1 -> tank.amount();
                    case 2 -> tank.pressure();
                    default -> 0;
                };
            }
            return switch (index) {
                case 0 -> (int) energyStored;
                case 1 -> (int) lastInput;
                case 2 -> (int) Math.round(progress * 1_000_000.0D);
                case 3 -> 1_000_000;
                case 4 -> currentDemand;
                case 5 -> (int) energyCapacity;
                case 6 -> didProcess ? 1 : 0;
                case 7 -> selectedRecipe().isPresent() ? 1 : 0;
                case 8 -> selectedRecipeMenuValue();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStored = value;
                case 1 -> lastInput = value;
                case 2 -> progress = value / 1_000_000.0D;
                case 4 -> currentDemand = Math.max(1, value);
                case 5 -> energyCapacity = Math.max(BASE_ENERGY_CAPACITY, value);
                case 6 -> didProcess = value != 0;
                case 8 -> setSelectedRecipeByMenuValue(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PurexBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PUREX.get(), pos, state);
        for (int i = 0; i < INPUT_TANK_COUNT; i++) {
            this.inputTanks[i] = new HbmFluidTank(TANK_CAPACITY);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PurexBlockEntity purex) {
        if (level.isClientSide) {
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, purex);
        purex.tickServer(level);
    }

    private void tickServer(Level level) {
        chargeFromBattery();
        Optional<RecipeHolder<PurexRecipe>> holder = selectedRecipe();
        if (holder.isEmpty()) {
            progress = 0.0D;
            didProcess = false;
            setChangedAndSync(level.getGameTime() % 20 == 0);
            return;
        }

        PurexRecipe recipe = holder.get().value();
        this.currentDemand = currentDemand(recipe);
        this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, Math.max(this.energyStored, (long) recipe.power() * 100L));
        setupTanks(recipe);

        if (autoSwitchRecipe(holder.get())) {
            this.progress = 0.0D;
            this.didProcess = false;
            setChangedAndSync(true);
            return;
        }

        if (!canProcess(recipe) || this.energyStored < this.currentDemand) {
            progress = 0.0D;
            didProcess = false;
            setChangedAndSync(level.getGameTime() % 20 == 0);
            return;
        }

        this.energyStored -= this.currentDemand;
        this.progress += Math.min(currentSpeed() / recipe.duration(), 1.0D);
        this.didProcess = true;
        if (this.progress >= 1.0D) {
            finishRecipe(recipe);
            if (canProcess(recipe) && this.energyStored >= this.currentDemand) {
                this.progress -= 1.0D;
            } else {
                this.progress = 0.0D;
            }
        }
        setChangedAndSync(true);
    }

    private Optional<RecipeHolder<PurexRecipe>> selectedRecipe() {
        if (this.level == null || this.selectedRecipeId == null) {
            return Optional.empty();
        }
        return findRecipe(this.level, this.selectedRecipeId);
    }

    private boolean canProcess(PurexRecipe recipe) {
        return recipe.matches(new PurexRecipe.Input(inputItemStacks(), inputFluidStacks()), this.level)
                && canFitOutputs(recipe);
    }

    private boolean canFitOutputs(PurexRecipe recipe) {
        if (recipe.outputItems().size() > OUTPUT_END - OUTPUT_START) {
            return false;
        }
        for (int index = 0; index < recipe.outputItems().size(); index++) {
            ItemStack result = recipe.outputItems().get(index).stack();
            ItemStack current = this.items.get(OUTPUT_START + index);
            if (current.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(current, result)
                    || current.getCount() + result.getCount() > current.getMaxStackSize()) {
                return false;
            }
        }
        if (recipe.outputFluids().isEmpty()) {
            return true;
        }
        PurexRecipe.PurexFluidStack output = recipe.outputFluids().getFirst();
        return this.outputTank.type() == output.type()
                && this.outputTank.pressure() == output.pressure()
                && this.outputTank.amount() + output.amount() <= this.outputTank.capacity();
    }

    private void finishRecipe(PurexRecipe recipe) {
        for (int index = 0; index < recipe.inputItems().size(); index++) {
            int slot = INPUT_START + index;
            ItemStack stack = this.items.get(slot);
            stack.shrink(recipe.inputItems().get(index).count());
            if (stack.isEmpty()) {
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
        for (int index = 0; index < recipe.inputFluids().size(); index++) {
            PurexRecipe.PurexFluidStack fluid = recipe.inputFluids().get(index);
            this.inputTanks[index].drain(fluid.type(), fluid.amount(), false);
        }
        for (int index = 0; index < recipe.outputItems().size(); index++) {
            ItemStack result = recipe.outputItems().get(index).roll(this.level.getRandom());
            if (result.isEmpty()) {
                continue;
            }
            ItemStack current = this.items.get(OUTPUT_START + index);
            if (current.isEmpty()) {
                this.items.set(OUTPUT_START + index, result);
            } else {
                current.grow(result.getCount());
            }
        }
        if (!recipe.outputFluids().isEmpty()) {
            PurexRecipe.PurexFluidStack output = recipe.outputFluids().getFirst();
            this.outputTank.fill(output.type(), output.amount(), output.pressure(), false);
        }
    }

    private void setupTanks(PurexRecipe recipe) {
        for (int index = 0; index < INPUT_TANK_COUNT; index++) {
            if (index < recipe.inputFluids().size()) {
                PurexRecipe.PurexFluidStack input = recipe.inputFluids().get(index);
                this.inputTanks[index].conform(input.type(), input.pressure());
            } else {
                this.inputTanks[index].clear();
            }
        }
        if (!recipe.outputFluids().isEmpty()) {
            PurexRecipe.PurexFluidStack output = recipe.outputFluids().getFirst();
            this.outputTank.conform(output.type(), output.pressure());
        } else {
            this.outputTank.clear();
        }
    }

    private void chargeFromBattery() {
        this.energyStored = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.energyStored, this.energyCapacity);
    }

    private double currentSpeed() {
        double speed = 1.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3) / 3.0D;
        speed += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3);
        return speed;
    }

    private int currentDemand(PurexRecipe recipe) {
        double multiplier = 1.0D;
        multiplier -= Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3) * 0.25D;
        multiplier += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        multiplier += Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) * 10.0D / 3.0D;
        return Math.max(1, (int) (recipe.power() * multiplier));
    }

    private boolean autoSwitchRecipe(RecipeHolder<PurexRecipe> current) {
        ItemStack firstInput = this.items.get(INPUT_START);
        String group = current.value().group();
        if (this.level == null || firstInput.isEmpty() || group.isBlank()) {
            return false;
        }
        for (RecipeHolder<PurexRecipe> candidate : availableRecipes(this.level)) {
            PurexRecipe recipe = candidate.value();
            if (candidate.id().equals(current.id()) || !group.equals(recipe.group()) || recipe.inputItems().isEmpty()) {
                continue;
            }
            if (recipe.inputItems().getFirst().ingredient().test(firstInput)) {
                this.selectedRecipeId = candidate.id();
                return true;
            }
        }
        return false;
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

    private List<HbmFluidStack> inputFluidStacks() {
        List<HbmFluidStack> stacks = new ArrayList<>(INPUT_TANK_COUNT);
        for (HbmFluidTank tank : this.inputTanks) {
            stacks.add(new HbmFluidStack(tank.type(), tank.amount(), tank.pressure()));
        }
        return stacks;
    }

    private List<ItemStack> inputItemStacks() {
        return List.of(this.items.get(INPUT_START), this.items.get(INPUT_START + 1), this.items.get(INPUT_START + 2));
    }

    public HbmFluidTank inputTank(int index) {
        return this.inputTanks[Math.max(0, Math.min(INPUT_TANK_COUNT - 1, index))];
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return Optional.ofNullable(this.selectedRecipeId);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId != null && this.level != null && findRecipe(this.level, recipeId).isEmpty()) {
            recipeId = null;
        }
        if (this.selectedRecipeId == recipeId || (this.selectedRecipeId != null && this.selectedRecipeId.equals(recipeId))) {
            return;
        }
        this.selectedRecipeId = recipeId;
        setChangedAndSync(true);
    }

    public List<RecipeHolder<PurexRecipe>> availableRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PUREX.get()).stream()
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    public void updateClientAnimation() {
        if (this.level == null || !this.level.isClientSide) {
            return;
        }
        long gameTime = this.level.getGameTime();
        if (this.lastClientAnimationTick == gameTime) {
            return;
        }
        if (this.lastClientAnimationTick == Long.MIN_VALUE || gameTime % 20L == 0L) {
            this.clientFrame = !this.level.getBlockState(this.worldPosition.above(5)).isAir();
        }
        int steps = this.lastClientAnimationTick == Long.MIN_VALUE
                ? 1
                : (int) Math.min(5L, Math.max(1L, gameTime - this.lastClientAnimationTick));
        for (int step = 0; step < steps; step++) {
            this.clientPrevAnim = this.clientAnim;
            if (this.didProcess) {
                this.clientAnim++;
            }
        }
        this.lastClientAnimationTick = gameTime;
    }

    public boolean clientFrame() {
        return this.clientFrame;
    }

    public double clientAnim(float partialTick) {
        return this.clientPrevAnim + (this.clientAnim - this.clientPrevAnim) * partialTick;
    }

    private Optional<RecipeHolder<PurexRecipe>> findRecipe(Level level, ResourceLocation recipeId) {
        return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PUREX.get()).stream()
                .filter(holder -> holder.id().equals(recipeId))
                .findFirst();
    }

    private int selectedRecipeMenuValue() {
        if (this.selectedRecipeId == null || this.level == null) {
            return 0;
        }
        List<RecipeHolder<PurexRecipe>> recipes = availableRecipes(this.level);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(this.selectedRecipeId)) {
                return index + 1;
            }
        }
        return 0;
    }

    private void setSelectedRecipeByMenuValue(int value) {
        if (value <= 0 || this.level == null) {
            this.selectedRecipeId = null;
            return;
        }
        List<RecipeHolder<PurexRecipe>> recipes = availableRecipes(this.level);
        int index = value - 1;
        this.selectedRecipeId = index >= 0 && index < recipes.size() ? recipes.get(index).id() : null;
    }

    private HbmFluidTank tankByFlatIndex(int index) {
        return index < INPUT_TANK_COUNT ? this.inputTanks[Math.max(0, index)] : this.outputTank;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new PurexFluidHandler();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        BlockPos pos = this.worldPosition;
        return List.of(
                pos.offset(3, 0, -2), pos.offset(3, 0, -1), pos.offset(3, 0, 0), pos.offset(3, 0, 1), pos.offset(3, 0, 2),
                pos.offset(-3, 0, -2), pos.offset(-3, 0, -1), pos.offset(-3, 0, 0), pos.offset(-3, 0, 1), pos.offset(-3, 0, 2),
                pos.offset(-2, 0, 3), pos.offset(-1, 0, 3), pos.offset(0, 0, 3), pos.offset(1, 0, 3), pos.offset(2, 0, 3),
                pos.offset(-2, 0, -3), pos.offset(-1, 0, -3), pos.offset(0, 0, -3), pos.offset(1, 0, -3), pos.offset(2, 0, -3)
        );
    }

    @Override
    public long getAvailableOutput() {
        return 0;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0, Math.min(this.currentDemand, this.energyCapacity - this.energyStored));
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.energyStored = Math.min(this.energyCapacity, this.energyStored + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.power.purex", this.lastInput, this.currentDemand, this.energyStored, this.energyCapacity);
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
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChangedAndSync(false);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
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
        if (slot == BATTERY_SLOT) {
            return stack.getItem() instanceof BatteryPackItem;
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return MachineUpgradeItem.isMachineUpgrade(stack);
        }
        if (slot == BLUEPRINT_SLOT) {
            return BlueprintItem.isBlueprint(stack);
        }
        if (slot >= INPUT_START && slot < INPUT_END && this.level != null) {
            Optional<RecipeHolder<PurexRecipe>> selected = selectedRecipe();
            if (selected.isEmpty()) {
                return false;
            }
            int inputIndex = slot - INPUT_START;
            List<PurexRecipe.CountedIngredient> inputs = selected.get().value().inputItems();
            if (inputIndex < inputs.size() && inputs.get(inputIndex).ingredient().test(stack)) {
                return true;
            }
            if (inputIndex == 0 && !selected.get().value().group().isBlank()) {
                String group = selected.get().value().group();
                return availableRecipes(this.level).stream()
                        .map(RecipeHolder::value)
                        .filter(recipe -> group.equals(recipe.group()) && !recipe.inputItems().isEmpty())
                        .anyMatch(recipe -> recipe.inputItems().getFirst().ingredient().test(stack));
            }
            return false;
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
        return slot >= OUTPUT_START && slot < OUTPUT_END
                || slot >= INPUT_START && slot < INPUT_END && !canPlaceItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.purex");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PurexMenu(containerId, inventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        for (int index = 0; index < INPUT_TANK_COUNT; index++) {
            tag.put("InputTank" + index, this.inputTanks[index].save());
        }
        tag.put("OutputTank", this.outputTank.save());
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastInput", this.lastInput);
        tag.putLong("EnergyCapacity", this.energyCapacity);
        tag.putDouble("Progress", this.progress);
        tag.putInt("CurrentDemand", this.currentDemand);
        tag.putBoolean("DidProcess", this.didProcess);
        if (this.selectedRecipeId != null) {
            tag.putString("SelectedRecipe", this.selectedRecipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        for (int index = 0; index < INPUT_TANK_COUNT; index++) {
            this.inputTanks[index].load(tag.getCompound("InputTank" + index));
        }
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.energyStored = tag.getLong("EnergyStored");
        this.lastInput = tag.getLong("LastInput");
        this.energyCapacity = Math.max(BASE_ENERGY_CAPACITY, tag.getLong("EnergyCapacity"));
        this.progress = Math.max(0.0D, tag.getDouble("Progress"));
        this.currentDemand = Math.max(1, tag.getInt("CurrentDemand"));
        this.didProcess = tag.getBoolean("DidProcess");
        this.selectedRecipeId = tag.contains("SelectedRecipe")
                ? ResourceLocation.tryParse(tag.getString("SelectedRecipe"))
                : null;
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

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (sync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private final class PurexFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return TOTAL_TANK_COUNT;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank >= 0 && tank < INPUT_TANK_COUNT) {
                return inputTanks[tank].getFluidInTank(0);
            }
            return tank == INPUT_TANK_COUNT ? outputTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < TOTAL_TANK_COUNT ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank >= 0 && tank < INPUT_TANK_COUNT && !stack.isEmpty()
                    && HbmFluids.fromNeoFluid(stack.getFluid()).filter(PurexBlockEntity.this::isRecipeFluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!isRecipeFluid(fluid)) {
                return 0;
            }
            int remaining = resource.getAmount();
            int filled = 0;
            for (HbmFluidTank tank : inputTanks) {
                if (remaining <= 0) {
                    break;
                }
                if (!tank.type().isNone() && tank.type() != fluid) {
                    continue;
                }
                int accepted = tank.fill(fluid, remaining, action.simulate());
                filled += accepted;
                remaining -= accepted;
            }
            if (filled > 0 && action.execute()) {
                setChangedAndSync(true);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || outputTank.type().isNone() || outputTank.pressure() != 0) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone() || outputTank.type() != fluid) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = outputTank.drain(fluid, resource.getAmount(), action.simulate());
            if (drained.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                setChangedAndSync(true);
            }
            return HbmFluids.toNeoStack(fluid, drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || outputTank.type().isNone() || outputTank.pressure() != 0) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = outputTank.type();
            HbmFluidStack drained = outputTank.drain(fluid, maxDrain, action.simulate());
            if (drained.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                setChangedAndSync(true);
            }
            return HbmFluids.toNeoStack(fluid, drained.amount());
        }
    }

    private boolean isRecipeFluid(HbmFluidDefinition fluid) {
        return this.level != null && !fluid.isNone() && this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PUREX.get()).stream()
                .anyMatch(holder -> holder.value().inputFluids().stream().anyMatch(stack -> stack.type() == fluid));
    }
}
