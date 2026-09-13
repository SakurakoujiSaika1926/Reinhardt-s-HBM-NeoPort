package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.blockentity.TurretCasingEffects;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class CogEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(CogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> META =
            SynchedEntityData.defineId(CogEntity.class, EntityDataSerializers.INT);

    private boolean collisionDisabled;

    public CogEntity(EntityType<? extends CogEntity> entityType, Level level) {
        super(entityType, level);
    }

    public CogEntity(Level level, double x, double y, double z) {
        this(HbmEntityTypes.COG.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ORIENTATION, 0);
        builder.define(META, 0);
    }

    public CogEntity setOrientation(int orientation) {
        this.entityData.set(ORIENTATION, orientation);
        return this;
    }

    public int orientation() {
        return this.entityData.get(ORIENTATION);
    }

    public CogEntity setMeta(int meta) {
        this.entityData.set(META, meta);
        return this;
    }

    public int meta() {
        return this.entityData.get(META);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return pickUp(player);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        return pickUp(player);
    }

    private InteractionResult pickUp(Player player) {
        if (!level().isClientSide) {
            ItemStack stack = gearStack(this.meta());
            if (player.getInventory().add(stack)) {
                discard();
                player.inventoryMenu.broadcastChanges();
            }
        }
        // EntityCog#interactFirst always returned false after attempting the
        // pickup; preserve that interaction result instead of consuming the
        // right-click in the modern wrapper.
        return InteractionResult.PASS;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (!level().isClientSide && entity.isAlive()) {
            boolean wasAlive = entity.isAlive();
            entity.hurt(damageSources().source(HbmDamageTypes.RUBBLE, this, this), 1000.0F);
            if (wasAlive && !entity.isAlive() && entity instanceof LivingEntity living) {
                TurretCasingEffects.spawnMaxwellGib(level(), living, false);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
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
                setDeltaMovement(
                        motion.x * (1 - Math.abs(side.getStepX()) * 2),
                        motion.y * (1 - Math.abs(side.getStepY()) * 2),
                        motion.z * (1 - Math.abs(side.getStepZ()) * 2)
                );
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
        if (!level().isClientSide) {
            int orientation = orientation();
            if (orientation >= 6 && !onGround()) {
                setOrientation(orientation - 6);
            }
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
    public boolean canBeCollidedWith() {
        return true;
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
        tag.putInt("meta", meta());
        tag.putBoolean("collisionDisabled", this.collisionDisabled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setOrientation(tag.getInt("rot"));
        setMeta(tag.getInt("meta"));
        this.collisionDisabled = tag.getBoolean("collisionDisabled");
    }

    private void breakWeakHitBlock(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (state.getDestroySpeed(level(), pos) >= 0.0F && state.getBlock().getExplosionResistance() < 50.0F) {
            level().destroyBlock(pos, false);
        }
    }

    private static ItemStack gearStack(int meta) {
        if (HbmItems.GEAR_LARGE.get() instanceof LegacyVariantItem) {
            return LegacyVariantItem.stackFor(HbmItems.GEAR_LARGE, meta == 1 ? "steel" : "normal");
        }
        return new ItemStack(HbmItems.GEAR_LARGE.get());
    }
}
