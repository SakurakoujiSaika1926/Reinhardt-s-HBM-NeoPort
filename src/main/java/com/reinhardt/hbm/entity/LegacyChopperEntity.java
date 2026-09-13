package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * EntityHunterChopper port.  The chopper deliberately never enters vanilla's
 * normal death timer: lethal damage starts the old falling-crash sequence.
 */
public final class LegacyChopperEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DYING =
            SynchedEntityData.defineId(LegacyChopperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(LegacyChopperEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.reinhardtshbm.entity_hunter_chopper"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private Vec3 waypoint = Vec3.ZERO;
    private int courseChangeCooldown;
    private int prevAttackCounter;
    private int attackCounter;
    private int mineDropCounter;
    private java.util.UUID targetUuid;
    private boolean crashResolved;

    public LegacyChopperEntity(EntityType<? extends LegacyChopperEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
        xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 750.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public boolean isCrashing() {
        return entityData.get(DYING);
    }

    public boolean isAttacking() {
        return entityData.get(ATTACKING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DYING, false);
        builder.define(ATTACKING, false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isCrashing() || getHealth() <= 0.1F) {
            return false;
        }
        // EntityHunterChopper only accepted full damage from explosions,
        // shrapnel, nuclear blasts, black holes, tau and subatomic rounds.
        // The last two were string-identified legacy sources; retain those
        // exact identifiers for callers that still construct them.
        String damageId = source.getMsgId();
        boolean fullDamage = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                || source.is(HbmDamageTypes.SHRAPNEL)
                || source.is(HbmDamageTypes.NUCLEAR_BLAST)
                || source.is(HbmDamageTypes.BLACK_HOLE)
                || "tau".equals(damageId)
                || "tauBlast".equals(damageId)
                || damageId.startsWith("subAtomic");
        if (!fullDamage) {
            amount *= 0.1F;
        }
        // The old EntityDamageSource guard rejected every entity-caused
        // source (including indirect projectiles), not just direct melee.
        // DamageSource#getEntity is the exact modern equivalent.
        if (isInvulnerable() || source.getEntity() != null) {
            return false;
        }
        if (amount >= getHealth()) {
            beginCrash();
            setHealth(0.1F);
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            if (random.nextInt(15) == 0 && !level().isClientSide) {
                LegacyProjectileUtil.standardExplosion(this, position(), 5.0F, 1.0F, true, true);
                dropDamageItem();
            }
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        if (isCrashing()) {
            tickCrash();
        } else {
            tickCombat();
        }
        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
    }

    private void tickCombat() {
        setNoGravity(true);
        level().playSound(null, blockPosition(), HbmSoundEvents.MISC_NULL_CHOPPER.get(),
                SoundSource.PLAYERS, 10.0F, 0.5F);
        prevAttackCounter = attackCounter;
        Entity target = target();
        if (target == null || !target.isAlive() || attackCounter <= 0) {
            target = findTarget();
            targetUuid = target == null ? null : target.getUUID();
        }
        if (waypoint == Vec3.ZERO || position().distanceToSqr(waypoint) < 1.0D || position().distanceToSqr(waypoint) > 3600.0D) {
            waypoint = newWaypoint(target);
        }
        if (courseChangeCooldown-- <= 0) {
            courseChangeCooldown += random.nextInt(5) + 2;
            Vec3 offset = waypoint.subtract(position());
            double distance = offset.length();
            if (distance > 0.001D && courseTraversable(offset, distance)) {
                setDeltaMovement(getDeltaMovement().add(offset.scale(0.1D / distance)));
            } else {
                waypoint = newWaypoint(target);
            }
        }
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.91D));
        updateRotation(target);

        if (target != null && distanceToSqr(target) < 64.0D * 64.0D) {
            attackCounter = (attackCounter + 1) % 200;
            entityData.set(ATTACKING, attackCounter > 10);
            if (attackCounter == 80) {
                level().playSound(null, blockPosition(), HbmSoundEvents.ENTITY_CHOPPER_CHARGE.get(), SoundSource.HOSTILE, 5.0F, 1.0F);
            }
            if (attackCounter >= 120 && attackCounter % 2 == 0) {
                fireBullet(target);
            }
            mineDropCounter++;
            if (mineDropCounter > 100 && random.nextInt(15) == 0) {
                dropMines();
            }
        } else {
            attackCounter = 0;
            entityData.set(ATTACKING, false);
        }
    }

    private void tickCrash() {
        setNoGravity(false);
        Vec3 motion = getDeltaMovement().add(0.0D, -0.08D, 0.0D);
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal * 1.2D < 1.8D) motion = new Vec3(motion.x * 1.2D, motion.y, motion.z * 1.2D);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        setYRot(getYRot() + 20.0F);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        if (random.nextInt(20) == 0) {
            LegacyProjectileUtil.standardExplosion(this, position(), 5.0F, 1.0F, true, true);
        }
        if (tickCount % 2 == 0) {
            level().playSound(null, blockPosition(), HbmSoundEvents.MISC_NULL_CRASHING.get(),
                    SoundSource.PLAYERS, 10.0F, 0.5F);
        }
        if (onGround() || horizontalCollision || verticalCollision) {
            if (!crashResolved) {
                crashResolved = true;
                LegacyProjectileUtil.standardExplosion(this, position(), 15.0F, 1.0F, true, true);
                dropCrashItems();
            }
            discard();
        }
    }

    private Entity findTarget() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(250.0D), entity -> {
                    if (!entity.isAlive() || entity instanceof LegacyChopperEntity) {
                        return false;
                    }
                    // The 1.7.10 predicate checked the player's
                    // disableDamage flag, so spectators are invalid targets
                    // just like creative players.
                    if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                        return false;
                    }
                    double radius = entity.isShiftKeyDown() ? 200.0D : 250.0D;
                    return distanceToSqr(entity) < radius * radius;
                }).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private Entity target() {
        return targetUuid != null && level() instanceof ServerLevel server ? server.getEntity(targetUuid) : null;
    }

    private Vec3 newWaypoint(Entity target) {
        double x = target == null ? getX() : target.getX();
        double z = target == null ? getZ() : target.getZ();
        x += (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        z += (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        return new Vec3(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + 10 + random.nextInt(15), z);
    }

    private boolean courseTraversable(Vec3 delta, double distance) {
        Vec3 step = delta.scale(1.0D / distance);
        AABB box = getBoundingBox();
        for (int stepIndex = 1; stepIndex < distance; stepIndex++) {
            box = box.move(step);
            if (!level().noCollision(this, box)) return false;
        }
        return true;
    }

    private void updateRotation(Entity target) {
        Vec3 direction = target == null ? getDeltaMovement() : position().subtract(target.position());
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontal < 1.0E-6D) {
            return;
        }
        float wantedYaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float yawDelta = getYRot() - wantedYaw;
        if (yawDelta >= 10.0F) {
            yRotO = getYRot() - 10.0F;
            setYRot(getYRot() - 10.0F);
        }
        if (yawDelta <= -10.0F) {
            yRotO = getYRot() + 10.0F;
            setYRot(getYRot() + 10.0F);
        }
        setXRot((float) Math.toDegrees(Math.atan2(getDeltaMovement().y,
                Math.sqrt(getDeltaMovement().x * getDeltaMovement().x
                        + getDeltaMovement().z * getDeltaMovement().z))));
        float pitch = getXRot();
        if (pitch <= 330.0F && pitch >= 30.0F) {
            setXRot(pitch < 180.0F ? 30.0F : 330.0F);
        }
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void fireBullet(Entity target) {
        Vec3 look = getLookAngle();
        Vec3 origin = position().add(look.x * 2.0D, -0.5D, look.z * 2.0D);
        Vec3 direction = target.getBoundingBox().getCenter().subtract(origin)
                .add(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1).normalize();
        level().addFreshEntity(new LegacyBossProjectileEntity(level(), this, origin, direction,
                LegacyBossProjectileEntity.Type.CHOPPER_BULLET, target));
        level().playSound(null, blockPosition(), HbmSoundEvents.WEAPON_OSIPR_SHOOT.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
    }

    private void dropMines() {
        level().playSound(null, blockPosition(), HbmSoundEvents.ENTITY_CHOPPER_DROP.get(), SoundSource.HOSTILE, 15.0F, 1.0F);
        spawnMine(0.0D, 0.0D);
        if (random.nextInt(3) == 0) {
            spawnMine(1.0D, 0.0D);
            spawnMine(0.0D, 1.0D);
            spawnMine(-1.0D, 0.0D);
            spawnMine(0.0D, -1.0D);
        }
        mineDropCounter = 0;
    }

    private void spawnMine(double x, double z) {
        LegacyChopperMineEntity mine = new LegacyChopperMineEntity(HbmEntityTypes.LEGACY_CHOPPER_MINE.get(), level());
        mine.setOwner(this);
        mine.setPos(getX(), getY() - 0.5D, getZ());
        mine.setDeltaMovement(x, -0.3D, z);
        level().addFreshEntity(mine);
    }

    private void beginCrash() {
        if (!isCrashing()) {
            entityData.set(DYING, true);
            entityData.set(ATTACKING, false);
            LegacyProjectileUtil.standardExplosion(this, position(), 10.0F, 1.0F, true, true);
            level().playSound(null, blockPosition(), HbmSoundEvents.ENTITY_CHOPPER_DAMAGE.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
        }
    }

    private void dropDamageItem() {
        String id = random.nextInt(10) < 6 ? "combine_scrap" : "plate_combine_steel";
        spawnItem(id, 1);
    }

    private void dropCrashItems() {
        spawnItem("combine_scrap", random.nextInt(8) + 1);
        spawnItem("plate_combine_steel", random.nextInt(5) + 1);
    }

    private void spawnItem(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        if (item != net.minecraft.world.item.Items.AIR) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(item, count)));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("dying", isCrashing());
        tag.putInt("attack", attackCounter);
        tag.putInt("mine", mineDropCounter);
        tag.putBoolean("crashResolved", crashResolved);
        if (targetUuid != null) tag.putUUID("target", targetUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DYING, tag.getBoolean("dying"));
        attackCounter = tag.getInt("attack");
        mineDropCounter = tag.getInt("mine");
        crashResolved = tag.getBoolean("crashResolved");
        targetUuid = tag.hasUUID("target") ? tag.getUUID("target") : null;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.reinhardtshbm.entity_hunter_chopper");
    }

    @Override
    protected float getSoundVolume() {
        return 10.0F;
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000.0D;
    }
}
