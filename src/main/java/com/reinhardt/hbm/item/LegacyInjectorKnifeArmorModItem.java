package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Direct port of ItemModKnife's 50-tick cumulative maximum-health loss. */
public final class LegacyInjectorKnifeArmorModItem extends ArmorModItem {
    public static final net.minecraft.resources.ResourceLocation HEALTH_MODIFIER = ReinhardtsHBM.id("injector_knife_health");

    public LegacyInjectorKnifeArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, true, false, false);
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (player.tickCount % 50 != 0 || player.getMaxHealth() <= 2.0F) {
            return;
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        double previousMaximum = player.getMaxHealth();
        maxHealth.removeModifier(HEALTH_MODIFIER);
        double unmodifiedMaximum = player.getMaxHealth();
        maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                HEALTH_MODIFIER,
                -(unmodifiedMaximum - previousMaximum + 2.0D),
                AttributeModifier.Operation.ADD_VALUE
        ));
        if (player.getMaxHealth() <= 2.0F) {
            HbmAdvancements.award(player, "some_wounds");
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.injector_knife").withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
