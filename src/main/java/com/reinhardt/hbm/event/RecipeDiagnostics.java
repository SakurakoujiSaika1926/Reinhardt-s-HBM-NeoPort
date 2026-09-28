package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FoundryCastingBlockEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialShapes;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import com.reinhardt.hbm.recipe.anvil.AnvilConstructionRecipe;
import com.reinhardt.hbm.recipe.anvil.AnvilIngredient;
import com.reinhardt.hbm.recipe.anvil.AnvilOutput;
import com.reinhardt.hbm.recipe.anvil.AnvilSmithingRecipe;
import com.reinhardt.hbm.recipe.anvil.HbmAnvilRecipes;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class RecipeDiagnostics {
    private static final String AUDIT_ENABLED_PROPERTY = "reinhardtshbm.recipeAudit";
    private static final String AUDIT_REPORT_PROPERTY = "reinhardtshbm.recipeAuditReport";
    private static final List<String> LEGACY_SMELTABLE_FOUNDRY_MATERIALS = List.of(
            "stone", "iron", "gold", "redstone", "obsidian", "wrought_iron", "pig_iron", "meteoric_iron",
            "uranium", "u233", "u235", "u238", "thorium", "plutonium", "pu_mix", "pu238", "pu239",
            "pu240", "pu241", "am_mix", "am241", "am242", "neptunium", "polonium", "technetium",
            "radium", "actinium", "co60", "au198", "pb209", "schrabidium", "solinium", "schrabidate",
            "schraranium", "ghiorsium", "titanium", "copper", "tungsten", "aluminium", "lead", "bismuth",
            "arsenic", "tantalium", "neodymium", "niobium", "beryllium", "cobalt", "boron", "borax",
            "lanthanium", "zirconium", "sodium", "strontium", "calcium", "lithium", "cadmium", "silicon",
            "asbestos", "osmiridium", "steel", "red_copper", "dura_steel", "desh", "starmetal",
            "ferrouranium", "tcalloy", "cdalloy", "bismuth_bronze", "arsenic_bronze", "bscco",
            "magnetized_tungsten", "combine_steel", "dineutronium", "slag", "mud", "gunmetal",
            "weaponsteel", "saturnite"
    );
    private static final List<String> PETROLEUM_ASSEMBLY_RECIPES = List.of(
            "machine_well",
            "machine_pumpjack",
            "machine_fracking_tower",
            "machine_flare",
            "machine_refinery",
            "machine_catalytic_cracker",
            "machine_coker",
            "machine_vacuum_distill",
            "machine_catalytic_reformer",
            "machine_hydrotreater",
            "machine_liquefactor",
            "machine_solidifier",
            "machine_diesel"
    );

    private RecipeDiagnostics() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = event.getServer()
                .getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get());
        List<RecipeHolder<AssemblyMachineRecipe>> noBlueprintRecipes = AssemblyMachineRecipe.activeVariants(
                recipes.stream()
                        .filter(holder -> holder.value().isVisibleForPool(Optional.empty()))
                        .toList()
        );
        List<String> missing = PETROLEUM_ASSEMBLY_RECIPES.stream()
                .filter(path -> !hasRecipe(recipes, path) && !hasRecipe(recipes, path + "_expensive"))
                .toList();
        List<String> hiddenWithoutBlueprint = HbmConfig.ENABLE_528_MODE.get()
                ? List.of()
                : PETROLEUM_ASSEMBLY_RECIPES.stream()
                        .filter(path -> !hasRecipe(noBlueprintRecipes, path))
                        .toList();

        ReinhardtsHBM.LOGGER.info(
                "HBM assembly recipe diagnostic: loaded={}, no-blueprint-visible={}, pumpjack={}, pumpjack_expensive={}, petroleum_missing={}, petroleum_hidden_without_blueprint={}",
                recipes.size(),
                noBlueprintRecipes.size(),
                hasRecipe(recipes, "machine_pumpjack"),
                hasRecipe(recipes, "machine_pumpjack_expensive"),
                missing,
                hiddenWithoutBlueprint
        );

        if (Boolean.getBoolean(AUDIT_ENABLED_PROPERTY)) {
            if (!hiddenWithoutBlueprint.isEmpty()) {
                throw new IllegalStateException("Non-528 petroleum assembly recipes incorrectly require blueprints: "
                        + hiddenWithoutBlueprint);
            }
            FuelCycleRecipeDiagnostics.validate(event.getServer());
            writeAuditReport(event.getServer());
            event.getServer().halt(false);
        }
    }

    private static void writeAuditReport(MinecraftServer server) {
        String reportName = System.getProperty(AUDIT_REPORT_PROPERTY);
        if (reportName == null || reportName.isBlank()) {
            throw new IllegalStateException("Recipe audit mode requires -D" + AUDIT_REPORT_PROPERTY + "=<path>");
        }

        List<String> report = new ArrayList<>();
        BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(ReinhardtsHBM.MOD_ID))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(id -> report.add("ITEM\t" + id));
        BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> id.getNamespace().equals(ReinhardtsHBM.MOD_ID))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(id -> report.add("BLOCK\t" + id));
        BuiltInRegistries.FLUID.keySet().stream()
                .filter(id -> id.getNamespace().equals(ReinhardtsHBM.MOD_ID))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(id -> report.add("FLUID\t" + id));
        BuiltInRegistries.ITEM.getTags()
                .forEach(pair -> report.add("ITEM_TAG\t" + pair.getFirst().location() + "\t" + pair.getSecond().size()));
        BuiltInRegistries.FLUID.getTags()
                .forEach(pair -> report.add("FLUID_TAG\t" + pair.getFirst().location() + "\t" + pair.getSecond().size()));
        server.getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().getNamespace().equals(ReinhardtsHBM.MOD_ID))
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .forEach(holder -> {
                    report.add("RECIPE\t" + holder.id() + "\t" + BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()));
                    ItemStack result = holder.value().getResultItem(server.registryAccess());
                    if (!result.isEmpty()) {
                        report.add("RECIPE_OUTPUT\t" + holder.id() + "\t" + BuiltInRegistries.ITEM.getKey(result.getItem()) + "\t" + result.getCount());
                    }
                    for (int inputIndex = 0; inputIndex < holder.value().getIngredients().size(); inputIndex++) {
                        int resolvedInputIndex = inputIndex;
                        var ingredient = holder.value().getIngredients().get(inputIndex);
                        if (ingredient.isEmpty()) {
                            continue;
                        }
                        ItemStack[] matchingStacks = ingredient.getItems();
                        if (matchingStacks.length == 0) {
                            report.add("RECIPE_INPUT_ERROR\t" + holder.id() + "\t" + inputIndex
                                    + "\tresolves to no registered items");
                            continue;
                        }
                        java.util.Arrays.stream(matchingStacks)
                                .filter(stack -> !stack.isEmpty())
                                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                                .distinct()
                                .sorted(Comparator.comparing(ResourceLocation::toString))
                                .forEach(itemId -> report.add("RECIPE_INPUT\t" + holder.id() + "\t"
                                        + resolvedInputIndex + "\t" + itemId));
                    }
                });
        writeAnvilAudit(report);
        writeFoundryAudit(server, report);

        Path output = Path.of(reportName).toAbsolutePath().normalize();
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(output, report, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write HBM recipe audit report to " + output, exception);
        }
        ReinhardtsHBM.LOGGER.info(
                "HBM_RECIPE_AUDIT_COMPLETE items={} blocks={} fluids={} loaded_recipes={} anvil_construction={} anvil_smithing={} report={}",
                report.stream().filter(line -> line.startsWith("ITEM\t")).count(),
                report.stream().filter(line -> line.startsWith("BLOCK\t")).count(),
                report.stream().filter(line -> line.startsWith("FLUID\t")).count(),
                report.stream().filter(line -> line.startsWith("RECIPE\t")).count(),
                report.stream().filter(line -> line.startsWith("ANVIL_CONSTRUCTION\t")).count(),
                report.stream().filter(line -> line.startsWith("ANVIL_SMITHING\t")).count(),
                output
        );
    }

    private static void writeFoundryAudit(MinecraftServer server, List<String> report) {
        Set<String> producingMolds = new HashSet<>();
        for (FoundryMoldItem.Mold mold : FoundryMoldItem.molds()) {
            for (FoundryMaterial material : FoundryMaterial.ordered()) {
                mold.outputFor(material).ifPresent(output -> {
                    producingMolds.add(mold.name());
                    report.add("FOUNDRY_OUTPUT\t" + mold.name() + "\t" + material.name() + "\t"
                            + BuiltInRegistries.ITEM.getKey(output.getItem()) + "\t" + output.getCount());
                });
            }
        }
        for (FoundryMoldItem.Mold mold : FoundryMoldItem.molds()) {
            if (!producingMolds.contains(mold.name())) {
                throw new IllegalStateException("Foundry mold has no reachable casting output: " + mold.name());
            }
        }

        validateLegacyFoundryMaterialRegistry();
        validateRegisteredFoundryOutputs(report);
        validateLegacySpecialFoundryOutputs(report);
        validateEveryFoundryMoldCasting(server, report);
        validateFoundryBasinCasting(server, report, "plates", "plate_dura_steel", 9);
        validateFoundryBasinCasting(server, report, "plates_cast", "plate_cast_dura_steel", 3);
    }

    private static void validateLegacyFoundryMaterialRegistry() {
        for (String materialName : LEGACY_SMELTABLE_FOUNDRY_MATERIALS) {
            FoundryMaterial material = FoundryMaterial.byName(materialName)
                    .orElseThrow(() -> new IllegalStateException("Missing legacy foundry material: " + materialName));
            if (material.behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE) {
                throw new IllegalStateException("Legacy foundry material is not smeltable: " + materialName);
            }
        }
    }

    /**
     * HBM 1.7.10 MoldShape resolved prefix+material through the ore dictionary,
     * preferring HBM's own item.  Every currently registered HBM material form
     * therefore has to remain reachable through its corresponding mold.
     */
    private static void validateRegisteredFoundryOutputs(List<String> report) {
        List<BasicMoldExpectation> basicMolds = List.of(
                new BasicMoldExpectation("nugget", "nugget", FoundryShape.NUGGET, 1),
                new BasicMoldExpectation("billet", "billet", FoundryShape.BILLET, 1),
                new BasicMoldExpectation("ingot", "ingot", FoundryShape.INGOT, 1),
                new BasicMoldExpectation("ingots", "ingot", FoundryShape.INGOT, 9),
                new BasicMoldExpectation("block", "block", FoundryShape.BLOCK, 1)
        );
        for (FoundryMaterial material : FoundryMaterial.ordered()) {
            if (material.behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE) {
                continue;
            }
            for (BasicMoldExpectation expectation : basicMolds) {
                ResourceLocation expectedId = ReinhardtsHBM.id(expectation.outputPrefix() + "_" + material.itemSuffix());
                if (BuiltInRegistries.ITEM.get(expectedId) == Items.AIR) {
                    continue;
                }
                validateFoundryOutput(report, expectation.moldName(), material.name(), expectedId, expectation.count());
                validateFoundryMaterialInput(report, expectedId, material.name(), expectation.shape().q(1));
            }
        }

        List<ShapeMoldExpectation> shapedMolds = List.of(
                new ShapeMoldExpectation("plate", FoundryShape.PLATE, 1),
                new ShapeMoldExpectation("plates", FoundryShape.PLATE, 9),
                new ShapeMoldExpectation("wire", FoundryShape.WIRE, 8),
                new ShapeMoldExpectation("shell", FoundryShape.SHELL, 1),
                new ShapeMoldExpectation("pipe", FoundryShape.PIPE, 1),
                new ShapeMoldExpectation("plate_cast", FoundryShape.CAST_PLATE, 1),
                new ShapeMoldExpectation("plates_cast", FoundryShape.CAST_PLATE, 3),
                new ShapeMoldExpectation("wire_dense", FoundryShape.DENSE_WIRE, 1),
                new ShapeMoldExpectation("wires_dense", FoundryShape.DENSE_WIRE, 9),
                new ShapeMoldExpectation("mechanism", FoundryShape.MECHANISM, 1)
        );
        for (ShapeMoldExpectation expectation : shapedMolds) {
            for (FoundryMaterial material : FoundryMaterial.ordered()) {
                if (material.behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE
                        || !FoundryMaterialShapes.supports(expectation.shape(), material)) {
                    continue;
                }
                ResourceLocation expectedId = ReinhardtsHBM.id(HbmItems.independentFoundryItemPath(
                        expectation.shape(), material));
                if (BuiltInRegistries.ITEM.get(expectedId) == Items.AIR) {
                    throw new IllegalStateException("Foundry shape table references an unregistered item: " + expectedId);
                }
                validateFoundryOutput(report, expectation.moldName(), material.name(), expectedId, expectation.count());
                validateFoundryMaterialInput(report, expectedId, material.name(), expectation.shape().q(1));
            }
        }
    }

    private static void validateLegacySpecialFoundryOutputs(List<String> report) {
        validateFoundryOutput(report, "block", "stone", ResourceLocation.withDefaultNamespace("stone"), 1);
        validateFoundryOutput(report, "block", "obsidian", ResourceLocation.withDefaultNamespace("obsidian"), 1);
        validateFoundryOutput(report, "stamp", "obsidian", ReinhardtsHBM.id("stamp_obsidian_flat"), 1);

        validateFoundryOutput(report, "nugget", "iron", ResourceLocation.withDefaultNamespace("iron_nugget"), 1);
        validateFoundryOutput(report, "ingot", "iron", ResourceLocation.withDefaultNamespace("iron_ingot"), 1);
        validateFoundryOutput(report, "ingots", "iron", ResourceLocation.withDefaultNamespace("iron_ingot"), 9);
        validateFoundryOutput(report, "block", "iron", ResourceLocation.withDefaultNamespace("iron_block"), 1);
        validateFoundryOutput(report, "nugget", "gold", ResourceLocation.withDefaultNamespace("gold_nugget"), 1);
        validateFoundryOutput(report, "ingot", "gold", ResourceLocation.withDefaultNamespace("gold_ingot"), 1);
        validateFoundryOutput(report, "ingots", "gold", ResourceLocation.withDefaultNamespace("gold_ingot"), 9);
        validateFoundryOutput(report, "block", "gold", ResourceLocation.withDefaultNamespace("gold_block"), 1);
        validateFoundryOutput(report, "ingot", "copper", ResourceLocation.withDefaultNamespace("copper_ingot"), 1);
        validateFoundryOutput(report, "ingots", "copper", ResourceLocation.withDefaultNamespace("copper_ingot"), 9);
        validateFoundryOutput(report, "block", "copper", ResourceLocation.withDefaultNamespace("copper_block"), 1);

        validateFoundryOutput(report, "blade", "titanium", ReinhardtsHBM.id("blade_titanium"), 1);
        validateFoundryOutput(report, "blade", "tungsten", ReinhardtsHBM.id("blade_tungsten"), 1);
        validateFoundryOutput(report, "blades", "steel", ReinhardtsHBM.id("blades_steel"), 1);
        validateFoundryOutput(report, "blades", "titanium", ReinhardtsHBM.id("blades_titanium"), 1);
        validateFoundryOutput(report, "stamp", "stone", ReinhardtsHBM.id("stamp_stone_flat"), 1);
        validateFoundryOutput(report, "stamp", "iron", ReinhardtsHBM.id("stamp_iron_flat"), 1);
        validateFoundryOutput(report, "stamp", "steel", ReinhardtsHBM.id("stamp_steel_flat"), 1);
        validateFoundryOutput(report, "stamp", "titanium", ReinhardtsHBM.id("stamp_titanium_flat"), 1);
        validateFoundryOutput(report, "pipes", "steel", ReinhardtsHBM.id("pipes_steel"), 1);

        validateFoundryMaterialInput(report, ResourceLocation.withDefaultNamespace("stone"), "stone", FoundryShape.BLOCK.q(1));
        validateFoundryMaterialInput(report, ResourceLocation.withDefaultNamespace("obsidian"), "obsidian", FoundryShape.BLOCK.q(1));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("blade_titanium"), "titanium", FoundryShape.INGOT.q(3));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("blade_tungsten"), "tungsten", FoundryShape.INGOT.q(3));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("blades_steel"), "steel", FoundryShape.INGOT.q(4));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("blades_titanium"), "titanium", FoundryShape.INGOT.q(4));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("blades_advanced_alloy"), "advanced_alloy", FoundryShape.INGOT.q(4));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("stamp_stone_flat"), "stone", FoundryShape.INGOT.q(3));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("stamp_iron_flat"), "iron", FoundryShape.INGOT.q(3));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("stamp_steel_flat"), "steel", FoundryShape.INGOT.q(3));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("stamp_titanium_flat"), "titanium", FoundryShape.INGOT.q(3));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("stamp_obsidian_flat"), "obsidian", FoundryShape.INGOT.q(3));
        validateFoundryMaterialInput(report, ReinhardtsHBM.id("pipes_steel"), "steel", FoundryShape.BLOCK.q(3));
    }

    private static void validateFoundryOutput(List<String> report, String moldName, String materialName,
                                              ResourceLocation expectedId, int expectedCount) {
        FoundryMoldItem.Mold mold = FoundryMoldItem.molds().stream()
                .filter(candidate -> candidate.name().equals(moldName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing foundry mold " + moldName));
        FoundryMaterial material = FoundryMaterial.byName(materialName)
                .orElseThrow(() -> new IllegalStateException("Missing foundry material " + materialName));
        ItemStack output = mold.outputFor(material)
                .orElseThrow(() -> new IllegalStateException("Foundry mold " + moldName
                        + " rejects required material " + materialName));
        ResourceLocation actualId = BuiltInRegistries.ITEM.getKey(output.getItem());
        if (!actualId.equals(expectedId) || output.getCount() != expectedCount) {
            throw new IllegalStateException("Foundry mold " + moldName + " maps " + materialName + " to "
                    + actualId + " x" + output.getCount() + " instead of " + expectedId + " x" + expectedCount);
        }
        report.add("FOUNDRY_REQUIRED\t" + moldName + "\t" + materialName + "\t" + expectedId + "\t" + expectedCount);
    }

    private static void validateFoundryMaterialInput(List<String> report, ResourceLocation itemId,
                                                     String expectedMaterialName, int expectedAmount) {
        ItemStack input = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
        FoundryMaterialStack materialStack = FoundryMaterial.materialFromItem(input)
                .orElseThrow(() -> new IllegalStateException("Foundry cannot recover material from " + itemId));
        if (!materialStack.material().name().equals(expectedMaterialName) || materialStack.amount() != expectedAmount) {
            throw new IllegalStateException("Foundry maps " + itemId + " to " + materialStack.material().name()
                    + " x" + materialStack.amount() + " instead of " + expectedMaterialName + " x" + expectedAmount);
        }
        report.add("FOUNDRY_INPUT\t" + itemId + "\t" + expectedMaterialName + "\t" + expectedAmount);
    }

    private static void validateEveryFoundryMoldCasting(MinecraftServer server, List<String> report) {
        for (FoundryMoldItem.Mold mold : FoundryMoldItem.molds()) {
            FoundryMaterial material = FoundryMaterial.ordered().stream()
                    .filter(candidate -> candidate.behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE)
                    .filter(candidate -> mold.outputFor(candidate).isPresent())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Foundry mold has no smeltable test material: " + mold.name()));
            ItemStack expected = mold.outputFor(material).orElseThrow();
            BlockPos pos = server.overworld().getSharedSpawnPos();
            FoundryCastingBlockEntity casting = new FoundryCastingBlockEntity(
                    pos,
                    (mold.size() == 0 ? HbmBlocks.FOUNDRY_MOLD.get() : HbmBlocks.FOUNDRY_BASIN.get()).defaultBlockState()
            );
            casting.setLevel(server.overworld());
            casting.setItem(FoundryCastingBlockEntity.MOLD_SLOT, FoundryMoldItem.stackFor(mold.id()));
            FoundryMaterialStack molten = new FoundryMaterialStack(material, mold.cost());
            if (!casting.canAcceptPartialPour(server.overworld(), pos, pos.getX() + 0.5D, pos.getY() + 1.0D,
                    pos.getZ() + 0.5D, Direction.UP, molten)) {
                throw new IllegalStateException("Foundry casting block rejects mold " + mold.name()
                        + " with material " + material.name());
            }
            if (casting.pour(server.overworld(), pos, pos.getX() + 0.5D, pos.getY() + 1.0D,
                    pos.getZ() + 0.5D, Direction.UP, molten) != null) {
                throw new IllegalStateException("Foundry casting block did not consume a full pour for mold " + mold.name());
            }
            for (int tick = 0; tick < 200; tick++) {
                FoundryCastingBlockEntity.tick(server.overworld(), pos, casting.getBlockState(), casting);
            }
            ItemStack actual = casting.getItem(FoundryCastingBlockEntity.OUTPUT_SLOT);
            if (!ItemStack.isSameItemSameComponents(actual, expected) || actual.getCount() != expected.getCount()) {
                throw new IllegalStateException("Foundry casting block failed end-to-end test for mold " + mold.name()
                        + ": expected " + expected + ", got " + actual);
            }
            report.add("FOUNDRY_MOLD_CASTING\t" + mold.name() + "\t" + material.name() + "\t"
                    + BuiltInRegistries.ITEM.getKey(actual.getItem()) + "\t" + actual.getCount());
        }
    }

    private static void validateFoundryBasinCasting(MinecraftServer server, List<String> report,
                                                     String moldName, String expectedOutputPath, int expectedCount) {
        FoundryMoldItem.Mold mold = FoundryMoldItem.molds().stream()
                .filter(candidate -> candidate.name().equals(moldName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing foundry mold " + moldName));
        FoundryMaterial material = FoundryMaterial.byName("dura_steel")
                .orElseThrow(() -> new IllegalStateException("Missing foundry material dura_steel"));
        ItemStack expected = mold.outputFor(material)
                .orElseThrow(() -> new IllegalStateException("Foundry mold " + moldName + " rejects high-speed steel"));
        ResourceLocation expectedId = ReinhardtsHBM.id(expectedOutputPath);
        ResourceLocation actualId = BuiltInRegistries.ITEM.getKey(expected.getItem());
        if (!actualId.equals(expectedId) || expected.getCount() != expectedCount) {
            throw new IllegalStateException("Foundry mold " + moldName + " maps high-speed steel to "
                    + actualId + " x" + expected.getCount() + " instead of " + expectedId + " x" + expectedCount);
        }

        BlockPos pos = server.overworld().getSharedSpawnPos();
        FoundryCastingBlockEntity casting = new FoundryCastingBlockEntity(pos, HbmBlocks.FOUNDRY_BASIN.get().defaultBlockState());
        casting.setLevel(server.overworld());
        casting.setItem(FoundryCastingBlockEntity.MOLD_SLOT, FoundryMoldItem.stackFor(mold.id()));
        FoundryMaterialStack molten = new FoundryMaterialStack(material, mold.cost());
        if (!casting.canAcceptPartialPour(server.overworld(), pos, pos.getX() + 0.5D, pos.getY() + 1.0D,
                pos.getZ() + 0.5D, Direction.UP, molten)) {
            throw new IllegalStateException("Foundry basin rejects a full high-speed-steel pour for mold " + moldName);
        }
        FoundryMaterialStack leftover = casting.pour(server.overworld(), pos, pos.getX() + 0.5D, pos.getY() + 1.0D,
                pos.getZ() + 0.5D, Direction.UP, molten);
        if (leftover != null) {
            throw new IllegalStateException("Foundry basin left " + leftover.amount()
                    + " high-speed-steel quanta for mold " + moldName);
        }
        for (int tick = 0; tick < 200; tick++) {
            FoundryCastingBlockEntity.tick(server.overworld(), pos, casting.getBlockState(), casting);
        }
        ItemStack result = casting.getItem(FoundryCastingBlockEntity.OUTPUT_SLOT);
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (!resultId.equals(expectedId) || result.getCount() != expectedCount) {
            throw new IllegalStateException("Foundry basin failed to finish high-speed-steel casting for mold "
                    + moldName + ": got " + resultId + " x" + result.getCount());
        }
        report.add("FOUNDRY_CASTING\t" + moldName + "\t" + expectedId + "\t" + expectedCount);
    }

    private static void writeAnvilAudit(List<String> report) {
        List<AnvilConstructionRecipe> constructionRecipes = HbmAnvilRecipes.construction();
        Set<ResourceLocation> anvilOutputIds = new HashSet<>();
        for (int index = 0; index < constructionRecipes.size(); index++) {
            AnvilConstructionRecipe recipe = constructionRecipes.get(index);
            String recipeId = "construction:" + index;
            report.add("ANVIL_CONSTRUCTION\t" + recipeId + "\t" + recipe.tierLower() + "\t"
                    + recipe.tierUpper() + "\t" + recipe.overlay());
            for (int inputIndex = 0; inputIndex < recipe.inputs().size(); inputIndex++) {
                validateAnvilIngredient(report, recipeId, inputIndex, recipe.inputs().get(inputIndex));
            }
            if (recipe.outputs().isEmpty()) {
                report.add("ANVIL_ERROR\t" + recipeId + " has no outputs");
            }
            for (AnvilOutput output : recipe.outputs()) {
                writeAnvilOutput(report, recipeId, output.stack());
                if (!output.stack().isEmpty()) {
                    anvilOutputIds.add(BuiltInRegistries.ITEM.getKey(output.stack().getItem()));
                }
            }
        }

        List<AnvilSmithingRecipe> smithingRecipes = HbmAnvilRecipes.smithing();
        for (int index = 0; index < smithingRecipes.size(); index++) {
            AnvilSmithingRecipe recipe = smithingRecipes.get(index);
            String recipeId = "smithing:" + index;
            report.add("ANVIL_SMITHING\t" + recipeId + "\t" + recipe.tier());
            validateAnvilIngredient(report, recipeId, 0, recipe.left());
            validateAnvilIngredient(report, recipeId, 1, recipe.right());
            ItemStack output = recipe.displayOutput();
            writeAnvilOutput(report, recipeId, output);
            if (!output.isEmpty()) {
                anvilOutputIds.add(BuiltInRegistries.ITEM.getKey(output.getItem()));
            }
        }

        for (FoundryMoldItem.Mold mold : FoundryMoldItem.molds()) {
            ResourceLocation moldItemId = BuiltInRegistries.ITEM.getKey(HbmItems.foundryMoldItem(mold.id()).get());
            if (!anvilOutputIds.contains(moldItemId)) {
                throw new IllegalStateException("Foundry mold has no anvil acquisition recipe: " + moldItemId);
            }
        }
    }

    private static void validateAnvilIngredient(List<String> report, String recipeId, int inputIndex,
                                                AnvilIngredient ingredient) {
        if (ingredient.isFluid()) {
            return;
        }
        List<ItemStack> matchingStacks = ingredient.searchStacks();
        if (matchingStacks.isEmpty()) {
            report.add("ANVIL_ERROR\t" + recipeId + " input " + inputIndex + " resolves to no registered items");
            return;
        }
        matchingStacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(itemId -> report.add("ANVIL_INPUT\t" + recipeId + "\t" + inputIndex + "\t" + itemId));
    }

    private static void writeAnvilOutput(List<String> report, String recipeId, ItemStack stack) {
        if (stack.isEmpty()) {
            report.add("ANVIL_ERROR\t" + recipeId + " has an empty output");
            return;
        }
        report.add("ANVIL_OUTPUT\t" + recipeId + "\t" + BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "\t" + stack.getCount());
    }

    private record BasicMoldExpectation(String moldName, String outputPrefix, FoundryShape shape, int count) {
    }

    private record ShapeMoldExpectation(String moldName, FoundryShape shape, int count) {
    }

    private static boolean hasRecipe(Collection<RecipeHolder<AssemblyMachineRecipe>> recipes, String path) {
        ResourceLocation id = ReinhardtsHBM.id("assembly_machine/" + path);
        return recipes.stream().anyMatch(holder -> holder.id().equals(id));
    }
}
