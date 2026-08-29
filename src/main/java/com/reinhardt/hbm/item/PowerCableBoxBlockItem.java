package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.PowerCableBoxBlock;
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

/** Carries the five former metadata values used by PowerCableBox. */
public final class PowerCableBoxBlockItem extends BlockItem {
    private static final String SIZE_TAG = "size";

    public PowerCableBoxBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(PowerCableBoxBlock.SIZE, size(context.getItemInHand()));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int size = 0; size < 5; size++) {
            output.accept(stackFor(this, size));
        }
    }

    public static ItemStack stackFor(PowerCableBoxBlockItem item, int size) {
        int clamped = Math.clamp(size, 0, 4);
        ItemStack stack = new ItemStack(item);
        CompoundTag data = new CompoundTag();
        data.putInt(SIZE_TAG, clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
        return stack;
    }

    public static int size(ItemStack stack) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.clamp(data.getInt(SIZE_TAG), 0, 4);
    }
}
