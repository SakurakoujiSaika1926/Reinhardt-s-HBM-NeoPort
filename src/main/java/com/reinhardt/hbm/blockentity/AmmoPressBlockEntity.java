package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.menu.AmmoPressMenu;
import com.reinhardt.hbm.recipe.AmmoPressRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class AmmoPressBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_START = 0;
    public static final int INPUT_END = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int SLOT_COUNT = 10;
    public static final int DATA_COUNT = 2;

    private static final int[] AUTOMATION_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private Optional<ResourceLocation> selectedRecipeId = Optional.empty();
    private int animationTicks;
    private AnimationState animationState = AnimationState.LIFTING;
    private float previousLift;
    private float lift;
    private float previousPress;
    private float press;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> AmmoPressBlockEntity.this.selectedRecipeIndex() + 1;
                case 1 -> AmmoPressBlockEntity.this.animationTicks;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                AmmoPressBlockEntity.this.selectRecipeByIndex(value - 1, false);
            } else if (index == 1) {
                AmmoPressBlockEntity.this.animationTicks = Math.max(0, value);
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AmmoPressBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.AMMO_PRESS.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AmmoPressBlockEntity ammoPress) {
        if (level.isClientSide) {
            ammoPress.tickAnimation();
            return;
        }

        if (ammoPress.animationTicks > 0) {
            ammoPress.animationTicks--;
        }
        if (ammoPress.processAll(level)) {
            ammoPress.animationTicks = 40;
            ammoPress.setChangedAndSync();
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
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
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return validSlot(slot) ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < INPUT_START || slot >= INPUT_END || stack.isEmpty()) {
            return false;
        }
        return selectedRecipe()
                .map(holder -> {
                    AmmoPressRecipe.SlotIngredient required = holder.value().input().get(slot);
                    return !required.isEmpty() && required.ingredient().orElseThrow().test(stack);
                })
                .orElse(false);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
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
        this.items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.ammo_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AmmoPressMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public List<RecipeHolder<AmmoPressRecipe>> recipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.AMMO_PRESS.get()).stream()
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    public Optional<RecipeHolder<AmmoPressRecipe>> selectedRecipe() {
        if (this.level == null || this.selectedRecipeId.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().byKey(this.selectedRecipeId.get())
                .filter(holder -> holder.value() instanceof AmmoPressRecipe)
                .map(holder -> {
                    @SuppressWarnings("unchecked")
                    RecipeHolder<AmmoPressRecipe> recipe = (RecipeHolder<AmmoPressRecipe>) holder;
                    return recipe;
                });
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId != null && this.selectedRecipeId.filter(recipeId::equals).isPresent()) {
            this.selectedRecipeId = Optional.empty();
        } else {
            this.selectedRecipeId = Optional.ofNullable(recipeId);
        }
        setChangedAndSync();
    }

    public void selectRecipeByIndex(int recipeIndex, boolean toggle) {
        if (this.level == null) {
            return;
        }
        List<RecipeHolder<AmmoPressRecipe>> recipes = recipes(this.level);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            this.selectedRecipeId = Optional.empty();
        } else {
            ResourceLocation id = recipes.get(recipeIndex).id();
            this.selectedRecipeId = toggle && this.selectedRecipeId.filter(id::equals).isPresent()
                    ? Optional.empty()
                    : Optional.of(id);
        }
        setChangedAndSync();
    }

    public int selectedRecipeIndex() {
        if (this.level == null || this.selectedRecipeId.isEmpty()) {
            return -1;
        }
        List<RecipeHolder<AmmoPressRecipe>> recipes = recipes(this.level);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(this.selectedRecipeId.get())) {
                return index;
            }
        }
        return -1;
    }

    public float lift(float partialTick) {
        return this.previousLift + (this.lift - this.previousLift) * partialTick;
    }

    public float press(float partialTick) {
        return this.previousPress + (this.press - this.previousPress) * partialTick;
    }

    public boolean showBullets() {
        return this.animationState == AnimationState.RETRACTING || this.animationState == AnimationState.LOWERING;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy());
                level.addFreshEntity(entity);
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        this.selectedRecipeId.ifPresent(id -> tag.putString("SelectedRecipe", id.toString()));
        tag.putInt("AnimationTicks", this.animationTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.selectedRecipeId = ResourceLocation.tryParse(tag.getString("SelectedRecipe")) == null
                ? Optional.empty()
                : Optional.of(ResourceLocation.parse(tag.getString("SelectedRecipe")));
        this.animationTicks = Math.max(0, tag.getInt("AnimationTicks"));
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

    private boolean processAll(Level level) {
        Optional<RecipeHolder<AmmoPressRecipe>> selected = selectedRecipe();
        if (selected.isEmpty()) {
            return false;
        }

        AmmoPressRecipe recipe = selected.get().value();
        boolean crafted = false;
        while (canProcess(recipe)) {
            for (int slot = INPUT_START; slot < INPUT_END; slot++) {
                AmmoPressRecipe.SlotIngredient required = recipe.input().get(slot);
                if (!required.isEmpty()) {
                    this.items.get(slot).shrink(required.count());
                    if (this.items.get(slot).isEmpty()) {
                        this.items.set(slot, ItemStack.EMPTY);
                    }
                }
            }

            ItemStack output = this.items.get(OUTPUT_SLOT);
            if (output.isEmpty()) {
                this.items.set(OUTPUT_SLOT, recipe.result().copy());
            } else {
                output.grow(recipe.result().getCount());
            }
            crafted = true;
        }
        return crafted;
    }

    private boolean canProcess(AmmoPressRecipe recipe) {
        if (!recipe.matches(new AmmoPressRecipe.Input(this.items.subList(INPUT_START, INPUT_END)), this.level)) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, recipe.result())
                && output.getCount() + recipe.result().getCount() <= output.getMaxStackSize());
    }

    private void tickAnimation() {
        this.previousLift = this.lift;
        this.previousPress = this.press;
        if (this.animationTicks > 0) {
            this.animationTicks--;
        }
        if (this.animationTicks <= 0 && this.lift <= 0.0F) {
            return;
        }

        switch (this.animationState) {
            case LIFTING -> {
                this.lift += 1.0F / 40.0F;
                if (this.lift >= 1.0F) {
                    this.lift = 1.0F;
                    this.animationState = AnimationState.PRESSING;
                }
            }
            case PRESSING -> {
                this.press += 1.0F / 20.0F;
                if (this.press >= 1.0F) {
                    this.press = 1.0F;
                    this.animationState = AnimationState.RETRACTING;
                }
            }
            case RETRACTING -> {
                this.press -= 1.0F / 20.0F;
                if (this.press <= 0.0F) {
                    this.press = 0.0F;
                    this.animationState = AnimationState.LOWERING;
                }
            }
            case LOWERING -> {
                this.lift -= 1.0F / 40.0F;
                if (this.lift <= 0.0F) {
                    this.lift = 0.0F;
                    this.animationState = AnimationState.LIFTING;
                }
            }
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private enum AnimationState {
        LIFTING,
        PRESSING,
        RETRACTING,
        LOWERING
    }
}
