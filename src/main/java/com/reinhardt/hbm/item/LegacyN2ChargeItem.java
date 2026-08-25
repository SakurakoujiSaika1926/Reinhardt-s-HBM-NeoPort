package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemN2 tooltip for the large nuclear explosive charge. */
public final class LegacyN2ChargeItem extends Item {
    public LegacyN2ChargeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm.n2_charge.used_in").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("block.reinhardtshbm.nuke_n2").withStyle(ChatFormatting.GRAY));
    }
}
