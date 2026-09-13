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
    /** 1.7.10 bismuth armour dash trigger. */
    public static final KeyMapping ARMOR_DASH = new KeyMapping(
            "key.reinhardtshbm.armor_dash",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.reinhardtshbm"
    );
    /** Exact 1.7.10 RBMK crane controls (arrow keys plus Enter). */
    public static final KeyMapping CRANE_UP = craneKey("crane_move_up", GLFW.GLFW_KEY_UP);
    public static final KeyMapping CRANE_DOWN = craneKey("crane_move_down", GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping CRANE_LEFT = craneKey("crane_move_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping CRANE_RIGHT = craneKey("crane_move_right", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping CRANE_LOAD = craneKey("crane_load", GLFW.GLFW_KEY_ENTER);

    private static KeyMapping craneKey(String name, int keyCode) {
        return new KeyMapping(
                "key.reinhardtshbm." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                keyCode,
                "key.categories.reinhardtshbm"
        );
    }

    private HbmKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MAGNET);
        event.register(ARMOR_DASH);
        event.register(CRANE_UP);
        event.register(CRANE_DOWN);
        event.register(CRANE_LEFT);
        event.register(CRANE_RIGHT);
        event.register(CRANE_LOAD);
    }
}
