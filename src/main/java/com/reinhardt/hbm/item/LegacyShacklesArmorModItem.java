package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** ItemModShackles only supplied the armor-mod slot contract and description. */
public final class LegacyShacklesArmorModItem extends ArmorModItem {
    public LegacyShacklesArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, false, true, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.shackles")
                .withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
