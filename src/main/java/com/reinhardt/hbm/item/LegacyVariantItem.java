package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class LegacyVariantItem extends Item {
    private static final String VARIANT_TAG = "variant";

    private final String baseId;
    private final List<Variant> variants;

    public LegacyVariantItem(Properties properties, String baseId, List<Variant> variants) {
        super(properties);
        this.baseId = baseId;
        this.variants = List.copyOf(variants);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm." + this.baseId + "." + variant(stack).id());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm." + this.baseId).withStyle(ChatFormatting.DARK_GRAY));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Variant variant : this.variants) {
            output.accept(stackFor(this, variant.id()));
        }
    }

    protected List<Variant> variants() {
        return this.variants;
    }

    public Variant variant(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT_TAG)) {
            return variantById(tag.getString(VARIANT_TAG));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? this.variants.getFirst() : variantByModelData(modelData.value());
    }

    public static ItemStack stackFor(Supplier<? extends Item> item, String variantId) {
        return stackFor(item.get(), variantId);
    }

    public static ItemStack stackFor(Item item, String variantId) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof LegacyVariantItem variantItem) {
            variantItem.setVariant(stack, variantId);
        }
        return stack;
    }

    private void setVariant(ItemStack stack, String variantId) {
        Variant variant = variantById(variantId);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(VARIANT_TAG, variant.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(variant.modelData()));
    }

    private Variant variantById(String id) {
        if (id != null && !id.isBlank()) {
            for (Variant variant : this.variants) {
                if (variant.id().equals(id)) {
                    return variant;
                }
            }
        }
        return this.variants.getFirst();
    }

    private Variant variantByModelData(int modelData) {
        for (Variant variant : this.variants) {
            if (variant.modelData() == modelData) {
                return variant;
            }
        }
        return this.variants.getFirst();
    }

    public record Variant(String id, int modelData) {
        public static Variant of(String id, int modelData) {
            return new Variant(id.toLowerCase(Locale.ROOT), modelData);
        }
    }

    public static List<Variant> variants(String... ids) {
        return Arrays.stream(ids)
                .map(id -> Variant.of(id, Arrays.asList(ids).indexOf(id)))
                .toList();
    }
}
