package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ArmorFSBItem extends ArmorItem {
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private final String fsbGroup;
    private final boolean noHelmet;
    private final List<FullSetEffect> fullSetEffects;

    public ArmorFSBItem(
            String fsbGroup,
            Holder<ArmorMaterial> material,
            Type type,
            boolean noHelmet,
            List<FullSetEffect> fullSetEffects,
            Properties properties
    ) {
        super(material, type, properties);
        this.fsbGroup = fsbGroup;
        this.noHelmet = noHelmet;
        this.fullSetEffects = List.copyOf(fullSetEffects);
    }

    public static FullSetEffect effect(Holder<MobEffect> effect, int duration, int amplifier) {
        return new FullSetEffect(effect, duration, amplifier);
    }

    public static boolean hasFSBArmor(Player player) {
        return hasFSBArmor(player, true);
    }

    /** Matches 1.7.10's hasFSBArmorIgnoreCharge(), used by HEV battery blocks. */
    public static boolean hasFSBArmorIgnoringCharge(Player player) {
        return hasFSBArmor(player, false);
    }

    private static boolean hasFSBArmor(Player player, boolean requireEnabled) {
        if (player == null) {
            return false;
        }

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ArmorFSBItem chestplate)) {
            return false;
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (chestplate.noHelmet && slot == EquipmentSlot.HEAD) {
                continue;
            }

            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorFSBItem armor)) {
                return false;
            }
            if (!armor.fsbGroup.equals(chestplate.fsbGroup)) {
                return false;
            }
            if (requireEnabled && !armor.isArmorEnabled(stack)) {
                return false;
            }
        }
        return true;
    }

    public static String fullSetGroup(LivingEntity living) {
        String group = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = living.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorFSBItem armor) || armor.noHelmet) {
                return "";
            }
            if (group == null) {
                group = armor.fsbGroup;
            } else if (!group.equals(armor.fsbGroup)) {
                return "";
            }
        }
        return group == null ? "" : group;
    }

    public static boolean hasFSBArmorHelmet(Player player) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        return chestStack.getItem() instanceof ArmorFSBItem chestplate
                && !chestplate.noHelmet
                && hasFSBArmor(player);
    }

    public static void tickFullSet(Player player) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ArmorFSBItem chestplate) || !hasFSBArmor(player)) {
            return;
        }

        for (FullSetEffect effect : chestplate.fullSetEffects) {
            player.addEffect(new MobEffectInstance(
                    effect.effect(),
                    effect.duration(),
                    effect.amplifier(),
                    false,
                    false
            ));
        }
    }

    /**
     * Fuelled and powered 1.7.10 suits only enabled their set bonuses while
     * every equipped component still had usable fuel or charge.
     */
    public boolean isArmorEnabled(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.fullSetEffects.isEmpty()) {
            return;
        }

        tooltip.add(Component.translatable("armor.reinhardtshbm.full_set_bonus").withStyle(ChatFormatting.GOLD));
        for (FullSetEffect effect : this.fullSetEffects) {
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable(effect.effect().value().getDescriptionId()))
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    public record FullSetEffect(Holder<MobEffect> effect, int duration, int amplifier) {
    }
}
