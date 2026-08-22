package com.reinhardt.hbm.recipe;

import com.mojang.datafixers.util.Either;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public record PurexRecipe(
        String group,
        int duration,
        int power,
        List<CountedIngredient> inputItems,
        List<PurexFluidStack> inputFluids,
        List<PurexItemOutput> outputItems,
        List<PurexFluidStack> outputFluids
) implements Recipe<PurexRecipe.Input> {
    public PurexRecipe {
        inputItems = List.copyOf(inputItems);
        inputFluids = List.copyOf(inputFluids);
        outputItems = List.copyOf(outputItems);
        outputFluids = List.copyOf(outputFluids);
        if (inputItems.size() > 3 || inputFluids.size() > 3 || outputItems.size() > 6 || outputFluids.size() > 1) {
            throw new IllegalArgumentException("PUREX recipe exceeds the 1.7.10 machine limits");
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (this.inputItems.size() > input.items().size()) {
            return false;
        }
        for (int index = 0; index < this.inputItems.size(); index++) {
            CountedIngredient required = this.inputItems.get(index);
            ItemStack held = input.items().get(index);
            if (held.isEmpty() || held.getCount() < required.count() || !required.ingredient().test(held)) {
                return false;
            }
        }
        if (this.inputFluids.size() > input.fluids().size()) {
            return false;
        }
        for (int index = 0; index < this.inputFluids.size(); index++) {
            PurexFluidStack required = this.inputFluids.get(index);
            HbmFluidStack held = input.fluids().get(index);
            if (held.isEmpty() || held.type() != required.type() || held.pressure() != required.pressure() || held.amount() < required.amount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.outputItems.isEmpty() ? ItemStack.EMPTY : this.outputItems.getFirst().stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.outputItems.isEmpty() ? ItemStack.EMPTY : this.outputItems.getFirst().stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (CountedIngredient ingredient : this.inputItems) {
            ingredients.add(ingredient.ingredient());
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.PUREX_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.PUREX.get();
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        private static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CountedIngredient decode(RegistryFriendlyByteBuf buffer) {
                return new CountedIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CountedIngredient ingredient) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.ingredient());
                buffer.writeVarInt(ingredient.count());
            }
        };
    }

    public record PurexFluidStack(HbmFluidDefinition type, int amount, int pressure) {
        private static final Codec<PurexFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.type().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(PurexFluidStack::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(PurexFluidStack::pressure)
        ).apply(instance, PurexFluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, PurexFluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PurexFluidStack decode(RegistryFriendlyByteBuf buffer) {
                return new PurexFluidStack(HbmFluids.byName(buffer.readUtf()).orElse(HbmFluids.none()), buffer.readVarInt(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PurexFluidStack stack) {
                buffer.writeUtf(stack.type().name());
                buffer.writeVarInt(stack.amount());
                buffer.writeVarInt(stack.pressure());
            }
        };

        private static PurexFluidStack fromName(String name, int amount, int pressure) {
            return new PurexFluidStack(HbmFluids.byName(name).orElse(HbmFluids.none()), amount, pressure);
        }
    }

    /** A 1.7.10 ChanceOutput: chance is rolled independently for every item in the stack. */
    public record PurexItemOutput(ItemStack stack, float chance) {
        private static final Codec<PurexItemOutput> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("stack").forGetter(PurexItemOutput::stack),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(PurexItemOutput::chance)
        ).apply(instance, PurexItemOutput::new));
        private static final Codec<PurexItemOutput> CODEC = Codec.either(ItemStack.STRICT_CODEC, FULL_CODEC)
                .xmap(value -> value.map(stack -> new PurexItemOutput(stack, 1.0F), output -> output),
                        output -> output.chance >= 1.0F ? Either.left(output.stack) : Either.right(output));
        private static final StreamCodec<RegistryFriendlyByteBuf, PurexItemOutput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PurexItemOutput decode(RegistryFriendlyByteBuf buffer) {
                return new PurexItemOutput(ItemStack.STREAM_CODEC.decode(buffer), buffer.readFloat());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PurexItemOutput output) {
                ItemStack.STREAM_CODEC.encode(buffer, output.stack);
                buffer.writeFloat(output.chance);
            }
        };

        public PurexItemOutput {
            stack = stack.copy();
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("PUREX output may not be empty");
            }
        }

        public ItemStack roll(RandomSource random) {
            if (this.chance >= 1.0F) {
                return this.stack.copy();
            }
            int count = 0;
            for (int index = 0; index < this.stack.getCount(); index++) {
                if (random.nextFloat() <= this.chance) {
                    count++;
                }
            }
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack result = this.stack.copy();
            result.setCount(count);
            return result;
        }
    }

    public record Input(List<ItemStack> items, List<HbmFluidStack> fluids) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < this.items.size() ? this.items.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return this.items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<PurexRecipe> {
        private static final MapCodec<PurexRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(PurexRecipe::group),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(PurexRecipe::duration),
                Codec.intRange(1, 1_000_000).fieldOf("power").forGetter(PurexRecipe::power),
                CountedIngredient.CODEC.listOf().fieldOf("input_items").forGetter(PurexRecipe::inputItems),
                PurexFluidStack.CODEC.listOf().optionalFieldOf("input_fluids", List.of()).forGetter(PurexRecipe::inputFluids),
                PurexItemOutput.CODEC.listOf().fieldOf("output_items").forGetter(PurexRecipe::outputItems),
                PurexFluidStack.CODEC.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(PurexRecipe::outputFluids)
        ).apply(instance, PurexRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PurexRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PurexRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                int duration = buffer.readVarInt();
                int power = buffer.readVarInt();
                List<CountedIngredient> inputItems = readList(buffer, CountedIngredient.STREAM_CODEC);
                List<PurexFluidStack> inputFluids = readList(buffer, PurexFluidStack.STREAM_CODEC);
                List<PurexItemOutput> outputItems = readList(buffer, PurexItemOutput.STREAM_CODEC);
                List<PurexFluidStack> outputFluids = readList(buffer, PurexFluidStack.STREAM_CODEC);
                return new PurexRecipe(group, duration, power, inputItems, inputFluids, outputItems, outputFluids);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PurexRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarInt(recipe.power());
                writeList(buffer, recipe.inputItems(), CountedIngredient.STREAM_CODEC);
                writeList(buffer, recipe.inputFluids(), PurexFluidStack.STREAM_CODEC);
                writeList(buffer, recipe.outputItems(), PurexItemOutput.STREAM_CODEC);
                writeList(buffer, recipe.outputFluids(), PurexFluidStack.STREAM_CODEC);
            }
        };

        @Override
        public MapCodec<PurexRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PurexRecipe> streamCodec() {
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

        private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> values, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            buffer.writeVarInt(values.size());
            for (T value : values) {
                codec.encode(buffer, value);
            }
        }
    }
}
