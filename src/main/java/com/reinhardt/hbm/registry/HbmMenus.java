package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.AssemblyFactoryMenu;
import com.reinhardt.hbm.menu.AssemblyMachineMenu;
import com.reinhardt.hbm.menu.ArcWelderMenu;
import com.reinhardt.hbm.menu.ArcFurnaceMenu;
import com.reinhardt.hbm.menu.StorageDrumMenu;
import com.reinhardt.hbm.menu.AmmoPressMenu;
import com.reinhardt.hbm.menu.ArmorTableMenu;
import com.reinhardt.hbm.menu.AshpitMenu;
import com.reinhardt.hbm.menu.BatteryReddMenu;
import com.reinhardt.hbm.menu.BatterySocketMenu;
import com.reinhardt.hbm.menu.BrickFurnaceMenu;
import com.reinhardt.hbm.menu.BreederReactorMenu;
import com.reinhardt.hbm.menu.CatalyticReformerMenu;
import com.reinhardt.hbm.menu.HydrotreaterMenu;
import com.reinhardt.hbm.menu.CentrifugeMenu;
import com.reinhardt.hbm.menu.ChemicalFactoryMenu;
import com.reinhardt.hbm.menu.ChemicalPlantMenu;
import com.reinhardt.hbm.menu.CombustionEngineMenu;
import com.reinhardt.hbm.menu.CompressorMenu;
import com.reinhardt.hbm.menu.CokerMenu;
import com.reinhardt.hbm.menu.CrystallizerMenu;
import com.reinhardt.hbm.menu.CrucibleMenu;
import com.reinhardt.hbm.menu.CyclotronMenu;
import com.reinhardt.hbm.menu.DfcCoreMenu;
import com.reinhardt.hbm.menu.DfcEmitterMenu;
import com.reinhardt.hbm.menu.DfcInjectorMenu;
import com.reinhardt.hbm.menu.DfcReceiverMenu;
import com.reinhardt.hbm.menu.DfcStabilizerMenu;
import com.reinhardt.hbm.menu.DieselGeneratorMenu;
import com.reinhardt.hbm.menu.ElectrolyzerMenu;
import com.reinhardt.hbm.menu.ExcavatorMenu;
import com.reinhardt.hbm.menu.ExposureChamberMenu;
import com.reinhardt.hbm.menu.FelMenu;
import com.reinhardt.hbm.menu.FilingCabinetMenu;
import com.reinhardt.hbm.menu.ElectricFurnaceMenu;
import com.reinhardt.hbm.menu.FluidPumpMenu;
import com.reinhardt.hbm.menu.FluidTankMenu;
import com.reinhardt.hbm.menu.FurnaceCombinationMenu;
import com.reinhardt.hbm.menu.FunnelMenu;
import com.reinhardt.hbm.menu.FrackingTowerMenu;
import com.reinhardt.hbm.menu.FusionMachineMenu;
import com.reinhardt.hbm.menu.GasCentrifugeMenu;
import com.reinhardt.hbm.menu.GasFlareMenu;
import com.reinhardt.hbm.menu.GasTurbineMenu;
import com.reinhardt.hbm.menu.HbmAnvilMenu;
import com.reinhardt.hbm.menu.HeaterMenu;
import com.reinhardt.hbm.menu.LegacyHeldInventoryMenu;
import com.reinhardt.hbm.menu.IronFurnaceMenu;
import com.reinhardt.hbm.menu.LargeTurbineMenu;
import com.reinhardt.hbm.menu.LegacyTurretMenu;
import com.reinhardt.hbm.menu.LiquefactorMenu;
import com.reinhardt.hbm.menu.MachineKeyForgeMenu;
import com.reinhardt.hbm.menu.MachineEPressMenu;
import com.reinhardt.hbm.menu.MachineBlastFurnaceMenu;
import com.reinhardt.hbm.menu.MachinePressMenu;
import com.reinhardt.hbm.menu.MiningLaserMenu;
import com.reinhardt.hbm.menu.MixerMenu;
import com.reinhardt.hbm.menu.MicrowaveMenu;
import com.reinhardt.hbm.menu.NukeBoyMenu;
import com.reinhardt.hbm.menu.LegacyNukeMenu;
import com.reinhardt.hbm.menu.BombMultiMenu;
import com.reinhardt.hbm.menu.OilDerrickMenu;
import com.reinhardt.hbm.menu.OreSlopperMenu;
import com.reinhardt.hbm.menu.ParticleAcceleratorMenu;
import com.reinhardt.hbm.menu.PurexMenu;
import com.reinhardt.hbm.menu.IcfPressMenu;
import com.reinhardt.hbm.menu.IcfCoreMenu;
import com.reinhardt.hbm.menu.PwrMenu;
import com.reinhardt.hbm.menu.RefineryMenu;
import com.reinhardt.hbm.menu.ResearchReactorMenu;
import com.reinhardt.hbm.menu.ReactorControlMenu;
import com.reinhardt.hbm.menu.RbmkComponentMenu;
import com.reinhardt.hbm.menu.RotaryFurnaceMenu;
import com.reinhardt.hbm.menu.ShredderMenu;
import com.reinhardt.hbm.menu.SolidifierMenu;
import com.reinhardt.hbm.menu.SilexMenu;
import com.reinhardt.hbm.menu.VacuumDistillMenu;
import com.reinhardt.hbm.menu.WasteDrumMenu;
import com.reinhardt.hbm.menu.WatzMenu;
import com.reinhardt.hbm.menu.WeaponTableMenu;
import com.reinhardt.hbm.menu.SolderingStationMenu;
import com.reinhardt.hbm.menu.SirenMenu;
import com.reinhardt.hbm.menu.SmallBoilerMenu;
import com.reinhardt.hbm.menu.SoyuzCapsuleMenu;
import com.reinhardt.hbm.menu.SoyuzLauncherMenu;
import com.reinhardt.hbm.menu.SteamTurbineMenu;
import com.reinhardt.hbm.menu.SteelFurnaceMenu;
import com.reinhardt.hbm.menu.StorageCrateMenu;
import com.reinhardt.hbm.menu.SafeMenu;
import com.reinhardt.hbm.menu.MassStorageMenu;
import com.reinhardt.hbm.menu.StrandCasterMenu;
import com.reinhardt.hbm.menu.TurretChekhovMenu;
import com.reinhardt.hbm.menu.TurretJeremyMenu;
import com.reinhardt.hbm.menu.ToolboxMenu;
import com.reinhardt.hbm.menu.LegacyCraftBookMenu;
import com.reinhardt.hbm.menu.WoodBurnerMenu;
import com.reinhardt.hbm.menu.ZirnoxReactorMenu;
import com.reinhardt.hbm.menu.RadGenMenu;
import com.reinhardt.hbm.menu.RtgMenu;
import com.reinhardt.hbm.menu.RadarMenu;
import com.reinhardt.hbm.menu.RadarSlotsMenu;
import com.reinhardt.hbm.menu.AnnihilatorMenu;
import com.reinhardt.hbm.menu.PrecisionAssemblerMenu;
import com.reinhardt.hbm.menu.RadiolysisMenu;
import com.reinhardt.hbm.menu.PyroOvenMenu;
import com.reinhardt.hbm.menu.AutocrafterMenu;
import com.reinhardt.hbm.menu.ForcefieldMenu;
import com.reinhardt.hbm.menu.OrbusMenu;
import com.reinhardt.hbm.menu.SatelliteLinkerMenu;
import com.reinhardt.hbm.menu.SatelliteDockMenu;
import com.reinhardt.hbm.menu.TurbofanMenu;
import com.reinhardt.hbm.menu.MissileAssemblyMenu;
import com.reinhardt.hbm.menu.DroneCrateMenu;
import com.reinhardt.hbm.menu.DroneGridMenu;
import com.reinhardt.hbm.menu.DroneRequesterMenu;
import com.reinhardt.hbm.menu.RebarPlacerMenu;
import com.reinhardt.hbm.menu.RadioRecMenu;
import com.reinhardt.hbm.menu.RadioTelexMenu;
import com.reinhardt.hbm.menu.AutocalMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<WoodBurnerMenu>> WOOD_BURNER =
            MENUS.register("wood_burner", () -> IMenuTypeExtension.create(WoodBurnerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RadioRecMenu>> RADIOREC =
            MENUS.register("radiorec", () -> IMenuTypeExtension.create(RadioRecMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RadioTelexMenu>> RADIO_TELEX =
            MENUS.register("radio_telex", () -> IMenuTypeExtension.create(RadioTelexMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AutocalMenu>> AUTOCAL =
            MENUS.register("radio_autocal", () -> IMenuTypeExtension.create(AutocalMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DieselGeneratorMenu>> DIESEL_GENERATOR =
            MENUS.register("machine_diesel", () -> IMenuTypeExtension.create(DieselGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CombustionEngineMenu>> COMBUSTION_ENGINE =
            MENUS.register("machine_combustion_engine", () -> IMenuTypeExtension.create(CombustionEngineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GasFlareMenu>> GAS_FLARE =
            MENUS.register("machine_flare", () -> IMenuTypeExtension.create(GasFlareMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE =
            MENUS.register("electric_furnace", () -> IMenuTypeExtension.create(ElectricFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ShredderMenu>> SHREDDER =
            MENUS.register("shredder", () -> IMenuTypeExtension.create(ShredderMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MicrowaveMenu>> MICROWAVE =
            MENUS.register("machine_microwave", () -> IMenuTypeExtension.create(MicrowaveMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ArmorTableMenu>> ARMOR_TABLE =
            MENUS.register("machine_armor_table", () -> IMenuTypeExtension.create(ArmorTableMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WeaponTableMenu>> WEAPON_TABLE =
            MENUS.register("machine_weapon_table", () -> IMenuTypeExtension.create(WeaponTableMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineKeyForgeMenu>> MACHINE_KEYFORGE =
            MENUS.register("machine_keyforge", () -> IMenuTypeExtension.create(MachineKeyForgeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DroneCrateMenu>> DRONE_CRATE =
            MENUS.register("drone_crate", () -> IMenuTypeExtension.create(DroneCrateMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DroneGridMenu>> DRONE_DOCK =
            MENUS.register("drone_dock", () -> IMenuTypeExtension.create((id, inventory, buffer) -> new DroneGridMenu(id, inventory, buffer, true)));

    public static final DeferredHolder<MenuType<?>, MenuType<DroneGridMenu>> DRONE_PROVIDER =
            MENUS.register("drone_crate_provider", () -> IMenuTypeExtension.create((id, inventory, buffer) -> new DroneGridMenu(id, inventory, buffer, false)));

    public static final DeferredHolder<MenuType<?>, MenuType<DroneRequesterMenu>> DRONE_REQUESTER =
            MENUS.register("drone_crate_requester", () -> IMenuTypeExtension.create(DroneRequesterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AssemblyMachineMenu>> ASSEMBLY_MACHINE =
            MENUS.register("assembly_machine", () -> IMenuTypeExtension.create(AssemblyMachineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AssemblyFactoryMenu>> ASSEMBLY_FACTORY =
            MENUS.register("assembly_factory", () -> IMenuTypeExtension.create(AssemblyFactoryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ChemicalPlantMenu>> CHEMICAL_PLANT =
            MENUS.register("chemical_plant", () -> IMenuTypeExtension.create(ChemicalPlantMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ChemicalFactoryMenu>> CHEMICAL_FACTORY =
            MENUS.register("chemical_factory", () -> IMenuTypeExtension.create(ChemicalFactoryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SolderingStationMenu>> SOLDERING_STATION =
            MENUS.register("soldering_station", () -> IMenuTypeExtension.create(SolderingStationMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ArcWelderMenu>> ARC_WELDER =
            MENUS.register("arc_welder", () -> IMenuTypeExtension.create(ArcWelderMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ArcFurnaceMenu>> ARC_FURNACE =
            MENUS.register("arc_furnace", () -> IMenuTypeExtension.create(ArcFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageDrumMenu>> STORAGE_DRUM =
            MENUS.register("storage_drum", () -> IMenuTypeExtension.create(StorageDrumMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CompressorMenu>> COMPRESSOR =
            MENUS.register("compressor", () -> IMenuTypeExtension.create(CompressorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MixerMenu>> MIXER =
            MENUS.register("mixer", () -> IMenuTypeExtension.create(MixerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AmmoPressMenu>> AMMO_PRESS =
            MENUS.register("machine_ammo_press", () -> IMenuTypeExtension.create(AmmoPressMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MachinePressMenu>> MACHINE_PRESS =
            MENUS.register("machine_press", () -> IMenuTypeExtension.create(MachinePressMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineEPressMenu>> MACHINE_EPRESS =
            MENUS.register("machine_epress", () -> IMenuTypeExtension.create(MachineEPressMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CentrifugeMenu>> CENTRIFUGE =
            MENUS.register("machine_centrifuge", () -> IMenuTypeExtension.create(CentrifugeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GasCentrifugeMenu>> GAS_CENTRIFUGE =
            MENUS.register("machine_gascent", () -> IMenuTypeExtension.create(GasCentrifugeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrystallizerMenu>> CRYSTALLIZER =
            MENUS.register("machine_crystallizer", () -> IMenuTypeExtension.create(CrystallizerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CyclotronMenu>> CYCLOTRON =
            MENUS.register("machine_cyclotron", () -> IMenuTypeExtension.create(CyclotronMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ExposureChamberMenu>> EXPOSURE_CHAMBER =
            MENUS.register("machine_exposure_chamber", () -> IMenuTypeExtension.create(ExposureChamberMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ParticleAcceleratorMenu>> PARTICLE_ACCELERATOR =
            MENUS.register("particle_accelerator", () -> IMenuTypeExtension.create(ParticleAcceleratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SilexMenu>> SILEX =
            MENUS.register("machine_silex", () -> IMenuTypeExtension.create(SilexMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FelMenu>> FEL =
            MENUS.register("machine_fel", () -> IMenuTypeExtension.create(FelMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MiningLaserMenu>> MINING_LASER =
            MENUS.register("machine_mining_laser", () -> IMenuTypeExtension.create(MiningLaserMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<OreSlopperMenu>> ORE_SLOPPER =
            MENUS.register("machine_ore_slopper", () -> IMenuTypeExtension.create(OreSlopperMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ExcavatorMenu>> EXCAVATOR =
            MENUS.register("machine_excavator", () -> IMenuTypeExtension.create(ExcavatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BreederReactorMenu>> BREEDER_REACTOR =
            MENUS.register("machine_reactor_breeding", () -> IMenuTypeExtension.create(BreederReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TurretJeremyMenu>> TURRET_JEREMY =
            MENUS.register("turret_jeremy", () -> IMenuTypeExtension.create(TurretJeremyMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TurretChekhovMenu>> TURRET_CHEKHOV =
            MENUS.register("turret_chekhov", () -> IMenuTypeExtension.create(TurretChekhovMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LegacyTurretMenu>> LEGACY_TURRET =
            MENUS.register("legacy_turret", () -> IMenuTypeExtension.create(LegacyTurretMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NukeBoyMenu>> NUKE_BOY =
            MENUS.register("nuke_boy", () -> IMenuTypeExtension.create(NukeBoyMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LegacyNukeMenu>> LEGACY_NUKE =
            MENUS.register("legacy_nuke", () -> IMenuTypeExtension.create(LegacyNukeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BombMultiMenu>> BOMB_MULTI =
            MENUS.register("bomb_multi", () -> IMenuTypeExtension.create(BombMultiMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SoyuzLauncherMenu>> SOYUZ_LAUNCHER =
            MENUS.register("soyuz_launcher", () -> IMenuTypeExtension.create(SoyuzLauncherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SoyuzCapsuleMenu>> SOYUZ_CAPSULE =
            MENUS.register("soyuz_capsule", () -> IMenuTypeExtension.create(SoyuzCapsuleMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ToolboxMenu>> TOOLBOX =
            MENUS.register("toolbox", () -> IMenuTypeExtension.create(ToolboxMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LegacyCraftBookMenu>> CRAFT_BOOK =
            MENUS.register("craft_book", () -> IMenuTypeExtension.create(LegacyCraftBookMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LegacyHeldInventoryMenu>> HELD_INVENTORY =
            MENUS.register("legacy_held_inventory", () -> IMenuTypeExtension.create(LegacyHeldInventoryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RebarPlacerMenu>> REBAR_PLACER =
            MENUS.register("rebar_placer", () -> IMenuTypeExtension.create(RebarPlacerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WasteDrumMenu>> WASTE_DRUM =
            MENUS.register("machine_waste_drum", () -> IMenuTypeExtension.create(WasteDrumMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WatzMenu>> WATZ =
            MENUS.register("watz", () -> IMenuTypeExtension.create(WatzMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PurexMenu>> PUREX =
            MENUS.register("machine_purex", () -> IMenuTypeExtension.create(PurexMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<IcfPressMenu>> ICF_PRESS =
            MENUS.register("machine_icf_press", () -> IMenuTypeExtension.create(IcfPressMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<IcfCoreMenu>> ICF =
            MENUS.register("icf", () -> IMenuTypeExtension.create(IcfCoreMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchReactorMenu>> RESEARCH_REACTOR =
            MENUS.register("machine_reactor_small", () -> IMenuTypeExtension.create(ResearchReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorControlMenu>> REACTOR_CONTROL =
            MENUS.register("machine_controller", () -> IMenuTypeExtension.create(ReactorControlMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ZirnoxReactorMenu>> ZIRNOX_REACTOR =
            MENUS.register("machine_zirnox", () -> IMenuTypeExtension.create(ZirnoxReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PwrMenu>> PWR =
            MENUS.register("pwr", () -> IMenuTypeExtension.create(PwrMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FusionMachineMenu>> FUSION_MACHINE =
            MENUS.register("fusion_machine", () -> IMenuTypeExtension.create(FusionMachineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DfcCoreMenu>> DFC_CORE =
            MENUS.register("dfc_core", () -> IMenuTypeExtension.create(DfcCoreMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DfcEmitterMenu>> DFC_EMITTER =
            MENUS.register("dfc_emitter", () -> IMenuTypeExtension.create(DfcEmitterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DfcReceiverMenu>> DFC_RECEIVER =
            MENUS.register("dfc_receiver", () -> IMenuTypeExtension.create(DfcReceiverMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DfcInjectorMenu>> DFC_INJECTOR =
            MENUS.register("dfc_injector", () -> IMenuTypeExtension.create(DfcInjectorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DfcStabilizerMenu>> DFC_STABILIZER =
            MENUS.register("dfc_stabilizer", () -> IMenuTypeExtension.create(DfcStabilizerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RbmkComponentMenu>> RBMK_COMPONENT =
            MENUS.register("rbmk_component", () -> IMenuTypeExtension.create(RbmkComponentMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HeaterMenu>> HEATER =
            MENUS.register("heater", () -> IMenuTypeExtension.create(HeaterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HbmAnvilMenu>> ANVIL =
            MENUS.register("anvil", () -> IMenuTypeExtension.create(HbmAnvilMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidPumpMenu>> FLUID_PUMP =
            MENUS.register("fluid_pump", () -> IMenuTypeExtension.create(FluidPumpMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidTankMenu>> FLUID_TANK =
            MENUS.register("fluid_tank", () -> IMenuTypeExtension.create(FluidTankMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FilingCabinetMenu>> FILING_CABINET =
            MENUS.register("filing_cabinet", () -> IMenuTypeExtension.create(FilingCabinetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SteamTurbineMenu>> STEAM_TURBINE =
            MENUS.register("machine_turbine", () -> IMenuTypeExtension.create(SteamTurbineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LargeTurbineMenu>> LARGE_TURBINE =
            MENUS.register("machine_large_turbine", () -> IMenuTypeExtension.create(LargeTurbineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GasTurbineMenu>> GAS_TURBINE =
            MENUS.register("machine_turbinegas", () -> IMenuTypeExtension.create(GasTurbineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SmallBoilerMenu>> SMALL_BOILER =
            MENUS.register("small_boiler", () -> IMenuTypeExtension.create(SmallBoilerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<OilDerrickMenu>> OIL_DERRICK =
            MENUS.register("machine_well", () -> IMenuTypeExtension.create(OilDerrickMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FrackingTowerMenu>> FRACKING_TOWER =
            MENUS.register("machine_fracking_tower", () -> IMenuTypeExtension.create(FrackingTowerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RefineryMenu>> REFINERY =
            MENUS.register("machine_refinery", () -> IMenuTypeExtension.create(RefineryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<VacuumDistillMenu>> VACUUM_DISTILL =
            MENUS.register("machine_vacuum_distill", () -> IMenuTypeExtension.create(VacuumDistillMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CokerMenu>> COKER =
            MENUS.register("machine_coker", () -> IMenuTypeExtension.create(CokerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FurnaceCombinationMenu>> FURNACE_COMBINATION =
            MENUS.register("furnace_combination", () -> IMenuTypeExtension.create(FurnaceCombinationMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RotaryFurnaceMenu>> ROTARY_FURNACE =
            MENUS.register("machine_rotary_furnace", () -> IMenuTypeExtension.create(RotaryFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SolidifierMenu>> SOLIDIFIER =
            MENUS.register("machine_solidifier", () -> IMenuTypeExtension.create(SolidifierMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectrolyzerMenu>> ELECTROLYZER =
            MENUS.register("machine_electrolyser", () -> IMenuTypeExtension.create(ElectrolyzerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LiquefactorMenu>> LIQUEFACTOR =
            MENUS.register("machine_liquefactor", () -> IMenuTypeExtension.create(LiquefactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CatalyticReformerMenu>> CATALYTIC_REFORMER =
            MENUS.register("machine_catalytic_reformer", () -> IMenuTypeExtension.create(CatalyticReformerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HydrotreaterMenu>> HYDROTREATER =
            MENUS.register("machine_hydrotreater", () -> IMenuTypeExtension.create(HydrotreaterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineBlastFurnaceMenu>> MACHINE_BLAST_FURNACE =
            MENUS.register("machine_blast_furnace", () -> IMenuTypeExtension.create(MachineBlastFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BrickFurnaceMenu>> BRICK_FURNACE =
            MENUS.register("machine_furnace_brick", () -> IMenuTypeExtension.create(BrickFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<IronFurnaceMenu>> IRON_FURNACE =
            MENUS.register("furnace_iron", () -> IMenuTypeExtension.create(IronFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SteelFurnaceMenu>> STEEL_FURNACE =
            MENUS.register("furnace_steel", () -> IMenuTypeExtension.create(SteelFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrucibleMenu>> CRUCIBLE =
            MENUS.register("machine_crucible", () -> IMenuTypeExtension.create(CrucibleMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StrandCasterMenu>> STRAND_CASTER =
            MENUS.register("machine_strand_caster", () -> IMenuTypeExtension.create(StrandCasterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageCrateMenu>> STORAGE_CRATE =
            MENUS.register("storage_crate", () -> IMenuTypeExtension.create(StorageCrateMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SafeMenu>> SAFE =
            MENUS.register("safe", () -> IMenuTypeExtension.create(SafeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MassStorageMenu>> MASS_STORAGE =
            MENUS.register("mass_storage", () -> IMenuTypeExtension.create(MassStorageMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AshpitMenu>> ASHPIT =
            MENUS.register("machine_ashpit", () -> IMenuTypeExtension.create(AshpitMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FunnelMenu>> FUNNEL =
            MENUS.register("machine_funnel", () -> IMenuTypeExtension.create(FunnelMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SirenMenu>> SIREN =
            MENUS.register("machine_siren", () -> IMenuTypeExtension.create(SirenMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BatteryReddMenu>> BATTERY_REDD =
            MENUS.register("machine_battery_redd", () -> IMenuTypeExtension.create(BatteryReddMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BatterySocketMenu>> BATTERY_SOCKET =
            MENUS.register("machine_battery_socket", () -> IMenuTypeExtension.create(BatterySocketMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RadGenMenu>> RADGEN =
            MENUS.register("machine_radgen", () -> IMenuTypeExtension.create(RadGenMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RtgMenu>> RTG_GREY =
            MENUS.register("machine_rtg_grey", () -> IMenuTypeExtension.create(RtgMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<OrbusMenu>> ORBUS =
            MENUS.register("machine_orbus", () -> IMenuTypeExtension.create(OrbusMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RadarMenu>> RADAR =
            MENUS.register("machine_radar", () -> IMenuTypeExtension.create(RadarMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RadarSlotsMenu>> RADAR_SLOTS =
            MENUS.register("machine_radar_slots", () -> IMenuTypeExtension.create(RadarSlotsMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AnnihilatorMenu>> ANNIHILATOR =
            MENUS.register("machine_annihilator", () -> IMenuTypeExtension.create(AnnihilatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TurbofanMenu>> TURBOFAN =
            MENUS.register("machine_turbofan", () -> IMenuTypeExtension.create(TurbofanMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MissileAssemblyMenu>> MISSILE_ASSEMBLY =
            MENUS.register("machine_missile_assembly", () -> IMenuTypeExtension.create(MissileAssemblyMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PrecisionAssemblerMenu>> PRECISION_ASSEMBLER =
            MENUS.register("machine_precass", () -> IMenuTypeExtension.create(PrecisionAssemblerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ForcefieldMenu>> FORCEFIELD =
            MENUS.register("machine_forcefield", () -> IMenuTypeExtension.create(ForcefieldMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SatelliteLinkerMenu>> SATELLITE_LINKER =
            MENUS.register("machine_satlinker", () -> IMenuTypeExtension.create(SatelliteLinkerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SatelliteDockMenu>> SAT_DOCK =
            MENUS.register("sat_dock", () -> IMenuTypeExtension.create(SatelliteDockMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AutocrafterMenu>> AUTOCRAFTER =
            MENUS.register("machine_autocrafter", () -> IMenuTypeExtension.create(AutocrafterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PyroOvenMenu>> PYRO_OVEN =
            MENUS.register("machine_pyrooven", () -> IMenuTypeExtension.create(PyroOvenMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RadiolysisMenu>> RADIOLYSIS =
            MENUS.register("machine_radiolysis", () -> IMenuTypeExtension.create(RadiolysisMenu::new));

    private HbmMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
