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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record CokerRecipe(
        String group,
        FluidIngredient input,
        ItemStack output,
        FluidOutput byproduct
) implements Recipe<CokerRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return input.fluid() == this.input.fluid() && input.amount() >= this.input.amount();
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
        if (!this.output.isEmpty()) {
            return this.output;
        }
        return FluidIconItem.forFluid(this.byproduct.fluid());
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
        return HbmRecipeTypes.COKER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.COKER.get();
    }

    public boolean hasByproduct() {
        return this.byproduct != null && !this.byproduct.isEmpty();
    }

    public HbmFluidStack inputStack() {
        return new HbmFluidStack(this.input.fluid(), this.input.amount(), 0);
    }

    public ItemStack displayIcon() {
        return FluidIconItem.forFluid(this.input.fluid());
    }

    public record FluidIngredient(HbmFluidDefinition fluid, int amount) {
        private static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidIngredient::amount)
        ).apply(instance, FluidIngredient::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidIngredient decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidIngredient ingredient) {
                buffer.writeUtf(ingredient.fluid().name());
                buffer.writeVarInt(ingredient.amount());
            }
        };

        private static FluidIngredient fromName(String fluid, int amount) {
            return new FluidIngredient(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }
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

    public record Input(HbmFluidDefinition fluid, int amount) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class Serializer implements RecipeSerializer<CokerRecipe> {
        private static final MapCodec<CokerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(CokerRecipe::group),
                FluidIngredient.CODEC.fieldOf("input").forGetter(CokerRecipe::input),
                ItemStack.STRICT_CODEC.optionalFieldOf("output", ItemStack.EMPTY).forGetter(CokerRecipe::output),
                FluidOutput.CODEC.optionalFieldOf("byproduct", FluidOutput.EMPTY).forGetter(CokerRecipe::byproduct)
        ).apply(instance, CokerRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CokerRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CokerRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                FluidIngredient input = FluidIngredient.STREAM_CODEC.decode(buffer);
                ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                FluidOutput byproduct = FluidOutput.STREAM_CODEC.decode(buffer);
                return new CokerRecipe(group, input, output, byproduct);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CokerRecipe recipe) {
                buffer.writeUtf(recipe.group());
                FluidIngredient.STREAM_CODEC.encode(buffer, recipe.input());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.output());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.byproduct());
            }
        };

        @Override
        public MapCodec<CokerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CokerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
