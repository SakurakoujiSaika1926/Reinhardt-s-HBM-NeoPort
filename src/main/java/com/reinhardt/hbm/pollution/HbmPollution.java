package com.reinhardt.hbm.pollution;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class HbmPollution {
    private static final double SOOT_UNREFINED_OIL = HbmPollutionConstants.SOOT_PER_SECOND * 0.1D;
    private static final double SOOT_REFINED_OIL = HbmPollutionConstants.SOOT_PER_SECOND * 0.025D;
    private static final double SOOT_GAS = HbmPollutionConstants.SOOT_PER_SECOND * 0.005D;
    private static final double LEAD_FUEL = HbmPollutionConstants.HEAVY_METAL_PER_SECOND * 0.025D;
    private static final double POISON_OIL = HbmPollutionConstants.POISON_PER_SECOND * 0.0025D;
    private static final double POISON_EXTREME = HbmPollutionConstants.POISON_PER_SECOND * 0.025D;
    private static final double POISON_MINOR = HbmPollutionConstants.POISON_PER_SECOND * 0.001D;
    private static final Set<String> P_OIL = Set.of(
            "oil", "hotoil", "heavyoil", "bitumen", "smear", "heatingoil", "lubricant",
            "crackoil", "coaloil", "hotcrackoil", "woodoil", "coalcreosote",
            "heavyoil_vacuum", "heatingoil_vacuum", "oil_coker", "naphtha_coker",
            "oil_ds", "hotoil_ds", "crackoil_ds", "hotcrackoil_ds"
    );
    private static final Set<String> P_FUEL = Set.of(
            "reclaimed", "petroil", "naphtha", "diesel", "lightoil", "kerosene",
            "biofuel", "nitan", "balefire", "gasoline", "coalgas", "ethanol",
            "naphtha_crack", "lightoil_crack", "diesel_crack", "reformate",
            "lightoil_vacuum", "xylene", "diesel_reform", "diesel_crack_reform",
            "kerosene_reform", "fishoil", "sunfloweroil", "naphtha_ds", "lightoil_ds"
    );
    private static final Set<String> P_FUEL_LEADED = Set.of(
            "petroil_leaded", "gasoline_leaded", "coalgas_leaded"
    );
    private static final Set<String> P_GAS = Set.of(
            "gas", "petroleum", "biogas", "aromatics", "unsaturateds", "reformgas", "gas_coker"
    );
    private static final Set<String> P_LIQUID_GAS = Set.of("lpg");

    public enum ReleaseType {
        SPILL,
        BURN,
        VOID
    }

    private HbmPollution() {
    }

    public static void increment(Level level, BlockPos pos, HbmPollutionType type, double amount) {
        if (!(level instanceof ServerLevel serverLevel) || !HbmConfig.ENABLE_POLLUTION.get()) {
            return;
        }
        HbmPollutionData.get(serverLevel).increment(pos, type, amount, HbmConfig.pollutionMultiplier());
    }

    public static void emitSmoke(Level level, BlockPos pos, HbmPollutionType type, double amount, double pollutionModifier) {
        increment(level, pos, type, amount * pollutionModifier);
    }

    public static void polluteFluid(Level level, BlockPos pos, HbmFluidDefinition fluid, ReleaseType releaseType, double millibuckets) {
        if (fluid == null || fluid.isNone() || releaseType == ReleaseType.VOID || millibuckets <= 0.0D) {
            return;
        }
        Map<HbmPollutionType, Double> map = releaseType == ReleaseType.BURN ? burnMap(fluid) : releaseMap(fluid);
        for (Map.Entry<HbmPollutionType, Double> entry : map.entrySet()) {
            increment(level, pos, entry.getKey(), entry.getValue() * millibuckets);
        }
    }

    public static void polluteFluid(Level level, BlockPos pos, String fluidName, ReleaseType releaseType, double millibuckets) {
        HbmFluids.byName(fluidName.toLowerCase(Locale.ROOT)).ifPresent(fluid -> polluteFluid(level, pos, fluid, releaseType, millibuckets));
    }

    public static void bufferedPollute(Level level, BlockPos pos, HbmPollutionType type, double amount, SmokeBuffer buffer) {
        if (level == null || pos == null || amount <= 0.0D) {
            return;
        }
        int fluidAmount = (int) Math.ceil(amount * 100.0D);
        int overflow = buffer.add(type, fluidAmount);
        if (overflow > 0) {
            increment(level, pos, type, overflow / 100.0D);
            if (level.random.nextInt(3) == 0) {
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1F, 1.5F);
            }
        }
    }

    public static void bufferedLegacyPollute(Level level, BlockPos pos, HbmPollutionType type, double amount, Function<HbmPollutionType, HbmFluidTank> tankResolver) {
        if (level == null || pos == null || amount <= 0.0D || tankResolver == null) {
            return;
        }
        HbmFluidTank tank = tankResolver.apply(type);
        if (tank == null) {
            return;
        }
        int fluidAmount = (int) Math.ceil(amount * 100.0D);
        int accepted = tank.fill(smokeFluid(type), fluidAmount, false);
        int overflow = fluidAmount - accepted;
        if (overflow > 0) {
            increment(level, pos, type, overflow / 100.0D);
            if (level.random.nextInt(3) == 0) {
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1F, 1.5F);
            }
        }
    }

    public static void bufferedLegacyPolluteFluid(Level level, BlockPos pos, HbmFluidDefinition fluid, ReleaseType releaseType, double millibuckets, Function<HbmPollutionType, HbmFluidTank> tankResolver) {
        if (fluid == null || fluid.isNone() || releaseType == ReleaseType.VOID || tankResolver == null || millibuckets <= 0.0D) {
            return;
        }
        Map<HbmPollutionType, Double> map = releaseType == ReleaseType.BURN ? burnMap(fluid) : releaseMap(fluid);
        for (Map.Entry<HbmPollutionType, Double> entry : map.entrySet()) {
            bufferedLegacyPollute(level, pos, entry.getKey(), entry.getValue() * millibuckets, tankResolver);
        }
    }

    public static boolean sendSmoke(Level level, BlockPos sourcePos, BlockPos target, Direction side, HbmFluidTank... tanks) {
        if (level == null || level.isClientSide || sourcePos == null || target == null || side == null || tanks == null) {
            return false;
        }
        boolean moved = false;
        for (HbmFluidTank tank : tanks) {
            if (tank == null || tank.amount() <= 0 || tank.type().isNone()) {
                continue;
            }
            FluidStack stack = HbmFluids.toNeoStack(tank.type(), tank.amount());
            int accepted = HbmFluidNetworks.fillInto(level, target, side, stack, sourcePos, true);
            if (accepted > 0) {
                tank.drain(tank.type(), accepted, false);
                moved = true;
            }
        }
        return moved;
    }

    public static HbmFluidDefinition smokeFluid(HbmPollutionType type) {
        return switch (type) {
            case HEAVYMETAL -> HbmFluids.byName("smoke_leaded").orElse(HbmFluids.none());
            case POISON -> HbmFluids.byName("smoke_poison").orElse(HbmFluids.none());
            case SOOT, FALLOUT -> HbmFluids.byName("smoke").orElse(HbmFluids.none());
        };
    }

    public static Map<HbmPollutionType, Double> burnMap(HbmFluidDefinition fluid) {
        EnumMap<HbmPollutionType, Double> map = new EnumMap<>(HbmPollutionType.class);
        String name = fluid.name();
        if (P_OIL.contains(name)) {
            map.put(HbmPollutionType.SOOT, SOOT_UNREFINED_OIL);
        } else if (P_FUEL.contains(name)) {
            map.put(HbmPollutionType.SOOT, SOOT_REFINED_OIL);
        } else if (P_FUEL_LEADED.contains(name)) {
            map.put(HbmPollutionType.SOOT, SOOT_REFINED_OIL);
            map.put(HbmPollutionType.HEAVYMETAL, LEAD_FUEL);
        } else if (P_GAS.contains(name)) {
            map.put(HbmPollutionType.SOOT, SOOT_GAS);
        } else if (P_LIQUID_GAS.contains(name)) {
            map.put(HbmPollutionType.SOOT, SOOT_GAS * 2.0D);
        } else if (name.equals("flue") || name.equals("sourgas")) {
            map.put(HbmPollutionType.SOOT, SOOT_GAS);
        }
        return map;
    }

    public static Map<HbmPollutionType, Double> releaseMap(HbmFluidDefinition fluid) {
        EnumMap<HbmPollutionType, Double> map = new EnumMap<>(HbmPollutionType.class);
        String name = fluid.name();
        if (P_OIL.contains(name) || P_FUEL.contains(name) || P_FUEL_LEADED.contains(name) || P_GAS.contains(name)) {
            map.put(HbmPollutionType.POISON, POISON_OIL);
        }
        if (P_FUEL_LEADED.contains(name)) {
            map.put(HbmPollutionType.HEAVYMETAL, LEAD_FUEL * 0.1D);
        }
        if (name.equals("flue")) {
            map.put(HbmPollutionType.SOOT, SOOT_GAS * 25.0D);
        }
        if (name.equals("carbondioxide") || name.equals("fullerene")) {
            map.put(HbmPollutionType.POISON, POISON_MINOR);
        }
        if (name.equals("watz") || name.equals("redmud") || name.equals("nitric_acid") || name.equals("sourgas") || name.equals("phosgene") || name.equals("mustardgas")) {
            map.put(HbmPollutionType.POISON, POISON_EXTREME);
        }
        return map;
    }

    public static final class SmokeBuffer {
        private final int capacity;
        private final EnumMap<HbmPollutionType, Integer> stored = new EnumMap<>(HbmPollutionType.class);

        public SmokeBuffer(int capacity) {
            this.capacity = Math.max(0, capacity);
        }

        public int add(HbmPollutionType type, int amount) {
            int current = stored.getOrDefault(type, 0);
            int next = current + Math.max(0, amount);
            int overflow = Math.max(0, next - capacity);
            stored.put(type, Math.min(capacity, next));
            return overflow;
        }
    }
}
