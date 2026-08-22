package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WasteMyceliumBlock extends WasteEarthBlock {
    public WasteMyceliumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    BlockPos above = target.above();
                    BlockState targetState = level.getBlockState(target);
                    if (!level.getBlockState(above).isCollisionShapeFullBlock(level, above)
                            && (targetState.is(Blocks.DIRT)
                            || targetState.is(Blocks.GRASS_BLOCK)
                            || targetState.is(Blocks.MYCELIUM)
                            || targetState.is(BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("waste_earth"))))) {
                        level.setBlock(target, state, 3);
                    }
                }
            }
        }
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
