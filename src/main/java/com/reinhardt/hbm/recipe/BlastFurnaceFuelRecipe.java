package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record BlastFurnaceFuelRecipe(String group, Ingredient ingredient, int power) implements Recipe<BlastFurnaceFuelRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return this.ingredient.test(input.stack());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
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
        return HbmRecipeTypes.BLAST_FURNACE_FUEL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.BLAST_FURNACE_FUEL.get();
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

    public static class Serializer implements RecipeSerializer<BlastFurnaceFuelRecipe> {
        private static final MapCodec<BlastFurnaceFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(BlastFurnaceFuelRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BlastFurnaceFuelRecipe::ingredient),
                Codec.intRange(1, 1_000_000).fieldOf("power").forGetter(BlastFurnaceFuelRecipe::power)
        ).apply(instance, BlastFurnaceFuelRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceFuelRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public BlastFurnaceFuelRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int power = buffer.readVarInt();
                return new BlastFurnaceFuelRecipe(group, ingredient, power);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, BlastFurnaceFuelRecipe recipe) {
                buffer.writeUtf(recipe.group());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                buffer.writeVarInt(recipe.power());
            }
        };

        @Override
        public MapCodec<BlastFurnaceFuelRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceFuelRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
