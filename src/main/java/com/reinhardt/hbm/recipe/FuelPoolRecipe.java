package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record FuelPoolRecipe(String group, Ingredient input, ItemStack result) implements Recipe<FuelPoolRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return this.input.test(input.stack());
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
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.FUEL_POOL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.FUEL_POOL.get();
    }

    public record Input(ItemStack stack) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.stack : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<FuelPoolRecipe> {
        private static final MapCodec<FuelPoolRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(FuelPoolRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(FuelPoolRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FuelPoolRecipe::result)
        ).apply(instance, FuelPoolRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FuelPoolRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FuelPoolRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new FuelPoolRecipe(buffer.readUtf(), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), ItemStack.STREAM_CODEC.decode(buffer));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FuelPoolRecipe recipe) {
                buffer.writeUtf(recipe.group());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input());
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result());
            }
        };

        @Override
        public MapCodec<FuelPoolRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FuelPoolRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
