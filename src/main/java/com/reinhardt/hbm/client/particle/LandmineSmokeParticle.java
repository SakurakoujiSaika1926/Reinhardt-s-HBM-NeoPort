package com.reinhardt.hbm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public final class LandmineSmokeParticle extends TextureSheetParticle {
    private final long renderSeed;

    private LandmineSmokeParticle(ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        this.renderSeed = this.random.nextLong();
        this.lifetime = 100 + this.random.nextInt(40);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.hasPhysics = false;
        this.setSprite(sprites.get(0, 1));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
        if (++this.age >= this.lifetime) {
            remove();
            return;
        }
        this.xd *= 0.7599999785D;
        this.yd *= 0.7599999785D;
        this.zd *= 0.7599999785D;
        move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        double baseX = this.x;
        double baseY = this.y;
        double baseZ = this.z;
        double oldX = this.xo;
        double oldY = this.yo;
        double oldZ = this.zo;
        float oldSize = this.quadSize;
        float oldRed = this.rCol;
        float oldGreen = this.gCol;
        float oldBlue = this.bCol;
        float oldAlpha = this.alpha;
        Random stable = new Random(this.renderSeed);
        for (int i = 0; i < 6; i++) {
            float shade = stable.nextFloat() * 0.25F + 0.25F;
            this.rCol = shade;
            this.gCol = shade;
            this.bCol = shade;
            this.quadSize = stable.nextFloat() + 0.5F;
            double offsetX = (stable.nextGaussian() - 1.0D) * 0.75D;
            double offsetY = (stable.nextGaussian() - 1.0D) * 0.75D;
            double offsetZ = (stable.nextGaussian() - 1.0D) * 0.75D;
            this.x = baseX + offsetX;
            this.y = baseY + offsetY;
            this.z = baseZ + offsetZ;
            this.xo = oldX + offsetX;
            this.yo = oldY + offsetY;
            this.zo = oldZ + offsetZ;
            super.render(buffer, camera, partialTick);
        }
        this.x = baseX;
        this.y = baseY;
        this.z = baseZ;
        this.xo = oldX;
        this.yo = oldY;
        this.zo = oldZ;
        this.quadSize = oldSize;
        this.rCol = oldRed;
        this.gCol = oldGreen;
        this.bCol = oldBlue;
        this.alpha = oldAlpha;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
            return new LandmineSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
