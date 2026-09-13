package com.reinhardt.hbm.advancement;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * Modern replacement for the 1.7.10 Achievement page and
 * {@code AchievementHandler.fire(player, stack)}.
 */
public final class HbmAdvancements {
    private static final Map<ResourceLocation, String> CRAFTED_STACK_ADVANCEMENTS = craftedStackAdvancements();

    private HbmAdvancements() {
    }

    public static boolean has(ServerPlayer player, String id) {
        AdvancementHolder advancement = holder(player.getServer(), id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static void award(Player player, String id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        award(serverPlayer, id);
    }

    public static void award(ServerPlayer player, String id) {
        AdvancementHolder advancement = holder(player.getServer(), id);
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }

        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    public static void awardNearby(ServerLevel level, AABB area, String id) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            award(player, id);
        }
    }

    public static void awardAll(ServerLevel level, String id) {
        for (ServerPlayer player : level.players()) {
            award(player, id);
        }
    }

    public static void awardForCraftedStack(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()) {
            return;
        }

        String advancement = CRAFTED_STACK_ADVANCEMENTS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (advancement != null) {
            award(serverPlayer, advancement);
        }
    }

    private static AdvancementHolder holder(MinecraftServer server, String id) {
        return server == null ? null : server.getAdvancements().get(ReinhardtsHBM.id(id));
    }

    private static Map<ResourceLocation, String> craftedStackAdvancements() {
        Map<ResourceLocation, String> map = new HashMap<>();

        // AchievementHandler.register(), HBM 1.7.10.
        add(map, "piston_selenium", "selenium");
        add(map, "gun_b92", "selenium");
        add(map, "battery_potatos", "potato");
        add(map, "machine_press", "burner_press");
        add(map, "rbmk_fuel_empty", "rbmk");
        add(map, "machine_chemical_plant", "chemplant");
        add(map, "concrete_smooth", "concrete");
        add(map, "concrete_asbestos", "concrete");
        add(map, "ingot_polymer", "polymer");
        add(map, "ingot_desh", "desh");
        add(map, "gem_tantalium", "tantalum");
        add(map, "machine_gascent", "gas_cent");
        add(map, "machine_centrifuge", "centrifuge");
        add(map, "ingot_schrabidium", "schrab");
        add(map, "nugget_schrabidium", "schrab");
        add(map, "machine_crystallizer", "acidizer");
        add(map, "machine_silex", "silex");
        add(map, "nugget_technetium", "technetium");
        add(map, "struct_watz_core", "watz");
        add(map, "nugget_bismuth", "bismuth");
        add(map, "nugget_am241", "breeding");
        add(map, "nugget_am242", "breeding");
        add(map, "missile_nuclear", "red_balloons");
        add(map, "missile_nuclear_cluster", "red_balloons");
        add(map, "missile_doomsday", "red_balloons");
        add(map, "mp_warhead_10_nuclear", "red_balloons");
        add(map, "mp_warhead_10_nuclear_large", "red_balloons");
        add(map, "mp_warhead_15_nuclear", "red_balloons");
        add(map, "mp_warhead_15_nuclear_shark", "red_balloons");
        add(map, "mp_warhead_15_boxcar", "red_balloons");
        add(map, "struct_torus_core", "fusion");
        add(map, "machine_blast_furnace", "blast_furnace");
        add(map, "machine_assembly_machine", "assembly");
        add(map, "billet_pu_mix", "chicago_pile");
        add(map, "particle_digamma", "omega12");

        return Map.copyOf(map);
    }

    private static void add(Map<ResourceLocation, String> map, String itemId, String advancementId) {
        map.put(ReinhardtsHBM.id(itemId), advancementId);
    }
}
