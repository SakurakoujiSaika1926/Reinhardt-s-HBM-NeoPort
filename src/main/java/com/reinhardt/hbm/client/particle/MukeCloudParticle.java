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
import org.jetbrains.annotations.Nullable;

public final class MukeCloudParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float friction;

    private MukeCloudParticle(ClientLevel level, double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        if (ySpeed > 0.0D) {
            this.friction = 0.9F;
            this.lifetime = ySpeed > 0.1D
                    ? 92 + this.random.nextInt(11) + (int) (ySpeed * 20.0D)
                    : 72 + this.random.nextInt(11);
        } else if (ySpeed == 0.0D) {
            this.friction = 0.95F;
            this.lifetime = 52 + this.random.nextInt(11);
        } else {
            this.friction = 0.85F;
            this.lifetime = 122 + this.random.nextInt(31);
            this.age = 80;
        }
        this.quadSize = 3.0F;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
        this.setSprite(this.sprites.get(0, 25));
    }

    @Override
    public void tick() {
        this.hasPhysics = this.age > 2;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime - 2) {
            remove();
            return;
        }
        move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        int frame = Math.min(24, this.age * 25 / Math.max(1, this.lifetime));
        this.setSprite(this.sprites.get(frame, 25));
        super.render(buffer, camera, partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new MukeCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
