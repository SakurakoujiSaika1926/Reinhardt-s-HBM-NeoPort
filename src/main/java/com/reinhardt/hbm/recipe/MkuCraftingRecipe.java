package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyBookLoreItem;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Exact seed-dependent 3x3 MKU recipe from MKUCraftingHandler. */
public final class MkuCraftingRecipe extends CustomRecipe {
    private static final List<String> INGREDIENT_IDS = List.of(
            "powder_iodine", "powder_fire", "dust", "ingot_mercury", "morning_glory", "syringe_metal_empty"
    );

    public MkuCraftingRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!(level instanceof ServerLevel serverLevel) || input.width() != 3 || input.height() != 3) {
            return false;
        }
        List<Item> recipe = recipeFor(serverLevel.getSeed());
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = input.getItem(slot);
            Item expected = recipe.get(slot);
            if (expected == null ? !stack.isEmpty() : !stack.is(expected)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return itemStack("syringe_mkunicorn");
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return itemStack("syringe_mkunicorn");
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (String id : INGREDIENT_IDS) {
            ingredients.add(Ingredient.of(item(id)));
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.MKU_SERIALIZER.get();
    }

    public static List<Item> recipeFor(long worldSeed) {
        List<Item> recipe = new ArrayList<>(9);
        for (String id : INGREDIENT_IDS) {
            recipe.add(item(id));
        }
        recipe.add(null);
        recipe.add(null);
        recipe.add(null);
        Collections.shuffle(recipe, new Random(worldSeed));
        return recipe;
    }

    public static ItemStack randomClueIngredient(Level level) {
        return itemStack(INGREDIENT_IDS.get(level.random.nextInt(INGREDIENT_IDS.size())));
    }

    public static ItemStack clueBook(Level level, Item ingredient) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return ItemStack.EMPTY;
        }
        List<Item> recipe = recipeFor(serverLevel.getSeed());
        int slot = recipe.indexOf(ingredient) + 1;
        if (slot <= 0) {
            return ItemStack.EMPTY;
        }
        String id = BuiltInRegistries.ITEM.getKey(ingredient).getPath();
        String key;
        int pages;
        switch (id) {
            case "powder_iodine" -> { key = "book_iodine"; pages = 3; }
            case "powder_fire" -> { key = "book_phosphorous"; pages = 2; }
            case "dust" -> { key = "book_dust"; pages = 3; }
            case "ingot_mercury" -> { key = "book_mercury"; pages = 2; }
            case "morning_glory" -> { key = "book_flower"; pages = 2; }
            case "syringe_metal_empty" -> { key = "book_syringe"; pages = 2; }
            default -> { return ItemStack.EMPTY; }
        }
        ItemStack book = LegacyBookLoreItem.create(new ItemStack(HbmItems.BOOK_LORE.get()), key, pages, 0x271E44, 0xFBFFF4);
        return LegacyBookLoreItem.addArguments(book, pages - 1, Integer.toString(slot));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ReinhardtsHBM.MOD_ID, id));
    }

    private static ItemStack itemStack(String id) {
        return new ItemStack(item(id));
    }

    public static final class Serializer implements RecipeSerializer<MkuCraftingRecipe> {
        private static final MapCodec<MkuCraftingRecipe> CODEC = MapCodec.unit(MkuCraftingRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, MkuCraftingRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override public MkuCraftingRecipe decode(RegistryFriendlyByteBuf buffer) { return new MkuCraftingRecipe(); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, MkuCraftingRecipe recipe) { }
        };

        @Override public MapCodec<MkuCraftingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, MkuCraftingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
