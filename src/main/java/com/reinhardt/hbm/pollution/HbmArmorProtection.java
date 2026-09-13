package com.reinhardt.hbm.pollution;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.GasMaskItem;
import com.reinhardt.hbm.item.FilterableGasMask;
import com.reinhardt.hbm.item.ArmorModItem;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

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
            entry("attachment_mask", EnumSet.of(HazardClass.SAND)),
            entry("mask_rag", EnumSet.of(HazardClass.PARTICLE_COARSE)),
            entry("mask_piss", EnumSet.of(HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG)),
            entry("asbestos_helmet", EnumSet.of(HazardClass.SAND, HazardClass.LIGHT)),
            entry("hazmat_helmet", EnumSet.of(HazardClass.SAND)),
            entry("hazmat_helmet_red", EnumSet.of(HazardClass.SAND)),
            entry("hazmat_helmet_grey", EnumSet.of(HazardClass.SAND)),
            entry("hazmat_paa_helmet", EnumSet.of(HazardClass.SAND, HazardClass.LIGHT)),
            entry("liquidator_helmet", EnumSet.of(HazardClass.SAND, HazardClass.LIGHT)),
            entry("goggles", EnumSet.of(HazardClass.SAND, HazardClass.LIGHT)),
            entry("ashglasses", EnumSet.of(HazardClass.SAND, HazardClass.LIGHT)),
            entry("schrabidium_helmet", FULL_PACKAGE),
            entry("euphemium_helmet", FULL_PACKAGE)
    );
    /**
     * 1.7.10 ArmorFSB hazard registrations.  The old registry attached these
     * entries directly to the helmet item; its hazard lookup did not impose a
     * complete-suit gate.  Suit completeness remains relevant to FSB features
     * (power, movement and radiation resistance), but not this hazard query.
     */
    private static final Map<String, Set<HazardClass>> FULL_SET_PROTECTION = Map.ofEntries(
            entry("t51", FULL_NO_LIGHT),
            entry("steamsuit", FULL_PACKAGE),
            entry("ajr", FULL_PACKAGE),
            entry("ajro", FULL_PACKAGE),
            entry("rpa", FULL_PACKAGE),
            entry("ncrpa", FULL_PACKAGE),
            entry("envsuit", FULL_PACKAGE),
            entry("hev", FULL_PACKAGE),
            entry("fau", FULL_PACKAGE),
            entry("dns", FULL_PACKAGE),
            entry("taurun", FULL_PACKAGE),
            entry("trenchmaster", FULL_PACKAGE)
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
    private static final Map<LivingEntity, RadiationMultiplierCache> RADIATION_MULTIPLIER_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private HbmArmorProtection() {
    }

    public static boolean hasHeadProtection(LivingEntity entity, HazardClass hazardClass) {
        return hasHeadProtectionInternal(entity, hazardClass);
    }

    /**
     * Checks protection and applies the legacy gas-mask filter wear side
     * effect.  In 1.7.10 the two operations were deliberately separate:
     * ArmorRegistry found protection first, then ArmorUtil damaged the
     * installed filter even when protection came from the helmet itself (or
     * from a full suit).  Keeping that order prevents a direct-protection
     * early return from silently making filters indestructible.
     */
    public static boolean hasHeadProtection(LivingEntity entity, HazardClass hazardClass, int filterDamage) {
        boolean protectedFromHazard = hasHeadProtectionInternal(entity, hazardClass);
        if (protectedFromHazard && filterDamage > 0) {
            damageInstalledFilter(entity, filterDamage);
        }
        return protectedFromHazard;
    }

    private static boolean hasHeadProtectionInternal(LivingEntity entity, HazardClass hazardClass) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return false;
        }
        String path = itemPath(head.getItem());
        Set<HazardClass> direct = HEAD_PROTECTION.get(path);
        if (direct != null && direct.contains(hazardClass)) {
            return true;
        }

        // ArmorFSB registers hazard classes on the helmet item itself in
        // 1.7.10. ArmorRegistry did not add an extra full-set gate, so keep
        // that observable legacy behavior here; full-set checks remain for
        // suit bonuses and powered features.
        if (path.endsWith("_helmet")) {
            String group = path.substring(0, path.length() - "_helmet".length());
            Set<HazardClass> fullSet = FULL_SET_PROTECTION.get(group);
            if (fullSet != null && fullSet.contains(hazardClass)) {
                return true;
            }
        }

        if (hasDirectProtectionRecursive(head, entity, hazardClass)) {
            return true;
        }
        if (hasFilterProtection(head, entity, hazardClass)) {
            return true;
        }
        for (ItemStack attachment : ArmorModHandler.pryMods(head, entity.registryAccess())) {
            if (hasDirectProtectionRecursive(attachment, entity, hazardClass)
                    || hasFilterProtection(attachment, entity, hazardClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Damages the filter in the worn gas mask, or in the legacy
     * {@code helmet_only} attachment.  This intentionally mirrors
     * ArmorUtil.damageGasMaskFilter(EntityLivingBase, int): it does not try to
     * infer protection again and therefore is safe to call after a successful
     * hazard check.
     */
    public static void damageInstalledFilter(LivingEntity entity, int damage) {
        if (damage <= 0) {
            return;
        }

        ItemStack mask = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (mask.isEmpty()) {
            return;
        }
        if (!(mask.getItem() instanceof FilterableGasMask)) {
            ItemStack[] mods = ArmorModHandler.pryMods(mask, entity.registryAccess());
            ItemStack helmetOnly = mods[ArmorModHandler.HELMET_ONLY];
            if (helmetOnly.isEmpty() || !(helmetOnly.getItem() instanceof FilterableGasMask)) {
                return;
            }
            mask = helmetOnly;
        }
        GasMaskItem.damageInstalledFilter(mask, entity, damage);
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

    /**
     * Returns the legacy HazmatRegistry resistance value for a player.  The
     * 1.7.10 implementation converted this logarithmically with 10^-resist;
     * keeping the calculation here makes the value available to both chunk
     * radiation and radioactive inventory hazards.
     */
    public static double radiationMultiplier(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return 1.0D;
        }

        long gameTime = player.level().getGameTime();
        int armorHash = armorHash(player);
        RadiationMultiplierCache cached = RADIATION_MULTIPLIER_CACHE.get(player);
        if (cached != null && cached.gameTime == gameTime && cached.armorHash == armorHash) {
            return cached.multiplier;
        }

        double resistance = 0.0D;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack armor = player.getItemBySlot(slot);
            resistance += pieceResistance(armor, slot, player.registryAccess());
        }
        double multiplier = multiplierForResistance(resistance);
        RADIATION_MULTIPLIER_CACHE.put(player, new RadiationMultiplierCache(gameTime, armorHash, multiplier));
        return multiplier;
    }

    /**
     * Returns the exact legacy HazmatRegistry value contributed by this item
     * stack. Armor stacks include their installed radiation cladding; cladding
     * items return their own additive value so their tooltip mirrors the old
     * ItemModCladding description.
     */
    public static double itemRadiationResistance(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return 0.0D;
        }

        double claddingItem = claddingResistance(itemPath(stack.getItem()));
        if (claddingItem > 0.0D) {
            return claddingItem;
        }

        if (!(stack.getItem() instanceof ArmorItem armor)) {
            return 0.0D;
        }
        return pieceResistance(stack, armor.getEquipmentSlot(), registries);
    }

    /**
     * Tooltip-only armor resistance path. Client tooltip contexts can be
     * created while registry-backed ItemStack parsing is not safe yet (JEI,
     * creative search and early reload are the common cases). The tooltip only
     * needs the installed cladding item ids, so read the saved ids directly
     * instead of parsing full ItemStacks.
     */
    public static double itemRadiationTooltipResistance(ItemStack stack) {
        return itemRadiationResistance(stack, null);
    }

    public static double multiplierForResistance(double resistance) {
        return Math.pow(10.0D, -Math.max(0.0D, resistance));
    }

    private static double pieceResistance(ItemStack stack, EquipmentSlot slot, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        String path = itemPath(stack.getItem());
        double coefficient = slotCoefficient(slot);

        double fullSet = fullSetResistance(path);
        if (fullSet > 0.0D) {
            return fullSet * coefficient + installedCladdingResistance(stack, registries);
        }

        double base = vanillaResistance(stack.getItem()) * coefficient;
        if (path.startsWith("hazmat_helmet_red") || path.startsWith("hazmat_plate_red")
                || path.startsWith("hazmat_legs_red") || path.startsWith("hazmat_boots_red")) {
            base = 1.0D * coefficient;
        } else if (path.startsWith("hazmat_helmet_grey") || path.startsWith("hazmat_plate_grey")
                || path.startsWith("hazmat_legs_grey") || path.startsWith("hazmat_boots_grey")) {
            base = 2.0D * coefficient;
        } else if (path.startsWith("hazmat_paa_") || path.startsWith("paa_")) {
            base = 1.7D * coefficient;
        } else if (path.startsWith("hazmat_")) {
            base = 0.6D * coefficient;
        } else if (path.startsWith("liquidator_")) {
            base = 2.4D * coefficient;
        } else if (path.startsWith("security_")) {
            base = 0.825D * coefficient;
        } else if (path.startsWith("starmetal_")) {
            base = 1.0D * coefficient;
        } else if (path.startsWith("steel_")) {
            base = 0.045D * coefficient;
        } else if (path.startsWith("titanium_")) {
            base = 0.045D * coefficient;
        } else if (path.startsWith("alloy_")) {
            base = 0.07D * coefficient;
        } else if (path.startsWith("cobalt_")) {
            base = 0.125D * coefficient;
        } else if (path.startsWith("cmb_")) {
            base = 1.3D * coefficient;
        } else if (path.startsWith("schrabidium_")) {
            base = 3.0D * coefficient;
        } else if (path.startsWith("euphemium_")) {
            base = 10.0D * coefficient;
        } else if (path.equals("gas_mask")) {
            base = 0.07D;
        } else if (path.equals("gas_mask_m65")) {
            base = 0.095D;
        } else if (path.equals("jackt") || path.equals("jackt2")) {
            base = 0.1D;
        }

        return base + installedCladdingResistance(stack, registries);
    }

    private static double fullSetResistance(String path) {
        if (path.startsWith("t51_")) return 1.0D;
        if (path.startsWith("steamsuit_")) return 1.3D;
        if (path.startsWith("ajr_")) return 1.3D;
        if (path.startsWith("ajro_")) return 1.3D;
        if (path.startsWith("rpa_")) return 2.0D;
        if (path.startsWith("ncrpa_")) return 1.7D;
        if (path.startsWith("bj_")) return 1.0D;
        if (path.startsWith("envsuit_")) return 1.0D;
        if (path.startsWith("hev_")) return 2.3D;
        if (path.startsWith("fau_")) return 4.0D;
        if (path.startsWith("dns_")) return 5.0D;
        if (path.startsWith("taurun_")) return 0.125D;
        if (path.startsWith("trenchmaster_")) return 1.0D;
        return 0.0D;
    }

    private static double installedCladdingResistance(ItemStack stack, HolderLookup.Provider registries) {
        double legacyNbt = legacyNbtCladdingResistance(stack);
        if (legacyNbt > 0.0D) {
            return legacyNbt;
        }
        if (registries == null) {
            return savedCladdingResistance(stack);
        }

        double resistance = 0.0D;
        for (ItemStack attachment : ArmorModHandler.pryMods(stack, registries)) {
            resistance += claddingResistance(itemPath(attachment.getItem()));
        }
        return resistance;
    }

    private static double savedCladdingResistance(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag mods = root.getCompound(ArmorModHandler.MOD_COMPOUND_KEY);
        double resistance = 0.0D;
        for (int slot = 0; slot < ArmorModHandler.MOD_SLOTS; slot++) {
            CompoundTag saved = mods.getCompound(ArmorModHandler.MOD_SLOT_KEY + slot);
            if (saved.isEmpty()) {
                continue;
            }
            resistance += claddingResistance(savedHbmItemPath(saved));
        }
        return resistance;
    }

    private static String savedHbmItemPath(CompoundTag savedStack) {
        String id = savedStack.getString("id");
        int separator = id.indexOf(':');
        if (separator <= 0 || separator >= id.length() - 1) {
            return "";
        }
        return id.substring(0, separator).equals(ReinhardtsHBM.MOD_ID) ? id.substring(separator + 1) : "";
    }

    private static double legacyNbtCladdingResistance(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        float resistance = root.getFloat("hfr_cladding");
        return resistance > 0.0F ? resistance : 0.0D;
    }

    private static double claddingResistance(String path) {
        return switch (path) {
            case "cladding_paint" -> 0.025D;
            case "cladding_rubber" -> 0.005D;
            case "cladding_lead" -> 0.1D;
            case "cladding_desh" -> 0.2D;
            case "cladding_ghiorsium" -> 0.5D;
            default -> 0.0D;
        };
    }

    private static int armorHash(Player player) {
        int hash = 1;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                hash = 31 * hash;
                continue;
            }
            hash = 31 * hash + System.identityHashCode(stack.getItem());
            hash = 31 * hash + stack.getDamageValue();
            hash = 31 * hash + stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).hashCode();
        }
        return hash;
    }

    private record RadiationMultiplierCache(long gameTime, int armorHash, double multiplier) {
    }

    private static double vanillaResistance(Item item) {
        if (item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE
                || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) {
            return 0.0225D;
        }
        if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE
                || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) {
            return 0.0225D;
        }
        return 0.0D;
    }

    private static double slotCoefficient(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0.2D;
            case CHEST -> 0.4D;
            case LEGS -> 0.3D;
            case FEET -> 0.1D;
            default -> 0.0D;
        };
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

    private static boolean hasFilterProtection(ItemStack mask, LivingEntity entity, HazardClass hazardClass) {
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
        return true;
    }

    /** Mirrors ArmorRegistry.getProtectionFromItem's recursive direct entries. */
    private static boolean hasDirectProtectionRecursive(ItemStack stack, LivingEntity entity, HazardClass hazardClass) {
        if (stack.isEmpty()) {
            return false;
        }
        Set<HazardClass> direct = HEAD_PROTECTION.get(itemPath(stack.getItem()));
        if (direct != null && direct.contains(hazardClass)) {
            return true;
        }
        for (ItemStack attachment : ArmorModHandler.pryMods(stack, entity.registryAccess())) {
            if (hasDirectProtectionRecursive(attachment, entity, hazardClass)) {
                return true;
            }
        }
        return false;
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
