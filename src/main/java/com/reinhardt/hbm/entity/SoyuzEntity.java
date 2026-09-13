package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.satellite.SatelliteSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SoyuzEntity extends Entity {
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(SoyuzEntity.class, EntityDataSerializers.INT);
    private double acceleration;
    private int mode;
    private int targetX;
    private int targetZ;
    private boolean playedSoyuzed;
    private final ItemStack[] payload = new ItemStack[18];

    public SoyuzEntity(EntityType<? extends SoyuzEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        setNoGravity(true);
        for (int i = 0; i < this.payload.length; i++) {
            this.payload[i] = ItemStack.EMPTY;
        }
    }

    public SoyuzEntity(Level level, int mode, int targetX, int targetZ, int skin) {
        this(HbmEntityTypes.SOYUZ.get(), level);
        this.mode = mode;
        this.targetX = targetX;
        this.targetZ = targetZ;
        setSkin(skin);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SKIN, 0);
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        if (motion.y < 2.0D) {
            this.acceleration += 0.00025D;
            motion = new Vec3(motion.x, motion.y + this.acceleration, motion.z);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);

        if (level().isClientSide) {
            spawnClientExhaust();
        } else {
            burnEntitiesBelow();
            spawnServerExhaust();
            if (getY() > 600.0D) {
                if (this.mode == 1) {
                    spawnCapsule();
                } else if (level() instanceof ServerLevel serverLevel) {
                    ItemStack payload = this.payload[0];
                    if (payload.is(HbmItems.FLAME_PONY.get())) {
                        HbmAdvancements.awardAll(serverLevel, "space");
                    }
                    if (payload.is(HbmItems.SAT_FOEQ.get())) {
                        HbmAdvancements.awardAll(serverLevel, "foeq");
                    }
                    SatelliteSavedData.get(serverLevel).orbit(serverLevel, payload);
                }
                discard();
            }
        }
    }

    public void setSkin(int skin) {
        this.entityData.set(SKIN, Math.max(0, skin));
    }

    public int skin() {
        return this.entityData.get(SKIN);
    }

    public void setCargoTarget(int x, int z) {
        this.targetX = x;
        this.targetZ = z;
    }

    public void setPayload(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.payload.length) {
            this.payload[slot] = stack.copy();
        }
    }

    private void burnEntitiesBelow() {
        List<Entity> entities = level().getEntities(this, new AABB(getX() - 5.0D, getY() - 15.0D, getZ() - 5.0D, getX() + 5.0D, getY(), getZ() + 5.0D));
        DamageSource source = damageSources().onFire();
        for (Entity entity : entities) {
            entity.igniteForSeconds(15.0F);
            entity.hurt(source, 100.0F);
            if (entity instanceof Player player) {
                HbmAdvancements.award(player, "soyuz");
                if (!this.playedSoyuzed) {
                    this.playedSoyuzed = true;
                    level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.SOYUZED.get(), SoundSource.RECORDS, 100.0F, 1.0F);
                }
            }
        }
    }

    private void spawnClientExhaust() {
        spawnExhaust(getX(), getY(), getZ(), 3);
        spawnExhaust(getX() + 2.75D, getY(), getZ(), 2);
        spawnExhaust(getX() - 2.75D, getY(), getZ(), 2);
        spawnExhaust(getX(), getY(), getZ() + 2.75D, 2);
        spawnExhaust(getX(), getY(), getZ() - 2.75D, 2);
    }

    private void spawnServerExhaust() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 2 != 0) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() - 1.0D, getZ(), 20, 1.5D, 0.4D, 1.5D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() - 0.5D, getZ(), 10, 1.2D, 0.2D, 1.2D, 0.05D);
    }

    private void spawnExhaust(double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            level().addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x + (random.nextDouble() - 0.5D) * 0.8D,
                    y - random.nextDouble() * 2.0D,
                    z + (random.nextDouble() - 0.5D) * 0.8D,
                    (random.nextDouble() - 0.5D) * 0.08D,
                    -0.05D - random.nextDouble() * 0.05D,
                    (random.nextDouble() - 0.5D) * 0.08D
            );
        }
    }

    private void spawnCapsule() {
        SoyuzCapsuleEntity capsule = new SoyuzCapsuleEntity(level(), skin());
        capsule.setPos(this.targetX + 0.5D, 600.0D, this.targetZ + 0.5D);
        for (int i = 0; i < this.payload.length; i++) {
            capsule.setPayload(i, this.payload[i]);
        }
        level().addFreshEntity(capsule);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("Acceleration", this.acceleration);
        tag.putInt("Mode", this.mode);
        tag.putInt("TargetX", this.targetX);
        tag.putInt("TargetZ", this.targetZ);
        tag.putInt("Skin", skin());
        tag.putBoolean("Soyuzed", this.playedSoyuzed);
        for (int i = 0; i < this.payload.length; i++) {
            if (!this.payload[i].isEmpty()) {
                tag.put("Payload" + i, this.payload[i].saveOptional(registryAccess()));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.acceleration = tag.getDouble("Acceleration");
        this.mode = tag.getInt("Mode");
        this.targetX = tag.getInt("TargetX");
        this.targetZ = tag.getInt("TargetZ");
        setSkin(tag.getInt("Skin"));
        this.playedSoyuzed = tag.getBoolean("Soyuzed");
        for (int i = 0; i < this.payload.length; i++) {
            this.payload[i] = ItemStack.parseOptional(registryAccess(), tag.getCompound("Payload" + i));
        }
    }
}
