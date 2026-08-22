package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.MetalFenceBlock;
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

public class MetalFenceBlockItem extends BlockItem {
    private static final String POST_TAG = "post";

    public MetalFenceBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isPost(stack)) {
            return Component.translatable("block.reinhardtshbm.fence_metal_post");
        }
        return super.getName(stack);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state != null && state.hasProperty(MetalFenceBlock.FORCE_POST)) {
            state = state.setValue(MetalFenceBlock.FORCE_POST, isPost(context.getItemInHand()))
                    .setValue(MetalFenceBlock.PILLAR, true);
        }
        return state;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        output.accept(new ItemStack(this));
        output.accept(postStack(this));
    }

    public static ItemStack postStack(MetalFenceBlockItem item) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(POST_TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return stack;
    }

    public static boolean isPost(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(POST_TAG);
    }
}
