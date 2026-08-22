package com.reinhardt.hbm.foundry;

import java.util.Set;

public final class FoundryMaterialShapes {
    private static final Set<String> PLATE_MATERIALS = Set.of(
            "iron",
            "gold",
            "schrabidium",
            "titanium",
            "copper",
            "advanced_alloy",
            "aluminium",
            "steel",
            "lead",
            "dura_steel",
            "combine_steel",
            "saturnite",
            "weaponsteel"
    );
    private static final Set<String> MECHANISM_MATERIALS = Set.of(
            "gunmetal",
            "weaponsteel",
            "saturnite"
    );

    private FoundryMaterialShapes() {
    }

    public static boolean supports(FoundryShape shape, FoundryMaterial material) {
        if (material == null) {
            return false;
        }
        return switch (shape) {
            case PLATE -> PLATE_MATERIALS.contains(material.name());
            case MECHANISM -> MECHANISM_MATERIALS.contains(material.name());
            default -> true;
        };
    }
}
