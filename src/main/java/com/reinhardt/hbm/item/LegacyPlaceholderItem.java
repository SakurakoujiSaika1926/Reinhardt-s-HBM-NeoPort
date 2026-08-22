package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LegacyPlaceholderItem extends Item {
    private final String legacyId;

    public LegacyPlaceholderItem(Properties properties, String legacyId) {
        super(properties);
        this.legacyId = legacyId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm.legacy_placeholder.hint", legacyId)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
