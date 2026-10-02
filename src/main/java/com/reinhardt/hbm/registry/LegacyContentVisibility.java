package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Visibility rules carried over from the latest 1.7.10 source.
 *
 * <p>Creative-tab visibility and recipe-browser visibility are deliberately
 * separate.  The old mod used {@code setCreativeTab(null)} (or simply never
 * assigned a tab) for internal content, while {@code NEIConfig} hid a smaller,
 * explicit set from NEI.  Treating those as one rule would hide legitimate
 * recipe ingredients from JEI.</p>
 */
public final class LegacyContentVisibility {
    /**
     * Exact registry paths whose latest 1.7.10 counterpart has no creative
     * tab.  This includes internal block states and flattened metadata items.
     */
    private static final Set<String> CREATIVE_HIDDEN_IDS = idSet("""
            achievement_icon acid_block acid_fluid ams_core_thingy apple_euphemium balefire barrel_corroded
            barricade bismuth_tool block_graphite_detector block_graphite_drilled block_graphite_fuel
            block_graphite_lithium block_graphite_plutonium block_graphite_rod block_graphite_source block_graphite_tritium
            bobmazon_hidden book_lemegeton book_lore book_of_ book_secret
            bottle2_empty bottle_empty brick_forgotten brick_forgotten_lock brick_red
            broken_item burnt_bark capacitor_bus capacitor_gold capacitor_niobium
            capacitor_schrabidate capacitor_tantalium card_aos card_qos cbt_device
            chlorine1 chlorine2 chlorine3 chlorine4 chlorine5
            chlorine6 chlorine7 chlorine8 circuit_star circuit_star_component
            circuit_star_piece clay_tablet cloud1 cloud2 cloud3
            cloud4 cloud5 cloud6 cloud7 cloud8 concrete_liquid corium_block corium_fluid
            coal_eternal conveyor conveyor_chute conveyor_double conveyor_express
            conveyor_lift conveyor_triple crate_red crystal_hardened crystal_pulsar
            crystal_virus deco_loot digamma_matter door_red dummy_block_blast
            dummy_plate_compact_launcher dummy_plate_launch_table dummy_port_compact_launcher dummy_port_launch_table dungeon_spawner
            euphemium_boots euphemium_helmet euphemium_kit euphemium_legs euphemium_plate
            event_tester field_disturber fire_digamma floodlight_beam fluid_icon geysir_chlorine geysir_nether
            fusion_hatch fusion_heater holotape_damaged holotape_image icf_block
            ingot_metal ingot_u238m2 item_secret key_red key_red_cracked
            kit_custom lamp_tritium_blue_on lamp_tritium_green_on lantern_behemoth logic_block logic_block_invis machine_battery
            machine_battery_potato machine_boiler_off machine_controller machine_difurnace_on machine_difurnace_rtg_on
            machine_dineutronium_battery machine_electric_furnace_on machine_furnace_brick_on machine_lithium_battery machine_puf6_tank
            machine_rtg_furnace_off machine_rtg_furnace_on machine_schrabidium_battery machine_uf6_tank mech_key
            memory missile_custom missile_test mud_block mud_fluid mush_block mush_block_stem
            nothing ntm_dirt obj_tester oil_pipe orange1
            orange2 orange3 orange4 orange5 orange6
            orange7 orange8 ore_bedrock ore_byproduct ore_centrifuged
            ore_cleaned ore_deepcleaned ore_enriched ore_nitrated ore_nitrocrystalline
            ore_purified ore_seared ore_separated padlock_unbreakable page_of_
            pc1 pc2 pc3 pc4 pc5
            pc6 pc7 pc8 pch pedestal
            pile_block pile_rod_boron pile_rod_detector pile_rod_lithium pile_rod_plutonium
            pile_rod_pu239 pile_rod_source pile_rod_uranium pink_double_slab pink_log
            pink_planks pink_slab pink_stairs piston_inserter plate_fuel_mox
            plate_fuel_pu238be plate_fuel_pu239 plate_fuel_ra226be plate_fuel_sa326 plate_fuel_u233
            plate_fuel_u235 pwr_block rail_large_buffer rail_large_curve rail_large_curve_7
            rail_large_curve_9 rail_large_ramp rail_large_straight rail_large_straight_short rail_large_switch
            rail_large_switch_flipped rail_narrow_curve rail_narrow_straight record_glass reinforced_lamp_on
            sat_foeq sat_laser sat_lunar_miner sat_mapper sat_miner
            rad_lava_block rad_lava_fluid sat_radar sat_resonator sat_scanner schrabidic_block schrabidic_fluid scrap_plastic seal_hatch
            skeleton_holder slag spotlight_beam spotlight_fluoro_off spotlight_halogen_off
            spotlight_incandescent_off stamp_book statue_elb_f stone_keyhole stone_keyhole_meta
            structure_anchor structure_pattern structure_randomized structure_randomly structure_single
            structure_solid sulfuric_acid_block sulfuric_acid_fluid syringe_mkunicorn taint template_folder toxic_block toxic_fluid wand_air
            wand_jigsaw wand_logic wand_loot wand_structure wand_tandem
            waste_plate_mox waste_plate_pu238be waste_plate_pu239 waste_plate_ra226be waste_plate_sa326
            volcanic_lava_block volcanic_lava_fluid waste_plate_u233 waste_plate_u235 watch zirnox_destroyed
            """);

    /** Metadata-backed 1.7.10 items flattened into one registry id per variant. */
    private static final Set<String> CREATIVE_HIDDEN_PREFIXES = Set.of(
            "achievement_icon_",
            "circuit_star_component_",
            "circuit_star_piece_",
            "ingot_metal_",
            "item_secret_",
            "page_of_",
            "scrap_plastic_"
    );

    /**
     * Latest 1.7.10 {@code NEIConfig} hide list, plus the port-only retired
     * single-block turbine which has no public item role.
     */
    private static final Set<String> JEI_HIDDEN_IDS = idSet("""
            ammo_secret ams_core_thingy bobmazon_hidden book_lore book_of_ book_secret
            brick_forgotten brick_forgotten_lock burnt_bark coal_eternal conveyor
            conveyor_chute conveyor_double conveyor_express conveyor_lift conveyor_triple
            dummy_block_blast dummy_plate_compact_launcher dummy_plate_launch_table dummy_port_compact_launcher dummy_port_launch_table
            euphemium_kit ingot_metal item_secret machine_difurnace_on machine_electric_furnace_on
            machine_large_turbine machine_rtg_furnace_on memory pink_double_slab pink_log
            pink_planks pink_slab pink_stairs reinforced_lamp_on spotlight_beam
            spotlight_fluoro_off spotlight_halogen_off spotlight_incandescent_off statue_elb_f
            """);

    private static final Set<String> JEI_HIDDEN_PREFIXES = Set.of(
            "ammo_secret_",
            "ingot_metal_",
            "item_secret_"
    );

    private LegacyContentVisibility() {
    }

    public static boolean isCreativeHidden(ItemStack stack) {
        return isHbmStackMatching(stack, CREATIVE_HIDDEN_IDS, CREATIVE_HIDDEN_PREFIXES);
    }

    public static boolean isCreativeHiddenPath(String path) {
        return matches(path, CREATIVE_HIDDEN_IDS, CREATIVE_HIDDEN_PREFIXES);
    }

    public static boolean isJeiHidden(ItemStack stack) {
        return isHbmStackMatching(stack, JEI_HIDDEN_IDS, JEI_HIDDEN_PREFIXES);
    }

    private static boolean isHbmStackMatching(ItemStack stack, Set<String> exactIds, Set<String> prefixes) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ReinhardtsHBM.MOD_ID.equals(id.getNamespace()) && matches(id.getPath(), exactIds, prefixes);
    }

    private static boolean matches(String path, Set<String> exactIds, Set<String> prefixes) {
        if (exactIds.contains(path)) {
            return true;
        }
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> idSet(String ids) {
        return Set.of(ids.strip().split("\\s+"));
    }
}
