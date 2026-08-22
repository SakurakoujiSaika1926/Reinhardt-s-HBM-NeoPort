package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.menu.FunnelMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FunnelBlockEntity extends BlockEntity implements WorldlyContainer, MachineInventory, MenuProvider {
    public static final int MODE_ALL = 0;
    public static final int MODE_3X3 = 1;
    public static final int MODE_2X2 = 2;
    public static final int INPUT_START = 0;
    public static final int INPUT_END = 9;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_END = 18;
    public static final int SLOT_COUNT = 18;
    public static final int DATA_COUNT = 1;

    private static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OUTPUT_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int mode = MODE_ALL;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? FunnelBlockEntity.this.mode : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                FunnelBlockEntity.this.mode = value >= MODE_ALL && value <= MODE_2X2 ? value : MODE_ALL;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FunnelBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FUNNEL.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FunnelBlockEntity funnel) {
        if (level.isClientSide) {
            return;
        }
        funnel.process(level);
    }

    public int mode() {
        return this.mode;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public void cycleMode() {
        this.mode = (this.mode + 1) % 3;
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void process(Level level) {
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            ItemStack input = this.items.get(slot);
            if (input.isEmpty()) {
                continue;
            }

            ItemStack result = ItemStack.EMPTY;
            int consumed = 0;
            if (this.mode != MODE_2X2 && input.getCount() >= 9) {
                result = getCraftingResult(level, input, 3);
                if (!result.isEmpty()) {
                    consumed = 9;
                }
            }
            if (result.isEmpty() && this.mode != MODE_3X3 && input.getCount() >= 4) {
                result = getCraftingResult(level, input, 2);
                if (!result.isEmpty()) {
                    consumed = 4;
                }
            }
            if (result.isEmpty() || !canMergeInto(slot + OUTPUT_START, result)) {
                continue;
            }

            ItemStack output = this.items.get(slot + OUTPUT_START);
            if (output.isEmpty()) {
                this.items.set(slot + OUTPUT_START, result.copy());
            } else {
                output.grow(result.getCount());
            }
            input.shrink(consumed);
            if (input.isEmpty()) {
                this.items.set(slot, ItemStack.EMPTY);
            }
            this.setChanged();
        }
    }

    private boolean canMergeInto(int outputSlot, ItemStack result) {
        ItemStack output = this.items.get(outputSlot);
        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    @Nullable
    private ItemStack getCraftingResult(Level level, ItemStack ingredient, int size) {
        NonNullList<ItemStack> grid = NonNullList.withSize(size * size, ItemStack.EMPTY);
        ItemStack singular = ingredient.copyWithCount(1);
        for (int slot = 0; slot < grid.size(); slot++) {
            grid.set(slot, singular.copy());
        }
        CraftingInput input = CraftingInput.of(size, size, grid);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(RecipeHolder::value)
                .map(recipe -> recipe.assemble(input, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
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
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < INPUT_START || slot >= INPUT_END || stack.isEmpty()) {
            return false;
        }
        return !this.items.get(slot).isEmpty() || !getCraftingResultForValidation(stack).isEmpty();
    }

    private ItemStack getCraftingResultForValidation(ItemStack stack) {
        if (this.level == null) {
            return ItemStack.EMPTY;
        }
        if (this.mode != MODE_2X2) {
            ItemStack result = getCraftingResult(this.level, stack, 3);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return this.mode != MODE_3X3 ? getCraftingResult(this.level, stack, 2) : ItemStack.EMPTY;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN ? slot >= OUTPUT_START : side != Direction.UP && slot < INPUT_END;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        this.clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_funnel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FunnelMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("mode", this.mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.mode = Math.max(MODE_ALL, Math.min(MODE_2X2, tag.getInt("mode")));
    }
}
