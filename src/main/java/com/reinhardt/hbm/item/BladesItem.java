package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BladesItem extends Item {
    public BladesItem(int durability) {
        super(properties(durability));
    }

    public static boolean isUsableBlade(ItemStack stack) {
        return stack.getItem() instanceof BladesItem && gearState(stack) > 0 && gearState(stack) < 3;
    }

    public static int gearState(ItemStack stack) {
        if (!(stack.getItem() instanceof BladesItem)) {
            return 0;
        }

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 1;
        }

        int damage = stack.getDamageValue();
        if (damage < maxDamage / 2) {
            return 1;
        }
        if (damage < maxDamage) {
            return 2;
        }
        return 3;
    }

    public static void damageBlade(ItemStack stack) {
        if (!(stack.getItem() instanceof BladesItem) || stack.getMaxDamage() <= 0) {
            return;
        }
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
    }

    private static Properties properties(int durability) {
        Properties properties = new Properties().stacksTo(1);
        return durability > 0 ? properties.durability(durability) : properties;
    }
}
