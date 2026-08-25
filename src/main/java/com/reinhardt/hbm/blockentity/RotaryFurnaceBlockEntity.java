package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.foundry.CrucibleUtil;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.RotaryFurnaceMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.recipe.RotaryFurnaceRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class RotaryFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_A_SLOT = 0;
    public static final int INPUT_B_SLOT = 1;
    public static final int INPUT_C_SLOT = 2;
    public static final int FLUID_IDENTIFIER_SLOT = 3;
    public static final int FUEL_SLOT = 4;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 13;
    public static final int ADDITIVE_CAPACITY = 16_000;
    public static final int STEAM_CAPACITY = 12_000;
    public static final int SPENT_STEAM_CAPACITY = 120;
    public static final int MAX_OUTPUT = 16 * 144;
    private static final int SMOKE_BUFFER_CAPACITY = 50;
    private static final int PROGRESS_SYNC_SCALE = 10_000;

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
    private float progress;
    private int burnTime;
    private int maxBurnTime;
    private int steamUsed;
    private double burnHeat = 1.0D;
    private boolean working;
    private boolean venting;
    private FoundryMaterialStack output;
    private int clientAnimation;
    private int lastClientAnimation;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> RotaryFurnaceBlockEntity.this.additiveTank.type().oldId();
                case 1 -> RotaryFurnaceBlockEntity.this.additiveTank.amount();
                case 2 -> RotaryFurnaceBlockEntity.this.steamTank.amount();
                case 3 -> RotaryFurnaceBlockEntity.this.spentSteamTank.amount();
                case 4 -> Math.round(RotaryFurnaceBlockEntity.this.progress * PROGRESS_SYNC_SCALE);
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
                case 4 -> RotaryFurnaceBlockEntity.this.progress = value / (float) PROGRESS_SYNC_SCALE;
                case 5 -> RotaryFurnaceBlockEntity.this.burnTime = value;
                case 6 -> RotaryFurnaceBlockEntity.this.maxBurnTime = value;
                case 7 -> RotaryFurnaceBlockEntity.this.output = FoundryMaterial.byId(value)
                        .map(material -> new FoundryMaterialStack(material,
                                RotaryFurnaceBlockEntity.this.output == null ? 0 : RotaryFurnaceBlockEntity.this.output.amount()))
                        .orElse(null);
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
        FluidPort port = fluidPort(queriedPos, side);
        return port == null ? null : new FluidPortHandler(port);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public int[] getSlotsForAccessor(BlockPos accessorPos, @Nullable Direction side) {
        Direction facing = getFacing();
        Direction rotation = LegacyMachineGeometry.forgeRotateUp(facing);
        if (side == facing.getOpposite()) {
            if (accessorPos.equals(this.worldPosition.relative(facing.getOpposite()).relative(rotation.getOpposite(), 2))) {
                return new int[]{INPUT_A_SLOT};
            }
            if (accessorPos.equals(this.worldPosition.relative(facing.getOpposite()).relative(rotation.getOpposite()))) {
                return new int[]{INPUT_B_SLOT};
            }
            if (accessorPos.equals(this.worldPosition.relative(facing.getOpposite()))) {
                return new int[]{INPUT_C_SLOT};
            }
        }
        if (side == facing
                && accessorPos.equals(this.worldPosition.relative(facing).relative(rotation.getOpposite()))) {
            return new int[]{FUEL_SLOT};
        }
        return NO_SLOTS;
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
            case FLUID_IDENTIFIER_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
            case FUEL_SLOT -> fuelDuration(stack) > 0;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
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
        tag.putFloat("Progress", this.progress);
        tag.putInt("BurnTime", this.burnTime);
        tag.putDouble("BurnHeat", this.burnHeat);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("SteamUsed", this.steamUsed);
        tag.putBoolean("Working", this.working);
        tag.putBoolean("Venting", this.venting);
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
        this.progress = tag.getFloat("Progress");
        this.burnTime = tag.getInt("BurnTime");
        this.burnHeat = tag.contains("BurnHeat") ? tag.getDouble("BurnHeat") : 1.0D;
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.steamUsed = tag.getInt("SteamUsed");
        this.working = tag.getBoolean("Working");
        this.venting = tag.getBoolean("Venting");
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
        pourMoltenOutput(level);
        setupAdditiveTank();
        process(level);
        if (level.getGameTime() % 10L == 0L) {
            sync();
        } else {
            setChanged();
        }
    }

    private void tickClient(Level level) {
        this.lastClientAnimation = this.clientAnimation;
        if (this.working) {
            this.clientAnimation += (int) Math.max(this.burnHeat, 1.0D);
        }
        if (this.burnTime > 0) {
            Direction facing = getFacing();
            Direction rotation = LegacyMachineGeometry.forgeRotateDown(facing);
            level.addParticle(
                    ParticleTypes.FLAME,
                    this.worldPosition.getX() + 0.5D + facing.getStepX() * 0.5D + rotation.getStepX()
                            + level.random.nextGaussian() * 0.25D,
                    this.worldPosition.getY() + 0.375D,
                    this.worldPosition.getZ() + 0.5D + facing.getStepZ() * 0.5D + rotation.getStepZ()
                            + level.random.nextGaussian() * 0.25D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
        if (this.venting && level.getGameTime() % 2L == 0L) {
            Direction rotation = LegacyMachineGeometry.forgeRotateDown(getFacing());
            double soot = 0x20 / 255.0D;
            level.addParticle(
                    HbmParticleTypes.ROTARY_FURNACE_TOWER.get(),
                    this.worldPosition.getX() + 0.5D + rotation.getStepX(),
                    this.worldPosition.getY() + 5.0D,
                    this.worldPosition.getZ() + 0.5D + rotation.getStepZ(),
                    soot,
                    soot,
                    soot
            );
        }
    }

    private void process(Level level) {
        this.working = false;
        this.venting = false;
        Optional<RecipeHolder<RotaryFurnaceRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            this.progress = 0;
            return;
        }
        RotaryFurnaceRecipe recipe = holder.get().value();
        if (this.burnTime <= 0) {
            burnFuel();
        }
        float steamUseMultiplier = steamUseMultiplier();
        if (!canProcess(recipe, steamUseMultiplier)) {
            this.progress = 0;
            condenseSpentSteam();
            return;
        }

        this.working = true;
        this.progress += (float) Math.max(this.burnHeat, 1.0D) / recipe.duration();
        int smokeAmount = (int) Math.ceil(HbmPollutionConstants.SOOT_PER_SECOND / 10.0D * 100.0D);
        this.venting = this.smokeTank.amount() + smokeAmount > this.smokeTank.capacity();
        HbmPollution.bufferedLegacyPollute(level, this.worldPosition, HbmPollutionType.SOOT,
                HbmPollutionConstants.SOOT_PER_SECOND / 10.0D, this::smokeTank);
        int steamConsumed = (int) (recipe.steam() * steamUseMultiplier);
        this.steamTank.drain(fluid("steam"), steamConsumed, false);
        this.steamUsed += steamConsumed;
        this.burnTime--;

        if (this.progress >= 1.0F) {
            this.progress -= 1.0F;
            consume(recipe);
            FoundryMaterialStack produced = recipe.output().stack();
            this.output = this.output == null
                    ? produced
                    : new FoundryMaterialStack(this.output.material(), this.output.amount() + produced.amount());
        }
        condenseSpentSteam();
    }

    /**
     * Direct equivalent of TileEntityMachineRotaryFurnace#updateEntity's
     * CrucibleUtil.pourSingleStack call. The modern helper mutates a list,
     * while the old rotary furnace holds exactly one material stack.
     */
    private void pourMoltenOutput(Level level) {
        if (this.output == null || this.output.amount() <= 0) {
            return;
        }
        Direction rotation = LegacyMachineGeometry.forgeRotateDown(getFacing());
        List<FoundryMaterialStack> stack = new ArrayList<>(List.of(this.output));
        FoundryMaterialStack poured = CrucibleUtil.pourFullStack(
                level,
                this.worldPosition.getX() + 0.5D + rotation.getStepX() * 2.875D,
                this.worldPosition.getY() + 1.25D,
                this.worldPosition.getZ() + 0.5D + rotation.getStepZ() * 2.875D,
                6.0D,
                true,
                stack,
                FoundryShape.INGOT.q(1),
                material -> true
        );
        if (poured != null) {
            this.output = stack.isEmpty() ? null : stack.getFirst();
            setChanged();
        }
    }

    private boolean canProcess(RotaryFurnaceRecipe recipe, float steamUseMultiplier) {
        if (this.burnTime <= 0 || this.steamTank.amount() < recipe.steam() * steamUseMultiplier) {
            return false;
        }
        if (this.spentSteamTank.capacity() - this.spentSteamTank.amount() < recipe.steam() * steamUseMultiplier / 100.0F) {
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

    private void condenseSpentSteam() {
        if (this.steamUsed < 100) {
            return;
        }
        int steamReturn = this.steamUsed / 100;
        int returned = Math.min(steamReturn, this.spentSteamTank.capacity() - this.spentSteamTank.amount());
        if (returned > 0) {
            this.steamUsed -= returned * 100;
            this.spentSteamTank.fill(fluid("spentsteam"), returned, false);
        }
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
        this.burnHeat = fuelHeatMultiplier(fuel);
        this.burnTime = (int) (duration * fuelTimeMultiplier(fuel)) / 2;
        this.maxBurnTime = this.burnTime;
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            this.items.set(FUEL_SLOT, ItemStack.EMPTY);
        }
    }

    private void setupAdditiveTank() {
        ItemStack identifier = this.items.get(FLUID_IDENTIFIER_SLOT);
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
            if (!selected.isNone() && selected != this.additiveTank.type()) {
                this.additiveTank.setType(selected);
            }
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
        return BrickFurnaceBlockEntity.fuelDuration(stack);
    }

    public double pistonOffset(float partialTick) {
        double animation = this.lastClientAnimation + (this.clientAnimation - this.lastClientAnimation) * partialTick;
        return steppedSine((animation * 0.75D) * 0.125D) * 0.5D - 0.5D;
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            for (FluidPortSpec port : fluidPorts()) {
                this.level.invalidateCapabilities(port.pos());
                this.level.invalidateCapabilities(port.pos().relative(port.face()));
            }
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void sendSmoke(Level level) {
        Direction facing = getFacing();
        Direction rot = LegacyMachineGeometry.forgeRotateDown(facing);
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

    private float steamUseMultiplier() {
        return (float) (10.0D * Math.log10(Math.max(this.burnHeat, 1.0D)) + 1.0D);
    }

    private static double fuelTimeMultiplier(ItemStack stack) {
        String path = fuelPath(stack);
        if (path.equals("solid_fuel") || path.equals("solid_fuel_presto") || path.equals("solid_fuel_presto_triplet")
                || path.equals("solid_fuel_bf") || path.equals("solid_fuel_presto_bf") || path.equals("solid_fuel_presto_triplet_bf")
                || path.equals("rocket_fuel")) {
            return 1.5D;
        }
        return path.contains("coke") ? 1.25D : 1.0D;
    }

    private static double fuelHeatMultiplier(ItemStack stack) {
        return switch (fuelPath(stack)) {
            case "solid_fuel", "solid_fuel_presto", "solid_fuel_presto_triplet" -> 1.5D;
            case "rocket_fuel" -> 3.0D;
            case "solid_fuel_bf", "solid_fuel_presto_bf", "solid_fuel_presto_triplet_bf" -> 10.0D;
            default -> 1.0D;
        };
    }

    private static String fuelPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
    }

    private static double steppedSine(double value) {
        return Math.sin(Math.PI * 0.5D * Math.cos(value));
    }

    @Nullable
    private FluidPort fluidPort(BlockPos queriedPos, @Nullable Direction side) {
        for (FluidPortSpec port : fluidPorts()) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return port.kind();
            }
        }
        return null;
    }

    private List<FluidPortSpec> fluidPorts() {
        Direction facing = getFacing();
        Direction rotation = LegacyMachineGeometry.forgeRotateDown(facing);
        return List.of(
                new FluidPortSpec(this.worldPosition.relative(facing.getOpposite()).relative(rotation.getOpposite()), facing.getOpposite(), FluidPort.STEAM),
                new FluidPortSpec(this.worldPosition.relative(facing.getOpposite()).relative(rotation.getOpposite(), 2), facing.getOpposite(), FluidPort.STEAM),
                new FluidPortSpec(this.worldPosition.relative(facing).relative(rotation, 2), rotation, FluidPort.ADDITIVE),
                new FluidPortSpec(this.worldPosition.relative(facing.getOpposite()).relative(rotation, 2), rotation, FluidPort.ADDITIVE)
        );
    }

    private enum FluidPort {
        ADDITIVE,
        STEAM
    }

    private record FluidPortSpec(BlockPos pos, Direction face, FluidPort kind) {
    }

    private final class FluidPortHandler implements IFluidHandler {
        private final FluidPort port;

        private FluidPortHandler(FluidPort port) {
            this.port = port;
        }

        @Override
        public int getTanks() {
            return this.port == FluidPort.STEAM ? 2 : 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            if (this.port == FluidPort.ADDITIVE) {
                return tankIndex == 0 ? additiveTank.getFluidInTank(0) : FluidStack.EMPTY;
            }
            return switch (tankIndex) {
                case 0 -> steamTank.getFluidInTank(0);
                case 1 -> spentSteamTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            if (this.port == FluidPort.ADDITIVE) {
                return tankIndex == 0 ? additiveTank.capacity() : 0;
            }
            return switch (tankIndex) {
                case 0 -> steamTank.capacity();
                case 1 -> spentSteamTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            if (this.port == FluidPort.ADDITIVE) {
                return tankIndex == 0 && fluid == additiveTank.type();
            }
            return tankIndex == 0 && fluid == fluid("steam");
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank tank;
            if (this.port == FluidPort.STEAM && fluid == fluid("steam")) {
                tank = steamTank;
            } else if (this.port == FluidPort.ADDITIVE && fluid == additiveTank.type()) {
                tank = additiveTank;
            } else {
                return 0;
            }
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
            if (this.port != FluidPort.STEAM || fluid != spentSteamTank.type()) {
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
            if (this.port != FluidPort.STEAM) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = spentSteamTank.type();
            HbmFluidStack drained = spentSteamTank.drain(fluid, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, drained.amount());
        }
    }
}
