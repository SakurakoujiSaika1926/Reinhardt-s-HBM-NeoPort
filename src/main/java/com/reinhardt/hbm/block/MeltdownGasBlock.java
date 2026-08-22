package com.reinhardt.hbm.block;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlocks;
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

public class MeltdownGasBlock extends HbmGasBlock {
    public MeltdownGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction firstDirection(Level level, BlockPos pos, RandomSource random) {
        return random.nextInt(2) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos target = pos.relative(direction);
        if (random.nextInt(7) == 0 && level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_RADON_DENSE.get().defaultBlockState(), 3);
        }
        if (level.canSeeSky(pos)) {
            ChunkRadiationData.get(level).incrementRadiation(pos, 5.0D, 5_000.0D);
        }
        if (random.nextInt(350) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        data.addEnvironmentRadiation(0.5F);
        if (!(living instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            data.addRadiation(0.5F);
            if (!HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.PARTICLE_FINE, 1)) {
                HbmLivingHazards hazards = HbmLivingHazards.get(living);
                hazards.addAsbestos(living, 5);
                HbmLivingHazards.set(living, hazards);
            }
        }
        HbmLivingRadiation.set(living, data);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.MYCELIUM,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.0D, 0.0D);
        }
    }

}
