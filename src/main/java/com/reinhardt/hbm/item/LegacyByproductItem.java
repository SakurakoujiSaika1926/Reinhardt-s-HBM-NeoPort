package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/** Direct port of ItemByproduct's fourteen metadata colours. */
public final class LegacyByproductItem extends Item {
    private static final String TYPE_TAG = "byproduct_type";

    public LegacyByproductItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.ore_byproduct.b_" + type(stack).id + ".name");
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Type type : Type.values()) {
            output.accept(stackFor(this, type));
        }
    }

    public static ItemStack stackFor(Item item, Type type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TYPE_TAG, type.id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
        return stack;
    }

    public static int tint(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? 0xFF000000 | type(stack).color : 0xFFFFFFFF;
    }

    private static Type type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String id = tag.getString(TYPE_TAG);
        for (Type type : Type.values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (model != null && model.value() >= 0 && model.value() < Type.values().length) {
            return Type.values()[model.value()];
        }
        return Type.IRON;
    }

    public enum Type {
        IRON("iron", 0xE2C0AA),
        COPPER("copper", 0xEC9A63),
        LITHIUM("lithium", 0xEDEDED),
        SILICON("silicon", 0xFFFBD1),
        LEAD("lead", 0x646470),
        TITANIUM("titanium", 0xF2EFE2),
        ALUMINIUM("aluminium", 0xE8F2F9),
        SULFUR("sulfur", 0xEAD377),
        CALCIUM("calcium", 0xCFCFA6),
        BISMUTH("bismuth", 0x8D8577),
        RADIUM("radium", 0xE9FAF6),
        TECHNETIUM("technetium", 0xCADFDF),
        POLONIUM("polonium", 0xCADFDF),
        URANIUM("uranium", 0x868D82);

        private final String id;
        private final int color;

        Type(String id, int color) {
            this.id = id;
            this.color = color;
        }
    }
}
