package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.LegacyVariantBlock;
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

public final class OreBasaltBlockItem extends BlockItem {
    private static final String VARIANT = "variant";
    private static final String[] IDS = {"sulfur", "fluorite", "asbestos", "gem", "molysite"};

    public OreBasaltBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.ore_basalt." + IDS[variantIndex(stack)]);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(LegacyVariantBlock.VARIANT, variantIndex(context.getItemInHand()));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int variant = 0; variant < IDS.length; variant++) {
            output.accept(stackFor(this, variant));
        }
    }

    public static ItemStack stackFor(OreBasaltBlockItem item, int variant) {
        int safeVariant = Math.max(0, Math.min(IDS.length - 1, variant));
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt(VARIANT, safeVariant);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(safeVariant));
        return stack;
    }

    /** Returns the clamped metadata variant carried by this legacy block item. */
    public static int variantIndex(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT)) {
            return Math.max(0, Math.min(IDS.length - 1, tag.getInt(VARIANT)));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? 0 : Math.max(0, Math.min(IDS.length - 1, modelData.value()));
    }

    public static String variantName(ItemStack stack) {
        return IDS[variantIndex(stack)];
    }
}
