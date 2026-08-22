package com.reinhardt.hbm.registry;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.condition.Enable528ModeCondition;
import com.reinhardt.hbm.recipe.condition.EnableInfiniteWaterTankRecipesCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class HbmConditionSerializers {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<Enable528ModeCondition>> ENABLE_528_MODE =
            CONDITION_SERIALIZERS.register("enable_528_mode", () -> Enable528ModeCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<EnableInfiniteWaterTankRecipesCondition>> ENABLE_INFINITE_WATER_TANK_RECIPES =
            CONDITION_SERIALIZERS.register("enable_infinite_water_tank_recipes", () -> EnableInfiniteWaterTankRecipesCondition.CODEC);

    private HbmConditionSerializers() {
    }

    public static void register(IEventBus eventBus) {
        CONDITION_SERIALIZERS.register(eventBus);
    }
}
