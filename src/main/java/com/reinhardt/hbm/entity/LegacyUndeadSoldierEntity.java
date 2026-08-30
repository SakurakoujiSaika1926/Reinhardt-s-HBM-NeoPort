package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 EntityUndeadSoldier. The old five firearm stacks remain deliberately
 * absent because the basic HBM firearm family was retired from this port.
 */
public final class LegacyUndeadSoldierEntity extends Zombie {
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(
            LegacyUndeadSoldierEntity.class, EntityDataSerializers.BYTE);
    public static final byte TYPE_ZOMBIE = 0;
    public static final byte TYPE_SKELETON = 1;

    public LegacyUndeadSoldierEntity(EntityType<? extends LegacyUndeadSoldierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, TYPE_ZOMBIE);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        entityData.set(TYPE, random.nextBoolean() ? TYPE_ZOMBIE : TYPE_SKELETON);
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(HbmItems.TAURUN_HELMET.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(HbmItems.TAURUN_PLATE.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(HbmItems.TAURUN_LEGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(HbmItems.TAURUN_BOOTS.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
        return result;
    }

    public boolean isSkeletonVariant() {
        return entityData.get(TYPE) == TYPE_SKELETON;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isSkeletonVariant() ? SoundEvents.SKELETON_AMBIENT : SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isSkeletonVariant() ? SoundEvents.SKELETON_HURT : SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isSkeletonVariant() ? SoundEvents.SKELETON_DEATH : SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        // EntityUndeadSoldier overrides both legacy item and equipment drops with no output.
    }

}
