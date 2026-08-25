package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModWD40: +4 maximum health and an 80% chance to undo one armor-damage point. */
public final class LegacyWd40ArmorModItem extends ArmorModItem {
    public LegacyWd40ArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    @Override
    public double extraHealth() {
        return 4.0D;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        if (armor.isDamageableItem() && armor.getDamageValue() > 0 && player.getRandom().nextInt(5) != 0) {
            armor.setDamageValue(armor.getDamageValue() - 1);
        }
        return amount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.wd40").withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
