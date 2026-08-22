package com.reinhardt.hbm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class FalloutRainParticle extends TextureSheetParticle {
    private static final int CONTROL_LIFETIME = 20 * 180;
    private static final int GRID_SIZE = 32;
    private static final int GRID_CENTER = 16;
    private final SpriteSet sprites;
    private final double radius;
    private final float[] rainXCoords = new float[GRID_SIZE * GRID_SIZE];
    private final float[] rainZCoords = new float[GRID_SIZE * GRID_SIZE];
    private final Random cellRandom = new Random();

    private FalloutRainParticle(ClientLevel level, double x, double y, double z, double radius, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.radius = Math.max(1.0D, radius);
        this.lifetime = CONTROL_LIFETIME;
        this.quadSize = 0.0F;
        this.alpha = 0.0F;
        this.hasPhysics = false;
        setSprite(sprites.get(this.random));
        initRainVectors();
    }

    private void initRainVectors() {
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                float dx = x - GRID_CENTER;
                float dz = z - GRID_CENTER;
                float length = Mth.sqrt(dx * dx + dz * dz);
                int index = z << 5 | x;
                if (length <= 0.0F) {
                    this.rainXCoords[index] = 0.0F;
                    this.rainZCoords[index] = 0.0F;
                } else {
                    this.rainXCoords[index] = -dz / length;
                    this.rainZCoords[index] = dx / length;
                }
            }
        }
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

        Player player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(this.x, player.getY(), this.z) > this.radius * this.radius) {
            return;
        }

        int renderLayerCount = Minecraft.getInstance().options.graphicsMode().get().getId() == 0 ? 10 : 5;
        int playerX = Mth.floor(player.getX());
        int playerY = Mth.floor(player.getY());
        int playerZ = Mth.floor(player.getZ());
        int playerHeight = Mth.floor(player.getY());

        for (int layerZ = playerZ - renderLayerCount; layerZ <= playerZ + renderLayerCount; layerZ++) {
            for (int layerX = playerX - renderLayerCount; layerX <= playerX + renderLayerCount; layerX++) {
                if (Math.hypot((layerX + 0.5D) - this.x, (layerZ + 0.5D) - this.z) > this.radius) {
                    continue;
                }
                spawnCell(layerX, layerZ, player, playerY, playerHeight, renderLayerCount);
            }
        }
    }

    private void spawnCell(int layerX, int layerZ, Player player, int playerY, int playerHeight, int renderLayerCount) {
        int rainCoord = (layerZ - Mth.floor(player.getZ()) + GRID_CENTER) * GRID_SIZE + layerX - Mth.floor(player.getX()) + GRID_CENTER;
        if (rainCoord < 0 || rainCoord >= this.rainXCoords.length) {
            return;
        }

        int rainHeight = this.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, layerX, layerZ);
        int minHeight = Math.max(playerY - renderLayerCount, rainHeight);
        int maxHeight = Math.max(playerY + renderLayerCount, rainHeight);
        int layerY = rainHeight < playerHeight ? playerHeight : rainHeight;
        if (minHeight == maxHeight) {
            return;
        }

        this.cellRandom.setSeed(layerX * (long) layerX * 3121L + layerX * 45238971L ^ layerZ * (long) layerZ * 418711L + layerZ * 13761L);
        float rainCoordX = this.rainXCoords[rainCoord] * 0.5F;
        float rainCoordZ = this.rainZCoords[rainCoord] * 0.5F;
        float fallVariation = 0.4F + this.cellRandom.nextFloat() * 0.2F;
        float swayVariation = this.cellRandom.nextFloat();
        double distX = layerX + 0.5D - player.getX();
        double distZ = layerZ + 0.5D - player.getZ();
        float intensityMod = Mth.sqrt((float) (distX * distX + distZ * distZ)) / renderLayerCount;
        float alpha = ((1.0F - intensityMod * intensityMod) * 0.3F + 0.5F);
        if (alpha <= 0.0F) {
            return;
        }

        FalloutRainSheetParticle sheet = new FalloutRainSheetParticle(
                this.level,
                layerX + 0.5D,
                minHeight,
                layerZ + 0.5D,
                minHeight,
                maxHeight,
                layerY,
                rainCoordX,
                rainCoordZ,
                fallVariation,
                swayVariation,
                alpha,
                this.sprites
        );
        Minecraft.getInstance().particleEngine.add(sheet);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.NO_RENDER;
    }

    private static final class FalloutRainSheetParticle extends TextureSheetParticle {
        private final double minY;
        private final double maxY;
        private final int lightY;
        private final float rainCoordX;
        private final float rainCoordZ;
        private final float fallVariation;
        private final float swayVariation;
        private final float baseAlpha;

        private FalloutRainSheetParticle(
                ClientLevel level,
                double x,
                double y,
                double z,
                double minY,
                double maxY,
                int lightY,
                float rainCoordX,
                float rainCoordZ,
                float fallVariation,
                float swayVariation,
                float alpha,
                SpriteSet sprites
        ) {
            super(level, x, y, z);
            setSprite(sprites.get(this.random));
            this.minY = minY;
            this.maxY = maxY;
            this.lightY = lightY;
            this.rainCoordX = rainCoordX;
            this.rainCoordZ = rainCoordZ;
            this.fallVariation = fallVariation;
            this.swayVariation = swayVariation;
            this.baseAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
            this.alpha = this.baseAlpha;
            this.rCol = 1.0F;
            this.gCol = 1.0F;
            this.bCol = 1.0F;
            this.quadSize = Math.max(0.5F, (float) (maxY - minY));
            this.lifetime = 2;
            this.hasPhysics = false;
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (++this.age >= this.lifetime) {
                remove();
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        public float getQuadSize(float scaleFactor) {
            return Math.max(0.5F, (float) (this.maxY - this.minY));
        }

        @Override
        public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
            var cameraPos = renderInfo.getPosition();
            float minX = (float) (this.x - this.rainCoordX - cameraPos.x());
            float maxX = (float) (this.x + this.rainCoordX - cameraPos.x());
            float minZ = (float) (this.z - this.rainCoordZ - cameraPos.z());
            float maxZ = (float) (this.z + this.rainCoordZ - cameraPos.z());
            float minY = (float) (this.minY - cameraPos.y());
            float maxY = (float) (this.maxY - cameraPos.y());

            Player player = Minecraft.getInstance().player;
            int timer = player == null ? 0 : player.tickCount;
            float swayLoop = ((timer & 511) + partialTicks) / 512.0F;
            float fallSpeed = 1.0F;
            float u0 = this.getU0();
            float u1 = this.getU1();
            float v0 = lerpV((float) (this.minY * fallSpeed / 4.0D + swayLoop * fallSpeed + this.swayVariation));
            float v1 = lerpV((float) (this.maxY * fallSpeed / 4.0D + swayLoop * fallSpeed + this.swayVariation));
            int light = getLightColor(partialTicks);

            buffer.addVertex(minX, minY, minZ).setUv(u0 + this.fallVariation * (u1 - u0), v0).setColor(this.rCol, this.gCol, this.bCol, this.baseAlpha).setLight(light);
            buffer.addVertex(maxX, minY, maxZ).setUv(u1 + this.fallVariation * (u1 - u0), v0).setColor(this.rCol, this.gCol, this.bCol, this.baseAlpha).setLight(light);
            buffer.addVertex(maxX, maxY, maxZ).setUv(u1 + this.fallVariation * (u1 - u0), v1).setColor(this.rCol, this.gCol, this.bCol, this.baseAlpha).setLight(light);
            buffer.addVertex(minX, maxY, minZ).setUv(u0 + this.fallVariation * (u1 - u0), v1).setColor(this.rCol, this.gCol, this.bCol, this.baseAlpha).setLight(light);
        }

        private float lerpV(float v) {
            float wrapped = v - Mth.floor(v);
            return Mth.lerp(wrapped, this.getV0(), this.getV1());
        }

        @Override
        protected int getLightColor(float partialTick) {
            return this.level.getLightEngine().getRawBrightness(net.minecraft.core.BlockPos.containing(this.x, this.lightY, this.z), 0);
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new FalloutRainParticle(level, x, y, z, xSpeed, this.sprites);
        }
    }
}
