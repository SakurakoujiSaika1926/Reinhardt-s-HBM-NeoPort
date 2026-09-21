package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FoundryCastingBlockEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
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
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class RecipeDiagnostics {
    private static final String AUDIT_ENABLED_PROPERTY = "reinhardtshbm.recipeAudit";
    private static final String AUDIT_REPORT_PROPERTY = "reinhardtshbm.recipeAuditReport";
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
        validateFoundryBasinCasting(server, report, "plates", "plate_dura_steel", 9);
        validateFoundryBasinCasting(server, report, "plates_cast", "plate_cast_dura_steel", 3);
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
        casting.setItem(FoundryCastingBlockEntity.MOLD_SLOT, FoundryMoldItem.stackFor(HbmItems.MOLD.get(), mold.id()));
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
            }
        }

        List<AnvilSmithingRecipe> smithingRecipes = HbmAnvilRecipes.smithing();
        for (int index = 0; index < smithingRecipes.size(); index++) {
            AnvilSmithingRecipe recipe = smithingRecipes.get(index);
            String recipeId = "smithing:" + index;
            report.add("ANVIL_SMITHING\t" + recipeId + "\t" + recipe.tier());
            validateAnvilIngredient(report, recipeId, 0, recipe.left());
            validateAnvilIngredient(report, recipeId, 1, recipe.right());
            writeAnvilOutput(report, recipeId, recipe.displayOutput());
        }
    }

    private static void validateAnvilIngredient(List<String> report, String recipeId, int inputIndex,
                                                AnvilIngredient ingredient) {
        if (!ingredient.isFluid() && ingredient.searchStacks().isEmpty()) {
            report.add("ANVIL_ERROR\t" + recipeId + " input " + inputIndex + " resolves to no registered items");
        }
    }

    private static void writeAnvilOutput(List<String> report, String recipeId, ItemStack stack) {
        if (stack.isEmpty()) {
            report.add("ANVIL_ERROR\t" + recipeId + " has an empty output");
            return;
        }
        report.add("ANVIL_OUTPUT\t" + recipeId + "\t" + BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "\t" + stack.getCount());
    }

    private static boolean hasRecipe(Collection<RecipeHolder<AssemblyMachineRecipe>> recipes, String path) {
        ResourceLocation id = ReinhardtsHBM.id("assembly_machine/" + path);
        return recipes.stream().anyMatch(holder -> holder.id().equals(id));
    }
}
