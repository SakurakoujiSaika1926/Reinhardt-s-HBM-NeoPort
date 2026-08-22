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

public class AmsLensItem extends Item {
    public static final long MAX_DAMAGE = 432_000_000L;
    private static final String DAMAGE_TAG = "damage";

    public AmsLensItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static long lensDamage(ItemStack stack) {
        return Math.max(0L, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong(DAMAGE_TAG));
    }

    public static void setLensDamage(ItemStack stack, long damage) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        long clamped = Math.max(0L, Math.min(MAX_DAMAGE, damage));
        if (clamped == 0L) {
            tag.remove(DAMAGE_TAG);
        } else {
            tag.putLong(DAMAGE_TAG, clamped);
        }
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static boolean isValidLens(ItemStack stack) {
        return stack.getItem() instanceof AmsLensItem && lensDamage(stack) < MAX_DAMAGE;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return lensDamage(stack) > 0L;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long remaining = Math.max(0L, MAX_DAMAGE - lensDamage(stack));
        return Math.round(13.0F * remaining / MAX_DAMAGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFAA00;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long remaining = Math.max(0L, MAX_DAMAGE - lensDamage(stack));
        long percent = remaining * 100L / MAX_DAMAGE;
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ams_lens.durability", remaining, MAX_DAMAGE, percent).withStyle(ChatFormatting.GRAY));
    }
}
