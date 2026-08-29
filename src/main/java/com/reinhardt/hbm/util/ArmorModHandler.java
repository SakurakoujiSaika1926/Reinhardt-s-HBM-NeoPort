package com.reinhardt.hbm.util;

import com.reinhardt.hbm.item.ArmorModItem;
import com.reinhardt.hbm.item.HealthArmorModItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Arrays;

public final class ArmorModHandler {
    public static final int HELMET_ONLY = 0;
    public static final int PLATE_ONLY = 1;
    public static final int LEGS_ONLY = 2;
    public static final int BOOTS_ONLY = 3;
    public static final int SERVOS = 4;
    public static final int CLADDING = 5;
    public static final int KEVLAR = 6;
    public static final int EXTRA = 7;
    public static final int BATTERY = 8;
    public static final int MOD_SLOTS = 9;

    public static final String MOD_COMPOUND_KEY = "ntm_armor_mods";
    public static final String MOD_SLOT_KEY = "mod_slot_";

    private ArmorModHandler() {
    }

    public static boolean isApplicable(ItemStack armor, ItemStack mod) {
        if (armor.isEmpty() || mod.isEmpty()) {
            return false;
        }
        if (!(armor.getItem() instanceof ArmorItem armorItem)) {
            return false;
        }
        if (!(mod.getItem() instanceof ArmorModItem armorMod)) {
            return false;
        }
        return armorMod.appliesTo(armorItem.getType());
    }

    public static void applyMod(ItemStack armor, ItemStack mod, HolderLookup.Provider registries) {
        if (!isApplicable(armor, mod) || !(mod.getItem() instanceof ArmorModItem armorMod)) {
            return;
        }
        CompoundTag root = armor.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag mods = root.getCompound(MOD_COMPOUND_KEY);
        mods.put(MOD_SLOT_KEY + armorMod.slotType(), mod.copyWithCount(1).saveOptional(registries));
        root.put(MOD_COMPOUND_KEY, mods);
        armor.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static void removeMod(ItemStack armor, int slot) {
        if (armor.isEmpty()) {
            return;
        }
        CompoundTag root = armor.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(MOD_COMPOUND_KEY)) {
            return;
        }
        CompoundTag mods = root.getCompound(MOD_COMPOUND_KEY);
        mods.remove(MOD_SLOT_KEY + slot);
        if (mods.isEmpty()) {
            root.remove(MOD_COMPOUND_KEY);
        } else {
            root.put(MOD_COMPOUND_KEY, mods);
        }
        if (root.isEmpty()) {
            armor.remove(DataComponents.CUSTOM_DATA);
        } else {
            armor.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    public static ItemStack[] pryMods(ItemStack armor, HolderLookup.Provider registries) {
        ItemStack[] slots = new ItemStack[MOD_SLOTS];
        Arrays.fill(slots, ItemStack.EMPTY);
        if (armor.isEmpty()) {
            return slots;
        }
        CompoundTag root = armor.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(MOD_COMPOUND_KEY)) {
            return slots;
        }
        CompoundTag mods = root.getCompound(MOD_COMPOUND_KEY);
        boolean changed = false;
        for (int slot = 0; slot < MOD_SLOTS; slot++) {
            CompoundTag saved = mods.getCompound(MOD_SLOT_KEY + slot);
            if (saved.isEmpty()) {
                slots[slot] = ItemStack.EMPTY;
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(registries, saved);
            slots[slot] = stack;
            if (stack.isEmpty()) {
                mods.remove(MOD_SLOT_KEY + slot);
                changed = true;
            }
        }
        if (changed) {
            if (mods.isEmpty()) {
                root.remove(MOD_COMPOUND_KEY);
            } else {
                root.put(MOD_COMPOUND_KEY, mods);
            }
            if (root.isEmpty()) {
                armor.remove(DataComponents.CUSTOM_DATA);
            } else {
                armor.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            }
        }
        return slots;
    }

    /** Returns the old ItemModHealth bonuses installed in the extra slots. */
    public static double healthBonus(Iterable<ItemStack> armor, HolderLookup.Provider registries) {
        double result = 0.0D;
        for (ItemStack stack : armor) {
            CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            CompoundTag mods = root.getCompound(MOD_COMPOUND_KEY);
            CompoundTag saved = mods.getCompound(MOD_SLOT_KEY + EXTRA);
            if (saved.isEmpty()) {
                continue;
            }
            ItemStack mod = ItemStack.parseOptional(registries, saved);
            if (mod.getItem() instanceof ArmorModItem armorMod) {
                result += armorMod.extraHealth();
            }
        }
        return result;
    }

    public static String slotTranslationKey(int slot) {
        return switch (slot) {
            case HELMET_ONLY -> "armor_mod.reinhardtshbm.type.helmet";
            case PLATE_ONLY -> "armor_mod.reinhardtshbm.type.chestplate";
            case LEGS_ONLY -> "armor_mod.reinhardtshbm.type.leggings";
            case BOOTS_ONLY -> "armor_mod.reinhardtshbm.type.boots";
            case SERVOS -> "armor_mod.reinhardtshbm.type.servo";
            case CLADDING -> "armor_mod.reinhardtshbm.type.cladding";
            case KEVLAR -> "armor_mod.reinhardtshbm.type.insert";
            case EXTRA -> "armor_mod.reinhardtshbm.type.special";
            case BATTERY -> "armor_mod.reinhardtshbm.type.battery";
            default -> "armor_mod.reinhardtshbm.type.unknown";
        };
    }
}
