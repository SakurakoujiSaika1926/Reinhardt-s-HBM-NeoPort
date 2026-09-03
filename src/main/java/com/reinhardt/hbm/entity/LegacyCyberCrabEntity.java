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

import java.util.List;

/** Shared 1.7.10 EntityCyberCrab behaviour. */
public class LegacyCyberCrabEntity extends Monster implements RangedAttackMob {
    public enum Kind { CYBER, TESLA, TAINT }
    private final Kind kind;
    private List<Vec3> targets = List.of();

    public LegacyCyberCrabEntity(EntityType<? extends LegacyCyberCrabEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
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
        goalSelector.addGoal(4, new RangedAttackGoal(this, 0.5D, 60, 80, 15.0F));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
                0, true, true,
                entity -> !(entity instanceof LegacyCyberCrabEntity) && !(entity instanceof Creeper)));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(HbmDamageTypes.DIGAMMA)) {
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
        super.die(source);
        if (!level().isClientSide && !isRemoved()) {
            float radius = kind == Kind.TAINT ? 3.0F : 0.1F;
            level().explode(this, getX(), getY(), getZ(), radius, false, Level.ExplosionInteraction.NONE);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        Vec3 direction = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(position().add(0.0D, getBbHeight() * 0.5D, 0.0D));
        LegacyBulletEntity bullet = new LegacyBulletEntity(
                level(), getX(), getY() + getBbHeight() * 0.5D, getZ(), direction,
                StandardAmmoItem.StandardAmmoType.TAU_URANIUM, 3.0F);
        bullet.setDeltaMovement(direction.normalize().scale(1.6D));
        level().addFreshEntity(bullet);
        playSound(HbmSoundEvents.WEAPON_TAU_SHOOT.get(), 1.0F, kind == Kind.TAINT ? 0.5F : 2.0F);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       DamageSource source, boolean recentlyHit) {
        if (kind == Kind.TESLA || kind == Kind.TAINT) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(HbmItems.COIL_COPPER.get()));
        }
        if (kind == Kind.TAINT) {
            net.minecraft.world.item.Item tungsten = BuiltInRegistries.ITEM.get(
                    com.reinhardt.hbm.ReinhardtsHBM.id("coil_magnetized_tungsten"));
            if (tungsten != net.minecraft.world.item.Items.AIR) {
                spawnAtLocation(new net.minecraft.world.item.ItemStack(tungsten));
            }
        }
    }

    protected Kind kind() {
        return kind;
    }
}
