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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record FusionBreederFluidRecipe(
        String group,
        FluidStack input,
        FluidStack output
) implements Recipe<FusionBreederFluidRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return input.fluid() == this.input.fluid() && input.amount() >= this.input.amount();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
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
        return HbmRecipeTypes.FUSION_BREEDER_FLUID_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.FUSION_BREEDER_FLUID.get();
    }

    public record FluidStack(HbmFluidDefinition fluid, int amount) {
        private static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidStack::amount)
        ).apply(instance, FluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                stack -> stack.fluid().name(),
                ByteBufCodecs.VAR_INT,
                FluidStack::amount,
                FluidStack::fromName
        );

        private static FluidStack fromName(String fluid, int amount) {
            return new FluidStack(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }

        public boolean isEmpty() {
            return this.fluid.isNone() || this.amount <= 0;
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

    public static class Serializer implements RecipeSerializer<FusionBreederFluidRecipe> {
        private static final MapCodec<FusionBreederFluidRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(FusionBreederFluidRecipe::group),
                FluidStack.CODEC.fieldOf("input").forGetter(FusionBreederFluidRecipe::input),
                FluidStack.CODEC.fieldOf("output").forGetter(FusionBreederFluidRecipe::output)
        ).apply(instance, FusionBreederFluidRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FusionBreederFluidRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                FusionBreederFluidRecipe::group,
                FluidStack.STREAM_CODEC,
                FusionBreederFluidRecipe::input,
                FluidStack.STREAM_CODEC,
                FusionBreederFluidRecipe::output,
                FusionBreederFluidRecipe::new
        );

        @Override
        public MapCodec<FusionBreederFluidRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FusionBreederFluidRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
