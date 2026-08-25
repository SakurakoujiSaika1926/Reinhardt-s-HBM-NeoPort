package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModCloud: chestplate-only dash module with a 12.5% speed multiplier. */
public final class LegacyCloudArmorModItem extends ArmorModItem {
    public LegacyCloudArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, false, true, false, false);
    }

    @Override
    public double movementMultiplier() {
        return 1.125D;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.cloud").withStyle(ChatFormatting.WHITE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
