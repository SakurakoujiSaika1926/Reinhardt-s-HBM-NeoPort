package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModNightVision for a powered, enabled full FSB suit. */
public final class LegacyNightVisionArmorModItem extends ArmorModItem {
    public LegacyNightVisionArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (armor.getItem() instanceof PoweredArmorFSBItem && ArmorFSBItem.hasFSBArmor(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20, 0, false, false));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.night_vision")
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
