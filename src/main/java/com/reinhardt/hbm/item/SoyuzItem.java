package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The launchable Soyuz rocket item; the launcher keys off this exact item id. */
public final class SoyuzItem extends Item {
    public SoyuzItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int skin = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getInt("skin");
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.soyuz.skin").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.soyuz.skin." + Math.clamp(skin, 0, 2))
                .withStyle(skin == 0 ? ChatFormatting.GOLD : skin == 1 ? ChatFormatting.BLUE : ChatFormatting.GREEN));
    }
}
