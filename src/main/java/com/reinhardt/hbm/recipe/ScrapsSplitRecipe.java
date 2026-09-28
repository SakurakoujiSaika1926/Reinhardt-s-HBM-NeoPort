package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.registry.HbmItems;
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

/**
 * Exact modern equivalent of 1.7.10's {@code ScrapsCraftingHandler}.
 * A single scraps item containing at least two foundry quanta is split into
 * two solid scraps items. Odd quantities intentionally lose one quantum, as
 * in the original integer division rule.
 */
public final class ScrapsSplitRecipe extends CustomRecipe {
    public ScrapsSplitRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        FoundryMaterialStack contents = contentsOfOnlyStack(input);
        return contents != null && contents.amount() >= 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        FoundryMaterialStack contents = contentsOfOnlyStack(input);
        if (contents == null || contents.amount() < 2) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ScrapsItem.create(
                new FoundryMaterialStack(contents.material(), contents.amount() / 2),
                false
        );
        result.setCount(2);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return HbmItems.SCRAPS_ITEMS.values().stream()
                .map(holder -> holder.get())
                .filter(ScrapsItem.class::isInstance)
                .map(item -> {
                    ScrapsItem scraps = (ScrapsItem) item;
                    ItemStack result = ScrapsItem.create(new FoundryMaterialStack(scraps.material(), 36), false);
                    result.setCount(2);
                    return result;
                })
                .findFirst()
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        Item[] scraps = HbmItems.SCRAPS_ITEMS.values().stream()
                .map(holder -> holder.get())
                .toArray(Item[]::new);
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(scraps));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.SCRAPS_SPLIT_SERIALIZER.get();
    }

    private static FoundryMaterialStack contentsOfOnlyStack(CraftingInput input) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!found.isEmpty() || !(stack.getItem() instanceof ScrapsItem)) {
                return null;
            }
            found = stack;
        }
        return found.isEmpty() ? null : ScrapsItem.contents(found);
    }

    public static final class Serializer implements RecipeSerializer<ScrapsSplitRecipe> {
        private static final MapCodec<ScrapsSplitRecipe> CODEC = MapCodec.unit(ScrapsSplitRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, ScrapsSplitRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ScrapsSplitRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new ScrapsSplitRecipe();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ScrapsSplitRecipe recipe) {
            }
        };

        @Override
        public MapCodec<ScrapsSplitRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ScrapsSplitRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
