package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.util.LegacyFurnaceFuels;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

/** Registers the 1.7.10 HBM furnace fuel table with NeoForge's fuel lookup. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class LegacyFurnaceFuelEvents {
    private LegacyFurnaceFuelEvents() {
    }

    @SubscribeEvent
    public static void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        int burnTime = LegacyFurnaceFuels.burnTime(event.getItemStack());
        if (burnTime > 0) {
            event.setBurnTime(burnTime);
        }
    }
}
