package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.block.DetonatableBlock;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The exact MASKMAN_BOLT profile emitted by the old orbital death ray.
 *
 * <p>This is deliberately not represented by {@link LegacyShrapnelEntity}:
 * the old bolt has no gravity or drag, penetrates entities, ricochets from
 * blocks with a fixed two-percent chance, and creates a non-griefing
 * half-strength VNT explosion when it stops.</p>
 */
public final class LegacyDeathRayBoltEntity extends Entity {
    private static final int MAX_AGE = 100;
    private static final float MIN_DAMAGE = 15.0F;
    private static final float MAX_DAMAGE = 20.0F;
    private static final float IMPACT_EXPLOSION_SIZE = 0.5F;
    /** BulletConfigFactory.standardBulletConfig().velocity. */
    private static final double MOTION_MULTIPLIER = 5.0D;
    private static final double BOUNCE_MULTIPLIER = 0.8D;
    private static final double TRAIL_SPACING = 0.5D;

    public LegacyDeathRayBoltEntity(EntityType<? extends LegacyDeathRayBoltEntity> type, Level level) {
        super(type, level);
    }

    public LegacyDeathRayBoltEntity(Level level, double x, double y, double z, Vec3 motion) {
        this(HbmEntityTypes.LEGACY_DEATH_RAY_BOLT.get(), level);
        setPos(x, y, z);
        // EntityDeathBlast writes the raw 0.2/-0.01 motion directly, then
        // EntityThrowableNT advances it through motionMult() == velocity 5.
        // Store the resulting per-tick displacement in the modern entity.
        Vec3 tickMotion = motion.scale(MOTION_MULTIPLIER);
        setDeltaMovement(tickMotion);
        updateRotation(tickMotion);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        updateRotation(motion);

        if (level().isClientSide) {
            setPos(position().add(motion));
            // EntityBulletBaseNT emits vPFX at every 0.5 block of actual
            // displacement, starting at the new position and walking back
            // along the normalized motion vector.
            double distance = Math.max(motion.length(), 0.1D);
            Vec3 direction = motion.lengthSqr() > 1.0E-12D ? motion.normalize() : Vec3.ZERO;
            for (double offset = 0.0D; offset < distance; offset += TRAIL_SPACING) {
                level().addParticle(DustParticleOptions.REDSTONE,
                        getX() - direction.x * offset,
                        getY() - direction.y * offset,
                        getZ() - direction.z * offset,
                        0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (tickCount > MAX_AGE || getY() < level().getMinBuildHeight() - 16) {
            discard();
            return;
        }

        Vec3 start = position();
        Vec3 end = start.add(motion);
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        // EntityThrowableNT calls onImpact for every intersected entity when
        // doesPenetrate is true. Preserve that behavior rather than selecting
        // only the nearest victim or consuming the bolt.
        for (EntityHitResult entityHit : findEntityHits(start, entityEnd)) {
            float damage = MIN_DAMAGE + random.nextFloat() * (MAX_DAMAGE - MIN_DAMAGE);
            LegacyProjectileUtil.hurtNoIFrame(entityHit.getEntity(),
                    level().damageSources().source(HbmDamageTypes.LASER), damage);
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            if (random.nextInt(100) < 2) {
                ricochet(blockHit, motion);
            } else {
                stopAtBlock(blockHit);
            }
            return;
        }

        setPos(end);
    }

    private List<EntityHitResult> findEntityHits(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        List<EntityHitResult> hits = new ArrayList<>();
        for (Entity entity : level().getEntities(this, area,
                candidate -> candidate.isAlive() && candidate.isPickable())) {
            Optional<Vec3> location = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            location.ifPresent(vec3 -> hits.add(new EntityHitResult(entity, vec3)));
        }
        return hits;
    }

    private void ricochet(BlockHitResult hit, Vec3 motion) {
        Vec3 reflected = switch (hit.getDirection().getAxis()) {
            case X -> new Vec3(-motion.x, motion.y, motion.z);
            case Y -> new Vec3(motion.x, -motion.y, motion.z);
            case Z -> new Vec3(motion.x, motion.y, -motion.z);
        };
        reflected = reflected.scale(BOUNCE_MULTIPLIER);
        setPos(hit.getLocation());
        setDeltaMovement(reflected);
        updateRotation(reflected);
        level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_RICOCHET.get(),
                SoundSource.PLAYERS, 0.25F, 1.0F);

        // The old throwable applies its (now reflected) movement after the
        // impact callback, moving the bolt back off the struck face.
        setPos(position().add(reflected));
    }

    private void stopAtBlock(BlockHitResult hit) {
        setPos(hit.getLocation());
        BlockPos blockPos = hit.getBlockPos();
        BlockState state = level().getBlockState(blockPos);

        LegacyProjectileUtil.deathRayBoltImpactExplosion(
                this, hit.getLocation(), IMPACT_EXPLOSION_SIZE);

        Block block = state.getBlock();
        if (isLegacyBreakableGlass(block)) {
            level().destroyBlock(blockPos, false, this);
        } else if (block instanceof DetonatableBlock detonatable) {
            detonatable.detonate(level(), blockPos, this);
        }
        discard();
    }

    private static boolean isLegacyBreakableGlass(Block block) {
        // 1.7.10 checked glass, glass panes, stained glass and stained panes.
        return block == Blocks.GLASS
                || block == Blocks.GLASS_PANE
                || block instanceof StainedGlassBlock
                || block instanceof StainedGlassPaneBlock;
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        setYRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(motion.x, motion.z))));
        setXRot((float) Mth.wrapDegrees(-Math.toDegrees(Math.atan2(motion.y, horizontal))));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityThrowableNT computes (.5 average edge * 4 * 64)^2.
        return distance < 16_384.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }
}
