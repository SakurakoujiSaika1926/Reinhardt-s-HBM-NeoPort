package com.reinhardt.hbm.client;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.network.PlayerInformPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class PlayerInformOverlay {
    private static final Map<Integer, Line> LINES = new HashMap<>();

    private PlayerInformOverlay() {
    }

    public static void accept(PlayerInformPayload payload) {
        Component message = payload.args().isEmpty()
                ? Component.literal(payload.message())
                : Component.translatable(payload.message(), payload.args().stream()
                        .map(arg -> arg.startsWith("@") ? Component.translatable(arg.substring(1)) : arg)
                        .toArray());
        LINES.put(payload.line(), new Line(message, payload.color(), System.currentTimeMillis() + payload.lifetimeMillis()));
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || LINES.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        LINES.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        if (LINES.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = 8;
        for (Map.Entry<Integer, Line> entry : LINES.entrySet()) {
            int y = 8 + Math.max(0, entry.getKey() - 100) * 10;
            Line line = entry.getValue();
            graphics.drawString(font, line.message(), x + 1, y + 1, 0x404000, false);
            graphics.drawString(font, line.message(), x, y, line.color(), false);
        }
    }

    private record Line(Component message, int color, long expiresAt) {
    }
}
