package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class BlockBlastResistanceTooltip {
    private static final float LEGACY_DISPLAY_THRESHOLD = 50.0F;

    private BlockBlastResistanceTooltip() {
    }

    public static void append(ItemStack stack, List<Component> tooltip) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        Block block = blockItem.getBlock();
        float resistance = block.getExplosionResistance();
        if (resistance <= LEGACY_DISPLAY_THRESHOLD) {
            return;
        }

        tooltip.add(Component.translatable("tooltip.reinhardtshbm.blast_resistance", format(resistance))
                .withStyle(ChatFormatting.GREEN));
    }

    private static String format(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }
}
