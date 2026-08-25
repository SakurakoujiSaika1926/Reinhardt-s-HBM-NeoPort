package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Direct port of EntityBobmazon's four 0.5-block descent steps per tick. */
public final class LegacyBobmazonEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> PAYLOAD =
            SynchedEntityData.defineId(LegacyBobmazonEntity.class, EntityDataSerializers.ITEM_STACK);

    public LegacyBobmazonEntity(EntityType<? extends LegacyBobmazonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public LegacyBobmazonEntity(Level level, double x, double z, ItemStack payload) {
        this(HbmEntityTypes.LEGACY_BOBMAZON.get(), level);
        setPos(x, 300.0D, z);
        setPayload(payload);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PAYLOAD, ItemStack.EMPTY);
    }

    public ItemStack payload() {
        return entityData.get(PAYLOAD);
    }

    public void setPayload(ItemStack payload) {
        entityData.set(PAYLOAD, payload.copy());
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 1.0D, getZ(), 0.0D, 0.03D, 0.0D);
            return;
        }

        for (int step = 0; step < 4; step++) {
            if (!level().getBlockState(impactPos()).isAir()) {
                deliver();
                return;
            }
            setPos(getX(), getY() - 0.5D, getZ());
        }
    }

    private BlockPos impactPos() {
        return new BlockPos((int) (getX() - 0.5D), (int) (getY() + 1.0D), (int) (getZ() - 0.5D));
    }

    private void deliver() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 50, 1.5D, 1.5D, 1.5D, 0.02D);
        }
        level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.ENTITY_OLD_EXPLOSION.get(),
                SoundSource.HOSTILE, 10.0F, 0.5F + random.nextFloat() * 0.1F);
        if (!payload().isEmpty()) {
            ItemEntity item = new ItemEntity(level(), getX(), getY() + 2.0D, getZ(), payload().copy());
            item.setDeltaMovement(0.0D, 0.0D, 0.0D);
            level().addFreshEntity(item);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("payload", payload().saveOptional(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setPayload(ItemStack.parseOptional(registryAccess(), tag.getCompound("payload")));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500_000.0D;
    }
}
