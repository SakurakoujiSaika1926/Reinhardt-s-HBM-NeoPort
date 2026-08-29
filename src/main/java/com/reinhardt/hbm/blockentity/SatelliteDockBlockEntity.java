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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    private static final long DELIVERY_DELAY_MILLIS = 10L * 60L * 1_000L;
    private static final LootEntry[] MINER_CARGO = {
            loot("powder_aluminium", 3, 3, 10), loot("powder_iron", 3, 3, 10),
            loot("powder_titanium", 2, 2, 8), loot("crystal_tungsten", 2, 2, 7),
            loot("powder_coal", 4, 4, 15), loot("powder_uranium", 2, 2, 5),
            loot("powder_plutonium", 1, 1, 5), loot("powder_thorium", 2, 2, 7),
            loot("powder_desh_mix", 3, 3, 5), loot("powder_diamond", 2, 2, 7),
            loot("minecraft:redstone", 5, 5, 15), loot("powder_nitan_mix", 2, 2, 5),
            loot("powder_power", 2, 2, 5), loot("powder_copper", 5, 5, 15),
            loot("powder_lead", 3, 3, 10), loot("fluorite", 4, 4, 15),
            loot("powder_lapis", 4, 4, 10), loot("crystal_aluminium", 1, 1, 5),
            loot("crystal_gold", 1, 1, 5), loot("crystal_phosphorus", 1, 1, 10),
            loot("gravel_diamond", 1, 1, 3), loot("crystal_uranium", 1, 1, 3),
            loot("crystal_plutonium", 1, 1, 3), loot("crystal_trixite", 1, 1, 1),
            loot("crystal_starmetal", 1, 1, 1), loot("crystal_lithium", 2, 2, 4)
    };
    private static final LootEntry[] LUNAR_CARGO = {
            loot("moon_turf", 48, 48, 5), loot("moon_turf", 32, 32, 7),
            loot("moon_turf", 16, 16, 5), loot("powder_lithium", 3, 3, 5),
            loot("powder_iron", 3, 3, 5), loot("crystal_iron", 1, 1, 1),
            loot("crystal_lithium", 1, 1, 1)
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
        if (satellite != null && satellite.lastOperation() + DELIVERY_DELAY_MILLIS < System.currentTimeMillis()) {
            int frequency = SatelliteChipItem.frequency(chip);
            MinerRocketEntity rocket = new MinerRocketEntity(level, frequency);
            rocket.setPos(pos.getX() + 0.5D, 300.0D, pos.getZ() + 0.5D);
            level.addFreshEntity(rocket);
            SatelliteSavedData.get(serverLevel).markMinerDelivery(frequency, System.currentTimeMillis());
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
                dock.unloadCargo(satellite.kind());
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

    private void unloadCargo(SatelliteSavedData.SatelliteKind kind) {
        LootEntry[] cargo = kind == SatelliteSavedData.SatelliteKind.LUNAR_MINER ? LUNAR_CARGO : MINER_CARGO;
        int amount = this.level.random.nextInt(6) + 10;
        for (int index = 0; index < amount; index++) {
            LootEntry entry = choose(cargo, this.level.random.nextInt(totalWeight(cargo)));
            ItemStack stack = entry.stack(this.level.random);
            if (!stack.isEmpty()) {
                addToOutput(stack);
            }
        }
        setChanged();
    }

    private void addToOutput(ItemStack stack) {
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
                // TileEntityMachineSatDock creates a one-item stack for a newly used slot.
                items.set(slot, stack.copyWithCount(1));
                return;
            }
        }
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
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

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

    private static LootEntry loot(String id, int min, int max, int weight) {
        return new LootEntry(id, min, max, weight);
    }

    private static int totalWeight(LootEntry[] cargo) {
        int total = 0;
        for (LootEntry entry : cargo) total += entry.weight;
        return total;
    }

    private static LootEntry choose(LootEntry[] cargo, int roll) {
        int remaining = roll;
        for (LootEntry entry : cargo) {
            remaining -= entry.weight;
            if (remaining < 0) return entry;
        }
        return cargo[cargo.length - 1];
    }

    private record LootEntry(String id, int min, int max, int weight) {
        private ItemStack stack(net.minecraft.util.RandomSource random) {
            ResourceLocation key = id.contains(":") ? ResourceLocation.parse(id)
                    : ResourceLocation.fromNamespaceAndPath("reinhardtshbm", id);
            Item item = BuiltInRegistries.ITEM.get(key);
            if (item == null || item == net.minecraft.world.level.block.Blocks.AIR.asItem()) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, min + random.nextInt(max - min + 1));
        }
    }
}
