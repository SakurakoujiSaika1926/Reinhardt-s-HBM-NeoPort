package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModHealth, installed in the extra armor-modification slot. */
public final class HealthArmorModItem extends ArmorModItem {
    private final double health;
    private final boolean nostalgia;

    public HealthArmorModItem(Properties properties, double health, boolean nostalgia) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
        this.health = health;
        this.nostalgia = nostalgia;
    }

    public double health() {
        return this.health;
    }

    @Override
    public double extraHealth() {
        return this.health;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.health_armor_mod", this.health).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (this.nostalgia) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.health_armor_mod.nostalgia").withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
