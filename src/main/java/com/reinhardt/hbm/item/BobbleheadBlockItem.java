package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.client.render.BobbleheadItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** 1.7.10 metadata-preserving bobblehead item, backed by the block entity's type. */
public final class BobbleheadBlockItem extends BlockItem {
    private static final String TYPE = "type";

    public BobbleheadBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static BobbleheadType type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TYPE)) return BobbleheadType.byId(tag.getString(TYPE));
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? BobbleheadType.NONE : BobbleheadType.byOrdinal(modelData.value());
    }

    public static ItemStack stackFor(BobbleheadBlockItem item, BobbleheadType type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
        return stack;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (BobbleheadType type : BobbleheadType.values()) {
            if (type != BobbleheadType.NONE) output.accept(stackFor(this, type));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.bobblehead");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(BobbleheadItemRenderer.clientExtension());
    }
}
