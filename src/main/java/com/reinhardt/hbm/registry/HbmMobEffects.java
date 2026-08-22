package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.effect.DeathEffect;
import com.reinhardt.hbm.effect.LeadPoisoningEffect;
import com.reinhardt.hbm.effect.PhosphorusBurnEffect;
import com.reinhardt.hbm.effect.TaintEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> LEAD_POISONING = MOB_EFFECTS.register(
            "lead_poisoning",
            LeadPoisoningEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> DEATH = MOB_EFFECTS.register(
            "death",
            DeathEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> PHOSPHORUS_BURN = MOB_EFFECTS.register(
            "phosphorus_burn",
            PhosphorusBurnEffect::new
    );
    public static final DeferredHolder<MobEffect, MobEffect> TAINT = MOB_EFFECTS.register(
            "taint",
            TaintEffect::new
    );
    private HbmMobEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
