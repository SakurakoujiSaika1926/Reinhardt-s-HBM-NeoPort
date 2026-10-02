package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 iron cladding: +0.5 knockback resistance on the fitted armor piece. */
public final class IronCladdingArmorModItem extends ArmorModItem {
    public IronCladdingArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.CLADDING, true, true, true, true);
    }

    @Override
    public double knockbackResistance(ItemStack armor) {
        return 0.5D;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.iron_cladding")
                .withStyle(ChatFormatting.WHITE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
