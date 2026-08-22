package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.registry.HbmBlocks;
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

public record ExposureChamberRecipe(
        String group,
        Ingredient particle,
        Ingredient ingredient,
        ItemStack output
) implements Recipe<ExposureChamberRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return this.particle.test(input.particle()) && this.ingredient.test(input.ingredient());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.output.copy();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.EXPOSURE_CHAMBER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.EXPOSURE_CHAMBER.get();
    }

    public record Input(ItemStack particle, ItemStack ingredient) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> particle;
                case 1 -> ingredient;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Serializer implements RecipeSerializer<ExposureChamberRecipe> {
        private static final MapCodec<ExposureChamberRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(ExposureChamberRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("particle").forGetter(ExposureChamberRecipe::particle),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ExposureChamberRecipe::ingredient),
                ItemStack.CODEC.fieldOf("output").forGetter(ExposureChamberRecipe::output)
        ).apply(instance, ExposureChamberRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ExposureChamberRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ExposureChamberRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new ExposureChamberRecipe(
                        buffer.readUtf(),
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        ItemStack.STREAM_CODEC.decode(buffer)
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ExposureChamberRecipe recipe) {
                buffer.writeUtf(recipe.group);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.particle);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
            }
        };

        @Override
        public MapCodec<ExposureChamberRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExposureChamberRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
