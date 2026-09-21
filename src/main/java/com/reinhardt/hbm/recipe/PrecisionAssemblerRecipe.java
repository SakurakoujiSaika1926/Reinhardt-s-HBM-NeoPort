package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.item.BlueprintItem;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Recipe contract of TileEntityMachinePrecAss.  It deliberately has its own
 * type: nine output slots and a weighted output pool cannot be represented by
 * the ordinary assembly machine's single-result contract.
 */
public record PrecisionAssemblerRecipe(
        String group,
        List<String> blueprintPools,
        int duration,
        int power,
        List<CountedIngredient> ingredients,
        List<FluidStack> inputFluids,
        List<FluidStack> outputFluids,
        List<ChanceOutput> outputs,
        OutputMode outputMode,
        List<Integer> expensiveOutputWeights
) implements Recipe<PrecisionAssemblerRecipe.Input> {
    public PrecisionAssemblerRecipe {
        blueprintPools = List.copyOf(blueprintPools);
        ingredients = List.copyOf(ingredients);
        inputFluids = List.copyOf(inputFluids);
        outputFluids = List.copyOf(outputFluids);
        outputs = List.copyOf(outputs);
        outputMode = outputMode == null ? OutputMode.WEIGHTED : outputMode;
        expensiveOutputWeights = expensiveOutputWeights == null ? List.of() : List.copyOf(expensiveOutputWeights);
        if (ingredients.size() > 9 || inputFluids.size() > 1 || outputFluids.size() > 1 || outputs.isEmpty() || outputs.size() > 9) {
            throw new IllegalArgumentException("Precision assembler recipe exceeds the 1.7.10 machine contract");
        }
        if (!expensiveOutputWeights.isEmpty() && expensiveOutputWeights.size() != outputs.size()) {
            throw new IllegalArgumentException("Precision assembler expensive output weights must match normal output weights");
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (this.ingredients.size() > input.stacks().size() || this.inputFluids.size() > input.fluids().size()) {
            return false;
        }
        for (int slot = 0; slot < this.ingredients.size(); slot++) {
            CountedIngredient required = this.ingredients.get(slot);
            ItemStack stack = input.stacks().get(slot);
            if (stack.isEmpty() || stack.getCount() < required.count() || !required.ingredient().test(stack)) {
                return false;
            }
        }
        for (int tank = 0; tank < this.inputFluids.size(); tank++) {
            FluidStack required = this.inputFluids.get(tank);
            HbmFluidStack held = input.fluids().get(tank);
            if (held.isEmpty() || held.type() != required.type() || held.pressure() != required.pressure() || held.amount() < required.amount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.outputs.getFirst().stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.outputs.getFirst().stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        for (CountedIngredient ingredient : this.ingredients) {
            result.add(ingredient.ingredient());
        }
        return result;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.PRECISION_ASSEMBLER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.PRECISION_ASSEMBLER.get();
    }

    public boolean isVisibleForPool(Optional<String> installedPool) {
        return BlueprintItem.isRecipeVisibleForPool(this.blueprintPools, installedPool);
    }

    /**
     * Ports both 1.7.10 GenericRecipes output contracts. ChanceOutputMulti
     * chooses one output while normal IOutput entries roll independently into
     * their matching output slots.
     */
    public List<ItemStack> rollOutputs(net.minecraft.util.RandomSource random) {
        if (this.outputMode == OutputMode.INDEPENDENT) {
            List<ItemStack> results = new ArrayList<>(this.outputs.size());
            for (int index = 0; index < this.outputs.size(); index++) {
                ChanceOutput output = this.outputs.get(index);
                results.add(random.nextInt(100) < outputWeight(index) ? output.stack().copy() : ItemStack.EMPTY);
            }
            return List.copyOf(results);
        }

        int totalWeight = 0;
        for (ChanceOutput output : this.outputs) {
            totalWeight += output.weight();
        }
        int roll = random.nextInt(totalWeight);
        for (ChanceOutput output : this.outputs) {
            roll -= output.weight();
            if (roll < 0) {
                return List.of(output.stack().copy());
            }
        }
        return List.of(this.outputs.getLast().stack().copy());
    }

    public int outputWeight(int index) {
        if (HbmConfig.ENABLE_EXPENSIVE_MODE.get() && !this.expensiveOutputWeights.isEmpty()) {
            return this.expensiveOutputWeights.get(index);
        }
        return this.outputs.get(index).weight();
    }

    public enum OutputMode {
        WEIGHTED("weighted"),
        INDEPENDENT("independent");

        private static final Codec<OutputMode> CODEC = Codec.STRING.xmap(OutputMode::fromSerializedName, OutputMode::serializedName);

        private final String serializedName;

        OutputMode(String serializedName) {
            this.serializedName = serializedName;
        }

        private String serializedName() {
            return this.serializedName;
        }

        private static OutputMode fromSerializedName(String name) {
            for (OutputMode value : values()) {
                if (value.serializedName.equals(name)) return value;
            }
            throw new IllegalArgumentException("Unknown precision assembler output mode: " + name);
        }

        private static OutputMode fromNetwork(int ordinal) {
            OutputMode[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                throw new IllegalArgumentException("Invalid precision assembler output mode ordinal: " + ordinal);
            }
            return values[ordinal];
        }
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
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.type().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidStack::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(FluidStack::pressure)
        ).apply(instance, FluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidStack decode(RegistryFriendlyByteBuf buffer) {
                return new FluidStack(HbmFluids.byName(buffer.readUtf()).orElse(HbmFluids.none()), buffer.readVarInt(), buffer.readVarInt());
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

    public record Input(List<ItemStack> stacks, List<HbmFluidStack> fluids) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < this.stacks.size() ? this.stacks.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return this.stacks.size();
        }
    }

    public static final class Serializer implements RecipeSerializer<PrecisionAssemblerRecipe> {
        private static final MapCodec<PrecisionAssemblerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(PrecisionAssemblerRecipe::group),
                Codec.STRING.listOf().optionalFieldOf("blueprint_pools", List.of()).forGetter(PrecisionAssemblerRecipe::blueprintPools),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(PrecisionAssemblerRecipe::duration),
                Codec.intRange(1, 1_000_000).fieldOf("power").forGetter(PrecisionAssemblerRecipe::power),
                CountedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(PrecisionAssemblerRecipe::ingredients),
                FluidStack.CODEC.listOf().optionalFieldOf("input_fluids", List.of()).forGetter(PrecisionAssemblerRecipe::inputFluids),
                FluidStack.CODEC.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(PrecisionAssemblerRecipe::outputFluids),
                ChanceOutput.CODEC.listOf().fieldOf("outputs").forGetter(PrecisionAssemblerRecipe::outputs),
                OutputMode.CODEC.optionalFieldOf("output_mode", OutputMode.WEIGHTED).forGetter(PrecisionAssemblerRecipe::outputMode),
                Codec.intRange(1, 100).listOf().optionalFieldOf("expensive_output_weights", List.of()).forGetter(PrecisionAssemblerRecipe::expensiveOutputWeights)
        ).apply(instance, PrecisionAssemblerRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PrecisionAssemblerRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PrecisionAssemblerRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                List<String> pools = readList(buffer, StreamCodec.of((buf, value) -> buf.writeUtf(value), RegistryFriendlyByteBuf::readUtf));
                int duration = buffer.readVarInt();
                int power = buffer.readVarInt();
                List<CountedIngredient> ingredients = readList(buffer, CountedIngredient.STREAM_CODEC);
                List<FluidStack> inputs = readList(buffer, FluidStack.STREAM_CODEC);
                List<FluidStack> outputs = readList(buffer, FluidStack.STREAM_CODEC);
                List<ChanceOutput> chances = readList(buffer, ChanceOutput.STREAM_CODEC);
                OutputMode outputMode = OutputMode.fromNetwork(buffer.readVarInt());
                List<Integer> expensiveWeights = readList(buffer, StreamCodec.of((buf, value) -> buf.writeVarInt(value), RegistryFriendlyByteBuf::readVarInt));
                return new PrecisionAssemblerRecipe(group, pools, duration, power, ingredients, inputs, outputs, chances, outputMode, expensiveWeights);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PrecisionAssemblerRecipe recipe) {
                buffer.writeUtf(recipe.group());
                writeList(buffer, recipe.blueprintPools(), StreamCodec.of((buf, value) -> buf.writeUtf(value), RegistryFriendlyByteBuf::readUtf));
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarInt(recipe.power());
                writeList(buffer, recipe.ingredients(), CountedIngredient.STREAM_CODEC);
                writeList(buffer, recipe.inputFluids(), FluidStack.STREAM_CODEC);
                writeList(buffer, recipe.outputFluids(), FluidStack.STREAM_CODEC);
                writeList(buffer, recipe.outputs(), ChanceOutput.STREAM_CODEC);
                buffer.writeVarInt(recipe.outputMode().ordinal());
                writeList(buffer, recipe.expensiveOutputWeights(), StreamCodec.of((buf, value) -> buf.writeVarInt(value), RegistryFriendlyByteBuf::readVarInt));
            }

            private static <T> List<T> readList(RegistryFriendlyByteBuf buffer, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
                int size = buffer.readVarInt();
                List<T> values = new ArrayList<>(size);
                for (int index = 0; index < size; index++) values.add(codec.decode(buffer));
                return List.copyOf(values);
            }

            private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> values, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
                buffer.writeVarInt(values.size());
                for (T value : values) codec.encode(buffer, value);
            }
        };

        @Override
        public MapCodec<PrecisionAssemblerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PrecisionAssemblerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
