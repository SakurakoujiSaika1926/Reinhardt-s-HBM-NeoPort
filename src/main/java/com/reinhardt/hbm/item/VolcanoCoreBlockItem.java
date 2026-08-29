package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.VolcanoCoreBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Preserves the five old metadata variants on item stacks. */
public final class VolcanoCoreBlockItem extends BlockItem {
    private static final String MODE = "mode";

    public VolcanoCoreBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm." + (getBlock() instanceof VolcanoCoreBlock core && core.radioactive()
                ? "volcano_rad_core" : "volcano_core"));
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(VolcanoCoreBlock.MODE, mode(context.getItemInHand()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                java.util.List<Component> tooltip, TooltipFlag flag) {
        int mode = mode(stack);
        if (mode == 4) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.volcano.shield")
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
        boolean growing = mode == 2 || mode == 3;
        boolean extinguishing = mode == 1 || mode == 3;
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.volcano." + (growing ? "grows" : "does_not_grow"))
                .withStyle(growing ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.volcano." + (extinguishing ? "extinguishes" : "does_not_extinguish"))
                .withStyle(extinguishing ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int mode = 0; mode < 5; mode++) {
            output.accept(stackFor(this, mode));
        }
    }

    public int mode(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(MODE)) {
            return clampMode(tag.getInt(MODE));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? 0 : clampMode(modelData.value());
    }

    public static ItemStack stackFor(VolcanoCoreBlockItem item, int mode) {
        int clamped = clampMode(mode);
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt(MODE, clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
        return stack;
    }

    private static int clampMode(int mode) {
        return Math.max(0, Math.min(4, mode));
    }
}
