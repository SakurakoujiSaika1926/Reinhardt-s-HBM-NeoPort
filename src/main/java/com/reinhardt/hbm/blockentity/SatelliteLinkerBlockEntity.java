package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.SatelliteChipItem;
import com.reinhardt.hbm.menu.SatelliteLinkerMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.satellite.SatelliteSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Exact three-slot inventory and frequency logic from TileEntityMachineSatLinker. */
public final class SatelliteLinkerBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_TARGET = 1;
    public static final int SLOT_RANDOMIZE = 2;
    public static final int SLOT_COUNT = 3;

    private static final int[] SLOTS_TOP = {SLOT_SOURCE};
    private static final int[] SLOTS_BOTTOM = {SLOT_TARGET};
    private static final int[] SLOTS_SIDE = {SLOT_RANDOMIZE};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public SatelliteLinkerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SATELLITE_LINKER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SatelliteLinkerBlockEntity linker) {
        if (level.isClientSide) return;

        boolean changed = false;
        ItemStack source = linker.items.get(SLOT_SOURCE);
        ItemStack target = linker.items.get(SLOT_TARGET);
        if (isSatelliteChip(source) && isSatelliteChip(target)) {
            int frequency = SatelliteChipItem.frequency(source);
            if (SatelliteChipItem.frequency(target) != frequency) {
                SatelliteChipItem.setFrequency(target, frequency);
                changed = true;
            }
        }

        ItemStack randomize = linker.items.get(SLOT_RANDOMIZE);
        if (isSatelliteChip(randomize) && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // The legacy TE rolled once per tick; a collision simply waited
            // for the following tick instead of using a replacement strategy.
            int frequency = level.random.nextInt(100_000);
            if (!SatelliteSavedData.get(serverLevel).isFrequencyTaken(frequency)
                    && SatelliteChipItem.frequency(randomize) != frequency) {
                SatelliteChipItem.setFrequency(randomize, frequency);
                changed = true;
            }
        }
        if (changed) linker.setChanged();
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return this.items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return SLOTS_TOP;
        if (side == Direction.DOWN) return SLOTS_BOTTOM;
        return SLOTS_SIDE;
    }

    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 0 && slot < SLOT_COUNT; }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.reinhardtshbm.sat_linker");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SatelliteLinkerMenu(containerId, inventory, this);
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
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    private static boolean isSatelliteChip(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SatelliteChipItem;
    }
}
