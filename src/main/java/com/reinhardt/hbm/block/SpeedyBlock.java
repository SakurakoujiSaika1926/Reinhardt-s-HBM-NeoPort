package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Exact 1.7.10 BlockSpeedy movement behavior. */
public class SpeedyBlock extends Block {
    private final double speed;

    public SpeedyBlock(Properties properties, double speed) {
        super(properties);
        this.speed = speed;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.zza != 0.0F || player.xxa != 0.0F) {
            var motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * speed, motion.y, motion.z * speed);
        }
    }
}
