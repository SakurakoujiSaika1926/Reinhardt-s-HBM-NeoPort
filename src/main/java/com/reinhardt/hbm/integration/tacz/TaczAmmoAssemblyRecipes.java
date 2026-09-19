package com.reinhardt.hbm.integration.tacz;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.AssemblyMachineBlockEntity;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class TaczAmmoAssemblyRecipes {
    private static final String TACZ_MOD_ID = "tacz";
    private static final ResourceLocation TACZ_GUN_SMITH_TABLE =
            ResourceLocation.fromNamespaceAndPath(TACZ_MOD_ID, "gun_smith_table_crafting");
    private static final ResourceLocation TACZ_AMMO =
            ResourceLocation.fromNamespaceAndPath(TACZ_MOD_ID, "ammo");
    private static final int DEFAULT_DURATION = 100;
    private static final int DEFAULT_POWER = 100;

    private TaczAmmoAssemblyRecipes() {
    }

    public static List<RecipeHolder<AssemblyMachineRecipe>> available(Level level) {
        if (level == null || !ModList.get().isLoaded(TACZ_MOD_ID)) {
            return List.of();
        }

        Optional<RecipeType<?>> recipeType = BuiltInRegistries.RECIPE_TYPE.getOptional(TACZ_GUN_SMITH_TABLE);
        if (recipeType.isEmpty()) {
            return List.of();
        }

        List<RecipeHolder<AssemblyMachineRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : getAllRecipesFor(level, recipeType.get())) {
            toAssemblyRecipe(level, holder).ifPresent(recipes::add);
        }
        recipes.sort(Comparator.comparing(holder -> holder.id().toString()));
        return List.copyOf(recipes);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<RecipeHolder<?>> getAllRecipesFor(Level level, RecipeType<?> recipeType) {
        return (List) level.getRecipeManager().getAllRecipesFor((RecipeType) recipeType);
    }

    private static Optional<RecipeHolder<AssemblyMachineRecipe>> toAssemblyRecipe(Level level, RecipeHolder<?> holder) {
        Object recipeObject = holder.value();
        if (!(recipeObject instanceof Recipe<?> recipe)) {
            return Optional.empty();
        }

        invokeInit(recipeObject, level.registryAccess());
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        if (!isTaczAmmo(result)) {
            result = invokeItemStack(recipeObject, "getOutput").orElse(ItemStack.EMPTY);
            if (!isTaczAmmo(result)) {
                return Optional.empty();
            }
        }

        List<AssemblyMachineRecipe.CountedIngredient> ingredients = inputs(recipeObject);
        if (ingredients.isEmpty() || ingredients.size() > AssemblyMachineBlockEntity.INPUT_END - AssemblyMachineBlockEntity.INPUT_START) {
            return Optional.empty();
        }

        ResourceLocation id = ReinhardtsHBM.id("tacz_ammo_assembler/" + holder.id().getNamespace() + "/" + holder.id().getPath());
        AssemblyMachineRecipe assemblyRecipe = new AssemblyMachineRecipe(
                "tacz_ammo",
                List.of(),
                DEFAULT_DURATION,
                DEFAULT_POWER,
                ingredients,
                List.of(),
                List.of(),
                result.copy()
        );
        return Optional.of(new RecipeHolder<>(id, assemblyRecipe));
    }

    private static boolean isTaczAmmo(ItemStack stack) {
        return !stack.isEmpty() && TACZ_AMMO.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static List<AssemblyMachineRecipe.CountedIngredient> inputs(Object recipeObject) {
        Object value = invoke(recipeObject, "getInputs").orElse(null);
        if (!(value instanceof List<?> taczInputs)) {
            return List.of();
        }

        List<AssemblyMachineRecipe.CountedIngredient> ingredients = new ArrayList<>(taczInputs.size());
        for (Object taczInput : taczInputs) {
            Ingredient ingredient = invokeIngredient(taczInput, "getIngredient").orElse(Ingredient.EMPTY);
            int count = invokeInt(taczInput, "getCount").orElse(1);
            if (ingredient.isEmpty() || count <= 0) {
                return List.of();
            }
            int remaining = count;
            while (remaining > 0) {
                int stackCount = Math.min(remaining, 64);
                ingredients.add(new AssemblyMachineRecipe.CountedIngredient(ingredient, stackCount));
                remaining -= stackCount;
            }
        }
        return List.copyOf(ingredients);
    }

    private static Optional<ItemStack> invokeItemStack(Object target, String methodName) {
        Object value = invoke(target, methodName).orElse(null);
        return value instanceof ItemStack stack ? Optional.of(stack.copy()) : Optional.empty();
    }

    private static Optional<Ingredient> invokeIngredient(Object target, String methodName) {
        Object value = invoke(target, methodName).orElse(null);
        return value instanceof Ingredient ingredient ? Optional.of(ingredient) : Optional.empty();
    }

    private static Optional<Integer> invokeInt(Object target, String methodName) {
        Object value = invoke(target, methodName).orElse(null);
        if (value instanceof Integer integer) {
            return Optional.of(integer);
        }
        return Optional.empty();
    }

    private static Optional<Object> invoke(Object target, String methodName) {
        if (target == null) {
            return Optional.empty();
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return Optional.ofNullable(method.invoke(target));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ReinhardtsHBM.LOGGER.debug("Could not read TaCZ gun smith table recipe method {}", methodName, exception);
            return Optional.empty();
        }
    }

    private static void invokeInit(Object target, HolderLookup.Provider registries) {
        try {
            Method method = target.getClass().getMethod("init", HolderLookup.Provider.class);
            method.invoke(target, registries);
        } catch (NoSuchMethodException ignored) {
            // Other recipe implementations do not need TaCZ's post-load result initialization.
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ReinhardtsHBM.LOGGER.debug("Could not initialize TaCZ gun smith table recipe result", exception);
        }
    }
}
