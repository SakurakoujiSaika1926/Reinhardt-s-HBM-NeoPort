package com.reinhardt.hbm.integration.curios;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.StorageCrateBlock;
import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import com.reinhardt.hbm.item.StorageCrateBlockItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/** Item-backed crate storage shared by the Curios bridge and its portable menu. */
public final class PortableCrateStorage {
    private static final String SLOT_PREFIX = "slot";
    public static final TagKey<Item> PORTABLE_CRATES = TagKey.create(
            Registries.ITEM, ReinhardtsHBM.id("portable_crates")
    );

    private PortableCrateStorage() {
    }

    public static boolean isCrate(ItemStack stack) {
        return !stack.isEmpty() && stack.is(PORTABLE_CRATES)
                && stack.getItem() instanceof StorageCrateBlockItem;
    }

    public static StorageCrateBlockEntity.Kind kind(ItemStack stack) {
        if (stack.getItem() instanceof StorageCrateBlockItem item
                && item.getBlock() instanceof StorageCrateBlock crate) {
            return crate.kind();
        }
        return StorageCrateBlockEntity.Kind.IRON;
    }

    public static NonNullList<ItemStack> load(ItemStack crate, HolderLookup.Provider registries,
                                                StorageCrateBlockEntity.Kind kind) {
        NonNullList<ItemStack> contents = NonNullList.withSize(kind.slots(), ItemStack.EMPTY);
        CompoundTag root = crate.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(StorageCrateBlockEntity.ITEM_DATA_KEY)) {
            return contents;
        }

        CompoundTag data = root.getCompound(StorageCrateBlockEntity.ITEM_DATA_KEY);
        for (int slot = 0; slot < contents.size(); slot++) {
            contents.set(slot, ItemStack.parseOptional(registries, data.getCompound(SLOT_PREFIX + slot)));
        }
        return contents;
    }

    /** Replaces only inventory entries and preserves lock, spider, and other crate metadata. */
    public static void save(ItemStack crate, List<ItemStack> contents, HolderLookup.Provider registries) {
        CompoundTag root = crate.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag data = root.contains(StorageCrateBlockEntity.ITEM_DATA_KEY)
                ? root.getCompound(StorageCrateBlockEntity.ITEM_DATA_KEY).copy()
                : new CompoundTag();

        for (String key : new ArrayList<>(data.getAllKeys())) {
            if (isSlotKey(key)) {
                data.remove(key);
            }
        }
        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack stack = contents.get(slot);
            if (!stack.isEmpty()) {
                data.put(SLOT_PREFIX + slot, stack.saveOptional(registries));
            }
        }

        root.put(StorageCrateBlockEntity.ITEM_DATA_KEY, data);
        crate.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /** Inserts as vanilla inventories do: merge existing stacks first, then use empty slots. */
    public static int insert(List<ItemStack> contents, ItemStack source) {
        if (source.isEmpty() || isCrate(source)) {
            return 0;
        }

        int original = source.getCount();
        for (int slot = 0; slot < contents.size() && !source.isEmpty(); slot++) {
            ItemStack target = contents.get(slot);
            if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, source)) {
                continue;
            }
            int room = Math.min(target.getMaxStackSize(), source.getMaxStackSize()) - target.getCount();
            if (room <= 0) {
                continue;
            }
            int moved = Math.min(room, source.getCount());
            target.grow(moved);
            source.shrink(moved);
        }

        for (int slot = 0; slot < contents.size() && !source.isEmpty(); slot++) {
            if (!contents.get(slot).isEmpty()) {
                continue;
            }
            int moved = Math.min(source.getCount(), source.getMaxStackSize());
            contents.set(slot, source.copyWithCount(moved));
            source.shrink(moved);
        }
        return original - source.getCount();
    }

    private static boolean isSlotKey(String key) {
        if (!key.startsWith(SLOT_PREFIX) || key.length() == SLOT_PREFIX.length()) {
            return false;
        }
        for (int index = SLOT_PREFIX.length(); index < key.length(); index++) {
            if (!Character.isDigit(key.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
