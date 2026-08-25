package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Exact gameplay port of 1.7.10 EntitySawblade. */
public final class SawbladeEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(SawbladeEntity.class, EntityDataSerializers.INT);
    private boolean collisionDisabled;

    public SawbladeEntity(EntityType<? extends SawbladeEntity> type, Level level) {
        super(type, level);
    }

    public SawbladeEntity(Level level, double x, double y, double z) {
        this(HbmEntityTypes.SAWBLADE.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ORIENTATION, 0);
    }

    public SawbladeEntity setOrientation(int orientation) {
        this.entityData.set(ORIENTATION, orientation);
        return this;
    }

    public int orientation() {
        return this.entityData.get(ORIENTATION);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return pickUp(player);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 location, InteractionHand hand) {
        return pickUp(player);
    }

    private InteractionResult pickUp(Player player) {
        if (!level().isClientSide && player.getInventory().add(sawbladeStack())) {
            discard();
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!level().isClientSide && target.isAlive()) {
            boolean wasAlive = target.isAlive();
            target.hurt(damageSources().source(HbmDamageTypes.RUBBLE, this, this), 1000.0F);
            if (wasAlive && !target.isAlive()) {
                level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F,
                        0.95F + level().random.nextFloat() * 0.2F);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide || this.tickCount <= 1) {
            return;
        }
        int orientation = orientation();
        if (orientation < 6) {
            Vec3 motion = getDeltaMovement();
            if (motion.length() < 0.75D) {
                setOrientation(orientation + 6);
                orientation += 6;
            } else {
                Direction side = result.getDirection();
                setDeltaMovement(motion.x * (1 - Math.abs(side.getStepX()) * 2),
                        motion.y * (1 - Math.abs(side.getStepY()) * 2),
                        motion.z * (1 - Math.abs(side.getStepZ()) * 2));
                level().explode(this, getX(), getY(), getZ(), 3.0F, Level.ExplosionInteraction.NONE);
                breakWeakHitBlock(result.getBlockPos());
            }
        }
        if (orientation >= 6) {
            setDeltaMovement(Vec3.ZERO);
            setOnGround(true);
            this.collisionDisabled = true;
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide && orientation() >= 6 && !onGround()) {
            setOrientation(orientation() - 6);
        }
        super.tick();
    }

    @Override
    protected double getDefaultGravity() {
        return this.onGround() || this.collisionDisabled ? 0.0D : 0.03D;
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public float getPickRadius() {
        return 0.35F;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !this.collisionDisabled && super.canBeHitByProjectile();
    }

    @Override
    public boolean isPushable() {
        return !this.collisionDisabled;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("rot", orientation());
        tag.putBoolean("collisionDisabled", this.collisionDisabled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setOrientation(tag.getInt("rot"));
        this.collisionDisabled = tag.getBoolean("collisionDisabled");
    }

    private void breakWeakHitBlock(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (!state.isAir() && state.getDestroySpeed(level(), pos) >= 0.0F && state.getBlock().getExplosionResistance() < 50.0F) {
            level().destroyBlock(pos, false);
        }
    }

    private static ItemStack sawbladeStack() {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("sawblade"));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
