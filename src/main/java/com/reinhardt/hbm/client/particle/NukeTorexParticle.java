package com.reinhardt.hbm.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NukeTorexParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final List<Cloudlet> cloudlets = new ArrayList<>();
    private final float scale;
    private double coreHeight = 3.0D;
    private double convectionHeight = 3.0D;
    private double torusWidth = 3.0D;
    private double rollerSize = 1.0D;
    private double heat = 1.0D;
    private double lastSpawnY = Double.NaN;

    protected NukeTorexParticle(ClientLevel level, double x, double y, double z, double scale, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.scale = Mth.clamp((float) scale, 0.5F, 5.0F);
        this.lifetime = Math.max(1, (int) (45 * 20 * this.scale));
        this.quadSize = 0.0F;
        this.hasPhysics = false;
        this.alpha = 0.0F;
        this.setSprite(sprites.get(this.random));
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

        if (Double.isNaN(this.lastSpawnY)) {
            this.lastSpawnY = this.y - 3.0D;
        }

        int spawnTarget = Math.max(this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(this.x), (int) Math.floor(this.z)) - 3, 1);
        double moveSpeed = 0.5D;
        if (Math.abs(spawnTarget - this.lastSpawnY) < moveSpeed) {
            this.lastSpawnY = spawnTarget;
        } else {
            this.lastSpawnY += moveSpeed * Math.signum(spawnTarget - this.lastSpawnY);
        }

        spawnCoreCloudlets();
        spawnShockCloudlets();
        spawnRingCloudlets();
        spawnCondensationCloudlets();

        for (Iterator<Cloudlet> iterator = this.cloudlets.iterator(); iterator.hasNext(); ) {
            Cloudlet cloudlet = iterator.next();
            cloudlet.tick();
            if (cloudlet.dead) {
                iterator.remove();
            }
        }

        this.coreHeight += 0.15D / this.scale;
        this.torusWidth += 0.05D / this.scale;
        this.rollerSize = this.torusWidth * 0.35D;
        this.convectionHeight = this.coreHeight + this.rollerSize;
        int maxHeat = (int) (50.0F * this.scale);
        this.heat = Math.max(1.0D, maxHeat - (maxHeat * (double) this.age) / (double) this.lifetime);
    }

    private void spawnCoreCloudlets() {
        double range = (this.torusWidth - this.rollerSize) * 0.25D;
        double simSpeed = simulationSpeed();
        int toSpawn = (int) Math.ceil(10.0D * simSpeed * simSpeed);
        int cloudLife = Math.min(this.age * this.age + 200, this.lifetime - this.age + 200);
        for (int i = 0; i < toSpawn; i++) {
            double cloudX = this.x + this.random.nextGaussian() * range;
            double cloudZ = this.z + this.random.nextGaussian() * range;
            Cloudlet cloudlet = new Cloudlet(cloudX, this.lastSpawnY, cloudZ, (float) (this.random.nextDouble() * Math.PI * 2.0D), cloudLife, CloudType.STANDARD);
            cloudlet.setScale(1.0F + this.age * 0.005F * this.scale, 5.0F * this.scale);
            this.cloudlets.add(cloudlet);
        }
    }

    private void spawnShockCloudlets() {
        if (this.age >= 150) {
            return;
        }
        int cloudCount = Math.min(this.age * 5, 180);
        int shockLife = Math.max(300 - this.age * 20, 50);
        for (int i = 0; i < cloudCount; i++) {
            float angle = (float) (Math.PI * 2.0D * this.random.nextDouble());
            double radius = (this.age * 1.5D + this.random.nextDouble()) * 1.5D;
            double cloudX = this.x + Math.cos(angle) * radius;
            double cloudZ = this.z + Math.sin(angle) * radius;
            int ground = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(cloudX) + 1, (int) Math.floor(cloudZ));
            Cloudlet cloudlet = new Cloudlet(cloudX, ground, cloudZ, angle, shockLife, CloudType.SHOCK);
            cloudlet.setScale(7.0F, 2.0F);
            cloudlet.motionMult = this.age > 15 ? 0.75D : 0.0D;
            this.cloudlets.add(cloudlet);
        }
    }

    private void spawnRingCloudlets() {
        if (this.age >= 130 * this.scale) {
            return;
        }
        int cloudLife = (int) Math.min((this.age * this.age + 200) * this.scale, (this.lifetime - this.age + 200) * this.scale);
        for (int i = 0; i < 2; i++) {
            Cloudlet cloudlet = new Cloudlet(this.x, this.y + this.coreHeight, this.z, (float) (this.random.nextDouble() * Math.PI * 2.0D), cloudLife, CloudType.RING);
            cloudlet.setScale(1.0F + this.age * 0.0025F * this.scale * this.scale, 3.0F * this.scale * this.scale);
            this.cloudlets.add(cloudlet);
        }
    }

    private void spawnCondensationCloudlets() {
        if (this.age <= 130 * this.scale || this.age >= 600 * this.scale) {
            return;
        }
        spawnCondensationLayer(this.coreHeight - 5.0D, 5.0D);
        if (this.age > 200 * this.scale) {
            spawnCondensationLayer(this.coreHeight + 25.0D, 3.0D);
        }
    }

    private void spawnCondensationLayer(double yOffset, double radiusOffset) {
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 4; j++) {
                float angle = (float) (Math.PI * 2.0D * this.random.nextDouble());
                double radius = this.torusWidth + this.rollerSize * (radiusOffset + this.random.nextDouble());
                double tilted = Math.PI / 45.0D * j;
                double cloudX = this.x + Math.cos(angle) * Math.cos(tilted) * radius;
                double cloudY = this.y + yOffset + j * this.scale + Math.sin(tilted) * radius;
                double cloudZ = this.z + Math.sin(angle) * Math.cos(tilted) * radius;
                int cloudLife = (int) ((20 + this.age / 10.0D) * (1.0D + this.random.nextDouble() * 0.1D));
                Cloudlet cloudlet = new Cloudlet(cloudX, cloudY, cloudZ, angle, cloudLife, CloudType.CONDENSATION);
                cloudlet.setScale(0.125F * this.scale, 3.0F * this.scale);
                this.cloudlets.add(cloudlet);
            }
        }
    }

    private double simulationSpeed() {
        int simSlow = this.lifetime / 4;
        int simStop = this.lifetime / 2;
        if (this.age > simStop) {
            return 0.0D;
        }
        if (this.age > simSlow) {
            return 1.0D - (double) (this.age - simSlow) / (double) (simStop - simSlow);
        }
        return 1.0D;
    }

    private float effectAlpha() {
        int fadeOut = this.lifetime * 3 / 4;
        if (this.age > fadeOut) {
            return 1.0F - (float) (this.age - fadeOut) / (float) (this.lifetime - fadeOut);
        }
        return 1.0F;
    }

    private double greying() {
        int greying = this.lifetime * 3 / 4;
        if (this.age > greying) {
            return 1.0D + (double) (this.age - greying) / (double) (this.lifetime - greying);
        }
        return 1.0D;
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, Camera camera, float partialTick) {
        double baseX = this.x;
        double baseY = this.y;
        double baseZ = this.z;
        double oldBaseX = this.xo;
        double oldBaseY = this.yo;
        double oldBaseZ = this.zo;
        float oldQuadSize = this.quadSize;
        float oldAlpha = this.alpha;
        float oldRed = this.rCol;
        float oldGreen = this.gCol;
        float oldBlue = this.bCol;

        for (Cloudlet cloudlet : this.cloudlets) {
            Vec3 pos = cloudlet.interpolatedPos(partialTick);
            Vec3 color = cloudlet.interpolatedColor(partialTick);
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;
            this.xo = cloudlet.prevX;
            this.yo = cloudlet.prevY;
            this.zo = cloudlet.prevZ;
            this.quadSize = cloudlet.renderScale();
            this.alpha = cloudlet.renderAlpha() * effectAlpha();
            this.rCol = (float) Mth.clamp(color.x, 0.0D, 1.8D);
            this.gCol = (float) Mth.clamp(color.y, 0.0D, 1.8D);
            this.bCol = (float) Mth.clamp(color.z, 0.0D, 1.8D);
            this.setSprite(this.sprites.get(Math.min(29, (int) ((cloudlet.age / (float) cloudlet.life) * 29.0F)), 30));
            super.render(buffer, camera, partialTick);
        }

        this.x = baseX;
        this.y = baseY;
        this.z = baseZ;
        this.xo = oldBaseX;
        this.yo = oldBaseY;
        this.zo = oldBaseZ;
        this.quadSize = oldQuadSize;
        this.alpha = oldAlpha;
        this.rCol = oldRed;
        this.gCol = oldGreen;
        this.bCol = oldBlue;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        double radius = Math.max(64.0D, this.torusWidth * this.scale * 12.0D);
        double height = Math.max(64.0D, this.coreHeight * this.scale * 12.0D);
        return new AABB(this.x - radius, this.y - 8.0D, this.z - radius, this.x + radius, this.y + height, this.z + radius);
    }

    private final class Cloudlet {
        private double x;
        private double y;
        private double z;
        private double prevX;
        private double prevY;
        private double prevZ;
        private Vec3 color = Vec3.ZERO;
        private Vec3 prevColor = Vec3.ZERO;
        private final float angle;
        private int age;
        private final int life;
        private final float rangeMod;
        private final float colorMod;
        private final CloudType type;
        private boolean dead;
        private float startScale = 1.0F;
        private float growScale = 5.0F;
        private double motionMult = 1.0D;

        private Cloudlet(double x, double y, double z, float angle, int life, CloudType type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.angle = angle;
            this.life = Math.max(1, life);
            this.type = type;
            this.rangeMod = 0.3F + NukeTorexParticle.this.random.nextFloat() * 0.7F;
            this.colorMod = 0.8F + NukeTorexParticle.this.random.nextFloat() * 0.2F;
            updateColor();
            this.prevColor = this.color;
        }

        private void tick() {
            if (++this.age > this.life) {
                this.dead = true;
                return;
            }
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;

            Vec3 motion = switch (this.type) {
                case STANDARD -> standardMotion();
                case SHOCK -> shockMotion();
                case RING -> ringMotion();
                case CONDENSATION -> condensationMotion();
            };
            double mult = this.motionMult * simulationSpeed();
            this.x += motion.x * mult;
            this.y += motion.y * mult;
            this.z += motion.z * mult;
            updateColor();
        }

        private Vec3 standardMotion() {
            double simX = NukeTorexParticle.this.x + horizontalDistance();
            double simZ = NukeTorexParticle.this.z;
            Vec3 convection = convectionMotion(simX, simZ);
            Vec3 lift = liftMotion(simX);
            double factor = Mth.clamp((this.y - NukeTorexParticle.this.y) / coreHeight, 0.0D, 1.0D);
            return convection.scale(factor).add(lift.scale(1.0D - factor));
        }

        private Vec3 shockMotion() {
            double factor = Mth.clamp((this.y - NukeTorexParticle.this.y) / coreHeight, 0.0D, 1.0D);
            return new Vec3(Math.cos(this.angle) * factor, 0.0D, Math.sin(this.angle) * factor);
        }

        private Vec3 ringMotion() {
            double simX = NukeTorexParticle.this.x + horizontalDistance();
            if (simX > NukeTorexParticle.this.x + torusWidth * 2.0D) {
                return Vec3.ZERO;
            }
            return torusMotion(simX, NukeTorexParticle.this.y + coreHeight * 0.5D, rollerSize * this.rangeMod * 0.25D, 0.001D);
        }

        private Vec3 convectionMotion(double simX, double simZ) {
            return torusMotion(simX, NukeTorexParticle.this.y + coreHeight, rollerSize * this.rangeMod, 1.0D);
        }

        private Vec3 torusMotion(double simX, double torusY, double roller, double speed) {
            double torusX = NukeTorexParticle.this.x + torusWidth;
            Vec3 delta = new Vec3(torusX - simX, torusY - this.y, 0.0D);
            double dist = Math.max(0.001D, delta.length() / Math.max(0.001D, roller) - 1.0D);
            double func = 1.0D - Math.exp(-dist);
            double rot = func * Math.PI * 0.5D;
            Vec3 radial = delta.scale(-1.0D / dist);
            double rx = radial.x * Math.cos(rot) - radial.y * Math.sin(rot);
            double ry = radial.x * Math.sin(rot) + radial.y * Math.cos(rot);
            Vec3 motion = new Vec3(torusX + rx - simX, torusY + ry - this.y, NukeTorexParticle.this.z - NukeTorexParticle.this.z);
            if (motion.lengthSqr() < 1.0E-6D) {
                return Vec3.ZERO;
            }
            motion = speed == 1.0D ? motion.normalize() : motion.scale(speed).normalize();
            return rotateY(motion, this.angle);
        }

        private Vec3 liftMotion(double simX) {
            double falloff = Mth.clamp(1.0D - (simX - (NukeTorexParticle.this.x + torusWidth)), 0.0D, 1.0D);
            Vec3 motion = new Vec3(NukeTorexParticle.this.x - this.x, NukeTorexParticle.this.y + convectionHeight - this.y, NukeTorexParticle.this.z - this.z);
            if (motion.lengthSqr() < 1.0E-6D) {
                return Vec3.ZERO;
            }
            return motion.normalize().scale(falloff);
        }

        private Vec3 condensationMotion() {
            Vec3 delta = new Vec3(this.x - NukeTorexParticle.this.x, 0.0D, this.z - NukeTorexParticle.this.z);
            return delta.scale(0.00002D * NukeTorexParticle.this.age);
        }

        private Vec3 rotateY(Vec3 vec, float angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            return new Vec3(vec.x * cos + vec.z * sin, vec.y, vec.z * cos - vec.x * sin);
        }

        private double horizontalDistance() {
            double dx = NukeTorexParticle.this.x - this.x;
            double dz = NukeTorexParticle.this.z - this.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        private void updateColor() {
            this.prevColor = this.color;
            if (this.type == CloudType.CONDENSATION) {
                this.color = new Vec3(1.0D, 1.0D, 1.0D);
                return;
            }
            double dx = NukeTorexParticle.this.x - this.x;
            double dy = NukeTorexParticle.this.y + coreHeight - this.y;
            double dz = NukeTorexParticle.this.z - this.z;
            double dist = Math.sqrt((dx * dx + dy * dy + dz * dz) / Math.max(1.0D, heat));
            double col = 2.0D / Math.max(1.0D, dist);
            double grey = greying() + (this.type == CloudType.RING ? 1.0D : 0.0D);
            this.color = new Vec3(
                    Math.max(col * 2.0D, 0.25D) * this.colorMod * grey,
                    Math.max(col * 1.5D, 0.25D) * this.colorMod * grey,
                    Math.max(col * 0.5D, 0.25D) * this.colorMod * grey
            );
        }

        private Vec3 interpolatedPos(float partialTick) {
            Vec3 base = new Vec3(
                    Mth.lerp(partialTick, this.prevX, this.x),
                    Mth.lerp(partialTick, this.prevY, this.y),
                    Mth.lerp(partialTick, this.prevZ, this.z)
            );
            if (this.type == CloudType.SHOCK) {
                return base;
            }
            return new Vec3(
                    (base.x - NukeTorexParticle.this.x) * scale + NukeTorexParticle.this.x,
                    (base.y - NukeTorexParticle.this.y) * scale + NukeTorexParticle.this.y,
                    (base.z - NukeTorexParticle.this.z) * scale + NukeTorexParticle.this.z
            );
        }

        private Vec3 interpolatedColor(float partialTick) {
            return new Vec3(
                    Mth.lerp(partialTick, this.prevColor.x, this.color.x),
                    Mth.lerp(partialTick, this.prevColor.y, this.color.y),
                    Mth.lerp(partialTick, this.prevColor.z, this.color.z)
            );
        }

        private float renderAlpha() {
            float alpha = (1.0F - this.age / (float) this.life);
            if (this.type == CloudType.CONDENSATION) {
                alpha *= 0.25F;
            }
            return alpha;
        }

        private float renderScale() {
            float base = this.startScale + (this.age / (float) this.life) * this.growScale;
            if (this.type != CloudType.SHOCK) {
                base *= scale;
            }
            return base;
        }

        private void setScale(float start, float grow) {
            this.startScale = start;
            this.growScale = grow;
        }
    }

    private enum CloudType {
        STANDARD,
        SHOCK,
        RING,
        CONDENSATION
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new NukeTorexParticle(level, x, y, z, xSpeed, this.sprites);
        }
    }
}
