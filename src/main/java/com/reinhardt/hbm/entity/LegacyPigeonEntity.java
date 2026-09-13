package com.reinhardt.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 EntityPigeon, with the vanilla animal flight-free movement base. */
public final class LegacyPigeonEntity extends PathfinderMob {
    private static final EntityDataAccessor<Byte> FAT = SynchedEntityData.defineId(
            LegacyPigeonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FLYING = SynchedEntityData.defineId(
            LegacyPigeonEntity.class, EntityDataSerializers.BYTE);

    public float fallTime;
    public float prevFallTime;
    public float dest;
    public float prevDest;
    public float offGroundTimer = 1.0F;

    public LegacyPigeonEntity(EntityType<? extends LegacyPigeonEntity> type, Level level) {
        super(type, level);
        // EntityAISwimmingConditional#setCanSwim(true) enabled water
        // navigation for the legacy pigeon.
        getNavigation().setCanFloat(true);
        setNoGravity(false);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                // EntityPigeon never overrides EntityCreature attributes in
                // 1.7.10 (the inherited values are 20 health and .25 speed).
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this) {
            @Override public boolean canUse() {
                return getFlyingState() == 0 && super.canUse();
            }
        });
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.2D) {
            @Override public boolean canUse() {
                return getFlyingState() == 0 && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return getFlyingState() == 0 && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FAT, (byte) 0);
        builder.define(FLYING, (byte) 0);
    }

    public boolean isFat() {
        return entityData.get(FAT) != 0;
    }

    public void setFat(boolean fat) {
        entityData.set(FAT, (byte) (fat ? 1 : 0));
    }

    public int getFlyingState() {
        return entityData.get(FLYING);
    }

    public void setFlyingState(int state) {
        entityData.set(FLYING, (byte) (state == 0 ? 0 : 1));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Fat", isFat());
        tag.putByte("Flying", (byte) getFlyingState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFat(tag.getBoolean("Fat"));
        setFlyingState(tag.getByte("Flying"));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (getFlyingState() == 0 && (getLastHurtByMob() != null || isOnFire() || random.nextInt(600) == 0)) {
                setFlyingState(1);
            } else if (getFlyingState() != 0 && random.nextInt(200) == 0) {
                setFlyingState(0);
            }

            if (!isFat() && getFlyingState() == 0) {
                for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(10.0D),
                        entity -> entity.getItem().is(Items.BREAD))) {
                    if (distanceToSqr(item) > 1.0D) {
                        getNavigation().moveTo(item, 0.4D);
                    } else {
                        ItemStack stack = item.getItem();
                        if (random.nextInt(3) == 0) {
                            // EntityAIEatBread in 1.7.10 replaced a stack
                            // larger than one with a new stack of count-1 at
                            // the exact same position, then killed the
                            // original entity.  Preserve that observable
                            // behavior instead of mutating the source stack.
                            if (stack.getCount() > 1) {
                                ItemEntity remainder = new ItemEntity(level(),
                                        item.getX(), item.getY(), item.getZ(),
                                        stack.copyWithCount(stack.getCount() - 1));
                                level().addFreshEntity(remainder);
                            }
                            item.discard();
                        }
                        setFat(true);
                        playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT,
                                0.5F + 0.5F * random.nextInt(2),
                                (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
                        break;
                    }
                }
            }
            // EntityPigeon's fat-flight action used ItemFertilizer to force
            // growth in the first viable block up to 25 blocks below it,
            // then had a 10% chance to lose its fat state.
            if (isFat() && getFlyingState() != 0 && random.nextInt(50) == 0
                    && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                BlockPos origin = blockPosition().below();
                for (int i = 0; i < 25; i++) {
                    BlockPos target = origin.below(i);
                    if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), serverLevel, target)) {
                        BoneMealItem.addGrowthParticles(serverLevel, target, 3);
                        serverLevel.levelEvent(2005, target, 0);
                        break;
                    }
                }
                if (random.nextInt(10) == 0) {
                    setFat(false);
                }
            }
        }
        if (getFlyingState() != 0) {
            setNoGravity(false);
            int surface = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    getBlockX(), getBlockZ());
            boolean ceiling = getY() - surface > 10.0D;
            double motionY = random.nextGaussian() * 0.05D + (ceiling ? 0.0D : 0.04D)
                    + (isInWater() ? 0.2D : 0.0D);
            if (onGround()) motionY = Math.abs(motionY) + 0.1D;
            if (random.nextInt(20) == 0) setYRot(getYRot() + (float) random.nextGaussian() * 30.0F);
            // EntityPigeon sets moveForward=1.5; airborne LivingEntity travel
            // applies that input with the legacy 0.02 air acceleration.
            moveRelative(0.02F, new Vec3(0.0D, 0.0D, 1.5D));
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, motionY, motion.z);
        } else {
            setNoGravity(false);
            if (!onGround() && getDeltaMovement().y < 0.0D) {
                setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.8D, 1.0D));
            }
        }
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
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // 1.7.10 EntityPigeon used the chicken step sound at this exact
        // volume, rather than inheriting the generic animal step.
        playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (amount >= getMaxHealth() * 2.0F && !level().isClientSide) {
            discard();
            for (int i = 0; i < 10; i++) {
                Vec3 velocity = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize().scale(0.5D);
                level().addFreshEntity(new ItemEntity(level(), getX() + velocity.x, getY() + getBbHeight() * 0.5D + velocity.y,
                        getZ() + velocity.z, new ItemStack(Items.FEATHER)) {{ setDeltaMovement(velocity); }});
            }
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        int looting = 0;
        if (getLastHurtByMob() != null && !getLastHurtByMob().getMainHandItem().isEmpty()) {
            looting = EnchantmentHelper.getItemEnchantmentLevel(
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING),
                    getLastHurtByMob().getMainHandItem());
        }
        int count = random.nextInt(3) + random.nextInt(1 + looting);
        for (int i = 0; i < count; i++) spawnAtLocation(Items.FEATHER);
        spawnAtLocation(isOnFire() ? Items.COOKED_CHICKEN : Items.CHICKEN, isFat() ? 3 : 1);
    }

}
