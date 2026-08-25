package com.reinhardt.hbm.recipe.anvil;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.SirenTrackItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class HbmAnvilRecipes {
    private static final List<AnvilSmithingRecipe> SMITHING = new ArrayList<>();
    private static final List<AnvilConstructionRecipe> CONSTRUCTION = new ArrayList<>();
    private static final Set<String> NON_STANDARD_PLATE_MATERIALS = Set.of("desh");
    private static boolean initialized;

    private HbmAnvilRecipes() {
    }

    public static List<AnvilSmithingRecipe> smithing() {
        ensureInitialized();
        return SMITHING;
    }

    public static List<AnvilConstructionRecipe> construction() {
        ensureInitialized();
        return CONSTRUCTION;
    }

    public static List<AnvilConstructionRecipe> constructionForTier(int tier) {
        ensureInitialized();
        List<AnvilConstructionRecipe> recipes = new ArrayList<>();
        for (AnvilConstructionRecipe recipe : CONSTRUCTION) {
            if (recipe.isTierValid(tier)) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    public static AnvilSmithingRecipe.Match findSmithing(ItemStack left, ItemStack right, int tier) {
        ensureInitialized();
        for (AnvilSmithingRecipe recipe : SMITHING) {
            if (recipe.tier() > tier) {
                continue;
            }

            AnvilSmithingRecipe.Match match = recipe.match(left, right);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        registerSmithing();
        registerConstruction();
        initialized = true;
    }

    private static void registerSmithing() {
        ItemLike[] baseAnvils = new ItemLike[]{
                HbmBlocks.ANVIL_IRON.get(),
                HbmBlocks.ANVIL_LEAD.get()
        };

        for (ItemLike baseAnvil : baseAnvils) {
            addAnvilUpgrade(baseAnvil, "ingot_steel", HbmBlocks.ANVIL_STEEL.get());
            addAnvilUpgrade(baseAnvil, "ingot_desh", HbmBlocks.ANVIL_DESH.get());
            addAnvilUpgrade(baseAnvil, "ingot_saturnite", HbmBlocks.ANVIL_SATURNITE.get());
            addAnvilUpgrade(baseAnvil, "ingot_ferrouranium", HbmBlocks.ANVIL_FERROURANIUM.get());
            addAnvilUpgrade(baseAnvil, "ingot_bismuth_bronze", HbmBlocks.ANVIL_BISMUTH_BRONZE.get());
            addAnvilUpgrade(baseAnvil, "ingot_arsenic_bronze", HbmBlocks.ANVIL_ARSENIC_BRONZE.get());
            addAnvilUpgrade(baseAnvil, "ingot_schrabidate", HbmBlocks.ANVIL_SCHRABIDATE.get());
            addAnvilUpgrade(baseAnvil, "ingot_dineutronium", HbmBlocks.ANVIL_DNT.get());
            addAnvilUpgrade(baseAnvil, "ingot_osmiridium", HbmBlocks.ANVIL_OSMIRIDIUM.get());
        }

        addHotSmithing(3, item("ingot_meteorite_forged", 1), ingredient("ingot_meteorite", 1), ingredient("ingot_meteorite", 1));
        addHotSmithing(3, item("blade_meteorite", 1), ingredient("ingot_meteorite_forged", 1), ingredient("ingot_meteorite_forged", 1));
        addHotSmithing(3, item("meteorite_sword_reforged", 1), ingredient("meteorite_sword_seared", 1), ingredient("ingot_meteorite_forged", 1));
        addHotSmithing(3, item("cobalt_decorated_sword", 1), ingredient("cobalt_sword", 1), ingredient("ingot_meteorite", 1));
        addHotSmithing(3, item("cobalt_decorated_pickaxe", 1), ingredient("cobalt_pickaxe", 1), ingredient("ingot_meteorite", 1));
        addHotSmithing(3, item("cobalt_decorated_axe", 1), ingredient("cobalt_axe", 1), ingredient("ingot_meteorite", 1));
        addHotSmithing(3, item("cobalt_decorated_shovel", 1), ingredient("cobalt_shovel", 1), ingredient("ingot_meteorite", 1));
        addHotSmithing(3, item("cobalt_decorated_hoe", 1), ingredient("cobalt_hoe", 1), ingredient("ingot_meteorite", 1));
        addSmithing(1, item("ingot_gunmetal", 1), ingredient("ingot_copper", 1, mc("copper_ingot")), ingredient("ingot_aluminium", 1));
        registerMoldSmithingRecipes();
    }

    private static void registerConstruction() {
        Set<String> plateMaterials = new LinkedHashSet<>();
        addLegacyBat9000RecyclingRecipe();
        addPlateRecipe(plateMaterials, "iron", 3, mc("iron_ingot"));
        addPlateRecipe(plateMaterials, "gold", 3, mc("gold_ingot"));
        addPlateRecipe(plateMaterials, "copper", 3, mc("copper_ingot"), hbm("ingot_copper"));

        addConstruction(
                List.of(
                        tagIngredient(ItemTags.PLANKS, 16, mc("oak_planks")),
                        ingredient("plate_steel", 6),
                        ingredient("ingot_copper", 8, mc("copper_ingot")),
                        ingredient("ingot_iron", 4, mc("iron_ingot")),
                        ingredient("sawblade", 1)
                ),
                blockItem("machine_sawmill", 1),
                2
        );

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!ReinhardtsHBM.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            String path = id.getPath();
            if (!path.startsWith("ingot_")) {
                continue;
            }

            String material = path.substring("ingot_".length());
            if (NON_STANDARD_PLATE_MATERIALS.contains(material)) {
                continue;
            }
            addPlateRecipe(plateMaterials, material, 3, id);
        }

        addOneToOne("powder_coal", new ItemStack(Items.COAL), 3);
        addOneToOne("powder_quartz", new ItemStack(Items.QUARTZ), 3);
        addOneToOne("powder_lapis", new ItemStack(Items.LAPIS_LAZULI), 3);
        addOneToOne("powder_diamond", new ItemStack(Items.DIAMOND), 3);
        addOneToOne("powder_emerald", new ItemStack(Items.EMERALD), 3);

        addFuelPlateRecipe("ingot_u233", "plate_fuel_u233");
        addFuelPlateRecipe("ingot_u235", "plate_fuel_u235");
        addFuelPlateRecipe("ingot_mox_fuel", "plate_fuel_mox");
        addFuelPlateRecipe("ingot_pu239", "plate_fuel_pu239");
        addFuelPlateRecipe("ingot_schrabidium", "plate_fuel_sa326");
        addFuelPlateRecipe("billet_ra226be", "plate_fuel_ra226be");
        addFuelPlateRecipe("billet_pu238be", "plate_fuel_pu238be");

        addConstruction(List.of(ingredient("ingot_aluminium", 1)), blockItem("deco_aluminium", 4), 1);
        addConstruction(List.of(ingredient("ingot_beryllium", 1)), blockItem("deco_beryllium", 4), 1);
        addConstruction(List.of(ingredient("ingot_lead", 1)), blockItem("deco_lead", 4), 1);
        addConstruction(List.of(ingredient("ingot_red_copper", 1)), blockItem("deco_red_copper", 4), 1);
        addConstruction(List.of(ingredient("ingot_steel", 1)), blockItem("deco_steel", 4), 1);
        addConstruction(List.of(ingredient("ingot_titanium", 1)), blockItem("deco_titanium", 4), 1);
        addConstruction(List.of(ingredient("ingot_tungsten", 1)), blockItem("deco_tungsten", 4), 1);
        addConstruction(List.of(ingredient("ingot_asbestos", 1)), blockItem("deco_asbestos", 4), 1);

        addConstruction(List.of(ingredient("coil_copper", 2)), item("coil_copper_torus", 1), 1);
        addShellRecipe("titanium", 1);
        addShellRecipe("copper", 1);
        addShellRecipe("aluminium", 1);
        addShellRecipe("steel", 1);
        addShellRecipe("weaponsteel", 1);
        addShellRecipe("saturnite", 1);
        addConstruction(List.of(ingredient("plate_copper", 3)), item("pipe", 1), 1);
        addConstruction(List.of(ingredient("coil_advanced_alloy", 2)), item("coil_advanced_torus", 1), 1);
        addConstruction(List.of(ingredient("coil_gold", 2)), item("coil_gold_torus", 1), 1);
        addConstruction(
                List.of(ingredient("plate_iron", 2), ingredient("coil_copper", 1), ingredient("coil_copper_torus", 1)),
                item("motor", 2),
                1
        );
        addConstruction(
                List.of(ingredient("plate_lead", 3)),
                item("pipe_lead", 1),
                1
        );
        addConstruction(
                List.of(ingredient("motor", 1), ingredient("ingot_polymer", 2), ingredient("ingot_desh", 2), ingredient("coil_gold_torus", 1)),
                item("motor_desh", 1),
                3
        );
        registerConstructionSirens();
        addConstruction(
                List.of(
                        tagIngredient(commonTag("ingots/desh"), 4, hbm("ingot_desh")),
                        tagIngredient(commonTag("dusts/plastic"), 2, hbm("powder_polymer")),
                        tagIngredient(commonTag("ingots/dura_steel"), 1, hbm("ingot_dura_steel"))
                ),
                item("plate_desh", 4),
                3
        );
        addConstruction(
                List.of(
                        ingredient("reinforced_stone", 16),
                        ingredient("plate_steel", 12),
                        foundryShapeIngredient(FoundryShape.SHELL, "steel", 2, HbmItems.SHELL.get()),
                        ingredient("coil_copper", 4),
                        ingredient("gear_large", 1)
                ),
                blockItem("machine_steam_engine", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("sulfur", 12),
                        foundryShapeIngredient(FoundryShape.SHELL, "steel", 4, HbmItems.SHELL.get()),
                        foundryShapeIngredient(FoundryShape.CAST_PLATE, "copper", 6, HbmItems.PLATE_CAST.get()),
                        ingredient("circuit_basic", 2)
                ),
                blockItem("machine_deuterium_extractor", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("deuterium_filter", 2),
                        foundryShapeIngredient(FoundryShape.SHELL, "steel", 5, HbmItems.SHELL.get()),
                        tagIngredient(commonTag("pipes/steel"), 12, hbm("pipe_steel")),
                        ingredient("concrete_asbestos", 8),
                        ingredient("steel_scaffold", 16),
                        fluidIngredient("sourgas", 8_000)
                ),
                blockItem("machine_deuterium_tower", 1),
                4
        );
        addConstruction(
                List.of(
                        ingredient("ingot_firebrick", 20),
                        ingredient("ingot_copper", 8, mc("copper_ingot")),
                        ingredient("plate_steel", 8)
                ),
                blockItem("machine_crucible", 1),
                2
        );
        registerPileRecipes();
        addConstruction(
                List.of(
                        ingredient("ingot_steel", 4),
                        foundryShapeIngredient(FoundryShape.CAST_PLATE, "copper", 16, HbmItems.PLATE_CAST.get()),
                        ingredient("plate_polymer", 8)
                ),
                blockItem("heat_boiler", 1),
                2
        );
        addConstruction(
                List.of(
                        foundryShapeIngredient(FoundryShape.CAST_PLATE, "steel", 8, HbmItems.PLATE_CAST.get()),
                        ingredient("ingot_copper", 8, mc("copper_ingot")),
                        ingredient("ingot_polymer", 4)
                ),
                blockItem("machine_industrial_boiler", 1),
                3
        );
        addConstruction(
                List.of(
                        tagIngredient(ItemTags.PLANKS, 16, mc("oak_planks")),
                        ingredient("plate_steel", 6),
                        ingredient("ingot_copper", 8, mc("copper_ingot")),
                        ingredient("coil_copper", 4),
                        ingredient("gear_large", 1)
                ),
                blockItem("machine_stirling", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("plate_steel", 16),
                        ingredient("ingot_beryllium", 6),
                        ingredient("ingot_copper", 8, mc("copper_ingot")),
                        ingredient("coil_gold", 16),
                        ingredient("gear_large", 1)
                ),
                blockItem("machine_stirling_steel", 1),
                2
        );
        addConstruction(
                List.of(ingredient("tank_steel", 1), ingredient("plate_lead", 2), ingredient("nuclear_waste", 10)),
                blockItem("yellow_barrel", 1),
                3
        );
        addConstruction(
                List.of(ingredient("tank_steel", 1), ingredient("plate_lead", 2), ingredient("nuclear_waste_vitrified", 10)),
                blockItem("vitrified_barrel", 1),
                3
        );
        registerStampConstructionRecipes();
        addConstruction(
                List.of(ingredient(mc("stone_bricks"), 4), ingredient("ingot_firebrick", 4), ingredient("plate_copper", 4)),
                blockItem("machine_difurnace_off", 1),
                1
        );
        addConstruction(
                List.of(ingredient(mc("stone_bricks"), 4), ingredient("ingot_firebrick", 32), ingredient("plate_copper", 8)),
                blockItem("machine_blast_furnace", 1),
                1
        );
        addConstruction(
                List.of(
                        ingredient(mc("stone_bricks"), 8),
                        ingredient("ingot_firebrick", 16),
                        tagIngredient(commonTag("ingots/iron"), 4, mc("iron_ingot")),
                        ingredient("plate_copper", 8)
                ),
                blockItem("machine_rotary_furnace", 1),
                2
        );
        // MachineAnnihilator, AnvilRecipes#registerConstructionRecipes (1.7.10).
        addConstruction(
                List.of(
                        ingredient(mc("stone_bricks"), 16),
                        tagIngredient(commonTag("ingots/firebrick"), 16, hbm("ingot_firebrick")),
                        tagIngredient(commonTag("ingots/iron"), 8, mc("iron_ingot")),
                        tagIngredient(commonTag("ingots/copper"), 8, hbm("ingot_copper"))
                ),
                blockItem("machine_annihilator", 1),
                2
        );
        addConstruction(
                List.of(ingredient("ingot_steel", 8), ingredient("plate_copper", 4), ingredient("motor", 2), ingredient("circuit_vacuum_tube", 4)),
                blockItem("machine_assembly_machine", 1),
                2
        );
        addConstruction(
                List.of(
                        tagIngredient(commonTag("cobblestones"), 8, mc("cobblestone")),
                        tagIngredient(ItemTags.PLANKS, 16, mc("oak_planks")),
                        ingredient("plate_copper", 8),
                        ingredient("pipe_lead", 2)
                ),
                blockItem("pump_steam", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient(mc("stone_bricks"), 8),
                        ingredient("plate_steel", 16),
                        ingredient("pipe_lead", 4),
                        ingredient("motor", 2),
                        ingredient("circuit_vacuum_tube", 4)
                ),
                blockItem("pump_electric", 1),
                3
        );
        addConstruction(
                List.of(
                        foundryShapeIngredient(FoundryShape.CAST_PLATE, "steel", 2, HbmItems.PLATE_CAST.get()),
                        ingredient("coil_copper", 4),
                        ingredient("bolt_tungsten", 4),
                        ingredient("circuit_vacuum_tube", 2)
                ),
                blockItem("machine_soldering_station", 1),
                2
        );
        addConstruction(
                List.of(
                        foundryShapeIngredient(FoundryShape.CAST_PLATE, "steel", 4, HbmItems.PLATE_CAST.get()),
                        ingredient("ingot_tungsten", 8),
                        ingredient("machine_transformer", 1),
                        ingredient("arc_electrode", 2)
                ),
                blockItem("machine_arc_welder", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("brick_concrete", 64),
                        ingredient(mc("iron_bars"), 128),
                        ingredient("machine_condenser", 4)
                ),
                blockItem("machine_tower_small", 1),
                3
        );
        addConstruction(
                List.of(
                        ingredient("concrete_smooth", 128),
                        ingredient("steel_scaffold", 32),
                        ingredient("machine_condenser", 16),
                        ingredient("pipe_steel", 8)
                ),
                blockItem("machine_tower_large", 1),
                4
        );
        addConstruction(
                List.of(
                        tagIngredient(commonTag("concretes"), 2, hbm("brick_concrete")),
                        ingredient("steel_scaffold", 8),
                        ingredient("plate_polymer", 8),
                        ingredient("coil_copper", 4)
                ),
                blockItem("red_pylon_large", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("red_connector", 4),
                        ingredient("steel_scaffold", 2),
                        ingredient("plate_polymer", 8),
                        ingredient("coil_copper", 4)
                ),
                blockItem("connector_red_super", 1),
                2
        );
        addConstruction(
                List.of(
                        tagIngredient(commonTag("concretes"), 8, hbm("brick_concrete")),
                        ingredient("ingot_steel", 8),
                        ingredient("plate_polymer", 12),
                        ingredient("coil_copper", 8)
                ),
                blockItem("substation", 2),
                2
        );
        addConstruction(
                List.of(ingredient(mc("furnace"), 1), ingredient("plate_steel", 8), ingredient("ingot_copper", 8, mc("copper_ingot"))),
                blockItem("heater_firebox", 1),
                2
        );
        addConstruction(
                List.of(ingredient("ingot_firebrick", 16), ingredient("plate_steel", 4), ingredient("ingot_copper", 8, mc("copper_ingot"))),
                blockItem("heater_oven", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("plate_steel", 8),
                        ingredient("ingot_iron", 12),
                        ingredient("ingot_copper", 2),
                        ingredient("circuit_vacuum_tube", 1)
                ),
                blockItem("machine_thresher", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("plate_steel", 4),
                        ingredient("ingot_iron", 12),
                        ingredient("ingot_copper", 2),
                        ingredient("circuit_vacuum_tube", 2),
                        ingredient("sawblade", 1)
                ),
                blockItem("machine_autosaw", 1),
                2
        );
        addConstruction(
                List.of(ingredient("tank_steel", 4), ingredient("pipe_steel", 3), ingredient("ingot_titanium", 12), ingredient("ingot_copper", 8, mc("copper_ingot"))),
                blockItem("heater_oilburner", 1),
                2
        );
        addConstruction(
                List.of(ingredient("ingot_polymer", 4), ingredient("ingot_copper", 8, mc("copper_ingot")), ingredient("plate_steel", 8), ingredient("coil_tungsten", 8), ingredient("circuit", 1)),
                blockItem("heater_electric", 1),
                3
        );
        addConstruction(
                List.of(ingredient("ingot_rubber", 4), ingredient("ingot_copper", 16, mc("copper_ingot")), ingredient("plate_steel", 16), ingredient("pipe_steel", 3)),
                blockItem("heater_heatex", 1),
                3
        );
        addConstruction(
                List.of(
                        ingredient(mc("stone"), 8),
                        ingredient("plate_steel", 2),
                        ingredient("ingot_iron", 4, mc("iron_ingot"))
                ),
                blockItem("machine_ashpit", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("plate_steel", 4),
                        ingredient(mc("bricks"), 16),
                        ingredient("steel_grate", 2)
                ),
                blockItem("chimney_brick", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient("plate_steel", 16),
                        tagIngredient(commonTag("concretes"), 64, hbm("brick_concrete")),
                        ingredient("steel_grate", 4),
                        ingredient("filter_coal", 4)
                ),
                blockItem("chimney_industrial", 1),
                3
        );
        // AnvilRecipes#constructionRecipes, HBM 1.7.10: combination furnace.
        addConstruction(
                List.of(
                        ingredient(mc("stone_bricks"), 8),
                        tagIngredient(ItemTags.LOGS, 16, mc("oak_log")),
                        foundryShapeIngredient(FoundryShape.CAST_PLATE, "copper", 2, HbmItems.PLATE_CAST.get()),
                        ingredient(mc("bricks"), 16)
                ),
                blockItem("furnace_combination", 1),
                2
        );
        addConstruction(
                List.of(
                        ingredient(mc("stone_bricks"), 16),
                        tagIngredient(commonTag("ingots/iron"), 4, mc("iron_ingot")),
                        ingredient("plate_steel", 16),
                        ingredient("ingot_copper", 8, mc("copper_ingot")),
                        ingredient("steel_grate", 16)
                ),
                blockItem("furnace_steel", 1),
                2
        );
    }

    private static void registerPileRecipes() {
        addConstruction(
                List.of(ingredient("billet_uranium", 3), ingredient("plate_iron", 2)),
                item("pile_rod_uranium", 1),
                2
        );
        addConstruction(
                List.of(ingredient("billet_ra226be", 3), ingredient("plate_iron", 2)),
                item("pile_rod_source", 1),
                2
        );
        addConstruction(
                List.of(ingredient("ingot_boron", 2), ingredient(mc("stick"), 2)),
                item("pile_rod_boron", 1),
                2
        );
        addConstruction(
                List.of(ingredient("ingot_boron", 2), ingredient("motor", 1), ingredient("circuit_vacuum_tube", 1)),
                item("pile_rod_detector", 1),
                2
        );
        addConstruction(
                List.of(ingredient("lithium", 1), ingredient("cell_empty", 1)),
                item("pile_rod_lithium", 1),
                2
        );

        if (HbmConfig.ENABLE_528_MODE.get()) {
            addConstruction(
                    List.of(ingredient("billet_pu_mix", 2), ingredient("billet_uranium", 1), ingredient("plate_iron", 2)),
                    item("pile_rod_plutonium", 1),
                    2
            );
            addConstruction(
                    List.of(ingredient("billet_pu239", 1), ingredient("billet_pu_mix", 1), ingredient("billet_uranium", 1), ingredient("plate_iron", 2)),
                    item("pile_rod_pu239", 1),
                    2
            );
            return;
        }

        addConstruction(
                List.of(ingredient("billet_pu_mix", 2), ingredient("billet_nuclear_waste", 1), ingredient("plate_iron", 1)),
                item("pile_rod_plutonium", 1),
                2
        );
        addConstruction(
                List.of(ingredient("billet_pu239", 1), ingredient("billet_pu_mix", 1), ingredient("billet_nuclear_waste", 1), ingredient("plate_iron", 2)),
                item("pile_rod_pu239", 1),
                2
        );
    }

    private static void registerMoldSmithingRecipes() {
        Optional<AnvilIngredient> blank = ingredientOptional("mold_base", 1);
        addMoldSmithing(0, ingredient(mc("gold_nugget"), 1), blank);
        addMoldSmithing(1, ingredient("billet_uranium", 1), blank);
        addMoldSmithing(2, tagIngredient(commonTag("ingots/iron"), 1, mc("iron_ingot")), blank);
        addMoldSmithing(3, tagIngredient(commonTag("plates/iron"), 1, hbm("plate_iron")), blank);
        addMoldSmithing(19, foundryShapeIngredient(FoundryShape.CAST_PLATE, "iron", 1, HbmItems.PLATE_CAST.get()), blank);
        addMoldSmithing(15, foundryShapeIngredient(FoundryShape.CAST_PLATE, "iron", 3, HbmItems.PLATE_CAST.get()), blank);
        addMoldSmithing(4, foundryShapeIngredient(FoundryShape.WIRE, "red_copper", 1, HbmItems.WIRE_FINE.get()), blank);
        addMoldSmithing(5, ingredients(1, "blade_titanium", "blade_tungsten"), blank);
        addMoldSmithing(6, ingredients(1, "blades_steel", "blades_titanium", "blades_advanced_alloy"), blank);
        addMoldSmithing(8, foundryShapeIngredient(FoundryShape.SHELL, "steel", 1, HbmItems.SHELL.get()), blank);
        addMoldSmithing(9, tagIngredient(commonTag("pipes/steel"), 1, hbm("pipe_steel")), blank);
        addMoldSmithing(10, tagIngredient(commonTag("ingots/iron"), 9, mc("iron_ingot")), blank);
        addMoldSmithing(11, tagIngredient(commonTag("plates/iron"), 9, hbm("plate_iron")), blank);
        addMoldSmithing(12, ingredient(mc("iron_block"), 1), blank);
        addMoldSmithing(13, ingredient("pipes_steel", 1), blank);
        addMoldSmithing(20, foundryShapeIngredient(FoundryShape.DENSE_WIRE, "advanced_alloy", 1, HbmItems.WIRE_DENSE.get()), blank);
        addMoldSmithing(21, foundryShapeIngredient(FoundryShape.DENSE_WIRE, "advanced_alloy", 9, HbmItems.WIRE_DENSE.get()), blank);
    }

    private static void registerStampConstructionRecipes() {
        addStampFamily("stone", 1);
        addStampFamily("iron", 1);
        addStampFamily("steel", 2);
        addStampFamily("titanium", 2);
        addStampFamily("obsidian", 2);
        addStampFamily("desh", 3);

        addConstruction(
                List.of(ingredient("stamp_iron_flat", 1), ingredient("ingot_gunmetal", 2)),
                item("stamp_9", 1),
                2
        );
        addConstruction(
                List.of(ingredient("stamp_iron_flat", 1), ingredient("ingot_gunmetal", 2)),
                item("stamp_50", 1),
                2
        );
        addConstruction(
                List.of(ingredient("stamp_desh_flat", 1), ingredient("ingot_weaponsteel", 4)),
                item("stamp_desh_9", 1),
                4
        );
        addConstruction(
                List.of(ingredient("stamp_desh_flat", 1), ingredient("ingot_weaponsteel", 4)),
                item("stamp_desh_50", 1),
                4
        );
    }

    private static void addStampFamily(String material, int tier) {
        String base = "stamp_" + material + "_flat";
        addConstruction(List.of(ingredient(base, 1)), item("stamp_" + material + "_plate", 1), tier);
        addConstruction(List.of(ingredient(base, 1)), item("stamp_" + material + "_wire", 1), tier);
        addConstruction(List.of(ingredient(base, 1)), item("stamp_" + material + "_circuit", 1), tier);
    }

    private static void addShellRecipe(String material, int tier) {
        addConstruction(
                List.of(ingredient("plate_" + material, 4)),
                foundryShapeStack(FoundryShape.SHELL, material, 1, HbmItems.SHELL.get()),
                tier
        );
    }

    private static void addFuelPlateRecipe(String inputId, String outputId) {
        item(outputId, 1).ifPresent(output -> addOneToOne(inputId, output, 4));
    }

    private static void registerConstructionSirens() {
        for (int trackId = 1; trackId <= 20; trackId++) {
            addConstruction(
                    List.of(ingredient("plate_steel", 1), ingredient("plate_polymer", 1)),
                    sirenTrack(trackId),
                    2
            );
        }
    }

    private static Optional<ItemStack> sirenTrack(int trackId) {
        if (trackId <= 0 || trackId >= SirenTrackItem.Track.values().length) {
            return Optional.empty();
        }
        ItemStack stack = new ItemStack(HbmItems.SIREN_TRACK.get());
        stack.setDamageValue(trackId);
        return Optional.of(stack);
    }

    private static void addAnvilUpgrade(ItemLike baseAnvil, String ingotId, ItemLike outputAnvil) {
        Optional<AnvilIngredient> ingot = ingredientOptional(ingotId, 10);
        ingot.ifPresent(anvilIngot -> SMITHING.add(new AnvilSmithingRecipe(
                1,
                new ItemStack(outputAnvil),
                AnvilIngredient.of(baseAnvil, 1),
                anvilIngot
        )));
    }

    private static void addSmithing(int tier, Optional<ItemStack> output, Optional<AnvilIngredient> left, Optional<AnvilIngredient> right) {
        if (output.isPresent() && left.isPresent() && right.isPresent()) {
            SMITHING.add(new AnvilSmithingRecipe(tier, output.get(), left.get(), right.get()));
        }
    }

    private static void addHotSmithing(int tier, Optional<ItemStack> output, Optional<AnvilIngredient> left, Optional<AnvilIngredient> right) {
        if (output.isPresent() && left.isPresent() && right.isPresent()) {
            SMITHING.add(new AnvilSmithingHotRecipe(tier, output.get(), left.get(), right.get()));
        }
    }

    private static void addMoldSmithing(int moldId, Optional<AnvilIngredient> reference, Optional<AnvilIngredient> moldBase) {
        if (reference.isPresent() && moldBase.isPresent()) {
            SMITHING.add(new AnvilMoldSmithingRecipe(
                    1,
                    FoundryMoldItem.stackFor(HbmItems.MOLD.get(), moldId),
                    reference.get(),
                    moldBase.get()
            ));
        }
    }

    private static void addPlateRecipe(Set<String> registeredMaterials, String material, int tier, ResourceLocation... inputIds) {
        if (!registeredMaterials.add(material)) {
            return;
        }

        Optional<AnvilIngredient> input = AnvilIngredient.ofExisting(1, inputIds);
        Optional<ItemStack> output = stack(hbm("plate_" + material), 1);
        if (input.isPresent() && output.isPresent()) {
            CONSTRUCTION.add(AnvilConstructionRecipe.oneToOne(input.get(), output.get(), tier));
        }
    }

    private static void addOneToOne(String inputId, ItemStack output, int tier) {
        ingredientOptional(inputId, 1).ifPresent(input -> CONSTRUCTION.add(AnvilConstructionRecipe.oneToOne(input, output, tier)));
    }

    private static void addConstruction(List<Optional<AnvilIngredient>> inputOptions, Optional<ItemStack> output, int tier) {
        if (output.isEmpty()) {
            return;
        }

        List<AnvilIngredient> inputs = new ArrayList<>();
        for (Optional<AnvilIngredient> input : inputOptions) {
            if (input.isEmpty()) {
                return;
            }
            inputs.add(input.get());
        }
        CONSTRUCTION.add(AnvilConstructionRecipe.construction(inputs, output.get(), tier));
    }

    private static void addLegacyBat9000RecyclingRecipe() {
        FoundryMaterial tcalloy = FoundryMaterial.get("tcalloy");
        Optional<ItemStack> steelPlates = item("plate_steel", 16);
        if (tcalloy == null || steelPlates.isEmpty()) {
            return;
        }

        CONSTRUCTION.add(new AnvilConstructionRecipe(
                List.of(AnvilIngredient.of(HbmBlocks.MACHINE_BAT9000.get(), 1)),
                List.of(
                        new AnvilOutput(FoundryShapeItem.stackFor(HbmItems.PLATE_WELDED.get(), tcalloy, 4)),
                        new AnvilOutput(steelPlates.get())
                ),
                3,
                -1,
                AnvilConstructionRecipe.OverlayType.RECYCLING
        ));
    }

    private static Optional<AnvilIngredient> ingredientOptional(String path, int count) {
        return AnvilIngredient.ofExisting(count, hbm(path));
    }

    private static Optional<AnvilIngredient> ingredient(String path, int count) {
        return ingredientOptional(path, count);
    }

    private static Optional<AnvilIngredient> ingredient(String path, int count, ResourceLocation fallback) {
        return AnvilIngredient.ofExisting(count, hbm(path), fallback);
    }

    private static Optional<AnvilIngredient> ingredient(ResourceLocation itemId, int count) {
        return AnvilIngredient.ofExisting(count, itemId);
    }

    private static Optional<AnvilIngredient> ingredients(int count, String... paths) {
        ResourceLocation[] ids = new ResourceLocation[paths.length];
        for (int index = 0; index < paths.length; index++) {
            ids[index] = hbm(paths[index]);
        }
        return AnvilIngredient.ofExisting(count, ids);
    }

    private static Optional<AnvilIngredient> tagIngredient(TagKey<Item> tag, int count, ResourceLocation displayItemId) {
        return AnvilIngredient.ofTag(count, tag, displayItemId);
    }

    private static Optional<AnvilIngredient> foundryShapeIngredient(FoundryShape shape, String materialName, int count, Item item) {
        FoundryMaterial material = FoundryMaterial.get(materialName);
        if (!FoundryShapeItem.supports(shape, material)) {
            return Optional.empty();
        }
        return AnvilIngredient.ofStacks(count, FoundryShapeItem.stackFor(item, material));
    }

    private static Optional<AnvilIngredient> fluidIngredient(String fluidName, int amount) {
        return HbmFluids.byName(fluidName).flatMap(fluid -> AnvilIngredient.ofFluid(fluid, amount));
    }

    private static Optional<ItemStack> foundryShapeStack(FoundryShape shape, String materialName, int count, Item item) {
        FoundryMaterial material = FoundryMaterial.get(materialName);
        if (!FoundryShapeItem.supports(shape, material)) {
            return Optional.empty();
        }
        return Optional.of(FoundryShapeItem.stackFor(item, material, count));
    }

    private static Optional<ItemStack> item(String path, int count) {
        return stack(hbm(path), count);
    }

    private static Optional<ItemStack> blockItem(String path, int count) {
        return stack(hbm(path), count);
    }

    private static Optional<ItemStack> stack(ResourceLocation id, int count) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(BuiltInRegistries.ITEM.get(id), count));
    }

    private static ResourceLocation hbm(String path) {
        return ReinhardtsHBM.id(path);
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
