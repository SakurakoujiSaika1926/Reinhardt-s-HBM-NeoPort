package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible, flaming fragments emitted by a Chicago Pile MK2 meltdown.
 *
 * <p>This is the exact gameplay contract of current-upstream
 * {@code TileEntityPileCore#pile_debris}: unit velocity, 0.35 heading spread,
 * 0.1 gravity, 200-tick lifetime, one fire-trail particle per client tick and
 * a strength-five flaming explosion which does not damage blocks.</p>
 */
public final class PileDebrisEntity extends Entity {
    private static final double HEADING_FORCE_MULTIPLIER = 0.0075D;
    private static final double INACCURACY = 0.35D;
    private static final double VELOCITY = 1.0D;
    private static final double GRAVITY = 0.1D;
    private static final int LIFETIME = 200;
    private static final float EXPLOSION_STRENGTH = 5.0F;

    public PileDebrisEntity(EntityType<? extends PileDebrisEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    private PileDebrisEntity(Level level, Vec3 position, double initialVerticalMotion) {
        this(HbmEntityTypes.PILE_DEBRIS.get(), level);
        setPos(position);

        // EntityThrowableNT#setThrowableHeading normalizes first, then adds
        // independent Gaussian error without normalizing the result again.
        Vec3 heading = new Vec3(0.0D, initialVerticalMotion, 0.0D).normalize().add(
                random.nextGaussian() * HEADING_FORCE_MULTIPLIER * INACCURACY,
                random.nextGaussian() * HEADING_FORCE_MULTIPLIER * INACCURACY,
                random.nextGaussian() * HEADING_FORCE_MULTIPLIER * INACCURACY
        );
        setDeltaMovement(heading.scale(VELOCITY));
    }

    public static void spawnMeltdownFragments(Level level, Vec3 position) {
        for (int index = 0; index < 15; index++) {
            double verticalMotion = level.random.nextDouble() * 0.5D + 1.0D;
            level.addFreshEntity(new PileDebrisEntity(level, position, verticalMotion));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(HbmParticleTypes.FLAMETHROWER_FIRE.get(),
                    getX(), getY() - 0.125D, getZ(), 0.0D, 0.0D, 0.0D);
            return;
        }
        if (tickCount > LIFETIME) {
            discard();
            return;
        }

        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 collisionEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, collisionEnd);
        HitResult hit = entityHit != null ? entityHit : blockHit;
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation());
            level().explode(this, getX(), getY(), getZ(), EXPLOSION_STRENGTH, true,
                    Level.ExplosionInteraction.NONE);
            discard();
            return;
        }

        setPos(end);
        setDeltaMovement(motion.x, motion.y - GRAVITY, motion.z);
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        Entity closest = null;
        Vec3 closestPosition = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity candidate : level().getEntities(this, area,
                entity -> entity.isAlive() && entity.isPickable())) {
            var impact = candidate.getBoundingBox().inflate(0.3D).clip(start, end);
            if (impact.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(impact.get());
            if (distance < closestDistance) {
                closest = candidate;
                closestPosition = impact.get();
                closestDistance = distance;
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestPosition);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }
}
