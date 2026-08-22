package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
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

public record ArcWelderRecipe(
        String group,
        int duration,
        long consumption,
        List<CountedIngredient> inputs,
        Optional<FluidIngredient> fluid,
        ItemStack result
) implements Recipe<ArcWelderRecipe.Input> {
    public ArcWelderRecipe {
        inputs = List.copyOf(inputs);
        fluid = fluid == null ? Optional.empty() : fluid;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return matchesGroup(input.stacks(), this.inputs)
                && matchesFluid(input);
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
        addIngredients(list, this.inputs);
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.ARC_WELDER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.ARC_WELDER.get();
    }

    public static boolean matchesGroup(List<ItemStack> stacks, List<CountedIngredient> ingredients) {
        List<CountedIngredient> remaining = new ArrayList<>(ingredients);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }

            boolean matched = false;
            for (int index = 0; index < remaining.size(); index++) {
                CountedIngredient ingredient = remaining.get(index);
                if (stack.getCount() >= ingredient.count() && ingredient.ingredient().test(stack)) {
                    remaining.remove(index);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    private static void addIngredients(NonNullList<Ingredient> list, List<CountedIngredient> ingredients) {
        for (CountedIngredient ingredient : ingredients) {
            list.add(ingredient.ingredient());
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

    public record Input(
            List<ItemStack> stacks,
            HbmFluidDefinition fluid,
            int fluidAmount
    ) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            if (index < 0) {
                return ItemStack.EMPTY;
            }
            return index < this.stacks.size() ? this.stacks.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return this.stacks.size();
        }
    }

    private boolean matchesFluid(Input input) {
        if (this.fluid.isEmpty()) {
            return true;
        }
        FluidIngredient ingredient = this.fluid.get();
        return input.fluid() == ingredient.fluid() && input.fluidAmount() >= ingredient.amount();
    }

    public static class Serializer implements RecipeSerializer<ArcWelderRecipe> {
        private static final MapCodec<ArcWelderRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ArcWelderRecipe::group),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(ArcWelderRecipe::duration),
                Codec.LONG.fieldOf("consumption").forGetter(ArcWelderRecipe::consumption),
                CountedIngredient.CODEC.listOf().fieldOf("inputs").forGetter(ArcWelderRecipe::inputs),
                FluidIngredient.CODEC.optionalFieldOf("fluid").forGetter(ArcWelderRecipe::fluid),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ArcWelderRecipe::result)
        ).apply(instance, ArcWelderRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ArcWelderRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ArcWelderRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                int duration = buffer.readVarInt();
                long consumption = buffer.readVarLong();
                List<CountedIngredient> inputs = readIngredients(buffer);
                Optional<FluidIngredient> fluid = buffer.readBoolean()
                        ? Optional.of(FluidIngredient.STREAM_CODEC.decode(buffer))
                        : Optional.empty();
                ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                return new ArcWelderRecipe(group, duration, consumption, inputs, fluid, result);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ArcWelderRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarLong(recipe.consumption());
                writeIngredients(buffer, recipe.inputs());
                buffer.writeBoolean(recipe.fluid().isPresent());
                recipe.fluid().ifPresent(ingredient -> FluidIngredient.STREAM_CODEC.encode(buffer, ingredient));
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result());
            }
        };

        @Override
        public MapCodec<ArcWelderRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ArcWelderRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static List<CountedIngredient> readIngredients(RegistryFriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            List<CountedIngredient> ingredients = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                ingredients.add(CountedIngredient.STREAM_CODEC.decode(buffer));
            }
            return List.copyOf(ingredients);
        }

        private static void writeIngredients(RegistryFriendlyByteBuf buffer, List<CountedIngredient> ingredients) {
            buffer.writeVarInt(ingredients.size());
            for (CountedIngredient ingredient : ingredients) {
                CountedIngredient.STREAM_CODEC.encode(buffer, ingredient);
            }
        }
    }
}
