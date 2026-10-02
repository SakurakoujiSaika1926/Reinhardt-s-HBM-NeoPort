package com.reinhardt.hbm.recipe.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.config.HbmConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.Locale;

/**
 * Datapack condition for the independent 1.7.10 Less Bullshit Mode switches.
 * The central switch is always forced off while 528 mode is active, matching
 * {@code GeneralConfig.loadFromConfig()} in the latest 1.7.10 source.
 */
public record LessBullshitModeCondition(Feature feature) implements ICondition {
    public static final MapCodec<LessBullshitModeCondition> CODEC = Codec.STRING
            .xmap(Feature::fromSerializedName, Feature::serializedName)
            .fieldOf("feature")
            .xmap(LessBullshitModeCondition::new, LessBullshitModeCondition::feature)
            .stable();

    @Override
    public boolean test(IContext context) {
        return switch (this.feature) {
            case ENABLED -> HbmConfig.lessBullshitMode();
            case FULL_SCHRAB -> HbmConfig.lbsmFullSchrab();
            case SHORTER_DECAY -> HbmConfig.lbsmShorterDecay();
            case SIMPLE_ARMOR -> HbmConfig.lbsmSimpleArmorRecipes();
            case SIMPLE_TOOLS -> HbmConfig.lbsmSimpleToolRecipes();
            case SIMPLE_ALLOY -> HbmConfig.lbsmSimpleAlloy();
            case SIMPLE_CHEMISTRY -> HbmConfig.lbsmSimpleChemistry();
            case SIMPLE_CENTRIFUGE -> HbmConfig.lbsmSimpleCentrifuge();
            case UNLOCK_ANVIL -> HbmConfig.lbsmUnlockAnvil();
            case SIMPLE_CRAFTING -> HbmConfig.lbsmSimpleCrafting();
            case SIMPLE_MEDICINE -> HbmConfig.lbsmSimpleMedicineRecipes();
            case SAFE_CRATES -> HbmConfig.lessBullshitMode() && HbmConfig.LBSM_SAFE_CRATES.get();
            case SAFE_ME_DRIVES -> HbmConfig.lessBullshitMode() && HbmConfig.LBSM_SAFE_ME_DRIVES.get();
        };
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "lessBullshitMode[" + this.feature.serializedName() + ']';
    }

    public enum Feature {
        ENABLED,
        FULL_SCHRAB,
        SHORTER_DECAY,
        SIMPLE_ARMOR,
        SIMPLE_TOOLS,
        SIMPLE_ALLOY,
        SIMPLE_CHEMISTRY,
        SIMPLE_CENTRIFUGE,
        UNLOCK_ANVIL,
        SIMPLE_CRAFTING,
        SIMPLE_MEDICINE,
        SAFE_CRATES,
        SAFE_ME_DRIVES;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static Feature fromSerializedName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown Less Bullshit Mode feature: " + name, exception);
            }
        }
    }
}
