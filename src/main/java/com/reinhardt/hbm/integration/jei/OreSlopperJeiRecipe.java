package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/** Exact JEI representation of 1.7.10's fixed Ore Slopper process. */
public record OreSlopperJeiRecipe(
        HbmFluidDefinition water,
        ItemStack bedrockOreBase,
        List<ItemStack> oreOutputs,
        HbmFluidDefinition slop
) {
    private static final int FLUID_AMOUNT = 1_000;

    public static OreSlopperJeiRecipe create() {
        List<ItemStack> oreOutputs = Arrays.stream(BedrockOreItem.Type.values())
                .map(type -> BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, BedrockOreItem.Grade.BASE, type))
                .toList();
        return new OreSlopperJeiRecipe(
                HbmFluids.byName("water").orElse(HbmFluids.none()),
                new ItemStack(HbmItems.BEDROCK_ORE_BASE.get()),
                oreOutputs,
                HbmFluids.byName("slop").orElse(HbmFluids.none())
        );
    }

    public int fluidAmount() {
        return FLUID_AMOUNT;
    }
}
