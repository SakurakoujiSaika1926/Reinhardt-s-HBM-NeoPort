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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record CombinationOvenRecipe(
        String group,
        Ingredient input,
        ItemStack output,
        FluidOutput fluid
) implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        if (!this.output.isEmpty()) {
            return this.output;
        }
        return hasFluid() ? FluidIconItem.forFluid(this.fluid.fluid()) : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.input);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.COMBINATION_OVEN_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.COMBINATION_OVEN.get();
    }

    public boolean hasItemOutput() {
        return !this.output.isEmpty();
    }

    public boolean hasFluid() {
        return this.fluid != null && !this.fluid.isEmpty();
    }

    public HbmFluidStack fluidStack() {
        return hasFluid() ? new HbmFluidStack(this.fluid.fluid(), this.fluid.amount(), 0) : HbmFluidStack.EMPTY;
    }

    public record FluidOutput(HbmFluidDefinition fluid, int amount) {
        public static final FluidOutput EMPTY = new FluidOutput(HbmFluids.none(), 0);

        private static final Codec<FluidOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(0, 1_000_000).fieldOf("amount").forGetter(FluidOutput::amount)
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

        public boolean isEmpty() {
            return this.fluid.isNone() || this.amount <= 0;
        }

        private static FluidOutput fromName(String fluid, int amount) {
            return new FluidOutput(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }
    }

    public static class Serializer implements RecipeSerializer<CombinationOvenRecipe> {
        private static final MapCodec<CombinationOvenRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(CombinationOvenRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(CombinationOvenRecipe::input),
                ItemStack.STRICT_CODEC.optionalFieldOf("output", ItemStack.EMPTY).forGetter(CombinationOvenRecipe::output),
                FluidOutput.CODEC.optionalFieldOf("fluid", FluidOutput.EMPTY).forGetter(CombinationOvenRecipe::fluid)
        ).apply(instance, CombinationOvenRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CombinationOvenRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CombinationOvenRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                FluidOutput fluid = FluidOutput.STREAM_CODEC.decode(buffer);
                return new CombinationOvenRecipe(group, input, output, fluid);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CombinationOvenRecipe recipe) {
                buffer.writeUtf(recipe.group());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.output());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.fluid());
            }
        };

        @Override
        public MapCodec<CombinationOvenRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CombinationOvenRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
