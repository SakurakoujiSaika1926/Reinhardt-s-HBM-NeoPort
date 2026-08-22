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

public record CyclotronRecipe(
        String group,
        Ingredient particle,
        Ingredient input,
        ItemStack output,
        int antimatter
) implements Recipe<CyclotronRecipe.Input> {
    public CyclotronRecipe {
        antimatter = Math.max(0, antimatter);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return this.particle.test(input.particle()) && this.input.test(input.input());
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
        return new ItemStack(HbmBlocks.MACHINE_CYCLOTRON.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CYCLOTRON_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.CYCLOTRON.get();
    }

    public record Input(ItemStack particle, ItemStack input) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> particle;
                case 1 -> input;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Serializer implements RecipeSerializer<CyclotronRecipe> {
        private static final MapCodec<CyclotronRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(CyclotronRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("particle").forGetter(CyclotronRecipe::particle),
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(CyclotronRecipe::input),
                ItemStack.CODEC.fieldOf("output").forGetter(CyclotronRecipe::output),
                com.mojang.serialization.Codec.INT.optionalFieldOf("antimatter", 0).forGetter(CyclotronRecipe::antimatter)
        ).apply(instance, CyclotronRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CyclotronRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CyclotronRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new CyclotronRecipe(
                        buffer.readUtf(),
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        ItemStack.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CyclotronRecipe recipe) {
                buffer.writeUtf(recipe.group);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.particle);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
                ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
                buffer.writeVarInt(recipe.antimatter);
            }
        };

        @Override
        public MapCodec<CyclotronRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CyclotronRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
