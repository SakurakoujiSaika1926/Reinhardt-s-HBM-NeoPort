package com.reinhardt.hbm.client;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.network.TomImpactSyncPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Client-side atmospheric part of the old WorldProviderNTM impact state. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class TomImpactClientEffects {
    private static float targetDust;
    private static float targetFire;
    private static float dust;
    private static float fire;

    private TomImpactClientEffects() { }

    public static void accept(TomImpactSyncPayload payload) {
        targetDust = payload.impact() ? payload.dust() : 0.0F;
        targetFire = payload.impact() ? payload.fire() : 0.0F;
    }

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide) return;
        dust = approach(dust, targetDust, 0.01F);
        fire = approach(fire, targetFire, 0.01F);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        targetDust = targetFire = dust = fire = 0.0F;
    }

    @SubscribeEvent
    public static void tintFog(ViewportEvent.ComputeFogColor event) {
        if (dust <= 0.0F && fire <= 0.0F) return;
        float red = event.getRed();
        float green = event.getGreen() * (1.0F - dust * 0.5F);
        float blue = event.getBlue() * (1.0F - dust);
        if (fire > 0.0F) {
            float factor = Math.max(1.0F - dust * 2.0F, 0.0F);
            event.setRed(red * factor);
            event.setGreen(green * factor);
            event.setBlue(blue * factor);
        } else {
            event.setRed(red * (1.0F - dust));
            event.setGreen(green * (1.0F - dust));
            event.setBlue(blue * (1.0F - dust));
        }
    }

    private static float approach(float value, float target, float step) {
        if (Math.abs(value - target) <= step) return target;
        return value < target ? value + step : value - step;
    }
}
