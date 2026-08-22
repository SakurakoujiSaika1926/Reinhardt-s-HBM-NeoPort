package com.reinhardt.hbm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public final class MukeFlashParticle extends TextureSheetParticle {
    private final boolean balefire;

    private MukeFlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, boolean balefire) {
        super(level, x, y, z);
        this.balefire = balefire;
        this.setSprite(sprites.get(0, 1));
        this.lifetime = 20;
        this.hasPhysics = false;
        this.rCol = 1.0F;
        this.gCol = 0.9F;
        this.bCol = 0.75F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        if (this.age == 15) {
            spawnClouds();
        }
    }

    private void spawnClouds() {
        SimpleParticleType cloud = this.balefire
                ? HbmParticleTypes.MUKE_CLOUD_BALEFIRE.get()
                : HbmParticleTypes.MUKE_CLOUD.get();
        for (int step = 0; step <= 18; step++) {
            double rise = step * 0.1D;
            this.level.addParticle(cloud, this.x, this.y, this.z,
                    this.random.nextGaussian() * 0.05D,
                    rise + this.random.nextGaussian() * 0.02D,
                    this.random.nextGaussian() * 0.05D);
        }
        for (int i = 0; i < 100; i++) {
            this.level.addParticle(cloud, this.x, this.y + 0.5D, this.z,
                    this.random.nextGaussian() * 0.5D,
                    this.random.nextInt(5) == 0 ? 0.02D : 0.0D,
                    this.random.nextGaussian() * 0.5D);
        }
        for (int i = 0; i < 75; i++) {
            double motionX = this.random.nextGaussian() * 0.5D;
            double motionZ = this.random.nextGaussian() * 0.5D;
            if (motionX * motionX + motionZ * motionZ > 1.5D) {
                motionX *= 0.5D;
                motionZ *= 0.5D;
            }
            double motionY = 1.8D
                    + (this.random.nextDouble() * 3.0D - 1.5D)
                    * (0.75D - (motionX * motionX + motionZ * motionZ)) * 0.5D;
            this.level.addParticle(cloud, this.x, this.y, this.z,
                    motionX,
                    motionY + this.random.nextGaussian() * 0.02D,
                    motionZ);
        }
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
        float oldAlpha = this.alpha;

        this.quadSize = (this.age + partialTick) * 3.0F + 1.0F;
        this.alpha = Mth.clamp(1.0F - (this.age + partialTick) / this.lifetime, 0.0F, 1.0F) * 0.5F;
        Random offsets = new Random();
        for (int i = 0; i < 24; i++) {
            offsets.setSeed(i * 31L + 1L);
            this.x = this.xo = baseX + offsets.nextDouble() * 15.0D - 7.5D;
            this.y = this.yo = baseY + offsets.nextDouble() * 7.5D - 3.75D;
            this.z = this.zo = baseZ + offsets.nextDouble() * 15.0D - 7.5D;
            super.render(buffer, camera, partialTick);
        }

        this.x = baseX;
        this.y = baseY;
        this.z = baseZ;
        this.xo = oldX;
        this.yo = oldY;
        this.zo = oldZ;
        this.quadSize = oldSize;
        this.alpha = oldAlpha;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return MukeParticleRenderType.ADDITIVE;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return getBoundingBox().inflate(72.0D);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean balefire;

        public Provider(SpriteSet sprites, boolean balefire) {
            this.sprites = sprites;
            this.balefire = balefire;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new MukeFlashParticle(level, x, y, z, this.sprites, this.balefire);
        }
    }
}
