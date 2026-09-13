package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.GasMaskItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The non-firearm portions of 1.7.10 MobUtil's slot pools. MobUtil inserted
 * integer slots 4 through 0 into a HashMap; the old HashMap bucket iteration
 * therefore consumed its random rolls in the concrete 0, 1, 2, 3, 4 order.
 */
public final class LegacyMobEquipment {
    private static final String PROTOCOL_COMPAT_MOD_ID = "reinhardt_protocol_compat";
    private static final Random RANDOM = new Random();
    private static final EquipmentSlot[] LEGACY_SLOT_ITERATION_ORDER = {
            EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private static final Map<EquipmentSlot, Pool> COMMON_SOOT = commonSootPools();
    private static final Map<EquipmentSlot, Pool> COMMON = commonPools();
    private static final Map<EquipmentSlot, Pool> ADVANCED = advancedPools();
    private static final Map<EquipmentSlot, Pool> RANGED_SOOT = rangedSootPools();
    private static final Map<EquipmentSlot, Pool> RANGED = rangedPools();
    private static final Map<EquipmentSlot, Pool> ADVANCED_RANGED = advancedRangedPools();

    private LegacyMobEquipment() {
    }

    /** MobUtil.slotPoolCommonS, used by the old global zombie spawn listener. */
    public static void assignCommonSoot(Mob mob) {
        assign(mob, COMMON_SOOT, RANDOM);
    }

    /** MobUtil.slotPoolCommon, used by the tier-one Logic Block zombie wave. */
    public static void assignCommon(Mob mob) {
        // LogicBlockActions constructed a fresh java.util.Random for every
        // MobUtil.assignItemsToEntity call.
        assign(mob, COMMON, new Random());
    }

    /** MobUtil.slotPoolAdv, used by FODDER_WAVE and tier-two zombie waves. */
    public static void assignAdvanced(Mob mob) {
        // LogicBlockActions constructed a fresh java.util.Random for every
        // MobUtil.assignItemsToEntity call.
        assign(mob, ADVANCED, new Random());
    }

    /** MobUtil.equipFullSet(...hazmat...). */
    public static void equipHazmat(Mob mob) {
        equipWithoutNativeDrop(mob, EquipmentSlot.HEAD, new ItemStack(item("hazmat_helmet")));
        equipWithoutNativeDrop(mob, EquipmentSlot.CHEST, new ItemStack(item("hazmat_plate")));
        equipWithoutNativeDrop(mob, EquipmentSlot.LEGS, new ItemStack(item("hazmat_legs")));
        equipWithoutNativeDrop(mob, EquipmentSlot.FEET, new ItemStack(item("hazmat_boots")));
    }

    /** Assign the legacy ranged skeleton armour pool; HBM combat guns are removed. */
    public static void assignSootRangedSkeleton(Skeleton skeleton) {
        assign(skeleton, RANGED_SOOT, RANDOM);
    }

    /** The three discrete LogicBlockActions skeleton equipment branches. */
    public static void assignLogicSkeletonTier(Skeleton skeleton, int tier) {
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("Unknown 1.7.10 skeleton equipment tier " + tier);
        }
        // LogicBlockActions created a fresh Random for each MobUtil call.
        assign(skeleton, tier == 3 ? ADVANCED_RANGED : RANGED, new Random());
    }

    private static void assign(Mob mob, Map<EquipmentSlot, Pool> pools, Random random) {
        for (EquipmentSlot slot : LEGACY_SLOT_ITERATION_ORDER) {
            Pool pool = pools.get(slot);
            if (pool == null) {
                continue;
            }
            apply(mob, slot, pool.choose(random));
        }
    }

    private static void apply(Mob mob, EquipmentSlot slot, String id) {
        if (id == null) {
            return;
        }
        ItemStack stack = new ItemStack(item(id));
        if (id.equals("gas_mask_m65") || id.equals("gas_mask_olde") || id.equals("gas_mask_mono")) {
            if (!GasMaskItem.installFilter(stack, new ItemStack(HbmItems.GAS_MASK_FILTER.get()), mob)) {
                throw new IllegalStateException("Could not install the legacy gas-mask filter on " + id);
            }
        }
        equipWithoutNativeDrop(mob, slot, stack);
    }

    /**
     * HBM owns the spawn/equipment pool, while the compatibility layer owns
     * the equipment reward roll when it is installed.  Keeping the stack
     * equipped but setting its native chance to zero lets the compat layer
     * read installed filters and armor-module data during LivingDropsEvent.
     * Without the compatibility mod this helper leaves HBM's native behavior
     * untouched.
     */
    private static void equipWithoutNativeDrop(Mob mob, EquipmentSlot slot, ItemStack stack) {
        mob.setItemSlot(slot, stack);
        if (isProtocolCompatLoaded()) {
            mob.setDropChance(slot, 0.0F);
        }
    }

    private static boolean isProtocolCompatLoaded() {
        return ModList.get().isLoaded(PROTOCOL_COMPAT_MOD_ID);
    }

    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ReinhardtsHBM.MOD_ID, id));
        if (item == net.minecraft.world.item.Items.AIR) {
            throw new IllegalStateException("Missing required 1.7.10 mob-pool item " + ReinhardtsHBM.MOD_ID + ":" + id);
        }
        return item;
    }

    private static Map<EquipmentSlot, Pool> commonSootPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(8000, entries(
                "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "cobalt_helmet", 2, "rag_piss", 1, "hat", 1,
                "alloy_helmet", 2, "titanium_helmet", 4, "steel_helmet", 8
        )));
        pools.put(EquipmentSlot.CHEST, pool(7000, entries(
                "starmetal_plate", 1, "cobalt_plate", 2, "robes_plate", 32, "jackt", 32, "jackt2", 32,
                "alloy_plate", 2, "steel_plate", 2
        )));
        pools.put(EquipmentSlot.LEGS, pool(7000, entries(
                "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32, "alloy_legs", 2
        )));
        pools.put(EquipmentSlot.FEET, pool(7000, entries(
                "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "alloy_boots", 2
        )));
        pools.put(EquipmentSlot.MAINHAND, pool(10000, entries(
                "weapon_pipe_lead", 30, "crowbar", 25, "geiger_counter", 20, "reer_graar", 16,
                "steel_pickaxe", 12, "stopsign", 10, "sopsign", 8, "chernobylsign", 6, "steel_sword", 15,
                "titanium_sword", 8, "lead_gavel", 4, "wrench_flipped", 2, "wrench", 20
        )));
        return pools;
    }

    private static Map<EquipmentSlot, Pool> commonPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(0, entries(
                "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "cobalt_helmet", 2, "rag_piss", 1, "hat", 1,
                "alloy_helmet", 2, "titanium_helmet", 4, "steel_helmet", 8
        )));
        pools.put(EquipmentSlot.CHEST, pool(10, entries(
                "starmetal_plate", 1, "cobalt_plate", 2, "robes_plate", 32, "jackt", 32, "jackt2", 32,
                "alloy_plate", 2, "steel_plate", 2
        )));
        pools.put(EquipmentSlot.LEGS, pool(20, entries(
                "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32, "alloy_legs", 2
        )));
        pools.put(EquipmentSlot.FEET, pool(10, entries(
                "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "alloy_boots", 2
        )));
        pools.put(EquipmentSlot.MAINHAND, pool(1000, entries(
                "weapon_pipe_lead", 30, "crowbar", 25, "geiger_counter", 20, "reer_graar", 16,
                "steel_pickaxe", 12, "stopsign", 10, "sopsign", 8, "chernobylsign", 6, "steel_sword", 15,
                "titanium_sword", 8, "lead_gavel", 4, "wrench_flipped", 2, "wrench", 20
        )));
        return pools;
    }

    private static Map<EquipmentSlot, Pool> advancedPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(0, entries(
                "security_helmet", 10, "t51_helmet", 4, "asbestos_helmet", 12, "liquidator_helmet", 4,
                "no9", 12, "hazmat_helmet", 6
        )));
        pools.put(EquipmentSlot.CHEST, pool(0, entries(
                "liquidator_plate", 4, "security_plate", 8, "asbestos_plate", 12, "t51_plate", 4,
                "hazmat_plate", 6, "steel_plate", 8
        )));
        pools.put(EquipmentSlot.LEGS, pool(0, entries(
                "liquidator_legs", 4, "security_legs", 8, "asbestos_legs", 12, "t51_legs", 4,
                "hazmat_legs", 6, "steel_legs", 8
        )));
        pools.put(EquipmentSlot.FEET, pool(0, entries(
                "liquidator_boots", 4, "security_boots", 8, "asbestos_boots", 12, "t51_boots", 4,
                "hazmat_boots", 6, "robes_boots", 8
        )));
        pools.put(EquipmentSlot.MAINHAND, pool(500, entries(
                "weapon_pipe_lead", 20, "crowbar", 10, "geiger_counter", 10, "reer_graar", 20,
                "wrench_flipped", 20, "stopsign", 16, "sopsign", 4, "chernobylsign", 16,
                "titanium_sword", 18, "lead_gavel", 8, "wrench", 20
        )));
        return pools;
    }

    private static Map<EquipmentSlot, Pool> rangedSootPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(8000, entries(
                "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "rag_piss", 1, "goggles", 1, "alloy_helmet", 2,
                "titanium_helmet", 4, "steel_helmet", 8
        )));
        pools.put(EquipmentSlot.CHEST, pool(7000, entries(
                "starmetal_plate", 1, "cobalt_plate", 2, "alloy_plate", 2, "steel_plate", 8, "titanium_plate", 4
        )));
        pools.put(EquipmentSlot.LEGS, pool(7000, entries(
                "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32, "alloy_legs", 2
        )));
        pools.put(EquipmentSlot.FEET, pool(10000, entries(
                "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "alloy_boots", 2, "titanium_boots", 6
        )));
        return pools;
    }

    private static Map<EquipmentSlot, Pool> rangedPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>();
        pools.put(EquipmentSlot.HEAD, pool(0, entries(
                "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "rag_piss", 1, "goggles", 1, "alloy_helmet", 2,
                "titanium_helmet", 4, "steel_helmet", 8
        )));
        pools.put(EquipmentSlot.CHEST, pool(10, entries(
                "starmetal_plate", 1, "cobalt_plate", 2, "alloy_plate", 2, "steel_plate", 8, "titanium_plate", 4
        )));
        pools.put(EquipmentSlot.LEGS, pool(10, entries(
                "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32, "alloy_legs", 2
        )));
        pools.put(EquipmentSlot.FEET, pool(10, entries(
                "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "alloy_boots", 2, "titanium_boots", 6
        )));
        return pools;
    }

    /** slotPoolAdvRanged was a copy of slotPoolAdv with only slot 0 removed. */
    private static Map<EquipmentSlot, Pool> advancedRangedPools() {
        Map<EquipmentSlot, Pool> pools = new HashMap<>(ADVANCED);
        pools.remove(EquipmentSlot.MAINHAND);
        return pools;
    }

    private static Pool pool(int emptyWeight, List<WeightedId> entries) {
        return new Pool(emptyWeight, entries);
    }

    private static List<WeightedId> entries(Object... values) {
        java.util.ArrayList<WeightedId> entries = new java.util.ArrayList<>(values.length / 2);
        for (int index = 0; index < values.length; index += 2) {
            entries.add(new WeightedId((String) values[index], (Integer) values[index + 1]));
        }
        return List.copyOf(entries);
    }

    private record WeightedId(String id, int weight) {
    }

    private record Pool(int emptyWeight, List<WeightedId> entries) {
        private String choose(Random random) {
            int total = emptyWeight;
            for (WeightedId entry : entries) {
                total += entry.weight;
            }
            int roll = random.nextInt(total);
            if (roll < emptyWeight) {
                return null;
            }
            roll -= emptyWeight;
            for (WeightedId entry : entries) {
                if (roll < entry.weight) {
                    return entry.id;
                }
                roll -= entry.weight;
            }
            throw new IllegalStateException("Legacy weighted mob-pool roll escaped its configured range");
        }
    }
}
