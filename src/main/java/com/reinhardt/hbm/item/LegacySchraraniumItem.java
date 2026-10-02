package com.reinhardt.hbm.item;

import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Exact 1.7.10 ItemSchraranium presentation, including its LBSM full-schrab
 * Nikonium disguise. The item identity remains schraranium just as it did in
 * the original; only its name, icon and one-line tooltip change.
 */
public final class LegacySchraraniumItem extends Item {
    public LegacySchraraniumItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return HbmConfig.lbsmFullSchrab() ? Component.literal("Nikonium Ingot") : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (HbmConfig.lbsmFullSchrab()) {
            tooltip.add(Component.literal("pankæk"));
        }
    }
}
