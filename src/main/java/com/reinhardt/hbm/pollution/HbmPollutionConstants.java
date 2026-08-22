package com.reinhardt.hbm.pollution;

public final class HbmPollutionConstants {
    public static final float SOOT_PER_SECOND = 1.0F / 25.0F;
    public static final float HEAVY_METAL_PER_SECOND = 1.0F / 50.0F;
    public static final float POISON_PER_SECOND = 1.0F / 50.0F;
    public static final double MAX_POLLUTION = 10_000.0D;
    public static final double EPSILON = 0.000001D;
    public static final int REGION_SHIFT = 6;
    public static final int REGION_SIZE = 1 << REGION_SHIFT;
    public static final int SOLVE_RATE_TICKS = 60;
    public static final float POISON_DESTRUCTION_THRESHOLD = 15.0F;
    public static final int POISON_DESTRUCTION_COUNT = 5;

    private HbmPollutionConstants() {
    }
}
