package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class HbmDamageTypes {
    public static final ResourceKey<DamageType> RADIATION = key("radiation");
    public static final ResourceKey<DamageType> DIGAMMA = key("digamma");
    public static final ResourceKey<DamageType> RUBBLE = key("rubble");
    public static final ResourceKey<DamageType> LEAD = key("lead");
    public static final ResourceKey<DamageType> METEORITE = key("meteorite");
    public static final ResourceKey<DamageType> MONOXIDE = key("monoxide");
    public static final ResourceKey<DamageType> ASBESTOS = key("asbestos");
    public static final ResourceKey<DamageType> BLACK_LUNG = key("blacklung");
    public static final ResourceKey<DamageType> SHRAPNEL = key("shrapnel");
    public static final ResourceKey<DamageType> SEDNA_PHYSICAL = key("sedna_physical");
    public static final ResourceKey<DamageType> SEDNA_EXPLOSIVE = key("sedna_explosive");
    public static final ResourceKey<DamageType> SEDNA_FIRE = key("sedna_fire");
    public static final ResourceKey<DamageType> ELECTRICITY = key("electricity");
    public static final ResourceKey<DamageType> MICROWAVE = key("microwave");
    public static final ResourceKey<DamageType> DEATH = key("death");
    public static final ResourceKey<DamageType> NUCLEAR_BLAST = key("nuclear_blast");
    public static final ResourceKey<DamageType> ACID = key("acid");
    public static final ResourceKey<DamageType> MUD_POISONING = key("mud_poisoning");
    public static final ResourceKey<DamageType> CLOUD = key("cloud");
    public static final ResourceKey<DamageType> TAINT = key("taint");
    public static final ResourceKey<DamageType> TURBOFAN = key("turbofan");

    private HbmDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ReinhardtsHBM.id(name));
    }
}
