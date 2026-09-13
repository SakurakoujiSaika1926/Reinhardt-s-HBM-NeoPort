package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.blockentity.TeslaCoilBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/** Shared 1.7.10 EntityCyberCrab behaviour. */
public class LegacyCyberCrabEntity extends Monster implements RangedAttackMob {
    public enum Kind { CYBER, TESLA, TAINT }
    private final Kind kind;
    private List<Vec3> targets = List.of();
    private boolean deathExplosionDone;

    public LegacyCyberCrabEntity(EntityType<? extends LegacyCyberCrabEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        // EntityCyberCrab explicitly opted out of water pathing in 1.7.10.
        getNavigation().setCanFloat(false);
    }

    public static AttributeSupplier.Builder createAttributes(double health, double speed) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed);
    }

    @Override
    protected void registerGoals() {
        if (kind != Kind.TAINT) {
            goalSelector.addGoal(0, new PanicGoal(this, 0.75D));
        }
        goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5D));
        // EntityTaintCrab replaces the base arrow goal with the old 5-tick,
        // 50-block attack cadence; the other two variants retain 60--80.
        if (kind == Kind.TAINT) {
            goalSelector.addGoal(4, new RangedAttackGoal(this, 0.5D, 5, 5, 50.0F));
        } else {
            goalSelector.addGoal(4, new RangedAttackGoal(this, 0.5D, 60, 80, 15.0F));
        }
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
                0, true, true,
                entity -> !(entity instanceof LegacyCyberCrabEntity) && !(entity instanceof Creeper)));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // EntityCyberCrab only rejected TAU damage in 1.7.10; digamma
        // damage remained effective.  Keep this distinction explicit.
        if (source.is(HbmDamageTypes.TAU)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (isInWaterOrRain() || isOnFire())) {
            hurt(damageSources().generic(), 10.0F);
        }
    }

    public List<Vec3> targets() {
        return targets;
    }

    protected final void updateTeslaTargets(double emitterHeight, double range) {
        Vec3 origin = position().add(0.0D, emitterHeight, 0.0D);
        targets = level().isClientSide
                ? TeslaCoilBlockEntity.targetPoints(level(), origin, range, this)
                : TeslaCoilBlockEntity.zap(level(), origin, range, this);
    }

    @Override
    public void die(DamageSource source) {
        // EntityCyberCrab marked itself dead first and then created the
        // explosion.  Modern super.die() marks the entity removed, so using
        // isRemoved() after it would silently skip the legacy explosion.
        if (!level().isClientSide && !deathExplosionDone) {
            deathExplosionDone = true;
            float radius = kind == Kind.TAINT ? 3.0F : 0.1F;
            level().explode(this, getX(), getY(), getZ(), radius, false, Level.ExplosionInteraction.NONE);
        }
        super.die(source);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        // EntityBullet's living-target constructor aims at target.minY +
        // target.height / 3 and starts at shooter.eyeY - 0.1.
        // The old EntityCyberCrab constructor also advanced the spawn point
        // by one horizontal block toward its target before creating the
        // projectile.  Keep that exact launch point; do not replace it with a
        // shared renderer/physics offset.
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        Vec3 origin = horizontal < 1.0E-7D
                ? position().add(0.0D, getEyeHeight() - 0.1D, 0.0D)
                : new Vec3(getX() + dx / horizontal,
                        getY() + getEyeHeight() - 0.1D,
                        getZ() + dz / horizontal);
        Vec3 direction = target.position().add(0.0D, target.getBbHeight() / 3.0D, 0.0D)
                .subtract(origin);
        LegacyBulletEntity bullet = new LegacyBulletEntity(
                level(), origin.x, origin.y, origin.z, direction,
                StandardAmmoItem.StandardAmmoType.TAU_URANIUM, 3.0F, this, 0.0F);
        bullet.setDeltaMovement(direction.normalize().scale(1.6D));
        level().addFreshEntity(bullet);
        playSound(HbmSoundEvents.WEAPON_SAW_SHOOT.get(), 1.0F, kind == Kind.TAINT ? 0.5F : 2.0F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HbmSoundEvents.ENTITY_CYBERCRAB.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundEvents.ENTITY_CYBERCRAB.get();
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       DamageSource source, boolean recentlyHit) {
        if (!recentlyHit) {
            return;
        }
        // EntityTaintCrab#getDropItem was reached through the old mob-loot
        // path: its common copper coil uses the inherited EntityMob count
        // formula.  EntityTeslaCrab has no common drop at all.
        if (kind == Kind.TAINT) {
            int count = random.nextInt(3) + random.nextInt(1 + legacyLootingLevel());
            if (count > 0) {
                spawnAtLocation(new net.minecraft.world.item.ItemStack(HbmItems.COIL_COPPER.get(), count));
            }
        }
        if (random.nextInt(200) < 5 + legacyLootingLevel()) {
            net.minecraft.world.item.Item tungsten = BuiltInRegistries.ITEM.get(
                    com.reinhardt.hbm.ReinhardtsHBM.id("coil_magnetized_tungsten"));
            if (kind == Kind.TESLA) {
                spawnAtLocation(new net.minecraft.world.item.ItemStack(HbmItems.COIL_COPPER.get()));
            } else if (tungsten != net.minecraft.world.item.Items.AIR) {
                spawnAtLocation(new net.minecraft.world.item.ItemStack(tungsten));
            }
        }
    }

    private int legacyLootingLevel() {
        LivingEntity killer = getLastHurtByMob();
        if (killer == null || killer.getMainHandItem().isEmpty()) {
            return 0;
        }
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),
                killer.getMainHandItem());
    }

    protected Kind kind() {
        return kind;
    }
}
