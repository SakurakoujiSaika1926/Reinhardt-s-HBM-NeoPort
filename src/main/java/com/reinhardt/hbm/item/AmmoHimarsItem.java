package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AmmoHimarsItem extends LegacyVariantItem {
    public AmmoHimarsItem(Properties properties) {
        super(properties, "ammo_himars", variants(
                "standard",
                "single",
                "standard_he",
                "standard_wp",
                "standard_tb",
                "single_tb",
                "standard_mini_nuke",
                "standard_lava"
        ));
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        output.accept(stackFor(this, "standard"));
        output.accept(stackFor(this, "standard_he"));
        output.accept(stackFor(this, "standard_wp"));
        output.accept(stackFor(this, "standard_tb"));
        output.accept(stackFor(this, "standard_lava"));
        output.accept(stackFor(this, "standard_mini_nuke"));
        output.accept(stackFor(this, "single"));
        output.accept(stackFor(this, "single_tb"));
    }

    public int rocketCount(net.minecraft.world.item.ItemStack stack) {
        return switch (variant(stack).id()) {
            case "single", "single_tb" -> 1;
            default -> 6;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (variant(stack).id()) {
            case "standard" -> {
                strength(tooltip, 20);
                damageModifier(tooltip, "3x");
                noBlockDamage(tooltip);
            }
            case "standard_he" -> {
                strength(tooltip, 20);
                damageModifier(tooltip, "3x");
                breaksBlocks(tooltip);
            }
            case "standard_wp" -> {
                strength(tooltip, 20);
                damageModifier(tooltip, "3x");
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_himars.phosphorus_splash").withStyle(ChatFormatting.RED));
                noBlockDamage(tooltip);
            }
            case "standard_tb" -> {
                strength(tooltip, 20);
                damageModifier(tooltip, "10x");
                breaksBlocks(tooltip);
            }
            case "standard_mini_nuke" -> {
                strength(tooltip, 20);
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_himars.nuclear_damage").withStyle(ChatFormatting.RED));
                breaksBlocks(tooltip);
            }
            case "standard_lava" -> {
                strength(tooltip, 20);
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_himars.volcanic_lava").withStyle(ChatFormatting.RED));
                breaksBlocks(tooltip);
            }
            case "single" -> {
                strength(tooltip, 50);
                damageModifier(tooltip, "5x");
                breaksBlocks(tooltip);
            }
            case "single_tb" -> {
                strength(tooltip, 50);
                damageModifier(tooltip, "12x");
                breaksBlocks(tooltip);
            }
            default -> {
            }
        }
    }

    private static void strength(List<Component> tooltip, int strength) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_himars.strength", strength).withStyle(ChatFormatting.YELLOW));
    }

    private static void damageModifier(List<Component> tooltip, String multiplier) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_himars.damage_modifier", multiplier).withStyle(ChatFormatting.YELLOW));
    }

    private static void breaksBlocks(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_himars.breaks_blocks").withStyle(ChatFormatting.RED));
    }

    private static void noBlockDamage(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_himars.no_block_damage").withStyle(ChatFormatting.BLUE));
    }
}
