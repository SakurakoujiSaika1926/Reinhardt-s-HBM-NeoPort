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

/** The 1.7.10 Ace/Queen of Spades helmet cards. */
public final class LegacyCardArmorModItem extends ArmorModItem {
    private final boolean queen;

    public LegacyCardArmorModItem(Item.Properties properties, boolean queen) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, true, false, false);
        this.queen = queen;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        return this.queen && player.getRandom().nextInt(3) == 0 ? 0.0F : amount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.queen
                        ? "tooltip.reinhardtshbm.card_qos"
                        : "tooltip.reinhardtshbm.card_aos")
                .withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
