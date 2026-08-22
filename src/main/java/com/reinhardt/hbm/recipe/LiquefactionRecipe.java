package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record LiquefactionRecipe(
        String group,
        Ingredient ingredient,
        FluidOutput output
) implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
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
        return HbmRecipeTypes.LIQUEFACTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.LIQUEFACTION.get();
    }

    public HbmFluidStack outputStack() {
        return new HbmFluidStack(this.output.fluid(), this.output.amount(), 0);
    }

    public ItemStack displayIcon() {
        return FluidIconItem.forFluid(this.output.fluid());
    }

    public record FluidOutput(HbmFluidDefinition fluid, int amount) {
        private static final Codec<FluidOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidOutput::amount)
        ).apply(instance, FluidOutput::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidOutput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidOutput decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidOutput output) {
                buffer.writeUtf(output.fluid().name());
                buffer.writeVarInt(output.amount());
            }
        };

        private static FluidOutput fromName(String fluid, int amount) {
            return new FluidOutput(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }
    }

    public static class Serializer implements RecipeSerializer<LiquefactionRecipe> {
        private static final MapCodec<LiquefactionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(LiquefactionRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(LiquefactionRecipe::ingredient),
                FluidOutput.CODEC.fieldOf("output").forGetter(LiquefactionRecipe::output)
        ).apply(instance, LiquefactionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, LiquefactionRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                LiquefactionRecipe::group,
                Ingredient.CONTENTS_STREAM_CODEC,
                LiquefactionRecipe::ingredient,
                FluidOutput.STREAM_CODEC,
                LiquefactionRecipe::output,
                LiquefactionRecipe::new
        );

        @Override
        public MapCodec<LiquefactionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LiquefactionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
