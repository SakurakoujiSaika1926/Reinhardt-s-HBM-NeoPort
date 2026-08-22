package com.reinhardt.hbm.fluid;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class HbmFluidDefinition {
    private final int oldId;
    private final String legacyFieldName;
    private final String name;
    private final int color;
    private final int poison;
    private final int flammability;
    private final int reactivity;
    private final HbmFluidSymbol symbol;
    private final int temperatureCelsius;
    private final Set<HbmFluidTrait> traits;
    private final String rawTraits;
    private final String canisterColor;
    private final String gasTankColors;
    private final int niceOrder;

    public HbmFluidDefinition(
            int oldId,
            String legacyFieldName,
            String name,
            int color,
            int poison,
            int flammability,
            int reactivity,
            HbmFluidSymbol symbol,
            int temperatureCelsius,
            Set<HbmFluidTrait> traits,
            String rawTraits,
            String canisterColor,
            String gasTankColors,
            int niceOrder
    ) {
        this.oldId = oldId;
        this.legacyFieldName = Objects.requireNonNull(legacyFieldName);
        this.name = Objects.requireNonNull(name).toLowerCase(Locale.ROOT);
        this.color = color;
        this.poison = poison;
        this.flammability = flammability;
        this.reactivity = reactivity;
        this.symbol = Objects.requireNonNull(symbol);
        this.temperatureCelsius = temperatureCelsius;
        this.traits = Collections.unmodifiableSet(
                traits.isEmpty() ? EnumSet.noneOf(HbmFluidTrait.class) : EnumSet.copyOf(traits)
        );
        this.rawTraits = rawTraits == null ? "" : rawTraits;
        this.canisterColor = canisterColor == null ? "" : canisterColor;
        this.gasTankColors = gasTankColors == null ? "" : gasTankColors;
        this.niceOrder = niceOrder;
    }

    public int oldId() {
        return oldId;
    }

    public String legacyFieldName() {
        return legacyFieldName;
    }

    public String name() {
        return name;
    }

    public int color() {
        return color;
    }

    public int poison() {
        return poison;
    }

    public int flammability() {
        return flammability;
    }

    public int reactivity() {
        return reactivity;
    }

    public HbmFluidSymbol symbol() {
        return symbol;
    }

    public int temperatureCelsius() {
        return temperatureCelsius;
    }

    public int temperatureKelvin() {
        return temperatureCelsius + 273;
    }

    public int niceOrder() {
        return niceOrder;
    }

    public String rawTraits() {
        return rawTraits;
    }

    public String canisterColor() {
        return canisterColor;
    }

    public String gasTankColors() {
        return gasTankColors;
    }

    public boolean hasTrait(HbmFluidTrait trait) {
        if (trait == HbmFluidTrait.FLAMMABLE) {
            return traits.contains(trait) || fuelProperties().isFlammable();
        }
        if (trait == HbmFluidTrait.COMBUSTIBLE) {
            return traits.contains(trait) || fuelProperties().isCombustible();
        }
        return traits.contains(trait);
    }

    public HbmFluidFuelProperties fuelProperties() {
        return HbmFluidFuelProperties.forFluid(this);
    }

    public long flammableHeatEnergy() {
        return fuelProperties().flammableHeatEnergy();
    }

    public int flammableHeatPerMillibucket() {
        return fuelProperties().flammableHeatPerMillibucket();
    }

    public CombustibleFuelGrade combustibleFuelGrade() {
        return fuelProperties().combustibleGrade();
    }

    public long combustibleHeatEnergy() {
        return fuelProperties().combustionEnergy();
    }

    public Set<HbmFluidTrait> traits() {
        return traits;
    }

    public boolean isNone() {
        return oldId == 0 || name.equals("none");
    }

    public boolean isVanillaMapped() {
        return name.equals("water") || name.equals("lava");
    }

    public boolean registersNeoForgeFluid() {
        return !isNone() && !isVanillaMapped() && !hasTrait(HbmFluidTrait.NO_FORGE);
    }

    public boolean allowsRegularContainer() {
        return !isNone()
                && !hasTrait(HbmFluidTrait.NO_CONTAINER)
                && !hasTrait(HbmFluidTrait.LEAD_CONTAINER)
                && !hasTrait(HbmFluidTrait.ANTIMATTER)
                && !hasTrait(HbmFluidTrait.PLASMA);
    }

    public boolean allowsBucket() {
        return registersNeoForgeFluid()
                && allowsRegularContainer()
                && hasTrait(HbmFluidTrait.LIQUID)
                && !hasTrait(HbmFluidTrait.GASEOUS)
                && !hasTrait(HbmFluidTrait.EVAPORATES);
    }

    public boolean allowsFluidIdentifier() {
        return !isNone() && !hasTrait(HbmFluidTrait.NO_IDENTIFIER);
    }

    public String translationKey() {
        return "hbmfluid." + name;
    }

    public ResourceLocation forgeFluidStillTexture() {
        String base = specialForgeTextureBase();
        return forgeFluidTexture(base.equals("gas_default") ? base : base + "_still");
    }

    public ResourceLocation forgeFluidFlowingTexture() {
        String base = specialForgeTextureBase();
        if (base.equals("gas_default")) {
            return forgeFluidTexture(base);
        }
        if (base.equals("fluid_default") || base.equals("fluid_viscous_default")) {
            return forgeFluidTexture(base + "_still");
        }
        return forgeFluidTexture(base + "_flowing");
    }

    private static ResourceLocation forgeFluidTexture(String textureName) {
        return ResourceLocation.fromNamespaceAndPath("reinhardtshbm", "block/forgefluid/" + textureName);
    }

    private String specialForgeTextureBase() {
        return switch (name) {
            case "corium_fluid" -> "corium";
            case "volcanic_lava_fluid" -> "volcanic_lava";
            case "mud_fluid" -> "mud";
            case "acid_fluid" -> "acid";
            case "toxic_fluid" -> "toxic";
            case "rad_lava_fluid" -> "rad_lava";
            case "peroxide" -> "acid";
            case "schrabidic" -> "schrabidic_acid";
            case "sulfuric_acid" -> "sulfuric_acid";
            case "watz", "watz_heavy" -> "mud";
            case "wastefluid" -> "toxic";
            default -> {
                if (hasTrait(HbmFluidTrait.GASEOUS)) {
                    yield "gas_default";
                }
                if (hasTrait(HbmFluidTrait.VISCOUS)) {
                    yield "fluid_viscous_default";
                }
                yield "fluid_default";
            }
        };
    }
}
