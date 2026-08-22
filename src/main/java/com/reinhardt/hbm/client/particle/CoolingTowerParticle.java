package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class CoolingTowerParticle extends TextureSheetParticle {
    private static final float STRAFE = 0.075F;
    private static final float ALPHA_MOD = 0.25F;
    private final float baseScale;
    private final float maxScale;
    private final float lift;

    protected CoolingTowerParticle(ClientLevel level, double x, double y, double z, boolean large, SpriteSet sprites) {
        super(level, x, y, z);
        this.baseScale = large ? 1.0F : 0.5F;
        this.maxScale = large ? 10.0F : 4.0F;
        this.lift = large ? 0.5F : 1.0F;
        this.setSprite(sprites.get(this.random));
        float shade = 0.9F + this.random.nextFloat() * 0.05F;
        this.rCol = shade;
        this.gCol = shade;
        this.bCol = shade;
        this.alpha = 0.30F;
        this.quadSize = this.baseScale;
        this.lifetime = large ? 750 + this.random.nextInt(250) : 250 + this.random.nextInt(250);
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float ageScale = (float) this.age / (float) this.lifetime;
        this.alpha = ALPHA_MOD - ageScale * ALPHA_MOD;
        this.quadSize = this.baseScale + (float) Math.pow(this.maxScale * ageScale - this.baseScale, 2);

        this.age++;
        if (this.yd < this.lift) {
            this.yd += 0.01F;
        }

        this.xd += this.random.nextGaussian() * STRAFE * ageScale;
        this.zd += this.random.nextGaussian() * STRAFE * ageScale;
        this.xd += 0.02D * ageScale;
        this.zd -= 0.01D * ageScale;

        if (this.age >= this.lifetime) {
            this.remove();
        }

        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.925D;
        this.yd *= 0.925D;
        this.zd *= 0.925D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new CoolingTowerParticle(level, x, y, z, xSpeed > 0.5D, this.sprites);
        }
    }
}
