package com.reinhardt.hbm.client;

import com.reinhardt.hbm.entity.FireworksEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;

/** Client-only implementation of the colored 1.7.10 firework spark burst. */
public final class FireworksClientEffects {
    private FireworksClientEffects() {
    }

    public static void spawnBurst(FireworksEntity fireworks) {
        Minecraft minecraft = Minecraft.getInstance();
        int color = fireworks.color();
        float red = ((color >> 16) & 0xff) / 255.0F;
        float green = ((color >> 8) & 0xff) / 255.0F;
        float blue = (color & 0xff) / 255.0F;

        for (int i = 0; i < 50; i++) {
            Particle particle = minecraft.particleEngine.createParticle(
                    ParticleTypes.FIREWORK,
                    fireworks.getX(), fireworks.getY(), fireworks.getZ(),
                    0.4D * fireworks.getRandom().nextGaussian(),
                    0.4D * fireworks.getRandom().nextGaussian(),
                    0.4D * fireworks.getRandom().nextGaussian());
            if (particle != null) {
                particle.setColor(red, green, blue);
            }
        }
    }
}
