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
import com.reinhardt.hbm.item.ArmorMagnetItem;
import com.reinhardt.hbm.item.ArmorPadsItem;
import com.reinhardt.hbm.item.GasMaskAttachmentItem;
import com.reinhardt.hbm.item.LegacyBathwaterArmorModItem;
import com.reinhardt.hbm.item.LegacyCardArmorModItem;
import com.reinhardt.hbm.item.LegacyReviveArmorModItem;
import com.reinhardt.hbm.item.LegacyArmorUtilityModItem;
import com.reinhardt.hbm.item.LegacyGasSensorArmorModItem;
import com.reinhardt.hbm.item.LegacyAlexandriteItem;
import com.reinhardt.hbm.item.LegacyAnchorRemoteItem;
import com.reinhardt.hbm.item.LegacyBdclItem;
import com.reinhardt.hbm.item.LegacyDigammaDiagnosticItem;
import com.reinhardt.hbm.item.LegacyDiscordRodItem;
import com.reinhardt.hbm.item.LegacyMedalArmorModItem;
import com.reinhardt.hbm.item.LegacyNightVisionArmorModItem;
import com.reinhardt.hbm.item.LegacyShacklesArmorModItem;
import com.reinhardt.hbm.item.LegacyShieldArmorModItem;
import com.reinhardt.hbm.item.LegacyOreDensityScannerItem;
import com.reinhardt.hbm.item.LegacyPotatosBatteryItem;
import com.reinhardt.hbm.item.LegacySurveyScannerItem;
import com.reinhardt.hbm.item.LegacyTeslaArmorModItem;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BombPartItem;
import com.reinhardt.hbm.item.BedrockOreBaseItem;
import com.reinhardt.hbm.item.BedrockOreFragmentItem;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.item.LegacyBedrockOreStageItem;
import com.reinhardt.hbm.item.LegacyByproductItem;
import com.reinhardt.hbm.item.LegacyMetaUpgradeItem;
import com.reinhardt.hbm.item.BladesItem;
import com.reinhardt.hbm.item.BlueprintFolderItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.BrokenItem;
import com.reinhardt.hbm.item.ChemistrySetItem;
import com.reinhardt.hbm.item.CombustionPistonSetItem;
import com.reinhardt.hbm.item.ColtanCompassItem;
import com.reinhardt.hbm.item.CounterfeitKeyKitItem;
import com.reinhardt.hbm.item.ConveyorWandItem;
import com.reinhardt.hbm.item.CustomMissileItem;
import com.reinhardt.hbm.item.DrillbitItem;
import com.reinhardt.hbm.item.DefuserItem;
import com.reinhardt.hbm.item.HbmAxeItem;
import com.reinhardt.hbm.item.ArmorFSBItem;
import com.reinhardt.hbm.item.FueledArmorFSBItem;
import com.reinhardt.hbm.item.PoweredArmorFSBItem;
import com.reinhardt.hbm.item.ArtilleryDesignatorItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.HbmPickaxeItem;
import com.reinhardt.hbm.item.HbmPoweredPickaxeItem;
import com.reinhardt.hbm.item.HbmShovelItem;
import com.reinhardt.hbm.item.HbmSwordItem;
import com.reinhardt.hbm.item.HbmSpecialWeaponItem;
import com.reinhardt.hbm.item.HbmToolBehavior;
import com.reinhardt.hbm.item.HbmToolProfile;
import com.reinhardt.hbm.item.HbmToolTier;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.item.IcfPelletItem;
import com.reinhardt.hbm.item.InfiniteBatteryItem;
import com.reinhardt.hbm.item.FixedFluidBarrelBlockItem;
import com.reinhardt.hbm.item.LegacyBarrelBlockItem;
import com.reinhardt.hbm.item.TankSteelItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.FelCrystalItem;
import com.reinhardt.hbm.item.FixedBatteryItem;
import com.reinhardt.hbm.item.GasMaskFilterItem;
import com.reinhardt.hbm.item.GasMaskItem;
import com.reinhardt.hbm.item.BlowtorchItem;
import com.reinhardt.hbm.item.BoltgunItem;
import com.reinhardt.hbm.item.GuideBookItem;
import com.reinhardt.hbm.item.HbmFluidDuctItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.LargeGearItem;
import com.reinhardt.hbm.item.LegacyToolingItem;
import com.reinhardt.hbm.item.LiquidatorArmorItem;
import com.reinhardt.hbm.item.LegacyHotItem;
import com.reinhardt.hbm.item.LegacyInjectorKnifeArmorModItem;
import com.reinhardt.hbm.item.LegacyHeldInventoryItem;
import com.reinhardt.hbm.item.LegacyDetonatorItem;
import com.reinhardt.hbm.item.LegacyDynamiteStickItem;
import com.reinhardt.hbm.item.UniversalGrenadeItem;
import com.reinhardt.hbm.item.LegacyFluidSiphonItem;
import com.reinhardt.hbm.item.LegacyPowerNetToolItem;
import com.reinhardt.hbm.item.LegacyCigaretteItem;
import com.reinhardt.hbm.item.LegacyBookLoreItem;
import com.reinhardt.hbm.item.LegacyCraftBookItem;
import com.reinhardt.hbm.item.LegacyCustomKitItem;
import com.reinhardt.hbm.item.LegacyAnalysisToolItem;
import com.reinhardt.hbm.item.LegacyCbtDeviceItem;
import com.reinhardt.hbm.item.LegacyPolaroidItem;
import com.reinhardt.hbm.item.LegacyCanteenItem;
import com.reinhardt.hbm.item.LegacyCloudArmorModItem;
import com.reinhardt.hbm.item.LegacyBalefireMatchItem;
import com.reinhardt.hbm.item.LegacyChemicalDyeItem;
import com.reinhardt.hbm.item.LegacyConserveItem;
import com.reinhardt.hbm.item.LegacyCrayonItem;
import com.reinhardt.hbm.item.LegacyCrucibleItem;
import com.reinhardt.hbm.item.LegacyDemonCoreItem;
import com.reinhardt.hbm.item.LegacyDefuserArmorModItem;
import com.reinhardt.hbm.item.LegacyDropItem;
import com.reinhardt.hbm.item.LegacyEnergyDrinkItem;
import com.reinhardt.hbm.item.LegacyEuphemiumArmorItem;
import com.reinhardt.hbm.item.LegacyFlaskItem;
import com.reinhardt.hbm.item.LegacySchraraniumItem;
import com.reinhardt.hbm.item.LegacyUnstableItem;
import com.reinhardt.hbm.item.LegacyWd40ArmorModItem;
import com.reinhardt.hbm.item.LegacyLemonItem;
import com.reinhardt.hbm.item.LegacyMeltdownToolItem;
import com.reinhardt.hbm.item.LegacyLootCrateItem;
import com.reinhardt.hbm.item.LegacyLoreItem;
import com.reinhardt.hbm.item.LegacyMatchItem;
import com.reinhardt.hbm.item.LegacyMarshmallowItem;
import com.reinhardt.hbm.item.LegacyMissileItem;
import com.reinhardt.hbm.item.LegacyN2ChargeItem;
import com.reinhardt.hbm.item.LegacyNo9ArmorItem;
import com.reinhardt.hbm.item.LegacyPillItem;
import com.reinhardt.hbm.item.LegacyPipetteItem;
import com.reinhardt.hbm.item.LegacyRangeDesignatorItem;
import com.reinhardt.hbm.item.LegacyCoordinateDesignatorItem;
import com.reinhardt.hbm.item.LegacyManualDesignatorItem;
import com.reinhardt.hbm.item.LegacyWrenchItem;
import com.reinhardt.hbm.item.LegacySatelliteToolItem;
import com.reinhardt.hbm.item.LegacyJetpackItem;
import com.reinhardt.hbm.item.LegacyWingsItem;
import com.reinhardt.hbm.item.LegacyPeasItem;
import com.reinhardt.hbm.item.LegacyGlitchItem;
import com.reinhardt.hbm.item.LegacyDebugCraneWandItem;
import com.reinhardt.hbm.item.LegacyJetpackTankItem;
import com.reinhardt.hbm.item.LegacyNeutrinoLensItem;
import com.reinhardt.hbm.item.LegacyStructureExportToolItem;
import com.reinhardt.hbm.item.LegacyRebarPlacerItem;
import com.reinhardt.hbm.item.LegacyHolotapeImageItem;
import com.reinhardt.hbm.item.LegacyRttyPagerItem;
import com.reinhardt.hbm.item.LegacyRubberBoatItem;
import com.reinhardt.hbm.item.LegacyDuckSpawnItem;
import com.reinhardt.hbm.item.LegacyBossSpawnItem;
import com.reinhardt.hbm.item.LegacyDroneItem;
import com.reinhardt.hbm.item.LegacyDroneLinkerItem;
import com.reinhardt.hbm.item.LegacyBombCallerItem;
import com.reinhardt.hbm.item.LegacyBobmazonItem;
import com.reinhardt.hbm.item.LegacyClayTabletItem;
import com.reinhardt.hbm.item.LegacyMinecartItem;
import com.reinhardt.hbm.item.LegacyTrainItem;
import com.reinhardt.hbm.item.LegacySpecialFoodItem;
import com.reinhardt.hbm.item.LegacyStarterKitItem;
import com.reinhardt.hbm.item.LegacyStarmetalItem;
import com.reinhardt.hbm.item.LegacySyringeItem;
import com.reinhardt.hbm.item.HealthArmorModItem;
import com.reinhardt.hbm.item.HbmRecordItem;
import com.reinhardt.hbm.item.FusionCoreItem;
import com.reinhardt.hbm.item.ArmorInsertItem;
import com.reinhardt.hbm.item.LegacyChainsawItem;
import com.reinhardt.hbm.item.LegacyContainerRemainderItem;
import com.reinhardt.hbm.item.LegacyFillWandItem;
import com.reinhardt.hbm.item.LittleBoyKitItem;
import com.reinhardt.hbm.item.KeyPinItem;
import com.reinhardt.hbm.item.LockItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.item.MeteorCharmItem;
import com.reinhardt.hbm.item.MeteorRemoteItem;
import com.reinhardt.hbm.item.MirrorToolItem;
import com.reinhardt.hbm.item.MissilePartItem;
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
import com.reinhardt.hbm.item.RbmkPelletItem;
import com.reinhardt.hbm.item.RbmkLidItem;
import com.reinhardt.hbm.item.RtgPelletItem;
import com.reinhardt.hbm.item.RtgDepletedPelletItem;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.item.SelfChargingBatteryItem;
import com.reinhardt.hbm.item.SatelliteChipItem;
import com.reinhardt.hbm.item.SirenTrackItem;
import com.reinhardt.hbm.item.StampItem;
import com.reinhardt.hbm.item.StampBookItem;
import com.reinhardt.hbm.item.SettingsToolItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.item.StructureWandItem;
import com.reinhardt.hbm.item.TemplateFolderItem;
import com.reinhardt.hbm.item.ToolboxItem;
import com.reinhardt.hbm.item.TeleLinkItem;
import com.reinhardt.hbm.item.TurretAmmoItem;
import com.reinhardt.hbm.item.TurretBiometryItem;
import com.reinhardt.hbm.item.WatzPelletItem;
import com.reinhardt.hbm.item.WasteFuelItem;
import com.reinhardt.hbm.item.WiringRedCopperItem;
import com.reinhardt.hbm.item.ZirnoxRodItem;
import com.reinhardt.hbm.item.SoyuzItem;
import com.reinhardt.hbm.item.OrbitalModuleItem;
import com.reinhardt.hbm.foundry.FoundryShape;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
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
    /**
     * Registered with the other late legacy-content ports during static bootstrap.
     * Drone variants share this item id and store their legacy metadata in a
     * data component.
     */
    public static DeferredItem<Item> DRONE;
    public static DeferredItem<Item> DRONE_LINKER;
    public static DeferredItem<Item> CHOPPER;
    public static DeferredItem<Item> SPAWN_UFO;
    public static DeferredItem<Item> SPAWN_WORM;
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
    public static final List<DeferredItem<Item>> RBMK_PELLET_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> NUCLEAR_BILLETS = new ArrayList<>();
    public static final List<DeferredItem<Item>> NUCLEAR_WEAPON_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> ROCKET_MISSILE_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> TURRET_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> SATELLITE_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> PORTED_PLAIN_ITEMS = new ArrayList<>();

    /**
     * Items constructed with vanilla Item in 1.7.10. Their specialized
     * stack-size and crafting-remainder behavior is registered below.
     */
    private static final Set<String> PORTED_PLAIN_ITEM_IDS = Set.of(
            "asbestos_cloth", "billet_silicon", "bottle_empty", "bottle2_empty", "can_empty", "can_key",
            "cap_fritz", "cap_korl", "cap_nuka", "cap_quantum", "cap_rad", "cap_sparkle",
            "chlorine1", "chlorine2", "chlorine3", "chlorine4", "chlorine5", "chlorine6",
            "chlorine7", "chlorine8", "cinnebar", "cloud1", "cloud2", "cloud3",
            "cloud4", "cloud5", "cloud6", "cloud7", "cloud8", "coal_infernal",
            "coin_token", "combine_scrap", "debris_concrete", "debris_element", "debris_exchanger", "debris_fuel",
            "debris_graphite", "debris_metal", "debris_shrapnel", "demon_core_closed", "dysfunctional_reactor", "egg_glyphid",
            "fins_big_steel", "fins_flat", "fins_quad_titanium", "fins_tri_steel", "fuel_tank_large", "fuel_tank_medium",
            "fuel_tank_small", "holotape_damaged", "nitra", "nitra_small", "nothing", "orange1",
            "orange2", "orange3", "orange4", "orange5", "orange6", "orange7",
            "orange8", "pc1", "pc2", "pc3", "pc4", "pc5",
            "pc6", "pc7", "pc8", "pedestal_steel", "photo_panel", "reactor_core",
            "ring_pull", "ring_starmetal", "rtg_unit", "safety_fuse", "sat_base", "sat_head_laser",
            "sat_head_mapper", "sat_head_radar", "sat_head_resonator", "sat_head_scanner", "scrap_oil", "seg_10",
            "seg_15", "seg_20", "shimmer_axe_head", "shimmer_handle", "shimmer_head", "solid_fuel_presto",
            "solid_fuel_presto_bf", "solid_fuel_presto_triplet", "solid_fuel_presto_triplet_bf", "stick_c4", "stick_semtex", "stick_tnt",
            "thruster_large", "thruster_medium", "thruster_nuclear", "thruster_small", "warhead_buster_large", "warhead_buster_medium",
            "warhead_buster_small", "warhead_cluster_large", "warhead_cluster_medium", "warhead_cluster_small", "warhead_generic_large", "warhead_generic_medium",
            "warhead_generic_small", "warhead_incendiary_large", "warhead_incendiary_medium", "warhead_incendiary_small", "warhead_mirv", "warhead_nuclear",
            "warhead_volcano", "battery_spark", "battery_trixite", "egg_balefire", "gadget_wireing", "launch_code",
            "launch_code_piece", "launch_key", "man_igniter", "mike_cooling_unit", "mike_core", "missile_assembly",
            "reacher", "tsar_core", "syringe_empty", "syringe_metal_empty", "cell_antimatter", "cell_anti_schrabidium",
            "cell_balefire", "cell_deuterium",
            "cell_puf6", "cell_uf6", "mike_deut", "rod_zirnox_les_fuel_depleted", "rod_zirnox_mox_fuel_depleted",
            "rod_zirnox_natural_uranium_fuel_depleted", "rod_zirnox_plutonium_fuel_depleted", "rod_zirnox_thorium_fuel_depleted",
            "rod_zirnox_u233_fuel_depleted", "rod_zirnox_u235_fuel_depleted", "rod_zirnox_uranium_fuel_depleted",
            "rod_zirnox_zfb_mox_depleted"
    );
    private static final Set<String> HIDDEN_PORTED_PLAIN_ITEM_IDS = Set.of(
            "holotape_damaged", "book_secret", "burnt_bark", "key_red", "key_red_cracked", "mech_key", "watch",
            "bismuth_tool", "item_secret", "scrap_plastic"
    );

    /**
     * Components and launch-control items which belong to the 1.7.10
     * MissileTab rather than the generic legacy-item tab.
     */
    private static final Set<String> ROCKET_MISSILE_PLAIN_ITEM_IDS = Set.of(
            "fins_big_steel", "fins_flat", "fins_quad_titanium", "fins_tri_steel",
            "fuel_tank_large", "fuel_tank_medium", "fuel_tank_small",
            "sat_base", "sat_head_laser", "sat_head_mapper", "sat_head_radar",
            "sat_head_resonator", "sat_head_scanner",
            "thruster_large", "thruster_medium", "thruster_nuclear", "thruster_small",
            "warhead_buster_large", "warhead_buster_medium", "warhead_buster_small",
            "warhead_cluster_large", "warhead_cluster_medium", "warhead_cluster_small",
            "warhead_generic_large", "warhead_generic_medium", "warhead_generic_small",
            "warhead_incendiary_large", "warhead_incendiary_medium", "warhead_incendiary_small",
            "warhead_mirv", "warhead_nuclear", "warhead_volcano",
            "launch_code", "launch_code_piece", "launch_key", "missile_assembly",
            "missile_kit", "loot_10", "loot_15", "loot_misc"
    );

    private static final Set<String> RETIRED_LEGACY_CATALOG_ITEM_IDS = Set.of(
            "ammo_bag", "ammo_bag_infinite", "ammo_container", "ammo_debug", "ammo_fireext", "ammo_secret",
            "battery_advanced", "cell", "coin_siege",
            "fluid_barrel_v2", "fluid_tank_lead_v2", "fluid_tank_v2", "gun_egon", "gun_vortex",
            "jetpack_glider", "mechanism_launcher_1", "mechanism_launcher_2", "mechanism_revolver_1",
            "mechanism_revolver_2", "mechanism_rifle_1", "mechanism_rifle_2", "mechanism_special",
            "multitool_beam", "multitool_decon", "multitool_dig", "multitool_ext", "multitool_hit",
            "multitool_joule", "multitool_mega", "multitool_miner", "multitool_silk", "multitool_sky",
            "pellet_canister", "pellet_chlorophyte", "pellet_claws", "pellet_flechette", "pellet_meteorite",
            "sliding_blast_door_skin0", "sliding_blast_door_skin1", "sliding_blast_door_skin2",
            "weapon_mod_caliber", "weapon_mod_generic", "weapon_mod_special", "weapon_mod_test",
            "weaponized_starblaster_cell", "weapon_bat", "weapon_bat_nail", "weapon_golf_club",
            "weapon_pipe_rusty", "weapon_saw"
    );

    /** Fluid buckets are registered together with their NeoForge fluid entries. */
    private static final Set<String> FLUID_BUCKET_ITEM_IDS = Set.of(
            "bucket_acid", "bucket_mud", "bucket_schrabidic_acid", "bucket_sulfuric_acid", "bucket_toxic"
    );

    /**
     * 1.7.10 classes which already have exact 1.21 implementations. These
     * stay separate from ordinary Item registrations so their use effects and
     * kit contents cannot silently regress to placeholder behavior.
     */
    private static final Set<String> PORTED_LEGACY_GAMEPLAY_ITEM_IDS = Set.of(
            "black_diamond", "bottle_cherry", "bottle_nuka", "bottle_quantum",
            "bottle_rad", "bottle_sparkle", "bottle2_fritz", "bottle2_korl", "can_bepis", "can_breen",
            "can_creature", "can_luna", "can_mrsugar", "can_mug", "can_overcharge", "can_redbomb", "can_smart",
            "chocolate_milk", "cigarette", "coffee", "coffee_radium", "crackpipe", "custom_kit",
            "euphemium_kit", "five_htp", "fleija_kit", "fmn", "gadget_kit", "hazmat_grey_kit", "hazmat_kit",
            "hazmat_red_kit", "heart_booster", "heart_container", "heart_fab", "heart_piece",
            "man_kit", "med_bag", "mike_kit", "missile_kit", "multi_kit", "nuke_advanced_kit", "nuke_commercially_kit",
            "nuke_electric_kit", "nuke_starter_kit", "pill_herbal", "pill_iodine", "pill_red", "plan_c", "prototype_kit",
            "radx", "siox", "solinium_kit", "stealth_boy", "syringe_metal_medx", "syringe_metal_psycho",
            "syringe_metal_stimpak", "syringe_metal_super", "syringe_mkunicorn", "syringe_taint", "tsar_kit", "xanax"
    );

    /**
     * Remaining ItemCustomLore registrations which are not already covered
     * by the dynamically registered material families. The original item
     * class supplied lore, rarity, rune glint, stack limits and remainders.
     */
    private static final Set<String> PORTED_LEGACY_LORE_ITEM_IDS = Set.of(
            "ball_resin", "bolt_spike", "book_secret", "bottle_mercury", "burnt_bark", "canister_napalm", "cell_sas3",
            "coin_creeper", "coin_maskman", "coin_radiation", "coin_ufo", "coin_worm", "crystal_horn", "custom_amat",
            "custom_dirty", "custom_fall", "custom_hydro", "custom_nuke", "custom_schrab", "custom_tnt",
            "early_explosive_lenses", "entanglement_kit", "explosive_lenses", "flame_conspiracy", "flame_opinion",
            "flame_politics", "fuse", "gadget_core", "gem_sodalite", "gem_volcanic", "igniter", "key_red",
            "key_red_cracked", "magnetron", "man_core", "mech_key", "pellet_cluster", "rune_blank", "rune_dagaz",
            "rune_hagalaz", "rune_isa", "rune_jera", "rune_thurisaz", "undefined", "watch"
    );

    /** Exact standalone 1.7.10 RBMK fuel-rod registrations. */
    private static final Set<String> PORTED_LEGACY_RBMK_FUEL_IDS = Set.of(
            "rbmk_fuel_balefire", "rbmk_fuel_balefire_gold", "rbmk_fuel_drx", "rbmk_fuel_flashlead",
            "rbmk_fuel_hea241", "rbmk_fuel_hea242", "rbmk_fuel_heaus", "rbmk_fuel_hen", "rbmk_fuel_hep",
            "rbmk_fuel_hep241", "rbmk_fuel_hes", "rbmk_fuel_heu233", "rbmk_fuel_heu235", "rbmk_fuel_lea",
            "rbmk_fuel_leaus", "rbmk_fuel_lep", "rbmk_fuel_les", "rbmk_fuel_mea", "rbmk_fuel_men",
            "rbmk_fuel_mep", "rbmk_fuel_mes", "rbmk_fuel_meu", "rbmk_fuel_mox", "rbmk_fuel_po210be",
            "rbmk_fuel_pu238be", "rbmk_fuel_ra226be", "rbmk_fuel_test", "rbmk_fuel_thmeu", "rbmk_fuel_ueu",
            "rbmk_fuel_uzh", "rbmk_fuel_zfb_am_mix", "rbmk_fuel_zfb_bismuth", "rbmk_fuel_zfb_pu241"
    );

    /** Exact standalone 1.7.10 RBMK pellet registrations. */
    private static final Set<String> PORTED_LEGACY_RBMK_PELLET_IDS = Set.of(
            "rbmk_pellet_balefire", "rbmk_pellet_balefire_gold", "rbmk_pellet_drx", "rbmk_pellet_flashlead",
            "rbmk_pellet_hea241", "rbmk_pellet_hea242", "rbmk_pellet_heaus", "rbmk_pellet_hen",
            "rbmk_pellet_hep239", "rbmk_pellet_hep241", "rbmk_pellet_hes", "rbmk_pellet_heu233",
            "rbmk_pellet_heu235", "rbmk_pellet_lea", "rbmk_pellet_leaus", "rbmk_pellet_lep", "rbmk_pellet_les",
            "rbmk_pellet_mea", "rbmk_pellet_men", "rbmk_pellet_mep", "rbmk_pellet_mes", "rbmk_pellet_meu",
            "rbmk_pellet_mox", "rbmk_pellet_po210be", "rbmk_pellet_pu238be", "rbmk_pellet_ra226be",
            "rbmk_pellet_thmeu", "rbmk_pellet_ueu", "rbmk_pellet_uzh", "rbmk_pellet_zfb_am_mix",
            "rbmk_pellet_zfb_bismuth", "rbmk_pellet_zfb_pu241"
    );

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
    public static final DeferredItem<Item> PIPE = material(
            MISC_MATERIALS,
            "pipe",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.PIPE)
    );
    public static final DeferredItem<Item> PIPE_LEAD = material(MISC_MATERIALS, "pipe_lead");
    public static final DeferredItem<Item> PIPE_STEEL = material(MISC_MATERIALS, "pipe_steel");
    public static final DeferredItem<Item> PIPE_DURA_STEEL = material(MISC_MATERIALS, "pipe_dura_steel");
    public static final DeferredItem<Item> TANK_STEEL = material(
            MISC_MATERIALS,
            "tank_steel",
            () -> new TankSteelItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> BOLT = material(
            MISC_MATERIALS,
            "bolt",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.BOLT)
    );
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
    public static final DeferredItem<Item> CIRCUIT = machineComponent(
            "circuit",
            () -> new LegacyVariantItem(new Item.Properties(), "circuit", LegacyVariantItem.variants(
                    "vacuum_tube", "capacitor", "capacitor_tantalium", "pcb", "silicon",
                    "chip", "chip_bismoid", "analog", "basic", "advanced", "capacitor_board",
                    "bismoid", "controller_chassis", "controller", "controller_advanced", "quantum",
                    "chip_quantum", "controller_quantum", "atomic_clock", "numitron"
            ))
    );
    public static final DeferredItem<Item> CRT_DISPLAY = material(MISC_MATERIALS, "crt_display");
    public static final DeferredItem<Item> MOTOR = material(MISC_MATERIALS, "motor");
    public static final DeferredItem<Item> MOTOR_BISMUTH = material(MISC_MATERIALS, "motor_bismuth");
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
    public static final DeferredItem<Item> PART_BARREL_LIGHT = material(
            MISC_MATERIALS,
            "part_barrel_light",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.LIGHT_BARREL)
    );
    public static final DeferredItem<Item> PART_BARREL_HEAVY = material(
            MISC_MATERIALS,
            "part_barrel_heavy",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.HEAVY_BARREL)
    );
    public static final DeferredItem<Item> PART_RECEIVER_LIGHT = material(
            MISC_MATERIALS,
            "part_receiver_light",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.LIGHT_RECEIVER)
    );
    public static final DeferredItem<Item> PART_RECEIVER_HEAVY = material(
            MISC_MATERIALS,
            "part_receiver_heavy",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.HEAVY_RECEIVER)
    );
    public static final DeferredItem<Item> PART_STOCK = material(
            MISC_MATERIALS,
            "part_stock",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.STOCK)
    );
    public static final DeferredItem<Item> PART_GRIP = material(
            MISC_MATERIALS,
            "part_grip",
            () -> new FoundryShapeItem(new Item.Properties(), FoundryShape.GRIP)
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
    public static final DeferredItem<Item> ROCKET_FUEL = material(ROCKET_MISSILE_ITEMS, "rocket_fuel");
    public static final DeferredItem<Item> MISSILE_SOYUZ = material(
            ROCKET_MISSILE_ITEMS,
            "missile_soyuz",
            () -> new SoyuzItem()
    );
    public static final DeferredItem<Item> MISSILE_SOYUZ_LANDER = material(
            ROCKET_MISSILE_ITEMS,
            "missile_soyuz_lander",
            () -> new OrbitalModuleItem()
    );
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
    public static final DeferredItem<Item> FINS_SMALL_STEEL = material(ROCKET_MISSILE_ITEMS, "fins_small_steel");
    public static final DeferredItem<Item> BOY_SHIELDING = material(NUCLEAR_WEAPON_ITEMS, "boy_shielding", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOY_TARGET = material(NUCLEAR_WEAPON_ITEMS, "boy_target", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BOY_BULLET = material(NUCLEAR_WEAPON_ITEMS, "boy_bullet", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BOY_PROPELLANT = material(NUCLEAR_WEAPON_ITEMS, "boy_propellant", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOY_IGNITER = material(NUCLEAR_WEAPON_ITEMS, "boy_igniter", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOY_KIT = material(NUCLEAR_WEAPON_ITEMS, "boy_kit", () -> new LittleBoyKitItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FLEIJA_CORE = material(NUCLEAR_WEAPON_ITEMS, "fleija_core", () -> new BombPartItem(BombPartItem.Target.FLEIJA, Rarity.COMMON));
    public static final DeferredItem<Item> FLEIJA_IGNITER = material(NUCLEAR_WEAPON_ITEMS, "fleija_igniter", () -> new BombPartItem(BombPartItem.Target.FLEIJA, Rarity.COMMON));
    public static final DeferredItem<Item> FLEIJA_PROPELLANT = material(NUCLEAR_WEAPON_ITEMS, "fleija_propellant", () -> new BombPartItem(BombPartItem.Target.FLEIJA, Rarity.RARE));
    public static final DeferredItem<Item> SOLINIUM_CORE = material(NUCLEAR_WEAPON_ITEMS, "solinium_core", () -> new BombPartItem(BombPartItem.Target.SOLINIUM, Rarity.COMMON));
    public static final DeferredItem<Item> SOLINIUM_IGNITER = material(NUCLEAR_WEAPON_ITEMS, "solinium_igniter", () -> new BombPartItem(BombPartItem.Target.SOLINIUM, Rarity.COMMON));
    public static final DeferredItem<Item> SOLINIUM_PROPELLANT = material(NUCLEAR_WEAPON_ITEMS, "solinium_propellant", () -> new BombPartItem(BombPartItem.Target.SOLINIUM, Rarity.COMMON));
    public static final DeferredItem<Item> CHOCOLATE = material(MISC_MATERIALS, "chocolate");
    public static final DeferredItem<Item> PELLET_CHARGED = material(MISC_MATERIALS, "pellet_charged");
    public static final DeferredItem<Item> PART_LITHIUM = material(MACHINE_COMPONENTS, "part_lithium");
    public static final DeferredItem<Item> PART_BERYLLIUM = material(MACHINE_COMPONENTS, "part_beryllium");
    public static final DeferredItem<Item> PART_CARBON = material(MACHINE_COMPONENTS, "part_carbon");
    public static final DeferredItem<Item> PART_COPPER = material(MACHINE_COMPONENTS, "part_copper");
    public static final DeferredItem<Item> PART_PLUTONIUM = material(MACHINE_COMPONENTS, "part_plutonium");
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
    // 1.7.10 ItemEnumMulti variants used by PressRecipes.
    public static final DeferredItem<Item> BRIQUETTE = material(
            MISC_MATERIALS,
            "briquette",
            () -> new LegacyVariantItem(new Item.Properties(), "briquette", LegacyVariantItem.variants(
                    "coal", "lignite", "wood"
            ))
    );
    public static final DeferredItem<Item> CASING = material(
            MISC_MATERIALS,
            "casing",
            () -> new LegacyVariantItem(new Item.Properties(), "casing", LegacyVariantItem.variants(
                    "small", "large", "small_steel", "large_steel", "shotshell", "buckshot", "buckshot_advanced"
            ))
    );
    // ItemStampBook was intentionally hidden from the 1.7.10 creative inventory.
    public static final DeferredItem<Item> STAMP_BOOK = coreItem(
            "stamp_book",
            () -> new StampBookItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> PAGE_OF = coreItem(
            "page_of_",
            () -> new LegacyVariantItem(new Item.Properties().stacksTo(1), "page_of_", LegacyVariantItem.variants(
                    "page1", "page2", "page3", "page4", "page5", "page6", "page7", "page8"
            ))
    );
    public static final DeferredItem<Item> OIL_TAR = material(
            MISC_MATERIALS,
            "oil_tar",
            () -> new OilTarItem(new Item.Properties())
    );
    public static final DeferredItem<Item> BATTERY_PACK = coreItem(
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
    public static final DeferredItem<Item> BATTERY_SC = material(
            MACHINE_COMPONENTS,
            "battery_sc",
            () -> new SelfChargingBatteryItem(new Item.Properties())
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
            () -> new LegacyBarrelBlockItem(HbmBlocks.TAINT_BARREL.get(), new Item.Properties())
    );
    public static final DeferredItem<Item> YELLOW_BARREL_ITEM = coreItem(
            "yellow_barrel",
            () -> new LegacyBarrelBlockItem(HbmBlocks.YELLOW_BARREL.get(), new Item.Properties())
    );
    public static final DeferredItem<Item> VITRIFIED_BARREL_ITEM = coreItem(
            "vitrified_barrel",
            () -> new LegacyBarrelBlockItem(HbmBlocks.VITRIFIED_BARREL.get(), new Item.Properties())
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
            () -> new HbmFluidDuctItem(new Item.Properties(), "ff_fluid_duct")
    );
    public static final DeferredItem<Item> FLUID_DUCT = fluidItem(
            "fluid_duct",
            () -> new HbmFluidDuctItem(new Item.Properties(), "fluid_duct")
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
    public static final DeferredItem<Item> LEGACY_BEDROCK_ORE = legacyBedrockOre("bedrock_ore");
    public static final DeferredItem<Item> ORE_BEDROCK = legacyBedrockOre("ore_bedrock");
    public static final DeferredItem<Item> ORE_CENTRIFUGED = legacyBedrockOre("ore_centrifuged");
    public static final DeferredItem<Item> ORE_CLEANED = legacyBedrockOre("ore_cleaned");
    public static final DeferredItem<Item> ORE_SEPARATED = legacyBedrockOre("ore_separated");
    public static final DeferredItem<Item> ORE_PURIFIED = legacyBedrockOre("ore_purified");
    public static final DeferredItem<Item> ORE_NITRATED = legacyBedrockOre("ore_nitrated");
    public static final DeferredItem<Item> ORE_NITROCRYSTALLINE = legacyBedrockOre("ore_nitrocrystalline");
    public static final DeferredItem<Item> ORE_DEEPCLEANED = legacyBedrockOre("ore_deepcleaned");
    public static final DeferredItem<Item> ORE_SEARED = legacyBedrockOre("ore_seared");
    public static final DeferredItem<Item> ORE_ENRICHED = legacyBedrockOre("ore_enriched");
    public static final DeferredItem<Item> ORE_BYPRODUCT = oreDrop(
            "ore_byproduct",
            () -> new LegacyByproductItem(new Item.Properties())
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
    public static final DeferredItem<Item> PELLET_RTG_GOLD = rtgPellet("pellet_rtg_gold", 100, 200, 194_400L, "mercury");
    public static final DeferredItem<Item> PELLET_RTG_LEAD = rtgPellet("pellet_rtg_lead", 200, 600, 21_600L, "bismuth");

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
            () -> new LargeGearItem(new Item.Properties())
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
    public static final DeferredItem<Item> HAND_DRILL = toolItem(
            "hand_drill",
            () -> LegacyToolingItem.handDrill(100)
    );
    public static final DeferredItem<Item> HAND_DRILL_DESH = toolItem(
            "hand_drill_desh",
            () -> LegacyToolingItem.handDrill(0)
    );
    public static final DeferredItem<Item> WRENCH_ARCHINEER = toolItem(
            "wrench_archineer",
            LegacyToolingItem::archineerWrench
    );
    public static final DeferredItem<Item> CONVEYOR_WAND = toolItem(
            "conveyor_wand",
            () -> new ConveyorWandItem(new Item.Properties())
    );
    public static final DeferredItem<Item> CHAINSAW = toolItem(
            "chainsaw",
            LegacyChainsawItem::new
    );
    public static final DeferredItem<Item> TEMPLATE_FOLDER = toolItem(
            "template_folder",
            () -> new TemplateFolderItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> TOOLBOX = toolItem(
            "toolbox",
            ToolboxItem::new
    );
    public static final DeferredItem<Item> BLOWTORCH = toolItem(
            "blowtorch",
            () -> new BlowtorchItem(new Item.Properties(), BlowtorchItem.Kind.BLOWTORCH)
    );
    public static final DeferredItem<Item> ACETYLENE_TORCH = toolItem(
            "acetylene_torch",
            () -> new BlowtorchItem(new Item.Properties(), BlowtorchItem.Kind.ACETYLENE)
    );
    public static final DeferredItem<Item> BOLTGUN = toolItem(
            "boltgun",
            () -> new BoltgunItem(new Item.Properties())
    );
    // 1.7.10 used the same textual id for a block and this hand-held book.
    // Modern registries share one namespace, so the formal book keeps the established compatibility id.
    public static final DeferredItem<Item> BOOK_GUIDE = toolItem(
            "book_guide_book",
            () -> new GuideBookItem(new Item.Properties())
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
    public static final DeferredItem<Item> ANALYSIS_TOOL = toolItem(
            "analysis_tool",
            () -> new LegacyAnalysisToolItem(new Item.Properties())
    );
    public static final DeferredItem<Item> POLAROID = toolItem(
            "polaroid",
            () -> new LegacyPolaroidItem(new Item.Properties())
    );
    public static final DeferredItem<Item> CBT_DEVICE = toolItem(
            "cbt_device",
            () -> new LegacyCbtDeviceItem(new Item.Properties())
    );
    public static final DeferredItem<Item> CRUCIBLE = toolItem(
            "crucible",
            () -> new LegacyCrucibleItem(new Item.Properties())
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
    public static final DeferredItem<Item> RANGEFINDER = rocketMissileItem(
            "rangefinder",
            () -> new RangefinderItem(new Item.Properties())
    );
    public static final DeferredItem<Item> DESIGNATOR_ARTY_RANGE = rocketMissileItem(
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
    public static final DeferredItem<Item> WAND_K = toolItem(
            "wand_k",
            LegacyFillWandItem::new
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
    public static final DeferredItem<Item> LIQUIDATOR_HELMET = armorItem(
            "liquidator_helmet",
            () -> new LiquidatorArmorItem(HbmArmorMaterials.LIQUIDATOR, ArmorItem.Type.HELMET, true, new Item.Properties().stacksTo(1).durability(8250))
    );
    public static final DeferredItem<Item> LIQUIDATOR_PLATE = armorItem(
            "liquidator_plate",
            () -> new LiquidatorArmorItem(HbmArmorMaterials.LIQUIDATOR, ArmorItem.Type.CHESTPLATE, false, new Item.Properties().stacksTo(1).durability(12000))
    );
    public static final DeferredItem<Item> LIQUIDATOR_LEGS = armorItem(
            "liquidator_legs",
            () -> new LiquidatorArmorItem(HbmArmorMaterials.LIQUIDATOR, ArmorItem.Type.LEGGINGS, false, new Item.Properties().stacksTo(1).durability(11250))
    );
    public static final DeferredItem<Item> LIQUIDATOR_BOOTS = armorItem(
            "liquidator_boots",
            () -> new LiquidatorArmorItem(HbmArmorMaterials.LIQUIDATOR, ArmorItem.Type.BOOTS, false, new Item.Properties().stacksTo(1).durability(9750))
    );
    public static final DeferredItem<Item> SERVO_SET = armorItem(
            "servo_set",
            () -> ArmorModItem.servos(new Item.Properties())
    );
    public static final DeferredItem<Item> SERVO_SET_DESH = armorItem(
            "servo_set_desh",
            () -> ArmorModItem.servos(new Item.Properties())
    );
    public static final DeferredItem<Item> PADS_RUBBER = armorItem(
            "pads_rubber",
            () -> new ArmorPadsItem(new Item.Properties(), 0.5F, false)
    );
    public static final DeferredItem<Item> PADS_SLIME = armorItem(
            "pads_slime",
            () -> new ArmorPadsItem(new Item.Properties(), 0.0F, false)
    );
    public static final DeferredItem<Item> PADS_STATIC = armorItem(
            "pads_static",
            () -> new ArmorPadsItem(new Item.Properties(), 0.5F, true)
    );
    public static final DeferredItem<Item> LODESTONE = armorItem(
            "lodestone",
            () -> new ArmorMagnetItem(new Item.Properties(), 5)
    );
    public static final DeferredItem<Item> HORSESHOE_MAGNET = armorItem(
            "horseshoe_magnet",
            () -> new ArmorMagnetItem(new Item.Properties(), 8)
    );
    public static final DeferredItem<Item> INDUSTRIAL_MAGNET = armorItem(
            "industrial_magnet",
            () -> new ArmorMagnetItem(new Item.Properties(), 12)
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
            () -> armorInsert(1500, 1.0F, 0.9F, 1.0F, 1.0F, false, false)
    );
    public static final DeferredItem<Item> INSERT_SAPI = armorItem("insert_sapi", () -> armorInsert(1750, 1.0F, 0.85F, 1.0F, 1.0F, false, false));
    public static final DeferredItem<Item> INSERT_ESAPI = armorItem("insert_esapi", () -> armorInsert(2000, 0.95F, 0.8F, 1.0F, 1.0F, false, false));
    public static final DeferredItem<Item> INSERT_XSAPI = armorItem("insert_xsapi", () -> armorInsert(2500, 0.9F, 0.75F, 1.0F, 1.0F, false, false));
    public static final DeferredItem<Item> INSERT_STEEL = armorItem("insert_steel", () -> armorInsert(1000, 1.0F, 0.95F, 0.75F, 0.95F, false, false));
    public static final DeferredItem<Item> INSERT_DU = armorItem("insert_du", () -> armorInsert(1500, 0.9F, 0.85F, 0.5F, 0.9F, false, false));
    public static final DeferredItem<Item> INSERT_POLONIUM = armorItem("insert_polonium", () -> armorInsert(500, 0.9F, 1.0F, 0.95F, 0.9F, true, false));
    public static final DeferredItem<Item> INSERT_GHIORSIUM = armorItem("insert_ghiorsium", () -> armorInsert(2000, 0.8F, 0.75F, 0.35F, 0.9F, false, false));
    public static final DeferredItem<Item> INSERT_ERA = armorItem("insert_era", () -> armorInsert(25, 0.5F, 1.0F, 0.25F, 1.0F, false, true));
    public static final DeferredItem<Item> INSERT_YHARONITE = armorItem("insert_yharonite", () -> armorInsert(9999, 0.01F, 1.0F, 1.0F, 1.0F, false, false));
    public static final DeferredItem<Item> INSERT_DOXIUM = armorItem("insert_doxium", () -> armorInsert(9999, 5.0F, 1.0F, 1.0F, 1.0F, false, false));
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
    public static final DeferredItem<Item> BACK_TESLA = armorItem(
            "back_tesla",
            () -> new LegacyTeslaArmorModItem(new Item.Properties())
    );

    // These were initialized in ModItemsArmor rather than ModItems in 1.7.10.
    public static final DeferredItem<Item> GOGGLES = armorItem(
            "goggles",
            () -> new ArmorItem(HbmArmorMaterials.GOGGLES, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.HELMET.getDurability(15)))
    );
    public static final DeferredItem<Item> ASHGLASSES = armorItem(
            "ashglasses",
            () -> new ArmorItem(HbmArmorMaterials.ASH_GLASSES, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.HELMET.getDurability(15)))
    );
    public static final DeferredItem<Item> MASK_OF_INFAMY = armorItem(
            "mask_of_infamy",
            () -> new ArmorItem(HbmArmorMaterials.INFAMY, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.HELMET.getDurability(15)))
    );
    public static final DeferredItem<Item> MASK_RAG = armorItem(
            "mask_rag",
            () -> new ArmorItem(HbmArmorMaterials.RAGS_DAMP, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.HELMET.getDurability(150)))
    );
    public static final DeferredItem<Item> MASK_PISS = armorItem(
            "mask_piss",
            () -> new ArmorItem(HbmArmorMaterials.RAGS_PISS, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.HELMET.getDurability(150)))
    );
    public static final DeferredItem<Item> HAT = armorItem(
            "hat",
            () -> new ArmorItem(HbmArmorMaterials.HAT, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.HELMET.getDurability(40)))
    );
    public static final DeferredItem<Item> NO9 = armorItem("no9", () -> new LegacyNo9ArmorItem(HbmArmorMaterials.NO9));
    public static final DeferredItem<Item> JACKT = armorItem(
            "jackt",
            () -> new ArmorItem(HbmArmorMaterials.JACKT, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.CHESTPLATE.getDurability(30)))
    );
    public static final DeferredItem<Item> JACKT2 = armorItem(
            "jackt2",
            () -> new ArmorItem(HbmArmorMaterials.JACKT2, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.CHESTPLATE.getDurability(30)))
    );
    public static final DeferredItem<Item> EUPHEMIUM_HELMET = armorItem(
            "euphemium_helmet", () -> new LegacyEuphemiumArmorItem(HbmArmorMaterials.EUPHEMIUM, ArmorItem.Type.HELMET)
    );
    public static final DeferredItem<Item> EUPHEMIUM_PLATE = armorItem(
            "euphemium_plate", () -> new LegacyEuphemiumArmorItem(HbmArmorMaterials.EUPHEMIUM, ArmorItem.Type.CHESTPLATE)
    );
    public static final DeferredItem<Item> EUPHEMIUM_LEGS = armorItem(
            "euphemium_legs", () -> new LegacyEuphemiumArmorItem(HbmArmorMaterials.EUPHEMIUM, ArmorItem.Type.LEGGINGS)
    );
    public static final DeferredItem<Item> EUPHEMIUM_BOOTS = armorItem(
            "euphemium_boots", () -> new LegacyEuphemiumArmorItem(HbmArmorMaterials.EUPHEMIUM, ArmorItem.Type.BOOTS)
    );
    public static final DeferredItem<Item> BISMUTH_HELMET = fsbArmor(
            "bismuth_helmet", "bismuth", HbmArmorMaterials.BISMUTH, ArmorItem.Type.HELMET, 100, false,
            ArmorFSBItem.effect(MobEffects.JUMP, 20, 6),
            ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 6),
            ArmorFSBItem.effect(MobEffects.REGENERATION, 20, 1),
            ArmorFSBItem.effect(MobEffects.NIGHT_VISION, 300, 0)
    );
    public static final DeferredItem<Item> BISMUTH_PLATE = fsbArmor("bismuth_plate", "bismuth", HbmArmorMaterials.BISMUTH, ArmorItem.Type.CHESTPLATE, 100, false);
    public static final DeferredItem<Item> BISMUTH_LEGS = fsbArmor("bismuth_legs", "bismuth", HbmArmorMaterials.BISMUTH, ArmorItem.Type.LEGGINGS, 100, false);
    public static final DeferredItem<Item> BISMUTH_BOOTS = fsbArmor("bismuth_boots", "bismuth", HbmArmorMaterials.BISMUTH, ArmorItem.Type.BOOTS, 100, false);
    public static final DeferredItem<Item> ZIRCONIUM_LEGS = fsbArmor("zirconium_legs", "zirconium", HbmArmorMaterials.ZIRCONIUM, ArmorItem.Type.LEGGINGS, 1000, false);
    public static final DeferredItem<Item> BETA = legacyItem("beta", () -> new LegacyDropItem(LegacyDropItem.Kind.BETA));
    public static final DeferredItem<Item> BLACK_HOLE = legacyItem("black_hole", () -> new LegacyDropItem(LegacyDropItem.Kind.BLACK_HOLE));
    public static final DeferredItem<Item> DETONATOR = legacyItem("detonator", () -> new LegacyDetonatorItem(new Item.Properties(), "detonator"));
    public static final DeferredItem<Item> DETONATOR_DE = legacyItem("detonator_de", () -> new LegacyDropItem(LegacyDropItem.Kind.DETONATOR_DE));
    public static final DeferredItem<Item> DETONATOR_DEADMAN = legacyItem("detonator_deadman", () -> new LegacyDropItem(LegacyDropItem.Kind.DETONATOR_DEADMAN));
    public static final DeferredItem<Item> DETONATOR_LASER = legacyItem("detonator_laser", () -> new LegacyDetonatorItem(new Item.Properties(), "detonator_laser"));
    public static final DeferredItem<Item> DETONATOR_MULTI = legacyItem("detonator_multi", () -> new LegacyDetonatorItem(new Item.Properties(), "detonator_multi"));
    public static final DeferredItem<Item> SIPHON = legacyItem("siphon", () -> new LegacyFluidSiphonItem(new Item.Properties()));
    public static final DeferredItem<Item> POWER_NET_TOOL = legacyItem("power_net_tool", () -> new LegacyPowerNetToolItem(new Item.Properties()));
    public static final DeferredItem<Item> STICK_DYNAMITE = legacyItem("stick_dynamite", () ->
            new LegacyDynamiteStickItem(new Item.Properties(), com.reinhardt.hbm.entity.LegacyGrenadeEntity.Kind.DYNAMITE));
    public static final DeferredItem<Item> STICK_DYNAMITE_FISHING = legacyItem("stick_dynamite_fishing", () ->
            new LegacyDynamiteStickItem(new Item.Properties(), com.reinhardt.hbm.entity.LegacyGrenadeEntity.Kind.FISHING));
    public static final DeferredItem<Item> GRENADE_SHELL = legacyVariantItem(
            "grenade_shell", "grenade_shell", "frag", "stick", "tech", "nuke"
    );
    public static final DeferredItem<Item> GRENADE_FILLING = legacyVariantItem(
            "grenade_filling", "grenade_filling",
            "powder", "he", "demo", "inc", "wp", "cluster", "emp", "plasma", "laser",
            "cluster_heavy", "nuclear", "nuclear_demo", "schrab"
    );
    public static final DeferredItem<Item> GRENADE_FUZE = legacyVariantItem(
            "grenade_fuze", "grenade_fuze", "s3", "s7", "s15", "impact", "airburst"
    );
    public static final DeferredItem<Item> GRENADE_EXTRA = legacyVariantItem(
            "grenade_extra", "grenade_extra", "glue", "proxy_fuze", "frag_sleeve", "triplex"
    );
    public static final DeferredItem<Item> GRENADE_UNIVERSAL = legacyItem(
            "grenade_universal", () -> new UniversalGrenadeItem(new Item.Properties())
    );
    public static final DeferredItem<Item> PELLET_ANTIMATTER = legacyItem("pellet_antimatter", () -> new LegacyDropItem(LegacyDropItem.Kind.PELLET_ANTIMATTER));
    public static final DeferredItem<Item> CRYSTAL_XEN = legacyItem("crystal_xen", () -> new LegacyDropItem(LegacyDropItem.Kind.CRYSTAL_XEN));
    public static final DeferredItem<Item> SINGULARITY = legacyItem("singularity", () -> new LegacyDropItem(LegacyDropItem.Kind.SINGULARITY));
    public static final DeferredItem<Item> SINGULARITY_COUNTER_RESONANT = legacyItem("singularity_counter_resonant", () -> new LegacyDropItem(LegacyDropItem.Kind.SINGULARITY_COUNTER_RESONANT));
    public static final DeferredItem<Item> SINGULARITY_SUPER_HEATED = legacyItem("singularity_super_heated", () -> new LegacyDropItem(LegacyDropItem.Kind.SINGULARITY_SUPER_HEATED));
    public static final DeferredItem<Item> LOOT_10 = legacyItem("loot_10", () -> new LegacyLootCrateItem(MissilePartItem.LootPool.SIZE_10));
    public static final DeferredItem<Item> LOOT_15 = legacyItem("loot_15", () -> new LegacyLootCrateItem(MissilePartItem.LootPool.SIZE_15));
    public static final DeferredItem<Item> LOOT_MISC = legacyItem("loot_misc", () -> new LegacyLootCrateItem(MissilePartItem.LootPool.MISC));

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

    // Complete 1.7.10 powered-suit families. These used to be registered by the
    // legacy fallback and consequently lost both armor behaviour and the Armor tab.
    public static final DeferredItem<Item> T51_HELMET = poweredFsbArmor("t51_helmet", "t51", HbmArmorMaterials.T51, ArmorItem.Type.HELMET, false, 1_000_000L, 10_000L, 1_000L, 5L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> T51_PLATE = poweredFsbArmor("t51_plate", "t51", HbmArmorMaterials.T51, ArmorItem.Type.CHESTPLATE, false, 1_000_000L, 10_000L, 1_000L, 5L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> T51_LEGS = poweredFsbArmor("t51_legs", "t51", HbmArmorMaterials.T51, ArmorItem.Type.LEGGINGS, false, 1_000_000L, 10_000L, 1_000L, 5L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> T51_BOOTS = poweredFsbArmor("t51_boots", "t51", HbmArmorMaterials.T51, ArmorItem.Type.BOOTS, false, 1_000_000L, 10_000L, 1_000L, 5L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));

    public static final DeferredItem<Item> STEAMSUIT_HELMET = fueledFsbArmor("steamsuit_helmet", "steamsuit", HbmArmorMaterials.STEAMSUIT, ArmorItem.Type.HELMET, false, "steam", 64_000, 500, 50, 1, ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 4));
    public static final DeferredItem<Item> STEAMSUIT_PLATE = fueledFsbArmor("steamsuit_plate", "steamsuit", HbmArmorMaterials.STEAMSUIT, ArmorItem.Type.CHESTPLATE, false, "steam", 64_000, 500, 50, 1, ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 4));
    public static final DeferredItem<Item> STEAMSUIT_LEGS = fueledFsbArmor("steamsuit_legs", "steamsuit", HbmArmorMaterials.STEAMSUIT, ArmorItem.Type.LEGGINGS, false, "steam", 64_000, 500, 50, 1, ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 4));
    public static final DeferredItem<Item> STEAMSUIT_BOOTS = fueledFsbArmor("steamsuit_boots", "steamsuit", HbmArmorMaterials.STEAMSUIT, ArmorItem.Type.BOOTS, false, "steam", 64_000, 500, 50, 1, ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 4));

    public static final DeferredItem<Item> DIESELSUIT_HELMET = fueledFsbArmor("dieselsuit_helmet", "dieselsuit", HbmArmorMaterials.DIESELSUIT, ArmorItem.Type.HELMET, false, "diesel", 64_000, 500, 50, 1, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 2), ArmorFSBItem.effect(MobEffects.JUMP, 20, 2));
    public static final DeferredItem<Item> DIESELSUIT_PLATE = fueledFsbArmor("dieselsuit_plate", "dieselsuit", HbmArmorMaterials.DIESELSUIT, ArmorItem.Type.CHESTPLATE, false, "diesel", 64_000, 500, 50, 1, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 2), ArmorFSBItem.effect(MobEffects.JUMP, 20, 2));
    public static final DeferredItem<Item> DIESELSUIT_LEGS = fueledFsbArmor("dieselsuit_legs", "dieselsuit", HbmArmorMaterials.DIESELSUIT, ArmorItem.Type.LEGGINGS, false, "diesel", 64_000, 500, 50, 1, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 2), ArmorFSBItem.effect(MobEffects.JUMP, 20, 2));
    public static final DeferredItem<Item> DIESELSUIT_BOOTS = fueledFsbArmor("dieselsuit_boots", "dieselsuit", HbmArmorMaterials.DIESELSUIT, ArmorItem.Type.BOOTS, false, "diesel", 64_000, 500, 50, 1, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 2), ArmorFSBItem.effect(MobEffects.JUMP, 20, 2));

    public static final DeferredItem<Item> AJR_HELMET = poweredFsbArmor("ajr_helmet", "ajr", HbmArmorMaterials.AJR, ArmorItem.Type.HELMET, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> AJR_PLATE = poweredFsbArmor("ajr_plate", "ajr", HbmArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> AJR_LEGS = poweredFsbArmor("ajr_legs", "ajr", HbmArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> AJR_BOOTS = poweredFsbArmor("ajr_boots", "ajr", HbmArmorMaterials.AJR, ArmorItem.Type.BOOTS, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));

    public static final DeferredItem<Item> AJRO_HELMET = poweredFsbArmor("ajro_helmet", "ajro", HbmArmorMaterials.AJR, ArmorItem.Type.HELMET, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> AJRO_PLATE = poweredFsbArmor("ajro_plate", "ajro", HbmArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> AJRO_LEGS = poweredFsbArmor("ajro_legs", "ajro", HbmArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> AJRO_BOOTS = poweredFsbArmor("ajro_boots", "ajro", HbmArmorMaterials.AJR, ArmorItem.Type.BOOTS, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));

    public static final DeferredItem<Item> NCRPA_HELMET = poweredFsbArmor("ncrpa_helmet", "ncrpa", HbmArmorMaterials.NCRPA, ArmorItem.Type.HELMET, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 3));
    public static final DeferredItem<Item> NCRPA_PLATE = poweredFsbArmor("ncrpa_plate", "ncrpa", HbmArmorMaterials.NCRPA, ArmorItem.Type.CHESTPLATE, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 3));
    public static final DeferredItem<Item> NCRPA_LEGS = poweredFsbArmor("ncrpa_legs", "ncrpa", HbmArmorMaterials.NCRPA, ArmorItem.Type.LEGGINGS, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 3));
    public static final DeferredItem<Item> NCRPA_BOOTS = poweredFsbArmor("ncrpa_boots", "ncrpa", HbmArmorMaterials.NCRPA, ArmorItem.Type.BOOTS, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 3));

    public static final DeferredItem<Item> BJ_HELMET = poweredFsbArmor("bj_helmet", "blackjack", HbmArmorMaterials.BLACKJACK, ArmorItem.Type.HELMET, false, 10_000_000L, 10_000L, 1_000L, 100L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_RESISTANCE, 20, 0));
    public static final DeferredItem<Item> BJ_PLATE = poweredFsbArmor("bj_plate", "blackjack", HbmArmorMaterials.BLACKJACK, ArmorItem.Type.CHESTPLATE, false, 10_000_000L, 10_000L, 1_000L, 100L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_RESISTANCE, 20, 0));
    public static final DeferredItem<Item> BJ_PLATE_JETPACK = poweredFsbArmor("bj_plate_jetpack", "blackjack", HbmArmorMaterials.BLACKJACK, ArmorItem.Type.CHESTPLATE, false, 10_000_000L, 10_000L, 1_000L, 100L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_RESISTANCE, 20, 0));
    public static final DeferredItem<Item> BJ_LEGS = poweredFsbArmor("bj_legs", "blackjack", HbmArmorMaterials.BLACKJACK, ArmorItem.Type.LEGGINGS, false, 10_000_000L, 10_000L, 1_000L, 100L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_RESISTANCE, 20, 0));
    public static final DeferredItem<Item> BJ_BOOTS = poweredFsbArmor("bj_boots", "blackjack", HbmArmorMaterials.BLACKJACK, ArmorItem.Type.BOOTS, false, 10_000_000L, 10_000L, 1_000L, 100L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0), ArmorFSBItem.effect(MobEffects.DAMAGE_RESISTANCE, 20, 0));

    public static final DeferredItem<Item> ENVSUIT_HELMET = poweredFsbArmor("envsuit_helmet", "envsuit", HbmArmorMaterials.ENVSUIT, ArmorItem.Type.HELMET, false, 100_000L, 1_000L, 250L, 0L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0));
    public static final DeferredItem<Item> ENVSUIT_PLATE = poweredFsbArmor("envsuit_plate", "envsuit", HbmArmorMaterials.ENVSUIT, ArmorItem.Type.CHESTPLATE, false, 100_000L, 1_000L, 250L, 0L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0));
    public static final DeferredItem<Item> ENVSUIT_LEGS = poweredFsbArmor("envsuit_legs", "envsuit", HbmArmorMaterials.ENVSUIT, ArmorItem.Type.LEGGINGS, false, 100_000L, 1_000L, 250L, 0L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0));
    public static final DeferredItem<Item> ENVSUIT_BOOTS = poweredFsbArmor("envsuit_boots", "envsuit", HbmArmorMaterials.ENVSUIT, ArmorItem.Type.BOOTS, false, 100_000L, 1_000L, 250L, 0L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0));

    public static final DeferredItem<Item> HEV_HELMET = poweredFsbArmor("hev_helmet", "hev", HbmArmorMaterials.HEV, ArmorItem.Type.HELMET, false, 1_000_000L, 10_000L, 2_500L, 0L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0));
    public static final DeferredItem<Item> HEV_PLATE = poweredFsbArmor("hev_plate", "hev", HbmArmorMaterials.HEV, ArmorItem.Type.CHESTPLATE, false, 1_000_000L, 10_000L, 2_500L, 0L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0));
    public static final DeferredItem<Item> HEV_LEGS = poweredFsbArmor("hev_legs", "hev", HbmArmorMaterials.HEV, ArmorItem.Type.LEGGINGS, false, 1_000_000L, 10_000L, 2_500L, 0L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0));
    public static final DeferredItem<Item> HEV_BOOTS = poweredFsbArmor("hev_boots", "hev", HbmArmorMaterials.HEV, ArmorItem.Type.BOOTS, false, 1_000_000L, 10_000L, 2_500L, 0L, ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 0));

    public static final DeferredItem<Item> FAU_HELMET = poweredFsbArmor("fau_helmet", "fau", HbmArmorMaterials.FAU, ArmorItem.Type.HELMET, false, 10_000_000L, 10_000L, 2_500L, 0L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 1));
    public static final DeferredItem<Item> FAU_PLATE = poweredFsbArmor("fau_plate", "fau", HbmArmorMaterials.FAU, ArmorItem.Type.CHESTPLATE, false, 10_000_000L, 10_000L, 2_500L, 0L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 1));
    public static final DeferredItem<Item> FAU_LEGS = poweredFsbArmor("fau_legs", "fau", HbmArmorMaterials.FAU, ArmorItem.Type.LEGGINGS, false, 10_000_000L, 10_000L, 2_500L, 0L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 1));
    public static final DeferredItem<Item> FAU_BOOTS = poweredFsbArmor("fau_boots", "fau", HbmArmorMaterials.FAU, ArmorItem.Type.BOOTS, false, 10_000_000L, 10_000L, 2_500L, 0L, ArmorFSBItem.effect(MobEffects.JUMP, 20, 1));

    public static final DeferredItem<Item> DNS_HELMET = poweredFsbArmor("dns_helmet", "dns", HbmArmorMaterials.DNS, ArmorItem.Type.HELMET, false, 1_000_000_000L, 1_000_000L, 100_000L, 115L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 9), ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 7), ArmorFSBItem.effect(MobEffects.JUMP, 20, 2));
    public static final DeferredItem<Item> DNS_PLATE = poweredFsbArmor("dns_plate", "dns", HbmArmorMaterials.DNS, ArmorItem.Type.CHESTPLATE, false, 1_000_000_000L, 1_000_000L, 100_000L, 115L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 9), ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 7), ArmorFSBItem.effect(MobEffects.JUMP, 20, 2));
    public static final DeferredItem<Item> DNS_LEGS = poweredFsbArmor("dns_legs", "dns", HbmArmorMaterials.DNS, ArmorItem.Type.LEGGINGS, false, 1_000_000_000L, 1_000_000L, 100_000L, 115L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 9), ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 7), ArmorFSBItem.effect(MobEffects.JUMP, 20, 2));
    public static final DeferredItem<Item> DNS_BOOTS = poweredFsbArmor("dns_boots", "dns", HbmArmorMaterials.DNS, ArmorItem.Type.BOOTS, false, 1_000_000_000L, 1_000_000L, 100_000L, 115L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 9), ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 7), ArmorFSBItem.effect(MobEffects.JUMP, 20, 2));

    public static final DeferredItem<Item> TAURUN_HELMET = fsbArmor("taurun_helmet", "taurun", HbmArmorMaterials.TAURUN, ArmorItem.Type.HELMET, 150, false, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> TAURUN_PLATE = fsbArmor("taurun_plate", "taurun", HbmArmorMaterials.TAURUN, ArmorItem.Type.CHESTPLATE, 150, false, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> TAURUN_LEGS = fsbArmor("taurun_legs", "taurun", HbmArmorMaterials.TAURUN, ArmorItem.Type.LEGGINGS, 150, false, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));
    public static final DeferredItem<Item> TAURUN_BOOTS = fsbArmor("taurun_boots", "taurun", HbmArmorMaterials.TAURUN, ArmorItem.Type.BOOTS, 150, false, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 0));

    public static final DeferredItem<Item> TRENCHMASTER_HELMET = fsbArmor("trenchmaster_helmet", "trenchmaster", HbmArmorMaterials.TRENCHMASTER, ArmorItem.Type.HELMET, 150, false, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 2), ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 1), ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 0));
    public static final DeferredItem<Item> TRENCHMASTER_PLATE = fsbArmor("trenchmaster_plate", "trenchmaster", HbmArmorMaterials.TRENCHMASTER, ArmorItem.Type.CHESTPLATE, 150, false, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 2), ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 1), ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 0));
    public static final DeferredItem<Item> TRENCHMASTER_LEGS = fsbArmor("trenchmaster_legs", "trenchmaster", HbmArmorMaterials.TRENCHMASTER, ArmorItem.Type.LEGGINGS, 150, false, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 2), ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 1), ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 0));
    public static final DeferredItem<Item> TRENCHMASTER_BOOTS = fsbArmor("trenchmaster_boots", "trenchmaster", HbmArmorMaterials.TRENCHMASTER, ArmorItem.Type.BOOTS, 150, false, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 2), ArmorFSBItem.effect(MobEffects.DIG_SPEED, 20, 1), ArmorFSBItem.effect(MobEffects.JUMP, 20, 1), ArmorFSBItem.effect(MobEffects.MOVEMENT_SPEED, 20, 0));

    public static final DeferredItem<Item> TITANIUM_SWORD = sword("titanium_sword", profile(HbmToolTier.TITANIUM, 6.5F, -2.4F));
    public static final DeferredItem<Item> TITANIUM_PICKAXE = pickaxe("titanium_pickaxe", profile(HbmToolTier.TITANIUM, 4.5F, -2.8F));
    public static final DeferredItem<Item> TITANIUM_AXE = axe("titanium_axe", profile(HbmToolTier.TITANIUM, 5.5F, -2.8F).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> TITANIUM_SHOVEL = shovel("titanium_shovel", profile(HbmToolTier.TITANIUM, 3.5F, -2.8F));
    public static final DeferredItem<Item> TITANIUM_HOE = hoe("titanium_hoe", HbmToolTier.TITANIUM);

    public static final DeferredItem<Item> STEEL_SWORD = sword("steel_sword", profile(HbmToolTier.STEEL, 6.0F, -2.4F));
    public static final DeferredItem<Item> STEEL_PICKAXE = pickaxe("steel_pickaxe", profile(HbmToolTier.STEEL, 4.0F, -2.8F));
    public static final DeferredItem<Item> STEEL_AXE = axe("steel_axe", profile(HbmToolTier.STEEL, 5.0F, -2.8F).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> STEEL_SHOVEL = shovel("steel_shovel", profile(HbmToolTier.STEEL, 3.0F, -2.8F));
    public static final DeferredItem<Item> STEEL_HOE = hoe("steel_hoe", HbmToolTier.STEEL);

    public static final DeferredItem<Item> ALLOY_SWORD = sword("alloy_sword", profile(HbmToolTier.ALLOY, 8.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.STUN, 0));
    public static final DeferredItem<Item> ALLOY_PICKAXE = pickaxe("alloy_pickaxe", profile(HbmToolTier.ALLOY, 5.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 0));
    public static final DeferredItem<Item> ALLOY_AXE = axe("alloy_axe", profile(HbmToolTier.ALLOY, 7.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> ALLOY_SHOVEL = shovel("alloy_shovel", profile(HbmToolTier.ALLOY, 4.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 0));

    public static final DeferredItem<Item> ELEC_SWORD = sword("elec_sword", profile(HbmToolTier.ELEC, 12.5F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.STUN, 2));
    public static final DeferredItem<Item> ELEC_PICKAXE = pickaxe("elec_pickaxe", profile(HbmToolTier.ELEC, 10.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> ELEC_AXE = axe("elec_axe", profile(HbmToolTier.ELEC, 10.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1).weapon(HbmToolBehavior.WeaponAbility.CHAINSAW, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> ELEC_SHOVEL = shovel("elec_shovel", profile(HbmToolTier.ELEC, 7.5F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> CENTRI_STICK = pickaxe(
            "centri_stick",
            profile(HbmToolTier.ELEC, 3.0F, -2.8F).asMiner().harvest(HbmToolBehavior.HarvestAbility.CENTRIFUGE, 0),
            50
    );
    public static final DeferredItem<Item> SMASHING_HAMMER = pickaxe(
            "smashing_hammer",
            profile(HbmToolTier.STEEL, 12.0F, -2.8F).movement(-0.1D).asMiner().harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0),
            2_500
    );
    public static final DeferredItem<Item> DRAX = poweredPickaxe(
            "drax",
            profile(HbmToolTier.ELEC, 10.0F, -2.8F).movement(-0.05D).asMiner()
                    .harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.LUCK, 1)
                    .area(HbmToolBehavior.AreaAbility.HAMMER, 1)
                    .area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1)
                    .area(HbmToolBehavior.AreaAbility.RECURSION, 2),
            500_000_000L, 100_000L, 5_000L
    );
    public static final DeferredItem<Item> DRAX_MK2 = poweredPickaxe(
            "drax_mk2",
            profile(HbmToolTier.ELEC, 15.0F, -2.8F).movement(-0.05D).asMiner()
                    .harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.CENTRIFUGE, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.LUCK, 2)
                    .area(HbmToolBehavior.AreaAbility.HAMMER, 2)
                    .area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 2)
                    .area(HbmToolBehavior.AreaAbility.RECURSION, 4),
            1_000_000_000L, 250_000L, 7_500L
    );
    public static final DeferredItem<Item> DRAX_MK3 = poweredPickaxe(
            "drax_mk3",
            profile(HbmToolTier.ELEC, 20.0F, -2.8F).movement(-0.05D).asMiner()
                    .harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.SHREDDER, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.CENTRIFUGE, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.CRYSTALLIZER, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.SILK, 0)
                    .harvest(HbmToolBehavior.HarvestAbility.LUCK, 3)
                    .area(HbmToolBehavior.AreaAbility.HAMMER, 3)
                    .area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 3)
                    .area(HbmToolBehavior.AreaAbility.RECURSION, 5),
            2_500_000_000L, 500_000L, 10_000L
    );

    public static final DeferredItem<Item> DESH_SWORD = sword("desh_sword", profile(HbmToolTier.DESH, 15.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.STUN, 0));
    public static final DeferredItem<Item> DESH_PICKAXE = pickaxe("desh_pickaxe", profile(HbmToolTier.DESH, 5.0F, -2.8F).movement(-0.05D).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).area(HbmToolBehavior.AreaAbility.RECURSION, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> DESH_AXE = axe("desh_axe", profile(HbmToolTier.DESH, 6.5F, -2.8F).movement(-0.05D).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).area(HbmToolBehavior.AreaAbility.RECURSION, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> DESH_SHOVEL = shovel("desh_shovel", profile(HbmToolTier.DESH, 4.0F, -2.8F).movement(-0.05D).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).area(HbmToolBehavior.AreaAbility.RECURSION, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> DESH_HOE = hoe("desh_hoe", HbmToolTier.DESH);

    public static final DeferredItem<Item> COBALT_SWORD = sword("cobalt_sword", profile(HbmToolTier.COBALT, 12.0F, -2.4F));
    public static final DeferredItem<Item> COBALT_PICKAXE = pickaxe("cobalt_pickaxe", profile(HbmToolTier.COBALT, 4.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 0));
    public static final DeferredItem<Item> COBALT_AXE = axe("cobalt_axe", profile(HbmToolTier.COBALT, 6.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> COBALT_SHOVEL = shovel("cobalt_shovel", profile(HbmToolTier.COBALT, 3.5F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 0));
    public static final DeferredItem<Item> COBALT_HOE = hoe("cobalt_hoe", HbmToolTier.COBALT);

    public static final DeferredItem<Item> COBALT_DECORATED_SWORD = sword("cobalt_decorated_sword", profile(HbmToolTier.COBALT_DECORATED, 15.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.BOBBLE, 0));
    public static final DeferredItem<Item> COBALT_DECORATED_PICKAXE = pickaxe("cobalt_decorated_pickaxe", profile(HbmToolTier.COBALT_DECORATED, 6.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> COBALT_DECORATED_AXE = axe("cobalt_decorated_axe", profile(HbmToolTier.COBALT_DECORATED, 8.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> COBALT_DECORATED_SHOVEL = shovel("cobalt_decorated_shovel", profile(HbmToolTier.COBALT_DECORATED, 5.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 1).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> COBALT_DECORATED_HOE = hoe("cobalt_decorated_hoe", HbmToolTier.COBALT_DECORATED);

    public static final DeferredItem<Item> STARMETAL_SWORD = sword("starmetal_sword", profile(HbmToolTier.STARMETAL, 25.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).weapon(HbmToolBehavior.WeaponAbility.STUN, 1).weapon(HbmToolBehavior.WeaponAbility.BOBBLE, 0));
    public static final DeferredItem<Item> STARMETAL_PICKAXE = pickaxe("starmetal_pickaxe", profile(HbmToolTier.STARMETAL, 8.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 3).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).weapon(HbmToolBehavior.WeaponAbility.STUN, 1));
    public static final DeferredItem<Item> STARMETAL_AXE = axe("starmetal_axe", profile(HbmToolTier.STARMETAL, 12.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 3).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).weapon(HbmToolBehavior.WeaponAbility.STUN, 1));
    public static final DeferredItem<Item> STARMETAL_SHOVEL = shovel("starmetal_shovel", profile(HbmToolTier.STARMETAL, 7.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 3).area(HbmToolBehavior.AreaAbility.HAMMER, 1).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 1).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 4).weapon(HbmToolBehavior.WeaponAbility.STUN, 1));
    public static final DeferredItem<Item> STARMETAL_HOE = hoe("starmetal_hoe", HbmToolTier.STARMETAL);

    public static final DeferredItem<Item> CMB_SWORD = sword("cmb_sword", profile(HbmToolTier.CMB, 35.0F, -2.4F).weapon(HbmToolBehavior.WeaponAbility.STUN, 0).weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 0));
    public static final DeferredItem<Item> CMB_PICKAXE = pickaxe("cmb_pickaxe", profile(HbmToolTier.CMB, 10.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> CMB_AXE = axe("cmb_axe", profile(HbmToolTier.CMB, 30.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> CMB_SHOVEL = shovel("cmb_shovel", profile(HbmToolTier.CMB, 8.0F, -2.8F).area(HbmToolBehavior.AreaAbility.RECURSION, 2).harvest(HbmToolBehavior.HarvestAbility.SMELTER, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> CMB_HOE = hoe("cmb_hoe", HbmToolTier.CMB);

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
    public static final DeferredItem<Item> SCHRABIDIUM_HOE = hoe("schrabidium_hoe", HbmToolTier.SCHRABIDIUM, Rarity.RARE);

    public static final DeferredItem<Item> MESE_PICKAXE = pickaxe("mese_pickaxe", profile(HbmToolTier.MESE, 35.0F, -2.8F).asMiner().area(HbmToolBehavior.AreaAbility.HAMMER, 2).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 2).area(HbmToolBehavior.AreaAbility.RECURSION, 2).area(HbmToolBehavior.AreaAbility.EXPLOSION, 3).harvest(HbmToolBehavior.HarvestAbility.CRYSTALLIZER, 0).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 5).weapon(HbmToolBehavior.WeaponAbility.STUN, 3).weapon(HbmToolBehavior.WeaponAbility.PHOSPHORUS, 0).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0).canBreakDepthRock());
    public static final DeferredItem<Item> MESE_AXE = axe("mese_axe", profile(HbmToolTier.MESE, 75.0F, -2.8F).area(HbmToolBehavior.AreaAbility.HAMMER, 2).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 2).area(HbmToolBehavior.AreaAbility.RECURSION, 2).area(HbmToolBehavior.AreaAbility.EXPLOSION, 3).harvest(HbmToolBehavior.HarvestAbility.SILK, 0).harvest(HbmToolBehavior.HarvestAbility.LUCK, 5).weapon(HbmToolBehavior.WeaponAbility.STUN, 4).weapon(HbmToolBehavior.WeaponAbility.PHOSPHORUS, 1).weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> MESE_GAVEL = sword("mese_gavel", profile(HbmToolTier.MESE, 250.0F, -2.4F)
            .movement(1.5D)
            .weapon(HbmToolBehavior.WeaponAbility.PHOSPHORUS, 0)
            .weapon(HbmToolBehavior.WeaponAbility.RADIATION, 2)
            .weapon(HbmToolBehavior.WeaponAbility.STUN, 3)
            .weapon(HbmToolBehavior.WeaponAbility.VAMPIRE, 4)
            .weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0));
    public static final DeferredItem<Item> DNT_SWORD = sword("dnt_sword", profile(HbmToolTier.MESE, 12.0F, -2.4F));
    public static final DeferredItem<Item> DWARVEN_PICKAXE = pickaxe("dwarven_pickaxe", profile(HbmToolTier.DWARVEN, 5.0F, -2.8F).asMiner().movement(-0.1D).area(HbmToolBehavior.AreaAbility.HAMMER, 0).area(HbmToolBehavior.AreaAbility.HAMMER_FLAT, 0));

    public static final DeferredItem<Item> REDSTONE_SWORD = sword("redstone_sword", profile(HbmToolTier.STONE_COMPAT, 5.0F, -2.4F).special(HbmToolBehavior.SpecialBehavior.REDSTONE_SWORD));
    public static final DeferredItem<Item> CROWBAR = sword("crowbar", profile(HbmToolTier.STEEL, 6.0F, -2.4F));
    public static final DeferredItem<Item> WEAPON_PIPE_LEAD = sword("weapon_pipe_lead", profile(HbmToolTier.PIPE_LEAD, 7.0F, -2.4F));
    public static final DeferredItem<Item> REER_GRAAR = sword("reer_graar", profile(HbmToolTier.TITANIUM, 6.5F, -2.4F));
    public static final DeferredItem<Item> BOTTLE_OPENER = specialWeapon("bottle_opener", profile(HbmToolTier.BOTTLE_OPENER, 4.5F, -2.4F), HbmSpecialWeaponItem.Kind.BOTTLE_OPENER);
    public static final DeferredItem<Item> CHERNOBYLSIGN = specialWeapon("chernobylsign", profile(HbmToolTier.ALLOY, 9.0F, -2.4F), HbmSpecialWeaponItem.Kind.CHERNOBYL_SIGN);
    public static final DeferredItem<Item> DIAMOND_GAVEL = specialWeapon("diamond_gavel", profile(HbmToolTier.DIAMOND_COMPAT, 7.0F, -2.4F), HbmSpecialWeaponItem.Kind.DIAMOND_GAVEL);
    public static final DeferredItem<Item> LEAD_GAVEL = specialWeapon("lead_gavel", profile(HbmToolTier.STEEL, 6.0F, -2.4F), HbmSpecialWeaponItem.Kind.LEAD_GAVEL);
    public static final DeferredItem<Item> MEMESPOON = specialWeapon("memespoon", profile(HbmToolTier.STEEL, 6.0F, -2.4F), HbmSpecialWeaponItem.Kind.MEME_SPOON);
    public static final DeferredItem<Item> SCHRABIDIUM_HAMMER = specialWeapon("schrabidium_hammer", profile(HbmToolTier.SCHRABIDIUM_HAMMER, 1_000_000_000.0F, -2.4F).rarity(Rarity.RARE), HbmSpecialWeaponItem.Kind.SCHRABIDIUM_HAMMER);
    public static final DeferredItem<Item> SHIMMER_SLEDGE = specialWeapon("shimmer_sledge", profile(HbmToolTier.SHIMMER, 30.0F, -2.4F).movement(-0.2D).rarity(Rarity.EPIC), HbmSpecialWeaponItem.Kind.SHIMMER_SLEDGE);
    public static final DeferredItem<Item> SOPSIGN = specialWeapon("sopsign", profile(HbmToolTier.ALLOY, 9.0F, -2.4F), HbmSpecialWeaponItem.Kind.SOP_SIGN);
    public static final DeferredItem<Item> STOPSIGN = specialWeapon("stopsign", profile(HbmToolTier.ALLOY, 9.0F, -2.4F), HbmSpecialWeaponItem.Kind.STOP_SIGN);
    public static final DeferredItem<Item> ULLAPOOL_CABER = specialWeapon("ullapool_caber", profile(HbmToolTier.STEEL, 6.0F, -2.4F), HbmSpecialWeaponItem.Kind.ULLAPOOL_CABER);
    public static final DeferredItem<Item> WOOD_GAVEL = specialWeapon("wood_gavel", profile(HbmToolTier.WOOD_COMPAT, 4.0F, -2.4F), HbmSpecialWeaponItem.Kind.WOOD_GAVEL);
    public static final DeferredItem<Item> WRENCH_FLIPPED = specialWeapon("wrench_flipped", profile(HbmToolTier.ELEC, 16.0F, -2.4F).movement(-0.1D), HbmSpecialWeaponItem.Kind.WRENCH_FLIPPED);
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

    // Direct 1.7.10 metadata items. Do not leave these behind as generic
    // catalog entries: recipes and creative tabs use every individual state.
    public static final DeferredItem<Item> CIRCUIT_STAR_COMPONENT = machineComponent(
            "circuit_star_component",
            () -> new LegacyVariantItem(new Item.Properties(), "circuit_star_component", LegacyVariantItem.variants(
                    "chipset", "cpu", "ram", "card"
            ))
    );
    public static final DeferredItem<Item> CIRCUIT_STAR_PIECE = machineComponent(
            "circuit_star_piece",
            () -> new LegacyVariantItem(new Item.Properties(), "circuit_star_piece", LegacyVariantItem.variants(
                    "board_blank", "board_transistor", "board_converter",
                    "bridge_north", "bridge_south", "bridge_io", "bridge_bus", "bridge_chipset", "bridge_cmos", "bridge_bios",
                    "cpu_register", "cpu_clock", "cpu_logic", "cpu_cache", "cpu_ext", "cpu_socket",
                    "mem_socket", "mem_16k_a", "mem_16k_b", "mem_16k_c", "mem_16k_d",
                    "card_board", "card_processor"
            ))
    );
    public static final DeferredItem<Item> INGOT_METAL = material(
            MISC_MATERIALS,
            "ingot_metal",
            () -> new LegacyVariantItem(new Item.Properties(), "ingot_metal", LegacyVariantItem.variants(
                    "scrap", "ingot", "counter", "key", "beacon", "casing", "clockwork", "bar", "detector"
            ))
    );
    public static final DeferredItem<Item> UPGRADE_STACK = machineComponent(
            "upgrade_stack", () -> new LegacyMetaUpgradeItem(new Item.Properties().stacksTo(1), "upgrade_stack")
    );
    public static final DeferredItem<Item> UPGRADE_EJECTOR = machineComponent(
            "upgrade_ejector", () -> new LegacyMetaUpgradeItem(new Item.Properties().stacksTo(1), "upgrade_ejector")
    );
    public static final DeferredItem<Item> LASER_CRYSTAL_DNT = reactorItem(
            "laser_crystal_dnt", () -> new FelCrystalItem(new Item.Properties(), com.reinhardt.hbm.util.Wavelength.GAMMA)
    );
    public static final DeferredItem<Item> CUBE_POWER = material(
            MISC_MATERIALS,
            "cube_power",
            () -> new FixedBatteryItem(new Item.Properties(), 1_000_000_000_000_000_000L, 1_000_000_000_000_000L, 1_000_000_000_000_000L)
    );

    public static final DeferredItem<Item> RPA_HELMET = poweredFsbArmor("rpa_helmet", "rpa", HbmArmorMaterials.AJR, ArmorItem.Type.HELMET, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 3));
    public static final DeferredItem<Item> RPA_PLATE = poweredFsbArmor("rpa_plate", "rpa", HbmArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 3));
    public static final DeferredItem<Item> RPA_LEGS = poweredFsbArmor("rpa_legs", "rpa", HbmArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 3));
    public static final DeferredItem<Item> RPA_BOOTS = poweredFsbArmor("rpa_boots", "rpa", HbmArmorMaterials.AJR, ArmorItem.Type.BOOTS, false, 2_500_000L, 10_000L, 2_000L, 25L, ArmorFSBItem.effect(MobEffects.DAMAGE_BOOST, 20, 3));

    public static final DeferredItem<Item> HEV_BATTERY = legacyPowerItem(
            "hev_battery", () -> new FusionCoreItem(new Item.Properties().stacksTo(4), 150_000L)
    );
    public static final DeferredItem<Item> FUSION_CORE = legacyPowerItem(
            "fusion_core", () -> new FusionCoreItem(new Item.Properties().stacksTo(1), 2_500_000L)
    );
    // Deprecated in 1.7.10, but still used by the centrifuge stick and legacy machine fuel lists.
    public static final DeferredItem<Item> ENERGY_CORE = legacyPowerItem(
            "energy_core", () -> new FixedBatteryItem(new Item.Properties(), 10_000_000L, 0L, 1_000L)
    );
    public static final DeferredItem<Item> BATTERY_POTATO = legacyPowerItem(
            "battery_potato", () -> new FixedBatteryItem(new Item.Properties(), 1_000L, 0L, 100L)
    );
    public static final DeferredItem<Item> BATTERY_POTATOS = legacyPowerItem(
            "battery_potatos", () -> new LegacyPotatosBatteryItem(new Item.Properties(), 500_000L, 0L, 100L)
    );
    public static final DeferredItem<Item> BDCL = legacyItem("bdcl", () -> new LegacyBdclItem(new Item.Properties()));
    public static final DeferredItem<Item> GEM_ALEXANDRITE = legacyItem(
            "gem_alexandrite", () -> new LegacyAlexandriteItem(new Item.Properties())
    );
    public static final DeferredItem<Item> ANCHOR_REMOTE = legacyItem(
            "anchor_remote", () -> new LegacyAnchorRemoteItem(new Item.Properties())
    );
    public static final DeferredItem<Item> FUSE = material(MISC_MATERIALS, "fuse");

    // Direct registrations for the remaining 1.7.10 gameplay items. Keeping
    // these here prevents LegacyHbmContent from treating real items as catalog
    // fallbacks while preserving their original specialised implementations.
    public static final DeferredItem<Item> APPLE_EUPHEMIUM = legacyItem("apple_euphemium", () -> LegacySpecialFoodItem.fromLegacyId("apple_euphemium"));
    public static final DeferredItem<Item> APPLE_LEAD = legacyItem("apple_lead", () -> LegacySpecialFoodItem.fromLegacyId("apple_lead"));
    public static final DeferredItem<Item> APPLE_SCHRABIDIUM = legacyItem("apple_schrabidium", () -> LegacySpecialFoodItem.fromLegacyId("apple_schrabidium"));
    public static final DeferredItem<Item> BALEFIRE_AND_HAM = legacySoup("balefire_and_ham");
    public static final DeferredItem<Item> BALEFIRE_SCRAMBLED = legacySoup("balefire_scrambled");
    public static final DeferredItem<Item> BOMB_WAFFLE = legacyItem("bomb_waffle", () -> LegacySpecialFoodItem.fromLegacyId("bomb_waffle"));
    public static final DeferredItem<Item> CANNED_CONSERVE = legacyItem("canned_conserve", () -> new LegacyConserveItem(new Item.Properties()));
    public static final DeferredItem<Item> CANTEEN_VODKA = legacyItem("canteen_vodka", () -> new LegacyCanteenItem(new Item.Properties()));
    public static final DeferredItem<Item> CHEESE = legacyLemon("cheese");
    public static final DeferredItem<Item> CHEESE_QUESADILLA = legacyLemon("cheese_quesadilla");
    public static final DeferredItem<Item> COLTAN_TOOL = legacyItem("coltan_tool", () -> new ColtanCompassItem(new Item.Properties()));
    public static final DeferredItem<Item> COTTON_CANDY = legacyItem("cotton_candy", () -> LegacySpecialFoodItem.fromLegacyId("cotton_candy"));
    public static final DeferredItem<Item> CRAYON = legacyItem("crayon", () -> new LegacyCrayonItem(new Item.Properties()));
    public static final DeferredItem<Item> DEFINITELYFOOD = legacyLemon("definitelyfood");
    public static final DeferredItem<Item> DESIGNATOR_RANGE = legacyItem("designator_range", () -> new LegacyRangeDesignatorItem(new Item.Properties()));
    public static final DeferredItem<Item> FLASK_INFUSION = legacyItem("flask_infusion", () -> new LegacyFlaskItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FOODITEM = legacyLemon("fooditem");
    public static final DeferredItem<Item> GLOWING_STEW = legacySoup("glowing_stew");
    public static final DeferredItem<Item> IV_BLOOD = legacyMedical("iv_blood");
    public static final DeferredItem<Item> IV_EMPTY = legacyMedical("iv_empty");
    public static final DeferredItem<Item> IV_XP = legacyMedical("iv_xp");
    public static final DeferredItem<Item> IV_XP_EMPTY = legacyMedical("iv_xp_empty");
    public static final DeferredItem<Item> LEMON = legacyLemon("lemon");
    public static final DeferredItem<Item> LOOP_STEW = legacyLemon("loop_stew");
    public static final DeferredItem<Item> LOOPS = legacyLemon("loops");
    public static final DeferredItem<Item> MED_IPECAC = legacyLemon("med_ipecac");
    public static final DeferredItem<Item> MED_PTSD = legacyLemon("med_ptsd");
    public static final DeferredItem<Item> MEMORY = legacyItem("memory", () -> new FixedBatteryItem(
            new Item.Properties(), Long.MAX_VALUE / 100L, 100_000_000_000_000L, 100_000_000_000_000L
    ));
    public static final DeferredItem<Item> MISSILE_CUSTOM = legacyItem("missile_custom", () -> new CustomMissileItem(new Item.Properties()));
    public static final DeferredItem<Item> MUCHO_MANGO = legacyItem("mucho_mango", () -> LegacySpecialFoodItem.fromLegacyId("mucho_mango"));
    public static final DeferredItem<Item> NUGGET = legacyLemon("nugget");
    public static final DeferredItem<Item> PANCAKE = legacyItem("pancake", () -> LegacySpecialFoodItem.fromLegacyId("pancake"));
    public static final DeferredItem<Item> PIPETTE = legacyItem("pipette", () -> new LegacyPipetteItem(new Item.Properties(), LegacyPipetteItem.Kind.NORMAL));
    public static final DeferredItem<Item> PIPETTE_BORON = legacyItem("pipette_boron", () -> new LegacyPipetteItem(new Item.Properties(), LegacyPipetteItem.Kind.BORON));
    public static final DeferredItem<Item> PIPETTE_LABORATORY = legacyItem("pipette_laboratory", () -> new LegacyPipetteItem(new Item.Properties(), LegacyPipetteItem.Kind.LABORATORY));
    public static final DeferredItem<Item> PUDDING = legacyLemon("pudding");
    public static final DeferredItem<Item> RADAWAY = legacyMedical("radaway");
    public static final DeferredItem<Item> RADAWAY_FLUSH = legacyMedical("radaway_flush");
    public static final DeferredItem<Item> RADAWAY_STRONG = legacyMedical("radaway_strong");
    public static final DeferredItem<Item> SCHNITZEL_VEGAN = legacyItem("schnitzel_vegan", () -> LegacySpecialFoodItem.fromLegacyId("schnitzel_vegan"));
    public static final DeferredItem<Item> SPONGEBOB_MACARONI = legacyLemon("spongebob_macaroni");
    public static final DeferredItem<Item> STATIC_SANDWICH = legacyLemon("static_sandwich");
    public static final DeferredItem<Item> SYRINGE_ANTIDOTE = legacyMedical("syringe_antidote");
    public static final DeferredItem<Item> SYRINGE_AWESOME = legacyMedical("syringe_awesome");
    public static final DeferredItem<Item> SYRINGE_POISON = legacyMedical("syringe_poison");
    public static final DeferredItem<Item> TEM_FLAKES = legacyItem("tem_flakes", () -> LegacySpecialFoodItem.fromLegacyId("tem_flakes"));
    public static final DeferredItem<Item> TWINKIE = legacyLemon("twinkie");

    // Metadata items from ItemEnumMulti and its direct subclasses.
    public static final DeferredItem<Item> ACHIEVEMENT_ICON = legacyVariantItem(
            "achievement_icon", "achievement_icon",
            "gofish", "acid", "balls", "digammasee", "digammafeel", "digammaknow", "digammakauaimoho",
            "digammaupontop", "digammaforourright", "questionmark"
    );
    public static final DeferredItem<Item> ITEM_SECRET = legacyVariantItem(
            "item_secret", "item_secret", "canister", "controller", "selenium_steel", "aberrator", "folly"
    );
    public static final DeferredItem<Item> PARTS_LEGENDARY = legacyVariantItem(
            "parts_legendary", "parts_legendary", false, "tier1", "tier2", "tier3"
    );
    public static final DeferredItem<Item> SCRAP_PLASTIC = legacyVariantItem(
            "scrap_plastic", "scrap_plastic",
            false,
            "board_blank", "board_transistor", "board_converter", "bridge_north", "bridge_south", "bridge_io",
            "bridge_bus", "bridge_chipset", "bridge_cmos", "bridge_bios", "cpu_register", "cpu_clock", "cpu_logic",
            "cpu_cache", "cpu_ext", "cpu_socket", "mem_socket", "mem_16k_a", "mem_16k_b", "mem_16k_c",
            "mem_16k_d", "card_board", "card_processor"
    );
    public static final DeferredItem<Item> CHEMICAL_DYE = legacyItem("chemical_dye", () -> new LegacyChemicalDyeItem(new Item.Properties()));

    public static final DeferredItem<Item> BALEFIRE_AND_STEEL = legacyItem("balefire_and_steel", () -> new LegacyBalefireMatchItem(new Item.Properties()));
    public static final DeferredItem<Item> BISMUTH_TOOL = legacyItem("bismuth_tool", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> COUPLING_TOOL = legacyItem("coupling_tool", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOOK_LORE = legacyItem("book_lore", () -> new LegacyBookLoreItem(new Item.Properties()));
    public static final DeferredItem<Item> BOOK_OF = legacyItem("book_of_", () -> new LegacyCraftBookItem(new Item.Properties(), LegacyCraftBookItem.Kind.BOXCARS));
    public static final DeferredItem<Item> BOOK_LEMEGETON = legacyItem("book_lemegeton", () -> new LegacyCraftBookItem(new Item.Properties(), LegacyCraftBookItem.Kind.LEMEGETON));
    public static final DeferredItem<Item> KIT_CUSTOM = legacyItem("kit_custom", () -> new LegacyCustomKitItem(new Item.Properties()));
    public static final DeferredItem<Item> DEMON_CORE_OPEN = legacyItem("demon_core_open", () -> new LegacyDemonCoreItem(new Item.Properties()));
    public static final DeferredItem<Item> MATCHSTICK = legacyItem("matchstick", () -> new LegacyMatchItem(new Item.Properties()));
    public static final DeferredItem<Item> N2_CHARGE = legacyItem("n2_charge", () -> new LegacyN2ChargeItem(new Item.Properties()));
    public static final DeferredItem<Item> MARSHMALLOW = legacyItem("marshmallow", () -> new LegacyMarshmallowItem(new Item.Properties()));

    // 1.7.10 ItemModRecord entries. The song component gives vanilla jukeboxes
    // the same insertion, comparator and tooltip behavior as normal records.
    public static final DeferredItem<Item> RECORD_LC = record("record_lc");
    public static final DeferredItem<Item> RECORD_SS = record("record_ss");
    public static final DeferredItem<Item> RECORD_VC = record("record_vc");
    public static final DeferredItem<Item> RECORD_GLASS = record("record_glass");

    public static final DeferredItem<Item> BOAT_RUBBER = toolItem(
            "boat_rubber",
            () -> new LegacyRubberBoatItem(new Item.Properties())
    );
    public static final DeferredItem<Item> CART = toolItem(
            "cart",
            () -> new LegacyMinecartItem(new Item.Properties())
    );
    public static final DeferredItem<Item> TRAIN = toolItem(
            "train",
            () -> new LegacyTrainItem(new Item.Properties())
    );
    public static final DeferredItem<Item> BOMB_CALLER = toolItem(
            "bomb_caller",
            () -> new LegacyBombCallerItem(new Item.Properties())
    );
    public static final DeferredItem<Item> BOBMAZON = toolItem(
            "bobmazon",
            () -> new LegacyBobmazonItem(new Item.Properties(), false)
    );
    public static final DeferredItem<Item> BOBMAZON_HIDDEN = legacyItem(
            "bobmazon_hidden",
            () -> new LegacyBobmazonItem(new Item.Properties(), true)
    );
    public static final DeferredItem<Item> CLAY_TABLET = legacyItem(
            "clay_tablet",
            () -> new LegacyClayTabletItem(new Item.Properties())
    );

    static {
        registerPortedMissileParts();
        registerPortedLegacyMissiles();
        registerPortedLegacyGameplayItems();
        registerPortedLegacyArmorMods();
        registerPortedLegacyScanners();
        registerPortedLegacyHeldInventories();
        registerPortedPlainItems();
        registerLegacyMaterialItems();
        registerPortedLegacyLoreItems();
        registerPortedLegacyRbmkItems();
        registerRawOres();
        registerPortedRemainingItems();
        verifyLegacyItemCoverage();
    }

    private HbmItems() {
    }

    private static DeferredItem<Item> ingot(String name) {
        return ingot(name, Rarity.COMMON);
    }

    private static DeferredItem<Item> ingot(String name, Rarity rarity) {
        DeferredItem<Item> item = coreItem(name, () -> legacyIngot(name, rarity));
        INGOT_MATERIALS.add(item);
        return item;
    }

    private static Item legacyIngot(String id, Rarity rarity) {
        if (LegacyLoreItem.isLegacyLoreItem(id)) {
            return LegacyLoreItem.fromLegacyId(id);
        }
        return switch (id) {
            case "ingot_meteorite", "ingot_meteorite_forged", "blade_meteorite" ->
                    new LegacyHotItem(new Item.Properties().rarity(rarity), 200, false);
            case "ingot_chainsteel" -> new LegacyHotItem(new Item.Properties().rarity(rarity), 100, false);
            case "ingot_steel_dusted" -> new LegacyHotItem(new Item.Properties().rarity(rarity), 200, true);
            case "ingot_u238m2" -> new LegacyUnstableItem(new Item.Properties().rarity(rarity));
            case "ingot_schraranium" -> new LegacySchraraniumItem(new Item.Properties().rarity(rarity));
            case "ingot_starmetal" -> new LegacyStarmetalItem(new Item.Properties().rarity(rarity));
            case "ingot_smore" -> new Item(new Item.Properties().rarity(rarity)
                    .food(new FoodProperties.Builder().nutrition(10).saturationModifier(20.0F).build()));
            case "ingot_semtex", "ingot_c4" -> LegacyLemonItem.fromLegacyId(id);
            default -> new Item(new Item.Properties().rarity(rarity));
        };
    }

    private static DeferredItem<Item> material(List<DeferredItem<Item>> group, String name) {
        return material(group, name, () -> legacyMaterial(name));
    }

    private static Item legacyMaterial(String id) {
        if (LegacyLoreItem.isLegacyLoreItem(id)) {
            return LegacyLoreItem.fromLegacyId(id);
        }
        return LegacyLemonItem.isLegacyLemon(id) ? LegacyLemonItem.fromLegacyId(id) : new Item(new Item.Properties());
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
        return rtgPellet(name, heat, heat, lifespan, depletedMaterial);
    }

    private static DeferredItem<Item> rtgPellet(String name, int heat, int decayingHeat, long lifespan, String depletedMaterial) {
        return material(MISC_MATERIALS, name,
                () -> new RtgPelletItem(heat, decayingHeat, lifespan, depletedMaterial, () -> PELLET_RTG_DEPLETED.get()));
    }

    private static DeferredItem<Item> particle(String name, boolean returnsEmptyCapsule) {
        return material(MISC_MATERIALS, name, () -> new ParticleCapsuleItem(new Item.Properties(), returnsEmptyCapsule));
    }

    private static DeferredItem<Item> machineComponent(String name) {
        return machineComponent(name, () -> legacyMachineComponent(name));
    }

    private static Item legacyMachineComponent(String id) {
        if (id.equals("blade_meteorite")) {
            return new LegacyHotItem(new Item.Properties(), 200, false);
        }
        return new MachineUpgradeItem(new Item.Properties().stacksTo(machineComponentStackSize(id)));
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

    private static DeferredItem<Item> legacyBedrockOre(String name) {
        return oreDrop(name, () -> new LegacyBedrockOreStageItem(new Item.Properties(), name));
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

    private static DeferredItem<Item> rocketMissileItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        ROCKET_MISSILE_ITEMS.add(registered);
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
        DeferredItem<Item> registered = coreItem(name, () -> new SatelliteChipItem(new Item.Properties().stacksTo(1), descriptionKey));
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

    private static DeferredItem<Item> poweredFsbArmor(
            String name,
            String fsbGroup,
            Holder<ArmorMaterial> material,
            ArmorItem.Type type,
            boolean noHelmet,
            long maxPower,
            long chargeRate,
            long consumption,
            long drain,
            ArmorFSBItem.FullSetEffect... effects
    ) {
        return armorItem(name, () -> new PoweredArmorFSBItem(
                fsbGroup, material, type, noHelmet, List.of(effects),
                maxPower, chargeRate, consumption, drain,
                new Item.Properties().stacksTo(1)
        ));
    }

    private static DeferredItem<Item> fueledFsbArmor(
            String name,
            String fsbGroup,
            Holder<ArmorMaterial> material,
            ArmorItem.Type type,
            boolean noHelmet,
            String fuelId,
            int maxFuel,
            int fillRate,
            int consumption,
            int drain,
            ArmorFSBItem.FullSetEffect... effects
    ) {
        return armorItem(name, () -> new FueledArmorFSBItem(
                fsbGroup, material, type, noHelmet, List.of(effects),
                fuelId, maxFuel, fillRate, consumption, drain,
                new Item.Properties().stacksTo(1)
        ));
    }

    private static DeferredItem<Item> sword(String name, HbmToolProfile profile) {
        return toolItem(name, () -> new HbmSwordItem(profile));
    }

    private static DeferredItem<Item> specialWeapon(String name, HbmToolProfile profile, HbmSpecialWeaponItem.Kind kind) {
        return toolItem(name, () -> new HbmSpecialWeaponItem(profile, kind));
    }

    private static DeferredItem<Item> pickaxe(String name, HbmToolProfile profile) {
        return toolItem(name, () -> new HbmPickaxeItem(profile));
    }

    private static DeferredItem<Item> pickaxe(String name, HbmToolProfile profile, int durability) {
        return toolItem(name, () -> new HbmPickaxeItem(profile, HbmToolBehavior.properties(profile).durability(durability)));
    }

    private static DeferredItem<Item> poweredPickaxe(String name, HbmToolProfile profile, long capacity, long rate, long consumption) {
        return toolItem(name, () -> new HbmPoweredPickaxeItem(profile, capacity, rate, consumption));
    }

    private static DeferredItem<Item> axe(String name, HbmToolProfile profile) {
        return toolItem(name, () -> new HbmAxeItem(profile));
    }

    private static DeferredItem<Item> shovel(String name, HbmToolProfile profile) {
        return toolItem(name, () -> new HbmShovelItem(profile));
    }

    private static DeferredItem<Item> hoe(String name, HbmToolTier tier) {
        return hoe(name, tier, Rarity.COMMON);
    }

    private static DeferredItem<Item> hoe(String name, HbmToolTier tier, Rarity rarity) {
        return toolItem(name, () -> new HoeItem(tier, new Item.Properties().rarity(rarity)));
    }

    private static HbmToolProfile profile(HbmToolTier tier, float attackDamage, float attackSpeed) {
        return HbmToolProfile.create(tier, attackDamage, attackSpeed);
    }

    private static DeferredItem<Item> coreItem(String name, Supplier<Item> item) {
        CORE_ITEM_IDS.add(name);
        return ITEMS.register(name, item);
    }

    private static ArmorInsertItem armorInsert(
            int durability,
            float damageMultiplier,
            float projectileMultiplier,
            float explosionMultiplier,
            float speedMultiplier,
            boolean radioactive,
            boolean reactive
    ) {
        return new ArmorInsertItem(
                new Item.Properties().durability(durability),
                damageMultiplier, projectileMultiplier, explosionMultiplier, speedMultiplier, radioactive, reactive
        );
    }

    private static DeferredItem<Item> legacyItem(String name, Supplier<Item> item) {
        DeferredItem<Item> registered = coreItem(name, item);
        PORTED_PLAIN_ITEMS.add(registered);
        return registered;
    }

    private static DeferredItem<Item> legacyPowerItem(String name, Supplier<Item> item) {
        return legacyItem(name, item);
    }

    private static DeferredItem<Item> legacyLemon(String name) {
        return legacyItem(name, () -> LegacyLemonItem.fromLegacyId(name));
    }

    private static DeferredItem<Item> legacyMedical(String name) {
        return legacyItem(name, () -> new LegacySyringeItem(new Item.Properties(), name));
    }

    private static DeferredItem<Item> legacySoup(String name) {
        return legacyItem(name, () -> new Item(new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build())
                .craftRemainder(Items.BOWL)));
    }

    private static DeferredItem<Item> legacyVariantItem(String name, String baseId, String... variants) {
        return legacyItem(name, () -> new LegacyVariantItem(new Item.Properties(), baseId, LegacyVariantItem.variants(variants)));
    }

    private static DeferredItem<Item> legacyVariantItem(String name, String baseId, boolean multiName, String... variants) {
        return legacyItem(name, () -> new LegacyVariantItem(new Item.Properties(), baseId, LegacyVariantItem.variants(variants), multiName));
    }

    private static DeferredItem<Item> record(String name) {
        ResourceKey<JukeboxSong> song = ResourceKey.create(Registries.JUKEBOX_SONG, ReinhardtsHBM.id(name));
        DeferredItem<Item> item = coreItem(name, () -> new HbmRecordItem(
                new Item.Properties()
                        .rarity(Rarity.RARE)
                        .component(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(song), true))
        ));
        PORTED_PLAIN_ITEMS.add(item);
        return item;
    }

    public static boolean isCoreItem(String id) {
        return CORE_ITEM_IDS.contains(id);
    }

    public static boolean isHiddenPortedPlainItem(DeferredItem<Item> item) {
        return HIDDEN_PORTED_PLAIN_ITEM_IDS.contains(item.getId().getPath());
    }

    public static boolean isHiddenMissilePart(DeferredItem<Item> item) {
        return item.get() instanceof MissilePartItem part && part.isHiddenInCreative();
    }

    public static boolean isHiddenLegacyMissile(DeferredItem<Item> item) {
        return item.getId().getPath().equals("missile_test");
    }

    private static void registerPortedMissileParts() {
        for (String id : loadLegacyItemIds()) {
            if (!MissilePartItem.isPartId(id)) {
                continue;
            }
            DeferredItem<Item> item = coreItem(id, () -> new MissilePartItem(new Item.Properties(), id));
            ROCKET_MISSILE_ITEMS.add(item);
        }
    }

    private static void registerPortedLegacyMissiles() {
        for (String id : loadLegacyItemIds()) {
            if (!LegacyMissileItem.isLegacyMissileId(id)) {
                continue;
            }
            DeferredItem<Item> item = coreItem(id, () -> LegacyMissileItem.fromLegacyId(id));
            ROCKET_MISSILE_ITEMS.add(item);
        }
    }

    private static void registerPortedLegacyGameplayItems() {
        for (String id : PORTED_LEGACY_GAMEPLAY_ITEM_IDS) {
            DeferredItem<Item> item = coreItem(id, () -> legacyGameplayItem(id));
            if (isHealthArmorMod(id)) {
                ARMOR_ITEMS.add(item);
            } else if (isRocketMissileItemId(id)) {
                ROCKET_MISSILE_ITEMS.add(item);
            } else {
                PORTED_PLAIN_ITEMS.add(item);
            }
        }
    }

    private static Item legacyGameplayItem(String id) {
        return switch (id) {
            case "black_diamond" -> new HealthArmorModItem(new Item.Properties(), 40.0D, true);
            case "heart_piece" -> new HealthArmorModItem(new Item.Properties(), 5.0D, false);
            case "heart_container" -> new HealthArmorModItem(new Item.Properties(), 20.0D, false);
            case "heart_booster" -> new HealthArmorModItem(new Item.Properties(), 40.0D, false);
            case "heart_fab" -> new HealthArmorModItem(new Item.Properties(), 60.0D, false);
            case "med_bag", "syringe_metal_medx", "syringe_metal_psycho",
                    "syringe_metal_stimpak", "syringe_metal_super", "syringe_mkunicorn", "syringe_taint" ->
                    new LegacySyringeItem(new Item.Properties(), id);
            case "five_htp" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.FIVE_HTP);
            case "fmn" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.FMN);
            case "pill_herbal" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.HERBAL);
            case "pill_iodine" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.IODINE);
            case "pill_red" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.RED);
            case "plan_c" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.PLAN_C);
            case "radx" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.RADX);
            case "siox" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.SIOX);
            case "xanax" -> new LegacyPillItem(new Item.Properties(), LegacyPillItem.Kind.XANAX);
            case "cigarette" -> new LegacyCigaretteItem(new Item.Properties().stacksTo(16), false);
            case "crackpipe" -> new LegacyCigaretteItem(new Item.Properties().stacksTo(1), true);
            case "bottle_cherry", "bottle_nuka", "bottle_quantum", "bottle_rad", "bottle_sparkle", "bottle2_fritz",
                    "bottle2_korl", "can_bepis", "can_breen", "can_creature", "can_luna", "can_mrsugar", "can_mug",
                    "can_overcharge", "can_redbomb", "can_smart", "chocolate_milk", "coffee", "coffee_radium" ->
                    new LegacyEnergyDrinkItem(new Item.Properties(), id);
            default -> new LegacyStarterKitItem(new Item.Properties(), id);
        };
    }

    private static boolean isHealthArmorMod(String id) {
        return id.equals("black_diamond") || id.equals("heart_piece") || id.equals("heart_container")
                || id.equals("heart_booster") || id.equals("heart_fab");
    }

    /** Items whose old behavior is attached to the armor-modification slots. */
    private static void registerPortedLegacyArmorMods() {
        registerLegacyArmorMod("bathwater", () -> new LegacyBathwaterArmorModItem(new Item.Properties(), false));
        registerLegacyArmorMod("bathwater_mk2", () -> new LegacyBathwaterArmorModItem(new Item.Properties(), true));
        registerLegacyArmorMod("card_aos", () -> new LegacyCardArmorModItem(new Item.Properties(), false));
        registerLegacyArmorMod("card_qos", () -> new LegacyCardArmorModItem(new Item.Properties(), true));
        registerLegacyArmorMod("scrumpy", () -> new LegacyReviveArmorModItem(new Item.Properties(), 1));
        registerLegacyArmorMod("wild_p", () -> new LegacyReviveArmorModItem(new Item.Properties(), 3));
        registerLegacyArmorMod("attachment_mask", () -> new GasMaskAttachmentItem(new Item.Properties(), false));
        registerLegacyArmorMod("attachment_mask_mono", () -> new GasMaskAttachmentItem(new Item.Properties(), true));
        registerLegacyArmorMod("armor_polish", () -> new LegacyArmorUtilityModItem(new Item.Properties(), LegacyArmorUtilityModItem.Kind.POLISH));
        registerLegacyArmorMod("bandaid", () -> new LegacyArmorUtilityModItem(new Item.Properties(), LegacyArmorUtilityModItem.Kind.BANDAID));
        registerLegacyArmorMod("serum", () -> new LegacyArmorUtilityModItem(new Item.Properties(), LegacyArmorUtilityModItem.Kind.SERUM));
        registerLegacyArmorMod("quartz_plutonium", () -> new LegacyArmorUtilityModItem(new Item.Properties(), LegacyArmorUtilityModItem.Kind.QUARTZ));
        registerLegacyArmorMod("morning_glory", () -> new LegacyArmorUtilityModItem(new Item.Properties(), LegacyArmorUtilityModItem.Kind.MORNING_GLORY));
        registerLegacyArmorMod("spider_milk", () -> new LegacyArmorUtilityModItem(new Item.Properties(), LegacyArmorUtilityModItem.Kind.SPIDER_MILK));
        registerLegacyArmorMod("ink", () -> new LegacyArmorUtilityModItem(new Item.Properties(), LegacyArmorUtilityModItem.Kind.INK));
        registerLegacyArmorMod("injector_5htp", () -> new LegacyArmorUtilityModItem(new Item.Properties(), LegacyArmorUtilityModItem.Kind.AUTO_INJECTOR));
        registerLegacyArmorMod("australium_iii", () -> new LegacyShieldArmorModItem(new Item.Properties(), 25.0F));
        registerLegacyArmorMod("medal_liquidator", () -> new LegacyMedalArmorModItem(new Item.Properties()));
        registerLegacyArmorMod("night_vision", () -> new LegacyNightVisionArmorModItem(new Item.Properties()));
        registerLegacyArmorMod("gas_tester", () -> new LegacyGasSensorArmorModItem(new Item.Properties()));
        registerLegacyArmorMod("shackles", () -> new LegacyShacklesArmorModItem(new Item.Properties()));
        registerLegacyArmorMod("bottled_cloud", () -> new LegacyCloudArmorModItem(new Item.Properties()));
        registerLegacyArmorMod("wd40", () -> new LegacyWd40ArmorModItem(new Item.Properties()));
        registerLegacyArmorMod("defuser_gold", () -> new LegacyDefuserArmorModItem(new Item.Properties()));
        registerLegacyArmorMod("injector_knife", () -> new LegacyInjectorKnifeArmorModItem(new Item.Properties()));
        // ItemModTwoKick declares no update/damage hooks in 1.7.10; its only
        // behavior is the original servo-slot applicability.
        registerLegacyArmorMod("ballistic_gauntlet", () -> ArmorModItem.servos(new Item.Properties()));
    }

    private static void registerLegacyArmorMod(String id, Supplier<Item> supplier) {
        DeferredItem<Item> item = coreItem(id, supplier);
        ARMOR_ITEMS.add(item);
    }

    private static void registerPortedLegacyScanners() {
        toolItem("digamma_diagnostic", () -> new LegacyDigammaDiagnosticItem(new Item.Properties()));
        toolItem("rod_of_discord", () -> new LegacyDiscordRodItem(new Item.Properties()));
        toolItem("meltdown_tool", () -> new LegacyMeltdownToolItem(new Item.Properties()));
        rocketMissileItem("designator", () -> new LegacyCoordinateDesignatorItem(new Item.Properties()));
        toolItem("ore_density_scanner", () -> new LegacyOreDensityScannerItem(new Item.Properties()));
        toolItem("survey_scanner", () -> new LegacySurveyScannerItem(new Item.Properties()));
    }

    /**
     * Standalone registrations that were previously left in the generic
     * catalog. Each entry below has its own 1.7.10 behavior implementation.
     */
    private static void registerPortedRemainingItems() {
        toolItem("wrench", () -> new LegacyWrenchItem(new Item.Properties()));
        rocketMissileItem("designator_manual", () -> new LegacyManualDesignatorItem(new Item.Properties()));
        rocketMissileItem("sat_coord", () -> new LegacySatelliteToolItem(new Item.Properties(), LegacySatelliteToolItem.Mode.COORDINATE));
        rocketMissileItem("sat_designator", () -> new LegacySatelliteToolItem(new Item.Properties(), LegacySatelliteToolItem.Mode.LASER));
        rocketMissileItem("sat_interface", () -> new LegacySatelliteToolItem(new Item.Properties(), LegacySatelliteToolItem.Mode.INTERFACE));
        toolItem("peas", () -> new LegacyPeasItem(new Item.Properties()));
        toolItem("glitch", () -> new LegacyGlitchItem(new Item.Properties()));
        armorItem("jetpack_fly", () -> new LegacyJetpackItem(new Item.Properties(), "kerosene", 12_000, LegacyJetpackItem.Profile.REGULAR));
        armorItem("jetpack_break", () -> new LegacyJetpackItem(new Item.Properties(), "kerosene", 12_000, LegacyJetpackItem.Profile.BREAK));
        armorItem("jetpack_vector", () -> new LegacyJetpackItem(new Item.Properties(), "kerosene", 16_000, LegacyJetpackItem.Profile.VECTOR));
        armorItem("jetpack_boost", () -> new LegacyJetpackItem(new Item.Properties(), "balefire", 32_000, LegacyJetpackItem.Profile.BOOST));
        armorItem("wings_limp", () -> new LegacyWingsItem(new Item.Properties(), false));
        armorItem("wings_murk", () -> new LegacyWingsItem(new Item.Properties(), true));
        toolItem("jetpack_tank", () -> new LegacyJetpackTankItem(new Item.Properties()));
        armorItem("neutrino_lens", () -> new LegacyNeutrinoLensItem(new Item.Properties()));
        toolItem("wand_d", () -> new LegacyDebugCraneWandItem(new Item.Properties()));
        toolItem("structure_pattern", () -> new LegacyStructureExportToolItem(new Item.Properties(), LegacyStructureExportToolItem.Mode.PATTERN));
        toolItem("structure_randomized", () -> new LegacyStructureExportToolItem(new Item.Properties(), LegacyStructureExportToolItem.Mode.RANDOMIZED));
        toolItem("structure_randomly", () -> new LegacyStructureExportToolItem(new Item.Properties(), LegacyStructureExportToolItem.Mode.RANDOMLY));
        toolItem("structure_single", () -> new LegacyStructureExportToolItem(new Item.Properties(), LegacyStructureExportToolItem.Mode.SINGLE));
        toolItem("structure_solid", () -> new LegacyStructureExportToolItem(new Item.Properties(), LegacyStructureExportToolItem.Mode.SOLID));
        toolItem("structure_custommachine", () -> new LegacyStructureExportToolItem(new Item.Properties(), LegacyStructureExportToolItem.Mode.CUSTOM_MACHINE));
        toolItem("rebar_placer", () -> new LegacyRebarPlacerItem(new Item.Properties()));
        toolItem("holotape_image", () -> new LegacyHolotapeImageItem(new Item.Properties()));
        toolItem("rtty_pager", () -> new LegacyRttyPagerItem(new Item.Properties()));
        toolItem("spawn_duck", () -> new LegacyDuckSpawnItem(new Item.Properties()));
        CHOPPER = legacyItem("chopper", () -> new LegacyBossSpawnItem(new Item.Properties(), LegacyBossSpawnItem.Type.CHOPPER));
        SPAWN_UFO = legacyItem("spawn_ufo", () -> new LegacyBossSpawnItem(new Item.Properties(), LegacyBossSpawnItem.Type.UFO));
        SPAWN_WORM = legacyItem("spawn_worm", () -> new LegacyBossSpawnItem(new Item.Properties(), LegacyBossSpawnItem.Type.WORM));
        DRONE = toolItem("drone", () -> new LegacyDroneItem(new Item.Properties()));
        DRONE_LINKER = toolItem("drone_linker", () -> new LegacyDroneLinkerItem(new Item.Properties()));
    }

    private static void registerPortedLegacyHeldInventories() {
        toolItem("plastic_bag", () -> new LegacyHeldInventoryItem(
                new Item.Properties(), LegacyHeldInventoryItem.Kind.PLASTIC_BAG
        ));
        toolItem("containment_box", () -> new LegacyHeldInventoryItem(
                new Item.Properties(), LegacyHeldInventoryItem.Kind.CONTAINMENT_BOX
        ));
    }

    private static void registerPortedPlainItems() {
        for (String id : PORTED_PLAIN_ITEM_IDS) {
            switch (id) {
                case "battery_spark", "battery_trixite", "egg_balefire", "gadget_wireing", "launch_code",
                        "launch_code_piece", "launch_key", "man_igniter", "mike_cooling_unit", "mike_core",
                        "missile_assembly", "reacher", "tsar_core" -> legacyPlainItem(id, new Item.Properties().stacksTo(1));
                case "syringe_empty", "syringe_metal_empty" -> legacyPlainItem(id, new Item.Properties());
                case "cell_antimatter", "cell_anti_schrabidium", "cell_balefire", "cell_deuterium", "cell_puf6", "cell_uf6" ->
                        legacyContainerItem(id, new Item.Properties(), () -> CELL_EMPTY.get());
                case "mike_deut" -> legacyContainerItem(id, new Item.Properties().stacksTo(1), () -> TANK_STEEL.get());
                case "rod_zirnox_les_fuel_depleted", "rod_zirnox_mox_fuel_depleted",
                        "rod_zirnox_natural_uranium_fuel_depleted", "rod_zirnox_plutonium_fuel_depleted",
                        "rod_zirnox_thorium_fuel_depleted", "rod_zirnox_u233_fuel_depleted",
                        "rod_zirnox_u235_fuel_depleted", "rod_zirnox_uranium_fuel_depleted",
                        "rod_zirnox_zfb_mox_depleted" ->
                        legacyContainerItem(id, new Item.Properties(), () -> ROD_ZIRNOX_EMPTY.get());
                default -> legacyPlainItem(id, new Item.Properties());
            }
        }
    }

    private static void registerPortedLegacyLoreItems() {
        for (String id : PORTED_LEGACY_LORE_ITEM_IDS) {
            if (CORE_ITEM_IDS.contains(id)) {
                continue;
            }
            DeferredItem<Item> item = coreItem(id, () -> legacyLoreItem(id));
            if (isNuclearLoreItem(id)) {
                NUCLEAR_WEAPON_ITEMS.add(item);
            } else if (isMaterialLoreItem(id)) {
                MISC_MATERIALS.add(item);
            } else {
                PORTED_PLAIN_ITEMS.add(item);
            }
        }
    }

    private static void registerPortedLegacyRbmkItems() {
        for (String id : PORTED_LEGACY_RBMK_FUEL_IDS) {
            String fuelId = id.substring("rbmk_fuel_".length());
            rbmkFuelRodItem(id, () -> new RbmkFuelRodItem(new Item.Properties(), fuelId));
        }
        for (String id : PORTED_LEGACY_RBMK_PELLET_IDS) {
            String pelletId = id.substring("rbmk_pellet_".length());
            DeferredItem<Item> pellet = coreItem(id, () -> new RbmkPelletItem(
                    new Item.Properties(), pelletId, rbmkPelletFullName(pelletId), rbmkPelletHasXenon(pelletId)
            ));
            RBMK_PELLET_ITEMS.add(pellet);
        }
    }

    private static boolean rbmkPelletHasXenon(String id) {
        return !Set.of("po210be", "ra226be", "balefire_gold", "flashlead", "balefire").contains(id);
    }

    private static String rbmkPelletFullName(String id) {
        return switch (id) {
            case "ueu" -> "Unenriched Uranium";
            case "meu" -> "Medium Enriched Uranium-235";
            case "heu233" -> "Highly Enriched Uranium-233";
            case "heu235" -> "Highly Enriched Uranium-235";
            case "uzh" -> "Uranium Zirconium Hydride";
            case "thmeu" -> "Thorium with MEU Driver Fuel";
            case "lep" -> "Low Enriched Plutonium-239";
            case "mep" -> "Medium Enriched Plutonium-239";
            case "hep239" -> "Highly Enriched Plutonium-239";
            case "hep241" -> "Highly Enriched Plutonium-241";
            case "lea" -> "Low Enriched Americium-242";
            case "mea" -> "Medium Enriched Americium-242";
            case "hea241" -> "Highly Enriched Americium-241";
            case "hea242" -> "Highly Enriched Americium-242";
            case "men" -> "Medium Enriched Neptunium-237";
            case "hen" -> "Highly Enriched Neptunium-237";
            case "mox" -> "Mixed MEU & LEP Oxide";
            case "les" -> "Low Enriched Schrabidium-326";
            case "mes" -> "Medium Enriched Schrabidium-326";
            case "hes" -> "Highly Enriched Schrabidium-326";
            case "leaus" -> "Low Enriched Australium (Tasmanite)";
            case "heaus" -> "Highly Enriched Australium (Ayerite)";
            case "po210be" -> "Polonium-210 & Beryllium Neutron Source";
            case "ra226be" -> "Radium-226 & Beryllium Neutron Source";
            case "pu238be" -> "Plutonium-238 & Beryllium Neutron Source";
            case "balefire_gold" -> "Antihydrogen in a Magnetized Gold-198 Lattice";
            case "flashlead" -> "Antihydrogen confined by a Magnetized Gold-198 and Lead-209 Lattice";
            case "balefire" -> "Draconic Flames";
            case "zfb_bismuth" -> "Zirconium Fast Breeder - LEU/HEP-241#Bi";
            case "zfb_pu241" -> "Zirconium Fast Breeder - HEU-235/HEP-240#Pu-241";
            case "zfb_am_mix" -> "Zirconium Fast Breeder - HEP-241#MEA";
            case "drx" -> "can't you hear, can't you hear the thunder?";
            default -> id;
        };
    }

    private static Item legacyLoreItem(String id) {
        return switch (id) {
            case "bottle_mercury" -> LegacyLoreItem.fromLegacyId(id, new Item.Properties(), () -> Items.GLASS_BOTTLE);
            case "canister_napalm" -> LegacyLoreItem.fromLegacyId(id, new Item.Properties(), () -> CANISTER_EMPTY.get());
            case "cell_sas3" -> LegacyLoreItem.fromLegacyId(id, new Item.Properties(), () -> CELL_EMPTY.get());
            case "custom_amat", "custom_dirty", "custom_fall", "custom_hydro", "custom_nuke", "custom_schrab", "custom_tnt",
                    "gadget_core", "igniter", "man_core", "key_red", "key_red_cracked", "mech_key", "rune_blank",
                    "rune_dagaz", "rune_hagalaz", "rune_isa", "rune_jera", "rune_thurisaz", "watch" ->
                    LegacyLoreItem.fromLegacyId(id, new Item.Properties().stacksTo(1));
            default -> LegacyLoreItem.fromLegacyId(id);
        };
    }

    private static boolean isNuclearLoreItem(String id) {
        return id.startsWith("custom_") || id.equals("early_explosive_lenses") || id.equals("explosive_lenses")
                || id.equals("gadget_core") || id.equals("igniter") || id.equals("man_core");
    }

    private static boolean isMaterialLoreItem(String id) {
        return switch (id) {
            case "ball_resin", "bolt_spike", "bottle_mercury", "crystal_horn", "entanglement_kit", "flame_conspiracy",
                    "flame_opinion", "flame_politics", "gem_sodalite", "gem_volcanic", "magnetron", "pellet_cluster",
                    "rune_blank", "rune_dagaz", "rune_hagalaz", "rune_isa", "rune_jera", "rune_thurisaz", "undefined" -> true;
            default -> false;
        };
    }

    private static DeferredItem<Item> legacyPlainItem(String name, Item.Properties properties) {
        DeferredItem<Item> item = coreItem(name, () -> new Item(properties));
        if (isRocketMissileItemId(name)) {
            ROCKET_MISSILE_ITEMS.add(item);
        } else {
            PORTED_PLAIN_ITEMS.add(item);
        }
        return item;
    }

    private static boolean isRocketMissileItemId(String id) {
        return ROCKET_MISSILE_PLAIN_ITEM_IDS.contains(id);
    }

    private static void legacyContainerItem(String name, Item.Properties properties, Supplier<Item> remainder) {
        DeferredItem<Item> item = coreItem(name, () -> new LegacyContainerRemainderItem(properties, remainder));
        PORTED_PLAIN_ITEMS.add(item);
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

    /**
     * Every obtainable 1.7.10 item must have an explicit modern registration.
     * Do not turn a missed port into a generic Item: that masks missing behavior,
     * recipes and data behind a valid-looking registry id.
     */
    private static void verifyLegacyItemCoverage() {
        Set<String> blockIds = loadLegacyBlockIds();
        List<String> unported = new ArrayList<>();
        for (String id : loadLegacyItemIds()) {
            if (CORE_ITEM_IDS.contains(id)
                    || blockIds.contains(id)
                    || RETIRED_LEGACY_CATALOG_ITEM_IDS.contains(id)
                    || FLUID_BUCKET_ITEM_IDS.contains(id)
                    || id.startsWith("gun_")) {
                continue;
            }
            unported.add(id);
        }
        if (!unported.isEmpty()) {
            throw new IllegalStateException(
                    "Missing recursive 1.7.10 item port(s): " + String.join(", ", unported)
            );
        }
    }

    private static List<String> loadLegacyItemIds() {
        // Keep the complete 1.7.10 catalog separate from the unresolved
        // placeholder manifest consumed by LegacyHbmContent.
        InputStream stream = HbmItems.class.getClassLoader().getResourceAsStream("legacy/reinhardtshbm/item_catalog.txt");
        if (stream == null) {
            throw new IllegalStateException("Missing generated HBM legacy item catalog.");
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
            throw new IllegalStateException("Failed to read generated HBM legacy item catalog.", exception);
        }
        return ids;
    }

    private static Set<String> loadLegacyBlockIds() {
        InputStream stream = HbmItems.class.getClassLoader().getResourceAsStream("legacy/reinhardtshbm/block_catalog.txt");
        if (stream == null) {
            throw new IllegalStateException("Missing generated HBM legacy block catalog.");
        }

        Set<String> ids = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.trim();
                if (!id.isEmpty() && id.charAt(0) == '\uFEFF') {
                    id = id.substring(1);
                }
                if (!id.isEmpty() && !id.startsWith("#") && isValidPath(id)) {
                    ids.add(id);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read generated HBM legacy block catalog.", exception);
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
