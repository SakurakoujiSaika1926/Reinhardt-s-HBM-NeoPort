package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.CapBlock;
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

/** Preserves the six 1.7.10 block metadata variants as explicit item stacks. */
public final class CapBlockItem extends BlockItem {
    private static final String TYPE_TAG = "type";

    public CapBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.block_cap." + type(stack).id());
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(CapBlock.TYPE, type(context.getItemInHand()));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (CapBlock.Type type : CapBlock.Type.values()) {
            output.accept(stackFor(this, type));
        }
    }

    public CapBlock.Type type(ItemStack stack) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.contains(TYPE_TAG)) {
            return CapBlock.Type.byId(data.getString(TYPE_TAG));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? CapBlock.Type.NUKA : CapBlock.Type.byModelData(modelData.value());
    }

    public static ItemStack stackFor(CapBlockItem item, CapBlock.Type type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag data = new CompoundTag();
        data.putString(TYPE_TAG, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
        return stack;
    }
}
