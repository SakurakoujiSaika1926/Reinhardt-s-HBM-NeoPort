package com.reinhardt.hbm.block;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AsbestosGasBlock extends HbmGasBlock {
    public AsbestosGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction firstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            return Direction.DOWN;
        }
        Direction[] directions = Direction.values();
        return directions[random.nextInt(directions.length)];
    }

    @Override
    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(50) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)
                || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (!HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.PARTICLE_FINE, 1)) {
            HbmLivingHazards hazards = HbmLivingHazards.get(living);
            hazards.addAsbestos(living, 1);
            HbmLivingHazards.set(living, hazards);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.MYCELIUM,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.0D, 0.0D);
        }
    }
}
