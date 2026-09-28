package com.reinhardt.hbm.satellite;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Advances the small persisted satellite registry independently of loaded machines. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class SatelliteEvents {
    private SatelliteEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SatelliteSavedData.get(level).tick(level);
        }
    }
}
