package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.item.StampItem;
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

public record PressRecipe(
        String group,
        StampItem.StampType stamp,
        Ingredient ingredient,
        int inputCount,
        ItemStack result
) implements Recipe<PressRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        ItemStack ingredientStack = input.ingredient();
        return ingredientStack.getCount() >= this.inputCount
                && this.ingredient.test(ingredientStack)
                && StampItem.isStampOfType(input.stamp(), this.stamp);
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
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.ingredient);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.PRESS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.PRESS.get();
    }

    public record Input(ItemStack ingredient, ItemStack stamp) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> this.ingredient;
                case 1 -> this.stamp;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Serializer implements RecipeSerializer<PressRecipe> {
        private static final MapCodec<PressRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(PressRecipe::group),
                StampItem.StampType.CODEC.fieldOf("stamp").forGetter(PressRecipe::stamp),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PressRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(PressRecipe::inputCount),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PressRecipe::result)
        ).apply(instance, PressRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PressRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PressRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                StampItem.StampType stamp = decodeStamp(buffer.readUtf());
                Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int inputCount = buffer.readVarInt();
                ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                return new PressRecipe(group, stamp, ingredient, inputCount, result);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PressRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeUtf(recipe.stamp().getSerializedName());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                buffer.writeVarInt(recipe.inputCount());
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result());
            }
        };

        private static StampItem.StampType decodeStamp(String name) {
            for (StampItem.StampType type : StampItem.StampType.values()) {
                if (type.getSerializedName().equals(name)) {
                    return type;
                }
            }
            return StampItem.StampType.FLAT;
        }

        @Override
        public MapCodec<PressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
