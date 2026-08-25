package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModShield, installed in the chestplate insert slot. */
public final class LegacyShieldArmorModItem extends ArmorModItem {
    private final float shield;

    public LegacyShieldArmorModItem(Item.Properties properties, float shield) {
        super(properties, ArmorModHandler.KEVLAR, false, true, false, false);
        this.shield = shield;
    }

    public float shield() {
        return this.shield;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.shield", this.shield)
                .withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
