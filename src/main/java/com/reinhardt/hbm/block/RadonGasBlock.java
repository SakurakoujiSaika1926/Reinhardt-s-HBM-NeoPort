package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RadonGasBlock extends HbmGasBlock {
    private final boolean tomb;

    public RadonGasBlock(Properties properties, boolean tomb) {
        super(properties);
        this.tomb = tomb;
    }

    @Override
    protected Direction firstDirection(Level level, BlockPos pos, RandomSource random) {
        return random.nextInt(tomb ? 3 : 5) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (tomb && random.nextInt(10) == 0) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(Blocks.GRASS_BLOCK)) {
                if (random.nextInt(5) == 0) {
                    level.setBlock(below, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                } else {
                    Block wasteEarth = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("waste_earth"));
                    if (wasteEarth != Blocks.AIR) {
                        level.setBlock(below, wasteEarth.defaultBlockState(), 3);
                    }
                }
            } else if (!belowState.isAir() && !belowState.isCollisionShapeFullBlock(level, below)
                    && (belowState.canBeReplaced() || belowState.is(Blocks.VINE))) {
                level.removeBlock(below, false);
            }
        }
        if (random.nextInt(tomb ? 600 : 50) == 0) {
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
        if (!tomb && HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.PARTICLE_FINE, 1)) {
            return;
        }
        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        float dose = tomb ? 0.5F : 0.05F;
        if (!(living instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            data.addRadiation(dose);
            data.addEnvironmentRadiation(dose);
            HbmLivingHazards hazards = HbmLivingHazards.get(living);
            hazards.addAsbestos(living, tomb ? 10 : 1);
            HbmLivingHazards.set(living, hazards);
        }
        HbmLivingRadiation.set(living, data);
    }
}
