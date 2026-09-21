package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.integration.createbigcannons.HbmCreateBigCannonsCompat;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.ConcreteColoredBlockItem;
import com.reinhardt.hbm.item.CokeBlockItem;
import com.reinhardt.hbm.item.DrillbitItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.HbmFluidDuctItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.LegacyVariantBlockItem;
import com.reinhardt.hbm.item.LegacyBedrockOreStageItem;
import com.reinhardt.hbm.item.LegacyByproductItem;
import com.reinhardt.hbm.item.MetalFenceBlockItem;
import com.reinhardt.hbm.item.MeteorOreBlockItem;
import com.reinhardt.hbm.item.NuclearWasteItem;
import com.reinhardt.hbm.item.OreBasaltBlockItem;
import com.reinhardt.hbm.item.SellafieldBlockItem;
import com.reinhardt.hbm.item.ToasterBlockItem;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.FusionComponentBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Set;
import java.util.function.Supplier;

public final class HbmCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ReinhardtsHBM.MOD_ID);

    public static final Supplier<CreativeModeTab> ORES = TABS.register(
            "ores",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.ores"))
                    .icon(() -> new ItemStack(HbmBlocks.ORE_URANIUM.get()))
                    .displayItems((parameters, output) -> {
                        for (var ore : HbmBlocks.OVERWORLD_ORES) {
                            if (!isPetroleumBlock(ore)) {
                                if (ore == HbmBlocks.ORE_METEOR && ore.get().asItem() instanceof MeteorOreBlockItem meteorOre) {
                                    meteorOre.addCreativeVariants(output);
                                } else if (ore == HbmBlocks.ORE_BASALT && ore.get().asItem() instanceof OreBasaltBlockItem basaltOre) {
                                    basaltOre.addCreativeVariants(output);
                                } else {
                                    acceptBlock(output, ore);
                                }
                            }
                        }
                        output.accept(HbmBlocks.ANCIENT_SCRAP);
                        for (var ore : HbmBlocks.DEEPSLATE_ORES) {
                            if (!isPetroleumBlock(ore)) {
                                acceptBlock(output, ore);
                            }
                        }
                        for (var ore : HbmBlocks.SCHIST_ORES) {
                            acceptBlock(output, ore);
                        }
                        for (var ore : HbmBlocks.NETHER_ORES) {
                            output.accept(ore);
                        }
                        for (var cluster : HbmBlocks.ORE_CLUSTERS) {
                            acceptBlock(output, cluster);
                        }
                        output.accept(HbmBlocks.ORE_VOLCANO);
                        for (var rawOre : HbmItems.RAW_ORES) {
                            output.accept(rawOre);
                        }
                        for (var crystal : HbmItems.MINERAL_CRYSTALS) {
                            output.accept(crystal);
                        }
                        for (var oreDrop : HbmItems.ORE_DROPS) {
                            if (oreDrop.get() instanceof LegacyVariantItem variantItem) {
                                variantItem.addCreativeVariants(output);
                            } else if (oreDrop.get() instanceof LegacyBedrockOreStageItem bedrockOre) {
                                bedrockOre.addCreativeVariants(output);
                            } else if (oreDrop.get() instanceof LegacyByproductItem byproduct) {
                                byproduct.addCreativeVariants(output);
                            } else if (oreDrop.get() instanceof BedrockOreItem bedrockOre) {
                                bedrockOre.addCreativeVariants(output);
                            } else {
                                output.accept(oreDrop);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> MATERIALS = TABS.register(
            "materials",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.materials"))
                    .icon(() -> new ItemStack(HbmItems.INGOT_STEEL.get()))
                    .displayItems((parameters, output) -> {
                        for (var powder : HbmItems.POWDER_MATERIALS) {
                            if (powder.get() instanceof LegacyVariantItem variantItem) {
                                variantItem.addCreativeVariants(output);
                            } else {
                                output.accept(powder);
                            }
                        }
                        for (var plate : HbmItems.PLATE_MATERIALS) {
                            output.accept(plate);
                        }
                        for (var material : HbmItems.MISC_MATERIALS) {
                            if (isPetroleumItem(material) || isNuclearBilletItem(material)) {
                                continue;
                            }
                            if (material == HbmItems.FALLOUT_ITEM) {
                                output.accept(material);
                            } else if (material.get() instanceof com.reinhardt.hbm.item.ScrapsItem scraps) {
                                scraps.addCreativeVariants(output);
                            } else if (material.get() instanceof NuclearWasteItem waste) {
                                waste.addCreativeVariants(output);
                            } else if (material.get() instanceof LegacyVariantItem variantItem) {
                                variantItem.addCreativeVariants(output);
                            } else {
                                output.accept(material);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> MATERIAL_NUGGETS = TABS.register(
            "material_nuggets",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.material_nuggets"))
                    .icon(() -> HbmItems.NUGGET_MATERIALS.isEmpty()
                            ? new ItemStack(HbmItems.INGOT_STEEL.get())
                            : new ItemStack(HbmItems.NUGGET_MATERIALS.get(0).get()))
                    .displayItems((parameters, output) -> {
                        for (var nugget : HbmItems.NUGGET_MATERIALS) {
                            output.accept(nugget);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> MATERIAL_INGOTS = TABS.register(
            "material_ingots",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.material_ingots"))
                    .icon(() -> new ItemStack(HbmItems.INGOT_STEEL.get()))
                    .displayItems((parameters, output) -> {
                        for (var ingot : HbmItems.INGOT_MATERIALS) {
                            output.accept(ingot);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> MATERIAL_BLOCKS = TABS.register(
            "material_blocks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.material_blocks"))
                    .icon(() -> new ItemStack(HbmBlocks.BLOCK_STEEL.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.MATERIAL_BLOCKS) {
                            if (block.get() == HbmBlocks.BLOCK_COKE.get()
                                    && HbmBlocks.BLOCK_COKE.get().asItem() instanceof CokeBlockItem cokeBlock) {
                                cokeBlock.addCreativeVariants(output);
                            } else {
                                acceptBlock(output, block);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> MACHINE_COMPONENTS = TABS.register(
            "machine_components",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.machine_components"))
                    .icon(() -> new ItemStack(HbmItems.UPGRADE_TEMPLATE.get()))
                    .displayItems((parameters, output) -> {
                        for (var component : HbmItems.MACHINE_COMPONENTS) {
                            if (component.get() instanceof LegacyVariantItem variantItem) {
                                variantItem.addCreativeVariants(output);
                            } else if (component.get() instanceof com.reinhardt.hbm.item.LegacyMetaUpgradeItem upgrade) {
                                upgrade.addCreativeVariants(output);
                            } else if (component.get() instanceof DrillbitItem drillbit) {
                                drillbit.addCreativeVariants(output);
                            } else {
                                output.accept(component);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> POWER_GRID = TABS.register(
            "power_grid",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.power_grid"))
                    .icon(() -> new ItemStack(HbmBlocks.RED_CABLE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(HbmBlocks.RED_CABLE);
                        output.accept(HbmBlocks.RED_CABLE_CLASSIC);
                        if (HbmBlocks.RED_CABLE_BOX.get().asItem() instanceof com.reinhardt.hbm.item.PowerCableBoxBlockItem boxCable) {
                            boxCable.addCreativeVariants(output);
                        }
                        output.accept(HbmBlocks.RED_CABLE_PAINTABLE);
                        output.accept(HbmBlocks.RED_CABLE_GAUGE);
                        output.accept(HbmBlocks.RED_WIRE_COATED);
                        output.accept(HbmBlocks.CABLE_DIODE);
                        output.accept(HbmBlocks.CABLE_SWITCH);
                        output.accept(HbmBlocks.CABLE_DETECTOR);
                        output.accept(HbmBlocks.CHARGER);
                        output.accept(HbmBlocks.RED_CONNECTOR);
                        output.accept(HbmBlocks.CONNECTOR_RED_SUPER);
                        output.accept(HbmBlocks.RED_PYLON);
                        output.accept(HbmBlocks.RED_PYLON_MEDIUM_WOOD);
                        output.accept(HbmBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER);
                        output.accept(HbmBlocks.RED_PYLON_MEDIUM_STEEL);
                        output.accept(HbmBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER);
                        output.accept(HbmBlocks.RED_PYLON_LARGE);
                        output.accept(HbmBlocks.SUBSTATION);
                        output.accept(HbmBlocks.MACHINE_DETECTOR);
                        output.accept(HbmItems.WIRING_RED_COPPER);
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> GENERATORS = TABS.register(
            "generators",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.generators"))
                    .icon(() -> new ItemStack(HbmBlocks.MACHINE_STIRLING.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(HbmBlocks.MACHINE_WOOD_BURNER);
                        output.accept(HbmBlocks.MACHINE_STIRLING);
                        output.accept(HbmBlocks.MACHINE_STIRLING_STEEL);
                        output.accept(HbmBlocks.MACHINE_STIRLING_CREATIVE);
                        output.accept(HbmBlocks.MACHINE_DIESEL);
                        output.accept(HbmBlocks.MACHINE_COMBUSTION_ENGINE);
                        output.accept(HbmBlocks.MACHINE_STEAM_ENGINE);
                        output.accept(HbmBlocks.MACHINE_TURBINE);
                        output.accept(HbmBlocks.MACHINE_INDUSTRIAL_TURBINE);
                        output.accept(HbmBlocks.MACHINE_CHUNGUS);
                        output.accept(HbmBlocks.MACHINE_TURBINEGAS);
                        output.accept(HbmBlocks.MACHINE_TURBOFAN);
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> POWER_STORAGE = TABS.register(
            "power_storage",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.power_storage"))
                    .icon(() -> LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_lead"))
                    .displayItems((parameters, output) -> {
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_redstone"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_lead"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_lithium"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_sodium"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_schrabidium"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_quantum"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "capacitor_copper"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "capacitor_gold"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "capacitor_niobium"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "capacitor_tantalum"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "capacitor_bismuth"));
                        output.accept(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "capacitor_spark"));
                        ((com.reinhardt.hbm.item.SelfChargingBatteryItem) HbmItems.BATTERY_SC.get()).addCreativeVariants(output);
                        output.accept(HbmItems.BATTERY_CREATIVE);
                        output.accept(HbmItems.BATTERY_POTATO);
                        output.accept(HbmItems.BATTERY_POTATOS);
                        output.accept(HbmItems.CUBE_POWER);
                        output.accept(HbmBlocks.MACHINE_BATTERY_REDD);
                        output.accept(HbmBlocks.MACHINE_BATTERY_SOCKET);
                        output.accept(HbmBlocks.CAPACITOR_COPPER);
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> REACTORS = TABS.register(
            "reactors",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.reactors"))
                    .icon(() -> new ItemStack(HbmBlocks.MACHINE_REACTOR_BREEDING.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.REACTOR_BLOCKS) {
                            if (block == HbmBlocks.FUSION_COMPONENT
                                    && block.get().asItem() instanceof FusionComponentBlockItem fusionComponent) {
                                fusionComponent.addCreativeVariants(output);
                            } else {
                                acceptBlock(output, block);
                            }
                        }
                        for (var item : HbmItems.REACTOR_ITEMS) {
                            output.accept(item);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> FUEL_RODS = TABS.register(
            "fuel_rods",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.fuel_rods"))
                    .icon(() -> LegacyVariantItem.stackFor(HbmItems.ROD, "u235"))
                    .displayItems((parameters, output) -> {
                        for (var item : HbmItems.FUEL_ROD_ITEMS) {
                            if (item.get() instanceof LegacyVariantItem variantItem) {
                                variantItem.addCreativeVariants(output);
                            } else if (item.get() instanceof com.reinhardt.hbm.item.WatzPelletItem pelletItem) {
                                pelletItem.addCreativeVariants(output);
                            } else if (item.get() instanceof com.reinhardt.hbm.item.IcfPelletItem pelletItem) {
                                pelletItem.addCreativeVariants(output);
                            } else {
                                output.accept(item);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> RBMK_REACTOR = TABS.register(
            "rbmk_reactor",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.rbmk_reactor"))
                    .icon(() -> new ItemStack(HbmBlocks.RBMK_ROD.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.RBMK_BLOCKS) {
                            acceptBlock(output, block);
                        }
                        for (var item : HbmItems.RBMK_REACTOR_ITEMS) {
                            output.accept(item);
                        }
                        for (var item : HbmItems.RBMK_FUEL_ROD_ITEMS) {
                            if (item.get() instanceof LegacyVariantItem variantItem) {
                                variantItem.addCreativeVariants(output);
                            } else if (item.get() instanceof com.reinhardt.hbm.item.RbmkPelletItem pelletItem) {
                                pelletItem.addCreativeVariants(output);
                            } else {
                                output.accept(item);
                            }
                        }
                        for (var item : HbmItems.RBMK_PELLET_ITEMS) {
                            if (item.get() instanceof com.reinhardt.hbm.item.RbmkPelletItem pelletItem) {
                                pelletItem.addCreativeVariants(output);
                            } else {
                                output.accept(item);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> NUCLEAR_BILLETS = TABS.register(
            "nuclear_billets",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.nuclear_billets"))
                    .icon(() -> HbmItems.NUCLEAR_BILLETS.isEmpty()
                            ? new ItemStack(HbmItems.INGOT_URANIUM.get())
                            : new ItemStack(HbmItems.NUCLEAR_BILLETS.get(0).get()))
                    .displayItems((parameters, output) -> {
                        for (var billet : HbmItems.NUCLEAR_BILLETS) {
                            output.accept(billet);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> NUCLEAR_WEAPONS = TABS.register(
            "nuclear_weapons",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.nuclear_weapons"))
                    .icon(() -> new ItemStack(HbmBlocks.NUKE_BOY.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.NUCLEAR_WEAPON_BLOCKS) {
                            if (block == HbmBlocks.VOLCANO_CORE || block == HbmBlocks.VOLCANO_RAD_CORE
                                    || block == HbmBlocks.CRASHED_BOMB) {
                                continue;
                            }
                            acceptBlock(output, block);
                        }
                        if (HbmBlocks.CRASHED_BOMB.get().asItem() instanceof com.reinhardt.hbm.item.CrashedBombBlockItem dud) {
                            dud.addCreativeVariants(output);
                        }
                        if (HbmBlocks.VOLCANO_CORE.get().asItem() instanceof com.reinhardt.hbm.item.VolcanoCoreBlockItem volcano) {
                            volcano.addCreativeVariants(output);
                        }
                        if (HbmBlocks.VOLCANO_RAD_CORE.get().asItem() instanceof com.reinhardt.hbm.item.VolcanoCoreBlockItem volcano) {
                            volcano.addCreativeVariants(output);
                        }
                        for (var item : HbmItems.NUCLEAR_WEAPON_ITEMS) {
                            output.accept(item);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> ROCKETS_MISSILES = TABS.register(
            "rockets_missiles",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.rockets_missiles"))
                    .icon(() -> new ItemStack(HbmItems.MISSILE_SOYUZ.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.ROCKET_MISSILE_BLOCKS) {
                            acceptBlock(output, block);
                        }
                        for (var item : HbmItems.ROCKET_MISSILE_ITEMS) {
                            if (!HbmItems.isHiddenMissilePart(item) && !HbmItems.isHiddenLegacyMissile(item)) {
                                outputLegacyItem(output, item);
                            }
                        }
                        for (var item : HbmItems.SATELLITE_ITEMS) {
                            outputLegacyItem(output, item);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> TURRETS = TABS.register(
            "turrets",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.turrets"))
                    .icon(() -> new ItemStack(HbmBlocks.TURRET_JEREMY.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.TURRET_BLOCKS) {
                            acceptBlock(output, block);
                        }
                        for (var item : HbmItems.TURRET_ITEMS) {
                            if (item.get() instanceof LegacyVariantItem variantItem) {
                                variantItem.addCreativeVariants(output);
                            } else {
                                output.accept(item);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> CREATE_ARTILLERY_INTEGRATION = TABS.register(
            "create_artillery_integration",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.create_artillery_integration"))
                    .icon(() -> {
                        ItemStack mustardShell = HbmCreateBigCannonsCompat.mustardGasFluidShellStack();
                        return mustardShell.isEmpty() ? new ItemStack(HbmBlocks.TURRET_ARTY.get()) : mustardShell;
                    })
                    .displayItems((parameters, output) -> {
                        ItemStack mustardShell = HbmCreateBigCannonsCompat.mustardGasFluidShellStack(parameters.holders());
                        if (!mustardShell.isEmpty()) {
                            output.accept(mustardShell);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> FLUIDS = TABS.register(
            "fluids",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.fluids"))
                    .icon(() -> com.reinhardt.hbm.item.FluidIconItem.forFluid(
                            HbmFluids.byName("water").orElse(HbmFluids.none())
                    ))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.FLUID_BLOCKS) {
                            acceptBlock(output, block);
                        }
                        for (var item : HbmItems.FLUID_ITEMS) {
                            if (item.get() instanceof com.reinhardt.hbm.item.HbmFluidContainerItem container) {
                                acceptFluidContainerVariants(output, container);
                            } else if (item.get() instanceof com.reinhardt.hbm.item.HbmFluidDuctItem duct) {
                                duct.addCreativeVariants(output);
                            } else {
                                output.accept(item);
                            }
                        }
                        for (var definition : HbmFluids.niceOrder()) {
                            if (definition.allowsFluidIdentifier()) {
                                output.accept(com.reinhardt.hbm.item.FluidIdentifierItem.forFluid(definition));
                                output.accept(com.reinhardt.hbm.item.FluidIconItem.forFluid(definition));
                            }
                        }
                        for (var entry : HbmFluids.entries()) {
                            entry.bucketItem().ifPresent(output::accept);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> PETROLEUM = TABS.register(
            "petroleum",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.petroleum"))
                    .icon(() -> new ItemStack(HbmBlocks.MACHINE_REFINERY.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.PETROLEUM_BLOCKS) {
                            acceptBlock(output, block);
                        }
                        for (var block : HbmBlocks.OIL_FIELD_BLOCKS) {
                            acceptBlock(output, block);
                        }

                        for (var block : LegacyHbmContent.LEGACY_BLOCKS) {
                            if (isPetroleumLegacyBlock(block)) {
                                acceptBlock(output, block);
                            }
                        }

                        HbmItems.OIL_TAR_ITEMS.values().forEach(output::accept);
                        output.accept(HbmItems.COKE_ITEMS.get("petroleum"));
                        output.accept(HbmItems.CANISTER_LUBRICANT);
                        output.accept(HbmItems.SOLID_FUEL);
                        output.accept(HbmItems.SOLID_FUEL_BF);
                        HbmItems.FUEL_ADDITIVE_ITEMS.values().forEach(output::accept);
                        for (var item : LegacyHbmContent.LEGACY_ITEMS) {
                            if (isPetroleumLegacyItem(item)) {
                                output.accept(item);
                            }
                        }

                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> CONTAINERS = TABS.register(
            "containers",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.containers"))
                    .icon(() -> new ItemStack(HbmBlocks.CRATE_STEEL.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.CONTAINER_BLOCKS) {
                            acceptBlock(output, block);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> THERMAL = TABS.register(
            "thermal",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.thermal"))
                    .icon(() -> new ItemStack(HbmBlocks.HEATER_FIREBOX.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.THERMAL_BLOCKS) {
                            acceptBlock(output, block);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> FOUNDRY = TABS.register(
            "foundry",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.foundry"))
                    .icon(() -> new ItemStack(HbmBlocks.MACHINE_CRUCIBLE.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.FOUNDRY_BLOCKS) {
                            acceptBlock(output, block);
                        }
                        for (var item : HbmItems.FOUNDRY_ITEMS) {
                            if (item.get() instanceof FoundryMoldItem mold) {
                                mold.addCreativeVariants(output);
                            } else {
                                output.accept(item);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> MINING_PROCESSING = TABS.register(
            "mining_processing",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.mining_processing"))
                    .icon(() -> new ItemStack(HbmBlocks.MACHINE_EXCAVATOR.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.MINING_PROCESSING_BLOCKS) {
                            acceptBlock(output, block);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> MACHINES = TABS.register(
            "machines",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.machines"))
                    .icon(() -> new ItemStack(HbmBlocks.MACHINE_ELECTRIC_FURNACE_OFF.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.MACHINE_BLOCKS) {
                            // A number of 1.7.10 machines own specialised BlockItems, so
                            // DeferredBlock#asItem points at AIR rather than the actual item.
                            // Resolve the registered item by its shared registry id instead.
                            Item item = BuiltInRegistries.ITEM.get(block.getId());
                            if (item == Items.AIR) {
                                continue;
                            }
                            if (item instanceof LegacyVariantBlockItem variants) {
                                variants.addCreativeVariants(output);
                            } else {
                                output.accept(item);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> BUILDING = TABS.register(
            "building",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.building"))
                    .icon(() -> new ItemStack(HbmBlocks.BRICK_CONCRETE.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.BUILDING_BLOCKS) {
                            if (block.get().asItem() instanceof ConcreteColoredBlockItem concrete) {
                                concrete.addCreativeVariants(output);
                            } else if (block.get().asItem() instanceof MetalFenceBlockItem metalFence) {
                                metalFence.addCreativeVariants(output);
                            } else if (block.get().asItem() instanceof SellafieldBlockItem sellafield) {
                                sellafield.addCreativeVariants(output);
                            } else if (block.get().asItem() instanceof com.reinhardt.hbm.item.GlyphBlockItem glyph) {
                                glyph.addCreativeVariants(output);
                            } else if (block == HbmBlocks.BRICK_JUNGLE_TRAP
                                    && BuiltInRegistries.ITEM.get(block.getId()) instanceof com.reinhardt.hbm.item.TrapBlockItem trap) {
                                trap.addCreativeVariants(output);
                            } else if (block.get().asItem() instanceof com.reinhardt.hbm.item.CaveSpikeBlockItem spike) {
                                spike.addCreativeVariants(output);
                            } else if (block.get().asItem() instanceof LegacyVariantBlockItem variants) {
                                variants.addCreativeVariants(output);
                            } else if (block.get().asItem() instanceof com.reinhardt.hbm.item.CapBlockItem caps) {
                                caps.addCreativeVariants(output);
                            } else if (block.get().asItem() instanceof ConcreteColoredBlockItem variants) {
                                variants.addCreativeVariants(output);
                            } else {
                                acceptBlock(output, block);
                            }
                        }
                        for (var block : HbmBlocks.METEOR_BLOCKS) {
                            acceptBlock(output, block);
                        }
                        for (var block : LegacyHbmContent.BUILDING_BLOCKS) {
                            acceptBlock(output, block);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> DECORATION_APPLIANCES = TABS.register(
            "decoration_appliances",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.decoration_appliances"))
                    .icon(() -> new ItemStack(HbmBlocks.DECO_COMPUTER.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.DECORATION_APPLIANCE_BLOCKS) {
                            acceptDecorationApplianceBlock(output, block);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> LIGHTING = TABS.register(
            "lighting",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.lighting"))
                    .icon(() -> new ItemStack(HbmBlocks.LANTERN.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.LIGHTING_BLOCKS) {
                            acceptBlock(output, block);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> DOORS = TABS.register(
            "doors",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.doors"))
                    .icon(() -> new ItemStack(HbmBlocks.DOOR_METAL.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : HbmBlocks.DOOR_BLOCKS) {
                            acceptBlock(output, block);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> TOOLS = TABS.register(
            "tools",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.tools"))
                    .icon(() -> new ItemStack(HbmItems.SCREWDRIVER.get()))
                    .displayItems((parameters, output) -> {
                        for (var anvil : HbmBlocks.ANVIL_BLOCKS) {
                            acceptBlock(output, anvil);
                        }
                        for (var tool : HbmItems.TOOL_ITEMS) {
                            if (isLegacySpawnItem(tool)) {
                                continue;
                            }
                            if (tool.get() instanceof BlueprintItem blueprints) {
                                blueprints.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.ConveyorWandItem conveyorWand) {
                                conveyorWand.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.BlueprintFolderItem folders) {
                                folders.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.SirenTrackItem tracks) {
                                tracks.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.GuideBookItem guideBook) {
                                guideBook.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.LegacyHolotapeImageItem holotapes) {
                                holotapes.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.LegacyBombCallerItem bombCaller) {
                                bombCaller.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.LegacyMinecartItem carts) {
                                carts.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.LegacyDroneItem drones) {
                                drones.addCreativeVariants(output);
                            } else if (tool.get() instanceof com.reinhardt.hbm.item.LegacyTrainItem trains) {
                                trains.addCreativeVariants(output);
                            } else {
                                output.accept(tool);
                            }
                        }
                        if (HbmBlocks.WAND_STRUCTURE.asItem() instanceof com.reinhardt.hbm.item.WandStructureBlockItem item) {
                            item.addCreativeVariants(output::accept);
                        }
                        output.accept(HbmBlocks.WAND_AIR);
                        output.accept(HbmBlocks.WAND_JIGSAW);
                        output.accept(HbmBlocks.WAND_TANDEM);
                        output.accept(HbmBlocks.WAND_LOOT);
                        output.accept(HbmBlocks.WAND_LOGIC);
                        output.accept(HbmBlocks.DECO_LOOT);
                        output.accept(HbmBlocks.LOGIC_BLOCK);
                        output.accept(HbmBlocks.MACHINE_KEYFORGE);
                        output.accept(HbmItems.DOSIMETER);
                        output.accept(HbmItems.GEIGER_COUNTER);
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> ARMOR = TABS.register(
            "armor",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.armor"))
                    .icon(() -> new ItemStack(HbmItems.GAS_MASK_M65.get()))
                    .displayItems((parameters, output) -> {
                        for (var armor : HbmItems.ARMOR_ITEMS) {
                            output.accept(armor);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> SPAWN_EGGS = TABS.register(
            "spawn_eggs",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.spawn_eggs"))
                    .icon(() -> new ItemStack(HbmItems.SPAWN_GLYPHID.get()))
                    .displayItems((parameters, output) -> {
                        for (var egg : HbmItems.GLYPHID_SPAWN_EGGS) {
                            output.accept(egg);
                        }
                        for (var egg : HbmItems.MOB_SPAWN_EGGS) {
                            output.accept(egg);
                        }
                        // The 1.7.10 port also has four standalone entity
                        // spawners which are not SpawnEggItem instances.
                        for (var tool : HbmItems.TOOL_ITEMS) {
                            if (isLegacySpawnItem(tool)) {
                                outputLegacyItem(output, tool);
                            }
                        }
                        for (var item : HbmItems.PORTED_PLAIN_ITEMS) {
                            if (isLegacySpawnItem(item)) {
                                outputLegacyItem(output, item);
                            }
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> MISCELLANEOUS = TABS.register(
            "miscellaneous",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.miscellaneous"))
                    .icon(() -> new ItemStack(HbmItems.INGOT_ADVANCED_ALLOY.get()))
                    .displayItems((parameters, output) -> {
                        for (var item : HbmItems.PORTED_PLAIN_ITEMS) {
                            if (!HbmItems.isHiddenPortedPlainItem(item)
                                    && !isPetroleumLegacyItem(item)
                                    && !isLegacySpawnItem(item)) {
                                outputLegacyItem(output, item);
                            }
                        }
                        if (HbmBlocks.VENDING_MACHINE.get().asItem() instanceof com.reinhardt.hbm.item.VendingMachineBlockItem vending) {
                            vending.addCreativeVariants(output);
                        }
                    })
                    .build()
    );

    public static final Supplier<CreativeModeTab> LEGACY_BLOCKS = TABS.register(
            "legacy_blocks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.reinhardtshbm.legacy_blocks"))
                    .icon(() -> new ItemStack(HbmBlocks.REINFORCED_STONE.get()))
                    .displayItems((parameters, output) -> {
                        for (var block : LegacyHbmContent.LEGACY_BLOCKS) {
                            if (!isPetroleumLegacyBlock(block)) {
                                acceptBlock(output, block);
                            }
                        }
                    })
                    .build()
    );

    private HbmCreativeTabs() {
    }

    /** Hidden structure-only blocks have no item and must not abort tab/search rebuilding. */
    private static void acceptBlock(CreativeModeTab.Output output, DeferredBlock<? extends Block> block) {
        Item item = block.get().asItem();
        if (item != null && item != Items.AIR) {
            output.accept(item);
        }
    }

    private static void acceptDecorationApplianceBlock(CreativeModeTab.Output output,
                                                       DeferredBlock<? extends Block> block) {
        Item item = BuiltInRegistries.ITEM.get(block.getId());
        if (item == Items.AIR) {
            return;
        }
        if (item instanceof ToasterBlockItem toaster) {
            toaster.addCreativeVariants(output);
        } else if (item instanceof com.reinhardt.hbm.item.BobbleheadBlockItem bobblehead) {
            bobblehead.addCreativeVariants(output);
        } else if (item instanceof com.reinhardt.hbm.item.SnowglobeBlockItem snowglobe) {
            snowglobe.addCreativeVariants(output);
        } else if (item instanceof com.reinhardt.hbm.item.PlushieBlockItem plushie) {
            plushie.addCreativeVariants(output);
        } else if (item instanceof com.reinhardt.hbm.item.DecoCrtBlockItem crt) {
            crt.addCreativeVariants(output);
        } else if (item instanceof com.reinhardt.hbm.item.FilingCabinetBlockItem filingCabinet) {
            filingCabinet.addCreativeVariants(output);
        } else {
            output.accept(item);
        }
    }

    private static void outputLegacyItem(CreativeModeTab.Output output, net.neoforged.neoforge.registries.DeferredItem<Item> item) {
        if (item.get() instanceof com.reinhardt.hbm.item.LegacyConserveItem conserve) {
            conserve.addCreativeVariants(output);
        } else if (item.get() instanceof com.reinhardt.hbm.item.LegacyCrayonItem crayon) {
            crayon.addCreativeVariants(output);
        } else if (item.get() instanceof com.reinhardt.hbm.item.LegacySpecialFoodItem food) {
            food.addCreativeVariants(output);
        } else if (item.get() instanceof LegacyVariantItem variant) {
            variant.addCreativeVariants(output);
        } else if (item.get() instanceof com.reinhardt.hbm.item.UniversalGrenadeItem grenade) {
            grenade.addCreativeVariants(output);
        } else {
            output.accept(item);
        }
    }

    private static final Set<String> PETROLEUM_LEGACY_BLOCKS = Set.of(
            "machine_coker",
            "machine_diesel"
    );

    private static final Set<String> PETROLEUM_LEGACY_ITEMS = Set.of(
            "oil_detector",
            "scrap_oil"
    );

    private static final Set<String> LEGACY_SPAWN_ITEM_IDS = Set.of(
            "spawn_duck",
            "chopper",
            "spawn_ufo",
            "spawn_worm"
    );

    private static boolean isPetroleumBlock(DeferredBlock<Block> block) {
        return block == HbmBlocks.ORE_OIL
                || block == HbmBlocks.ORE_OIL_EMPTY
                || block == HbmBlocks.ORE_OIL_SAND
                || block == HbmBlocks.ORE_BEDROCK_OIL
                || block == HbmBlocks.ORE_DEEPSLATE_OIL
                || block == HbmBlocks.ORE_DEEPSLATE_OIL_EMPTY;
    }

    private static boolean isPetroleumLegacyBlock(DeferredBlock<Block> block) {
        return PETROLEUM_LEGACY_BLOCKS.contains(block.getId().getPath());
    }

    private static boolean isPetroleumItem(DeferredItem<Item> item) {
        return HbmItems.OIL_TAR_ITEMS.containsValue(item)
                || item == HbmItems.CANISTER_LUBRICANT
                || item == HbmItems.SOLID_FUEL
                || item == HbmItems.SOLID_FUEL_BF
                || item == HbmItems.ROCKET_FUEL
                || HbmItems.FUEL_ADDITIVE_ITEMS.containsValue(item);
    }

    private static boolean isPetroleumLegacyItem(DeferredItem<Item> item) {
        return PETROLEUM_LEGACY_ITEMS.contains(item.getId().getPath());
    }

    private static boolean isLegacySpawnItem(DeferredItem<Item> item) {
        return LEGACY_SPAWN_ITEM_IDS.contains(item.getId().getPath());
    }

    private static boolean isNuclearBilletItem(DeferredItem<Item> item) {
        return HbmItems.NUCLEAR_BILLETS.contains(item);
    }

    private static void acceptFluidContainerVariants(CreativeModeTab.Output output, HbmFluidContainerItem container) {
        if (!container.isFilledContainer()) {
            output.accept(container);
            return;
        }
        for (var definition : HbmFluids.niceOrder()) {
            if (container.kind().allows(definition)) {
                output.accept(container.filledStack(definition));
            }
        }
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
