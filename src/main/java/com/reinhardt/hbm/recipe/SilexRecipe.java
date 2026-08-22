package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.Wavelength;
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
import java.util.Optional;

public record SilexRecipe(
        String group,
        Optional<Ingredient> ingredient,
        Optional<String> fluidInput,
        int fluidProduced,
        int fluidConsumed,
        Wavelength wavelength,
        List<WeightedOutput> outputs
) implements Recipe<SilexRecipe.Input> {
    public SilexRecipe {
        ingredient = ingredient == null ? Optional.empty() : ingredient;
        fluidInput = fluidInput == null ? Optional.empty() : fluidInput.map(name -> name.toLowerCase(java.util.Locale.ROOT));
        outputs = List.copyOf(outputs);
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input.stack().isEmpty()) {
            return false;
        }
        if (this.ingredient.isPresent() && this.ingredient.get().test(input.stack())) {
            return true;
        }
        if (this.fluidInput.isEmpty()) {
            return false;
        }
        return FluidIconItem.fluid(input.stack())
                .map(fluid -> fluid.name().equals(this.fluidInput.get()))
                .orElse(false);
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.outputs.isEmpty() ? ItemStack.EMPTY : this.outputs.getFirst().stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.outputs.isEmpty() ? ItemStack.EMPTY : this.outputs.getFirst().stack().copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        this.ingredient.ifPresent(ingredients::add);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.SILEX_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.SILEX.get();
    }

    public boolean isFluidRecipe() {
        return this.fluidInput.isPresent();
    }

    public boolean acceptsFluid(HbmFluidDefinition fluid) {
        return fluid != null && this.fluidInput.filter(name -> name.equals(fluid.name())).isPresent();
    }

    public int totalWeight() {
        int total = 0;
        for (WeightedOutput output : this.outputs) {
            total += Math.max(0, output.weight());
        }
        return total;
    }

    public ItemStack deterministicOutput(int recipeIndex) {
        int total = totalWeight();
        if (total <= 0 || this.outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = Math.floorMod(recipeIndex, total);
        int weight = 0;
        for (WeightedOutput output : this.outputs) {
            weight += Math.max(0, output.weight());
            if (index < weight) {
                return output.stack().copy();
            }
        }
        return this.outputs.getLast().stack().copy();
    }

    public record Input(ItemStack stack) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.stack : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public record WeightedOutput(ItemStack stack, int weight) {
        private static final Codec<WeightedOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("item").forGetter(WeightedOutput::stack),
                Codec.intRange(1, 1_000_000).fieldOf("weight").forGetter(WeightedOutput::weight)
        ).apply(instance, WeightedOutput::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, WeightedOutput> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC,
                WeightedOutput::stack,
                ByteBufCodecs.VAR_INT,
                WeightedOutput::weight,
                WeightedOutput::new
        );
    }

    public static class Serializer implements RecipeSerializer<SilexRecipe> {
        private static final Codec<Wavelength> WAVELENGTH_CODEC = Codec.STRING.xmap(Wavelength::byName, Wavelength::getSerializedName);
        private static final MapCodec<SilexRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(SilexRecipe::group),
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient").forGetter(SilexRecipe::ingredient),
                Codec.STRING.optionalFieldOf("fluid_input").forGetter(SilexRecipe::fluidInput),
                Codec.intRange(0, 1_000_000).fieldOf("fluid_produced").forGetter(SilexRecipe::fluidProduced),
                Codec.intRange(1, 1_000_000).fieldOf("fluid_consumed").forGetter(SilexRecipe::fluidConsumed),
                WAVELENGTH_CODEC.fieldOf("wavelength").forGetter(SilexRecipe::wavelength),
                WeightedOutput.CODEC.listOf(1, 64).fieldOf("outputs").forGetter(SilexRecipe::outputs)
        ).apply(instance, SilexRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SilexRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SilexRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                Optional<Ingredient> ingredient = buffer.readBoolean()
                        ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer))
                        : Optional.empty();
                Optional<String> fluidInput = buffer.readBoolean() ? Optional.of(buffer.readUtf()) : Optional.empty();
                int fluidProduced = buffer.readVarInt();
                int fluidConsumed = buffer.readVarInt();
                Wavelength wavelength = Wavelength.byName(buffer.readUtf());
                int count = buffer.readVarInt();
                java.util.ArrayList<WeightedOutput> outputs = new java.util.ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    outputs.add(WeightedOutput.STREAM_CODEC.decode(buffer));
                }
                return new SilexRecipe(group, ingredient, fluidInput, fluidProduced, fluidConsumed, wavelength, outputs);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, SilexRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeBoolean(recipe.ingredient().isPresent());
                recipe.ingredient().ifPresent(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient));
                buffer.writeBoolean(recipe.fluidInput().isPresent());
                recipe.fluidInput().ifPresent(buffer::writeUtf);
                buffer.writeVarInt(recipe.fluidProduced());
                buffer.writeVarInt(recipe.fluidConsumed());
                buffer.writeUtf(recipe.wavelength().getSerializedName());
                buffer.writeVarInt(recipe.outputs().size());
                for (WeightedOutput output : recipe.outputs()) {
                    WeightedOutput.STREAM_CODEC.encode(buffer, output);
                }
            }
        };

        @Override
        public MapCodec<SilexRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SilexRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
