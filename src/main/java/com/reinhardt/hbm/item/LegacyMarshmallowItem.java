package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/** Two metadata states from 1.7.10 ItemMarshmallow. */
public final class LegacyMarshmallowItem extends LegacyVariantItem {
    public LegacyMarshmallowItem(Properties properties) {
        super(properties.stacksTo(1), "marshmallow", variants("raw", "roasted"), false);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm." + (isRoasted(stack) ? "marshmallow_roasted" : "marshmallow"));
    }

    private boolean isRoasted(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getString("variant").equals("roasted")) {
            return true;
        }
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data != null && data.value() == 1;
    }
}
