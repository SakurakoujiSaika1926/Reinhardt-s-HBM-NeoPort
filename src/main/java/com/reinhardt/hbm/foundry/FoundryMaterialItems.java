package com.reinhardt.hbm.foundry;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class FoundryMaterialItems {
    private static final Set<String> LEGACY_RAW_INGOT_MATERIALS = Set.of(
            "redstone",
            "neodymium",
            "borax",
            "sodium",
            "strontium",
            "slag"
    );

    private static final List<PathShape> PATH_SHAPES = List.of(
            new PathShape("part_mechanism_", FoundryShape.MECHANISM),
            new PathShape("plate_welded_", FoundryShape.WELDED_PLATE),
            new PathShape("wire_dense_", FoundryShape.DENSE_WIRE),
            new PathShape("plate_cast_", FoundryShape.CAST_PLATE),
            new PathShape("ingot_raw_", FoundryShape.INGOT),
            new PathShape("plate_", FoundryShape.PLATE),
            new PathShape("wire_", FoundryShape.WIRE),
            new PathShape("bolt_", FoundryShape.BOLT),
            new PathShape("pipe_", FoundryShape.PIPE),
            new PathShape("shell_", FoundryShape.SHELL)
    );

    private FoundryMaterialItems() {
    }

    public static boolean supportsRawIngot(FoundryMaterial material) {
        return material != null && LEGACY_RAW_INGOT_MATERIALS.contains(material.name());
    }

    public static boolean supportsShape(FoundryShape shape, FoundryMaterial material) {
        return material != null && FoundryMaterialShapes.supports(shape, material);
    }

    public static String rawIngotItemPath(FoundryMaterial material) {
        return "ingot_raw_" + material.itemSuffix();
    }

    public static String foundryItemPath(FoundryShape shape, FoundryMaterial material) {
        return foundryItemPrefix(shape) + "_" + foundryItemSuffix(shape, material);
    }

    public static Optional<FoundryMaterialStack> materialStackFromIndependentItemPath(String path) {
        for (PathShape entry : PATH_SHAPES) {
            if (!path.startsWith(entry.prefix())) {
                continue;
            }
            String materialName = path.substring(entry.prefix().length());
            Optional<FoundryMaterial> material = FoundryMaterial.byName(materialName);
            if (material.isEmpty()) {
                return Optional.empty();
            }
            FoundryMaterial foundryMaterial = material.get();
            if (entry.shape() == FoundryShape.INGOT && !supportsRawIngot(foundryMaterial)) {
                return Optional.empty();
            }
            if (entry.shape() != FoundryShape.INGOT && !supportsShape(entry.shape(), foundryMaterial)) {
                return Optional.empty();
            }
            return Optional.of(new FoundryMaterialStack(foundryMaterial, entry.shape().q(1)));
        }
        return Optional.empty();
    }

    private static String foundryItemPrefix(FoundryShape shape) {
        return switch (shape) {
            case PLATE -> "plate";
            case CAST_PLATE -> "plate_cast";
            case WELDED_PLATE -> "plate_welded";
            case WIRE -> "wire";
            case DENSE_WIRE -> "wire_dense";
            case MECHANISM -> "part_mechanism";
            default -> shape.key();
        };
    }

    private static String foundryItemSuffix(FoundryShape shape, FoundryMaterial material) {
        if (shape == FoundryShape.WIRE && material.name().equals("carbon")) {
            return "carbon";
        }
        return material.itemSuffix();
    }

    private record PathShape(String prefix, FoundryShape shape) {
    }
}
