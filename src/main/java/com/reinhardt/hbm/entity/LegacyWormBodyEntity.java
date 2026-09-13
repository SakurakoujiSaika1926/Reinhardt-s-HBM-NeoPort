package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/** A BOTPrime segment follows its UUID-linked predecessor and forwards all valid damage to the head. */
public final class LegacyWormBodyEntity extends Monster {
    private UUID headUuid;
    private UUID previousUuid;
    private int partNumber;
    private int attackCounter;
    private int resolveCooldown;
    private int damageCooldown;

    public LegacyWormBodyEntity(EntityType<? extends LegacyWormBodyEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15_000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public void initialize(UUID headUuid, UUID previousUuid, int partNumber) {
        this.headUuid = headUuid;
        this.previousUuid = previousUuid;
        this.partNumber = partNumber;
    }

    public boolean belongsTo(UUID otherHead) {
        return headUuid != null && headUuid.equals(otherHead);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (damageCooldown > 0 || source.getEntity() instanceof LegacyWormBodyEntity body && belongsTo(body.headUuid)
                || source.getEntity() instanceof LegacyWormHeadEntity head && belongsTo(head.getUUID())) {
            return false;
        }
        LegacyWormHeadEntity head = head();
        if (head == null || !head.isAlive()) return false;
        damageCooldown = 10;
        return head.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        setNoGravity(true);
        if (level().isClientSide) return;
        if (damageCooldown > 0) damageCooldown--;
        LegacyWormHeadEntity head = head();
        Entity previous = previous();
        if (head == null || !head.isAlive() || previous == null || !previous.isAlive()) {
            if (previous == null || !previous.isAlive()) {
                // EntityWormBaseNT occasionally detonates a detached segment
                // while it is resolving the predecessor link.
                if (random.nextInt(60) == 0) {
                    level().explode(this, getX(), getY(), getZ(), 2.0F, Level.ExplosionInteraction.NONE);
                }
            }
            discard();
            return;
        }
        follow(previous);
        Entity target = target(head);
        if (target != null) {
            double dx = target.getX() - getX();
            double dy = target.getY() - getY();
            double dz = target.getZ() - getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            setYRot((float) Math.toDegrees(Math.atan2(dx, dz)));
            setXRot((float) Math.toDegrees(Math.atan2(dy, horizontal)));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
        if (target instanceof LivingEntity living && canSee(target)) {
            if (++attackCounter == 10) {
                fireBolt(living);
                attackCounter = -20;
            }
        } else if (attackCounter > 0) {
            attackCounter--;
        }
        if (tickCount % 5 == 0) damageNearby();
    }

    private void follow(Entity previous) {
        Vec3 delta = previous.position().subtract(position());
        double distance = delta.length();
        Vec3 motion;
        if (distance < 3.5D * 0.895D) {
            motion = getDeltaMovement().scale(0.8D);
        } else {
            double speed = Math.max(0.0D, Math.min(distance - 3.5D, 1.4D));
            motion = delta.scale(speed / distance);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal > 1.0E-6D) {
            setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
            setXRot((float) Math.toDegrees(Math.atan2(motion.y, horizontal)));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        // EntityBOTPrimeBody#isPotionApplicable always returned false.
        return false;
    }

    private Entity target(LegacyWormHeadEntity head) {
        Entity headTarget = null;
        if (head.getLastHurtMob() != null && head.getLastHurtMob().isAlive()) headTarget = head.getLastHurtMob();
        if (headTarget != null) return headTarget;
        return level().getEntities(this, getBoundingBox().inflate(128.0D), entity ->
                        entity instanceof Player player && !player.isCreative() && !player.isSpectator())
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private boolean canSee(Entity entity) {
        HitResult result = level().clip(new ClipContext(getEyePosition(), entity.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS;
    }

    private void fireBolt(LivingEntity target) {
        double sourceY = getY() + getEyeHeight() - 0.1D;
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-7D) return;
        Vec3 origin = new Vec3(getX() + dx / horizontal, sourceY, getZ() + dz / horizontal);
        Vec3 direction = new Vec3(dx,
                target.getY() + target.getBbHeight() / 3.0D - sourceY,
                dz);
        level().addFreshEntity(new LegacyBossProjectileEntity(level(), this, origin,
                direction, LegacyBossProjectileEntity.Type.WORM_BOLT, target, 0.125F));
        level().playSound(null, blockPosition(), HbmSoundEvents.WEAPON_BALLS_LASER.get(), SoundSource.HOSTILE, 5.0F, 1.0F);
    }

    private void damageNearby() {
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(0.5D), Entity::isAlive)) {
            if (entity instanceof LivingEntity living && !(entity instanceof LegacyWormBodyEntity body && belongsTo(body.headUuid))
                    && !(entity instanceof LegacyWormHeadEntity head && belongsTo(head.getUUID()))) {
                if (!living.hurt(damageSources().mobAttack(this), living.getHealth() * 0.75F)) {
                    continue;
                }
                Vec3 push = living.position().subtract(position());
                double amount = push.lengthSqr() + 0.1D;
                living.push(push.x / amount, push.y / amount, push.z / amount);
            }
        }
    }

    private LegacyWormHeadEntity head() {
        if (headUuid == null || !(level() instanceof ServerLevel server)) return null;
        Entity entity = server.getEntity(headUuid);
        return entity instanceof LegacyWormHeadEntity head ? head : null;
    }

    private Entity previous() {
        if (previousUuid == null || !(level() instanceof ServerLevel server)) return null;
        return server.getEntity(previousUuid);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (headUuid != null) tag.putUUID("head", headUuid);
        if (previousUuid != null) tag.putUUID("previous", previousUuid);
        tag.putInt("part", partNumber);
        tag.putInt("attack", attackCounter);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        headUuid = tag.hasUUID("head") ? tag.getUUID("head") : null;
        previousUuid = tag.hasUUID("previous") ? tag.getUUID("previous") : null;
        partNumber = tag.getInt("part");
        attackCounter = tag.getInt("attack");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Same EntityBOTPrimeBase renderDistanceWeight = 15 as the head;
        // this registration is 2 blocks wide.
        return distance < 3_686_400.0D;
    }
}
