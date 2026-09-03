package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityTaintCrab. */
public final class LegacyTaintCrabEntity extends LegacyCyberCrabEntity {
    public LegacyTaintCrabEntity(EntityType<? extends LegacyTaintCrabEntity> type, Level level) {
        super(type, level, Kind.TAINT);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(25.0D, 0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        updateTeslaTargets(1.25D, 10.0D);
        if (level().isClientSide) {
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
                    && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                radiation.addRadiation(0.8F);
            }
            HbmLivingRadiation.set(target, radiation);
        }
    }

    private static boolean isLegacyRadiationImmune(LivingEntity target) {
        return target instanceof LegacyNuclearCreeperEntity
                || target instanceof MushroomCow
                || target instanceof Zombie
                || target instanceof Skeleton
                || target instanceof LegacyQuackosEntity
                || target instanceof Ocelot;
    }
}
