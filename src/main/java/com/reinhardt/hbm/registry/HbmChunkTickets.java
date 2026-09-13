package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

public final class HbmChunkTickets {
    public static final TicketController LAUNCHER_MISSILES = new TicketController(ReinhardtsHBM.id("launcher_missiles"));
    public static final TicketController ARTILLERY_PROJECTILES =
            new TicketController(ReinhardtsHBM.id("artillery_projectiles"));
    public static final TicketController DELIVERY_DRONES =
            new TicketController(ReinhardtsHBM.id("delivery_drones"));

    private HbmChunkTickets() {
    }

    public static void register(RegisterTicketControllersEvent event) {
        event.register(LAUNCHER_MISSILES);
        event.register(ARTILLERY_PROJECTILES);
        event.register(DELIVERY_DRONES);
    }
}
