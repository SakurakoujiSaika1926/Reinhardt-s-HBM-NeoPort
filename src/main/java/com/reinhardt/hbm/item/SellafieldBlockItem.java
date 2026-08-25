package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.SellafieldBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Carries the former metadata stage through inventory, placement and creative variants. */
public final class SellafieldBlockItem extends BlockItem {
    private static final String LEVEL_KEY = "level";

    public SellafieldBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(SellafieldBlock.LEVEL, level(context.getItemInHand()));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int level = 0; level < SellafieldBlock.LEVELS; level++) {
            output.accept(stackFor(this, level));
        }
    }

    public static ItemStack stackFor(SellafieldBlockItem item, int level) {
        int safeLevel = Math.max(0, Math.min(SellafieldBlock.LEVELS - 1, level));
        ItemStack stack = new ItemStack(item);
        CompoundTag data = new CompoundTag();
        data.putInt(LEVEL_KEY, safeLevel);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(safeLevel));
        return stack;
    }

    private static int level(ItemStack stack) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.contains(LEVEL_KEY)) {
            return Math.max(0, Math.min(SellafieldBlock.LEVELS - 1, data.getInt(LEVEL_KEY)));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? 0 : Math.max(0, Math.min(SellafieldBlock.LEVELS - 1, modelData.value()));
    }
}
