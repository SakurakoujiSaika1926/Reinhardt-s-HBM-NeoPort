package com.reinhardt.hbm.fluid;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record HbmFluidFuelProperties(long flammableHeatEnergy, CombustibleFuelGrade combustibleGrade, long combustionEnergy) {
    private static final Map<String, HbmFluidFuelProperties> CALCULATED_112 = calculated112Defaults();

    public static HbmFluidFuelProperties forFluid(HbmFluidDefinition definition) {
        if (definition == null || definition.isNone()) {
            return empty();
        }
        HbmFluidFuelProperties calculated = CALCULATED_112.get(definition.name());
        if (calculated != null) {
            return calculated;
        }
        return fromRawTraits(definition.rawTraits());
    }

    public static HbmFluidFuelProperties empty() {
        return new HbmFluidFuelProperties(0L, null, 0L);
    }

    public boolean isFlammable() {
        return flammableHeatEnergy > 0L;
    }

    public boolean isCombustible() {
        return combustibleGrade != null && combustionEnergy > 0L;
    }

    public int flammableHeatPerMillibucket() {
        if (flammableHeatEnergy <= 0L) {
            return 0;
        }
        return Math.max(1, (int) (flammableHeatEnergy / 1_000L));
    }

    private static HbmFluidFuelProperties fromRawTraits(String rawTraits) {
        long flammable = 0L;
        CombustibleFuelGrade grade = null;
        long combustible = 0L;
        if (rawTraits != null && !rawTraits.isBlank()) {
            for (String token : rawTraits.split("\\|")) {
                String trimmed = token.trim();
                if (trimmed.startsWith("FLAMMABLE:")) {
                    flammable = Math.max(flammable, parseLongPart(trimmed, 1));
                } else if (trimmed.startsWith("COMBUSTIBLE:")) {
                    String[] parts = trimmed.split(":");
                    if (parts.length >= 3) {
                        CombustibleFuelGrade parsed = CombustibleFuelGrade.parse(parts[1]);
                        long energy = parseLong(parts[2]);
                        if (parsed != null && energy > 0L) {
                            grade = parsed;
                            combustible = energy;
                        }
                    }
                }
            }
        }
        return new HbmFluidFuelProperties(flammable, grade, combustible);
    }

    private static Map<String, HbmFluidFuelProperties> calculated112Defaults() {
        Map<String, HbmFluidFuelProperties> map = new HashMap<>();

        long baseline = 100_000L;
        double demandVeryLow = 0.5D;
        double demandLow = 1.0D;
        double demandMedium = 1.5D;
        double demandHigh = 2.0D;
        double complexityRefinery = 1.1D;
        double complexityFraction = 1.05D;
        double complexityCracking = 1.25D;
        double complexityCoker = 1.25D;
        double complexityChemplant = 1.1D;
        double complexityLubed = 1.15D;
        double complexityLeaded = 1.5D;
        double complexityVacuum = 3.0D;
        double complexityReform = 2.5D;
        double complexityHydro = 2.0D;
        double flammabilityLow = 0.25D;
        double flammabilityNormal = 1.0D;
        double flammabilityHigh = 2.0D;

        registerCalculatedFuel(map, "oil", baseline / 1D * flammabilityLow * demandLow, 0D, null);
        registerCalculatedFuel(map, "oil_ds", baseline / 1D * flammabilityLow * demandLow * complexityHydro, 0D, null);
        registerCalculatedFuel(map, "crackoil", baseline / 1D * flammabilityLow * demandLow * complexityCracking, 0D, null);
        registerCalculatedFuel(map, "crackoil_ds", baseline / 1D * flammabilityLow * demandLow * complexityCracking * complexityHydro, 0D, null);
        registerCalculatedFuel(map, "oil_coker", baseline / 1D * flammabilityLow * demandLow * complexityCoker, 0D, null);
        registerCalculatedFuel(map, "gas", baseline / 1D * flammabilityNormal * demandVeryLow, 1.5D, CombustibleFuelGrade.GAS);
        registerCalculatedFuel(map, "gas_coker", baseline / 1D * flammabilityNormal * demandVeryLow * complexityCoker, 1.5D, CombustibleFuelGrade.GAS);
        registerCalculatedFuel(map, "heavyoil", baseline / 0.5D * flammabilityLow * demandLow * complexityRefinery, 1.25D, CombustibleFuelGrade.LOW);
        registerCalculatedFuel(map, "smear", baseline / 0.35D * flammabilityLow * demandLow * complexityRefinery * complexityFraction, 1.25D, CombustibleFuelGrade.LOW);
        registerCalculatedFuel(map, "reclaimed", baseline / 0.28D * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant, 1.25D, CombustibleFuelGrade.LOW);
        registerCalculatedFuel(map, "petroil", baseline / 0.28D * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant * complexityLubed, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "petroil_leaded", baseline / 0.28D * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant * complexityLubed * complexityLeaded, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "heatingoil", baseline / 0.31D * flammabilityNormal * demandLow * complexityRefinery * complexityFraction * complexityFraction, 1.25D, CombustibleFuelGrade.LOW);
        registerCalculatedFuel(map, "naphtha", baseline / 0.25D * flammabilityLow * demandLow * complexityRefinery, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "naphtha_ds", baseline / 0.25D * flammabilityLow * demandLow * complexityRefinery * complexityHydro, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "naphtha_crack", baseline / 0.40D * flammabilityLow * demandLow * complexityRefinery * complexityCracking, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "naphtha_coker", baseline / 0.25D * flammabilityLow * demandLow * complexityCoker, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "gasoline", baseline / 0.20D * flammabilityNormal * demandLow * complexityRefinery * complexityChemplant, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "gasoline_leaded", baseline / 0.20D * flammabilityNormal * demandLow * complexityRefinery * complexityChemplant * complexityLeaded, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "diesel", baseline / 0.21D * flammabilityNormal * demandLow * complexityRefinery * complexityFraction, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "diesel_crack", baseline / 0.28D * flammabilityNormal * demandLow * complexityRefinery * complexityCracking * complexityFraction, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "lightoil", baseline / 0.15D * flammabilityNormal * demandHigh * complexityRefinery, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "lightoil_ds", baseline / 0.15D * flammabilityNormal * demandHigh * complexityRefinery * complexityHydro, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "lightoil_crack", baseline / 0.30D * flammabilityNormal * demandHigh * complexityRefinery * complexityCracking, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "kerosene", baseline / 0.09D * flammabilityNormal * demandHigh * complexityRefinery * complexityFraction, 1.5D, CombustibleFuelGrade.AERO);
        registerCalculatedFuel(map, "petroleum", baseline / 0.10D * flammabilityNormal * demandMedium * complexityRefinery, 1.5D, CombustibleFuelGrade.GAS);
        registerCalculatedFuel(map, "aromatics", baseline / 0.15D * flammabilityLow * demandHigh * complexityRefinery * complexityCracking, 0D, null);
        registerCalculatedFuel(map, "unsaturateds", baseline / 0.15D * flammabilityHigh * demandHigh * complexityRefinery * complexityCracking, 0D, null);
        registerCalculatedFuel(map, "lpg", baseline / 0.1D * flammabilityNormal * demandMedium * complexityRefinery * complexityChemplant, 2.5D, CombustibleFuelGrade.HIGH);

        long keroseneHeat = map.get("kerosene").flammableHeatEnergy();
        registerCalculatedFuel(map, "nitan", keroseneHeat * 25D, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "balefire", keroseneHeat * 100D, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "heavyoil_vacuum", baseline / 0.4D * flammabilityLow * demandLow * complexityVacuum, 1.25D, CombustibleFuelGrade.LOW);
        registerCalculatedFuel(map, "reformate", baseline / 0.25D * flammabilityNormal * demandHigh * complexityVacuum, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "lightoil_vacuum", baseline / 0.20D * flammabilityNormal * demandHigh * complexityVacuum, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "sourgas", baseline / 0.15D * flammabilityLow * demandVeryLow * complexityVacuum, 0D, null);
        registerCalculatedFuel(map, "xylene", baseline / 0.15D * flammabilityNormal * demandMedium * complexityVacuum * complexityFraction, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "heatingoil_vacuum", baseline / 0.24D * flammabilityNormal * demandLow * complexityVacuum * complexityFraction, 1.25D, CombustibleFuelGrade.LOW);
        registerCalculatedFuel(map, "diesel_reform", map.get("diesel").flammableHeatEnergy() * complexityReform, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "diesel_crack_reform", map.get("diesel_crack").flammableHeatEnergy() * complexityReform, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "kerosene_reform", keroseneHeat * complexityReform, 1.5D, CombustibleFuelGrade.AERO);
        registerCalculatedFuel(map, "reformgas", baseline / 0.06D * flammabilityHigh * demandLow * complexityVacuum * complexityFraction, 1.5D, CombustibleFuelGrade.GAS);

        int coalHeat = 400_000;
        registerCalculatedFuel(map, "coaloil", coalHeat * (1000D / 100D) * flammabilityLow * demandLow * complexityChemplant, 0D, null);
        long coaloil = map.get("coaloil").flammableHeatEnergy();
        registerCalculatedFuel(map, "coalgas", coaloil / 0.3D * flammabilityNormal * demandMedium * complexityChemplant * complexityFraction, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "coalgas_leaded", coaloil / 0.3D * flammabilityNormal * demandMedium * complexityChemplant * complexityFraction * complexityLeaded, 1.5D, CombustibleFuelGrade.MEDIUM);
        registerCalculatedFuel(map, "ethanol", 275_000D, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "biogas", 250_000D * flammabilityLow, 1.25D, CombustibleFuelGrade.GAS);
        registerCalculatedFuel(map, "biofuel", 500_000D, 2.5D, CombustibleFuelGrade.HIGH);
        registerCalculatedFuel(map, "woodoil", 110_000D, 0D, null);
        registerCalculatedFuel(map, "coalcreosote", 250_000D, 0D, null);
        registerCalculatedFuel(map, "fishoil", 75_000D, 0D, null);
        registerCalculatedFuel(map, "sunfloweroil", 50_000D, 0D, null);
        registerCalculatedFuel(map, "solvent", 100_000D, 0D, null);
        registerCalculatedFuel(map, "radiosolvent", 150_000D, 0D, null);
        registerCalculatedFuel(map, "syngas", coalHeat * (1000D / 100D) * flammabilityLow * demandLow * complexityChemplant * 1.5D, 1.25D, CombustibleFuelGrade.GAS);
        registerCalculatedFuel(map, "oxyhydrogen", 5_000D, 3D, CombustibleFuelGrade.GAS);

        return Map.copyOf(map);
    }

    private static void registerCalculatedFuel(Map<String, HbmFluidFuelProperties> map, String fluidName, double base, double combustMult, CombustibleFuelGrade grade) {
        long flammable = round((long) base);
        long combustible = round((long) (base * combustMult));
        map.put(fluidName.toLowerCase(Locale.ROOT), new HbmFluidFuelProperties(
                flammable,
                combustible > 0L && grade != null ? grade : null,
                combustible > 0L && grade != null ? combustible : 0L
        ));
    }

    private static long round(long value) {
        if (value > 10_000_000L) {
            return value - value % 100_000L;
        }
        if (value > 1_000_000L) {
            return value - value % 10_000L;
        }
        if (value > 100_000L) {
            return value - value % 1_000L;
        }
        if (value > 10_000L) {
            return value - value % 100L;
        }
        if (value > 1_000L) {
            return value - value % 10L;
        }
        return value;
    }

    private static long parseLongPart(String token, int index) {
        String[] parts = token.split(":");
        return parts.length > index ? parseLong(parts[index]) : 0L;
    }

    private static long parseLong(String raw) {
        if (raw == null) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(raw.trim()));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
