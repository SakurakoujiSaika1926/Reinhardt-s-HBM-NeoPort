package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

public class AmmoArtyItem extends LegacyVariantItem {
    public static final String CARGO_TAG = "cargo";
    public static final int CARGO_MODEL_DATA = 8;
    public static final int CARGO_FULL_MODEL_DATA = 12;

    public AmmoArtyItem(Properties properties) {
        super(properties, "ammo_arty", variants(
                "ammo_arty",
                "ammo_arty_classic",
                "ammo_arty_he",
                "ammo_arty_mini_nuke",
                "ammo_arty_nuke",
                "ammo_arty_phosphorus",
                "ammo_arty_mini_nuke_multi",
                "ammo_arty_phosphorus_multi",
                "ammo_arty_cargo",
                "ammo_arty_chlorine",
                "ammo_arty_phosgene",
                "ammo_arty_mustard_gas"
        ));
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        output.accept(stackFor(this, "ammo_arty"));
        output.accept(stackFor(this, "ammo_arty_classic"));
        output.accept(stackFor(this, "ammo_arty_he"));
        output.accept(stackFor(this, "ammo_arty_phosphorus"));
        output.accept(stackFor(this, "ammo_arty_phosphorus_multi"));
        output.accept(stackFor(this, "ammo_arty_mini_nuke"));
        output.accept(stackFor(this, "ammo_arty_mini_nuke_multi"));
        output.accept(stackFor(this, "ammo_arty_nuke"));
        output.accept(stackFor(this, "ammo_arty_cargo"));
        output.accept(stackFor(this, "ammo_arty_chlorine"));
        output.accept(stackFor(this, "ammo_arty_phosgene"));
        output.accept(stackFor(this, "ammo_arty_mustard_gas"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (variant(stack).id()) {
            case "ammo_arty" -> {
                strength(tooltip, 10);
                damageModifier(tooltip, "3x");
                noBlockDamage(tooltip);
            }
            case "ammo_arty_classic" -> {
                strength(tooltip, 15);
                damageModifier(tooltip, "5x");
                noBlockDamage(tooltip);
            }
            case "ammo_arty_he" -> {
                strength(tooltip, 15);
                damageModifier(tooltip, "3x");
                breaksBlocks(tooltip);
            }
            case "ammo_arty_phosphorus" -> {
                strength(tooltip, 10);
                damageModifier(tooltip, "3x");
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.phosphorus_splash")
                        .withStyle(ChatFormatting.RED));
                noBlockDamage(tooltip);
            }
            case "ammo_arty_phosphorus_multi" -> tooltip.add(
                    Component.translatable("tooltip.reinhardtshbm.ammo_arty.splits", 10).withStyle(ChatFormatting.RED));
            case "ammo_arty_mini_nuke" -> {
                strength(tooltip, 20);
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.nuclear_damage")
                        .withStyle(ChatFormatting.RED));
                breaksBlocks(tooltip);
            }
            case "ammo_arty_mini_nuke_multi" -> tooltip.add(
                    Component.translatable("tooltip.reinhardtshbm.ammo_arty.splits", 5).withStyle(ChatFormatting.RED));
            case "ammo_arty_nuke" -> {
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.nuke_symbol")
                        .withStyle(ChatFormatting.RED));
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.nuke_note_1")
                        .withStyle(ChatFormatting.RED));
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.nuke_note_2")
                        .withStyle(ChatFormatting.RED));
            }
            case "ammo_arty_cargo" -> {
                ItemStack cargo = cargo(stack, context.registries());
                if (cargo.isEmpty()) {
                    tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.cargo.empty")
                            .withStyle(ChatFormatting.RED));
                } else {
                    tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.cargo", cargo.getHoverName())
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
            default -> {
            }
        }
    }

    private static void strength(List<Component> tooltip, int strength) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.strength", strength)
                .withStyle(ChatFormatting.YELLOW));
    }

    private static void damageModifier(List<Component> tooltip, String multiplier) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.damage_modifier", multiplier)
                .withStyle(ChatFormatting.YELLOW));
    }

    private static void breaksBlocks(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.breaks_blocks")
                .withStyle(ChatFormatting.RED));
    }

    private static void noBlockDamage(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.ammo_arty.no_block_damage")
                .withStyle(ChatFormatting.BLUE));
    }

    public static boolean isCargoVariant(ItemStack stack) {
        return stack.getItem() instanceof AmmoArtyItem item && item.variant(stack).modelData() == CARGO_MODEL_DATA;
    }

    public static boolean hasCargo(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(CARGO_TAG);
    }

    public static ItemStack cargo(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(CARGO_TAG)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registries, tag.getCompound(CARGO_TAG));
    }

    public static ItemStack withCargo(ItemStack shell, ItemStack cargo, HolderLookup.Provider registries) {
        if (!isCargoVariant(shell) || cargo.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = shell.copyWithCount(1);
        ItemStack cargoCopy = cargo.copyWithCount(1);
        CompoundTag tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(CARGO_TAG, cargoCopy.saveOptional(registries));
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        result.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(CARGO_FULL_MODEL_DATA));
        return result;
    }
}
