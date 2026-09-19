package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class DeconBlockEntity extends BlockEntity {
    private static final AABB EFFECT_AREA = new AABB(-0.5D, 0.0D, -0.5D, 1.5D, 2.0D, 1.5D);

    public DeconBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DECON.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DeconBlockEntity decon) {
        if (level.isClientSide) {
            return;
        }

        AABB area = EFFECT_AREA.move(pos.getX(), pos.getY(), pos.getZ());
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
            HbmLivingRadiation radiation = HbmLivingRadiation.get(living);
            radiation.addRadiation(-0.5F);
            HbmLivingRadiation.set(living, radiation);
            HbmLivingHazards.clear(living);
        }
    }

    public static void clientTick(Level level, BlockPos pos) {
        float color = 0.5F + level.random.nextFloat() * 0.5F;
        int red = (int) (0.8F * color * 255.0F);
        int green = (int) (0.9F * color * 255.0F);
        int blue = (int) (color * 255.0F);
        int argb = 0xFF000000 | (red << 16) | (green << 8) | blue;
        level.addParticle(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, argb),
                pos.getX() + 0.125D + level.random.nextDouble() * 0.75D,
                pos.getY() + 1.1D,
                pos.getZ() + 0.125D + level.random.nextDouble() * 0.75D,
                0.0D, 0.04D, 0.0D
        );
    }
}
