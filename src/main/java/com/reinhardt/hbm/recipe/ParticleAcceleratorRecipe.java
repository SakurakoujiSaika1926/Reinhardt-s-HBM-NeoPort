package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
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

public record ParticleAcceleratorRecipe(
        String group,
        Ingredient input1,
        Ingredient input2,
        ItemStack output1,
        ItemStack output2,
        int momentum
) implements Recipe<ParticleAcceleratorRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return matchesSymmetric(input.first(), input.second());
    }

    public boolean matchesSymmetric(ItemStack first, ItemStack second) {
        return (this.input1.test(first) && this.input2.test(second))
                || (this.input1.test(second) && this.input2.test(first));
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.output1.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.output1.copy();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(HbmBlocks.PA_DETECTOR.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.PARTICLE_ACCELERATOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.PARTICLE_ACCELERATOR.get();
    }

    public record Input(ItemStack first, ItemStack second) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> first;
                case 1 -> second;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Serializer implements RecipeSerializer<ParticleAcceleratorRecipe> {
        private static final MapCodec<ParticleAcceleratorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ParticleAcceleratorRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("input1").forGetter(ParticleAcceleratorRecipe::input1),
                Ingredient.CODEC_NONEMPTY.fieldOf("input2").forGetter(ParticleAcceleratorRecipe::input2),
                ItemStack.STRICT_CODEC.fieldOf("output1").forGetter(ParticleAcceleratorRecipe::output1),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("output2", ItemStack.EMPTY).forGetter(ParticleAcceleratorRecipe::output2),
                Codec.intRange(0, 1_000_000_000).fieldOf("momentum").forGetter(ParticleAcceleratorRecipe::momentum)
        ).apply(instance, ParticleAcceleratorRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ParticleAcceleratorRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ParticleAcceleratorRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new ParticleAcceleratorRecipe(
                        buffer.readUtf(),
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        ItemStack.STREAM_CODEC.decode(buffer),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                        buffer.readVarInt()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ParticleAcceleratorRecipe recipe) {
                buffer.writeUtf(recipe.group());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input1());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input2());
                ItemStack.STREAM_CODEC.encode(buffer, recipe.output1());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.output2());
                buffer.writeVarInt(recipe.momentum());
            }
        };

        @Override
        public MapCodec<ParticleAcceleratorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ParticleAcceleratorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
