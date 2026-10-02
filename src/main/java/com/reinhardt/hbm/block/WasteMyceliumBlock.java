package com.reinhardt.hbm.block;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class WasteMyceliumBlock extends WasteEarthBlock {
    public WasteMyceliumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 1.7.10 did not let the mycelium block itself flood-fill nearby
        // terrain.  Spread is driven by HbmMushroomBlock and its config flag.
        super.randomTick(state, level, pos, random);
    }

    @Override
    public void stepOn(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide && entity instanceof LivingEntity living && !(living instanceof Player player && player.isCreative())) {
            HbmLivingRadiation data = HbmLivingRadiation.get(living);
            data.addRadiation(4.0F);
            data.addEnvironmentRadiation(4.0F);
            HbmLivingRadiation.set(living, data);
        }
    }
}
