package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record BreederReactorRecipe(
        String group,
        Ingredient ingredient,
        ItemStack result,
        int flux
) implements Recipe<BreederReactorRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return !input.item().isEmpty() && this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.ingredient);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.BREEDER_REACTOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.BREEDER_REACTOR.get();
    }

    public record Input(ItemStack item) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.item : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<BreederReactorRecipe> {
        private static final MapCodec<BreederReactorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(BreederReactorRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BreederReactorRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(BreederReactorRecipe::result),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("flux").forGetter(BreederReactorRecipe::flux)
        ).apply(instance, BreederReactorRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BreederReactorRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                BreederReactorRecipe::group,
                Ingredient.CONTENTS_STREAM_CODEC,
                BreederReactorRecipe::ingredient,
                ItemStack.STREAM_CODEC,
                BreederReactorRecipe::result,
                ByteBufCodecs.VAR_INT,
                BreederReactorRecipe::flux,
                BreederReactorRecipe::new
        );

        @Override
        public MapCodec<BreederReactorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BreederReactorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
