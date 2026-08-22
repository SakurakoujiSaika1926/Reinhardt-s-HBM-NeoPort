package com.reinhardt.hbm.blockentity;

import java.util.Map;

/**
 * The inventory and power contract copied from the 1.7.10 machine classes.
 * Machine blocks are registered through one BE type, but their contracts are
 * deliberately kept per machine instead of being inferred from the model.
 */
public record LegacyMachineProfile(
        String id,
        int slots,
        int outputStart,
        long energyCapacity,
        long energyPerTick,
        int processTime,
        boolean menu,
        int[] fluidTankCapacities,
        int[] fillTanks,
        int[] drainTanks,
        boolean energyOutput
) {
    private static final LegacyMachineProfile NONE = new LegacyMachineProfile(
            "unknown", 0, 0, 0L, 0L, 1, false, new int[0], new int[0], new int[0], false
    );

    private static final Map<String, LegacyMachineProfile> PROFILES = Map.ofEntries(
            entry("machine_annihilator", 11, 10, 0L, 0L, 1, true, tanks(2_500_000), indices(0), indices(), false),
            entry("machine_autocrafter", 21, 19, 10_000L, 100L, 1, true, tanks(), indices(), indices(), false),
            entry("machine_autosaw", 0, 0, 0L, 0L, 1, false, tanks(100), indices(0), indices(), false),
            entry("machine_thresher", 0, 0, 0L, 0L, 1, false, tanks(100), indices(0), indices(), false),
            entry("machine_lpw2", 0, 0, 0L, 0L, 1, false, tanks(), indices(), indices(), false),
            entry("machine_conveyor_press", 1, 0, 50_000L, 100L, 20, false, tanks(), indices(), indices(), false),
            entry("machine_forcefield", 3, 3, 1_000_000L, 0L, 1, true, tanks(), indices(), indices(), false),
            entry("machine_missile_assembly", 6, 5, 0L, 0L, 1, true, tanks(), indices(), indices(), false),
            entry("machine_orbus", 6, 6, 0L, 0L, 1, true, tanks(512_000), indices(0), indices(0), false),
            entry("machine_precass", 22, 13, 100_000L, 100L, 200, true, tanks(4_000, 4_000), indices(0), indices(1), false),
            // The 1.7.10 pyro oven owns input/output plus three 50 mB smoke buffers.
            entry("machine_pyrooven", 6, 2, 10_000_000L, 10_000L, 200, true, tanks(24_000, 24_000, 50, 50, 50), indices(0), indices(1, 2, 3, 4), false),
            entry("machine_radar", 10, 0, 100_000L, 500L, 1, true, tanks(), indices(), indices(), false),
            entry("machine_radar_large", 10, 0, 100_000L, 500L, 1, true, tanks(), indices(), indices(), false),
            entry("machine_radgen", 24, 12, 1_000_000L, 0L, 1, true, tanks(), indices(), indices(), true),
            entry("machine_radiolysis", 15, 13, 1_000_000L, 0L, 1, true, tanks(2_000, 2_000, 2_000), indices(0), indices(1, 2), true),
            entry("machine_rtg_grey", 15, 15, 100_000L, 0L, 1, true, tanks(), indices(), indices(), true),
            entry("machine_satlinker", 3, 3, 0L, 0L, 1, true, tanks(), indices(), indices(), false),
            // TileEntitySawmill has no GUI: slot 0 is the hand-fed input and
            // slots 1/2 are collected directly with a right click.
            entry("machine_sawmill", 3, 1, 0L, 0L, 600, false, tanks(), indices(), indices(), false),
            // TileEntityMachineTeleporter has no GUI or inventory; its target
            // is assigned by ItemTeleLink in 1.7.10.
            entry("machine_teleporter", 0, 0, 1_500_000L, 0L, 1, false, tanks(), indices(), indices(), false),
            entry("machine_turbofan", 5, 0, 1_000_000L, 0L, 1, true, tanks(24_000, 24_000, 150, 150, 150), indices(0), indices(1, 2, 3, 4), true)
    );

    private static Map.Entry<String, LegacyMachineProfile> entry(
            String id, int slots, int outputStart, long capacity, long demand, int time,
            boolean menu, int[] tankCapacities, int[] fillTanks, int[] drainTanks, boolean energyOutput
    ) {
        return Map.entry(id, new LegacyMachineProfile(id, slots, outputStart, capacity, demand, time,
                menu, tankCapacities, fillTanks, drainTanks, energyOutput));
    }

    public boolean fluidInput() {
        return this.fillTanks.length > 0;
    }

    public boolean fluidOutput() {
        return this.drainTanks.length > 0;
    }

    private static int[] tanks(int... capacities) {
        return capacities;
    }

    private static int[] indices(int... values) {
        return values;
    }

    public static LegacyMachineProfile forId(String id) {
        return PROFILES.getOrDefault(id, NONE);
    }
}
