package com.reinhardt.hbm.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.network.PollutionSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class PollutionFogOverlay {
    private static float targetSoot;
    private static float renderSoot;

    private PollutionFogOverlay() {
    }

    public static void accept(PollutionSyncPayload payload) {
        targetSoot = (float) payload.sootValue();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        targetSoot = 0.0F;
        renderSoot = 0.0F;
    }

    @SubscribeEvent
    public static void onClientLevelTick(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide) {
            return;
        }
        if (!HbmConfig.ENABLE_SOOT_FOG.get()) {
            targetSoot = 0.0F;
            renderSoot = 0.0F;
            return;
        }
        float step = 0.05F;
        if (Math.abs(renderSoot - targetSoot) < step) {
            renderSoot = targetSoot;
        } else if (renderSoot < targetSoot) {
            renderSoot += step;
        } else {
            renderSoot -= step;
        }
    }

    @SubscribeEvent
    public static void thickenFog(ViewportEvent.RenderFog event) {
        if (!HbmConfig.ENABLE_SOOT_FOG.get() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        float soot = renderSoot - HbmConfig.POLLUTION_SOOT_FOG_THRESHOLD.get().floatValue();
        if (soot <= 0.0F) {
            return;
        }
        int renderDistance = Minecraft.getInstance().options.renderDistance().get();
        float farPlaneDistance = renderDistance * 16.0F;
        float fogDistance = farPlaneDistance / (1.0F + soot * 5.0F / HbmConfig.POLLUTION_SOOT_FOG_DIVISOR.get().floatValue());
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), fogDistance));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tintFog(ViewportEvent.ComputeFogColor event) {
        if (!HbmConfig.ENABLE_SOOT_FOG.get()) {
            return;
        }
        float soot = renderSoot - HbmConfig.POLLUTION_SOOT_FOG_THRESHOLD.get().floatValue();
        if (soot <= 0.0F) {
            return;
        }
        float sootColor = 0.15F;
        float interpolation = Math.min(soot / HbmConfig.POLLUTION_SOOT_FOG_DIVISOR.get().floatValue(), 1.0F);
        event.setRed(event.getRed() * (1.0F - interpolation) + sootColor * interpolation);
        event.setGreen(event.getGreen() * (1.0F - interpolation) + sootColor * interpolation);
        event.setBlue(event.getBlue() * (1.0F - interpolation) + sootColor * interpolation);
    }
}
