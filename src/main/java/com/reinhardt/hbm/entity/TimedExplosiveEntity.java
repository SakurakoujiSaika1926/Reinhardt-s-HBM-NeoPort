package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.block.DetonatableBlock;
import com.reinhardt.hbm.blockentity.WallChargeExplosions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Moving 1.7.10 EntityTNTPrimedBase for the Semtex and C-4 blocks. */
public final class TimedExplosiveEntity extends Entity {
    private static final EntityDataAccessor<Integer> FUSE =
            SynchedEntityData.defineId(TimedExplosiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(TimedExplosiveEntity.class, EntityDataSerializers.INT);
    private Entity owner;

    public TimedExplosiveEntity(EntityType<? extends TimedExplosiveEntity> type, Level level) {
        super(type, level);
    }

    public TimedExplosiveEntity(Level level, double x, double y, double z, Entity owner, int fuse, Kind kind) {
        this(HbmEntityTypes.TIMED_EXPLOSIVE.get(), level);
        setPos(x, y, z);
        this.owner = owner;
        setFuse(fuse);
        setKind(kind);
        float angle = (float) (random.nextDouble() * Math.PI * 2.0D);
        setDeltaMovement(-Math.sin(angle) * 0.02D, 0.2D, -Math.cos(angle) * 0.02D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUSE, 80);
        builder.define(KIND, Kind.SEMTEX.ordinal());
    }

    public int fuse() {
        return entityData.get(FUSE);
    }

    public void setFuse(int fuse) {
        entityData.set(FUSE, fuse);
    }

    public Kind kind() {
        return Kind.byOrdinal(entityData.get(KIND));
    }

    public void setKind(Kind kind) {
        entityData.set(KIND, kind.ordinal());
    }

    public Entity getOwner() {
        return owner;
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();

        Vec3 motion = getDeltaMovement().add(0.0D, -0.04D, 0.0D);
        move(MoverType.SELF, motion);
        motion = getDeltaMovement().scale(0.98D);
        if (onGround()) {
            motion = new Vec3(motion.x * 0.7D, motion.y * -0.5D, motion.z * 0.7D);
        }
        setDeltaMovement(motion);

        int fuse = fuse();
        if (fuse <= 0) {
            discard();
            if (!level().isClientSide) {
                if (level() instanceof net.minecraft.server.level.ServerLevel server
                        && isDetonatableKind(kind())) {
                    DetonatableBlock.detonatePrimed(server,
                            net.minecraft.core.BlockPos.containing(getX(), getY(), getZ()), kind(), owner);
                } else if (kind() == Kind.FISSURE && level() instanceof net.minecraft.server.level.ServerLevel server) {
                    com.reinhardt.hbm.block.FissureBombBehavior.detonate(server, this, new Vec3(getX(), getY(), getZ()));
                } else {
                    level().explode(this, getX(), getY(), getZ(), kind().explosionRadius(), true, Level.ExplosionInteraction.TNT);
                }
            }
            return;
        }
        setFuse(fuse - 1);
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5D, getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean isDetonatableKind(Kind kind) {
        return switch (kind) {
            case DET_CORD, DET_CHARGE, DET_NUKE, DET_MINER -> true;
            default -> false;
        };
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuse", fuse());
        tag.putString("Kind", kind().getSerializedName());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setFuse(tag.getInt("Fuse"));
        setKind(Kind.byId(tag.getString("Kind")));
    }

    public enum Kind implements StringRepresentable {
        DYNAMITE("dynamite", 8.0F),
        TNT("tnt_ntm", 10.0F),
        SEMTEX("semtex", 12.0F),
        C4("c4", 15.0F),
        FISSURE("fissure_bomb", 5.0F),
        DET_CORD("det_cord", 0.0F),
        DET_CHARGE("det_charge", 0.0F),
        DET_NUKE("det_nuke", 0.0F),
        DET_MINER("det_miner", 0.0F);

        private final String id;
        private final float explosionRadius;

        Kind(String id, float explosionRadius) {
            this.id = id;
            this.explosionRadius = explosionRadius;
        }

        public float explosionRadius() {
            return explosionRadius;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        public static Kind byOrdinal(int ordinal) {
            Kind[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SEMTEX;
        }

        public static Kind byId(String id) {
            for (Kind value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return SEMTEX;
        }
    }
}
