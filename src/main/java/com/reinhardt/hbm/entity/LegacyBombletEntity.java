package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.explosion.LegacyMukeExplosion;
import com.reinhardt.hbm.explosion.NukeExplosionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Direct 1.7.10 EntityBombletZeta port used by the airstrike designator. */
public final class LegacyBombletEntity extends Entity {
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(LegacyBombletEntity.class, EntityDataSerializers.INT);

    public LegacyBombletEntity(EntityType<? extends LegacyBombletEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LegacyBombletEntity(Level level, Vec3 position, Vec3 motion, int type) {
        this(HbmEntityTypes.LEGACY_BOMBLET.get(), level);
        setPos(position);
        setDeltaMovement(motion);
        entityData.set(TYPE, type);
        updateRotation(motion);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.x * 0.99D, motion.y - 0.05D, motion.z * 0.99D);
        updateRotation(getDeltaMovement());

        if (!level().isClientSide && !level().getBlockState(legacyBlockPos()).isAir()) {
            impact();
        }
    }

    private void impact() {
        Vec3 position = position();
        switch (entityData.get(TYPE)) {
            case 0 -> LegacyProjectileUtil.standardExplosion(this, position.add(0.5D, 1.5D, 0.5D), 4.0F, 1.0F, true, true);
            case 1 -> {
                LegacyProjectileUtil.standardExplosion(this, position.add(0.5D, 1.5D, 0.5D), 4.0F, 1.0F, true, true);
                LegacyProjectileUtil.igniteArea(level(), legacyBlockPos(), 4);
            }
            case 2 -> {
                level().playSound(null, getX() + 0.5D, getY() + 0.5D, getZ() + 0.5D, SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.HOSTILE, 5.0F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
                Vec3 motion = getDeltaMovement();
                level().addFreshEntity(new LegacyMistEntity(
                        level(),
                        getX() - motion.x,
                        getY() - motion.y,
                        getZ() - motion.z,
                        LegacyMistEntity.MistType.CHLORINE,
                        15.0F,
                        7.5F,
                        150
                ));
            }
            case 4 -> {
                int radius = (int) (HbmConfig.FATMAN_RADIUS.get() * 1.5D);
                if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    NukeExplosionManager.scheduleMk5Nuclear(serverLevel,
                            position.x, position.y, position.z, radius);
                    LegacyMukeExplosion.sendMukeEffect(serverLevel, position);
                }
            }
            default -> {
                // The old entity has no payload behavior for its unused type values.
            }
        }
        discard();
    }

    private BlockPos legacyBlockPos() {
        return new BlockPos((int) getX(), (int) getY(), (int) getZ());
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        yRotO = getYRot();
        xRotO = getXRot();
        setYRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(motion.x, motion.z))));
        setXRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(motion.y, horizontal)) - 90.0D));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("type", entityData.get(TYPE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TYPE, tag.getInt("type"));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25_000.0D;
    }
}
