package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

final class HbmJeiFluidTooltips {
    private HbmJeiFluidTooltips() {
    }

    static void addTo(IRecipeSlotBuilder slot) {
        slot.addRichTooltipCallback(HbmJeiFluidTooltips::appendFluidTooltip);
    }

    private static void appendFluidTooltip(IRecipeSlotView view, ITooltipBuilder tooltip) {
        FluidStack stack = view.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK).orElse(FluidStack.EMPTY);
        if (stack.isEmpty()) {
            return;
        }
        HbmFluids.fromNeoFluid(stack.getFluid()).ifPresent(fluid -> {
            List<Component> lines = new ArrayList<>();
            HbmFluidTooltip.appendTraitInfo(lines, fluid, false);
            tooltip.addAll(lines);
        });
    }
}
