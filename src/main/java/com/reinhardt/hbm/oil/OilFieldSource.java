package com.reinhardt.hbm.oil;

public enum OilFieldSource {
    UNDETERMINED,
    HBM,
    HBM_BEDROCK,
    CREATE_DIESEL_GENERATORS;

    public static OilFieldSource byName(String name) {
        if (name == null || name.isBlank()) {
            return UNDETERMINED;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return UNDETERMINED;
        }
    }
}
