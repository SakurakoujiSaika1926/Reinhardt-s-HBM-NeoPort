package com.reinhardt.hbm.item;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components used by standalone 1.7.10 item variants. */
public final class LegacyItemComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DRONE_TYPE = COMPONENTS.register(
            "drone_type", () -> DataComponentType.<Integer>builder().persistent(Codec.INT).build()
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> DRONE_LINK_ORIGIN = COMPONENTS.register(
            "drone_link_origin", () -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).build()
    );

    private LegacyItemComponents() {
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
