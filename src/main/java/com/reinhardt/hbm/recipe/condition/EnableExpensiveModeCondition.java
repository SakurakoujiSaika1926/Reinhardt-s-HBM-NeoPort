package com.reinhardt.hbm.recipe.condition;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.config.HbmConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Loads the legacy {@code inputItemsEx} recipe branch. Expensive mode and
 * 528 mode are independent settings in HBM 1.7.10 and must not share a
 * datapack condition.
 */
public final class EnableExpensiveModeCondition implements ICondition {
    public static final EnableExpensiveModeCondition INSTANCE = new EnableExpensiveModeCondition();
    public static final MapCodec<EnableExpensiveModeCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private EnableExpensiveModeCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return HbmConfig.ENABLE_EXPENSIVE_MODE.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "enableExpensiveMode";
    }
}
