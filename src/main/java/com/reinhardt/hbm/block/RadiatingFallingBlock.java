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
import net.minecraft.world.level.block.state.BlockState;

/** Falling-block equivalent of {@link RadiatingBlock}. */
public final class RadiatingFallingBlock extends HbmFallingBlock {
    private static final double SOURCE_STEP = 0.1D;
    private final double radiation;

    public RadiatingFallingBlock(Properties properties, double radiation) {
        super(properties);
        this.radiation = Math.max(0.0D, radiation);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        if (level.getBlockState(pos).is(this)) {
            emit(level, pos);
            level.scheduleTick(pos, this, 60 + random.nextInt(500));
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        emit(level, pos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        irradiate(level, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        irradiate(level, entity);
    }

    private void emit(ServerLevel level, BlockPos pos) {
        if (radiation > 0.0D) {
            ChunkRadiationData.get(level).incrementRadiation(pos, radiation * SOURCE_STEP, radiation);
        }
    }

    private void irradiate(Level level, Entity entity) {
        if (level.isClientSide || radiation <= 0.0D || !(entity instanceof LivingEntity living)) {
            return;
        }
        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        float dose = (float) (radiation / 20.0D);
        data.addEnvironmentRadiation(dose);
        if (!(living instanceof Player player && (player.isCreative() || player.isSpectator() || player.tickCount < 200))) {
            data.addRadiation(dose);
        }
        HbmLivingRadiation.set(living, data);
    }
}
