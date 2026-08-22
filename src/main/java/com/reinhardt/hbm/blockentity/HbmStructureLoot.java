package com.reinhardt.hbm.blockentity;

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

final class HbmStructureLoot {
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

    static void applyPileLoot(DecoLootBlockEntity loot, String pool, RandomSource random) {
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

    static void fillContainer(Container container, String pool, int min, int max, RandomSource random) {
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
                boolean gun = random.nextBoolean();
                if (gun) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_GUN", random), 0.125D, 0.025D, 0.25D);
                }
                if (!gun || random.nextBoolean()) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_WRENCH", random), -0.25D, 0.0D, -0.28125D);
                }
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
                addItemWithDeviation(loot, random, stack("reinhardtshbm:gun_flaregun", 1), 0.0D, 0.0D, -0.25D);
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

    private static ItemStack randomStack(String pool, RandomSource random) {
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
        pool("POOL_PILE_HIVE",
                e("minecraft:iron_ingot", 1, 3, 10),
                e("reinhardtshbm:ingot_steel", 1, 2, 10),
                e("reinhardtshbm:ingot_aluminium", 1, 2, 10),
                e("reinhardtshbm:scrap", 3, 6, 10),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 10),
                e("reinhardtshbm:steel_pickaxe", 1, 1, 5),
                e("reinhardtshbm:steel_shovel", 1, 1, 5),
                e("reinhardtshbm:gun_maresleg", 1, 1, 5),
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
                e("reinhardtshbm:syringe_metal_psycho", 1, 1, 5)
        );
        pool("POOL_PILE_MED_PILLS",
                e("reinhardtshbm:radaway", 1, 1, 10),
                e("reinhardtshbm:radx", 1, 1, 10),
                e("reinhardtshbm:iv_blood", 1, 1, 15),
                e("reinhardtshbm:siox", 1, 1, 5)
        );
        pool("POOL_PILE_MAKESHIFT_GUN", e("reinhardtshbm:gun_maresleg", 1, 1, 10));
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
                e("reinhardtshbm:bolt", 0, 2, 15),
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
                e("reinhardtshbm:gun_light_revolver", 1, 1, 3),
                e("reinhardtshbm:gun_maresleg", 1, 1, 1),
                e("reinhardtshbm:casing", 4, 10, 3),
                e("reinhardtshbm:cordite", 4, 6, 5),
                e("reinhardtshbm:battery_pack", 1, 1, 1),
                e("reinhardtshbm:scrap", 1, 3, 10),
                e("reinhardtshbm:dust", 2, 4, 9),
                e("reinhardtshbm:bottle_opener", 1, 1, 2),
                e("reinhardtshbm:bottle_nuka", 1, 3, 4),
                e("reinhardtshbm:stealth_boy", 1, 1, 1),
                e("reinhardtshbm:cap_nuka", 1, 15, 7),
                e("reinhardtshbm:canister_full", 1, 2, 5),
                e("reinhardtshbm:gas_mask_m65", 1, 1, 2),
                e("reinhardtshbm:gas_mask_filter", 1, 1, 3),
                e("reinhardtshbm:blueprint_folder", 1, 1, 1),
                e("reinhardtshbm:coin_token", 1, 1, 2)
        );
        pool("POOL_MACHINE_PARTS",
                e("reinhardtshbm:plate_steel", 1, 5, 5),
                e("reinhardtshbm:shell", 1, 3, 3),
                e("reinhardtshbm:plate_polymer", 1, 6, 5),
                e("reinhardtshbm:bolt", 4, 16, 6),
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
        pool("POOL_BLUEPRINTS",
                e("reinhardtshbm:blueprint_folder", 1, 1, 16)
        );
        pool("POOL_SUPPLIES",
                e("reinhardtshbm:definitelyfood", 3, 10, 25),
                e("reinhardtshbm:syringe_metal_stimpak", 1, 3, 10),
                e("reinhardtshbm:canister_full", 1, 4, 5),
                e("reinhardtshbm:geiger_counter", 1, 1, 2),
                e("reinhardtshbm:radaway", 1, 5, 10)
        );
        pool("POOL_WEAPONS",
                e("reinhardtshbm:gun_light_revolver", 1, 1, 100),
                e("reinhardtshbm:gun_maresleg", 1, 1, 100),
                e("reinhardtshbm:gun_carbine", 1, 1, 50),
                e("reinhardtshbm:gun_heavy_revolver", 1, 1, 50)
        );
        pool("POOL_AMMO",
                e("reinhardtshbm:ammo_standard", 6, 12, 40),
                e("reinhardtshbm:ammo_container", 1, 1, 1)
        );
    }

    private static Entry e(String id, int min, int max, int weight) {
        return new Entry(id, min, max, weight);
    }

    private static void pool(String name, Entry... entries) {
        POOLS.put(name, List.of(entries));
    }

    private record Entry(String id, int min, int max, int weight) {
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
            return new ItemStack(item, count);
        }
    }
}
