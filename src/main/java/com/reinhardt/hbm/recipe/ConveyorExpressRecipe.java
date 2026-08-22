package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.ConveyorWandItem;
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
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Optional;

/** The 1.7.10 express-conveyor recipe, including compatible 1,000 mB lubricant containers. */
public final class ConveyorExpressRecipe extends CustomRecipe {
    private static final int LUBRICANT_AMOUNT = 1_000;

    public ConveyorExpressRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (slot == 4) {
                if (!isLubricantContainer(stack)) {
                    return false;
                }
            } else if (!isRegularConveyor(stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return matchesLayout(input)
                ? ConveyorWandItem.stackFor(HbmItems.CONVEYOR_WAND, ConveyorWandItem.ConveyorType.EXPRESS).copyWithCount(8)
                : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        if (input.size() > 4) {
            drainLubricant(input.getItem(4)).ifPresent(stack -> remaining.set(4, stack));
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ConveyorWandItem.stackFor(HbmItems.CONVEYOR_WAND, ConveyorWandItem.ConveyorType.EXPRESS).copyWithCount(8);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ItemStack regular = ConveyorWandItem.stackFor(HbmItems.CONVEYOR_WAND, ConveyorWandItem.ConveyorType.REGULAR);
        Ingredient regularIngredient = DataComponentIngredient.of(false, regular);
        for (int slot = 0; slot < 9; slot++) {
            ingredients.add(slot == 4
                    ? Ingredient.of(HbmItems.CANISTER_FULL.get(), HbmItems.FLUID_TANK_FULL.get(), HbmItems.FLUID_TANK_LEAD_FULL.get())
                    : regularIngredient);
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CONVEYOR_EXPRESS_SERIALIZER.get();
    }

    private static boolean matchesLayout(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        for (int slot = 0; slot < input.size(); slot++) {
            if (slot == 4 ? !isLubricantContainer(input.getItem(slot)) : !isRegularConveyor(input.getItem(slot))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRegularConveyor(ItemStack stack) {
        return stack.getItem() instanceof ConveyorWandItem wand
                && wand.type(stack) == ConveyorWandItem.ConveyorType.REGULAR;
    }

    private static boolean isLubricantContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack single = stack.copyWithCount(1);
        Optional<IFluidHandlerItem> optional = FluidUtil.getFluidHandler(single);
        if (optional.isEmpty()) {
            return false;
        }
        IFluidHandlerItem handler = optional.get();
        HbmFluidDefinition lubricant = HbmFluids.byName("lubricant").orElse(HbmFluids.none());
        if (lubricant.isNone()) {
            return false;
        }

        int amount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (fluid.isEmpty()) {
                continue;
            }
            HbmFluidDefinition contained = HbmFluids.fromNeoFluid(fluid.getFluid()).orElse(HbmFluids.none());
            if (contained != lubricant) {
                return false;
            }
            amount += fluid.getAmount();
        }
        if (amount != LUBRICANT_AMOUNT) {
            return false;
        }
        FluidStack drained = handler.drain(
                HbmFluids.toNeoStack(lubricant, LUBRICANT_AMOUNT),
                IFluidHandler.FluidAction.SIMULATE
        );
        return drained.getAmount() == LUBRICANT_AMOUNT;
    }

    private static Optional<ItemStack> drainLubricant(ItemStack stack) {
        if (!isLubricantContainer(stack)) {
            return Optional.empty();
        }
        ItemStack single = stack.copyWithCount(1);
        Optional<IFluidHandlerItem> optional = FluidUtil.getFluidHandler(single);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        IFluidHandlerItem handler = optional.get();
        HbmFluidDefinition lubricant = HbmFluids.byName("lubricant").orElse(HbmFluids.none());
        FluidStack drained = handler.drain(
                HbmFluids.toNeoStack(lubricant, LUBRICANT_AMOUNT),
                IFluidHandler.FluidAction.EXECUTE
        );
        if (drained.getAmount() != LUBRICANT_AMOUNT) {
            return Optional.empty();
        }
        ItemStack result = handler.getContainer().copy();
        result.setCount(1);
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }

    public static final class Serializer implements RecipeSerializer<ConveyorExpressRecipe> {
        private static final MapCodec<ConveyorExpressRecipe> CODEC = MapCodec.unit(ConveyorExpressRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, ConveyorExpressRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ConveyorExpressRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new ConveyorExpressRecipe();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ConveyorExpressRecipe recipe) {
            }
        };

        @Override
        public MapCodec<ConveyorExpressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConveyorExpressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
