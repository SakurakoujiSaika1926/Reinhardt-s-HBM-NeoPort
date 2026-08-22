package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

public final class HbmChunkTickets {
    public static final TicketController ARTILLERY_PROJECTILES =
            new TicketController(ReinhardtsHBM.id("artillery_projectiles"));

    private HbmChunkTickets() {
    }

    public static void register(RegisterTicketControllersEvent event) {
        event.register(ARTILLERY_PROJECTILES);
    }
}
