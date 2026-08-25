package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Map;

/**
 * Direct 1.7.10 ItemBatterySC port. These batteries always report their
 * isotope-specific maximum charge and never accept or lose stored energy.
 */
public final class SelfChargingBatteryItem extends LegacyVariantItem {
    private static final Map<String, Long> OUTPUTS = Map.ofEntries(
            Map.entry("empty", 0L),
            Map.entry("waste", 150L),
            Map.entry("ra226", 200L),
            Map.entry("tc99", 500L),
            Map.entry("co60", 750L),
            Map.entry("pu238", 1_000L),
            Map.entry("po210", 1_250L),
            Map.entry("au198", 1_500L),
            Map.entry("pb209", 2_000L),
            Map.entry("am241", 2_500L)
    );

    public SelfChargingBatteryItem(Properties properties) {
        super(properties.stacksTo(1), "battery_sc", LegacyVariantItem.variants(
                "empty", "waste", "ra226", "tc99", "co60", "pu238", "po210", "au198", "pb209", "am241"
        ));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long output = output(stack);
        if (output > 0L) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.battery.discharge_rate", output)
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    public long output(ItemStack stack) {
        return OUTPUTS.getOrDefault(variant(stack).id(), 0L);
    }
}
