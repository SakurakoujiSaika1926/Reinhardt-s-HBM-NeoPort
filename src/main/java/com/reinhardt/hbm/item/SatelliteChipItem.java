package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** 1.7.10 ItemSatChip: each satellite module carries its mutable frequency in NBT. */
public final class SatelliteChipItem extends Item {
    private static final String FREQUENCY_KEY = "freq";
    private final String descriptionKey;

    public SatelliteChipItem(Properties properties, String descriptionKey) {
        super(properties);
        this.descriptionKey = descriptionKey;
    }

    public static int frequency(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt(FREQUENCY_KEY);
    }

    public static void setFrequency(ItemStack stack, int frequency) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(FREQUENCY_KEY, frequency);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm.satellite.frequency", frequency(stack)).withStyle(ChatFormatting.GRAY));
        if (!this.descriptionKey.isEmpty()) {
            tooltip.add(Component.translatable(this.descriptionKey).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
