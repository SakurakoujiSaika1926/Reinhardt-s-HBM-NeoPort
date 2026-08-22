package com.reinhardt.hbm.block;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MonoxideGasBlock extends HbmGasBlock {
    public MonoxideGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction firstDirection(Level level, BlockPos pos, RandomSource random) {
        return Direction.DOWN;
    }

    @Override
    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
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
        if (!HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.GAS_MONOXIDE, 1)) {
            living.hurt(level.damageSources().source(HbmDamageTypes.MONOXIDE), 1.0F);
        }
    }
}
