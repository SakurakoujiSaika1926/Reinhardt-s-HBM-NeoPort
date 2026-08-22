package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.ToasterBlock;
import com.reinhardt.hbm.client.render.ToasterItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public final class ToasterBlockItem extends BlockItem {
    private static final String VARIANT_TAG = "variant";

    public ToasterBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final ToasterItemRenderer renderer = new ToasterItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    public static int variant(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Mth.clamp(tag.getInt(VARIANT_TAG), 0, 2);
    }

    public static ItemStack stackFor(Item item, int variant) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        int clamped = Mth.clamp(variant, 0, 2);
        tag.putInt(VARIANT_TAG, clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
        return stack;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int variant = 0; variant < 3; variant++) {
            output.accept(stackFor(this, variant));
        }
    }
}
