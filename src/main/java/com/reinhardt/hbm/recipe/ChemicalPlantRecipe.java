package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
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

public record ChemicalPlantRecipe(
        String group,
        List<String> blueprintPools,
        int duration,
        int power,
        ItemStack icon,
        List<CountedIngredient> inputItems,
        List<ChemicalFluidStack> inputFluids,
        List<ItemStack> outputItems,
        List<ChemicalFluidStack> outputFluids
) implements Recipe<ChemicalPlantRecipe.Input> {
    public static final int ITEM_LIMIT = 3;
    public static final int FLUID_LIMIT = 3;

    public ChemicalPlantRecipe {
        blueprintPools = List.copyOf(blueprintPools);
        inputItems = List.copyOf(inputItems);
        inputFluids = List.copyOf(inputFluids);
        outputItems = List.copyOf(outputItems);
        outputFluids = List.copyOf(outputFluids);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return canCraft(input.items(), input.fluids(), this.inputItems, this.inputFluids);
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.outputItems.isEmpty() ? ItemStack.EMPTY : this.outputItems.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.outputItems.isEmpty() ? ItemStack.EMPTY : this.outputItems.getFirst();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (CountedIngredient ingredient : this.inputItems) {
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
        return HbmRecipeTypes.CHEMICAL_PLANT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.CHEMICAL_PLANT.get();
    }

    public ItemStack displayIcon() {
        if (!this.icon.isEmpty()) {
            return this.icon.copy();
        }
        if (!this.outputItems.isEmpty() && !this.outputItems.getFirst().isEmpty()) {
            return this.outputItems.getFirst().copy();
        }
        if (!this.outputFluids.isEmpty()) {
            return FluidIconItem.forFluid(this.outputFluids.getFirst().type());
        }
        if (!this.inputFluids.isEmpty()) {
            return FluidIconItem.forFluid(this.inputFluids.getFirst().type());
        }
        return new ItemStack(HbmItems.TEMPLATE_FOLDER.get());
    }

    public boolean isVisibleForPool(Optional<String> installedPool) {
        return this.blueprintPools.isEmpty() || installedPool.filter(this.blueprintPools::contains).isPresent();
    }

    public static boolean canCraft(
            List<ItemStack> itemStacks,
            List<HbmFluidStack> fluidStacks,
            List<CountedIngredient> itemInputs,
            List<ChemicalFluidStack> fluidInputs
    ) {
        if (itemInputs.size() > itemStacks.size() || fluidInputs.size() > fluidStacks.size()) {
            return false;
        }

        for (int slot = 0; slot < itemInputs.size(); slot++) {
            CountedIngredient ingredient = itemInputs.get(slot);
            ItemStack stack = itemStacks.get(slot);
            if (stack.isEmpty() || stack.getCount() < ingredient.count() || !ingredient.ingredient().test(stack)) {
                return false;
            }
        }

        for (int index = 0; index < fluidInputs.size(); index++) {
            ChemicalFluidStack required = fluidInputs.get(index);
            HbmFluidStack held = fluidStacks.get(index);
            if (held.isEmpty()
                    || held.type() != required.type()
                    || held.pressure() != required.pressure()
                    || held.amount() < required.amount()) {
                return false;
            }
        }
        return true;
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

    public record ChemicalFluidStack(HbmFluidDefinition type, int amount, int pressure) {
        private static final Codec<ChemicalFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.type().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(ChemicalFluidStack::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(ChemicalFluidStack::pressure)
        ).apply(instance, ChemicalFluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, ChemicalFluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ChemicalFluidStack decode(RegistryFriendlyByteBuf buffer) {
                HbmFluidDefinition fluid = HbmFluids.byName(buffer.readUtf()).orElse(HbmFluids.none());
                int amount = buffer.readVarInt();
                int pressure = buffer.readVarInt();
                return new ChemicalFluidStack(fluid, amount, pressure);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ChemicalFluidStack stack) {
                buffer.writeUtf(stack.type().name());
                buffer.writeVarInt(stack.amount());
                buffer.writeVarInt(stack.pressure());
            }
        };

        private static ChemicalFluidStack fromName(String name, int amount, int pressure) {
            HbmFluidDefinition fluid = HbmFluids.byName(name).orElse(HbmFluids.none());
            return new ChemicalFluidStack(fluid, amount, pressure);
        }

        public boolean isEmpty() {
            return this.type.isNone() || this.amount <= 0;
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

    public static class Serializer implements RecipeSerializer<ChemicalPlantRecipe> {
        private static final MapCodec<ChemicalPlantRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ChemicalPlantRecipe::group),
                Codec.STRING.listOf().optionalFieldOf("blueprint_pools", List.of()).forGetter(ChemicalPlantRecipe::blueprintPools),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(ChemicalPlantRecipe::duration),
                Codec.intRange(1, 1_000_000).fieldOf("power").forGetter(ChemicalPlantRecipe::power),
                ItemStack.STRICT_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(ChemicalPlantRecipe::icon),
                CountedIngredient.CODEC.listOf().optionalFieldOf("input_items", List.of()).forGetter(ChemicalPlantRecipe::inputItems),
                ChemicalFluidStack.CODEC.listOf().optionalFieldOf("input_fluids", List.of()).forGetter(ChemicalPlantRecipe::inputFluids),
                ItemStack.STRICT_CODEC.listOf().optionalFieldOf("output_items", List.of()).forGetter(ChemicalPlantRecipe::outputItems),
                ChemicalFluidStack.CODEC.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(ChemicalPlantRecipe::outputFluids)
        ).apply(instance, ChemicalPlantRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ChemicalPlantRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ChemicalPlantRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                List<String> blueprintPools = readStringList(buffer);
                int duration = buffer.readVarInt();
                int power = buffer.readVarInt();
                ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                List<CountedIngredient> inputItems = readList(buffer, CountedIngredient.STREAM_CODEC);
                List<ChemicalFluidStack> inputFluids = readList(buffer, ChemicalFluidStack.STREAM_CODEC);
                List<ItemStack> outputItems = readList(buffer, ItemStack.STREAM_CODEC);
                List<ChemicalFluidStack> outputFluids = readList(buffer, ChemicalFluidStack.STREAM_CODEC);
                return new ChemicalPlantRecipe(group, blueprintPools, duration, power, icon, inputItems, inputFluids, outputItems, outputFluids);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ChemicalPlantRecipe recipe) {
                buffer.writeUtf(recipe.group());
                writeStringList(buffer, recipe.blueprintPools());
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarInt(recipe.power());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.icon());
                writeList(buffer, recipe.inputItems(), CountedIngredient.STREAM_CODEC);
                writeList(buffer, recipe.inputFluids(), ChemicalFluidStack.STREAM_CODEC);
                writeList(buffer, recipe.outputItems(), ItemStack.STREAM_CODEC);
                writeList(buffer, recipe.outputFluids(), ChemicalFluidStack.STREAM_CODEC);
            }
        };

        @Override
        public MapCodec<ChemicalPlantRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChemicalPlantRecipe> streamCodec() {
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

        private static List<String> readStringList(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<String> values = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                values.add(buffer.readUtf());
            }
            return List.copyOf(values);
        }

        private static void writeStringList(RegistryFriendlyByteBuf buffer, List<String> values) {
            buffer.writeVarInt(values.size());
            for (String value : values) {
                buffer.writeUtf(value);
            }
        }
    }

    public static Optional<ChemicalPlantRecipe> recipeValue(Optional<? extends net.minecraft.world.item.crafting.RecipeHolder<ChemicalPlantRecipe>> holder) {
        return holder.map(net.minecraft.world.item.crafting.RecipeHolder::value);
    }
}
