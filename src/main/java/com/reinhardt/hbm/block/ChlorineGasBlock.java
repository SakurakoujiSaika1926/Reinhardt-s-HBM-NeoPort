package com.reinhardt.hbm.block;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class ChlorineGasBlock extends HbmGasBlock {
    public ChlorineGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        if (random.nextInt(10) == 0) {
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
        if (HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.GAS_LUNG, 1)) {
            return;
        }
        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
        living.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
        living.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30 * 20, 2));
    }
}
