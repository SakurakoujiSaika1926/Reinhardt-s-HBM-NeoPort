package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.BrickFurnaceBlock;
import com.reinhardt.hbm.menu.BrickFurnaceMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class BrickFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int ASH_SLOT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_COUNT = 5;
    private static final int PROCESS_TIME = 200;
    private static final int ASH_THRESHOLD = 2_000;
    private static final int[] TOP_SLOTS = {INPUT_SLOT};
    private static final int[] SIDE_SLOTS = {FUEL_SLOT};
    private static final int[] BOTTOM_SLOTS = {OUTPUT_SLOT, FUEL_SLOT, ASH_SLOT};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int burnTime;
    private int maxBurnTime;
    private int progress;
    private int ashLevelWood;
    private int ashLevelCoal;
    private int ashLevelMisc;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BrickFurnaceBlockEntity.this.maxBurnTime;
                case 1 -> BrickFurnaceBlockEntity.this.burnTime;
                case 2 -> BrickFurnaceBlockEntity.this.progress;
                case 3 -> BrickFurnaceBlockEntity.this.burnTime > 0 ? 1 : 0;
                case 4 -> PROCESS_TIME;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> BrickFurnaceBlockEntity.this.maxBurnTime = value;
                case 1 -> BrickFurnaceBlockEntity.this.burnTime = value;
                case 2 -> BrickFurnaceBlockEntity.this.progress = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public BrickFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.BRICK_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BrickFurnaceBlockEntity furnace) {
        if (!level.isClientSide) {
            furnace.tickServer(level);
        }
    }

    public static int processSpeed(ItemStack stack) {
        if (stack.is(Items.CLAY_BALL) || stack.is(HbmItems.BALL_FIRECLAY.get()) || stack.is(Items.NETHERRACK)) {
            return 4;
        }
        if (stack.is(Items.COBBLESTONE) || stack.is(Items.SAND) || isLegacyLog(stack)) {
            return 2;
        }
        return 1;
    }

    public static int fuelDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int vanilla = stack.getBurnTime(null);
        if (vanilla > 0) {
            return vanilla;
        }
        String path = registryPath(stack);
        if (path.startsWith("coke_")) {
            return 3_200;
        }
        if (path.startsWith("briquette_")) {
            return briquetteDuration(path);
        }
        if (path.startsWith("powder_ash_")) {
            return ashFuelDuration(path);
        }
        return switch (path) {
            case "solid_fuel" -> 3_200;
            case "solid_fuel_presto" -> 8_000;
            case "solid_fuel_presto_triplet" -> 40_000;
            case "solid_fuel_bf" -> 32_000;
            case "solid_fuel_presto_bf" -> 80_000;
            case "solid_fuel_presto_triplet_bf" -> 400_000;
            case "rocket_fuel" -> 6_400;
            case "biomass" -> 400;
            case "biomass_compressed" -> 800;
            case "powder_coal" -> 1_600;
            case "scrap" -> 50;
            case "dust" -> 25;
            case "block_scrap" -> 400;
            case "powder_fire" -> 6_400;
            case "lignite", "powder_lignite" -> 1_200;
            case "block_coke" -> 32_000;
            case "book_guide" -> 200;
            case "coal_infernal" -> 4_800;
            case "crystal_coal" -> 6_400;
            case "powder_sawdust" -> 100;
            default -> 0;
        };
    }

    public static boolean hasSmeltingRecipe(Level level, ItemStack stack) {
        return !stack.isEmpty() && level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent();
    }

    public boolean isWorking() {
        return this.burnTime > 0 && canSmelt(this.level);
    }

    public ContainerData menuData() {
        return this.menuData;
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
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!result.isEmpty()) {
            if (slot == INPUT_SLOT) {
                this.progress = 0;
            }
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
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
            case INPUT_SLOT -> true;
            case FUEL_SLOT -> fuelDuration(stack) > 0;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return TOP_SLOTS;
        }
        if (side == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }
        return SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return (side == Direction.UP && slot == INPUT_SLOT) || (side != Direction.UP && side != Direction.DOWN && slot == FUEL_SLOT && fuelDuration(stack) > 0);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT || slot == ASH_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.progress = 0;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.furnace_brick");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BrickFurnaceMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("Progress", this.progress);
        tag.putInt("AshWood", this.ashLevelWood);
        tag.putInt("AshCoal", this.ashLevelCoal);
        tag.putInt("AshMisc", this.ashLevelMisc);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.progress = tag.getInt("Progress");
        this.ashLevelWood = tag.getInt("AshWood");
        this.ashLevelCoal = tag.getInt("AshCoal");
        this.ashLevelMisc = tag.getInt("AshMisc");
    }

    private void tickServer(Level level) {
        boolean wasBurning = this.burnTime > 0;
        if (this.burnTime > 0) {
            this.burnTime--;
        }

        if (this.burnTime == 0 && canSmelt(level)) {
            int duration = fuelDuration(this.items.get(FUEL_SLOT));
            if (duration > 0) {
                ItemStack consumed = this.items.get(FUEL_SLOT).copy();
                this.items.get(FUEL_SLOT).shrink(1);
                if (this.items.get(FUEL_SLOT).isEmpty()) {
                    this.items.set(FUEL_SLOT, craftingRemaining(consumed));
                }
                addAsh(ashType(consumed), duration);
                this.maxBurnTime = this.burnTime = duration;
            }
        }

        if (this.burnTime > 0 && canSmelt(level)) {
            this.progress += processSpeed(this.items.get(INPUT_SLOT));
            if (this.progress >= PROCESS_TIME) {
                this.progress = 0;
                smelt(level);
            }
        } else {
            this.progress = 0;
        }

        boolean burning = this.burnTime > 0;
        setLit(level, burning);
        if (wasBurning != burning || burning && canSmelt(level)) {
            setChanged();
        }
    }

    private boolean canSmelt(Level level) {
        if (level == null || this.items.get(INPUT_SLOT).isEmpty()) {
            return false;
        }
        SingleRecipeInput input = new SingleRecipeInput(this.items.get(INPUT_SLOT));
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (recipe.isEmpty()) {
            return false;
        }
        ItemStack output = recipe.get().value().assemble(input, level.registryAccess());
        ItemStack current = this.items.get(OUTPUT_SLOT);
        return current.isEmpty() || ItemStack.isSameItemSameComponents(current, output)
                && current.getCount() + output.getCount() <= Math.min(current.getMaxStackSize(), getMaxStackSize(output));
    }

    private void smelt(Level level) {
        SingleRecipeInput input = new SingleRecipeInput(this.items.get(INPUT_SLOT));
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (recipe.isEmpty()) {
            return;
        }
        ItemStack output = recipe.get().value().assemble(input, level.registryAccess());
        if (this.items.get(OUTPUT_SLOT).isEmpty()) {
            this.items.set(OUTPUT_SLOT, output.copy());
        } else {
            this.items.get(OUTPUT_SLOT).grow(output.getCount());
        }
        this.items.get(INPUT_SLOT).shrink(1);
        if (this.items.get(INPUT_SLOT).isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private void addAsh(AshType type, int amount) {
        switch (type) {
            case WOOD -> this.ashLevelWood += amount;
            case COAL -> this.ashLevelCoal += amount;
            case MISC -> this.ashLevelMisc += amount;
        }
        this.ashLevelWood = processAsh(this.ashLevelWood, "wood");
        this.ashLevelCoal = processAsh(this.ashLevelCoal, "coal");
        this.ashLevelMisc = processAsh(this.ashLevelMisc, "misc");
    }

    private int processAsh(int level, String variant) {
        if (level < ASH_THRESHOLD) {
            return level;
        }
        ItemStack ash = HbmItems.variantStack(HbmItems.POWDER_ASH_ITEMS, variant);
        ItemStack current = this.items.get(ASH_SLOT);
        if (current.isEmpty()) {
            this.items.set(ASH_SLOT, ash);
            return level - ASH_THRESHOLD;
        }
        if (ItemStack.isSameItemSameComponents(current, ash) && current.getCount() < current.getMaxStackSize()) {
            current.grow(1);
            return level - ASH_THRESHOLD;
        }
        return level;
    }

    private AshType ashType(ItemStack stack) {
        String path = registryPath(stack).toLowerCase(java.util.Locale.ROOT);
        if (path.contains("coke") || path.contains("coal") || path.contains("lignite")) {
            return AshType.COAL;
        }
        if (stack.is(ItemTags.LOGS) || path.contains("wood") || path.contains("sapling")) {
            return AshType.WOOD;
        }
        return AshType.MISC;
    }

    private static boolean isLegacyLog(ItemStack stack) {
        return stack.is(Items.OAK_LOG)
                || stack.is(Items.SPRUCE_LOG)
                || stack.is(Items.BIRCH_LOG)
                || stack.is(Items.JUNGLE_LOG)
                || stack.is(Items.ACACIA_LOG)
                || stack.is(Items.DARK_OAK_LOG);
    }

    private void setLit(Level level, boolean lit) {
        BlockState state = level.getBlockState(this.worldPosition);
        if (state.hasProperty(BrickFurnaceBlock.LIT) && state.getValue(BrickFurnaceBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(BrickFurnaceBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private static ItemStack craftingRemaining(ItemStack stack) {
        if (stack.isEmpty() || !stack.getItem().hasCraftingRemainingItem(stack)) {
            return ItemStack.EMPTY;
        }
        return stack.getItem().getCraftingRemainingItem(stack.copyWithCount(1));
    }

    private static int briquetteDuration(String path) {
        return switch (path) {
            case "briquette_coal" -> 2_000;
            case "briquette_lignite" -> 1_600;
            case "briquette_wood" -> 400;
            default -> 0;
        };
    }

    private static int ashFuelDuration(String path) {
        return switch (path) {
            case "powder_ash_coal", "powder_ash_fly" -> 200;
            case "powder_ash_wood", "powder_ash_misc", "powder_ash_soot" -> 100;
            default -> 0;
        };
    }

    private static String registryPath(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private enum AshType {
        WOOD,
        COAL,
        MISC
    }
}
