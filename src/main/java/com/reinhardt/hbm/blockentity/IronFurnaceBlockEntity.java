package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LegacyFurnaceBlock;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.IronFurnaceMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class IronFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_A_SLOT = 1;
    public static final int FUEL_B_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int UPGRADE_SLOT = 4;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 6;
    public static final int BASE_TIME = 200;
    private static final int[] AUTOMATION_SLOTS = {INPUT_SLOT, FUEL_A_SLOT, FUEL_B_SLOT, OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int maxBurnTime;
    private int burnTime;
    private int progress;
    private int processingTime = BASE_TIME;
    private boolean wasOn;
    private boolean canSmelt;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> IronFurnaceBlockEntity.this.maxBurnTime;
                case 1 -> IronFurnaceBlockEntity.this.burnTime;
                case 2 -> IronFurnaceBlockEntity.this.progress;
                case 3 -> IronFurnaceBlockEntity.this.processingTime;
                case 4 -> IronFurnaceBlockEntity.this.wasOn ? 1 : 0;
                case 5 -> IronFurnaceBlockEntity.this.canSmelt ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> IronFurnaceBlockEntity.this.maxBurnTime = value;
                case 1 -> IronFurnaceBlockEntity.this.burnTime = value;
                case 2 -> IronFurnaceBlockEntity.this.progress = value;
                case 3 -> IronFurnaceBlockEntity.this.processingTime = Math.max(1, value);
                case 4 -> IronFurnaceBlockEntity.this.wasOn = value != 0;
                case 5 -> IronFurnaceBlockEntity.this.canSmelt = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public IronFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.IRON_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IronFurnaceBlockEntity furnace) {
        if (!level.isClientSide) {
            furnace.tickServer(level);
        }
    }

    public static int fuelDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        int base = stack.getBurnTime(null);
        if (base <= 0) {
            base = fallbackFuelDuration(stack);
        }
        if (base <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(base * fuelMultiplier(stack)));
    }

    public static boolean isSpeedUpgrade(ItemStack stack) {
        return MachineUpgradeItem.isMachineUpgrade(stack)
                && MachineUpgradeItem.upgradeType(stack) == MachineUpgradeItem.UpgradeType.SPEED;
    }

    public static boolean hasSmeltingRecipe(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent();
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
            if (slot == INPUT_SLOT) {
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
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }
        if (slot == INPUT_SLOT) {
            this.progress = 0;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> this.level == null || hasSmeltingRecipe(this.level, stack);
            case FUEL_A_SLOT, FUEL_B_SLOT -> fuelDuration(stack) > 0;
            case UPGRADE_SLOT -> isSpeedUpgrade(stack);
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
        return Component.translatable("container.reinhardtshbm.furnace_iron");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IronFurnaceMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData menuData() {
        return this.menuData;
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
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("Progress", this.progress);
        tag.putInt("ProcessingTime", this.processingTime);
        tag.putBoolean("WasOn", this.wasOn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.burnTime = tag.getInt("BurnTime");
        this.progress = tag.getInt("Progress");
        this.processingTime = Math.max(1, tag.getInt("ProcessingTime"));
        this.wasOn = tag.getBoolean("WasOn");
    }

    private void tickServer(Level level) {
        this.processingTime = currentProcessingTime();
        Optional<RecipeHolder<SmeltingRecipe>> recipe = currentRecipe(level);
        this.canSmelt = recipe.isPresent();

        boolean changed = false;
        if (this.burnTime <= 0 && this.canSmelt) {
            changed |= tryConsumeFuel();
        }

        this.wasOn = this.canSmelt && this.burnTime > 0;
        if (this.wasOn) {
            this.progress++;
            this.burnTime = Math.max(0, this.burnTime - 1);
            changed = true;
            if (level.getGameTime() % 20L == 0L) {
                HbmPollution.increment(level, this.worldPosition, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND);
            }

            if (this.progress % 15 == 0) {
                level.playSound(null, this.worldPosition, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 0.5F);
            }

            if (this.progress >= this.processingTime && recipe.isPresent()) {
                finishSmelting(level, recipe.get());
                this.progress = 0;
            }
        } else if (this.progress != 0) {
            this.progress = 0;
            changed = true;
        }

        setLit(level, this.wasOn);
        if (changed) {
            setChanged();
        }
    }

    private int currentProcessingTime() {
        ItemStack upgrade = this.items.get(UPGRADE_SLOT);
        int speed = isSpeedUpgrade(upgrade) ? Math.max(0, MachineUpgradeItem.upgradeTier(upgrade)) : 0;
        return Math.max(1, BASE_TIME - 15 * speed);
    }

    private Optional<RecipeHolder<SmeltingRecipe>> currentRecipe(Level level) {
        ItemStack input = this.items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SmeltingRecipe>> holder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        ItemStack result = holder.get().value().assemble(recipeInput, level.registryAccess());
        return canPlaceOutput(result) ? holder : Optional.empty();
    }

    private boolean tryConsumeFuel() {
        for (int slot = FUEL_A_SLOT; slot <= FUEL_B_SLOT; slot++) {
            ItemStack fuel = this.items.get(slot);
            int duration = fuelDuration(fuel);
            if (duration <= 0) {
                continue;
            }

            ItemStack remainder = craftingRemaining(fuel, 1);
            if (!remainder.isEmpty() && fuel.getCount() > 1 && !canPlaceOutput(remainder)) {
                continue;
            }

            ItemStack consumed = fuel.copy();
            fuel.shrink(1);
            if (fuel.isEmpty()) {
                this.items.set(slot, craftingRemaining(consumed, 1));
            } else if (!remainder.isEmpty()) {
                addOutput(remainder);
            }

            this.maxBurnTime = duration;
            this.burnTime = duration;
            return true;
        }
        return false;
    }

    private void finishSmelting(Level level, RecipeHolder<SmeltingRecipe> recipe) {
        SingleRecipeInput input = new SingleRecipeInput(this.items.get(INPUT_SLOT));
        ItemStack result = recipe.value().assemble(input, level.registryAccess());
        addOutput(result);
        this.items.get(INPUT_SLOT).shrink(1);
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean canPlaceOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return stack.getCount() <= stack.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(output, stack)
                && output.getCount() + stack.getCount() <= Math.min(output.getMaxStackSize(), getMaxStackSize(output));
    }

    private void addOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, stack.copy());
        } else if (ItemStack.isSameItemSameComponents(output, stack)) {
            output.grow(stack.getCount());
        }
    }

    private void setLit(Level level, boolean lit) {
        BlockState state = level.getBlockState(this.worldPosition);
        if (state.hasProperty(LegacyFurnaceBlock.LIT) && state.getValue(LegacyFurnaceBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(LegacyFurnaceBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < this.items.size();
    }

    private static int fallbackFuelDuration(ItemStack stack) {
        String path = registryPath(stack);
        if (path.startsWith("coke_")) {
            return 3_200;
        }
        return switch (path) {
            case "solid_fuel" -> 3_200;
            case "solid_fuel_presto" -> 6_400;
            case "solid_fuel_presto_triplet" -> 19_200;
            case "solid_fuel_bf" -> 32_000;
            case "solid_fuel_presto_bf" -> 80_000;
            case "solid_fuel_presto_triplet_bf" -> 400_000;
            case "rocket_fuel" -> 6_400;
            case "lignite", "powder_lignite" -> 1_200;
            default -> 0;
        };
    }

    private static double fuelMultiplier(ItemStack stack) {
        String path = registryPath(stack);
        if (stack.is(Items.COAL) || path.equals("lignite") || path.equals("powder_lignite")) {
            return 1.25D;
        }
        if (path.startsWith("coke_")) {
            return 1.5D;
        }
        if (path.startsWith("solid_fuel") || path.equals("rocket_fuel") || path.contains("balefire") || path.endsWith("_bf")) {
            return 2.0D;
        }
        return 1.0D;
    }

    private static String registryPath(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static ItemStack craftingRemaining(ItemStack stack, int count) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (stack.is(HbmItems.SOLID_FUEL_BF.get()) || stack.is(HbmItems.SOLID_FUEL.get()) || stack.is(HbmItems.ROCKET_FUEL.get())) {
            return ItemStack.EMPTY;
        }

        ItemStack single = stack.copy();
        single.setCount(1);
        Item item = single.getItem();
        if (!item.hasCraftingRemainingItem(single)) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = item.getCraftingRemainingItem(single);
        if (!remainder.isEmpty()) {
            remainder.setCount(remainder.getCount() * count);
        }
        return remainder;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }
}
