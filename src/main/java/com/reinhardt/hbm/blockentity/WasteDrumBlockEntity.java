package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.menu.WasteDrumMenu;
import com.reinhardt.hbm.recipe.FuelPoolRecipe;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.item.RbmkFuelRodItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;

import javax.annotation.Nullable;
import java.util.Optional;

public class WasteDrumBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 12;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public WasteDrumBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WASTE_DRUM.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WasteDrumBlockEntity drum) {
        if (level.isClientSide) {
            return;
        }
        int water = 0;
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                water++;
            }
        }
        if (water <= 0) {
            return;
        }
        int chance = 60 * 60 * 20 / water;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = drum.items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof RbmkFuelRodItem) {
                RbmkFuelRodItem.coolInSpentFuelPool(
                        stack,
                        HbmConfig.RBMK_FUEL_DIFFUSION_MOD.get(),
                        HbmConfig.RBMK_HEAT_PROVISION.get()
                );
                drum.setChanged();
                continue;
            }
            if (level.random.nextInt(chance) != 0) {
                continue;
            }
            Optional<RecipeHolder<FuelPoolRecipe>> recipe = drum.recipeFor(stack);
            if (recipe.isPresent()) {
                drum.items.set(slot, recipe.get().value().result().copy());
                drum.setChanged();
            }
        }
    }

    public Optional<RecipeHolder<FuelPoolRecipe>> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(HbmRecipeTypes.FUEL_POOL.get(), new FuelPoolRecipe.Input(stack), this.level);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
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
            setChanged();
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
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof RbmkFuelRodItem || recipeFor(stack).isPresent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (stack.getItem() instanceof RbmkFuelRodItem) {
            return RbmkFuelRodItem.coreHeat(stack) < 50.0F && RbmkFuelRodItem.hullHeat(stack) < 50.0F;
        }
        return recipeFor(stack).isEmpty();
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
        return Component.translatable("container.reinhardtshbm.waste_drum");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WasteDrumMenu(containerId, inventory, this);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }
}
