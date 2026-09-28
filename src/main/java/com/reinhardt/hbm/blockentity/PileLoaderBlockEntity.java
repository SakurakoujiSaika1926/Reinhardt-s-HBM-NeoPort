package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PileMk2Block;
import com.reinhardt.hbm.item.PileRodMk2Item;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Current-upstream Chicago Pile MK2 fuel loader. */
public final class PileLoaderBlockEntity extends PileMk2DeviceBlockEntity implements MachineInventory, WorldlyContainer {
    private static final int SLOT_ROD = 0;
    private static final int[] SLOTS = {SLOT_ROD};
    private static final double SPEED = 1.0D / 7.0D;

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private double actuatorLevel;
    private double previousLevel;
    private double synchronizedLevel;
    private int interpolationTicks;
    private int delay;
    private boolean loading;
    private boolean wasRedstone;
    private ItemStack channelStack = ItemStack.EMPTY;
    private double channelDepletion;
    private double channelTemperature;

    public PileLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PILE_MK2_LOADER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileLoaderBlockEntity loader) {
        if (level.isClientSide) {
            loader.tickClient();
        } else {
            loader.tickServer();
        }
    }

    private void tickServer() {
        if (this.level == null) return;
        Direction outward = facing();
        BlockPos entry = worldPosition.relative(outward.getOpposite());
        PileMk2CoreBlockEntity.PileChannel channel = null;
        channelStack = ItemStack.EMPTY;
        channelDepletion = 0.0D;
        channelTemperature = 0.0D;

        BlockState entryState = this.level.getBlockState(entry);
        if (entryState.getBlock() instanceof PileMk2Block
                && entryState.getValue(PileMk2Block.KIND) == PileMk2Block.Kind.FUEL_IN) {
            PileMk2CoreBlockEntity core = resolveCore(entry);
            if (core != null) {
                channel = core.getFuelChannel(entry);
                if (channel != null) {
                    channelNumber = core.getFuelChannelNumber(channel);
                    channelStack = channel.lastRod().copy();
                    channelDepletion = PileRodMk2Item.depletionPercent(channelStack);
                    channelTemperature = channel.heat();
                }
            }
        }
        if (channel == null) channelNumber = -1;

        BlockPos signalPos = worldPosition.relative(outward);
        boolean redstone = this.level.getSignal(signalPos, outward.getOpposite()) > 0;
        if (redstone && !wasRedstone && delay <= 0 && actuatorLevel <= 0.0D) {
            loading = true;
        }
        wasRedstone = redstone;

        boolean changed = false;
        if (delay > 0) {
            delay--;
        } else if (loading) {
            if (actuatorLevel == 0.0D) playBolt(1.0F);
            actuatorLevel = Math.min(1.0D, actuatorLevel + SPEED);
            changed = true;
            if (actuatorLevel >= 1.0D) {
                loading = false;
                delay = 5;
            }
        } else {
            if (actuatorLevel == 1.0D) {
                playBolt(0.75F);
                if (channel != null && !items.get(SLOT_ROD).isEmpty()) {
                    channel.loadItem(items.get(SLOT_ROD));
                    items.set(SLOT_ROD, ItemStack.EMPTY);
                    setChanged();
                }
            }
            if (actuatorLevel > 0.0D) {
                actuatorLevel = Math.max(0.0D, actuatorLevel - SPEED);
                changed = true;
            }
        }

        if (changed || this.level.getGameTime() % 35L == 0L) sync();
    }

    private void tickClient() {
        previousLevel = actuatorLevel;
        if (interpolationTicks > 0) {
            actuatorLevel += (synchronizedLevel - actuatorLevel) / interpolationTicks;
            interpolationTicks--;
        } else {
            actuatorLevel = synchronizedLevel;
        }
    }

    private void playBolt(float pitch) {
        if (this.level != null) {
            this.level.playSound(null, worldPosition, HbmSoundEvents.PILE_BOLT_OPEN.get(),
                    SoundSource.BLOCKS, 1.0F, pitch);
        }
    }

    public boolean startLoading() {
        if (delay > 0 || actuatorLevel > 0.0D || loading) return false;
        loading = true;
        sync();
        return true;
    }

    public double renderedLevel(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, previousLevel, actuatorLevel);
    }

    public ItemStack loadingStack() {
        return items.get(SLOT_ROD);
    }

    public ItemStack channelStack() {
        return channelStack;
    }

    public double channelDepletion() {
        return channelDepletion;
    }

    public double channelTemperature() {
        return channelTemperature;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putDouble("Level", actuatorLevel);
        tag.putInt("Delay", delay);
        tag.putBoolean("Loading", loading);
        tag.putBoolean("Redstone", wasRedstone);
        tag.put("ChannelStack", channelStack.saveOptional(registries));
        tag.putDouble("ChannelDepletion", channelDepletion);
        tag.putDouble("ChannelTemperature", channelTemperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        double loadedLevel = tag.getDouble("Level");
        if (this.actuatorLevel != loadedLevel) {
            this.synchronizedLevel = loadedLevel;
            this.interpolationTicks = 2;
            if (this.level == null || !this.level.isClientSide) this.actuatorLevel = loadedLevel;
        }
        delay = tag.getInt("Delay");
        loading = tag.getBoolean("Loading");
        wasRedstone = tag.getBoolean("Redstone");
        channelStack = tag.contains("ChannelStack")
                ? ItemStack.parseOptional(registries, tag.getCompound("ChannelStack")) : ItemStack.EMPTY;
        channelDepletion = tag.getDouble("ChannelDepletion");
        channelTemperature = tag.getDouble("ChannelTemperature");
    }

    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(SLOT_ROD).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == SLOT_ROD ? items.get(SLOT_ROD) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != SLOT_ROD) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) sync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot == SLOT_ROD ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != SLOT_ROD) return;
        ItemStack placed = stack;
        if (!placed.isEmpty() && placed.getCount() > 1) {
            placed = placed.copyWithCount(1);
        }
        items.set(slot, placed);
        sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == SLOT_ROD && PileRodMk2Item.isPileRod(stack); }
    @Override public int getMaxStackSize() { return 1; }
    @Override public void clearContent() { items.set(SLOT_ROD, ItemStack.EMPTY); }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        ItemStack stack = items.get(SLOT_ROD);
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, stack.copy()));
        }
        clearContent();
    }
}
