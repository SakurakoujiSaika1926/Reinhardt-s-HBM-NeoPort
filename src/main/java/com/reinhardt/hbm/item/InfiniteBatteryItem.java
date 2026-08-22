package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class InfiniteBatteryItem extends Item {
    public static final long POWER = Long.MAX_VALUE;
    public static final long TRANSFER_RATE = 1_000_000_000_000L;

    public InfiniteBatteryItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return 13;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x55FFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.battery.infinite").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.battery.discharge_rate", "1T").withStyle(ChatFormatting.YELLOW));
    }
}
