package com.reinhardt.hbm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.Random;

/** Direct ParticleSmokePlume / ParticleExSmoke motion and six-billboard geometry. */
public final class LauncherSmokeParticle extends TextureSheetParticle {
    private final long seed;
    private final boolean plume;
    private float scale = 0.25F;

    private LauncherSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy,
                                  double dz, SpriteSet sprites, boolean plume) {
        super(level, x, y, z);
        this.plume = plume;
        this.seed = random.nextLong();
        this.lifetime = plume ? 80 + random.nextInt(20) : 100 + random.nextInt(40);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.hasPhysics = true;
        setSprite(sprites.get(0, 1));
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        alpha = 1.0F - (float) age / lifetime;
        float previous = scale;
        if (plume) scale = 0.25F + (float) age / lifetime * 2.0F;
        if (++age >= lifetime) remove();
        if (plume) {
            double speed = Math.sqrt(xd * xd + yd * yd + zd * zd);
            double oldY = y;
            double vertical = yd + scale - previous;
            move(xd, vertical, zd);
            if (Math.abs((y - oldY) - vertical) > 1.0E-7D) yd = speed;
            xd *= 0.925D; yd *= 0.925D; zd *= 0.925D;
        } else {
            xd *= 0.7599999785423279D;
            yd *= 0.7599999785423279D;
            zd *= 0.7599999785423279D;
            move(xd, yd, zd);
        }
    }

    @Override public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        double px = x, py = y, pz = z, ox = xo, oy = yo, oz = zo;
        Random stable = new Random(seed);
        for (int i = 0; i < 6; i++) {
            rCol = gCol = bCol = plume ? stable.nextFloat() * 0.75F + 0.1F : stable.nextFloat() * 0.25F + 0.25F;
            quadSize = plume ? scale : stable.nextFloat() + 0.5F;
            double dx = plume ? stable.nextGaussian() * 0.5D * scale : (stable.nextGaussian() - 1) * 0.75D;
            double dy = plume ? stable.nextGaussian() * 0.5D * scale : (stable.nextGaussian() - 1) * 0.75D;
            double dz = plume ? stable.nextGaussian() * 0.5D * scale : (stable.nextGaussian() - 1) * 0.75D;
            x = px + dx; y = py + dy; z = pz + dz;
            xo = ox + dx; yo = oy + dy; zo = oz + dz;
            super.render(buffer, camera, partialTick);
        }
        x = px; y = py; z = pz; xo = ox; yo = oy; zo = oz;
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    @Override protected int getLightColor(float tick) { return plume ? LightTexture.FULL_BRIGHT : super.getLightColor(tick); }

    public record Provider(SpriteSet sprites, boolean plume) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double dx, double dy, double dz) {
            return new LauncherSmokeParticle(level, x, y, z, dx, dy, dz, sprites, plume);
        }
    }
}
