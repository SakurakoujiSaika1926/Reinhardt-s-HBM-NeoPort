package com.reinhardt.hbm.api.entity;

public interface LegacyRadarDetectable {
    int TIER0 = 0;
    int TIER1 = 1;
    int TIER2 = 2;
    int TIER3 = 3;
    int TIER4 = 4;
    int TIER10 = 5;
    int TIER10_15 = 6;
    int TIER15 = 7;
    int TIER15_20 = 8;
    int TIER20 = 9;
    int TIER_AB = 10;
    int PLAYER = 11;
    int ARTY = 12;
    int SPECIAL = 13;

    String translationKey();

    int blipLevel();

    boolean canBeSeenBy(Object radar);

    boolean paramsApplicable(RadarScanParams params);

    boolean suppliesRedstone(RadarScanParams params);

    record RadarScanParams(boolean scanMissiles, boolean scanShells, boolean scanPlayers, boolean smartMode) {
    }
}
