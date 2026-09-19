package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Exact 1.7.10 DemonLamp radiation falloff and resistance ray sampling. */
public final class DemonLampBlockEntity extends BlockEntity {
    private static final float RADIATION_PER_TICK = 100_000.0F;
    private static final double RANGE = 25.0D;

    public DemonLampBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DEMON_LAMP.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DemonLampBlockEntity lamp) {
        if (!level.isClientSide) {
            lamp.radiate(level);
        }
    }

    private void radiate(Level level) {
        Vec3 origin = Vec3.atCenterOf(worldPosition);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(RANGE))) {
            Vec3 target = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
            Vec3 delta = target.subtract(origin);
            double distance = delta.length();
            if (distance <= 0.0D || distance > RANGE) {
                continue;
            }

            Vec3 unit = delta.scale(1.0D / distance);
            float resistance = 0.0F;
            for (int step = 1; step < distance; step++) {
                BlockPos sample = BlockPos.containing(origin.add(unit.scale(step)));
                resistance += level.getBlockState(sample).getBlock().getExplosionResistance();
            }
            resistance = Math.max(1.0F, resistance);

            HbmLivingRadiation radiation = HbmLivingRadiation.get(entity);
            radiation.addRadiationWithReadout((float) (RADIATION_PER_TICK / resistance / (distance * distance)));
            HbmLivingRadiation.set(entity, radiation);
            if (distance < 2.0D) {
                entity.hurt(entity.damageSources().inFire(), 100.0F);
            }
        }
    }
}
