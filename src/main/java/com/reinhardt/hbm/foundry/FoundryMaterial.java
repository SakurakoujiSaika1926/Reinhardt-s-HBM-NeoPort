package com.reinhardt.hbm.foundry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class FoundryMaterial {
    public enum SmeltingBehavior {
        NOT_SMELTABLE,
        SMELTABLE,
        ADDITIVE
    }

    private static final List<FoundryMaterial> ORDERED = new ArrayList<>();
    private static final Map<Integer, FoundryMaterial> BY_ID = new HashMap<>();
    private static final Map<String, FoundryMaterial> BY_NAME = new HashMap<>();
    private static boolean bootstrapped;

    private final int id;
    private final String name;
    private final String itemSuffix;
    private final int moltenColor;
    private final SmeltingBehavior behavior;
    private FoundryMaterial smeltsInto;
    private int conversionIn = 1;
    private int conversionOut = 1;

    private FoundryMaterial(int id, String name, String itemSuffix, int moltenColor, SmeltingBehavior behavior) {
        this.id = id;
        this.name = name;
        this.itemSuffix = itemSuffix;
        this.moltenColor = moltenColor;
        this.behavior = behavior;
        this.smeltsInto = this;
        ORDERED.add(this);
        BY_ID.put(id, this);
        BY_NAME.put(name, this);
        BY_NAME.put(itemSuffix, this);
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        FoundryMaterial stone = material(0, "stone", "stone", 0x4D2F23);
        additive(699, "carbon", "coal", 0x404040);
        convert(nonSmeltable(600, "coal", "coal", 0x404040), get("carbon"), 2, 1);
        convert(nonSmeltable(601, "lignite", "lignite", 0x472913), get("carbon"), 3, 1);

        material(2600, "iron", "iron", 0xFFA259);
        material(7900, "gold", "gold", 0xE8D754);
        material(1, "redstone", "redstone", 0xFF1000);
        material(2900, "copper", "copper", 0xC18336);
        material(2200, "titanium", "titanium", 0xA99E79);
        material(7400, "tungsten", "tungsten", 0x977474);
        material(8200, "lead", "lead", 0x646470);
        material(2700, "cobalt", "cobalt", 0x8F72AE);
        material(4100, "niobium", "niobium", 0x3BF1B6);
        material(8300, "bismuth", "bismuth", 0xB200FF);
        material(3300, "arsenic", "arsenic", 0x558080);
        material(3800, "strontium", "strontium", 0xCAC193);
        material(2000, "calcium", "calcium", 0xB7B784);
        material(4800, "cadmium", "cadmium", 0xA85600);
        material(4399, "technetium", "technetium", 0xCADFDF);
        material(9238, "u238", "u238", 0x9AA196);
        material(9000, "uranium", "uranium", 0x9AA196);
        material(9023, "thorium", "th232", 0x3B332A);
        material(9400, "plutonium", "plutonium", 0x969B8F);
        material(8400, "polonium", "polonium", 0x6B6F62);
        material(8800, "radium", "ra226", 0xA8B27E);
        material(12626, "schrabidium", "schrabidium", 0x32FFFF);
        material(1300, "aluminium", "aluminium", 0xD0B8EB);
        material(300, "lithium", "lithium", 0xB6BBC8);
        material(500, "boron", "boron", 0x777777);
        material(400, "beryllium", "beryllium", 0xAE9572);
        material(501, "borax", "borax", 0xFFECC6);
        material(1400, "silicon", "silicon", 0x878B9E);
        material(1401, "asbestos", "asbestos", 0xB0B3A8);
        material(5700, "lanthanium", "lanthanium", 0xA1B9B9);
        material(6000, "neodymium", "neodymium", 0x8F8F5F);
        material(7300, "tantalium", "tantalium", 0xA89B74);
        material(1100, "sodium", "sodium", 0x7E9493);
        material(4000, "zirconium", "zirconium", 0xADA688);
        material(7699, "osmiridium", "osmiridium", 0xACBDD9);

        additive(2601, "hematite", "hematite", 0x6E463D);
        additive(2901, "malachite", "malachite", 0x61AF87);
        additive(40, "flux", "flux", 0xDECCAD);

        material(30, "steel", "steel", 0x4A4A4A);
        material(31, "red_copper", "red_copper", 0xE44C0F);
        material(32, "advanced_alloy", "advanced_alloy", 0xFF7318);
        material(33, "dura_steel", "dura_steel", 0x42665C);
        material(42, "desh", "desh", 0xF22929);
        material(35, "starmetal", "starmetal", 0xA5A5D3);
        material(37, "ferrouranium", "ferrouranium", 0x6B6B8B);
        material(36, "tcalloy", "tcalloy", 0x9CA6A6);
        material(43, "cdalloy", "cdalloy", 0xFBD368);
        material(46, "bismuth_bronze", "bismuth_bronze", 0x987D65);
        material(47, "arsenic_bronze", "arsenic_bronze", 0x77644D);
        material(48, "bscco", "bscco", 0x5E62C0);
        material(38, "magnetized_tungsten", "magnetized_tungsten", 0x22A2A2);
        material(39, "combine_steel", "combine_steel", 0x6F6FB4);
        material(41, "slag", "slag", 0x6C6562);
        material(44, "mud", "mud", 0x96783B);
        material(34, "saturnite", "saturnite", 0x30A4B7);
        material(49, "gunmetal", "gunmetal", 0xF9C62C);
        material(50, "weaponsteel", "weaponsteel", 0x808080);
        material(12600, "schrabidate", "schrabidate", 0x6589B4);
        material(45, "dineutronium", "dineutronium", 0x455289);

        BY_NAME.put("alloy", get("advanced_alloy"));
        BY_NAME.put("mingrade", get("red_copper"));
        BY_NAME.put("dura", get("dura_steel"));
        BY_NAME.put("star", get("starmetal"));
        BY_NAME.put("ferro", get("ferrouranium"));
        BY_NAME.put("bbronze", get("bismuth_bronze"));
        BY_NAME.put("abronze", get("arsenic_bronze"));
        BY_NAME.put("magtung", get("magnetized_tungsten"));
        BY_NAME.put("cmb", get("combine_steel"));
        BY_NAME.put("bigmt", get("saturnite"));
        BY_NAME.put("sbd", get("schrabidate"));
        BY_NAME.put("dnt", get("dineutronium"));
        BY_NAME.put("cu", get("copper"));
        BY_NAME.put("ti", get("titanium"));
        BY_NAME.put("w", get("tungsten"));
        BY_NAME.put("pb", get("lead"));
        BY_NAME.put("u", get("uranium"));
        BY_NAME.put("pu", get("plutonium"));
        BY_NAME.put("na", get("sodium"));
        BY_NAME.put("sa326", get("schrabidium"));

        // Keep stone as a valid block mold override, but do not let arbitrary stone melt into castable metal.
        stone.smeltsInto = stone;
    }

    private static FoundryMaterial material(int id, String name, String itemSuffix, int moltenColor) {
        return new FoundryMaterial(id, name, itemSuffix, moltenColor, SmeltingBehavior.SMELTABLE);
    }

    private static FoundryMaterial additive(int id, String name, String itemSuffix, int moltenColor) {
        return new FoundryMaterial(id, name, itemSuffix, moltenColor, SmeltingBehavior.ADDITIVE);
    }

    private static FoundryMaterial nonSmeltable(int id, String name, String itemSuffix, int moltenColor) {
        return new FoundryMaterial(id, name, itemSuffix, moltenColor, SmeltingBehavior.NOT_SMELTABLE);
    }

    private static void convert(FoundryMaterial material, FoundryMaterial target, int in, int out) {
        material.smeltsInto = target;
        material.conversionIn = in;
        material.conversionOut = out;
    }

    public static FoundryMaterial get(String name) {
        bootstrap();
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static Optional<FoundryMaterial> byId(int id) {
        bootstrap();
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<FoundryMaterial> byName(String name) {
        bootstrap();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT)));
    }

    public static List<FoundryMaterial> ordered() {
        bootstrap();
        return ORDERED.stream()
                .sorted(Comparator.comparingInt(FoundryMaterial::id))
                .toList();
    }

    public static Optional<FoundryMaterialStack> materialFromItem(ItemStack stack) {
        return FoundryMaterialLookup.materialFromItem(stack);
    }

    public static List<FoundryMaterialStack> smeltingMaterialsFromItem(ItemStack stack) {
        return FoundryMaterialLookup.smeltingMaterialsFromItem(stack);
    }

    public int id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String itemSuffix() {
        return this.itemSuffix;
    }

    public int moltenColor() {
        return this.moltenColor;
    }

    public SmeltingBehavior behavior() {
        return this.behavior;
    }

    public FoundryMaterial smeltsInto() {
        return this.smeltsInto;
    }

    public int conversionIn() {
        return this.conversionIn;
    }

    public int conversionOut() {
        return this.conversionOut;
    }

    public String translationKey() {
        return "hbmmat." + this.name;
    }

    public ResourceLocation itemId(String prefix) {
        return ResourceLocation.fromNamespaceAndPath("reinhardtshbm", prefix + "_" + this.itemSuffix);
    }
}
