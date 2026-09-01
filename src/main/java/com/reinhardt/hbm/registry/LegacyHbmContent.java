package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LegacyDirectionalBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.block.LegacyVariantSlabBlock;
import com.reinhardt.hbm.block.LegacyVariantStrengths;
import com.reinhardt.hbm.block.RadiatingBlock;
import com.reinhardt.hbm.block.ReinforcedLampBlock;
import com.reinhardt.hbm.block.SteelScaffoldBlock;
import com.reinhardt.hbm.block.SteelPolesBlock;
import com.reinhardt.hbm.item.LegacyDetonatorItem;
import com.reinhardt.hbm.item.LegacyCigaretteItem;
import com.reinhardt.hbm.item.LegacyEnergyDrinkItem;
import com.reinhardt.hbm.item.LegacyMissileItem;
import com.reinhardt.hbm.item.LegacyPillItem;
import com.reinhardt.hbm.item.LegacyRangeDesignatorItem;
import com.reinhardt.hbm.item.LegacySyringeItem;
import com.reinhardt.hbm.item.LegacyHotItem;
import com.reinhardt.hbm.item.LegacyPipetteItem;
import com.reinhardt.hbm.item.LegacyStarterKitItem;
import com.reinhardt.hbm.item.HealthArmorModItem;
import com.reinhardt.hbm.item.LegacyCanteenItem;
import com.reinhardt.hbm.item.LegacyLemonItem;
import com.reinhardt.hbm.item.LegacyLoreItem;
import com.reinhardt.hbm.item.LegacyConserveItem;
import com.reinhardt.hbm.item.LegacyCrayonItem;
import com.reinhardt.hbm.item.LegacyFlaskItem;
import com.reinhardt.hbm.item.LegacySpecialFoodItem;
import com.reinhardt.hbm.item.ColtanCompassItem;
import com.reinhardt.hbm.item.FixedBatteryItem;
import com.reinhardt.hbm.item.CustomMissileItem;
import com.reinhardt.hbm.item.MissilePartItem;
import com.reinhardt.hbm.radiation.HbmHazardSystem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LegacyHbmContent {
    private record BlockStrength(float hardness, float resistance) {
    }

    private static final Map<String, BlockStrength> BUILDING_STRENGTHS = Map.ofEntries(
            Map.entry("reinforced_stone", new BlockStrength(15.0F, 100.0F)),
            Map.entry("reinforced_brick", new BlockStrength(15.0F, 300.0F)),
            Map.entry("reinforced_glass", new BlockStrength(2.0F, 25.0F)),
            Map.entry("reinforced_glass_pane", new BlockStrength(2.0F, 25.0F)),
            Map.entry("reinforced_light", new BlockStrength(15.0F, 80.0F)),
            Map.entry("reinforced_laminate", new BlockStrength(15.0F, 300.0F)),
            Map.entry("reinforced_laminate_pane", new BlockStrength(15.0F, 300.0F)),
            Map.entry("reinforced_sand", new BlockStrength(15.0F, 40.0F)),
            Map.entry("concrete", new BlockStrength(15.0F, 140.0F)),
            Map.entry("concrete_smooth", new BlockStrength(15.0F, 140.0F)),
            Map.entry("concrete_colored", new BlockStrength(15.0F, 140.0F)),
            Map.entry("concrete_colored_ext", new BlockStrength(15.0F, 140.0F)),
            Map.entry("concrete_asbestos", new BlockStrength(15.0F, 150.0F)),
            Map.entry("concrete_rebar", new BlockStrength(50.0F, 240.0F)),
            Map.entry("concrete_super", new BlockStrength(150.0F, 1000.0F)),
            Map.entry("concrete_super_broken", new BlockStrength(10.0F, 20.0F)),
            Map.entry("concrete_pillar", new BlockStrength(15.0F, 180.0F)),
            Map.entry("brick_concrete_marked", new BlockStrength(15.0F, 160.0F)),
            Map.entry("brick_obsidian", new BlockStrength(15.0F, 120.0F)),
            Map.entry("brick_light", new BlockStrength(5.0F, 20.0F)),
            Map.entry("brick_compound", new BlockStrength(15.0F, 400.0F)),
            Map.entry("cmb_brick", new BlockStrength(25.0F, 5000.0F)),
            Map.entry("cmb_brick_reinforced", new BlockStrength(25.0F, 50000.0F)),
            Map.entry("brick_asbestos", new BlockStrength(5.0F, 1000.0F)),
            Map.entry("brick_fire", new BlockStrength(5.0F, 35.0F)),
            Map.entry("ducrete_smooth", new BlockStrength(20.0F, 500.0F)),
            Map.entry("ducrete", new BlockStrength(20.0F, 500.0F)),
            Map.entry("brick_ducrete", new BlockStrength(15.0F, 750.0F)),
            Map.entry("brick_ducrete_stairs", new BlockStrength(15.0F, 750.0F)),
            Map.entry("reinforced_ducrete", new BlockStrength(20.0F, 1000.0F)),
            Map.entry("tile_lab", new BlockStrength(1.0F, 20.0F)),
            Map.entry("tile_lab_cracked", new BlockStrength(1.0F, 20.0F)),
            Map.entry("tile_lab_broken", new BlockStrength(1.0F, 20.0F)),
            Map.entry("steel_scaffold", new BlockStrength(5.0F, 15.0F)),
            Map.entry("steel_beam", new BlockStrength(5.0F, 15.0F)),
            Map.entry("steel_poles", new BlockStrength(5.0F, 15.0F)),
            Map.entry("steel_wall", new BlockStrength(5.0F, 15.0F)),
            Map.entry("steel_corner", new BlockStrength(15.0F, 15.0F)),
            Map.entry("steel_roof", new BlockStrength(5.0F, 15.0F)),
            Map.entry("struct_scaffold", new BlockStrength(5.0F, 10.0F)),
            Map.entry("asphalt", new BlockStrength(15.0F, 120.0F)),
            Map.entry("asphalt_light", new BlockStrength(15.0F, 120.0F))
    );
    private static final Set<String> CORE_BLOCKS = Set.of(
            "anvil_arsenic_bronze",
            "anvil_bismuth_bronze",
            "anvil_desh",
            "anvil_dnt",
            "anvil_ferrouranium",
            "anvil_iron",
            "anvil_lead",
            "anvil_murky",
            "anvil_osmiridium",
            "anvil_saturnite",
            "anvil_schrabidate",
            "anvil_steel",
            "barrel_antimatter",
            "barrel_corroded",
            "barrel_plastic",
            "barrel_steel",
            "barrel_tcalloy",
            "blast_door",
            "block_meteor",
            "block_meteor_broken",
            "block_meteor_cobble",
            "block_meteor_molten",
            "block_meteor_treasure",
            "chimney_brick",
            "chimney_industrial",
            "machine_ashpit",
            "brick_concrete",
            "brick_concrete_broken",
            "brick_concrete_cracked",
            "brick_concrete_mossy",
            "brick_fire",
            "brick_fire_double_slab",
            "brick_fire_slab",
            "brick_fire_stairs",
            "cluster_copper",
            "concrete",
            "crate",
            "crate_ammo",
            "crate_can",
            "crate_jungle",
            "crate_lead",
            "crate_metal",
            "crate_red",
            "crate_supply",
            "crate_weapon",
            "dummy_block_blast",
            "ore_aluminium",
            "ore_asbestos",
            "ore_beryllium",
            "ore_cinnabar",
            "ore_cobalt",
            "ore_coltan",
            "ore_fluorite",
            "ore_lead",
            "ore_lignite",
            "ore_niter",
            "ore_deepslate_oil",
            "ore_deepslate_oil_empty",
            "ore_oil",
            "ore_oil_empty",
            "ore_oil_sand",
            "ore_rare",
            "ore_schrabidium",
            "ore_sulfur",
            "ore_thorium",
            "ore_titanium",
            "ore_tungsten",
            "ore_uranium",
            "ore_uranium_scorched",
            "fusion_heater",
            "fluid_counter_valve",
            "fluid_duct_box",
            "fluid_duct_exhaust",
            "fluid_duct_gauge",
            "fluid_duct_mk2",
            "fluid_duct_paintable",
            "fluid_duct_paintable_block_exhaust",
            "fluid_duct_solid",
            "fluid_duct_solid_sealed",
            "fluid_pump",
            "fluid_switch",
            "fluid_valve",
            "machine_solar_boiler",
            "heater_electric",
            "heater_firebox",
            "heater_heatex",
            "heater_oilburner",
            "heater_oven",
            "machine_electric_furnace_off",
            "machine_electric_furnace_on",
            "machine_assembly_machine",
            "machine_chemical_plant",
            "machine_annihilator",
            "machine_autocrafter",
            "machine_autosaw",
            "machine_coker",
            "machine_cyclotron",
            "machine_exposure_chamber",
            "machine_forcefield",
            "machine_missile_assembly",
            "machine_orbus",
            "machine_precass",
            "machine_pyrooven",
            "machine_radar",
            "machine_radar_large",
            "machine_radgen",
            "machine_radiolysis",
            "machine_rtg_grey",
            "machine_satlinker",
            "machine_sawmill",
            "machine_solidifier",
            "machine_epress",
            "machine_condenser",
            "machine_fluidtank",
            "machine_industrial_turbine",
            "machine_large_turbine",
            "machine_press",
            "machine_shredder",
            "machine_soldering_station",
            "machine_steam_engine",
            "machine_stirling",
            "machine_stirling_creative",
            "machine_stirling_steel",
            "machine_tower_large",
            "machine_tower_small",
            "machine_teleporter",
            "machine_turbine",
            "machine_turbinegas",
            "machine_turbofan",
            "machine_uf6_tank",
            "machine_puf6_tank",
            "machine_well",
            "machine_wood_burner",
            "machine_zirnox",
            "meteor_brick",
            "meteor_brick_chiseled",
            "meteor_brick_cracked",
            "meteor_brick_mossy",
            "meteor_pillar",
            "meteor_polished",
            "meteor_spawner",
            "oil_pipe",
            "press_preheater",
            "pump_electric",
            "pump_steam",
            "rbmk_heater",
            "red_connector_super",
            "red_cable",
            "red_connector",
            "red_pylon",
            "red_pylon_large",
            "red_wire_coated",
            "reinforced_stone",
            "solar_mirror",
            "substation",
            "zirnox_destroyed"
    );
    private static final List<String> LEGACY_STRUCTURE_BLOCKS = List.of(
            "brick_slab",
            "brick_double_slab",
            "concrete_brick_slab",
            "concrete_brick_double_slab"
    );

    /* These registrations are explicitly deprecated in the 1.7.10 source. */
    private static final Set<String> RETIRED_LEGACY_BLOCK_IDS = Set.of(
            "capacitor_gold",
            "capacitor_niobium",
            "capacitor_schrabidate",
            "capacitor_tantalium",
            "machine_battery",
            "machine_battery_potato",
            "machine_dineutronium_battery",
            "machine_fensu",
            "machine_lithium_battery",
            "machine_minirtg",
            "machine_powerrtg",
            "machine_rtg_furnace_off",
            "machine_rtg_furnace_on",
            "machine_schrabidium_battery"
    );

    /*
     * These are not 1.7.10 gameplay items. They are superseded 1.12 aliases or
     * components belonging solely to the deliberately removed basic firearm
     * system. Development saves are intentionally not supported, so retaining
     * them as fake steel-ingot items would only hide missing content.
     */
    private static final Set<String> RETIRED_LEGACY_ITEM_IDS = Set.of(
            "ammo_container",
            "battery_advanced",
            "book_guide_book",
            "boltgun",
            "cell",
            "coin_siege",
            "fluid_barrel_v2",
            "fluid_tank_lead_v2",
            "fluid_tank_v2",
            "gun_egon",
            "gun_vortex",
            "jetpack_glider",
            "mechanism_launcher_1",
            "mechanism_launcher_2",
            "mechanism_revolver_1",
            "mechanism_revolver_2",
            "mechanism_rifle_1",
            "mechanism_rifle_2",
            "mechanism_special",
            "multitool_beam",
            "multitool_decon",
            "multitool_dig",
            "multitool_ext",
            "multitool_hit",
            "multitool_joule",
            "multitool_mega",
            "multitool_miner",
            "multitool_silk",
            "multitool_sky",
            "pellet_canister",
            "pellet_chlorophyte",
            "pellet_claws",
            "pellet_flechette",
            "pellet_meteorite",
            "sliding_blast_door_skin0",
            "sliding_blast_door_skin1",
            "sliding_blast_door_skin2",
            "weaponized_starblaster_cell",
            "weapon_bat",
            "weapon_bat_nail",
            "weapon_golf_club",
            "weapon_pipe_rusty",
            "weapon_saw"
    );

    public static final List<DeferredItem<Item>> LEGACY_ITEMS = new ArrayList<>();
    public static final List<DeferredBlock<Block>> LEGACY_BLOCKS = new ArrayList<>();
    public static final List<DeferredBlock<Block>> BUILDING_BLOCKS = new ArrayList<>();

    private static boolean bootstrapped;

    private LegacyHbmContent() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }

        List<String> legacyBlockIds = new ArrayList<>(loadIds("legacy/reinhardtshbm/blocks.txt"));
        for (String id : LEGACY_STRUCTURE_BLOCKS) {
            if (!legacyBlockIds.contains(id)) {
                legacyBlockIds.add(id);
            }
        }
        Set<String> blockItemIds = new HashSet<>(legacyBlockIds);

        for (String id : legacyBlockIds) {
            if (RETIRED_LEGACY_BLOCK_IDS.contains(id)
                    || CORE_BLOCKS.contains(id)
                    || HbmBlocks.isCoreBlock(id)) {
                continue;
            }

            boolean buildingBlock = isBuildingBlock(id);
            DeferredBlock<Block> block = HbmBlocks.BLOCKS.register(id, () -> createPlaceholderBlock(id));
            if (buildingBlock) {
                BUILDING_BLOCKS.add(block);
            } else {
                LEGACY_BLOCKS.add(block);
            }
            HbmItems.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        }

        for (String id : loadIds("legacy/reinhardtshbm/items.txt")) {
            if (HbmItems.isCoreItem(id) || HbmBlocks.isCoreBlock(id) || blockItemIds.contains(id) || isRetiredLegacyItem(id)) {
                continue;
            }

            DeferredItem<Item> item = HbmItems.ITEMS.register(
                    id,
                    () -> createLegacyItem(id)
            );
            LEGACY_ITEMS.add(item);
        }

        bootstrapped = true;
    }

    private static Block createPlaceholderBlock(String id) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
        BlockStrength strength = strengthFor(id);

        if (id.equals("steel_poles")) {
            return new SteelPolesBlock(properties
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.METAL)
                    .noOcclusion());
        }

        if (id.equals("reinforced_glass_pane")) {
            return new IronBarsBlock(properties
                    .mapColor(MapColor.NONE)
                    .requiresCorrectToolForDrops()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.GLASS)
                    .noOcclusion());
        }

        if (id.equals("reinforced_lamp_off") || id.equals("reinforced_lamp_on")) {
            boolean lit = id.endsWith("_on");
            return new ReinforcedLampBlock(properties
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(15.0F, 80.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> lit ? 15 : 0), lit);
        }

        if (id.equals("steel_scaffold")) {
            return new SteelScaffoldBlock(properties
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.METAL)
                    .noOcclusion());
        }

        if (id.equals("capacitor_copper")) {
            return new LegacyDirectionalBlock(properties
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());
        }

        if (id.equals("brick_slab")) {
            return new LegacyVariantSlabBlock(properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(LegacyVariantStrengths.maxHardness(LegacyVariantStrengths.BRICK_SLAB),
                            LegacyVariantStrengths.maxResistance(LegacyVariantStrengths.BRICK_SLAB))
                    .sound(SoundType.STONE), LegacyVariantStrengths.BRICK_SLAB);
        }

        if (id.equals("concrete_brick_slab")) {
            return new LegacyVariantSlabBlock(properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(LegacyVariantStrengths.maxHardness(LegacyVariantStrengths.CONCRETE_BRICK_SLAB),
                            LegacyVariantStrengths.maxResistance(LegacyVariantStrengths.CONCRETE_BRICK_SLAB))
                    .sound(SoundType.STONE), LegacyVariantStrengths.CONCRETE_BRICK_SLAB);
        }

        if (id.equals("brick_double_slab")) {
            return new LegacyVariantBlock(properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(LegacyVariantStrengths.maxHardness(LegacyVariantStrengths.BRICK_SLAB),
                            LegacyVariantStrengths.maxResistance(LegacyVariantStrengths.BRICK_SLAB))
                    .sound(SoundType.STONE), LegacyVariantStrengths.BRICK_SLAB);
        }

        if (id.equals("concrete_brick_double_slab")) {
            return new LegacyVariantBlock(properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(LegacyVariantStrengths.maxHardness(LegacyVariantStrengths.CONCRETE_BRICK_SLAB),
                            LegacyVariantStrengths.maxResistance(LegacyVariantStrengths.CONCRETE_BRICK_SLAB))
                    .sound(SoundType.STONE), LegacyVariantStrengths.CONCRETE_BRICK_SLAB);
        }

        if (id.endsWith("_stairs")) {
            return new StairBlock(Blocks.STONE.defaultBlockState(), properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.STONE));
        }

        if (id.endsWith("_slab") && !id.endsWith("_double_slab")) {
            return new SlabBlock(properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.STONE));
        }

        if (id.equals("concrete_pillar")) {
            return new RotatedPillarBlock(properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.STONE));
        }

        if (id.startsWith("gas_")) {
            return hazardAwareBlock(id, properties
                    .mapColor(MapColor.NONE)
                    .replaceable()
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY));
        }

        if (id.startsWith("fire_") || id.equals("balefire")) {
            return hazardAwareBlock(id, properties
                    .mapColor(MapColor.FIRE)
                    .replaceable()
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state -> 12)
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY));
        }

        if (id.startsWith("ore_")) {
            return hazardAwareBlock(id, properties
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.STONE));
        }

        if (id.contains("glass") || id.equals("reinforced_laminate")) {
            return hazardAwareBlock(id, properties
                    .mapColor(MapColor.NONE)
                    .requiresCorrectToolForDrops()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.GLASS)
                    .noOcclusion());
        }

        if (id.equals("reinforced_light")) {
            return hazardAwareBlock(id, properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(15.0F, 80.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 15));
        }

        return hazardAwareBlock(id, properties
                .mapColor(MapColor.COLOR_GRAY)
                .requiresCorrectToolForDrops()
                .strength(strength.hardness(), strength.resistance())
                .sound(SoundType.STONE));
    }

    private static BlockStrength strengthFor(String id) {
        BlockStrength direct = BUILDING_STRENGTHS.get(id);
        if (direct != null) {
            return direct;
        }
        String base = baseBuildingId(id);
        BlockStrength baseStrength = BUILDING_STRENGTHS.get(base);
        if (baseStrength != null) {
            return baseStrength;
        }
        if (id.startsWith("brick_concrete")) {
            return new BlockStrength(15.0F, id.contains("cracked") ? 60.0F : id.contains("broken") ? 45.0F : 160.0F);
        }
        if (isConcreteColorBlock(id)) {
            return BUILDING_STRENGTHS.get("concrete");
        }
        if (id.startsWith("tile_lab")) {
            return BUILDING_STRENGTHS.get("tile_lab");
        }
        return new BlockStrength(2.0F, 10.0F);
    }

    private static boolean isConcreteColorBlock(String id) {
        String base = baseBuildingId(id);
        return base.equals("concrete_black")
                || base.equals("concrete_blue")
                || base.equals("concrete_brown")
                || base.equals("concrete_cyan")
                || base.equals("concrete_gray")
                || base.equals("concrete_green")
                || base.equals("concrete_light_blue")
                || base.equals("concrete_lime")
                || base.equals("concrete_magenta")
                || base.equals("concrete_orange")
                || base.equals("concrete_pink")
                || base.equals("concrete_purple")
                || base.equals("concrete_red")
                || base.equals("concrete_silver")
                || base.equals("concrete_white")
                || base.equals("concrete_yellow");
    }

    private static String baseBuildingId(String id) {
        if (id.endsWith("_double_slab")) {
            return id.substring(0, id.length() - "_double_slab".length());
        }
        if (id.endsWith("_slab")) {
            return id.substring(0, id.length() - "_slab".length());
        }
        if (id.endsWith("_stairs")) {
            return id.substring(0, id.length() - "_stairs".length());
        }
        return id;
    }

    private static Block hazardAwareBlock(String id, BlockBehaviour.Properties properties) {
        double radiation = HbmHazardSystem.rawRadiationForId(id);
        if (radiation > 0.0D) {
            return new RadiatingBlock(properties.randomTicks(), radiation);
        }
        return new Block(properties);
    }

    private static boolean isBuildingBlock(String id) {
        return id.startsWith("deco_")
                || id.startsWith("concrete")
                || id.startsWith("brick_concrete")
                || id.equals("brick_slab")
                || id.equals("brick_double_slab")
                || id.equals("concrete_brick_slab")
                || id.equals("concrete_brick_double_slab")
                || id.startsWith("glass_")
                || id.startsWith("cmb_brick")
                || id.startsWith("reinforced_brick")
                || id.startsWith("reinforced_stone")
                || id.startsWith("reinforced_sand")
                || id.equals("reinforced_glass")
                || id.equals("reinforced_glass_pane")
                || id.equals("reinforced_laminate")
                || id.equals("reinforced_laminate_pane")
                || id.equals("reinforced_light")
                || id.equals("brick_compound")
                || id.startsWith("brick_compound_")
                || id.equals("brick_obsidian")
                || id.startsWith("brick_obsidian_")
                || id.equals("brick_light")
                || id.startsWith("brick_light_")
                || id.equals("brick_asbestos")
                || id.startsWith("brick_asbestos_")
                || id.startsWith("ducrete")
                || id.equals("brick_ducrete")
                || id.startsWith("brick_ducrete_")
                || id.equals("reinforced_ducrete")
                || id.startsWith("reinforced_ducrete_")
                || id.startsWith("asphalt")
                || id.startsWith("basalt")
                || id.startsWith("tile_lab")
                || id.equals("steel_scaffold")
                || id.equals("steel_beam")
                || id.equals("steel_poles")
                || id.equals("steel_wall")
                || id.equals("steel_corner")
                || id.equals("steel_roof")
                || id.equals("struct_scaffold");
    }

    private static boolean isDetonator(String id) {
        return id.equals("detonator")
                || id.equals("detonator_de")
                || id.equals("detonator_deadman")
                || id.equals("detonator_laser")
                || id.equals("detonator_multi");
    }

    private static Item createLegacyItem(String id) {
        if (LegacyLoreItem.isLegacyLoreItem(id)) {
            return LegacyLoreItem.fromLegacyId(id);
        }
        if (id.equals("missile_custom")) {
            return new CustomMissileItem(new Item.Properties());
        }
        if (MissilePartItem.isPartId(id)) {
            return new MissilePartItem(new Item.Properties(), id);
        }
        if (isDetonator(id)) {
            return new LegacyDetonatorItem(new Item.Properties(), id);
        }
        return switch (id) {
            case "ingot_meteorite", "ingot_meteorite_forged", "blade_meteorite" ->
                    new LegacyHotItem(new Item.Properties(), 200, false);
            case "ingot_chainsteel" -> new LegacyHotItem(new Item.Properties(), 100, false);
            case "ingot_steel_dusted" -> new LegacyHotItem(new Item.Properties(), 200, true);
            case "heart_piece" -> new HealthArmorModItem(new Item.Properties(), 5.0D, false);
            case "heart_container" -> new HealthArmorModItem(new Item.Properties(), 20.0D, false);
            case "heart_booster" -> new HealthArmorModItem(new Item.Properties(), 40.0D, false);
            case "heart_fab" -> new HealthArmorModItem(new Item.Properties(), 60.0D, false);
            case "black_diamond" -> new HealthArmorModItem(new Item.Properties(), 40.0D, true);
            case "pipette" -> new LegacyPipetteItem(new Item.Properties(), LegacyPipetteItem.Kind.NORMAL);
            case "pipette_boron" -> new LegacyPipetteItem(new Item.Properties(), LegacyPipetteItem.Kind.BORON);
            case "pipette_laboratory" -> new LegacyPipetteItem(new Item.Properties(), LegacyPipetteItem.Kind.LABORATORY);
            case "glowing_stew", "balefire_scrambled", "balefire_and_ham" -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build())
                    .craftRemainder(net.minecraft.world.item.Items.BOWL));
            case "canteen_vodka" -> new LegacyCanteenItem(new Item.Properties());
            case "canned_conserve" -> new LegacyConserveItem(new Item.Properties());
            case "flask_infusion" -> new LegacyFlaskItem(new Item.Properties().stacksTo(1));
            case "crayon" -> new LegacyCrayonItem(new Item.Properties());
            case "bomb_waffle", "schnitzel_vegan", "cotton_candy", "apple_lead", "apple_schrabidium",
                    "tem_flakes", "pancake", "mucho_mango", "apple_euphemium" -> LegacySpecialFoodItem.fromLegacyId(id);
            case "lemon", "definitelyfood", "med_ipecac", "med_ptsd", "med_schizophrenia", "loops", "loop_stew",
                    "spongebob_macaroni", "fooditem", "twinkie", "static_sandwich", "pudding", "nugget", "cheese",
                    "cheese_quesadilla", "glyphid_meat", "glyphid_meat_grilled" -> LegacyLemonItem.fromLegacyId(id);
            case "stealth_boy", "nuke_starter_kit", "nuke_advanced_kit", "nuke_commercially_kit", "nuke_electric_kit",
                    "gadget_kit", "boy_kit", "man_kit", "mike_kit", "tsar_kit", "multi_kit", "custom_kit", "fleija_kit",
                    "solinium_kit", "prototype_kit", "missile_kit", "euphemium_kit", "hazmat_kit", "hazmat_red_kit",
                    "hazmat_grey_kit" -> new LegacyStarterKitItem(new Item.Properties(), id);
            case "chainsaw" -> HbmItems.CHAINSAW.get();
            case "wand_k" -> HbmItems.WAND_K.get();
            case "missile_soyuz" -> HbmItems.MISSILE_SOYUZ.get();
            case "missile_soyuz_lander" -> HbmItems.MISSILE_SOYUZ_LANDER.get();
            case "missile_generic" -> missile(id, LegacyMissileItem.FormFactor.V2, LegacyMissileItem.Tier.TIER1);
            case "missile_anti_ballistic" -> missile(id, LegacyMissileItem.FormFactor.ABM, LegacyMissileItem.Tier.TIER1);
            case "missile_incendiary", "missile_cluster", "missile_buster", "missile_decoy" -> missile(id, LegacyMissileItem.FormFactor.V2, LegacyMissileItem.Tier.TIER1);
            case "missile_strong", "missile_incendiary_strong", "missile_cluster_strong", "missile_buster_strong", "missile_emp_strong", "missile_stealth" -> missile(id, LegacyMissileItem.FormFactor.STRONG, LegacyMissileItem.Tier.TIER2);
            case "missile_burst", "missile_inferno", "missile_rain", "missile_drill" -> missile(id, LegacyMissileItem.FormFactor.HUGE, LegacyMissileItem.Tier.TIER3);
            case "missile_nuclear", "missile_nuclear_cluster", "missile_volcano", "missile_doomsday" -> missile(id, LegacyMissileItem.FormFactor.ATLAS, LegacyMissileItem.Tier.TIER4);
            case "missile_doomsday_rusted" -> new LegacyMissileItem(new Item.Properties(), LegacyMissileItem.FormFactor.ATLAS, LegacyMissileItem.Tier.TIER4, LegacyMissileItem.Fuel.JETFUEL_LOXY, 16_000, false);
            case "missile_taint", "missile_micro", "missile_bhole", "missile_schrabidium", "missile_emp", "missile_test" -> missile(id, LegacyMissileItem.FormFactor.MICRO, LegacyMissileItem.Tier.TIER0);
            case "missile_shuttle" -> missile(id, LegacyMissileItem.FormFactor.OTHER, LegacyMissileItem.Tier.TIER3);
            case "toolbox" -> HbmItems.TOOLBOX.get();
            case "can_smart", "can_creature", "can_redbomb", "can_mrsugar", "can_overcharge", "can_luna",
                    "can_bepis", "can_breen", "can_mug", "bottle_nuka", "bottle_cherry", "bottle_quantum",
                    "bottle_sparkle", "bottle_rad", "bottle2_korl", "bottle2_fritz", "chocolate_milk", "coffee",
                    "coffee_radium" -> new LegacyEnergyDrinkItem(new Item.Properties(), id);
            case "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super",
                    "syringe_taint", "syringe_mkunicorn", "med_bag", "syringe_antidote", "syringe_poison",
                    "syringe_awesome", "radaway", "radaway_strong", "radaway_flush", "iv_empty", "iv_blood",
                    "iv_xp_empty", "iv_xp" -> new LegacySyringeItem(new Item.Properties(), id);
            case "radx" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.RADX);
            case "siox" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.SIOX);
            case "pill_herbal" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.HERBAL);
            case "xanax" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.XANAX);
            case "fmn" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.FMN);
            case "five_htp" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.FIVE_HTP);
            case "pill_iodine" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.IODINE);
            case "plan_c" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.PLAN_C);
            case "pill_red" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.RED);
            case "chocolate" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.CHOCOLATE);
            case "cigarette" -> new LegacyCigaretteItem(new Item.Properties().stacksTo(16), false);
            case "crackpipe" -> new LegacyCigaretteItem(new Item.Properties().stacksTo(1), true);
            case "designator_range" -> new LegacyRangeDesignatorItem(new Item.Properties());
            case "coltan_tool" -> new ColtanCompassItem(new Item.Properties());
            case "memory" -> new FixedBatteryItem(
                    new Item.Properties(), Long.MAX_VALUE / 100L, 100_000_000_000_000L, 100_000_000_000_000L
            );
            default -> createSimpleLegacyItem();
        };
    }

    private static Item createSimpleLegacyItem() {
        // Resource and behaviour-specific registrations are moved into HbmItems
        // as they are ported. Remaining simple legacy entries are real Items,
        // never a steel-ingot placeholder masquerading as gameplay content.
        return new Item(new Item.Properties());
    }

    /** Base firearms are intentionally omitted in favour of the dedicated modern firearms mod. */
    private static boolean isRetiredLegacyItem(String id) {
        return RETIRED_LEGACY_ITEM_IDS.contains(id) || id.startsWith("gun_");
    }

    private static Item missile(String id, LegacyMissileItem.FormFactor formFactor, LegacyMissileItem.Tier tier) {
        return new LegacyMissileItem(new Item.Properties(), formFactor, tier);
    }

    private static List<String> loadIds(String path) {
        InputStream stream = LegacyHbmContent.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing generated HBM legacy id list: " + path);
        }

        List<String> ids = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.trim();
                if (!id.isEmpty() && id.charAt(0) == '\uFEFF') {
                    id = id.substring(1);
                }
                if (id.isEmpty() || id.startsWith("#")) {
                    continue;
                }
                if (!isValidPath(id)) {
                    ReinhardtsHBM.LOGGER.warn("Skipping invalid legacy HBM id '{}'", id);
                    continue;
                }
                ids.add(id);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read generated HBM legacy id list: " + path, exception);
        }
        return ids;
    }

    private static boolean isValidPath(String path) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/') {
                continue;
            }
            return false;
        }
        return !path.isBlank() && path.charAt(0) != '/' && path.charAt(path.length() - 1) != '/';
    }
}
