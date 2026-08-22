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

import java.util.List;

public record CentrifugeRecipe(
        String group,
        Ingredient ingredient,
        int inputCount,
        List<ItemStack> results
) implements Recipe<CentrifugeRecipe.Input> {
    public CentrifugeRecipe {
        results = List.copyOf(results.stream().filter(stack -> !stack.isEmpty()).limit(4).toList());
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.item().getCount() >= this.inputCount && this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.results.isEmpty() ? ItemStack.EMPTY : this.results.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.results.isEmpty() ? ItemStack.EMPTY : this.results.getFirst();
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
        return HbmRecipeTypes.CENTRIFUGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.CENTRIFUGE.get();
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

    public static class Serializer implements RecipeSerializer<CentrifugeRecipe> {
        private static final MapCodec<CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(CentrifugeRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CentrifugeRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(CentrifugeRecipe::inputCount),
                ItemStack.STRICT_CODEC.listOf(1, 4).fieldOf("results").forGetter(CentrifugeRecipe::results)
        ).apply(instance, CentrifugeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                CentrifugeRecipe::group,
                Ingredient.CONTENTS_STREAM_CODEC,
                CentrifugeRecipe::ingredient,
                ByteBufCodecs.VAR_INT,
                CentrifugeRecipe::inputCount,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(4)),
                CentrifugeRecipe::results,
                CentrifugeRecipe::new
        );

        @Override
        public MapCodec<CentrifugeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
