package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.ArmorFSBItem;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class LegacyMistEntity extends Entity {
    private static final EntityDataAccessor<Integer> MIST_TYPE =
            SynchedEntityData.defineId(LegacyMistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AREA_WIDTH =
            SynchedEntityData.defineId(LegacyMistEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AREA_HEIGHT =
            SynchedEntityData.defineId(LegacyMistEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(LegacyMistEntity.class, EntityDataSerializers.INT);

    public LegacyMistEntity(EntityType<? extends LegacyMistEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public LegacyMistEntity(Level level, double x, double y, double z, MistType type, float width, float height) {
        this(level, x, y, z, type, width, height, 150);
    }

    public LegacyMistEntity(Level level, double x, double y, double z, MistType type,
                            float width, float height, int duration) {
        this(HbmEntityTypes.LEGACY_MIST.get(), level);
        setPos(x, y, z);
        this.entityData.set(MIST_TYPE, type.ordinal());
        this.entityData.set(AREA_WIDTH, width);
        this.entityData.set(AREA_HEIGHT, height);
        this.entityData.set(MAX_AGE, duration);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MIST_TYPE, MistType.CHLORINE.ordinal());
        builder.define(AREA_WIDTH, 0.0F);
        builder.define(AREA_HEIGHT, 0.0F);
        builder.define(MAX_AGE, 150);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnLegacyParticles();
            return;
        }
        int maxAge = Math.max(1, this.entityData.get(MAX_AGE));
        double intensity = 1.0D - (double) this.tickCount / (double) maxAge;
        MistType type = mistType();
        for (Entity entity : level().getEntities(this, effectBounds(), Entity::isAlive)) {
            entity.clearFire();
            if (entity instanceof LivingEntity living) {
                affectLiving(type, living, intensity);
            }
        }
        if (this.tickCount >= maxAge) {
            discard();
        }
    }

    private void affectLiving(MistType type, LivingEntity living, double intensity) {
        if (type == MistType.SULFURIC_ACID) {
            LegacyProjectileUtil.hurtNoIFrame(living,
                    damageSources().source(HbmDamageTypes.ACID, this, null), 50.0F / 60.0F);
            for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                    net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET}) {
                living.getItemBySlot(slot).hurtAndBreak(1, living, slot);
            }
            return;
        }
        if (type == MistType.PHEROMONE || type == MistType.PHEROMONE_M) {
            int multiplier = type == MistType.PHEROMONE ? 1 : 2;
            boolean matchingTarget = type == MistType.PHEROMONE
                    ? living instanceof GlyphidEntity
                    : living instanceof net.minecraft.world.entity.player.Player;
            if (matchingTarget) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, multiplier * 60 * 20, 1));
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, multiplier * 60 * 20, 1));
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, multiplier * 2 * 20, 0));
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, multiplier * 60 * 20, 0));
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, multiplier * 60 * 20, 1));
                living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, multiplier * 60 * 20, 0));
            }
            return;
        }
        if (type == MistType.CHLORINE) {
            LegacyProjectileUtil.hurtNoIFrame(
                    living,
                    damageSources().source(HbmDamageTypes.ACID, this, null),
                    25.0F / 60.0F
            );
        }

        HbmArmorProtection.HazardClass hazard = type == MistType.MUSTARD
                ? HbmArmorProtection.HazardClass.GAS_BLISTERING
                : HbmArmorProtection.HazardClass.GAS_LUNG;
        boolean protectedFromDirectDamage = HbmArmorProtection.hasHeadProtection(living, hazard, 1);
        int damageDelay = type == MistType.MUSTARD ? 10 : 20;
        if (!protectedFromDirectDamage && level().getGameTime() % damageDelay == 0L) {
            float damage = (type == MistType.CHLORINE ? 2.0F : 4.0F) * (float) intensity;
            living.hurt(damageSources().source(HbmDamageTypes.CLOUD, this, null), damage);
        }

        if (type != MistType.MUSTARD) {
            return;
        }
        boolean maskProtected = HbmArmorProtection.hasHeadProtection(
                living,
                HbmArmorProtection.HazardClass.GAS_BLISTERING,
                1
        );
        String armorGroup = ArmorFSBItem.fullSetGroup(living);
        boolean hazmatProtected = armorGroup.startsWith("hazmat") || armorGroup.equals("schrabidium");
        if (!maskProtected || !hazmatProtected) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) (100.0D * intensity), 1));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) (100.0D * intensity), 0));
        }
    }

    private void spawnLegacyParticles() {
        MistType type = mistType();
        AABB bounds = effectBounds();
        for (int i = 0; i < 2; i++) {
            double x = bounds.minX + random.nextDouble() * (bounds.maxX - bounds.minX);
            double y = bounds.minY + random.nextDouble() * (bounds.maxY - bounds.minY);
            double z = bounds.minZ + random.nextDouble() * (bounds.maxZ - bounds.minZ);
            level().addParticle(
                    HbmParticleTypes.LEGACY_MIST.get(),
                    x, y, z,
                    ((type.color >>> 16) & 0xFF) / 255.0D,
                    ((type.color >>> 8) & 0xFF) / 255.0D,
                    (type.color & 0xFF) / 255.0D
            );
        }
    }

    private AABB effectBounds() {
        double width = this.entityData.get(AREA_WIDTH);
        double height = this.entityData.get(AREA_HEIGHT);
        double halfWidth = width * 0.5D;
        return new AABB(
                getX() - halfWidth,
                getY(),
                getZ() - halfWidth,
                getX() + halfWidth,
                getY() + height,
                getZ() + halfWidth
        );
    }

    private MistType mistType() {
        int id = this.entityData.get(MIST_TYPE);
        return id >= 0 && id < MistType.values().length ? MistType.values()[id] : MistType.CHLORINE;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("type", this.entityData.get(MIST_TYPE));
        tag.putFloat("width", this.entityData.get(AREA_WIDTH));
        tag.putFloat("height", this.entityData.get(AREA_HEIGHT));
        tag.putInt("max_age", this.entityData.get(MAX_AGE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(MIST_TYPE, tag.getInt("type"));
        this.entityData.set(AREA_WIDTH, tag.getFloat("width"));
        this.entityData.set(AREA_HEIGHT, tag.getFloat("height"));
        this.entityData.set(MAX_AGE, Math.max(1, tag.getInt("max_age")));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public enum MistType {
        CHLORINE(0xBAB572),
        PHOSGENE(0xCFC4A4),
        MUSTARD(0xBAB572),
        SULFURIC_ACID(0xB0AA64),
        PHEROMONE(0x5FA6E8),
        PHEROMONE_M(0x48C9B0);

        private final int color;

        MistType(int color) {
            this.color = color;
        }
    }
}
