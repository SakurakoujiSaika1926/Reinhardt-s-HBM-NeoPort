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

public record FusionBreederItemRecipe(
        String group,
        Ingredient ingredient,
        ItemStack result,
        int flux
) implements Recipe<FusionBreederItemRecipe.Input> {
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
        return HbmRecipeTypes.FUSION_BREEDER_ITEM_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.FUSION_BREEDER_ITEM.get();
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

    public static class Serializer implements RecipeSerializer<FusionBreederItemRecipe> {
        private static final MapCodec<FusionBreederItemRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(FusionBreederItemRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FusionBreederItemRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FusionBreederItemRecipe::result),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("flux").forGetter(FusionBreederItemRecipe::flux)
        ).apply(instance, FusionBreederItemRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FusionBreederItemRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                FusionBreederItemRecipe::group,
                Ingredient.CONTENTS_STREAM_CODEC,
                FusionBreederItemRecipe::ingredient,
                ItemStack.STREAM_CODEC,
                FusionBreederItemRecipe::result,
                ByteBufCodecs.VAR_INT,
                FusionBreederItemRecipe::flux,
                FusionBreederItemRecipe::new
        );

        @Override
        public MapCodec<FusionBreederItemRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FusionBreederItemRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
