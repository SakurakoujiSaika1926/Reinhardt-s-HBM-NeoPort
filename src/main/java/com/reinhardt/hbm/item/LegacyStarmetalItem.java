package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/** 1.7.10 ItemStarmetal, including its hidden Astra, Ursa and Orion variants. */
public final class LegacyStarmetalItem extends Item {
    private static final List<String> VARIANTS = List.of("", "Astra", "Ursa", "Orion");

    public LegacyStarmetalItem(Properties properties) {
        super(properties);
    }

    public static ItemStack stackFor(Item item, int variant) {
        ItemStack stack = new ItemStack(item);
        if (variant > 0 && variant < VARIANTS.size()) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(variant));
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int variant = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0)).value();
        if (variant > 0 && variant < VARIANTS.size()) {
            tooltip.add(Component.literal(VARIANTS.get(variant)).withStyle(ChatFormatting.ITALIC));
        }
    }
}
