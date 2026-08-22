package com.reinhardt.hbm.blockentity;

public interface HeatSourceBlockEntity {
    int getHeatStored();

    void useHeat(int amount);
}
