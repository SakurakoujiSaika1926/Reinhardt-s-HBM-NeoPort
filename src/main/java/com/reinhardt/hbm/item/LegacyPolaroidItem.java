package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Direct 1.7.10 ItemPolaroid passive low-health protection. */
public final class LegacyPolaroidItem extends Item {
    public LegacyPolaroidItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player && player.getHealth() < 10.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 2));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.polaroid.fate").withStyle(ChatFormatting.GRAY));
    }
}
