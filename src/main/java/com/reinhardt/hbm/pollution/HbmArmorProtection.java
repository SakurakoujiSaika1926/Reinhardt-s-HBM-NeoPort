package com.reinhardt.hbm.pollution;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.GasMaskItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
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

        if (!(head.getItem() instanceof GasMaskItem gasMask) || gasMask.blacklist().contains(hazardClass)) {
            return false;
        }

        ItemStack installedFilter = GasMaskItem.getInstalledFilter(head, entity.registryAccess());
        if (installedFilter.isEmpty()) {
            return false;
        }
        Set<HazardClass> filter = FILTER_PROTECTION.get(itemPath(installedFilter.getItem()));
        if (filter == null || !filter.contains(hazardClass)) {
            return false;
        }
        if (filterDamage > 0) {
            GasMaskItem.damageInstalledFilter(head, entity, filterDamage);
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
