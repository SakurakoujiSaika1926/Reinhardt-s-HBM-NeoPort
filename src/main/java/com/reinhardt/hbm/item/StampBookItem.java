package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import javax.annotation.Nullable;

/** Stack-variant equivalent of 1.7.10 ItemStampBook's eight metadata values. */
public final class StampBookItem extends StampItem {
    private static final String VARIANT_TAG = "variant";
    private static final StampType[] TYPES = {
            StampType.PRINTING1,
            StampType.PRINTING2,
            StampType.PRINTING3,
            StampType.PRINTING4,
            StampType.PRINTING5,
            StampType.PRINTING6,
            StampType.PRINTING7,
            StampType.PRINTING8
    };

    public StampBookItem(Properties properties) {
        super(properties, null);
    }

    @Override
    @Nullable
    public StampType type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT_TAG)) {
            return typeByName(tag.getString(VARIANT_TAG));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return typeAt(modelData == null ? 0 : modelData.value());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.stamp_book." + type(stack).getSerializedName());
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (StampType type : TYPES) {
            output.accept(stackFor(this, type));
        }
    }

    public static ItemStack stackFor(StampBookItem item, StampType type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(VARIANT_TAG, type.getSerializedName());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal() - StampType.PRINTING1.ordinal()));
        return stack;
    }

    private static StampType typeByName(String name) {
        for (StampType type : TYPES) {
            if (type.getSerializedName().equals(name)) {
                return type;
            }
        }
        return StampType.PRINTING1;
    }

    private static StampType typeAt(int index) {
        return TYPES[Math.floorMod(index, TYPES.length)];
    }
}
