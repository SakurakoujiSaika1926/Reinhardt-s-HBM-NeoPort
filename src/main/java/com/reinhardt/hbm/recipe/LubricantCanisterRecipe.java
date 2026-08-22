package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.ChemistrySetItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.registry.HbmFluids;
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

public class LubricantCanisterRecipe extends CustomRecipe {
    private static final int CONTAINER_AMOUNT = 1_000;

    public LubricantCanisterRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return recipeState(input).valid();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return recipeState(input).valid() ? result() : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            if (item instanceof HbmFluidContainerItem container && container.isFilledContainer()) {
                remaining.set(slot, container.kind().emptyStack());
            } else if (item.hasCraftingRemainingItem(stack)) {
                remaining.set(slot, item.getCraftingRemainingItem(stack));
            }
        }
        return remaining;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(HbmItems.CANISTER_EMPTY.get()));
        ingredients.add(Ingredient.of(HbmItems.CANISTER_EMPTY.get()));
        ingredients.add(Ingredient.of(HbmItems.CHEMISTRY_SET.get(), HbmItems.CHEMISTRY_SET_BORON.get()));
        ingredients.add(Ingredient.of(HbmItems.CANISTER_FULL.get(), HbmItems.FLUID_TANK_FULL.get(), HbmItems.FLUID_TANK_LEAD_FULL.get()));
        ingredients.add(Ingredient.of(HbmItems.GAS_FULL.get(), HbmItems.FLUID_TANK_FULL.get(), HbmItems.FLUID_TANK_LEAD_FULL.get()));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.LUBRICANT_CANISTER_SERIALIZER.get();
    }

    private static ItemStack result() {
        return new ItemStack(HbmItems.CANISTER_LUBRICANT.get(), 2);
    }

    private static RecipeState recipeState(CraftingInput input) {
        HbmFluidDefinition heatingOil = HbmFluids.byName("heatingoil").orElse(HbmFluids.none());
        HbmFluidDefinition unsaturateds = HbmFluids.byName("unsaturateds").orElse(HbmFluids.none());
        int emptyCanisters = 0;
        int chemistrySets = 0;
        int heatingOilContainers = 0;
        int unsaturatedsContainers = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            if (item == HbmItems.CANISTER_EMPTY.get()) {
                emptyCanisters++;
            } else if (item instanceof ChemistrySetItem) {
                chemistrySets++;
            } else if (isFluidContainer(stack, heatingOil)) {
                heatingOilContainers++;
            } else if (isFluidContainer(stack, unsaturateds)) {
                unsaturatedsContainers++;
            } else {
                return RecipeState.INVALID;
            }

            if (emptyCanisters > 2 || chemistrySets > 1 || heatingOilContainers > 1 || unsaturatedsContainers > 1) {
                return RecipeState.INVALID;
            }
        }

        if (emptyCanisters == 2 && chemistrySets == 1 && heatingOilContainers == 1 && unsaturatedsContainers == 1) {
            return RecipeState.VALID;
        }
        return RecipeState.INVALID;
    }

    private static boolean isFluidContainer(ItemStack stack, HbmFluidDefinition required) {
        if (required.isNone()) {
            return false;
        }

        Item item = stack.getItem();
        if (item instanceof HbmFluidContainerItem container
                && container.isFilledContainer()
                && container.kind().capacity() == CONTAINER_AMOUNT
                && sameFluid(HbmFluidContainerItem.fluid(stack), required)) {
            return true;
        }

        return HbmFluids.entry(required.name())
                .flatMap(HbmFluids.HbmFluidEntry::bucketItem)
                .map(bucket -> item == bucket.get())
                .orElse(false);
    }

    private static boolean sameFluid(HbmFluidDefinition first, HbmFluidDefinition second) {
        return first == second || first.name().equals(second.name());
    }

    private record RecipeState(boolean valid) {
        private static final RecipeState VALID = new RecipeState(true);
        private static final RecipeState INVALID = new RecipeState(false);
    }

    public static class Serializer implements RecipeSerializer<LubricantCanisterRecipe> {
        private static final MapCodec<LubricantCanisterRecipe> CODEC = MapCodec.unit(LubricantCanisterRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, LubricantCanisterRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public LubricantCanisterRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new LubricantCanisterRecipe();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, LubricantCanisterRecipe recipe) {
            }
        };

        @Override
        public MapCodec<LubricantCanisterRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LubricantCanisterRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
