package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.entity.MinerRocketEntity;
import com.reinhardt.hbm.item.SatelliteChipItem;
import com.reinhardt.hbm.menu.SatelliteDockMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.satellite.SatelliteSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Exact inventory, timing and cargo-table port of TileEntityMachineSatDock. */
public final class SatelliteDockBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int OUTPUT_SLOT_COUNT = 15;
    public static final int SLOT_SATELLITE = 15;
    public static final int SLOT_COUNT = 16;
    private static final int[] ACCESSIBLE_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14
    };
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public SatelliteDockBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SAT_DOCK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SatelliteDockBlockEntity dock) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack chip = dock.items.get(SLOT_SATELLITE);
        SatelliteSavedData.SatelliteRecord satellite = dock.minerSatellite(serverLevel, chip);
        if (satellite != null && level.getGameTime() % 20L == 0L
                && !satellite.requestableItems().isEmpty()) {
            int frequency = SatelliteChipItem.frequency(chip);
            List<ItemStack> cargo = satellite.takeRequestableItems(serverLevel);
            MinerRocketEntity rocket = new MinerRocketEntity(level, frequency, cargo);
            rocket.setPos(pos.getX() + 0.5D, 300.0D, pos.getZ() + 0.5D);
            level.addFreshEntity(rocket);
        }

        AABB dockingVolume = new AABB(pos.getX() + 0.25D, pos.getY() + 0.75D, pos.getZ() + 0.25D,
                pos.getX() + 0.75D, pos.getY() + 2.0D, pos.getZ() + 0.75D);
        for (MinerRocketEntity rocket : level.getEntitiesOfClass(MinerRocketEntity.class, dockingVolume)) {
            if (satellite == null || rocket.frequency() != SatelliteChipItem.frequency(chip)) {
                rocket.discard();
                level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        10.0F, Level.ExplosionInteraction.BLOCK);
                break;
            }
            if (rocket.shouldUnloadCargo()) {
                dock.unloadCargo(rocket.takeCargo());
            }
        }

        dock.ejectInto(pos.offset(2, 0, 0));
        dock.ejectInto(pos.offset(-2, 0, 0));
        dock.ejectInto(pos.offset(0, 0, 2));
        dock.ejectInto(pos.offset(0, 0, -2));
    }

    @Nullable
    private SatelliteSavedData.SatelliteRecord minerSatellite(ServerLevel level, ItemStack chip) {
        if (!(chip.getItem() instanceof SatelliteChipItem)) {
            return null;
        }
        return SatelliteSavedData.get(level).satellite(SatelliteChipItem.frequency(chip))
                .filter(record -> record.kind() == SatelliteSavedData.SatelliteKind.MINER
                        || record.kind() == SatelliteSavedData.SatelliteKind.LUNAR_MINER)
                .orElse(null);
    }

    private void unloadCargo(List<ItemStack> cargo) {
        for (ItemStack supplied : cargo) {
            ItemStack remainder = addToOutput(supplied.copy());
            if (!remainder.isEmpty()) {
                Block.popResource(this.level, this.worldPosition.above(), remainder);
            }
        }
        setChanged();
    }

    private ItemStack addToOutput(ItemStack stack) {
        for (int slot = 0; slot < OUTPUT_SLOT_COUNT && !stack.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int accepted = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
                if (accepted > 0) {
                    existing.grow(accepted);
                    stack.shrink(accepted);
                }
            }
        }
        for (int slot = 0; slot < OUTPUT_SLOT_COUNT && !stack.isEmpty(); slot++) {
            if (items.get(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                items.set(slot, stack.copyWithCount(moved));
                stack.shrink(moved);
            }
        }
        return stack;
    }

    private void ejectInto(BlockPos targetPos) {
        if (!(this.level.getBlockEntity(targetPos) instanceof Container target)) {
            return;
        }
        for (int slot = 0; slot < OUTPUT_SLOT_COUNT; slot++) {
            ItemStack output = items.get(slot);
            if (output.isEmpty()) {
                continue;
            }
            for (int targetSlot = 0; targetSlot < target.getContainerSize(); targetSlot++) {
                ItemStack existing = target.getItem(targetSlot);
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, output)
                        && existing.getCount() < Math.min(existing.getMaxStackSize(), target.getMaxStackSize())
                        && target.canPlaceItem(targetSlot, output.copyWithCount(1))) {
                    existing.grow(1);
                    output.shrink(1);
                    target.setChanged();
                    if (output.isEmpty()) {
                        items.set(slot, ItemStack.EMPTY);
                    }
                    setChanged();
                    return;
                }
            }
        }
        for (int slot = 0; slot < OUTPUT_SLOT_COUNT; slot++) {
            ItemStack output = items.get(slot);
            if (output.isEmpty()) {
                continue;
            }
            for (int targetSlot = 0; targetSlot < target.getContainerSize(); targetSlot++) {
                if (target.getItem(targetSlot).isEmpty() && target.canPlaceItem(targetSlot, output.copyWithCount(1))) {
                    target.setItem(targetSlot, output.copyWithCount(1));
                    output.shrink(1);
                    target.setChanged();
                    if (output.isEmpty()) {
                        items.set(slot, ItemStack.EMPTY);
                    }
                    setChanged();
                    return;
                }
            }
        }
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < SLOT_COUNT ? items.get(slot) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_SATELLITE && stack.getItem() instanceof SatelliteChipItem;
    }
    @Override public int[] getSlotsForFace(Direction side) { return ACCESSIBLE_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 0 && slot < OUTPUT_SLOT_COUNT; }
    @Override public boolean stillValid(Player player) { return MachineDummyMenuAccess.stillValid(this, player); }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.reinhardtshbm.sat_dock");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SatelliteDockMenu(containerId, inventory, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

}
