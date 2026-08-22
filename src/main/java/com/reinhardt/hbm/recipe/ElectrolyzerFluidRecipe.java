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

import java.util.List;

public record ElectrolyzerFluidRecipe(
        String group,
        FluidAmount input,
        FluidAmount output1,
        FluidAmount output2,
        int duration,
        List<ItemStack> byproducts
) implements Recipe<ElectrolyzerFluidRecipe.Input> {
    public ElectrolyzerFluidRecipe {
        duration = Math.max(1, duration);
        byproducts = List.copyOf(byproducts);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.fluid() == this.input.fluid() && input.amount() >= this.input.amount();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return FluidIconItem.forFluid(this.output1.fluid());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return FluidIconItem.forFluid(this.output1.fluid());
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
        return HbmRecipeTypes.ELECTROLYZER_FLUID_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.ELECTROLYZER_FLUID.get();
    }

    public ItemStack displayIcon() {
        return FluidIconItem.forFluid(this.input.fluid());
    }

    public record FluidAmount(HbmFluidDefinition fluid, int amount) {
        private static final Codec<FluidAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(0, 1_000_000).fieldOf("amount").forGetter(FluidAmount::amount)
        ).apply(instance, FluidAmount::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidAmount> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidAmount decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidAmount stack) {
                buffer.writeUtf(stack.fluid().name());
                buffer.writeVarInt(stack.amount());
            }
        };

        private static FluidAmount fromName(String fluid, int amount) {
            return new FluidAmount(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
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

    public static class Serializer implements RecipeSerializer<ElectrolyzerFluidRecipe> {
        private static final MapCodec<ElectrolyzerFluidRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ElectrolyzerFluidRecipe::group),
                FluidAmount.CODEC.fieldOf("input").forGetter(ElectrolyzerFluidRecipe::input),
                FluidAmount.CODEC.fieldOf("output1").forGetter(ElectrolyzerFluidRecipe::output1),
                FluidAmount.CODEC.fieldOf("output2").forGetter(ElectrolyzerFluidRecipe::output2),
                Codec.intRange(1, 72000).optionalFieldOf("duration", 20).forGetter(ElectrolyzerFluidRecipe::duration),
                ItemStack.STRICT_CODEC.listOf().optionalFieldOf("byproducts", List.of()).forGetter(ElectrolyzerFluidRecipe::byproducts)
        ).apply(instance, ElectrolyzerFluidRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ElectrolyzerFluidRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ElectrolyzerFluidRecipe::group,
                FluidAmount.STREAM_CODEC,
                ElectrolyzerFluidRecipe::input,
                FluidAmount.STREAM_CODEC,
                ElectrolyzerFluidRecipe::output1,
                FluidAmount.STREAM_CODEC,
                ElectrolyzerFluidRecipe::output2,
                ByteBufCodecs.VAR_INT,
                ElectrolyzerFluidRecipe::duration,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                ElectrolyzerFluidRecipe::byproducts,
                ElectrolyzerFluidRecipe::new
        );

        @Override
        public MapCodec<ElectrolyzerFluidRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ElectrolyzerFluidRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
