package com.reinhardt.hbm.foundry;

import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.RawIngotItem;
import com.reinhardt.hbm.item.ScrapsItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

final class FoundryMaterialLookup {
    private FoundryMaterialLookup() {
    }

    static List<FoundryMaterialStack> smeltingMaterialsFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        List<FoundryMaterialStack> oreMaterials = oreSmeltingMaterials(stack);
        if (!oreMaterials.isEmpty()) {
            return oreMaterials;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null
                && id.getNamespace().equals("reinhardtshbm")
                && id.getPath().equals("chunk_ore_cryolite")) {
            return List.of(
                    new FoundryMaterialStack(FoundryMaterial.get("aluminium"), FoundryShape.INGOT.q(1)),
                    new FoundryMaterialStack(FoundryMaterial.get("sodium"), FoundryShape.INGOT.q(1))
            );
        }
        List<FoundryMaterialStack> rawMaterials = directRawMaterial(id);
        if (!rawMaterials.isEmpty()) {
            return rawMaterials;
        }

        Optional<FoundryMaterialStack> base = materialFromItem(stack);
        if (base.isEmpty()) {
            return List.of();
        }
        FoundryMaterialStack materialStack = base.get();
        FoundryMaterial target = materialStack.material().smeltsInto();
        if (target.behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE
                && target.behavior() != FoundryMaterial.SmeltingBehavior.ADDITIVE) {
            return List.of();
        }
        int amount = materialStack.amount() * materialStack.material().conversionOut()
                / Math.max(1, materialStack.material().conversionIn());
        return List.of(new FoundryMaterialStack(target, amount));
    }

    private static List<FoundryMaterialStack> oreSmeltingMaterials(ItemStack stack) {
        return stack.getTags()
                .map(TagKey::location)
                .filter(tag -> tag.getNamespace().equals("c") || tag.getNamespace().equals("forge"))
                .map(ResourceLocation::getPath)
                .filter(path -> path.startsWith("ores/") || path.startsWith("raw_materials/"))
                .map(path -> canonicalMaterialName(path.substring(path.indexOf('/') + 1)))
                .map(FoundryMaterialLookup::oreSmeltingMaterials)
                .filter(materials -> !materials.isEmpty())
                .findFirst()
                .orElseGet(List::of);
    }

    /** Exact custom ore distributions registered by HBM 1.7.10 MatDistribution. */
    private static List<FoundryMaterialStack> oreSmeltingMaterials(String ore) {
        return switch (ore) {
            case "iron" -> materials("iron", 144, "titanium", 24, "stone", 162);
            case "titanium" -> materials("titanium", 144, "iron", 24, "stone", 162);
            case "tungsten" -> materials("tungsten", 144, "stone", 162);
            case "aluminium" -> materials("aluminium", 144, "sodium", 24, "stone", 162);
            case "coal" -> materials("carbon", 216, "stone", 162);
            case "gold" -> materials("gold", 144, "lead", 24, "stone", 162);
            case "uranium" -> materials("uranium", 144, "lead", 24, "stone", 162);
            case "thorium" -> materials("thorium", 144, "uranium", 24, "stone", 162);
            case "copper" -> materials("copper", 144, "stone", 162);
            case "lead" -> materials("lead", 144, "gold", 8, "stone", 162);
            case "beryllium" -> materials("beryllium", 144, "stone", 162);
            case "cobalt" -> materials("cobalt", 72, "stone", 162);
            case "redstone" -> materials("redstone", 288, "stone", 162);
            case "hematite" -> materials("hematite", 72);
            case "malachite" -> materials("malachite", 432);
            default -> List.of();
        };
    }

    private static List<FoundryMaterialStack> directRawMaterial(ResourceLocation id) {
        if (id == null) {
            return List.of();
        }
        String material = switch (id.toString()) {
            case "minecraft:raw_iron" -> "iron";
            case "minecraft:raw_gold" -> "gold";
            case "minecraft:raw_copper" -> "copper";
            default -> id.getNamespace().equals("reinhardtshbm") && id.getPath().startsWith("raw_")
                    ? canonicalMaterialName(id.getPath().substring("raw_".length()))
                    : "";
        };
        return material.isBlank() ? List.of() : oreSmeltingMaterials(material);
    }

    private static List<FoundryMaterialStack> materials(Object... entries) {
        java.util.ArrayList<FoundryMaterialStack> materials = new java.util.ArrayList<>(entries.length / 2);
        for (int index = 0; index < entries.length; index += 2) {
            FoundryMaterial material = FoundryMaterial.get((String) entries[index]);
            if (material != null) {
                materials.add(new FoundryMaterialStack(material, (int) entries[index + 1]));
            }
        }
        return List.copyOf(materials);
    }

    static Optional<FoundryMaterialStack> materialFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof ScrapsItem) {
            return Optional.ofNullable(ScrapsItem.contents(stack));
        }
        if (stack.getItem() instanceof FoundryShapeItem shapeItem) {
            FoundryMaterial material = shapeItem.material(stack);
            if (material != null) {
                return Optional.of(new FoundryMaterialStack(material, shapeItem.shape().q(1)));
            }
        }
        if (stack.getItem() instanceof RawIngotItem rawIngot) {
            FoundryMaterial material = rawIngot.material(stack);
            if (material != null) {
                return Optional.of(new FoundryMaterialStack(material, FoundryShape.INGOT.q(1)));
            }
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && id.getNamespace().equals("reinhardtshbm")
                && id.getPath().equals("bedrock_ore_fragment")) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            FoundryMaterial material = FoundryMaterial.byId(tag.getInt("material_id"))
                    .or(() -> FoundryMaterial.byName(tag.getString("material")))
                    .orElse(null);
            return material == null
                    ? Optional.empty()
                    : Optional.of(new FoundryMaterialStack(material, FoundryShape.QUANTUM.q(8)));
        }
        if (id == null) {
            return Optional.empty();
        }
        String namespace = id.getNamespace();
        String path = id.getPath();

        Optional<FoundryMaterialStack> vanilla = vanillaMaterial(namespace, path);
        if (vanilla.isPresent()) {
            return vanilla;
        }

        Optional<FoundryMaterialStack> tagged = taggedMaterial(stack);
        if (tagged.isPresent()) {
            return tagged;
        }

        return hbmMaterial(path);
    }

    /**
     * HBM 1.7.10 used the ore dictionary for foundry inputs.  In modern
     * NeoForge the equivalent contract is a common item tag, not a foreign
     * item's registry path.  This keeps the foundry interoperable with both
     * current c: tags and older Forge-tagged add-ons.
     */
    private static Optional<FoundryMaterialStack> taggedMaterial(ItemStack stack) {
        return stack.getTags()
                .map(TagKey::location)
                .filter(tag -> tag.getNamespace().equals("c") || tag.getNamespace().equals("forge"))
                .map(FoundryMaterialLookup::stackFromMaterialTag)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<FoundryMaterialStack> stackFromMaterialTag(ResourceLocation tag) {
        String[] path = tag.getPath().split("/", 2);
        if (path.length != 2 || path[1].isBlank()) {
            return Optional.empty();
        }

        FoundryShape shape = switch (path[0]) {
            case "ingots" -> FoundryShape.INGOT;
            case "nuggets" -> FoundryShape.NUGGET;
            case "dusts", "powders" -> FoundryShape.DUST;
            case "plates" -> FoundryShape.PLATE;
            case "wires" -> FoundryShape.WIRE;
            case "dense_wires" -> FoundryShape.DENSE_WIRE;
            case "bolts" -> FoundryShape.BOLT;
            case "billets" -> FoundryShape.BILLET;
            case "pipes" -> FoundryShape.PIPE;
            case "storage_blocks" -> FoundryShape.BLOCK;
            case "raw_materials" -> FoundryShape.INGOT;
            default -> null;
        };
        if (shape == null) {
            return Optional.empty();
        }

        return FoundryMaterial.byName(canonicalMaterialName(path[1]))
                .filter(material -> FoundryMaterialShapes.supports(shape, material))
                .map(material -> new FoundryMaterialStack(material, shape.q(1)));
    }

    private static String canonicalMaterialName(String materialName) {
        return switch (materialName) {
            case "aluminum" -> "aluminium";
            // HBM 1.7.10 keeps graphite as a carbon conversion material. Its
            // ICF pellet press accepts a graphite ingot as the carbon fuel.
            case "graphite" -> "carbon";
            default -> materialName;
        };
    }

    private static Optional<FoundryMaterialStack> vanillaMaterial(String namespace, String path) {
        if (!namespace.equals("minecraft")) {
            return Optional.empty();
        }
        return switch (path) {
            case "iron_ingot" -> stack("iron", FoundryShape.INGOT);
            case "gold_ingot" -> stack("gold", FoundryShape.INGOT);
            case "iron_nugget" -> stack("iron", FoundryShape.NUGGET);
            case "gold_nugget" -> stack("gold", FoundryShape.NUGGET);
            case "redstone" -> stack("redstone", FoundryShape.NUGGET);
            case "coal", "charcoal" -> stack("coal", FoundryShape.INGOT);
            default -> Optional.empty();
        };
    }

    private static Optional<FoundryMaterialStack> hbmMaterial(String path) {
        if (path.equals("lithium")) {
            return stack("lithium", FoundryShape.INGOT);
        }
        if (path.equals("plate_cast_steel")) {
            return stack("steel", FoundryShape.CAST_PLATE);
        }
        if (path.equals("plate_cast_copper")) {
            return stack("copper", FoundryShape.CAST_PLATE);
        }
        if (path.equals("pipe")) {
            return stack("copper", FoundryShape.PIPE);
        }
        if (path.equals("pipes_steel")) {
            return stack("steel", FoundryShape.BLOCK, 3);
        }

        List<Prefix> prefixes = List.of(
                new Prefix("ingot_", FoundryShape.INGOT),
                new Prefix("nugget_", FoundryShape.NUGGET),
                new Prefix("powder_", FoundryShape.DUST),
                new Prefix("dust_", FoundryShape.DUST),
                new Prefix("plate_cast_", FoundryShape.CAST_PLATE),
                new Prefix("plate_welded_", FoundryShape.WELDED_PLATE),
                new Prefix("plate_", FoundryShape.PLATE),
                new Prefix("wire_dense_", FoundryShape.DENSE_WIRE),
                new Prefix("wire_", FoundryShape.WIRE),
                new Prefix("bolt_", FoundryShape.BOLT),
                new Prefix("billet_", FoundryShape.BILLET),
                new Prefix("pipe_", FoundryShape.PIPE),
                new Prefix("pipes_", FoundryShape.PIPE),
                new Prefix("block_", FoundryShape.BLOCK),
                new Prefix("raw_", FoundryShape.INGOT)
        );

        for (Prefix prefix : prefixes) {
            if (!path.startsWith(prefix.value())) {
                continue;
            }
            String suffix = path.substring(prefix.value().length());
            Optional<FoundryMaterialStack> found = stack(suffix, prefix.shape());
            if (found.isPresent() && FoundryMaterialShapes.supports(prefix.shape(), found.get().material())) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static Optional<FoundryMaterialStack> stack(String materialName, FoundryShape shape) {
        return stack(materialName, shape, 1);
    }

    private static Optional<FoundryMaterialStack> stack(String materialName, FoundryShape shape, int count) {
        return FoundryMaterial.byName(materialName)
                .map(material -> new FoundryMaterialStack(material, shape.q(count)));
    }

    private record Prefix(String value, FoundryShape shape) {
    }
}
