package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityTaintCrab. */
public final class LegacyTaintCrabEntity extends LegacyCyberCrabEntity {
    public LegacyTaintCrabEntity(EntityType<? extends LegacyTaintCrabEntity> type, Level level) {
        super(type, level, Kind.TAINT);
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(25.0D, 0.5D);
    }

    @Override
    public void tick() {
        updateTeslaTargets(1.25D, 10.0D);
        if (level().isClientSide) {
            super.tick();
            return;
        }
        AABB area = new AABB(getX() - 5.0D, getY() - 5.0D, getZ() - 5.0D,
                getX() + 5.0D, getY() + 5.0D, getZ() + 5.0D);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (target instanceof LegacyCyberCrabEntity) {
                continue;
            }
            HbmLivingRadiation radiation = HbmLivingRadiation.get(target);
            radiation.addEnvironmentRadiation(0.8F);
            if (!isLegacyRadiationImmune(target)
                    && !(target instanceof Player player
                    && (player.isCreative() || player.isSpectator() || player.tickCount < 200))) {
                radiation.addRadiation((float) (0.8D * HbmArmorProtection.radiationMultiplier(target)));
            }
            HbmLivingRadiation.set(target, radiation);
        }
        super.tick();
    }

    /** EntityTaintCrab uses the 7.62 mm FMJ bullet configuration, with a
     * one-block/tick projectile and base damage 10; it is not the tau round
     * used by the generic cyber crab. */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        Vec3 origin = position().add(0.0D, getEyeHeight() - 0.1D, 0.0D);
        Vec3 direction = getLookAngle().normalize();
        LegacyBulletEntity bullet = new LegacyBulletEntity(
                level(), origin.x, origin.y, origin.z, direction,
                StandardAmmoItem.StandardAmmoType.R762_FMJ, 10.0F, this, 0.0F);
        bullet.setDeltaMovement(direction);
        level().addFreshEntity(bullet);
        playSound(HbmSoundEvents.WEAPON_SAW_SHOOT.get(), 1.0F, 0.5F);
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
