package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Direct 1.7.10 EntityPlasticBag movement and drop behaviour. */
public final class LegacyPlasticBagEntity extends WaterAnimal {
    private float rotation;
    private float previousRotation;
    private float randomMotionSpeed;
    private float rotationVelocity;
    private float randomMotionX;
    private float randomMotionY;
    private float randomMotionZ;

    public LegacyPlasticBagEntity(EntityType<? extends LegacyPlasticBagEntity> type, Level level) {
        super(type, level);
        rotationVelocity = 1.0F / (random.nextFloat() + 1.0F) * 0.2F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                // EntityPlasticBag leaves EntityWaterMob's inherited
                // attributes untouched in 1.7.10.
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void tick() {
        super.tick();

        previousRotation = rotation;
        rotation += rotationVelocity;
        if (rotation > Math.PI * 2.0F) {
            rotation -= Math.PI * 2.0F;
            if (random.nextInt(10) == 0) {
                rotationVelocity = 1.0F / (random.nextFloat() + 1.0F) * 0.2F;
            }
        }

        boolean inWater = isLegacyInWater();
        if (inWater) {
            float phase = rotation < Math.PI
                    ? rotation / (float) Math.PI
                    : 0.0F;
            if (phase > 0.75F) {
                randomMotionSpeed = 0.1F;
            } else if (rotation >= Math.PI) {
                randomMotionSpeed *= 0.999F;
            }
            if (!level().isClientSide) {
                setDeltaMovement(randomMotionX * randomMotionSpeed,
                        randomMotionY * randomMotionSpeed,
                        randomMotionZ * randomMotionSpeed);
            }
            Vec3 motion = getDeltaMovement();
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (horizontal > 1.0E-7D) {
                float desiredYaw = (float) (-Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
                float smoothedYaw = yBodyRot
                        + (float) ((desiredYaw - yBodyRot) * 0.1D);
                setYRot(smoothedYaw);
                yBodyRot = smoothedYaw;
                yHeadRot = smoothedYaw;
            }
            setXRot((float) (Math.atan2(motion.y, horizontal) * 180.0D / Math.PI));
        } else if (!level().isClientSide) {
            setDeltaMovement(0.0D, (getDeltaMovement().y - 0.08D) * 0.98D, 0.0D);
        }

        updateRandomMotion(inWater);
    }

    @Override
    public void travel(Vec3 ignored) {
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && isAlive()) {
            discard();
            LegacyBuoyantItemEntity drop = new LegacyBuoyantItemEntity(level(), getX(), getY(), getZ(),
                    new net.minecraft.world.item.ItemStack(HbmItems.PLASTIC_BAG.get()));
            drop.setPickUpDelay(10);
            level().addFreshEntity(drop);
        }
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        if (reason == MobSpawnType.NATURAL
                && (getY() <= 45.0D || getY() >= 63.0D || random.nextInt(10) != 0)) {
            return false;
        }
        return super.checkSpawnRules(level, reason);
    }

    /** EntityPlasticBag#canTriggerWalking: the bag never activates block triggers. */
    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    private void updateRandomMotion(boolean inWater) {
        // EntityPlasticBag used EntityLiving.entityAge, which is the
        // modern mob's noActionTime counter (and is reset by the normal
        // nearby-player despawn check), rather than the immutable world
        // tickCount.
        if (noActionTime > 100) {
            randomMotionX = randomMotionY = randomMotionZ = 0.0F;
        } else if (random.nextInt(50) == 0 || !inWater
                || randomMotionX == 0.0F && randomMotionY == 0.0F && randomMotionZ == 0.0F) {
            float angle = random.nextFloat() * (float) Math.PI * 2.0F;
            randomMotionX = (float) Math.cos(angle) * 0.2F;
            randomMotionY = -0.1F + random.nextFloat() * 0.2F;
            randomMotionZ = (float) Math.sin(angle) * 0.2F;
        }
    }

    private boolean isLegacyInWater() {
        AABB box = getBoundingBox().move(0.0D, -0.6D, 0.0D);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (level().getFluidState(new BlockPos(x, y, z)).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
