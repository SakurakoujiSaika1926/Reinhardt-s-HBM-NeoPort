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
    private static final String STATE_MIGRATED_TAG = "rbmkStateV2";
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
        double inFlux = flux + fuel.selfRate();
        return (float) Math.max(0.0D, Math.min(1.0D, depletion(stack) + inFlux / fuel.yield()));
    }

    public double remainingReactivity(ItemStack stack) {
        return 1.0D - depletion(stack);
    }

    public double inputFlux(ItemStack stack, com.reinhardt.hbm.blockentity.NeutronFluxProvider.NeutronFlux flux) {
        return flux.effectiveFor(fuel(stack).neutronIn());
    }

    public double outputFastRatio(ItemStack stack) {
        return switch (fuel(stack).neutronOut()) {
            case SLOW -> 0.0D;
            case FAST -> 1.0D;
            // TileEntityRBMKRod used `rType == SLOW ? 0 : 1`; ANY was
            // therefore emitted as a fully-fast stream in 1.7.10.
            case ANY -> 1.0D;
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
        double workingFlux = inFlux + fuel.selfRate();
        double xenonLevel = xenon(stack);

        if (xenonEnabled) {
            // ItemRBMKRod.burn() reads getPoisonLevel(stack) after reducing
            // its local poison value but before writing it back.  That means
            // attenuation uses the poison present at the start of the tick,
            // not the post-burn value.
            double oldXenonLevel = Math.max(0.0D, Math.min(1.0D, xenonLevel));
            xenonLevel -= xenonBurn(fuel, workingFlux) / 100.0D;
            workingFlux *= 1.0D - oldXenonLevel;
            xenonLevel += xenonGen(fuel, workingFlux) / 100.0D;
            xenonLevel = Math.max(0.0D, Math.min(1.0D, xenonLevel));
        }

        // ItemRBMKRod.burn applies the optional heat coefficient to the fuel
        // enrichment before calculating output flux.  In 1.7.10 this is used
        // by UZH (start 1000, length 500); all other fuels keep multiplier 1.
        double coreHeat = coreHeat(stack);
        double enrichment = 1.0D - depletion(stack);
        if (fuel.heatCoeffStart() != 0.0D && coreHeat >= fuel.heatCoeffStart()) {
            double progress = (coreHeat - fuel.heatCoeffStart()) / fuel.heatCoeffLength();
            if (progress > 1.0D) {
                progress = 1.0D;
            }
            enrichment *= Math.sin((progress * Math.PI + Math.PI) / 2.0D);
        }
        double outputFlux = reactivity(fuel, workingFlux, enrichment) * reactivityMod;
        double depletion = depletion(stack);
        if (depletionEnabled) {
            depletion = Math.max(0.0D, Math.min(1.0D, depletion + workingFlux / fuel.yield()));
        }
        coreHeat = rectify(coreHeat + outputFlux * fuel.heat());
        double hullHeat = hullHeat(stack);

        if (coreHeat > hullHeat) {
            double mid = (coreHeat - hullHeat) / 2.0D;
            double diffusion = fuel.diffusion() * diffusionMod;
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
            double removableHeat = (hullHeat - channelHeat) / 2.0D;
            providedHeat = removableHeat * heatProvision;
            // The old implementation subtracts the same scaled amount that
            // it returns.  Subtracting the unscaled half (as the port did)
            // loses heat too quickly whenever heat provision is below 1.
            hullHeat -= providedHeat;
        }

        setState(stack, depletion, xenonLevel, coreHeat, hullHeat);
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
        return (int) Math.min(13L, Math.round(13.0D * depletion(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x66d15f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Fuel fuel = fuel(stack);
        boolean digamma = isDigammaFuel(stack);
        if (hullHeat(stack) >= 50.0F || coreHeat(stack) >= 50.0F) {
            tooltip.add(Component.translatable("desc.item.wasteCooling").withStyle(ChatFormatting.GOLD));
        }
        if (fuel.selfRate() > 0.0D || fuel.function() == BurnFunction.SIGMOID) {
            tooltip.add(Component.translatable(tooltipKey(digamma, "source")).withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.full_name", fuel.fullName()).withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(tooltipKey(digamma, "depletion"), percent(depletion(stack)) + "%").withStyle(ChatFormatting.GREEN));
        // The legacy NBT stored xenon as a 0..100 percentage.  The port keeps
        // the state normalized to 0..1, so convert only at the display edge.
        tooltip.add(Component.translatable(tooltipKey(digamma, "xenon"), round(xenon(stack) * 100.0D, 3) + "%").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable(tooltipKey(digamma, "splits_with"), neutronTypeName(fuel.neutronIn(), digamma)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable(tooltipKey(digamma, "splits_into"), neutronTypeName(fuel.neutronOut(), digamma)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable(tooltipKey(digamma, "flux_func"), fluxFunction(stack, fuel)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(tooltipKey(digamma, "func_type"), fuel.function().title()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(tooltipKey(digamma, "xenon_gen"), "x * " + round(fuel.xenonGen(), 3)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(tooltipKey(digamma, "xenon_burn"), "x² / " + round(fuel.xenonBurn(), 3)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(tooltipKey(digamma, "heat"), round(fuel.heat(), 3) + "°C").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(tooltipKey(digamma, "diffusion"), round(fuel.diffusion(), 4) + "¹/²").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(tooltipKey(digamma, "skin_temp"), round(hullHeat(stack), 1) + (digamma ? "m" : "°C")).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(tooltipKey(digamma, "core_temp"), round(coreHeat(stack), 1) + (digamma ? "m" : "°C")).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(tooltipKey(digamma, "melt"), round(fuel.meltingPoint(), 1) + (digamma ? "m" : "°C")).withStyle(ChatFormatting.DARK_RED));
    }

    public static double depletion(ItemStack stack) {
        return stateData(stack).getDouble(DEPLETION_TAG);
    }

    public static double xenon(ItemStack stack) {
        return stateData(stack).getDouble(XENON_TAG);
    }

    public static double coreHeat(ItemStack stack) {
        return stateData(stack).getDouble(CORE_HEAT_TAG);
    }

    private static String tooltipKey(boolean digamma, String key) {
        return "tooltip.reinhardtshbm.rbmk_fuel." + (digamma ? "drx." : "") + key;
    }

    private static Component neutronTypeName(NeutronType type, boolean digamma) {
        return Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.neutron_type."
                + (digamma ? "drx." : "") + type.name().toLowerCase(Locale.ROOT));
    }

    public static double hullHeat(ItemStack stack) {
        return stateData(stack).getDouble(HULL_HEAT_TAG);
    }

    public static void setState(ItemStack stack, double depletion, double xenon, double coreHeat, double hullHeat) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        // ItemRBMKRod#setDouble persisted all four values as NBT doubles;
        // retaining double precision is part of the legacy burn result.
        tag.putDouble(DEPLETION_TAG, Math.max(0.0D, depletion));
        tag.putDouble(XENON_TAG, Math.max(0.0D, xenon));
        tag.putDouble(CORE_HEAT_TAG, Math.max(0.0D, coreHeat));
        tag.putDouble(HULL_HEAT_TAG, Math.max(0.0D, hullHeat));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Reads the modern normalized state while converting an old rod's exact
     * NBT keys and units on first access.  1.7.10 stored remaining fuel under
     * {@code yield} (an absolute double), xenon as 0..100, and temperatures
     * under {@code core}/{@code hull}; the port stores depletion/xenon as
     * normalized doubles and uses coreHeat/hullHeat.
     */
    private static CompoundTag stateData(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!(stack.getItem() instanceof RbmkFuelRodItem rod)) {
            return tag;
        }

        boolean legacy = tag.contains("yield") || tag.contains("core") || tag.contains("hull");
        boolean changed = false;
        Fuel fuel = rod.fuel(stack);
        if (legacy && !tag.getBoolean(STATE_MIGRATED_TAG)) {
            // Convert all old keys together.  The marker is required because
            // the old and new xenon keys share the spelling "xenon"; without
            // it a second read could not distinguish 0..100 from 0..1.
            double remaining = tag.contains("yield") ? tag.getDouble("yield") : fuel.yield();
            double poison = tag.contains("xenon") ? tag.getDouble("xenon") : 0.0D;
            tag.putDouble(DEPLETION_TAG, Math.max(0.0D,
                    Math.min(1.0D, (fuel.yield() - remaining) / fuel.yield())));
            tag.putDouble(XENON_TAG, Math.max(0.0D, Math.min(1.0D, poison / 100.0D)));
            tag.putDouble(CORE_HEAT_TAG, tag.contains("core") ? tag.getDouble("core") : 20.0D);
            tag.putDouble(HULL_HEAT_TAG, tag.contains("hull") ? tag.getDouble("hull") : 20.0D);
            tag.putBoolean(STATE_MIGRATED_TAG, true);
            changed = true;
        } else {
            if (!tag.contains(DEPLETION_TAG)) {
                tag.putDouble(DEPLETION_TAG, 0.0D);
                changed = true;
            }
            if (!tag.contains(XENON_TAG)) {
                tag.putDouble(XENON_TAG, 0.0D);
                changed = true;
            }
            if (!tag.contains(CORE_HEAT_TAG)) {
                tag.putDouble(CORE_HEAT_TAG, 20.0D);
                changed = true;
            }
            if (!tag.contains(HULL_HEAT_TAG)) {
                tag.putDouble(HULL_HEAT_TAG, 20.0D);
                changed = true;
            }
        }
        if (changed) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return tag;
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
            double movedHeat = (core - hull) / 2.0D * fuel.diffusion() * diffusionModifier * 0.025D;
            core = rectify(core - movedHeat);
            hull = rectify(hull + movedHeat);
        }

        if (hull > fuel.meltingPoint()) {
            double average = (20.0D + hull + core) / 3.0D;
            core = average;
            hull = average;
        } else if (hull > 20.0D) {
            double removedHeat = (hull - 20.0D) / 2.0D;
            removedHeat *= heatProvision * 0.025D;
            hull -= removedHeat;
        }

        setState(stack, depletion(stack), xenon(stack), core, hull);
    }

    private static String fluxFunction(ItemStack stack, Fuel fuel) {
        String function = switch (fuel.function()) {
            case PASSIVE -> Double.toString(fuel.selfRate());
            case LOG_TEN -> "log10(%1$s + 1) * 0.5 * %2$s";
            case PLATEU -> "(1 - e^(-%1$s / 25)) * %2$s";
            case ARCH -> "(%1$s - %1$s² / 10000) / 100 * %2$s [0;∞]";
            case SIGMOID -> "%2$s / (1 + e^(-(%1$s - 50) / 10))";
            case SQUARE_ROOT -> "sqrt(%1$s) * %2$s / 10";
            case LINEAR -> "%1$s / 100 * %2$s";
            case QUADRATIC -> "%1$s² / 10000 * %2$s";
            case EXPERIMENTAL -> "%1$s * (sin(%1$s) + 1) * %2$s";
        };
        String x = fuel.selfRate() > 0.0D ? "(x + " + fuel.selfRate() + ")" : "x";
        // Tooltip enrichment is the same getEnrichment() value used by the
        // legacy rod: remaining yield normalized to the rod's full yield.
        double enrichment = 1.0D - depletion(stack);
        if (enrichment < 1.0D) {
            double multiplier = reactivityModByEnrichment(fuel.depletionFunction(), enrichment);
            double reactivity = ((int) (fuel.reactivity() * multiplier * 1_000.0D)) / 1_000.0D;
            double enrichmentPercent = ((int) (multiplier * 1_000.0D)) / 10.0D;
            return String.format(Locale.US, function, x, reactivity) + " (" + enrichmentPercent + "%)";
        }
        return String.format(Locale.US, function, x, fuel.reactivity());
    }

    private static double percent(double depletion) {
        return Math.floor(Math.max(0.0D, depletion) * 100_000.0D) / 1_000.0D;
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
            double heatCoeffStart,
            double heatCoeffLength,
            double meltingPoint,
            NeutronType neutronIn,
            NeutronType neutronOut
    ) {
        private Fuel(String id, String fullName, double yield, double reactivity, double selfRate, BurnFunction function) {
            this(id, fullName, yield, reactivity, selfRate, function, DepletionFunction.GENTLE_SLOPE, 0.5D, 50.0D, 1.0D, 0.02D, 0.0D, 0.0D, 1000.0D, NeutronType.SLOW, NeutronType.FAST);
        }

        private Fuel deplete(DepletionFunction value) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, value, xenonGen, xenonBurn, heat, diffusion, heatCoeffStart, heatCoeffLength, meltingPoint, neutronIn, neutronOut);
        }

        private Fuel xenon(double gen, double burn) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, gen, burn, heat, diffusion, heatCoeffStart, heatCoeffLength, meltingPoint, neutronIn, neutronOut);
        }

        private Fuel heat(double value) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, value, diffusion, heatCoeffStart, heatCoeffLength, meltingPoint, neutronIn, neutronOut);
        }

        private Fuel diffusion(double value) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, heat, value, heatCoeffStart, heatCoeffLength, meltingPoint, neutronIn, neutronOut);
        }

        private Fuel melt(double value) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, heat, diffusion, heatCoeffStart, heatCoeffLength, value, neutronIn, neutronOut);
        }

        private Fuel neutrons(NeutronType in, NeutronType out) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, heat, diffusion, heatCoeffStart, heatCoeffLength, meltingPoint, in, out);
        }

        private Fuel heatCoeff(double start, double length) {
            return new Fuel(id, fullName, yield, reactivity, selfRate, function, depletionFunction, xenonGen, xenonBurn, heat, diffusion, start, length, meltingPoint, neutronIn, neutronOut);
        }
    }

    private static final List<Fuel> FUELS = List.of(
            new Fuel("ueu", "Natural Uranium", 100_000_000D, 15D, 0D, BurnFunction.LOG_TEN).deplete(DepletionFunction.RAISING_SLOPE).heat(0.65D).melt(2865D),
            new Fuel("meu", "Medium Enriched Uranium", 100_000_000D, 20D, 0D, BurnFunction.LOG_TEN).deplete(DepletionFunction.RAISING_SLOPE).heat(0.65D).melt(2865D),
            new Fuel("heu233", "Highly Enriched Uranium-233", 100_000_000D, 27.5D, 0D, BurnFunction.LINEAR).heat(1.25D).melt(2865D),
            new Fuel("heu235", "Highly Enriched Uranium-235", 100_000_000D, 50D, 0D, BurnFunction.SQUARE_ROOT).melt(2865D),
            new Fuel("uzh", "Uranium Zirconium Hydride", 50_000_000D, 30D, 0D, BurnFunction.LOG_TEN).heat(0.75D).heatCoeff(1_000D, 500D).diffusion(0.1D).melt(1845D),
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
