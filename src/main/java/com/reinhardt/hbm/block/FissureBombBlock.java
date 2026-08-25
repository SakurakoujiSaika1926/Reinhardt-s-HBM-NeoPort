package com.reinhardt.hbm.block;

import com.reinhardt.hbm.entity.TimedExplosiveEntity;

public final class FissureBombBlock extends TimedExplosiveBlock {
    public FissureBombBlock(Properties properties) {
        super(properties, TimedExplosiveEntity.Kind.FISSURE);
    }
}
