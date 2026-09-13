package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.item.GasMaskItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** 1.7.10 EntityMaskMan base attributes and target behaviour. */
public final class LegacyMaskManEntity extends Monster {
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.reinhardtshbm.entity_mob_mask_man"),
            net.minecraft.world.BossEvent.BossBarColor.RED,
            net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
    private float previousHealth = 1000.0F;

    public LegacyMaskManEntity(EntityType<? extends LegacyMaskManEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        // EntityAIMaskmanMinigun (priority 2) and EntityAIMaskmanLasergun
        // (priority 3) were the only attack goals in 1.7.10.  They must be
        // registered before the casual approach goal, otherwise the approach
        // goal monopolises MOVE/LOOK and the boss only walks toward players.
        goalSelector.addGoal(2, new MaskmanMinigunGoal(this));
        goalSelector.addGoal(3, new MaskmanLasergunGoal(this));
        // EntityAIMaskmanCasualApproach: repeatedly path to a random point
        // around the player instead of using vanilla melee reach/attack.
        goalSelector.addGoal(4, new CasualApproachGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
            return false;
        }
        if (source.getDirectEntity() instanceof ThrownEgg && random.nextInt(10) == 0) {
            xpReward = 0;
            setHealth(0.0F);
            return true;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            amount *= 0.5F;
        }
        if (amount > 50.0F) {
            amount = 50.0F + (amount - 50.0F) * 0.5F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive() && previousHealth >= getMaxHealth() * 0.5F && getHealth() < getMaxHealth() * 0.5F) {
            previousHealth = getHealth();
            if (!level().isClientSide) {
                level().explode(this, getX(), getY() + 4.0D, getZ(), 2.5F, true, ExplosionInteraction.TNT);
            }
        } else {
            previousHealth = getHealth();
        }
        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (level() instanceof net.minecraft.server.level.ServerLevel level) {
            HbmAdvancements.awardNearby(level, getBoundingBox().inflate(50.0D), "boss_maskman");
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        Item maskItem = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("gas_mask_m65"));
        Item filterItem = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("gas_mask_filter_combo"));
        if (maskItem != Items.AIR && filterItem != Items.AIR) {
            ItemStack mask = new ItemStack(maskItem);
            GasMaskItem.installFilter(mask, new ItemStack(filterItem), this);
            spawnAtLocation(mask);
        }
        spawnLegacyDrop(level, "coin_maskman");
        spawnLegacyDrop(level, "bottled_cloud");
        spawnAtLocation(new ItemStack(Items.SKELETON_SKULL));
    }

    private void spawnLegacyDrop(net.minecraft.server.level.ServerLevel level, String id) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        if (item != Items.AIR) {
            spawnAtLocation(new ItemStack(item));
        }
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
        return Component.translatable("entity.reinhardtshbm.entity_mob_mask_man");
    }

    private static final class CasualApproachGoal extends Goal {
        private final LegacyMaskManEntity owner;
        private Vec3 pathTarget;
        private int pathTimer;
        private int failedPathPenalty;
        private double lastX, lastY, lastZ;

        private CasualApproachGoal(LegacyMaskManEntity owner) {
            this.owner = owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = owner.getTarget();
            if (target == null || !target.isAlive() || !(target instanceof Player)) return false;
            if (--pathTimer <= 0) {
                pathTarget = approachPosition(target);
                owner.getNavigation().createPath(pathTarget.x, pathTarget.y, pathTarget.z, 0);
                pathTimer = 4 + owner.random.nextInt(7);
                return owner.getNavigation().getPath() != null;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = owner.getTarget();
            return target != null && target.isAlive() && !owner.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (pathTarget != null) owner.getNavigation().moveTo(pathTarget.x, pathTarget.y, pathTarget.z, 1.0D);
            pathTimer = 0;
        }

        @Override
        public void stop() {
            owner.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = owner.getTarget();
            if (target == null) return;
            owner.getLookControl().setLookAt(target, 30.0F, 30.0F);
            pathTimer--;
            double distance = owner.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            if (!owner.getSensing().hasLineOfSight(target)) {
                return;
            }
            if (pathTimer <= 0 && (lastX == 0.0D && lastY == 0.0D && lastZ == 0.0D
                    || target.distanceToSqr(lastX, lastY, lastZ) >= 1.0D
                    || owner.random.nextFloat() < 0.05F)) {
                lastX = target.getX(); lastY = target.getBoundingBox().minY; lastZ = target.getZ();
                pathTimer = failedPathPenalty + 4 + owner.random.nextInt(7);
                Path path = owner.getNavigation().getPath();
                if (path != null) {
                    Node finalNode = path.getEndNode();
                    if (finalNode != null
                            && target.distanceToSqr(finalNode.x, finalNode.y, finalNode.z) < 1.0D) {
                        failedPathPenalty = 0;
                    } else {
                        failedPathPenalty += 10;
                    }
                } else {
                    failedPathPenalty += 10;
                }
                if (distance > 1024.0D) {
                    pathTimer += 10;
                } else if (distance > 256.0D) {
                    pathTimer += 5;
                }
                Vec3 next = approachPosition(target);
                if (!owner.getNavigation().moveTo(next.x, next.y, next.z, 1.0D)) {
                    pathTimer += 15;
                }
            }
        }

        private Vec3 approachPosition(LivingEntity target) {
            Vec3 delta = owner.position().subtract(target.position());
            double range = Math.min(delta.length(), 20.0D) - 10.0D;
            delta = delta.normalize();
            return owner.position().add(delta.scale(range)).add(
                    owner.random.nextGaussian() * 2.0D,
                    delta.y - 5.0D + owner.random.nextInt(11),
                    owner.random.nextGaussian() * 2.0D);
        }
    }

    /** 1.7.10 minigun burst: only active from 5 (exclusive) to 10 blocks. */
    private static final class MaskmanMinigunGoal extends Goal {
        private final LegacyMaskManEntity owner;
        private LivingEntity target;
        private int timer = 3;

        private MaskmanMinigunGoal(LegacyMaskManEntity owner) {
            this.owner = owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = owner.getTarget();
            if (target == null || !target.isAlive()) return false;
            double distance = owner.distanceTo(target);
            return distance > 5.0D && distance < 10.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || !owner.getNavigation().isDone();
        }

        @Override
        public void start() {
            timer = 3;
        }

        @Override
        public void tick() {
            target = owner.getTarget();
            if (target == null) return;
            owner.getLookControl().setLookAt(target, 30.0F, 30.0F);
            owner.setYRot(owner.getYHeadRot());
            if (--timer <= 0) {
                timer = 3;
                owner.fireMaskmanBullet(target, StandardAmmoItem.StandardAmmoType.R556_SP, 15.0F);
            }
        }
    }

    /** 1.7.10 laser AI: long-range alternating projectile bursts. */
    private static final class MaskmanLasergunGoal extends Goal {
        private final LegacyMaskManEntity owner;
        private LivingEntity target;
        private int timer;
        private int attackCount;
        private int attack;

        private MaskmanLasergunGoal(LegacyMaskManEntity owner) {
            this.owner = owner;
            this.attack = owner.random.nextInt(3);
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = owner.getTarget();
            return target != null && target.isAlive() && owner.distanceTo(target) > 10.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || !owner.getNavigation().isDone();
        }

        @Override
        public void start() {
            timer = 1;
        }

        @Override
        public void tick() {
            target = owner.getTarget();
            if (target == null) return;
            owner.getLookControl().setLookAt(target, 30.0F, 30.0F);
            owner.setYRot(owner.getYHeadRot());
            int delay = attack == 0 ? 60 : attack == 1 ? 10 : 40;
            int amount = attack == 0 ? 5 : attack == 1 ? 10 : 3;
            if (--timer <= 0) {
                timer = delay;
                for (int i = 0; i < (attack == 2 ? 5 : 1); i++) {
                    owner.fireMaskmanBullet(target, StandardAmmoItem.StandardAmmoType.BMG50_SP, 35.0F);
                }
                if (++attackCount >= amount) {
                    attackCount = 0;
                    attack = (attack + 1 + owner.random.nextInt(2)) % 3;
                }
            }
        }
    }

    private void fireMaskmanBullet(LivingEntity target, StandardAmmoItem.StandardAmmoType ammo, float damage) {
        if (level().isClientSide || !(level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        Vec3 direction = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(position().add(0.0D, getBbHeight() * 0.65D, 0.0D));
        LegacyBulletEntity bullet = new LegacyBulletEntity(server,
                getX(), getY() + getBbHeight() * 0.65D, getZ(), direction,
                ammo, damage, this, 0.0F);
        server.addFreshEntity(bullet);
        playSound(HbmSoundEvents.WEAPON_TESLA.get(), 1.0F, 1.0F);
    }
}
