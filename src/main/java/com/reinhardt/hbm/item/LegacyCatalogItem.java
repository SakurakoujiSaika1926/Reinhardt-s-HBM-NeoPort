package com.reinhardt.hbm.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

/**
 * Formal registration for an ordinary 1.7.10 ModItems entry whose specialized
 * behavior is not represented by another modern item class yet. It keeps the
 * legacy id and resource contract and intentionally has no placeholder hint.
 */
public final class LegacyCatalogItem extends Item {
    private static final Set<String> SINGLE_STACK_ITEMS = Set.of(
            "achievement_icon", "analysis_tool", "anchor_remote", "bismuth_tool", "black_hole",
            "bobmazon", "bobmazon_hidden", "bomb_caller", "book_lemegeton", "book_lore", "book_of_",
            "cbt_device", "containment_box", "cube_power", "defuser_gold", "demon_core_open",
            "designator", "designator_manual", "detonator", "detonator_de", "detonator_deadman",
            "detonator_laser", "detonator_multi", "digamma_diagnostic", "drone", "drone_linker",
            "fusion_core", "gas_tester", "glitch", "hand_drill", "hand_drill_desh", "hev_battery",
            "holotape_image", "horseshoe_magnet", "industrial_magnet", "item_secret", "jetpack_boost",
            "jetpack_break", "jetpack_fly", "jetpack_tank", "jetpack_vector", "kit_custom", "mask_of_infamy",
            "meltdown_tool", "morning_glory", "neutrino_lens", "night_vision", "ore_density_scanner",
            "polaroid", "power_net_tool", "rebar_placer", "rod_of_discord", "rtty_pager", "sat_coord",
            "sat_designator", "sat_interface", "schrabidium_hammer", "schrabidium_hoe", "shimmer_sledge",
            "singularity", "singularity_counter_resonant", "singularity_super_heated", "siphon",
            "smashing_hammer", "solinium_core", "solinium_igniter", "solinium_propellant", "survey_scanner",
            "train", "wand_d", "wrench", "wrench_archineer", "wrench_flipped"
    );

    private final String legacyId;

    private LegacyCatalogItem(Properties properties, String legacyId) {
        super(properties);
        this.legacyId = legacyId;
    }

    public static LegacyCatalogItem fromLegacyId(String legacyId) {
        Properties properties = new Properties();
        if (SINGLE_STACK_ITEMS.contains(legacyId)) {
            properties.stacksTo(1);
        }
        return new LegacyCatalogItem(properties, legacyId);
    }

    public String legacyId() {
        return legacyId;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm." + legacyId);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Description keys, when present, are supplied by the copied language
        // files. Never expose an implementation or placeholder marker here.
    }
}
