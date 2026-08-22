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

public record GasCentrifugeRecipe(
        String group,
        FluidIngredient input,
        FluidOutput output,
        boolean highSpeed,
        List<ItemStack> results
) implements Recipe<GasCentrifugeRecipe.Input> {
    public GasCentrifugeRecipe {
        results = List.copyOf(results.stream().filter(stack -> !stack.isEmpty()).limit(4).toList());
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.fluid() == this.input.fluid() && input.amount() >= this.input.amount();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.results.isEmpty() ? ItemStack.EMPTY : this.results.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.results.isEmpty() ? displayIcon() : this.results.getFirst();
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
        return HbmRecipeTypes.GAS_CENTRIFUGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.GAS_CENTRIFUGE.get();
    }

    public boolean hasFluidOutput() {
        return this.output != null && !this.output.isEmpty();
    }

    public ItemStack displayIcon() {
        return FluidIconItem.forFluid(this.input.fluid(), this.input.amount(), 0);
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

    public static class Serializer implements RecipeSerializer<GasCentrifugeRecipe> {
        private static final MapCodec<GasCentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(GasCentrifugeRecipe::group),
                FluidIngredient.CODEC.fieldOf("input").forGetter(GasCentrifugeRecipe::input),
                FluidOutput.CODEC.optionalFieldOf("output", FluidOutput.EMPTY).forGetter(GasCentrifugeRecipe::output),
                Codec.BOOL.optionalFieldOf("high_speed", false).forGetter(GasCentrifugeRecipe::highSpeed),
                ItemStack.STRICT_CODEC.listOf(0, 4).fieldOf("results").forGetter(GasCentrifugeRecipe::results)
        ).apply(instance, GasCentrifugeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, GasCentrifugeRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public GasCentrifugeRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                FluidIngredient input = FluidIngredient.STREAM_CODEC.decode(buffer);
                FluidOutput output = FluidOutput.STREAM_CODEC.decode(buffer);
                boolean highSpeed = buffer.readBoolean();
                List<ItemStack> results = ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(4)).decode(buffer);
                return new GasCentrifugeRecipe(group, input, output, highSpeed, results);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, GasCentrifugeRecipe recipe) {
                buffer.writeUtf(recipe.group());
                FluidIngredient.STREAM_CODEC.encode(buffer, recipe.input());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.output());
                buffer.writeBoolean(recipe.highSpeed());
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(4)).encode(buffer, recipe.results());
            }
        };

        @Override
        public MapCodec<GasCentrifugeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GasCentrifugeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
