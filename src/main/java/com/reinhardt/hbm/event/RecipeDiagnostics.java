package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RotaryFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.FoundryCastingBlockEntity;
import com.reinhardt.hbm.blockentity.MachineBlastFurnaceBlockEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialShapes;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.fusion.FusionNetwork;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.HbmChargeableItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import com.reinhardt.hbm.recipe.BlastFurnaceRecipe;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
import com.reinhardt.hbm.recipe.FusionRecipe;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
import com.reinhardt.hbm.recipe.RotaryFurnaceRecipe;
import com.reinhardt.hbm.recipe.anvil.AnvilConstructionRecipe;
import com.reinhardt.hbm.recipe.anvil.AnvilIngredient;
import com.reinhardt.hbm.recipe.anvil.AnvilOutput;
import com.reinhardt.hbm.recipe.anvil.AnvilSmithingRecipe;
import com.reinhardt.hbm.recipe.anvil.HbmAnvilRecipes;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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
                        .filter(path -> !hasRecipe(noBlueprintRecipes, path)
                                && !hasRecipe(noBlueprintRecipes, path + "_expensive"))
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
            validateBatteryIngredientsIgnoreCharge(event.getServer());
            validateBlastFurnaceRuntime(event.getServer());
            validateRotaryFurnaceRuntime(event.getServer());
            validateFusionMachineRuntime(event.getServer());
            FuelCycleRecipeDiagnostics.validate(event.getServer());
            writeAuditReport(event.getServer());
            event.getServer().halt(false);
        }
    }

    private static void validateBatteryIngredientsIgnoreCharge(MinecraftServer server) {
        List<ItemStack> batteryModels = new ArrayList<>();
        BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof HbmChargeableItem)
                .forEach(item -> {
                    if (item instanceof BatteryPackItem) {
                        BatteryPackItem.knownVariants().stream()
                                .sorted()
                                .map(variant -> LegacyVariantItem.stackFor(item, variant))
                                .forEach(batteryModels::add);
                    } else {
                        batteryModels.add(new ItemStack(item));
                    }
                });

        int matchedInputs = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(ReinhardtsHBM.MOD_ID)) {
                continue;
            }
            for (int inputIndex = 0; inputIndex < holder.value().getIngredients().size(); inputIndex++) {
                var ingredient = holder.value().getIngredients().get(inputIndex);
                for (ItemStack model : batteryModels) {
                    long capacity = BatteryPackItem.capacity(model);
                    if (capacity <= 0L) {
                        continue;
                    }
                    ItemStack empty = model.copy();
                    ItemStack half = model.copy();
                    ItemStack full = model.copy();
                    BatteryPackItem.setStoredCharge(empty, 0L);
                    BatteryPackItem.setStoredCharge(half, capacity / 2L);
                    BatteryPackItem.setStoredCharge(full, capacity);

                    boolean acceptsEmpty = ingredient.test(empty);
                    boolean acceptsHalf = ingredient.test(half);
                    boolean acceptsFull = ingredient.test(full);
                    if (!acceptsEmpty && !acceptsHalf && !acceptsFull) {
                        continue;
                    }
                    matchedInputs++;
                    if (!(acceptsEmpty && acceptsHalf && acceptsFull)) {
                        throw new IllegalStateException("Battery ingredient depends on stored charge: recipe="
                                + holder.id() + ", input=" + inputIndex + ", item="
                                + BuiltInRegistries.ITEM.getKey(model.getItem()) + ", variant="
                                + BatteryPackItem.variantId(model) + ", empty=" + acceptsEmpty
                                + ", half=" + acceptsHalf + ", full=" + acceptsFull);
                    }
                }
            }
        }
        if (matchedInputs == 0) {
            throw new IllegalStateException("Battery ingredient audit did not find any battery recipe inputs");
        }
        ReinhardtsHBM.LOGGER.info(
                "HBM battery ingredient audit passed: {} chargeable models, {} matching recipe inputs; empty/half/full states are equivalent",
                batteryModels.size(),
                matchedInputs
        );
    }

    private static void validateBlastFurnaceRuntime(MinecraftServer server) {
        var level = server.overworld();
        int coalPower = MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(Items.COAL));
        if (coalPower != MachineBlastFurnaceBlockEntity.FUEL_COAL
                || coalPower / MachineBlastFurnaceBlockEntity.FUEL_RATE != 2) {
            throw new IllegalStateException("Blast furnace coal fuel parity failed: expected "
                    + MachineBlastFurnaceBlockEntity.FUEL_COAL + " (two operations), got " + coalPower);
        }

        int plankPower = MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(Items.OAK_PLANKS));
        if (plankPower <= 0) {
            throw new IllegalStateException("Blast furnace no longer accepts ordinary furnace fuels");
        }

        validateBlastFurnaceFuel(level, "solid_fuel", 3_200);
        validateBlastFurnaceFuel(level, "solid_fuel_presto", 8_000);
        validateBlastFurnaceFuel(level, "briquette_coal", 2_000);
        validateBlastFurnaceFuel(level, "briquette_lignite", 1_600);
        validateBlastFurnaceFuel(level, "briquette_wood", 400);

        List<RecipeHolder<BlastFurnaceRecipe>> recipes = server.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get());
        if (recipes.isEmpty()) {
            throw new IllegalStateException("Blast furnace work recipe registry is empty");
        }
        for (RecipeHolder<BlastFurnaceRecipe> holder : recipes) {
            BlastFurnaceRecipe recipe = holder.value();
            ItemStack[] firstCandidates = recipe.inputA().getItems();
            ItemStack[] secondCandidates = recipe.inputB().getItems();
            if (firstCandidates.length == 0 || secondCandidates.length == 0) {
                throw new IllegalStateException("Blast furnace recipe has an unresolved input: " + holder.id());
            }
            ItemStack first = firstCandidates[0].copy();
            ItemStack second = secondCandidates[0].copy();
            first.setCount(recipe.inputACount());
            second.setCount(recipe.inputBCount());
            if (recipe.match(first, second).isEmpty()) {
                throw new IllegalStateException("Blast furnace recipe cannot match its resolved inputs: " + holder.id());
            }
        }
        ReinhardtsHBM.LOGGER.info(
                "HBM blast furnace runtime audit passed: coal_power={}, plank_power={}, work_recipes={}",
                coalPower,
                plankPower,
                recipes.size()
        );
    }

    /**
     * Exercises the complete gunmetal path instead of merely counting decoded
     * recipe JSONs.  This keeps the three-slot matcher, solid fuel, legacy
     * steam/spent-steam ports and molten output contract under one runtime
     * regression check.
     */
    private static void validateRotaryFurnaceRuntime(MinecraftServer server) {
        List<RecipeHolder<RotaryFurnaceRecipe>> recipes = server.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ROTARY_FURNACE.get());
        if (recipes.size() != 12) {
            throw new IllegalStateException("Rotary furnace recipe parity failed: expected 12 recipes, got "
                    + recipes.size());
        }

        for (RecipeHolder<RotaryFurnaceRecipe> holder : recipes) {
            RotaryFurnaceRecipe recipe = holder.value();
            if (recipe.inputs().size() > 3) {
                throw new IllegalStateException("Rotary furnace recipe exceeds its three input slots: " + holder.id());
            }
            ItemStack[] resolved = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
            for (int index = 0; index < recipe.inputs().size(); index++) {
                RotaryFurnaceRecipe.CountedIngredient counted = recipe.inputs().get(index);
                ItemStack[] candidates = counted.ingredient().getItems();
                if (candidates.length == 0) {
                    throw new IllegalStateException("Rotary furnace recipe has an unresolved input: " + holder.id()
                            + " slot=" + index);
                }
                resolved[index] = candidates[0].copyWithCount(counted.count());
            }
            RotaryFurnaceRecipe.Input input = new RotaryFurnaceRecipe.Input(resolved[0], resolved[1], resolved[2]);
            if (!recipe.matches(input, server.overworld())) {
                throw new IllegalStateException("Rotary furnace recipe cannot match its resolved inputs: " + holder.id());
            }
        }

        RecipeHolder<RotaryFurnaceRecipe> gunmetalHolder = recipes.stream()
                .filter(holder -> holder.id().equals(ReinhardtsHBM.id("rotary_furnace/gunmetal")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing rotary furnace gunmetal recipe"));
        RotaryFurnaceRecipe gunmetal = gunmetalHolder.value();
        var level = server.overworld();
        BlockState state = HbmBlocks.MACHINE_ROTARY_FURNACE.get().defaultBlockState()
                .setValue(LargeMachineBlock.FACING, Direction.SOUTH);
        BlockPos spawn = level.getSharedSpawnPos();
        int auditX = spawn.getX() + 32;
        int auditZ = spawn.getZ() + 32;
        int auditY = Math.min(
                level.getMaxBuildHeight() - 16,
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, auditX, auditZ) + 16
        );
        BlockPos corePos = new BlockPos(auditX, auditY, auditZ);
        // Exact latest-1.7.10 SOUTH-facing PortDef coordinate:
        // core - facing - rotate(DOWN), exposed on facing.getOpposite().
        BlockPos steamPortPos = corePos.relative(Direction.NORTH).relative(Direction.WEST);
        if (!level.getBlockState(corePos).isAir() || !level.getBlockState(steamPortPos).isAir()) {
            throw new IllegalStateException("Rotary furnace audit site is unexpectedly occupied");
        }

        RotaryFurnaceBlockEntity furnace = null;
        try {
            level.setBlock(corePos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
            if (!(level.getBlockEntity(corePos) instanceof RotaryFurnaceBlockEntity placedFurnace)) {
                throw new IllegalStateException("Rotary furnace core did not create its block entity");
            }
            furnace = placedFurnace;

            level.setBlock(steamPortPos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
            if (!(level.getBlockEntity(steamPortPos) instanceof MachineDummyBlockEntity dummy)) {
                throw new IllegalStateException("Rotary furnace steam port did not create its dummy block entity");
            }
            dummy.setCorePos(corePos);

            for (int index = 0; index < gunmetal.inputs().size(); index++) {
                RotaryFurnaceRecipe.CountedIngredient counted = gunmetal.inputs().get(index);
                ItemStack candidate = counted.ingredient().getItems()[0].copyWithCount(counted.count());
                furnace.setItem(index, candidate);
            }
            furnace.setItem(RotaryFurnaceBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));

            IFluidHandler steamPort = level.getCapability(
                    Capabilities.FluidHandler.BLOCK, steamPortPos, Direction.NORTH);
            if (steamPort == null
                    || level.getCapability(Capabilities.FluidHandler.BLOCK, steamPortPos, Direction.SOUTH) != null
                    || level.getCapability(Capabilities.FluidHandler.BLOCK, corePos, Direction.NORTH) != null) {
                throw new IllegalStateException("Rotary furnace dummy steam capability no longer matches latest 1.7.10");
            }

            var steam = HbmFluids.byName("steam")
                    .orElseThrow(() -> new IllegalStateException("Missing HBM steam fluid"));
            var spentSteam = HbmFluids.byName("spentsteam")
                    .orElseThrow(() -> new IllegalStateException("Missing HBM spent-steam fluid"));
            int ticks = gunmetal.duration() + 20;
            for (int tick = 0; tick < ticks && furnace.output() == null; tick++) {
                steamPort.drain(HbmFluids.toNeoStack(spentSteam, 1_000), IFluidHandler.FluidAction.EXECUTE);
                int accepted = steamPort.fill(HbmFluids.toNeoStack(steam, 1_000), IFluidHandler.FluidAction.EXECUTE);
                if (accepted <= 0 && furnace.steamTank().amount() < gunmetal.steam()) {
                    throw new IllegalStateException("Rotary furnace dummy steam port stopped accepting steam at tick " + tick);
                }
                RotaryFurnaceBlockEntity.tick(level, corePos, state, furnace);
            }

            if (furnace.output() == null
                    || furnace.output().material() != gunmetal.output().material()
                    || furnace.output().amount() != gunmetal.output().amount()) {
                throw new IllegalStateException("Rotary furnace failed its full gunmetal processing cycle: output="
                        + furnace.output());
            }
            ReinhardtsHBM.LOGGER.info(
                    "HBM rotary-furnace runtime audit passed through placed dummy capability: recipes={}, gunmetal_amount={}, steam_port={}",
                    recipes.size(), furnace.output().amount(), steamPortPos.subtract(corePos)
            );
        } finally {
            MachineDummyBlock.runWithoutCoreDestroy(() -> {
                level.removeBlock(steamPortPos, false);
                level.removeBlock(corePos, false);
            });
        }
    }

    /**
     * Exercises the shared fusion chain with real registered recipes and real
     * block entities.  The two node pairs deliberately use the exact old
     * klystron/torus/forge spacing, so a directional or dummy-core regression
     * fails here before a release is deployed.
     */
    private static void validateFusionMachineRuntime(MinecraftServer server) {
        var level = server.overworld();
        RecipeHolder<FusionRecipe> fusionHolder = server.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.FUSION.get())
                .stream()
                .filter(holder -> holder.id().equals(ReinhardtsHBM.id("fusion/dd")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing fusion D-D recipe"));
        RecipeHolder<PlasmaForgeRecipe> forgeHolder = server.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.PLASMA_FORGE.get())
                .stream()
                .filter(holder -> holder.id().equals(ReinhardtsHBM.id("plasma_forge/weldiron")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing plasma-forge welded iron recipe"));

        BlockPos spawn = level.getSharedSpawnPos();
        int auditX = spawn.getX() + 96;
        int auditZ = spawn.getZ() + 96;
        // The torus foundation spans 11 x 11 blocks and the connected machines
        // extend farther north/east. Load the complete audit footprint before
        // inspecting block states; otherwise a previously unseen column can
        // appear empty before its chunk reaches FULL.
        for (int x = auditX - 5; x <= auditX + 13; x++) {
            for (int z = auditZ - 12; z <= auditZ + 5; z++) {
                level.getChunk(x >> 4, z >> 4);
            }
        }

        // Select an actually empty vertical slice instead of deriving one from a
        // heightmap. Trees, overhangs and structure blocks can legally occupy a
        // column above MOTION_BLOCKING_NO_LEAVES and made this audit intermittent.
        BlockPos selectedTorusPos = null;
        for (int auditY = level.getMaxBuildHeight() - 3;
                auditY >= level.getMinBuildHeight() + 1; auditY--) {
            BlockPos candidate = new BlockPos(auditX, auditY, auditZ);
            if (fusionAuditPositions(candidate).stream().allMatch(pos -> level.getBlockState(pos).isAir())) {
                selectedTorusPos = candidate;
                break;
            }
        }
        if (selectedTorusPos == null) {
            throw new IllegalStateException("Fusion runtime audit could not find an empty vertical slice");
        }
        BlockPos torusPos = selectedTorusPos;
        BlockPos klystronPos = torusPos.offset(0, 0, -12);
        BlockPos forgePos = torusPos.offset(13, 0, 0);
        BlockPos torusKlystronNode = torusPos.offset(0, 2, -7);
        BlockPos klystronNode = torusPos.offset(0, 2, -8);
        BlockPos torusPlasmaNode = torusPos.offset(7, 2, 0);
        BlockPos forgeNode = torusPos.offset(8, 2, 0);
        List<BlockPos> cleanup = fusionAuditPositions(torusPos);

        BlockState torusState = HbmBlocks.FUSION_TORUS.get().defaultBlockState()
                .setValue(LargeMachineBlock.FACING, Direction.SOUTH);
        BlockState klystronState = HbmBlocks.FUSION_KLYSTRON.get().defaultBlockState()
                .setValue(LargeMachineBlock.FACING, Direction.NORTH);
        BlockState forgeState = HbmBlocks.FUSION_PLASMA_FORGE.get().defaultBlockState()
                .setValue(LargeMachineBlock.FACING, Direction.SOUTH);
        try {
            for (int index = 0; index < 36; index++) {
                level.setBlock(torusPos.offset(-5 + index / 6 * 2, -1, -5 + index % 6 * 2),
                        Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
            level.setBlock(torusPos, torusState, net.minecraft.world.level.block.Block.UPDATE_ALL);
            level.setBlock(klystronPos, klystronState, net.minecraft.world.level.block.Block.UPDATE_ALL);
            level.setBlock(forgePos, forgeState, net.minecraft.world.level.block.Block.UPDATE_ALL);
            if (!(level.getBlockEntity(torusPos) instanceof FusionMachineBlockEntity torus)
                    || !(level.getBlockEntity(klystronPos) instanceof FusionMachineBlockEntity klystron)
                    || !(level.getBlockEntity(forgePos) instanceof FusionMachineBlockEntity forge)) {
                throw new IllegalStateException("Fusion runtime audit failed to create all three machine cores");
            }

            bindFusionDummy(level, torusKlystronNode, torusPos);
            bindFusionDummy(level, klystronNode, klystronPos);
            bindFusionDummy(level, torusPlasmaNode, torusPos);
            bindFusionDummy(level, forgeNode, forgePos);
            if (!FusionNetwork.provideKlystron(level, klystron, fusionHolder.value().ignitionTemp())) {
                throw new IllegalStateException("Fusion klystron-to-torus node contract is disconnected");
            }
            if (!FusionNetwork.providePlasma(level, torus, forgeHolder.value().ignitionTemp(),
                    0.0D, 0.7F, 0.7F, 1.0F)) {
                throw new IllegalStateException("Fusion torus-to-plasma-forge node contract is disconnected");
            }
            if (level.getCapability(Capabilities.FluidHandler.BLOCK, torusPos, Direction.DOWN) == null) {
                throw new IllegalStateException("Fusion torus core-bottom legacy port is unavailable");
            }

            RecipeHolder<FusionRecipe> twoFluidHolder = server.getRecipeManager()
                    .getAllRecipesFor(HbmRecipeTypes.FUSION.get())
                    .stream()
                    .filter(holder -> holder.value().inputFluids().size() >= 2)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Fusion audit needs a two-fluid recipe"));
            torus.setSelectedFusionRecipe(twoFluidHolder.id());
            FusionRecipe.FusionFluidStack obsoleteFluid = twoFluidHolder.value().inputFluids().get(1);
            torus.tank(1).fill(obsoleteFluid.fluid(), 100, false);
            FusionMachineBlockEntity.tick(level, torusPos, torusState, torus);
            torus.setSelectedFusionRecipe(fusionHolder.id());
            FusionMachineBlockEntity.tick(level, torusPos, torusState, torus);
            if (!torus.tank(1).type().isNone() || torus.tank(1).amount() != 0) {
                throw new IllegalStateException("Fusion recipe switch left stale fluid in an unused input tank");
            }

            FusionRecipe fusion = fusionHolder.value();
            for (int index = 0; index < fusion.inputFluids().size(); index++) {
                FusionRecipe.FusionFluidStack input = fusion.inputFluids().get(index);
                int accepted = torus.tank(index).fill(input.fluid(), FusionMachineBlockEntity.TORUS_TANK_CAPACITY, false);
                if (accepted != FusionMachineBlockEntity.TORUS_TANK_CAPACITY) {
                    throw new IllegalStateException("Fusion input tank rejected configured fluid " + input.fluid().name());
                }
            }
            var coldCoolant = HbmFluids.byName("perfluoromethyl_cold")
                    .orElseThrow(() -> new IllegalStateException("Missing cold perfluoromethyl coolant"));
            torus.tank(4).fill(coldCoolant, FusionMachineBlockEntity.TORUS_TANK_CAPACITY, false);
            int fusionTicks = fusion.duration() + 40;
            for (int tick = 0; tick < fusionTicks && torus.tank(3).amount() == 0; tick++) {
                torus.applyPower(0L, torus.maxPower());
                torus.receiveKlystronEnergy(fusion.ignitionTemp());
                FusionMachineBlockEntity.tick(level, torusPos, torusState, torus);
            }
            if (fusion.outputFluids().isEmpty()
                    || torus.tank(3).type() != fusion.outputFluids().getFirst().fluid()
                    || torus.tank(3).amount() < fusion.outputFluids().getFirst().amount()) {
                throw new IllegalStateException("Fusion torus failed a complete registered D-D recipe cycle");
            }

            var staleFluid = HbmFluids.byName("reformgas")
                    .orElseThrow(() -> new IllegalStateException("Missing reformgas fluid"));
            forge.tank(0).fill(staleFluid, 100, false);
            forge.setSelectedPlasmaForgeRecipe(forgeHolder.id());
            if (!forge.tank(0).type().isNone() || forge.tank(0).amount() != 0) {
                throw new IllegalStateException("Fluidless plasma-forge recipe did not clear its stale input tank");
            }
            PlasmaForgeRecipe forgeRecipe = forgeHolder.value();
            PlasmaForgeRecipe.CountedIngredient forgeInput = forgeRecipe.inputItems().getFirst();
            ItemStack[] candidates = forgeInput.ingredient().getItems();
            if (candidates.length == 0) {
                throw new IllegalStateException("Plasma-forge audit recipe has an unresolved item input");
            }
            ItemStack input = candidates[0].copyWithCount(forgeInput.count());
            if (!forge.canPlaceItem(FusionMachineBlockEntity.PLASMA_INPUT_START, input)) {
                throw new IllegalStateException("Plasma forge rejects its selected recipe input");
            }
            forge.setItem(FusionMachineBlockEntity.PLASMA_INPUT_START, input);
            int forgeTicks = forgeRecipe.duration() + 5;
            for (int tick = 0; tick < forgeTicks && forge.getItem(FusionMachineBlockEntity.PLASMA_OUTPUT_SLOT).isEmpty(); tick++) {
                forge.applyPower(0L, forge.maxPower());
                forge.receiveFusionPower(forgeRecipe.ignitionTemp(), 0.0D, 0.7F, 0.7F, 1.0F);
                FusionMachineBlockEntity.tick(level, forgePos, forgeState, forge);
            }
            if (!ItemStack.isSameItemSameComponents(
                    forge.getItem(FusionMachineBlockEntity.PLASMA_OUTPUT_SLOT), forgeRecipe.result())) {
                throw new IllegalStateException("Plasma forge failed a complete registered welded-iron recipe cycle");
            }
            ReinhardtsHBM.LOGGER.info(
                    "HBM fusion runtime audit passed: fusion={}, plasma_forge={}, klystron_node={}, plasma_node={}",
                    fusionHolder.id(), forgeHolder.id(), klystronNode.subtract(torusKlystronNode),
                    forgeNode.subtract(torusPlasmaNode)
            );
        } finally {
            MachineDummyBlock.runWithoutCoreDestroy(() -> {
                for (BlockPos pos : cleanup) {
                    level.removeBlock(pos, false);
                }
            });
        }
    }

    private static List<BlockPos> fusionAuditPositions(BlockPos torusPos) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(torusPos.offset(0, 2, -7));
        positions.add(torusPos.offset(0, 2, -8));
        positions.add(torusPos.offset(7, 2, 0));
        positions.add(torusPos.offset(8, 2, 0));
        positions.add(torusPos.offset(0, 0, -12));
        positions.add(torusPos.offset(13, 0, 0));
        positions.add(torusPos);
        for (int index = 0; index < 36; index++) {
            positions.add(torusPos.offset(-5 + index / 6 * 2, -1, -5 + index % 6 * 2));
        }
        return List.copyOf(positions);
    }

    private static void bindFusionDummy(net.minecraft.server.level.ServerLevel level, BlockPos dummyPos, BlockPos corePos) {
        level.setBlock(dummyPos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        if (!(level.getBlockEntity(dummyPos) instanceof MachineDummyBlockEntity dummy)) {
            throw new IllegalStateException("Fusion runtime audit failed to create node dummy at " + dummyPos);
        }
        dummy.setCorePos(corePos);
    }

    private static void validateBlastFurnaceFuel(net.minecraft.world.level.Level level, String path, int expectedPower) {
        var item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(path));
        if (item == Items.AIR) {
            throw new IllegalStateException("Missing blast furnace fuel item: " + path);
        }
        int actualPower = MachineBlastFurnaceBlockEntity.fuelPower(level, new ItemStack(item));
        if (actualPower != expectedPower) {
            throw new IllegalStateException("Blast furnace fuel parity failed for " + path
                    + ": expected " + expectedPower + ", got " + actualPower);
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
                    if (holder.value() instanceof ChemicalPlantRecipe chemicalRecipe) {
                        for (int fluidIndex = 0; fluidIndex < chemicalRecipe.inputFluids().size(); fluidIndex++) {
                            ChemicalPlantRecipe.ChemicalFluidStack fluid = chemicalRecipe.inputFluids().get(fluidIndex);
                            report.add("CHEMICAL_FLUID_INPUT\t" + holder.id() + "\t" + fluidIndex + "\t"
                                    + fluid.type().name() + "\t" + fluid.amount() + "\t" + fluid.pressure());
                        }
                    }
                    if (holder.value() instanceof PrecisionAssemblerRecipe precisionRecipe) {
                        StringBuilder weights = new StringBuilder();
                        for (int outputIndex = 0; outputIndex < precisionRecipe.outputs().size(); outputIndex++) {
                            if (outputIndex > 0) {
                                weights.append(',');
                            }
                            weights.append(precisionRecipe.outputWeight(outputIndex));
                        }
                        report.add("PRECISION_OUTPUT_WEIGHTS\t" + holder.id() + "\t" + weights);
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
                .forEach(itemId -> report.add("ANVIL_INPUT\t" + recipeId + "\t" + inputIndex + "\t"
                        + itemId + "\t" + ingredient.count()));
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
