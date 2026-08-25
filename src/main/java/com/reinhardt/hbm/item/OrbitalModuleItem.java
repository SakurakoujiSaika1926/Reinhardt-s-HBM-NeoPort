package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Dedicated Soyuz orbital module item accepted by the launcher payload slot. */
public final class OrbitalModuleItem extends Item {
    public OrbitalModuleItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.soyuz.orbital_module").withStyle(ChatFormatting.GRAY));
    }
}
