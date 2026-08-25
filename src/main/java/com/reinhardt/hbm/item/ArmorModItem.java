package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ArmorModItem extends Item {
    private final int slotType;
    private final boolean helmet;
    private final boolean chestplate;
    private final boolean leggings;
    private final boolean boots;

    public ArmorModItem(Properties properties, int slotType, boolean helmet, boolean chestplate, boolean leggings, boolean boots) {
        super(properties.stacksTo(1));
        this.slotType = slotType;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    public static ArmorModItem helmet(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
    }

    public static ArmorModItem servos(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.SERVOS, false, true, true, false);
    }

    public static ArmorModItem cladding(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.CLADDING, true, true, true, true);
    }

    public static ArmorModItem kevlar(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.KEVLAR, false, true, false, false);
    }

    public static ArmorModItem battery(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.BATTERY, true, true, true, true);
    }

    public int slotType() {
        return this.slotType;
    }

    public boolean appliesTo(net.minecraft.world.item.ArmorItem.Type armorType) {
        return switch (armorType) {
            case HELMET -> this.helmet;
            case CHESTPLATE -> this.chestplate;
            case LEGGINGS -> this.leggings;
            case BOOTS -> this.boots;
            default -> false;
        };
    }

    /**
     * Direct equivalent of 1.7.10 ItemArmorMod#modUpdate. The armor event
     * dispatches this once for every installed modifier on the server.
     */
    public void tickArmor(Player player, ItemStack armor) {
    }

    /** Direct equivalent of 1.7.10 ItemArmorMod#modDamage. */
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        return amount;
    }

    /** Attribute contribution calculated once per player tick from installed mods. */
    public double extraHealth() {
        return 0.0D;
    }

    /** Equivalent to the old operation-2 movement-speed modifier. */
    public double movementMultiplier() {
        return 1.0D;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(ArmorModHandler.slotTranslationKey(this.slotType)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
