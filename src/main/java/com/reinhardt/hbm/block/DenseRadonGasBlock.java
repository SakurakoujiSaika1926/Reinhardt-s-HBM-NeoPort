package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DenseRadonGasBlock extends HbmGasBlock {
    public DenseRadonGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction firstDirection(Level level, BlockPos pos, RandomSource random) {
        return random.nextInt(5) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            Block wasteEarth = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("waste_earth"));
            if (wasteEarth != Blocks.AIR) {
                level.setBlock(pos.below(), wasteEarth.defaultBlockState(), 3);
            }
        }
        if (random.nextInt(30) == 0) {
            level.removeBlock(pos, false);
            Block fallout = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("fallout"));
            if (fallout != Blocks.AIR && fallout.defaultBlockState().canSurvive(level, pos)) {
                level.setBlock(pos, fallout.defaultBlockState(), 3);
            }
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
        if (HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.PARTICLE_FINE, 1)) {
            return;
        }
        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        data.addEnvironmentRadiation(0.5F);
        if (!(living instanceof Player player && player.isCreative())) {
            data.addRadiation(0.5F);
        }
        // ContaminationType.CREATIVE only suppressed accumulated radiation in
        // 1.7.10; the asbestos side effect was deliberately unconditional.
        HbmLivingHazards hazards = HbmLivingHazards.get(living);
        hazards.addAsbestos(living, 5);
        HbmLivingHazards.set(living, hazards);
        HbmLivingRadiation.set(living, data);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }
}
