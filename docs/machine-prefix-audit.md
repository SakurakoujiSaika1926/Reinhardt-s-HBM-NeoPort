# machine_ Migration Audit

Generated from the 1.7.10 source declarations by `tools/audit-machine-prefix.ps1`.
`Complete` means the block has a real 1.21.1 registration. Rendering, collision, ports, GUI, recipes, and runtime behavior remain separately testable migration contracts.

- Legacy source: `E:\\MC\\Modsource\\HBM_1.7.10`
- Declared 1.7.10 machine_ IDs: **131**
- Explicitly deprecated in 1.7.10: **17**

| Legacy ID | Legacy block class | Legacy tile mapping | Status | Modern ID | BE registration |
| --- | --- | --- | --- | --- | --- |
| `machine_ammo_press` | MachineAmmoPress | TileEntityMachineAmmoPress -> tileentity_ammo_press | Complete registration | `machine_ammo_press` | registered |
| `machine_annihilator` | MachineAnnihilator | TileEntityMachineAnnihilator -> tileentity_annihilator | Complete registration | `machine_annihilator` | registered |
| `machine_arc_furnace` | MachineArcFurnaceLarge | TileEntityMachineArcFurnaceLarge -> tileentity_arc_furnace_large | Complete registration | `machine_arc_furnace` | registered |
| `machine_arc_welder` | MachineArcWelder | TileEntityMachineArcWelder -> tileentity_arc_welder | Complete registration | `machine_arc_welder` | registered |
| `machine_armor_table` | BlockArmorTable | - | Complete registration | `machine_armor_table` | not required or custom |
| `machine_ashpit` | MachineAshpit | - | Complete registration | `machine_ashpit` | registered |
| `machine_assembly_factory` | MachineAssemblyFactory | TileEntityMachineAssemblyFactory -> tileentity_assemblyfactory | Complete registration | `machine_assembly_factory` | registered |
| `machine_assembly_machine` | MachineAssemblyMachine | TileEntityMachineAssemblyMachine -> tileentity_assemblymachine | Complete registration | `machine_assembly_machine` | registered |
| `machine_autocrafter` | MachineAutocrafter | TileEntityMachineAutocrafter -> tileentity_autocrafter | Complete registration | `machine_autocrafter` | registered |
| `machine_autosaw` | MachineAutosaw | TileEntityMachineAutosaw -> tileentity_autosaw | Complete registration | `machine_autosaw` | registered |
| `machine_bat9000` | MachineBigAssTank9000 | - | Deprecated in 1.7.10 - not ported | `machine_bat9000` | - |
| `machine_battery` | MachineBattery | TileEntityMachineBattery -> tileentity_battery | Deprecated in 1.7.10 - not ported | `machine_battery` | - |
| `machine_battery_potato` | MachineBattery | TileEntityMachineBattery -> tileentity_battery | Deprecated in 1.7.10 - not ported | `machine_battery_potato` | - |
| `machine_battery_redd` | MachineBatteryREDD | - | Complete registration | `machine_battery_redd` | registered |
| `machine_battery_socket` | MachineBatterySocket | - | Complete registration | `machine_battery_socket` | registered |
| `machine_bigasstank` | MachineBigAssTank | TileEntityMachineBigAssTank -> tileentity_bigasstank | Complete registration (renamed) | `machine_bat9000` | registered |
| `machine_blast_furnace` | MachineBlastFurnace | TileEntityMachineBlastFurnace -> tilentity_blast_furnace | Complete registration | `machine_blast_furnace` | registered |
| `machine_boiler` | MachineHeatBoiler | - | Complete registration (renamed) | `machine_boiler_off` | registered |
| `machine_boiler_off` | MachineBoiler | - | Complete registration | `machine_boiler_off` | registered |
| `machine_catalytic_cracker` | MachineCatalyticCracker | TileEntityMachineCatalyticCracker -> tileentity_catalytic_cracker | Complete registration | `machine_catalytic_cracker` | registered |
| `machine_catalytic_reformer` | MachineCatalyticReformer | TileEntityMachineCatalyticReformer -> tileentity_catalytic_reformer | Complete registration | `machine_catalytic_reformer` | registered |
| `machine_centrifuge` | MachineCentrifuge | TileEntityMachineCentrifuge -> tileentity_centrifuge | Complete registration | `machine_centrifuge` | registered |
| `machine_chemical_factory` | MachineChemicalFactory | TileEntityMachineChemicalFactory -> tileentity_chemicalfactory | Complete registration | `machine_chemical_factory` | registered |
| `machine_chemical_plant` | MachineChemicalPlant | TileEntityMachineChemicalPlant -> tileentity_chemicalplant | Complete registration | `machine_chemical_plant` | registered |
| `machine_chungus` | MachineChungus | - | Complete registration | `machine_chungus` | registered |
| `machine_coker` | MachineCoker | TileEntityMachineCoker -> tileentity_coker | Complete registration | `machine_coker` | registered |
| `machine_combustion_engine` | MachineCombustionEngine | TileEntityMachineCombustionEngine -> tileentity_combustion_engine | Complete registration | `machine_combustion_engine` | registered |
| `machine_compressor` | MachineCompressor | TileEntityMachineCompressor -> tileentity_compressor | Complete registration | `machine_compressor` | registered |
| `machine_compressor_compact` | MachineCompressorCompact | TileEntityMachineCompressorCompact -> tileentity_compressor_compact | Complete registration | `machine_compressor_compact` | registered |
| `machine_condenser` | MachineCondenser | - | Complete registration | `machine_condenser` | registered |
| `machine_condenser_powered` | MachineCondenserPowered | - | Complete registration | `machine_condenser_powered` | registered |
| `machine_controller` | MachineReactorControl | - | Complete registration | `machine_controller` | registered |
| `machine_converter_he_rf` | BlockConverterHeRf | - | Complete registration | `machine_converter_he_rf` | registered |
| `machine_converter_rf_he` | BlockConverterRfHe | - | Complete registration | `machine_converter_rf_he` | registered |
| `machine_conveyor_press` | MachineConveyorPress | - | Complete registration | `machine_conveyor_press` | registered |
| `machine_crucible` | MachineCrucible | - | Complete registration | `machine_crucible` | registered |
| `machine_crystallizer` | MachineCrystallizer | TileEntityMachineCrystallizer -> tileentity_acidomatic | Complete registration | `machine_crystallizer` | registered |
| `machine_cyclotron` | MachineCyclotron | TileEntityMachineCyclotron -> tileentity_cyclotron | Complete registration | `machine_cyclotron` | registered |
| `machine_detector` | PowerDetector | - | Complete registration | `machine_detector` | registered |
| `machine_deuterium_extractor` | MachineDeuteriumExtractor | - | Complete registration | `machine_deuterium_extractor` | registered |
| `machine_deuterium_tower` | DeuteriumTower | TileEntityDeuteriumTower -> tileentity_deuterium_tower | Complete registration | `machine_deuterium_tower` | registered |
| `machine_diesel` | MachineDiesel | TileEntityMachineDiesel -> tileentity_diesel_generator | Complete registration | `machine_diesel` | registered |
| `machine_difurnace_extension` | MachineDiFurnaceExtension | - | Deprecated in 1.7.10 - not ported | `machine_difurnace_ext` | - |
| `machine_difurnace_off` | MachineDiFurnace | - | Deprecated in 1.7.10 - not ported | `machine_difurnace_off` | - |
| `machine_difurnace_on` | MachineDiFurnace | - | Deprecated in 1.7.10 - not ported | `machine_difurnace_on` | - |
| `machine_difurnace_rtg_off` | MachineDiFurnaceRTG | - | Deprecated in 1.7.10 - not ported | `machine_difurnace_rtg_off` | - |
| `machine_difurnace_rtg_on` | MachineDiFurnaceRTG | - | Deprecated in 1.7.10 - not ported | `machine_difurnace_rtg_on` | - |
| `machine_dineutronium_battery` | MachineBattery | TileEntityMachineBattery -> tileentity_battery | Deprecated in 1.7.10 - not ported | `machine_dineutronium_battery` | - |
| `machine_drain` | MachineDrain | TileEntityMachineDrain -> tileentity_fluid_drain | Complete registration | `machine_drain` | registered |
| `machine_electric_furnace_off` | MachineElectricFurnace | TileEntityMachineElectricFurnace -> tileentity_electric_furnace | Complete registration | `machine_electric_furnace_off` | registered |
| `machine_electric_furnace_on` | MachineElectricFurnace | TileEntityMachineElectricFurnace -> tileentity_electric_furnace | Complete registration | `machine_electric_furnace_on` | registered |
| `machine_electrolyser` | MachineElectrolyser | - | Complete registration | `machine_electrolyser` | registered |
| `machine_epress` | MachineEPress | TileEntityMachineEPress -> tileentity_electric_press | Complete registration | `machine_epress` | registered |
| `machine_excavator` | MachineExcavator | TileEntityMachineExcavator -> tileentity_ntm_excavator | Complete registration | `machine_excavator` | registered |
| `machine_exposure_chamber` | MachineExposureChamber | TileEntityMachineExposureChamber -> tileentity_exposure_chamber | Complete registration | `machine_exposure_chamber` | registered |
| `machine_fel` | MachineFEL | - | Complete registration | `machine_fel` | registered |
| `machine_fensu` | MachineFENSU | TileEntityMachineFENSU -> tileentity_fensu | Deprecated in 1.7.10 - not ported | `machine_battery_redd` | - |
| `machine_flare` | MachineGasFlare | TileEntityMachineGasFlare -> tileentity_gasflare | Complete registration | `machine_flare` | registered |
| `machine_fluidtank` | MachineFluidTank | TileEntityMachineFluidTank -> tileentity_fluid_tank | Complete registration | `machine_fluidtank` | registered |
| `machine_forcefield` | MachineForceField | - | Complete registration | `machine_forcefield` | registered |
| `machine_fracking_tower` | MachineFrackingTower | TileEntityMachineFrackingTower -> tileentity_fracking_tower | Complete registration | `machine_fracking_tower` | registered |
| `machine_fraction_tower` | MachineFractionTower | TileEntityMachineFractionTower -> tileentity_fraction_tower | Complete registration | `machine_fraction_tower` | registered |
| `machine_funnel` | MachineFunnel | TileEntityMachineFunnel -> tileentity_funnel | Complete registration | `machine_funnel` | registered |
| `machine_furnace_brick_off` | MachineBrickFurnace | - | Complete registration | `machine_furnace_brick_off` | registered |
| `machine_furnace_brick_on` | MachineBrickFurnace | - | Complete registration | `machine_furnace_brick_on` | registered |
| `machine_gascent` | MachineGasCent | TileEntityMachineGasCent -> tileentity_gas_centrifuge | Complete registration | `machine_gascent` | registered |
| `machine_hephaestus` | MachineHephaestus | TileEntityMachineHephaestus -> tileentity_hephaestus | Complete registration | `machine_hephaestus` | registered |
| `machine_hydrotreater` | MachineHydrotreater | TileEntityMachineHydrotreater -> tileentity_hydrotreater | Complete registration | `machine_hydrotreater` | registered |
| `machine_icf_press` | MachineICFPress | - | Complete registration | `machine_icf_press` | registered |
| `machine_industrial_boiler` | MachineHeatBoilerIndustrial | - | Complete registration | `machine_industrial_boiler` | registered |
| `machine_industrial_generator` | MachineIGenerator | TileEntityMachineIGenerator -> tileentity_igenerator | Retired in 1.7.10 - not ported | `machine_industrial_generator` | - |
| `machine_industrial_turbine` | MachineIndustrialTurbine | TileEntityMachineIndustrialTurbine -> tileentity_ind_turbine | Complete registration | `machine_industrial_turbine` | registered |
| `machine_intake` | MachineIntake | TileEntityMachineIntake -> tileentity_intake | Complete registration | `machine_intake` | registered |
| `machine_keyforge` | MachineKeyForge | TileEntityMachineKeyForge -> tileentity_key_forge | Complete registration | `machine_keyforge` | registered |
| `machine_large_turbine` | MachineLargeTurbine | TileEntityMachineLargeTurbine -> tileentity_industrial_turbine | Deprecated in 1.7.10 - not ported | `machine_large_turbine` | - |
| `machine_liquefactor` | MachineLiquefactor | TileEntityMachineLiquefactor -> tileentity_liquefactor | Complete registration | `machine_liquefactor` | registered |
| `machine_lithium_battery` | MachineBattery | TileEntityMachineBattery -> tileentity_battery | Deprecated in 1.7.10 - not ported | `machine_lithium_battery` | - |
| `machine_lpw2` | MachineLPW2 | TileEntityMachineLPW2 -> tileentity_machine_lpw2 | Complete registration | `machine_lpw2` | registered |
| `machine_microwave` | MachineMicrowave | - | Complete registration | `machine_microwave` | registered |
| `machine_mining_laser` | MachineMiningLaser | TileEntityMachineMiningLaser -> tileentity_mining_laser | Complete registration | `machine_mining_laser` | registered |
| `machine_minirtg` | MachineMiniRTG | TileEntityMachineMiniRTG -> tileentity_mini_rtg | Deprecated in 1.7.10 - not ported | `machine_minirtg` | - |
| `machine_missile_assembly` | MachineMissileAssembly | TileEntityMachineMissileAssembly -> tileentity_missile_assembly | Complete 1.7.10 model, support-gantry renderer, menu, manual-only automation, construction, sound, recipe and resource migration | `machine_missile_assembly` | source-audited |
| `machine_mixer` | MachineMixer | TileEntityMachineMixer -> tileentity_mixer | Complete registration | `machine_mixer` | registered |
| `machine_orbus` | MachineOrbus | TileEntityMachineOrbus -> tileentity_orbus | Complete registration | `machine_orbus` | registered |
| `machine_ore_slopper` | MachineOreSlopper | TileEntityMachineOreSlopper -> tileentity_ore_slopper | Complete registration | `machine_ore_slopper` | registered |
| `machine_powerrtg` | MachineMiniRTG | TileEntityMachineMiniRTG -> tileentity_mini_rtg | Deprecated in 1.7.10 - not ported | `machine_powerrtg` | - |
| `machine_precass` | MachinePrecAss | TileEntityMachinePrecAss -> tileentity_precass | Complete registration | `machine_precass` | registered |
| `machine_press` | MachinePress | TileEntityMachinePress -> tileentity_press | Complete registration | `machine_press` | registered |
| `machine_puf6_tank` | MachinePuF6Tank | TileEntityMachinePuF6Tank -> tileentity_puf6_tank | Complete registration | `machine_puf6_tank` | registered |
| `machine_pumpjack` | MachinePumpjack | TileEntityMachinePumpjack -> tileentity_machine_pumpjack | Complete registration | `machine_pumpjack` | registered |
| `machine_purex` | MachinePUREX | TileEntityMachinePUREX -> tileentity_purex | Complete registration | `machine_purex` | registered |
| `machine_pyrooven` | MachinePyroOven | TileEntityMachinePyroOven -> tileentity_pyrooven | Complete registration | `machine_pyrooven` | registered |
| `machine_radar` | MachineRadar | - | Complete registration | `machine_radar` | registered |
| `machine_radar_large` | MachineRadarLarge | TileEntityMachineRadarLarge -> tileentity_radar_large | Complete registration | `machine_radar_large` | registered |
| `machine_radgen` | MachineRadGen | TileEntityMachineRadGen -> tileentity_radgen | Complete registration | `machine_radgen` | registered |
| `machine_radiolysis` | MachineRadiolysis | TileEntityMachineRadiolysis -> tileentity_radiolysis | Complete registration | `machine_radiolysis` | registered |
| `machine_reactor_breeding` | MachineReactorBreeding | TileEntityMachineReactorBreeding -> tileentity_reactor | Complete registration | `machine_reactor_breeding` | registered |
| `machine_refinery` | MachineRefinery | TileEntityMachineRefinery -> tileentity_refinery | Complete registration | `machine_refinery` | registered |
| `machine_rotary_furnace` | MachineRotaryFurnace | TileEntityMachineRotaryFurnace -> tileentity_rotary_furnace | Complete registration | `machine_rotary_furnace` | registered |
| `machine_rtg_furnace_off` | MachineRtgFurnace | - | Deprecated in 1.7.10 - not ported | `machine_rtg_furnace_off` | - |
| `machine_rtg_furnace_on` | MachineRtgFurnace | - | Deprecated in 1.7.10 - not ported | `machine_rtg_furnace_on` | - |
| `machine_rtg_grey` | MachineRTG | TileEntityMachineRTG -> tileentity_machine_rtg | Complete registration | `machine_rtg_grey` | registered |
| `machine_satlinker` | MachineSatLinker | TileEntityMachineSatLinker -> tileentity_satlinker | Complete registration | `machine_satlinker` | registered |
| `machine_sawmill` | MachineSawmill | - | Complete registration | `machine_sawmill` | registered |
| `machine_schrabidium_battery` | MachineBattery | TileEntityMachineBattery -> tileentity_battery | Deprecated in 1.7.10 - not ported | `machine_schrabidium_battery` | - |
| `machine_shredder` | MachineShredder | TileEntityMachineShredder -> tileentity_machine_shredder | Complete registration | `machine_shredder` | registered |
| `machine_silex` | MachineSILEX | - | Complete registration | `machine_silex` | registered |
| `machine_siren` | MachineSiren | TileEntityMachineSiren -> tileentity_siren | Complete registration | `machine_siren` | registered |
| `machine_solar_boiler` | MachineSolarBoiler | - | Complete registration | `machine_solar_boiler` | registered |
| `machine_soldering_station` | MachineSolderingStation | TileEntityMachineSolderingStation -> tileentity_soldering_station | Complete registration | `machine_soldering_station` | registered |
| `machine_solidifier` | MachineSolidifier | TileEntityMachineSolidifier -> tileentity_solidifier | Complete registration | `machine_solidifier` | registered |
| `machine_steam_engine` | MachineSteamEngine | - | Complete registration | `machine_steam_engine` | registered |
| `machine_stirling` | MachineStirling | - | Complete registration | `machine_stirling` | registered |
| `machine_stirling_creative` | MachineStirling | - | Complete registration | `machine_stirling_creative` | registered |
| `machine_stirling_steel` | MachineStirling | - | Complete registration | `machine_stirling_steel` | registered |
| `machine_storage_drum` | StorageDrum | TileEntityStorageDrum -> tileentity_waste_storage_drum | Complete registration | `machine_storage_drum` | registered |
| `machine_strand_caster` | MachineStrandCaster | TileEntityMachineStrandCaster -> tileentity_strand_caster | Complete registration | `machine_strand_caster` | registered |
| `machine_teleporter` | MachineTeleporter | TileEntityMachineTeleporter -> tileentity_teleblock | Complete registration | `machine_teleporter` | registered |
| `machine_thresher` | MachineThresher | TileEntityMachineThresher -> tileentity_thresher | Complete registration | `machine_thresher` | registered |
| `machine_tower_large` | MachineTowerLarge | - | Complete registration | `machine_tower_large` | registered |
| `machine_tower_small` | MachineTowerSmall | - | Complete registration | `machine_tower_small` | registered |
| `machine_transformer` | MachineTransformer | - | Complete registration | `machine_transformer` | not required or custom |
| `machine_turbine` | MachineTurbine | TileEntityMachineTurbine -> tileentity_turbine | Complete registration | `machine_turbine` | registered |
| `machine_turbinegas` | MachineTurbineGas | TileEntityMachineTurbineGas -> tileentity_machine_gasturbine | Complete registration (renamed) | `machine_turbine_gas` | registered |
| `machine_turbofan` | MachineTurbofan | TileEntityMachineTurbofan -> tileentity_machine_turbofan | Complete registration | `machine_turbofan` | registered |
| `machine_uf6_tank` | MachineUF6Tank | TileEntityMachineUF6Tank -> tileentity_uf6_tank | Complete registration | `machine_uf6_tank` | registered |
| `machine_vacuum_distill` | MachineVacuumDistill | TileEntityMachineVacuumDistill -> tileentity_vacuuum_distill | Complete registration | `machine_vacuum_distill` | registered |
| `machine_waste_drum` | WasteDrum | TileEntityWasteDrum -> tileentity_waste_drum | Complete registration | `machine_waste_drum` | registered |
| `machine_weapon_table` | BlockWeaponTable | - | retired: HBM combat firearms removed in favour of TACZ | - | not registered |
| `machine_well` | MachineOilWell | TileEntityMachineOilWell -> tileentity_derrick | Complete registration | `machine_well` | registered |
| `machine_wood_burner` | MachineWoodBurner | TileEntityMachineWoodBurner -> tileentity_wood_burner | Complete registration | `machine_wood_burner` | registered |

## Result

- Active legacy machines registered in 1.21.1: **113**
- Active legacy machines still missing: **0**
