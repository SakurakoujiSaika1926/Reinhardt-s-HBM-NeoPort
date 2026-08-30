package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COOLING_TOWER =
            PARTICLES.register("cooling_tower", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GAS_FLARE_SMOKE =
            PARTICLES.register("gas_flare_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GAS_FLARE_FLAME =
            PARTICLES.register("gas_flare_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GAS_FLARE_BURN_SMOKE =
            PARTICLES.register("gas_flare_burn_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHIMNEY_SMOKE_BRICK =
            PARTICLES.register("chimney_smoke_brick", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHIMNEY_SMOKE_INDUSTRIAL =
            PARTICLES.register("chimney_smoke_industrial", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARC_FURNACE_SMOKE =
            PARTICLES.register("arc_furnace_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOMIT =
            PARTICLES.register("vomit", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_VOMIT =
            PARTICLES.register("blood_vomit", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RBMK_MUSH =
            PARTICLES.register("rbmk_mush", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUKE_TOREX =
            PARTICLES.register("nuke_torex", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RBMK_FIRE =
            PARTICLES.register("rbmk_fire", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOLCANO_SMOKE =
            PARTICLES.register("volcano_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FALLOUT_RAIN =
            PARTICLES.register("fallout_rain", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RADIATION_FOG =
            PARTICLES.register("radiation_fog", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METEOR_TAIL =
            PARTICLES.register("meteor_tail", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KEROSENE_ROCKET_FLAME =
            PARTICLES.register("kerosene_rocket_flame", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TAU_SPARK =
            PARTICLES.register("tau_spark", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TAU_HADRON =
            PARTICLES.register("tau_hadron", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_FLASH =
            PARTICLES.register("muke_flash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_FLASH_BALEFIRE =
            PARTICLES.register("muke_flash_balefire", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_WAVE =
            PARTICLES.register("muke_wave", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_CLOUD =
            PARTICLES.register("muke_cloud", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUKE_CLOUD_BALEFIRE =
            PARTICLES.register("muke_cloud_balefire", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HAZE =
            PARTICLES.register("haze", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGACY_EXPLOSION_CLOUD =
            PARTICLES.register("legacy_explosion_cloud", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGACY_SMALL_EXPLOSION =
            PARTICLES.register("legacy_small_explosion", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGACY_MIST =
            PARTICLES.register("legacy_mist", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGACY_PLASMA_BLAST =
            PARTICLES.register("legacy_plasma_blast", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLAMETHROWER_FIRE =
            PARTICLES.register("flamethrower_fire", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLAMETHROWER_BALEFIRE =
            PARTICLES.register("flamethrower_balefire", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GIBLET_MEAT =
            PARTICLES.register("giblet_meat", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GIBLET_SLIME =
            PARTICLES.register("giblet_slime", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GIBLET_METAL =
            PARTICLES.register("giblet_metal", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGACY_CHLORINE_CLOUD =
            PARTICLES.register("legacy_chlorine_cloud", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGACY_CLOUD =
            PARTICLES.register("legacy_cloud", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGACY_PINK_CLOUD =
            PARTICLES.register("legacy_pink_cloud", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEGACY_ORANGE_CLOUD =
            PARTICLES.register("legacy_orange_cloud", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GEYSER_FIRE =
            PARTICLES.register("geyser_fire", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PART_EMITTER_TOWER_SMALL =
            PARTICLES.register("part_emitter_tower_small", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PART_EMITTER_TOWER_LARGE =
            PARTICLES.register("part_emitter_tower_large", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LANDMINE_SMOKE =
            PARTICLES.register("landmine_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LANDMINE_FOAM =
            PARTICLES.register("landmine_foam", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAIN_TOWER =
            PARTICLES.register("drain_tower", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PYRO_OVEN_TOWER =
            PARTICLES.register("pyro_oven_tower", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROTARY_FURNACE_TOWER =
            PARTICLES.register("rotary_furnace_tower", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAIN_SPLASH =
            PARTICLES.register("drain_splash", () -> new SimpleParticleType(false));

    private HbmParticleTypes() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
