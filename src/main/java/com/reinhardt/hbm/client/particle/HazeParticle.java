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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public final class HazeParticle extends TextureSheetParticle {
    private static final HazeQuad[] QUADS = createOldQuads();

    private HazeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = 600 + this.random.nextInt(100);
        this.quadSize = 10.0F;
        this.rCol = 0.0F;
        this.gCol = 0.0F;
        this.bCol = 0.0F;
        this.hasPhysics = false;
        this.setSprite(sprites.get(0, 1));
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
        this.xd *= 0.9599999785423279D;
        this.yd *= 0.9599999785423279D;
        this.zd *= 0.9599999785423279D;
        int surfaceX = Mth.floor(this.x) + this.random.nextInt(15) - 7;
        int surfaceZ = Mth.floor(this.z) + this.random.nextInt(15) - 7;
        int surfaceY = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, surfaceX, surfaceZ);
        this.level.addParticle(ParticleTypes.LAVA,
                surfaceX + this.random.nextDouble(), surfaceY + 0.1D, surfaceZ + this.random.nextDouble(),
                0.0D, 0.0D, 0.0D);
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
        this.alpha = Math.max(0.0F, Mth.sin((float) (this.age * Math.PI / 400.0D)) * 0.025F);
        for (HazeQuad quad : QUADS) {
            this.x = baseX + quad.x;
            this.y = baseY + quad.y;
            this.z = baseZ + quad.z;
            this.xo = oldX + quad.x;
            this.yo = oldY + quad.y;
            this.zo = oldZ + quad.z;
            this.quadSize = 10.0F * quad.scale;
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
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    private static HazeQuad[] createOldQuads() {
        Random random = new Random(50L);
        HazeQuad[] quads = new HazeQuad[25];
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (int i = 0; i < quads.length; i++) {
            x += random.nextGaussian() * 2.5D;
            y += random.nextGaussian() * 0.15D;
            z += random.nextGaussian() * 2.5D;
            float scale = (float) (random.nextDouble() * 0.25D + 0.75D);
            quads[i] = new HazeQuad(
                    x + random.nextGaussian() * 0.5D,
                    y + random.nextGaussian() * 0.5D,
                    z + random.nextGaussian() * 0.5D,
                    scale
            );
        }
        return quads;
    }

    private record HazeQuad(double x, double y, double z, float scale) {
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
            return new HazeParticle(level, x, y, z, this.sprites);
        }
    }
}
