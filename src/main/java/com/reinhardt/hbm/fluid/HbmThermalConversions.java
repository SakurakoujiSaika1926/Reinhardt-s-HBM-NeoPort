package com.reinhardt.hbm.fluid;

import com.reinhardt.hbm.registry.HbmFluids;

import java.util.Optional;

public final class HbmThermalConversions {
    private HbmThermalConversions() {
    }

    public static Optional<HeatingStep> firstBoilerStep(HbmFluidDefinition input) {
        if (input == null || input.isNone()) {
            return Optional.empty();
        }
        return switch (input.name()) {
            case "air" -> heating(input, "airblast", 5, 1, 1, 1.0D);
            case "water" -> heating(input, "steam", 200, 1, 100, 1.0D);
            case "oil" -> heating(input, "hotoil", 10, 1, 1, 1.0D);
            case "oil_ds" -> heating(input, "hotoil_ds", 10, 1, 1, 1.0D);
            case "crackoil" -> heating(input, "hotcrackoil", 10, 1, 1, 1.0D);
            case "crackoil_ds" -> heating(input, "hotcrackoil_ds", 10, 1, 1, 1.0D);
            default -> Optional.empty();
        };
    }

    public static Optional<HeatingStep> firstHeatExchangerStep(HbmFluidDefinition input) {
        if (input == null || input.isNone()) {
            return Optional.empty();
        }
        return switch (input.name()) {
            // TileEntityRBMKHeater accepts every 1.7.10 FT_Heatable fluid
            // which has a HEATEXCHANGER efficiency, not coolant alone.  The
            // water step is the sole non-unit-efficiency entry (0.25).
            case "water" -> heating(input, "steam", 200, 1, 100, 0.25D);
            case "oil" -> heating(input, "hotoil", 10, 1, 1, 1.0D);
            case "oil_ds" -> heating(input, "hotoil_ds", 10, 1, 1, 1.0D);
            case "crackoil" -> heating(input, "hotcrackoil", 10, 1, 1, 1.0D);
            case "crackoil_ds" -> heating(input, "hotcrackoil_ds", 10, 1, 1, 1.0D);
            case "coolant" -> heating(input, "coolant_hot", 300, 1, 1, 1.0D);
            case "perfluoromethyl" -> heating(input, "perfluoromethyl_hot", 300, 1, 1, 1.0D);
            case "mug" -> heating(input, "mug_hot", 400, 1, 1, 1.0D);
            case "blood" -> heating(input, "blood_hot", 500, 1, 1, 1.0D);
            default -> Optional.empty();
        };
    }

    /** TileEntityICF uses the same 1:1, 400 TU sodium heating step as the old heat exchanger. */
    public static Optional<HeatingStep> firstIcfStep(HbmFluidDefinition input) {
        if (input == null || input.isNone()) {
            return Optional.empty();
        }
        return switch (input.name()) {
            case "sodium" -> heating(input, "sodium_hot", 400, 1, 1, 1.0D);
            default -> Optional.empty();
        };
    }

    public static Optional<HeatingStep> firstGeothermalStep(HbmFluidDefinition input) {
        if (input == null || input.isNone()) {
            return Optional.empty();
        }
        return switch (input.name()) {
            case "water" -> heating(input, "steam", 200, 1, 100, 1.0D);
            case "oil" -> heating(input, "hotoil", 10, 1, 1, 1.0D);
            case "oil_ds" -> heating(input, "hotoil_ds", 10, 1, 1, 1.0D);
            case "crackoil" -> heating(input, "hotcrackoil", 10, 1, 1, 1.0D);
            case "crackoil_ds" -> heating(input, "hotcrackoil_ds", 10, 1, 1, 1.0D);
            case "coolant" -> heating(input, "coolant_hot", 300, 1, 1, 1.0D);
            case "perfluoromethyl" -> heating(input, "perfluoromethyl_hot", 300, 1, 1, 1.0D);
            case "mug" -> heating(input, "mug_hot", 400, 1, 1, 1.0D);
            case "blood" -> heating(input, "blood_hot", 500, 1, 1, 1.0D);
            default -> Optional.empty();
        };
    }

    public static Optional<CoolingStep> turbineStep(HbmFluidDefinition input) {
        if (input == null || input.isNone()) {
            return Optional.empty();
        }
        return switch (input.name()) {
            case "steam" -> cooling(input, "spentsteam", 100, 1, 200, 1.0D);
            case "hotsteam" -> cooling(input, "steam", 1, 10, 2, 1.0D);
            case "superhotsteam" -> cooling(input, "hotsteam", 1, 10, 18, 1.0D);
            case "ultrahotsteam" -> cooling(input, "superhotsteam", 1, 10, 120, 1.0D);
            default -> Optional.empty();
        };
    }

    public static Optional<CoolingStep> condenserStep(HbmFluidDefinition input) {
        if (input == null || input.isNone()) {
            return Optional.empty();
        }
        if (!input.name().equals("spentsteam")) {
            return Optional.empty();
        }
        return cooling(input, "water", 1, 1, 0, 1.0D);
    }

    /** Mirrors the 1.7.10 FT_Coolable entries accepted by TileEntityHeaterHeatex. */
    public static Optional<CoolingStep> heatExchangerCoolingStep(HbmFluidDefinition input) {
        if (input == null || input.isNone()) {
            return Optional.empty();
        }
        return switch (input.name()) {
            case "steam" -> cooling(input, "spentsteam", 100, 1, 200, 0.5D);
            case "hotsteam" -> cooling(input, "steam", 1, 10, 2, 0.5D);
            case "superhotsteam" -> cooling(input, "hotsteam", 1, 10, 18, 0.5D);
            case "ultrahotsteam" -> cooling(input, "superhotsteam", 1, 10, 120, 0.5D);
            case "hotoil" -> cooling(input, "oil", 1, 1, 10, 1.0D);
            case "hotoil_ds" -> cooling(input, "oil_ds", 1, 1, 10, 1.0D);
            case "hotcrackoil" -> cooling(input, "crackoil", 1, 1, 10, 1.0D);
            case "hotcrackoil_ds" -> cooling(input, "crackoil_ds", 1, 1, 10, 1.0D);
            case "coolant_hot" -> cooling(input, "coolant", 1, 1, 300, 1.0D);
            case "perfluoromethyl_hot" -> cooling(input, "perfluoromethyl", 1, 1, 300, 1.0D);
            case "mug_hot" -> cooling(input, "mug", 1, 1, 400, 1.0D);
            case "blood_hot" -> cooling(input, "blood", 1, 1, 500, 1.0D);
            case "heavywater_hot" -> cooling(input, "heavywater", 1, 1, 300, 1.0D);
            case "sodium_hot" -> cooling(input, "sodium", 1, 1, 400, 1.0D);
            case "lead_hot" -> cooling(input, "lead", 1, 1, 680, 1.0D);
            case "thorium_salt_hot" -> cooling(input, "thorium_salt_depleted", 1, 1, 400, 1.0D);
            default -> Optional.empty();
        };
    }

    private static Optional<HeatingStep> heating(
            HbmFluidDefinition input,
            String output,
            int heatReq,
            int amountReq,
            int amountProduced,
            double boilerEfficiency
    ) {
        return HbmFluids.byName(output)
                .map(definition -> new HeatingStep(input, definition, heatReq, amountReq, amountProduced, boilerEfficiency));
    }

    private static Optional<CoolingStep> cooling(
            HbmFluidDefinition input,
            String output,
            int amountReq,
            int amountProduced,
            int heatEnergy,
            double turbineEfficiency
    ) {
        return HbmFluids.byName(output)
                .map(definition -> new CoolingStep(input, definition, amountReq, amountProduced, heatEnergy, turbineEfficiency));
    }

    public record HeatingStep(
            HbmFluidDefinition input,
            HbmFluidDefinition output,
            int heatReq,
            int amountReq,
            int amountProduced,
            double boilerEfficiency
    ) {
    }

    public record CoolingStep(
            HbmFluidDefinition input,
            HbmFluidDefinition output,
            int amountReq,
            int amountProduced,
            int heatEnergy,
            double turbineEfficiency
    ) {
    }
}
