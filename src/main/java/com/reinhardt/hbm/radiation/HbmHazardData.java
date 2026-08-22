package com.reinhardt.hbm.radiation;

public record HbmHazardData(
        double radiation,
        double contaminating,
        double digamma,
        double hot,
        double blinding
) {
    public static final HbmHazardData EMPTY = new HbmHazardData(0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    public boolean isEmpty() {
        return radiation <= 0.0D && contaminating <= 0.0D && digamma <= 0.0D && hot <= 0.0D && blinding <= 0.0D;
    }

    public HbmHazardData multiply(double multiplier) {
        if (multiplier == 1.0D || isEmpty()) {
            return this;
        }
        return new HbmHazardData(
                radiation * multiplier,
                contaminating * multiplier,
                digamma * multiplier,
                hot * multiplier,
                blinding * multiplier
        );
    }

    public HbmHazardData plus(HbmHazardData other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }
        return new HbmHazardData(
                this.radiation + other.radiation,
                this.contaminating + other.contaminating,
                this.digamma + other.digamma,
                this.hot + other.hot,
                this.blinding + other.blinding
        );
    }
}
