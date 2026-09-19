package com.reinhardt.hbm.block;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.radiation.RadiationShielding;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class CoriumBlock extends LiquidBlock {
    public CoriumBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid.get(), properties);
    }

    public static void afterFluidTick(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0
                && level.getBlockState(pos).getBlock() instanceof CoriumBlock block
                && !level.getBlockState(pos.below()).is(block)) {
            level.setBlock(pos, (random.nextInt(3) == 0
                    ? HbmBlocks.BLOCK_CORIUM.get()
                    : HbmBlocks.BLOCK_CORIUM_COBBLE.get()).defaultBlockState(), 3);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        burnAndIrradiate(level, pos, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        burnAndIrradiate(level, pos, entity);
    }

    private static void burnAndIrradiate(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        entity.igniteForSeconds(3.0F);
        DamageSources damageSources = level.damageSources();
        entity.hurt(damageSources.onFire(), 2.0F);
        if (entity instanceof LivingEntity living) {
            float dose = level instanceof ServerLevel serverLevel
                    ? RadiationShielding.attenuateDirectDose(serverLevel, pos, living, 1.0F)
                    : 1.0F;
            HbmLivingRadiation data = HbmLivingRadiation.get(living);
            data.addEnvironmentRadiation(dose);
            if (!(living instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                data.addRadiation(dose);
            }
            HbmLivingRadiation.set(living, data);
        }
    }
}
