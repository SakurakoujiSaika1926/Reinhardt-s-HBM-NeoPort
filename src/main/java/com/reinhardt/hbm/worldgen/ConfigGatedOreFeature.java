package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import java.util.function.BooleanSupplier;

/** Vanilla ore placement with a legacy config gate. */
public final class ConfigGatedOreFeature extends OreFeature {
    private final BooleanSupplier enabled;

    public ConfigGatedOreFeature(Codec<OreConfiguration> codec, BooleanSupplier enabled) {
        super(codec);
        this.enabled = enabled;
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        return this.enabled.getAsBoolean() && super.place(context);
    }
}
