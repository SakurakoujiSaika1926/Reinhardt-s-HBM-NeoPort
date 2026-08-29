package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.explosion.LegacyMukeExplosion;
import com.reinhardt.hbm.item.AmmoArtyItem;
import com.reinhardt.hbm.item.AmmoHimarsItem;
import com.reinhardt.hbm.item.ArmorFSBItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.network.LegacyExplosionEffectPayload;
import com.reinhardt.hbm.network.LegacySmallExplosionEffectPayload;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmMobEffects;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.neoforged.neoforge.network.PacketDistributor;
import javax.annotation.Nullable;

public final class LegacyProjectileUtil {
    private LegacyProjectileUtil() {
    }

    public static int variantModelData(ItemStack stack) {
        if (stack.getItem() instanceof LegacyVariantItem variantItem) {
            return variantItem.variant(stack).modelData();
        }
        return 0;
    }

    public static String variantId(ItemStack stack) {
        if (stack.getItem() instanceof LegacyVariantItem variantItem) {
            return variantItem.variant(stack).id();
        }
        return "";
    }

    public static ArtyType artyType(ItemStack stack) {
        if (!(stack.getItem() instanceof AmmoArtyItem)) {
            return ArtyType.NORMAL;
        }
        return ArtyType.byModelData(variantModelData(stack));
    }

    public static HimarsType himarsType(ItemStack stack) {
        if (!(stack.getItem() instanceof AmmoHimarsItem)) {
            return HimarsType.STANDARD;
        }
        return HimarsType.byModelData(variantModelData(stack));
    }

    public static void hurtNoIFrame(Entity entity, DamageSource source, float amount) {
        entity.invulnerableTime = 0;
        entity.hurt(source, amount);
    }

    public static void standardExplosion(Entity source, Vec3 pos, float radius, boolean breakBlocks) {
        standardExplosion(source, pos, radius, 1.0F, breakBlocks, true);
    }

    public static void standardExplosion(Entity source, Vec3 pos, float radius, float rangeMod, boolean breakBlocks) {
        standardExplosion(source, pos, radius, rangeMod, breakBlocks, true);
    }

    public static void standardExplosion(Entity source, Vec3 pos, float radius, float rangeMod, boolean breakBlocks, boolean slagDebris) {
        standardExplosion(source, pos, radius, rangeMod, breakBlocks, slagDebris, true);
    }

    public static void standardExplosion(
            Entity source,
            Vec3 pos,
            float radius,
            float rangeMod,
            boolean breakBlocks,
            boolean slagDebris,
            boolean genericEffects
    ) {
        Level level = source.level();
        if (breakBlocks && level instanceof ServerLevel serverLevel) {
            allocateLegacyExplosionBlocks(source, serverLevel, pos, radius, slagDebris);
        }
        applyLegacyCrossDamage(source, pos, radius, rangeMod);
        if (genericEffects) {
            sendExplosionParticles(level, pos, Math.max(6, Math.round(radius)));
        }
    }

    public static void composeExplosionEffect(
            Level level,
            Vec3 pos,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMultiplier,
            float waveScale,
            int debrisCount,
            int debrisSize,
            int debrisRetry,
            float debrisVelocity,
            float debrisHorizontalDeviation,
            float debrisVerticalOffset,
            float soundRange
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LegacyExplosionEffectPayload payload = new LegacyExplosionEffectPayload(
                pos.x,
                pos.y,
                pos.z,
                cloudCount,
                cloudScale,
                cloudSpeedMultiplier,
                waveScale,
                debrisCount,
                debrisSize,
                debrisRetry,
                debrisVelocity,
                debrisHorizontalDeviation,
                debrisVerticalOffset,
                soundRange
        );
        double packetRange = Math.max(300.0D, soundRange);
        double packetRangeSquared = packetRange * packetRange;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(pos) <= packetRangeSquared) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void fixedDamageExplosion(Entity source, Vec3 pos, float radius, float damage, boolean breakBlocks) {
        fixedDamageExplosion(source.level(), source, pos, radius, damage, breakBlocks);
    }

    /** Direct 1.7.10 ExplosiveCharge detonation: ExplosionNT radius 15 and resolution 64. */
    public static void detonateExplosiveCharge(ServerLevel level, Vec3 pos, @Nullable Entity owner) {
        allocateLegacyExplosionBlocks(owner, level, pos, 15.0F, 64, 0.0F, null);
        applyLegacyCrossDamage(level, owner, pos, 15.0F, 1.0F);
        composeExplosionEffect(level, pos.add(0.0D, 1.0D, 0.0D),
                15, 5.0F, 1.0F, 45.0F,
                10, 16, 50, 1.0F, 3.0F, -2.0F, 200.0F);
    }

    /** Direct 1.7.10 DetMiner detonation: all block drops, no entity damage. */
    public static void detonateMiningCharge(ServerLevel level, Vec3 pos) {
        allocateLegacyExplosionBlocks(null, level, pos, 4.0F, 48, 1.0F, null);
        sendSmallExplosionEffect(level, pos, 15, 3.0F, 1.25F);
    }

    public static void fixedDamageExplosion(Level level, @Nullable Entity source, Vec3 pos,
                                            float radius, float damage, boolean breakBlocks) {
        if (breakBlocks && level instanceof ServerLevel serverLevel) {
            allocateLegacyWeaponExplosionBlocks(source, serverLevel, pos, radius);
        }
        applyLegacyFixedCrossDamage(level, source, pos, radius, damage, 1.0D);
        sendSmallExplosionEffect(level, pos, 10, 2.5F, 1.0F);
    }

    public static void volcanicExplosion(Entity source, Vec3 pos, float radius, float rangeMod) {
        if (source.level() instanceof ServerLevel serverLevel) {
            allocateLegacyExplosionBlocks(source, serverLevel, pos, radius,
                    HbmBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState());
        }
        applyLegacyCrossDamage(source, pos, radius, rangeMod);
    }

    /** 1.7.10 volcano-core explosion: terrain only, with no drops, sound, or entity damage. */
    public static void volcanicTerrainExplosion(ServerLevel level, Vec3 pos, float radius, BlockState lava) {
        allocateLegacyExplosionBlocks(null, level, pos, radius, 48, 0.0F, lava, true);
    }

    public static void promptNuke(Entity source, Vec3 pos, boolean full) {
        if (source.level() instanceof ServerLevel serverLevel) {
            if (full) {
                NukeExplosionManager.scheduleMissileNuke(serverLevel, pos.x, pos.y, pos.z);
            } else {
                LegacyMukeExplosion.detonateMediumMiniNuke(serverLevel, source, pos);
            }
        }
    }

    public static void phosphorus(Entity source, Vec3 pos, int radius) {
        phosphorus(source, pos, pos, radius, radius, Math.max(6.0F, radius * 0.65F));
    }

    public static void phosphorus(Entity source, Vec3 pos, int radius, float explosionSize) {
        phosphorus(source, pos, pos, radius, radius, explosionSize);
    }

    public static void phosphorus(Entity source, Vec3 pos, int radius, int ignitionRadius, float explosionSize) {
        phosphorus(source, pos, pos, radius, ignitionRadius, explosionSize, radius, Math.max(1, radius / 3), explosionSize);
    }

    public static void phosphorus(Entity source, Vec3 explosionPos, Vec3 effectPos, int radius, int ignitionRadius, float explosionSize) {
        phosphorus(source, explosionPos, effectPos, radius, ignitionRadius, explosionSize,
                radius, Math.max(1, radius / 3), explosionSize);
    }

    public static void phosphorus(Entity source, Vec3 explosionPos, Vec3 effectPos,
                                  int radius, int ignitionRadius, float explosionSize,
                                  int shrapnelCount, int hazeCount, float mushroomScale) {
        phosphorus(source, explosionPos, effectPos, radius, ignitionRadius, explosionSize,
                shrapnelCount, hazeCount, radius * 2.0D / 3.0D, mushroomScale);
    }

    public static void phosphorus(Entity source, Vec3 explosionPos, Vec3 effectPos,
                                  int radius, int ignitionRadius, float explosionSize,
                                  int shrapnelCount, int hazeCount, double hazeSpread, float mushroomScale) {
        source.level().playSound(null, source.getX(), source.getY(), source.getZ(),
                HbmSoundEvents.WEAPON_EXPLOSION_MEDIUM.get(), SoundSource.HOSTILE,
                20.0F, 0.9F + source.level().random.nextFloat() * 0.2F);
        standardExplosion(source, explosionPos, explosionSize, 3.0F, false, true, false);
        spawnShrapnel(source.level(), effectPos, shrapnelCount);
        igniteArea(source.level(), BlockPos.containing(effectPos), ignitionRadius);
        AABB area = new AABB(source.position(), source.position()).inflate(radius);
        for (Entity entity : source.level().getEntities(source, area, Entity::isAlive)) {
            entity.igniteForSeconds(5.0F);
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(HbmMobEffects.PHOSPHORUS_BURN, 30 * 20, 0, true, true));
            }
        }
        if (source.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < hazeCount; i++) {
                serverLevel.sendParticles(HbmParticleTypes.HAZE.get(),
                        effectPos.x + serverLevel.random.nextGaussian() * hazeSpread,
                        effectPos.y,
                        effectPos.z + serverLevel.random.nextGaussian() * hazeSpread,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            serverLevel.sendParticles(HbmParticleTypes.RBMK_MUSH.get(),
                    effectPos.x, effectPos.y, effectPos.z, 0, mushroomScale, 0.0D, 0.0D, 1.0D);
        }
    }

    public static void spawnShrapnel(Level level, Vec3 center, int count) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < count; i++) {
            int verticalMultiplier = 1 + count / (15 + serverLevel.random.nextInt(21));
            double motionY = ((serverLevel.random.nextFloat() * 0.5D) + 0.5D) * verticalMultiplier
                    + (serverLevel.random.nextFloat() / 50.0D * count);
            double horizontalMultiplier = 1 + count / 50;
            Vec3 motion = new Vec3(
                    serverLevel.random.nextGaussian() * horizontalMultiplier,
                    motionY,
                    serverLevel.random.nextGaussian() * horizontalMultiplier
            );
            serverLevel.addFreshEntity(new LegacyShrapnelEntity(
                    serverLevel, center.x, center.y, center.z, motion, serverLevel.random.nextInt(3) == 0));
        }
    }

    public static void spawnShrapnelShower(Level level, Vec3 center, Vec3 motion, int count, double deviation) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < count; i++) {
            Vec3 randomizedMotion = new Vec3(
                    motion.x + serverLevel.random.nextGaussian() * deviation,
                    motion.y + serverLevel.random.nextGaussian() * deviation,
                    motion.z + serverLevel.random.nextGaussian() * deviation
            );
            serverLevel.addFreshEntity(new LegacyShrapnelEntity(
                    serverLevel,
                    center.x,
                    center.y,
                    center.z,
                    randomizedMotion,
                    serverLevel.random.nextInt(3) == 0
            ));
        }
    }

    public static void landmineExplosion(
            ServerLevel level,
            Vec3 center,
            float radius,
            float fixedDamage,
            double nodeDistance,
            float thresholdNegation,
            float resistancePiercing,
            boolean breakBlocks,
            int resolution,
            boolean preserveFluids,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMultiplier
    ) {
        if (breakBlocks) {
            allocateLandmineBlocks(level, center, radius, resolution, preserveFluids);
        }
        applyLandmineDamage(level, center, radius, fixedDamage, nodeDistance,
                thresholdNegation, resistancePiercing, 1.0F);
        sendSmallExplosionEffect(level, center, cloudCount, cloudScale, cloudSpeedMultiplier);
    }

    public static void landmineDamage(
            ServerLevel level,
            Vec3 center,
            float radius,
            float fixedDamage,
            double nodeDistance,
            float thresholdNegation,
            float resistancePiercing,
            float rangeMultiplier
    ) {
        applyLandmineDamage(level, center, radius, fixedDamage, nodeDistance,
                thresholdNegation, resistancePiercing, rangeMultiplier);
    }

    public static void gas(Entity source, Vec3 hitPos, LegacyMistEntity.MistType type) {
        Level level = source.level();
        Vec3 direction = source.getDeltaMovement().normalize();
        Vec3 center = hitPos.subtract(direction);
        int cloudCount;
        float width;
        float height;
        double yOffset;
        double spread;
        double poisonPollution;
        double heavyMetalPollution;
        switch (type) {
            case CHLORINE -> {
                cloudCount = 1;
                width = 15.0F;
                height = 7.5F;
                yOffset = -3.0D;
                spread = 0.0D;
                poisonPollution = 0.0D;
                heavyMetalPollution = 5.0D;
            }
            case PHOSGENE -> {
                cloudCount = 3;
                width = 15.0F;
                height = 10.0F;
                yOffset = -5.0D;
                spread = 15.0D;
                poisonPollution = 15.0D;
                heavyMetalPollution = 10.0D;
            }
            case MUSTARD -> {
                cloudCount = 5;
                width = 20.0F;
                height = 10.0F;
                yOffset = -5.0D;
                spread = 25.0D;
                poisonPollution = 30.0D;
                heavyMetalPollution = 15.0D;
            }
            default -> throw new IllegalStateException("Unexpected mist type: " + type);
        }
        level.explode(source, center.x, center.y, center.z, 5.0F, Level.ExplosionInteraction.NONE);
        if (!level.isClientSide) {
            for (int i = 0; i < cloudCount; i++) {
                double x = center.x;
                double z = center.z;
                if (i > 0) {
                    x += level.random.nextGaussian() * spread;
                    z += level.random.nextGaussian() * spread;
                }
                level.addFreshEntity(new LegacyMistEntity(level, x, center.y + yOffset, z, type, width, height));
            }
        }
        BlockPos pollutionPos = BlockPos.containing(hitPos);
        HbmPollution.increment(level, pollutionPos, HbmPollutionType.POISON, poisonPollution);
        HbmPollution.increment(level, pollutionPos, HbmPollutionType.HEAVYMETAL, heavyMetalPollution);
    }

    public static SednaImpact bulletImpact(StandardAmmoItem.StandardAmmoType ammo, Entity projectile, Entity target, Vec3 hitLocation, float damageRemaining) {
        if (!(target instanceof LivingEntity living)) {
            hurtNoIFrame(target, projectile.damageSources().source(HbmDamageTypes.SEDNA_PHYSICAL, projectile, null), damageRemaining);
            return new SednaImpact(damageRemaining, true);
        }
        float intendedDamage = damageRemaining;
        double head = living.getBbHeight() - living.getEyeHeight();
        if (living.isAlive()
                && hitLocation.y > living.getY() + living.getBbHeight() - head * 2.0D) {
            intendedDamage *= ammo.headshotMultiplier();
        }

        float previousHealth = living.getHealth();
        float finalDamage = sednaPhysicalDamage(living, intendedDamage, ammo.armorThresholdNegation(), ammo.armorPiercingPercent());
        hurtNoIFrame(living, projectile.damageSources().source(HbmDamageTypes.SEDNA_PHYSICAL, projectile, null), finalDamage);
        float remaining = damageRemaining;
        if (ammo.damageFalloffByPenetration()) {
            remaining -= Math.max(previousHealth - living.getHealth(), 0.0F) * 0.5F;
        }
        return new SednaImpact(remaining, ammo.penetrates() && remaining >= 0.0F);
    }

    public static SednaImpact turretShellImpact(Entity projectile, LivingEntity living, Vec3 hitLocation, float damageRemaining, boolean penetrates, boolean damageFalloff) {
        float intendedDamage = damageRemaining;
        double head = living.getBbHeight() - living.getEyeHeight();
        if (living.isAlive() && hitLocation.y > living.getY() + living.getBbHeight() - head * 2.0D) {
            intendedDamage *= 1.25F;
        }
        float previousHealth = living.getHealth();
        float finalDamage = sednaPhysicalDamage(living, intendedDamage, 0.0F, 0.0F);
        hurtNoIFrame(living, projectile.damageSources().source(HbmDamageTypes.SEDNA_PHYSICAL, projectile, null), finalDamage);
        float remaining = damageRemaining;
        if (damageFalloff) {
            remaining -= Math.max(previousHealth - living.getHealth(), 0.0F) * 0.5F;
        }
        return new SednaImpact(remaining, penetrates && remaining >= 0.0F);
    }

    private static float sednaPhysicalDamage(LivingEntity living, float amount, float thresholdNegation, float armorPiercing) {
        Resistance resistance = physicalResistance(ArmorFSBItem.fullSetGroup(living));
        float threshold = Math.max(0.0F, resistance.threshold() - thresholdNegation);
        if (threshold >= amount) {
            return 0.0F;
        }
        amount -= threshold;
        float resistanceFactor = resistance.resistance() * Mth.clamp(1.0F - armorPiercing, 0.0F, 2.0F);
        amount *= 1.0F - resistanceFactor;
        float effectiveArmor = living.getArmorValue() * (1.0F - armorPiercing);
        return amount * Math.max(0.0F, 25.0F - effectiveArmor) / 25.0F;
    }

    private static Resistance physicalResistance(String group) {
        return switch (group) {
            case "steel" -> new Resistance(2.0F, 0.10F);
            case "titanium" -> new Resistance(3.0F, 0.10F);
            case "alloy", "cobalt" -> new Resistance(2.0F, 0.10F);
            case "starmetal" -> new Resistance(3.0F, 0.25F);
            case "security", "cmb" -> new Resistance(5.0F, 0.50F);
            case "schrabidium" -> new Resistance(10.0F, 0.65F);
            default -> Resistance.NONE;
        };
    }

    private static Resistance explosiveResistance(String group) {
        return switch (group) {
            case "security" -> new Resistance(2.0F, 0.25F);
            case "starmetal" -> new Resistance(1.0F, 0.10F);
            case "cmb" -> new Resistance(5.0F, 0.25F);
            case "schrabidium" -> new Resistance(5.0F, 0.50F);
            default -> Resistance.NONE;
        };
    }

    private static float sednaExplosiveDamage(
            LivingEntity living,
            float amount,
            float thresholdNegation,
            float resistancePiercing
    ) {
        Resistance resistance = explosiveResistance(ArmorFSBItem.fullSetGroup(living));
        float threshold = Math.max(0.0F, resistance.threshold() - thresholdNegation);
        if (threshold >= amount) {
            return 0.0F;
        }
        amount -= threshold;
        return amount * (1.0F - resistance.resistance()
                * Mth.clamp(1.0F - resistancePiercing, 0.0F, 2.0F));
    }

    public static void hurtSednaFire(Entity target, Entity projectile, float amount) {
        if (target instanceof LivingEntity living) {
            Resistance resistance = fireResistance(ArmorFSBItem.fullSetGroup(living));
            if (resistance.threshold() >= amount) {
                return;
            }
            amount = (amount - resistance.threshold()) * (1.0F - resistance.resistance());
        }
        hurtNoIFrame(target,
                projectile.damageSources().source(HbmDamageTypes.SEDNA_FIRE, projectile, null),
                amount);
    }

    private static Resistance fireResistance(String group) {
        return switch (group) {
            case "asbestos" -> new Resistance(10.0F, 0.90F);
            case "starmetal" -> new Resistance(1.0F, 0.10F);
            case "cmb" -> new Resistance(5.0F, 0.25F);
            case "schrabidium" -> new Resistance(5.0F, 0.50F);
            default -> Resistance.NONE;
        };
    }

    public record SednaImpact(float remainingDamage, boolean continueFlight) {
    }

    private record Resistance(float threshold, float resistance) {
        private static final Resistance NONE = new Resistance(0.0F, 0.0F);
    }

    public static void rocketImpact(StandardAmmoItem.StandardAmmoType ammo, Entity projectile, Vec3 pos, @Nullable Entity directHit) {
        switch (ammo) {
            case ROCKET_HE -> fixedDamageExplosion(projectile, pos, 5.0F, ammo.damage(), false);
            case ROCKET_HEAT -> {
                fixedDamageExplosion(projectile, pos, 3.5F, ammo.damage(), false);
                if (directHit instanceof LivingEntity living && living.isAlive()) {
                    hurtHeatDirect(living, projectile, ammo.damage() * 3.0F);
                } else if (directHit != null && directHit.isAlive()) {
                    directHit.hurt(projectile.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE), ammo.damage() * 3.0F);
                }
            }
            case ROCKET_DEMO -> fixedDamageExplosion(projectile, pos, 5.0F, ammo.damage(), true);
            case ROCKET_INC -> {
                fixedDamageExplosion(projectile, pos, 3.0F, ammo.damage(), false);
                spawnLingeringFire(projectile, pos, LegacyLingeringFireEntity.FireType.DIESEL, 6.0F, 2.0F, 300, false);
                igniteCube(projectile.level(), BlockPos.containing(pos), 2);
            }
            case ROCKET_PHOSPHORUS -> {
                fixedDamageExplosion(projectile, pos, 3.0F, ammo.damage(), false);
                spawnLingeringFire(projectile, pos, LegacyLingeringFireEntity.FireType.PHOSPHORUS, 6.0F, 2.0F, 600, false);
                igniteCube(projectile.level(), BlockPos.containing(pos), 2);
            }
            default -> fixedDamageExplosion(projectile, pos, 5.0F, ammo.damage(), false);
        }
    }

    private static void hurtHeatDirect(LivingEntity living, Entity projectile, float amount) {
        Resistance resistance = explosiveResistance(ArmorFSBItem.fullSetGroup(living));
        float threshold = Math.max(0.0F, resistance.threshold() - 5.0F);
        if (threshold >= amount) {
            return;
        }
        amount -= threshold;
        amount *= 1.0F - resistance.resistance() * 0.8F;
        float effectiveArmor = living.getArmorValue() * 0.8F;
        float damage = amount * Math.max(0.0F, 25.0F - effectiveArmor) / 25.0F;
        hurtNoIFrame(living, projectile.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE, projectile, null), damage);
    }

    public static void spawnLingeringFire(
            Entity source,
            Vec3 pos,
            LegacyLingeringFireEntity.FireType type,
            float width,
            float height,
            int duration,
            boolean avoidDuplicates
    ) {
        Level level = source.level();
        if (level.isClientSide) {
            return;
        }
        if (avoidDuplicates) {
            AABB search = new AABB(pos, pos).inflate(width * 0.5D + 0.5D, height * 0.5D + 0.5D, width * 0.5D + 0.5D);
            if (!level.getEntitiesOfClass(LegacyLingeringFireEntity.class, search).isEmpty()) {
                return;
            }
        }
        level.addFreshEntity(new LegacyLingeringFireEntity(level, pos, type, width, height, duration));
    }

    private static void igniteCube(Level level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isEmptyBlock(cursor)) {
                        continue;
                    }
                    for (Direction direction : Direction.values()) {
                        BlockPos support = cursor.relative(direction);
                        if (level.getBlockState(support).isFlammable(level, support, direction.getOpposite())) {
                            level.setBlock(cursor, Blocks.FIRE.defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static void igniteArea(Level level, BlockPos center, int radius) {
        int r2 = radius * radius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z > r2 || level.random.nextInt(3) != 0) {
                        continue;
                    }
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isEmptyBlock(cursor)) {
                        continue;
                    }
                    for (Direction direction : Direction.values()) {
                        BlockPos support = cursor.relative(direction);
                        if (level.getBlockState(support).isFlammable(level, support, direction.getOpposite())) {
                            level.setBlock(cursor, Blocks.FIRE.defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }
        }
    }

    /** Direct ExplosionChaos#igniteAllBlocks port used only by BombMulti. */
    public static void igniteAllBlocksLegacy(Level level, BlockPos center, int radius) {
        int radiusSquared = radius * radius;
        int innerRadiusSquared = radiusSquared / 2;
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (int x = -radius; x < radius; x++) {
            int xSquared = x * x;
            for (int y = -radius; y < radius; y++) {
                int xySquared = xSquared + y * y;
                for (int z = -radius; z < radius; z++) {
                    if (xySquared + z * z >= innerRadiusSquared) {
                        continue;
                    }
                    target.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    above.set(target.getX(), target.getY() + 1, target.getZ());
                    BlockState aboveState = level.getBlockState(above);
                    if ((aboveState.isAir() || aboveState.is(Blocks.SNOW)) && !level.getBlockState(target).isAir()) {
                        level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /** Direct ExplosionNukeGeneric#wasteNoSchrab port used by BombMulti poison pellets. */
    public static void wasteNoSchrab(ServerLevel level, BlockPos center, int radius) {
        int radiusSquared = radius * radius;
        int innerRadiusSquared = radiusSquared / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x < radius; x++) {
            int xSquared = x * x;
            for (int y = -radius; y < radius; y++) {
                int xySquared = xSquared + y * y;
                for (int z = -radius; z < radius; z++) {
                    if (xySquared + z * z >= innerRadiusSquared + level.random.nextInt(innerRadiusSquared / 5)) {
                        continue;
                    }
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir()) {
                        wasteDestinationNoSchrab(level, cursor, state);
                    }
                }
            }
        }
    }

    private static void wasteDestinationNoSchrab(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.GLASS) || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassBlock
                || isLegacyDoor(state) || isLegacyLeaves(state)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, HbmBlocks.WASTE_EARTH.get().defaultBlockState(), 3);
        } else if (state.is(Blocks.MYCELIUM)) {
            level.setBlock(pos, HbmBlocks.WASTE_MYCELIUM.get().defaultBlockState(), 3);
        } else if (state.is(Blocks.SAND)) {
            if (level.random.nextInt(20) == 1) {
                level.setBlock(pos, HbmBlocks.WASTE_TRINITITE.get().defaultBlockState(), 3);
            }
        } else if (state.is(Blocks.RED_SAND)) {
            if (level.random.nextInt(20) == 1) {
                level.setBlock(pos, HbmBlocks.WASTE_TRINITITE_RED.get().defaultBlockState(), 3);
            }
        } else if (state.is(Blocks.CLAY)) {
            level.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), 3);
        } else if (state.is(Blocks.MOSSY_COBBLESTONE)) {
            level.setBlock(pos, Blocks.COAL_ORE.defaultBlockState(), 3);
        } else if (state.is(Blocks.COAL_ORE)) {
            int roll = level.random.nextInt(30);
            if (roll == 1 || roll == 2 || roll == 3) {
                level.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
            }
            if (roll == 29) {
                level.setBlock(pos, Blocks.EMERALD_ORE.defaultBlockState(), 3);
            }
        } else if (isLegacyLog(state) || state.is(Blocks.MUSHROOM_STEM)) {
            level.setBlock(pos, HbmBlocks.WASTE_LOG.get().defaultBlockState(), 3);
        } else if (isLegacyPlank(state)) {
            level.setBlock(pos, HbmBlocks.WASTE_PLANKS.get().defaultBlockState(), 3);
        } else if (state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static boolean isLegacyDoor(BlockState state) {
        return state.is(Blocks.IRON_DOOR) || state.is(Blocks.OAK_DOOR) || state.is(Blocks.SPRUCE_DOOR)
                || state.is(Blocks.BIRCH_DOOR) || state.is(Blocks.JUNGLE_DOOR) || state.is(Blocks.ACACIA_DOOR)
                || state.is(Blocks.DARK_OAK_DOOR);
    }

    private static boolean isLegacyLeaves(BlockState state) {
        return state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.BIRCH_LEAVES)
                || state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.ACACIA_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES);
    }

    private static boolean isLegacyLog(BlockState state) {
        return state.is(Blocks.OAK_LOG) || state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.BIRCH_LOG)
                || state.is(Blocks.JUNGLE_LOG) || state.is(Blocks.ACACIA_LOG) || state.is(Blocks.DARK_OAK_LOG);
    }

    private static boolean isLegacyPlank(BlockState state) {
        return state.is(Blocks.OAK_PLANKS) || state.is(Blocks.SPRUCE_PLANKS) || state.is(Blocks.BIRCH_PLANKS)
                || state.is(Blocks.JUNGLE_PLANKS) || state.is(Blocks.ACACIA_PLANKS) || state.is(Blocks.DARK_OAK_PLANKS);
    }

    private static void applyLegacyCrossDamage(Entity source, Vec3 pos, float radius, float rangeMod) {
        applyLegacyCrossDamage(source.level(), source, pos, radius, rangeMod);
    }

    private static void applyLegacyCrossDamage(Level level, @Nullable Entity source, Vec3 pos, float radius, float rangeMod) {
        double size = radius * 2.0D * rangeMod;
        AABB area = new AABB(pos, pos).inflate(size + 1.0D);
        Vec3[] nodes = legacyCrossNodes(pos, 7.5D);
        for (Entity entity : level.getEntities(source, area, entity -> entity.isAlive() && entity != source)) {
            double distanceScaled = legacyBoxDistance(entity, pos) / size;
            if (distanceScaled > 1.0D) {
                continue;
            }
            Vec3 delta = new Vec3(entity.getX() - pos.x, entity.getEyeY() - pos.y, entity.getZ() - pos.z);
            double distance = delta.length();
            if (distance == 0.0D) {
                continue;
            }
            Vec3 normal = delta.scale(1.0D / distance);
            double density = 0.0D;
            for (Vec3 node : nodes) {
                density = Math.max(density, Explosion.getSeenPercent(node, entity));
            }
            double knockback = (1.0D - distanceScaled) * density;
            float damage = (float) ((int) ((knockback * knockback + knockback) / 2.0D * 8.0D * size + 1.0D));
            if (damage > 0.0F) {
                entity.hurt(level.damageSources().explosion(null), damage);
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(normal.scale(knockback)));
            entity.hurtMarked = true;
        }
    }

    private static void applyLegacyFixedCrossDamage(Entity source, Vec3 pos, float radius, float fixedDamage, double nodeDist) {
        applyLegacyFixedCrossDamage(source.level(), source, pos, radius, fixedDamage, nodeDist);
    }

    private static void applyLegacyFixedCrossDamage(Level level, @Nullable Entity source, Vec3 pos,
                                                    float radius, float fixedDamage, double nodeDist) {
        double size = radius * 2.0D;
        AABB area = new AABB(pos, pos).inflate(size + 1.0D);
        Vec3[] nodes = legacyCrossNodes(pos, nodeDist);
        for (Entity entity : level.getEntities(source, area, entity -> entity.isAlive())) {
            double distanceScaled = legacyBoxDistance(entity, pos) / size;
            if (distanceScaled > 1.0D) {
                continue;
            }
            Vec3 delta = new Vec3(entity.getX() - pos.x, entity.getEyeY() - pos.y, entity.getZ() - pos.z);
            double distance = delta.length();
            if (distance == 0.0D) {
                continue;
            }
            double density = 0.0D;
            for (Vec3 node : nodes) {
                density = Math.max(density, Explosion.getSeenPercent(node, entity));
            }
            if (density < 0.125D) {
                continue;
            }
            double knockback = (1.0D - distanceScaled) * density;
            float damage = (float) (fixedDamage * (1.0D - distanceScaled));
            if (damage > 0.0F) {
                entity.hurt(level.damageSources().explosion(source, null), damage);
            }
            Vec3 normal = delta.scale(1.0D / distance);
            entity.setDeltaMovement(entity.getDeltaMovement().add(normal.scale(knockback)));
            entity.hurtMarked = true;
        }
    }

    private static void applyLandmineDamage(
            ServerLevel level,
            Vec3 center,
            float radius,
            float fixedDamage,
            double nodeDistance,
            float thresholdNegation,
            float resistancePiercing,
            float rangeMultiplier
    ) {
        double size = radius * 2.0D * rangeMultiplier;
        AABB area = new AABB(center, center).inflate(size + 1.0D);
        Vec3[] nodes = legacyCrossNodes(center, nodeDistance);
        for (Entity entity : level.getEntities((Entity) null, area, Entity::isAlive)) {
            double distanceScaled = legacyBoxDistance(entity, center) / size;
            if (distanceScaled > 1.0D) {
                continue;
            }
            Vec3 delta = new Vec3(entity.getX() - center.x, entity.getEyeY() - center.y, entity.getZ() - center.z);
            double distance = delta.length();
            if (distance == 0.0D) {
                continue;
            }
            double density = 0.0D;
            for (Vec3 node : nodes) {
                density = Math.max(density, Explosion.getSeenPercent(node, entity));
            }
            if (density < 0.125D) {
                continue;
            }
            float damage = (float) (fixedDamage * (1.0D - distanceScaled));
            if (entity instanceof LivingEntity living) {
                damage = sednaExplosiveDamage(living, damage, thresholdNegation, resistancePiercing);
            }
            if (damage > 0.0F) {
                hurtNoIFrame(entity, level.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE), damage);
            }
            double knockback = (1.0D - distanceScaled) * density;
            entity.setDeltaMovement(entity.getDeltaMovement().add(delta.scale(knockback / distance)));
            entity.hurtMarked = true;
        }
    }

    private static Vec3[] legacyCrossNodes(Vec3 pos, double nodeDist) {
        return new Vec3[] {
                pos,
                pos.add(0.0D, -nodeDist, 0.0D),
                pos.add(0.0D, nodeDist, 0.0D),
                pos.add(0.0D, 0.0D, -nodeDist),
                pos.add(0.0D, 0.0D, nodeDist),
                pos.add(-nodeDist, 0.0D, 0.0D),
                pos.add(nodeDist, 0.0D, 0.0D)
        };
    }

    private static double legacyBoxDistance(Entity entity, Vec3 pos) {
        AABB box = entity.getBoundingBox();
        double xDist = box.minX <= pos.x && box.maxX >= pos.x ? 0.0D : Math.min(Math.abs(box.minX - pos.x), Math.abs(box.maxX - pos.x));
        double yDist = box.minY <= pos.y && box.maxY >= pos.y ? 0.0D : Math.min(Math.abs(box.minY - pos.y), Math.abs(box.maxY - pos.y));
        double zDist = box.minZ <= pos.z && box.maxZ >= pos.z ? 0.0D : Math.min(Math.abs(box.minZ - pos.z), Math.abs(box.maxZ - pos.z));
        return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
    }

    private static void allocateLegacyExplosionBlocks(Entity source, ServerLevel level, Vec3 pos, float radius, boolean slagDebris) {
        allocateLegacyExplosionBlocks(source, level, pos, radius, 48, 0.0F,
                slagDebris ? HbmBlocks.BLOCK_SLAG.get().defaultBlockState() : null);
    }

    private static void allocateLegacyExplosionBlocks(Entity source, ServerLevel level, Vec3 pos, float radius, BlockState debris) {
        allocateLegacyExplosionBlocks(source, level, pos, radius, 48, 0.0F, debris);
    }

    private static void allocateLegacyWeaponExplosionBlocks(Entity source, ServerLevel level, Vec3 pos, float radius) {
        allocateLegacyExplosionBlocks(source, level, pos, radius, 16, 1.0F / radius, null);
    }

    private static void allocateLegacyExplosionBlocks(Entity source, ServerLevel level, Vec3 pos, float radius,
                                                       int resolution, float dropChance, @Nullable BlockState debris) {
        allocateLegacyExplosionBlocks(source, level, pos, radius, resolution, dropChance, debris, false);
    }

    private static void allocateLegacyExplosionBlocks(Entity source, ServerLevel level, Vec3 pos, float radius,
                                                       int resolution, float dropChance, @Nullable BlockState debris,
                                                       boolean replaceAffectedWithDebris) {
        Set<BlockPos> affected = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                for (int k = 0; k < resolution; k++) {
                    if (i != 0 && i != resolution - 1 && j != 0 && j != resolution - 1 && k != 0 && k != resolution - 1) {
                        continue;
                    }
                    double dx = (double) i / ((double) resolution - 1.0D) * 2.0D - 1.0D;
                    double dy = (double) j / ((double) resolution - 1.0D) * 2.0D - 1.0D;
                    double dz = (double) k / ((double) resolution - 1.0D) * 2.0D - 1.0D;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (len <= 1.0E-7D) {
                        continue;
                    }
                    dx /= len;
                    dy /= len;
                    dz /= len;
                    float power = radius * (0.7F + level.random.nextFloat() * 0.6F);
                    double x = pos.x;
                    double y = pos.y;
                    double z = pos.z;
                    for (float step = 0.3F; power > 0.0F; power -= step * 0.75F) {
                        cursor.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
                        if (!level.isInWorldBounds(cursor)) {
                            break;
                        }
                        BlockState state = level.getBlockState(cursor);
                        if (!state.isAir()) {
                            power -= (state.getBlock().getExplosionResistance() + 0.3F) * step;
                        }
                        if (power > 0.0F && !state.isAir()) {
                            affected.add(cursor.immutable());
                        }
                        x += dx * step;
                        y += dy * step;
                        z += dz * step;
                    }
                }
            }
        }
        for (BlockPos blockPos : affected) {
            BlockState state = level.getBlockState(blockPos);
            if (!state.isAir() && state.getDestroySpeed(level, blockPos) >= 0.0F) {
                if (replaceAffectedWithDebris) {
                    // ExplosionNT's LAVA_V/LAVA_R path replaces every affected normal cube directly.
                    level.setBlock(blockPos,
                            state.isSolidRender(level, blockPos) ? debris : Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL);
                    continue;
                }
                boolean drops = dropChance > 0.0F && level.random.nextFloat() < dropChance;
                level.destroyBlock(blockPos, drops, source);
            }
        }
        if (replaceAffectedWithDebris) {
            return;
        }
        if (debris == null) {
            return;
        }
        for (BlockPos blockPos : affected) {
            if (!level.isEmptyBlock(blockPos) || !hasSolidNeighbor(level, blockPos, debris.getBlock())) {
                continue;
            }
            level.setBlock(blockPos, debris, Block.UPDATE_ALL);
        }
    }

    private static void allocateLandmineBlocks(
            ServerLevel level,
            Vec3 center,
            float radius,
            int resolution,
            boolean preserveFluids
    ) {
        Set<BlockPos> affected = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int xIndex = 0; xIndex < resolution; xIndex++) {
            for (int yIndex = 0; yIndex < resolution; yIndex++) {
                for (int zIndex = 0; zIndex < resolution; zIndex++) {
                    if (xIndex != 0 && xIndex != resolution - 1
                            && yIndex != 0 && yIndex != resolution - 1
                            && zIndex != 0 && zIndex != resolution - 1) {
                        continue;
                    }
                    double xDirection = (double) xIndex / (resolution - 1.0D) * 2.0D - 1.0D;
                    double yDirection = (double) yIndex / (resolution - 1.0D) * 2.0D - 1.0D;
                    double zDirection = (double) zIndex / (resolution - 1.0D) * 2.0D - 1.0D;
                    double length = Math.sqrt(xDirection * xDirection + yDirection * yDirection + zDirection * zDirection);
                    xDirection /= length;
                    yDirection /= length;
                    zDirection /= length;
                    float power = radius * (0.7F + level.random.nextFloat() * 0.6F);
                    double x = center.x;
                    double y = center.y;
                    double z = center.z;
                    for (float step = 0.3F; power > 0.0F; power -= step * 0.75F) {
                        cursor.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
                        if (!level.isInWorldBounds(cursor)) {
                            break;
                        }
                        BlockState state = level.getBlockState(cursor);
                        boolean fluid = !state.getFluidState().isEmpty();
                        if (!state.isAir() && !(preserveFluids && fluid)) {
                            power -= (state.getBlock().getExplosionResistance() + 0.3F) * step;
                        }
                        if (power > 0.0F && !state.isAir() && !(preserveFluids && fluid)) {
                            affected.add(cursor.immutable());
                        }
                        x += xDirection * step;
                        y += yDirection * step;
                        z += zDirection * step;
                    }
                }
            }
        }
        float dropChance = 1.0F / radius;
        for (BlockPos blockPos : affected) {
            BlockState state = level.getBlockState(blockPos);
            if (!state.isAir() && state.getDestroySpeed(level, blockPos) >= 0.0F) {
                level.destroyBlock(blockPos, level.random.nextFloat() < dropChance);
            }
        }
    }

    private static boolean hasSolidNeighbor(Level level, BlockPos pos, Block debrisBlock) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState state = level.getBlockState(neighbor);
            if (!state.is(debrisBlock) && state.isSolid()) {
                return true;
            }
        }
        return false;
    }

    private static void sendExplosionParticles(Level level, Vec3 pos, int count) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, count, 1.5D, 1.5D, 1.5D, 0.1D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, count * 2, 2.0D, 1.5D, 2.0D, 0.08D);
        }
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 12.0F, 0.9F + level.random.nextFloat() * 0.2F);
    }

    /** Shared 1.7.10 ExplosionEffectWeapon-compatible small explosion visual. */
    public static void sendSmallExplosionEffect(Level level, Vec3 pos, int cloudCount, float cloudScale, float cloudSpeedMultiplier) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LegacySmallExplosionEffectPayload payload = new LegacySmallExplosionEffectPayload(
                pos.x, pos.y, pos.z, cloudCount, cloudScale, cloudSpeedMultiplier);
        double rangeSquared = 200.0D * 200.0D;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(pos) <= rangeSquared) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public enum ArtyType {
        NORMAL(0, "ammo_arty"),
        CLASSIC(1, "ammo_arty_classic"),
        HE(2, "ammo_arty_he"),
        MINI_NUKE(3, "ammo_arty_mini_nuke"),
        NUKE(4, "ammo_arty_nuke"),
        PHOSPHORUS(5, "ammo_arty_phosphorus"),
        MINI_NUKE_MULTI(6, "ammo_arty_mini_nuke_multi"),
        PHOSPHORUS_MULTI(7, "ammo_arty_phosphorus_multi"),
        CARGO(8, "ammo_arty_cargo"),
        CHLORINE(9, "ammo_arty_chlorine"),
        PHOSGENE(10, "ammo_arty_phosgene"),
        MUSTARD(11, "ammo_arty_mustard_gas");

        private final int modelData;
        private final String id;

        ArtyType(int modelData, String id) {
            this.modelData = modelData;
            this.id = id;
        }

        public int modelData() {
            return modelData;
        }

        public String id() {
            return id;
        }

        public static ArtyType byModelData(int modelData) {
            for (ArtyType type : values()) {
                if (type.modelData == modelData) {
                    return type;
                }
            }
            return NORMAL;
        }
    }

    public enum HimarsType {
        STANDARD(0, "standard", 6, 0),
        SINGLE(1, "single", 1, 1),
        STANDARD_HE(2, "standard_he", 6, 0),
        STANDARD_WP(3, "standard_wp", 6, 0),
        STANDARD_TB(4, "standard_tb", 6, 0),
        SINGLE_TB(5, "single_tb", 1, 1),
        STANDARD_MINI_NUKE(6, "standard_mini_nuke", 6, 0),
        STANDARD_LAVA(7, "standard_lava", 6, 0);

        private final int modelData;
        private final String id;
        private final int amount;
        private final int modelType;

        HimarsType(int modelData, String id, int amount, int modelType) {
            this.modelData = modelData;
            this.id = id;
            this.amount = amount;
            this.modelType = modelType;
        }

        public int modelData() {
            return modelData;
        }

        public String id() {
            return id;
        }

        public int amount() {
            return amount;
        }

        public int modelType() {
            return modelType;
        }

        public String textureName() {
            return ("himars_" + id).toLowerCase(Locale.ROOT);
        }

        public static HimarsType byModelData(int modelData) {
            for (HimarsType type : values()) {
                if (type.modelData == modelData) {
                    return type;
                }
            }
            return STANDARD;
        }
    }
}
