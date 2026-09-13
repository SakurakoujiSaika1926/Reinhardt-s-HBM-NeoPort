package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/** Exact BOTPrime head cadence: 74 segments, target pursuit, five lasers every 30 ticks and passive repair. */
public final class LegacyWormHeadEntity extends Monster {
    public static final int SEGMENT_COUNT = 74;
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.reinhardtshbm.entity_bot_prime_head"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos spawnPoint = BlockPos.ZERO;
    private UUID targetUuid;
    private int attackCounter;
    private int damageCooldown;
    private int healLockTicks;
    private int courseChangeCooldown;
    private boolean nearGround;
    private boolean segmentsCreated;

    public LegacyWormHeadEntity(EntityType<? extends LegacyWormHeadEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        noCulling = true;
        xpReward = 1000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15_000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public void createSegments(ServerLevel level) {
        if (segmentsCreated) return;
        segmentsCreated = true;
        spawnPoint = blockPosition();
        UUID previous = getUUID();
        for (int part = 0; part < SEGMENT_COUNT; part++) {
            LegacyWormBodyEntity body = new LegacyWormBodyEntity(HbmEntityTypes.LEGACY_WORM_BODY.get(), level);
            body.initialize(getUUID(), previous, part);
            // EntityBOTPrimeHead#onSpawnWithEgg floors the head position and
            // places every body part at those integer coordinates.
            body.moveTo(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), getYRot(), getXRot());
            level.addFreshEntity(body);
            previous = body.getUUID();
        }
    }

    /**
     * Modern equivalent of EntityBOTPrimeHead#onSpawnWithEgg.  That method
     * floors the requested ItemChopper/Mechanist Circle position before it
     * creates the 74 body parts and records the wandering origin.
     */
    public void initializeLegacySpawn(ServerLevel level) {
        int x = Mth.floor(getX());
        int y = Mth.floor(getY());
        int z = Mth.floor(getZ());
        moveTo(x, y, z, getYRot(), getXRot());
        createSegments(level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (damageCooldown > 0 || source.getEntity() instanceof LegacyWormHeadEntity head && head.getUUID().equals(getUUID())
                || source.getEntity() instanceof LegacyWormBodyEntity body && body.belongsTo(getUUID())) {
            return false;
        }
        boolean hit = super.hurt(source, amount);
        if (hit) {
            damageCooldown = 10;
            healLockTicks = 100;
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        setNoGravity(true);
        if (level().isClientSide) return;
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        if (!segmentsCreated && level() instanceof ServerLevel server) createSegments(server);
        if (damageCooldown > 0) damageCooldown--;
        if (healLockTicks > 0) healLockTicks--;

        Entity target = target();
        if (target == null || !target.isAlive() || distanceToSqr(target) > 150.0D * 150.0D) {
            target = findTarget();
            targetUuid = target == null ? null : target.getUUID();
        }
        moveHead(target);
        if (target instanceof LivingEntity living && distanceToSqr(target) < 150.0D * 150.0D && canSee(target)) {
            if (++attackCounter == 30) {
                fireLasers(living);
                attackCounter = 0;
            }
        } else {
            attackCounter = 0;
        }
        if (tickCount % 5 == 0) {
            damageNearby();
        }
        if (getHealth() < getMaxHealth() && tickCount % 6 == 0) {
            heal(target == null && healLockTicks == 0 ? 4.0F : 1.0F);
        }
        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
    }

    private void moveHead(Entity target) {
        Vec3 waypoint;
        if (target != null) {
            if (nearGround) {
                waypoint = target.position();
                if (random.nextInt(80) == 0 && getY() > spawnPoint.getY() && !isInsideOpaque()) nearGround = false;
            } else {
                waypoint = new Vec3(target.getX(), 10.0D, target.getZ());
                if (getY() < 15.0D) nearGround = true;
            }
        } else {
            waypoint = new Vec3(spawnPoint.getX() - 50 + random.nextInt(100), spawnPoint.getY() - 30 + random.nextInt(60),
                    spawnPoint.getZ() - 50 + random.nextInt(100));
        }
        Vec3 delta = waypoint.subtract(position());
        double distance = delta.length();
        Vec3 motion = getDeltaMovement();
        // EntityWormBaseNT prevents the boss from being lost below the world.
        if (getY() < -10.0D) {
            motion = new Vec3(motion.x, 1.0D, motion.z);
        } else if (getY() < 3.0D) {
            motion = new Vec3(motion.x, 0.3D, motion.z);
        }
        if (courseChangeCooldown-- <= 0 && distance > 0.001D) {
            courseChangeCooldown += random.nextInt(5) + 2;
            if (motion.lengthSqr() < 1.0D) {
                if (!isInsideOpaque()) delta = delta.scale(1.0D / 8.0D);
                motion = motion.add(delta.normalize().scale(0.15D));
            }
        }
        if (!isInsideOpaque()) motion = motion.add(0.0D, -0.006D, 0.0D);
        setDeltaMovement(motion.scale(0.995D));
        move(MoverType.SELF, getDeltaMovement());
        updateRotation(getDeltaMovement());
    }

    private boolean isInsideOpaque() {
        return !level().getBlockState(blockPosition()).isAir();
    }

    private Entity findTarget() {
        return level().getEntities(this, getBoundingBox().inflate(128.0D), entity ->
                        entity instanceof Player player && !player.isCreative() && !player.isSpectator())
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private Entity target() {
        return targetUuid != null && level() instanceof ServerLevel server ? server.getEntity(targetUuid) : null;
    }

    private boolean canSee(Entity entity) {
        HitResult result = level().clip(new ClipContext(getEyePosition(), entity.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS;
    }

    private void damageNearby() {
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(0.5D), Entity::isAlive)) {
            if (!(entity instanceof LivingEntity living)
                    || entity instanceof LegacyWormBodyEntity body && body.belongsTo(getUUID())
                    || entity instanceof LegacyWormHeadEntity head && head.getUUID().equals(getUUID())) {
                continue;
            }
            if (!living.hurt(damageSources().mobAttack(this), 1000.0F)) {
                continue;
            }
            Vec3 delta = living.position().subtract(position());
            double knockback = delta.lengthSqr() + 0.1D;
            living.push(delta.x / knockback, delta.y / knockback, delta.z / knockback);
        }
    }

    private void fireLasers(LivingEntity target) {
        double sourceY = getY() + getEyeHeight() - 0.1D;
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-7D) return;
        Vec3 origin = new Vec3(getX() + dx / horizontal, sourceY, getZ() + dz / horizontal);
        Vec3 direction = new Vec3(dx,
                target.getY() + target.getBbHeight() / 3.0D - sourceY,
                dz);
        for (int index = 0; index < 5; index++) {
            level().addFreshEntity(new LegacyBossProjectileEntity(level(), this, origin, direction,
                    LegacyBossProjectileEntity.Type.WORM_LASER, target, index * 0.05F));
        }
        level().playSound(null, blockPosition(), HbmSoundEvents.WEAPON_BALLS_LASER.get(), SoundSource.HOSTILE, 5.0F, 0.75F);
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-6D) return;
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float) Math.toDegrees(Math.atan2(motion.y, horizontal)));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            Item coin = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("coin_worm"));
            for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(200.0D))) {
                HbmAdvancements.award(player, "boss_worm");
                if (coin != net.minecraft.world.item.Items.AIR) {
                    player.getInventory().add(new ItemStack(coin));
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("spawn", spawnPoint.asLong());
        tag.putBoolean("nearGround", nearGround);
        tag.putBoolean("segmentsCreated", segmentsCreated);
        tag.putInt("attack", attackCounter);
        tag.putInt("healLock", healLockTicks);
        if (targetUuid != null) tag.putUUID("target", targetUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spawnPoint = BlockPos.of(tag.getLong("spawn"));
        nearGround = tag.getBoolean("nearGround");
        segmentsCreated = tag.getBoolean("segmentsCreated");
        attackCounter = tag.getInt("attack");
        healLockTicks = tag.getInt("healLock");
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
        return Component.translatable("entity.reinhardtshbm.entity_bot_prime_head");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityBOTPrimeBase sets renderDistanceWeight = 15.  The head
        // registration is 3 blocks wide, giving the legacy 2,880-block
        // render radius.
        return distance < 8_294_400.0D;
    }
}
