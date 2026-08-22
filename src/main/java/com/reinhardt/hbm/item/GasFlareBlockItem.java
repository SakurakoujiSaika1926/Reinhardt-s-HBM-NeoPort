package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class GasFlareBlockItem extends LegacyOffsetBlockItem {
    public GasFlareBlockItem(Block block, Item.Properties properties) {
        super(block, properties, 1, true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_flare.use").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_flare.burn_rate").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_flare.vent_rate").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_flare.efficiency").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_flare.gas_efficiency").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_flare.liquid_efficiency").withStyle(ChatFormatting.YELLOW));
    }
}
