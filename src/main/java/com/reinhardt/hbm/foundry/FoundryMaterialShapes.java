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
    private static final Set<String> WIRE_MATERIALS = Set.of(
            "gold", "schrabidium", "copper", "tungsten", "aluminium", "lead",
            "zirconium", "steel", "red_copper", "carbon", "magnetized_tungsten"
    );
    private static final Set<String> DENSE_WIRE_MATERIALS = Set.of(
            "gold", "schrabidium", "schrabidate", "titanium", "copper", "tungsten",
            "red_copper", "steel", "neodymium", "niobium", "starmetal", "bscco",
            "magnetized_tungsten", "dineutronium"
    );
    private static final Set<String> CAST_PLATE_MATERIALS = Set.of(
            "iron", "gold", "schrabidium", "schrabidate", "titanium", "copper",
            "tungsten", "aluminium", "lead", "zirconium", "osmiridium", "steel",
            "dura_steel", "desh", "starmetal", "ferrouranium", "tcalloy", "cdalloy",
            "bismuth_bronze", "arsenic_bronze", "combine_steel", "weaponsteel", "saturnite"
    );
    private static final Set<String> WELDED_PLATE_MATERIALS = Set.of(
            "iron", "titanium", "copper", "tungsten", "aluminium", "zirconium", "steel",
            "tcalloy", "cdalloy", "combine_steel", "osmiridium"
    );
    private static final Set<String> SHELL_MATERIALS = Set.of(
            "titanium", "copper", "aluminium", "steel", "weaponsteel", "saturnite"
    );
    private static final Set<String> PIPE_MATERIALS = Set.of(
            "iron", "copper", "aluminium", "lead", "steel", "dura_steel", "rubber"
    );
    private static final Set<String> BOLT_MATERIALS = Set.of("tungsten", "lead", "steel", "dura_steel");

    private FoundryMaterialShapes() {
    }

    public static boolean supports(FoundryShape shape, FoundryMaterial material) {
        if (material == null) {
            return false;
        }
        return switch (shape) {
            case PLATE -> PLATE_MATERIALS.contains(material.name());
            case MECHANISM -> MECHANISM_MATERIALS.contains(material.name());
            case WIRE -> WIRE_MATERIALS.contains(material.name());
            case DENSE_WIRE -> DENSE_WIRE_MATERIALS.contains(material.name());
            case CAST_PLATE -> CAST_PLATE_MATERIALS.contains(material.name());
            case WELDED_PLATE -> WELDED_PLATE_MATERIALS.contains(material.name());
            case SHELL -> SHELL_MATERIALS.contains(material.name());
            case PIPE -> PIPE_MATERIALS.contains(material.name());
            case BOLT -> BOLT_MATERIALS.contains(material.name());
            default -> true;
        };
    }
}
