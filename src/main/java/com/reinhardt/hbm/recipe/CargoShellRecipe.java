package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.item.AmmoArtyItem;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
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

public class CargoShellRecipe extends CustomRecipe {
    public CargoShellRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return recipeState(input).valid();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        RecipeState state = recipeState(input);
        return state.valid() ? AmmoArtyItem.withCargo(state.shell(), state.cargo(), registries) : ItemStack.EMPTY;
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
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(com.reinhardt.hbm.registry.HbmItems.AMMO_ARTY.get()));
        ingredients.add(Ingredient.EMPTY);
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CARGO_SHELL_SERIALIZER.get();
    }

    private static RecipeState recipeState(CraftingInput input) {
        ItemStack shell = ItemStack.EMPTY;
        ItemStack cargo = ItemStack.EMPTY;
        int itemCount = 0;
        int shellCount = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            itemCount++;
            if (AmmoArtyItem.isCargoVariant(stack) && !AmmoArtyItem.hasCargo(stack)) {
                shellCount++;
                shell = stack;
            } else {
                Item item = stack.getItem();
                if (item.hasCraftingRemainingItem(stack) || !cargo.isEmpty()) {
                    return RecipeState.INVALID;
                }
                cargo = stack;
            }

            if (itemCount > 2 || shellCount > 1) {
                return RecipeState.INVALID;
            }
        }

        return itemCount == 2 && shellCount == 1 && !shell.isEmpty() && !cargo.isEmpty()
                ? new RecipeState(shell, cargo)
                : RecipeState.INVALID;
    }

    private record RecipeState(ItemStack shell, ItemStack cargo) {
        private static final RecipeState INVALID = new RecipeState(ItemStack.EMPTY, ItemStack.EMPTY);

        private boolean valid() {
            return !shell.isEmpty() && !cargo.isEmpty();
        }
    }

    public static class Serializer implements RecipeSerializer<CargoShellRecipe> {
        private static final MapCodec<CargoShellRecipe> CODEC = MapCodec.unit(CargoShellRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, CargoShellRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CargoShellRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new CargoShellRecipe();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CargoShellRecipe recipe) {
            }
        };

        @Override
        public MapCodec<CargoShellRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CargoShellRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
