package com.reinhardt.hbm.item;

import java.util.List;
import java.util.Locale;

public final class BedrockOreFragments {
    private static final String INDEPENDENT_PREFIX = "bedrock_ore_fragment_";

    private static final List<MaterialRef> MATERIALS = List.of(
            material(2600, "iron"),
            material(2900, "copper"),
            material(2200, "titanium"),
            material(2902, "bauxite"),
            material(2903, "cryolite"),
            material(1701, "chlorocalcite"),
            material(300, "lithium"),
            material(1100, "sodium"),
            material(7400, "tungsten"),
            material(8200, "lead"),
            material(7900, "gold"),
            material(400, "beryllium"),
            material(8300, "bismuth"),
            material(7300, "tantalium"),
            material(2700, "cobalt"),
            material(20000, "rareearth"),
            material(500, "boron"),
            material(5700, "lanthanium"),
            material(4100, "niobium"),
            material(6000, "neodymium"),
            material(3800, "strontium"),
            material(4000, "zirconium"),
            material(9200, "uranium"),
            material(9032, "thorium"),
            material(8826, "radium"),
            material(8410, "polonium"),
            material(4399, "technetium"),
            material(9238, "u238"),
            material(600, "coal"),
            material(1600, "sulfur"),
            material(601, "lignite"),
            material(700, "kno"),
            material(900, "fluorite"),
            material(1500, "phosphorus"),
            material(1400, "silicon"),
            material(1, "redstone"),
            material(8001, "cinnabar"),
            material(1101, "sodalite"),
            material(1401, "asbestos"),
            material(1430, "diamond"),
            material(401, "emerald"),
            material(501, "borax"),
            material(1702, "molysite")
    );

    private BedrockOreFragments() {
    }

    public static List<MaterialRef> materials() {
        return MATERIALS;
    }

    public static String independentItemPath(MaterialRef material) {
        return independentItemPath(material.name());
    }

    public static String independentItemPath(String material) {
        return INDEPENDENT_PREFIX + material.toLowerCase(Locale.ROOT);
    }

    public static String materialNameFromIndependentPath(String path) {
        return path.startsWith(INDEPENDENT_PREFIX) ? path.substring(INDEPENDENT_PREFIX.length()) : "";
    }

    private static MaterialRef material(int id, String name) {
        return new MaterialRef(id, name);
    }

    public record MaterialRef(int id, String name) {
    }
}
