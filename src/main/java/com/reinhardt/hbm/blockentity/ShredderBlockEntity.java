package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PowerMachineBlock;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BladesItem;
import com.reinhardt.hbm.menu.ShredderMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ShredderRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class ShredderBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_START = 0;
    public static final int INPUT_END = 9;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_END = 27;
    public static final int LEFT_BLADE_SLOT = 27;
    public static final int RIGHT_BLADE_SLOT = 28;
    public static final int BATTERY_SLOT = 29;
    public static final int SLOT_COUNT = 30;
    public static final int DATA_COUNT = 6;
    public static final int PROCESSING_SPEED = 60;
    public static final long ENERGY_CAPACITY = 10_000L;
    public static final long DEMAND_PER_TICK = 5L;

    private static final int[] ALL_SLOTS = createSlots(INPUT_START, SLOT_COUNT);

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private long energyStored;
    private long lastInput;
    private int progress;
    private int soundCycle;
    private int completedCycles;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) ShredderBlockEntity.this.energyStored;
                case 1 -> (int) ShredderBlockEntity.this.lastInput;
                case 2 -> ShredderBlockEntity.this.progress;
                case 3 -> ShredderBlockEntity.this.getGearLeft();
                case 4 -> ShredderBlockEntity.this.getGearRight();
                case 5 -> ShredderBlockEntity.this.completedCycles;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ShredderBlockEntity.this.energyStored = value;
                case 1 -> ShredderBlockEntity.this.lastInput = value;
                case 2 -> ShredderBlockEntity.this.progress = value;
                case 5 -> ShredderBlockEntity.this.completedCycles = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ShredderBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SHREDDER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShredderBlockEntity blockEntity) {
        PowerNetworkManager.tickFromEndpoint(level, blockEntity);
        blockEntity.tickWork();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (this.energyStored >= ENERGY_CAPACITY) {
            return 0L;
        }
        return ENERGY_CAPACITY - this.energyStored;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.energyStored = Math.min(ENERGY_CAPACITY, this.energyStored + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int percent = this.progress * 100 / PROCESSING_SPEED;
        return Component.translatable(
                "message.reinhardtshbm.power.shredder",
                this.lastInput,
                ENERGY_CAPACITY,
                this.energyStored,
                ENERGY_CAPACITY,
                percent,
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

        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }

        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        if (isInputSlot(slot)) {
            this.progress = 0;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isInputSlot(slot)) {
            return canAcceptInput(stack);
        }
        if (slot == LEFT_BLADE_SLOT || slot == RIGHT_BLADE_SLOT) {
            return stack.getItem() instanceof BladesItem;
        }
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
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
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if ((slot >= OUTPUT_START && slot != LEFT_BLADE_SLOT && slot != RIGHT_BLADE_SLOT) || !canPlaceItem(slot, stack)) {
            return false;
        }

        ItemStack existing = this.items.get(slot);
        if (existing.isEmpty()) {
            return true;
        }

        int size = existing.getCount();
        for (int inputSlot = INPUT_START; inputSlot < INPUT_END; inputSlot++) {
            ItemStack input = this.items.get(inputSlot);
            if (input.isEmpty()) {
                return false;
            }
            if (ItemStack.isSameItemSameComponents(input, stack) && input.getCount() < size) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (slot >= OUTPUT_START && slot < OUTPUT_END) {
            return true;
        }
        if (slot == LEFT_BLADE_SLOT || slot == RIGHT_BLADE_SLOT) {
            return BladesItem.gearState(stack) == 3;
        }
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.shredder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ShredderMenu(containerId, playerInventory, this, this.menuData);
    }

    public int getGearLeft() {
        return BladesItem.gearState(this.items.get(LEFT_BLADE_SLOT));
    }

    public int getGearRight() {
        return BladesItem.gearState(this.items.get(RIGHT_BLADE_SLOT));
    }

    public static boolean canAcceptInput(ItemStack stack) {
        return !stack.isEmpty() && !(stack.getItem() instanceof BladesItem);
    }

    public ContainerData getMenuData() {
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
        tag.putLong("EnergyStored", this.energyStored);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Progress", this.progress);
        tag.putInt("CompletedCycles", this.completedCycles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.energyStored = tag.getLong("EnergyStored");
        this.lastInput = tag.getLong("LastInput");
        this.progress = tag.getInt("Progress");
        this.completedCycles = tag.getInt("CompletedCycles");
    }

    private void tickWork() {
        if (this.progress == 0) {
            this.soundCycle = 0;
        }

        if (!canProcess()) {
            this.progress = 0;
            setLit(false);
            chargeFromBattery();
            setChanged();
            return;
        }

        if (this.energyStored < DEMAND_PER_TICK) {
            this.progress = 0;
            setLit(false);
            chargeFromBattery();
            setChanged();
            return;
        }

        this.energyStored -= DEMAND_PER_TICK;
        this.progress++;
        playWorkingSound();
        setLit(true);

        if (this.progress >= PROCESSING_SPEED) {
            damageBlades();
            processItems();
            this.progress = 0;
            this.completedCycles++;
        }
        chargeFromBattery();
        setChanged();
    }

    private void playWorkingSound() {
        if (this.level == null) {
            return;
        }

        if (this.soundCycle == 0) {
            this.level.playSound(null, this.worldPosition, SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 1.0F, 0.75F);
        }

        this.soundCycle++;
        if (this.soundCycle >= 50) {
            this.soundCycle = 0;
        }
    }

    private boolean canProcess() {
        if (!BladesItem.isUsableBlade(this.items.get(LEFT_BLADE_SLOT)) || !BladesItem.isUsableBlade(this.items.get(RIGHT_BLADE_SLOT))) {
            return false;
        }

        for (int slot = INPUT_START; slot < INPUT_END; slot++) {
            ItemStack input = this.items.get(slot);
            if (!input.isEmpty() && canProcessInput(input)) {
                return true;
            }
        }
        return false;
    }

    private void processItems() {
        for (int inputSlot = INPUT_START; inputSlot < INPUT_END; inputSlot++) {
            ItemStack input = this.items.get(inputSlot);
            if (input.isEmpty() || !hasSpace(input)) {
                continue;
            }

            Optional<RecipeHolder<ShredderRecipe>> holder = getRecipe(input);
            int inputCount = 1;
            ItemStack result;
            if (holder.isPresent()) {
                ShredderRecipe recipe = holder.get().value();
                inputCount = recipe.inputCount();
                result = recipe.assemble(new SingleRecipeInput(input), this.level.registryAccess());
            } else {
                if (hasExplicitIngredientMatch(input)) {
                    continue;
                }

                // Preserve the 1.7.10 ore-dictionary fallback for compatible
                // materials which do not have an explicit JSON recipe.
                result = dynamicTagResult(input)
                        .orElseGet(() -> new ItemStack(com.reinhardt.hbm.registry.HbmItems.SCRAP.get()));
            }
            if (result.isEmpty()) {
                continue;
            }

            insertOutput(result);
            input.shrink(inputCount);
            if (input.isEmpty()) {
                this.items.set(inputSlot, ItemStack.EMPTY);
            }
        }
    }

    private boolean canProcessInput(ItemStack input) {
        return !input.isEmpty() && hasSpace(input);
    }

    private boolean hasSpace(ItemStack input) {
        ItemStack result = getResult(input);
        if (result.isEmpty()) {
            return false;
        }

        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack output = this.items.get(slot);
            if (output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result)
                    && output.getCount() + result.getCount() <= result.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private void insertOutput(ItemStack result) {
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack output = this.items.get(slot);
            if (!output.isEmpty() && ItemStack.isSameItemSameComponents(output, result)
                    && output.getCount() + result.getCount() <= result.getMaxStackSize()) {
                output.grow(result.getCount());
                return;
            }
        }

        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, result.copy());
                return;
            }
        }
    }

    private void damageBlades() {
        BladesItem.damageBlade(this.items.get(LEFT_BLADE_SLOT));
        BladesItem.damageBlade(this.items.get(RIGHT_BLADE_SLOT));
    }

    private ItemStack getResult(ItemStack stack) {
        if (this.level == null) {
            return ItemStack.EMPTY;
        }
        Optional<RecipeHolder<ShredderRecipe>> recipe = getRecipe(stack);
        if (recipe.isPresent()) {
            return recipe.get().value().assemble(new SingleRecipeInput(stack), this.level.registryAccess());
        }

        // An explicit recipe can require a batch (for example, two coarse ores
        // for three powders). Do not fall through to the legacy dynamic-tag
        // fallback and run a cycle that produces scrap while the batch is
        // incomplete.
        if (hasExplicitIngredientMatch(stack)) {
            return ItemStack.EMPTY;
        }

        return dynamicTagResult(stack)
                .orElseGet(() -> new ItemStack(com.reinhardt.hbm.registry.HbmItems.SCRAP.get()));
    }

    private boolean hasExplicitIngredientMatch(ItemStack stack) {
        return this.level != null
                && this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.SHREDDER.get())
                .stream()
                .anyMatch(holder -> holder.value().ingredient().test(stack));
    }

    private Optional<RecipeHolder<ShredderRecipe>> getRecipe(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return java.util.Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(HbmRecipeTypes.SHREDDER.get(), new SingleRecipeInput(stack), this.level);
    }

    private void setLit(boolean lit) {
        if (this.level == null) {
            return;
        }

        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof PowerMachineBlock && state.getValue(PowerMachineBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(PowerMachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isInputSlot(int slot) {
        return slot >= INPUT_START && slot < INPUT_END;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    public static boolean isBattery(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (BatteryPackItem.isBattery(stack)) {
            return true;
        }

        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.startsWith("battery_")
                || path.startsWith("armor_battery")
                || path.equals("hev_battery")
                || path.endsWith("_battery");
    }

    private static int[] createSlots(int start, int count) {
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) {
            slots[i] = start + i;
        }
        return slots;
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

    private void chargeFromBattery() {
        this.energyStored = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.energyStored, ENERGY_CAPACITY);
    }

    /**
     * Direct translation of ShredderRecipes.registerPost(): 1.7.10 generated
     * these recipes from every ore-dictionary material after all mods loaded.
     */
    private static Optional<ItemStack> dynamicTagResult(ItemStack stack) {
        return stack.getTags()
                .map(TagKey::location)
                .filter(tag -> tag.getNamespace().equals("c") || tag.getNamespace().equals("forge"))
                .map(ShredderBlockEntity::dynamicTagResult)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<ItemStack> dynamicTagResult(ResourceLocation tag) {
        String[] path = tag.getPath().split("/", 2);
        if (path.length != 2 || path[1].isBlank()) {
            return Optional.empty();
        }

        String category = path[0];
        String material = canonicalMaterialName(path[1]);
        return switch (category) {
            case "ingots", "plates", "gems", "crystals" -> dustFor(tag.getNamespace(), material, 1);
            case "ores" -> dustFor(tag.getNamespace(), material, 2);
            case "storage_blocks" -> blockDustFor(tag.getNamespace(), material);
            case "tiny_dusts" -> Optional.of(new ItemStack(com.reinhardt.hbm.registry.HbmItems.DUST_TINY.get()));
            case "dusts" -> Optional.of(new ItemStack(com.reinhardt.hbm.registry.HbmItems.DUST.get()));
            default -> Optional.empty();
        };
    }

    private static Optional<ItemStack> blockDustFor(String namespace, String material) {
        Optional<ItemStack> dust = dustFor(namespace, material, 1);
        if (dust.isEmpty()) {
            return Optional.empty();
        }

        boolean hasIngotOrGem = itemTagHasEntries(namespace, "ingots/" + material)
                || itemTagHasEntries(namespace, "gems/" + material);
        ItemStack result = dust.get();
        result.setCount(hasIngotOrGem ? 9 : 4);
        return Optional.of(result);
    }

    private static Optional<ItemStack> dustFor(String namespace, String material, int count) {
        TagKey<net.minecraft.world.item.Item> dustTag = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(namespace, "dusts/" + material)
        );
        return BuiltInRegistries.ITEM.getTag(dustTag)
                .flatMap(entries -> entries.stream().findFirst())
                .map(entry -> new ItemStack(entry.value(), count));
    }

    private static boolean itemTagHasEntries(String namespace, String path) {
        TagKey<net.minecraft.world.item.Item> tag = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(namespace, path)
        );
        return BuiltInRegistries.ITEM.getTag(tag)
                .map(entries -> entries.iterator().hasNext())
                .orElse(false);
    }

    private static String canonicalMaterialName(String material) {
        return material.equals("aluminum") ? "aluminium" : material;
    }
}
