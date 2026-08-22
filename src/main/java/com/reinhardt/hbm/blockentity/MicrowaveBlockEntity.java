package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.MicrowaveBlock;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.menu.MicrowaveMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class MicrowaveBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_COUNT = 4;
    public static final long MAX_POWER = 50_000L;
    public static final int CONSUMPTION = 50;
    public static final int MAX_TIME = 300;
    public static final int MAX_SPEED = 5;

    private final ItemStack[] items = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private long power;
    private long lastInput;
    private int time;
    private int speed;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) MicrowaveBlockEntity.this.power;
                case 1 -> (int) MicrowaveBlockEntity.this.lastInput;
                case 2 -> MicrowaveBlockEntity.this.time;
                case 3 -> MicrowaveBlockEntity.this.speed;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> MicrowaveBlockEntity.this.power = value;
                case 1 -> MicrowaveBlockEntity.this.lastInput = value;
                case 2 -> MicrowaveBlockEntity.this.time = value;
                case 3 -> MicrowaveBlockEntity.this.speed = Math.max(0, Math.min(MAX_SPEED, value));
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MicrowaveBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.MICROWAVE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MicrowaveBlockEntity microwave) {
        if (level.isClientSide) {
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, microwave);
        microwave.tickWork(level);
    }

    private void tickWork(Level level) {
        boolean wasAnimating = this.time > 0;
        this.power = BatteryPackItem.dischargeIntoMachine(this.items[BATTERY_SLOT], this.power, MAX_POWER);

        if (this.canProcess(level)) {
            if (this.speed >= MAX_SPEED) {
                level.removeBlock(this.worldPosition, false);
                level.explode(null,
                        this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 0.5D,
                        this.worldPosition.getZ() + 0.5D,
                        5.0F,
                        true,
                        Level.ExplosionInteraction.BLOCK);
                return;
            }

            if (this.time >= MAX_TIME) {
                this.process(level);
                this.time = 0;
            }

            if (this.canProcess(level)) {
                this.power -= CONSUMPTION;
                this.time += this.speed * 2;
            }
        }

        if (wasAnimating != this.time > 0) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
        this.setChanged();
    }

    private void process(Level level) {
        ItemStack result = this.recipeResult(level);
        if (result.isEmpty()) {
            return;
        }
        if (this.items[OUTPUT_SLOT].isEmpty()) {
            this.items[OUTPUT_SLOT] = result.copy();
        } else {
            this.items[OUTPUT_SLOT].grow(result.getCount());
        }
        this.items[INPUT_SLOT].shrink(1);
        this.setChanged();
    }

    public boolean canProcess(Level level) {
        if (this.speed == 0 || this.power < CONSUMPTION) {
            return false;
        }
        ItemStack input = this.items[INPUT_SLOT];
        ItemStack result = this.recipeResult(level);
        if (input.isEmpty() || result.isEmpty()) {
            return false;
        }
        if (!input.has(DataComponents.FOOD) && !result.has(DataComponents.FOOD)) {
            return false;
        }
        ItemStack output = this.items[OUTPUT_SLOT];
        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    public boolean canAcceptInput(ItemStack stack) {
        return this.level == null || this.level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), this.level)
                .isPresent();
    }

    private ItemStack recipeResult(Level level) {
        if (this.items[INPUT_SLOT].isEmpty()) {
            return ItemStack.EMPTY;
        }
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(this.items[INPUT_SLOT]), level);
        return recipe.map(holder -> holder.value().assemble(
                new SingleRecipeInput(this.items[INPUT_SLOT]), level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    public int speed() {
        return this.speed;
    }

    public int time() {
        return this.time;
    }

    public void changeSpeed(int delta) {
        this.speed = Math.max(0, Math.min(MAX_SPEED, this.speed + delta));
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean isWorking() {
        return this.time > 0;
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.power * pixels / MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.time * pixels / MAX_TIME);
    }

    public int speedScaled(int pixels) {
        return Math.min(pixels, this.speed * pixels / MAX_SPEED);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public BlockPos blockPos() {
        return this.worldPosition;
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
        return Math.max(0L, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        this.setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.power.microwave", this.lastInput, this.power, MAX_POWER);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items[0].isEmpty() && this.items[1].isEmpty() && this.items[2].isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? this.items[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items[slot].split(amount);
        if (this.items[slot].isEmpty()) {
            this.items[slot] = ItemStack.EMPTY;
        }
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items[slot];
        this.items[slot] = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (isValidSlot(slot)) {
            this.items[slot] = stack;
            if (stack.getCount() > this.getMaxStackSize(stack)) {
                stack.setCount(this.getMaxStackSize(stack));
            }
            this.setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && this.canAcceptInput(stack)
                || slot == BATTERY_SLOT && ShredderBlockEntity.isBattery(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{INPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && this.canAcceptInput(stack);
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
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items[slot] = ItemStack.EMPTY;
        }
        this.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_microwave");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MicrowaveMenu(containerId, playerInventory, this, this.menuData, this.worldPosition);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", this.items[INPUT_SLOT].saveOptional(registries));
        tag.put("Output", this.items[OUTPUT_SLOT].saveOptional(registries));
        tag.put("Battery", this.items[BATTERY_SLOT].saveOptional(registries));
        tag.putLong("Power", this.power);
        tag.putInt("Speed", this.speed);
        tag.putInt("Time", this.time);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items[INPUT_SLOT] = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        this.items[OUTPUT_SLOT] = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        this.items[BATTERY_SLOT] = ItemStack.parseOptional(registries, tag.getCompound("Battery"));
        this.power = tag.getLong("Power");
        this.speed = Math.max(0, Math.min(MAX_SPEED, tag.getInt("Speed")));
        this.time = Math.max(0, Math.min(MAX_TIME, tag.getInt("Time")));
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

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }
}
