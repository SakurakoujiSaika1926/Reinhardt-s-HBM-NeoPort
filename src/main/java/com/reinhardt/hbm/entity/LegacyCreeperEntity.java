package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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
            if (amount <= 0.0F) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        int swellDirection = getSwellDir();
        legacyPreviousFuse = legacyFuse;
        if (swellDirection > 0) {
            legacyFuse = Math.min(legacyFuseTime(), legacyFuse + 1);
        } else {
            legacyFuse = Math.max(0, legacyFuse - 1);
        }
        if (!level().isClientSide && isAlive() && legacyFuse >= legacyFuseTime()) {
            detonateLegacy();
            return;
        }

        // Creeper's private vanilla fuse would otherwise detonate at 30 ticks.
        // Keep its AI direction synced while making the legacy fuse authoritative.
        setSwellDir(-1);
        super.tick();
        setSwellDir(swellDirection);

        if (isAlive() && (kind == Kind.NUCLEAR || kind == Kind.TAINTED)
                && getHealth() < getMaxHealth() && tickCount % 10 == 0) {
            heal(1.0F);
        }
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
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       DamageSource source, boolean recentlyHit) {
        switch (kind) {
            case NUCLEAR -> {
                spawnAtLocation(new ItemStack(Items.TNT));
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
            case TAINTED, PHOSGENE -> spawnAtLocation(new ItemStack(Items.TNT));
            case VOLATILE -> {
                spawnLegacyItem(level, "sulfur", 2 + random.nextInt(3));
                spawnLegacyItem(level, "stick_tnt", 1 + random.nextInt(2));
            }
            case GOLD -> {
                int amount = recentlyHit ? 5 + random.nextInt(6) : 3;
                spawnLegacyItem(level, "crystal_gold", amount);
            }
        }
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
}
