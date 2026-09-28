package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Exact data contract of the latest 1.7.10 {@code SuperComputerRecipes}. */
public record SupercomputerRecipe(
        String group,
        int duration,
        int power,
        List<CountedIngredient> ingredients,
        List<FluidStack> inputFluids,
        List<FluidStack> outputFluids,
        List<ChanceOutput> outputs
) implements Recipe<SupercomputerRecipe.Input> {
    public static final int ITEM_INPUT_LIMIT = 3;
    public static final int ITEM_OUTPUT_LIMIT = 3;
    public static final int FLUID_LIMIT = 1;

    public SupercomputerRecipe {
        ingredients = List.copyOf(ingredients);
        inputFluids = List.copyOf(inputFluids);
        outputFluids = List.copyOf(outputFluids);
        outputs = List.copyOf(outputs);
        if (ingredients.size() > ITEM_INPUT_LIMIT
                || inputFluids.size() > FLUID_LIMIT
                || outputFluids.size() > FLUID_LIMIT
                || outputs.isEmpty()
                || outputs.size() > ITEM_OUTPUT_LIMIT) {
            throw new IllegalArgumentException("Supercomputer recipe exceeds the 1.7.10 machine contract");
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (ingredients.size() > input.items().size() || inputFluids.size() > input.fluids().size()) {
            return false;
        }
        for (int index = 0; index < ingredients.size(); index++) {
            CountedIngredient required = ingredients.get(index);
            ItemStack held = input.items().get(index);
            if (held.isEmpty() || held.getCount() < required.count() || !required.ingredient().test(held)) {
                return false;
            }
        }
        for (int index = 0; index < inputFluids.size(); index++) {
            FluidStack required = inputFluids.get(index);
            HbmFluidStack held = input.fluids().get(index);
            if (held.isEmpty()
                    || held.type() != required.type()
                    || held.pressure() != required.pressure()
                    || held.amount() < required.amount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return outputs.getFirst().stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs.getFirst().stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (CountedIngredient input : ingredients) {
            list.add(input.ingredient());
        }
        return list;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.SUPERCOMPUTER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.SUPERCOMPUTER.get();
    }

    public ItemStack displayIcon() {
        return outputs.getFirst().stack().copy();
    }

    /** Legacy ChanceOutputMulti: exactly one weighted result is selected per completed cycle. */
    public ItemStack rollOutput(RandomSource random) {
        int totalWeight = 0;
        for (ChanceOutput output : outputs) {
            totalWeight = Math.addExact(totalWeight, output.weight());
        }
        int roll = random.nextInt(totalWeight);
        for (ChanceOutput output : outputs) {
            roll -= output.weight();
            if (roll < 0) {
                return output.stack().copy();
            }
        }
        throw new IllegalStateException("Supercomputer output pool contained no selectable entry");
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        private static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).fieldOf("count").forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CountedIngredient decode(RegistryFriendlyByteBuf buffer) {
                return new CountedIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CountedIngredient value) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, value.ingredient());
                buffer.writeVarInt(value.count());
            }
        };
    }

    public record FluidStack(HbmFluidDefinition type, int amount, int pressure) {
        private static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(value -> value.type().name()),
                Codec.intRange(1, 2_000_000).fieldOf("amount").forGetter(FluidStack::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(FluidStack::pressure)
        ).apply(instance, FluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidStack decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidStack value) {
                buffer.writeUtf(value.type().name());
                buffer.writeVarInt(value.amount());
                buffer.writeVarInt(value.pressure());
            }
        };

        private static FluidStack fromName(String name, int amount, int pressure) {
            return new FluidStack(HbmFluids.byName(name).orElse(HbmFluids.none()), amount, pressure);
        }
    }

    public record ChanceOutput(ItemStack stack, int weight) {
        private static final Codec<ChanceOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ChanceOutput::stack),
                Codec.intRange(1, 100_000).fieldOf("weight").forGetter(ChanceOutput::weight)
        ).apply(instance, ChanceOutput::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ChanceOutput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ChanceOutput decode(RegistryFriendlyByteBuf buffer) {
                return new ChanceOutput(ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ChanceOutput value) {
                ItemStack.STREAM_CODEC.encode(buffer, value.stack());
                buffer.writeVarInt(value.weight());
            }
        };
    }

    public record Input(List<ItemStack> items, List<HbmFluidStack> fluids) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static final class Serializer implements RecipeSerializer<SupercomputerRecipe> {
        private static final MapCodec<SupercomputerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(SupercomputerRecipe::group),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(SupercomputerRecipe::duration),
                Codec.intRange(1, 100_000_000).fieldOf("power").forGetter(SupercomputerRecipe::power),
                CountedIngredient.CODEC.listOf(0, ITEM_INPUT_LIMIT).optionalFieldOf("ingredients", List.of()).forGetter(SupercomputerRecipe::ingredients),
                FluidStack.CODEC.listOf(0, FLUID_LIMIT).optionalFieldOf("input_fluids", List.of()).forGetter(SupercomputerRecipe::inputFluids),
                FluidStack.CODEC.listOf(0, FLUID_LIMIT).optionalFieldOf("output_fluids", List.of()).forGetter(SupercomputerRecipe::outputFluids),
                ChanceOutput.CODEC.listOf(1, ITEM_OUTPUT_LIMIT).fieldOf("outputs").forGetter(SupercomputerRecipe::outputs)
        ).apply(instance, SupercomputerRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SupercomputerRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SupercomputerRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new SupercomputerRecipe(
                        buffer.readUtf(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        readList(buffer, CountedIngredient.STREAM_CODEC),
                        readList(buffer, FluidStack.STREAM_CODEC),
                        readList(buffer, FluidStack.STREAM_CODEC),
                        readList(buffer, ChanceOutput.STREAM_CODEC));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, SupercomputerRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarInt(recipe.power());
                writeList(buffer, recipe.ingredients(), CountedIngredient.STREAM_CODEC);
                writeList(buffer, recipe.inputFluids(), FluidStack.STREAM_CODEC);
                writeList(buffer, recipe.outputFluids(), FluidStack.STREAM_CODEC);
                writeList(buffer, recipe.outputs(), ChanceOutput.STREAM_CODEC);
            }
        };

        @Override
        public MapCodec<SupercomputerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SupercomputerRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static <T> List<T> readList(RegistryFriendlyByteBuf buffer, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            int size = buffer.readVarInt();
            List<T> values = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                values.add(codec.decode(buffer));
            }
            return List.copyOf(values);
        }

        private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> values,
                                           StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            buffer.writeVarInt(values.size());
            for (T value : values) {
                codec.encode(buffer, value);
            }
        }
    }
}
