package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.CokeBlock;
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

import java.util.Locale;

public class CokeBlockItem extends BlockItem {
    private static final String TYPE = "type";

    public CokeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.block_coke." + type(stack).id());
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        return state.setValue(CokeBlock.VARIANT, type(context.getItemInHand()).ordinal());
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (CokeType type : CokeType.values()) {
            output.accept(stackFor(this, type));
        }
    }

    public CokeType type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TYPE)) {
            return CokeType.byId(tag.getString(TYPE));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? CokeType.COAL : CokeType.byModelData(modelData.value());
    }

    public static ItemStack stackFor(CokeBlockItem item, CokeType type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TYPE, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
        return stack;
    }

    public enum CokeType {
        COAL,
        LIGNITE,
        PETROLEUM;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static CokeType byId(String id) {
            if (id != null && !id.isBlank()) {
                for (CokeType type : values()) {
                    if (type.id().equals(id) || type.name().equalsIgnoreCase(id)) {
                        return type;
                    }
                }
            }
            return COAL;
        }

        private static CokeType byModelData(int modelData) {
            CokeType[] values = values();
            return modelData >= 0 && modelData < values.length ? values[modelData] : COAL;
        }
    }
}
