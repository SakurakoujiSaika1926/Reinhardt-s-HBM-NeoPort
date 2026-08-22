package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.menu.SilexMenu;
import com.reinhardt.hbm.recipe.SilexRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import com.reinhardt.hbm.util.Wavelength;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class SilexBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FLUID_IDENTIFIER_SLOT = 1;
    public static final int FLUID_INPUT_SLOT = 2;
    public static final int FLUID_OUTPUT_SLOT = 3;
    public static final int CURRENT_OUTPUT_SLOT = 4;
    public static final int QUEUE_START = 5;
    public static final int QUEUE_END = 11;
    public static final int SLOT_COUNT = 11;
    public static final int DATA_COUNT = 11;
    public static final int TANK_CAPACITY = 16_000;
    public static final int MAX_FILL = 16_000;
    public static final int PROCESS_TIME = 100;
    public static final int PRIME = 137;

    private static final int[] ACCESSIBLE_SLOTS = {INPUT_SLOT, QUEUE_START, QUEUE_START + 1, QUEUE_START + 2, QUEUE_START + 3, QUEUE_START + 4, QUEUE_START + 5};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank tank = new HbmFluidTank(peroxide(), TANK_CAPACITY);
    private ItemStack currentDisplay = ItemStack.EMPTY;
    private int currentFill;
    private int progress;
    private int recipeIndex;
    private Wavelength mode = Wavelength.NULL;
    private int loadDelay;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SilexBlockEntity.this.tank.type().oldId();
                case 1 -> SilexBlockEntity.this.tank.amount();
                case 2 -> SilexBlockEntity.this.currentFill;
                case 3 -> SilexBlockEntity.this.progress;
                case 4 -> SilexBlockEntity.this.mode.ordinal();
                case 5 -> ItemStack.isSameItemSameComponents(SilexBlockEntity.this.currentDisplay, ItemStack.EMPTY)
                        ? -1
                        : net.minecraft.world.item.Item.getId(SilexBlockEntity.this.currentDisplay.getItem());
                case 6 -> SilexBlockEntity.this.currentDisplay.isEmpty() ? 0 : SilexBlockEntity.this.currentDisplay.getDamageValue();
                case 7 -> SilexBlockEntity.this.recipeIndex;
                case 8 -> PROCESS_TIME;
                case 9 -> TANK_CAPACITY;
                case 10 -> MAX_FILL;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> SilexBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> SilexBlockEntity.this.tank.setAmount(value);
                case 2 -> SilexBlockEntity.this.currentFill = value;
                case 3 -> SilexBlockEntity.this.progress = value;
                case 4 -> {
                    Wavelength[] values = Wavelength.values();
                    SilexBlockEntity.this.mode = value >= 0 && value < values.length ? values[value] : Wavelength.NULL;
                }
                case 5 -> {
                    if (value < 0) {
                        SilexBlockEntity.this.currentDisplay = ItemStack.EMPTY;
                    } else {
                        net.minecraft.world.item.Item item = net.minecraft.world.item.Item.byId(value);
                        SilexBlockEntity.this.currentDisplay = item == null ? ItemStack.EMPTY : new ItemStack(item);
                    }
                }
                case 6 -> {
                    if (!SilexBlockEntity.this.currentDisplay.isEmpty()) {
                        SilexBlockEntity.this.currentDisplay.setDamageValue(value);
                    }
                }
                case 7 -> SilexBlockEntity.this.recipeIndex = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SilexBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SILEX.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SilexBlockEntity silex) {
        if (level.isClientSide) {
            return;
        }
        silex.tickServer(level);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public HbmFluidTank tank() {
        return this.tank;
    }

    public ItemStack currentDisplay() {
        return this.currentDisplay;
    }

    public int currentFill() {
        return this.currentFill;
    }

    public int progress() {
        return this.progress;
    }

    public Wavelength mode() {
        return this.mode;
    }

    public void setWavelength(Wavelength wavelength) {
        this.mode = wavelength == null ? Wavelength.NULL : wavelength;
        setChangedAndSync(true);
    }

    public void clearCurrent() {
        this.currentFill = 0;
        this.currentDisplay = ItemStack.EMPTY;
        this.progress = 0;
        setChangedAndSync(true);
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new TankFluidHandler();
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
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            setChangedAndSync(false);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> hasItemRecipe(stack);
            case FLUID_IDENTIFIER_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
            case FLUID_INPUT_SLOT -> isDrainableContainer(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= QUEUE_START;
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
        this.currentDisplay = ItemStack.EMPTY;
        this.currentFill = 0;
        this.progress = 0;
        setChangedAndSync(true);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_silex");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SilexMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            drop(level, pos, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.put("Tank", this.tank.save());
        tag.put("CurrentDisplay", this.currentDisplay.saveOptional(registries));
        tag.putInt("CurrentFill", this.currentFill);
        tag.putInt("Progress", this.progress);
        tag.putInt("RecipeIndex", this.recipeIndex);
        tag.putString("Mode", this.mode.getSerializedName());
        tag.putInt("LoadDelay", this.loadDelay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.tank.load(tag.getCompound("Tank"));
        if (this.tank.type().isNone()) {
            this.tank.setType(peroxide());
        }
        this.currentDisplay = ItemStack.parseOptional(registries, tag.getCompound("CurrentDisplay"));
        this.currentFill = tag.getInt("CurrentFill");
        this.progress = tag.getInt("Progress");
        this.recipeIndex = tag.getInt("RecipeIndex");
        this.mode = Wavelength.byName(tag.getString("Mode"));
        this.loadDelay = tag.getInt("LoadDelay");
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
        applyFluidIdentifierSlot();
        boolean tankChanged = drainContainerIntoTank();
        loadFluid();
        if (!process(level)) {
            this.progress = 0;
        }
        dequeue();
        if (this.currentFill <= 0) {
            this.currentDisplay = ItemStack.EMPTY;
        }
        setChanged();
        if (tankChanged) {
            setChangedAndSync(true);
        }
        this.mode = Wavelength.NULL;
    }

    private void loadFluid() {
        Optional<HbmFluidDefinition> inputFluid = fluidBackedCurrent();
        if (inputFluid.isPresent()) {
            fillCurrentFromFluid(inputFluid.get());
            return;
        }

        this.loadDelay++;
        if (this.loadDelay > 20) {
            this.loadDelay = 0;
        }
        if (this.loadDelay != 0) {
            return;
        }
        ItemStack stack = this.items.get(INPUT_SLOT);
        if (stack.isEmpty() || this.tank.type() != peroxide()) {
            return;
        }
        Optional<RecipeHolder<SilexRecipe>> recipeHolder = recipeForItem(stack);
        if (recipeHolder.isEmpty()) {
            return;
        }
        SilexRecipe recipe = recipeHolder.get().value();
        if (recipe.fluidProduced() <= 0) {
            return;
        }
        ItemStack singular = stack.copyWithCount(1);
        if (!this.currentDisplay.isEmpty() && !ItemStack.isSameItemSameComponents(this.currentDisplay, singular)) {
            return;
        }
        if (recipe.fluidProduced() > MAX_FILL - this.currentFill || recipe.fluidProduced() > this.tank.amount()) {
            return;
        }
        this.currentFill += recipe.fluidProduced();
        this.currentDisplay = singular;
        this.tank.drain(this.tank.type(), recipe.fluidProduced(), false);
        stack.shrink(1);
        if (stack.isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        setChangedAndSync(true);
    }

    private Optional<HbmFluidDefinition> fluidBackedCurrent() {
        if (this.tank.type().isNone()) {
            return Optional.empty();
        }
        String tankName = this.tank.type().name();
        if (tankName.equals("uf6") || tankName.equals("puf6") || tankName.equals("death")) {
            return Optional.of(this.tank.type());
        }
        if (this.level == null) {
            return Optional.empty();
        }
        boolean recipeUsesFluid = this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.SILEX.get())
                .stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.acceptsFluid(this.tank.type()));
        return recipeUsesFluid ? Optional.of(this.tank.type()) : Optional.empty();
    }

    private void fillCurrentFromFluid(HbmFluidDefinition fluid) {
        ItemStack icon = FluidIconItem.forFluid(fluid, 1, 0);
        if (this.currentFill == 0) {
            this.currentDisplay = icon;
        }
        if (!this.currentDisplay.isEmpty() && ItemStack.isSameItemSameComponents(this.currentDisplay, icon)) {
            int toFill = Math.min(50, Math.min(MAX_FILL - this.currentFill, this.tank.amount()));
            if (toFill > 0) {
                this.currentFill += toFill;
                this.tank.drain(fluid, toFill, false);
                setChangedAndSync(true);
            }
        }
    }

    private boolean process(Level level) {
        if (this.currentDisplay.isEmpty() || this.currentFill <= 0) {
            return false;
        }
        Optional<RecipeHolder<SilexRecipe>> holder = recipeForCurrent(this.currentDisplay);
        if (holder.isEmpty()) {
            return false;
        }
        SilexRecipe recipe = holder.get().value();
        if (recipe.wavelength().ordinal() > this.mode.ordinal()) {
            return false;
        }
        if (this.currentFill < recipe.fluidConsumed()) {
            return false;
        }
        if (!this.items.get(CURRENT_OUTPUT_SLOT).isEmpty()) {
            return false;
        }

        int progressSpeed = Math.max(1, (int) Math.pow(2, this.mode.ordinal() - recipe.wavelength().ordinal() + 1) / 2);
        this.progress += progressSpeed;

        if (this.progress >= PROCESS_TIME) {
            this.currentFill -= recipe.fluidConsumed();
            ItemStack result = recipe.deterministicOutput(this.recipeIndex);
            this.items.set(CURRENT_OUTPUT_SLOT, result);
            this.progress = 0;
            this.recipeIndex += PRIME;
            setChangedAndSync(true);
        }
        return true;
    }

    private void dequeue() {
        ItemStack currentOutput = this.items.get(CURRENT_OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            return;
        }
        for (int slot = QUEUE_START; slot < QUEUE_END; slot++) {
            ItemStack queued = this.items.get(slot);
            if (!queued.isEmpty() && ItemStack.isSameItemSameComponents(queued, currentOutput) && queued.getCount() < queued.getMaxStackSize()) {
                queued.grow(1);
                currentOutput.shrink(1);
                if (currentOutput.isEmpty()) {
                    this.items.set(CURRENT_OUTPUT_SLOT, ItemStack.EMPTY);
                }
                setChangedAndSync(true);
                return;
            }
        }
        for (int slot = QUEUE_START; slot < QUEUE_END; slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, currentOutput.copy());
                this.items.set(CURRENT_OUTPUT_SLOT, ItemStack.EMPTY);
                setChangedAndSync(true);
                return;
            }
        }
    }

    private Optional<RecipeHolder<SilexRecipe>> recipeForCurrent(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(HbmRecipeTypes.SILEX.get(), new SilexRecipe.Input(stack), this.level);
    }

    private Optional<RecipeHolder<SilexRecipe>> recipeForItem(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SILEX.get()).stream()
                .filter(holder -> holder.value().ingredient().isPresent() && holder.value().ingredient().get().test(stack))
                .findFirst();
    }

    private boolean hasItemRecipe(ItemStack stack) {
        return recipeForItem(stack).isPresent();
    }

    private void applyFluidIdentifierSlot() {
        ItemStack stack = this.items.get(FLUID_IDENTIFIER_SLOT);
        if (stack.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition fluid = FluidIdentifierItem.primary(stack);
            if (!fluid.isNone() && fluid != this.tank.type()) {
                this.tank.setType(fluid);
                setChangedAndSync(true);
            }
        }
    }

    private boolean drainContainerIntoTank() {
        ItemStack input = this.items.get(FLUID_INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        boolean changed = HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.tank,
                fluid -> fluid == this.tank.type(),
                output -> canPlaceOutput(FLUID_OUTPUT_SLOT, output),
                output -> placeOutput(FLUID_OUTPUT_SLOT, output)
        );
        if (input.isEmpty()) {
            this.items.set(FLUID_INPUT_SLOT, ItemStack.EMPTY);
        }
        return changed;
    }

    private boolean isDrainableContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof InfiniteFluidContainerItem infinite) {
            return infinite.sourceFor(this.tank) == this.tank.type() && !this.tank.type().isNone();
        }
        if (stack.getItem() instanceof HbmFluidContainerItem item && item.isFilledContainer()) {
            return HbmFluidContainerItem.fluid(stack) == this.tank.type()
                    && canPlaceOutput(FLUID_OUTPUT_SLOT, item.getCraftingRemainingItem(stack));
        }
        return FluidUtil.getFluidContained(stack)
                .flatMap(contained -> HbmFluids.fromNeoFluid(contained.getFluid()))
                .filter(fluid -> fluid == this.tank.type())
                .isPresent();
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (sync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static HbmFluidDefinition peroxide() {
        return HbmFluids.byName("peroxide").orElse(HbmFluids.none());
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
    }

    private final class TankFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tankIndex == 0 ? tank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tankIndex == 0 ? tank.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            if (tankIndex != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return !fluid.isNone() && fluid == tank.type() && tank.pressure() == 0;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone() || fluid != tank.type()) {
                return 0;
            }
            int filled = tank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                setChangedAndSync(true);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
