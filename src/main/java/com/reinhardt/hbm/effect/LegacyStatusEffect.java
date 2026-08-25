package com.reinhardt.hbm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Status effects that existed in HBM 1.7.10 but do not need a per-tick action
 * of their own. Their gameplay hooks live at the relevant radiation and item
 * call sites, just as the old potion checks did.
 */
public class LegacyStatusEffect extends MobEffect {
    public LegacyStatusEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
