package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
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
import java.util.Optional;

/**
 * Datapack representation of the 1.7.10 rock mill contract.
 *
 * <p>The {@code outputs} list is one weighted pool. Exactly one entry is
 * selected when a cycle completes; entries are not independent chance rolls.</p>
 */
public record RockMillRecipe(
        String group,
        Optional<String> autoSwitchGroup,
        CountedIngredient input,
        Optional<FluidStack> inputFluid,
        Optional<FluidStack> outputFluid,
        List<WeightedOutput> outputs,
        int duration,
        long power
) implements Recipe<RockMillRecipe.Input> {
    public RockMillRecipe {
        group = group == null ? "" : group;
        autoSwitchGroup = autoSwitchGroup == null ? Optional.empty() : autoSwitchGroup.filter(value -> !value.isBlank());
        inputFluid = inputFluid == null ? Optional.empty() : inputFluid;
        outputFluid = outputFluid == null ? Optional.empty() : outputFluid;
        outputs = List.copyOf(outputs);
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("Rock mill recipes require at least one weighted output");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Rock mill recipe duration must be positive");
        }
        if (power <= 0L || power > Long.MAX_VALUE / 100L) {
            throw new IllegalArgumentException("Rock mill recipe power must be positive and safely scalable");
        }
        long totalWeight = 0L;
        for (WeightedOutput output : outputs) {
            if (output.stack().isEmpty() || output.weight() <= 0) {
                throw new IllegalArgumentException("Rock mill weighted outputs require a non-empty stack and positive weight");
            }
            totalWeight += output.weight();
            if (totalWeight > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Rock mill output weight total exceeds the random selection range");
            }
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input.item().isEmpty()
                || input.item().getCount() < this.input.count()
                || !this.input.ingredient().test(input.item())) {
            return false;
        }
        if (this.inputFluid.isEmpty()) {
            return true;
        }
        FluidStack required = this.inputFluid.get();
        return input.fluid() == required.type() && input.fluidAmount() >= required.amount();
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
        return this.outputs.getFirst().stack().copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.input.ingredient());
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.ROCK_MILL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.ROCK_MILL.get();
    }

    public ItemStack displayIcon() {
        ItemStack[] candidates = this.input.ingredient().getItems();
        if (candidates.length == 0) {
            return new ItemStack(HbmItems.TEMPLATE_FOLDER.get());
        }
        ItemStack icon = candidates[0].copy();
        icon.setCount(this.input.count());
        return icon;
    }

    public boolean canAutoSwitchFrom(ItemStack stack) {
        return !stack.isEmpty() && this.input.ingredient().test(stack);
    }

    public ItemStack selectOutput(RandomSource random) {
        int totalWeight = 0;
        for (WeightedOutput output : this.outputs) {
            totalWeight = Math.addExact(totalWeight, output.weight());
        }
        int selected = random.nextInt(totalWeight);
        for (WeightedOutput output : this.outputs) {
            selected -= output.weight();
            if (selected < 0) {
                return output.stack().copy();
            }
        }
        throw new IllegalStateException("Weighted rock mill output pool did not select an entry");
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
            public void encode(RegistryFriendlyByteBuf buffer, CountedIngredient ingredient) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.ingredient());
                buffer.writeVarInt(ingredient.count());
            }
        };
    }

    public record FluidStack(HbmFluidDefinition type, int amount) {
        private static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.type().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidStack::amount)
        ).apply(instance, FluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidStack decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidStack stack) {
                buffer.writeUtf(stack.type().name());
                buffer.writeVarInt(stack.amount());
            }
        };

        private static FluidStack fromName(String name, int amount) {
            HbmFluidDefinition definition = HbmFluids.byName(name)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown HBM fluid in rock mill recipe: " + name));
            if (definition.isNone()) {
                throw new IllegalArgumentException("Rock mill recipes cannot use the NONE fluid");
            }
            return new FluidStack(definition, amount);
        }
    }

    public record WeightedOutput(ItemStack stack, int weight) {
        private static final Codec<WeightedOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("stack").forGetter(WeightedOutput::stack),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("weight").forGetter(WeightedOutput::weight)
        ).apply(instance, WeightedOutput::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, WeightedOutput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public WeightedOutput decode(RegistryFriendlyByteBuf buffer) {
                return new WeightedOutput(ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, WeightedOutput output) {
                ItemStack.STREAM_CODEC.encode(buffer, output.stack());
                buffer.writeVarInt(output.weight());
            }
        };
    }

    public record Input(ItemStack item, HbmFluidDefinition fluid, int fluidAmount) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.item : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static final class Serializer implements RecipeSerializer<RockMillRecipe> {
        private static final MapCodec<RockMillRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(RockMillRecipe::group),
                Codec.STRING.optionalFieldOf("auto_switch_group").forGetter(RockMillRecipe::autoSwitchGroup),
                CountedIngredient.CODEC.fieldOf("input").forGetter(RockMillRecipe::input),
                FluidStack.CODEC.optionalFieldOf("input_fluid").forGetter(RockMillRecipe::inputFluid),
                FluidStack.CODEC.optionalFieldOf("output_fluid").forGetter(RockMillRecipe::outputFluid),
                WeightedOutput.CODEC.listOf().fieldOf("outputs").forGetter(RockMillRecipe::outputs),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(RockMillRecipe::duration),
                Codec.LONG.fieldOf("power").forGetter(RockMillRecipe::power)
        ).apply(instance, RockMillRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, RockMillRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public RockMillRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                Optional<String> autoSwitchGroup = buffer.readBoolean() ? Optional.of(buffer.readUtf()) : Optional.empty();
                CountedIngredient input = CountedIngredient.STREAM_CODEC.decode(buffer);
                Optional<FluidStack> inputFluid = buffer.readBoolean()
                        ? Optional.of(FluidStack.STREAM_CODEC.decode(buffer)) : Optional.empty();
                Optional<FluidStack> outputFluid = buffer.readBoolean()
                        ? Optional.of(FluidStack.STREAM_CODEC.decode(buffer)) : Optional.empty();
                int outputCount = buffer.readVarInt();
                List<WeightedOutput> outputs = new ArrayList<>(outputCount);
                for (int index = 0; index < outputCount; index++) {
                    outputs.add(WeightedOutput.STREAM_CODEC.decode(buffer));
                }
                return new RockMillRecipe(group, autoSwitchGroup, input, inputFluid, outputFluid,
                        outputs, buffer.readVarInt(), buffer.readVarLong());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, RockMillRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeBoolean(recipe.autoSwitchGroup().isPresent());
                recipe.autoSwitchGroup().ifPresent(buffer::writeUtf);
                CountedIngredient.STREAM_CODEC.encode(buffer, recipe.input());
                buffer.writeBoolean(recipe.inputFluid().isPresent());
                recipe.inputFluid().ifPresent(stack -> FluidStack.STREAM_CODEC.encode(buffer, stack));
                buffer.writeBoolean(recipe.outputFluid().isPresent());
                recipe.outputFluid().ifPresent(stack -> FluidStack.STREAM_CODEC.encode(buffer, stack));
                buffer.writeVarInt(recipe.outputs().size());
                for (WeightedOutput output : recipe.outputs()) {
                    WeightedOutput.STREAM_CODEC.encode(buffer, output);
                }
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarLong(recipe.power());
            }
        };

        @Override
        public MapCodec<RockMillRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RockMillRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
