package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public record ElectrolyzerMetalRecipe(
        String group,
        Ingredient ingredient,
        MaterialOutput output1,
        MaterialOutput output2,
        int duration,
        List<ItemStack> byproducts
) implements Recipe<SingleRecipeInput> {
    public ElectrolyzerMetalRecipe {
        duration = Math.max(1, duration);
        byproducts = List.copyOf(byproducts);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return output1Stack().map(stack -> ScrapsItem.create(stack, true)).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return assemble(new SingleRecipeInput(ItemStack.EMPTY), registries);
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
        return HbmRecipeTypes.ELECTROLYZER_METAL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.ELECTROLYZER_METAL.get();
    }

    public java.util.Optional<FoundryMaterialStack> output1Stack() {
        return this.output1.toStack();
    }

    public java.util.Optional<FoundryMaterialStack> output2Stack() {
        return this.output2.toStack();
    }

    public record MaterialOutput(String material, int amount) {
        public static final MaterialOutput EMPTY = new MaterialOutput("", 0);

        private static final Codec<MaterialOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("material", "").forGetter(MaterialOutput::material),
                Codec.intRange(0, 1_000_000).optionalFieldOf("amount", 0).forGetter(MaterialOutput::amount)
        ).apply(instance, MaterialOutput::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MaterialOutput> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                MaterialOutput::material,
                ByteBufCodecs.VAR_INT,
                MaterialOutput::amount,
                MaterialOutput::new
        );

        public java.util.Optional<FoundryMaterialStack> toStack() {
            if (this.material.isBlank() || this.amount <= 0) {
                return java.util.Optional.empty();
            }
            return FoundryMaterial.byName(this.material).map(material -> new FoundryMaterialStack(material, this.amount));
        }
    }

    public static class Serializer implements RecipeSerializer<ElectrolyzerMetalRecipe> {
        private static final MapCodec<ElectrolyzerMetalRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ElectrolyzerMetalRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ElectrolyzerMetalRecipe::ingredient),
                MaterialOutput.CODEC.optionalFieldOf("output1", MaterialOutput.EMPTY).forGetter(ElectrolyzerMetalRecipe::output1),
                MaterialOutput.CODEC.optionalFieldOf("output2", MaterialOutput.EMPTY).forGetter(ElectrolyzerMetalRecipe::output2),
                Codec.intRange(1, 72000).optionalFieldOf("duration", 600).forGetter(ElectrolyzerMetalRecipe::duration),
                ItemStack.STRICT_CODEC.listOf().optionalFieldOf("byproducts", List.of()).forGetter(ElectrolyzerMetalRecipe::byproducts)
        ).apply(instance, ElectrolyzerMetalRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ElectrolyzerMetalRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ElectrolyzerMetalRecipe::group,
                Ingredient.CONTENTS_STREAM_CODEC,
                ElectrolyzerMetalRecipe::ingredient,
                MaterialOutput.STREAM_CODEC,
                ElectrolyzerMetalRecipe::output1,
                MaterialOutput.STREAM_CODEC,
                ElectrolyzerMetalRecipe::output2,
                ByteBufCodecs.VAR_INT,
                ElectrolyzerMetalRecipe::duration,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                ElectrolyzerMetalRecipe::byproducts,
                ElectrolyzerMetalRecipe::new
        );

        @Override
        public MapCodec<ElectrolyzerMetalRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ElectrolyzerMetalRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
