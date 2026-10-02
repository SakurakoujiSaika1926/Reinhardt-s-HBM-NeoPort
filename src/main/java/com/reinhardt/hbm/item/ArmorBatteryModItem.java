package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Capacity multiplier installed in the dedicated powered-armor battery slot. */
public final class ArmorBatteryModItem extends ArmorModItem {
    private final double capacityMultiplier;

    public ArmorBatteryModItem(Item.Properties properties, double capacityMultiplier) {
        super(properties, ArmorModHandler.BATTERY, true, true, true, true);
        this.capacityMultiplier = capacityMultiplier;
    }

    public double capacityMultiplier() {
        return this.capacityMultiplier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.battery",
                        Math.round((this.capacityMultiplier - 1.0D) * 100.0D))
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
