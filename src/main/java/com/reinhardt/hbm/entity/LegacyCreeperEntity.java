package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Shared 1.7.10 creeper base used by the five concrete HBM creepers. */
public class LegacyCreeperEntity extends Creeper {
    protected enum Kind { NUCLEAR, TAINTED, PHOSGENE, VOLATILE, GOLD }

    private final Kind kind;
    private int legacyFuse;
    private int legacyPreviousFuse;

    protected LegacyCreeperEntity(EntityType<? extends Creeper> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
    }

    protected static AttributeSupplier.Builder createAttributes(double health, double speed) {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // EntityCreeperNuclear.attackEntityFrom returned false after the
        // entity had already died; preserve that legacy guard before any
        // healing or resistance branch.
        if (kind == Kind.NUCLEAR && !isAlive()) {
            return false;
        }
        if ((kind == Kind.NUCLEAR || kind == Kind.TAINTED) && (source.is(HbmDamageTypes.RADIATION)
                || source.is(HbmDamageTypes.MUD_POISONING))) {
            if (isAlive()) {
                heal(amount);
            }
            return false;
        }
        if (kind == Kind.PHOSGENE && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            amount -= 4.0F;
            if (amount < 0.0F) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        legacyPreviousFuse = legacyFuse;

        // EntityCreeperNuclear's 1.7.10 onUpdate contaminated every nearby
        // living entity before the vanilla creeper tick.  The environmental
        // buffer was deliberately unmitigated while the accumulated dose
        // respected radiation resistance and creative/early-login guards.
        if (kind == Kind.NUCLEAR && !level().isClientSide) {
            AABB area = getBoundingBox().inflate(5.0D);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != this && entity.isAlive())) {
                HbmLivingRadiation data = HbmLivingRadiation.get(target);
                data.addEnvironmentRadiation(0.25F);
                if (!isLegacyRadiationImmune(target)
                        && !(target instanceof Player player
                        && (player.isCreative() || player.isSpectator() || player.tickCount < 200))) {
                    float dose = (float) (0.25D * HbmArmorProtection.radiationMultiplier(target));
                    data.addRadiationWithReadout(dose);
                }
                HbmLivingRadiation.set(target, data);
            }
        }

        // Creeper.tick() owns the vanilla 30-tick swell counter and would
        // detonate through its private explodeCreeper() before our legacy
        // handlers get a chance to run.  The custom aiStep below captures the
        // direction selected by SwellGoal, advances the legacy fuse, and then
        // resets the vanilla direction so its private counter stays at zero.
        super.tick();
        if (!isAlive()) {
            return;
        }
        if (!level().isClientSide && isAlive() && legacyFuse >= legacyFuseTime()) {
            detonateLegacy();
            return;
        }

        if (isAlive() && (kind == Kind.NUCLEAR || kind == Kind.TAINTED)
                && getHealth() < getMaxHealth() && tickCount % 10 == 0) {
            heal(1.0F);
        }
    }

    @Override
    public void aiStep() {
        // Mob.aiStep runs after Creeper's private swell check.  At this point
        // SwellGoal has written the direction for the next tick, so consume it
        // for the 1.7.10 fuse before clearing it to disable the vanilla fuse.
        super.aiStep();
        int swellDirection = getSwellDir();
        if (swellDirection > 0) {
            legacyFuse = Math.min(legacyFuseTime(), legacyFuse + 1);
        } else {
            legacyFuse = Math.max(0, legacyFuse - 1);
        }
        setSwellDir(-1);
    }

    @Override
    public float getSwelling(float partialTick) {
        return Mth.lerp(partialTick, legacyPreviousFuse, legacyFuse)
                / (float) legacyFuseTime();
    }

    private int legacyFuseTime() {
        return kind == Kind.NUCLEAR || kind == Kind.PHOSGENE ?
                (kind == Kind.NUCLEAR ? 75 : 20) : 30;
    }

    private void detonateLegacy() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = position();
        boolean griefing = level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean powered = isPowered();
        discard();
        switch (kind) {
            case NUCLEAR -> {
                if (powered) {
                    if (griefing) {
                        NukeExplosionManager.scheduleLegacyNuke(level, getX(), getY(), getZ(), 50);
                    } else {
                        com.reinhardt.hbm.explosion.LegacyMukeExplosion.detonateNuclearDamage(
                                level, this, center.add(0.0D, 0.5D, 0.0D), 100.0D);
                    }
                } else if (griefing) {
                    com.reinhardt.hbm.explosion.LegacyMukeExplosion.detonateMediumMiniNuke(
                            level, this, center.add(0.0D, 0.5D, 0.0D));
                } else {
                    com.reinhardt.hbm.entity.LegacyProjectileUtil.detonateNuclearSafe(
                            level, this, center.add(0.0D, 0.5D, 0.0D));
                }
            }
            case TAINTED -> LegacyProjectileUtil.detonateTaintedCreeper(level, this, center, powered);
            case PHOSGENE -> LegacyProjectileUtil.detonatePhosgeneCreeper(level, this, center);
            case VOLATILE -> LegacyProjectileUtil.detonateBulkieCreeper(level, this, center, powered, false);
            case GOLD -> LegacyProjectileUtil.detonateBulkieCreeper(level, this, center, powered, true);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (kind == Kind.NUCLEAR && level() instanceof ServerLevel level) {
            HbmAdvancements.awardNearby(level, getBoundingBox().inflate(50.0D), "boss_creeper");
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       DamageSource source, boolean recentlyHit) {
        int looting = legacyLootingLevel(level);
        switch (kind) {
            case NUCLEAR -> {
                dropLegacyCreeperItem(Items.TNT, looting);
                if (random.nextInt(3) == 0) {
                    spawnLegacyItem(level, "coin_creeper");
                }
                if (source.getEntity() instanceof Skeleton
                        || source.is(DamageTypeTags.IS_PROJECTILE)
                        && source.getDirectEntity() instanceof AbstractArrow arrow
                        && arrow.getOwner() == null) {
                    spawnAtLocation(com.reinhardt.hbm.item.StandardAmmoItem.stackFor(
                            com.reinhardt.hbm.registry.HbmItems.AMMO_STANDARD.get(),
                            com.reinhardt.hbm.item.StandardAmmoItem.StandardAmmoType.NUKE_STANDARD));
                }
            }
            case TAINTED -> dropLegacyCreeperItem(Items.TNT, looting);
            case PHOSGENE -> dropLegacyCreeperItem(Items.GUNPOWDER, looting);
            case VOLATILE -> {
                spawnLegacyItem(level, "sulfur", 2 + random.nextInt(3));
                spawnLegacyItem(level, "stick_tnt", 1 + random.nextInt(2));
            }
            case GOLD -> {
                int amount = recentlyHit ? 5 + random.nextInt(6 + looting * 2) : 3;
                spawnLegacyItem(level, "crystal_gold", amount);
            }
        }
    }

    private void dropLegacyCreeperItem(Item item, int looting) {
        int count = random.nextInt(3) + random.nextInt(1 + looting);
        if (count > 0) {
            spawnAtLocation(new ItemStack(item, count));
        }
    }

    private int legacyLootingLevel(ServerLevel level) {
        LivingEntity killer = getLastHurtByMob();
        if (killer == null || killer.getMainHandItem().isEmpty()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.LOOTING),
                killer.getMainHandItem());
    }

    private void spawnLegacyItem(ServerLevel level, String id) {
        spawnLegacyItem(level, id, 1);
    }

    private void spawnLegacyItem(ServerLevel level, String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(com.reinhardt.hbm.ReinhardtsHBM.id(id));
        if (item != Items.AIR) {
            spawnAtLocation(new ItemStack(item, count));
        }
    }

    protected Kind kind() {
        return kind;
    }

    private static boolean isLegacyRadiationImmune(LivingEntity target) {
        return target instanceof LegacyNuclearCreeperEntity
                || target instanceof LegacyTaintedCreeperEntity
                || target instanceof LegacyCyberCrabEntity
                || target instanceof LegacyMaskManEntity
                || target instanceof LegacyRadBeastEntity
                || target instanceof LegacyUfoEntity
                || target instanceof LegacyChopperEntity
                || target instanceof LegacyWormHeadEntity
                || target instanceof LegacyWormBodyEntity
                || target instanceof MushroomCow
                || target instanceof Zombie
                || target instanceof Skeleton
                || target instanceof LegacyQuackosEntity
                || target instanceof Ocelot;
    }
}
