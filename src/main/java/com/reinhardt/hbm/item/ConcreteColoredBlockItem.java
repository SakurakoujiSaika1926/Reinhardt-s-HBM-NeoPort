package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.ConcreteColoredBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;

public class ConcreteColoredBlockItem extends BlockItem {
    private static final String[] COLORS = {
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "silver",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black"
    };
    private static final String[] EXT_COLORS = {
            "machine",
            "machine_stripe",
            "indigo",
            "purple",
            "pink",
            "hazard",
            "sand",
            "bronze"
    };
    private final String baseTranslationKey;
    private final String[] variants;

    public ConcreteColoredBlockItem(Block block, Properties properties) {
        this(block, properties, "block.reinhardtshbm.concrete_colored", COLORS);
    }

    public ConcreteColoredBlockItem(Block block, Properties properties, String baseTranslationKey, String[] variants) {
        super(block, properties);
        this.baseTranslationKey = baseTranslationKey;
        this.variants = variants.clone();
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.baseTranslationKey + "." + this.variants[clampMeta(ConcreteColoredBlock.meta(stack))]);
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int meta = 0; meta < this.variants.length; meta++) {
            output.accept(stackFor(this, meta));
        }
    }

    public static ConcreteColoredBlockItem ext(Block block, Properties properties) {
        return new ConcreteColoredBlockItem(block, properties, "block.reinhardtshbm.concrete_colored_ext", EXT_COLORS);
    }

    public static ItemStack stackFor(ConcreteColoredBlockItem item, int meta) {
        ItemStack stack = new ItemStack(item);
        int clamped = item.clampMeta(meta);
        CompoundTag tag = new CompoundTag();
        tag.putInt(ConcreteColoredBlock.META_TAG, clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
        return stack;
    }

    private int clampMeta(int meta) {
        return Mth.clamp(meta, 0, this.variants.length - 1);
    }
}
