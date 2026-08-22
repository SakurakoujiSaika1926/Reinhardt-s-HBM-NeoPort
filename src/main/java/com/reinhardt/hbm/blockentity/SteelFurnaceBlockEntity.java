package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LegacyFurnaceBlock;
import com.reinhardt.hbm.item.OilTarItem;
import com.reinhardt.hbm.menu.SteelFurnaceMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

public class SteelFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int FIRST_INPUT_SLOT = 0;
    public static final int FIRST_OUTPUT_SLOT = 3;
    public static final int SLOT_COUNT = 6;
    public static final int DATA_COUNT = 8;
    public static final int PROCESS_TIME = 40_000;
    public static final int MAX_HEAT = 100_000;
    public static final double DIFFUSION = 0.05D;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final int[] progress = new int[3];
    private final int[] bonus = new int[3];
    private final ItemStack[] lastItems = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private int heat;
    private boolean wasOn;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0, 1, 2 -> SteelFurnaceBlockEntity.this.progress[index];
                case 3, 4, 5 -> SteelFurnaceBlockEntity.this.bonus[index - 3];
                case 6 -> SteelFurnaceBlockEntity.this.heat;
                case 7 -> SteelFurnaceBlockEntity.this.wasOn ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0, 1, 2 -> SteelFurnaceBlockEntity.this.progress[index] = value;
                case 3, 4, 5 -> SteelFurnaceBlockEntity.this.bonus[index - 3] = value;
                case 6 -> SteelFurnaceBlockEntity.this.heat = Math.max(0, Math.min(MAX_HEAT, value));
                case 7 -> SteelFurnaceBlockEntity.this.wasOn = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SteelFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.STEEL_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteelFurnaceBlockEntity furnace) {
        if (!level.isClientSide) {
            furnace.tickServer(level);
        }
    }

    public static boolean hasSmeltingRecipe(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent();
    }

    public int progress(int index) {
        return index >= 0 && index < this.progress.length ? this.progress[index] : 0;
    }

    public int bonus(int index) {
        return index >= 0 && index < this.bonus.length ? this.bonus[index] : 0;
    }

    public int heat() {
        return this.heat;
    }

    public boolean wasOn() {
        return this.wasOn;
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
            if (isInputSlot(slot)) {
                resetLine(slot);
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
        if (isInputSlot(slot)) {
            resetLine(slot);
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isInputSlot(slot) && this.level != null && hasSmeltingRecipe(this.level, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isInputSlot(slot) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isOutputSlot(slot);
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
        for (int index = 0; index < 3; index++) {
            this.progress[index] = 0;
            this.bonus[index] = 0;
            this.lastItems[index] = ItemStack.EMPTY;
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.furnace_steel");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SteelFurnaceMenu(containerId, playerInventory, this, this.menuData);
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
        tag.putIntArray("Progress", this.progress);
        tag.putIntArray("Bonus", this.bonus);
        tag.putInt("Heat", this.heat);
        tag.putBoolean("WasOn", this.wasOn);
        for (int index = 0; index < this.lastItems.length; index++) {
            tag.put("LastItem" + index, this.lastItems[index].saveOptional(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        loadArray(tag.getIntArray("Progress"), this.progress);
        loadArray(tag.getIntArray("Bonus"), this.bonus);
        this.heat = Math.max(0, Math.min(MAX_HEAT, tag.getInt("Heat")));
        this.wasOn = tag.getBoolean("WasOn");
        for (int index = 0; index < this.lastItems.length; index++) {
            this.lastItems[index] = ItemStack.parseOptional(registries, tag.getCompound("LastItem" + index));
        }
    }

    private void tickServer(Level level) {
        tryPullHeat(level);
        this.wasOn = false;
        int burn = (this.heat - MAX_HEAT / 3) / 10;

        for (int index = 0; index < 3; index++) {
            ItemStack input = this.items.get(index);
            if (input.isEmpty() || this.lastItems[index].isEmpty() || !ItemStack.isSameItemSameComponents(input, this.lastItems[index])) {
                this.progress[index] = 0;
                this.bonus[index] = 0;
            }

            Optional<RecipeHolder<SmeltingRecipe>> recipe = canSmelt(level, index);
            if (recipe.isPresent() && burn > 0) {
                this.progress[index] += burn;
                this.heat = Math.max(0, this.heat - burn);
                this.wasOn = true;
            }

            this.lastItems[index] = input.copyWithCount(1);

            if (this.progress[index] >= PROCESS_TIME && recipe.isPresent()) {
                finishSmelting(level, index, recipe.get());
                this.progress[index] = 0;
            }
        }

        setLit(level, this.wasOn);
        if (this.wasOn && level.getGameTime() % 20L == 0L) {
            HbmPollution.increment(level, this.worldPosition, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND * 2.0D);
        }
        setChanged();
    }

    private void tryPullHeat(Level level) {
        if (this.heat >= MAX_HEAT) {
            return;
        }

        HeatSourceBlockEntity source = heatSourceBelow(level);
        if (source == null) {
            coolPassively();
            return;
        }

        int diff = source.getHeatStored() - this.heat;
        if (diff > 0) {
            int pulled = (int) Math.ceil(diff * DIFFUSION);
            pulled = Math.min(pulled, MAX_HEAT - this.heat);
            if (pulled > 0) {
                source.useHeat(pulled);
                this.heat = Math.min(MAX_HEAT, this.heat + pulled);
            }
        } else if (diff < 0) {
            coolPassively();
        }
    }

    @Nullable
    private HeatSourceBlockEntity heatSourceBelow(Level level) {
        BlockEntity below = level.getBlockEntity(this.worldPosition.below());
        if (below instanceof HeatSourceBlockEntity source && below != this) {
            return source;
        }
        if (below instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof HeatSourceBlockEntity source) {
            return source;
        }
        return null;
    }

    private void coolPassively() {
        if (this.heat > 0) {
            this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
        }
    }

    private Optional<RecipeHolder<SmeltingRecipe>> canSmelt(Level level, int index) {
        if (this.heat < MAX_HEAT / 3) {
            return Optional.empty();
        }
        ItemStack input = this.items.get(index);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SmeltingRecipe>> holder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        ItemStack result = holder.get().value().assemble(recipeInput, level.registryAccess());
        return canPlaceOutput(index + 3, result) ? holder : Optional.empty();
    }

    private void finishSmelting(Level level, int index, RecipeHolder<SmeltingRecipe> recipe) {
        ItemStack input = this.items.get(index);
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        ItemStack result = recipe.value().assemble(recipeInput, level.registryAccess());
        addOutput(index + 3, result);
        addBonus(input, index);

        while (this.bonus[index] >= 100 && canPlaceOutput(index + 3, result)) {
            addOutput(index + 3, result);
            this.bonus[index] -= 100;
        }

        input.shrink(1);
        if (input.isEmpty()) {
            this.items.set(index, ItemStack.EMPTY);
        }
    }

    private void addBonus(ItemStack stack, int index) {
        if (isOre(stack)) {
            this.bonus[index] += 25;
        } else if (stack.is(ItemTags.LOGS) || isTar(stack)) {
            this.bonus[index] += 50;
        }
    }

    private static boolean isOre(ItemStack stack) {
        for (TagKey<net.minecraft.world.item.Item> tag : stack.getTags().toList()) {
            if ("c".equals(tag.location().getNamespace()) && tag.location().getPath().startsWith("ores")) {
                return true;
            }
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.startsWith("ore_") || path.contains("_ore") || path.startsWith("raw_");
    }

    private static boolean isTar(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return stack.getItem() instanceof OilTarItem || path.contains("tar");
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(slot);
        if (output.isEmpty()) {
            return stack.getCount() <= stack.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(output, stack)
                && output.getCount() + stack.getCount() <= Math.min(output.getMaxStackSize(), getMaxStackSize(output));
    }

    private void addOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack output = this.items.get(slot);
        if (output.isEmpty()) {
            this.items.set(slot, stack.copy());
        } else if (ItemStack.isSameItemSameComponents(output, stack)) {
            output.grow(stack.getCount());
        }
    }

    private void resetLine(int inputSlot) {
        this.progress[inputSlot] = 0;
        this.bonus[inputSlot] = 0;
        this.lastItems[inputSlot] = ItemStack.EMPTY;
    }

    private static boolean isInputSlot(int slot) {
        return slot >= FIRST_INPUT_SLOT && slot < FIRST_OUTPUT_SLOT;
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < SLOT_COUNT;
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < this.items.size();
    }

    private void setLit(Level level, boolean lit) {
        BlockState state = level.getBlockState(this.worldPosition);
        if (state.hasProperty(LegacyFurnaceBlock.LIT) && state.getValue(LegacyFurnaceBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(LegacyFurnaceBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private static void loadArray(int[] source, int[] target) {
        for (int index = 0; index < target.length && index < source.length; index++) {
            target[index] = source[index];
        }
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }
}
