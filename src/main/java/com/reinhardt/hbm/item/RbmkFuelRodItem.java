package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;

public class RbmkFuelRodItem extends LegacyVariantItem {
    private static final String DEPLETION_TAG = "depletion";
    private static final String XENON_TAG = "xenon";
    private static final String CORE_HEAT_TAG = "coreHeat";
    private static final String HULL_HEAT_TAG = "hullHeat";
    private final String fixedFuelId;

    public static final List<String> FUEL_IDS = List.of(
            "ueu",
            "meu",
            "heu233",
            "heu235",
            "uzh",
            "thmeu",
            "lep",
            "mep",
            "hep",
            "hep241",
            "lea",
            "mea",
            "hea241",
            "hea242",
            "men",
            "hen",
            "mox",
            "les",
            "mes",
            "hes",
            "leaus",
            "heaus",
            "po210be",
            "ra226be",
            "pu238be",
            "balefire_gold",
            "flashlead",
            "balefire",
            "zfb_bismuth",
            "zfb_pu241",
            "zfb_am_mix",
            "drx"
    );

    public RbmkFuelRodItem(Properties properties) {
        super(properties.stacksTo(1), "rbmk_fuel", variants(FUEL_IDS.toArray(String[]::new)));
        this.fixedFuelId = null;
    }

    /**
     * Direct 1.7.10 registration form.  The old game registered every fuel
     * rod as a separate item, so its fuel type must not depend on mutable
     * variant data attached to the stack.
     */
    public RbmkFuelRodItem(Properties properties, String fixedFuelId) {
        super(properties.stacksTo(1), "rbmk_fuel", variants(fixedFuelId));
        this.fixedFuelId = fixedFuelId;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (this.fixedFuelId != null) {
            return Component.translatable("item.reinhardtshbm.rbmk_fuel_" + this.fixedFuelId);
        }
        return super.getName(stack);
    }

    public Fuel fuel(ItemStack stack) {
        String id = this.fixedFuelId == null ? variant(stack).id() : this.fixedFuelId;
        for (Fuel fuel : FUELS) {
            if (fuel.id().equals(id)) {
                return fuel;
            }
        }
        return FUELS.getFirst();
    }

    public double meltingPoint(ItemStack stack) {
        return fuel(stack).meltingPoint();
    }

    public boolean isDigammaFuel(ItemStack stack) {
        return fuel(stack).id().equals("drx");
    }

    public int channelColor(ItemStack stack) {
        return switch (fuel(stack).id()) {
            case "ueu", "meu", "heu233", "heu235", "mox" -> 0x868D82;
            case "men", "hen" -> 0x757E73;
            case "lep", "mep", "hep", "hep241", "pu238be" -> 0x656E6B;
            case "lea", "mea", "hea241", "hea242" -> 0xA88A8F;
            case "thmeu" -> 0x665448;
            case "zfb_bismuth", "zfb_pu241", "zfb_am_mix" -> 0xAAA36A;
            case "les", "mes", "hes" -> 0x2D9A94;
            case "uzh" -> 0x7077AF;
            case "po210be" -> 0x563A26;
            case "ra226be" -> 0xB3B6AD;
            case "leaus", "heaus" -> 0xFFEE00;
            case "balefire_gold" -> 0xDC9613;
            case "flashlead" -> 0x7B7B87;
            case "balefire" -> 0xB2FF1B;
            case "drx" -> 0xD77276;
            default -> 0x304825;
        };
    }

    public float nextDepletion(ItemStack stack, double flux) {
        Fuel fuel = fuel(stack);
        double inFlux = Math.max(0.0D, flux) + fuel.selfRate();
        return (float) Math.max(0.0D, Math.min(1.0D, depletion(stack) + inFlux / fuel.yield()));
    }

    public double remainingReactivity(ItemStack stack) {
        return Math.max(0.0D, 1.0D - depletion(stack));
    }

    public double inputFlux(ItemStack stack, com.reinhardt.hbm.blockentity.NeutronFluxProvider.NeutronFlux flux) {
        return flux.effectiveFor(fuel(stack).neutronIn());
    }

    public double outputFastRatio(ItemStack stack) {
        return switch (fuel(stack).neutronOut()) {
            case SLOW -> 0.0D;
            case FAST -> 1.0D;
            case ANY -> 0.5D;
        };
    }

    public FuelTickResult burnAndProvideHeat(
            ItemStack stack,
            double inFlux,
            double channelHeat,
            double reactivityMod,
            double diffusionMod,
            double heatProvision,
            boolean depletionEnabled,
            boolean xenonEnabled
    ) {
        Fuel fuel = fuel(stack);
        double workingFlux = Math.max(0.0D, inFlux) + fuel.selfRate();
        double xenonLevel = xenon(stack);

        if (xenonEnabled) {
            xenonLevel -= xenonBurn(fuel, workingFlux) / 100.0D;
            workingFlux *= 1.0D - Math.max(0.0D, Math.min(1.0D, xenonLevel));
            xenonLevel += xenonGen(fuel, workingFlux) / 100.0D;
            xenonLevel = Math.max(0.0D, Math.min(1.0D, xenonLevel));
        }

        double outputFlux = reactivity(fuel, workingFlux, remainingReactivity(stack)) * Math.max(0.0D, reactivityMod);
        double depletion = depletion(stack);
        if (depletionEnabled) {
            depletion = Math.max(0.0D, Math.min(1.0D, depletion + workingFlux / fuel.yield()));
        }
        double coreHeat = rectify(coreHeat(stack) + outputFlux * fuel.heat());
        double hullHeat = rectify(hullHeat(stack));

        if (coreHeat > hullHeat) {
            double mid = (coreHeat - hullHeat) / 2.0D;
            double diffusion = fuel.diffusion() * Math.max(0.0D, diffusionMod);
            coreHeat = rectify(coreHeat - mid * diffusion);
            hullHeat = rectify(hullHeat + mid * diffusion);
        }

        double providedHeat;
        if (hullHeat > fuel.meltingPoint()) {
            double average = (channelHeat + hullHeat + coreHeat) / 3.0D;
            coreHeat = average;
            hullHeat = average;
            providedHeat = average - channelHeat;
        } else if (hullHeat <= channelHeat) {
            providedHeat = 0.0D;
        } else {
            providedHeat = (hullHeat - channelHeat) / 2.0D;
            hullHeat -= providedHeat;
            providedHeat *= Math.max(0.0D, Math.min(1.0D, heatProvision));
        }

        setState(stack, (float) depletion, (float) xenonLevel, (float) coreHeat, (float) hullHeat);
        return new FuelTickResult(outputFlux, providedHeat, hullHeat);
    }

    private static double reactivity(Fuel fuel, double inFlux, double enrichment) {
        double flux = inFlux * reactivityModByEnrichment(fuel.depletionFunction(), enrichment);
        return switch (fuel.function()) {
            case PASSIVE -> fuel.selfRate() * enrichment;
            case LOG_TEN -> Math.log10(flux + 1.0D) * 0.5D * fuel.reactivity();
            case PLATEU -> (1.0D - Math.pow(Math.E, -flux / 25.0D)) * fuel.reactivity();
            case ARCH -> Math.max((flux - (flux * flux / 10000.0D)) / 100.0D * fuel.reactivity(), 0.0D);
            case SIGMOID -> fuel.reactivity() / (1.0D + Math.pow(Math.E, -(flux - 50.0D) / 10.0D));
            case SQUARE_ROOT -> Math.sqrt(flux) * fuel.reactivity() / 10.0D;
            case LINEAR -> flux / 100.0D * fuel.reactivity();
            case QUADRATIC -> flux * flux / 10000.0D * fuel.reactivity();
            case EXPERIMENTAL -> flux * (Math.sin(flux) + 1.0D) * fuel.reactivity();
        };
    }

    private static double reactivityModByEnrichment(DepletionFunction function, double enrichment) {
        return switch (function) {
            case LINEAR -> enrichment;
            case STATIC -> 1.0D;
            case BOOSTED_SLOPE -> enrichment + Math.sin((enrichment - 1.0D) * (enrichment - 1.0D) * Math.PI);
            case RAISING_SLOPE -> enrichment + (Math.sin(enrichment * Math.PI) / 2.0D);
            case GENTLE_SLOPE -> enrichment + (Math.sin(enrichment * Math.PI) / 3.0D);
        };
    }

    private static double xenonGen(Fuel fuel, double flux) {
        return flux * fuel.xenonGen();
    }

    private static double xenonBurn(Fuel fuel, double flux) {
        return (flux * flux) / fuel.xenonBurn();
    }

    private static double rectify(double value) {
        if (Double.isNaN(value) || value < 20.0D) {
            return 20.0D;
        }
        return Math.min(1_000_000.0D, value);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return depletion(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(13.0F * depletion(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x66d15f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Fuel fuel = fuel(stack);
        if (hullHeat(stack) >= 50.0F || coreHeat(stack) >= 50.0F) {
            tooltip.add(Component.translatable("desc.item.wasteCooling").withStyle(ChatFormatting.GOLD));
        }
        if (fuel.selfRate() > 0.0D || fuel.function() == BurnFunction.SIGMOID) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.source").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.full_name", fuel.fullName()).withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.depletion", percent(depletion(stack))).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.xenon", round(xenon(stack), 3)).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.splits_with", fuel.neutronIn().title()).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.splits_into", fuel.neutronOut().title()).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.flux_func", fluxFunction(fuel)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.func_type", fuel.function().title()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.xenon_gen", "x * " + round(fuel.xenonGen(), 3)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.xenon_burn", "x^2 / " + round(fuel.xenonBurn(), 3)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.heat", round(fuel.heat(), 3)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.diffusion", round(fuel.diffusion(), 4)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.skin_temp", round(hullHeat(stack), 1)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.core_temp", round(coreHeat(stack), 1)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.melt", round(fuel.meltingPoint(), 1)).withStyle(ChatFormatting.DARK_RED));
    }

    public static float depletion(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getFloat(DEPLETION_TAG);
    }

    public static float xenon(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getFloat(XENON_TAG);
    }

    public static float coreHeat(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getFloat(CORE_HEAT_TAG);
    }

    public static float hullHeat(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getFloat(HULL_HEAT_TAG);
    }

    public static void setState(ItemStack stack, float depletion, float xenon, float coreHeat, float hullHeat) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putFloat(DEPLETION_TAG, Math.max(0.0F, depletion));
        tag.putFloat(XENON_TAG, Math.max(0.0F, xenon));
        tag.putFloat(CORE_HEAT_TAG, Math.max(0.0F, coreHeat));
        tag.putFloat(HULL_HEAT_TAG, Math.max(0.0F, hullHeat));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Exact 1.7.10 spent-fuel-pool path: updateHeat(..., 0.025), followed by
     * provideHeat(..., 20, 0.025), with the released heat discarded into water.
     */
    public static void coolInSpentFuelPool(ItemStack stack, double diffusionModifier, double heatProvision) {
        if (!(stack.getItem() instanceof RbmkFuelRodItem rod)) {
            return;
        }

        Fuel fuel = rod.fuel(stack);
        double core = rectify(coreHeat(stack));
        double hull = rectify(hullHeat(stack));
        if (core > hull) {
            double movedHeat = (core - hull) / 2.0D * fuel.diffusion() * Math.max(0.0D, diffusionModifier) * 0.025D;
            core = rectify(core - movedHeat);
            hull = rectify(hull + movedHeat);
        }

        if (hull > fuel.meltingPoint()) {
            double average = (20.0D + hull + core) / 3.0D;
            core = average;
            hull = average;
        } else if (hull > 20.0D) {
            double removedHeat = (hull - 20.0D) / 2.0D;
            removedHeat *= Math.max(0.0D, Math.min(1.0D, heatProvision)) * 0.025D;
            hull -= removedHeat;
        }

        setState(stack, depletion(stack), xenon(stack), (float) core, (float) hull);
    }

    private static String fluxFunction(Fuel fuel) {
        String x = fuel.selfRate() > 0.0D ? "(x + " + round(fuel.selfRate(), 3) + ")" : "x";
        String r = round(fuel.reactivity(), 3);
        return switch (fuel.function()) {
            case PASSIVE -> round(fuel.selfRate(), 3);
            case LOG_TEN -> "log10(" + x + " + 1) * 0.5 * " + r;
            case PLATEU -> "(1 - e^(-" + x + " / 25)) * " + r;
            case ARCH -> "(" + x + " - " + x + "^2 / 10000) / 100 * " + r + " [0;inf]";
            case SIGMOID -> r + " / (1 + e^(-(" + x + " - 50) / 10))";
            case SQUARE_ROOT -> "sqrt(" + x + ") * " + r + " / 10";
            case LINEAR -> x + " / 100 * " + r;
            case QUADRATIC -> x + "^2 / 10000 * " + r;
            case EXPERIMENTAL -> x + " * (sin(" + x + ") + 1) * " + r;
        };
    }

    private static double percent(float depletion) {
        return Math.floor(Math.max(0.0F, depletion) * 100_000.0D) / 1_000.0D;
    }

    private static String round(double value, int places) {
        double scale = Math.pow(10.0D, places);
        return String.format(Locale.US, "%." + places + "f", Math.round(value * scale) / scale);
    }

    private enum BurnFunction {
        PASSIVE("SAFE / PASSIVE"),
        LOG_TEN("MEDIUM / LOGARITHMIC"),
        PLATEU("SAFE / EULER"),
        ARCH("DANGEROUS / NEGATIVE-QUADRATIC"),
        SIGMOID("SAFE / SIGMOID"),
        SQUARE_ROOT("MEDIUM / SQUARE ROOT"),
        LINEAR("DANGEROUS / LINEAR"),
        QUADRATIC("DANGEROUS / QUADRATIC"),
        EXPERIMENTAL("EXPERIMENTAL / SINE SLOPE");

        private final String title;

        BurnFunction(String title) {
            this.title = title;
        }

        String title() {
            return this.title;
        }
    }

    private enum DepletionFunction {
        LINEAR,
        RAISING_SLOPE,
        BOOSTED_SLOPE,
        GENTLE_SLOPE,
        STATIC
    }

    public enum NeutronType {
        SLOW("SLOW"),
        FAST("FAST"),
        ANY("ANY");

        private final String title;

        NeutronType(String title) {
            this.title = title;
        }

        public String title() {
            return this.title;
        }
    }

    public record FuelTickResult(double outputFlux, double providedHeat, double hullHeat) {
    }

    private record Fuel(
            String id,
            String fullName,
            double yield,
            double reactivity,
            double selfRate,
            BurnFunction function,
            DepletionFunction depletionFunction,
            double xenonGen,
            double xenonBurn,
            double heat,
            double diffusion,
            double meltingPoint,
            NeutronType neutronIn,
            NeutronType neutronOut
    ) {
        private Fuel(String id, String fullName, double yield, double reactivity, double selfRate, BurnFunction function) {
            this(id, fullName, yield, reactivity, selfRate, function, DepletionFunction.GENTLE_SLOPE, 0.5D, 50.0D, 1.0D, 0.02D, 1000.0D, NeutronType.SLOW, NeutronType.FAST);
        }

        private Fuel deplete(DepletionFunction value) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, value, xenonGen, xenonBurn, heat, diffusion, meltingPoint, neutronIn, neutronOut);
        }

        private Fuel xenon(double gen, double burn) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, gen, burn, heat, diffusion, meltingPoint, neutronIn, neutronOut);
        }

        private Fuel heat(double value) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, value, diffusion, meltingPoint, neutronIn, neutronOut);
        }

        private Fuel diffusion(double value) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, heat, value, meltingPoint, neutronIn, neutronOut);
        }

        private Fuel melt(double value) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, heat, diffusion, value, neutronIn, neutronOut);
        }

        private Fuel neutrons(NeutronType in, NeutronType out) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, heat, diffusion, meltingPoint, in, out);
        }
    }

    private static final List<Fuel> FUELS = List.of(
            new Fuel("ueu", "Natural Uranium", 100_000_000D, 15D, 0D, BurnFunction.LOG_TEN).deplete(DepletionFunction.RAISING_SLOPE).heat(0.65D).melt(2865D),
            new Fuel("meu", "Medium Enriched Uranium", 100_000_000D, 20D, 0D, BurnFunction.LOG_TEN).deplete(DepletionFunction.RAISING_SLOPE).heat(0.65D).melt(2865D),
            new Fuel("heu233", "Highly Enriched Uranium-233", 100_000_000D, 27.5D, 0D, BurnFunction.LINEAR).heat(1.25D).melt(2865D),
            new Fuel("heu235", "Highly Enriched Uranium-235", 100_000_000D, 50D, 0D, BurnFunction.SQUARE_ROOT).melt(2865D),
            new Fuel("uzh", "Uranium Zirconium Hydride", 50_000_000D, 30D, 0D, BurnFunction.LOG_TEN).heat(0.75D).diffusion(0.1D).melt(1845D),
            new Fuel("thmeu", "Thorium with MEU Driver Fuel", 100_000_000D, 20D, 0D, BurnFunction.PLATEU).deplete(DepletionFunction.BOOSTED_SLOPE).heat(0.65D).melt(3350D),
            new Fuel("lep", "Low Enriched Plutonium-239", 100_000_000D, 35D, 0D, BurnFunction.LOG_TEN).deplete(DepletionFunction.RAISING_SLOPE).heat(0.75D).melt(2744D),
            new Fuel("mep", "Medium Enriched Plutonium-239", 100_000_000D, 35D, 0D, BurnFunction.SQUARE_ROOT).melt(2744D),
            new Fuel("hep", "Highly Enriched Plutonium-239", 100_000_000D, 30D, 0D, BurnFunction.LINEAR).heat(1.25D).melt(2744D),
            new Fuel("hep241", "Highly Enriched Plutonium-241", 100_000_000D, 40D, 0D, BurnFunction.LINEAR).heat(1.75D).melt(2744D),
            new Fuel("lea", "Low Enriched Americium", 100_000_000D, 60D, 10D, BurnFunction.SQUARE_ROOT).deplete(DepletionFunction.RAISING_SLOPE).heat(1.5D).melt(2386D),
            new Fuel("mea", "Medium Enriched Americium", 100_000_000D, 35D, 20D, BurnFunction.ARCH).heat(1.75D).melt(2386D),
            new Fuel("hea241", "Highly Enriched Americium-241", 100_000_000D, 65D, 15D, BurnFunction.SQUARE_ROOT).heat(1.85D).melt(2386D).neutrons(NeutronType.FAST, NeutronType.FAST),
            new Fuel("hea242", "Highly Enriched Americium-242", 100_000_000D, 45D, 0D, BurnFunction.LINEAR).heat(2D).melt(2386D),
            new Fuel("men", "Medium Enriched Neptunium", 100_000_000D, 30D, 0D, BurnFunction.SQUARE_ROOT).deplete(DepletionFunction.RAISING_SLOPE).heat(0.75D).melt(2800D).neutrons(NeutronType.ANY, NeutronType.FAST),
            new Fuel("hen", "Highly Enriched Neptunium", 100_000_000D, 40D, 0D, BurnFunction.SQUARE_ROOT).melt(2800D).neutrons(NeutronType.FAST, NeutronType.FAST),
            new Fuel("mox", "MOX", 100_000_000D, 40D, 0D, BurnFunction.LOG_TEN).deplete(DepletionFunction.RAISING_SLOPE).melt(2815D),
            new Fuel("les", "Low Enriched Sa326", 100_000_000D, 50D, 0D, BurnFunction.SQUARE_ROOT).heat(1.25D).melt(2500D).neutrons(NeutronType.SLOW, NeutronType.SLOW),
            new Fuel("mes", "Medium Enriched Sa326", 100_000_000D, 75D, 0D, BurnFunction.ARCH).heat(1.5D).melt(2750D),
            new Fuel("hes", "Highly Enriched Sa326", 100_000_000D, 90D, 0D, BurnFunction.LINEAR).deplete(DepletionFunction.LINEAR).heat(1.75D).melt(3000D),
            new Fuel("leaus", "Low Enriched Australium", 100_000_000D, 30D, 0D, BurnFunction.SIGMOID).deplete(DepletionFunction.LINEAR).xenon(0.05D, 50D).heat(1.5D).melt(7029D),
            new Fuel("heaus", "Highly Enriched Australium", 100_000_000D, 35D, 0D, BurnFunction.LINEAR).xenon(0.05D, 50D).heat(1.5D).melt(5211D),
            new Fuel("po210be", "Po210Be Neutron Source", 25_000_000D, 0D, 50D, BurnFunction.PASSIVE).deplete(DepletionFunction.LINEAR).xenon(0D, 50D).heat(0.1D).diffusion(0.05D).melt(1287D).neutrons(NeutronType.SLOW, NeutronType.SLOW),
            new Fuel("ra226be", "Ra226Be Neutron Source", 100_000_000D, 0D, 20D, BurnFunction.PASSIVE).deplete(DepletionFunction.LINEAR).xenon(0D, 50D).heat(0.035D).diffusion(0.5D).melt(700D).neutrons(NeutronType.SLOW, NeutronType.SLOW),
            new Fuel("pu238be", "Pu238Be Neutron Source", 50_000_000D, 40D, 40D, BurnFunction.SQUARE_ROOT).heat(0.1D).diffusion(0.05D).melt(1287D).neutrons(NeutronType.SLOW, NeutronType.SLOW),
            new Fuel("balefire_gold", "Flashgold", 100_000_000D, 50D, 10D, BurnFunction.ARCH).deplete(DepletionFunction.LINEAR).xenon(0D, 50D).melt(2000D),
            new Fuel("flashlead", "Flashlead", 250_000_000D, 40D, 50D, BurnFunction.ARCH).deplete(DepletionFunction.LINEAR).xenon(0D, 50D).melt(2050D),
            new Fuel("balefire", "Balefire", 100_000_000D, 100D, 35D, BurnFunction.LINEAR).xenon(0D, 50D).heat(3D).melt(3652D),
            new Fuel("zfb_bismuth", "Bismuth ZFB", 50_000_000D, 20D, 0D, BurnFunction.SQUARE_ROOT).heat(1.75D).melt(2744D),
            new Fuel("zfb_pu241", "Pu-241 ZFB", 50_000_000D, 20D, 0D, BurnFunction.SQUARE_ROOT).melt(2865D),
            new Fuel("zfb_am_mix", "Fuel Grade Americium ZFB", 50_000_000D, 20D, 0D, BurnFunction.LINEAR).heat(1.75D).melt(2744D),
            new Fuel("drx", "Digamma", 10_000_000D, 1000D, 10D, BurnFunction.QUADRATIC).heat(0.1D).melt(100_000D),
            new Fuel("test", "THE VOICES", 1_000_000D, 100D, 0D, BurnFunction.EXPERIMENTAL).heat(1.0D).melt(100_000D)
    );
}
