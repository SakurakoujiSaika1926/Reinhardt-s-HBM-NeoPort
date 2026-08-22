package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Equivalent of 1.7.10 BrokenItem. The legacy NBT item ID and metadata are
 * represented by an item ID plus the two components needed by unified HBM material items.
 */
public final class BrokenItem extends Item {
    private static final String ITEM_ID = "item_id";
    private static final String ITEM_COUNT = "item_count";
    private static final String ITEM_DATA = "item_data";
    private static final String MODEL_DATA = "model_data";

    public BrokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack original = originalStack(stack);
        return original.isEmpty()
                ? super.getName(stack)
                : Component.translatable("item.reinhardtshbm.broken_item.prefix", original.getHoverName());
    }

    public static ItemStack make(ItemStack source) {
        ItemStack result = new ItemStack(HbmItems.BROKEN_ITEM.get(), source.getCount());
        CompoundTag data = new CompoundTag();
        data.putString(ITEM_ID, BuiltInRegistries.ITEM.getKey(source.getItem()).toString());
        data.putInt(ITEM_COUNT, source.getCount());
        CustomData sourceData = source.get(DataComponents.CUSTOM_DATA);
        if (sourceData != null && !sourceData.isEmpty()) {
            data.put(ITEM_DATA, sourceData.copyTag());
        }
        CustomModelData modelData = source.get(DataComponents.CUSTOM_MODEL_DATA);
        if (modelData != null) {
            data.putInt(MODEL_DATA, modelData.value());
        }
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return result;
    }

    public static ItemStack make(Item item) {
        return make(new ItemStack(item));
    }

    public static ItemStack originalStack(ItemStack stack) {
        if (!stack.is(HbmItems.BROKEN_ITEM.get())) {
            return ItemStack.EMPTY;
        }
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ResourceLocation id = ResourceLocation.tryParse(data.getString(ITEM_ID));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        ItemStack original = new ItemStack(BuiltInRegistries.ITEM.get(id), Math.max(1, data.getInt(ITEM_COUNT)));
        if (data.contains(ITEM_DATA, CompoundTag.TAG_COMPOUND)) {
            original.set(DataComponents.CUSTOM_DATA, CustomData.of(data.getCompound(ITEM_DATA).copy()));
        }
        if (data.contains(MODEL_DATA)) {
            original.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(data.getInt(MODEL_DATA)));
        }
        return original;
    }

    public static boolean matches(ItemStack broken, ItemStack source) {
        ItemStack original = originalStack(broken);
        return !original.isEmpty() && ItemStack.isSameItemSameComponents(original, source.copyWithCount(original.getCount()));
    }
}
