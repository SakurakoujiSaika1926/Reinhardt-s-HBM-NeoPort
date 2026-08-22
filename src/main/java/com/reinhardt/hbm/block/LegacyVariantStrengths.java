package com.reinhardt.hbm.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class LegacyVariantStrengths {
    public record Strength(float hardness, float resistance) {
    }

    public static final Strength[] BRICK_SLAB = {
            new Strength(15.0F, 100.0F),
            new Strength(15.0F, 300.0F),
            new Strength(15.0F, 120.0F),
            new Strength(5.0F, 20.0F),
            new Strength(15.0F, 400.0F),
            new Strength(5.0F, 1000.0F),
            new Strength(5.0F, 35.0F)
    };

    public static final Strength[] CONCRETE_BRICK_SLAB = {
            new Strength(15.0F, 160.0F),
            new Strength(15.0F, 160.0F),
            new Strength(15.0F, 60.0F),
            new Strength(15.0F, 45.0F),
            new Strength(15.0F, 750.0F)
    };

    private LegacyVariantStrengths() {
    }

    public static Strength strengthFor(BlockState state, Strength[] strengths) {
        return strengths[variant(state, strengths.length)];
    }

    public static float maxHardness(Strength[] strengths) {
        float result = 0.0F;
        for (Strength strength : strengths) {
            result = Math.max(result, strength.hardness());
        }
        return result;
    }

    public static float maxResistance(Strength[] strengths) {
        float result = 0.0F;
        for (Strength strength : strengths) {
            result = Math.max(result, strength.resistance());
        }
        return result;
    }

    private static int variant(BlockState state, int count) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty("variant");
        if (property != null && state.getValue(property) instanceof Integer value) {
            return Math.floorMod(value, count);
        }
        return 0;
    }
}
