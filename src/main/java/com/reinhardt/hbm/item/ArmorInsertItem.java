package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The old ItemModInsert's chestplate-only armor insert values. */
public final class ArmorInsertItem extends ArmorModItem {
    private final float damageMultiplier;
    private final float projectileMultiplier;
    private final float explosionMultiplier;
    private final float speedMultiplier;
    private final boolean radioactive;
    private final boolean reactive;

    public ArmorInsertItem(
            Properties properties,
            float damageMultiplier,
            float projectileMultiplier,
            float explosionMultiplier,
            float speedMultiplier,
            boolean radioactive,
            boolean reactive
    ) {
        super(properties, ArmorModHandler.KEVLAR, false, true, false, false);
        this.damageMultiplier = damageMultiplier;
        this.projectileMultiplier = projectileMultiplier;
        this.explosionMultiplier = explosionMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.radioactive = radioactive;
        this.reactive = reactive;
    }

    public float modifyDamage(float amount, boolean projectile, boolean explosion) {
        float result = amount * this.damageMultiplier;
        if (projectile) result *= this.projectileMultiplier;
        if (explosion) result *= this.explosionMultiplier;
        return result;
    }

    public float speedMultiplier() {
        return this.speedMultiplier;
    }

    public boolean radioactive() {
        return this.radioactive;
    }

    public boolean reactive() {
        return this.reactive;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.damageMultiplier != 1.0F) {
            tooltip.add(modifier("tooltip.reinhardtshbm.armor_insert.damage", this.damageMultiplier, ChatFormatting.RED));
        }
        if (this.projectileMultiplier != 1.0F) {
            tooltip.add(modifier("tooltip.reinhardtshbm.armor_insert.projectile", this.projectileMultiplier, ChatFormatting.YELLOW));
        }
        if (this.explosionMultiplier != 1.0F) {
            tooltip.add(modifier("tooltip.reinhardtshbm.armor_insert.explosion", this.explosionMultiplier, ChatFormatting.YELLOW));
        }
        if (this.speedMultiplier != 1.0F) {
            tooltip.add(modifier("tooltip.reinhardtshbm.armor_insert.speed", this.speedMultiplier, ChatFormatting.BLUE));
        }
        if (this.radioactive) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_insert.radiation").withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_insert.durability", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static Component modifier(String key, float multiplier, ChatFormatting color) {
        int percent = Math.round(Math.abs((1.0F - multiplier) * 100.0F));
        String sign = multiplier < 1.0F ? "-" : "+";
        return Component.translatable(key, sign + percent + "%").withStyle(color);
    }
}
