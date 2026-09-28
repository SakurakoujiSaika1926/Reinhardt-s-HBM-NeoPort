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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Datapack form of the latest 1.7.10 {@code SpaceAssemblerRecipes} contract. */
public record SpaceAssemblerRecipe(
        String group,
        Ingredient ingredient,
        int inputCount,
        List<ItemStack> results,
        int duration
) implements Recipe<SingleRecipeInput> {
    public static final int OUTPUT_LIMIT = 9;

    public SpaceAssemblerRecipe {
        group = group == null ? "" : group;
        results = results.stream().map(ItemStack::copy).toList();
        if (inputCount <= 0 || inputCount > 64) {
            throw new IllegalArgumentException("Space assembler input count must be between 1 and 64");
        }
        if (results.isEmpty() || results.size() > OUTPUT_LIMIT || results.stream().anyMatch(ItemStack::isEmpty)) {
            throw new IllegalArgumentException("Space assembler recipes require one to nine non-empty outputs");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Space assembler duration must be positive");
        }
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        ItemStack stack = input.item();
        return !stack.isEmpty() && stack.getCount() >= this.inputCount && this.ingredient.test(stack);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return this.results.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.results.getFirst().copy();
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
        return HbmRecipeTypes.SPACE_ASSEMBLER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.SPACE_ASSEMBLER.get();
    }

    public static final class Serializer implements RecipeSerializer<SpaceAssemblerRecipe> {
        private static final MapCodec<SpaceAssemblerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(SpaceAssemblerRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SpaceAssemblerRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(SpaceAssemblerRecipe::inputCount),
                ItemStack.STRICT_CODEC.listOf(1, OUTPUT_LIMIT).fieldOf("results").forGetter(SpaceAssemblerRecipe::results),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(SpaceAssemblerRecipe::duration)
        ).apply(instance, SpaceAssemblerRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SpaceAssemblerRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SpaceAssemblerRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int inputCount = buffer.readVarInt();
                int resultCount = buffer.readVarInt();
                if (resultCount < 1 || resultCount > OUTPUT_LIMIT) {
                    throw new IllegalArgumentException("Invalid space assembler output count: " + resultCount);
                }
                List<ItemStack> results = new ArrayList<>(resultCount);
                for (int index = 0; index < resultCount; index++) {
                    results.add(ItemStack.STREAM_CODEC.decode(buffer));
                }
                return new SpaceAssemblerRecipe(group, ingredient, inputCount, results, buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, SpaceAssemblerRecipe recipe) {
                buffer.writeUtf(recipe.group());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                buffer.writeVarInt(recipe.inputCount());
                buffer.writeVarInt(recipe.results().size());
                for (ItemStack result : recipe.results()) {
                    ItemStack.STREAM_CODEC.encode(buffer, result);
                }
                buffer.writeVarInt(recipe.duration());
            }
        };

        @Override
        public MapCodec<SpaceAssemblerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SpaceAssemblerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
