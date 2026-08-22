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

public record HydrotreatingRecipe(
        String group,
        FluidIngredient input,
        FluidIngredient hydrogen,
        FluidOutput output1,
        FluidOutput output2,
        int power
) implements Recipe<HydrotreatingRecipe.Input> {
    public static final int DEFAULT_POWER = 20_000;

    @Override
    public boolean matches(Input input, Level level) {
        return input.input().type() == this.input.fluid()
                && input.input().amount() >= this.input.amount()
                && input.hydrogen().type() == this.hydrogen.fluid()
                && input.hydrogen().amount() >= this.hydrogen.amount()
                && input.hydrogen().pressure() == this.hydrogen.pressure();
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
        return HbmRecipeTypes.HYDROTREATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.HYDROTREATING.get();
    }

    public record FluidIngredient(HbmFluidDefinition fluid, int amount, int pressure) {
        private static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(value -> value.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidIngredient::amount),
                Codec.intRange(0, 100).optionalFieldOf("pressure", 0).forGetter(FluidIngredient::pressure)
        ).apply(instance, FluidIngredient::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidIngredient decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidIngredient value) {
                buffer.writeUtf(value.fluid().name());
                buffer.writeVarInt(value.amount());
                buffer.writeVarInt(value.pressure());
            }
        };

        private static FluidIngredient fromName(String name, int amount, int pressure) {
            return new FluidIngredient(HbmFluids.byName(name).orElse(HbmFluids.none()), amount, pressure);
        }
    }

    public record FluidOutput(HbmFluidDefinition fluid, int amount) {
        private static final Codec<FluidOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(value -> value.fluid().name()),
                Codec.intRange(0, 1_000_000).fieldOf("amount").forGetter(FluidOutput::amount)
        ).apply(instance, FluidOutput::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidOutput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidOutput decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidOutput value) {
                buffer.writeUtf(value.fluid().name());
                buffer.writeVarInt(value.amount());
            }
        };

        public boolean isEmpty() {
            return this.fluid.isNone() || this.amount <= 0;
        }

        private static FluidOutput fromName(String name, int amount) {
            return new FluidOutput(HbmFluids.byName(name).orElse(HbmFluids.none()), amount);
        }
    }

    public record Input(com.reinhardt.hbm.fluid.HbmFluidStack input, com.reinhardt.hbm.fluid.HbmFluidStack hydrogen)
            implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class Serializer implements RecipeSerializer<HydrotreatingRecipe> {
        private static final MapCodec<HydrotreatingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(HydrotreatingRecipe::group),
                FluidIngredient.CODEC.fieldOf("input").forGetter(HydrotreatingRecipe::input),
                FluidIngredient.CODEC.fieldOf("hydrogen").forGetter(HydrotreatingRecipe::hydrogen),
                FluidOutput.CODEC.fieldOf("output1").forGetter(HydrotreatingRecipe::output1),
                FluidOutput.CODEC.fieldOf("output2").forGetter(HydrotreatingRecipe::output2),
                Codec.intRange(1, 1_000_000_000).optionalFieldOf("power", DEFAULT_POWER).forGetter(HydrotreatingRecipe::power)
        ).apply(instance, HydrotreatingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, HydrotreatingRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public HydrotreatingRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new HydrotreatingRecipe(
                        buffer.readUtf(),
                        FluidIngredient.STREAM_CODEC.decode(buffer),
                        FluidIngredient.STREAM_CODEC.decode(buffer),
                        FluidOutput.STREAM_CODEC.decode(buffer),
                        FluidOutput.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, HydrotreatingRecipe recipe) {
                buffer.writeUtf(recipe.group());
                FluidIngredient.STREAM_CODEC.encode(buffer, recipe.input());
                FluidIngredient.STREAM_CODEC.encode(buffer, recipe.hydrogen());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.output1());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.output2());
                buffer.writeVarInt(recipe.power());
            }
        };

        @Override
        public MapCodec<HydrotreatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HydrotreatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
