package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.util.RandomSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7.10 ItemCustomMissilePart data. The old system keeps the part contract on
 * the item itself; the assembly station must not infer compatibility from IDs.
 */
public final class MissilePartItem extends Item {
    public enum Type { CHIP, WARHEAD, FUSELAGE, FINS, THRUSTER }
    public enum Size { ANY, NONE, SIZE_10, SIZE_15, SIZE_20 }
    public enum Fuel { KEROSENE, SOLID, HYDROGEN, XENON, BALEFIRE }
    public enum Warhead { HE, INC, BUSTER, CLUSTER, NUCLEAR, TX, N2, BALEFIRE, SCHRAB, TAINT, CLOUD, TURBINE, MIRV, VOLCANO }

    public record Definition(Type type, Size top, Size bottom, Fuel fuel, Warhead warhead,
                             float primary, float secondary, float health) {
        public static Definition chip(float inaccuracy) {
            return new Definition(Type.CHIP, Size.ANY, Size.ANY, null, null, inaccuracy, 0.0F, 0.0F);
        }

        public static Definition warhead(Warhead type, float strength, float weight, Size size, float health) {
            return new Definition(Type.WARHEAD, Size.NONE, size, null, type, strength, weight, health);
        }

        public static Definition fuselage(Fuel fuel, float capacity, Size top, Size bottom, float health) {
            return new Definition(Type.FUSELAGE, top, bottom, fuel, null, capacity, 0.0F, health);
        }

        public static Definition fins(float inaccuracy, Size size, float health) {
            return new Definition(Type.FINS, size, size, null, null, inaccuracy, 0.0F, health);
        }

        public static Definition thruster(Fuel fuel, float consumption, float thrust, Size size, float health) {
            return new Definition(Type.THRUSTER, size, Size.NONE, fuel, null, consumption, thrust, health);
        }
    }

    private enum LegacyRarity {
        COMMON(ChatFormatting.GRAY),
        UNCOMMON(ChatFormatting.YELLOW),
        RARE(ChatFormatting.AQUA),
        EPIC(ChatFormatting.LIGHT_PURPLE),
        LEGENDARY(ChatFormatting.DARK_GREEN),
        STRANGE(ChatFormatting.DARK_AQUA);

        private final ChatFormatting color;

        LegacyRarity(ChatFormatting color) {
            this.color = color;
        }

        private static LegacyRarity fromLegacy(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return value.equals("SEWS_CLOTHES_AND_SUCKS_HORSE_COCK") ? STRANGE : valueOf(value);
        }
    }

    private record Cosmetic(String title, String author, String witty, LegacyRarity rarity, boolean hidden) {
        private static final Cosmetic EMPTY = new Cosmetic("", "", "", null, false);
    }

    private static final Map<String, Cosmetic> COSMETICS = loadCosmetics();

    private final String legacyId;
    private final Definition definition;
    private final Cosmetic cosmetic;

    public MissilePartItem(Properties properties, String legacyId) {
        super(properties.stacksTo(1));
        this.legacyId = legacyId;
        this.definition = definitionFor(legacyId);
        this.cosmetic = COSMETICS.getOrDefault(legacyId, Cosmetic.EMPTY);
    }

    public Definition definition() {
        return this.definition;
    }

    public static boolean isPartId(String id) {
        return id.startsWith("mp_c_") || id.startsWith("mp_warhead_") || id.startsWith("mp_fuselage_")
                || id.startsWith("mp_stability_") || id.startsWith("mp_thruster_") || id.equals("mp_s_20");
    }

    public static Definition definition(ItemStack stack) {
        return stack.getItem() instanceof MissilePartItem part ? part.definition : null;
    }

    public static String id(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    public boolean isHiddenInCreative() {
        return this.cosmetic.hidden();
    }

    /**
     * The old ItemLootCrate only accepted cosmetic parts that had called
     * setRarity(), splitting fuselages by their upper size and everything
     * else into the misc crate.
     */
    public boolean belongsToLegacyLootPool(LootPool pool) {
        if (this.cosmetic.rarity() == null) {
            return false;
        }
        return switch (pool) {
            case SIZE_10 -> this.definition.type() == Type.FUSELAGE && this.definition.top() == Size.SIZE_10;
            case SIZE_15 -> this.definition.type() == Type.FUSELAGE && this.definition.top() == Size.SIZE_15;
            case MISC -> this.definition.type() != Type.FUSELAGE;
        };
    }

    /** Matches ItemLootCrate.choose(): uncommon 1/5 through strange 1/100. */
    public boolean winsLegacyLootRoll(RandomSource random) {
        return switch (this.cosmetic.rarity()) {
            case COMMON -> true;
            case UNCOMMON -> random.nextInt(5) == 0;
            case RARE -> random.nextInt(10) == 0;
            case EPIC -> random.nextInt(25) == 0;
            case LEGENDARY -> random.nextInt(50) == 0;
            case STRANGE -> random.nextInt(100) == 0;
            case null -> false;
        };
    }

    public enum LootPool { SIZE_10, SIZE_15, MISC }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!this.cosmetic.title().isBlank()) {
            tooltip.add(Component.literal("\"" + this.cosmetic.title() + "\"").withStyle(ChatFormatting.DARK_PURPLE));
        }
        switch (this.definition.type()) {
            case CHIP -> tooltip.add(line("tooltip.reinhardtshbm.missile.inaccuracy", percent(this.definition.primary())));
            case WARHEAD -> {
                tooltip.add(line("tooltip.reinhardtshbm.missile.size", sizeName(this.definition.bottom())));
                tooltip.add(line("tooltip.reinhardtshbm.missile.warhead", Component.translatable("tooltip.reinhardtshbm.missile.warhead." + this.definition.warhead().name().toLowerCase())));
                tooltip.add(line("tooltip.reinhardtshbm.missile.strength", this.definition.primary()));
                tooltip.add(line("tooltip.reinhardtshbm.missile.weight", this.definition.secondary()));
            }
            case FUSELAGE -> {
                tooltip.add(line("tooltip.reinhardtshbm.missile.top_size", sizeName(this.definition.top())));
                tooltip.add(line("tooltip.reinhardtshbm.missile.bottom_size", sizeName(this.definition.bottom())));
                tooltip.add(line("tooltip.reinhardtshbm.missile.fuel", fuelName(this.definition.fuel())));
                tooltip.add(line("tooltip.reinhardtshbm.missile.fuel_amount", this.definition.primary()));
            }
            case FINS -> {
                tooltip.add(line("tooltip.reinhardtshbm.missile.size", sizeName(this.definition.top())));
                tooltip.add(line("tooltip.reinhardtshbm.missile.inaccuracy", percent(this.definition.primary())));
            }
            case THRUSTER -> {
                tooltip.add(line("tooltip.reinhardtshbm.missile.size", sizeName(this.definition.top())));
                tooltip.add(line("tooltip.reinhardtshbm.missile.fuel", fuelName(this.definition.fuel())));
                tooltip.add(line("tooltip.reinhardtshbm.missile.fuel_consumption", this.definition.primary()));
                tooltip.add(line("tooltip.reinhardtshbm.missile.max_payload", this.definition.secondary()));
            }
        }
        if (this.definition.type() != Type.CHIP) {
            tooltip.add(line("tooltip.reinhardtshbm.missile.health", this.definition.health()));
        }
        if (this.cosmetic.rarity() != null) {
            tooltip.add(line("tooltip.reinhardtshbm.missile.rarity",
                    Component.translatable("tooltip.reinhardtshbm.missile.rarity." + this.cosmetic.rarity().name().toLowerCase())
                            .withStyle(this.cosmetic.rarity().color)));
        }
        if (!this.cosmetic.author().isBlank()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.missile.by", this.cosmetic.author())
                    .withStyle(ChatFormatting.WHITE));
        }
        if (!this.cosmetic.witty().isBlank()) {
            tooltip.add(Component.literal("\"" + this.cosmetic.witty() + "\"")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    private static Component line(String key, Object value) {
        return Component.translatable(key, value).withStyle(ChatFormatting.GRAY);
    }

    private static Component sizeName(Size size) {
        return Component.translatable("tooltip.reinhardtshbm.missile.size." + size.name().toLowerCase());
    }

    private static Component fuelName(Fuel fuel) {
        return Component.translatable("tooltip.reinhardtshbm.missile.fuel." + fuel.name().toLowerCase());
    }

    private static String percent(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", value * 100.0F);
    }

    private static Map<String, Cosmetic> loadCosmetics() {
        InputStream stream = MissilePartItem.class.getClassLoader()
                .getResourceAsStream("legacy/reinhardtshbm/missile_part_metadata.tsv");
        if (stream == null) {
            throw new IllegalStateException("Missing generated legacy missile part metadata.");
        }

        Map<String, Cosmetic> cosmetics = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 6) {
                    throw new IllegalStateException("Malformed legacy missile part metadata: " + line);
                }
                cosmetics.put(fields[0], new Cosmetic(
                        fields[1], fields[2], fields[3], LegacyRarity.fromLegacy(fields[4]), Boolean.parseBoolean(fields[5])));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read legacy missile part metadata.", exception);
        }
        return Map.copyOf(cosmetics);
    }

    private static Definition definitionFor(String id) {
        return switch (id) {
            case "mp_c_1" -> Definition.chip(0.10F);
            case "mp_c_2" -> Definition.chip(0.05F);
            case "mp_c_3" -> Definition.chip(0.01F);
            case "mp_c_4" -> Definition.chip(0.005F);
            case "mp_c_5" -> Definition.chip(0.0F);

            case "mp_warhead_10_he" -> Definition.warhead(Warhead.HE, 15.0F, 1.5F, Size.SIZE_10, 5.0F);
            case "mp_warhead_10_incendiary" -> Definition.warhead(Warhead.INC, 15.0F, 1.5F, Size.SIZE_10, 5.0F);
            case "mp_warhead_10_buster" -> Definition.warhead(Warhead.BUSTER, 5.0F, 1.5F, Size.SIZE_10, 5.0F);
            case "mp_warhead_10_nuclear" -> Definition.warhead(Warhead.NUCLEAR, 35.0F, 1.5F, Size.SIZE_10, 10.0F);
            case "mp_warhead_10_nuclear_large" -> Definition.warhead(Warhead.NUCLEAR, 75.0F, 2.5F, Size.SIZE_10, 15.0F);
            case "mp_warhead_10_taint" -> Definition.warhead(Warhead.TAINT, 15.0F, 1.5F, Size.SIZE_10, 20.0F);
            case "mp_warhead_10_cloud" -> Definition.warhead(Warhead.CLOUD, 15.0F, 1.5F, Size.SIZE_10, 20.0F);
            case "mp_warhead_15_he" -> Definition.warhead(Warhead.HE, 50.0F, 2.5F, Size.SIZE_15, 10.0F);
            case "mp_warhead_15_incendiary" -> Definition.warhead(Warhead.INC, 35.0F, 2.5F, Size.SIZE_15, 10.0F);
            case "mp_warhead_15_nuclear", "mp_warhead_15_nuclear_shark", "mp_warhead_15_nuclear_mimi", "mp_warhead_15_nuclear_moon", "mp_warhead_15_nuclear_australium" -> Definition.warhead(Warhead.NUCLEAR, 125.0F, 5.0F, Size.SIZE_15, 15.0F);
            case "mp_warhead_15_boxcar", "mp_warhead_15_thermo" -> Definition.warhead(Warhead.TX, 250.0F, 7.5F, Size.SIZE_15, 35.0F);
            case "mp_warhead_15_mirv" -> Definition.warhead(Warhead.MIRV, 100.0F, 7.0F, Size.SIZE_15, 20.0F);
            case "mp_warhead_15_n2" -> Definition.warhead(Warhead.N2, 100.0F, 5.0F, Size.SIZE_15, 20.0F);
            case "mp_warhead_15_balefire" -> Definition.warhead(Warhead.BALEFIRE, 100.0F, 7.5F, Size.SIZE_15, 15.0F);
            case "mp_warhead_15_volcano" -> Definition.warhead(Warhead.VOLCANO, 10.0F, 6.5F, Size.SIZE_15, 25.0F);
            case "mp_warhead_15_turbine" -> Definition.warhead(Warhead.TURBINE, 200.0F, 5.0F, Size.SIZE_15, 250.0F);
            default -> exactLegacyDefinition(id);
        };
    }

    /**
     * Exact 1.7.10 ItemCustomMissilePart table. Each registered item is named
     * directly here so a cosmetic name can never silently alter flight data.
     */
    private static Definition exactLegacyDefinition(String id) {
        return switch (id) {
            case "mp_stability_10_flat" -> Definition.fins(0.5F, Size.SIZE_10, 10.0F);
            case "mp_stability_10_cruise" -> Definition.fins(0.25F, Size.SIZE_10, 5.0F);
            case "mp_stability_10_space" -> Definition.fins(0.35F, Size.SIZE_10, 5.0F);
            case "mp_stability_15_flat" -> Definition.fins(0.5F, Size.SIZE_15, 10.0F);
            case "mp_stability_15_thin" -> Definition.fins(0.35F, Size.SIZE_15, 5.0F);
            case "mp_stability_15_soyuz" -> Definition.fins(0.25F, Size.SIZE_15, 15.0F);
            case "mp_s_20" -> Definition.fins(0.5F, Size.SIZE_20, 0.0F);

            case "mp_thruster_10_kerosene" -> Definition.thruster(Fuel.KEROSENE, 1.0F, 1.5F, Size.SIZE_10, 10.0F);
            case "mp_thruster_10_solid" -> Definition.thruster(Fuel.SOLID, 1.0F, 1.5F, Size.SIZE_10, 15.0F);
            case "mp_thruster_10_xenon" -> Definition.thruster(Fuel.XENON, 1.0F, 1.5F, Size.SIZE_10, 5.0F);
            case "mp_thruster_15_kerosene" -> Definition.thruster(Fuel.KEROSENE, 1.0F, 7.5F, Size.SIZE_15, 15.0F);
            case "mp_thruster_15_kerosene_dual" -> Definition.thruster(Fuel.KEROSENE, 1.0F, 2.5F, Size.SIZE_15, 15.0F);
            case "mp_thruster_15_kerosene_triple" -> Definition.thruster(Fuel.KEROSENE, 1.0F, 5.0F, Size.SIZE_15, 15.0F);
            case "mp_thruster_15_solid" -> Definition.thruster(Fuel.SOLID, 1.0F, 5.0F, Size.SIZE_15, 20.0F);
            case "mp_thruster_15_solid_hexdecuple" -> Definition.thruster(Fuel.SOLID, 1.0F, 5.0F, Size.SIZE_15, 25.0F);
            case "mp_thruster_15_hydrogen" -> Definition.thruster(Fuel.HYDROGEN, 1.0F, 7.5F, Size.SIZE_15, 20.0F);
            case "mp_thruster_15_hydrogen_dual" -> Definition.thruster(Fuel.HYDROGEN, 1.0F, 2.5F, Size.SIZE_15, 15.0F);
            case "mp_thruster_15_balefire_short", "mp_thruster_15_balefire" -> Definition.thruster(Fuel.BALEFIRE, 1.0F, 5.0F, Size.SIZE_15, 25.0F);
            case "mp_thruster_15_balefire_large", "mp_thruster_15_balefire_large_rad" -> Definition.thruster(Fuel.BALEFIRE, 1.0F, 7.5F, Size.SIZE_15, 35.0F);
            case "mp_thruster_20_kerosene", "mp_thruster_20_kerosene_dual", "mp_thruster_20_kerosene_triple" -> Definition.thruster(Fuel.KEROSENE, 1.0F, 100.0F, Size.SIZE_20, 30.0F);
            case "mp_thruster_20_solid", "mp_thruster_20_solid_multi", "mp_thruster_20_solid_multier" -> Definition.thruster(Fuel.SOLID, 1.0F, 100.0F, Size.SIZE_20, 35.0F);

            case "mp_fuselage_10_kerosene", "mp_fuselage_10_kerosene_camo", "mp_fuselage_10_kerosene_desert", "mp_fuselage_10_kerosene_sky", "mp_fuselage_10_kerosene_flames", "mp_fuselage_10_kerosene_taint" -> Definition.fuselage(Fuel.KEROSENE, 2500.0F, Size.SIZE_10, Size.SIZE_10, 20.0F);
            case "mp_fuselage_10_kerosene_insulation" -> Definition.fuselage(Fuel.KEROSENE, 2500.0F, Size.SIZE_10, Size.SIZE_10, 25.0F);
            case "mp_fuselage_10_kerosene_sleek" -> Definition.fuselage(Fuel.KEROSENE, 2500.0F, Size.SIZE_10, Size.SIZE_10, 35.0F);
            case "mp_fuselage_10_kerosene_metal" -> Definition.fuselage(Fuel.KEROSENE, 2500.0F, Size.SIZE_10, Size.SIZE_10, 30.0F);
            case "mp_fuselage_10_solid", "mp_fuselage_10_solid_flames", "mp_fuselage_10_solid_cathedral", "mp_fuselage_10_solid_moonlit" -> Definition.fuselage(Fuel.SOLID, 2500.0F, Size.SIZE_10, Size.SIZE_10, 25.0F);
            case "mp_fuselage_10_solid_insulation", "mp_fuselage_10_solid_battery", "mp_fuselage_10_solid_duracell" -> Definition.fuselage(Fuel.SOLID, 2500.0F, Size.SIZE_10, Size.SIZE_10, 30.0F);
            case "mp_fuselage_10_solid_sleek", "mp_fuselage_10_solid_soviet_glory" -> Definition.fuselage(Fuel.SOLID, 2500.0F, Size.SIZE_10, Size.SIZE_10, 35.0F);
            case "mp_fuselage_10_xenon", "mp_fuselage_10_xenon_bhole" -> Definition.fuselage(Fuel.XENON, 5000.0F, Size.SIZE_10, Size.SIZE_10, 20.0F);
            case "mp_fuselage_10_long_kerosene", "mp_fuselage_10_long_kerosene_camo", "mp_fuselage_10_long_kerosene_desert", "mp_fuselage_10_long_kerosene_sky", "mp_fuselage_10_long_kerosene_flames", "mp_fuselage_10_long_kerosene_dash", "mp_fuselage_10_long_kerosene_taint", "mp_fuselage_10_long_kerosene_vap" -> Definition.fuselage(Fuel.KEROSENE, 5000.0F, Size.SIZE_10, Size.SIZE_10, 30.0F);
            case "mp_fuselage_10_long_kerosene_insulation", "mp_fuselage_10_long_kerosene_metal" -> Definition.fuselage(Fuel.KEROSENE, 5000.0F, Size.SIZE_10, Size.SIZE_10, 35.0F);
            case "mp_fuselage_10_long_kerosene_sleek" -> Definition.fuselage(Fuel.KEROSENE, 5000.0F, Size.SIZE_10, Size.SIZE_10, 40.0F);
            case "mp_fuselage_10_long_solid", "mp_fuselage_10_long_solid_flames", "mp_fuselage_10_long_solid_bullet", "mp_fuselage_10_long_solid_silvermoonlight" -> Definition.fuselage(Fuel.SOLID, 5000.0F, Size.SIZE_10, Size.SIZE_10, 35.0F);
            case "mp_fuselage_10_long_solid_insulation" -> Definition.fuselage(Fuel.SOLID, 5000.0F, Size.SIZE_10, Size.SIZE_10, 40.0F);
            case "mp_fuselage_10_long_solid_sleek", "mp_fuselage_10_long_solid_soviet_glory" -> Definition.fuselage(Fuel.SOLID, 5000.0F, Size.SIZE_10, Size.SIZE_10, 45.0F);
            case "mp_fuselage_10_15_kerosene" -> Definition.fuselage(Fuel.KEROSENE, 10000.0F, Size.SIZE_10, Size.SIZE_15, 40.0F);
            case "mp_fuselage_10_15_solid" -> Definition.fuselage(Fuel.SOLID, 10000.0F, Size.SIZE_10, Size.SIZE_15, 40.0F);
            case "mp_fuselage_10_15_hydrogen" -> Definition.fuselage(Fuel.HYDROGEN, 10000.0F, Size.SIZE_10, Size.SIZE_15, 40.0F);
            case "mp_fuselage_10_15_balefire" -> Definition.fuselage(Fuel.BALEFIRE, 10000.0F, Size.SIZE_10, Size.SIZE_15, 40.0F);
            case "mp_fuselage_15_kerosene", "mp_fuselage_15_kerosene_camo", "mp_fuselage_15_kerosene_desert", "mp_fuselage_15_kerosene_sky", "mp_fuselage_15_kerosene_minuteman", "mp_fuselage_15_kerosene_pip", "mp_fuselage_15_kerosene_taint" -> Definition.fuselage(Fuel.KEROSENE, 15000.0F, Size.SIZE_15, Size.SIZE_15, 50.0F);
            case "mp_fuselage_15_kerosene_insulation" -> Definition.fuselage(Fuel.KEROSENE, 15000.0F, Size.SIZE_15, Size.SIZE_15, 55.0F);
            case "mp_fuselage_15_kerosene_metal", "mp_fuselage_15_kerosene_decorated", "mp_fuselage_15_kerosene_steampunk", "mp_fuselage_15_kerosene_polite", "mp_fuselage_15_kerosene_yuck" -> Definition.fuselage(Fuel.KEROSENE, 15000.0F, Size.SIZE_15, Size.SIZE_15, 60.0F);
            case "mp_fuselage_15_kerosene_blackjack" -> Definition.fuselage(Fuel.KEROSENE, 15000.0F, Size.SIZE_15, Size.SIZE_15, 100.0F);
            case "mp_fuselage_15_kerosene_lambda" -> Definition.fuselage(Fuel.KEROSENE, 15000.0F, Size.SIZE_15, Size.SIZE_15, 75.0F);
            case "mp_fuselage_15_solid", "mp_fuselage_15_solid_silvermoonlight", "mp_fuselage_15_solid_snowy", "mp_fuselage_15_solid_panorama", "mp_fuselage_15_solid_roses", "mp_fuselage_15_solid_mimi" -> Definition.fuselage(Fuel.SOLID, 15000.0F, Size.SIZE_15, Size.SIZE_15, 60.0F);
            case "mp_fuselage_15_solid_insulation" -> Definition.fuselage(Fuel.SOLID, 15000.0F, Size.SIZE_15, Size.SIZE_15, 65.0F);
            case "mp_fuselage_15_solid_desh" -> Definition.fuselage(Fuel.SOLID, 15000.0F, Size.SIZE_15, Size.SIZE_15, 80.0F);
            case "mp_fuselage_15_solid_soviet_glory" -> Definition.fuselage(Fuel.SOLID, 15000.0F, Size.SIZE_15, Size.SIZE_15, 70.0F);
            case "mp_fuselage_15_solid_soviet_stank" -> Definition.fuselage(Fuel.SOLID, 15000.0F, Size.SIZE_15, Size.SIZE_15, 15.0F);
            case "mp_fuselage_15_solid_faust" -> Definition.fuselage(Fuel.SOLID, 15000.0F, Size.SIZE_15, Size.SIZE_15, 250.0F);
            case "mp_fuselage_15_hydrogen", "mp_fuselage_15_hydrogen_cathedral" -> Definition.fuselage(Fuel.HYDROGEN, 15000.0F, Size.SIZE_15, Size.SIZE_15, 50.0F);
            case "mp_fuselage_15_balefire" -> Definition.fuselage(Fuel.BALEFIRE, 15000.0F, Size.SIZE_15, Size.SIZE_15, 75.0F);
            case "mp_fuselage_15_20_kerosene", "mp_fuselage_15_20_kerosene_magnusson" -> Definition.fuselage(Fuel.KEROSENE, 20000.0F, Size.SIZE_15, Size.SIZE_20, 70.0F);
            case "mp_fuselage_15_20_solid" -> Definition.fuselage(Fuel.SOLID, 20000.0F, Size.SIZE_15, Size.SIZE_20, 70.0F);
            default -> throw new IllegalArgumentException("Unknown legacy missile part: " + id);
        };
    }
}
