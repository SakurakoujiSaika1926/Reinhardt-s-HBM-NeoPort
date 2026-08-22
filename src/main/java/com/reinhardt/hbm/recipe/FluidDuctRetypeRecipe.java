package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.HbmFluidDuctItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class FluidDuctRetypeRecipe extends CustomRecipe {
    public FluidDuctRetypeRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return recipeState(input).valid();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        RecipeState state = recipeState(input);
        return state.valid() ? HbmFluidDuctItem.forFluid(state.fluid(), state.ducts()) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof FluidIdentifierItem) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                remaining.set(slot, copy);
                break;
            }
        }
        return remaining;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(HbmItems.FLUID_IDENTIFIER_MULTI.get()));
        ingredients.add(Ingredient.of(HbmItems.FF_FLUID_DUCT.get(), HbmBlocks.FLUID_DUCT_MK2.asItem()));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.FLUID_DUCT_RETYPE_SERIALIZER.get();
    }

    private static RecipeState recipeState(CraftingInput input) {
        HbmFluidDefinition fluid = null;
        int identifiers = 0;
        int ducts = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            if (item instanceof FluidIdentifierItem) {
                identifiers++;
                fluid = FluidIdentifierItem.primary(stack);
                if (identifiers > 1 || fluid.isNone()) {
                    return RecipeState.INVALID;
                }
            } else if (item == HbmItems.FF_FLUID_DUCT.get() || item == HbmBlocks.FLUID_DUCT_MK2.asItem()) {
                ducts++;
            } else {
                return RecipeState.INVALID;
            }
        }

        if (identifiers == 1 && ducts > 0 && fluid != null && !fluid.isNone()) {
            return new RecipeState(fluid, ducts);
        }
        return RecipeState.INVALID;
    }

    private record RecipeState(HbmFluidDefinition fluid, int ducts) {
        private static final RecipeState INVALID = new RecipeState(null, 0);

        private boolean valid() {
            return fluid != null && ducts > 0;
        }
    }

    public static class Serializer implements RecipeSerializer<FluidDuctRetypeRecipe> {
        private static final MapCodec<FluidDuctRetypeRecipe> CODEC = MapCodec.unit(FluidDuctRetypeRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, FluidDuctRetypeRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidDuctRetypeRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new FluidDuctRetypeRecipe();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidDuctRetypeRecipe recipe) {
            }
        };

        @Override
        public MapCodec<FluidDuctRetypeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FluidDuctRetypeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
