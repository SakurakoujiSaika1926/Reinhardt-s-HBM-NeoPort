package com.reinhardt.hbm.recipe.condition;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.config.HbmConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class EnableInfiniteWaterTankRecipesCondition implements ICondition {
    public static final EnableInfiniteWaterTankRecipesCondition INSTANCE = new EnableInfiniteWaterTankRecipesCondition();
    public static final MapCodec<EnableInfiniteWaterTankRecipesCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private EnableInfiniteWaterTankRecipesCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return HbmConfig.ENABLE_INFINITE_WATER_TANK_RECIPES.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "enableInfiniteWaterTankRecipes";
    }
}
