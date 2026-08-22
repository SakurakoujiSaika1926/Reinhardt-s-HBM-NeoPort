package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.registry.HbmFluids;
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

public record CompressorRecipe(
        String group,
        FluidIngredient input,
        FluidIngredient output,
        int duration
) implements Recipe<CompressorRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return input.fluid() == this.input.fluid()
                && input.pressure() == this.input.pressure()
                && input.amount() >= this.input.amount();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return FluidIconItem.forFluid(this.output.fluid());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return FluidIconItem.forFluid(this.output.fluid());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.COMPRESSOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.COMPRESSOR.get();
    }

    public record Input(HbmFluidDefinition fluid, int amount, int pressure) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public record FluidIngredient(HbmFluidDefinition fluid, int amount, int pressure) {
        public static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidIngredient::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(FluidIngredient::pressure)
        ).apply(instance, FluidIngredient::fromName));

        public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidIngredient decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidIngredient ingredient) {
                buffer.writeUtf(ingredient.fluid().name());
                buffer.writeVarInt(ingredient.amount());
                buffer.writeVarInt(ingredient.pressure());
            }
        };

        private static FluidIngredient fromName(String fluid, int amount, int pressure) {
            return new FluidIngredient(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount, pressure);
        }
    }

    public static class Serializer implements RecipeSerializer<CompressorRecipe> {
        private static final MapCodec<CompressorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(CompressorRecipe::group),
                FluidIngredient.CODEC.fieldOf("input").forGetter(CompressorRecipe::input),
                FluidIngredient.CODEC.fieldOf("output").forGetter(CompressorRecipe::output),
                Codec.intRange(1, 20_000).fieldOf("duration").forGetter(CompressorRecipe::duration)
        ).apply(instance, CompressorRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CompressorRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CompressorRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new CompressorRecipe(
                        buffer.readUtf(),
                        FluidIngredient.STREAM_CODEC.decode(buffer),
                        FluidIngredient.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CompressorRecipe recipe) {
                buffer.writeUtf(recipe.group());
                FluidIngredient.STREAM_CODEC.encode(buffer, recipe.input());
                FluidIngredient.STREAM_CODEC.encode(buffer, recipe.output());
                buffer.writeVarInt(recipe.duration());
            }
        };

        @Override
        public MapCodec<CompressorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CompressorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
