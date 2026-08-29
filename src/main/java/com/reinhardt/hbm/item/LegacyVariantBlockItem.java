package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.block.LegacyVariantSlabBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Locale;

/** BlockItem counterpart for 1.7.10 metadata blocks represented by modern components. */
public class LegacyVariantBlockItem extends BlockItem {
    private static final String VARIANT_TAG = "variant";

    private final Property<?> property;
    private final String translationBase;
    private final String[] variants;

    public LegacyVariantBlockItem(Block block, Properties properties, Property<?> property,
                                  String translationBase, String... variants) {
        super(block, properties);
        this.property = property;
        this.translationBase = translationBase;
        this.variants = variants.clone();
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.translationBase + "." + variant(stack));
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null || !state.hasProperty(this.property)) {
            return state;
        }
        int index = variantIndex(context.getItemInHand());
        return setVariant(state, index);
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int index = 0; index < this.variants.length; index++) {
            output.accept(stackFor(this, index));
        }
    }

    public int variantIndex(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT_TAG)) {
            return indexOf(tag.getString(VARIANT_TAG));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? 0 : Math.max(0, Math.min(this.variants.length - 1, modelData.value()));
    }

    public String variant(ItemStack stack) {
        return this.variants[variantIndex(stack)];
    }

    public static ItemStack stackFor(LegacyVariantBlockItem item, int variant) {
        int clamped = Math.max(0, Math.min(item.variants.length - 1, variant));
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putString(VARIANT_TAG, item.variants[clamped]);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
        return stack;
    }

    public static IntegerProperty propertyFor(Block block) {
        if (block instanceof LegacyVariantSlabBlock) {
            return LegacyVariantSlabBlock.VARIANT;
        }
        return LegacyVariantBlock.VARIANT;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private BlockState setVariant(BlockState state, int index) {
        Object value = this.property.getPossibleValues().stream()
                .skip(Math.max(0, Math.min(index, this.property.getPossibleValues().size() - 1)))
                .findFirst()
                .orElse(null);
        return value == null ? state : setVariantValue(state, this.property, value);
    }

    private static <T extends Comparable<T>> BlockState setVariantValue(BlockState state,
                                                                          Property<T> property,
                                                                          Object value) {
        return state.setValue(property, (T) value);
    }

    private int indexOf(String value) {
        if (value != null) {
            for (int index = 0; index < this.variants.length; index++) {
                if (this.variants[index].equals(value.toLowerCase(Locale.ROOT))) {
                    return index;
                }
            }
        }
        return 0;
    }
}
