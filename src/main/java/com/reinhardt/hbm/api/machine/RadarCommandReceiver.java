package com.reinhardt.hbm.api.machine;

import net.minecraft.world.entity.Entity;

/** Direct modern equivalent of 1.7.10 IRadarCommandReceiver. */
public interface RadarCommandReceiver {
    boolean sendCommandPosition(int x, int y, int z);

    boolean sendCommandEntity(Entity target);
}
