package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NukeTorexEntity extends Entity {
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(NukeTorexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(NukeTorexEntity.class, EntityDataSerializers.INT);

    public double coreHeight = 3.0D;
    public double convectionHeight = 3.0D;
    public double torusWidth = 3.0D;
    public double rollerSize = 1.0D;
    public double heat = 1.0D;
    public double lastSpawnY = -1.0D;
    public final List<Cloudlet> cloudlets = new ArrayList<>();
    public boolean didPlaySound;
    public boolean didShake;

    public NukeTorexEntity(EntityType<? extends NukeTorexEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        setNoGravity(true);
    }

    public NukeTorexEntity(Level level, double x, double y, double z, float scale, int type) {
        this(HbmEntityTypes.NUKE_TOREX.get(), level);
        setPos(x, y, z);
        setScale(scale);
        setTorexType(type);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0F);
        builder.define(TYPE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (this.tickCount > maxAge()) {
                discard();
            }
            return;
        }
        clientTick();
    }

    private void clientTick() {
        double scale = 1.5D;
        double colorScale = 1.5D;
        int maxAge = maxAge();

        if (this.tickCount == 1) {
            setScale((float) scale);
        }
        if (this.lastSpawnY == -1.0D) {
            this.lastSpawnY = getY() - 3.0D;
        }

        int spawnTarget = Math.max(level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(getX()), (int) Math.floor(getZ())) - 3, 1);
        double moveSpeed = 0.5D;
        if (Math.abs(spawnTarget - this.lastSpawnY) < moveSpeed) {
            this.lastSpawnY = spawnTarget;
        } else {
            this.lastSpawnY += moveSpeed * Math.signum(spawnTarget - this.lastSpawnY);
        }

        spawnMushClouds(colorScale, maxAge);
        spawnShockClouds();
        spawnRingClouds(scale, colorScale, maxAge);
        spawnCondensationClouds(scale, colorScale);

        for (Iterator<Cloudlet> iterator = this.cloudlets.iterator(); iterator.hasNext(); ) {
            Cloudlet cloudlet = iterator.next();
            cloudlet.update();
            if (cloudlet.dead) {
                iterator.remove();
            }
        }

        this.coreHeight += 0.15D / scale;
        this.torusWidth += 0.05D / scale;
        this.rollerSize = this.torusWidth * 0.35D;
        this.convectionHeight = this.coreHeight + this.rollerSize;

        int maxHeat = (int) (50.0D * colorScale);
        this.heat = Math.max(1.0D, maxHeat - Math.pow((maxHeat * this.tickCount) / (double) maxAge, 1.0D));
    }

    private void spawnMushClouds(double colorScale, int maxAge) {
        double range = (this.torusWidth - this.rollerSize) * 0.25D;
        double simSpeed = simulationSpeed();
        int toSpawn = (int) Math.ceil(10.0D * simSpeed * simSpeed);
        int lifetime = Math.min(this.tickCount * this.tickCount + 200, maxAge - this.tickCount + 200);
        for (int i = 0; i < toSpawn; i++) {
            double x = getX() + this.random.nextGaussian() * range;
            double z = getZ() + this.random.nextGaussian() * range;
            Cloudlet cloudlet = new Cloudlet(x, this.lastSpawnY, z, (float) (this.random.nextDouble() * 2.0D * Math.PI), lifetime, TorexType.STANDARD);
            cloudlet.setScale(1.0F + this.tickCount * 0.005F * (float) colorScale, 5.0F * (float) colorScale);
            this.cloudlets.add(cloudlet);
        }
    }

    private void spawnShockClouds() {
        if (this.tickCount >= 150) {
            return;
        }
        int cloudCount = this.tickCount * 5;
        int shockLife = Math.max(300 - this.tickCount * 20, 50);
        for (int i = 0; i < cloudCount; i++) {
            float angle = (float) (Math.PI * 2.0D * this.random.nextDouble());
            double radius = (this.tickCount * 1.5D + this.random.nextDouble()) * 1.5D;
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            int y = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x + 1, (int) z);
            Cloudlet cloudlet = new Cloudlet(x, y, z, angle, shockLife, TorexType.SHOCK);
            cloudlet.setScale(7.0F, 2.0F);
            cloudlet.setMotion(this.tickCount > 15 ? 0.75D : 0.0D);
            this.cloudlets.add(cloudlet);
        }
    }

    private void spawnRingClouds(double scale, double colorScale, int maxAge) {
        if (this.tickCount >= 130.0D * scale) {
            return;
        }
        int lifetime = (int) (Math.min(this.tickCount * this.tickCount + 200, maxAge - this.tickCount + 200) * scale);
        for (int i = 0; i < 2; i++) {
            Cloudlet cloudlet = new Cloudlet(getX(), getY() + this.coreHeight, getZ(), (float) (this.random.nextDouble() * 2.0D * Math.PI), lifetime, TorexType.RING);
            cloudlet.setScale(1.0F + this.tickCount * 0.0025F * (float) (colorScale * colorScale), 3.0F * (float) (colorScale * colorScale));
            this.cloudlets.add(cloudlet);
        }
    }

    private void spawnCondensationClouds(double scale, double colorScale) {
        if (this.tickCount > 130.0D * scale && this.tickCount < 600.0D * scale) {
            spawnCondensationLayer(this.coreHeight - 5.0D, 5.0D, colorScale);
        }
        if (this.tickCount > 200.0D * scale && this.tickCount < 600.0D * scale) {
            spawnCondensationLayer(this.coreHeight + 25.0D, 3.0D, colorScale);
        }
    }

    private void spawnCondensationLayer(double yBase, double radiusBase, double colorScale) {
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 4; j++) {
                float angle = (float) (Math.PI * 2.0D * this.random.nextDouble());
                double radius = this.torusWidth + this.rollerSize * (radiusBase + this.random.nextDouble());
                double tilt = Math.PI / 45.0D * j;
                double x = getX() + Math.cos(angle) * Math.cos(tilt) * radius;
                double y = getY() + yBase + j * colorScale + Math.sin(tilt) * radius;
                double z = getZ() + Math.sin(angle) * Math.cos(tilt) * radius;
                int lifetime = (int) ((20 + this.tickCount / 10.0D) * (1.0D + this.random.nextDouble() * 0.1D));
                Cloudlet cloudlet = new Cloudlet(x, y, z, angle, lifetime, TorexType.CONDENSATION);
                cloudlet.setScale(0.125F * (float) colorScale, 3.0F * (float) colorScale);
                this.cloudlets.add(cloudlet);
            }
        }
    }

    public NukeTorexEntity setScale(float scale) {
        if (!level().isClientSide) {
            this.entityData.set(SCALE, scale);
        }
        this.coreHeight = this.coreHeight / 1.5D * scale;
        this.convectionHeight = this.convectionHeight / 1.5D * scale;
        this.torusWidth = this.torusWidth / 1.5D * scale;
        this.rollerSize = this.rollerSize / 1.5D * scale;
        return this;
    }

    public NukeTorexEntity setTorexType(int type) {
        this.entityData.set(TYPE, type);
        return this;
    }

    public double getScale() {
        return this.entityData.get(SCALE);
    }

    public int getTorexType() {
        return this.entityData.get(TYPE);
    }

    public double simulationSpeed() {
        int maxAge = maxAge();
        int simSlow = maxAge / 4;
        int simStop = maxAge / 2;
        if (this.tickCount > simStop) {
            return 0.0D;
        }
        if (this.tickCount > simSlow) {
            return 1.0D - (double) (this.tickCount - simSlow) / (double) (simStop - simSlow);
        }
        return 1.0D;
    }

    public double greying() {
        int maxAge = maxAge();
        int greying = maxAge * 3 / 4;
        if (this.tickCount > greying) {
            return 1.0D + (double) (this.tickCount - greying) / (double) (maxAge - greying);
        }
        return 1.0D;
    }

    public float effectAlpha() {
        int maxAge = maxAge();
        int fadeOut = maxAge * 3 / 4;
        if (this.tickCount > fadeOut) {
            return 1.0F - (float) (this.tickCount - fadeOut) / (float) (maxAge - fadeOut);
        }
        return 1.0F;
    }

    public int maxAge() {
        return (int) (45 * 20 * getScale());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ticksExisted", this.tickCount);
        tag.putFloat("scale", this.entityData.get(SCALE));
        tag.putInt("type", this.entityData.get(TYPE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getInt("ticksExisted");
        this.entityData.set(SCALE, tag.contains("scale") ? tag.getFloat("scale") : 1.0F);
        this.entityData.set(TYPE, tag.getInt("type"));
    }

    public final class Cloudlet {
        public double posX;
        public double posY;
        public double posZ;
        public double prevPosX;
        public double prevPosY;
        public double prevPosZ;
        public int age;
        public final int cloudletLife;
        public final float angle;
        public boolean dead;
        public float rangeMod = 1.0F;
        public float colorMod = 1.0F;
        public Vec3 color;
        public Vec3 prevColor;
        public final TorexType type;
        private float startScale = 1.0F;
        private float growScale = 5.0F;
        private double motionMult = 1.0D;

        public Cloudlet(double posX, double posY, double posZ, float angle, int cloudletLife, TorexType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.prevPosX = posX;
            this.prevPosY = posY;
            this.prevPosZ = posZ;
            this.angle = angle;
            this.cloudletLife = Math.max(1, cloudletLife);
            this.rangeMod = 0.3F + random.nextFloat() * 0.7F;
            this.colorMod = 0.8F + random.nextFloat() * 0.2F;
            this.type = type;
            updateColor();
            this.prevColor = this.color;
        }

        private void update() {
            this.age++;
            if (this.age > this.cloudletLife) {
                this.dead = true;
                return;
            }
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            Vec3 motion = switch (this.type) {
                case STANDARD -> standardMotion();
                case SHOCK -> shockMotion();
                case RING -> ringMotion();
                case CONDENSATION -> condensationMotion();
            };
            double mult = this.motionMult * simulationSpeed();
            this.posX += motion.x * mult;
            this.posY += motion.y * mult;
            this.posZ += motion.z * mult;
            updateColor();
        }

        private Vec3 standardMotion() {
            double simX = getX() + horizontalDistance();
            double factor = Mth.clamp((this.posY - getY()) / coreHeight, 0.0D, 1.0D);
            return convectionMotion(simX).scale(factor).add(liftMotion(simX).scale(1.0D - factor));
        }

        private Vec3 shockMotion() {
            double factor = Mth.clamp((this.posY - getY()) / coreHeight, 0.0D, 1.0D);
            return new Vec3(Math.cos(this.angle) * factor, 0.0D, Math.sin(this.angle) * factor);
        }

        private Vec3 ringMotion() {
            double simX = getX() + horizontalDistance();
            if (simX > getX() + torusWidth * 2.0D) {
                return Vec3.ZERO;
            }
            return torusMotion(simX, getY() + coreHeight * 0.5D, rollerSize * this.rangeMod * 0.25D, 0.001D);
        }

        private Vec3 convectionMotion(double simX) {
            return torusMotion(simX, getY() + coreHeight, rollerSize * this.rangeMod, 1.0D);
        }

        private Vec3 torusMotion(double simX, double torusY, double roller, double speed) {
            double torusX = getX() + torusWidth;
            Vec3 delta = new Vec3(torusX - simX, torusY - this.posY, 0.0D);
            double dist = Math.max(0.001D, delta.length() / Math.max(0.001D, roller) - 1.0D);
            double func = 1.0D - Math.exp(-dist);
            double rot = func * Math.PI * 0.5D;
            Vec3 radial = delta.scale(-1.0D / dist);
            double rx = radial.x * Math.cos(rot) - radial.y * Math.sin(rot);
            double ry = radial.x * Math.sin(rot) + radial.y * Math.cos(rot);
            Vec3 motion = new Vec3(torusX + rx - simX, torusY + ry - this.posY, 0.0D);
            if (motion.lengthSqr() < 1.0E-6D) {
                return Vec3.ZERO;
            }
            motion = speed == 1.0D ? motion.normalize() : motion.scale(speed).normalize();
            return rotateY(motion, this.angle);
        }

        private Vec3 liftMotion(double simX) {
            double scale = Mth.clamp(1.0D - (simX - (getX() + torusWidth)), 0.0D, 1.0D);
            Vec3 motion = new Vec3(getX() - this.posX, getY() + convectionHeight - this.posY, getZ() - this.posZ);
            return motion.lengthSqr() < 1.0E-6D ? Vec3.ZERO : motion.normalize().scale(scale);
        }

        private Vec3 condensationMotion() {
            Vec3 delta = new Vec3(this.posX - getX(), 0.0D, this.posZ - getZ());
            return delta.scale(0.00002D * tickCount);
        }

        private Vec3 rotateY(Vec3 vec, float angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            return new Vec3(vec.x * cos + vec.z * sin, vec.y, vec.z * cos - vec.x * sin);
        }

        private double horizontalDistance() {
            double dx = getX() - this.posX;
            double dz = getZ() - this.posZ;
            return Math.sqrt(dx * dx + dz * dz);
        }

        private void updateColor() {
            this.prevColor = this.color;
            double dx = getX() - this.posX;
            double dy = getY() + coreHeight - this.posY;
            double dz = getZ() - this.posZ;
            double dist = Math.sqrt((dx * dx + dy * dy + dz * dz) / Math.max(1.0D, heat));
            double col = 2.0D / Math.max(1.0D, dist);
            int torexType = getTorexType();
            if (this.type == TorexType.CONDENSATION) {
                this.color = new Vec3(1.0D, 1.0D, 1.0D);
            } else if (torexType == 1) {
                this.color = new Vec3(Math.max(col, 0.25D), Math.max(col * 2.0D, 0.25D), Math.max(col * 0.5D, 0.25D));
            } else if (torexType == 2 && this.type != TorexType.RING) {
                Color hue = Color.getHSBColor(this.angle / 2.0F / (float) Math.PI, 1.0F, 1.0F);
                this.color = new Vec3(hue.getRed() / 255.0D, hue.getGreen() / 255.0D, hue.getBlue() / 255.0D);
            } else {
                this.color = new Vec3(Math.max(col * 2.0D, 0.25D), Math.max(col * 1.5D, 0.25D), Math.max(col * 0.5D, 0.25D));
            }
        }

        public Vec3 interpolatedPos(float partialTick) {
            Vec3 base = new Vec3(
                    Mth.lerp(partialTick, this.prevPosX, this.posX),
                    Mth.lerp(partialTick, this.prevPosY, this.posY),
                    Mth.lerp(partialTick, this.prevPosZ, this.posZ)
            );
            if (this.type != TorexType.SHOCK) {
                float scale = (float) getScale();
                base = new Vec3(
                        (base.x - getX()) * scale + getX(),
                        (base.y - getY()) * scale + getY(),
                        (base.z - getZ()) * scale + getZ()
                );
            }
            return base;
        }

        public Vec3 interpolatedColor(float partialTick) {
            if (this.type == TorexType.CONDENSATION) {
                return new Vec3(1.0D, 1.0D, 1.0D);
            }
            double greying = greying() + (this.type == TorexType.RING ? 1.0D : 0.0D);
            return new Vec3(
                    Mth.lerp(partialTick, this.prevColor.x, this.color.x) * greying,
                    Mth.lerp(partialTick, this.prevColor.y, this.color.y) * greying,
                    Mth.lerp(partialTick, this.prevColor.z, this.color.z) * greying
            );
        }

        public float alpha() {
            float alpha = (1.0F - this.age / (float) this.cloudletLife) * effectAlpha();
            if (this.type == TorexType.CONDENSATION) {
                alpha *= 0.25F;
            }
            return alpha;
        }

        public float renderScale() {
            float base = this.startScale + (this.age / (float) this.cloudletLife) * this.growScale;
            if (this.type != TorexType.SHOCK) {
                base *= (float) getScale();
            }
            return base;
        }

        public Cloudlet setScale(float start, float grow) {
            this.startScale = start;
            this.growScale = grow;
            return this;
        }

        public Cloudlet setMotion(double motionMult) {
            this.motionMult = motionMult;
            return this;
        }
    }

    public enum TorexType {
        STANDARD,
        SHOCK,
        RING,
        CONDENSATION
    }
}
