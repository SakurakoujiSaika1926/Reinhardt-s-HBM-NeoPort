package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * Position-sensitive 3x3 recipe used by the legacy ammo press. Empty cells are
 * part of the recipe contract and must remain empty while processing.
 */
public record AmmoPressRecipe(String group, List<SlotIngredient> input, ItemStack result)
        implements Recipe<AmmoPressRecipe.Input> {
    private static final Codec<List<SlotIngredient>> INPUT_CODEC = SlotIngredient.CODEC.codec().listOf().validate(
            input -> input.size() == 9
                    ? DataResult.success(List.copyOf(input))
                    : DataResult.error(() -> "Ammo press recipes require exactly nine input cells")
    );

    public AmmoPressRecipe {
        input = List.copyOf(input);
        if (input.size() != 9) {
            throw new IllegalArgumentException("Ammo press recipes require exactly nine input cells");
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        for (int slot = 0; slot < this.input.size(); slot++) {
            SlotIngredient required = this.input.get(slot);
            ItemStack held = input.getItem(slot);
            if (required.isEmpty()) {
                if (!held.isEmpty()) {
                    return false;
                }
            } else if (held.isEmpty() || held.getCount() < required.count() || !required.ingredient().orElseThrow().test(held)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (SlotIngredient slot : this.input) {
            slot.ingredient().ifPresent(ingredients::add);
        }
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.AMMO_PRESS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.AMMO_PRESS.get();
    }

    public record Input(List<ItemStack> stacks) implements RecipeInput {
        public Input {
            stacks = List.copyOf(stacks);
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

    public record SlotIngredient(Optional<Ingredient> ingredient, int count) {
        private static final MapCodec<SlotIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.optionalFieldOf("ingredient").forGetter(SlotIngredient::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(SlotIngredient::count)
        ).apply(instance, SlotIngredient::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SlotIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SlotIngredient decode(RegistryFriendlyByteBuf buffer) {
                boolean hasIngredient = buffer.readBoolean();
                return hasIngredient
                        ? new SlotIngredient(Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)), buffer.readVarInt())
                        : empty();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, SlotIngredient ingredient) {
                buffer.writeBoolean(ingredient.ingredient.isPresent());
                if (ingredient.ingredient.isPresent()) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.ingredient.get());
                    buffer.writeVarInt(ingredient.count);
                }
            }
        };

        public static SlotIngredient empty() {
            return new SlotIngredient(Optional.empty(), 1);
        }

        public boolean isEmpty() {
            return this.ingredient.isEmpty();
        }
    }

    public static class Serializer implements RecipeSerializer<AmmoPressRecipe> {
        private static final MapCodec<AmmoPressRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(AmmoPressRecipe::group),
                INPUT_CODEC.fieldOf("input").forGetter(AmmoPressRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AmmoPressRecipe::result)
        ).apply(instance, AmmoPressRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AmmoPressRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public AmmoPressRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                List<SlotIngredient> input = new ArrayList<>(9);
                for (int slot = 0; slot < 9; slot++) {
                    input.add(SlotIngredient.STREAM_CODEC.decode(buffer));
                }
                return new AmmoPressRecipe(group, input, ItemStack.STREAM_CODEC.decode(buffer));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, AmmoPressRecipe recipe) {
                buffer.writeUtf(recipe.group);
                for (SlotIngredient ingredient : recipe.input) {
                    SlotIngredient.STREAM_CODEC.encode(buffer, ingredient);
                }
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            }
        };

        @Override
        public MapCodec<AmmoPressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AmmoPressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
