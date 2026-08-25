package com.reinhardt.hbm.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HbmKeyMappings {
    public static final KeyMapping TOGGLE_MAGNET = new KeyMapping(
            "key.reinhardtshbm.toggle_magnet",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.reinhardtshbm"
    );

    private HbmKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MAGNET);
    }
}
