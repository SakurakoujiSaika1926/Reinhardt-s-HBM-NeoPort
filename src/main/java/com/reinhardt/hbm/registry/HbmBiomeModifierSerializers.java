package com.reinhardt.hbm.registry;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.worldgen.LegacyAllBiomeCreeperSpawnsModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Codecs for HBM's data-driven biome modifiers. */
public final class HbmBiomeModifierSerializers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ReinhardtsHBM.MOD_ID);

    static {
        SERIALIZERS.register("legacy_all_biome_creepers", () -> LegacyAllBiomeCreeperSpawnsModifier.CODEC);
    }

    private HbmBiomeModifierSerializers() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
