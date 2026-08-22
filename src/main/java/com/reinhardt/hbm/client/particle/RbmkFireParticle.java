package com.reinhardt.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class RbmkFireParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected RbmkFireParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int maxAge, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = Math.max(20, maxAge);
        this.quadSize = 1.0F + this.random.nextFloat();
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 0.0F;
        this.hasPhysics = false;
        setOldFrame();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age >= this.lifetime) {
            remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.96D;
        this.yd *= 0.985D;
        this.zd *= 0.96D;
        int fadeTicks = 20;
        float fadeIn = Math.min(1.0F, this.age / (float) fadeTicks);
        float fadeOut = Math.min(1.0F, (this.lifetime - this.age) / (float) fadeTicks);
        this.alpha = Math.max(0.0F, Math.min(fadeIn, fadeOut)) * 0.5F;
        setOldFrame();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private void setOldFrame() {
        int frame = this.age * 5 % 14;
        setSprite(this.sprites.get(frame, 14));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            int maxAge = xSpeed > 1.0D ? (int) Math.round(xSpeed) : 50;
            double motionX = xSpeed > 1.0D ? 0.0D : xSpeed;
            return new RbmkFireParticle(level, x, y, z, motionX, ySpeed, zSpeed, maxAge, this.sprites);
        }
    }
}
