package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.BlastFurnaceBlock;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.menu.BlastFurnaceMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.recipe.BlastFurnaceFuelRecipe;
import com.reinhardt.hbm.recipe.BlastFurnaceRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlastFurnaceBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int UPPER_INPUT_SLOT = 0;
    public static final int LOWER_INPUT_SLOT = 1;
    public static final int FUEL_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int SLOT_COUNT = 4;

    public static final int MAX_FUEL = 12_800;
    public static final int PROCESSING_SPEED = 400;
    public static final int DATA_COUNT = 6;
    private static final int SMOKE_BUFFER_CAPACITY = 50;

    private static final int[] ACCESSIBLE_SLOTS = {UPPER_INPUT_SLOT, LOWER_INPUT_SLOT, FUEL_SLOT, OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank smokeTank = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.SOOT), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokeLeadedTank = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.HEAVYMETAL), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokePoisonTank = new HbmFluidTank(HbmPollution.smokeFluid(HbmPollutionType.POISON), SMOKE_BUFFER_CAPACITY);
    private int fuel;
    private int progress;
    private boolean canProcess;
    private byte sideUpper = 1;
    private byte sideLower = 1;
    private byte sideFuel = 1;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BlastFurnaceBlockEntity.this.fuel;
                case 1 -> BlastFurnaceBlockEntity.this.progress;
                case 2 -> BlastFurnaceBlockEntity.this.canProcess ? 1 : 0;
                case 3 -> BlastFurnaceBlockEntity.this.sideUpper;
                case 4 -> BlastFurnaceBlockEntity.this.sideLower;
                case 5 -> BlastFurnaceBlockEntity.this.sideFuel;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> BlastFurnaceBlockEntity.this.fuel = value;
                case 1 -> BlastFurnaceBlockEntity.this.progress = value;
                case 2 -> BlastFurnaceBlockEntity.this.canProcess = value != 0;
                case 3 -> BlastFurnaceBlockEntity.this.sideUpper = (byte) value;
                case 4 -> BlastFurnaceBlockEntity.this.sideLower = (byte) value;
                case 5 -> BlastFurnaceBlockEntity.this.sideFuel = (byte) value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.BLAST_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlastFurnaceBlockEntity furnace) {
        if (!level.isClientSide) {
            furnace.tickServer(level);
        }
    }

    public static boolean hasExtension(@Nullable Level level, BlockPos pos) {
        return level != null && level.getBlockState(pos.above()).is(HbmBlocks.MACHINE_DIFURNACE_EXT.get());
    }

    public static boolean canAcceptRecipeInput(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<BlastFurnaceRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get())) {
            BlastFurnaceRecipe recipe = holder.value();
            if (recipe.inputA().test(stack) || recipe.inputB().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static int fuelPower(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        for (RecipeHolder<BlastFurnaceFuelRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE_FUEL.get())) {
            BlastFurnaceFuelRecipe recipe = holder.value();
            if (recipe.ingredient().test(stack)) {
                return recipe.power();
            }
        }
        return 0;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public int sideModeForSlot(int slot) {
        return switch (slot) {
            case UPPER_INPUT_SLOT -> this.sideUpper;
            case LOWER_INPUT_SLOT -> this.sideLower;
            case FUEL_SLOT -> this.sideFuel;
            default -> 0;
        };
    }

    public void cycleSideMode(int slot) {
        switch (slot) {
            case UPPER_INPUT_SLOT -> this.sideUpper = (byte) ((this.sideUpper + 1) % 6);
            case LOWER_INPUT_SLOT -> this.sideLower = (byte) ((this.sideLower + 1) % 6);
            case FUEL_SLOT -> this.sideFuel = (byte) ((this.sideFuel + 1) % 6);
            default -> {
                return;
            }
        }
        setChangedAndSync();
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
            if (slot == UPPER_INPUT_SLOT || slot == LOWER_INPUT_SLOT) {
                this.progress = 0;
            }
            setChangedAndSync();
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
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        if (slot == UPPER_INPUT_SLOT || slot == LOWER_INPUT_SLOT) {
            this.progress = 0;
        }
        setChangedAndSync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= UPPER_INPUT_SLOT && slot <= FUEL_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (side == null || !canPlaceItem(slot, stack)) {
            return false;
        }
        int sideId = side.get3DDataValue();
        if (slot == UPPER_INPUT_SLOT && this.sideUpper != sideId) {
            return false;
        }
        if (slot == LOWER_INPUT_SLOT && this.sideLower != sideId) {
            return false;
        }
        return slot != FUEL_SLOT || this.sideFuel == sideId;
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
        this.items.replaceAll(stack -> ItemStack.EMPTY);
        this.progress = 0;
        setChangedAndSync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.blast_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BlastFurnaceMenu(containerId, playerInventory, this, this.menuData);
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
        tag.putInt("Fuel", this.fuel);
        tag.putInt("Progress", this.progress);
        tag.putInt("powerTime", this.fuel);
        tag.putShort("cookTime", (short) this.progress);
        tag.putByteArray("modes", new byte[]{this.sideFuel, this.sideUpper, this.sideLower});
        tag.put("Smoke", this.smokeTank.save());
        tag.put("SmokeLeaded", this.smokeLeadedTank.save());
        tag.put("SmokePoison", this.smokePoisonTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.fuel = tag.contains("Fuel") ? tag.getInt("Fuel") : tag.getInt("powerTime");
        this.progress = tag.contains("Progress") ? tag.getInt("Progress") : tag.getShort("cookTime");
        byte[] modes = tag.getByteArray("modes");
        if (modes.length >= 3) {
            this.sideFuel = modes[0];
            this.sideUpper = modes[1];
            this.sideLower = modes[2];
        }
        this.smokeTank.load(tag.getCompound("Smoke"));
        this.smokeLeadedTank.load(tag.getCompound("SmokeLeaded"));
        this.smokePoisonTank.load(tag.getCompound("SmokePoison"));
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
        boolean changed = sendSmoke(level);
        changed |= tryConsumeFuel(level);
        Optional<BlastFurnaceRecipe.Match> match = findMatch(level);
        this.canProcess = match.isPresent() && this.fuel > 0;

        if (this.canProcess) {
            this.fuel--;
            boolean extension = hasExtension(level, this.worldPosition);
            this.progress += extension ? 3 : 1;
            if (level.getGameTime() % 20L == 0L) {
                HbmPollution.bufferedLegacyPollute(level, this.worldPosition, HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND * (extension ? 3.0D : 1.0D), this::smokeTank);
            }
            if (this.progress >= PROCESSING_SPEED) {
                this.progress -= PROCESSING_SPEED;
                finishProcessing(match.get());
            }
            if (this.fuel < 0) {
                this.fuel = 0;
            }
            changed = true;
        } else if (this.progress != 0) {
            this.progress = 0;
            changed = true;
        }

        setLit(level, this.progress > 0);
        if (changed) {
            setChangedAndSync();
        }
    }

    private boolean sendSmoke(Level level) {
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            changed |= HbmPollution.sendSmoke(level, this.worldPosition, this.worldPosition.relative(direction), direction.getOpposite(), this.smokeTank, this.smokeLeadedTank, this.smokePoisonTank);
        }
        if (hasExtension(level, this.worldPosition)) {
            changed |= HbmPollution.sendSmoke(level, this.worldPosition, this.worldPosition.above(2), Direction.DOWN, this.smokeTank, this.smokeLeadedTank, this.smokePoisonTank);
        }
        return changed;
    }

    private HbmFluidTank smokeTank(HbmPollutionType type) {
        return switch (type) {
            case HEAVYMETAL -> this.smokeLeadedTank;
            case POISON -> this.smokePoisonTank;
            case SOOT, FALLOUT -> this.smokeTank;
        };
    }

    private boolean tryConsumeFuel(Level level) {
        ItemStack fuelStack = this.items.get(FUEL_SLOT);
        int fuelPower = fuelPower(level, fuelStack);
        if (fuelStack.isEmpty() || fuelPower <= 0 || this.fuel > MAX_FUEL - fuelPower) {
            return false;
        }

        ItemStack consumed = fuelStack.copy();
        consumed.setCount(1);
        fuelStack.shrink(1);
        if (fuelStack.isEmpty()) {
            this.items.set(FUEL_SLOT, craftingRemaining(consumed, 1));
        }
        this.fuel += fuelPower;
        return true;
    }

    private Optional<BlastFurnaceRecipe.Match> findMatch(Level level) {
        for (RecipeHolder<BlastFurnaceRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get())) {
            Optional<BlastFurnaceRecipe.Match> match = holder.value().match(this.items.get(UPPER_INPUT_SLOT), this.items.get(LOWER_INPUT_SLOT));
            if (match.isPresent() && canPlaceOutput(match.get().result())) {
                return match;
            }
        }
        return Optional.empty();
    }

    private void finishProcessing(BlastFurnaceRecipe.Match match) {
        addOutput(match.result().copy());
        this.items.get(UPPER_INPUT_SLOT).shrink(match.upperCount());
        if (this.items.get(UPPER_INPUT_SLOT).isEmpty()) {
            this.items.set(UPPER_INPUT_SLOT, ItemStack.EMPTY);
        }
        this.items.get(LOWER_INPUT_SLOT).shrink(match.lowerCount());
        if (this.items.get(LOWER_INPUT_SLOT).isEmpty()) {
            this.items.set(LOWER_INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean canPlaceOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return stack.getCount() <= stack.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(output, stack)
                && output.getCount() + stack.getCount() <= Math.min(output.getMaxStackSize(), this.getMaxStackSize(output));
    }

    private void addOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, stack.copy());
        } else if (ItemStack.isSameItemSameComponents(output, stack)) {
            output.grow(stack.getCount());
        }
    }

    private void setLit(Level level, boolean lit) {
        BlockState state = level.getBlockState(this.worldPosition);
        if (state.hasProperty(BlastFurnaceBlock.LIT) && state.getValue(BlastFurnaceBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(BlastFurnaceBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < this.items.size();
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static ItemStack craftingRemaining(ItemStack stack, int count) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack single = stack.copy();
        single.setCount(1);
        Item item = single.getItem();
        if (!item.hasCraftingRemainingItem(single)) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = item.getCraftingRemainingItem(single);
        if (!remainder.isEmpty()) {
            remainder.setCount(remainder.getCount() * count);
        }
        return remainder;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }
}
