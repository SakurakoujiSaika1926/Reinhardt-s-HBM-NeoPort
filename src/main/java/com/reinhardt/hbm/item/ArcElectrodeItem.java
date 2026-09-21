package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ArcElectrodeItem extends Item {
    private static final String DURABILITY = "arc_durability";
    private final String variantId;

    public ArcElectrodeItem(Properties properties, String variantId) {
        super(properties.stacksTo(1));
        this.variantId = variantId;
    }

    public static int maxDurability(ItemStack stack) {
        return switch (variantId(stack)) {
            case "lanthanium" -> 100;
            case "desh" -> 500;
            case "saturnite" -> 1500;
            default -> 10;
        };
    }

    public static int durability(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return Math.max(0, tag.getInt(DURABILITY));
    }

    public static boolean damage(ItemStack stack) {
        if (!(stack.getItem() instanceof ArcElectrodeItem)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        int next = durability(stack) + 1;
        tag.putInt(DURABILITY, next);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return next >= maxDurability(stack);
    }

    public static String variantId(ItemStack stack) {
        if (stack.getItem() instanceof ArcElectrodeItem electrode) {
            return electrode.variantId;
        }
        return "graphite";
    }

    public static int barWidth(ItemStack stack) {
        return Math.round(13.0F * Math.min(1.0F, durability(stack) / (float) maxDurability(stack)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return durability(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xD9A441;
    }
}
