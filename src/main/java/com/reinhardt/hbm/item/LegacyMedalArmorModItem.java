package com.reinhardt.hbm.item;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Direct port of ItemModMedal's ten RAD-per-second body-radiation reduction. */
public final class LegacyMedalArmorModItem extends ArmorModItem {
    public LegacyMedalArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, true, true, false, false);
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
        radiation.setRadiation(radiation.getRadiation() - 0.5F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.medal_liquidator")
                .withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
