package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.registry.HbmFluids;

import java.util.List;

/** Generated from the same FT_Heatable first-step table used by the boilers. */
public record BoilingJeiRecipe(
        HbmFluidDefinition input,
        HbmFluidDefinition output,
        int inputAmount,
        int outputAmount,
        int heatRequired
) {
    public static List<BoilingJeiRecipe> createAll() {
        return HbmFluids.niceOrder().stream()
                .map(HbmThermalConversions::firstBoilerStep)
                .flatMap(java.util.Optional::stream)
                .map(step -> new BoilingJeiRecipe(
                        step.input(),
                        step.output(),
                        step.amountReq(),
                        step.amountProduced(),
                        step.heatReq()
                ))
                .toList();
    }
}
