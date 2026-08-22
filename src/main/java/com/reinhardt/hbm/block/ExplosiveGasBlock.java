package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ExplosiveGasBlock extends FlammableGasBlock {
    public ExplosiveGasBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void combust(Level level, BlockPos pos) {
        super.combust(level, pos);
        level.explode(null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                3.0F,
                true,
                Level.ExplosionInteraction.NONE);
    }
}
