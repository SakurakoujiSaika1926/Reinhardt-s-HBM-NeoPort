package com.reinhardt.hbm.recipe.condition;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.config.HbmConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class Enable528ModeCondition implements ICondition {
    public static final Enable528ModeCondition INSTANCE = new Enable528ModeCondition();
    public static final MapCodec<Enable528ModeCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private Enable528ModeCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return HbmConfig.ENABLE_528_MODE.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "enable528Mode";
    }
}
