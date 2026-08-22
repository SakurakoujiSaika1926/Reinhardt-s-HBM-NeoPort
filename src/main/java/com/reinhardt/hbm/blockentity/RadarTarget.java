package com.reinhardt.hbm.blockentity;

/** Network-safe snapshot of the 1.7.10 RadarEntry fields. */
public record RadarTarget(String name, int blipLevel, int x, int y, int z, int entityId, boolean redstone) {
}
