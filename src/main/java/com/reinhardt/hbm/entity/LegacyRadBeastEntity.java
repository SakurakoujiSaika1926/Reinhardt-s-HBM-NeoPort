package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 EntityRADBeast attributes and melee targeting. */
public final class LegacyRadBeastEntity extends Monster {
    private static final EntityDataAccessor<Integer> UNFORTUNATE_SOUL =
            SynchedEntityData.defineId(LegacyRadBeastEntity.class, EntityDataSerializers.INT);
    private float heightOffset = 0.5F;
    private int heightOffsetUpdateTime;
    private int attackCooldown;
    public LegacyRadBeastEntity(EntityType<? extends LegacyRadBeastEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 30;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(UNFORTUNATE_SOUL, 0);
    }

    /** The target id synchronized by EntityRADBeast for the client beam renderer. */
    public Entity getUnfortunateSoul() {
        int id = entityData.get(UNFORTUNATE_SOUL);
        return id == 0 ? null : level().getEntity(id);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    /** HBM 1.7.10 EntityRADBeast.makeLeader(). */
    public LegacyRadBeastEntity makeLeader() {
        setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        Item radiationCoin = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("coin_radiation"));
        if (radiationCoin == Items.AIR) {
            throw new IllegalStateException("Missing required legacy item reinhardtshbm:coin_radiation");
        }
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(radiationCoin));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(360.0D);
        setHealth(getMaxHealth());
        return this;
    }

    @Override
    protected void registerGoals() {
        // EntityRADBeast supplied its own melee/radiation attack from
        // updateAITasks; adding a modern MeleeAttackGoal would apply a
        // second vanilla hit on top of that legacy attack.
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return switch (random.nextInt(6)) {
            case 0 -> HbmSoundEvents.GEIGER_1.get();
            case 1 -> HbmSoundEvents.GEIGER_2.get();
            case 2 -> HbmSoundEvents.GEIGER_3.get();
            case 3 -> HbmSoundEvents.GEIGER_4.get();
            case 4 -> HbmSoundEvents.GEIGER_5.get();
            default -> HbmSoundEvents.GEIGER_6.get();
        };
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.BLAZE_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return HbmSoundEvents.STEP_IRON.get();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getLightLevelDependentMagicValue() {
        // EntityRADBeast#getBrightness returned 1.0F and
        // getBrightnessForRender returned full-bright (15728880), so the
        // modern light-dependent hook must not sample the world light.
        return 1.0F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (isInWaterOrRain()) hurt(damageSources().drown(), 1.0F);
            if (--heightOffsetUpdateTime <= 0) {
                heightOffsetUpdateTime = 100;
                heightOffset = 0.5F + (float) random.nextGaussian() * 3.0F;
            }
            LivingEntity target = getTarget();
            if (target != null && target.getEyeY() > getEyeY() + heightOffset) {
                setDeltaMovement(getDeltaMovement().add(0.0D,
                        (0.3D - getDeltaMovement().y) * 0.3D, 0.0D));
            }
            if (attackCooldown > 0) attackCooldown--;
            if (target != null && isAlive()) {
                double distance = distanceTo(target);
                if (distance < 2.0D
                        && target.getBoundingBox().maxY > getBoundingBox().minY
                        && target.getBoundingBox().minY < getBoundingBox().maxY) {
                    if (attackCooldown <= 0) {
                        attackCooldown = 20;
                        doHurtTarget(target);
                    }
                } else if (distance < 30.0D && attackCooldown <= 0) {
                    ChunkRadiationData.get((ServerLevel) level()).incrementRadiation(blockPosition(), 100.0D);
                    target.hurt(damageSources().source(HbmDamageTypes.RADIATION, this, this), 16.0F);
                    attackCooldown = 20;
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    playSound(getAmbientSound(), 1.0F, 1.0F);
                }
                if (distance < 30.0D) {
                    double deltaX = target.getX() - getX();
                    double deltaZ = target.getZ() - getZ();
                    float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
                    setYRot(yaw);
                    yBodyRot = yaw;
                    yHeadRot = yaw;
                }
            }
            if (target != null && attackCooldown < 10) {
                entityData.set(UNFORTUNATE_SOUL, target.getId());
            } else {
                entityData.set(UNFORTUNATE_SOUL, 0);
            }
        }
        if (!onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        }
        if (level().isClientSide) {
            if (getMaxHealth() <= 150.0D) {
                for (int i = 0; i < 6; i++) {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5D,
                            getY() + random.nextDouble() * getBbHeight(),
                            getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5D,
                            0.0D, 0.0D, 0.0D);
                }
                if (random.nextInt(6) == 0) {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                            getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                            getY() + random.nextDouble() * getBbHeight() * 0.75D,
                            getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                            0.0D, 0.0D, 0.0D);
                }
            } else {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.LAVA,
                        getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                        getY() + random.nextDouble() * getBbHeight() * 0.75D,
                        getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                        0.0D, 0.0D, 0.0D);
            }
        }
        super.tick();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (getMaxHealth() > 150.0F && level() instanceof ServerLevel level) {
            HbmAdvancements.awardNearby(level, getBoundingBox().inflate(50.0D), "boss_meltdown");
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        if (!recentlyHit) return;
        int looting = legacyLootingLevel();
        if (looting > 0) {
            spawnLegacyDrop(level, "nugget_polonium", looting);
        }
        int count = random.nextInt(3) + 1;
        for (int i = 0; i < count; i++) {
            int choice = random.nextInt(3);
            if (isInWaterOrRain()) {
                spawnLegacyDrop(level, choice == 0 ? "waste_uranium" : choice == 1 ? "waste_mox" : "waste_plutonium", 2);
            } else {
                spawnLegacyDrop(level, choice == 0 ? "rod_zirnox_uranium_fuel_depleted" :
                        choice == 1 ? "rod_zirnox_mox_fuel_depleted" : "rod_zirnox_plutonium_fuel_depleted", 1);
            }
        }
    }

    private void spawnLegacyDrop(ServerLevel level, String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(com.reinhardt.hbm.ReinhardtsHBM.id(id));
        if (item != Items.AIR) spawnAtLocation(new ItemStack(item, count));
    }

    private int legacyLootingLevel() {
        LivingEntity killer = getLastHurtByMob();
        if (killer == null) {
            return 0;
        }
        ItemStack weapon = killer.getMainHandItem();
        if (weapon.isEmpty()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                killer.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.LOOTING), weapon);
    }
}
