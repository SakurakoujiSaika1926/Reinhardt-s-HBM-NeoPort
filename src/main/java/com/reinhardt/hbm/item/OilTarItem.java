package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;

public class OilTarItem extends Item {
    private static final String VARIANT_TAG = "variant";

    public OilTarItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.oil_tar." + variant(stack).id());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Variant variant = variant(stack);
        if (variant != Variant.CRUDE) {
            tooltip.add(Component.translatable("item.reinhardtshbm.oil_tar").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Variant variant : Variant.values()) {
            output.accept(stack(variant));
        }
    }

    public static ItemStack stack(Variant variant) {
        ItemStack stack = new ItemStack(com.reinhardt.hbm.registry.HbmItems.OIL_TAR.get());
        setVariant(stack, variant);
        return stack;
    }

    public static Variant variant(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT_TAG)) {
            return Variant.byId(tag.getString(VARIANT_TAG));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? Variant.CRUDE : Variant.byModelData(modelData.value());
    }

    public static void setVariant(ItemStack stack, Variant variant) {
        Variant next = variant == null ? Variant.CRUDE : variant;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(VARIANT_TAG, next.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(next.modelData()));
    }

    public enum Variant {
        CRUDE,
        CRACK,
        COAL,
        WOOD,
        WAX,
        PARAFFIN;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int modelData() {
            return ordinal();
        }

        public static Variant byId(String id) {
            if (id == null || id.isBlank()) {
                return CRUDE;
            }
            for (Variant variant : values()) {
                if (variant.id().equals(id)) {
                    return variant;
                }
            }
            return CRUDE;
        }

        public static Variant byModelData(int modelData) {
            Variant[] values = values();
            if (modelData < 0 || modelData >= values.length) {
                return CRUDE;
            }
            return values[modelData];
        }
    }
}
