package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Exact 1.7.10 bathwater armor-mod damage reactions. */
public final class LegacyBathwaterArmorModItem extends ArmorModItem {
    private final boolean markTwo;

    public LegacyBathwaterArmorModItem(Item.Properties properties, boolean markTwo) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
        this.markTwo = markTwo;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(
                    this.markTwo ? MobEffects.WITHER : MobEffects.POISON,
                    200,
                    this.markTwo ? 4 : 2
            ));
        }
        return amount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.markTwo
                        ? "tooltip.reinhardtshbm.bathwater_mk2"
                        : "tooltip.reinhardtshbm.bathwater")
                .withStyle(this.markTwo ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
