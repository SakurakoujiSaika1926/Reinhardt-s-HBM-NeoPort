package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.AmmoArtyItem;
import com.reinhardt.hbm.item.ArcElectrodeBurntItem;
import com.reinhardt.hbm.item.ArcElectrodeItem;
import com.reinhardt.hbm.item.AmmoHimarsItem;
import com.reinhardt.hbm.item.AmsCatalystItem;
import com.reinhardt.hbm.item.AmsCoreItem;
import com.reinhardt.hbm.item.AmsLensItem;
import com.reinhardt.hbm.item.ArmorModItem;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BedrockOreBaseItem;
import com.reinhardt.hbm.item.BedrockOreFragmentItem;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.item.BladesItem;
import com.reinhardt.hbm.item.BlueprintFolderItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.BrokenItem;
import com.reinhardt.hbm.item.ChemistrySetItem;
import com.reinhardt.hbm.item.CombustionPistonSetItem;
import com.reinhardt.hbm.item.CounterfeitKeyKitItem;
import com.reinhardt.hbm.item.ConveyorWandItem;
import com.reinhardt.hbm.item.DrillbitItem;
import com.reinhardt.hbm.item.DefuserItem;
import com.reinhardt.hbm.item.HbmAxeItem;
import com.reinhardt.hbm.item.ArmorFSBItem;
import com.reinhardt.hbm.item.ArtilleryDesignatorItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.HbmPickaxeItem;
import com.reinhardt.hbm.item.HbmShovelItem;
import com.reinhardt.hbm.item.HbmSwordItem;
import com.reinhardt.hbm.item.HbmToolBehavior;
import com.reinhardt.hbm.item.HbmToolProfile;
import com.reinhardt.hbm.item.HbmToolTier;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.item.IcfPelletItem;
import com.reinhardt.hbm.item.InfiniteBatteryItem;
import com.reinhardt.hbm.item.FixedFluidBarrelBlockItem;
import com.reinhardt.hbm.item.TankSteelItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.FelCrystalItem;
import com.reinhardt.hbm.item.GasMaskFilterItem;
import com.reinhardt.hbm.item.GasMaskItem;
import com.reinhardt.hbm.item.HbmFluidDuctItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.LittleBoyKitItem;
import com.reinhardt.hbm.item.KeyPinItem;
import com.reinhardt.hbm.item.LockItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.item.MeteorCharmItem;
import com.reinhardt.hbm.item.MeteorRemoteItem;
import com.reinhardt.hbm.item.MirrorToolItem;
import com.reinhardt.hbm.item.NuclearWasteItem;
import com.reinhardt.hbm.item.OilDetectorItem;
import com.reinhardt.hbm.item.OilTarItem;
import com.reinhardt.hbm.item.PACoilItem;
import com.reinhardt.hbm.item.ParticleCapsuleItem;
import com.reinhardt.hbm.item.PileRodItem;
import com.reinhardt.hbm.item.PlateFuelItem;
import com.reinhardt.hbm.item.PollutionDetectorItem;
import com.reinhardt.hbm.item.PwrFuelItem;
import com.reinhardt.hbm.item.RadarLinkerItem;
import com.reinhardt.hbm.item.ReactorSensorItem;
import com.reinhardt.hbm.item.PwrPrinterItem;
import com.reinhardt.hbm.item.RawIngotItem;
import com.reinhardt.hbm.item.RangefinderItem;
import com.reinhardt.hbm.item.RagItem;
import com.reinhardt.hbm.item.RadiationSurveyItem;
import com.reinhardt.hbm.item.RbmkConsoleLinkerItem;
import com.reinhardt.hbm.item.RbmkFuelRodItem;
import com.reinhardt.hbm.item.RbmkLidItem;
import com.reinhardt.hbm.item.RtgPelletItem;
import com.reinhardt.hbm.item.RtgDepletedPelletItem;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.item.SatelliteChipItem;
import com.reinhardt.hbm.item.SirenTrackItem;
import com.reinhardt.hbm.item.StampItem;
import com.reinhardt.hbm.item.SettingsToolItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.item.StructureWandItem;
import com.reinhardt.hbm.item.TemplateFolderItem;
import com.reinhardt.hbm.item.TeleLinkItem;
import com.reinhardt.hbm.item.TurretAmmoItem;
import com.reinhardt.hbm.item.TurretBiometryItem;
import com.reinhardt.hbm.item.WatzPelletItem;
import com.reinhardt.hbm.item.WasteFuelItem;
import com.reinhardt.hbm.item.WiringRedCopperItem;
import com.reinhardt.hbm.item.ZirnoxRodItem;
import com.reinhardt.hbm.foundry.FoundryShape;
import net.minecraft.core.Holder;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import com.reinhardt.hbm.util.Wavelength;

public final class HbmItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ReinhardtsHBM.MOD_ID);

    private static final Set<String> CORE_ITEM_IDS = new LinkedHashSet<>();

    public static final List<DeferredItem<Item>> INGOT_MATERIALS = new ArrayList<>();
    public static final List<DeferredItem<Item>> NUGGET_MATERIALS = new ArrayList<>();
    public static final List<DeferredItem<Item>> POWDER_MATERIALS = new ArrayList<>();
    public static final List<DeferredItem<Item>> PLATE_MATERIALS = new ArrayList<>();
    public static final List<DeferredItem<Item>> MISC_MATERIALS = new ArrayList<>();
    public static final List<DeferredItem<Item>> ORE_DROPS = new ArrayList<>();
    public static final List<DeferredItem<Item>> RAW_ORES = new ArrayList<>();
    public static final List<DeferredItem<Item>> MINERAL_CRYSTALS = new ArrayList<>();
    public static final List<DeferredItem<Item>> MACHINE_COMPONENTS = new ArrayList<>();
    public static final List<DeferredItem<Item>> TOOL_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> FOUNDRY_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> ARMOR_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> FLUID_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> REACTOR_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> FUEL_ROD_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> RBMK_REACTOR_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> RBMK_FUEL_ROD_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> NUCLEAR_BILLETS = new ArrayList<>();
    public static final List<DeferredItem<Item>> NUCLEAR_WEAPON_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> TURRET_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> SATELLITE_ITEMS = new ArrayList<>();

    public static final List<String> BREEDING_ROD_VARIANTS = List.of(
            "lithium",
            "tritium",
            "co",
            "co60",
            "th232",
            "thf",
            "u235",
            "np237",
            "u238",
            "pu238",
            "pu239",
            "rgp",
            "waste",
            "lead",
            "uranium",
            "ra226",
            "ac227"
    );

    public static final DeferredItem<Item> INGOT_URANIUM = ingot("ingot_uranium");
    public static final DeferredItem<Item> INGOT_U235 = ingot("ingot_u235");
    public static final DeferredItem<Item> INGOT_U238 = ingot("ingot_u238");
    public static final DeferredItem<Item> INGOT_PLUTONIUM = ingot("ingot_plutonium", Rarity.UNCOMMON);
    public static final DeferredItem<Item> INGOT_SCHRABIDIUM = ingot("ingot_schrabidium", Rarity.RARE);
    public static final DeferredItem<Item> INGOT_STEEL = ingot("ingot_steel");
    public static final DeferredItem<Item> INGOT_TH232 = ingot("ingot_th232");
    public static final DeferredItem<Item> INGOT_ALUMINIUM = ingot("ingot_aluminium");
    public static final DeferredItem<Item> INGOT_COPPER = ingot("ingot_copper");
    public static final DeferredItem<Item> INGOT_TITANIUM = ingot("ingot_titanium");
    public static final DeferredItem<Item> INGOT_TUNGSTEN = ingot("ingot_tungsten");
    public static final DeferredItem<Item> INGOT_TUNGSTEN_CARBIDE = ingot("ingot_tungsten_carbide");
    public static final DeferredItem<Item> INGOT_LEAD = ingot("ingot_lead");
    public static final DeferredItem<Item> INGOT_COBALT = ingot("ingot_cobalt");
    public static final DeferredItem<Item> INGOT_BORON = ingot("ingot_boron");
    public static final DeferredItem<Item> INGOT_BERYLLIUM = ingot("ingot_beryllium");
    public static final DeferredItem<Item> INGOT_ASBESTOS = ingot("ingot_asbestos");
    public static final DeferredItem<Item> ARC_ELECTRODE = machineComponent(
            "arc_electrode",
            () -> new ArcElectrodeItem(new Item.Properties())
    );
    public static final DeferredItem<Item> ARC_ELECTRODE_BURNT = machineComponent(
            "arc_electrode_burnt",
            () -> new ArcElectrodeBurntItem(new Item.Properties())
    );
    public static final DeferredItem<Item> INGOT_GRAPHITE = ingot("ingot_graphite");
    public static final DeferredItem<Item> INGOT_ADVANCED_ALLOY = ingot("ingot_advanced_alloy", Rarity.UNCOMMON);
    public static final DeferredItem<Item> INGOT_AU198 = ingot("ingot_au198");
    public static final DeferredItem<Item> NUGGET_AU198 = material(NUGGET_MATERIALS, "nugget_au198");
    public static final DeferredItem<Item> POWDER_AU198 = material(POWDER_MATERIALS, "powder_au198");
    public static final DeferredItem<Item> POWDER_AU198_TINY = material(POWDER_MATERIALS, "powder_au198_tiny");
    public static final DeferredItem<Item> BILLET_AU198 = material(NUCLEAR_BILLETS, "billet_au198");
    public static final DeferredItem<Item> INGOT_RAW = material(
            INGOT_MATERIALS,
            "ingot_raw",
            () -> new RawIngotItem(new Item.Properties())
    );
    public static final DeferredItem<Item> FALLOUT_ITEM = material(MISC_MATERIALS, "falloutitem");
    // ItemCustomLore in 1.7.10; its turbofan level-100 easter egg is functional.
    public static final DeferredItem<Item> FLAME_PONY = material(MISC_MATERIALS, "flame_pony");
    public static final DeferredItem<Item> POWDER_ASH = material(
            POWDER_MATERIALS,
            "powder_ash",
            () -> new LegacyVariantItem(new Item.Properties(), "powder_ash", LegacyVariantItem.variants(
                    "wood",
                    "coal",
                    "misc",
                    "fly",
                    "soot",
                    "fullerene"
            ))
    );

    public static final DeferredItem<Item> WIRE_FINE = material(
            MISC_MATERIALS,
            "wire_fine",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.WIRE)
    );
    public static final DeferredItem<Item> WIRE_DENSE = material(
            MISC_MATERIALS,
            "wire_dense",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.DENSE_WIRE)
    );
    public static final DeferredItem<Item> PIPE = material(MISC_MATERIALS, "pipe");
    public static final DeferredItem<Item> PIPE_LEAD = material(MISC_MATERIALS, "pipe_lead");
    public static final DeferredItem<Item> PIPE_STEEL = material(MISC_MATERIALS, "pipe_steel");
    public static final DeferredItem<Item> PIPE_DURA_STEEL = material(MISC_MATERIALS, "pipe_dura_steel");
    public static final DeferredItem<Item> TANK_STEEL = material(
            MISC_MATERIALS,
            "tank_steel",
            () -> new TankSteelItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> BOLT = material(MISC_MATERIALS, "bolt");
    public static final DeferredItem<Item> BOLT_DURA_STEEL = material(MISC_MATERIALS, "bolt_dura_steel");
    public static final DeferredItem<Item> BOLT_TUNGSTEN = material(MISC_MATERIALS, "bolt_tungsten");
    public static final DeferredItem<Item> COIL_COPPER = material(MISC_MATERIALS, "coil_copper");
    public static final DeferredItem<Item> COIL_COPPER_TORUS = material(MISC_MATERIALS, "coil_copper_torus");
    public static final DeferredItem<Item> CIRCUIT_VACUUM_TUBE = material(MISC_MATERIALS, "circuit_vacuum_tube");
    public static final DeferredItem<Item> CIRCUIT_CAPACITOR = material(MISC_MATERIALS, "circuit_capacitor");
    public static final DeferredItem<Item> CIRCUIT_PCB = material(MISC_MATERIALS, "circuit_pcb");
    public static final DeferredItem<Item> CIRCUIT_NUMITRON = material(MISC_MATERIALS, "circuit_numitron");
    public static final DeferredItem<Item> CIRCUIT_ATOMIC_CLOCK = material(MISC_MATERIALS, "circuit_atomic_clock");
    public static final DeferredItem<Item> CIRCUIT_BASIC = material(MISC_MATERIALS, "circuit_basic");
    public static final DeferredItem<Item> CIRCUIT_CHIP = material(MISC_MATERIALS, "circuit_chip");
    public static final DeferredItem<Item> CIRCUIT_CHIP_BISMOID = material(MISC_MATERIALS, "circuit_chip_bismoid");
    public static final DeferredItem<Item> CIRCUIT_CHIP_QUANTUM = material(MISC_MATERIALS, "circuit_chip_quantum");
    public static final DeferredItem<Item> CIRCUIT_SILICON = material(MISC_MATERIALS, "circuit_silicon");
    public static final DeferredItem<Item> CIRCUIT_ANALOG = material(MISC_MATERIALS, "circuit_analog");
    public static final DeferredItem<Item> CIRCUIT_ADVANCED = material(MISC_MATERIALS, "circuit_advanced");
    public static final DeferredItem<Item> CIRCUIT_BISMOID = material(MISC_MATERIALS, "circuit_bismoid");
    public static final DeferredItem<Item> CIRCUIT_QUANTUM = material(MISC_MATERIALS, "circuit_quantum");
    public static final DeferredItem<Item> CIRCUIT_CAPACITOR_BOARD = material(MISC_MATERIALS, "circuit_capacitor_board");
    public static final DeferredItem<Item> CIRCUIT_CAPACITOR_TANTALIUM = material(MISC_MATERIALS, "circuit_capacitor_tantalium");
    public static final DeferredItem<Item> CIRCUIT_CONTROLLER_CHASSIS = material(MISC_MATERIALS, "circuit_controller_chassis");
    public static final DeferredItem<Item> CIRCUIT_CONTROLLER = material(MISC_MATERIALS, "circuit_controller");
    public static final DeferredItem<Item> CIRCUIT_CONTROLLER_ADVANCED = material(MISC_MATERIALS, "circuit_controller_advanced");
    public static final DeferredItem<Item> CIRCUIT_CONTROLLER_QUANTUM = material(MISC_MATERIALS, "circuit_controller_quantum");
    public static final DeferredItem<Item> CRT_DISPLAY = material(MISC_MATERIALS, "crt_display");
    public static final DeferredItem<Item> MOTOR = material(MISC_MATERIALS, "motor");
    public static final DeferredItem<Item> MOTOR_DESH = material(MISC_MATERIALS, "motor_desh");
    public static final DeferredItem<Item> FILTER_COAL = material(MISC_MATERIALS, "filter_coal");
    public static final DeferredItem<Item> RAG = material(MISC_MATERIALS, "rag", () -> new RagItem(new Item.Properties()));
    public static final DeferredItem<Item> RAG_DAMP = material(MISC_MATERIALS, "rag_damp");
    public static final DeferredItem<Item> RAG_PISS = material(MISC_MATERIALS, "rag_piss");
    public static final DeferredItem<Item> CATALYST_CLAY = material(MISC_MATERIALS, "catalyst_clay");
    public static final DeferredItem<Item> DEUTERIUM_FILTER = material(MISC_MATERIALS, "deuterium_filter");
    public static final DeferredItem<Item> HAZMAT_CLOTH = material(MISC_MATERIALS, "hazmat_cloth");
    public static final DeferredItem<Item> HAZMAT_CLOTH_RED = material(MISC_MATERIALS, "hazmat_cloth_red");
    public static final DeferredItem<Item> HAZMAT_CLOTH_GREY = material(MISC_MATERIALS, "hazmat_cloth_grey");
    public static final DeferredItem<Item> SPHERE_STEEL = material(MISC_MATERIALS, "sphere_steel");
    public static final DeferredItem<Item> THERMO_ELEMENT = material(MACHINE_COMPONENTS, "thermo_element");
    public static final DeferredItem<Item> PISTON_SELENIUM = material(MACHINE_COMPONENTS, "piston_selenium");
    public static final DeferredItem<Item> PISTON_SET = material(
            MACHINE_COMPONENTS,
            "piston_set",
            () -> new CombustionPistonSetItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> SHELL = material(
            MISC_MATERIALS,
            "shell",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.SHELL)
    );
    public static final DeferredItem<Item> PART_MECHANISM = material(
            MACHINE_COMPONENTS,
            "part_mechanism",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.MECHANISM)
    );
    public static final DeferredItem<Item> CANISTER_LUBRICANT = material(MISC_MATERIALS, "canister_lubricant");
    public static final DeferredItem<Item> BIOMASS = material(MISC_MATERIALS, "biomass");
    public static final DeferredItem<Item> BIOMASS_COMPRESSED = material(MISC_MATERIALS, "biomass_compressed");
    public static final DeferredItem<Item> BIO_WAFER = material(MISC_MATERIALS, "bio_wafer");
    public static final DeferredItem<Item> PLANT_ITEM = material(
            MISC_MATERIALS,
            "plant_item",
            () -> new LegacyVariantItem(new Item.Properties(), "plant_item", LegacyVariantItem.variants(
                    "tobacco",
                    "rope",
                    "mustardwillow"
            ))
    );
    public static final DeferredItem<Item> SOLID_FUEL = material(MISC_MATERIALS, "solid_fuel");
    public static final DeferredItem<Item> SOLID_FUEL_BF = material(MISC_MATERIALS, "solid_fuel_bf");
    public static final DeferredItem<Item> ROCKET_FUEL = material(MISC_MATERIALS, "rocket_fuel");
    public static final DeferredItem<Item> CORDITE = material(MISC_MATERIALS, "cordite");
    public static final DeferredItem<Item> BALLISTITE = material(MISC_MATERIALS, "ballistite");
    public static final DeferredItem<Item> BALL_DYNAMITE = material(MISC_MATERIALS, "ball_dynamite");
    public static final DeferredItem<Item> BALL_TNT = material(MISC_MATERIALS, "ball_tnt");
    public static final DeferredItem<Item> BALL_TATB = material(MISC_MATERIALS, "ball_tatb");
    public static final DeferredItem<Item> BALL_FIRECLAY = material(MISC_MATERIALS, "ball_fireclay");
    public static final DeferredItem<Item> PLATE_POLYMER = material(MISC_MATERIALS, "plate_polymer");
    public static final DeferredItem<Item> PELLET_BUCKSHOT = material(MISC_MATERIALS, "pellet_buckshot");
    public static final DeferredItem<Item> DUCTTAPE = material(MISC_MATERIALS, "ducttape");
    public static final DeferredItem<Item> ASSEMBLY_NUKE = material(
            NUCLEAR_WEAPON_ITEMS,
            "assembly_nuke",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> NEUTRON_REFLECTOR = material(MACHINE_COMPONENTS, "neutron_reflector");
    public static final DeferredItem<Item> FINS_SMALL_STEEL = material(MACHINE_COMPONENTS, "fins_small_steel");
    public static final DeferredItem<Item> BOY_SHIELDING = material(NUCLEAR_WEAPON_ITEMS, "boy_shielding", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOY_TARGET = material(NUCLEAR_WEAPON_ITEMS, "boy_target", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BOY_BULLET = material(NUCLEAR_WEAPON_ITEMS, "boy_bullet", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BOY_PROPELLANT = material(NUCLEAR_WEAPON_ITEMS, "boy_propellant", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOY_IGNITER = material(NUCLEAR_WEAPON_ITEMS, "boy_igniter", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOY_KIT = material(NUCLEAR_WEAPON_ITEMS, "boy_kit", () -> new LittleBoyKitItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CHOCOLATE = material(MISC_MATERIALS, "chocolate");
    public static final DeferredItem<Item> PELLET_CHARGED = material(MISC_MATERIALS, "pellet_charged");
    public static final DeferredItem<Item> PA_COIL = material(
            MACHINE_COMPONENTS,
            "pa_coil",
            () -> new PACoilItem(new Item.Properties())
    );
    public static final DeferredItem<Item> PARTICLE_EMPTY = particle("particle_empty", false);
    public static final DeferredItem<Item> PARTICLE_COPPER = particle("particle_copper", true);
    public static final DeferredItem<Item> PARTICLE_LEAD = particle("particle_lead", true);
    public static final DeferredItem<Item> PARTICLE_HYDROGEN = particle("particle_hydrogen", true);
    public static final DeferredItem<Item> PARTICLE_ANTIMATTER = particle("particle_amat", true);
    public static final DeferredItem<Item> PARTICLE_ANTIELECTRON = particle("particle_aelectron", true);
    public static final DeferredItem<Item> PARTICLE_ANTIPROTON = particle("particle_aproton", true);
    public static final DeferredItem<Item> PARTICLE_ANTISCHRABIDIUM = particle("particle_aschrab", true);
    public static final DeferredItem<Item> PARTICLE_DARK = particle("particle_dark", true);
    public static final DeferredItem<Item> PARTICLE_MUON = particle("particle_muon", true);
    public static final DeferredItem<Item> PARTICLE_HIGGS = particle("particle_higgs", true);
    public static final DeferredItem<Item> PARTICLE_TACHYON = particle("particle_tachyon", true);
    public static final DeferredItem<Item> PARTICLE_STRANGE = particle("particle_strange", true);
    public static final DeferredItem<Item> PARTICLE_SPARKTICLE = particle("particle_sparkticle", true);
    public static final DeferredItem<Item> PARTICLE_DIGAMMA = particle("particle_digamma", true);
    public static final DeferredItem<Item> PARTICLE_LUTECE = particle("particle_lutece", true);
    public static final DeferredItem<Item> GEM_TANTALIUM = material(MISC_MATERIALS, "gem_tantalium");
    public static final DeferredItem<Item> EGG_BALEFIRE_SHARD = material(MISC_MATERIALS, "egg_balefire_shard");
    public static final DeferredItem<Item> GLYPHID_MEAT = material(MISC_MATERIALS, "glyphid_meat");
    public static final DeferredItem<Item> GLYPHID_MEAT_GRILLED = material(MISC_MATERIALS, "glyphid_meat_grilled");
    public static final DeferredItem<Item> LASER_CRYSTAL_CO2 = machineComponent("laser_crystal_co2", () -> new FelCrystalItem(new Item.Properties(), Wavelength.IR));
    public static final DeferredItem<Item> LASER_CRYSTAL_BISMUTH = machineComponent("laser_crystal_bismuth", () -> new FelCrystalItem(new Item.Properties(), Wavelength.VISIBLE));
    public static final DeferredItem<Item> LASER_CRYSTAL_CMB = machineComponent("laser_crystal_cmb", () -> new FelCrystalItem(new Item.Properties(), Wavelength.UV));
    public static final DeferredItem<Item> LASER_CRYSTAL_BALE = machineComponent("laser_crystal_bale", () -> new FelCrystalItem(new Item.Properties(), Wavelength.GAMMA));
    public static final DeferredItem<Item> LASER_CRYSTAL_DIGAMMA = machineComponent("laser_crystal_digamma", () -> new FelCrystalItem(new Item.Properties(), Wavelength.DRX));
    public static final DeferredItem<Item> COKE = material(
            MISC_MATERIALS,
            "coke",
            () -> new LegacyVariantItem(new Item.Properties(), "coke", LegacyVariantItem.variants(
                    "coal",
                    "lignite",
                    "petroleum"
            ))
    );
    public static final DeferredItem<Item> OIL_TAR = material(
            MISC_MATERIALS,
            "oil_tar",
            () -> new OilTarItem(new Item.Properties())
    );
    public static final DeferredItem<Item> BATTERY_PACK = material(
            MACHINE_COMPONENTS,
            "battery_pack",
            () -> new BatteryPackItem(new Item.Properties(), LegacyVariantItem.variants(
                    "battery_redstone",
                    "battery_lead",
                    "battery_lithium",
                    "battery_sodium",
                    "battery_schrabidium",
                    "battery_quantum",
                    "capacitor_copper",
                    "capacitor_gold",
                    "capacitor_niobium",
                    "capacitor_tantalum",
                    "capacitor_bismuth",
                    "capacitor_spark"
            ))
    );
    public static final DeferredItem<Item> BATTERY_CREATIVE = material(
            MACHINE_COMPONENTS,
            "battery_creative",
            () -> new InfiniteBatteryItem(new Item.Properties().rarity(Rarity.EPIC))
    );
    public static final DeferredItem<Item> TURRET_CHIP = turretItem(
            "turret_chip",
            () -> new TurretBiometryItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> TURRET_BIOMETRY = turretItem(
            "turret_biometry",
            () -> new TurretBiometryItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> AMMO_SHELL = turretItem(
            "ammo_shell",
            () -> new TurretAmmoItem(new Item.Properties())
    );
    public static final DeferredItem<Item> AMMO_STANDARD = turretItem(
            "ammo_standard",
            () -> new StandardAmmoItem(new Item.Properties())
    );
    public static final DeferredItem<Item> DEFUSER = toolItem("defuser", () -> new DefuserItem(new Item.Properties().durability(100)));
    public static final DeferredItem<Item> AMMO_DGK = turretItem(
            "ammo_dgk",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> AMMO_ARTY = turretItem(
            "ammo_arty",
            () -> new AmmoArtyItem(new Item.Properties())
    );
    public static final DeferredItem<Item> AMMO_HIMARS = turretItem(
            "ammo_himars",
            () -> new AmmoHimarsItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> FUEL_ADDITIVE = material(
            MISC_MATERIALS,
            "fuel_additive",
            () -> new LegacyVariantItem(new Item.Properties(), "fuel_additive", LegacyVariantItem.variants(
                    "antiknock",
                    "deicer"
            ))
    );
    public static final DeferredItem<Item> PART_GENERIC = material(
            MACHINE_COMPONENTS,
            "part_generic",
            () -> new LegacyVariantItem(new Item.Properties(), "part_generic", LegacyVariantItem.variants(
                    "piston_pneumatic",
                    "piston_hydraulic",
                    "piston_electric",
                    "lde",
                    "hde",
                    "glass_polarized"
            ))
    );
    public static final DeferredItem<Item> PELLET_GAS = machineComponent(
            "pellet_gas",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> CHLORINE_PINWHEEL = machineComponent(
            "chlorine_pinwheel",
            () -> new InfiniteFluidContainerItem(new Item.Properties(), "chlorine", 1)
    );
    public static final DeferredItem<Item> FLUID_ICON = fluidItem(
            "fluid_icon",
            () -> new FluidIconItem(new Item.Properties())
    );
    public static final DeferredItem<Item> FLUID_IDENTIFIER_MULTI = fluidItem(
            "fluid_identifier_multi",
            () -> new FluidIdentifierItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> INF_WATER = fluidItem(
            "inf_water",
            () -> new InfiniteFluidContainerItem(new Item.Properties().stacksTo(1), "water", 50)
    );
    public static final DeferredItem<Item> INF_WATER_MK2 = fluidItem(
            "inf_water_mk2",
            () -> new InfiniteFluidContainerItem(new Item.Properties().stacksTo(1), "water", 500)
    );
    public static final DeferredItem<Item> FLUID_BARREL_INFINITE = fluidItem(
            "fluid_barrel_infinite",
            () -> new InfiniteFluidContainerItem(new Item.Properties().stacksTo(1), null, 1_000_000_000)
    );
    public static final DeferredItem<Item> CANISTER_EMPTY = fluidItem(
            "canister_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.CANISTER, false)
    );
    public static final DeferredItem<Item> CANISTER_FULL = fluidItem(
            "canister_full",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.CANISTER, true)
    );
    public static final DeferredItem<Item> GAS_EMPTY = fluidItem(
            "gas_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.GAS_TANK, false)
    );
    public static final DeferredItem<Item> GAS_FULL = fluidItem(
            "gas_full",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.GAS_TANK, true)
    );
    public static final DeferredItem<Item> FLUID_TANK_EMPTY = fluidItem(
            "fluid_tank_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.FLUID_TANK, false)
    );
    public static final DeferredItem<Item> FLUID_TANK_FULL = fluidItem(
            "fluid_tank_full",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.FLUID_TANK, true)
    );
    public static final DeferredItem<Item> FLUID_TANK_LEAD_EMPTY = fluidItem(
            "fluid_tank_lead_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.LEAD_TANK, false)
    );
    public static final DeferredItem<Item> FLUID_TANK_LEAD_FULL = fluidItem(
            "fluid_tank_lead_full",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.LEAD_TANK, true)
    );
    public static final DeferredItem<Item> FLUID_BARREL_EMPTY = fluidItem(
            "fluid_barrel_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.FLUID_BARREL, false)
    );
    public static final DeferredItem<Item> FLUID_BARREL_FULL = fluidItem(
            "fluid_barrel_full",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.FLUID_BARREL, true)
    );
    public static final DeferredItem<Item> RED_BARREL_ITEM = coreItem(
            "red_barrel",
            () -> new FixedFluidBarrelBlockItem(HbmBlocks.RED_BARREL.get(), new Item.Properties(), "diesel")
    );
    public static final DeferredItem<Item> PINK_BARREL_ITEM = coreItem(
            "pink_barrel",
            () -> new FixedFluidBarrelBlockItem(HbmBlocks.PINK_BARREL.get(), new Item.Properties(), "kerosene")
    );
    public static final DeferredItem<Item> LOX_BARREL_ITEM = coreItem(
            "lox_barrel",
            () -> new FixedFluidBarrelBlockItem(HbmBlocks.LOX_BARREL.get(), new Item.Properties(), "oxygen")
    );
    public static final DeferredItem<Item> TAINT_BARREL_ITEM = coreItem(
            "taint_barrel",
            () -> new BlockItem(HbmBlocks.TAINT_BARREL.get(), new Item.Properties())
    );
    public static final DeferredItem<Item> YELLOW_BARREL_ITEM = coreItem(
            "yellow_barrel",
            () -> new BlockItem(HbmBlocks.YELLOW_BARREL.get(), new Item.Properties())
    );
    public static final DeferredItem<Item> VITRIFIED_BARREL_ITEM = coreItem(
            "vitrified_barrel",
            () -> new BlockItem(HbmBlocks.VITRIFIED_BARREL.get(), new Item.Properties())
    );
    public static final DeferredItem<Item> FLUID_PACK_EMPTY = fluidItem(
            "fluid_pack_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.FLUID_PACK, false)
    );
    public static final DeferredItem<Item> FLUID_PACK_FULL = fluidItem(
            "fluid_pack_full",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.FLUID_PACK, true)
    );
    public static final DeferredItem<Item> DISPERSER_CANISTER_EMPTY = fluidItem(
            "disperser_canister_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.DISPERSER, false)
    );
    public static final DeferredItem<Item> DISPERSER_CANISTER = fluidItem(
            "disperser_canister",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.DISPERSER, true)
    );
    public static final DeferredItem<Item> GLYPHID_GLAND_EMPTY = fluidItem(
            "glyphid_gland_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.GLYPHID_GLAND, false)
    );
    public static final DeferredItem<Item> GLYPHID_GLAND = fluidItem(
            "glyphid_gland",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.GLYPHID_GLAND, true)
    );
    public static final DeferredItem<Item> CELL_EMPTY = fluidItem(
            "cell_empty",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.CELL, false)
    );
    public static final DeferredItem<Item> CELL_TRITIUM = fluidItem(
            "cell_tritium",
            () -> new HbmFluidContainerItem(new Item.Properties(), HbmFluidContainerItem.Kind.CELL, true)
    );
    public static final DeferredItem<Item> FF_FLUID_DUCT = fluidItem(
            "ff_fluid_duct",
            () -> new HbmFluidDuctItem(new Item.Properties())
    );
    public static final DeferredItem<Item> PLATE_CAST = material(
            PLATE_MATERIALS,
            "plate_cast",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.CAST_PLATE)
    );
    public static final DeferredItem<Item> PLATE_WELDED = material(
            PLATE_MATERIALS,
            "plate_welded",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.WELDED_PLATE)
    );
    public static final DeferredItem<Item> SCRAPS = material(
            MISC_MATERIALS,
            "scraps",
            () -> new ScrapsItem(new Item.Properties())
    );
    public static final DeferredItem<Item> SCRAP = material(MISC_MATERIALS, "scrap");
    public static final DeferredItem<Item> DUST = material(POWDER_MATERIALS, "dust");
    public static final DeferredItem<Item> DUST_TINY = material(POWDER_MATERIALS, "dust_tiny");

    public static final DeferredItem<Item> SULFUR = oreDrop("sulfur");
    public static final DeferredItem<Item> NITER = oreDrop("niter");
    public static final DeferredItem<Item> FLUORITE = oreDrop("fluorite");
    public static final DeferredItem<Item> LIGNITE = oreDrop("lignite");
    public static final DeferredItem<Item> CINNABAR = oreDrop("cinnabar");
    public static final DeferredItem<Item> CHUNK_ORE = oreDrop(
            "chunk_ore",
            () -> new LegacyVariantItem(new Item.Properties(), "chunk_ore", LegacyVariantItem.variants(
                    "rare",
                    "malachite",
                    "moonstone"
            ))
    );
    public static final DeferredItem<Item> CHUNK_ORE_CRYOLITE = oreDrop("chunk_ore_cryolite");
    public static final DeferredItem<Item> BEDROCK_ORE_NEW = oreDrop(
            "bedrock_ore_new",
            () -> new BedrockOreItem(new Item.Properties())
    );
    public static final DeferredItem<Item> BEDROCK_ORE_BASE = oreDrop(
            "bedrock_ore_base",
            () -> new BedrockOreBaseItem(new Item.Properties())
    );
    public static final DeferredItem<Item> BEDROCK_ORE_FRAGMENT = oreDrop(
            "bedrock_ore_fragment",
            () -> new BedrockOreFragmentItem(new Item.Properties())
    );
    public static final DeferredItem<Item> CRYSTAL_ALUMINIUM = mineralCrystal("crystal_aluminium");
    public static final DeferredItem<Item> CRYSTAL_COPPER = mineralCrystal("crystal_copper");
    public static final DeferredItem<Item> CRYSTAL_IRON = mineralCrystal("crystal_iron");
    public static final DeferredItem<Item> CRYSTAL_TITANIUM = mineralCrystal("crystal_titanium");
    public static final DeferredItem<Item> CRYSTAL_TUNGSTEN = mineralCrystal("crystal_tungsten");
    public static final DeferredItem<Item> FRAGMENT_COBALT = oreDrop("fragment_cobalt");
    public static final DeferredItem<Item> FRAGMENT_COLTAN = oreDrop("fragment_coltan");
    public static final DeferredItem<Item> LITHIUM = ingot("lithium");
    public static final DeferredItem<Item> TRINITITE = oreDrop("trinitite");
    public static final DeferredItem<Item> NUCLEAR_WASTE = material(MISC_MATERIALS, "nuclear_waste");
    public static final DeferredItem<Item> NUCLEAR_WASTE_TINY = material(MISC_MATERIALS, "nuclear_waste_tiny");
    public static final DeferredItem<Item> NUCLEAR_WASTE_VITRIFIED = material(MISC_MATERIALS, "nuclear_waste_vitrified");
    public static final DeferredItem<Item> NUCLEAR_WASTE_VITRIFIED_TINY = material(MISC_MATERIALS, "nuclear_waste_vitrified_tiny");
    public static final DeferredItem<Item> NUCLEAR_WASTE_LONG = nuclearWaste("nuclear_waste_long", NuclearWasteItem.Family.LONG, false, false);
    public static final DeferredItem<Item> NUCLEAR_WASTE_LONG_TINY = nuclearWaste("nuclear_waste_long_tiny", NuclearWasteItem.Family.LONG, false, true);
    public static final DeferredItem<Item> NUCLEAR_WASTE_LONG_DEPLETED = nuclearWaste("nuclear_waste_long_depleted", NuclearWasteItem.Family.LONG, true, false);
    public static final DeferredItem<Item> NUCLEAR_WASTE_LONG_DEPLETED_TINY = nuclearWaste("nuclear_waste_long_depleted_tiny", NuclearWasteItem.Family.LONG, true, true);
    public static final DeferredItem<Item> NUCLEAR_WASTE_SHORT = nuclearWaste("nuclear_waste_short", NuclearWasteItem.Family.SHORT, false, false);
    public static final DeferredItem<Item> NUCLEAR_WASTE_SHORT_TINY = nuclearWaste("nuclear_waste_short_tiny", NuclearWasteItem.Family.SHORT, false, true);
    public static final DeferredItem<Item> NUCLEAR_WASTE_SHORT_DEPLETED = nuclearWaste("nuclear_waste_short_depleted", NuclearWasteItem.Family.SHORT, true, false);
    public static final DeferredItem<Item> NUCLEAR_WASTE_SHORT_DEPLETED_TINY = nuclearWaste("nuclear_waste_short_depleted_tiny", NuclearWasteItem.Family.SHORT, true, true);
    public static final DeferredItem<Item> SCRAP_NUCLEAR = material(MISC_MATERIALS, "scrap_nuclear");
    public static final DeferredItem<Item> GEM_RAD = material(MISC_MATERIALS, "gem_rad");
    public static final DeferredItem<Item> PELLET_RTG_DEPLETED = material(MISC_MATERIALS, "pellet_rtg_depleted",
            () -> new RtgDepletedPelletItem(new Item.Properties()));
    public static final DeferredItem<Item> PELLET_RTG_RADIUM = rtgPellet("pellet_rtg_radium", 3, 11_520_000_000L, "lead");
    public static final DeferredItem<Item> PELLET_RTG_WEAK = rtgPellet("pellet_rtg_weak", 5, 720_000_000L, "lead");
    public static final DeferredItem<Item> PELLET_RTG = rtgPellet("pellet_rtg", 10, 631_440_000L, "lead");
    public static final DeferredItem<Item> PELLET_RTG_STRONTIUM = rtgPellet("pellet_rtg_strontium", 15, 208_800_000L, "zirconium");
    public static final DeferredItem<Item> PELLET_RTG_COBALT = rtgPellet("pellet_rtg_cobalt", 15, 38_160_000L, "nickel");
    public static final DeferredItem<Item> PELLET_RTG_ACTINIUM = rtgPellet("pellet_rtg_actinium", 20, 156_960_000L, "lead");
    public static final DeferredItem<Item> PELLET_RTG_AMERICIUM = rtgPellet("pellet_rtg_americium", 20, 3_384_000_000L, "neptunium");
    public static final DeferredItem<Item> PELLET_RTG_POLONIUM = rtgPellet("pellet_rtg_polonium", 50, 9_936_000L, "lead");
    public static final DeferredItem<Item> PELLET_RTG_GOLD = rtgPellet("pellet_rtg_gold", 100, 194_400L, "mercury");
    public static final DeferredItem<Item> PELLET_RTG_LEAD = rtgPellet("pellet_rtg_lead", 200, 21_600L, "bismuth");

    public static final DeferredItem<Item> DOSIMETER = coreItem(
            "dosimeter",
            () -> new RadiationSurveyItem(new Item.Properties().stacksTo(1), 0x9dd36d, "dosimeter")
    );
    public static final DeferredItem<Item> GEIGER_COUNTER = coreItem(
            "geiger_counter",
            () -> new RadiationSurveyItem(new Item.Properties().stacksTo(1), ChatFormatting.GREEN.getColor(), "geiger_counter")
    );
    public static final DeferredItem<Item> WIRING_RED_COPPER = coreItem(
            "wiring_red_copper",
            () -> new WiringRedCopperItem(new Item.Properties())
    );
    public static final DeferredItem<Item> RBMK_FUEL_EMPTY = rbmkFuelRodItem("rbmk_fuel_empty");
    public static final DeferredItem<Item> RBMK_FUEL = rbmkFuelRodItem(
            "rbmk_fuel",
            () -> new RbmkFuelRodItem(new Item.Properties())
    );
    public static final DeferredItem<Item> RBMK_LID = rbmkReactorItem(
            "rbmk_lid",
            () -> new RbmkLidItem(new Item.Properties(), com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity.LidType.NORMAL)
    );
    public static final DeferredItem<Item> RBMK_LID_GLASS = rbmkReactorItem(
            "rbmk_lid_glass",
            () -> new RbmkLidItem(new Item.Properties(), com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity.LidType.GLASS)
    );
    public static final DeferredItem<Item> ROD_EMPTY = fuelRodItem("rod_empty");
    public static final DeferredItem<Item> ROD_DUAL_EMPTY = fuelRodItem("rod_dual_empty");
    public static final DeferredItem<Item> ROD_QUAD_EMPTY = fuelRodItem("rod_quad_empty");
    public static final DeferredItem<Item> ROD = fuelRodItem(
            "rod",
            () -> new LegacyVariantItem(
                    new Item.Properties(),
                    "rod",
                    LegacyVariantItem.variants(BREEDING_ROD_VARIANTS.toArray(String[]::new))
            )
    );
    public static final DeferredItem<Item> ROD_DUAL = fuelRodItem(
            "rod_dual",
            () -> new LegacyVariantItem(
                    new Item.Properties(),
                    "rod_dual",
                    LegacyVariantItem.variants(BREEDING_ROD_VARIANTS.toArray(String[]::new))
            )
    );
    public static final DeferredItem<Item> ROD_QUAD = fuelRodItem(
            "rod_quad",
            () -> new LegacyVariantItem(
                    new Item.Properties(),
                    "rod_quad",
                    LegacyVariantItem.variants(BREEDING_ROD_VARIANTS.toArray(String[]::new))
            )
    );
    public static final DeferredItem<Item> ROD_ZIRNOX_EMPTY = fuelRodItem("rod_zirnox_empty");
    public static final DeferredItem<Item> ROD_ZIRNOX = fuelRodItem(
            "rod_zirnox",
            () -> new ZirnoxRodItem(new Item.Properties())
    );
    public static final DeferredItem<Item> ROD_ZIRNOX_DEPLETED = fuelRodItem(
            "rod_zirnox_depleted",
            () -> new LegacyVariantItem(new Item.Properties().stacksTo(1), "rod_zirnox_depleted", LegacyVariantItem.variants(
                    "natural_uranium_fuel",
                    "uranium_fuel",
                    "thorium_fuel",
                    "mox_fuel",
                    "plutonium_fuel",
                    "u233_fuel",
                    "u235_fuel",
                    "les_fuel",
                    "zfb_mox_fuel"
            ))
    );
    public static final DeferredItem<Item> ROD_ZIRNOX_TRITIUM = fuelRodItem("rod_zirnox_tritium");
    public static final DeferredItem<Item> PWR_FUEL = fuelRodItem(
            "pwr_fuel",
            () -> new PwrFuelItem(new Item.Properties())
    );
    public static final DeferredItem<Item> PWR_FUEL_HOT = fuelRodItem(
            "pwr_fuel_hot",
            () -> new PwrFuelItem(new Item.Properties())
    );
    public static final DeferredItem<Item> PWR_FUEL_DEPLETED = fuelRodItem(
            "pwr_fuel_depleted",
            () -> new PwrFuelItem(new Item.Properties())
    );
    public static final DeferredItem<Item> PWR_PRINTER = reactorItem(
            "pwr_printer",
            () -> new PwrPrinterItem(new Item.Properties())
    );
    public static final DeferredItem<Item> PILE_ROD_URANIUM = reactorItem(
            "pile_rod_uranium",
            () -> new PileRodItem(new Item.Properties(), PileRodItem.Kind.URANIUM)
    );
    public static final DeferredItem<Item> PILE_ROD_PU239 = reactorItem(
            "pile_rod_pu239",
            () -> new PileRodItem(new Item.Properties(), PileRodItem.Kind.PU239)
    );
    public static final DeferredItem<Item> PILE_ROD_PLUTONIUM = reactorItem(
            "pile_rod_plutonium",
            () -> new PileRodItem(new Item.Properties(), PileRodItem.Kind.PLUTONIUM)
    );
    public static final DeferredItem<Item> PILE_ROD_SOURCE = reactorItem(
            "pile_rod_source",
            () -> new PileRodItem(new Item.Properties(), PileRodItem.Kind.SOURCE)
    );
    public static final DeferredItem<Item> PILE_ROD_BORON = reactorItem(
            "pile_rod_boron",
            () -> new PileRodItem(new Item.Properties(), PileRodItem.Kind.BORON)
    );
    public static final DeferredItem<Item> PILE_ROD_LITHIUM = reactorItem(
            "pile_rod_lithium",
            () -> new PileRodItem(new Item.Properties(), PileRodItem.Kind.LITHIUM)
    );
    public static final DeferredItem<Item> PILE_ROD_DETECTOR = reactorItem(
            "pile_rod_detector",
            () -> new PileRodItem(new Item.Properties(), PileRodItem.Kind.DETECTOR)
    );
    public static final DeferredItem<Item> AMS_LENS = reactorItem(
            "ams_lens",
            () -> new AmsLensItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> AMS_CORE_SING = reactorItem(
            "ams_core_sing",
            () -> new AmsCoreItem(new Item.Properties().stacksTo(1), AmsCoreItem.Kind.SING)
    );
    public static final DeferredItem<Item> AMS_CORE_WORMHOLE = reactorItem(
            "ams_core_wormhole",
            () -> new AmsCoreItem(new Item.Properties().stacksTo(1), AmsCoreItem.Kind.WORMHOLE)
    );
    public static final DeferredItem<Item> AMS_CORE_EYEOFHARMONY = reactorItem(
            "ams_core_eyeofharmony",
            () -> new AmsCoreItem(new Item.Properties().stacksTo(1), AmsCoreItem.Kind.EYEOFHARMONY)
    );
    public static final DeferredItem<Item> AMS_CORE_THINGY = reactorItem(
            "ams_core_thingy",
            () -> new AmsCoreItem(new Item.Properties().stacksTo(1), AmsCoreItem.Kind.THINGY)
    );
    public static final DeferredItem<Item> AMS_CATALYST_BLANK = reactorItem("ams_catalyst_blank", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> AMS_CATALYST_ALUMINIUM = amsCatalyst("ams_catalyst_aluminium", 0xCCCCCC, 1_000_000L, 1.15F, 0.85F, 1.15F);
    public static final DeferredItem<Item> AMS_CATALYST_BERYLLIUM = amsCatalyst("ams_catalyst_beryllium", 0x97978B, 0L, 1.25F, 0.95F, 1.05F);
    public static final DeferredItem<Item> AMS_CATALYST_CAESIUM = amsCatalyst("ams_catalyst_caesium", 0x6400FF, 2_500_000L, 1.00F, 0.85F, 1.15F);
    public static final DeferredItem<Item> AMS_CATALYST_CERIUM = amsCatalyst("ams_catalyst_cerium", 0x1D3FFF, 1_000_000L, 1.15F, 1.15F, 0.85F);
    public static final DeferredItem<Item> AMS_CATALYST_COBALT = amsCatalyst("ams_catalyst_cobalt", 0x789BBE, 0L, 1.25F, 1.05F, 0.95F);
    public static final DeferredItem<Item> AMS_CATALYST_COPPER = amsCatalyst("ams_catalyst_copper", 0xAADE29, 0L, 1.25F, 1.00F, 1.00F);
    public static final DeferredItem<Item> AMS_CATALYST_DINEUTRONIUM = amsCatalyst("ams_catalyst_dineutronium", 0x334077, 2_500_000L, 1.00F, 1.15F, 0.85F);
    public static final DeferredItem<Item> AMS_CATALYST_EUPHEMIUM = amsCatalyst("ams_catalyst_euphemium", 0xFF9CD2, 2_500_000L, 1.00F, 1.00F, 1.00F);
    public static final DeferredItem<Item> AMS_CATALYST_IRON = amsCatalyst("ams_catalyst_iron", 0xFF7E22, 1_000_000L, 1.15F, 0.95F, 1.05F);
    public static final DeferredItem<Item> AMS_CATALYST_LITHIUM = amsCatalyst("ams_catalyst_lithium", 0xFF2727, 0L, 1.25F, 0.85F, 1.15F);
    public static final DeferredItem<Item> AMS_CATALYST_NIOBIUM = amsCatalyst("ams_catalyst_niobium", 0x3BF1B6, 1_000_000L, 1.15F, 1.05F, 0.95F);
    public static final DeferredItem<Item> AMS_CATALYST_SCHRABIDIUM = amsCatalyst("ams_catalyst_schrabidium", 0x32FFFF, 2_500_000L, 1.00F, 1.05F, 0.95F);
    public static final DeferredItem<Item> AMS_CATALYST_STRONTIUM = amsCatalyst("ams_catalyst_strontium", 0xDD0D35, 1_000_000L, 1.15F, 1.00F, 1.00F);
    public static final DeferredItem<Item> AMS_CATALYST_THORIUM = amsCatalyst("ams_catalyst_thorium", 0x653B22, 2_500_000L, 1.00F, 0.95F, 1.05F);
    public static final DeferredItem<Item> AMS_CATALYST_TUNGSTEN = amsCatalyst("ams_catalyst_tungsten", 0xF5FF48, 0L, 1.25F, 1.15F, 0.85F);
    public static final DeferredItem<Item> SINGULARITY_SPARK = material(
            MISC_MATERIALS,
            "singularity_spark",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> ICF_PELLET_EMPTY = fuelRodItem(
            "icf_pellet_empty",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> ICF_PELLET = fuelRodItem(
            "icf_pellet",
            () -> new IcfPelletItem(new Item.Properties())
    );
    public static final DeferredItem<Item> ICF_PELLET_DEPLETED = fuelRodItem(
            "icf_pellet_depleted",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> WATZ_PELLET = fuelRodItem(
            "watz_pellet",
            () -> new WatzPelletItem(new Item.Properties(), false)
    );
    public static final DeferredItem<Item> WATZ_PELLET_DEPLETED = fuelRodItem(
            "watz_pellet_depleted",
            () -> new WatzPelletItem(new Item.Properties(), true)
    );
    public static final DeferredItem<Item> WASTE_NATURAL_URANIUM = wasteFuelItem("waste_natural_uranium");
    public static final DeferredItem<Item> WASTE_URANIUM = wasteFuelItem("waste_uranium");
    public static final DeferredItem<Item> WASTE_THORIUM = wasteFuelItem("waste_thorium");
    public static final DeferredItem<Item> WASTE_MOX = wasteFuelItem("waste_mox");
    public static final DeferredItem<Item> WASTE_PLUTONIUM = wasteFuelItem("waste_plutonium");
    public static final DeferredItem<Item> WASTE_U233 = wasteFuelItem("waste_u233");
    public static final DeferredItem<Item> WASTE_U235 = wasteFuelItem("waste_u235");
    public static final DeferredItem<Item> WASTE_SCHRABIDIUM = wasteFuelItem("waste_schrabidium");
    public static final DeferredItem<Item> WASTE_ZFB_MOX = wasteFuelItem("waste_zfb_mox");
    public static final DeferredItem<Item> PLATE_FUEL_U233 = fuelRodItem(
            "plate_fuel_u233",
            () -> new PlateFuelItem(new Item.Properties(), 2_200_000, PlateFuelItem.Function.SQUARE_ROOT, 50)
    );
    public static final DeferredItem<Item> PLATE_FUEL_U235 = fuelRodItem(
            "plate_fuel_u235",
            () -> new PlateFuelItem(new Item.Properties(), 2_200_000, PlateFuelItem.Function.SQUARE_ROOT, 40)
    );
    public static final DeferredItem<Item> PLATE_FUEL_MOX = fuelRodItem(
            "plate_fuel_mox",
            () -> new PlateFuelItem(new Item.Properties(), 2_400_000, PlateFuelItem.Function.LOGARITHM, 50)
    );
    public static final DeferredItem<Item> PLATE_FUEL_PU239 = fuelRodItem(
            "plate_fuel_pu239",
            () -> new PlateFuelItem(new Item.Properties(), 2_000_000, PlateFuelItem.Function.NEGATIVE_QUADRATIC, 50)
    );
    public static final DeferredItem<Item> PLATE_FUEL_SA326 = fuelRodItem(
            "plate_fuel_sa326",
            () -> new PlateFuelItem(new Item.Properties(), 2_000_000, PlateFuelItem.Function.LINEAR, 80)
    );
    public static final DeferredItem<Item> PLATE_FUEL_RA226BE = fuelRodItem(
            "plate_fuel_ra226be",
            () -> new PlateFuelItem(new Item.Properties(), 1_300_000, PlateFuelItem.Function.PASSIVE, 30)
    );
    public static final DeferredItem<Item> PLATE_FUEL_PU238BE = fuelRodItem(
            "plate_fuel_pu238be",
            () -> new PlateFuelItem(new Item.Properties(), 1_000_000, PlateFuelItem.Function.PASSIVE, 50)
    );
    public static final DeferredItem<Item> WASTE_PLATE_U233 = wasteFuelItem("waste_plate_u233");
    public static final DeferredItem<Item> WASTE_PLATE_U235 = wasteFuelItem("waste_plate_u235");
    public static final DeferredItem<Item> WASTE_PLATE_MOX = wasteFuelItem("waste_plate_mox");
    public static final DeferredItem<Item> WASTE_PLATE_PU239 = wasteFuelItem("waste_plate_pu239");
    public static final DeferredItem<Item> WASTE_PLATE_SA326 = wasteFuelItem("waste_plate_sa326");
    public static final DeferredItem<Item> WASTE_PLATE_RA226BE = wasteFuelItem("waste_plate_ra226be");
    public static final DeferredItem<Item> WASTE_PLATE_PU238BE = wasteFuelItem("waste_plate_pu238be");
    public static final DeferredItem<Item> CHEMISTRY_SET = toolItem(
            "chemistry_set",
            () -> new ChemistrySetItem(new Item.Properties().stacksTo(1).durability(100))
    );
    public static final DeferredItem<Item> CHEMISTRY_SET_BORON = toolItem(
            "chemistry_set_boron",
            () -> new ChemistrySetItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<Item> BLADES_STEEL = machineComponent(
            "blades_steel",
            () -> new BladesItem(200)
    );
    public static final DeferredItem<Item> BLADES_TITANIUM = machineComponent(
            "blades_titanium",
            () -> new BladesItem(350)
    );
    public static final DeferredItem<Item> BLADES_ADVANCED_ALLOY = machineComponent(
            "blades_advanced_alloy",
            () -> new BladesItem(700)
    );
    public static final DeferredItem<Item> BLADES_DESH = machineComponent(
            "blades_desh",
            () -> new BladesItem(0)
    );
    public static final DeferredItem<Item> UPGRADE_TEMPLATE = machineComponent("upgrade_template");
    public static final DeferredItem<Item> PART_PISTON_HYDRAULIC = machineComponent(
            "part_piston_hydraulic",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> CENTRIFUGE_ELEMENT = machineComponent(
            "centrifuge_element",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> GEAR_LARGE = machineComponent(
            "gear_large",
            () -> new LegacyVariantItem(new Item.Properties(), "gear_large", LegacyVariantItem.variants(
                    "normal",
                    "steel"
            ))
    );
    public static final DeferredItem<Item> DRILL_TITANIUM = machineComponent(
            "drill_titanium",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> DRILLBIT = machineComponent(
            "drillbit",
            () -> new DrillbitItem(new Item.Properties())
    );
    public static final DeferredItem<Item> CATALYTIC_CONVERTER = machineComponent(
            "catalytic_converter",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> ITEM_EXPENSIVE = machineComponent(
            "item_expensive",
            () -> new LegacyVariantItem(new Item.Properties(), "item_expensive", LegacyVariantItem.variants(
                    "steel_plating",
                    "heavy_frame",
                    "circuit",
                    "lead_plating",
                    "ferro_plating",
                    "computer",
                    "bronze_tubes",
                    "plastic",
                    "gold_dust",
                    "degenerate_matter"
            ))
    );

    public static final DeferredItem<Item> SCREWDRIVER = toolItem(
            "screwdriver",
            () -> new ScrewdriverItem(new Item.Properties().durability(100))
    );
    public static final DeferredItem<Item> SCREWDRIVER_DESH = toolItem(
            "screwdriver_desh",
            () -> new ScrewdriverItem(new Item.Properties())
    );
    public static final DeferredItem<Item> CONVEYOR_WAND = toolItem(
            "conveyor_wand",
            () -> new ConveyorWandItem(new Item.Properties())
    );
    public static final DeferredItem<Item> TEMPLATE_FOLDER = toolItem(
            "template_folder",
            () -> new TemplateFolderItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> SIREN_TRACK = toolItem(
            "siren_track",
            () -> new SirenTrackItem(new Item.Properties())
    );
    public static final DeferredItem<Item> BLUEPRINTS = toolItem(
            "blueprints",
            () -> new BlueprintItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> BLUEPRINT_FOLDER = toolItem(
            "blueprint_folder",
            () -> new BlueprintFolderItem(new Item.Properties())
    );
    public static final DeferredItem<Item> BROKEN_ITEM = machineComponent(
            "broken_item",
            () -> new BrokenItem(new Item.Properties())
    );
    public static final DeferredItem<Item> SETTINGS_TOOL = toolItem(
            "settings_tool",
            () -> new SettingsToolItem(new Item.Properties())
    );
    public static final DeferredItem<Item> MIRROR_TOOL = toolItem(
            "mirror_tool",
            () -> new MirrorToolItem(new Item.Properties())
    );
    public static final DeferredItem<Item> RBMK_TOOL = rbmkReactorItem(
            "rbmk_tool",
            () -> new RbmkConsoleLinkerItem(new Item.Properties())
    );
    public static final DeferredItem<Item> REACTOR_SENSOR = reactorItem(
            "reactor_sensor",
            () -> new ReactorSensorItem(new Item.Properties())
    );
    public static final DeferredItem<Item> OIL_DETECTOR = toolItem(
            "oil_detector",
            () -> new OilDetectorItem(new Item.Properties())
    );
    public static final DeferredItem<Item> RANGEFINDER = toolItem(
            "rangefinder",
            () -> new RangefinderItem(new Item.Properties())
    );
    public static final DeferredItem<Item> DESIGNATOR_ARTY_RANGE = toolItem(
            "designator_arty_range",
            () -> new ArtilleryDesignatorItem(new Item.Properties())
    );
    public static final DeferredItem<Item> POLLUTION_DETECTOR = toolItem(
            "pollution_detector",
            () -> new PollutionDetectorItem(new Item.Properties())
    );
    public static final DeferredItem<Item> STRUCTURE_WAND = toolItem(
            "wand_s",
            () -> new StructureWandItem(new Item.Properties())
    );
    public static final DeferredItem<Item> KEY = toolItem(
            "key",
            () -> new KeyPinItem(new Item.Properties())
    );
    public static final DeferredItem<Item> KEY_FAKE = toolItem(
            "key_fake",
            () -> new KeyPinItem(new Item.Properties(), false)
    );
    public static final DeferredItem<Item> KEY_KIT = toolItem(
            "key_kit",
            () -> new CounterfeitKeyKitItem(new Item.Properties())
    );
    public static final DeferredItem<Item> PIN = toolItem(
            "pin",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> PADLOCK_RUSTY = toolItem(
            "padlock_rusty",
            () -> new LockItem(new Item.Properties(), 1.0D)
    );
    public static final DeferredItem<Item> PADLOCK = toolItem(
            "padlock",
            () -> new LockItem(new Item.Properties(), 0.1D)
    );
    public static final DeferredItem<Item> PADLOCK_REINFORCED = toolItem(
            "padlock_reinforced",
            () -> new LockItem(new Item.Properties(), 0.02D)
    );
    public static final DeferredItem<Item> PADLOCK_UNBREAKABLE = toolItem(
            "padlock_unbreakable",
            () -> new LockItem(new Item.Properties(), 0.0D)
    );
    public static final DeferredItem<Item> METEOR_REMOTE = toolItem(
            "meteor_remote",
            () -> new MeteorRemoteItem(new Item.Properties().stacksTo(1).durability(2))
    );
    public static final DeferredItem<Item> METEOR_CHARM = toolItem(
            "meteor_charm",
            () -> new MeteorCharmItem(new Item.Properties().stacksTo(1), MeteorCharmItem.Kind.METEOR)
    );
    public static final DeferredItem<Item> PROTECTION_CHARM = toolItem(
            "protection_charm",
            () -> new MeteorCharmItem(new Item.Properties().stacksTo(1), MeteorCharmItem.Kind.PROTECTION)
    );
    public static final DeferredItem<Item> MOLD_BASE = foundryItem(
            "mold_base",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> MOLD = foundryItem(
            "mold",
            () -> new FoundryMoldItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<Item> GAS_MASK = armorItem(
            "gas_mask",
            () -> new GasMaskItem(HbmArmorMaterials.STEEL, GasMaskItem.Kind.STANDARD, new Item.Properties().stacksTo(1).durability(165))
    );
    public static final DeferredItem<Item> GAS_MASK_M65 = armorItem(
            "gas_mask_m65",
            () -> new GasMaskItem(HbmArmorMaterials.STEEL, GasMaskItem.Kind.M65, new Item.Properties().stacksTo(1).durability(165))
    );
    public static final DeferredItem<Item> GAS_MASK_OLDE = armorItem(
            "gas_mask_olde",
            () -> new GasMaskItem(HbmArmorMaterials.STEEL, GasMaskItem.Kind.OLDE, new Item.Properties().stacksTo(1).durability(165))
    );
    public static final DeferredItem<Item> GAS_MASK_MONO = armorItem(
            "gas_mask_mono",
            () -> new GasMaskItem(HbmArmorMaterials.STEEL, GasMaskItem.Kind.MONO, new Item.Properties().stacksTo(1).durability(165))
    );
    public static final DeferredItem<Item> GAS_MASK_FILTER = armorItem(
            "gas_mask_filter",
            () -> new GasMaskFilterItem(new Item.Properties().stacksTo(1).durability(20000))
    );
    public static final DeferredItem<Item> GAS_MASK_FILTER_MONO = armorItem(
            "gas_mask_filter_mono",
            () -> new GasMaskFilterItem(new Item.Properties().stacksTo(1).durability(20000))
    );
    public static final DeferredItem<Item> GAS_MASK_FILTER_COMBO = armorItem(
            "gas_mask_filter_combo",
            () -> new GasMaskFilterItem(new Item.Properties().stacksTo(1).durability(20000))
    );
    public static final DeferredItem<Item> GAS_MASK_FILTER_RAG = armorItem(
            "gas_mask_filter_rag",
            () -> new GasMaskFilterItem(new Item.Properties().stacksTo(1).durability(20000))
    );
    public static final DeferredItem<Item> GAS_MASK_FILTER_PISS = armorItem(
            "gas_mask_filter_piss",
            () -> new GasMaskFilterItem(new Item.Properties().stacksTo(1).durability(20000))
    );
    public static final DeferredItem<Item> SERVO_SET = armorItem(
            "servo_set",
            () -> ArmorModItem.servos(new Item.Properties())
    );
    public static final DeferredItem<Item> SERVO_SET_DESH = armorItem(
            "servo_set_desh",
            () -> ArmorModItem.servos(new Item.Properties())
    );
    public static final DeferredItem<Item> CLADDING_PAINT = armorItem(
            "cladding_paint",
            () -> ArmorModItem.cladding(new Item.Properties())
    );
    public static final DeferredItem<Item> CLADDING_RUBBER = armorItem(
            "cladding_rubber",
            () -> ArmorModItem.cladding(new Item.Properties())
    );
    public static final DeferredItem<Item> CLADDING_LEAD = armorItem(
            "cladding_lead",
            () -> ArmorModItem.cladding(new Item.Properties())
    );
    public static final DeferredItem<Item> CLADDING_DESH = armorItem(
            "cladding_desh",
            () -> ArmorModItem.cladding(new Item.Properties())
    );
    public static final DeferredItem<Item> CLADDING_GHIORSIUM = armorItem(
            "cladding_ghiorsium",
            () -> ArmorModItem.cladding(new Item.Properties())
    );
    public static final DeferredItem<Item> CLADDING_IRON = armorItem(
            "cladding_iron",
            () -> ArmorModItem.cladding(new Item.Properties())
    );
    public static final DeferredItem<Item> CLADDING_OBSIDIAN = armorItem(
            "cladding_obsidian",
            () -> ArmorModItem.cladding(new Item.Properties())
    );
    public static final DeferredItem<Item> INSERT_KEVLAR = armorItem(
            "insert_kevlar",
            () -> ArmorModItem.kevlar(new Item.Properties().durability(1500))
    );
    public static final DeferredItem<Item> ARMOR_BATTERY = armorItem(
            "armor_battery",
            () -> ArmorModItem.battery(new Item.Properties())
    );
    public static final DeferredItem<Item> ARMOR_BATTERY_MK2 = armorItem(
            "armor_battery_mk2",
            () -> ArmorModItem.battery(new Item.Properties())
    );
    public static final DeferredItem<Item> ARMOR_BATTERY_MK3 = armorItem(
            "armor_battery_mk3",
            () -> ArmorModItem.battery(new Item.Properties())
    );

    public static final DeferredItem<Item> HAZMAT_HELMET = fsbArmor("hazmat_helmet", "hazmat", HbmArmorMaterials.HAZMAT, ArmorItem.Type.HELMET, 5, false);
    public static final DeferredItem<Item> HAZMAT_PLATE = fsbArmor("hazmat_plate", "hazmat", HbmArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE, 5, false);
    public static final DeferredItem<Item> HAZMAT_LEGS = fsbArmor("hazmat_legs", "hazmat", HbmArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS, 5, false);
    public static final DeferredItem<Item> HAZMAT_BOOTS = fsbArmor("hazmat_boots", "hazmat", HbmArmorMaterials.HAZMAT, ArmorItem.Type.BOOTS, 5, false);

    public static final DeferredItem<Item> HAZMAT_HELMET_RED = fsbArmor("hazmat_helmet_red", "hazmat_red", HbmArmorMaterials.HAZMAT_RED, ArmorItem.Type.HELMET, 10, false);
    public static final DeferredItem<Item> HAZMAT_PLATE_RED = fsbArmor("hazmat_plate_red", "hazmat_red", HbmArmorMaterials.HAZMAT_RED, ArmorItem.Type.CHESTPLATE, 10, false);
    public static final DeferredItem<Item> HAZMAT_LEGS_RED = fsbArmor("hazmat_legs_red", "hazmat_red", HbmArmorMaterials.HAZMAT_RED, ArmorItem.Type.LEGGINGS, 10, false);
    public static final DeferredItem<Item> HAZMAT_BOOTS_RED = fsbArmor("hazmat_boots_red", "hazmat_red", HbmArmorMaterials.HAZMAT_RED, ArmorItem.Type.BOOTS, 10, false);

    public static final DeferredItem<Item> HAZMAT_HELMET_GREY = fsbArmor("hazmat_helmet_grey", "hazmat_grey", HbmArmorMaterials.HAZMAT_GREY, ArmorItem.Type.HELMET, 15, false);
    public static final DeferredItem<Item> HAZMAT_PLATE_GREY = fsbArmor("hazmat_plate_grey", "hazmat_grey", HbmArmorMaterials.HAZMAT_GREY, ArmorItem.Type.CHESTPLATE, 15, false);
    public static final DeferredItem<Item> HAZMAT_LEGS_GREY = fsbArmor("hazmat_legs_grey", "hazmat_grey", HbmArmorMaterials.HAZMAT_GREY, ArmorItem.Type.LEGGINGS, 15, false);
    public static final DeferredItem<Item> HAZMAT_BOOTS_GREY = fsbArmor("hazmat_boots_grey", "hazmat_grey", HbmArmorMaterials.HAZMAT_GREY, ArmorItem.Type.BOOTS, 15, false);

    public static final DeferredItem<Item> HAZMAT_PAA_HELMET = fsbArmor("hazmat_paa_helmet", "hazmat_paa", HbmArmorMaterials.PAA, ArmorItem.Type.HELMET, 75, false);
    public static final DeferredItem<Item> HAZMAT_PAA_PLATE = fsbArmor("hazmat_paa_plate", "hazmat_paa", HbmArmorMaterials.PAA, ArmorItem.Type.CHESTPLATE, 75, false);
    public static final DeferredItem<Item> HAZMAT_PAA_LEGS = fsbArmor("hazmat_paa_legs", "hazmat_paa", HbmArmorMaterials.PAA, ArmorItem.Type.LEGGINGS, 75, false);
    public static final DeferredItem<Item> HAZMAT_PAA_BOOTS = fsbArmor("hazmat_paa_boots", "hazmat_paa", HbmArmorMaterials.PAA, ArmorItem.Type.BOOTS, 75, false);

    public static final DeferredItem<Item> PAA_PLATE = fsbArmor(
            "paa_plate", "paa", HbmArmorMaterials.PAA, ArmorItem.Type.CHESTPLATE, 75, true,
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 30, 0)
    );
    public static final DeferredItem<Item> PAA_LEGS = fsbArmor(
            "paa_legs", "paa", HbmArmorMaterials.PAA, ArmorItem.Type.LEGGINGS, 75, true,
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 30, 0)
    );
    public static final DeferredItem<Item> PAA_BOOTS = fsbArmor(
            "paa_boots", "paa", HbmArmorMaterials.PAA, ArmorItem.Type.BOOTS, 75, true,
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 30, 0)
    );

    public static final DeferredItem<Item> ASBESTOS_HELMET = fsbArmor("asbestos_helmet", "asbestos", HbmArmorMaterials.ASBESTOS, ArmorItem.Type.HELMET, 20, false);
    public static final DeferredItem<Item> ASBESTOS_PLATE = fsbArmor("asbestos_plate", "asbestos", HbmArmorMaterials.ASBESTOS, ArmorItem.Type.CHESTPLATE, 20, false);
    public static final DeferredItem<Item> ASBESTOS_LEGS = fsbArmor("asbestos_legs", "asbestos", HbmArmorMaterials.ASBESTOS, ArmorItem.Type.LEGGINGS, 20, false);
    public static final DeferredItem<Item> ASBESTOS_BOOTS = fsbArmor("asbestos_boots", "asbestos", HbmArmorMaterials.ASBESTOS, ArmorItem.Type.BOOTS, 20, false);

    public static final DeferredItem<Item> STEEL_HELMET = fsbArmor("steel_helmet", "steel", HbmArmorMaterials.STEEL, ArmorItem.Type.HELMET, 20, false);
    public static final DeferredItem<Item> STEEL_PLATE = fsbArmor("steel_plate", "steel", HbmArmorMaterials.STEEL, ArmorItem.Type.CHESTPLATE, 20, false);
    public static final DeferredItem<Item> STEEL_LEGS = fsbArmor("steel_legs", "steel", HbmArmorMaterials.STEEL, ArmorItem.Type.LEGGINGS, 20, false);
    public static final DeferredItem<Item> STEEL_BOOTS = fsbArmor("steel_boots", "steel", HbmArmorMaterials.STEEL, ArmorItem.Type.BOOTS, 20, false);

    public static final DeferredItem<Item> TITANIUM_HELMET = fsbArmor("titanium_helmet", "titanium", HbmArmorMaterials.TITANIUM, ArmorItem.Type.HELMET, 25, false);
    public static final DeferredItem<Item> TITANIUM_PLATE = fsbArmor("titanium_plate", "titanium", HbmArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, 25, false);
    public static final DeferredItem<Item> TITANIUM_LEGS = fsbArmor("titanium_legs", "titanium", HbmArmorMaterials.TITANIUM, ArmorItem.Type.LEGGINGS, 25, false);
    public static final DeferredItem<Item> TITANIUM_BOOTS = fsbArmor("titanium_boots", "titanium", HbmArmorMaterials.TITANIUM, ArmorItem.Type.BOOTS, 25, false);

    public static final DeferredItem<Item> ALLOY_HELMET = fsbArmor("alloy_helmet", "alloy", HbmArmorMaterials.ALLOY, ArmorItem.Type.HELMET, 40, false);
    public static final DeferredItem<Item> ALLOY_PLATE = fsbArmor("alloy_plate", "alloy", HbmArmorMaterials.ALLOY, ArmorItem.Type.CHESTPLATE, 40, false);
    public static final DeferredItem<Item> ALLOY_LEGS = fsbArmor("alloy_legs", "alloy", HbmArmorMaterials.ALLOY, ArmorItem.Type.LEGGINGS, 40, false);
    public static final DeferredItem<Item> ALLOY_BOOTS = fsbArmor("alloy_boots", "alloy", HbmArmorMaterials.ALLOY, ArmorItem.Type.BOOTS, 40, false);

    public static final DeferredItem<Item> COBALT_HELMET = fsbArmor("cobalt_helmet", "cobalt", HbmArmorMaterials.COBALT, ArmorItem.Type.HELMET, 70, false);
    public static final DeferredItem<Item> COBALT_PLATE = fsbArmor("cobalt_plate", "cobalt", HbmArmorMaterials.COBALT, ArmorItem.Type.CHESTPLATE, 70, false);
    public static final DeferredItem<Item> COBALT_LEGS = fsbArmor("cobalt_legs", "cobalt", HbmArmorMaterials.COBALT, ArmorItem.Type.LEGGINGS, 70, false);
    public static final DeferredItem<Item> COBALT_BOOTS = fsbArmor("cobalt_boots", "cobalt", HbmArmorMaterials.COBALT, ArmorItem.Type.BOOTS, 70, false);

    public static final DeferredItem<Item> SECURITY_HELMET = fsbArmor("security_helmet", "security", HbmArmorMaterials.SECURITY, ArmorItem.Type.HELMET, 100, false);
    public static final DeferredItem<Item> SECURITY_PLATE = fsbArmor("security_plate", "security", HbmArmorMaterials.SECURITY, ArmorItem.Type.CHESTPLATE, 100, false);
    public static final DeferredItem<Item> SECURITY_LEGS = fsbArmor("security_legs", "security", HbmArmorMaterials.SECURITY, ArmorItem.Type.LEGGINGS, 100, false);
    public static final DeferredItem<Item> SECURITY_BOOTS = fsbArmor("security_boots", "security", HbmArmorMaterials.SECURITY, ArmorItem.Type.BOOTS, 100, false);

    public static final DeferredItem<Item> STARMETAL_HELMET = fsbArmor("starmetal_helmet", "starmetal", HbmArmorMaterials.STARMETAL, ArmorItem.Type.HELMET, 150, false);
    public static final DeferredItem<Item> STARMETAL_PLATE = fsbArmor("starmetal_plate", "starmetal", HbmArmorMaterials.STARMETAL, ArmorItem.Type.CHESTPLATE, 150, false);
    public static final DeferredItem<Item> STARMETAL_LEGS = fsbArmor("starmetal_legs", "starmetal", HbmArmorMaterials.STARMETAL, ArmorItem.Type.LEGGINGS, 150, false);
    public static final DeferredItem<Item> STARMETAL_BOOTS = fsbArmor("starmetal_boots", "starmetal", HbmArmorMaterials.STARMETAL, ArmorItem.Type.BOOTS, 150, false);

    public static final DeferredItem<Item> ROBES_HELMET = fsbArmor("robes_helmet", "robes", HbmArmorMaterials.ROBES, ArmorItem.Type.HELMET, 20, false);
    public static final DeferredItem<Item> ROBES_PLATE = fsbArmor("robes_plate", "robes", HbmArmorMaterials.ROBES, ArmorItem.Type.CHESTPLATE, 20, false);
    public static final DeferredItem<Item> ROBES_LEGS = fsbArmor("robes_legs", "robes", HbmArmorMaterials.ROBES, ArmorItem.Type.LEGGINGS, 20, false);
    public static final DeferredItem<Item> ROBES_BOOTS = fsbArmor("robes_boots", "robes", HbmArmorMaterials.ROBES, ArmorItem.Type.BOOTS, 20, false);

    public static final DeferredItem<Item> DNT_HELMET = fsbArmor("dnt_helmet", "dnt", HbmArmorMaterials.DNT, ArmorItem.Type.HELMET, 3, false);
    public static final DeferredItem<Item> DNT_PLATE = fsbArmor("dnt_plate", "dnt", HbmArmorMaterials.DNT, ArmorItem.Type.CHESTPLATE, 3, false);
    public static final DeferredItem<Item> DNT_LEGS = fsbArmor("dnt_legs", "dnt", HbmArmorMaterials.DNT, ArmorItem.Type.LEGGINGS, 3, false);
    public static final DeferredItem<Item> DNT_BOOTS = fsbArmor("dnt_boots", "dnt", HbmArmorMaterials.DNT, ArmorItem.Type.BOOTS, 3, false);

    public static final DeferredItem<Item> CMB_HELMET = fsbArmor(
            "cmb_helmet", "cmb", HbmArmorMaterials.CMB, ArmorItem.Type.HELMET, 60, false,
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 30, 2),
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 30, 0),
            ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 30, 0)
    );
    public static final DeferredItem<Item> CMB_PLATE = fsbArmor(
            "cmb_plate", "cmb", HbmArmorMaterials.CMB, ArmorItem.Type.CHESTPLATE, 60, false,
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 30, 2),
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 30, 0),
            ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 30, 0)
    );
    public static final DeferredItem<Item> CMB_LEGS = fsbArmor(
            "cmb_legs", "cmb", HbmArmorMaterials.CMB, ArmorItem.Type.LEGGINGS, 60, false,
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 30, 2),
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 30, 0),
            ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 30, 0)
    );
    public static final DeferredItem<Item> CMB_BOOTS = fsbArmor(
            "cmb_boots", "cmb", HbmArmorMaterials.CMB, ArmorItem.Type.BOOTS, 60, false,
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 30, 2),
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 30, 0),
            ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 30, 0)
    );

    public static final DeferredItem<Item> SCHRABIDIUM_HELMET = fsbArmor(
            "schrabidium_helmet", "schrabidium", HbmArmorMaterials.SCHRABIDIUM, ArmorItem.Type.HELMET, 100, false,
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 2),
            ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 2),
            ArmorFSBItem.effect(MobEffects.JUMP, 20, 1),
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 2)
    );
    public static final DeferredItem<Item> SCHRABIDIUM_PLATE = fsbArmor(
            "schrabidium_plate", "schrabidium", HbmArmorMaterials.SCHRABIDIUM, ArmorItem.Type.CHESTPLATE, 100, false,
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 2),
            ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 2),
            ArmorFSBItem.effect(MobEffects.JUMP, 20, 1),
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 2)
    );
    public static final DeferredItem<Item> SCHRABIDIUM_LEGS = fsbArmor(
            "schrabidium_legs", "schrabidium", HbmArmorMaterials.SCHRABIDIUM, ArmorItem.Type.LEGGINGS, 100, false,
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 2),
            ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 2),
            ArmorFSBItem.effect(MobEffects.JUMP, 20, 1),
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 2)
    );
    public static final DeferredItem<Item> SCHRABIDIUM_BOOTS = fsbArmor(
            "schrabidium_boots", "schrabidium", HbmArmorMaterials.SCHRABIDIUM, ArmorItem.Type.BOOTS, 100, false,
            ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 2),
            ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 2),
            ArmorFSBItem.effect(MobEffects.JUMP, 20, 1),
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 2)
    );

    public static final DeferredItem<Item> TITANIUM_SWORD = sword("titanium_sword", profile(HbmToolTier.TITANIUM, 6.5F, -2.4F));
    public static final DeferredItem<Item> TITANIUM_PICKAXE = pickaxe("titanium_pickaxe", profile(HbmToolTier.TITANIUM, 4.5F, -2.8F));
    public static final DeferredItem<Item> TITANIUM_AXE = axe("titanium_axe", profile(HbmToolTier.TITANIUM, 5.5F, -2.8F).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> TITANIUM_SHOVEL = shovel("titanium_shovel", profile(HbmToolTier.TITANIUM, 3.5F, -2.8F));

    public static final DeferredItem<Item> STEEL_SWORD = sword("steel_sword", profile(HbmToolTier.STEEL, 6.0F, -2.4F));
    public static final DeferredItem<Item> STEEL_PICKAXE = pickaxe("steel_pickaxe", profile(HbmToolTier.STEEL, 4.0F, -2.8F));
    public static final DeferredItem<Item> STEEL_AXE = axe("steel_axe", profile(HbmToolTier.STEEL, 5.0F, -2.8F).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> STEEL_SHOVEL = shovel("steel_shovel", profile(HbmToolTier.STEEL, 3.0F, -2.8F));

    public static final DeferredItem<Item> ALLOY_SWORD = sword("alloy_sword", profile(HbmToolTier.ALLOY, 8.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.STUN, 0));
    public static final DeferredItem<Item> ALLOY_PICKAXE = pickaxe("alloy_pickaxe", profile(HbmToolTier.ALLOY, 5.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 0));
    public static final DeferredItem<Item> ALLOY_AXE = axe("alloy_axe", profile(HbmToolTier.ALLOY, 7.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> ALLOY_SHOVEL = shovel("alloy_shovel", profile(HbmToolTier.ALLOY, 4.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 0));

    public static final DeferredItem<Item> ELEC_SWORD = sword("elec_sword", profile(HbmToolTier.ELEC, 12.5F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.STUN, 2));
    public static final DeferredItem<Item> ELEC_PICKAXE = pickaxe("elec_pickaxe", profile(HbmToolTier.ELEC, 10.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> ELEC_AXE = axe("elec_axe", profile(HbmToolTier.ELEC, 10.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1).weapon(HbmToolBehavior.WeaponAbility.CHAINSAW, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> ELEC_SHOVEL = shovel("elec_shovel", profile(HbmToolTier.ELEC, 7.5F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1));

    public static final DeferredItem<Item> DESH_SWORD = sword("desh_sword", profile(HbmToolTier.DESH, 15.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.STUN, 0));
    public static final DeferredItem<Item> DESH_PICKAXE = pickaxe("desh_pickaxe", profile(HbmToolTier.DESH, 5.0F, -2.8F).movement(-0.05D).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).area(HbmToolBehavior.AreaAbility.RECURSION, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> DESH_AXE = axe("desh_axe", profile(HbmToolTier.DESH, 6.5F, -2.8F).movement(-0.05D).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).area(HbmToolBehavior.AreaAbility.RECURSION, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> DESH_SHOVEL = shovel("desh_shovel", profile(HbmToolTier.DESH, 4.0F, -2.8F).movement(-0.05D).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).area(HbmToolBehavior.AreaAbility.RECURSION, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1));

    public static final DeferredItem<Item> COBALT_SWORD = sword("cobalt_sword", profile(HbmToolTier.COBALT, 12.0F, -2.4F));
    public static final DeferredItem<Item> COBALT_PICKAXE = pickaxe("cobalt_pickaxe", profile(HbmToolTier.COBALT, 4.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 0));
    public static final DeferredItem<Item> COBALT_AXE = axe("cobalt_axe", profile(HbmToolTier.COBALT, 6.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> COBALT_SHOVEL = shovel("cobalt_shovel", profile(HbmToolTier.COBALT, 3.5F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 0));

    public static final DeferredItem<Item> COBALT_DECORATED_SWORD = sword("cobalt_decorated_sword", profile(HbmToolTier.COBALT_DECORATED, 15.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.BOBBLE, 0));
    public static final DeferredItem<Item> COBALT_DECORATED_PICKAXE = pickaxe("cobalt_decorated_pickaxe", profile(HbmToolTier.COBALT_DECORATED, 6.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> COBALT_DECORATED_AXE = axe("cobalt_decorated_axe", profile(HbmToolTier.COBALT_DECORATED, 8.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> COBALT_DECORATED_SHOVEL = shovel("cobalt_decorated_shovel", profile(HbmToolTier.COBALT_DECORATED, 5.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2));

    public static final DeferredItem<Item> STARMETAL_SWORD = sword("starmetal_sword", profile(HbmToolTier.STARMETAL, 25.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).weapon(HbmToolBehavior.WeaponAbility.STUN, 1).weapon(HbmToolBehavior.WeaponAbility.BOBBLE, 0));
    public static final DeferredItem<Item> STARMETAL_PICKAXE = pickaxe("starmetal_pickaxe", profile(HbmToolTier.STARMETAL, 8.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 3).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).weapon(HbmToolBehavior.WeaponAbility.STUN, 1));
    public static final DeferredItem<Item> STARMETAL_AXE = axe("starmetal_axe", profile(HbmToolTier.STARMETAL, 12.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 3).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).weapon(HbmToolBehavior.WeaponAbility.STUN, 1));
    public static final DeferredItem<Item> STARMETAL_SHOVEL = shovel("starmetal_shovel", profile(HbmToolTier.STARMETAL, 7.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 3).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).weapon(HbmToolBehavior.WeaponAbility.STUN, 1));

    public static final DeferredItem<Item> CMB_SWORD = sword("cmb_sword", profile(HbmToolTier.CMB, 35.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.STUN, 0).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 0));
    public static final DeferredItem<Item> CMB_PICKAXE = pickaxe("cmb_pickaxe", profile(HbmToolTier.CMB, 10.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> CMB_AXE = axe("cmb_axe", profile(HbmToolTier.CMB, 30.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> CMB_SHOVEL = shovel("cmb_shovel", profile(HbmToolTier.CMB, 8.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2));

    public static final DeferredItem<Item> BISMUTH_AXE = axe("bismuth_axe", profile(HbmToolTier.BISMUTH, 25.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).weapon(HbmToolBehavior.WeaponAbility.STUN, 3).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 1).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> BISMUTH_PICKAXE = pickaxe("bismuth_pickaxe", profile(HbmToolTier.BISMUTH, 15.0F, -2.8F).asMiner().area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).weapon(HbmToolBehavior.WeaponAbility.STUN, 2).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).canBreakDepthRock());

    public static final DeferredItem<Item> VOLCANIC_AXE = axe("volcanic_axe", profile(HbmToolTier.VOLCANIC, 25.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).weapon(HbmToolBehavior.WeaponAbility.FIRE, 1).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 1).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> VOLCANIC_PICKAXE = pickaxe("volcanic_pickaxe", profile(HbmToolTier.VOLCANIC, 15.0F, -2.8F).asMiner().area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).weapon(HbmToolBehavior.WeaponAbility.FIRE, 0).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).canBreakDepthRock());

    public static final DeferredItem<Item> CHLOROPHYTE_AXE = axe("chlorophyte_axe", profile(HbmToolTier.CHLOROPHYTE, 50.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.LUCK, 3).weapon(HbmToolBehavior.WeaponAbility.STUN, 4).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 3).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> CHLOROPHYTE_PICKAXE = pickaxe("chlorophyte_pickaxe", profile(HbmToolTier.CHLOROPHYTE, 20.0F, -2.8F).asMiner().area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.LUCK, 3).harvest(HbmToolBehavior.HarvestAbility.CENTRIFUGE, 0).harvest(HbmToolBehavior.HarvestAbility.MERCURY, 0).weapon(HbmToolBehavior.WeaponAbility.STUN, 3).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 2).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).canBreakDepthRock());

    public static final DeferredItem<Item> SCHRABIDIUM_SWORD = sword("schrabidium_sword", profile(HbmToolTier.SCHRABIDIUM, 75.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.RADIATION, 1).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 0).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SCHRABIDIUM_PICKAXE = pickaxe("schrabidium_pickaxe", profile(HbmToolTier.SCHRABIDIUM, 20.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 6).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0).weapon(HbmToolBehavior.WeaponAbility.RADIATION, 0).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SCHRABIDIUM_AXE = axe("schrabidium_axe", profile(HbmToolTier.SCHRABIDIUM, 25.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 6).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0).weapon(HbmToolBehavior.WeaponAbility.RADIATION, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SCHRABIDIUM_SHOVEL = shovel("schrabidium_shovel", profile(HbmToolTier.SCHRABIDIUM, 15.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 6).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0).weapon(HbmToolBehavior.WeaponAbility.RADIATION, 0).rarity(Rarity.RARE));

    public static final DeferredItem<Item> MESE_PICKAXE = pickaxe("mese_pickaxe", profile(HbmToolTier.MESE, 35.0F, -2.8F).asMiner().area(HbmToolBehavior.AreaAbility.HAMMER, 2).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 2).area(HbmToolBehavior.AreaAbility.RECURSION, 2).area(HbmToolBehavior.AreaAbility.EXPLOSION, 3).harvest(HbmToolBehavior.HarvestAbility.CRYSTALLIZER, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 5).weapon(HbmToolBehavior.WeaponAbility.STUN, 3).weapon(HbmToolBehavior.WeaponAbility.PHOSPHORUS, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).canBreakDepthRock());
    public static final DeferredItem<Item> MESE_AXE = axe("mese_axe", profile(HbmToolTier.MESE, 75.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 2).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 2).area(HbmToolBehavior.AreaAbility.RECURSION, 2).area(HbmToolBehavior.AreaAbility.EXPLOSION, 3).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 5).weapon(HbmToolBehavior.WeaponAbility.STUN, 4).weapon(HbmToolBehavior.WeaponAbility.PHOSPHORUS, 1).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> DNT_SWORD = sword("dnt_sword", profile(HbmToolTier.MESE, 12.0F, -2.4F));
    public static final DeferredItem<Item> DWARVEN_PICKAXE = pickaxe("dwarven_pickaxe", profile(HbmToolTier.DWARVEN, 5.0F, -2.8F).asMiner().movement(-0.1D).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0));

    public static final DeferredItem<Item> REDSTONE_SWORD = sword("redstone_sword", profile(HbmToolTier.STONE_COMPAT, 5.0F, -2.4F).special(HbmToolBehavior.SpecialBehavior.REDSTONE_SWORD));
    public static final DeferredItem<Item> BIG_SWORD = sword("big_sword", profile(HbmToolTier.DIAMOND_COMPAT, 7.0F, -2.4F));
    public static final DeferredItem<Item> MYSTERY_SHOVEL = shovel("mysteryshovel", profile(HbmToolTier.STONE_COMPAT, 2.5F, -2.8F).special(HbmToolBehavior.SpecialBehavior.MYSTERY_SHOVEL));
    public static final DeferredItem<Item> SHIMMER_AXE = axe("shimmer_axe", profile(HbmToolTier.SHIMMER, 26.0F, -2.8F).special(HbmToolBehavior.SpecialBehavior.SHIMMER_AXE).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> METEORITE_SWORD = sword("meteorite_sword", profile(HbmToolTier.METEORITE, 9.0F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_SEARED = sword("meteorite_sword_seared", profile(HbmToolTier.METEORITE, 10.0F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_REFORGED = sword("meteorite_sword_reforged", profile(HbmToolTier.METEORITE, 12.5F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_HARDENED = sword("meteorite_sword_hardened", profile(HbmToolTier.METEORITE, 15.0F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_ALLOYED = sword("meteorite_sword_alloyed", profile(HbmToolTier.METEORITE, 17.5F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_MACHINED = sword("meteorite_sword_machined", profile(HbmToolTier.METEORITE, 20.0F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_TREATED = sword("meteorite_sword_treated", profile(HbmToolTier.METEORITE, 22.5F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_ETCHED = sword("meteorite_sword_etched", profile(HbmToolTier.METEORITE, 25.0F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_BRED = sword("meteorite_sword_bred", profile(HbmToolTier.METEORITE, 30.0F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_IRRADIATED = sword("meteorite_sword_irradiated", profile(HbmToolTier.METEORITE, 35.0F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_FUSED = sword("meteorite_sword_fused", profile(HbmToolTier.METEORITE, 50.0F, -2.4F));
    public static final DeferredItem<Item> METEORITE_SWORD_BALEFUL = sword("meteorite_sword_baleful", profile(HbmToolTier.METEORITE, 75.0F, -2.4F));

    public static final DeferredItem<Item> LINKER = toolItem(
            "linker",
            () -> new TeleLinkItem(new Item.Properties())
    );
    public static final DeferredItem<Item> RADAR_LINKER = toolItem(
            "radar_linker",
            () -> new RadarLinkerItem(new Item.Properties())
    );
    public static final DeferredItem<Item> SAT_CHIP = satelliteItem("sat_chip", "");
    public static final DeferredItem<Item> SAT_FOEQ = satelliteItem("sat_foeq", "item.reinhardtshbm.satellite.foeq");
    public static final DeferredItem<Item> SAT_GERALD = satelliteItem("sat_gerald", "item.reinhardtshbm.satellite.gerald");
    public static final DeferredItem<Item> SAT_LASER = satelliteItem("sat_laser", "item.reinhardtshbm.satellite.laser");
    public static final DeferredItem<Item> SAT_LUNAR_MINER = satelliteItem("sat_lunar_miner", "item.reinhardtshbm.satellite.lunar_miner");
    public static final DeferredItem<Item> SAT_MAPPER = satelliteItem("sat_mapper", "item.reinhardtshbm.satellite.mapper");
    public static final DeferredItem<Item> SAT_MINER = satelliteItem("sat_miner", "item.reinhardtshbm.satellite.miner");
    public static final DeferredItem<Item> SAT_RADAR = satelliteItem("sat_radar", "item.reinhardtshbm.satellite.radar");
    public static final DeferredItem<Item> SAT_RELAY = satelliteItem("sat_relay", "");
    public static final DeferredItem<Item> SAT_RESONATOR = satelliteItem("sat_resonator", "item.reinhardtshbm.satellite.resonator");
    public static final DeferredItem<Item> SAT_SCANNER = satelliteItem("sat_scanner", "item.reinhardtshbm.satellite.scanner");

    static {
        registerLegacyMaterialItems();
        registerRawOres();
    }

    private HbmItems() {
    }

    private static DeferredItem<Item> ingot(String name) {
        return ingot(name, Rarity.COMMON);
    }

    private static DeferredItem<Item> ingot(String name, Rarity rarity) {
        DeferredItem<Item> item = coreItem(name, () -> new Item(new Item.Properties().rarity(rarity)));
        INGOT_MATERIALS.add(item);
        return item;
    }

    private static DeferredItem<Item> material(List<DeferredItem<Item>> group, String name) {
        return material(group, name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> material(List<DeferredItem<Item>> group, String name, Supplier<Item> supplier) {
        DeferredItem<Item> item = coreItem(name, supplier);
        group.add(item);
        return item;
    }

    private static DeferredItem<Item> nuclearWaste(String name, NuclearWasteItem.Family family, boolean depleted, boolean tiny) {
        return material(MISC_MATERIALS, name, () -> new NuclearWasteItem(new Item.Properties(), family, depleted, tiny));
    }

    private static DeferredItem<Item> rtgPellet(String name, int heat, long lifespan, String depletedMaterial) {
        return material(MISC_MATERIALS, name,
                () -> new RtgPelletItem(heat, lifespan, depletedMaterial, () -> PELLET_RTG_DEPLETED.get()));
    }

    private static DeferredItem<Item> particle(String name, boolean returnsEmptyCapsule) {
        return material(MISC_MATERIALS, name, () -> new ParticleCapsuleItem(new Item.Properties(), returnsEmptyCapsule));
    }

    private static DeferredItem<Item> machineComponent(String name) {
        return machineComponent(name, () -> new MachineUpgradeItem(new Item.Properties().stacksTo(machineComponentStackSize(name))));
    }

    private static DeferredItem<Item> machineComponent(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        MACHINE_COMPONENTS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> fluidItem(String name) {
        return fluidItem(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> fluidItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        FLUID_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> fuelRodItem(String name) {
        return fuelRodItem(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> fuelRodItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        FUEL_ROD_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> wasteFuelItem(String name) {
        return fuelRodItem(name, () -> new WasteFuelItem(new Item.Properties()));
    }

    private static DeferredItem<Item> rbmkFuelRodItem(String name) {
        return rbmkFuelRodItem(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> rbmkFuelRodItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        RBMK_FUEL_ROD_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> rbmkReactorItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        RBMK_REACTOR_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> reactorItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        REACTOR_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> amsCatalyst(String name, int color, long powerAbs, float powerMod, float heatMod, float fuelMod) {
        return reactorItem(name, () -> new AmsCatalystItem(new Item.Properties().stacksTo(1), color, powerAbs, powerMod, heatMod, fuelMod));
    }

    private static DeferredItem<Item> oreDrop(String name) {
        return oreDrop(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> oreDrop(String name, Supplier<Item> supplier) {
        DeferredItem<Item> item = coreItem(name, supplier);
        ORE_DROPS.add(item);
        return item;
    }

    private static DeferredItem<Item> rawOre(String name) {
        DeferredItem<Item> item = coreItem(name, () -> new Item(new Item.Properties()));
        RAW_ORES.add(item);
        return item;
    }

    private static DeferredItem<Item> mineralCrystal(String name) {
        DeferredItem<Item> item = coreItem(name, () -> new Item(new Item.Properties()));
        MINERAL_CRYSTALS.add(item);
        return item;
    }

    private static DeferredItem<Item> mineralCrystal(String name, Supplier<Item> supplier) {
        DeferredItem<Item> item = coreItem(name, supplier);
        MINERAL_CRYSTALS.add(item);
        return item;
    }

    private static DeferredItem<Item> toolItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        TOOL_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> foundryItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        FOUNDRY_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> turretItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        TURRET_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> satelliteItem(String name, String descriptionKey) {
        DeferredItem<Item> registered = coreItem(name, () -> new SatelliteChipItem(new Item.Properties(), descriptionKey));
        SATELLITE_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> armorItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        ARMOR_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> fsbArmor(
            String name,
            String fsbGroup,
            Holder<ArmorMaterial> material,
            ArmorItem.Type type,
            int durabilityFactor,
            boolean noHelmet,
            ArmorFSBItem.FullSetEffect... effects
    ) {
        return armorItem(name, () -> new ArmorFSBItem(
                fsbGroup,
                material,
                type,
                noHelmet,
                List.of(effects),
                new Item.Properties()
                        .stacksTo(1)
                        .durability(type.getDurability(durabilityFactor))
        ));
    }

    private static DeferredItem<Item> sword(String name, HbmToolProfile profile) {
        return toolItem(name, () -> new HbmSwordItem(profile));
    }

    private static DeferredItem<Item> pickaxe(String name, HbmToolProfile profile) {
        return toolItem(name, () -> new HbmPickaxeItem(profile));
    }

    private static DeferredItem<Item> axe(String name, HbmToolProfile profile) {
        return toolItem(name, () -> new HbmAxeItem(profile));
    }

    private static DeferredItem<Item> shovel(String name, HbmToolProfile profile) {
        return toolItem(name, () -> new HbmShovelItem(profile));
    }

    private static HbmToolProfile profile(HbmToolTier tier, float attackDamage, float attackSpeed) {
        return HbmToolProfile.create(tier, attackDamage, attackSpeed);
    }

    private static DeferredItem<Item> coreItem(String name, Supplier<Item> item) {
        CORE_ITEM_IDS.add(name);
        return ITEMS.register(name, item);
    }

    public static boolean isCoreItem(String id) {
        return CORE_ITEM_IDS.contains(id);
    }

    private static void registerLegacyMaterialItems() {
        for (String id : loadLegacyItemIds()) {
            if (CORE_ITEM_IDS.contains(id)) {
                continue;
            }

            if (id.startsWith("ingot_")) {
                ingot(id);
            } else if (id.startsWith("nugget_")) {
                material(NUGGET_MATERIALS, id);
            } else if (id.startsWith("powder_") || id.equals("dust") || id.equals("dust_tiny")) {
                material(POWDER_MATERIALS, id);
            } else if (id.startsWith("plate_")) {
                material(PLATE_MATERIALS, id);
            } else if (isNuclearBillet(id)) {
                material(NUCLEAR_BILLETS, id);
            } else if (isMiscMaterial(id)) {
                material(MISC_MATERIALS, id);
            } else if (isMineralCrystal(id)) {
                mineralCrystal(id);
            } else if (id.startsWith("stamp_")) {
                machineComponent(id, () -> StampItem.fromLegacyId(id));
            } else if (isMachineComponent(id)) {
                machineComponent(id);
            } else if (id.startsWith("fragment_") || id.startsWith("bedrock_ore_")) {
                oreDrop(id);
            }
        }
    }

    private static boolean isMiscMaterial(String id) {
        return id.startsWith("coil_")
                || id.startsWith("wire_")
                || id.startsWith("billet_")
                || id.startsWith("pipe_")
                || id.startsWith("circuit_")
                || id.startsWith("pipes_")
                || id.startsWith("tank_")
                || id.equals("casing")
                || id.startsWith("casing_");
    }

    private static boolean isNuclearBillet(String id) {
        return id.startsWith("billet_")
                && !id.equals("billet_silicon");
    }

    private static boolean isMachineComponent(String id) {
        return id.startsWith("upgrade_")
                || id.equals("blade_meteorite")
                || id.equals("blade_titanium")
                || id.equals("blade_tungsten")
                || id.equals("turbine_titanium")
                || id.equals("turbine_tungsten")
                || id.equals("flywheel_beryllium")
                || id.equals("sawblade");
    }

    private static int machineComponentStackSize(String id) {
        if (id.equals("upgrade_radius") || id.equals("upgrade_health")) {
            return 16;
        }
        if (id.startsWith("upgrade_") && !id.equals("upgrade_muffler")) {
            return 1;
        }
        return 64;
    }

    private static boolean isMineralCrystal(String id) {
        return id.startsWith("crystal_")
                && !id.equals("crystal_energy")
                && !id.equals("crystal_horn")
                && !id.equals("crystal_xen");
    }

    private static void registerRawOres() {
        rawOre("raw_uranium");
        rawOre("raw_thorium");
        rawOre("raw_titanium");
        rawOre("raw_tungsten");
        rawOre("raw_aluminium");
        rawOre("raw_lead");
        rawOre("raw_beryllium");
        rawOre("raw_cobalt");
        rawOre("raw_coltan");
        rawOre("raw_schrabidium");
    }

    private static List<String> loadLegacyItemIds() {
        InputStream stream = HbmItems.class.getClassLoader().getResourceAsStream("legacy/reinhardtshbm/items.txt");
        if (stream == null) {
            throw new IllegalStateException("Missing generated HBM legacy item id list.");
        }

        List<String> ids = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.trim();
                if (!id.isEmpty() && id.charAt(0) == '\uFEFF') {
                    id = id.substring(1);
                }
                if (id.isEmpty() || id.startsWith("#")) {
                    continue;
                }
                if (isValidPath(id)) {
                    ids.add(id);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read generated HBM legacy item id list.", exception);
        }
        return ids;
    }

    private static boolean isValidPath(String path) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/') {
                continue;
            }
            return false;
        }
        return !path.isBlank() && path.charAt(0) != '/' && path.charAt(path.length() - 1) != '/';
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
