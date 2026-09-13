package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.BlueprintFolderItem;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.LegacyBombCallerItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.RtgDepletedPelletItem;
import com.reinhardt.hbm.item.StampBookItem;
import com.reinhardt.hbm.item.UniversalGrenadeItem;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HbmStructureLoot {
    private static final Map<String, List<Entry>> POOLS = new LinkedHashMap<>();
    private static final List<Entry> BACKUP_POOL = List.of(
            new Entry("minecraft:bread", 1, 3, 10),
            new Entry("minecraft:stick", 2, 5, 10),
            new Entry("reinhardtshbm:scrap", 1, 3, 10),
            new Entry("reinhardtshbm:dust", 2, 5, 5)
    );

    static {
        registerPilePools();
        registerCommonContainerPools();
    }

    private HbmStructureLoot() {
    }

    public static void applyPileLoot(DecoLootBlockEntity loot, String pool, RandomSource random) {
        if (!loot.items().isEmpty()) {
            return;
        }

        List<String> names = lootNames();
        int start = names.indexOf(pool);
        if (start < 0) {
            start = names.indexOf("LOOT_BONES");
        }

        for (int i = Math.max(0, start); i < names.size(); i++) {
            applyVisiblePile(loot, names.get(i), random);
            if (!loot.items().isEmpty()) {
                return;
            }
        }
        lootBones(loot, random);
    }

    /**
     * Fills a generated inventory using the legacy wand item-count range
     * (minimum inclusive, maximum exclusive), while each pool entry keeps its
     * own legacy inclusive stack-size range.
     * This is public because procedural (non-NBT) structure pieces place their
     * containers directly and must use the same validated pools as wand_loot.
     */
    public static void fillContainer(Container container, String pool, int min, int max, RandomSource random) {
        int count = Math.max(0, min);
        if (max > min) {
            count += random.nextInt(max - min);
        }
        for (int i = 0; i < count; i++) {
            ItemStack stack = randomStack(pool, random);
            if (!insert(container, stack)) {
                return;
            }
        }
    }

    private static void applyVisiblePile(DecoLootBlockEntity loot, String pool, RandomSource random) {
        switch (pool) {
            case "LOOT_BOOKLET" -> loot.addItem(stack("minecraft:book", 1), 0.0D, 0.0D, 0.0D);
            case "LOOT_CAPNUKE" -> {
                loot.addItem(random.nextInt(5) == 0 ? stack("reinhardtshbm:ammo_standard", 1) : stack("reinhardtshbm:bomb_caller", 1), -0.25D, 0.0D, -0.125D);
                for (int i = 0; i < 4; i++) {
                    addItemWithDeviation(loot, random, stack("reinhardtshbm:cap_nuka", 2), 0.125D, i * 0.03125D, 0.25D);
                }
                for (int i = 0; i < 2; i++) {
                    addItemWithDeviation(loot, random, stack("reinhardtshbm:syringe_metal_stimpak", 1), -0.25D, i * 0.03125D, 0.25D);
                }
                for (int i = 0; i < 6; i++) {
                    addItemWithDeviation(loot, random, stack("reinhardtshbm:cap_nuka", 2), 0.125D, i * 0.03125D, -0.25D);
                }
            }
            case "LOOT_MEDICINE" -> {
                for (int i = 0; i < 4; i++) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MED_SYRINGE", random), 0.125D, i * 0.03125D, 0.25D);
                }
                addItemWithDeviation(loot, random, randomStack("POOL_PILE_MED_PILLS", random), -0.25D, 0.0D, -0.125D);
            }
            case "LOOT_CAPSTASH" -> {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        int count = random.nextInt(5) + 3;
                        for (int i = 0; i < count; i++) {
                            addItemWithDeviation(loot, random, randomStack("POOL_PILE_CAPS", random), x * 0.3125D, i * 0.03125D, z * 0.3125D);
                        }
                    }
                }
            }
            case "LOOT_MAKESHIFT_GUN" -> {
                // The legacy firearm formerly found in this pile is removed;
                // retain the mechanical salvage portion of the structure loot.
                addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_WRENCH", random), -0.25D, 0.0D, -0.28125D);
                int count = random.nextInt(2) + 1;
                for (int i = 0; i < count; i++) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_PLATES", random), -0.25D, i * 0.03125D, 0.3125D);
                }
                count = random.nextInt(2) + 2;
                for (int i = 0; i < count; i++) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_WIRE", random), 0.25D, i * 0.03125D, 0.1875D);
                }
            }
            case "LOOT_NUKE_STORAGE" -> {
                for (int x = 0; x < 4; x++) {
                    for (int z = 0; z < 4; z++) {
                        if (random.nextBoolean()) {
                            loot.addItem(randomStack("POOL_PILE_NUKE_STORAGE", random), -0.375D + x * 0.25D, 0.0D, -0.375D + z * 0.25D);
                        }
                    }
                }
            }
            case "LOOT_BONES" -> lootBones(loot, random);
            case "LOOT_GLYPHID_HIVE" -> pile(loot, "POOL_PILE_HIVE", random, random.nextInt(3) + 3);
            case "LOOT_METEOR" -> {
                addItemWithDeviation(loot, random, stack("reinhardtshbm:egg_glyphid", 1), 0.0D, 0.0D, 0.25D);
                addItemWithDeviation(loot, random, stack("minecraft:book", 1), 0.0D, 0.0D, -0.25D);
            }
            case "LOOT_FLAREGUN" -> {
                int count = random.nextInt(3) + 2;
                for (int i = 0; i < count; i++) {
                    addItemWithDeviation(loot, random, stack("reinhardtshbm:ammo_standard", 1), -0.25D, i * 0.03125D, 0.25D);
                }
                addItemWithDeviation(loot, random, stack("reinhardtshbm:ammo_standard", 1), 0.25D, 0.0D, 0.125D);
            }
            case "LOOT_SHIT" -> pile(loot, "POOL_PILE_OF_GARBAGE", random, random.nextInt(3) + 3);
            case "LOOT_MECHANICAL" -> pile(loot, "POOL_PILE_MECHANICAL", random, random.nextInt(6) + 1);
            case "LOOT_GEAR" -> pile(loot, "POOL_PILE_GEAR", random, random.nextInt(6) + 1);
            default -> lootBones(loot, random);
        }
    }

    private static void lootBones(DecoLootBlockEntity loot, RandomSource random) {
        pile(loot, "POOL_PILE_BONES", random, random.nextInt(3) + 3);
    }

    private static void pile(DecoLootBlockEntity loot, String pool, RandomSource random, int count) {
        for (int i = 0; i < count; i++) {
            addItemWithDeviation(loot, random, randomStack(pool, random), random.nextDouble() - 0.5D, i * 0.03125D, random.nextDouble() - 0.5D);
        }
    }

    private static void addItemWithDeviation(DecoLootBlockEntity loot, RandomSource random, ItemStack stack, double x, double y, double z) {
        loot.addItem(stack, x + random.nextGaussian() * 0.02D, y, z + random.nextGaussian() * 0.02D);
    }

    public static ItemStack randomStack(String pool, RandomSource random) {
        List<Entry> entries = availablePool(pool);
        int total = entries.stream().mapToInt(Entry::weight).sum();
        int roll = random.nextInt(Math.max(1, total));
        for (Entry entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry.stack(random);
            }
        }
        return entries.get(0).stack(random);
    }

    private static List<Entry> availablePool(String pool) {
        List<Entry> entries = POOLS.get(pool);
        if (entries == null) {
            entries = BACKUP_POOL;
        }
        List<Entry> available = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.exists()) {
                available.add(entry);
            }
        }
        if (!available.isEmpty()) {
            return available;
        }
        return BACKUP_POOL;
    }

    private static ItemStack stack(String id, int count) {
        Entry entry = new Entry(id, count, count, 1);
        return entry.exists() ? entry.stack(null) : ItemStack.EMPTY;
    }

    private static boolean insert(Container container, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.canPlaceItem(slot, remaining)) {
                continue;
            }
            ItemStack inSlot = container.getItem(slot);
            if (inSlot.isEmpty()) {
                container.setItem(slot, remaining);
                return true;
            }
            if (ItemStack.isSameItemSameComponents(inSlot, remaining) && inSlot.getCount() < inSlot.getMaxStackSize()) {
                int move = Math.min(remaining.getCount(), inSlot.getMaxStackSize() - inSlot.getCount());
                inSlot.grow(move);
                remaining.shrink(move);
                container.setChanged();
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    static List<String> lootNames() {
        return List.of(
                "LOOT_BOOKLET",
                "LOOT_CAPNUKE",
                "LOOT_MEDICINE",
                "LOOT_CAPSTASH",
                "LOOT_MAKESHIFT_GUN",
                "LOOT_NUKE_STORAGE",
                "LOOT_BONES",
                "LOOT_GLYPHID_HIVE",
                "LOOT_METEOR",
                "LOOT_FLAREGUN",
                "LOOT_MECHANICAL",
                "LOOT_GEAR",
                "LOOT_SHIT"
        );
    }

    static List<String> containerPoolNames() {
        return List.copyOf(POOLS.keySet());
    }

    private static void registerPilePools() {
        pool("POOL_RED_PEDESTAL",
                e("reinhardtshbm:ballistic_gauntlet", 1, 1, 10),
                e("reinhardtshbm:armor_polish", 1, 1, 10),
                e("reinhardtshbm:bandaid", 1, 1, 10),
                e("reinhardtshbm:serum", 1, 1, 10),
                e("reinhardtshbm:quartz_plutonium", 1, 1, 10),
                e("reinhardtshbm:morning_glory", 1, 1, 10),
                e("reinhardtshbm:spider_milk", 1, 1, 10),
                e("reinhardtshbm:ink", 1, 1, 10),
                e("reinhardtshbm:heart_container", 1, 1, 10),
                e("reinhardtshbm:black_diamond", 1, 1, 10),
                e("reinhardtshbm:scrumpy", 1, 1, 10),
                e("reinhardtshbm:wild_p", 1, 1, 5),
                e("reinhardtshbm:card_aos", 1, 1, 5),
                e("reinhardtshbm:card_qos", 1, 1, 5),
                e("reinhardtshbm:starmetal_sword", 1, 1, 5),
                e("reinhardtshbm:gem_alexandrite", 1, 1, 5),
                e("reinhardtshbm:crackpipe", 1, 1, 5),
                e("reinhardtshbm:flask_infusion", 1, 1, 5),
                e("reinhardtshbm:boxcar", 1, 1, 5),
                e("reinhardtshbm:book_of_", 1, 1, 5),
                e("reinhardtshbm:item_secret", 1, 1, 1, "folly"),
                e("reinhardtshbm:weapon_mod_special", 1, 1, 1, "nickel"),
                e("reinhardtshbm:weapon_mod_special", 1, 1, 1, "doubloons")
        );
        pool("POOL_BLACK_SLAB",
                e("reinhardtshbm:clay_tablet", 1, 1, 10)
        );
        pool("POOL_BLACK_PART",
                e("reinhardtshbm:item_secret", 4, 4, 10, "selenium_steel"),
                e("reinhardtshbm:item_secret", 1, 1, 10, "controller"),
                e("reinhardtshbm:item_secret", 1, 1, 10, "canister"),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1, "secret")
        );
        pool("POOL_PILE_HIVE",
                e("minecraft:iron_ingot", 1, 3, 10),
                e("reinhardtshbm:ingot_steel", 1, 2, 10),
                e("reinhardtshbm:ingot_aluminium", 1, 2, 10),
                e("reinhardtshbm:scrap", 3, 6, 10),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 10),
                e("reinhardtshbm:steel_pickaxe", 1, 1, 5),
                e("reinhardtshbm:steel_shovel", 1, 1, 5),
                e("reinhardtshbm:bottle_nuka", 1, 2, 20),
                e("reinhardtshbm:definitelyfood", 5, 12, 20),
                e("reinhardtshbm:egg_glyphid", 1, 3, 30),
                e("reinhardtshbm:syringe_metal_stimpak", 1, 1, 5),
                e("reinhardtshbm:iv_blood", 1, 1, 10),
                e("minecraft:experience_bottle", 1, 3, 5)
        );
        pool("POOL_PILE_BONES",
                e("minecraft:bone", 1, 1, 10),
                e("minecraft:rotten_flesh", 1, 1, 5),
                e("reinhardtshbm:biomass", 1, 1, 2)
        );
        pool("POOL_PILE_CAPS",
                e("reinhardtshbm:cap_nuka", 4, 4, 20),
                e("reinhardtshbm:cap_quantum", 4, 4, 3),
                e("reinhardtshbm:cap_sparkle", 4, 4, 1)
        );
        pool("POOL_PILE_MED_SYRINGE",
                e("reinhardtshbm:syringe_metal_stimpak", 1, 1, 10),
                e("reinhardtshbm:syringe_metal_medx", 1, 1, 5),
                e("reinhardtshbm:syringe_metal_psycho", 1, 1, 5),
                // Optional Reinhardt Protocol Medical additions.  Entry.exists()
                // filters these IDs before the weighted roll, so when Medical is
                // absent this pool keeps exactly the original entries and weights.
                e("reinhardt_protocol_medical:battle_stimulant", 1, 1, 3),
                e("reinhardt_protocol_medical:high_pressure_impact_syringe", 1, 1, 2),
                e("reinhardt_protocol_medical:calcification_syringe", 1, 1, 1),
                e("reinhardt_protocol_medical:extreme_overload_syringe", 1, 1, 1),
                e("reinhardt_protocol_medical:twilight_syringe", 1, 1, 1)
        );
        pool("POOL_PILE_MED_PILLS",
                e("reinhardtshbm:radaway", 1, 1, 10),
                e("reinhardtshbm:radx", 1, 1, 10),
                e("reinhardtshbm:iv_blood", 1, 1, 15),
                e("reinhardtshbm:siox", 1, 1, 5)
        );
        pool("POOL_PILE_MAKESHIFT_WRENCH", e("reinhardtshbm:wrench", 1, 1, 10));
        pool("POOL_PILE_MAKESHIFT_PLATES", e("reinhardtshbm:plate_steel", 1, 1, 10));
        pool("POOL_PILE_MAKESHIFT_WIRE", e("reinhardtshbm:wire_fine", 1, 1, 10));
        pool("POOL_PILE_NUKE_STORAGE",
                e("reinhardtshbm:ammo_standard", 1, 1, 50),
                e("reinhardtshbm:bomb_caller", 1, 1, 10),
                e("reinhardtshbm:launch_code_piece", 1, 1, 10)
        );
        pool("POOL_PILE_OF_GARBAGE",
                e("reinhardtshbm:pipe", 0, 2, 20),
                e("reinhardtshbm:scrap", 1, 5, 20),
                e("reinhardtshbm:dust", 1, 3, 40),
                e("reinhardtshbm:dust_tiny", 1, 7, 40),
                e("reinhardtshbm:powder_cement", 1, 6, 40),
                e("reinhardtshbm:nugget_lead", 0, 3, 20),
                e("reinhardtshbm:wire_fine", 1, 2, 20),
                e("reinhardtshbm:powder_ash", 0, 1, 15),
                e("reinhardtshbm:plate_lead", 0, 1, 15),
                e("minecraft:string", 0, 1, 15),
                e("reinhardtshbm:bolt", 0, 2, 15, "lead"),
                e("reinhardtshbm:cap_nuka", 0, 8, 15),
                e("reinhardtshbm:plate_iron", 0, 2, 15),
                e("reinhardtshbm:fallout", 0, 2, 15),
                e("reinhardtshbm:coil_tungsten", 0, 2, 15),
                e("reinhardtshbm:can_empty", 0, 1, 15),
                e("reinhardtshbm:ingot_asbestos", 0, 1, 15),
                e("reinhardtshbm:syringe_metal_empty", 0, 1, 15),
                e("reinhardtshbm:syringe_empty", 0, 1, 15),
                e("reinhardtshbm:pipe_lead", 0, 1, 5),
                e("reinhardtshbm:motor", 0, 1, 5),
                e("reinhardtshbm:canned_conserve", 0, 1, 5)
        );
        pool("POOL_PILE_MECHANICAL",
                e("reinhardtshbm:defuser", 1, 1, 30),
                e("reinhardtshbm:screwdriver", 1, 1, 30),
                e("reinhardtshbm:wire_fine", 8, 12, 120),
                e("reinhardtshbm:plate_steel", 3, 8, 40),
                e("reinhardtshbm:plate_copper", 2, 5, 40),
                e("reinhardtshbm:coil_copper", 2, 5, 40),
                e("reinhardtshbm:coil_tungsten", 2, 5, 40)
        );
        pool("POOL_PILE_GEAR",
                e("reinhardtshbm:defuser", 1, 1, 40),
                e("reinhardtshbm:screwdriver", 1, 1, 30),
                e("reinhardtshbm:canteen_vodka", 1, 1, 40),
                e("reinhardtshbm:casing", 1, 4, 30),
                e("reinhardtshbm:casing_buckshot", 3, 8, 40),
                e("reinhardtshbm:definitelyfood", 2, 5, 40),
                e("reinhardtshbm:taurun_helmet", 1, 1, 20),
                e("reinhardtshbm:taurun_plate", 1, 1, 20),
                e("reinhardtshbm:taurun_legs", 1, 1, 20),
                e("reinhardtshbm:taurun_boots", 1, 1, 20)
        );
    }

    private static void registerCommonContainerPools() {
        pool("POOL_GENERIC",
                e("minecraft:bread", 1, 5, 8),
                e("reinhardtshbm:twinkie", 1, 3, 6),
                e("minecraft:iron_ingot", 2, 6, 10),
                e("reinhardtshbm:ingot_steel", 2, 5, 7),
                e("reinhardtshbm:ingot_beryllium", 1, 2, 4),
                e("reinhardtshbm:ingot_titanium", 1, 1, 3),
                e("reinhardtshbm:circuit_vacuum_tube", 1, 1, 5),
                e("reinhardtshbm:casing", 4, 10, 3, "small"),
                e("reinhardtshbm:casing", 4, 10, 3, "shotshell"),
                e("reinhardtshbm:cordite", 4, 6, 5),
                e("reinhardtshbm:battery_pack", 1, 1, 1, "battery_redstone"),
                e("reinhardtshbm:scrap", 1, 3, 10),
                e("reinhardtshbm:dust", 2, 4, 9),
                e("reinhardtshbm:bottle_opener", 1, 1, 2),
                e("reinhardtshbm:bottle_nuka", 1, 3, 4),
                e("reinhardtshbm:stealth_boy", 1, 1, 1),
                e("reinhardtshbm:cap_nuka", 1, 15, 7),
                e("reinhardtshbm:canister_full", 1, 2, 2, "diesel"),
                e("reinhardtshbm:canister_full", 1, 2, 3, "biofuel"),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 2),
                e("reinhardtshbm:gas_mask_filter", 1, 1, 3),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1),
                e("reinhardtshbm:coin_token", 1, 1, 2)
        );
        pool("POOL_MACHINE_PARTS",
                e("reinhardtshbm:plate_steel", 1, 5, 5),
                e("reinhardtshbm:shell", 1, 3, 3, "steel"),
                e("reinhardtshbm:plate_polymer", 1, 6, 5),
                e("reinhardtshbm:bolt", 4, 16, 3, "steel"),
                e("reinhardtshbm:bolt_tungsten", 4, 16, 3),
                e("reinhardtshbm:coil_tungsten", 1, 2, 5),
                e("reinhardtshbm:motor", 1, 2, 4),
                e("reinhardtshbm:coil_copper", 1, 3, 4),
                e("reinhardtshbm:coil_copper_torus", 1, 2, 3),
                e("reinhardtshbm:wire_fine", 1, 8, 5),
                e("reinhardtshbm:piston_selenium", 1, 1, 3),
                e("reinhardtshbm:battery_pack", 1, 1, 3),
                e("reinhardtshbm:circuit_vacuum_tube", 1, 2, 4),
                e("reinhardtshbm:circuit_pcb", 1, 3, 5),
                e("reinhardtshbm:circuit_capacitor", 1, 1, 3),
                e("reinhardtshbm:blade_titanium", 1, 8, 1),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1)
        );
        pool("POOL_FILING_CABINET",
                e("minecraft:paper", 1, 12, 240),
                e("minecraft:book", 1, 3, 90),
                e("minecraft:map", 1, 1, 50),
                e("minecraft:writable_book", 1, 1, 30),
                e("reinhardtshbm:cigarette", 1, 16, 20),
                e("reinhardtshbm:dust", 1, 1, 40),
                e("reinhardtshbm:dust_tiny", 1, 3, 75),
                e("reinhardtshbm:ink", 1, 1, 1),
                e("reinhardtshbm:screwdriver", 1, 1, 10),
                e("reinhardtshbm:blueprint_folder", 1, 1, 5),
                e("reinhardtshbm:coin_token", 1, 1, 30)
        );
        pool("POOL_BLUEPRINTS",
                e("reinhardtshbm:blueprint_folder", 1, 1, 16)
        );
        pool("POOL_SUPPLIES",
                e("reinhardtshbm:definitelyfood", 3, 10, 25),
                e("reinhardtshbm:syringe_metal_stimpak", 1, 3, 10),
                e("reinhardtshbm:pill_iodine", 1, 2, 2),
                e("reinhardtshbm:canister_full", 1, 4, 5, "diesel"),
                e("reinhardtshbm:machine_diesel", 1, 1, 1),
                e("reinhardtshbm:geiger_counter", 1, 1, 2),
                e("reinhardtshbm:med_bag", 1, 1, 3),
                e("reinhardtshbm:radaway", 1, 5, 10),
                // Supplies are a second, lower-frequency route for the two
                // non-experimental syringes; experimental effects remain in
                // the dedicated medical pile only.
                e("reinhardt_protocol_medical:battle_stimulant", 1, 1, 3),
                e("reinhardt_protocol_medical:high_pressure_impact_syringe", 1, 1, 2)
        );
        pool("POOL_AMMO",
                // The old C130 pool used firearm calibres that are not
                // registered after the HBM gun system was removed.  Keep the
                // surviving standard-ammo variants and their old stack sizes.
                e("reinhardtshbm:ammo_standard", 12, 12, 10, "p9_sp"),
                e("reinhardtshbm:ammo_standard", 6, 6, 10, "p9_fmj"),
                e("reinhardtshbm:ammo_standard", 6, 6, 5, "r762_sp"),
                e("reinhardtshbm:ammo_standard", 6, 6, 10, "g12_bp"),
                e("reinhardtshbm:ammo_standard", 1, 1, 3, "rocket_he")
        );

        // The following pools are referenced by the legacy NBT structures.
        // Keep their names and weights instead of silently routing them to
        // BACKUP_POOL: a missing pool changes the contents of an entire
        // structure into bread/sticks/scrap.  Entries whose old HBM firearm
        // no longer exists are harmlessly filtered by Entry.exists(), while
        // the remaining entries retain the 1.7.10 distribution.
        pool("POOL_ANTENNA",
                e("reinhardtshbm:twinkie", 1, 3, 4),
                e("reinhardtshbm:ingot_steel", 1, 2, 7),
                e("reinhardtshbm:ingot_red_copper", 1, 1, 4),
                e("reinhardtshbm:ingot_titanium", 1, 3, 5),
                e("reinhardtshbm:wire_fine", 2, 3, 7),
                e("reinhardtshbm:circuit_vacuum_tube", 1, 1, 4),
                e("reinhardtshbm:circuit_capacitor", 1, 1, 2),
                e("reinhardtshbm:battery_pack", 1, 1, 1, "battery_redstone"),
                e("reinhardtshbm:powder_iodine", 1, 1, 1),
                e("reinhardtshbm:powder_bromine", 1, 1, 1),
                e("reinhardtshbm:steel_poles", 1, 4, 8),
                e("reinhardtshbm:steel_scaffold", 1, 3, 8),
                e("reinhardtshbm:pole_top", 1, 1, 4),
                e("reinhardtshbm:pole_satellite_receiver", 1, 1, 7),
                e("reinhardtshbm:scrap", 1, 3, 10),
                e("reinhardtshbm:dust", 2, 4, 9),
                e("reinhardtshbm:bottle_opener", 1, 1, 2),
                e("reinhardtshbm:bottle_nuka", 1, 3, 4),
                e("reinhardtshbm:bottle_cherry", 1, 1, 2),
                e("reinhardtshbm:stealth_boy", 1, 1, 1),
                e("reinhardtshbm:cap_nuka", 1, 15, 7),
                e("reinhardtshbm:bomb_caller", 1, 1, 1),
                e("reinhardtshbm:gas_mask_filter", 1, 1, 2)
        );
        pool("POOL_EXPENSIVE",
                e("reinhardtshbm:chlorine_pinwheel", 1, 1, 1),
                e("reinhardtshbm:circuit_vacuum_tube", 1, 1, 4),
                e("reinhardtshbm:circuit_analog", 1, 1, 3),
                e("reinhardtshbm:circuit_chip", 1, 1, 2),
                e("reinhardtshbm:gun_kit_1", 1, 3, 6),
                e("reinhardtshbm:gun_kit_2", 1, 2, 3),
                e("reinhardtshbm:ammo_standard", 1, 4, 5, "rocket_he"),
                e("reinhardtshbm:ammo_standard", 1, 1, 5, "g26_flare"),
                e("reinhardtshbm:grenade_universal", 1, 1, 2, "nuke:nuclear:s7"),
                e("reinhardtshbm:grenade_universal", 1, 3, 3, "frag:cluster:s7"),
                e("reinhardtshbm:grenade_extra", 1, 1, 1, "triplex"),
                e("reinhardtshbm:stealth_boy", 1, 1, 2),
                e("reinhardtshbm:battery_pack", 1, 1, 1, "battery_lithium"),
                e("reinhardtshbm:syringe_awesome", 1, 1, 1),
                e("reinhardtshbm:fusion_core", 1, 1, 4),
                e("reinhardtshbm:bottle_nuka", 1, 3, 6),
                e("reinhardtshbm:bottle_quantum", 1, 1, 3),
                e("reinhardtshbm:red_barrel", 1, 1, 6),
                e("reinhardtshbm:canister_full", 1, 2, 2, "diesel"),
                e("reinhardtshbm:canister_full", 1, 2, 3, "biofuel"),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 5),
                e("reinhardtshbm:bomb_caller", 1, 1, 2, "carpet"),
                e("reinhardtshbm:bomb_caller", 1, 1, 1, "napalm"),
                e("reinhardtshbm:bomb_caller", 1, 1, 1, "chlorine"),
                e("reinhardtshbm:gas_mask_filter", 1, 1, 4),
                e("reinhardtshbm:launch_code_piece", 1, 1, 1),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1, "secret")
        );
        pool("POOL_NUKE_FUEL",
                e("reinhardtshbm:billet_uranium", 1, 4, 4),
                e("reinhardtshbm:billet_th232", 1, 3, 3),
                e("reinhardtshbm:billet_uranium_fuel", 1, 3, 5),
                e("reinhardtshbm:billet_mox_fuel", 1, 3, 5),
                e("reinhardtshbm:billet_thorium_fuel", 1, 3, 3),
                e("reinhardtshbm:billet_ra226be", 1, 2, 2),
                e("reinhardtshbm:billet_beryllium", 1, 1, 1),
                e("reinhardtshbm:nugget_u233", 1, 1, 1),
                e("reinhardtshbm:nugget_uranium_fuel", 1, 1, 1),
                e("reinhardtshbm:rod_zirnox_empty", 1, 3, 3),
                e("reinhardtshbm:ingot_graphite", 1, 4, 3),
                e("reinhardtshbm:pile_rod_uranium", 2, 5, 3),
                e("reinhardtshbm:pile_rod_source", 1, 2, 2),
                e("reinhardtshbm:reacher", 1, 1, 3),
                e("reinhardtshbm:screwdriver", 1, 1, 2)
        );
        pool("POOL_OFFICE_TRASH",
                e("minecraft:paper", 1, 12, 10),
                e("minecraft:book", 1, 3, 4),
                e("reinhardtshbm:twinkie", 1, 2, 6),
                e("reinhardtshbm:coffee", 1, 1, 4),
                e("reinhardtshbm:flame_politics", 1, 1, 2),
                e("reinhardtshbm:ring_pull", 1, 1, 4),
                e("reinhardtshbm:can_empty", 1, 1, 2),
                e("reinhardtshbm:can_creature", 1, 2, 2),
                e("reinhardtshbm:can_smart", 1, 3, 2),
                e("reinhardtshbm:can_mrsugar", 1, 2, 2),
                e("reinhardtshbm:cap_nuka", 1, 16, 2),
                e("reinhardtshbm:book_guide_book", 1, 1, 1),
                e("reinhardtshbm:deco_computer", 1, 1, 1),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1),
                e("reinhardtshbm:coin_token", 1, 1, 2)
        );
        pool("POOL_SOLID_FUEL",
                e("reinhardtshbm:solid_fuel", 1, 5, 1),
                e("reinhardtshbm:solid_fuel_presto", 1, 2, 2),
                e("reinhardtshbm:ball_dynamite", 1, 4, 2),
                e("reinhardtshbm:coke", 1, 3, 1, "petroleum"),
                e("minecraft:redstone", 1, 3, 1),
                e("reinhardtshbm:niter", 1, 3, 1)
        );
        pool("POOL_VAULT_LAB",
                e("reinhardtshbm:blowtorch", 1, 1, 4),
                e("reinhardtshbm:chemistry_set", 1, 1, 15),
                e("reinhardtshbm:screwdriver", 1, 1, 10),
                e("reinhardtshbm:nugget_mercury", 1, 1, 3),
                e("reinhardtshbm:morning_glory", 1, 1, 1),
                e("reinhardtshbm:filter_coal", 1, 1, 5),
                e("reinhardtshbm:dust", 1, 3, 25),
                e("minecraft:paper", 1, 2, 15),
                e("reinhardtshbm:cell_empty", 1, 1, 5),
                e("minecraft:glass_bottle", 1, 1, 5),
                e("reinhardtshbm:powder_iodine", 1, 1, 1),
                e("reinhardtshbm:powder_bromine", 1, 1, 1),
                e("reinhardtshbm:powder_cobalt", 1, 1, 1),
                e("reinhardtshbm:powder_neodymium", 1, 1, 1),
                e("reinhardtshbm:powder_boron", 1, 1, 1),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1, "secret")
        );
        pool("POOL_VAULT_LOCKERS",
                e("reinhardtshbm:robes_helmet", 1, 1, 1),
                e("reinhardtshbm:robes_plate", 1, 1, 1),
                e("reinhardtshbm:robes_legs", 1, 1, 1),
                e("reinhardtshbm:robes_boots", 1, 1, 1),
                e("reinhardtshbm:jackt", 1, 1, 1),
                e("reinhardtshbm:jackt2", 1, 1, 1),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 2),
                e("reinhardtshbm:gas_mask_mono", 1, 1, 2),
                e("reinhardtshbm:goggles", 1, 1, 2),
                e("reinhardtshbm:gas_mask_filter", 1, 1, 4),
                e("reinhardtshbm:flame_opinion", 1, 3, 5),
                e("reinhardtshbm:flame_conspiracy", 1, 3, 5),
                e("reinhardtshbm:flame_politics", 1, 3, 5),
                e("reinhardtshbm:definitelyfood", 2, 7, 5),
                e("reinhardtshbm:cigarette", 1, 8, 5),
                e("reinhardtshbm:armor_polish", 1, 1, 3),
                e("reinhardtshbm:rag", 1, 3, 5),
                e("minecraft:paper", 1, 6, 7),
                e("minecraft:clock", 1, 1, 3),
                e("minecraft:book", 1, 5, 10),
                e("minecraft:experience_bottle", 1, 3, 1),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1),
                e("reinhardtshbm:ammo_container", 1, 1, 1),
                e("reinhardtshbm:coin_token", 1, 1, 5)
        );
        pool("POOL_OIL_RIG",
                e("reinhardtshbm:oil_detector", 1, 1, 1),
                e("reinhardtshbm:canister_full", 1, 4, 5, "oil"),
                e("reinhardtshbm:canister_empty", 4, 16, 10),
                e("reinhardtshbm:circuit_analog", 1, 4, 1),
                e("reinhardtshbm:circuit_capacitor", 1, 1, 3)
        );
        pool("POOL_RTG",
                e("reinhardtshbm:pellet_rtg_depleted", 1, 1, 40, "lead"),
                e("reinhardtshbm:pellet_rtg_weak", 0, 1, 1)
        );
        pool("POOL_REPAIR_MATERIALS",
                e("reinhardtshbm:ingot_aluminium", 2, 8, 3),
                e("reinhardtshbm:ingot_steel", 1, 12, 4),
                e("reinhardtshbm:plate_aluminium", 5, 12, 3),
                e("reinhardtshbm:plate_iron", 6, 16, 3),
                e("reinhardtshbm:plate_steel", 2, 12, 2),
                e("reinhardtshbm:ingot_tungsten", 1, 2, 1),
                e("reinhardtshbm:deco_aluminium", 12, 24, 4),
                e("reinhardtshbm:deco_steel", 5, 12, 2),
                e("reinhardtshbm:block_aluminium", 1, 2, 1),
                e("reinhardtshbm:block_steel", 1, 1, 1),
                e("reinhardtshbm:bolt", 4, 16, 3, "steel"),
                e("reinhardtshbm:circuit_vacuum_tube", 1, 2, 4),
                e("reinhardtshbm:circuit_analog", 1, 3, 5),
                e("reinhardtshbm:circuit_capacitor", 1, 1, 3)
        );
        pool("POOL_SILO",
                e("reinhardtshbm:missile_generic", 1, 1, 4),
                e("reinhardtshbm:missile_incendiary", 1, 1, 4),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 5),
                e("reinhardtshbm:battery_pack", 1, 1, 3, "battery_lead"),
                e("reinhardtshbm:designator", 1, 1, 5),
                e("reinhardtshbm:thruster_small", 1, 1, 5),
                e("reinhardtshbm:thruster_medium", 1, 1, 4),
                e("reinhardtshbm:fuel_tank_small", 1, 1, 5),
                e("reinhardtshbm:fuel_tank_medium", 1, 1, 4),
                e("reinhardtshbm:bomb_caller", 1, 1, 1, "carpet"),
                e("reinhardtshbm:bomb_caller", 1, 1, 1, "agent_orange"),
                e("reinhardtshbm:bottle_nuka", 1, 3, 10)
        );
        pool("POOL_VERTIBIRD",
                e("reinhardtshbm:t51_helmet", 1, 1, 15),
                e("reinhardtshbm:t51_plate", 1, 1, 15),
                e("reinhardtshbm:t51_legs", 1, 1, 15),
                e("reinhardtshbm:t51_boots", 1, 1, 15),
                e("reinhardtshbm:fusion_core", 1, 1, 10),
                e("reinhardtshbm:gun_light_revolver", 1, 1, 4),
                e("reinhardtshbm:gun_kit_1", 1, 3, 4),
                e("reinhardtshbm:ammo_standard", 1, 24, 4, "m357_fmj"),
                e("reinhardtshbm:grenade_universal", 1, 2, 2, "nuke:nuclear:s7"),
                e("reinhardtshbm:ammo_standard", 1, 1, 5, "g26_flare"),
                e("reinhardtshbm:rod", 1, 1, 2, "u235"),
                e("reinhardtshbm:billet_uranium_fuel", 1, 1, 2),
                e("reinhardtshbm:ingot_uranium_fuel", 1, 1, 2),
                e("reinhardtshbm:bottle_nuka", 1, 3, 6),
                e("reinhardtshbm:bottle_quantum", 1, 1, 3),
                e("reinhardtshbm:stealth_boy", 1, 1, 7),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 5),
                e("reinhardtshbm:gas_mask_filter", 1, 1, 5),
                e("reinhardtshbm:bomb_caller", 1, 1, 1, "carpet"),
                e("reinhardtshbm:bomb_caller", 1, 1, 1, "napalm"),
                e("reinhardtshbm:bomb_caller", 1, 1, 2, "chlorine")
        );
        // The legacy C130 weapon pool is retained as a named pool so old
        // markers remain valid.  HBM firearm entries are filtered when the
        // firearm registry is disabled; TACZ integration owns replacements.
        pool("POOL_WEAPONS",
                e("reinhardtshbm:gun_light_revolver", 1, 1, 100),
                e("reinhardtshbm:gun_henry", 1, 1, 100),
                e("reinhardtshbm:gun_maresleg", 1, 1, 100),
                e("reinhardtshbm:gun_greasegun", 1, 1, 100),
                e("reinhardtshbm:gun_carbine", 1, 1, 50),
                e("reinhardtshbm:gun_heavy_revolver", 1, 1, 50),
                e("reinhardtshbm:gun_panzerschreck", 1, 1, 20),
                e("reinhardtshbm:gun_double_barrel", 1, 1, 10),
                e("reinhardtshbm:gun_n_i_4_n_i", 1, 1, 1)
        );
        // Remaining single-structure pools from ItemPoolsLegacy and
        // ItemPoolsSingle.  They are kept as separate named pools because
        // old structure markers select these names directly.
        pool("POOL_NUKE_TRASH",
                e("reinhardtshbm:nugget_u238", 3, 12, 5),
                e("reinhardtshbm:nugget_pu240", 3, 8, 5),
                e("reinhardtshbm:nugget_neptunium", 1, 4, 3),
                e("reinhardtshbm:rod", 1, 1, 3, "u238"),
                e("reinhardtshbm:rod_dual", 1, 1, 3, "u238"),
                e("reinhardtshbm:rod_quad", 1, 1, 3, "u238"),
                e("reinhardtshbm:bottle_quantum", 1, 1, 1),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 5),
                e("reinhardtshbm:hazmat_kit", 1, 1, 1),
                e("reinhardtshbm:gas_mask_filter", 1, 1, 5),
                e("reinhardtshbm:yellow_barrel", 1, 1, 2)
        );
        pool("POOL_NUKE_MISC",
                e("reinhardtshbm:nugget_u235", 3, 12, 5),
                e("reinhardtshbm:nugget_pu238", 3, 12, 5),
                e("reinhardtshbm:nugget_ra226", 3, 6, 5),
                e("reinhardtshbm:rod", 1, 1, 3, "u235"),
                e("reinhardtshbm:rod_dual", 1, 1, 3, "u235"),
                e("reinhardtshbm:rod_quad", 1, 1, 3, "u235"),
                e("reinhardtshbm:rod_zirnox", 1, 1, 4, "uranium_fuel"),
                e("reinhardtshbm:rod_zirnox", 1, 1, 4, "mox_fuel"),
                e("reinhardtshbm:rod_zirnox", 1, 1, 3, "lithium_fuel"),
                e("reinhardtshbm:rod_zirnox", 1, 1, 3, "thorium_fuel"),
                e("reinhardtshbm:rod_dual", 1, 1, 3, "thf"),
                e("reinhardtshbm:rod_zirnox_tritium", 1, 1, 1),
                e("reinhardtshbm:rod_zirnox", 1, 1, 1, "u233_fuel"),
                e("reinhardtshbm:rod_zirnox", 1, 1, 1, "u235_fuel"),
                e("reinhardtshbm:pellet_rtg", 1, 1, 3),
                e("reinhardtshbm:powder_thorium", 1, 1, 1),
                e("reinhardtshbm:powder_neptunium", 1, 1, 1),
                e("reinhardtshbm:powder_strontium", 1, 1, 1),
                e("reinhardtshbm:powder_cobalt", 1, 1, 1),
                e("reinhardtshbm:bottle_quantum", 1, 1, 1),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 5),
                e("reinhardtshbm:hazmat_kit", 1, 1, 2),
                e("reinhardtshbm:gas_mask_filter", 1, 1, 5),
                e("reinhardtshbm:yellow_barrel", 1, 1, 3)
        );
        pool("POOL_SPACESHIP",
                e("reinhardtshbm:battery_pack", 1, 1, 2, "battery_lead"),
                e("reinhardtshbm:coil_copper", 1, 2, 5),
                e("reinhardtshbm:wire_fine", 8, 32, 5),
                e("reinhardtshbm:cell_deuterium", 1, 8, 5),
                e("reinhardtshbm:cell_tritium", 1, 8, 5),
                e("reinhardtshbm:cell_antimatter", 1, 1, 1),
                e("reinhardtshbm:powder_neodymium", 1, 1, 1),
                e("reinhardtshbm:powder_niobium", 1, 1, 1),
                e("reinhardtshbm:wire_dense", 2, 4, 5),
                e("reinhardtshbm:wire_dense", 1, 3, 5, "gold"),
                e("reinhardtshbm:pwr_fuel", 1, 2, 5),
                e("reinhardtshbm:block_tungsten", 1, 8, 5),
                e("reinhardtshbm:red_wire_coated", 1, 8, 5),
                e("reinhardtshbm:red_cable", 1, 16, 5)
        );
        pool("POOL_METEOR_SAFE",
                e("reinhardtshbm:book_of_", 1, 1, 1),
                e("reinhardtshbm:stamp_book", 1, 1, 1, "printing1"),
                e("reinhardtshbm:stamp_book", 1, 1, 1, "printing2"),
                e("reinhardtshbm:stamp_book", 1, 1, 1, "printing3"),
                e("reinhardtshbm:stamp_book", 1, 1, 1, "printing4"),
                e("reinhardtshbm:stamp_book", 1, 1, 1, "printing5"),
                e("reinhardtshbm:stamp_book", 1, 1, 1, "printing6"),
                e("reinhardtshbm:stamp_book", 1, 1, 1, "printing7"),
                e("reinhardtshbm:stamp_book", 1, 1, 1, "printing8")
        );
        pool("POOL_METEORITE_TREASURE",
                e("reinhardtshbm:cobalt_pickaxe", 1, 1, 10),
                e("reinhardtshbm:ingot_zirconium", 1, 16, 10),
                e("reinhardtshbm:ingot_niobium", 1, 16, 10),
                e("reinhardtshbm:ingot_cobalt", 1, 16, 10),
                e("reinhardtshbm:ingot_boron", 1, 16, 10),
                e("reinhardtshbm:ingot_starmetal", 1, 1, 5),
                e("reinhardtshbm:crystal_gold", 1, 4, 10),
                e("reinhardtshbm:circuit_vacuum_tube", 4, 8, 10),
                e("reinhardtshbm:circuit_chip", 2, 4, 10),
                e("reinhardtshbm:definitelyfood", 16, 32, 25),
                e("reinhardtshbm:crate_can", 1, 3, 10),
                e("reinhardtshbm:pill_herbal", 1, 2, 10),
                e("reinhardtshbm:serum", 1, 1, 5),
                e("reinhardtshbm:heart_piece", 1, 1, 5),
                e("reinhardtshbm:scrumpy", 1, 1, 5),
                e("reinhardtshbm:launch_code_piece", 1, 1, 5),
                e("reinhardtshbm:egg_glyphid", 1, 1, 5),
                e("reinhardtshbm:gem_alexandrite", 1, 1, 1),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1, "discover")
        );
        pool("POOL_VAULT_RUSTY",
                e("minecraft:gold_ingot", 3, 14, 1),
                e("reinhardtshbm:gun_heavy_revolver", 1, 1, 2),
                e("reinhardtshbm:pin", 8, 8, 1),
                e("reinhardtshbm:gun_am180", 1, 1, 1),
                e("reinhardtshbm:bottle_quantum", 1, 3, 1),
                e("reinhardtshbm:ingot_cobalt", 4, 12, 1),
                e("reinhardtshbm:ammo_standard", 24, 48, 1, "bmg50_fmj"),
                e("reinhardtshbm:ammo_standard", 48, 64, 2, "p9_jhp"),
                e("reinhardtshbm:circuit_chip", 3, 6, 1),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 1),
                e("reinhardtshbm:grenade_universal", 1, 1, 1, "frag:he:s3"),
                e("reinhardtshbm:grenade_universal", 1, 1, 1, "frag:inc:s3"),
                e("minecraft:diamond", 1, 2, 1)
        );
        pool("POOL_VAULT_STANDARD",
                e("reinhardtshbm:ingot_desh", 2, 6, 1),
                e("reinhardtshbm:powder_desh_mix", 1, 5, 1),
                e("minecraft:diamond", 3, 6, 1),
                e("reinhardtshbm:ammo_standard", 1, 1, 1, "nuke_standard"),
                e("reinhardtshbm:grenade_universal", 1, 1, 1, "nuke:nuclear:s7"),
                e("reinhardtshbm:grenade_universal", 1, 6, 1, "tech:emp:s3"),
                e("reinhardtshbm:powder_yellowcake", 16, 24, 1),
                e("reinhardtshbm:circuit_vacuum_tube", 12, 16, 1),
                e("reinhardtshbm:circuit_chip", 2, 6, 1)
        );
        pool("POOL_VAULT_REINFORCED",
                e("reinhardtshbm:ingot_desh", 6, 16, 1),
                e("reinhardtshbm:powder_power", 1, 5, 1),
                e("reinhardtshbm:sat_chip", 1, 1, 1),
                e("minecraft:diamond", 5, 9, 1),
                e("reinhardtshbm:ammo_standard", 1, 3, 1, "nuke_standard"),
                e("reinhardtshbm:grenade_universal", 1, 2, 1, "nuke:nuclear:s7"),
                e("reinhardtshbm:grenade_universal", 1, 1, 1, "stick:he:impact:triplex"),
                e("reinhardtshbm:powder_yellowcake", 26, 42, 1),
                e("reinhardtshbm:circuit_chip", 18, 32, 1),
                e("reinhardtshbm:circuit_basic", 6, 12, 1)
        );
        pool("POOL_VAULT_UNBREAKABLE",
                e("reinhardtshbm:ammo_standard", 2, 3, 1, "nuke_demo"),
                e("reinhardtshbm:ammo_standard", 16, 32, 1, "r762_sp"),
                e("reinhardtshbm:circuit_advanced", 6, 12, 1),
                e("reinhardtshbm:ingot_steel", 1, 2, 1)
        );
    }

    private static Entry e(String id, int min, int max, int weight) {
        return new Entry(id, min, max, weight, null);
    }

    private static Entry e(String id, int min, int max, int weight, String variant) {
        return new Entry(id, min, max, weight, variant);
    }

    private static void pool(String name, Entry... entries) {
        POOLS.put(name, List.of(entries));
    }

    private record Entry(String id, int min, int max, int weight, String variant) {
        private Entry(String id, int min, int max, int weight) {
            this(id, min, max, weight, null);
        }

        private boolean exists() {
            ResourceLocation location = ResourceLocation.tryParse(id);
            return location != null && BuiltInRegistries.ITEM.containsKey(location);
        }

        private ItemStack stack(RandomSource random) {
            ResourceLocation location = ResourceLocation.tryParse(id);
            Item item = location != null && BuiltInRegistries.ITEM.containsKey(location) ? BuiltInRegistries.ITEM.get(location) : Items.AIR;
            if (item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            int low = Math.max(0, min);
            int high = Math.max(low, max);
            int count = random == null || high == low ? low : low + random.nextInt(high - low + 1);
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack;
            if ("reinhardtshbm:blueprint_folder".equals(id) && variant != null) {
                BlueprintFolderItem.Variant folderVariant = switch (variant) {
                    case "discover" -> BlueprintFolderItem.Variant.DISCOVER;
                    case "secret" -> BlueprintFolderItem.Variant.SECRET;
                    default -> BlueprintFolderItem.Variant.STANDARD;
                };
                stack = BlueprintFolderItem.stackFor(folderVariant);
            } else if (variant != null && item instanceof HbmFluidContainerItem fluidItem && fluidItem.isFilledContainer()) {
                // 1.7.10 stored the fluid as the old metadata value.  The
                // modern container keeps that value in its fluid component;
                // do not silently turn a full canister into the default fluid.
                var fluid = HbmFluids.byName(variant);
                stack = fluid.isPresent() ? fluidItem.filledStack(fluid.get()) : ItemStack.EMPTY;
            } else if (variant != null && item instanceof LegacyBombCallerItem) {
                try {
                    stack = LegacyBombCallerItem.stackFor(LegacyBombCallerItem.Type.valueOf(variant.toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    stack = new ItemStack(item, count);
                }
            } else if (variant != null && item instanceof UniversalGrenadeItem) {
                String[] parts = variant.split(":", -1);
                if (parts.length >= 3) {
                    try {
                        UniversalGrenadeItem.Shell shell = UniversalGrenadeItem.Shell.valueOf(parts[0].toUpperCase(java.util.Locale.ROOT));
                        UniversalGrenadeItem.Filling filling = UniversalGrenadeItem.Filling.valueOf(parts[1].toUpperCase(java.util.Locale.ROOT));
                        UniversalGrenadeItem.Fuze fuze = UniversalGrenadeItem.Fuze.valueOf(parts[2].toUpperCase(java.util.Locale.ROOT));
                        UniversalGrenadeItem.Extra extra = parts.length > 3 && !parts[3].isBlank()
                                ? UniversalGrenadeItem.Extra.valueOf(parts[3].toUpperCase(java.util.Locale.ROOT)) : null;
                        stack = UniversalGrenadeItem.make(shell, filling, fuze, extra);
                    } catch (IllegalArgumentException ignored) {
                        stack = new ItemStack(item, count);
                    }
                } else {
                    stack = new ItemStack(item, count);
                }
            } else if (variant != null && item instanceof RtgDepletedPelletItem pellet) {
                stack = RtgDepletedPelletItem.stackFor(pellet, variant);
            } else if (variant != null && item instanceof StampBookItem stampBook) {
                try {
                    stack = StampBookItem.stackFor(stampBook,
                            com.reinhardt.hbm.item.StampItem.StampType.valueOf(variant.toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    stack = new ItemStack(item, count);
                }
            } else if (variant != null && item instanceof FoundryShapeItem shapeItem) {
                var material = FoundryMaterial.byName(variant);
                stack = material.filter(value -> FoundryShapeItem.supports(shapeItem.shape(), value))
                        .map(value -> FoundryShapeItem.stackFor(item, value, count))
                        .orElseGet(() -> new ItemStack(item, count));
            } else if (variant != null && item instanceof LegacyVariantItem) {
                stack = LegacyVariantItem.stackFor(item, variant);
            } else {
                stack = new ItemStack(item, count);
            }
            stack.setCount(count);
            return stack;
        }
    }
}
