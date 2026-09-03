package com.reinhardt.hbm.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

public class HbmNeoFluidType extends FluidType {
    private final HbmFluidDefinition definition;

    public HbmNeoFluidType(HbmFluidDefinition definition) {
        super(propertiesFor(definition));
        this.definition = definition;
    }

    public HbmFluidDefinition definition() {
        return definition;
    }

    private static Properties propertiesFor(HbmFluidDefinition definition) {
        int density = switch (definition.name()) {
            case "mud_fluid", "acid_fluid", "toxic_fluid" -> 2500;
            case "schrabidic" -> 31_200;
            case "corium_fluid" -> 600_000;
            case "volcanic_lava_fluid", "rad_lava_fluid" -> 3000;
            case "sulfuric_acid" -> 1840;
            default -> definition.hasTrait(HbmFluidTrait.GASEOUS) ? -1000 : definition.hasTrait(HbmFluidTrait.VISCOUS) ? 2000 : 1000;
        };
        int viscosity = switch (definition.name()) {
            case "mud_fluid", "volcanic_lava_fluid", "rad_lava_fluid" -> 3000;
            case "acid_fluid" -> 1500;
            case "toxic_fluid" -> 2000;
            case "schrabidic" -> 500;
            case "corium_fluid" -> 12_000;
            case "sulfuric_acid" -> 1000;
            default -> definition.hasTrait(HbmFluidTrait.VISCOUS) ? 6000 : 1000;
        };
        int light = switch (definition.name()) {
            case "mud_fluid", "acid_fluid" -> 5;
            case "toxic_fluid", "volcanic_lava_fluid", "rad_lava_fluid" -> 15;
            case "corium_fluid" -> 10;
            default -> definition.temperatureCelsius() >= 1000 || definition.name().contains("plasma") || definition.name().contains("balefire") ? 8 : 0;
        };
        return Properties.create()
                .descriptionId(definition.translationKey())
                .density(density)
                .temperature(definition.temperatureKelvin())
                .viscosity(viscosity)
                .lightLevel(Math.min(15, light))
                .pathType(definition.hasTrait(HbmFluidTrait.GASEOUS) ? PathType.OPEN : PathType.WATER)
                .adjacentPathType(definition.hasTrait(HbmFluidTrait.GASEOUS) ? PathType.OPEN : PathType.WATER_BORDER)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
    }

    /**
     * Client extension mapping registered through RegisterClientExtensionsEvent.
     */
    public IClientFluidTypeExtensions clientExtensions() {
        return new IClientFluidTypeExtensions() {
            @Override
            public int getTintColor() {
                return 0xFF000000 | definition.color();
            }

            @Override
            public ResourceLocation getStillTexture() {
                return definition.forgeFluidStillTexture();
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return definition.forgeFluidFlowingTexture();
            }
        };
    }
}
