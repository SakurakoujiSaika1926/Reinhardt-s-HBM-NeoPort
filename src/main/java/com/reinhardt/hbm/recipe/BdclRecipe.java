package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * 1.7.10's BDCL recipe accepted every legacy tar container and any 1000mB
 * water container. A vanilla JSON ingredient cannot express either predicate.
 */
public final class BdclRecipe extends CustomRecipe {
    private static final Set<String> TARS = Set.of("oil_tar", "coal_tar", "crack_tar", "wood_tar");
    private static final TagKey<Item> WHITE_DYES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "dyes/white"));

    public BdclRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return state(input).valid();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return state(input).valid() ? new ItemStack(HbmItems.BDCL.get()) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(HbmItems.BDCL.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (isStandardContainer(stack)) {
                remaining.set(slot, HbmFluidContainerItem.Kind.FLUID_TANK.emptyStack());
            } else if (stack.is(Items.WATER_BUCKET)) {
                remaining.set(slot, new ItemStack(Items.BUCKET));
            }
        }
        return remaining;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(HbmItems.FLUID_TANK_FULL.get()));
        ingredients.add(Ingredient.of(Items.WATER_BUCKET, HbmItems.FLUID_TANK_FULL.get()));
        ingredients.add(Ingredient.of(WHITE_DYES));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.BDCL_SERIALIZER.get();
    }

    private static State state(CraftingInput input) {
        int tar = 0;
        int water = 0;
        int dye = 0;
        int total = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            total++;
            if (isTarContainer(stack)) {
                tar++;
            } else if (isWaterContainer(stack)) {
                water++;
            } else if (stack.is(WHITE_DYES)) {
                dye++;
            } else {
                return State.INVALID;
            }
        }
        return total == 3 && tar == 1 && water == 1 && dye == 1 ? State.VALID : State.INVALID;
    }

    private static boolean isTarContainer(ItemStack stack) {
        return isStandardContainer(stack) && TARS.contains(HbmFluidContainerItem.fluid(stack).name());
    }

    private static boolean isWaterContainer(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET)
                || (isStandardContainer(stack) && HbmFluidContainerItem.fluid(stack).name().equals("water"));
    }

    private static boolean isStandardContainer(ItemStack stack) {
        return stack.getItem() instanceof HbmFluidContainerItem container
                && container.isFilledContainer()
                && container.kind() == HbmFluidContainerItem.Kind.FLUID_TANK;
    }

    private enum State {
        VALID,
        INVALID;

        private boolean valid() {
            return this == VALID;
        }
    }

    public static final class Serializer implements RecipeSerializer<BdclRecipe> {
        private static final MapCodec<BdclRecipe> CODEC = MapCodec.unit(BdclRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, BdclRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public BdclRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new BdclRecipe();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, BdclRecipe recipe) {
            }
        };

        @Override
        public MapCodec<BdclRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BdclRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
