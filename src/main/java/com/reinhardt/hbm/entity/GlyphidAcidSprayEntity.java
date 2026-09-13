package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.Optional;

/** The liquid EntityChemical projectile emitted by the 1.7.10 behemoth. */
public final class GlyphidAcidSprayEntity extends Entity {
    private static final float ACID_DAMAGE = 1.0F;
    private Entity owner;

    public GlyphidAcidSprayEntity(EntityType<? extends GlyphidAcidSprayEntity> type, Level level) {
        super(type, level);
        noCulling = true;
        setNoGravity(true);
    }

    public GlyphidAcidSprayEntity(Level level, LivingEntity owner) {
        this(HbmEntityTypes.GLYPHID_ACID_SPRAY.get(), level);
        this.owner = owner;
        setPos(owner.getX(), owner.getEyeY(), owner.getZ());
        setDeltaMovement(owner.getLookAngle().scale(0.4D));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, entityEnd);
        if (entityHit != null && entityHit.getEntity() != owner) {
            if (!level().isClientSide && entityHit.getEntity() instanceof LivingEntity living) {
                GlyphidEntity glyphid = living instanceof GlyphidEntity g ? g : null;
                if (glyphid == null) {
                    LegacyProjectileUtil.hurtNoIFrame(living,
                            damageSources().source(HbmDamageTypes.ACID, this, owner), ACID_DAMAGE);
                    for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                            net.minecraft.world.entity.EquipmentSlot.HEAD,
                            net.minecraft.world.entity.EquipmentSlot.CHEST,
                            net.minecraft.world.entity.EquipmentSlot.LEGS,
                            net.minecraft.world.entity.EquipmentSlot.FEET}) {
                        living.getItemBySlot(slot).hurtAndBreak(1, living, slot);
                    }
                }
            }
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            discard();
            return;
        }
        if (!level().isClientSide) {
            move(MoverType.SELF, motion);
        } else {
            move(MoverType.SELF, motion);
            level().addParticle(new DustParticleOptions(new Vector3f(0.69F, 0.67F, 0.39F), 0.7F),
                    getX(), getY(), getZ(), motion.x, motion.y, motion.z);
        }
        setDeltaMovement(motion.scale(0.99D).add(0.0D, -0.03D, 0.0D));
        if (!level().isClientSide && tickCount > 600) {
            discard();
        }
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        return level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable() && entity != owner)
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(0.3D).clip(start, end)
                        .map(point -> new EntityHitResult(entity, point)))
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())))
                .orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("owner", owner.getUUID());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner") && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            owner = serverLevel.getEntity(tag.getUUID("owner"));
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }
}
