package com.reinhardt.hbm.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry-provider-free preview of ItemStacks saved inside custom data.
 *
 * <p>This is intentionally limited to tooltip display. Gameplay paths should
 * keep using {@link ItemStack#parseOptional(net.minecraft.core.HolderLookup.Provider, CompoundTag)}
 * so full data components are restored when a real stack is needed.</p>
 */
public final class SavedItemStackPreview {
    private SavedItemStackPreview() {
    }

    public static ItemStack fromCustomData(ItemStack holder, String key) {
        CompoundTag root = holder.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(key, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return fromTag(root.getCompound(key));
    }

    public static List<ItemStack> listFromCustomData(ItemStack holder, String key) {
        CompoundTag root = holder.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag serialized = root.getList(key, Tag.TAG_COMPOUND);
        if (serialized.isEmpty()) {
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>();
        for (int index = 0; index < serialized.size(); index++) {
            ItemStack stack = fromTag(serialized.getCompound(index));
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    public static ItemStack fromTag(CompoundTag saved) {
        ResourceLocation id = ResourceLocation.tryParse(saved.getString("id"));
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        int count = savedCount(saved);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, count);
        int damage = savedDamage(saved);
        if (damage > 0 && stack.isDamageableItem()) {
            stack.setDamageValue(Math.min(damage, stack.getMaxDamage()));
        }
        return stack;
    }

    private static int savedCount(CompoundTag saved) {
        if (saved.contains("count", Tag.TAG_ANY_NUMERIC)) {
            return saved.getInt("count");
        }
        if (saved.contains("Count", Tag.TAG_ANY_NUMERIC)) {
            return saved.getInt("Count");
        }
        return 1;
    }

    private static int savedDamage(CompoundTag saved) {
        if (saved.contains("Damage", Tag.TAG_ANY_NUMERIC)) {
            return Math.max(0, saved.getInt("Damage"));
        }
        if (saved.contains("components", Tag.TAG_COMPOUND)) {
            CompoundTag components = saved.getCompound("components");
            if (components.contains("minecraft:damage", Tag.TAG_ANY_NUMERIC)) {
                return Math.max(0, components.getInt("minecraft:damage"));
            }
        }
        return 0;
    }
}
