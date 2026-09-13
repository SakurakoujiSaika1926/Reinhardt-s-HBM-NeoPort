package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class LegacyLingeringFireEntity extends Entity {
    private static final EntityDataAccessor<Integer> FIRE_TYPE =
            SynchedEntityData.defineId(LegacyLingeringFireEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AREA_WIDTH =
            SynchedEntityData.defineId(LegacyLingeringFireEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AREA_HEIGHT =
            SynchedEntityData.defineId(LegacyLingeringFireEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(LegacyLingeringFireEntity.class, EntityDataSerializers.INT);
    private static final ParticleOptions BALEFIRE_PARTICLE =
            new DustParticleOptions(new Vector3f(0.16F, 0.88F, 0.18F), 1.25F);

    public LegacyLingeringFireEntity(EntityType<? extends LegacyLingeringFireEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.noCulling = true;
    }

    public LegacyLingeringFireEntity(Level level, Vec3 position, FireType type, float width, float height, int duration) {
        this(HbmEntityTypes.LEGACY_LINGERING_FIRE.get(), level);
        setPos(position);
        this.entityData.set(FIRE_TYPE, type.id());
        this.entityData.set(AREA_WIDTH, width);
        this.entityData.set(AREA_HEIGHT, height);
        this.entityData.set(MAX_AGE, duration);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FIRE_TYPE, FireType.DIESEL.id());
        builder.define(AREA_WIDTH, 0.0F);
        builder.define(AREA_HEIGHT, 0.0F);
        builder.define(MAX_AGE, 150);
    }

    @Override
    public void tick() {
        super.tick();
        // 1.7.10 calls setSize(watchedWidth, watchedHeight) every update.
        refreshDimensions();
        if (level().isClientSide) {
            spawnLegacyFlames();
            return;
        }
        if (tickCount >= this.entityData.get(MAX_AGE)) {
            discard();
            return;
        }

        float width = this.entityData.get(AREA_WIDTH);
        float height = this.entityData.get(AREA_HEIGHT);
        AABB area = new AABB(
                getX() - width * 0.5D,
                getY(),
                getZ() - width * 0.5D,
                getX() + width * 0.5D,
                getY() + height,
                getZ() + width * 0.5D
        );
        FireType type = fireType();
        for (Entity entity : level().getEntities(this, area, Entity::isAlive)) {
            if (!(entity instanceof LivingEntity living)) {
                entity.igniteForSeconds(4.0F);
                continue;
            }
            switch (type) {
                case DIESEL -> HbmLivingHazards.get(living).extendFire(60);
                case PHOSPHORUS -> HbmLivingHazards.get(living).extendFire(300);
                case BALEFIRE -> HbmLivingHazards.get(living).extendBalefire(100);
                case BLACK -> HbmLivingHazards.get(living).extendBlackFire();
                case OXY -> {
                    // TYPE_OXY has no living hazard in the 1.7.10 handler; it only
                    // affects non-living entities through the branch above.
                }
            }
        }
    }

    private void spawnLegacyFlames() {
        float width = this.entityData.get(AREA_WIDTH);
        float height = this.entityData.get(AREA_HEIGHT);
        int count = width >= 5.0F ? 2 : 1;
        ParticleOptions particle = switch (fireType()) {
            case BALEFIRE -> BALEFIRE_PARTICLE;
            case BLACK -> HbmParticleTypes.FLAMETHROWER_BLACK.get();
            default -> ParticleTypes.FLAME;
        };
        for (int i = 0; i < count; i++) {
            double x = getX() - width * 0.5D + random.nextDouble() * width;
            double z = getZ() - width * 0.5D + random.nextDouble() * width;
            Vec3 top = new Vec3(x, getY() + height, z);
            Vec3 bottom = new Vec3(x, getY() - height, z);
            HitResult hit = level().clip(new ClipContext(top, bottom, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            double y = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y : bottom.y;
            level().addParticle(particle, x, y + 0.05D, z, 0.0D, 0.01D, 0.0D);
        }
    }

    private FireType fireType() {
        return FireType.byId(this.entityData.get(FIRE_TYPE));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (AREA_WIDTH.equals(key) || AREA_HEIGHT.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(this.entityData.get(AREA_WIDTH), this.entityData.get(AREA_HEIGHT));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double width = this.entityData.get(AREA_WIDTH);
        double height = this.entityData.get(AREA_HEIGHT);
        double edge = (2.0D * width + height) / 3.0D * 64.0D;
        return distance < edge * edge;
    }

    public enum FireType {
        DIESEL(0),
        BALEFIRE(1),
        PHOSPHORUS(2),
        OXY(3),
        BLACK(4);

        private final int id;

        FireType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static FireType byId(int id) {
            for (FireType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return DIESEL;
        }
    }
}
