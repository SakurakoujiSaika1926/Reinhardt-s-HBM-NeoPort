package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.MeteorOreBlock;
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

public class MeteorOreBlockItem extends BlockItem {
    private static final String VARIANT = "variant";

    public MeteorOreBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.ore_meteor." + type(stack).id());
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        return state.setValue(MeteorOreBlock.VARIANT, type(context.getItemInHand()).ordinal());
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
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data == null ? Type.IRON : Type.byModelData(data.value());
    }

    public static ItemStack stackFor(MeteorOreBlockItem item, Type type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(VARIANT, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
        return stack;
    }

    public enum Type {
        IRON,
        COPPER,
        ALUMINIUM,
        RAREEARTH,
        COBALT;

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
            return IRON;
        }

        private static Type byModelData(int modelData) {
            Type[] values = values();
            return modelData >= 0 && modelData < values.length ? values[modelData] : IRON;
        }
    }
}
