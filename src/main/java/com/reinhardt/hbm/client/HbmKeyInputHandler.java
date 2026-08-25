package com.reinhardt.hbm.client;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.network.ToggleMagnetPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class HbmKeyInputHandler {
    private HbmKeyInputHandler() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        while (HbmKeyMappings.TOGGLE_MAGNET.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleMagnetPayload());
        }
    }
}
