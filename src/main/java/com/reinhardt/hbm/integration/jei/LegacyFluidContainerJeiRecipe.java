package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI display for fixed 1.7.10 FluidContainerRegistry mappings. */
public record LegacyFluidContainerJeiRecipe(
        ItemStack inputItem,
        HbmFluidDefinition fluid,
        int amount,
        ItemStack outputItem,
        Mode mode
) {
    public static final int LEGACY_BARREL_AMOUNT = 10_000;

    public static List<LegacyFluidContainerJeiRecipe> createAll() {
        List<LegacyFluidContainerJeiRecipe> recipes = new ArrayList<>();
        addBarrelMapping(recipes, "diesel", HbmItems.RED_BARREL_ITEM.get());
        addBarrelMapping(recipes, "kerosene", HbmItems.PINK_BARREL_ITEM.get());
        addBarrelMapping(recipes, "oxygen", HbmItems.LOX_BARREL_ITEM.get());
        addFixedMapping(
                recipes,
                "xpjuice",
                100,
                HbmItems.IV_XP_EMPTY.get(),
                HbmItems.IV_XP.get()
        );
        return List.copyOf(recipes);
    }

    private static void addFixedMapping(
            List<LegacyFluidContainerJeiRecipe> recipes,
            String fluidName,
            int amount,
            Item emptyContainer,
            Item filledContainer
    ) {
        HbmFluidDefinition fluid = HbmFluids.byName(fluidName).orElse(HbmFluids.none());
        if (fluid.isNone()) {
            return;
        }
        ItemStack empty = new ItemStack(emptyContainer);
        ItemStack filled = new ItemStack(filledContainer);
        recipes.add(new LegacyFluidContainerJeiRecipe(
                empty.copy(), fluid, amount, filled.copy(), Mode.FILL
        ));
        recipes.add(new LegacyFluidContainerJeiRecipe(
                filled.copy(), fluid, amount, empty.copy(), Mode.DRAIN
        ));
    }

    private static void addBarrelMapping(List<LegacyFluidContainerJeiRecipe> recipes, String fluidName, Item filledBarrel) {
        HbmFluidDefinition fluid = HbmFluids.byName(fluidName).orElse(HbmFluids.none());
        if (fluid.isNone()) {
            return;
        }
        ItemStack emptyTank = new ItemStack(HbmItems.TANK_STEEL.get());
        ItemStack barrel = new ItemStack(filledBarrel);
        recipes.add(new LegacyFluidContainerJeiRecipe(
                emptyTank.copy(),
                fluid,
                LEGACY_BARREL_AMOUNT,
                barrel.copy(),
                Mode.FILL
        ));
        recipes.add(new LegacyFluidContainerJeiRecipe(
                barrel.copy(),
                fluid,
                LEGACY_BARREL_AMOUNT,
                emptyTank.copy(),
                Mode.DRAIN
        ));
    }

    public enum Mode {
        FILL,
        DRAIN
    }
}
