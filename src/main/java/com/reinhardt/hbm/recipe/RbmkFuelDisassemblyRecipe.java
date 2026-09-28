package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.item.RbmkFuelRodItem;
import com.reinhardt.hbm.item.RbmkPelletItem;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Direct port of 1.7.10 RBMKFuelCraftingHandler. A cool, used fuel rod is
 * dismantled into eight pellets while preserving its depletion and xenon band.
 */
public final class RbmkFuelDisassemblyRecipe extends CustomRecipe {
    public RbmkFuelDisassemblyRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack rod = onlyStack(input);
        return !rod.isEmpty() && resultFor(rod).isEmpty() == false;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return resultFor(onlyStack(input));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return HbmItems.RBMK_PELLET_ITEMS.stream()
                .map(holder -> holder.get())
                .filter(RbmkPelletItem.class::isInstance)
                .map(RbmkPelletItem.class::cast)
                .filter(pellet -> pellet.legacyId().equals("ueu"))
                .findFirst()
                .map(pellet -> {
                    ItemStack result = pellet.stackFor(0);
                    result.setCount(8);
                    return result;
                })
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        Item[] rods = HbmItems.RBMK_FUEL_ROD_ITEMS.stream()
                .map(holder -> holder.get())
                .filter(RbmkFuelRodItem.class::isInstance)
                .map(RbmkFuelRodItem.class::cast)
                .filter(rod -> RbmkFuelRodItem.FUEL_IDS.contains(rod.fuelId(new ItemStack(rod))))
                .toArray(Item[]::new);
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(rods));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.RBMK_FUEL_DISASSEMBLY_SERIALIZER.get();
    }

    private static ItemStack onlyStack(CraftingInput input) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!found.isEmpty()) {
                return ItemStack.EMPTY;
            }
            found = stack;
        }
        return found;
    }

    private static ItemStack resultFor(ItemStack stack) {
        if (!(stack.getItem() instanceof RbmkFuelRodItem rod)) {
            return ItemStack.EMPTY;
        }
        double depletion = RbmkFuelRodItem.depletion(stack);
        if (depletion < 0.01D
                || RbmkFuelRodItem.hullHeat(stack) >= 50.0D
                || RbmkFuelRodItem.coreHeat(stack) >= 50.0D) {
            return ItemStack.EMPTY;
        }

        String pelletId = rod.fuelId(stack);
        if (pelletId.equals("hep")) {
            pelletId = "hep239";
        }
        final String expectedId = pelletId;
        RbmkPelletItem pellet = HbmItems.RBMK_PELLET_ITEMS.stream()
                .map(holder -> holder.get())
                .filter(RbmkPelletItem.class::isInstance)
                .map(RbmkPelletItem.class::cast)
                .filter(candidate -> candidate.legacyId().equals(expectedId))
                .findFirst()
                .orElse(null);
        if (pellet == null) {
            return ItemStack.EMPTY;
        }

        double enrichment = 1.0D - depletion;
        int state = 4 - Mth.clamp((int) Math.ceil(enrichment * 5.0D - 1.0D), 0, 4);
        if (pellet.hasXenon() && RbmkFuelRodItem.xenon(stack) >= 0.5D) {
            state += 5;
        }
        ItemStack result = pellet.stackFor(state);
        result.setCount(8);
        return result;
    }

    public static final class Serializer implements RecipeSerializer<RbmkFuelDisassemblyRecipe> {
        private static final MapCodec<RbmkFuelDisassemblyRecipe> CODEC = MapCodec.unit(RbmkFuelDisassemblyRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, RbmkFuelDisassemblyRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public RbmkFuelDisassemblyRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new RbmkFuelDisassemblyRecipe();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, RbmkFuelDisassemblyRecipe recipe) {
            }
        };

        @Override
        public MapCodec<RbmkFuelDisassemblyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RbmkFuelDisassemblyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
