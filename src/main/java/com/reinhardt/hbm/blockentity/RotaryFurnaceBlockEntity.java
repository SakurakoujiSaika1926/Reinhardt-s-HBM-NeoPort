package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.menu.RotaryFurnaceMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.recipe.RotaryFurnaceRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RotaryFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_A_SLOT = 0;
    public static final int INPUT_B_SLOT = 1;
    public static final int INPUT_C_SLOT = 2;
    public static final int FUEL_SLOT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_COUNT = 13;
    public static final int ADDITIVE_CAPACITY = 16_000;
    public static final int STEAM_CAPACITY = 12_000;
    public static final int SPENT_STEAM_CAPACITY = 120;
    public static final int MAX_OUTPUT = 16 * 144;
    private static final int SMOKE_BUFFER_CAPACITY = 50;

    private static final int[] INPUT_SLOTS = {INPUT_A_SLOT, INPUT_B_SLOT, INPUT_C_SLOT};
    private static final int[] FUEL_SLOTS = {FUEL_SLOT};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank additiveTank = new HbmFluidTank(HbmFluids.none(), ADDITIVE_CAPACITY);
    private final HbmFluidTank steamTank = new HbmFluidTank(fluid("steam"), STEAM_CAPACITY);
    private final HbmFluidTank spentSteamTank = new HbmFluidTank(fluid("spentsteam"), SPENT_STEAM_CAPACITY);
    private final HbmFluidTank smokeTank = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.SOOT), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokeLeadedTank = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.HEAVYMETAL), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokePoisonTank = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.POISON), SMOKE_BUFFER_CAPACITY);
    private int progress;
    private int burnTime;
    private int maxBurnTime;
    private int steamUsed;
    private boolean working;
    private FoundryMaterialStack output;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> RotaryFurnaceBlockEntity.this.additiveTank.type().oldId();
                case 1 -> RotaryFurnaceBlockEntity.this.additiveTank.amount();
                case 2 -> RotaryFurnaceBlockEntity.this.steamTank.amount();
                case 3 -> RotaryFurnaceBlockEntity.this.spentSteamTank.amount();
                case 4 -> RotaryFurnaceBlockEntity.this.progress;
                case 5 -> RotaryFurnaceBlockEntity.this.burnTime;
                case 6 -> RotaryFurnaceBlockEntity.this.maxBurnTime;
                case 7 -> RotaryFurnaceBlockEntity.this.output == null ? -1 : RotaryFurnaceBlockEntity.this.output.material().id();
                case 8 -> RotaryFurnaceBlockEntity.this.output == null ? 0 : RotaryFurnaceBlockEntity.this.output.amount();
                case 9 -> ADDITIVE_CAPACITY;
                case 10 -> STEAM_CAPACITY;
                case 11 -> SPENT_STEAM_CAPACITY;
                case 12 -> RotaryFurnaceBlockEntity.this.working ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> RotaryFurnaceBlockEntity.this.additiveTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> RotaryFurnaceBlockEntity.this.additiveTank.setAmount(value);
                case 2 -> RotaryFurnaceBlockEntity.this.steamTank.setAmount(value);
                case 3 -> RotaryFurnaceBlockEntity.this.spentSteamTank.setAmount(value);
                case 4 -> RotaryFurnaceBlockEntity.this.progress = value;
                case 5 -> RotaryFurnaceBlockEntity.this.burnTime = value;
                case 6 -> RotaryFurnaceBlockEntity.this.maxBurnTime = value;
                case 8 -> {
                    if (RotaryFurnaceBlockEntity.this.output != null) {
                        RotaryFurnaceBlockEntity.this.output = new FoundryMaterialStack(RotaryFurnaceBlockEntity.this.output.material(), value);
                    }
                }
                case 12 -> RotaryFurnaceBlockEntity.this.working = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public RotaryFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ROTARY_FURNACE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RotaryFurnaceBlockEntity furnace) {
        if (level.isClientSide) {
            furnace.tickClient(level);
        } else {
            furnace.tickServer(level);
        }
    }

    public HbmFluidTank additiveTank() {
        return this.additiveTank;
    }

    public HbmFluidTank steamTank() {
        return this.steamTank;
    }

    public HbmFluidTank spentSteamTank() {
        return this.spentSteamTank;
    }

    @Nullable
    public FoundryMaterialStack output() {
        return this.output;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        return new FluidPortHandler();
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
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
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        sync();
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
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_A_SLOT, INPUT_B_SLOT, INPUT_C_SLOT -> !stack.isEmpty();
            case FUEL_SLOT -> fuelDuration(stack) > 0;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? INPUT_SLOTS : side == Direction.DOWN ? NO_SLOTS : FUEL_SLOTS;
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
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        sync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_rotary_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RotaryFurnaceMenu(containerId, playerInventory, this, this.menuData);
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
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.put("AdditiveTank", this.additiveTank.save());
        tag.put("SteamTank", this.steamTank.save());
        tag.put("SpentSteamTank", this.spentSteamTank.save());
        tag.put("Smoke", this.smokeTank.save());
        tag.put("SmokeLeaded", this.smokeLeadedTank.save());
        tag.put("SmokePoison", this.smokePoisonTank.save());
        tag.putInt("Progress", this.progress);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("SteamUsed", this.steamUsed);
        tag.putBoolean("Working", this.working);
        if (this.output != null) {
            tag.put("Output", this.output.save());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.additiveTank.load(tag.getCompound("AdditiveTank"));
        this.steamTank.load(tag.getCompound("SteamTank"));
        this.spentSteamTank.load(tag.getCompound("SpentSteamTank"));
        this.smokeTank.load(tag.getCompound("Smoke"));
        this.smokeLeadedTank.load(tag.getCompound("SmokeLeaded"));
        this.smokePoisonTank.load(tag.getCompound("SmokePoison"));
        this.progress = tag.getInt("Progress");
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.steamUsed = tag.getInt("SteamUsed");
        this.working = tag.getBoolean("Working");
        this.output = tag.contains("Output") ? FoundryMaterialStack.load(tag.getCompound("Output")) : null;
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
        sendSmoke(level);
        setupAdditiveTank();
        process(level);
        if (level.getGameTime() % 10L == 0L) {
            sync();
        } else {
            setChanged();
        }
    }

    private void tickClient(Level level) {
        if (!this.working) {
            return;
        }
        this.progress++;
        if (level.random.nextInt(5) == 0) {
            level.addParticle(
                    ParticleTypes.FLAME,
                    this.worldPosition.getX() + 0.5D + level.random.nextGaussian() * 0.4D,
                    this.worldPosition.getY() + 0.35D,
                    this.worldPosition.getZ() + 0.5D + level.random.nextGaussian() * 0.4D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private void process(Level level) {
        this.working = false;
        Optional<RecipeHolder<RotaryFurnaceRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            this.progress = 0;
            return;
        }
        RotaryFurnaceRecipe recipe = holder.get().value();
        if (this.burnTime <= 0) {
            burnFuel();
        }
        if (!canProcess(recipe)) {
            this.progress = 0;
            return;
        }

        this.working = true;
        this.progress++;
        HbmPollution.bufferedLegacyPollute(level, this.worldPosition, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND / 10.0D, this::smokeTank);
        this.steamTank.drain(fluid("steam"), recipe.steam(), false);
        this.steamUsed += recipe.steam();
        while (this.steamUsed >= 100 && this.spentSteamTank.amount() < this.spentSteamTank.capacity()) {
            this.steamUsed -= 100;
            this.spentSteamTank.fill(fluid("spentsteam"), 1, false);
        }
        this.burnTime--;

        if (level.getGameTime() % 20L == 0L) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.STEAM_ENGINE_OPERATE.get(), SoundSource.BLOCKS, 0.25F, 0.65F);
        }

        if (this.progress < recipe.duration()) {
            return;
        }

        this.progress = 0;
        consume(recipe);
        FoundryMaterialStack produced = recipe.output().stack();
        this.output = this.output == null
                ? produced
                : new FoundryMaterialStack(this.output.material(), this.output.amount() + produced.amount());
        sync();
    }

    private boolean canProcess(RotaryFurnaceRecipe recipe) {
        if (this.burnTime <= 0 || this.steamTank.amount() < recipe.steam()) {
            return false;
        }
        if (this.spentSteamTank.capacity() - this.spentSteamTank.amount() <= 0 && this.steamUsed >= 100) {
            return false;
        }
        if (recipe.hasFluid()) {
            if (this.additiveTank.type() != recipe.fluid().fluid() || this.additiveTank.amount() < recipe.fluid().amount()) {
                return false;
            }
        }
        if (this.output != null) {
            if (this.output.material() != recipe.output().material()) {
                return false;
            }
            return this.output.amount() + recipe.output().amount() <= MAX_OUTPUT;
        }
        return true;
    }

    private void consume(RotaryFurnaceRecipe recipe) {
        for (RotaryFurnaceRecipe.CountedIngredient ingredient : recipe.inputs()) {
            for (int slot : INPUT_SLOTS) {
                ItemStack stack = this.items.get(slot);
                if (ingredient.matches(stack)) {
                    stack.shrink(ingredient.count());
                    if (stack.isEmpty()) {
                        this.items.set(slot, ItemStack.EMPTY);
                    }
                    break;
                }
            }
        }
        if (recipe.hasFluid()) {
            this.additiveTank.drain(recipe.fluid().fluid(), recipe.fluid().amount(), false);
        }
    }

    private void burnFuel() {
        ItemStack fuel = this.items.get(FUEL_SLOT);
        int duration = fuelDuration(fuel);
        if (duration <= 0) {
            return;
        }
        this.burnTime = Math.max(1, duration / 2);
        this.maxBurnTime = this.burnTime;
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            this.items.set(FUEL_SLOT, ItemStack.EMPTY);
        }
    }

    private void setupAdditiveTank() {
        Optional<RecipeHolder<RotaryFurnaceRecipe>> holder = currentRecipe();
        if (holder.isPresent() && holder.get().value().hasFluid()) {
            RotaryFurnaceRecipe recipe = holder.get().value();
            if (this.additiveTank.amount() == 0 || this.additiveTank.type() == recipe.fluid().fluid()) {
                this.additiveTank.setType(recipe.fluid().fluid());
            }
        } else if (this.additiveTank.amount() == 0) {
            this.additiveTank.clear();
        }
        this.steamTank.setType(fluid("steam"));
        this.spentSteamTank.setType(fluid("spentsteam"));
    }

    private Optional<RecipeHolder<RotaryFurnaceRecipe>> currentRecipe() {
        if (this.level == null) {
            return Optional.empty();
        }
        RotaryFurnaceRecipe.Input input = new RotaryFurnaceRecipe.Input(
                this.items.get(INPUT_A_SLOT),
                this.items.get(INPUT_B_SLOT),
                this.items.get(INPUT_C_SLOT)
        );
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ROTARY_FURNACE.get())
                .stream()
                .filter(holder -> holder.value().matches(input, this.level))
                .findFirst();
    }

    public static int fuelDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int base = stack.getBurnTime(null);
        return base <= 0 ? 0 : Math.max(1, base);
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void sendSmoke(Level level) {
        Direction facing = getFacing();
        Direction rot = facing.getClockWise();
        BlockPos target = this.worldPosition.relative(rot).above(5);
        if (HbmPollution.sendSmoke(level, this.worldPosition, target, Direction.DOWN, this.smokeTank, this.smokeLeadedTank, this.smokePoisonTank)) {
            setChanged();
        }
    }

    private Direction getFacing() {
        BlockState state = this.getBlockState();
        return state.hasProperty(com.reinhardt.hbm.block.LargeMachineBlock.FACING)
                ? state.getValue(com.reinhardt.hbm.block.LargeMachineBlock.FACING)
                : Direction.SOUTH;
    }

    private HbmFluidTank smokeTank(HbmPollutionType type) {
        return switch (type) {
            case HEAVYMETAL -> this.smokeLeadedTank;
            case POISON -> this.smokePoisonTank;
            case SOOT, FALLOUT -> this.smokeTank;
        };
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private final class FluidPortHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return switch (tankIndex) {
                case 0 -> additiveTank.getFluidInTank(0);
                case 1 -> steamTank.getFluidInTank(0);
                case 2 -> spentSteamTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return switch (tankIndex) {
                case 0 -> additiveTank.capacity();
                case 1 -> steamTank.capacity();
                case 2 -> spentSteamTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return switch (tankIndex) {
                case 0 -> !fluid.isNone() && fluid != fluid("steam") && fluid != fluid("spentsteam");
                case 1 -> fluid == fluid("steam");
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank tank = fluid == fluid("steam") ? steamTank : additiveTank;
            int filled = tank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != spentSteamTank.type()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = spentSteamTank.drain(fluid, resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            HbmFluidDefinition fluid = spentSteamTank.type();
            HbmFluidStack drained = spentSteamTank.drain(fluid, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }
    }
}
