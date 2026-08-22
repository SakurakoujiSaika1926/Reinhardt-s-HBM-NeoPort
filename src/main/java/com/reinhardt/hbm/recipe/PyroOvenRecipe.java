package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
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

/** Data-driven equivalent of 1.7.10 PyroOvenRecipes.PyroOvenRecipe. */
public record PyroOvenRecipe(
        String group,
        ItemInput itemInput,
        FluidInput fluidInput,
        ItemStack itemOutput,
        FluidOutput fluidOutput,
        int duration
) implements Recipe<PyroOvenRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        boolean itemMatches = this.itemInput.isEmpty()
                ? input.item().isEmpty()
                : this.itemInput.matches(input.item());
        boolean fluidMatches = this.fluidInput.isEmpty()
                || this.fluidInput.matches(input.fluid(), input.fluidAmount());
        return itemMatches && fluidMatches;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.itemOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.itemOutput.isEmpty() && !this.fluidOutput.isEmpty()
                ? FluidIconItem.forFluid(this.fluidOutput.fluid())
                : this.itemOutput;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        if (!this.itemInput.isEmpty()) ingredients.add(this.itemInput.ingredient());
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.PYRO_OVEN_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.PYRO_OVEN.get();
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

    public record ItemInput(Ingredient ingredient, int count) {
        public static final ItemInput EMPTY = new ItemInput(Ingredient.EMPTY, 0);

        private static final Codec<ItemInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ItemInput::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ItemInput::count)
        ).apply(instance, ItemInput::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ItemInput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ItemInput decode(RegistryFriendlyByteBuf buffer) {
                return new ItemInput(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ItemInput input) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, input.ingredient());
                buffer.writeVarInt(input.count());
            }
        };

        public boolean isEmpty() {
            return this.count <= 0 || this.ingredient.isEmpty();
        }

        public boolean matches(ItemStack stack) {
            return stack.getCount() >= this.count && this.ingredient.test(stack);
        }
    }

    public record FluidInput(HbmFluidDefinition fluid, int amount) {
        public static final FluidInput EMPTY = new FluidInput(HbmFluids.none(), 0);

        private static final Codec<FluidInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(input -> input.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidInput::amount)
        ).apply(instance, FluidInput::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidInput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidInput decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidInput input) {
                buffer.writeUtf(input.fluid().name());
                buffer.writeVarInt(input.amount());
            }
        };

        public boolean isEmpty() {
            return this.fluid.isNone() || this.amount <= 0;
        }

        public boolean matches(HbmFluidDefinition fluid, int available) {
            return this.fluid == fluid && available >= this.amount;
        }

        public HbmFluidStack stack() {
            return isEmpty() ? HbmFluidStack.EMPTY : new HbmFluidStack(this.fluid, this.amount, 0);
        }

        private static FluidInput fromName(String fluid, int amount) {
            return new FluidInput(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }
    }

    public record FluidOutput(HbmFluidDefinition fluid, int amount) {
        public static final FluidOutput EMPTY = new FluidOutput(HbmFluids.none(), 0);

        private static final Codec<FluidOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(output -> output.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidOutput::amount)
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

        public HbmFluidStack stack() {
            return isEmpty() ? HbmFluidStack.EMPTY : new HbmFluidStack(this.fluid, this.amount, 0);
        }

        private static FluidOutput fromName(String fluid, int amount) {
            return new FluidOutput(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }
    }

    public static class Serializer implements RecipeSerializer<PyroOvenRecipe> {
        private static final MapCodec<PyroOvenRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(PyroOvenRecipe::group),
                ItemInput.CODEC.optionalFieldOf("item_input", ItemInput.EMPTY).forGetter(PyroOvenRecipe::itemInput),
                FluidInput.CODEC.optionalFieldOf("fluid_input", FluidInput.EMPTY).forGetter(PyroOvenRecipe::fluidInput),
                ItemStack.STRICT_CODEC.optionalFieldOf("item_output", ItemStack.EMPTY).forGetter(PyroOvenRecipe::itemOutput),
                FluidOutput.CODEC.optionalFieldOf("fluid_output", FluidOutput.EMPTY).forGetter(PyroOvenRecipe::fluidOutput),
                Codec.intRange(1, 20_000).fieldOf("duration").forGetter(PyroOvenRecipe::duration)
        ).apply(instance, PyroOvenRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PyroOvenRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PyroOvenRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new PyroOvenRecipe(
                        buffer.readUtf(),
                        ItemInput.STREAM_CODEC.decode(buffer),
                        FluidInput.STREAM_CODEC.decode(buffer),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                        FluidOutput.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PyroOvenRecipe recipe) {
                buffer.writeUtf(recipe.group());
                ItemInput.STREAM_CODEC.encode(buffer, recipe.itemInput());
                FluidInput.STREAM_CODEC.encode(buffer, recipe.fluidInput());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.itemOutput());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.fluidOutput());
                buffer.writeVarInt(recipe.duration());
            }
        };

        @Override
        public MapCodec<PyroOvenRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PyroOvenRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
