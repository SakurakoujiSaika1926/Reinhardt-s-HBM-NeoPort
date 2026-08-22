package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record AssemblyMachineRecipe(
        String group,
        List<String> blueprintPools,
        int duration,
        int power,
        List<CountedIngredient> ingredients,
        List<AssemblyFluidStack> inputFluids,
        List<AssemblyFluidStack> outputFluids,
        ItemStack result
) implements Recipe<AssemblyMachineRecipe.Input> {
    public AssemblyMachineRecipe {
        blueprintPools = List.copyOf(blueprintPools);
        ingredients = List.copyOf(ingredients);
        inputFluids = List.copyOf(inputFluids);
        outputFluids = List.copyOf(outputFluids);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return canCraft(input.stacks(), input.fluids(), this.ingredients, this.inputFluids);
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (CountedIngredient ingredient : this.ingredients) {
            list.add(ingredient.ingredient());
        }
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.ASSEMBLY_MACHINE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.ASSEMBLY_MACHINE.get();
    }

    public static boolean canCraft(
            List<ItemStack> stacks,
            List<HbmFluidStack> fluids,
            List<CountedIngredient> ingredients,
            List<AssemblyFluidStack> inputFluids
    ) {
        if (ingredients.size() > stacks.size() || inputFluids.size() > fluids.size()) {
            return false;
        }

        for (int slot = 0; slot < ingredients.size(); slot++) {
            CountedIngredient ingredient = ingredients.get(slot);
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty() || stack.getCount() < ingredient.count() || !ingredient.ingredient().test(stack)) {
                return false;
            }
        }

        for (int index = 0; index < inputFluids.size(); index++) {
            AssemblyFluidStack required = inputFluids.get(index);
            HbmFluidStack held = fluids.get(index);
            if (held.isEmpty()
                    || held.type() != required.type()
                    || held.pressure() != required.pressure()
                    || held.amount() < required.amount()) {
                return false;
            }
        }
        return true;
    }

    public boolean isVisibleForPool(Optional<String> installedPool) {
        return this.blueprintPools.isEmpty() || installedPool.filter(this.blueprintPools::contains).isPresent();
    }

    /**
     * Resource conditions are evaluated during recipe loading, but older packs may still contain
     * both sides of a 528 pair after a reload. Keep the machine UI and its server-side selection
     * on the same configured variant without collapsing normal material alternatives.
     */
    public static List<RecipeHolder<AssemblyMachineRecipe>> activeVariants(Collection<RecipeHolder<AssemblyMachineRecipe>> recipes) {
        Set<String> expensiveGroups = new HashSet<>();
        for (RecipeHolder<AssemblyMachineRecipe> holder : recipes) {
            if (isExpensiveVariant(holder) && !holder.value().group().isBlank()) {
                expensiveGroups.add(holder.value().group());
            }
        }

        boolean enable528Mode = HbmConfig.ENABLE_528_MODE.get();
        return recipes.stream()
                .filter(holder -> {
                    boolean expensive = isExpensiveVariant(holder);
                    String group = holder.value().group();
                    if (enable528Mode) {
                        return group.isBlank() || !expensiveGroups.contains(group) || expensive;
                    }
                    return !expensive;
                })
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    private static boolean isExpensiveVariant(RecipeHolder<AssemblyMachineRecipe> holder) {
        return holder.id().getPath().endsWith("_expensive");
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        private static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).fieldOf("count").forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CountedIngredient decode(RegistryFriendlyByteBuf buffer) {
                Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int count = buffer.readVarInt();
                return new CountedIngredient(ingredient, count);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CountedIngredient countedIngredient) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, countedIngredient.ingredient());
                buffer.writeVarInt(countedIngredient.count());
            }
        };
    }

    public record AssemblyFluidStack(HbmFluidDefinition type, int amount, int pressure) {
        private static final Codec<AssemblyFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.type().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(AssemblyFluidStack::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(AssemblyFluidStack::pressure)
        ).apply(instance, AssemblyFluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, AssemblyFluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public AssemblyFluidStack decode(RegistryFriendlyByteBuf buffer) {
                HbmFluidDefinition fluid = HbmFluids.byName(buffer.readUtf()).orElse(HbmFluids.none());
                int amount = buffer.readVarInt();
                int pressure = buffer.readVarInt();
                return new AssemblyFluidStack(fluid, amount, pressure);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, AssemblyFluidStack stack) {
                buffer.writeUtf(stack.type().name());
                buffer.writeVarInt(stack.amount());
                buffer.writeVarInt(stack.pressure());
            }
        };

        private static AssemblyFluidStack fromName(String name, int amount, int pressure) {
            HbmFluidDefinition fluid = HbmFluids.byName(name).orElse(HbmFluids.none());
            return new AssemblyFluidStack(fluid, amount, pressure);
        }

        public boolean isEmpty() {
            return this.type.isNone() || this.amount <= 0;
        }
    }

    public record Input(List<ItemStack> stacks, List<HbmFluidStack> fluids) implements RecipeInput {
        public Input(List<ItemStack> stacks) {
            this(stacks, List.of());
        }

        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < this.stacks.size() ? this.stacks.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return this.stacks.size();
        }
    }

    public static class Serializer implements RecipeSerializer<AssemblyMachineRecipe> {
        private static final MapCodec<AssemblyMachineRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(AssemblyMachineRecipe::group),
                Codec.STRING.listOf().optionalFieldOf("blueprint_pools", List.of()).forGetter(AssemblyMachineRecipe::blueprintPools),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(AssemblyMachineRecipe::duration),
                Codec.intRange(1, 1_000_000).fieldOf("power").forGetter(AssemblyMachineRecipe::power),
                CountedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(AssemblyMachineRecipe::ingredients),
                AssemblyFluidStack.CODEC.listOf().optionalFieldOf("input_fluids", List.of()).forGetter(AssemblyMachineRecipe::inputFluids),
                AssemblyFluidStack.CODEC.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(AssemblyMachineRecipe::outputFluids),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AssemblyMachineRecipe::result)
        ).apply(instance, AssemblyMachineRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AssemblyMachineRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public AssemblyMachineRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                List<String> blueprintPools = readStringList(buffer);
                int duration = buffer.readVarInt();
                int power = buffer.readVarInt();
                int ingredientCount = buffer.readVarInt();
                List<CountedIngredient> ingredients = new ArrayList<>(ingredientCount);
                for (int i = 0; i < ingredientCount; i++) {
                    ingredients.add(CountedIngredient.STREAM_CODEC.decode(buffer));
                }
                List<AssemblyFluidStack> inputFluids = readList(buffer, AssemblyFluidStack.STREAM_CODEC);
                List<AssemblyFluidStack> outputFluids = readList(buffer, AssemblyFluidStack.STREAM_CODEC);
                ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                return new AssemblyMachineRecipe(group, blueprintPools, duration, power, List.copyOf(ingredients), inputFluids, outputFluids, result);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, AssemblyMachineRecipe recipe) {
                buffer.writeUtf(recipe.group());
                writeStringList(buffer, recipe.blueprintPools());
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarInt(recipe.power());
                buffer.writeVarInt(recipe.ingredients().size());
                for (CountedIngredient ingredient : recipe.ingredients()) {
                    CountedIngredient.STREAM_CODEC.encode(buffer, ingredient);
                }
                writeList(buffer, recipe.inputFluids(), AssemblyFluidStack.STREAM_CODEC);
                writeList(buffer, recipe.outputFluids(), AssemblyFluidStack.STREAM_CODEC);
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result());
            }

            private <T> List<T> readList(RegistryFriendlyByteBuf buffer, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
                int size = buffer.readVarInt();
                List<T> values = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    values.add(codec.decode(buffer));
                }
                return List.copyOf(values);
            }

            private <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> values, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
                buffer.writeVarInt(values.size());
                for (T value : values) {
                    codec.encode(buffer, value);
                }
            }

            private List<String> readStringList(RegistryFriendlyByteBuf buffer) {
                int size = buffer.readVarInt();
                List<String> values = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    values.add(buffer.readUtf());
                }
                return List.copyOf(values);
            }

            private void writeStringList(RegistryFriendlyByteBuf buffer, List<String> values) {
                buffer.writeVarInt(values.size());
                for (String value : values) {
                    buffer.writeUtf(value);
                }
            }
        };

        @Override
        public MapCodec<AssemblyMachineRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AssemblyMachineRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
