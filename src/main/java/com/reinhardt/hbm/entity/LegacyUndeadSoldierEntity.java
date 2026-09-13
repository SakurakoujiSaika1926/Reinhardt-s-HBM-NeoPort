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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 EntityUndeadSoldier. The old five firearm stacks remain deliberately
 * absent because the basic HBM firearm family was retired from this port.
 */
public final class LegacyUndeadSoldierEntity extends Monster {
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(
            LegacyUndeadSoldierEntity.class, EntityDataSerializers.BYTE);
    public static final byte TYPE_ZOMBIE = 0;
    public static final byte TYPE_SKELETON = 1;

    public LegacyUndeadSoldierEntity(EntityType<? extends LegacyUndeadSoldierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    /** EntityUndeadSoldier only registered these six legacy goals; it did not
     * inherit Zombie's door breaking, conversion, reinforcement or melee goal. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, 0, true, false, null));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<Villager>(this, Villager.class, 0, true, false, null));
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
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setCanPickUpLoot(false);
        return result;
    }

    public boolean isSkeletonVariant() {
        return entityData.get(TYPE) == TYPE_SKELETON;
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(isSkeletonVariant() ? SoundEvents.SKELETON_STEP : SoundEvents.ZOMBIE_STEP,
                0.15F, 1.0F);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        // EntityUndeadSoldier overrides both legacy item and equipment drops with no output.
    }

}
