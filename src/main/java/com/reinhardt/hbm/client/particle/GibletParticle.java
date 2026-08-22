package com.reinhardt.hbm.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public final class GibletParticle extends TextureSheetParticle {
    public static final int MEAT = 0;
    public static final int SLIME = 1;
    public static final int METAL = 2;

    private final int gibType;

    private GibletParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            int gibType
    ) {
        super(level, x, y, z);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gibType = gibType;
        this.lifetime = 140 + this.random.nextInt(20);
        this.gravity = gibType == METAL ? 4.0F : 2.0F;
        this.friction = 0.98F;
        this.hasPhysics = true;
        this.quadSize = (this.random.nextFloat() * 0.5F + 0.5F) * 0.1F;
        this.setSprite(sprites.get(this.random));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && !this.onGround && this.gibType != METAL) {
            BlockParticleOption option = new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    (this.gibType == SLIME ? Blocks.MELON : Blocks.REDSTONE_BLOCK).defaultBlockState()
            );
            Particle dust = Minecraft.getInstance().particleEngine.createParticle(
                    option, this.x, this.y, this.z, 0.0D, 0.0D, 0.0D
            );
            if (dust != null) {
                dust.setLifetime(20 + this.random.nextInt(20));
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final int gibType;

        public Provider(SpriteSet sprites, int gibType) {
            this.sprites = sprites;
            this.gibType = gibType;
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
            return new GibletParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, this.gibType);
        }
    }
}
