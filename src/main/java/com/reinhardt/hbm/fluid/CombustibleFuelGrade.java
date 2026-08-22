package com.reinhardt.hbm.fluid;

import java.util.Locale;

public enum CombustibleFuelGrade {
    LOW("trait.reinhardtshbm.combustible.low"),
    MEDIUM("trait.reinhardtshbm.combustible.medium"),
    HIGH("trait.reinhardtshbm.combustible.high"),
    AERO("trait.reinhardtshbm.combustible.aero"),
    GAS("trait.reinhardtshbm.combustible.gas");

    private final String translationKey;

    CombustibleFuelGrade(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public static CombustibleFuelGrade parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
