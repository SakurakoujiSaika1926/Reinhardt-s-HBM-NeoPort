package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.GlyphBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Preserves the separate glyph stacks and labels from ItemGlyphBlock. */
public final class GlyphBlockItem extends BlockItem {
    public GlyphBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack stackFor(GlyphBlockItem item, int glyph) {
        int clamped = Mth.clamp(glyph, 0, 15);
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt(GlyphBlock.GLYPH_TAG, clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
        return stack;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int glyph = 0; glyph < 16; glyph++) {
            output.accept(stackFor(this, glyph));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.brick_jungle_glyph." + GlyphBlock.glyph(stack))
                .withStyle(ChatFormatting.GRAY));
    }
}
