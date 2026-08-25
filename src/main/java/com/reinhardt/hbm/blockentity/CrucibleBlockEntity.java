package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.foundry.CrucibleAcceptor;
import com.reinhardt.hbm.foundry.CrucibleUtil;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.menu.CrucibleMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.recipe.CrucibleRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CrucibleBlockEntity extends BlockEntity implements CrucibleAcceptor, WorldlyContainer, MenuProvider, MachineInventory {
    public static final int SLOT_COUNT = 10;
    public static final int INPUT_START = 1;
    public static final int INPUT_END = 10;
    public static final int DATA_COUNT = 7;
    public static final int RECIPE_CAPACITY = FoundryShape.BLOCK.q(16);
    public static final int WASTE_CAPACITY = FoundryShape.BLOCK.q(16);
    public static final int PROCESS_TIME = 20_000;
    public static final int MAX_HEAT = 100_000;
    private static final double DIFFUSION = 0.25D;
    private static final int[] INPUT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    private final List<ItemStack> items = new ArrayList<>(SLOT_COUNT);
    private final List<FoundryMaterialStack> recipeStack = new ArrayList<>();
    private final List<FoundryMaterialStack> wasteStack = new ArrayList<>();
    @Nullable
    private ResourceLocation selectedRecipe;
    private int heat;
    private int progress;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CrucibleBlockEntity.this.heat;
                case 1 -> CrucibleBlockEntity.this.progress;
                case 2 -> RECIPE_CAPACITY;
                case 3 -> WASTE_CAPACITY;
                case 4 -> CrucibleBlockEntity.this.selectedRecipeIndex() + 1;
                case 5 -> CrucibleBlockEntity.this.totalAmount(CrucibleBlockEntity.this.recipeStack);
                case 6 -> CrucibleBlockEntity.this.totalAmount(CrucibleBlockEntity.this.wasteStack);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                CrucibleBlockEntity.this.heat = value;
            } else if (index == 1) {
                CrucibleBlockEntity.this.progress = value;
            } else if (index == 4) {
                CrucibleBlockEntity.this.setSelectedRecipeByIndex(value - 1);
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CRUCIBLE.get(), pos, state);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.add(ItemStack.EMPTY);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        if (level.isClientSide) {
            return;
        }

        crucible.tryPullHeat();
        if (level.getGameTime() % 5L == 0L) {
            crucible.collectDroppedItems();
        }
        crucible.damageEntitiesInMoltenSurface();

        if (!crucible.trySmelt()) {
            crucible.progress = 0;
        }
        crucible.tryRecipe();
        crucible.tryPourStacks();
        // TileEntityCrucible emitted soot for each occupied molten stack while it
        // attempted to pour, not merely while an input was actively smelting.
        if (!crucible.wasteStack.isEmpty()) {
            HbmPollution.increment(level, pos, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND / 20.0D);
        }
        if (!crucible.recipeStack.isEmpty()) {
            HbmPollution.increment(level, pos, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND / 20.0D);
        }
        crucible.cleanupStacks();

        crucible.setChanged();
        if (level.getGameTime() % 10L == 0L) {
            crucible.syncToClient();
        }
    }

    public List<FoundryMaterialStack> recipeStack() {
        return List.copyOf(this.recipeStack);
    }

    public List<FoundryMaterialStack> wasteStack() {
        return List.copyOf(this.wasteStack);
    }

    public int heat() {
        return this.heat;
    }

    public int progress() {
        return this.progress;
    }

    public Optional<ResourceLocation> selectedRecipeId() {
        return Optional.ofNullable(this.selectedRecipe);
    }

    public Optional<RecipeHolder<CrucibleRecipe>> selectedRecipe(Level level) {
        if (this.selectedRecipe == null) {
            return Optional.empty();
        }
        return recipes(level).stream().filter(holder -> holder.id().equals(this.selectedRecipe)).findFirst();
    }

    public List<RecipeHolder<CrucibleRecipe>> availableRecipes(Level level) {
        return recipes(level);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        this.selectedRecipe = recipeId;
        setChanged();
        syncToClient();
    }

    public void clearMoltenToScraps(Player player) {
        List<FoundryMaterialStack> stacks = new ArrayList<>();
        stacks.addAll(this.recipeStack);
        stacks.addAll(this.wasteStack);
        for (FoundryMaterialStack stack : stacks) {
            ItemStack scrap = ScrapsItem.create(stack, false);
            if (!player.getInventory().add(scrap)) {
                player.drop(scrap, false);
            }
        }
        this.recipeStack.clear();
        this.wasteStack.clear();
        setChanged();
        syncToClient();
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack) {
        if (stack.material().behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE) {
            return false;
        }
        Optional<RecipeHolder<CrucibleRecipe>> recipe = selectedRecipe(level);
        if (recipe.isEmpty()) {
            return totalAmount(this.wasteStack) < WASTE_CAPACITY;
        }

        CrucibleRecipe value = recipe.get().value();
        int required = amountInRecipe(value.input(), stack.material());
        if (required <= 0) {
            return false;
        }
        int maximum = required * RECIPE_CAPACITY / Math.max(1, value.inputAmount());
        return amountIn(this.recipeStack, stack.material()) < maximum && totalAmount(this.recipeStack) < RECIPE_CAPACITY;
    }

    @Override
    public FoundryMaterialStack pour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack) {
        Optional<RecipeHolder<CrucibleRecipe>> recipe = selectedRecipe(level);
        if (recipe.isEmpty()) {
            return addWithCapacity(this.wasteStack, stack, WASTE_CAPACITY);
        }

        CrucibleRecipe value = recipe.get().value();
        int required = amountInRecipe(value.input(), stack.material());
        if (required <= 0) {
            return stack;
        }
        int materialMaximum = required * RECIPE_CAPACITY / Math.max(1, value.inputAmount());
        int materialRoom = materialMaximum - amountIn(this.recipeStack, stack.material());
        int totalRoom = RECIPE_CAPACITY - totalAmount(this.recipeStack);
        int accepted = Math.min(stack.amount(), Math.min(materialRoom, totalRoom));
        if (accepted <= 0) {
            return stack;
        }
        addToStack(this.recipeStack, new FoundryMaterialStack(stack.material(), accepted));
        setChanged();
        syncToClient();
        int leftover = stack.amount() - accepted;
        return leftover <= 0 ? null : new FoundryMaterialStack(stack.material(), leftover);
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
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        if (slot >= 0 && slot < SLOT_COUNT) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            this.items.set(slot, stack);
            if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
                stack.setCount(getMaxStackSize(stack));
            }
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= INPUT_START && slot < INPUT_END && isItemSmeltable(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return INPUT_SLOTS;
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
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_crucible");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrucibleMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            drop(level, pos, stack);
        }
        for (FoundryMaterialStack stack : this.recipeStack) {
            drop(level, pos, ScrapsItem.create(stack, false));
        }
        for (FoundryMaterialStack stack : this.wasteStack) {
            drop(level, pos, ScrapsItem.create(stack, false));
        }
        clearContent();
        this.recipeStack.clear();
        this.wasteStack.clear();
    }

    private void tryPullHeat() {
        if (this.level == null || this.heat >= MAX_HEAT) {
            return;
        }

        HeatSourceBlockEntity source = heatSourceBelow();
        if (source != null) {
            int diff = Math.min(source.getHeatStored() - this.heat, MAX_HEAT - this.heat);
            if (diff > 0) {
                int pulled = (int) Math.ceil(diff * DIFFUSION);
                source.useHeat(pulled);
                this.heat = Math.min(MAX_HEAT, this.heat + pulled);
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    @Nullable
    private HeatSourceBlockEntity heatSourceBelow() {
        if (this.level == null) {
            return null;
        }
        BlockEntity below = this.level.getBlockEntity(this.worldPosition.below());
        if (below instanceof HeatSourceBlockEntity source) {
            return source;
        }
        if (below instanceof MachineDummyBlockEntity dummy
                && this.level.getBlockEntity(dummy.getCorePos()) instanceof HeatSourceBlockEntity source) {
            return source;
        }
        return null;
    }

    private void collectDroppedItems() {
        if (this.level == null) {
            return;
        }
        AABB area = new AABB(this.worldPosition).inflate(1.0D, 0.0D, 1.0D).move(0.0D, 0.75D, 0.0D);
        for (ItemEntity itemEntity : this.level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (!itemEntity.isAlive()) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (!isItemSmeltable(stack)) {
                continue;
            }
            for (int slot = INPUT_START; slot < INPUT_END; slot++) {
                if (!getItem(slot).isEmpty()) {
                    continue;
                }
                ItemStack inserted = stack.copyWithCount(1);
                setItem(slot, inserted);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(stack);
                    itemEntity.setPickUpDelay(60);
                }
                break;
            }
        }
    }

    private void damageEntitiesInMoltenSurface() {
        if (this.level == null) {
            return;
        }
        int totalCapacity = RECIPE_CAPACITY + WASTE_CAPACITY;
        int totalMass = totalAmount(this.recipeStack) + totalAmount(this.wasteStack);
        if (totalMass <= 0) {
            return;
        }
        double moltenLevel = ((double) totalMass / (double) totalCapacity) * 0.875D;
        AABB area = new AABB(
                this.worldPosition.getX() - 1.0D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() - 1.0D,
                this.worldPosition.getX() + 2.0D,
                this.worldPosition.getY() + 0.5D + moltenLevel,
                this.worldPosition.getZ() + 2.0D
        );
        DamageSources damageSources = this.level.damageSources();
        for (LivingEntity entity : this.level.getEntitiesOfClass(LivingEntity.class, area)) {
            entity.hurt(damageSources.lava(), 5.0F);
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 100));
        }
    }

    private boolean trySmelt() {
        if (this.heat < MAX_HEAT / 2) {
            return false;
        }
        int slot = firstSmeltableSlot();
        if (slot < 0) {
            return false;
        }

        int delta = (int) ((this.heat - (MAX_HEAT / 2)) * 0.05D);
        this.progress += delta;
        this.heat -= delta;
        if (this.progress >= PROCESS_TIME) {
            this.progress = 0;
            List<FoundryMaterialStack> materials = FoundryMaterial.smeltingMaterialsFromItem(getItem(slot));
            Optional<RecipeHolder<CrucibleRecipe>> recipe = this.level == null ? Optional.empty() : selectedRecipe(this.level);
            for (FoundryMaterialStack material : materials) {
                boolean recipeMaterial = recipe.isPresent()
                        && (amountInRecipe(recipe.get().value().input(), material.material()) > 0
                        || amountInRecipe(recipe.get().value().output(), material.material()) > 0);
                if (recipeMaterial) {
                    addToStack(this.recipeStack, material);
                } else {
                    addToStack(this.wasteStack, material);
                }
            }
            getItem(slot).shrink(1);
        }
        return true;
    }

    private void tryRecipe() {
        if (this.level == null) {
            return;
        }
        Optional<RecipeHolder<CrucibleRecipe>> recipe = selectedRecipe(this.level);
        if (recipe.isEmpty()) {
            return;
        }
        CrucibleRecipe value = recipe.get().value();
        if (this.level.getGameTime() % value.frequency() != 0L) {
            return;
        }
        for (CrucibleRecipe.MaterialIngredient input : value.input()) {
            if (amountIn(this.recipeStack, input.material()) < input.amount()) {
                return;
            }
        }
        for (CrucibleRecipe.MaterialIngredient input : value.input()) {
            subtractFromStack(this.recipeStack, input.material(), input.amount());
        }
        for (CrucibleRecipe.MaterialIngredient output : value.output()) {
            addToStack(this.recipeStack, output.stack());
        }
    }

    private void tryPourStacks() {
        if (this.level == null) {
            return;
        }
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        tryPour(this.wasteStack, facing.getOpposite(), material -> true);

        Optional<RecipeHolder<CrucibleRecipe>> recipe = selectedRecipe(this.level);
        Predicate<FoundryMaterial> filter = material -> recipe.isEmpty()
                || amountInRecipe(recipe.get().value().output(), material) > 0;
        tryPour(this.recipeStack, facing, filter);
    }

    private void tryPour(List<FoundryMaterialStack> stacks, Direction direction, Predicate<FoundryMaterial> materialFilter) {
        if (this.level == null || stacks.isEmpty()) {
            return;
        }
        double x = this.worldPosition.getX() + 0.5D + direction.getStepX() * 1.875D;
        double y = this.worldPosition.getY() + 0.25D;
        double z = this.worldPosition.getZ() + 0.5D + direction.getStepZ() * 1.875D;
        FoundryMaterialStack poured = CrucibleUtil.pourFullStack(
                this.level,
                x,
                y,
                z,
                6.0D,
                true,
                stacks,
                FoundryShape.NUGGET.q(3),
                materialFilter
        );
        if (poured != null) {
            setChanged();
            syncToClient();
        }
    }

    private int firstSmeltableSlot() {
        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            if (isItemSmeltable(getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    public boolean isItemSmeltable(ItemStack stack) {
        List<FoundryMaterialStack> materials = FoundryMaterial.smeltingMaterialsFromItem(stack);
        if (materials.isEmpty()) {
            return false;
        }

        Optional<RecipeHolder<CrucibleRecipe>> recipe = this.level == null ? Optional.empty() : selectedRecipe(this.level);
        boolean matchesRecipe = recipe.isEmpty();
        int recipeContent = recipe.map(holder -> holder.value().inputAmount()).orElse(0);
        int recipeAmount = totalAmount(this.recipeStack);
        int wasteAmount = totalAmount(this.wasteStack);

        for (FoundryMaterialStack material : materials) {
            int required = recipe.map(holder -> amountInRecipe(holder.value().input(), material.material())).orElse(0);
            if (recipe.isPresent() && amountInRecipe(recipe.get().value().output(), material.material()) > 0) {
                recipeAmount += material.amount();
                matchesRecipe = true;
                continue;
            }

            if (required == 0) {
                // With no selected recipe, legacy crucibles classify all input
                // as waste and discharge it through the rear pour outlet.
                wasteAmount += material.amount();
            } else {
                int maximum = required * RECIPE_CAPACITY / Math.max(1, recipeContent);
                int stored = amountIn(this.recipeStack, material.material());
                matchesRecipe = true;
                recipeAmount += material.amount();
                if (stored + material.amount() > maximum) {
                    return false;
                }
            }
        }

        return recipeAmount <= RECIPE_CAPACITY && wasteAmount <= WASTE_CAPACITY && matchesRecipe;
    }

    private void cleanupStacks() {
        this.recipeStack.removeIf(stack -> stack.amount() <= 0);
        this.wasteStack.removeIf(stack -> stack.amount() <= 0);
    }

    private FoundryMaterialStack addWithCapacity(List<FoundryMaterialStack> stacks, FoundryMaterialStack stack, int capacity) {
        int room = capacity - totalAmount(stacks);
        int accepted = Math.min(stack.amount(), room);
        if (accepted <= 0) {
            return stack;
        }
        addToStack(stacks, new FoundryMaterialStack(stack.material(), accepted));
        setChanged();
        syncToClient();
        int leftover = stack.amount() - accepted;
        return leftover <= 0 ? null : new FoundryMaterialStack(stack.material(), leftover);
    }

    private static void addToStack(List<FoundryMaterialStack> stacks, FoundryMaterialStack materialStack) {
        for (int index = 0; index < stacks.size(); index++) {
            FoundryMaterialStack current = stacks.get(index);
            if (current.material() == materialStack.material()) {
                stacks.set(index, new FoundryMaterialStack(current.material(), current.amount() + materialStack.amount()));
                return;
            }
        }
        stacks.add(materialStack.copy());
    }

    private static void subtractFromStack(List<FoundryMaterialStack> stacks, FoundryMaterial material, int amount) {
        int remaining = amount;
        for (int index = 0; index < stacks.size() && remaining > 0; index++) {
            FoundryMaterialStack current = stacks.get(index);
            if (current.material() != material) {
                continue;
            }
            int taken = Math.min(remaining, current.amount());
            stacks.set(index, new FoundryMaterialStack(current.material(), current.amount() - taken));
            remaining -= taken;
        }
        stacks.removeIf(stack -> stack.amount() <= 0);
    }

    private int selectedRecipeIndex() {
        if (this.level == null || this.selectedRecipe == null) {
            return -1;
        }
        List<RecipeHolder<CrucibleRecipe>> recipes = recipes(this.level);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(this.selectedRecipe)) {
                return index;
            }
        }
        return -1;
    }

    private void setSelectedRecipeByIndex(int index) {
        if (this.level == null || index < 0) {
            this.selectedRecipe = null;
            return;
        }
        List<RecipeHolder<CrucibleRecipe>> recipes = recipes(this.level);
        this.selectedRecipe = index >= recipes.size() ? null : recipes.get(index).id();
    }

    private static int amountIn(List<FoundryMaterialStack> stacks, @Nullable FoundryMaterial material) {
        int sum = 0;
        for (FoundryMaterialStack stack : stacks) {
            if (material == null || stack.material() == material) {
                sum += stack.amount();
            }
        }
        return sum;
    }

    private static int amountInRecipe(List<CrucibleRecipe.MaterialIngredient> stacks, FoundryMaterial material) {
        int sum = 0;
        for (CrucibleRecipe.MaterialIngredient stack : stacks) {
            if (stack.material() == material) {
                sum += stack.amount();
            }
        }
        return sum;
    }

    private int totalAmount(List<FoundryMaterialStack> stacks) {
        return amountIn(stacks, null);
    }

    private static List<RecipeHolder<CrucibleRecipe>> recipes(Level level) {
        return level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CRUCIBLE.get())
                .stream()
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
        }
    }

    private void syncToClient() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        if (this.selectedRecipe != null) {
            tag.putString("Recipe", this.selectedRecipe.toString());
        }
        tag.putInt("Heat", this.heat);
        tag.putInt("Progress", this.progress);
        tag.put("RecipeStack", saveMaterialList(this.recipeStack));
        tag.put("WasteStack", saveMaterialList(this.wasteStack));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.selectedRecipe = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        this.heat = tag.getInt("Heat");
        this.progress = tag.getInt("Progress");
        this.recipeStack.clear();
        this.recipeStack.addAll(loadMaterialList(tag.getList("RecipeStack", Tag.TAG_COMPOUND)));
        this.wasteStack.clear();
        this.wasteStack.addAll(loadMaterialList(tag.getList("WasteStack", Tag.TAG_COMPOUND)));
    }

    private static ListTag saveMaterialList(List<FoundryMaterialStack> stacks) {
        ListTag list = new ListTag();
        for (FoundryMaterialStack stack : stacks) {
            list.add(stack.save());
        }
        return list;
    }

    private static List<FoundryMaterialStack> loadMaterialList(ListTag list) {
        List<FoundryMaterialStack> stacks = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            FoundryMaterialStack stack = FoundryMaterialStack.load(list.getCompound(index));
            if (stack != null && stack.amount() > 0) {
                stacks.add(stack);
            }
        }
        return stacks;
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
}
