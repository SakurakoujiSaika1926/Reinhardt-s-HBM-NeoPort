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

import java.util.Optional;

public record BlastFurnaceRecipe(
        String group,
        Ingredient inputA,
        int inputACount,
        Ingredient inputB,
        int inputBCount,
        int duration,
        ItemStack result,
        boolean hidden
) implements Recipe<BlastFurnaceRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return match(input.upper(), input.lower()).isPresent();
    }

    public Optional<Match> match(ItemStack upper, ItemStack lower) {
        if (matchesOrdered(upper, this.inputA, this.inputACount, lower, this.inputB, this.inputBCount)) {
            return Optional.of(new Match(this.inputACount, this.inputBCount, this.duration, this.result.copy()));
        }
        if (matchesOrdered(upper, this.inputB, this.inputBCount, lower, this.inputA, this.inputACount)) {
            return Optional.of(new Match(this.inputBCount, this.inputACount, this.duration, this.result.copy()));
        }
        return Optional.empty();
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
        ingredients.add(this.inputA);
        ingredients.add(this.inputB);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.BLAST_FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.BLAST_FURNACE.get();
    }

    private static boolean matchesOrdered(ItemStack upper, Ingredient upperIngredient, int upperCount, ItemStack lower, Ingredient lowerIngredient, int lowerCount) {
        return !upper.isEmpty()
                && upper.getCount() >= upperCount
                && upperIngredient.test(upper)
                && !lower.isEmpty()
                && lower.getCount() >= lowerCount
                && lowerIngredient.test(lower);
    }

    public record Match(int upperCount, int lowerCount, int duration, ItemStack result) {
    }

    public record Input(ItemStack upper, ItemStack lower) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> this.upper;
                case 1 -> this.lower;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Serializer implements RecipeSerializer<BlastFurnaceRecipe> {
        private static final MapCodec<BlastFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(BlastFurnaceRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("input_a").forGetter(BlastFurnaceRecipe::inputA),
                Codec.intRange(1, 64).optionalFieldOf("input_a_count", 1).forGetter(BlastFurnaceRecipe::inputACount),
                Ingredient.CODEC_NONEMPTY.fieldOf("input_b").forGetter(BlastFurnaceRecipe::inputB),
                Codec.intRange(1, 64).optionalFieldOf("input_b_count", 1).forGetter(BlastFurnaceRecipe::inputBCount),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(BlastFurnaceRecipe::duration),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(BlastFurnaceRecipe::result),
                Codec.BOOL.optionalFieldOf("hidden", false).forGetter(BlastFurnaceRecipe::hidden)
        ).apply(instance, BlastFurnaceRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public BlastFurnaceRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                Ingredient inputA = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int inputACount = buffer.readVarInt();
                Ingredient inputB = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int inputBCount = buffer.readVarInt();
                int duration = buffer.readVarInt();
                ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                boolean hidden = buffer.readBoolean();
                return new BlastFurnaceRecipe(group, inputA, inputACount, inputB, inputBCount, duration, result, hidden);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, BlastFurnaceRecipe recipe) {
                buffer.writeUtf(recipe.group());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.inputA());
                buffer.writeVarInt(recipe.inputACount());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.inputB());
                buffer.writeVarInt(recipe.inputBCount());
                buffer.writeVarInt(recipe.duration());
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result());
                buffer.writeBoolean(recipe.hidden());
            }
        };

        @Override
        public MapCodec<BlastFurnaceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
