package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AmsCatalystItem extends Item {
    private final int color;
    private final long powerAbs;
    private final float powerMod;
    private final float heatMod;
    private final float fuelMod;

    public AmsCatalystItem(Properties properties, int color, long powerAbs, float powerMod, float heatMod, float fuelMod) {
        super(properties);
        this.color = color;
        this.powerAbs = powerAbs;
        this.powerMod = powerMod;
        this.heatMod = heatMod;
        this.fuelMod = fuelMod;
    }

    public int color() {
        return this.color;
    }

    public long powerAbs() {
        return this.powerAbs;
    }

    public float powerMod() {
        return this.powerMod;
    }

    public float heatMod() {
        return this.heatMod;
    }

    public float fuelMod() {
        return this.fuelMod;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ams_catalyst.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ams_catalyst.line2").withStyle(ChatFormatting.GRAY));
    }

    public static boolean isCatalyst(ItemStack stack) {
        return stack.getItem() instanceof AmsCatalystItem;
    }

    public static int color(ItemStack stack) {
        return stack.getItem() instanceof AmsCatalystItem catalyst ? catalyst.color() : 0;
    }
}
