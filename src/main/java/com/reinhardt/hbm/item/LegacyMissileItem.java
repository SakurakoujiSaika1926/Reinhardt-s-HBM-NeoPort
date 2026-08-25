package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

/**
 * ItemMissile's immutable launch metadata. Launcher code can consume this
 * directly instead of inferring a missile's tier or fuel from its registry id.
 */
public class LegacyMissileItem extends Item {
    private static final Set<String> LEGACY_IDS = Set.of(
            "missile_anti_ballistic", "missile_bhole", "missile_burst", "missile_buster",
            "missile_buster_strong", "missile_cluster", "missile_cluster_strong", "missile_decoy",
            "missile_doomsday", "missile_doomsday_rusted", "missile_drill", "missile_emp",
            "missile_emp_strong", "missile_generic", "missile_incendiary", "missile_incendiary_strong",
            "missile_inferno", "missile_micro", "missile_nuclear", "missile_nuclear_cluster",
            "missile_rain", "missile_schrabidium", "missile_shuttle", "missile_stealth",
            "missile_strong", "missile_taint", "missile_test", "missile_volcano"
    );
    public enum FormFactor {
        ABM(Fuel.SOLID),
        MICRO(Fuel.SOLID),
        V2(Fuel.ETHANOL_PEROXIDE),
        STRONG(Fuel.KEROSENE_PEROXIDE),
        HUGE(Fuel.KEROSENE_LOXY),
        ATLAS(Fuel.JETFUEL_LOXY),
        OTHER(Fuel.KEROSENE_PEROXIDE);

        private final Fuel defaultFuel;

        FormFactor(Fuel defaultFuel) {
            this.defaultFuel = defaultFuel;
        }
    }

    public enum Tier {
        TIER0,
        TIER1,
        TIER2,
        TIER3,
        TIER4
    }

    public enum Fuel {
        SOLID("solid", 0),
        ETHANOL_PEROXIDE("ethanol_peroxide", 4_000),
        KEROSENE_PEROXIDE("kerosene_peroxide", 8_000),
        KEROSENE_LOXY("kerosene_loxy", 12_000),
        JETFUEL_LOXY("jetfuel_loxy", 16_000);

        private final String translationId;
        private final int defaultCapacity;

        Fuel(String translationId, int defaultCapacity) {
            this.translationId = translationId;
            this.defaultCapacity = defaultCapacity;
        }
    }

    private final FormFactor formFactor;
    private final Tier tier;
    private final Fuel fuel;
    private final int fuelCapacity;
    private final boolean launchable;

    public LegacyMissileItem(Properties properties, FormFactor formFactor, Tier tier) {
        this(properties, formFactor, tier, formFactor.defaultFuel, formFactor.defaultFuel.defaultCapacity, true);
    }

    public LegacyMissileItem(Properties properties, FormFactor formFactor, Tier tier, Fuel fuel, int fuelCapacity, boolean launchable) {
        super(properties.stacksTo(1));
        this.formFactor = formFactor;
        this.tier = tier;
        this.fuel = fuel;
        this.fuelCapacity = fuelCapacity;
        this.launchable = launchable;
    }

    public FormFactor formFactor() {
        return formFactor;
    }

    public Tier tier() {
        return tier;
    }

    public Fuel fuel() {
        return fuel;
    }

    public int fuelCapacity() {
        return fuelCapacity;
    }

    public boolean launchable() {
        return launchable;
    }

    public static boolean isLegacyMissileId(String id) {
        return LEGACY_IDS.contains(id);
    }

    public static LegacyMissileItem fromLegacyId(String id) {
        return switch (id) {
            case "missile_generic", "missile_incendiary", "missile_cluster", "missile_buster", "missile_decoy" ->
                    standard(FormFactor.V2, Tier.TIER1);
            case "missile_anti_ballistic" -> standard(FormFactor.ABM, Tier.TIER1);
            case "missile_strong", "missile_incendiary_strong", "missile_cluster_strong", "missile_buster_strong", "missile_emp_strong" ->
                    standard(FormFactor.STRONG, Tier.TIER2);
            case "missile_stealth" -> standard(FormFactor.STRONG, Tier.TIER1);
            case "missile_burst", "missile_inferno", "missile_rain", "missile_drill" ->
                    standard(FormFactor.HUGE, Tier.TIER3);
            case "missile_nuclear", "missile_nuclear_cluster", "missile_volcano", "missile_doomsday" ->
                    standard(FormFactor.ATLAS, Tier.TIER4);
            case "missile_doomsday_rusted" -> new LegacyMissileItem(
                    new Properties(), FormFactor.ATLAS, Tier.TIER4, Fuel.JETFUEL_LOXY, 16_000, false);
            case "missile_taint", "missile_micro", "missile_bhole", "missile_schrabidium", "missile_emp", "missile_test" ->
                    standard(FormFactor.MICRO, Tier.TIER0);
            case "missile_shuttle" -> standard(FormFactor.OTHER, Tier.TIER3);
            default -> throw new IllegalArgumentException("Unknown 1.7.10 missile id: " + id);
        };
    }

    private static LegacyMissileItem standard(FormFactor formFactor, Tier tier) {
        return new LegacyMissileItem(new Properties(), formFactor, tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.legacy_missile.tier." + tier.name().toLowerCase()).withStyle(ChatFormatting.ITALIC));
        if (!launchable) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.legacy_missile.not_launchable").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.legacy_missile.fuel",
                Component.translatable("tooltip.reinhardtshbm.legacy_missile.fuel." + fuel.translationId)));
        if (fuelCapacity > 0) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.legacy_missile.capacity", fuelCapacity));
        }
    }
}
