package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityPigeon, with the vanilla animal flight-free movement base. */
public final class LegacyPigeonEntity extends Animal {
    private static final EntityDataAccessor<Byte> FAT = SynchedEntityData.defineId(
            LegacyPigeonEntity.class, EntityDataSerializers.BYTE);

    public float fallTime;
    public float prevFallTime;
    public float dest;
    public float prevDest;
    public float offGroundTimer = 1.0F;

    public LegacyPigeonEntity(EntityType<? extends LegacyPigeonEntity> type, Level level) {
        super(type, level);
        setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.2D));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FAT, (byte) 0);
    }

    public boolean isFat() {
        return entityData.get(FAT) != 0;
    }

    public void setFat(boolean fat) {
        entityData.set(FAT, (byte) (fat ? 1 : 0));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Fat", isFat());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFat(tag.getBoolean("Fat"));
    }

    @Override
    public void tick() {
        super.tick();
        prevFallTime = fallTime;
        prevDest = dest;
        dest += (onGround() ? -1.0F : 4.0F) * 0.3F;
        dest = Math.max(0.0F, Math.min(1.0F, dest));
        if (!onGround() && offGroundTimer < 1.0F) {
            offGroundTimer = 1.0F;
        }
        offGroundTimer *= 0.9F;
        if (!onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        }
        fallTime += offGroundTimer * 2.0F;
    }

    @Override
    public LegacyPigeonEntity getBreedOffspring(net.minecraft.server.level.ServerLevel level,
                                                 AgeableMob parent) {
        return HbmEntityTypes.PIGEON.get().create(level);
    }

    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.WHEAT_SEEDS);
    }
}
