package com.reinhardt.hbm.block;

import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RadiatingBlock extends Block {
    private static final double SOURCE_STEP = 0.1D;

    private final double radiation;

    public RadiatingBlock(Properties properties, double radiation) {
        super(properties);
        this.radiation = Math.max(0.0D, radiation);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && oldState.getBlock() != state.getBlock() && this.radiation > 0.0D) {
            emit(level, pos);
            level.scheduleTick(pos, this, nextTickDelay(level.random));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        emit(level, pos);
        level.scheduleTick(pos, this, nextTickDelay(random));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        emit(level, pos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        irradiateTouchingEntity(level, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        irradiateTouchingEntity(level, entity);
    }

    private void emit(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || this.radiation <= 0.0D) {
            return;
        }
        ChunkRadiationData.get(serverLevel).incrementRadiation(pos, this.radiation * SOURCE_STEP, this.radiation);
    }

    private void irradiateTouchingEntity(Level level, Entity entity) {
        if (level.isClientSide || this.radiation <= 0.0D || !(entity instanceof LivingEntity living)) {
            return;
        }
        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        float dose = (float) (this.radiation / 20.0D);
        data.addEnvironmentRadiation(dose);
        if (canReceiveDose(living)) {
            data.addRadiation(dose);
        }
        HbmLivingRadiation.set(living, data);
    }

    private static boolean canReceiveDose(LivingEntity living) {
        return !(living instanceof Player player && (player.isCreative() || player.isSpectator() || player.tickCount < 200));
    }

    private static int nextTickDelay(RandomSource random) {
        return 60 + random.nextInt(500);
    }
}
