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

import java.util.Locale;

public class FusionComponentBlockItem extends BlockItem {
    private static final String VARIANT = "variant";

    public FusionComponentBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.fusion_component." + type(stack).id());
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null || !state.hasProperty(LegacyVariantBlock.VARIANT)) {
            return state;
        }
        return state.setValue(LegacyVariantBlock.VARIANT, type(context.getItemInHand()).ordinal());
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Type type : Type.values()) {
            output.accept(stackFor(this, type));
        }
    }

    public Type type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT)) {
            return Type.byId(tag.getString(VARIANT));
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? Type.BSCCO : Type.byModelData(modelData.value());
    }

    public static ItemStack stackFor(FusionComponentBlockItem item, Type type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putString(VARIANT, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
        return stack;
    }

    public enum Type {
        BSCCO,
        BSCCO_WELDED,
        BLANKET,
        MOTOR;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static Type byId(String id) {
            if (id != null && !id.isBlank()) {
                for (Type type : values()) {
                    if (type.id().equals(id) || type.name().equalsIgnoreCase(id)) {
                        return type;
                    }
                }
            }
            return BSCCO;
        }

        private static Type byModelData(int modelData) {
            Type[] values = values();
            return modelData >= 0 && modelData < values.length ? values[modelData] : BSCCO;
        }
    }
}
