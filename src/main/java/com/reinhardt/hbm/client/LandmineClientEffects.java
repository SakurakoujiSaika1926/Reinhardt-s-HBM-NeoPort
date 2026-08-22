package com.reinhardt.hbm.client;

import com.reinhardt.hbm.network.LandmineEffectPayload;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;

public final class LandmineClientEffects {
    private LandmineClientEffects() {
    }

    public static void accept(LandmineEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        for (int i = 0; i < payload.smokeCount(); i++) {
            minecraft.particleEngine.createParticle(
                    HbmParticleTypes.LANDMINE_SMOKE.get(),
                    payload.x(), payload.y() + 1.5D, payload.z(),
                    random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        }
        for (int i = 0; i < payload.foamCount(); i++) {
            minecraft.particleEngine.createParticle(
                    HbmParticleTypes.LANDMINE_FOAM.get(),
                    payload.x(), payload.y(), payload.z(), 0.0D, 0.0D, 0.0D);
        }
    }
}
