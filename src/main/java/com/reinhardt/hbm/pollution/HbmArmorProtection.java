package com.reinhardt.hbm.pollution;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.GasMaskItem;
import com.reinhardt.hbm.item.FilterableGasMask;
import com.reinhardt.hbm.item.ArmorModItem;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HbmArmorProtection {
    private static final Set<HazardClass> FULL_NO_LIGHT = EnumSet.of(
            HazardClass.PARTICLE_COARSE,
            HazardClass.PARTICLE_FINE,
            HazardClass.GAS_LUNG,
            HazardClass.BACTERIA,
            HazardClass.GAS_BLISTERING,
            HazardClass.GAS_MONOXIDE,
            HazardClass.SAND
    );
    private static final Set<HazardClass> FULL_PACKAGE = EnumSet.of(
            HazardClass.PARTICLE_COARSE,
            HazardClass.PARTICLE_FINE,
            HazardClass.GAS_LUNG,
            HazardClass.BACTERIA,
            HazardClass.GAS_BLISTERING,
            HazardClass.GAS_MONOXIDE,
            HazardClass.SAND,
            HazardClass.LIGHT
    );
    private static final Map<String, Set<HazardClass>> HEAD_PROTECTION = Map.ofEntries(
            entry("gas_mask", EnumSet.of(HazardClass.SAND, HazardClass.LIGHT)),
            entry("gas_mask_m65", EnumSet.of(HazardClass.SAND)),
            entry("mask_rag", EnumSet.of(HazardClass.PARTICLE_COARSE)),
            entry("mask_piss", EnumSet.of(HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG)),
            entry("asbestos_helmet", EnumSet.of(HazardClass.SAND, HazardClass.LIGHT)),
            entry("hazmat_helmet", EnumSet.of(HazardClass.SAND)),
            entry("hazmat_helmet_red", EnumSet.of(HazardClass.SAND)),
            entry("hazmat_helmet_grey", EnumSet.of(HazardClass.SAND)),
            entry("hazmat_paa_helmet", EnumSet.of(HazardClass.SAND, HazardClass.LIGHT)),
            entry("schrabidium_helmet", FULL_PACKAGE),
            entry("euphemium_helmet", FULL_PACKAGE)
    );
    private static final Map<String, Set<HazardClass>> FILTER_PROTECTION = Map.ofEntries(
            entry("gas_mask_filter", EnumSet.of(HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA)),
            entry("gas_mask_filter_mono", EnumSet.of(HazardClass.PARTICLE_COARSE, HazardClass.GAS_MONOXIDE)),
            entry("gas_mask_filter_combo", EnumSet.of(HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA, HazardClass.GAS_MONOXIDE)),
            entry("gas_mask_filter_rag", EnumSet.of(HazardClass.PARTICLE_COARSE)),
            entry("gas_mask_filter_piss", EnumSet.of(HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG))
    );
    private static final List<String> FARADAY_MATERIALS = List.of(
            "chainmail", "iron", "silver", "gold", "platinum", "tin", "lead", "liquidator",
            "schrabidium", "euphemium", "steel", "cmb", "titanium", "alloy", "copper", "bronze",
            "electrum", "t45", "t51", "bj", "starmetal", "hazmat", "rubber", "hev", "ajr", "rpa",
            "spacesuit"
    );

    private HbmArmorProtection() {
    }

    public static boolean hasHeadProtection(LivingEntity entity, HazardClass hazardClass) {
        return hasHeadProtection(entity, hazardClass, 0);
    }

    public static boolean hasHeadProtection(LivingEntity entity, HazardClass hazardClass, int filterDamage) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return false;
        }
        String path = itemPath(head.getItem());
        Set<HazardClass> direct = HEAD_PROTECTION.get(path);
        if (direct != null && direct.contains(hazardClass)) {
            return true;
        }

        if (hasFilterProtection(head, entity, hazardClass, filterDamage)) {
            return true;
        }
        for (ItemStack attachment : ArmorModHandler.pryMods(head, entity.registryAccess())) {
            if (hasFilterProtection(attachment, entity, hazardClass, filterDamage)) {
                return true;
            }
        }
        return false;
    }

    /** Exact 1.7.10 all-four-slots Faraday check used by Tesla damage. */
    public static boolean hasFaradayProtection(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
            return false;
        }
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack armor = entity.getItemBySlot(slot);
            if (armor.isEmpty() || !isFaradayArmor(armor, entity)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFaradayArmor(ItemStack stack, LivingEntity entity) {
        String path = itemPath(stack.getItem());
        for (String material : FARADAY_MATERIALS) {
            if (path.contains(material)) {
                return true;
            }
        }
        for (ItemStack attachment : ArmorModHandler.pryMods(stack, entity.registryAccess())) {
            if (attachment.getItem() instanceof ArmorModItem mod
                    && mod.slotType() == ArmorModHandler.CLADDING) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFilterProtection(ItemStack mask, LivingEntity entity, HazardClass hazardClass, int filterDamage) {
        if (!(mask.getItem() instanceof FilterableGasMask gasMask) || gasMask.blacklist().contains(hazardClass)) {
            return false;
        }
        ItemStack installedFilter = GasMaskItem.getInstalledFilter(mask, entity.registryAccess());
        if (installedFilter.isEmpty()) {
            return false;
        }
        Set<HazardClass> filter = FILTER_PROTECTION.get(itemPath(installedFilter.getItem()));
        if (filter == null || !filter.contains(hazardClass)) {
            return false;
        }
        if (filterDamage > 0) {
            GasMaskItem.damageInstalledFilter(mask, entity, filterDamage);
        }
        return true;
    }

    private static Map.Entry<String, Set<HazardClass>> entry(String id, Set<HazardClass> hazards) {
        return Map.entry(id, Set.copyOf(hazards));
    }

    private static String itemPath(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key.getNamespace().equals(ReinhardtsHBM.MOD_ID) ? key.getPath() : "";
    }

    public enum HazardClass {
        GAS_LUNG,
        GAS_MONOXIDE,
        GAS_INERT,
        PARTICLE_COARSE,
        PARTICLE_FINE,
        BACTERIA,
        GAS_BLISTERING,
        SAND,
        LIGHT
    }
}
