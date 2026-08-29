package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.PlushieType;
import com.reinhardt.hbm.client.render.PlushieItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;
import java.util.List;

public final class PlushieBlockItem extends BlockItem {
    private static final String TYPE = "type";

    public PlushieBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static PlushieType type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TYPE)) return PlushieType.byId(tag.getString(TYPE));
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? PlushieType.NONE : PlushieType.byOrdinal(modelData.value());
    }

    public static ItemStack stackFor(PlushieBlockItem item, PlushieType type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
        return stack;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (PlushieType type : PlushieType.values()) {
            if (type != PlushieType.NONE) output.accept(stackFor(this, type));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.plushie", type(stack).label());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String inscription = type(stack).inscription();
        if (inscription != null) tooltip.add(Component.literal(inscription));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(PlushieItemRenderer.clientExtension());
    }
}
