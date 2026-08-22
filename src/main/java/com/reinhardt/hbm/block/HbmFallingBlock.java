package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;

public class HbmFallingBlock extends FallingBlock {
    private static final MapCodec<HbmFallingBlock> CODEC = simpleCodec(HbmFallingBlock::new);

    public HbmFallingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
}
