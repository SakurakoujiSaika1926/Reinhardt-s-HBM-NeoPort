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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record ShredderRecipe(String group, Ingredient ingredient, int inputCount, ItemStack result) implements Recipe<SingleRecipeInput> {
    public ShredderRecipe(Ingredient ingredient, ItemStack result) {
        this("", ingredient, 1, result);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
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
        return HbmRecipeTypes.SHREDDER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.SHREDDER.get();
    }

    public static class Serializer implements RecipeSerializer<ShredderRecipe> {
        private static final MapCodec<ShredderRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShredderRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ShredderRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(ShredderRecipe::inputCount),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ShredderRecipe::result)
        ).apply(instance, ShredderRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ShredderRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ShredderRecipe::group,
                Ingredient.CONTENTS_STREAM_CODEC,
                ShredderRecipe::ingredient,
                ByteBufCodecs.VAR_INT,
                ShredderRecipe::inputCount,
                ItemStack.STREAM_CODEC,
                ShredderRecipe::result,
                ShredderRecipe::new
        );

        @Override
        public MapCodec<ShredderRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShredderRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
