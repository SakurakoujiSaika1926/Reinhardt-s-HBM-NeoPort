package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.PwrFuelItem;
import com.reinhardt.hbm.item.RbmkFuelRodItem;
import com.reinhardt.hbm.item.RbmkPelletItem;
import com.reinhardt.hbm.item.WasteFuelItem;
import com.reinhardt.hbm.item.ZirnoxRodItem;
import com.reinhardt.hbm.recipe.FuelPoolRecipe;
import com.reinhardt.hbm.recipe.PurexRecipe;
import com.reinhardt.hbm.recipe.SilexRecipe;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict runtime checks for the three reactor fuel cycles ported from 1.7.10. */
final class FuelCycleRecipeDiagnostics {
    private static final Map<String, String> ZIRNOX_SPENT_FUEL = Map.ofEntries(
            Map.entry("natural_uranium_fuel", "waste_natural_uranium"),
            Map.entry("uranium_fuel", "waste_uranium"),
            Map.entry("thorium_fuel", "waste_thorium"),
            Map.entry("mox_fuel", "waste_mox"),
            Map.entry("plutonium_fuel", "waste_plutonium"),
            Map.entry("u233_fuel", "waste_u233"),
            Map.entry("u235_fuel", "waste_u235"),
            Map.entry("les_fuel", "waste_schrabidium"),
            Map.entry("zfb_mox_fuel", "waste_zfb_mox")
    );

    private static final Map<String, Integer> RBMK_SILEX_STATES = rbmkSilexStates();

    private FuelCycleRecipeDiagnostics() {
    }

    static void validate(MinecraftServer server) {
        ServerLevel level = server.overworld();
        List<RecipeHolder<CraftingRecipe>> crafting = server.getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getNamespace().equals(ReinhardtsHBM.MOD_ID))
                .toList();

        validateConstructionRecipes(server, crafting);
        validatePwrCycle(server, level);
        validateZirnoxCycle(server, level);
        validateRbmkCycle(level);

        ReinhardtsHBM.LOGGER.info(
                "HBM fuel-cycle audit passed: PWR={} ZIRNOX={} RBMK={} RBMK_SILEX={}",
                PwrFuelItem.FUELS.size(),
                ZirnoxRodItem.FUELS.size(),
                RbmkFuelRodItem.FUEL_IDS.size(),
                RBMK_SILEX_STATES.values().stream().mapToInt(Integer::intValue).sum()
        );
    }

    private static void validateConstructionRecipes(
            MinecraftServer server,
            List<RecipeHolder<CraftingRecipe>> crafting
    ) {
        List<ItemStack> results = crafting.stream()
                .map(holder -> holder.value().getResultItem(server.registryAccess()))
                .filter(stack -> !stack.isEmpty())
                .toList();

        for (int index = 0; index < PwrFuelItem.FUELS.size(); index++) {
            int fuelIndex = index;
            requireExactlyOne(
                    "PWR construction recipe " + PwrFuelItem.FUELS.get(index).id(),
                    results.stream()
                            .filter(stack -> stack.is(HbmItems.PWR_FUEL.get()))
                            .filter(stack -> PwrFuelItem.fuelIndex(stack) == fuelIndex)
                            .count()
            );
        }

        for (ZirnoxRodItem.Fuel fuel : ZirnoxRodItem.FUELS) {
            requireExactlyOne(
                    "ZIRNOX construction recipe " + fuel.id(),
                    results.stream()
                            .filter(stack -> stack.is(HbmItems.ROD_ZIRNOX.get()))
                            .filter(stack -> variantId(stack).equals(fuel.id()))
                            .count()
            );
        }

        if (BuiltInRegistries.ITEM.containsKey(ReinhardtsHBM.id("rbmk_fuel"))) {
            throw new IllegalStateException("Obsolete aggregate RBMK fuel item is still registered");
        }
        for (String fuelId : RbmkFuelRodItem.FUEL_IDS) {
            RbmkFuelRodItem rod = rbmkFuelItem(fuelId);
            requireExactlyOne(
                    "RBMK construction recipe " + fuelId,
                    results.stream()
                            .filter(stack -> stack.is(rod))
                            .count()
            );
        }
        RbmkFuelRodItem testRod = rbmkFuelItem("test");
        requireExactly(
                "RBMK test rod construction recipe",
                0,
                results.stream().filter(stack -> stack.is(testRod)).count()
        );
    }

    private static void validatePwrCycle(MinecraftServer server, ServerLevel level) {
        List<RecipeHolder<PurexRecipe>> purex = server.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.PUREX.get());
        for (int index = 0; index < PwrFuelItem.FUELS.size(); index++) {
            String fuelId = PwrFuelItem.FUELS.get(index).id();
            ItemStack hot = PwrFuelItem.stackForFuel(HbmItems.PWR_FUEL_HOT.get(), index);
            FuelPoolRecipe.Input input = new FuelPoolRecipe.Input(hot);
            RecipeHolder<FuelPoolRecipe> cooling = server.getRecipeManager()
                    .getRecipeFor(HbmRecipeTypes.FUEL_POOL.get(), input, level)
                    .orElseThrow(() -> new IllegalStateException("Missing PWR cooling recipe for "
                            + fuelId));
            ItemStack cooled = cooling.value().assemble(input, server.registryAccess());
            if (!cooled.is(HbmItems.PWR_FUEL_DEPLETED.get()) || PwrFuelItem.fuelIndex(cooled) != index) {
                throw new IllegalStateException("PWR cooling recipe returns the wrong fuel state for "
                        + fuelId);
            }
            requireExactlyOne(
                    "PWR PUREX recipe " + fuelId,
                    purex.stream()
                            .map(RecipeHolder::value)
                            .filter(recipe -> recipe.group().equals("autoswitch.pwr"))
                            .filter(recipe -> firstInputAccepts(recipe, cooled))
                            .count()
            );
        }
    }

    private static void validateZirnoxCycle(MinecraftServer server, ServerLevel level) {
        List<RecipeHolder<PurexRecipe>> purex = server.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.PUREX.get());
        for (Map.Entry<String, String> entry : ZIRNOX_SPENT_FUEL.entrySet()) {
            String variant = entry.getKey();
            ItemStack depletedRod = LegacyVariantItem.stackFor(HbmItems.ROD_ZIRNOX_DEPLETED.get(), variant);
            CraftingInput craftingInput = CraftingInput.of(1, 1, List.of(depletedRod));
            RecipeHolder<CraftingRecipe> unloading = server.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftingInput, level)
                    .orElseThrow(() -> new IllegalStateException("Missing ZIRNOX unloading recipe for " + variant));
            ItemStack hotWaste = unloading.value().assemble(craftingInput, server.registryAccess());
            ResourceLocation wasteId = BuiltInRegistries.ITEM.getKey(hotWaste.getItem());
            if (!wasteId.equals(ReinhardtsHBM.id(entry.getValue()))
                    || hotWaste.getCount() != 2
                    || !WasteFuelItem.isHot(hotWaste)) {
                throw new IllegalStateException("ZIRNOX unloading recipe returns the wrong spent fuel for " + variant);
            }
            ItemStack remainder = depletedRod.getItem().getCraftingRemainingItem(depletedRod);
            if (!remainder.is(HbmItems.ROD_ZIRNOX_EMPTY.get())) {
                throw new IllegalStateException("ZIRNOX depleted rod does not return its empty casing for " + variant);
            }

            FuelPoolRecipe.Input coolingInput = new FuelPoolRecipe.Input(hotWaste);
            RecipeHolder<FuelPoolRecipe> cooling = server.getRecipeManager()
                    .getRecipeFor(HbmRecipeTypes.FUEL_POOL.get(), coolingInput, level)
                    .orElseThrow(() -> new IllegalStateException("Missing ZIRNOX cooling recipe for " + variant));
            ItemStack cooledWaste = cooling.value().assemble(coolingInput, server.registryAccess());
            if (cooledWaste.getItem() != hotWaste.getItem() || WasteFuelItem.isHot(cooledWaste)) {
                throw new IllegalStateException("ZIRNOX cooling recipe returns the wrong state for " + variant);
            }
            requireExactlyOne(
                    "ZIRNOX PUREX recipe " + variant,
                    purex.stream()
                            .map(RecipeHolder::value)
                            .filter(recipe -> recipe.group().equals("autoswitch.zirnox"))
                            .filter(recipe -> firstInputAccepts(recipe, cooledWaste))
                            .count()
            );
        }
    }

    private static void validateRbmkCycle(ServerLevel level) {
        validateRbmkDisassembly(level);

        List<RecipeHolder<SilexRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.SILEX.get());
        long rbmkRecipeCount = recipes.stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.group().startsWith("silex.rbmk_pellet_"))
                .count();
        int expectedCount = RBMK_SILEX_STATES.values().stream().mapToInt(Integer::intValue).sum();
        requireExactly("RBMK SILEX recipe total", expectedCount, rbmkRecipeCount);

        for (Map.Entry<String, Integer> entry : RBMK_SILEX_STATES.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("rbmk_pellet_" + entry.getKey()));
            if (!(item instanceof RbmkPelletItem pellet)) {
                throw new IllegalStateException("Missing RBMK pellet item for SILEX family " + entry.getKey());
            }
            for (int state = 0; state < entry.getValue(); state++) {
                int pelletState = state;
                ItemStack stack = pellet.stackFor(pelletState);
                SilexRecipe.Input input = new SilexRecipe.Input(stack);
                RecipeHolder<SilexRecipe> recipe = level.getRecipeManager()
                        .getRecipeFor(HbmRecipeTypes.SILEX.get(), input, level)
                        .orElseThrow(() -> new IllegalStateException("Missing RBMK SILEX recipe for "
                                + entry.getKey() + " state " + pellet.state(stack)));
                String expectedGroup = "silex.rbmk_pellet_" + entry.getKey() + "_" + pelletState;
                if (!recipe.value().group().equals(expectedGroup)) {
                    throw new IllegalStateException("RBMK SILEX state matched the wrong recipe: expected "
                            + expectedGroup + ", got " + recipe.value().group());
                }
            }
        }
    }

    private static void validateRbmkDisassembly(ServerLevel level) {
        RecipeHolder<CraftingRecipe> disassembly = level.getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().equals(ReinhardtsHBM.id("rbmk_fuel_disassembly")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing RBMK fuel disassembly recipe"));

        for (String fuelId : RbmkFuelRodItem.FUEL_IDS) {
            RbmkFuelRodItem rod = rbmkFuelItem(fuelId);
            ItemStack usedRod = new ItemStack(rod);
            RbmkFuelRodItem.setState(usedRod, 0.5D, 0.0D, 20.0D, 20.0D);
            CraftingInput input = CraftingInput.of(1, 1, List.of(usedRod));
            if (!disassembly.value().matches(input, level)) {
                throw new IllegalStateException("RBMK disassembly does not accept " + fuelId);
            }

            ItemStack output = disassembly.value().assemble(input, level.registryAccess());
            String pelletId = fuelId.equals("hep") ? "hep239" : fuelId;
            ResourceLocation expectedPellet = ReinhardtsHBM.id("rbmk_pellet_" + pelletId);
            if (!BuiltInRegistries.ITEM.getKey(output.getItem()).equals(expectedPellet) || output.getCount() != 8) {
                throw new IllegalStateException("RBMK disassembly returns the wrong pellets for " + fuelId);
            }

            List<ItemStack> remaining = disassembly.value().getRemainingItems(input);
            if (remaining.size() != 1 || !remaining.getFirst().is(HbmItems.RBMK_FUEL_EMPTY.get())) {
                throw new IllegalStateException("RBMK disassembly does not return the empty casing for " + fuelId);
            }
        }

        RbmkFuelRodItem testRod = rbmkFuelItem("test");
        ItemStack testStack = new ItemStack(testRod);
        RbmkFuelRodItem.setState(testStack, 0.5D, 0.0D, 20.0D, 20.0D);
        CraftingInput testInput = CraftingInput.of(1, 1, List.of(testStack));
        if (disassembly.value().matches(testInput, level)) {
            throw new IllegalStateException("RBMK test rod must not be disassemblable");
        }
    }

    private static RbmkFuelRodItem rbmkFuelItem(String fuelId) {
        ResourceLocation id = ReinhardtsHBM.id("rbmk_fuel_" + fuelId);
        Item item = BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof RbmkFuelRodItem rod)) {
            throw new IllegalStateException("Missing standalone RBMK fuel item " + id);
        }
        ItemStack stack = new ItemStack(rod);
        if (!rod.fuelId(stack).equals(fuelId)) {
            throw new IllegalStateException("RBMK fuel item " + id + " resolves as " + rod.fuelId(stack));
        }
        return rod;
    }

    private static boolean firstInputAccepts(PurexRecipe recipe, ItemStack stack) {
        return !recipe.inputItems().isEmpty() && recipe.inputItems().getFirst().ingredient().test(stack);
    }

    private static String variantId(ItemStack stack) {
        return stack.getItem() instanceof LegacyVariantItem variantItem
                ? variantItem.variant(stack).id()
                : "";
    }

    private static void requireExactlyOne(String label, long actual) {
        requireExactly(label, 1, actual);
    }

    private static void requireExactly(String label, long expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(label + ": expected " + expected + ", found " + actual);
        }
    }

    private static Map<String, Integer> rbmkSilexStates() {
        Map<String, Integer> states = new LinkedHashMap<>();
        for (String id : List.of("balefire", "balefire_gold", "flashlead", "po210be", "ra226be")) {
            states.put(id, 5);
        }
        for (String id : List.of(
                "drx", "heaus", "hen", "hep239", "hep241", "hes", "heu233", "heu235", "leaus",
                "lep", "les", "men", "mep", "mes", "meu", "mox", "pu238be", "thmeu", "ueu",
                "uzh", "zfb_am_mix", "zfb_bismuth", "zfb_pu241"
        )) {
            states.put(id, 10);
        }
        return Map.copyOf(states);
    }
}
