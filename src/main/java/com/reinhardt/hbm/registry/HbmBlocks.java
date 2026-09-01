package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.AssemblyFactoryBlock;
import com.reinhardt.hbm.block.AssemblyMachineBlock;
import com.reinhardt.hbm.block.AirCompressorBlock;
import com.reinhardt.hbm.block.AmmoPressBlock;
import com.reinhardt.hbm.block.AmmoCrateBlock;
import com.reinhardt.hbm.block.ArcWelderBlock;
import com.reinhardt.hbm.block.ArcFurnaceBlock;
import com.reinhardt.hbm.block.AutocalBlock;
import com.reinhardt.hbm.block.StorageDrumBlock;
import com.reinhardt.hbm.block.AncientScrapBlock;
import com.reinhardt.hbm.block.ArmorTableBlock;
import com.reinhardt.hbm.block.AsbestosGasBlock;
import com.reinhardt.hbm.block.AshpitBlock;
import com.reinhardt.hbm.block.AsbestosDecoBlock;
import com.reinhardt.hbm.block.BarbedWireBlock;
import com.reinhardt.hbm.block.BatteryReddBlock;
import com.reinhardt.hbm.block.BatterySocketBlock;
import com.reinhardt.hbm.block.BoatBlock;
import com.reinhardt.hbm.block.BobbleheadBlock;
import com.reinhardt.hbm.block.CanCrateBlock;
import com.reinhardt.hbm.block.SnowglobeBlock;
import com.reinhardt.hbm.block.PlushieBlock;
import com.reinhardt.hbm.block.LanternBlock;
import com.reinhardt.hbm.block.LanternBehemothBlock;
import com.reinhardt.hbm.block.BrickFurnaceBlock;
import com.reinhardt.hbm.block.BedrockOreBlock;
import com.reinhardt.hbm.block.BigAssTankBlock;
import com.reinhardt.hbm.block.BlastDoorBlock;
import com.reinhardt.hbm.block.BlastDoorDummyBlock;
import com.reinhardt.hbm.block.BalefireBlock;
import com.reinhardt.hbm.block.BreederReactorBlock;
import com.reinhardt.hbm.block.BroadcasterBlock;
import com.reinhardt.hbm.block.GeigerBlock;
import com.reinhardt.hbm.block.CatalyticCrackerBlock;
import com.reinhardt.hbm.block.CatalyticReformerBlock;
import com.reinhardt.hbm.block.CapBlock;
import com.reinhardt.hbm.block.HydrotreaterBlock;
import com.reinhardt.hbm.block.ChlorineGasBlock;
import com.reinhardt.hbm.block.ChimneyBlock;
import com.reinhardt.hbm.block.CentrifugeBlock;
import com.reinhardt.hbm.block.ChemicalFactoryBlock;
import com.reinhardt.hbm.block.ChemicalPlantBlock;
import com.reinhardt.hbm.block.CoatedEnergyCableBlock;
import com.reinhardt.hbm.block.PaintableEnergyCableBlock;
import com.reinhardt.hbm.block.PowerCableBoxBlock;
import com.reinhardt.hbm.block.PowerGaugeBlock;
import com.reinhardt.hbm.block.CableDiodeBlock;
import com.reinhardt.hbm.block.CableDetectorBlock;
import com.reinhardt.hbm.block.CableSwitchBlock;
import com.reinhardt.hbm.block.ChargerBlock;
import com.reinhardt.hbm.block.CoalDustGasBlock;
import com.reinhardt.hbm.block.CombustionEngineBlock;
import com.reinhardt.hbm.block.CompressorBlock;
import com.reinhardt.hbm.block.ConveyorBlock;
import com.reinhardt.hbm.block.ConveyorPressBlock;
import com.reinhardt.hbm.block.CraneMachineBlock;
import com.reinhardt.hbm.block.CustomMachineBlock;
import com.reinhardt.hbm.block.IndustrialGeneratorBlock;
import com.reinhardt.hbm.block.ConcreteColoredBlock;
import com.reinhardt.hbm.block.VinylTileBlock;
import com.reinhardt.hbm.block.CokeBlock;
import com.reinhardt.hbm.item.CapBlockItem;
import com.reinhardt.hbm.item.BobbleheadBlockItem;
import com.reinhardt.hbm.item.SnowglobeBlockItem;
import com.reinhardt.hbm.item.PlushieBlockItem;
import com.reinhardt.hbm.item.LanternBlockItem;
import com.reinhardt.hbm.block.CoolingTowerBlock;
import com.reinhardt.hbm.block.CapacitorBlock;
import com.reinhardt.hbm.block.CapacitorBusBlock;
import com.reinhardt.hbm.block.CoriumBlock;
import com.reinhardt.hbm.block.CaveSpikeBlock;
import com.reinhardt.hbm.block.CargoElevatorBlock;
import com.reinhardt.hbm.block.CrystallizerBlock;
import com.reinhardt.hbm.block.JungleCrateBlock;
import com.reinhardt.hbm.block.LootCrateBlock;
import com.reinhardt.hbm.block.CrystalPulsarBlock;
import com.reinhardt.hbm.block.CrystalVirusBlock;
import com.reinhardt.hbm.block.CyclotronBlock;
import com.reinhardt.hbm.block.DfcComponentBlock;
import com.reinhardt.hbm.block.DfcCoreBlock;
import com.reinhardt.hbm.block.DemonLampBlock;
import com.reinhardt.hbm.block.DecorationEmitterBlock;
import com.reinhardt.hbm.block.DeadPlantBlock;
import com.reinhardt.hbm.block.DepthOreBlock;
import com.reinhardt.hbm.block.DepthRockBlock;
import com.reinhardt.hbm.block.DetonatableBlock;
import com.reinhardt.hbm.block.DropOreBlock;
import com.reinhardt.hbm.block.BiomeStoneBlock;
import com.reinhardt.hbm.block.ForgottenLockBlock;
import com.reinhardt.hbm.block.LegacyPillarBlock;
import com.reinhardt.hbm.block.LegacyNukeBlock;
import com.reinhardt.hbm.block.LegacyNukeDefinition;
import com.reinhardt.hbm.block.ResourceStoneBlock;
import com.reinhardt.hbm.block.WoodStructureBlock;
import com.reinhardt.hbm.block.DenseRadonGasBlock;
import com.reinhardt.hbm.block.DeuteriumExtractorBlock;
import com.reinhardt.hbm.block.DeuteriumTowerBlock;
import com.reinhardt.hbm.block.DieselGeneratorBlock;
import com.reinhardt.hbm.block.DeconBlock;
import com.reinhardt.hbm.block.DrainBlock;
import com.reinhardt.hbm.item.DrainBlockItem;
import com.reinhardt.hbm.block.DroneWaypointBlock;
import com.reinhardt.hbm.block.DroneNetworkContainerBlock;
import com.reinhardt.hbm.block.DungeonChainBlock;
import com.reinhardt.hbm.block.DungeonSpawnerBlock;
import com.reinhardt.hbm.block.EnargiteBrickBlock;
import com.reinhardt.hbm.block.ElectrolyzerBlock;
import com.reinhardt.hbm.block.EnergyConverterBlock;
import com.reinhardt.hbm.block.ExplosiveGasBlock;
import com.reinhardt.hbm.block.ExcavatorBlock;
import com.reinhardt.hbm.block.ExposureChamberBlock;
import com.reinhardt.hbm.block.FelBlock;
import com.reinhardt.hbm.block.FalloutBlock;
import com.reinhardt.hbm.block.LegacyHazardLiquidBlock;
import com.reinhardt.hbm.block.LegacyEmitterBlock;
import com.reinhardt.hbm.block.FalloutFallingBlock;
import com.reinhardt.hbm.block.EnergyCableBlock;
import com.reinhardt.hbm.block.FluidDuctBlock;
import com.reinhardt.hbm.block.FluidBarrelBlock;
import com.reinhardt.hbm.block.FluidPumpBlock;
import com.reinhardt.hbm.block.FluidTankBlock;
import com.reinhardt.hbm.block.FlammableGasBlock;
import com.reinhardt.hbm.block.FractionSpacerBlock;
import com.reinhardt.hbm.block.FractionTowerBlock;
import com.reinhardt.hbm.block.FunnelBlock;
import com.reinhardt.hbm.item.FunnelBlockItem;
import com.reinhardt.hbm.block.FoundryCastingBlock;
import com.reinhardt.hbm.block.FoundryChannelBlock;
import com.reinhardt.hbm.block.FoundryOutletBlock;
import com.reinhardt.hbm.block.FoundrySlagBlock;
import com.reinhardt.hbm.block.FoundryTankBlock;
import com.reinhardt.hbm.block.FurnaceCombinationBlock;
import com.reinhardt.hbm.block.FrackingTowerBlock;
import com.reinhardt.hbm.block.FissureBlock;
import com.reinhardt.hbm.block.FissureBombBlock;
import com.reinhardt.hbm.block.FireworksBlock;
import com.reinhardt.hbm.block.FusionHatchBlock;
import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.block.FusionTorusStructBlock;
import com.reinhardt.hbm.block.GasFlareBlock;
import com.reinhardt.hbm.block.GasTurbineBlock;
import com.reinhardt.hbm.block.GroundwaterPumpBlock;
import com.reinhardt.hbm.block.GeothermalHeatExchangerBlock;
import com.reinhardt.hbm.block.GlyphBlock;
import com.reinhardt.hbm.block.GlyphidBaseBlock;
import com.reinhardt.hbm.block.GlyphidSpawnerBlock;
import com.reinhardt.hbm.block.GuideTerminalBlock;
import com.reinhardt.hbm.block.HbmFallingBlock;
import com.reinhardt.hbm.block.HangingPhosphorVineBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorPartBlock;
import com.reinhardt.hbm.block.HbmLegacyDoorBlock;
import com.reinhardt.hbm.block.HbmLegacyTrapDoorBlock;
import com.reinhardt.hbm.block.HbmRailBlock;
import com.reinhardt.hbm.block.LegacyNarrowStraightRailBlock;
import com.reinhardt.hbm.block.LegacyRedstoneBombBlock;
import com.reinhardt.hbm.block.RadioRecBlock;
import com.reinhardt.hbm.block.PartEmitterBlock;
import com.reinhardt.hbm.block.RadioboxBlock;
import com.reinhardt.hbm.block.RefuelerBlock;
import com.reinhardt.hbm.block.VendingMachineBlock;
import com.reinhardt.hbm.block.FieldDisturberBlock;
import com.reinhardt.hbm.block.PneumaticStorageBlock;
import com.reinhardt.hbm.block.PneumaticTubeBlock;
import com.reinhardt.hbm.block.HadronCoilBlock;
import com.reinhardt.hbm.block.HbmMushroomBlock;
import com.reinhardt.hbm.block.HbmAnvilBlock;
import com.reinhardt.hbm.block.HeatBoilerBlock;
import com.reinhardt.hbm.block.HevBatteryBlock;
import com.reinhardt.hbm.block.HexafluorideTankBlock;
import com.reinhardt.hbm.block.HeaterBlock;
import com.reinhardt.hbm.block.IndustrialBoilerBlock;
import com.reinhardt.hbm.block.IcfPressBlock;
import com.reinhardt.hbm.block.IcfCoreBlock;
import com.reinhardt.hbm.block.IcfStructBlock;
import com.reinhardt.hbm.block.IcfControllerBlock;
import com.reinhardt.hbm.block.IcfLaserComponentBlock;
import com.reinhardt.hbm.block.IcfAssembledLaserBlock;
import com.reinhardt.hbm.block.CrucibleBlock;
import com.reinhardt.hbm.block.IndustrialTurbineBlock;
import com.reinhardt.hbm.block.LargeTurbineBlock;
import com.reinhardt.hbm.block.LegacyTurretBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.LargeFluidTankBlock;
import com.reinhardt.hbm.block.PipeAnchorBlock;
import com.reinhardt.hbm.block.PistonInserterBlock;
import com.reinhardt.hbm.item.LegacyTankBlockItem;
import com.reinhardt.hbm.item.VolcanoCoreBlockItem;
import com.reinhardt.hbm.item.VendingMachineBlockItem;
import com.reinhardt.hbm.item.PowerPylonBlockItem;
import com.reinhardt.hbm.item.PowerCableBoxBlockItem;
import com.reinhardt.hbm.block.CompactLauncherBlock;
import com.reinhardt.hbm.block.LauncherDummyBlock;
import com.reinhardt.hbm.block.LauncherStructCoreBlock;
import com.reinhardt.hbm.block.LaunchPadBlock;
import com.reinhardt.hbm.block.LaunchTableBlock;
import com.reinhardt.hbm.block.LeviathanTurbineBlock;
import com.reinhardt.hbm.block.LegacyFurnaceBlock;
import com.reinhardt.hbm.block.LegacyDirectionalBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.block.LegacyVariantSlabBlock;
import com.reinhardt.hbm.block.LegacyVariantStrengths;
import com.reinhardt.hbm.block.LegacyBarrelBlock;
import com.reinhardt.hbm.block.TaintBlock;
import com.reinhardt.hbm.block.LiquefactorBlock;
import com.reinhardt.hbm.block.LandmineBlock;
import com.reinhardt.hbm.block.LegacyMachineBlock;
import com.reinhardt.hbm.block.LegacyDisplayStandBlock;
import com.reinhardt.hbm.block.SealControllerBlock;
import com.reinhardt.hbm.block.SealHatchBlock;
import com.reinhardt.hbm.block.StoneKeyholeBlock;
import com.reinhardt.hbm.item.LandmineBlockItem;
import com.reinhardt.hbm.item.MicrowaveBlockItem;
import com.reinhardt.hbm.item.ToasterBlockItem;
import com.reinhardt.hbm.block.MachineBlastFurnaceBlock;
import com.reinhardt.hbm.block.MachineCokerBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.block.MachineKeyForgeBlock;
import com.reinhardt.hbm.block.SatelliteLinkerBlock;
import com.reinhardt.hbm.block.SatelliteDockBlock;
import com.reinhardt.hbm.block.SatelliteDockDummyBlock;
import com.reinhardt.hbm.block.MicrowaveBlock;
import com.reinhardt.hbm.block.MiningLaserBlock;
import com.reinhardt.hbm.block.MixerBlock;
import com.reinhardt.hbm.block.MetalFenceBlock;
import com.reinhardt.hbm.block.MeteorOreBlock;
import com.reinhardt.hbm.block.MeteorMoltenBlock;
import com.reinhardt.hbm.block.MeltdownGasBlock;
import com.reinhardt.hbm.block.MonoxideGasBlock;
import com.reinhardt.hbm.block.MustardGasBlock;
import com.reinhardt.hbm.block.MustardWillowFlowerBlock;
import com.reinhardt.hbm.block.MustardWillowTallBlock;
import com.reinhardt.hbm.block.NukeBoyBlock;
import com.reinhardt.hbm.block.BombMultiBlock;
import com.reinhardt.hbm.block.CrashedBombBlock;
import com.reinhardt.hbm.block.SoyuzCapsuleBlock;
import com.reinhardt.hbm.block.SoyuzLauncherBlock;
import com.reinhardt.hbm.block.OilSpillBlock;
import com.reinhardt.hbm.block.OilDepositBlock;
import com.reinhardt.hbm.block.OilDerrickBlock;
import com.reinhardt.hbm.block.OilPipeBlock;
import com.reinhardt.hbm.block.OilPumpjackBlock;
import com.reinhardt.hbm.block.OilSandBlock;
import com.reinhardt.hbm.block.OreSlopperBlock;
import com.reinhardt.hbm.block.OreBasaltBlock;
import com.reinhardt.hbm.block.OutgassingRadiatingBlock;
import com.reinhardt.hbm.block.ParticleAcceleratorBlock;
import com.reinhardt.hbm.block.PhosgeneGasBlock;
import com.reinhardt.hbm.block.PorousStoneBlock;
import com.reinhardt.hbm.block.PowerMachineBlock;
import com.reinhardt.hbm.block.PowerDetectorBlock;
import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.block.PoleTopBlock;
import com.reinhardt.hbm.block.PoleSatelliteReceiverBlock;
import com.reinhardt.hbm.block.PoweredSteamCondenserBlock;
import com.reinhardt.hbm.block.PileGraphiteBlock;
import com.reinhardt.hbm.block.RebarBlock;
import com.reinhardt.hbm.block.PinkDoubleSlabBlock;
import com.reinhardt.hbm.block.PressMachineBlock;
import com.reinhardt.hbm.block.PurexBlock;
import com.reinhardt.hbm.block.PwrBlock;
import com.reinhardt.hbm.block.RadonGasBlock;
import com.reinhardt.hbm.block.RadarScreenBlock;
import com.reinhardt.hbm.block.RadiatingBlock;
import com.reinhardt.hbm.block.RefineryBlock;
import com.reinhardt.hbm.block.ReactorControlBlock;
import com.reinhardt.hbm.block.RadiationAbsorberBlock;
import com.reinhardt.hbm.block.ResearchReactorBlock;
import com.reinhardt.hbm.block.ReedsBlock;
import com.reinhardt.hbm.block.RbmkDebrisBlock;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.block.RadioTorchBlock;
import com.reinhardt.hbm.block.RotaryFurnaceBlock;
import com.reinhardt.hbm.block.SolderingStationBlock;
import com.reinhardt.hbm.block.SirenBlock;
import com.reinhardt.hbm.block.SilexBlock;
import com.reinhardt.hbm.block.SmallBoilerBlock;
import com.reinhardt.hbm.block.SolarBoilerBlock;
import com.reinhardt.hbm.block.SolarMirrorBlock;
import com.reinhardt.hbm.block.SolidifierBlock;
import com.reinhardt.hbm.block.SteamCondenserBlock;
import com.reinhardt.hbm.block.SteamEngineBlock;
import com.reinhardt.hbm.block.SteamTurbineBlock;
import com.reinhardt.hbm.block.SteelGrateBlock;
import com.reinhardt.hbm.block.SteelPolesBlock;
import com.reinhardt.hbm.block.SteelScaffoldBlock;
import com.reinhardt.hbm.block.SteelWallBlock;
import com.reinhardt.hbm.block.SellafieldBlock;
import com.reinhardt.hbm.block.SellafieldOreBlock;
import com.reinhardt.hbm.block.LegacyGlassBlock;
import com.reinhardt.hbm.block.LegacyGlassPaneBlock;
import com.reinhardt.hbm.block.ReinforcedLampBlock;
import com.reinhardt.hbm.block.SpotlightBeamBlock;
import com.reinhardt.hbm.block.SpotlightBlock;
import com.reinhardt.hbm.block.FanBlock;
import com.reinhardt.hbm.block.FloodlightBeamBlock;
import com.reinhardt.hbm.block.FloodlightBlock;
import com.reinhardt.hbm.block.TritiumLampBlock;
import com.reinhardt.hbm.block.RedBrickBlock;
import com.reinhardt.hbm.block.SpeedyBlock;
import com.reinhardt.hbm.block.SpeedyStairsBlock;
import com.reinhardt.hbm.block.FragileBrickBlock;
import com.reinhardt.hbm.block.MechanistCircleBlock;
import com.reinhardt.hbm.block.SpikeBlock;
import com.reinhardt.hbm.block.TrappedBrickBlock;
import com.reinhardt.hbm.block.TimedExplosiveBlock;
import com.reinhardt.hbm.block.ThermalBombBlock;
import com.reinhardt.hbm.block.TeslaCoilBlock;
import com.reinhardt.hbm.block.WallChargeBlock;
import com.reinhardt.hbm.entity.TimedExplosiveEntity;
import com.reinhardt.hbm.block.StorageCrateBlock;
import com.reinhardt.hbm.block.SupplyCrateBlock;
import com.reinhardt.hbm.block.SafeBlock;
import com.reinhardt.hbm.block.StrandCasterBlock;
import com.reinhardt.hbm.block.StirlingGeneratorBlock;
import com.reinhardt.hbm.block.TrinititeBlock;
import com.reinhardt.hbm.block.SoyuzStructCoreBlock;
import com.reinhardt.hbm.block.ToxicBlock;
import com.reinhardt.hbm.block.TurretChekhovBlock;
import com.reinhardt.hbm.block.TurretJeremyBlock;
import com.reinhardt.hbm.block.VacuumDistillBlock;
import com.reinhardt.hbm.block.VolcanicLavaBlock;
import com.reinhardt.hbm.block.VolcanoCoreBlock;
import com.reinhardt.hbm.block.WatzBlock;
import com.reinhardt.hbm.block.WatzPumpBlock;
import com.reinhardt.hbm.block.WatzStructBlock;
import com.reinhardt.hbm.block.WasteDrumBlock;
import com.reinhardt.hbm.block.WasteEarthBlock;
import com.reinhardt.hbm.block.WasteLeavesBlock;
import com.reinhardt.hbm.block.WasteLogBlock;
import com.reinhardt.hbm.block.WasteMyceliumBlock;
import com.reinhardt.hbm.block.WeaponTableBlock;
import com.reinhardt.hbm.block.WoodBurnerBlock;
import com.reinhardt.hbm.block.ZirnoxDestroyedBlock;
import com.reinhardt.hbm.block.ZirnoxReactorBlock;
import com.reinhardt.hbm.block.DecoLootBlock;
import com.reinhardt.hbm.block.DecoModelBlock;
import com.reinhardt.hbm.block.DecoCrtBlock;
import com.reinhardt.hbm.block.FilingCabinetBlock;
import com.reinhardt.hbm.block.FrozenMaterialBlock;
import com.reinhardt.hbm.block.ImpactDirtBlock;
import com.reinhardt.hbm.block.TapeRecorderBlock;
import com.reinhardt.hbm.block.ToasterBlock;
import com.reinhardt.hbm.block.LogicRuntimeBlock;
import com.reinhardt.hbm.block.WandAirBlock;
import com.reinhardt.hbm.block.WandJigsawBlock;
import com.reinhardt.hbm.block.WandLogicBlock;
import com.reinhardt.hbm.block.WandLootBlock;
import com.reinhardt.hbm.block.WandStructureBlock;
import com.reinhardt.hbm.block.WandTandemBlock;
import com.reinhardt.hbm.block.SandMixBlock;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.blockentity.CompressorBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretType;
import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import com.reinhardt.hbm.block.MassStorageBlock;
import com.reinhardt.hbm.item.ConcreteColoredBlockItem;
import com.reinhardt.hbm.item.CaveSpikeBlockItem;
import com.reinhardt.hbm.item.ConveyorBlockItem;
import com.reinhardt.hbm.item.ConveyorPressBlockItem;
import com.reinhardt.hbm.item.BlastDoorBlockItem;
import com.reinhardt.hbm.item.CokeBlockItem;
import com.reinhardt.hbm.item.ElectrolyzerBlockItem;
import com.reinhardt.hbm.item.ExposureChamberBlockItem;
import com.reinhardt.hbm.item.ExcavatorBlockItem;
import com.reinhardt.hbm.item.FluidTankBlockItem;
import com.reinhardt.hbm.item.FluidBarrelBlockItem;
import com.reinhardt.hbm.item.FusionComponentBlockItem;
import com.reinhardt.hbm.item.LegacyVariantBlockItem;
import com.reinhardt.hbm.item.GasFlareBlockItem;
import com.reinhardt.hbm.item.GasTurbineBlockItem;
import com.reinhardt.hbm.item.GlyphBlockItem;
import com.reinhardt.hbm.item.TrapBlockItem;
import com.reinhardt.hbm.item.GeothermalHeatExchangerBlockItem;
import com.reinhardt.hbm.item.GeigerBlockItem;
import com.reinhardt.hbm.item.DemonLampBlockItem;
import com.reinhardt.hbm.item.DeuteriumTowerBlockItem;
import com.reinhardt.hbm.item.GroundwaterPumpBlockItem;
import com.reinhardt.hbm.item.HbmHeavyDoorBlockItem;
import com.reinhardt.hbm.item.HbmRailBlockItem;
import com.reinhardt.hbm.item.HexafluorideTankBlockItem;
import com.reinhardt.hbm.item.IndustrialTurbineBlockItem;
import com.reinhardt.hbm.item.LegacyOffsetBlockItem;
import com.reinhardt.hbm.item.LegacyMachineRendererBlockItem;
import com.reinhardt.hbm.item.LegacyTurretBlockItem;
import com.reinhardt.hbm.item.LargeFactoryBlockItem;
import com.reinhardt.hbm.item.MiningLaserBlockItem;
import com.reinhardt.hbm.item.MetalFenceBlockItem;
import com.reinhardt.hbm.item.MeteorOreBlockItem;
import com.reinhardt.hbm.item.MeteorBatteryBlockItem;
import com.reinhardt.hbm.item.OreBasaltBlockItem;
import com.reinhardt.hbm.item.ObjMachineBlockItem;
import com.reinhardt.hbm.item.CrashedBombBlockItem;
import com.reinhardt.hbm.item.DecoCrtBlockItem;
import com.reinhardt.hbm.item.FilingCabinetBlockItem;
import com.reinhardt.hbm.item.ObjMachineLegacyOffsetBlockItem;
import com.reinhardt.hbm.item.PoleBlockItem;
import com.reinhardt.hbm.item.ParticleAcceleratorBlockItem;
import com.reinhardt.hbm.item.PurexBlockItem;
import com.reinhardt.hbm.item.RbmkComponentBlockItem;
import com.reinhardt.hbm.item.RbmkFuelChannelBlockItem;
import com.reinhardt.hbm.item.RadarScreenBlockItem;
import com.reinhardt.hbm.item.SellafieldBlockItem;
import com.reinhardt.hbm.item.SoyuzLauncherBlockItem;
import com.reinhardt.hbm.item.SteamEngineBlockItem;
import com.reinhardt.hbm.item.StirlingGeneratorBlockItem;
import com.reinhardt.hbm.item.StorageCrateBlockItem;
import com.reinhardt.hbm.item.TurretJeremyBlockItem;
import com.reinhardt.hbm.item.WandStructureBlockItem;
import com.reinhardt.hbm.radiation.HbmHazardSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class HbmBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ReinhardtsHBM.MOD_ID);
    private static final Set<String> CORE_BLOCK_IDS = new LinkedHashSet<>();

    public static final DeferredBlock<Block> ORE_URANIUM = ore("ore_uranium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_URANIUM_SCORCHED = ore("ore_uranium_scorched", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_THORIUM = ore("ore_thorium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_TITANIUM = ore("ore_titanium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_SULFUR = ore("ore_sulfur", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_NITER = ore("ore_niter", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_TUNGSTEN = ore("ore_tungsten", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_ALUMINIUM = ore("ore_aluminium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_FLUORITE = ore("ore_fluorite", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_LEAD = ore("ore_lead", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_BERYLLIUM = ore("ore_beryllium", 5.0F, 15.0F);
    public static final DeferredBlock<Block> ORE_LIGNITE = ore("ore_lignite", 5.0F, 15.0F);
    public static final DeferredBlock<Block> ORE_ASBESTOS = ore("ore_asbestos", 5.0F, 15.0F);
    public static final DeferredBlock<Block> ORE_OIL_EMPTY = ore("ore_oil_empty", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_COAL_OIL = ore("ore_coal_oil", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_OIL = oilDeposit("ore_oil");
    public static final DeferredBlock<Block> ORE_OIL_SAND = oilSand("ore_oil_sand");
    public static final DeferredBlock<Block> ORE_BEDROCK_BLOCK = bedrockOre("ore_bedrock_block");
    /** Legacy 1.7.10 worldgen id. The item with this id is a separate resource item. */
    public static final DeferredBlock<Block> ORE_BEDROCK = registerBlockWithoutItem("ore_bedrock",
            () -> new BedrockOreBlock(rock().strength(-1.0F, 3_600_000.0F)));
    public static final DeferredBlock<Block> ORE_BEDROCK_COLTAN = bedrockOre("ore_bedrock_coltan");
    public static final DeferredBlock<Block> ORE_BEDROCK_OIL = bedrockOil("ore_bedrock_oil");
    public static final DeferredBlock<Block> BOOK_GUIDE = guideTerminal("book_guide");
    // 1.7.10 DecoBlock used by EntityBoxcar after its terminal impact.
    public static final DeferredBlock<Block> BOXCAR = registerBlock("boxcar",
            () -> new Block(metal().strength(10.0F, 10.0F)));
    public static final DeferredBlock<Block> HEV_BATTERY_BLOCK = hevBattery("hev_battery_block");
    public static final DeferredBlock<Block> ORE_RARE = ore("ore_rare", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_COBALT = ore("ore_cobalt", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_CINNABAR = ore("ore_cinnabar", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_CINNEBAR = registerBlock("ore_cinnebar",
            () -> new DropOreBlock(rock().strength(5.0F, 10.0F), "cinnabar"));
    /** 1.7.10 BlockDragonProof ore; this is not a legacy placeholder. */
    public static final DeferredBlock<Block> ORE_TIKITE = registerBlock("ore_tikite",
            () -> new Block(rock().strength(5.0F, 10.0F)));
    /** 1.7.10 sand-based tektite ore. */
    public static final DeferredBlock<Block> ORE_TEKTITE_OSMIRIDIUM = registerBlock("ore_tektite_osmiridium",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5F, 0.0F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> ORE_COPPER = ore("ore_copper", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_ALEXANDRITE = depthOre("ore_alexandrite", "gem_alexandrite", 1, 1);
    public static final DeferredBlock<Block> ORE_AUSTRALIUM = ore("ore_australium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_COLTAN = ore("ore_coltan", 15.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_SCHRABIDIUM = ore("ore_schrabidium", 15.0F, 600.0F);
    public static final DeferredBlock<Block> ORE_METEOR = meteorOre("ore_meteor");
    public static final DeferredBlock<Block> ORE_BASALT = oreBasalt("ore_basalt");

    public static final DeferredBlock<Block> ORE_GNEISS_IRON = ore("ore_gneiss_iron", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_GOLD = ore("ore_gneiss_gold", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_URANIUM = ore("ore_gneiss_uranium", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_URANIUM_SCORCHED = ore("ore_gneiss_uranium_scorched", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_COPPER = ore("ore_gneiss_copper", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_ASBESTOS = ore("ore_gneiss_asbestos", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_LITHIUM = ore("ore_gneiss_lithium", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_SCHRABIDIUM = ore("ore_gneiss_schrabidium", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_RARE = ore("ore_gneiss_rare", 1.5F, 10.0F);
    public static final DeferredBlock<Block> ORE_GNEISS_GAS = ore("ore_gneiss_gas", 1.5F, 10.0F);

    public static final DeferredBlock<Block> ORE_NETHER_COAL = oreWithLight("ore_nether_coal", 0.4F, 10.0F, 10);
    public static final DeferredBlock<Block> ORE_NETHER_SMOLDERING = oreWithLight("ore_nether_smoldering", 0.4F, 10.0F, 15);
    public static final DeferredBlock<Block> ORE_NETHER_URANIUM = ore("ore_nether_uranium", 0.4F, 10.0F);
    public static final DeferredBlock<Block> ORE_NETHER_URANIUM_SCORCHED = ore("ore_nether_uranium_scorched", 0.4F, 10.0F);
    public static final DeferredBlock<Block> ORE_NETHER_PLUTONIUM = ore("ore_nether_plutonium", 0.4F, 10.0F);
    public static final DeferredBlock<Block> ORE_NETHER_TUNGSTEN = ore("ore_nether_tungsten", 0.4F, 10.0F);
    public static final DeferredBlock<Block> ORE_NETHER_SULFUR = ore("ore_nether_sulfur", 0.4F, 10.0F);
    public static final DeferredBlock<Block> ORE_NETHER_FIRE = ore("ore_nether_fire", 0.4F, 10.0F);
    public static final DeferredBlock<Block> ORE_NETHER_COBALT = ore("ore_nether_cobalt", 0.4F, 10.0F);
    public static final DeferredBlock<Block> ORE_NETHER_SCHRABIDIUM = ore("ore_nether_schrabidium", 15.0F, 600.0F);
    public static final DeferredBlock<Block> ORE_DEPTH_NETHER_NEODYMIUM = ore("ore_depth_nether_neodymium", 100.0F, 1_000.0F);
    public static final DeferredBlock<Block> ORE_DEPTH_NETHER_NITAN = ore("ore_depth_nether_nitan", 100.0F, 1_000.0F);
    public static final DeferredBlock<Block> ORE_DEPTH_CINNEBAR = depthOre("ore_depth_cinnebar", "cinnabar", 2, 3);
    public static final DeferredBlock<Block> ORE_DEPTH_BORAX = depthOre("ore_depth_borax", "powder_borax", 1, 1);
    public static final DeferredBlock<Block> ORE_DEPTH_ZIRCONIUM = depthOre("ore_depth_zirconium", "nugget_zirconium", 2, 2);

    public static final DeferredBlock<Block> ORE_DEEPSLATE_URANIUM = ore("ore_deepslate_uranium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_URANIUM_SCORCHED = ore("ore_deepslate_uranium_scorched", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_THORIUM = ore("ore_deepslate_thorium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_TITANIUM = ore("ore_deepslate_titanium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_SULFUR = ore("ore_deepslate_sulfur", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_NITER = ore("ore_deepslate_niter", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_TUNGSTEN = ore("ore_deepslate_tungsten", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_ALUMINIUM = ore("ore_deepslate_aluminium", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_FLUORITE = ore("ore_deepslate_fluorite", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_LEAD = ore("ore_deepslate_lead", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_BERYLLIUM = ore("ore_deepslate_beryllium", 5.0F, 15.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_LIGNITE = ore("ore_deepslate_lignite", 5.0F, 15.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_ASBESTOS = ore("ore_deepslate_asbestos", 5.0F, 15.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_OIL_EMPTY = ore("ore_deepslate_oil_empty", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_OIL = deepslateOilDeposit("ore_deepslate_oil");
    public static final DeferredBlock<Block> ORE_DEEPSLATE_RARE = ore("ore_deepslate_rare", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_COBALT = ore("ore_deepslate_cobalt", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_CINNABAR = ore("ore_deepslate_cinnabar", 5.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_COLTAN = ore("ore_deepslate_coltan", 15.0F, 10.0F);
    public static final DeferredBlock<Block> ORE_DEEPSLATE_SCHRABIDIUM = ore("ore_deepslate_schrabidium", 15.0F, 600.0F);

    public static final DeferredBlock<Block> CLUSTER_IRON = ore("cluster_iron", 5.0F, 35.0F);
    public static final DeferredBlock<Block> CLUSTER_TITANIUM = ore("cluster_titanium", 5.0F, 35.0F);
    public static final DeferredBlock<Block> CLUSTER_ALUMINIUM = ore("cluster_aluminium", 5.0F, 35.0F);
    public static final DeferredBlock<Block> CLUSTER_COPPER = ore("cluster_copper", 5.0F, 35.0F);
    public static final DeferredBlock<Block> CLUSTER_DEPTH_IRON = ore("cluster_depth_iron", 100.0F, 1_000.0F);
    public static final DeferredBlock<Block> CLUSTER_DEPTH_TITANIUM = ore("cluster_depth_titanium", 100.0F, 1_000.0F);
    public static final DeferredBlock<Block> CLUSTER_DEPTH_TUNGSTEN = ore("cluster_depth_tungsten", 100.0F, 1_000.0F);

    public static final DeferredBlock<Block> DIRT_DEAD = fallingSoil("dirt_dead", MapColor.DIRT, SoundType.GRAVEL);
    public static final DeferredBlock<Block> DIRT_OILY = fallingSoil("dirt_oily", MapColor.DIRT, SoundType.GRAVEL);
    public static final DeferredBlock<Block> SAND_DIRTY = fallingSoil("sand_dirty", MapColor.SAND, SoundType.SAND);
    public static final DeferredBlock<Block> SAND_DIRTY_RED = fallingSoil("sand_dirty_red", MapColor.COLOR_ORANGE, SoundType.SAND);
    public static final DeferredBlock<Block> SAND_QUARTZ = fallingSoil("sand_quartz", MapColor.SAND, SoundType.SAND);
    public static final DeferredBlock<Block> SAND_LEAD = fallingSoil("sand_lead", MapColor.SAND, SoundType.SAND);
    public static final DeferredBlock<Block> SAND_URANIUM = fallingSoil("sand_uranium", MapColor.SAND, SoundType.SAND);
    public static final DeferredBlock<Block> SAND_POLONIUM = fallingSoil("sand_polonium", MapColor.SAND, SoundType.SAND);
    public static final DeferredBlock<Block> STONE_CRACKED = registerBlock("stone_cracked",
            () -> new Block(rock().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> STONE_POROUS = registerBlock("stone_porous",
            () -> new PorousStoneBlock(rock().strength(1.5F, 30.0F)));
    public static final DeferredBlock<Block> STONE_DEPTH = registerBlock("stone_depth",
            () -> new DepthRockBlock(rock().strength(100.0F, 1_000.0F)));
    public static final DeferredBlock<Block> STONE_DEPTH_NETHER = registerBlock("stone_depth_nether",
            () -> new DepthRockBlock(rock().strength(100.0F, 1_000.0F)));
    public static final DeferredBlock<Block> STONE_GNEISS = registerBlock("stone_gneiss",
            () -> new Block(rock().strength(1.5F, 10.0F)));
    public static final DeferredBlock<Block> STONE_RESOURCE = registerVariantBlock("stone_resource",
            () -> new ResourceStoneBlock(rock().strength(5.0F, 10.0F)), LegacyVariantBlock.VARIANT,
            "block.reinhardtshbm.stone_resource", "sulfur", "asbestos", "hematite", "malachite", "limestone", "bauxite");
    public static final DeferredBlock<Block> STONE_BIOME = registerVariantBlock("stone_biome",
            () -> new BiomeStoneBlock(rock().strength(5.0F, 10.0F)), BiomeStoneBlock.VARIANT,
            "block.reinhardtshbm.stone_biome", "desert", "woodland");
    public static final DeferredBlock<Block> DEPTH_BRICK = depthBlock("depth_brick");
    public static final DeferredBlock<Block> DEPTH_DNT = registerBlock("depth_dnt",
            () -> new DepthRockBlock(rock().strength(100.0F, 60_000.0F)));
    public static final DeferredBlock<Block> DEPTH_TILES = depthBlock("depth_tiles");
    public static final DeferredBlock<Block> DEPTH_NETHER_BRICK = depthBlock("depth_nether_brick");
    public static final DeferredBlock<Block> DEPTH_NETHER_TILES = depthBlock("depth_nether_tiles");
    public static final DeferredBlock<Block> GNEISS_BRICK = registerBlock("gneiss_brick",
            () -> new Block(rock().strength(1.5F, 10.0F)));
    public static final DeferredBlock<Block> GNEISS_TILE = registerBlock("gneiss_tile",
            () -> new Block(rock().strength(1.5F, 10.0F)));
    public static final DeferredBlock<Block> GNEISS_CHISELED = registerBlock("gneiss_chiseled",
            () -> new RotatedPillarBlock(rock().strength(1.5F, 10.0F)));
    public static final DeferredBlock<Block> OIL_SPILL = registerBlock("oil_spill",
            () -> new OilSpillBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .replaceable()
                    .strength(0.1F, 0.0F)
                    .sound(SoundType.SNOW)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> PLANT_DEAD = registerBlock("plant_dead",
            () -> new DeadPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.0F, 0.0F)
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> PLANT_FLOWER = registerBlock("plant_flower",
            () -> new MustardWillowFlowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.0F, 0.0F)
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> PLANT_TALL = registerBlock("plant_tall",
            () -> new MustardWillowTallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.0F, 0.0F)
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> PLANT_REEDS = registerBlock("plant_reeds",
            () -> new ReedsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.0F, 0.0F)
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> VINE_PHOSPHOR = registerBlock("vine_phosphor",
            () -> new HangingPhosphorVineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.5F)
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> STALAGMITE = caveSpike("stalagmite", false);
    public static final DeferredBlock<Block> STALACTITE = caveSpike("stalactite", true);
    public static final DeferredBlock<Block> BRICK_JUNGLE = registerBlock("brick_jungle",
            () -> new Block(rock().strength(15.0F, 360.0F)));
    public static final DeferredBlock<Block> BRICK_JUNGLE_CRACKED = registerBlock("brick_jungle_cracked",
            () -> new Block(rock().strength(15.0F, 360.0F)));
    public static final DeferredBlock<Block> BRICK_JUNGLE_FRAGILE = registerBlock("brick_jungle_fragile",
            () -> new FragileBrickBlock(rock().strength(15.0F, 360.0F)));
    public static final DeferredBlock<Block> BRICK_JUNGLE_LAVA = registerBlock("brick_jungle_lava",
            () -> new Block(rock().strength(15.0F, 360.0F).lightLevel(state -> 5)));
    public static final DeferredBlock<Block> BRICK_JUNGLE_OOZE = registerBlock("brick_jungle_ooze",
            () -> new EnargiteBrickBlock(rock().strength(15.0F, 360.0F).lightLevel(state -> 5),
                    EnargiteBrickBlock.Kind.RADIOACTIVE));
    public static final DeferredBlock<Block> BRICK_JUNGLE_MYSTIC = registerBlock("brick_jungle_mystic",
            () -> new EnargiteBrickBlock(rock().strength(15.0F, 360.0F).lightLevel(state -> 5),
                    EnargiteBrickBlock.Kind.MYSTIC));
    public static final DeferredBlock<Block> BRICK_JUNGLE_TRAP = registerBlockWithoutItem("brick_jungle_trap",
            () -> new TrappedBrickBlock(rock().strength(15.0F, 360.0F)));
    public static final DeferredItem<Item> BRICK_JUNGLE_TRAP_ITEM = HbmItems.ITEMS.register("brick_jungle_trap",
            () -> new TrapBlockItem(BRICK_JUNGLE_TRAP.get(), new Item.Properties()));
    public static final DeferredBlock<Block> BRICK_JUNGLE_GLYPH = glyphBlock("brick_jungle_glyph");
    public static final DeferredBlock<Block> GLYPHID_BASE = registerVariantBlock("glyphid_base",
            () -> new GlyphidBaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F, 5.0F)
                    .sound(SoundType.WOOL)
                    .noLootTable()),
            LegacyVariantBlock.VARIANT,
            "block.reinhardtshbm.glyphid_base", "standard", "infested", "radioactive");
    public static final DeferredBlock<Block> GLYPHID_SPAWNER = registerVariantBlock("glyphid_spawner",
            () -> new GlyphidSpawnerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F, 5.0F)
                    .sound(SoundType.WOOL)
                    .noLootTable()),
            LegacyVariantBlock.VARIANT,
            "block.reinhardtshbm.glyphid_spawner", "standard", "infested", "radioactive");
    public static final DeferredBlock<Block> BRICK_JUNGLE_CIRCLE = registerBlock("brick_jungle_circle",
            () -> new MechanistCircleBlock(rock().strength(15.0F, 360.0F)));
    public static final DeferredBlock<Block> CRYSTAL_VIRUS = registerBlock("crystal_virus",
            () -> new CrystalVirusBlock(metal().strength(15.0F, Float.POSITIVE_INFINITY)));
    public static final DeferredBlock<Block> CRYSTAL_HARDENED = registerBlock("crystal_hardened",
            () -> new Block(metal().strength(15.0F, Float.POSITIVE_INFINITY)));
    public static final DeferredBlock<Block> CRYSTAL_PULSAR = registerBlock("crystal_pulsar",
            () -> new CrystalPulsarBlock(metal().strength(15.0F, Float.POSITIVE_INFINITY)));
    public static final DeferredBlock<Block> SPIKES = registerBlock("spikes",
            () -> new SpikeBlock(metal().strength(2.5F, 5.0F)));

    public static final DeferredBlock<Block> BLOCK_METEOR = meteorBlock("block_meteor");
    public static final DeferredBlock<Block> BLOCK_METEOR_BROKEN = meteorBlock("block_meteor_broken");
    public static final DeferredBlock<Block> BLOCK_METEOR_COBBLE = meteorBlock("block_meteor_cobble");
    public static final DeferredBlock<Block> BLOCK_METEOR_MOLTEN = registerBlock("block_meteor_molten",
            () -> new MeteorMoltenBlock(rock()
                    .strength(15.0F, 360.0F)
                    .lightLevel(state -> 12)
                    .randomTicks()));
    public static final DeferredBlock<Block> BLOCK_METEOR_TREASURE = meteorBlock("block_meteor_treasure");
    public static final DeferredBlock<Block> METEOR_BRICK = meteorBlock("meteor_brick");
    public static final DeferredBlock<Block> METEOR_BRICK_MOSSY = meteorBlock("meteor_brick_mossy");
    public static final DeferredBlock<Block> METEOR_BRICK_CRACKED = meteorBlock("meteor_brick_cracked");
    public static final DeferredBlock<Block> METEOR_BRICK_CHISELED = meteorBlock("meteor_brick_chiseled");
    public static final DeferredBlock<Block> METEOR_PILLAR = registerBlock("meteor_pillar",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(rock().strength(15.0F, 360.0F)));
    public static final DeferredBlock<Block> METEOR_POLISHED = meteorBlock("meteor_polished");
    public static final DeferredBlock<Block> METEOR_SPAWNER = meteorBlock("meteor_spawner");
    // 1.7.10 BlockPillar: a coil above this block is kept fully charged.
    public static final DeferredBlock<Block> METEOR_BATTERY = meteorBattery("meteor_battery");
    public static final DeferredBlock<Block> TESLA = registerObjBlock("tesla",
            () -> new TeslaCoilBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> MUD_BLOCK = registerBlock("mud_block",
            () -> new LegacyHazardLiquidBlock(() -> HbmFluids.source("mud_fluid"),
                    legacyFluidProperties(MapColor.TERRACOTTA_BROWN, 5), LegacyHazardLiquidBlock.Kind.MUD));
    public static final DeferredBlock<Block> ACID_BLOCK = registerBlock("acid_block",
            () -> new LegacyHazardLiquidBlock(() -> HbmFluids.source("acid_fluid"),
                    legacyFluidProperties(MapColor.COLOR_LIGHT_GREEN, 5), LegacyHazardLiquidBlock.Kind.ACID));
    public static final DeferredBlock<Block> TOXIC_BLOCK = registerBlock("toxic_block",
            () -> new LegacyHazardLiquidBlock(() -> HbmFluids.source("toxic_fluid"),
                    legacyFluidProperties(MapColor.COLOR_GREEN, 15), LegacyHazardLiquidBlock.Kind.TOXIC));
    public static final DeferredBlock<Block> SCHRABIDIC_BLOCK = registerBlock("schrabidic_block",
            () -> new LegacyHazardLiquidBlock(() -> HbmFluids.source("schrabidic"),
                    legacyFluidProperties(MapColor.COLOR_CYAN, 0), LegacyHazardLiquidBlock.Kind.SCHRABIDIC));
    public static final DeferredBlock<Block> RAD_LAVA_BLOCK = registerBlock("rad_lava_block",
            () -> new LegacyHazardLiquidBlock(() -> HbmFluids.source("rad_lava_fluid"),
                    legacyFluidProperties(MapColor.COLOR_LIGHT_GREEN, 15), LegacyHazardLiquidBlock.Kind.RAD_LAVA));
    public static final DeferredBlock<Block> SULFURIC_ACID_BLOCK = registerBlock("sulfuric_acid_block",
            () -> new LegacyHazardLiquidBlock(() -> HbmFluids.source("sulfuric_acid"),
                    legacyFluidProperties(MapColor.COLOR_YELLOW, 0), LegacyHazardLiquidBlock.Kind.SULFURIC_ACID));
    public static final DeferredBlock<Block> BLOCK_CORIUM = registerBlock("block_corium",
            () -> new RadiatingBlock(rock()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(100.0F, 6000.0F)
                    .lightLevel(state -> 6)
                    .randomTicks(), 150.0D));
    public static final DeferredBlock<Block> BLOCK_CORIUM_COBBLE = registerBlock("block_corium_cobble",
            () -> new OutgassingRadiatingBlock(rock()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(100.0F, 6000.0F)
                    .lightLevel(state -> 3)
                    .randomTicks(), 150.0D));
    public static final DeferredBlock<Block> CORIUM_BLOCK = registerBlockWithoutItem("corium_block",
            () -> new CoriumBlock(HbmFluids::coriumSource,
                    BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(100.0F, 500.0F)
                    .lightLevel(state -> 12)
                    .replaceable()
                    .noCollission()
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> VOLCANIC_LAVA_BLOCK = registerBlock("volcanic_lava_block",
            () -> new VolcanicLavaBlock(HbmFluids::volcanicLavaSource,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.FIRE)
                            .strength(100.0F, 500.0F)
                            .lightLevel(state -> 15)
                            .replaceable()
                            .noCollission()
                            .noLootTable()
                            .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> GAS_MELTDOWN = registerBlock("gas_meltdown",
            () -> new MeltdownGasBlock(gasProperties(MapColor.COLOR_GREEN)));
    public static final DeferredBlock<Block> GAS_RADON = registerBlock("gas_radon",
            () -> new RadonGasBlock(gasProperties(MapColor.COLOR_GREEN), false));
    public static final DeferredBlock<Block> GAS_RADON_DENSE = registerBlock("gas_radon_dense",
            () -> new DenseRadonGasBlock(gasProperties(MapColor.COLOR_GREEN)));
    public static final DeferredBlock<Block> GAS_RADON_TOMB = registerBlock("gas_radon_tomb",
            () -> new RadonGasBlock(gasProperties(MapColor.COLOR_GREEN), true));
    public static final DeferredBlock<Block> ANCIENT_SCRAP = registerBlock("ancient_scrap",
            () -> new AncientScrapBlock(metal()
                    .requiresCorrectToolForDrops()
                    .strength(100.0F, 6000.0F)
                    .randomTicks(), 150.0D));
    public static final DeferredBlock<Block> GAS_MONOXIDE = registerBlock("gas_monoxide",
            () -> new MonoxideGasBlock(gasProperties(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<Block> GAS_ASBESTOS = registerBlock("gas_asbestos",
            () -> new AsbestosGasBlock(gasProperties(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<Block> GAS_COAL = registerBlock("gas_coal",
            () -> new CoalDustGasBlock(gasProperties(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<Block> GAS_FLAMMABLE = registerBlock("gas_flammable",
            () -> new FlammableGasBlock(gasProperties(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<Block> GAS_EXPLOSIVE = registerBlock("gas_explosive",
            () -> new ExplosiveGasBlock(gasProperties(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<Block> CHLORINE_GAS = registerBlock("chlorine_gas",
            () -> new ChlorineGasBlock(gasProperties(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<Block> GEYSIR_CHLORINE = registerBlock("geysir_chlorine",
            () -> new LegacyEmitterBlock(rock()
                    .strength(5.0F)
                    .sound(SoundType.STONE)
                    .noLootTable(), LegacyEmitterBlock.Kind.GEYSER_CHLORINE));
    public static final DeferredBlock<Block> GEYSIR_NETHER = registerBlock("geysir_nether",
            () -> new LegacyEmitterBlock(rock()
                    .strength(2.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 15)
                    .noLootTable(), LegacyEmitterBlock.Kind.GEYSER_NETHER));
    public static final DeferredBlock<Block> VENT_CHLORINE = registerBlock("vent_chlorine",
            () -> new LegacyEmitterBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noLootTable(), LegacyEmitterBlock.Kind.VENT_CHLORINE));
    public static final DeferredBlock<Block> VENT_CLOUD = registerBlock("vent_cloud",
            () -> new LegacyEmitterBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noLootTable(), LegacyEmitterBlock.Kind.VENT_CLOUD));
    public static final DeferredBlock<Block> VENT_PINK_CLOUD = registerBlock("vent_pink_cloud",
            () -> new LegacyEmitterBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noLootTable(), LegacyEmitterBlock.Kind.VENT_PINK_CLOUD));
    public static final DeferredBlock<Block> VENT_CHLORINE_SEAL = registerBlock("vent_chlorine_seal",
            () -> new LegacyEmitterBlock(metal()
                    .strength(5.0F, 10.0F), LegacyEmitterBlock.Kind.VENT_CHLORINE_SEAL));
    public static final DeferredBlock<Block> PHOSGENE_GAS = registerBlockWithoutItem("phosgene_gas",
            () -> new PhosgeneGasBlock(gasProperties(MapColor.TERRACOTTA_LIGHT_GRAY)));
    public static final DeferredBlock<Block> MUSTARD_GAS = registerBlockWithoutItem("mustard_gas",
            () -> new MustardGasBlock(gasProperties(MapColor.TERRACOTTA_YELLOW)));
    public static final DeferredBlock<Block> FALLOUT = registerBlock("fallout",
            () -> new FalloutBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .replaceable()
                    .strength(0.1F, 0.0F)
                    .sound(SoundType.SAND)
                    .noOcclusion()));
    public static final DeferredBlock<Block> BLOCK_FALLOUT = registerBlock("block_fallout",
            () -> new FalloutFallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.2F, 0.0F)
                    .sound(SoundType.GRAVEL)));
    public static final DeferredBlock<Block> WASTE_EARTH = registerBlock("waste_earth",
            () -> new WasteEarthBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.5F, 1.0F)
                    .sound(SoundType.GRASS)
                    .randomTicks()));
    public static final DeferredBlock<Block> WASTE_MYCELIUM = registerBlock("waste_mycelium",
            () -> new WasteMyceliumBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.6F, 1.0F)
                    .sound(SoundType.GRASS)
                    .lightLevel(state -> 15)
                    .randomTicks()));
    public static final DeferredBlock<Block> WASTE_LOG = registerBlock("waste_log",
            () -> new WasteLogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(5.0F, 2.5F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> FROZEN_GRASS = registerBlock("frozen_grass",
            () -> new WasteEarthBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5F, 2.5F)
                    .sound(SoundType.GLASS), WasteEarthBlock.Kind.FROZEN));
    public static final DeferredBlock<Block> FROZEN_LOG = registerBlock("frozen_log",
            () -> new WasteLogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F, 2.5F)
                    .sound(SoundType.GLASS), true));
    public static final DeferredBlock<Block> BURNING_EARTH = registerBlock("burning_earth",
            () -> new WasteEarthBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.6F, 1.0F)
                    .sound(SoundType.GRASS)
                    .randomTicks(), WasteEarthBlock.Kind.BURNING));
    public static final DeferredBlock<Block> WASTE_TRINITITE = registerBlock("waste_trinitite",
            () -> new TrinititeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5F, 2.5F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> WASTE_TRINITITE_RED = registerBlock("waste_trinitite_red",
            () -> new TrinititeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F, 2.5F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> SELLAFIELD_SLAKED = registerBlock("sellafield_slaked",
            () -> new Block(rock().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> SELLAFIELD_BEDROCK = registerBlock("sellafield_bedrock",
            () -> new Block(rock().strength(-1.0F, 6_000_000.0F)
                    .isValidSpawn((state, level, pos, entityType) -> false)));
    public static final DeferredBlock<Block> SELLAFIELD = sellafield("sellafield");
    public static final DeferredBlock<Block> ORE_SELLAFIELD_DIAMOND = sellafieldOre("ore_sellafield_diamond", SellafieldOreBlock.Drop.DIAMOND);
    public static final DeferredBlock<Block> ORE_SELLAFIELD_EMERALD = sellafieldOre("ore_sellafield_emerald", SellafieldOreBlock.Drop.EMERALD);
    public static final DeferredBlock<Block> ORE_SELLAFIELD_URANIUM_SCORCHED = sellafieldOre("ore_sellafield_uranium_scorched", SellafieldOreBlock.Drop.URANIUM_SCORCHED);
    public static final DeferredBlock<Block> ORE_SELLAFIELD_SCHRABIDIUM = sellafieldOre("ore_sellafield_schrabidium", SellafieldOreBlock.Drop.SCHRABIDIUM);
    public static final DeferredBlock<Block> ORE_SELLAFIELD_RADGEM = sellafieldOre("ore_sellafield_radgem", SellafieldOreBlock.Drop.RAD_GEM);
    public static final DeferredBlock<Block> WASTE_LEAVES = registerBlock("waste_leaves",
            () -> new WasteLeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.1F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredBlock<Block> LEAVES_LAYER = registerBlock("leaves_layer",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .strength(0.1F)
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> WASTE_PLANKS = registerBlock("waste_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F, 2.5F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> MUSH = registerBlock("mush",
            () -> new HbmMushroomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .lightLevel(state -> 8)
                    .randomTicks()));
    public static final DeferredBlock<Block> MUSH_BLOCK = registerBlock("mush_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .lightLevel(state -> 15)));
    public static final DeferredBlock<Block> MUSH_BLOCK_STEM = registerBlock("mush_block_stem",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .lightLevel(state -> 15)));
    public static final DeferredBlock<Block> BLOCK_FOAM = registerBlock("block_foam",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.2F, 0.2F)
                    .sound(SoundType.SNOW)));
    public static final DeferredBlock<Block> FOAM_LAYER = registerBlock("foam_layer",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .replaceable()
                    .strength(0.1F, 0.1F)
                    .sound(SoundType.SNOW)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> SAND_BORON = registerBlock("sand_boron",
            () -> new HbmFallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> SAND_BORON_LAYER = registerBlock("sand_boron_layer",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .replaceable()
                    .strength(0.1F)
                    .sound(SoundType.SAND)
                    .noCollission()
                    .noOcclusion()));
    public static final DeferredBlock<Block> ASH_DIGAMMA = registerBlock("ash_digamma",
            () -> new HbmFallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F, 150.0F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> FIRE_DIGAMMA = registerBlock("fire_digamma",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .replaceable()
                    .strength(0.0F, 150.0F)
                    .lightLevel(state -> 15)
                    .sound(SoundType.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()));
    public static final DeferredBlock<Block> BALEFIRE = registerBlockWithoutItem("balefire",
            () -> new BalefireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .replaceable()
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state -> 15)
                    .noLootTable()));
    public static final DeferredBlock<Block> DIGAMMA_MATTER = registerBlock("digamma_matter",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 18_000_000.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 15)));
    public static final DeferredBlock<Block> PRIBRIS = rbmkDebris("pribris", RbmkDebrisBlock.Kind.NORMAL);
    public static final DeferredBlock<Block> PRIBRIS_BURNING = rbmkDebris("pribris_burning", RbmkDebrisBlock.Kind.BURNING);
    public static final DeferredBlock<Block> PRIBRIS_RADIATING = rbmkDebris("pribris_radiating", RbmkDebrisBlock.Kind.RADIATING);
    public static final DeferredBlock<Block> PRIBRIS_DIGAMMA = rbmkDebris("pribris_digamma", RbmkDebrisBlock.Kind.DIGAMMA);
    public static final DeferredBlock<Block> CHIMNEY_BRICK = registerObjBlock("chimney_brick",
            () -> new ChimneyBlock(metal()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(5.0F, 100.0F)
                    .noOcclusion(), false));
    public static final DeferredBlock<Block> CHIMNEY_INDUSTRIAL = registerObjBlock("chimney_industrial",
            () -> new ChimneyBlock(metal()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 100.0F)
                    .noOcclusion(), true));
    public static final DeferredBlock<Block> GLASS_QUARTZ = registerBlock("glass_quartz",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.3F, 1.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredBlock<Block> DECON = registerBlock("decon",
            () -> new DeconBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> RAD_ABSORBER = registerVariantBlock("rad_absorber",
            () -> new RadiationAbsorberBlock(metal().strength(5.0F, 10.0F)),
            LegacyVariantBlock.VARIANT,
            "block.reinhardtshbm.rad_absorber",
            "base", "red", "green", "pink");
    public static final DeferredBlock<Block> GEIGER = registerBlockWithoutItem("geiger",
            () -> new GeigerBlock(metal().strength(15.0F, 0.25F).noOcclusion()));
    public static final DeferredItem<Item> GEIGER_ITEM = HbmItems.ITEMS.register("geiger",
            () -> new GeigerBlockItem(GEIGER.get(), new Item.Properties()));
    public static final DeferredBlock<Block> MACHINE_ASHPIT = registerObjBlock("machine_ashpit",
            () -> new AshpitBlock(metal()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 10.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> DECO_TOASTER = registerBlockWithoutItem("deco_toaster",
            () -> new ToasterBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredItem<Item> DECO_TOASTER_ITEM = HbmItems.ITEMS.register("deco_toaster",
            () -> new ToasterBlockItem(DECO_TOASTER.get(), new Item.Properties()));
    public static final DeferredBlock<Block> DECO_COMPUTER = registerBlockWithoutItem("deco_computer",
            () -> new DecoModelBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredItem<Item> DECO_COMPUTER_ITEM = HbmItems.ITEMS.register("deco_computer",
            () -> new ObjMachineBlockItem(DECO_COMPUTER.get(), new Item.Properties()));
    public static final DeferredBlock<Block> DECO_CRT = registerBlockWithoutItem("deco_crt",
            () -> new DecoCrtBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredItem<Item> DECO_CRT_ITEM = HbmItems.ITEMS.register("deco_crt",
            () -> new DecoCrtBlockItem(DECO_CRT.get(), new Item.Properties()));
    public static final DeferredBlock<Block> FILING_CABINET = registerBlockWithoutItem("filing_cabinet",
            () -> new FilingCabinetBlock(metal().strength(10.0F, 15.0F).noOcclusion()));
    public static final DeferredItem<Item> FILING_CABINET_ITEM = HbmItems.ITEMS.register("filing_cabinet",
            () -> new FilingCabinetBlockItem(FILING_CABINET.get(), new Item.Properties()));
    public static final DeferredBlock<Block> TAPE_RECORDER = registerBlockWithoutItem("tape_recorder",
            () -> new TapeRecorderBlock(metal().strength(5.0F, 15.0F).noOcclusion()));
    public static final DeferredItem<Item> TAPE_RECORDER_ITEM = HbmItems.ITEMS.register("tape_recorder",
            () -> new ObjMachineBlockItem(TAPE_RECORDER.get(), new Item.Properties()));
    public static final DeferredBlock<Block> PEDESTAL = registerBlock("pedestal",
            () -> new LegacyDisplayStandBlock(rock().strength(2.0F, 10.0F),
                    LegacyDisplayStandBlock.Kind.PEDESTAL));
    public static final DeferredBlock<Block> SKELETON_HOLDER = registerObjBlock("skeleton_holder",
            () -> new LegacyDisplayStandBlock(rock().strength(2.0F, 10.0F).noOcclusion(),
                    LegacyDisplayStandBlock.Kind.SKELETON_HOLDER));
    public static final DeferredBlock<Block> SEAL_CONTROLLER = registerBlock("seal_controller",
            () -> new SealControllerBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> SEAL_FRAME = registerBlock("seal_frame",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> SEAL_HATCH = registerBlock("seal_hatch",
            () -> new SealHatchBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> STONE_KEYHOLE = registerBlock("stone_keyhole",
            () -> new StoneKeyholeBlock(rock().strength(-1.0F, 6_000_000.0F), StoneKeyholeBlock.Kind.STONE));
    public static final DeferredBlock<Block> STONE_KEYHOLE_META = registerBlock("stone_keyhole_meta",
            () -> new StoneKeyholeBlock(rock().strength(-1.0F, 6_000_000.0F), StoneKeyholeBlock.Kind.RED_BRICK));
    public static final DeferredBlock<Block> BLOCK_ALUMINIUM = materialBlock("block_aluminium");
    public static final DeferredBlock<Block> BLOCK_GRAPHITE = registerBlock("block_graphite",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.GRAPHITE));
    public static final DeferredBlock<Block> BLOCK_GRAPHITE_DRILLED = registerBlockWithoutItem("block_graphite_drilled",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.DRILLED));
    public static final DeferredBlock<Block> BLOCK_GRAPHITE_FUEL = registerBlockWithoutItem("block_graphite_fuel",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.FUEL));
    public static final DeferredBlock<Block> BLOCK_GRAPHITE_PLUTONIUM = registerBlockWithoutItem("block_graphite_plutonium",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.PLUTONIUM));
    public static final DeferredBlock<Block> BLOCK_GRAPHITE_ROD = registerBlockWithoutItem("block_graphite_rod",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.CONTROL));
    public static final DeferredBlock<Block> BLOCK_GRAPHITE_SOURCE = registerBlockWithoutItem("block_graphite_source",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.SOURCE));
    public static final DeferredBlock<Block> BLOCK_GRAPHITE_LITHIUM = registerBlockWithoutItem("block_graphite_lithium",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.LITHIUM));
    public static final DeferredBlock<Block> BLOCK_GRAPHITE_TRITIUM = registerBlockWithoutItem("block_graphite_tritium",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.TRITIUM));
    public static final DeferredBlock<Block> BLOCK_GRAPHITE_DETECTOR = registerBlockWithoutItem("block_graphite_detector",
            () -> new PileGraphiteBlock(metal().strength(5.0F, 10.0F), PileGraphiteBlock.Kind.DETECTOR));
    public static final DeferredBlock<Block> BLOCK_BORON = materialBlock("block_boron");
    public static final DeferredBlock<Block> BLOCK_SCHRARANIUM = materialBlock("block_schraranium");
    public static final DeferredBlock<Block> BLOCK_LANTHANIUM = materialBlock("block_lanthanium");
    public static final DeferredBlock<Block> BLOCK_RA226 = materialBlock("block_ra226");
    public static final DeferredBlock<Block> BLOCK_ACTINIUM = materialBlock("block_actinium");
    public static final DeferredBlock<Block> BLOCK_SCHRABIDATE = materialBlock("block_schrabidate");
    public static final DeferredBlock<Block> BLOCK_COLTAN = materialBlock("block_coltan");
    public static final DeferredBlock<Block> BLOCK_SMORE = registerBlock("block_smore",
            () -> hazardAwareBlock("block_smore", rock().strength(15.0F, 450.0F)));
    public static final DeferredBlock<Block> BLOCK_SLAG = registerBlock("block_slag",
            () -> new Block(rock().strength(2.0F, 10.0F)));
    public static final DeferredBlock<Block> BLOCK_COKE = cokeBlock("block_coke");
    public static final DeferredBlock<Block> BLOCK_CAP = capBlock("block_cap");
    public static final DeferredBlock<Block> BLOCK_SEMTEX = materialBlock("block_semtex");
    public static final DeferredBlock<Block> BLOCK_C4 = materialBlock("block_c4");
    public static final DeferredBlock<Block> BLOCK_POLYMER = materialBlock("block_polymer");
    public static final DeferredBlock<Block> BLOCK_BAKELITE = materialBlock("block_bakelite");
    public static final DeferredBlock<Block> BLOCK_RUBBER = materialBlock("block_rubber");
    public static final DeferredBlock<Block> BLOCK_CADMIUM = materialBlock("block_cadmium");
    public static final DeferredBlock<Block> BLOCK_TCALLOY = materialBlock("block_tcalloy");
    public static final DeferredBlock<Block> BLOCK_CDALLOY = materialBlock("block_cdalloy");
    public static final DeferredBlock<Block> BLOCK_NIOBIUM = materialBlock("block_niobium");
    public static final DeferredBlock<Block> BLOCK_BISMUTH = materialBlock("block_bismuth");
    public static final DeferredBlock<Block> BLOCK_TANTALIUM = materialBlock("block_tantalium");
    public static final DeferredBlock<Block> BLOCK_ZIRCONIUM = materialBlock("block_zirconium");
    public static final DeferredBlock<Block> BLOCK_DINEUTRONIUM = materialBlock("block_dineutronium");
    public static final DeferredBlock<Block> BLOCK_WASTE_VITRIFIED = materialBlock("block_waste_vitrified");
    public static final DeferredBlock<Block> BLOCK_PU_MIX = materialBlock("block_pu_mix");
    public static final DeferredBlock<Block> BLOCK_COPPER = materialBlock("block_copper");
    public static final DeferredBlock<Block> BLOCK_FLUORITE = materialBlock("block_fluorite");
    public static final DeferredBlock<Block> BLOCK_NITER = materialBlock("block_niter");
    public static final DeferredBlock<Block> BLOCK_RED_COPPER = materialBlock("block_red_copper");
    public static final DeferredBlock<Block> BLOCK_STEEL = materialBlock("block_steel");
    public static final DeferredBlock<Block> BLOCK_SULFUR = materialBlock("block_sulfur");
    public static final DeferredBlock<Block> BLOCK_TITANIUM = materialBlock("block_titanium");
    public static final DeferredBlock<Block> BLOCK_TUNGSTEN = materialBlock("block_tungsten");
    public static final DeferredBlock<Block> BLOCK_URANIUM = materialBlock("block_uranium");
    public static final DeferredBlock<Block> BLOCK_THORIUM = materialBlock("block_thorium");
    public static final DeferredBlock<Block> BLOCK_LEAD = materialBlock("block_lead");
    public static final DeferredBlock<Block> BLOCK_TRINITITE = materialBlock("block_trinitite");
    public static final DeferredBlock<Block> BLOCK_WASTE = materialBlock("block_waste");
    public static final DeferredBlock<Block> BLOCK_WASTE_PAINTED = materialBlock("block_waste_painted");
    public static final DeferredBlock<Block> BLOCK_SCRAP = materialBlock("block_scrap");
    public static final DeferredBlock<Block> BLOCK_BERYLLIUM = materialBlock("block_beryllium");
    public static final DeferredBlock<Block> BLOCK_SCHRABIDIUM = materialBlock("block_schrabidium");
    public static final DeferredBlock<Block> BLOCK_SCHRABIDIUM_CLUSTER = registerBlock("block_schrabidium_cluster",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(rock().strength(5.0F, 30000.0F)));
    public static final DeferredBlock<Block> BLOCK_EUPHEMIUM_CLUSTER = registerBlock("block_euphemium_cluster",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(rock().strength(5.0F, 60000.0F)));
    public static final DeferredBlock<Block> BLOCK_EUPHEMIUM = materialBlock("block_euphemium");
    public static final DeferredBlock<Block> BLOCK_TRITIUM = registerBlock("block_tritium",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(3.0F, 2.0F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredBlock<Block> BLOCK_ELECTRICAL_SCRAP = registerBlock("block_electrical_scrap",
            () -> new HbmFallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F, 5.0F)
                    .sound(SoundType.METAL)));
    public static final DeferredBlock<Block> BLOCK_ADVANCED_ALLOY = materialBlock("block_advanced_alloy");
    public static final DeferredBlock<Block> BLOCK_MAGNETIZED_TUNGSTEN = materialBlock("block_magnetized_tungsten");
    public static final DeferredBlock<Block> BLOCK_COMBINE_STEEL = materialBlock("block_combine_steel");
    public static final DeferredBlock<Block> BLOCK_AUSTRALIUM = materialBlock("block_australium");
    public static final DeferredBlock<Block> BLOCK_DESH = materialBlock("block_desh");
    public static final DeferredBlock<Block> BLOCK_DURA_STEEL = materialBlock("block_dura_steel");
    public static final DeferredBlock<Block> BLOCK_YELLOWCAKE = materialBlock("block_yellowcake");
    public static final DeferredBlock<Block> BLOCK_STARMETAL = materialBlock("block_starmetal");
    public static final DeferredBlock<Block> BLOCK_U233 = materialBlock("block_u233");
    public static final DeferredBlock<Block> BLOCK_U235 = materialBlock("block_u235");
    public static final DeferredBlock<Block> BLOCK_U238 = materialBlock("block_u238");
    public static final DeferredBlock<Block> BLOCK_URANIUM_FUEL = materialBlock("block_uranium_fuel");
    public static final DeferredBlock<Block> BLOCK_NEPTUNIUM = materialBlock("block_neptunium");
    public static final DeferredBlock<Block> BLOCK_POLONIUM = materialBlock("block_polonium");
    public static final DeferredBlock<Block> BLOCK_PLUTONIUM = materialBlock("block_plutonium");
    public static final DeferredBlock<Block> BLOCK_PU238 = materialBlock("block_pu238");
    public static final DeferredBlock<Block> BLOCK_PU239 = materialBlock("block_pu239");
    public static final DeferredBlock<Block> BLOCK_PU240 = materialBlock("block_pu240");
    public static final DeferredBlock<Block> BLOCK_MOX_FUEL = materialBlock("block_mox_fuel");
    public static final DeferredBlock<Block> BLOCK_PLUTONIUM_FUEL = materialBlock("block_plutonium_fuel");
    public static final DeferredBlock<Block> BLOCK_THORIUM_FUEL = materialBlock("block_thorium_fuel");
    public static final DeferredBlock<Block> BLOCK_SOLINIUM = materialBlock("block_solinium");
    public static final DeferredBlock<Block> BLOCK_SCHRABIDIUM_FUEL = materialBlock("block_schrabidium_fuel");
    public static final DeferredBlock<Block> BLOCK_LITHIUM = materialBlock("block_lithium");
    public static final DeferredBlock<Block> BLOCK_WHITE_PHOSPHORUS = materialBlock("block_white_phosphorus");
    public static final DeferredBlock<Block> BLOCK_RED_PHOSPHORUS = materialBlock("block_red_phosphorus");
    public static final DeferredBlock<Block> BLOCK_INSULATOR = materialBlock("block_insulator");
    public static final DeferredBlock<Block> BLOCK_ASBESTOS = materialBlock("block_asbestos");
    public static final DeferredBlock<Block> BLOCK_FIBERGLASS = registerBlock("block_fiberglass",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.WOOL)));
    public static final DeferredBlock<Block> BLOCK_COBALT = materialBlock("block_cobalt");
    public static final DeferredBlock<Block> BLOCK_AU198 = registerBlock("block_au198",
            () -> new RadiatingBlock(metal()
                    .strength(5.0F, 300.0F)
                    .randomTicks(), HbmHazardSystem.rawRadiationForId("block_au198")));

    public static final DeferredBlock<Block> ANVIL_IRON = anvil("anvil_iron", HbmAnvilBlock.TIER_IRON);
    public static final DeferredBlock<Block> ANVIL_LEAD = anvil("anvil_lead", HbmAnvilBlock.TIER_IRON);
    public static final DeferredBlock<Block> ANVIL_STEEL = anvil("anvil_steel", HbmAnvilBlock.TIER_STEEL);
    public static final DeferredBlock<Block> ANVIL_DESH = anvil("anvil_desh", HbmAnvilBlock.TIER_OIL);
    public static final DeferredBlock<Block> ANVIL_FERROURANIUM = anvil("anvil_ferrouranium", HbmAnvilBlock.TIER_NUCLEAR);
    public static final DeferredBlock<Block> ANVIL_SATURNITE = anvil("anvil_saturnite", HbmAnvilBlock.TIER_RBMK);
    public static final DeferredBlock<Block> ANVIL_BISMUTH_BRONZE = anvil("anvil_bismuth_bronze", HbmAnvilBlock.TIER_RBMK);
    public static final DeferredBlock<Block> ANVIL_ARSENIC_BRONZE = anvil("anvil_arsenic_bronze", HbmAnvilBlock.TIER_RBMK);
    public static final DeferredBlock<Block> ANVIL_SCHRABIDATE = anvil("anvil_schrabidate", HbmAnvilBlock.TIER_FUSION);
    public static final DeferredBlock<Block> ANVIL_DNT = anvil("anvil_dnt", HbmAnvilBlock.TIER_PARTICLE);
    public static final DeferredBlock<Block> ANVIL_OSMIRIDIUM = anvil("anvil_osmiridium", HbmAnvilBlock.TIER_GERALD);
    public static final DeferredBlock<Block> ANVIL_MURKY = anvil("anvil_murky", 1_916_169);

    public static final DeferredBlock<Block> RED_CABLE = energyCable("red_cable", 2.5D);
    public static final DeferredBlock<Block> RED_CABLE_CLASSIC = energyCable("red_cable_classic", 2.5D);
    public static final DeferredBlock<Block> RED_CABLE_BOX = powerCableBox("red_cable_box");
    public static final DeferredBlock<Block> RED_CABLE_PAINTABLE = registerBlock("red_cable_paintable",
            () -> new PaintableEnergyCableBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> RED_CABLE_GAUGE = registerBlock("red_cable_gauge",
            () -> new PowerGaugeBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> POLE_TOP = poleTop("pole_top");
    public static final DeferredBlock<Block> POLE_SATELLITE_RECEIVER = poleSatelliteReceiver("pole_satellite_receiver");
    public static final DeferredBlock<Block> RED_WIRE_COATED = coatedEnergyCable("red_wire_coated");
    public static final DeferredBlock<Block> CABLE_DIODE = registerBlock("cable_diode",
            () -> new CableDiodeBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> CABLE_SWITCH = registerBlock("cable_switch",
            () -> new CableSwitchBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> CABLE_DETECTOR = registerBlock("cable_detector",
            () -> new CableDetectorBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> RED_CONNECTOR = powerPylon("red_connector", PowerPylonBlock.Kind.RED_CONNECTOR);
    public static final DeferredBlock<Block> CONNECTOR_RED_SUPER = powerPylon("red_connector_super", PowerPylonBlock.Kind.CONNECTOR_RED_SUPER);
    public static final DeferredBlock<Block> RED_PYLON = powerPylon("red_pylon", PowerPylonBlock.Kind.RED_PYLON);
    public static final DeferredBlock<Block> RED_PYLON_MEDIUM_WOOD = powerPylonWood("red_pylon_medium_wood", PowerPylonBlock.Kind.RED_PYLON_MEDIUM_WOOD);
    public static final DeferredBlock<Block> RED_PYLON_MEDIUM_WOOD_TRANSFORMER = powerPylonWood("red_pylon_medium_wood_transformer", PowerPylonBlock.Kind.RED_PYLON_MEDIUM_WOOD_TRANSFORMER);
    public static final DeferredBlock<Block> RED_PYLON_MEDIUM_STEEL = powerPylon("red_pylon_medium_steel", PowerPylonBlock.Kind.RED_PYLON_MEDIUM_STEEL);
    public static final DeferredBlock<Block> RED_PYLON_MEDIUM_STEEL_TRANSFORMER = powerPylon("red_pylon_medium_steel_transformer", PowerPylonBlock.Kind.RED_PYLON_MEDIUM_STEEL_TRANSFORMER);
    public static final DeferredBlock<Block> RED_PYLON_LARGE = powerPylon("red_pylon_large", PowerPylonBlock.Kind.RED_PYLON_LARGE);
    public static final DeferredBlock<Block> SUBSTATION = powerPylon("substation", PowerPylonBlock.Kind.SUBSTATION);
    public static final DeferredBlock<Block> FLUID_DUCT_MK2 = fluidDuct("fluid_duct_mk2", FluidDuctBlock.Kind.MK2);
    public static final DeferredBlock<Block> FLUID_DUCT_NEO = fluidDuct("fluid_duct_neo", FluidDuctBlock.Kind.MK2);
    public static final DeferredBlock<Block> FLUID_DUCT_BOX = fluidDuct("fluid_duct_box", FluidDuctBlock.Kind.BOX);
    public static final DeferredBlock<Block> FLUID_DUCT_EXHAUST = fluidDuct("fluid_duct_exhaust", FluidDuctBlock.Kind.EXHAUST);
    public static final DeferredBlock<Block> FLUID_DUCT_GAUGE = fluidDuct("fluid_duct_gauge", FluidDuctBlock.Kind.GAUGE);
    public static final DeferredBlock<Block> FLUID_DUCT_PAINTABLE = fluidDuct("fluid_duct_paintable", FluidDuctBlock.Kind.PAINTABLE);
    public static final DeferredBlock<Block> FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST = fluidDuct("fluid_duct_paintable_block_exhaust", FluidDuctBlock.Kind.PAINTABLE_EXHAUST);
    public static final DeferredBlock<Block> FLUID_DUCT_SOLID = fluidDuct("fluid_duct_solid", FluidDuctBlock.Kind.SOLID);
    public static final DeferredBlock<Block> FLUID_DUCT_SOLID_SEALED = fluidDuct("fluid_duct_solid_sealed", FluidDuctBlock.Kind.SOLID_SEALED);
    public static final DeferredBlock<Block> FLUID_VALVE = fluidDuct("fluid_valve", FluidDuctBlock.Kind.VALVE);
    public static final DeferredBlock<Block> FLUID_SWITCH = fluidDuct("fluid_switch", FluidDuctBlock.Kind.SWITCH);
    public static final DeferredBlock<Block> FLUID_COUNTER_VALVE = fluidDuct("fluid_counter_valve", FluidDuctBlock.Kind.COUNTER_VALVE);
    public static final DeferredBlock<Block> PIPE_ANCHOR = registerObjBlock("pipe_anchor", () -> new PipeAnchorBlock(metal()
            .strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> PISTON_INSERTER = registerObjBlock("piston_inserter", () -> new PistonInserterBlock(metal()
            .strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> FLUID_PUMP = fluidPump("fluid_pump");
    public static final DeferredBlock<Block> MACHINE_DRAIN = drain("machine_drain");
    public static final DeferredBlock<Block> OIL_PIPE = oilPipe("oil_pipe");
    public static final DeferredBlock<Block> BARREL_PLASTIC = fluidBarrel("barrel_plastic", FluidBarrelBlock.Kind.PLASTIC);
    public static final DeferredBlock<Block> BARREL_CORRODED = fluidBarrel("barrel_corroded", FluidBarrelBlock.Kind.CORRODED);
    public static final DeferredBlock<Block> BARREL_STEEL = fluidBarrel("barrel_steel", FluidBarrelBlock.Kind.STEEL);
    public static final DeferredBlock<Block> BARREL_TCALLOY = fluidBarrel("barrel_tcalloy", FluidBarrelBlock.Kind.TCALLOY);
    public static final DeferredBlock<Block> BARREL_ANTIMATTER = fluidBarrel("barrel_antimatter", FluidBarrelBlock.Kind.ANTIMATTER);
    public static final DeferredBlock<Block> RED_BARREL = legacyBarrel("red_barrel", LegacyBarrelBlock.Kind.RED);
    public static final DeferredBlock<Block> PINK_BARREL = legacyBarrel("pink_barrel", LegacyBarrelBlock.Kind.PINK);
    public static final DeferredBlock<Block> LOX_BARREL = legacyBarrel("lox_barrel", LegacyBarrelBlock.Kind.LOX);
    public static final DeferredBlock<Block> TAINT_BARREL = legacyBarrel("taint_barrel", LegacyBarrelBlock.Kind.TAINT);
    public static final DeferredBlock<Block> YELLOW_BARREL = legacyBarrel("yellow_barrel", LegacyBarrelBlock.Kind.YELLOW);
    public static final DeferredBlock<Block> VITRIFIED_BARREL = legacyBarrel("vitrified_barrel", LegacyBarrelBlock.Kind.VITRIFIED);
    public static final DeferredBlock<Block> TAINT = registerBlockWithoutItem("taint", () -> new TaintBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(15.0F, 10.0F)
                    .noLootTable()
                    .randomTicks()
    ));
    public static final DeferredBlock<Block> MACHINE_FLUIDTANK = fluidTank("machine_fluidtank");
    public static final DeferredBlock<Block> MACHINE_UF6_TANK = hexafluorideTank("machine_uf6_tank");
    public static final DeferredBlock<Block> MACHINE_PUF6_TANK = hexafluorideTank("machine_puf6_tank");
    public static final DeferredBlock<Block> MACHINE_BAT9000 = bigAssTank("machine_bat9000");
    public static final DeferredBlock<Block> MACHINE_BIGASSTANK = largeFluidTank("machine_bigasstank");
    public static final DeferredBlock<Block> MACHINE_BATTERY_REDD = batteryRedd("machine_battery_redd");
    public static final DeferredBlock<Block> MACHINE_BATTERY_SOCKET = batterySocket("machine_battery_socket");
    public static final DeferredBlock<Block> CAPACITOR_COPPER = registerObjBlock("capacitor_copper",
            () -> new CapacitorBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> CAPACITOR_BUS = registerBlockWithoutItem("capacitor_bus",
            () -> new CapacitorBusBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> CHARGER = registerObjBlock("charger", () -> new ChargerBlock(metal()
            .strength(5.0F, 10.0F)
            .noOcclusion()));
    // 1.7.10 BlockConveyorBase family. These use dedicated block items solely for
    // pick-block and commands; normal construction and recovery use Conveyor Wand.
    public static final DeferredBlock<ConveyorBlock> CONVEYOR = conveyor("conveyor", ConveyorBlock.Kind.REGULAR);
    public static final DeferredBlock<ConveyorBlock> CONVEYOR_EXPRESS = conveyor("conveyor_express", ConveyorBlock.Kind.EXPRESS);
    public static final DeferredBlock<ConveyorBlock> CONVEYOR_DOUBLE = conveyor("conveyor_double", ConveyorBlock.Kind.DOUBLE);
    public static final DeferredBlock<ConveyorBlock> CONVEYOR_TRIPLE = conveyor("conveyor_triple", ConveyorBlock.Kind.TRIPLE);
    public static final DeferredBlock<ConveyorBlock> CONVEYOR_LIFT = conveyor("conveyor_lift", ConveyorBlock.Kind.LIFT);
    public static final DeferredBlock<ConveyorBlock> CONVEYOR_CHUTE = conveyor("conveyor_chute", ConveyorBlock.Kind.CHUTE);

    // Legacy 1.7.10 conveyor crane family.  These are stateful directional
    // blocks rather than catalog placeholders; their old tile-entity filters
    // can be layered on without changing the registry contract.
    public static final DeferredBlock<Block> CRANE_BOXER = crane("crane_boxer", CraneMachineBlock.Kind.BOXER);
    public static final DeferredBlock<Block> CRANE_EXTRACTOR = crane("crane_extractor", CraneMachineBlock.Kind.EXTRACTOR);
    public static final DeferredBlock<Block> CRANE_GRABBER = crane("crane_grabber", CraneMachineBlock.Kind.GRABBER);
    public static final DeferredBlock<Block> CRANE_INSERTER = crane("crane_inserter", CraneMachineBlock.Kind.INSERTER);
    public static final DeferredBlock<Block> CRANE_PARTITIONER = crane("crane_partitioner", CraneMachineBlock.Kind.PARTITIONER);
    public static final DeferredBlock<Block> CRANE_ROUTER = crane("crane_router", CraneMachineBlock.Kind.ROUTER);
    public static final DeferredBlock<Block> CRANE_SPLITTER = crane("crane_splitter", CraneMachineBlock.Kind.SPLITTER);
    public static final DeferredBlock<Block> CRANE_UNBOXER = crane("crane_unboxer", CraneMachineBlock.Kind.UNBOXER);

    // Custom-machine casing components retain their 1.7.10 metadata variants.
    public static final DeferredBlock<Block> CM_BLOCK = cmVariant("cm_block", "block.reinhardtshbm.cm_block",
            new String[]{"steel", "alloy", "desh", "tcalloy"}, false);
    public static final DeferredBlock<Block> CM_SHEET = cmVariant("cm_sheet", "block.reinhardtshbm.cm_sheet",
            new String[]{"steel", "alloy", "desh", "tcalloy"}, false);
    public static final DeferredBlock<Block> CM_TANK = cmVariant("cm_tank", "block.reinhardtshbm.cm_tank",
            new String[]{"steel", "alloy", "desh", "tcalloy"}, true);
    public static final DeferredBlock<Block> CM_PORT = cmVariant("cm_port", "block.reinhardtshbm.cm_port",
            new String[]{"steel", "alloy", "desh", "tcalloy"}, false);
    public static final DeferredBlock<Block> CM_ENGINE = cmVariant("cm_engine", "block.reinhardtshbm.cm_engine",
            new String[]{"standard", "desh", "bismuth"}, false);
    public static final DeferredBlock<Block> CM_CIRCUIT = cmVariant("cm_circuit", "block.reinhardtshbm.cm_circuit",
            new String[]{"aluminium", "copper", "red_copper", "gold", "schrabidium"}, false);
    public static final DeferredBlock<Block> CM_FLUX = registerBlock("cm_flux", () -> new RotatedPillarBlock(
            metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> CM_HEAT = registerBlock("cm_heat", () -> new RotatedPillarBlock(
            metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> CUSTOM_MACHINE = registerBlock("custom_machine", () -> new CustomMachineBlock(
            metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> CUSTOM_MACHINE_ANCHOR = registerBlock("custom_machine_anchor",
            () -> new LegacyDirectionalBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> MACHINE_INDUSTRIAL_GENERATOR = registerBlock("machine_industrial_generator",
            () -> new IndustrialGeneratorBlock(metal().strength(5.0F, 10.0F)));
    /** Compatibility id used by old 1.7.10 breeding-reactor item stacks. */
    public static final DeferredBlock<Block> MACHINE_REACTOR = registerBlock("machine_reactor",
            () -> new LegacyDirectionalBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> MACHINE_CONVERTER_HE_RF = registerBlock("machine_converter_he_rf",
            () -> new EnergyConverterBlock(metal().strength(5.0F, 10.0F), EnergyConverterBlock.Kind.HE_TO_FE));
    public static final DeferredBlock<Block> MACHINE_CONVERTER_RF_HE = registerBlock("machine_converter_rf_he",
            () -> new EnergyConverterBlock(metal().strength(5.0F, 10.0F), EnergyConverterBlock.Kind.FE_TO_HE));
    public static final DeferredBlock<Block> MACHINE_DETECTOR = registerBlock("machine_detector",
            () -> new PowerDetectorBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> MACHINE_TRANSFORMER = registerBlock("machine_transformer",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    // 1.7.10 machine_ devices which were still being exposed through the legacy placeholder registry.
    // Their dimensions are kept in the old {up, down, north, south, west, east} order.
    public static final DeferredBlock<Block> MACHINE_ANNIHILATOR = legacyMachine("machine_annihilator", new int[]{2, 0, 4, 4, 1, 1}, 4);
    public static final DeferredBlock<Block> MACHINE_AUTOCRAFTER = legacyMachine("machine_autocrafter", new int[]{0, 0, 0, 0, 0, 0}, 0);
    public static final DeferredBlock<Block> MACHINE_AUTOSAW = legacyMachine("machine_autosaw", new int[]{0, 0, 0, 0, 0, 0}, 0);
    public static final DeferredBlock<Block> MACHINE_THRESHER = legacyMachine("machine_thresher", new int[]{0, 0, 0, 0, 0, 0}, 0);
    public static final DeferredBlock<Block> MACHINE_LPW2 = legacyMachine("machine_lpw2", new int[]{6, 0, 3, 3, 9, 10}, 3);
    public static final DeferredBlock<Block> MACHINE_CONVEYOR_PRESS = conveyorPress("machine_conveyor_press");
    public static final DeferredBlock<Block> MACHINE_FORCEFIELD = legacyMachine("machine_forcefield", new int[]{0, 0, 0, 0, 0, 0}, 0);
    public static final DeferredBlock<Block> MACHINE_MISSILE_ASSEMBLY = legacyMachine("machine_missile_assembly", new int[]{0, 0, 0, 0, 0, 0}, 0);
    public static final DeferredBlock<Block> MACHINE_ORBUS = legacyMachine("machine_orbus", new int[]{4, 0, 2, 1, 2, 1}, 1);
    public static final DeferredBlock<Block> MACHINE_PRECASS = legacyMachine("machine_precass", new int[]{2, 0, 1, 1, 1, 1}, 1);
    public static final DeferredBlock<Block> MACHINE_PYROOVEN = legacyMachine("machine_pyrooven", new int[]{2, 0, 3, 3, 2, 2}, 3);
    public static final DeferredBlock<Block> RADAR_SCREEN = radarScreen("radar_screen");
    public static final DeferredBlock<Block> MACHINE_RADAR = legacyMachine("machine_radar", new int[]{0, 0, 0, 0, 0, 0}, 0);
    public static final DeferredBlock<Block> MACHINE_RADAR_LARGE = legacyMachine("machine_radar_large", new int[]{4, 0, 1, 1, 1, 1}, 1);
    public static final DeferredBlock<Block> MACHINE_RADGEN = legacyMachine("machine_radgen", new int[]{2, 0, 3, 2, 1, 1}, 2);
    public static final DeferredBlock<Block> MACHINE_RADIOLYSIS = legacyMachine("machine_radiolysis", new int[]{2, 0, 1, 1, 1, 1}, 0);
    public static final DeferredBlock<Block> MACHINE_RTG_GREY = legacyMachine("machine_rtg_grey", new int[]{0, 0, 0, 0, 0, 0}, 0);
    public static final DeferredBlock<Block> MACHINE_SATLINKER = registerBlock("machine_satlinker",
            () -> new SatelliteLinkerBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> MACHINE_SAWMILL = sawmillMachine("machine_sawmill", new int[]{1, 0, 1, 1, 1, 1}, 1);
    public static final DeferredBlock<Block> MACHINE_TELEPORTER = legacyMachine("machine_teleporter", new int[]{0, 0, 0, 0, 0, 0}, 0);
    public static final DeferredBlock<Block> TELEANCHOR = registerBlock("teleanchor",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> MACHINE_TURBOFAN = legacyMachine("machine_turbofan", new int[]{2, 0, 1, 1, 3, 3}, 1);
    public static final DeferredBlock<Block> FAN = registerObjBlock("fan",
            () -> new FanBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> FLOODLIGHT = registerObjBlock("floodlight",
            () -> new FloodlightBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> CARGO_ELEVATOR = registerObjBlock("cargo_elevator",
            () -> new CargoElevatorBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> BROADCASTER_PC = registerBlock("broadcaster_pc",
            () -> new BroadcasterBlock(metal().strength(5.0F, 15.0F).noOcclusion()));
    public static final DeferredBlock<Block> LAMP_DEMON = demonLamp("lamp_demon");
    public static final DeferredBlock<Block> RADIOBOX = registerBlock("radiobox",
            () -> new RadioboxBlock(metal().strength(3.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> RADIOREC = registerBlock("radiorec",
            () -> new RadioRecBlock(metal().strength(3.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> RADIO_AUTOCAL = registerObjBlock("radio_autocal",
            () -> new AutocalBlock(metal().strength(3.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> REFUELER = registerObjBlock("refueler",
            () -> new RefuelerBlock(metal().strength(3.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> DECO_EMITTER = registerBlock("deco_emitter",
            () -> new DecorationEmitterBlock(metal().strength(5.0F, 20.0F).noOcclusion()));
    public static final DeferredBlock<Block> PART_EMITTER = registerBlock("part_emitter",
            () -> new PartEmitterBlock(metal().strength(5.0F, 20.0F)));
    public static final DeferredBlock<Block> DUNGEON_SPAWNER = registerBlock("dungeon_spawner",
            () -> new DungeonSpawnerBlock(rock().strength(-1.0F, 500_000.0F)));
    // Redstone-over-radio endpoint family. These replace the generic legacy
    // registrations so each block now has directional support and live state.
    public static final DeferredBlock<Block> RADIO_TORCH_SENDER = radioTorch("radio_torch_sender", RadioTorchBlock.Kind.SENDER);
    public static final DeferredBlock<Block> RADIO_TORCH_RECEIVER = radioTorch("radio_torch_receiver", RadioTorchBlock.Kind.RECEIVER);
    public static final DeferredBlock<Block> RADIO_TORCH_COUNTER = radioTorch("radio_torch_counter", RadioTorchBlock.Kind.COUNTER);
    public static final DeferredBlock<Block> RADIO_TORCH_LOGIC = radioTorch("radio_torch_logic", RadioTorchBlock.Kind.LOGIC);
    public static final DeferredBlock<Block> RADIO_TORCH_READER = radioTorch("radio_torch_reader", RadioTorchBlock.Kind.READER);
    public static final DeferredBlock<Block> RADIO_TORCH_CONTROLLER = radioTorch("radio_torch_controller", RadioTorchBlock.Kind.CONTROLLER);
    public static final DeferredBlock<Block> RADIO_TELEX = registerObjBlock("radio_telex",
            () -> new com.reinhardt.hbm.block.RadioTelexBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .sound(SoundType.WOOD)
                    .strength(3.0F, 10.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> BOAT = registerObjBlock("boat",
            () -> new BoatBlock(metal().strength(10.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> BOBBLEHEAD = bobblehead("bobblehead");
    public static final DeferredBlock<Block> SNOWGLOBE = snowglobe("snowglobe");
    public static final DeferredBlock<Block> PLUSHIE = plushie("plushie");
    public static final DeferredBlock<Block> LANTERN = lantern("lantern", false);
    public static final DeferredBlock<Block> LANTERN_BEHEMOTH = lantern("lantern_behemoth", true);
    public static final DeferredBlock<Block> CRATE = lootCrate("crate", SoundType.WOOD, 5.0F, 10.0F, LootCrateBlock.Kind.SUPPLY);
    public static final DeferredBlock<Block> CRATE_WEAPON = lootCrate("crate_weapon", SoundType.WOOD, 5.0F, 10.0F, LootCrateBlock.Kind.WEAPON);
    public static final DeferredBlock<Block> CRATE_LEAD = lootCrate("crate_lead", SoundType.METAL, 5.0F, 10.0F, LootCrateBlock.Kind.LEAD);
    public static final DeferredBlock<Block> CRATE_METAL = lootCrate("crate_metal", SoundType.METAL, 5.0F, 10.0F, LootCrateBlock.Kind.METAL);
    public static final DeferredBlock<Block> CRATE_RED = lootCrate("crate_red", SoundType.METAL, 5.0F, 10.0F, LootCrateBlock.Kind.RED);
    public static final DeferredBlock<Block> CRATE_CAN = registerBlock("crate_can", () -> new CanCrateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(1.0F, 2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<Block> CRATE_JUNGLE = registerBlock("crate_jungle", () -> new JungleCrateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(1.0F, 2.5F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> CRATE_AMMO = registerBlock("crate_ammo", () -> new AmmoCrateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.0F, 2.5F).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> CRATE_SUPPLY = registerBlock("crate_supply", () -> new SupplyCrateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(1.0F, 2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<Block> CRATE_IRON = storageCrate("crate_iron", StorageCrateBlockEntity.Kind.IRON, 5.0F, 10.0F);
    public static final DeferredBlock<Block> CRATE_STEEL = storageCrate("crate_steel", StorageCrateBlockEntity.Kind.STEEL, 5.0F, 20.0F);
    public static final DeferredBlock<Block> CRATE_DESH = storageCrate("crate_desh", StorageCrateBlockEntity.Kind.DESH, 7.5F, 300.0F);
    public static final DeferredBlock<Block> CRATE_TEMPLATE = storageCrate("crate_template", StorageCrateBlockEntity.Kind.TEMPLATE, 7.5F, 300.0F);
    public static final DeferredBlock<Block> CRATE_TUNGSTEN = storageCrate("crate_tungsten", StorageCrateBlockEntity.Kind.TUNGSTEN, 15.0F, 10_000.0F);
    public static final DeferredBlock<Block> DRONE_WAYPOINT = droneWaypoint("drone_waypoint");
    public static final DeferredBlock<Block> DRONE_WAYPOINT_REQUEST = droneRequestWaypoint("drone_waypoint_request");
    public static final DeferredBlock<Block> DRONE_CRATE = droneContainer("drone_crate", DroneNetworkContainerBlock.Kind.CRATE);
    public static final DeferredBlock<Block> DRONE_DOCK = droneContainer("drone_dock", DroneNetworkContainerBlock.Kind.DOCK);
    public static final DeferredBlock<Block> DRONE_CRATE_PROVIDER = droneContainer("drone_crate_provider", DroneNetworkContainerBlock.Kind.PROVIDER);
    public static final DeferredBlock<Block> DRONE_CRATE_REQUESTER = droneContainer("drone_crate_requester", DroneNetworkContainerBlock.Kind.REQUESTER);
    public static final DeferredBlock<Block> SAFE = registerBlock("safe", () -> new SafeBlock(metal().strength(7.5F, 10_000.0F)));
    public static final DeferredBlock<Block> MASS_STORAGE_IRON = massStorage("mass_storage_iron", MassStorageBlock.Kind.IRON);
    public static final DeferredBlock<Block> MASS_STORAGE_DESH = massStorage("mass_storage_desh", MassStorageBlock.Kind.DESH);
    public static final DeferredBlock<Block> MASS_STORAGE = massStorage("mass_storage", MassStorageBlock.Kind.RESISTANT);
    public static final DeferredBlock<Block> MASS_STORAGE_WOOD = massStorage("mass_storage_wood", MassStorageBlock.Kind.WOOD);
    public static final DeferredBlock<Block> MACHINE_WOOD_BURNER = woodBurner("machine_wood_burner");
    public static final DeferredBlock<Block> MACHINE_ELECTRIC_FURNACE_OFF = powerMachine(
            "machine_electric_furnace_off",
            PowerMachineBlock.MachineType.ELECTRIC_FURNACE
    );
    public static final DeferredBlock<Block> MACHINE_ELECTRIC_FURNACE_ON = powerMachine(
            "machine_electric_furnace_on",
            PowerMachineBlock.MachineType.ELECTRIC_FURNACE_ON
    );
    public static final DeferredBlock<Block> MACHINE_SHREDDER = powerMachine(
            "machine_shredder",
            PowerMachineBlock.MachineType.SHREDDER
    );
    public static final DeferredBlock<Block> MACHINE_MICROWAVE = microwave("machine_microwave");
    public static final DeferredBlock<Block> MACHINE_ARMOR_TABLE = registerBlock("machine_armor_table",
            () -> new ArmorTableBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> MACHINE_WEAPON_TABLE = registerBlock("machine_weapon_table",
            () -> new WeaponTableBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> MACHINE_ASSEMBLY_FACTORY = assemblyFactory("machine_assembly_factory");
    public static final DeferredBlock<Block> MACHINE_ASSEMBLY_MACHINE = assemblyMachine("machine_assembly_machine");
    public static final DeferredBlock<Block> MACHINE_CHEMICAL_FACTORY = chemicalFactory("machine_chemical_factory");
    public static final DeferredBlock<Block> MACHINE_CHEMICAL_PLANT = chemicalPlant("machine_chemical_plant");
    public static final DeferredBlock<Block> MACHINE_SOLDERING_STATION = solderingStation("machine_soldering_station");
    public static final DeferredBlock<Block> MACHINE_ARC_WELDER = arcWelder("machine_arc_welder");
    public static final DeferredBlock<Block> MACHINE_ARC_FURNACE = arcFurnace("machine_arc_furnace");
    public static final DeferredBlock<Block> MACHINE_STORAGE_DRUM = storageDrum("machine_storage_drum");
    public static final DeferredBlock<Block> MACHINE_COMPRESSOR = compressor("machine_compressor", CompressorBlockEntity.Kind.NORMAL);
    public static final DeferredBlock<Block> MACHINE_COMPRESSOR_COMPACT = compressor("machine_compressor_compact", CompressorBlockEntity.Kind.COMPACT);
    public static final DeferredBlock<Block> MACHINE_MIXER = mixer("machine_mixer");
    public static final DeferredBlock<Block> MACHINE_FUNNEL = registerBlockWithoutItem("machine_funnel",
            () -> new FunnelBlock(metal().strength(10.0F, 20.0F).noOcclusion()));
    public static final DeferredItem<Item> MACHINE_FUNNEL_ITEM = HbmItems.ITEMS.register("machine_funnel",
            () -> new FunnelBlockItem(MACHINE_FUNNEL.get(), new Item.Properties()));
    public static final DeferredBlock<Block> MACHINE_SIREN = registerBlock("machine_siren",
            () -> new SirenBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> MACHINE_AMMO_PRESS = ammoPress("machine_ammo_press");
    public static final DeferredBlock<Block> MACHINE_PRESS = pressMachine("machine_press");
    public static final DeferredBlock<Block> MACHINE_EPRESS = pressMachine("machine_epress");
    public static final DeferredBlock<Block> MACHINE_CENTRIFUGE = centrifuge("machine_centrifuge", CentrifugeBlock.Kind.NORMAL);
    public static final DeferredBlock<Block> MACHINE_GASCENT = centrifuge("machine_gascent", CentrifugeBlock.Kind.GAS);
    public static final DeferredBlock<Block> MACHINE_CRYSTALLIZER = crystallizer("machine_crystallizer");
    public static final DeferredBlock<Block> MACHINE_CYCLOTRON = cyclotron("machine_cyclotron");
    public static final DeferredBlock<Block> MACHINE_EXPOSURE_CHAMBER = exposureChamber("machine_exposure_chamber");
    public static final DeferredBlock<Block> MACHINE_DEUTERIUM_EXTRACTOR = deuteriumExtractor("machine_deuterium_extractor");
    public static final DeferredBlock<Block> MACHINE_DEUTERIUM_TOWER = deuteriumTower("machine_deuterium_tower");
    public static final DeferredBlock<Block> PA_SOURCE = particleAccelerator("pa_source", ParticleAcceleratorBlock.Kind.SOURCE);
    public static final DeferredBlock<Block> PA_BEAMLINE = particleAccelerator("pa_beamline", ParticleAcceleratorBlock.Kind.BEAMLINE);
    public static final DeferredBlock<Block> PA_RFC = particleAccelerator("pa_rfc", ParticleAcceleratorBlock.Kind.RFC);
    public static final DeferredBlock<Block> PA_QUADRUPOLE = particleAccelerator("pa_quadrupole", ParticleAcceleratorBlock.Kind.QUADRUPOLE);
    public static final DeferredBlock<Block> PA_DIPOLE = particleAccelerator("pa_dipole", ParticleAcceleratorBlock.Kind.DIPOLE);
    public static final DeferredBlock<Block> PA_DETECTOR = particleAccelerator("pa_detector", ParticleAcceleratorBlock.Kind.DETECTOR);
    public static final DeferredBlock<Block> MACHINE_SILEX = silex("machine_silex");
    public static final DeferredBlock<Block> MACHINE_FEL = fel("machine_fel");
    public static final DeferredBlock<Block> MACHINE_EXCAVATOR = excavator("machine_excavator");
    public static final DeferredBlock<Block> MACHINE_MINING_LASER = miningLaser("machine_mining_laser");
    public static final DeferredBlock<Block> MACHINE_ORE_SLOPPER = oreSlopper("machine_ore_slopper");
    public static final DeferredBlock<Block> MACHINE_STIRLING = stirlingGenerator("machine_stirling");
    public static final DeferredBlock<Block> MACHINE_STIRLING_STEEL = stirlingGenerator("machine_stirling_steel");
    public static final DeferredBlock<Block> MACHINE_STIRLING_CREATIVE = stirlingGenerator("machine_stirling_creative");
    public static final DeferredBlock<Block> MACHINE_DIESEL = dieselGenerator("machine_diesel");
    public static final DeferredBlock<Block> MACHINE_COMBUSTION_ENGINE = combustionEngine("machine_combustion_engine");
    public static final DeferredBlock<Block> MACHINE_FLARE = gasFlare("machine_flare");
    public static final DeferredBlock<Block> MACHINE_STEAM_ENGINE = steamEngine("machine_steam_engine");
    public static final DeferredBlock<Block> MACHINE_SOLAR_BOILER = solarBoiler("machine_solar_boiler");
    public static final DeferredBlock<Block> MACHINE_INTAKE = airCompressor("machine_intake");
    public static final DeferredBlock<Block> MACHINE_HEPHAESTUS = geothermalHeatExchanger("machine_hephaestus");
    public static final DeferredBlock<Block> SOLAR_MIRROR = solarMirror("solar_mirror");
    /** 1.7.10 MachineHeatBoiler registry id. */
    public static final DeferredBlock<Block> MACHINE_BOILER = heatBoiler("machine_boiler");
    public static final DeferredBlock<Block> MACHINE_INDUSTRIAL_BOILER = industrialBoiler("machine_industrial_boiler");
    public static final DeferredBlock<Block> MACHINE_BOILER_OFF = smallBoiler("machine_boiler_off", SmallBoilerBlock.Kind.OLD);
    public static final DeferredBlock<Block> MACHINE_BOILER_ELECTRIC_OFF = smallBoiler("machine_boiler_electric_off", SmallBoilerBlock.Kind.ELECTRIC);
    public static final DeferredBlock<Block> MACHINE_TURBINE = steamTurbine("machine_turbine");
    public static final DeferredBlock<Block> MACHINE_INDUSTRIAL_TURBINE = industrialTurbine("machine_industrial_turbine");
    public static final DeferredBlock<Block> MACHINE_LARGE_TURBINE = largeTurbine("machine_large_turbine");
    public static final DeferredBlock<Block> MACHINE_CHUNGUS = leviathanTurbine("machine_chungus");
    public static final DeferredBlock<Block> MACHINE_TURBINEGAS = gasTurbine("machine_turbinegas");
    public static final DeferredBlock<Block> MACHINE_CONDENSER = steamCondenser("machine_condenser");
    public static final DeferredBlock<Block> MACHINE_CONDENSER_POWERED = poweredSteamCondenser("machine_condenser_powered");
    public static final DeferredBlock<Block> MACHINE_TOWER_SMALL = coolingTower("machine_tower_small", CoolingTowerBlock.Kind.SMALL);
    public static final DeferredBlock<Block> MACHINE_TOWER_LARGE = coolingTower("machine_tower_large", CoolingTowerBlock.Kind.LARGE);
    public static final DeferredBlock<Block> PUMP_STEAM = groundwaterPump("pump_steam", GroundwaterPumpBlock.Kind.STEAM);
    public static final DeferredBlock<Block> PUMP_ELECTRIC = groundwaterPump("pump_electric", GroundwaterPumpBlock.Kind.ELECTRIC);
    public static final DeferredBlock<Block> MACHINE_WELL = oilDerrick("machine_well");
    public static final DeferredBlock<Block> MACHINE_PUMPJACK = oilPumpjack("machine_pumpjack");
    public static final DeferredBlock<Block> MACHINE_FRACKING_TOWER = frackingTower("machine_fracking_tower");
    public static final DeferredBlock<Block> MACHINE_REFINERY = refinery("machine_refinery");
    public static final DeferredBlock<Block> MACHINE_VACUUM_DISTILL = vacuumDistill("machine_vacuum_distill");
    public static final DeferredBlock<Block> MACHINE_COKER = coker("machine_coker");
    public static final DeferredBlock<Block> MACHINE_ROTARY_FURNACE = rotaryFurnace("machine_rotary_furnace");
    public static final DeferredBlock<Block> MACHINE_SOLIDIFIER = solidifier("machine_solidifier");
    public static final DeferredBlock<Block> MACHINE_LIQUEFACTOR = liquefactor("machine_liquefactor");
    public static final DeferredBlock<Block> MACHINE_FRACTION_TOWER = fractionTower("machine_fraction_tower");
    public static final DeferredBlock<Block> FRACTION_SPACER = fractionSpacer("fraction_spacer");
    public static final DeferredBlock<Block> MACHINE_CATALYTIC_CRACKER = catalyticCracker("machine_catalytic_cracker");
    public static final DeferredBlock<Block> MACHINE_CATALYTIC_REFORMER = catalyticReformer("machine_catalytic_reformer");
    public static final DeferredBlock<Block> MACHINE_HYDROTREATER = hydrotreater("machine_hydrotreater");
    public static final DeferredBlock<Block> MACHINE_ELECTROLYSER = electrolyzer("machine_electrolyser");
    public static final DeferredBlock<Block> HADRON_COIL_ALLOY = hadronCoil("hadron_coil_alloy", 10);
    public static final DeferredBlock<Block> HADRON_COIL_GOLD = hadronCoil("hadron_coil_gold", 25);
    public static final DeferredBlock<Block> HADRON_COIL_NEODYMIUM = hadronCoil("hadron_coil_neodymium", 50);
    public static final DeferredBlock<Block> HADRON_COIL_MAGTUNG = hadronCoil("hadron_coil_magtung", 100);
    public static final DeferredBlock<Block> HADRON_COIL_SCHRABIDIUM = hadronCoil("hadron_coil_schrabidium", 250);
    public static final DeferredBlock<Block> HADRON_COIL_SCHRABIDATE = hadronCoil("hadron_coil_schrabidate", 500);
    public static final DeferredBlock<Block> HADRON_COIL_STARMETAL = hadronCoil("hadron_coil_starmetal", 1000);
    public static final DeferredBlock<Block> HADRON_COIL_CHLOROPHYTE = hadronCoil("hadron_coil_chlorophyte", 2500);
    public static final DeferredBlock<Block> HADRON_COIL_MESE = hadronCoil("hadron_coil_mese", 10000);
    /** 1.7.10 ReactorResearch registry id. */
    public static final DeferredBlock<Block> MACHINE_REACTOR_SMALL = researchReactor("machine_reactor_small");
    public static final DeferredBlock<Block> MACHINE_CONTROLLER = reactorControl("machine_controller");
    public static final DeferredBlock<Block> MACHINE_REACTOR_BREEDING = breederReactor("machine_reactor_breeding");
    public static final DeferredBlock<Block> TURRET_JEREMY = turretJeremy("turret_jeremy");
    public static final DeferredBlock<Block> TURRET_CHEKHOV = turretChekhov("turret_chekhov");
    public static final DeferredBlock<Block> TURRET_FRIENDLY = legacyTurret("turret_friendly", LegacyTurretType.FRIENDLY);
    public static final DeferredBlock<Block> TURRET_FRITZ = legacyTurret("turret_fritz", LegacyTurretType.FRITZ);
    public static final DeferredBlock<Block> TURRET_HOWARD = legacyTurret("turret_howard", LegacyTurretType.HOWARD);
    public static final DeferredBlock<Block> TURRET_HOWARD_DAMAGED = legacyTurret("turret_howard_damaged", LegacyTurretType.HOWARD_DAMAGED);
    public static final DeferredBlock<Block> TURRET_MAXWELL = legacyTurret("turret_maxwell", LegacyTurretType.MAXWELL);
    public static final DeferredBlock<Block> TURRET_RICHARD = legacyTurret("turret_richard", LegacyTurretType.RICHARD);
    public static final DeferredBlock<Block> TURRET_TAUON = legacyTurret("turret_tauon", LegacyTurretType.TAUON);
    public static final DeferredBlock<Block> TURRET_ARTY = legacyTurret("turret_arty", LegacyTurretType.ARTY);
    public static final DeferredBlock<Block> TURRET_HIMARS = legacyTurret("turret_himars", LegacyTurretType.HIMARS);
    public static final DeferredBlock<Block> TURRET_SENTRY = legacyTurret("turret_sentry", LegacyTurretType.SENTRY);
    public static final DeferredBlock<Block> TURRET_SENTRY_DAMAGED = legacyTurret("turret_sentry_damaged", LegacyTurretType.SENTRY_DAMAGED);
    public static final DeferredBlock<Block> NUKE_BOY = nukeBoy("nuke_boy");
    public static final DeferredBlock<Block> NUKE_CUSTOM = legacyNuke("nuke_custom", LegacyNukeDefinition.CUSTOM);
    public static final DeferredBlock<Block> NUKE_FLEIJA = legacyNuke("nuke_fleija", LegacyNukeDefinition.FLEIJA);
    public static final DeferredBlock<Block> NUKE_FSTBMB = legacyNuke("nuke_fstbmb", LegacyNukeDefinition.BALEFIRE);
    public static final DeferredBlock<Block> NUKE_GADGET = legacyNuke("nuke_gadget", LegacyNukeDefinition.GADGET);
    public static final DeferredBlock<Block> NUKE_MAN = legacyNuke("nuke_man", LegacyNukeDefinition.MAN);
    public static final DeferredBlock<Block> NUKE_MIKE = legacyNuke("nuke_mike", LegacyNukeDefinition.MIKE);
    public static final DeferredBlock<Block> NUKE_N2 = legacyNuke("nuke_n2", LegacyNukeDefinition.N2);
    public static final DeferredBlock<Block> NUKE_PROTOTYPE = legacyNuke("nuke_prototype", LegacyNukeDefinition.PROTOTYPE);
    public static final DeferredBlock<Block> NUKE_SOLINIUM = legacyNuke("nuke_solinium", LegacyNukeDefinition.SOLINIUM);
    public static final DeferredBlock<Block> NUKE_TSAR = legacyNuke("nuke_tsar", LegacyNukeDefinition.TSAR);
    public static final DeferredBlock<Block> BOMB_MULTI = bombMulti("bomb_multi");
    public static final DeferredBlock<Block> VENDING_MACHINE = vendingMachine("vending_machine");
    public static final DeferredBlock<Block> EMP_BOMB = registerBlock("emp_bomb", () -> new LegacyRedstoneBombBlock(
            metal().strength(5.0F, 10.0F), LegacyRedstoneBombBlock.Kind.EMP));
    public static final DeferredBlock<Block> FLOAT_BOMB = registerBlock("float_bomb", () -> new LegacyRedstoneBombBlock(
            metal().strength(5.0F, 10.0F), LegacyRedstoneBombBlock.Kind.FLOAT));
    public static final DeferredBlock<Block> FLAME_WAR = registerBlock("flame_war", () -> new LegacyRedstoneBombBlock(
            metal().strength(5.0F, 10.0F), LegacyRedstoneBombBlock.Kind.FLAME_WAR));
    public static final DeferredBlock<Block> FIELD_DISTURBER = registerBlock("field_disturber", () -> new FieldDisturberBlock(
            metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> CRASHED_BOMB = crashedBomb("crashed_bomb");
    public static final DeferredBlock<Block> SEMTEX = registerBlock("semtex", () -> new TimedExplosiveBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.0F, 0.0F).sound(SoundType.GRASS),
            TimedExplosiveEntity.Kind.SEMTEX
    ));
    public static final DeferredBlock<Block> C4 = registerBlock("c4", () -> new TimedExplosiveBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.0F, 0.0F).sound(SoundType.GRASS),
            TimedExplosiveEntity.Kind.C4
    ));
    public static final DeferredBlock<Block> DYNAMITE = registerBlock("dynamite", () -> new TimedExplosiveBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.0F, 0.0F).sound(SoundType.GRASS),
            TimedExplosiveEntity.Kind.DYNAMITE
    ));
    public static final DeferredBlock<Block> THERM_ENDO = registerBlock("therm_endo", () -> new ThermalBombBlock(
            metal().strength(5.0F, 200.0F), ThermalBombBlock.Kind.ENDOTHERMIC
    ));
    public static final DeferredBlock<Block> THERM_EXO = registerBlock("therm_exo", () -> new ThermalBombBlock(
            metal().strength(5.0F, 200.0F), ThermalBombBlock.Kind.EXOTHERMIC
    ));
    public static final DeferredBlock<Block> DET_CORD = registerBlock("det_cord", () -> new DetonatableBlock(
            metal().strength(0.1F, 0.0F).noOcclusion(), DetonatableBlock.Kind.CORD
    ));
    public static final DeferredBlock<Block> DET_CHARGE = registerBlock("det_charge", () -> new DetonatableBlock(
            metal().strength(0.1F, 0.0F), DetonatableBlock.Kind.CHARGE
    ));
    public static final DeferredBlock<Block> DET_NUKE = registerBlock("det_nuke", () -> new DetonatableBlock(
            metal().strength(0.1F, 0.0F), DetonatableBlock.Kind.NUKE
    ));
    public static final DeferredBlock<Block> DET_MINER = registerBlock("det_miner", () -> new DetonatableBlock(
            metal().strength(0.1F, 0.0F), DetonatableBlock.Kind.MINER
    ));
    public static final DeferredBlock<Block> TNT_NTM = registerBlock("tnt_ntm", () -> new TimedExplosiveBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.0F, 0.0F).sound(SoundType.GRASS),
            TimedExplosiveEntity.Kind.TNT
    ));
    public static final DeferredBlock<Block> CHARGE_DYNAMITE = registerBlock("charge_dynamite", () -> new WallChargeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.0F, 0.0F).noOcclusion().noCollission(),
            WallChargeBlock.Kind.DYNAMITE
    ));
    public static final DeferredBlock<Block> CHARGE_MINER = registerBlock("charge_miner", () -> new WallChargeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.0F, 0.0F).noOcclusion().noCollission(),
            WallChargeBlock.Kind.MINER
    ));
    public static final DeferredBlock<Block> CHARGE_C4 = registerBlock("charge_c4", () -> new WallChargeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.0F, 0.0F).noOcclusion().noCollission(),
            WallChargeBlock.Kind.C4
    ));
    public static final DeferredBlock<Block> CHARGE_SEMTEX = registerBlock("charge_semtex", () -> new WallChargeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.0F, 0.0F).noOcclusion().noCollission(),
            WallChargeBlock.Kind.SEMTEX
    ));
    public static final DeferredBlock<Block> FISSURE_BOMB = registerBlock("fissure_bomb", () -> new FissureBombBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.0F, 0.0F).sound(SoundType.GRASS)
    ));
    public static final DeferredBlock<Block> FIREWORKS = registerBlock("fireworks", () -> new FireworksBlock(
            metal().strength(5.0F, 5.0F)
    ));
    public static final DeferredBlock<Block> MINE_AP = landmine("mine_ap", LandmineBlock.LandmineType.AP);
    public static final DeferredBlock<Block> MINE_HE = landmine("mine_he", LandmineBlock.LandmineType.HE);
    public static final DeferredBlock<Block> MINE_SHRAP = landmine("mine_shrap", LandmineBlock.LandmineType.SHRAPNEL);
    public static final DeferredBlock<Block> MINE_FAT = landmine("mine_fat", LandmineBlock.LandmineType.NUCLEAR);
    public static final DeferredBlock<Block> MINE_NAVAL = landmine("mine_naval", LandmineBlock.LandmineType.NAVAL);
    public static final DeferredBlock<Block> STRUCT_LAUNCHER = registerBlock("struct_launcher",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> STRUCT_SCAFFOLD = registerBlock("struct_scaffold",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> SAT_DOCK = registerObjBlock("sat_dock",
            () -> new SatelliteDockBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    /** Hidden 1.7.10 cargo-pad cells; they are not separate items or ports. */
    public static final DeferredBlock<Block> DUMMY_PLATE_CARGO = registerBlockWithoutItem("dummy_plate_cargo",
            () -> new SatelliteDockDummyBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    public static final DeferredBlock<Block> STRUCT_LAUNCHER_CORE = launcherStructCore("struct_launcher_core", false);
    public static final DeferredBlock<Block> STRUCT_LAUNCHER_CORE_LARGE = launcherStructCore("struct_launcher_core_large", true);
    public static final DeferredBlock<Block> STRUCT_SOYUZ_CORE = structSoyuzCore("struct_soyuz_core");
    public static final DeferredBlock<Block> COMPACT_LAUNCHER = compactLauncher("compact_launcher");
    public static final DeferredBlock<Block> LAUNCH_TABLE = launchTable("launch_table");
    public static final DeferredBlock<Block> LAUNCH_PAD = launchPad("launch_pad", LaunchPadBlock.Kind.SILO);
    public static final DeferredBlock<Block> LAUNCH_PAD_LARGE = launchPad("launch_pad_large", LaunchPadBlock.Kind.LARGE);
    public static final DeferredBlock<Block> LAUNCH_PAD_RUSTED = launchPad("launch_pad_rusted", LaunchPadBlock.Kind.RUSTED);
    public static final DeferredBlock<Block> SOYUZ_LAUNCHER = soyuzLauncher("soyuz_launcher");
    public static final DeferredBlock<Block> SOYUZ_CAPSULE = soyuzCapsule("soyuz_capsule");
    public static final DeferredBlock<Block> DUMMY_PLATE_COMPACT_LAUNCHER = launcherDummy(
            "dummy_plate_compact_launcher",
            Shapes.box(0.0D, 1.0D, 0.0D, 1.0D, 1.0D, 1.0D),
            false
    );
    public static final DeferredBlock<Block> DUMMY_PORT_COMPACT_LAUNCHER = launcherDummy(
            "dummy_port_compact_launcher",
            Shapes.block(),
            true
    );
    public static final DeferredBlock<Block> DUMMY_PLATE_LAUNCH_TABLE = launcherDummy(
            "dummy_plate_launch_table",
            Shapes.box(0.0D, 1.0D, 0.0D, 1.0D, 1.0D, 1.0D),
            false
    );
    public static final DeferredBlock<Block> DUMMY_PORT_LAUNCH_TABLE = launcherDummy(
            "dummy_port_launch_table",
            Shapes.block(),
            true
    );
    /** Hidden 1.7.10 worldgen substrate: visually vanilla dirt and always drops vanilla dirt. */
    public static final DeferredBlock<Block> NTM_DIRT = registerBlockWithoutItem("ntm_dirt",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));
    public static final DeferredBlock<Block> STRUCT_WATZ_CORE = watzStruct("struct_watz_core");
    public static final DeferredBlock<Block> WATZ = watz("watz");
    public static final DeferredBlock<Block> WATZ_END = registerVariantBlock("watz_end",
            () -> new LegacyVariantBlock(metal().strength(5.0F, 10.0F), 1), LegacyVariantBlock.VARIANT,
            "block.reinhardtshbm.watz_end", "plain", "bolted");
    /** Transitional pre-port alias retained for existing development worlds. */
    public static final DeferredBlock<Block> WATZ_CASING = registerBlock("watz_casing",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> WATZ_COOLER = registerBlock("watz_cooler",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> WATZ_ELEMENT = registerBlock("watz_element",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> WATZ_PUMP = watzPump("watz_pump");
    public static final DeferredBlock<Block> MACHINE_ZIRNOX = zirnoxReactor("machine_zirnox");
    public static final DeferredBlock<Block> PWR_BLOCK = pwrBlock("pwr_block", PwrBlock.Kind.BLOCK);
    public static final DeferredBlock<Block> PWR_CASING = pwrBlock("pwr_casing", PwrBlock.Kind.CASING);
    public static final DeferredBlock<Block> PWR_CHANNEL = pwrBlock("pwr_channel", PwrBlock.Kind.CHANNEL);
    public static final DeferredBlock<Block> PWR_CONTROL = pwrBlock("pwr_control", PwrBlock.Kind.CONTROL);
    public static final DeferredBlock<Block> PWR_CONTROLLER = pwrBlock("pwr_controller", PwrBlock.Kind.CONTROLLER);
    public static final DeferredBlock<Block> PWR_FUELROD = pwrBlock("pwr_fuelrod", PwrBlock.Kind.FUEL);
    /** Legacy 1.7.10 block id; keep separate from the pwr_fuel item registry entry. */
    public static final DeferredBlock<Block> PWR_FUEL = registerBlockWithoutItem("pwr_fuel",
            () -> new PwrBlock(metal().strength(5.0F, 10.0F).noOcclusion(), PwrBlock.Kind.FUEL));
    public static final DeferredBlock<Block> PWR_HEATEX = pwrBlock("pwr_heatex", PwrBlock.Kind.HEATEX);
    public static final DeferredBlock<Block> PWR_HEATSINK = pwrBlock("pwr_heatsink", PwrBlock.Kind.HEATSINK);
    public static final DeferredBlock<Block> PWR_NEUTRON_SOURCE = pwrBlock("pwr_neutron_source", PwrBlock.Kind.NEUTRON_SOURCE);
    public static final DeferredBlock<Block> PWR_PORT = pwrBlock("pwr_port", PwrBlock.Kind.PORT);
    public static final DeferredBlock<Block> PWR_REFLECTOR = pwrBlock("pwr_reflector", PwrBlock.Kind.REFLECTOR);
    public static final DeferredBlock<Block> MACHINE_WASTE_DRUM = wasteDrum("machine_waste_drum");
    public static final DeferredBlock<Block> MACHINE_PUREX = purex("machine_purex");
    public static final DeferredBlock<Block> MACHINE_ICF_PRESS = icfPress("machine_icf_press");
    public static final DeferredBlock<Block> STRUCT_ICF_CORE = icfStruct("struct_icf_core");
    public static final DeferredBlock<Block> ICF = icfCore("icf");
    public static final DeferredBlock<Block> ICF_COMPONENT = icfComponent("icf_component");
    public static final DeferredBlock<Block> ICF_CONTROLLER = icfController("icf_controller");
    public static final DeferredBlock<Block> ICF_LASER_COMPONENT = icfLaserComponent("icf_laser_component");
    public static final DeferredBlock<Block> ICF_BLOCK = icfAssembledLaser("icf_block");
    public static final DeferredBlock<Block> ZIRNOX_DESTROYED = zirnoxDestroyed("zirnox_destroyed");
    public static final DeferredBlock<Block> MACHINE_BLAST_FURNACE = machineBlastFurnace("machine_blast_furnace");
    public static final DeferredBlock<Block> MACHINE_CRUCIBLE = crucible("machine_crucible");
    public static final DeferredBlock<Block> FOUNDRY_MOLD = foundryCasting("foundry_mold", FoundryCastingBlock.Kind.MOLD);
    public static final DeferredBlock<Block> FOUNDRY_BASIN = foundryCasting("foundry_basin", FoundryCastingBlock.Kind.BASIN);
    public static final DeferredBlock<Block> FOUNDRY_CHANNEL = foundryChannel("foundry_channel");
    public static final DeferredBlock<Block> FOUNDRY_TANK = foundryTank("foundry_tank");
    public static final DeferredBlock<Block> FOUNDRY_OUTLET = foundryOutlet("foundry_outlet", FoundryOutletBlock.Kind.OUTLET);
    public static final DeferredBlock<Block> FOUNDRY_SLAGTAP = foundryOutlet("foundry_slagtap", FoundryOutletBlock.Kind.SLAGTAP);
    public static final DeferredBlock<Block> MACHINE_STRAND_CASTER = strandCaster("machine_strand_caster");
    public static final DeferredBlock<Block> MACHINE_KEYFORGE = registerBlock("machine_keyforge",
            () -> new MachineKeyForgeBlock(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> SLAG = foundrySlag("slag");
    public static final DeferredBlock<Block> FURNACE_COMBINATION = furnaceCombination("furnace_combination");
    public static final DeferredBlock<Block> MACHINE_FURNACE_BRICK_OFF = brickFurnace("machine_furnace_brick_off");
    public static final DeferredBlock<Block> MACHINE_FURNACE_BRICK_ON = registerBlockWithoutItem("machine_furnace_brick_on",
            () -> new BrickFurnaceBlock(metal().strength(5.0F, 10.0F)
                    .lightLevel(state -> state.getValue(BrickFurnaceBlock.LIT) ? 15 : 0)));
    public static final DeferredBlock<Block> FURNACE_IRON = legacyFurnace("furnace_iron", LegacyFurnaceBlock.Kind.IRON);
    public static final DeferredBlock<Block> FURNACE_STEEL = legacyFurnace("furnace_steel", LegacyFurnaceBlock.Kind.STEEL);
    public static final DeferredBlock<Block> DOOR_METAL = vanillaDoor("door_metal");
    public static final DeferredBlock<Block> DOOR_OFFICE = vanillaDoor("door_office");
    public static final DeferredBlock<Block> DOOR_BUNKER = vanillaDoor("door_bunker");
    public static final DeferredBlock<Block> DOOR_RED = vanillaDoor("door_red");
    public static final DeferredBlock<Block> TRAPDOOR_STEEL = vanillaTrapdoor("trapdoor_steel");
    public static final DeferredBlock<Block> BLAST_DOOR = registerBlockWithoutItem("blast_door",
            () -> new BlastDoorBlock(metal()
                    .strength(10.0F, 1000.0F)
                    .noOcclusion()));
    public static final DeferredItem<Item> BLAST_DOOR_ITEM = HbmItems.ITEMS.register(
            "blast_door",
            () -> new BlastDoorBlockItem(BLAST_DOOR.get(), new Item.Properties())
    );
    public static final DeferredBlock<Block> DUMMY_BLOCK_BLAST = registerBlockWithoutItem("dummy_block_blast",
            () -> new BlastDoorDummyBlock(metal()
                    .strength(10.0F, 1000.0F)
                    .noOcclusion()
                    .noLootTable()));
    public static final DeferredBlock<Block> HEAVY_DOOR_PART = registerBlockWithoutItem("heavy_door_part",
            () -> new HbmHeavyDoorPartBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> FIRE_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.FIRE_DOOR);
    public static final DeferredBlock<Block> TRANSITION_SEAL = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.TRANSITION_SEAL);
    public static final DeferredBlock<Block> SLIDING_BLAST_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.SLIDING_BLAST_DOOR);
    public static final DeferredBlock<Block> SLIDING_BLAST_DOOR_2 = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.SLIDING_BLAST_DOOR_2);
    public static final DeferredBlock<Block> SLIDING_GATE_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.SLIDING_GATE_DOOR);
    public static final DeferredBlock<Block> QE_SLIDING = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.QE_SLIDING);
    public static final DeferredBlock<Block> QE_SLIDING_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.QE_SLIDING_DOOR);
    public static final DeferredBlock<Block> QE_CONTAINMENT = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.QE_CONTAINMENT);
    public static final DeferredBlock<Block> SLIDING_SEAL_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.SLIDING_SEAL_DOOR);
    public static final DeferredBlock<Block> SECURE_ACCESS_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.SECURE_ACCESS_DOOR);
    public static final DeferredBlock<Block> ROUND_AIRLOCK_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.ROUND_AIRLOCK_DOOR);
    public static final DeferredBlock<Block> LARGE_VEHICLE_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.LARGE_VEHICLE_DOOR);
    public static final DeferredBlock<Block> VAULT_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.VAULT_DOOR);
    public static final DeferredBlock<Block> WATER_DOOR = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.WATER_DOOR);
    public static final DeferredBlock<Block> SILO_HATCH = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.SILO_HATCH);
    public static final DeferredBlock<Block> SILO_HATCH_LARGE = heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl.SILO_HATCH_LARGE);

    public static final DeferredBlock<Block> HEATER_FIREBOX = heaterBlock("heater_firebox", HeaterBlockEntity.Kind.FIREBOX, LargeMachineBlock.Footprint.centered(1, 1, 1));
    public static final DeferredBlock<Block> HEATER_OVEN = heaterBlock("heater_oven", HeaterBlockEntity.Kind.OVEN, LargeMachineBlock.Footprint.centered(1, 1, 1));
    public static final DeferredBlock<Block> HEATER_OILBURNER = heaterBlock("heater_oilburner", HeaterBlockEntity.Kind.OILBURNER, oilburnerFootprint());
    public static final DeferredBlock<Block> HEATER_ELECTRIC = heaterBlock("heater_electric", HeaterBlockEntity.Kind.ELECTRIC, electricHeaterFootprint(), legacyElectricHeaterFootprint());
    public static final DeferredBlock<Block> HEATER_HEATEX = heaterBlock("heater_heatex", HeaterBlockEntity.Kind.HEATEX, heatexFootprint());
    public static final DeferredBlock<Block> FUSION_HEATER = machineBlock("fusion_heater");
    public static final DeferredBlock<Block> FUSION_HATCH = fusionHatch("fusion_hatch");
    public static final DeferredBlock<Block> FUSION_COMPONENT = fusionComponent("fusion_component");
    public static final DeferredBlock<Block> STRUCT_TORUS_CORE = fusionTorusStruct("struct_torus_core");
    public static final DeferredBlock<Block> FUSION_TORUS = fusionMachine("fusion_torus", FusionMachineBlock.Kind.TORUS);
    public static final DeferredBlock<Block> FUSION_KLYSTRON = fusionMachine("fusion_klystron", FusionMachineBlock.Kind.KLYSTRON);
    public static final DeferredBlock<Block> FUSION_KLYSTRON_CREATIVE = fusionMachine("fusion_klystron_creative", FusionMachineBlock.Kind.KLYSTRON_CREATIVE);
    public static final DeferredBlock<Block> FUSION_BREEDER = fusionMachine("fusion_breeder", FusionMachineBlock.Kind.BREEDER);
    public static final DeferredBlock<Block> FUSION_COLLECTOR = fusionMachine("fusion_collector", FusionMachineBlock.Kind.COLLECTOR);
    public static final DeferredBlock<Block> FUSION_BOILER = fusionMachine("fusion_boiler", FusionMachineBlock.Kind.BOILER);
    public static final DeferredBlock<Block> FUSION_MHDT = fusionMachine("fusion_mhdt", FusionMachineBlock.Kind.MHDT);
    public static final DeferredBlock<Block> FUSION_COUPLER = fusionMachine("fusion_coupler", FusionMachineBlock.Kind.COUPLER);
    public static final DeferredBlock<Block> FUSION_PLASMA_FORGE = fusionMachine("fusion_plasma_forge", FusionMachineBlock.Kind.PLASMA_FORGE);
    public static final DeferredBlock<Block> DFC_CORE = dfcCore("dfc_core");
    public static final DeferredBlock<Block> DFC_EMITTER = dfcComponent("dfc_emitter", DfcComponentBlock.Kind.EMITTER);
    public static final DeferredBlock<Block> DFC_RECEIVER = dfcComponent("dfc_receiver", DfcComponentBlock.Kind.RECEIVER);
    public static final DeferredBlock<Block> DFC_INJECTOR = dfcComponent("dfc_injector", DfcComponentBlock.Kind.INJECTOR);
    public static final DeferredBlock<Block> DFC_STABILIZER = dfcComponent("dfc_stabilizer", DfcComponentBlock.Kind.STABILIZER);
    public static final DeferredBlock<Block> PLASMA_HEATER = machineBlock("plasma_heater");
    public static final DeferredBlock<Block> PRESS_PREHEATER = machineBlock("press_preheater");
    public static final DeferredBlock<Block> RBMK_ABSORBER = rbmkComponent("rbmk_absorber", RbmkComponentBlock.Kind.ABSORBER);
    public static final DeferredBlock<Block> RBMK_AUTOLOADER = rbmkComponent("rbmk_autoloader", RbmkComponentBlock.Kind.AUTOLOADER);
    public static final DeferredBlock<Block> RBMK_BLANK = rbmkComponent("rbmk_blank", RbmkComponentBlock.Kind.BLANK);
    public static final DeferredBlock<Block> RBMK_BOILER = rbmkComponent("rbmk_boiler", RbmkComponentBlock.Kind.BOILER);
    public static final DeferredBlock<Block> RBMK_CONSOLE = rbmkComponent("rbmk_console", RbmkComponentBlock.Kind.CONSOLE);
    public static final DeferredBlock<Block> RBMK_CONTROL = rbmkComponent("rbmk_control", RbmkComponentBlock.Kind.CONTROL);
    public static final DeferredBlock<Block> RBMK_CONTROL_AUTO = rbmkComponent("rbmk_control_auto", RbmkComponentBlock.Kind.CONTROL_AUTO);
    public static final DeferredBlock<Block> RBMK_CONTROL_MOD = rbmkComponent("rbmk_control_mod", RbmkComponentBlock.Kind.CONTROL_MOD);
    public static final DeferredBlock<Block> RBMK_CONTROL_REASIM = rbmkComponent("rbmk_control_reasim", RbmkComponentBlock.Kind.CONTROL_REASIM);
    public static final DeferredBlock<Block> RBMK_CONTROL_REASIM_AUTO = rbmkComponent("rbmk_control_reasim_auto", RbmkComponentBlock.Kind.CONTROL_REASIM_AUTO);
    public static final DeferredBlock<Block> RBMK_COOLER = rbmkComponent("rbmk_cooler", RbmkComponentBlock.Kind.COOLER);
    public static final DeferredBlock<Block> RBMK_CRANE_CONSOLE = rbmkComponent("rbmk_crane_console", RbmkComponentBlock.Kind.CRANE_CONSOLE);
    public static final DeferredBlock<Block> RBMK_DISPLAY = rbmkComponent("rbmk_display", RbmkComponentBlock.Kind.DISPLAY);
    public static final DeferredBlock<Block> RBMK_DISPLAY_BLANK = rbmkComponent("rbmk_display_blank", RbmkComponentBlock.Kind.DISPLAY_BLANK);
    public static final DeferredBlock<Block> RBMK_GAUGE = rbmkComponent("rbmk_gauge", RbmkComponentBlock.Kind.GAUGE);
    public static final DeferredBlock<Block> RBMK_GRAPH = rbmkComponent("rbmk_graph", RbmkComponentBlock.Kind.GRAPH);
    public static final DeferredBlock<Block> RBMK_HEATER = rbmkComponent("rbmk_heater", RbmkComponentBlock.Kind.HEATER);
    public static final DeferredBlock<Block> RBMK_INDICATOR = rbmkComponent("rbmk_indicator", RbmkComponentBlock.Kind.INDICATOR);
    public static final DeferredBlock<Block> RBMK_KEY_PAD = rbmkComponent("rbmk_key_pad", RbmkComponentBlock.Kind.KEY_PAD);
    public static final DeferredBlock<Block> RBMK_LEVER = rbmkComponent("rbmk_lever", RbmkComponentBlock.Kind.LEVER);
    public static final DeferredBlock<Block> RBMK_LOADER = rbmkComponent("rbmk_loader", RbmkComponentBlock.Kind.LOADER);
    public static final DeferredBlock<Block> RBMK_MODERATOR = rbmkComponent("rbmk_moderator", RbmkComponentBlock.Kind.MODERATOR);
    public static final DeferredBlock<Block> RBMK_NUMITRON = rbmkComponent("rbmk_numitron", RbmkComponentBlock.Kind.NUMITRON);
    public static final DeferredBlock<Block> RBMK_OUTGASSER = rbmkComponent("rbmk_outgasser", RbmkComponentBlock.Kind.OUTGASSER);
    public static final DeferredBlock<Block> RBMK_REFLECTOR = rbmkComponent("rbmk_reflector", RbmkComponentBlock.Kind.REFLECTOR);
    public static final DeferredBlock<Block> RBMK_TERMINAL = rbmkComponent("rbmk_terminal", RbmkComponentBlock.Kind.TERMINAL);
    public static final DeferredBlock<Block> RBMK_ROD = rbmkFuelChannelComponent("rbmk_rod", RbmkComponentBlock.Kind.FUEL_ROD);
    public static final DeferredBlock<Block> RBMK_ROD_MOD = rbmkFuelChannelComponent("rbmk_rod_mod", RbmkComponentBlock.Kind.FUEL_ROD_MOD);
    public static final DeferredBlock<Block> RBMK_ROD_REASIM = rbmkFuelChannelComponent("rbmk_rod_reasim", RbmkComponentBlock.Kind.FUEL_ROD_REASIM);
    public static final DeferredBlock<Block> RBMK_ROD_REASIM_MOD = rbmkFuelChannelComponent("rbmk_rod_reasim_mod", RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD);
    public static final DeferredBlock<Block> RBMK_STEAM_INLET = rbmkComponent("rbmk_steam_inlet", RbmkComponentBlock.Kind.STEAM_INLET);
    public static final DeferredBlock<Block> RBMK_STEAM_OUTLET = rbmkComponent("rbmk_steam_outlet", RbmkComponentBlock.Kind.STEAM_OUTLET);
    public static final DeferredBlock<Block> RBMK_STORAGE = rbmkComponent("rbmk_storage", RbmkComponentBlock.Kind.STORAGE);
    public static final DeferredBlock<Block> MACHINE_DUMMY = registerBlock("machine_dummy",
            () -> new MachineDummyBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> ORE_VOLCANO = registerBlock("ore_volcano",
            () -> new FissureBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)
                    .strength(-1.0F, 1_000_000.0F)
                    .lightLevel(state -> 15)
                    .randomTicks()));
    public static final DeferredBlock<Block> VOLCANO_CORE = volcanoCore("volcano_core", false);
    public static final DeferredBlock<Block> VOLCANO_RAD_CORE = volcanoCore("volcano_rad_core", true);
    public static final DeferredBlock<Block> WAND_AIR = registerBlock("wand_air",
            () -> new WandAirBlock(BlockBehaviour.Properties.of()
                    .strength(0.3F, 1.0F)
                    .noOcclusion()
                    .noLootTable()));
    public static final DeferredBlock<Block> WAND_STRUCTURE = wandStructure("wand_structure");
    public static final DeferredBlock<Block> WAND_JIGSAW = registerBlock("wand_jigsaw",
            () -> new WandJigsawBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> WAND_TANDEM = registerBlock("wand_tandem",
            () -> new WandTandemBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> WAND_LOOT = registerBlock("wand_loot",
            () -> new WandLootBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> WAND_LOGIC = registerBlock("wand_logic",
            () -> new WandLogicBlock(metal()
                    .strength(5.0F, 10.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> DECO_LOOT = registerBlock("deco_loot",
            () -> new DecoLootBlock(metal()
                    .strength(1.0F, 5.0F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> LOGIC_BLOCK = registerBlock("logic_block",
            () -> new LogicRuntimeBlock(rock()
                    .strength(1.5F, 6.0F)
                    .noOcclusion(), false));
    public static final DeferredBlock<Block> LOGIC_BLOCK_INVIS = registerBlockWithoutItem("logic_block_invis",
            () -> new LogicRuntimeBlock(rock()
                    .strength(1.5F, 6.0F)
                    .noOcclusion()
                    .noLootTable(), true));

    public static final List<DeferredBlock<Block>> POWER_BLOCKS = List.of(
            RED_CABLE,
            RED_CABLE_CLASSIC,
            RED_CABLE_BOX,
            RED_CABLE_PAINTABLE,
            RED_CABLE_GAUGE,
            RED_WIRE_COATED,
            CABLE_DIODE,
            CABLE_SWITCH,
            CABLE_DETECTOR,
            RED_CONNECTOR,
            CONNECTOR_RED_SUPER,
            RED_PYLON,
            RED_PYLON_LARGE,
            SUBSTATION,
            MACHINE_BATTERY_REDD,
            CAPACITOR_COPPER,
            MACHINE_WOOD_BURNER,
            MACHINE_STIRLING,
            MACHINE_STIRLING_STEEL,
            MACHINE_STIRLING_CREATIVE,
            MACHINE_STEAM_ENGINE,
            MACHINE_TURBINE,
            MACHINE_INDUSTRIAL_TURBINE,
            MACHINE_CHUNGUS,
            MACHINE_TURBINEGAS
    );

    public static final List<DeferredBlock<Block>> FLUID_BLOCKS = List.of(
            FLUID_DUCT_MK2,
            FLUID_DUCT_NEO,
            FLUID_DUCT_BOX,
            FLUID_DUCT_EXHAUST,
            FLUID_DUCT_GAUGE,
            FLUID_DUCT_PAINTABLE,
            FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST,
            FLUID_DUCT_SOLID,
            FLUID_DUCT_SOLID_SEALED,
            FLUID_VALVE,
            FLUID_SWITCH,
            FLUID_COUNTER_VALVE,
            FLUID_PUMP,
            MACHINE_DRAIN,
            REFUELER
    );

    public static final List<DeferredBlock<Block>> CONTAINER_BLOCKS = List.of(
            BARREL_PLASTIC,
            BARREL_STEEL,
            BARREL_TCALLOY,
            BARREL_ANTIMATTER,
            RED_BARREL,
            PINK_BARREL,
            LOX_BARREL,
            TAINT_BARREL,
            YELLOW_BARREL,
            VITRIFIED_BARREL,
            MACHINE_FLUIDTANK,
            MACHINE_BAT9000,
            MACHINE_BIGASSTANK,
            CRATE,
            CRATE_WEAPON,
            CRATE_LEAD,
            CRATE_METAL,
            CRATE_RED,
            CRATE_CAN,
            CRATE_JUNGLE,
            CRATE_AMMO,
            CRATE_IRON,
            CRATE_STEEL,
            CRATE_DESH,
            CRATE_TEMPLATE,
            CRATE_TUNGSTEN,
            SAFE,
            MASS_STORAGE_WOOD,
            MASS_STORAGE_IRON,
            MASS_STORAGE_DESH,
            MASS_STORAGE,
            MACHINE_STORAGE_DRUM
    );

    // Declare these holders before the creative-tab lists that reference them.
    public static final DeferredBlock<Block> SPOTLIGHT_BEAM = registerBlockWithoutItem("spotlight_beam",
            () -> new SpotlightBeamBlock(rock().strength(-1.0F, 1_000_000.0F)
                    .noOcclusion().lightLevel(state -> 15)));
    public static final DeferredBlock<Block> SPOTLIGHT_INCANDESCENT = registerObjBlock("spotlight_incandescent",
            () -> new SpotlightBlock(metal().strength(0.5F, 10.0F).noOcclusion(), SpotlightBlock.Kind.INCANDESCENT, true));
    public static final DeferredBlock<Block> SPOTLIGHT_INCANDESCENT_OFF = registerObjBlock("spotlight_incandescent_off",
            () -> new SpotlightBlock(metal().strength(0.5F, 10.0F).noOcclusion(), SpotlightBlock.Kind.INCANDESCENT, false));
    public static final DeferredBlock<Block> SPOTLIGHT_FLUORO = registerObjBlock("spotlight_fluoro",
            () -> new SpotlightBlock(metal().strength(0.5F, 10.0F).noOcclusion(), SpotlightBlock.Kind.FLUORESCENT, true));
    public static final DeferredBlock<Block> SPOTLIGHT_FLUORO_OFF = registerObjBlock("spotlight_fluoro_off",
            () -> new SpotlightBlock(metal().strength(0.5F, 10.0F).noOcclusion(), SpotlightBlock.Kind.FLUORESCENT, false));
    public static final DeferredBlock<Block> SPOTLIGHT_HALOGEN = registerObjBlock("spotlight_halogen",
            () -> new SpotlightBlock(metal().strength(0.5F, 10.0F).noOcclusion(), SpotlightBlock.Kind.HALOGEN, true));
    public static final DeferredBlock<Block> SPOTLIGHT_HALOGEN_OFF = registerObjBlock("spotlight_halogen_off",
            () -> new SpotlightBlock(metal().strength(0.5F, 10.0F).noOcclusion(), SpotlightBlock.Kind.HALOGEN, false));

    public static final List<DeferredBlock<Block>> THERMAL_BLOCKS = List.of(
            MACHINE_ELECTRIC_FURNACE_OFF,
            MACHINE_HEPHAESTUS,
            CHIMNEY_BRICK,
            CHIMNEY_INDUSTRIAL,
            HEATER_FIREBOX,
            HEATER_OVEN,
            HEATER_OILBURNER,
            HEATER_ELECTRIC,
            HEATER_HEATEX,
            MACHINE_ASHPIT,
            SPOTLIGHT_INCANDESCENT,
            SPOTLIGHT_FLUORO,
            SPOTLIGHT_HALOGEN,
            DECON,
            MACHINE_BOILER,
            MACHINE_INDUSTRIAL_BOILER,
            MACHINE_SOLAR_BOILER,
            SOLAR_MIRROR,
            MACHINE_CONDENSER,
            MACHINE_CONDENSER_POWERED,
            MACHINE_TOWER_SMALL,
            MACHINE_TOWER_LARGE,
            MACHINE_BLAST_FURNACE,
            FURNACE_COMBINATION,
            MACHINE_FURNACE_BRICK_OFF,
            FURNACE_IRON,
            FURNACE_STEEL,
            FUSION_HEATER,
            PLASMA_HEATER,
            PRESS_PREHEATER,
            RBMK_HEATER
    );

    public static final List<DeferredBlock<Block>> RBMK_BLOCKS = List.of(
            RBMK_BLANK,
            RBMK_ROD,
            RBMK_ROD_MOD,
            RBMK_ROD_REASIM,
            RBMK_ROD_REASIM_MOD,
            RBMK_CONTROL,
            RBMK_CONTROL_AUTO,
            RBMK_CONTROL_MOD,
            RBMK_CONTROL_REASIM,
            RBMK_CONTROL_REASIM_AUTO,
            RBMK_BOILER,
            RBMK_HEATER,
            RBMK_COOLER,
            RBMK_LOADER,
            RBMK_STEAM_INLET,
            RBMK_STEAM_OUTLET,
            RBMK_OUTGASSER,
            RBMK_STORAGE,
            RBMK_MODERATOR,
            RBMK_REFLECTOR,
            RBMK_ABSORBER,
            RBMK_AUTOLOADER,
            RBMK_CONSOLE,
            RBMK_CRANE_CONSOLE,
            RBMK_DISPLAY,
            RBMK_DISPLAY_BLANK,
            RBMK_GAUGE,
            RBMK_GRAPH,
            RBMK_INDICATOR,
            RBMK_KEY_PAD,
            RBMK_LEVER,
            RBMK_NUMITRON,
            RBMK_TERMINAL
    );

    public static final List<DeferredBlock<Block>> MACHINE_BLOCKS = List.of(
            VENT_CHLORINE,
            VENT_CLOUD,
            VENT_PINK_CLOUD,
            VENT_CHLORINE_SEAL,
            CHLORINE_GAS,
            GAS_RADON,
            GAS_RADON_DENSE,
            GAS_RADON_TOMB,
            GAS_MELTDOWN,
            GAS_MONOXIDE,
            GAS_ASBESTOS,
            GAS_COAL,
            GAS_FLAMMABLE,
            GAS_EXPLOSIVE,
            RAD_ABSORBER,
            GEIGER,
            TESLA,
            MACHINE_SHREDDER,
            MACHINE_MICROWAVE,
            MACHINE_ARMOR_TABLE,
            MACHINE_WEAPON_TABLE,
            MACHINE_ASSEMBLY_FACTORY,
            MACHINE_ASSEMBLY_MACHINE,
            MACHINE_CHEMICAL_FACTORY,
            MACHINE_CHEMICAL_PLANT,
            MACHINE_SOLDERING_STATION,
            MACHINE_ARC_WELDER,
            MACHINE_ARC_FURNACE,
            MACHINE_COMPRESSOR,
            MACHINE_COMPRESSOR_COMPACT,
            MACHINE_MIXER,
            MACHINE_FUNNEL,
            MACHINE_SIREN,
            MACHINE_AMMO_PRESS,
            MACHINE_PRESS,
            MACHINE_EPRESS,
            MACHINE_CENTRIFUGE,
            MACHINE_GASCENT,
            MACHINE_CYCLOTRON,
            MACHINE_EXPOSURE_CHAMBER,
            MACHINE_DEUTERIUM_EXTRACTOR,
            MACHINE_DEUTERIUM_TOWER,
            PA_SOURCE,
            PA_BEAMLINE,
            PA_RFC,
            PA_QUADRUPOLE,
            PA_DIPOLE,
            PA_DETECTOR,
            MACHINE_INTAKE,
            PUMP_STEAM,
            PUMP_ELECTRIC,
            MACHINE_ROTARY_FURNACE,
            MACHINE_ELECTROLYSER,
            HADRON_COIL_ALLOY,
            HADRON_COIL_GOLD,
            HADRON_COIL_NEODYMIUM,
            HADRON_COIL_MAGTUNG,
            HADRON_COIL_SCHRABIDIUM,
            HADRON_COIL_SCHRABIDATE,
            HADRON_COIL_STARMETAL,
            HADRON_COIL_CHLOROPHYTE,
            HADRON_COIL_MESE,
            MACHINE_WASTE_DRUM,
            MACHINE_PUREX,
            MACHINE_ICF_PRESS,
            MACHINE_TRANSFORMER
            , PIPE_ANCHOR
            , PISTON_INSERTER
            , MACHINE_ANNIHILATOR
            , MACHINE_AUTOCRAFTER
            , MACHINE_AUTOSAW
            , MACHINE_THRESHER
            , MACHINE_LPW2
            , MACHINE_CONVEYOR_PRESS
            , MACHINE_ORBUS
            , MACHINE_PRECASS
            , MACHINE_PYROOVEN
            , MACHINE_RADGEN
            , MACHINE_RADIOLYSIS
            , MACHINE_RTG_GREY
            , MACHINE_SAWMILL
            , MACHINE_TELEPORTER
            , TELEANCHOR
            , MACHINE_TURBOFAN
            , FAN
            , FLOODLIGHT
            , CARGO_ELEVATOR
            , BROADCASTER_PC
            , LAMP_DEMON
            , RADIOBOX
            , RADIOREC
            , RADIO_AUTOCAL
            , RADIO_TORCH_SENDER
            , RADIO_TORCH_RECEIVER
            , RADIO_TORCH_COUNTER
            , RADIO_TORCH_LOGIC
            , RADIO_TORCH_READER
            , RADIO_TORCH_CONTROLLER
            , RADIO_TELEX
    );

    public static final List<DeferredBlock<Block>> MINING_PROCESSING_BLOCKS = List.of(
            MACHINE_EXCAVATOR,
            MACHINE_MINING_LASER,
            MACHINE_CRYSTALLIZER,
            MACHINE_ORE_SLOPPER,
            MACHINE_SILEX,
            MACHINE_FEL
    );

    public static final List<DeferredBlock<Block>> PETROLEUM_BLOCKS = List.of(
            ORE_OIL,
            ORE_DEEPSLATE_OIL,
            ORE_OIL_EMPTY,
            ORE_DEEPSLATE_OIL_EMPTY,
            ORE_OIL_SAND,
            ORE_BEDROCK_OIL,
            OIL_PIPE,
            MACHINE_WELL,
            MACHINE_PUMPJACK,
            MACHINE_FRACKING_TOWER,
            MACHINE_REFINERY,
            MACHINE_VACUUM_DISTILL,
            MACHINE_COKER,
            MACHINE_FLARE,
            MACHINE_SOLIDIFIER,
            MACHINE_LIQUEFACTOR,
            MACHINE_FRACTION_TOWER,
            FRACTION_SPACER,
            MACHINE_CATALYTIC_CRACKER,
            MACHINE_CATALYTIC_REFORMER,
            MACHINE_HYDROTREATER
    );

    public static final List<DeferredBlock<Block>> FOUNDRY_BLOCKS = List.of(
            MACHINE_CRUCIBLE,
            FOUNDRY_MOLD,
            FOUNDRY_BASIN,
            FOUNDRY_CHANNEL,
            FOUNDRY_TANK,
            FOUNDRY_OUTLET,
            FOUNDRY_SLAGTAP,
            MACHINE_STRAND_CASTER
    );

    public static final List<DeferredBlock<Block>> REACTOR_BLOCKS = List.of(
            MACHINE_REACTOR_SMALL,
            MACHINE_CONTROLLER,
            MACHINE_REACTOR_BREEDING,
            STRUCT_WATZ_CORE,
            WATZ,
            WATZ_END,
            WATZ_CASING,
            WATZ_COOLER,
            WATZ_ELEMENT,
            WATZ_PUMP,
            MACHINE_ZIRNOX,
            PWR_BLOCK,
            PWR_CASING,
            PWR_CHANNEL,
            PWR_CONTROL,
            PWR_CONTROLLER,
            PWR_FUELROD,
            PWR_HEATEX,
            PWR_HEATSINK,
            PWR_NEUTRON_SOURCE,
            PWR_PORT,
            PWR_REFLECTOR,
            STRUCT_ICF_CORE,
            ICF,
            ICF_COMPONENT,
            ICF_CONTROLLER,
            ICF_LASER_COMPONENT,
            ICF_BLOCK,
            STRUCT_TORUS_CORE,
            FUSION_HATCH,
            FUSION_COMPONENT,
            FUSION_TORUS,
            FUSION_KLYSTRON,
            FUSION_KLYSTRON_CREATIVE,
            FUSION_BREEDER,
            FUSION_COLLECTOR,
            FUSION_BOILER,
            FUSION_MHDT,
            FUSION_COUPLER,
            FUSION_PLASMA_FORGE,
            DFC_CORE,
            DFC_EMITTER,
            DFC_RECEIVER,
            DFC_INJECTOR,
            DFC_STABILIZER
    );

    public static final List<DeferredBlock<Block>> TURRET_BLOCKS = List.of(
            TURRET_JEREMY,
            TURRET_CHEKHOV,
            TURRET_FRIENDLY,
            TURRET_FRITZ,
            TURRET_HOWARD,
            TURRET_HOWARD_DAMAGED,
            TURRET_MAXWELL,
            TURRET_RICHARD,
            TURRET_TAUON,
            TURRET_ARTY,
            TURRET_HIMARS,
            TURRET_SENTRY,
            TURRET_SENTRY_DAMAGED
    );

    public static final List<DeferredBlock<Block>> NUCLEAR_WEAPON_BLOCKS = List.of(
            NUKE_BOY,
            NUKE_CUSTOM,
            NUKE_FLEIJA,
            NUKE_FSTBMB,
            NUKE_GADGET,
            NUKE_MAN,
            NUKE_MIKE,
            NUKE_N2,
            NUKE_PROTOTYPE,
            NUKE_SOLINIUM,
            NUKE_TSAR,
            BOMB_MULTI,
            CRASHED_BOMB,
            VOLCANO_CORE,
            VOLCANO_RAD_CORE,
            DYNAMITE,
            DET_CORD,
            DET_CHARGE,
            DET_NUKE,
            DET_MINER,
            TNT_NTM,
            SEMTEX,
            C4,
            CHARGE_DYNAMITE,
            CHARGE_MINER,
            CHARGE_C4,
            CHARGE_SEMTEX,
            FISSURE_BOMB,
            FIREWORKS,
            MINE_AP,
            MINE_HE,
            MINE_SHRAP,
            MINE_FAT,
            MINE_NAVAL
    );

    public static final List<DeferredBlock<Block>> ROCKET_MISSILE_BLOCKS = List.of(
            STRUCT_LAUNCHER,
            STRUCT_SCAFFOLD,
            STRUCT_LAUNCHER_CORE,
            STRUCT_LAUNCHER_CORE_LARGE,
            STRUCT_SOYUZ_CORE,
            COMPACT_LAUNCHER,
            LAUNCH_TABLE,
            LAUNCH_PAD,
            LAUNCH_PAD_LARGE,
            LAUNCH_PAD_RUSTED,
            SOYUZ_LAUNCHER,
            SOYUZ_CAPSULE,
            SAT_DOCK,
            CRATE_SUPPLY,
            MACHINE_FORCEFIELD,
            MACHINE_MISSILE_ASSEMBLY,
            RADAR_SCREEN,
            MACHINE_RADAR,
            MACHINE_RADAR_LARGE,
            MACHINE_SATLINKER
    );

    public static final List<DeferredBlock<Block>> ANVIL_BLOCKS = List.of(
            ANVIL_IRON,
            ANVIL_LEAD,
            ANVIL_STEEL,
            ANVIL_DESH,
            ANVIL_SATURNITE,
            ANVIL_FERROURANIUM,
            ANVIL_BISMUTH_BRONZE,
            ANVIL_ARSENIC_BRONZE,
            ANVIL_SCHRABIDATE,
            ANVIL_DNT,
            ANVIL_OSMIRIDIUM,
            ANVIL_MURKY
    );

    public static final List<DeferredBlock<Block>> DOOR_BLOCKS = List.of(
            DOOR_METAL,
            DOOR_OFFICE,
            DOOR_BUNKER,
            DOOR_RED,
            TRAPDOOR_STEEL,
            BLAST_DOOR,
            FIRE_DOOR,
            TRANSITION_SEAL,
            SLIDING_BLAST_DOOR,
            SLIDING_BLAST_DOOR_2,
            SLIDING_GATE_DOOR,
            QE_SLIDING,
            QE_SLIDING_DOOR,
            QE_CONTAINMENT,
            SLIDING_SEAL_DOOR,
            SECURE_ACCESS_DOOR,
            ROUND_AIRLOCK_DOOR,
            LARGE_VEHICLE_DOOR,
            VAULT_DOOR,
            WATER_DOOR,
            SILO_HATCH,
            SILO_HATCH_LARGE,
            SEAL_CONTROLLER,
            SEAL_FRAME,
            SEAL_HATCH
    );

    public static final Map<com.reinhardt.hbm.door.HbmDoorDecl, DeferredBlock<Block>> HEAVY_DOOR_BLOCKS = Map.ofEntries(
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.FIRE_DOOR, FIRE_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.TRANSITION_SEAL, TRANSITION_SEAL),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.SLIDING_BLAST_DOOR, SLIDING_BLAST_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.SLIDING_BLAST_DOOR_2, SLIDING_BLAST_DOOR_2),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.SLIDING_GATE_DOOR, SLIDING_GATE_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.QE_SLIDING, QE_SLIDING),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.QE_SLIDING_DOOR, QE_SLIDING_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.QE_CONTAINMENT, QE_CONTAINMENT),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.SLIDING_SEAL_DOOR, SLIDING_SEAL_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.SECURE_ACCESS_DOOR, SECURE_ACCESS_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.ROUND_AIRLOCK_DOOR, ROUND_AIRLOCK_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.LARGE_VEHICLE_DOOR, LARGE_VEHICLE_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.VAULT_DOOR, VAULT_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.WATER_DOOR, WATER_DOOR),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.SILO_HATCH, SILO_HATCH),
            Map.entry(com.reinhardt.hbm.door.HbmDoorDecl.SILO_HATCH_LARGE, SILO_HATCH_LARGE)
    );

    public static final List<DeferredBlock<Block>> OVERWORLD_ORES = List.of(
            ORE_URANIUM,
            ORE_URANIUM_SCORCHED,
            ORE_THORIUM,
            ORE_TITANIUM,
            ORE_SULFUR,
            ORE_NITER,
            ORE_TUNGSTEN,
            ORE_ALUMINIUM,
            ORE_FLUORITE,
            ORE_LEAD,
            ORE_BERYLLIUM,
            ORE_LIGNITE,
            ORE_ASBESTOS,
            ORE_OIL,
            ORE_OIL_EMPTY,
            ORE_COAL_OIL,
            ORE_OIL_SAND,
            ORE_BEDROCK_OIL,
            ORE_BEDROCK_BLOCK,
            ORE_BEDROCK,
            ORE_BEDROCK_COLTAN,
            ORE_RARE,
            ORE_COBALT,
            ORE_CINNABAR,
            ORE_CINNEBAR,
            ORE_COPPER,
            ORE_ALEXANDRITE,
            ORE_AUSTRALIUM,
            ORE_DEPTH_CINNEBAR,
            ORE_DEPTH_BORAX,
            ORE_DEPTH_ZIRCONIUM,
            ORE_COLTAN,
            ORE_SCHRABIDIUM,
            ORE_METEOR,
            ORE_BASALT
    );

    public static final List<DeferredBlock<Block>> SCHIST_ORES = List.of(
            STONE_GNEISS,
            ORE_GNEISS_IRON,
            ORE_GNEISS_GOLD,
            ORE_GNEISS_URANIUM,
            ORE_GNEISS_URANIUM_SCORCHED,
            ORE_GNEISS_COPPER,
            ORE_GNEISS_ASBESTOS,
            ORE_GNEISS_LITHIUM,
            ORE_GNEISS_SCHRABIDIUM,
            ORE_GNEISS_RARE,
            ORE_GNEISS_GAS,
            GNEISS_BRICK,
            GNEISS_TILE,
            GNEISS_CHISELED
    );

    public static final List<DeferredBlock<Block>> NETHER_ORES = List.of(
            ORE_NETHER_COAL,
            ORE_NETHER_SMOLDERING,
            ORE_NETHER_URANIUM,
            ORE_NETHER_URANIUM_SCORCHED,
            ORE_NETHER_PLUTONIUM,
            ORE_NETHER_TUNGSTEN,
            ORE_NETHER_SULFUR,
            ORE_NETHER_FIRE,
            ORE_NETHER_COBALT,
            ORE_NETHER_SCHRABIDIUM,
            ORE_DEPTH_NETHER_NEODYMIUM,
            ORE_DEPTH_NETHER_NITAN
    );

    public static final List<DeferredBlock<Block>> DEEPSLATE_ORES = List.of(
            ORE_DEEPSLATE_URANIUM,
            ORE_DEEPSLATE_URANIUM_SCORCHED,
            ORE_DEEPSLATE_THORIUM,
            ORE_DEEPSLATE_TITANIUM,
            ORE_DEEPSLATE_SULFUR,
            ORE_DEEPSLATE_NITER,
            ORE_DEEPSLATE_TUNGSTEN,
            ORE_DEEPSLATE_ALUMINIUM,
            ORE_DEEPSLATE_FLUORITE,
            ORE_DEEPSLATE_LEAD,
            ORE_DEEPSLATE_BERYLLIUM,
            ORE_DEEPSLATE_LIGNITE,
            ORE_DEEPSLATE_ASBESTOS,
            ORE_DEEPSLATE_OIL,
            ORE_DEEPSLATE_OIL_EMPTY,
            ORE_DEEPSLATE_RARE,
            ORE_DEEPSLATE_COBALT,
            ORE_DEEPSLATE_CINNABAR,
            ORE_DEEPSLATE_COLTAN,
            ORE_DEEPSLATE_SCHRABIDIUM
    );

    public static final List<DeferredBlock<Block>> ORE_CLUSTERS = List.of(
            CLUSTER_IRON,
            CLUSTER_TITANIUM,
            CLUSTER_ALUMINIUM,
            CLUSTER_COPPER,
            CLUSTER_DEPTH_IRON,
            CLUSTER_DEPTH_TITANIUM,
            CLUSTER_DEPTH_TUNGSTEN
    );

    public static final List<DeferredBlock<Block>> OIL_FIELD_BLOCKS = List.of(
            DIRT_DEAD,
            DIRT_OILY,
            SAND_DIRTY,
            SAND_DIRTY_RED,
            STONE_CRACKED,
            STONE_POROUS,
            STONE_DEPTH,
            STONE_DEPTH_NETHER,
            OIL_SPILL,
            PLANT_DEAD,
            PLANT_FLOWER,
            PLANT_TALL
    );

    public static final DeferredBlock<Block> ASPHALT = registerBlock("asphalt",
            () -> new SpeedyBlock(rock().strength(15.0F, 120.0F), 1.5D));
    public static final DeferredBlock<Block> ASPHALT_LIGHT = registerBlock("asphalt_light",
            () -> new SpeedyBlock(rock().strength(15.0F, 120.0F).lightLevel(state -> 1), 1.5D));
    public static final DeferredBlock<Block> ASPHALT_STAIRS = registerBlock("asphalt_stairs",
            () -> new SpeedyStairsBlock(ASPHALT.get().defaultBlockState(), rock().strength(15.0F, 120.0F), 1.5D));

    public static final DeferredBlock<Block> BASALT = registerBlock("basalt",
            () -> new RotatedPillarBlock(rock().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> BASALT_SMOOTH = registerBlock("basalt_smooth",
            () -> new Block(rock().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> BASALT_BRICK = registerBlock("basalt_brick",
            () -> new Block(rock().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> BASALT_POLISHED = registerBlock("basalt_polished",
            () -> new Block(rock().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> BASALT_TILES = registerBlock("basalt_tiles",
            () -> new Block(rock().strength(5.0F, 10.0F)));

    public static final DeferredBlock<Block> BRICK_RED = registerBlock("brick_red",
            () -> new RedBrickBlock(rock().strength(0.0F, 10_000.0F)));
    public static final DeferredBlock<Block> BRICK_FORGOTTEN = registerBlock("brick_forgotten",
            () -> new LegacyPillarBlock(rock().strength(-1.0F, 666_666.0F)));
    public static final DeferredBlock<Block> BRICK_FORGOTTEN_LOCK = registerBlock("brick_forgotten_lock",
            () -> new ForgottenLockBlock(rock().strength(-1.0F, 666_666.0F)));
    public static final DeferredBlock<Block> WOOD_BARRIER = registerBlock("wood_barrier",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(5.0F, 15.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> WOOD_STRUCTURE = registerVariantBlock("wood_structure",
            () -> new WoodStructureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(5.0F, 15.0F).sound(SoundType.WOOD).noOcclusion()),
            WoodStructureBlock.TYPE, "block.reinhardtshbm.wood_structure", "roof", "scaffold", "ceiling");
    public static final DeferredBlock<Block> BRICK_SLAB = legacyVariantSlab("brick_slab", LegacyVariantStrengths.BRICK_SLAB);
    public static final DeferredBlock<Block> BRICK_DOUBLE_SLAB = legacyVariantBlock("brick_double_slab", LegacyVariantStrengths.BRICK_SLAB);
    public static final DeferredBlock<Block> CONCRETE_BRICK_SLAB = legacyVariantSlab("concrete_brick_slab", LegacyVariantStrengths.CONCRETE_BRICK_SLAB);
    public static final DeferredBlock<Block> CONCRETE_BRICK_DOUBLE_SLAB = legacyVariantBlock("concrete_brick_double_slab", LegacyVariantStrengths.CONCRETE_BRICK_SLAB);
    public static final DeferredBlock<Block> CONCRETE_SLAB = legacyVariantSlab("concrete_slab", LegacyVariantStrengths.CONCRETE_SLAB);
    public static final DeferredBlock<Block> CONCRETE_DOUBLE_SLAB = legacyVariantBlock("concrete_double_slab", LegacyVariantStrengths.CONCRETE_SLAB);
    public static final DeferredBlock<Block> BRICK_CONCRETE = registerBlock("brick_concrete",
            () -> new Block(concrete().strength(15.0F, 160.0F)));
    public static final DeferredBlock<Block> BRICK_CONCRETE_MOSSY = registerBlock("brick_concrete_mossy",
            () -> new Block(concrete().strength(15.0F, 160.0F)));
    public static final DeferredBlock<Block> BRICK_CONCRETE_CRACKED = registerBlock("brick_concrete_cracked",
            () -> new Block(concrete().strength(15.0F, 60.0F)));
    public static final DeferredBlock<Block> BRICK_CONCRETE_BROKEN = registerBlock("brick_concrete_broken",
            () -> new Block(concrete().strength(15.0F, 45.0F)));
    public static final DeferredBlock<Block> BRICK_CONCRETE_MARKED = registerBlock("brick_concrete_marked",
            () -> new Block(concrete().strength(15.0F, 160.0F)));
    public static final DeferredBlock<Block> DECO_STEEL = registerBlock("deco_steel",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> DECO_TITANIUM = registerBlock("deco_titanium",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> DECO_RED_COPPER = registerBlock("deco_red_copper",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> DECO_TUNGSTEN = registerBlock("deco_tungsten",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> DECO_ALUMINIUM = registerBlock("deco_aluminium",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> DECO_RUSTY_STEEL = registerBlock("deco_rusty_steel",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> DECO_LEAD = registerBlock("deco_lead",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> DECO_BERYLLIUM = registerBlock("deco_beryllium",
            () -> new Block(metal().strength(5.0F, 10.0F)));
    public static final DeferredBlock<Block> DECO_ASBESTOS = registerBlock("deco_asbestos",
            () -> new AsbestosDecoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DECO_RBMK = registerBlock("deco_rbmk",
            () -> new Block(metal().strength(5.0F, 100.0F)));
    public static final DeferredBlock<Block> DECO_RBMK_SMOOTH = registerBlock("deco_rbmk_smooth",
            () -> new Block(metal().strength(5.0F, 100.0F)));
    public static final DeferredBlock<Block> DECO_PIPE = decorativePipe("deco_pipe");
    public static final DeferredBlock<Block> DECO_PIPE_RUSTED = decorativePipe("deco_pipe_rusted");
    public static final DeferredBlock<Block> DECO_PIPE_GREEN = decorativePipe("deco_pipe_green");
    public static final DeferredBlock<Block> DECO_PIPE_GREEN_RUSTED = decorativePipe("deco_pipe_green_rusted");
    public static final DeferredBlock<Block> DECO_PIPE_RED = decorativePipe("deco_pipe_red");
    public static final DeferredBlock<Block> DECO_PIPE_MARKED = decorativePipe("deco_pipe_marked");
    public static final DeferredBlock<Block> DECO_PIPE_RIM = decorativePipe("deco_pipe_rim");
    public static final DeferredBlock<Block> DECO_PIPE_RIM_RUSTED = decorativePipe("deco_pipe_rim_rusted");
    public static final DeferredBlock<Block> DECO_PIPE_RIM_GREEN = decorativePipe("deco_pipe_rim_green");
    public static final DeferredBlock<Block> DECO_PIPE_RIM_GREEN_RUSTED = decorativePipe("deco_pipe_rim_green_rusted");
    public static final DeferredBlock<Block> DECO_PIPE_RIM_RED = decorativePipe("deco_pipe_rim_red");
    public static final DeferredBlock<Block> DECO_PIPE_RIM_MARKED = decorativePipe("deco_pipe_rim_marked");
    public static final DeferredBlock<Block> DECO_PIPE_FRAMED = decorativePipe("deco_pipe_framed");
    public static final DeferredBlock<Block> DECO_PIPE_FRAMED_RUSTED = decorativePipe("deco_pipe_framed_rusted");
    public static final DeferredBlock<Block> DECO_PIPE_FRAMED_GREEN = decorativePipe("deco_pipe_framed_green");
    public static final DeferredBlock<Block> DECO_PIPE_FRAMED_GREEN_RUSTED = decorativePipe("deco_pipe_framed_green_rusted");
    public static final DeferredBlock<Block> DECO_PIPE_FRAMED_RED = decorativePipe("deco_pipe_framed_red");
    public static final DeferredBlock<Block> DECO_PIPE_FRAMED_MARKED = decorativePipe("deco_pipe_framed_marked");
    public static final DeferredBlock<Block> DECO_PIPE_QUAD = decorativePipe("deco_pipe_quad");
    public static final DeferredBlock<Block> DECO_PIPE_QUAD_RUSTED = decorativePipe("deco_pipe_quad_rusted");
    public static final DeferredBlock<Block> DECO_PIPE_QUAD_GREEN = decorativePipe("deco_pipe_quad_green");
    public static final DeferredBlock<Block> DECO_PIPE_QUAD_GREEN_RUSTED = decorativePipe("deco_pipe_quad_green_rusted");
    public static final DeferredBlock<Block> DECO_PIPE_QUAD_RED = decorativePipe("deco_pipe_quad_red");
    public static final DeferredBlock<Block> DECO_PIPE_QUAD_MARKED = decorativePipe("deco_pipe_quad_marked");
    public static final DeferredBlock<Block> REINFORCED_STONE = registerBlock("reinforced_stone",
            () -> new Block(concrete().strength(15.0F, 100.0F)));
    public static final DeferredBlock<Block> REINFORCED_BRICK = registerBlock("reinforced_brick",
            () -> new Block(concrete().strength(15.0F, 300.0F)));
    public static final DeferredBlock<Block> CONCRETE = registerBlock("concrete",
            () -> new Block(concrete().strength(15.0F, 140.0F)));
    public static final DeferredBlock<Block> CONCRETE_SMOOTH = registerBlock("concrete_smooth",
            () -> new Block(concrete().strength(15.0F, 140.0F)));
    public static final DeferredBlock<Block> CONCRETE_ASBESTOS = registerBlock("concrete_asbestos",
            () -> new Block(concrete().strength(15.0F, 150.0F)));
    public static final DeferredBlock<Block> CONCRETE_REBAR = registerBlock("concrete_rebar",
            () -> new Block(concrete().strength(50.0F, 240.0F)));
    public static final DeferredBlock<Block> REBAR = registerBlock("rebar",
            () -> new RebarBlock(metal().strength(15.0F, 20.0F)));
    public static final DeferredBlock<Block> CONCRETE_SUPER = registerBlock("concrete_super",
            () -> new Block(concrete().strength(150.0F, 1000.0F)));
    public static final DeferredBlock<Block> CONCRETE_SUPER_BROKEN = registerBlock("concrete_super_broken",
            () -> new HbmFallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(10.0F, 20.0F)
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> CONCRETE_PILLAR = registerBlock("concrete_pillar",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(concrete().strength(15.0F, 180.0F)));
    public static final DeferredBlock<Block> CONCRETE_COLORED = concreteColored("concrete_colored");
    public static final DeferredBlock<Block> CONCRETE_COLORED_EXT = concreteColoredExt("concrete_colored_ext");
    public static final DeferredBlock<Block> BRICK_FIRE = registerBlock("brick_fire",
            () -> new Block(rock().strength(5.0F, 35.0F)));
    public static final DeferredBlock<Block> BRICK_FIRE_DOUBLE_SLAB = registerBlock("brick_fire_double_slab",
            () -> new Block(rock().strength(15.0F, 35.0F)));
    public static final DeferredBlock<Block> BRICK_FIRE_SLAB = registerBlock("brick_fire_slab",
            () -> new SlabBlock(rock().strength(15.0F, 35.0F)));
    public static final DeferredBlock<Block> BRICK_FIRE_STAIRS = registerBlock("brick_fire_stairs",
            () -> new StairBlock(BRICK_FIRE.get().defaultBlockState(), rock().strength(15.0F, 35.0F)));
    public static final DeferredBlock<Block> BRICK_CONCRETE_STAIRS = stairs("brick_concrete_stairs", BRICK_CONCRETE,
            concrete().strength(15.0F, 160.0F));
    public static final DeferredBlock<Block> BRICK_CONCRETE_MOSSY_STAIRS = stairs("brick_concrete_mossy_stairs", BRICK_CONCRETE_MOSSY,
            concrete().strength(15.0F, 160.0F));
    public static final DeferredBlock<Block> BRICK_CONCRETE_CRACKED_STAIRS = stairs("brick_concrete_cracked_stairs", BRICK_CONCRETE_CRACKED,
            concrete().strength(15.0F, 60.0F));
    public static final DeferredBlock<Block> BRICK_CONCRETE_BROKEN_STAIRS = stairs("brick_concrete_broken_stairs", BRICK_CONCRETE_BROKEN,
            concrete().strength(15.0F, 45.0F));
    public static final DeferredBlock<Block> BRICK_OBSIDIAN = registerBlock("brick_obsidian",
            () -> new Block(concrete().strength(15.0F, 120.0F)));
    public static final DeferredBlock<Block> BRICK_OBSIDIAN_STAIRS = stairs("brick_obsidian_stairs", BRICK_OBSIDIAN,
            concrete().strength(15.0F, 120.0F));
    public static final DeferredBlock<Block> BRICK_LIGHT = registerBlock("brick_light",
            () -> new Block(concrete().strength(5.0F, 20.0F)));
    public static final DeferredBlock<Block> BRICK_LIGHT_STAIRS = stairs("brick_light_stairs", BRICK_LIGHT,
            concrete().strength(5.0F, 20.0F));
    public static final DeferredBlock<Block> BRICK_ASBESTOS = registerBlock("brick_asbestos",
            () -> new Block(rock().strength(5.0F, 1000.0F)));
    public static final DeferredBlock<Block> BRICK_ASBESTOS_STAIRS = stairs("brick_asbestos_stairs", BRICK_ASBESTOS,
            rock().strength(5.0F, 1000.0F));
    public static final DeferredBlock<Block> BRICK_COMPOUND = registerBlock("brick_compound",
            () -> new Block(concrete().strength(15.0F, 400.0F)));
    public static final DeferredBlock<Block> BRICK_COMPOUND_STAIRS = stairs("brick_compound_stairs", BRICK_COMPOUND,
            concrete().strength(15.0F, 400.0F));
    public static final DeferredBlock<Block> CMB_BRICK = registerBlock("cmb_brick",
            () -> new Block(concrete().strength(25.0F, 5000.0F)));
    public static final DeferredBlock<Block> CMB_BRICK_REINFORCED = registerBlock("cmb_brick_reinforced",
            () -> new Block(concrete().strength(25.0F, 50000.0F)));
    public static final DeferredBlock<Block> CMB_BRICK_REINFORCED_STAIRS = stairs("cmb_brick_reinforced_stairs", CMB_BRICK_REINFORCED,
            concrete().strength(25.0F, 50000.0F));
    public static final DeferredBlock<Block> DUCRETE_SMOOTH = registerBlock("ducrete_smooth",
            () -> new Block(concrete().strength(20.0F, 500.0F)));
    public static final DeferredBlock<Block> DUCRETE = registerBlock("ducrete",
            () -> new Block(concrete().strength(20.0F, 500.0F)));
    public static final DeferredBlock<Block> DUCRETE_SMOOTH_STAIRS = stairs("ducrete_smooth_stairs", DUCRETE_SMOOTH,
            concrete().strength(20.0F, 500.0F));
    public static final DeferredBlock<Block> DUCRETE_STAIRS = stairs("ducrete_stairs", DUCRETE,
            concrete().strength(20.0F, 500.0F));
    public static final DeferredBlock<Block> BRICK_DUCRETE = registerBlock("brick_ducrete",
            () -> new Block(concrete().strength(15.0F, 750.0F)));
    public static final DeferredBlock<Block> BRICK_DUCRETE_STAIRS = registerBlock("brick_ducrete_stairs",
            () -> new StairBlock(BRICK_DUCRETE.get().defaultBlockState(), concrete().strength(15.0F, 750.0F)));
    public static final DeferredBlock<Block> REINFORCED_DUCRETE = registerBlock("reinforced_ducrete",
            () -> new Block(concrete().strength(20.0F, 1000.0F)));
    public static final DeferredBlock<Block> REINFORCED_SAND = registerBlock("reinforced_sand",
            () -> new Block(concrete().strength(15.0F, 40.0F)));
    public static final DeferredBlock<Block> CONCRETE_STAIRS = stairs("concrete_stairs", CONCRETE,
            concrete().strength(15.0F, 140.0F));
    public static final DeferredBlock<Block> CONCRETE_SMOOTH_STAIRS = stairs("concrete_smooth_stairs", CONCRETE_SMOOTH,
            concrete().strength(15.0F, 140.0F));
    public static final DeferredBlock<Block> CONCRETE_ASBESTOS_STAIRS = stairs("concrete_asbestos_stairs", CONCRETE_ASBESTOS,
            concrete().strength(15.0F, 150.0F));
    public static final DeferredBlock<Block> REINFORCED_STONE_STAIRS = stairs("reinforced_stone_stairs", REINFORCED_STONE,
            concrete().strength(15.0F, 100.0F));
    public static final DeferredBlock<Block> REINFORCED_BRICK_STAIRS = stairs("reinforced_brick_stairs", REINFORCED_BRICK,
            concrete().strength(15.0F, 300.0F));
    public static final DeferredBlock<Block> REINFORCED_SAND_STAIRS = stairs("reinforced_sand_stairs", REINFORCED_SAND,
            concrete().strength(15.0F, 40.0F));
    public static final DeferredBlock<Block> REINFORCED_GLASS = glass("reinforced_glass", 2.0F, 25.0F, 0, false);
    public static final DeferredBlock<Block> REINFORCED_GLASS_PANE = glassPane("reinforced_glass_pane", 2.0F, 25.0F, false);
    public static final DeferredBlock<Block> REINFORCED_LAMINATE = glass("reinforced_laminate", 15.0F, 300.0F, 0, true);
    public static final DeferredBlock<Block> REINFORCED_LAMINATE_PANE = glassPane("reinforced_laminate_pane", 15.0F, 300.0F, true);
    public static final DeferredBlock<Block> REINFORCED_LIGHT = registerBlock("reinforced_light",
            () -> new Block(rock().strength(15.0F, 80.0F).lightLevel(state -> 15)));
    public static final DeferredBlock<Block> REINFORCED_LAMP_OFF = registerBlock("reinforced_lamp_off",
            () -> new ReinforcedLampBlock(rock().strength(15.0F, 80.0F), false));
    public static final DeferredBlock<Block> REINFORCED_LAMP_ON = registerBlock("reinforced_lamp_on",
            () -> new ReinforcedLampBlock(rock().strength(15.0F, 80.0F).lightLevel(state -> 15), true));

    // 1.7.10 TritiumLamp uses separate on/off blocks and an invisible beam
    // block whose tile entity stores incoming directions.
    public static final DeferredBlock<Block> FLOODLIGHT_BEAM = registerBlockWithoutItem("floodlight_beam",
            () -> new FloodlightBeamBlock(rock().strength(-1.0F, 1_000_000.0F)
                    .noOcclusion().lightLevel(state -> 15)));
    public static final DeferredBlock<Block> LAMP_TRITIUM_GREEN_OFF = registerBlock("lamp_tritium_green_off",
            () -> new TritiumLampBlock(rock().strength(3.0F, 10.0F), false, false));
    public static final DeferredBlock<Block> LAMP_TRITIUM_GREEN_ON = registerBlock("lamp_tritium_green_on",
            () -> new TritiumLampBlock(rock().strength(3.0F, 10.0F).lightLevel(state -> 15), true, false));
    public static final DeferredBlock<Block> LAMP_TRITIUM_BLUE_OFF = registerBlock("lamp_tritium_blue_off",
            () -> new TritiumLampBlock(rock().strength(3.0F, 10.0F), false, true));
    public static final DeferredBlock<Block> LAMP_TRITIUM_BLUE_ON = registerBlock("lamp_tritium_blue_on",
            () -> new TritiumLampBlock(rock().strength(3.0F, 10.0F).lightLevel(state -> 15), true, true));

    public static final DeferredBlock<Block> GLASS_ASH = glass("glass_ash", 3.0F, 3.0F, 0, false);
    public static final DeferredBlock<Block> GLASS_BORON = glass("glass_boron", 0.3F, 0.3F, 0, false);
    public static final DeferredBlock<Block> GLASS_LEAD = glass("glass_lead", 0.3F, 0.3F, 0, false);
    public static final DeferredBlock<Block> GLASS_POLARIZED = glass("glass_polarized", 0.3F, 0.3F, 0, false);
    public static final DeferredBlock<Block> GLASS_POLONIUM = glass("glass_polonium", 0.3F, 0.3F, 5, false);
    public static final DeferredBlock<Block> GLASS_TRINITITE = glass("glass_trinitite", 0.3F, 0.3F, 5, false);
    public static final DeferredBlock<Block> GLASS_URANIUM = glass("glass_uranium", 0.3F, 0.3F, 5, false);

    public static final DeferredBlock<Block> STEEL_BEAM = registerBlock("steel_beam",
            () -> new Block(metal().strength(5.0F, 15.0F)));
    public static final DeferredBlock<Block> STEEL_POLES = registerBlock("steel_poles",
            () -> new SteelPolesBlock(metal().strength(5.0F, 15.0F).noOcclusion()));
    public static final DeferredBlock<Block> STEEL_ROOF = registerBlock("steel_roof",
            () -> new Block(metal().strength(5.0F, 15.0F)));
    public static final DeferredBlock<Block> STEEL_SCAFFOLD = registerBlock("steel_scaffold",
            () -> new SteelScaffoldBlock(metal().strength(5.0F, 15.0F).noOcclusion()));

    public static final DeferredBlock<Block> LIGHTSTONE = registerBlock("lightstone",
            () -> new Block(rock().strength(2.0F, 15.0F)));
    public static final DeferredBlock<Block> LIGHTSTONE_TILE = registerBlock("lightstone_tile",
            () -> new Block(rock().strength(2.0F, 15.0F)));
    public static final DeferredBlock<Block> LIGHTSTONE_BRICKS = registerBlock("lightstone_bricks",
            () -> new Block(rock().strength(2.0F, 15.0F)));
    public static final DeferredBlock<Block> STONES_SLAB = registerVariantBlock("stones_slab",
            () -> new LegacyVariantSlabBlock(rock().strength(2.0F, 15.0F), 1),
            LegacyVariantSlabBlock.VARIANT, "block.reinhardtshbm.stones_slab", "tile", "bricks");
    public static final DeferredBlock<Block> STONES_DOUBLE_SLAB = registerVariantBlock("stones_double_slab",
            () -> new LegacyVariantBlock(rock().strength(2.0F, 15.0F), 1),
            LegacyVariantBlock.VARIANT, "block.reinhardtshbm.stones_slab", "tile", "bricks");
    public static final DeferredBlock<Block> LIGHTSTONE_TILE_STAIRS = stairs("lightstone_tile_stairs", LIGHTSTONE_TILE,
            rock().strength(2.0F, 15.0F));
    public static final DeferredBlock<Block> LIGHTSTONE_BRICKS_STAIRS = stairs("lightstone_bricks_stairs", LIGHTSTONE_BRICKS,
            rock().strength(2.0F, 15.0F));
    public static final DeferredBlock<Block> TILE_LAB = registerBlock("tile_lab",
            () -> new Block(rock().strength(1.0F, 20.0F).sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> TILE_LAB_CRACKED = registerBlock("tile_lab_cracked",
            () -> new Block(rock().strength(1.0F, 20.0F).sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> TILE_LAB_BROKEN = registerBlock("tile_lab_broken",
            () -> new Block(rock().strength(1.0F, 20.0F).sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> TILE_LAB_STAIRS = stairs("tile_lab_stairs", TILE_LAB,
            rock().strength(1.0F, 20.0F).sound(SoundType.GLASS));
    public static final DeferredBlock<Block> TILE_LAB_CRACKED_STAIRS = stairs("tile_lab_cracked_stairs", TILE_LAB_CRACKED,
            rock().strength(1.0F, 20.0F).sound(SoundType.GLASS));
    public static final DeferredBlock<Block> TILE_LAB_BROKEN_STAIRS = stairs("tile_lab_broken_stairs", TILE_LAB_BROKEN,
            rock().strength(1.0F, 20.0F).sound(SoundType.GLASS));
    public static final DeferredBlock<Block> PINK_PLANKS = registerBlock("pink_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PINK_LOG = registerBlock("pink_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PINK_SLAB = registerBlock("pink_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PINK_DOUBLE_SLAB = registerBlock("pink_double_slab",
            () -> new PinkDoubleSlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F)
                    .sound(SoundType.WOOD), PINK_SLAB));
    public static final DeferredBlock<Block> PINK_STAIRS = stairs("pink_stairs", PINK_PLANKS,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F)
                    .sound(SoundType.WOOD));
    public static final DeferredBlock<Block> VINYL_TILE = vinylTile("vinyl_tile");
    public static final DeferredBlock<Block> STEEL_WALL = steelWall("steel_wall", SteelWallBlock.Kind.WALL, 5.0F);
    public static final DeferredBlock<Block> STEEL_CORNER = steelWall("steel_corner", SteelWallBlock.Kind.CORNER, 15.0F);
    public static final DeferredBlock<Block> STEEL_GRATE = steelGrate("steel_grate", false);
    public static final DeferredBlock<Block> STEEL_GRATE_WIDE = steelGrate("steel_grate_wide", true);
    public static final DeferredBlock<Block> BARBED_WIRE = barbedWire("barbed_wire", BarbedWireBlock.Kind.NORMAL);
    public static final DeferredBlock<Block> BARBED_WIRE_ACID = barbedWire("barbed_wire_acid", BarbedWireBlock.Kind.ACID);
    public static final DeferredBlock<Block> BARBED_WIRE_FIRE = barbedWire("barbed_wire_fire", BarbedWireBlock.Kind.FIRE);
    public static final DeferredBlock<Block> BARBED_WIRE_POISON = barbedWire("barbed_wire_poison", BarbedWireBlock.Kind.POISON);
    public static final DeferredBlock<Block> BARBED_WIRE_ULTRADEATH = barbedWire("barbed_wire_ultradeath", BarbedWireBlock.Kind.ULTRADEATH);
    public static final DeferredBlock<Block> BARBED_WIRE_WITHER = barbedWire("barbed_wire_wither", BarbedWireBlock.Kind.WITHER);
    public static final DeferredBlock<Block> FENCE_METAL = metalFence("fence_metal");
    public static final DeferredBlock<Block> DUNGEON_CHAIN = dungeonChain("dungeon_chain");
    public static final DeferredBlock<Block> GRAVEL_DIAMOND = registerBlock("gravel_diamond",
            () -> new HbmFallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.6F, 1.0F)
                    .sound(SoundType.GRAVEL)));
    public static final DeferredBlock<Block> GRAVEL_OBSIDIAN = registerBlock("gravel_obsidian",
            () -> new HbmFallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(5.0F, 240.0F)
                    .sound(SoundType.GRAVEL)));
    public static final DeferredBlock<Block> SAND_MIX = registerVariantBlock("sand_mix",
            () -> new SandMixBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5F)
                    .sound(SoundType.SAND)), SandMixBlock.VARIANT,
            "block.reinhardtshbm.sand_mix", "boron", "lead", "uranium", "polonium", "quartz");
    public static final DeferredBlock<Block> STRUCTURE_ANCHOR = registerBlock("structure_anchor",
            () -> new Block(metal().strength(2.5F, 10.0F)));
    public static final DeferredBlock<Block> MOON_TURF = registerBlock("moon_turf",
            () -> new HbmFallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5F, 1.0F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> FROZEN_DIRT = registerBlock("frozen_dirt",
            () -> new FrozenMaterialBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5F, 2.5F)
                    .sound(SoundType.GLASS), true));
    public static final DeferredBlock<Block> FROZEN_PLANKS = registerBlock("frozen_planks",
            () -> new FrozenMaterialBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F, 2.5F)
                    .sound(SoundType.GLASS), false));
    public static final DeferredBlock<Block> TEKTITE = registerBlock("tektite",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5F, 1.0F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> IMPACT_DIRT = registerBlock("impact_dirt",
            () -> new ImpactDirtBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(0.5F, 1.0F)
                    .sound(SoundType.GRAVEL)
                    .randomTicks()));
    public static final DeferredBlock<Block> RAIL_WOOD = railBlock("rail_wood",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion(),
            0.2F, true, true, false);
    public static final DeferredBlock<Block> RAIL_NARROW = railBlock("rail_narrow",
            metal()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion(),
            0.4F, true, true, false);
    public static final DeferredBlock<Block> RAIL_NARROW_STRAIGHT = registerBlockWithoutItem("rail_narrow_straight",
            () -> new LegacyNarrowStraightRailBlock(metal().strength(5.0F, 10.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> RAIL_NARROW_CURVE = railBlock("rail_narrow_curve",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, true, false, false);
    public static final DeferredBlock<Block> RAIL_LARGE_STRAIGHT = railBlock("rail_large_straight",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, false, false, false);
    public static final DeferredBlock<Block> RAIL_LARGE_STRAIGHT_SHORT = railBlock("rail_large_straight_short",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, false, false, false);
    public static final DeferredBlock<Block> RAIL_LARGE_CURVE = railBlock("rail_large_curve",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, true, false, false);
    public static final DeferredBlock<Block> RAIL_LARGE_CURVE_7 = railBlock("rail_large_curve_7",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, true, false, false);
    public static final DeferredBlock<Block> RAIL_LARGE_CURVE_9 = railBlock("rail_large_curve_9",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, true, false, false);
    public static final DeferredBlock<Block> RAIL_LARGE_RAMP = railBlock("rail_large_ramp",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, false, true, false);
    public static final DeferredBlock<Block> RAIL_LARGE_BUFFER = railBlock("rail_large_buffer",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.0F, false, false, false);
    public static final DeferredBlock<Block> RAIL_LARGE_SWITCH = railBlock("rail_large_switch",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, true, false, false);
    public static final DeferredBlock<Block> RAIL_LARGE_SWITCH_FLIPPED = railBlock("rail_large_switch_flipped",
            metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(),
            0.4F, true, false, false);
    public static final DeferredBlock<Block> PNEUMATIC_TUBE = registerBlock("pneumatic_tube",
            () -> new PneumaticTubeBlock(metal().strength(0.1F, 10.0F).sound(SoundType.METAL).noOcclusion(), false));
    public static final DeferredBlock<Block> PNEUMATIC_TUBE_PAINTABLE = registerBlock("pneumatic_tube_paintable",
            () -> new PneumaticTubeBlock(metal().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion(), true));
    public static final DeferredBlock<Block> PNEUMATIC_STORAGE_ACCESS = registerBlock("pneumatic_storage_access",
            () -> new PneumaticStorageBlock(metal().strength(0.1F, 10.0F).sound(SoundType.METAL), PneumaticStorageBlock.Kind.ACCESS));
    public static final DeferredBlock<Block> PNEUMATIC_STORAGE_CLUTTER = registerBlock("pneumatic_storage_clutter",
            () -> new PneumaticStorageBlock(metal().strength(0.1F, 10.0F).sound(SoundType.METAL), PneumaticStorageBlock.Kind.CLUTTER));
    public static final DeferredBlock<Block> PNEUMATIC_STORAGE_MONO = registerBlock("pneumatic_storage_mono",
            () -> new PneumaticStorageBlock(metal().strength(0.1F, 10.0F).sound(SoundType.METAL), PneumaticStorageBlock.Kind.MONO));
    public static final DeferredBlock<Block> RAIL_HIGHSPEED = railBlock("rail_highspeed",
            metal()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion(),
            1.0F, false, true, false);
    public static final DeferredBlock<Block> RAIL_BOOSTER = railBlock("rail_booster",
            metal()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion(),
            1.0F, false, true, true);
    public static final DeferredBlock<Block> LADDER_STURDY = ladderBlock("ladder_sturdy", MapColor.WOOD);
    public static final DeferredBlock<Block> LADDER_GOLD = ladderBlock("ladder_gold", MapColor.GOLD);
    public static final DeferredBlock<Block> LADDER_COPPER = ladderBlock("ladder_copper", MapColor.COLOR_ORANGE);
    public static final DeferredBlock<Block> LADDER_TITANIUM = ladderBlock("ladder_titanium", MapColor.METAL);
    public static final DeferredBlock<Block> LADDER_STEEL = ladderBlock("ladder_steel", MapColor.METAL);
    public static final DeferredBlock<Block> SANDBAGS = registerBlock("sandbags",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(5.0F, 30.0F)
                    .sound(SoundType.SAND)));
    public static final DeferredBlock<Block> BARRICADE = registerBlockWithoutItem("barricade",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(1.0F, 2.5F)
                    .sound(SoundType.SAND)));

    public static final List<DeferredBlock<Block>> BUILDING_BLOCKS = List.of(
            DECO_TOASTER,
            DECO_EMITTER,
            PART_EMITTER,
            BOAT,
            BOBBLEHEAD,
            SNOWGLOBE,
            PLUSHIE,
            LANTERN,
            GLYPHID_BASE,
            DECO_COMPUTER,
            DECO_CRT,
            FILING_CABINET,
            TAPE_RECORDER,
            PEDESTAL,
            SKELETON_HOLDER,
            STONE_KEYHOLE,
            STONE_KEYHOLE_META,
            GRAVEL_DIAMOND,
            GRAVEL_OBSIDIAN,
            MOON_TURF,
            TEKTITE,
            IMPACT_DIRT,
            PINK_LOG,
            ASPHALT,
            ASPHALT_LIGHT,
            ASPHALT_STAIRS,
            BASALT,
            BASALT_SMOOTH,
            BASALT_BRICK,
            BASALT_POLISHED,
            BASALT_TILES,
            BRICK_RED,
            BRICK_SLAB,
            BRICK_DOUBLE_SLAB,
            CONCRETE_BRICK_SLAB,
            CONCRETE_BRICK_DOUBLE_SLAB,
            CONCRETE_SLAB,
            CONCRETE_DOUBLE_SLAB,
            SAND_QUARTZ,
            SAND_BORON,
            SAND_LEAD,
            SAND_URANIUM,
            SAND_POLONIUM,
            CONCRETE,
            CONCRETE_SMOOTH,
            CONCRETE_STAIRS,
            CONCRETE_SMOOTH_STAIRS,
            CONCRETE_COLORED,
            CONCRETE_COLORED_EXT,
            BRICK_CONCRETE,
            BRICK_CONCRETE_MOSSY,
            BRICK_CONCRETE_CRACKED,
            BRICK_CONCRETE_BROKEN,
            BRICK_CONCRETE_MARKED,
            DECO_STEEL,
            DECO_TITANIUM,
            DECO_RED_COPPER,
            DECO_TUNGSTEN,
            DECO_ALUMINIUM,
            DECO_RUSTY_STEEL,
            DECO_LEAD,
            DECO_BERYLLIUM,
            DECO_ASBESTOS,
            DECO_RBMK,
            DECO_RBMK_SMOOTH,
            DECO_PIPE,
            DECO_PIPE_RUSTED,
            DECO_PIPE_GREEN,
            DECO_PIPE_GREEN_RUSTED,
            DECO_PIPE_RED,
            DECO_PIPE_MARKED,
            DECO_PIPE_RIM,
            DECO_PIPE_RIM_RUSTED,
            DECO_PIPE_RIM_GREEN,
            DECO_PIPE_RIM_GREEN_RUSTED,
            DECO_PIPE_RIM_RED,
            DECO_PIPE_RIM_MARKED,
            DECO_PIPE_FRAMED,
            DECO_PIPE_FRAMED_RUSTED,
            DECO_PIPE_FRAMED_GREEN,
            DECO_PIPE_FRAMED_GREEN_RUSTED,
            DECO_PIPE_FRAMED_RED,
            DECO_PIPE_FRAMED_MARKED,
            DECO_PIPE_QUAD,
            DECO_PIPE_QUAD_RUSTED,
            DECO_PIPE_QUAD_GREEN,
            DECO_PIPE_QUAD_GREEN_RUSTED,
            DECO_PIPE_QUAD_RED,
            DECO_PIPE_QUAD_MARKED,
            STEEL_BEAM,
            STEEL_POLES,
            STEEL_ROOF,
            STEEL_SCAFFOLD,
            BRICK_CONCRETE_STAIRS,
            BRICK_CONCRETE_MOSSY_STAIRS,
            BRICK_CONCRETE_CRACKED_STAIRS,
            BRICK_CONCRETE_BROKEN_STAIRS,
            REINFORCED_STONE,
            REINFORCED_BRICK,
            REINFORCED_STONE_STAIRS,
            REINFORCED_BRICK_STAIRS,
            STONE_DEPTH,
            STONE_DEPTH_NETHER,
            DEPTH_BRICK,
            DEPTH_DNT,
            DEPTH_TILES,
            DEPTH_NETHER_BRICK,
            DEPTH_NETHER_TILES,
            STONE_RESOURCE,
            STONE_BIOME,
            WOOD_BARRIER,
            WOOD_STRUCTURE,
            BRICK_FORGOTTEN,
            BRICK_FORGOTTEN_LOCK,
            GNEISS_BRICK,
            GNEISS_TILE,
            GNEISS_CHISELED,
            CONCRETE_ASBESTOS,
            CONCRETE_ASBESTOS_STAIRS,
            CONCRETE_REBAR,
            REBAR,
            CONCRETE_SUPER,
            CONCRETE_SUPER_BROKEN,
            CONCRETE_PILLAR,
            BRICK_FIRE,
            BRICK_FIRE_DOUBLE_SLAB,
            BRICK_FIRE_SLAB,
            BRICK_FIRE_STAIRS,
            BRICK_OBSIDIAN,
            BRICK_OBSIDIAN_STAIRS,
            BRICK_LIGHT,
            BRICK_LIGHT_STAIRS,
            BRICK_ASBESTOS,
            BRICK_ASBESTOS_STAIRS,
            BRICK_COMPOUND,
            BRICK_COMPOUND_STAIRS,
            CMB_BRICK,
            CMB_BRICK_REINFORCED,
            CMB_BRICK_REINFORCED_STAIRS,
            DUCRETE_SMOOTH,
            DUCRETE,
            DUCRETE_SMOOTH_STAIRS,
            DUCRETE_STAIRS,
            BRICK_DUCRETE,
            BRICK_DUCRETE_STAIRS,
            REINFORCED_DUCRETE,
            REINFORCED_SAND,
            REINFORCED_SAND_STAIRS,
            LIGHTSTONE,
            LIGHTSTONE_TILE,
            LIGHTSTONE_BRICKS,
            STONES_SLAB,
            STONES_DOUBLE_SLAB,
            LIGHTSTONE_TILE_STAIRS,
            LIGHTSTONE_BRICKS_STAIRS,
            TILE_LAB,
            TILE_LAB_CRACKED,
            TILE_LAB_BROKEN,
            TILE_LAB_STAIRS,
            TILE_LAB_CRACKED_STAIRS,
            TILE_LAB_BROKEN_STAIRS,
            GLASS_QUARTZ,
            GLASS_ASH,
            GLASS_BORON,
            GLASS_LEAD,
            GLASS_POLARIZED,
            GLASS_POLONIUM,
            GLASS_TRINITITE,
            GLASS_URANIUM,
            REINFORCED_GLASS,
            REINFORCED_GLASS_PANE,
            REINFORCED_LAMINATE,
            REINFORCED_LAMINATE_PANE,
            REINFORCED_LIGHT,
            REINFORCED_LAMP_OFF,
            REINFORCED_LAMP_ON,
            STEEL_WALL,
            STEEL_CORNER,
            STEEL_GRATE,
            STEEL_GRATE_WIDE,
            BARBED_WIRE,
            BARBED_WIRE_ACID,
            BARBED_WIRE_FIRE,
            BARBED_WIRE_POISON,
            BARBED_WIRE_ULTRADEATH,
            BARBED_WIRE_WITHER,
            FENCE_METAL,
            DUNGEON_CHAIN,
            RAIL_WOOD,
            RAIL_NARROW,
            RAIL_NARROW_CURVE,
            RAIL_LARGE_STRAIGHT,
            RAIL_LARGE_STRAIGHT_SHORT,
            RAIL_LARGE_CURVE,
            RAIL_LARGE_CURVE_7,
            RAIL_LARGE_CURVE_9,
            RAIL_LARGE_RAMP,
            RAIL_LARGE_BUFFER,
            RAIL_LARGE_SWITCH,
            RAIL_LARGE_SWITCH_FLIPPED,
            RAIL_HIGHSPEED,
            RAIL_BOOSTER,
            PNEUMATIC_TUBE,
            PNEUMATIC_TUBE_PAINTABLE,
            PNEUMATIC_STORAGE_ACCESS,
            PNEUMATIC_STORAGE_CLUTTER,
            PNEUMATIC_STORAGE_MONO,
            LADDER_STURDY,
            LADDER_GOLD,
            LADDER_COPPER,
            LADDER_TITANIUM,
            LADDER_STEEL,
            SANDBAGS,
            STALAGMITE,
            STALACTITE,
            BRICK_JUNGLE,
            BRICK_JUNGLE_CRACKED,
            BRICK_JUNGLE_FRAGILE,
            BRICK_JUNGLE_LAVA,
            BRICK_JUNGLE_OOZE,
            BRICK_JUNGLE_MYSTIC,
            BRICK_JUNGLE_TRAP,
            BRICK_JUNGLE_GLYPH,
            BRICK_JUNGLE_CIRCLE,
            SPIKES,
            BLOCK_CAP,
            VINYL_TILE,
            PLANT_REEDS,
            WASTE_TRINITITE,
            WASTE_TRINITITE_RED,
            FROZEN_GRASS,
            FROZEN_LOG,
            BURNING_EARTH,
            SELLAFIELD,
            SELLAFIELD_SLAKED,
            SELLAFIELD_BEDROCK,
            ORE_SELLAFIELD_DIAMOND,
            ORE_SELLAFIELD_EMERALD,
            ORE_SELLAFIELD_URANIUM_SCORCHED,
            ORE_SELLAFIELD_SCHRABIDIUM,
            ORE_SELLAFIELD_RADGEM
    );

    public static final List<DeferredBlock<Block>> METEOR_BLOCKS = List.of(
            BLOCK_METEOR,
            BLOCK_METEOR_BROKEN,
            BLOCK_METEOR_COBBLE,
            BLOCK_METEOR_MOLTEN,
            BLOCK_METEOR_TREASURE,
            METEOR_BRICK,
            METEOR_BRICK_MOSSY,
            METEOR_BRICK_CRACKED,
            METEOR_BRICK_CHISELED,
            METEOR_PILLAR,
            METEOR_POLISHED,
            METEOR_SPAWNER,
            METEOR_BATTERY
    );

    public static final List<DeferredBlock<Block>> MATERIAL_BLOCKS = List.of(
            BLOCK_ALUMINIUM,
            BLOCK_GRAPHITE,
            BLOCK_BORON,
            BLOCK_SCHRARANIUM,
            BLOCK_LANTHANIUM,
            BLOCK_RA226,
            BLOCK_ACTINIUM,
            BLOCK_SCHRABIDATE,
            BLOCK_COLTAN,
            BLOCK_SMORE,
            BLOCK_SLAG,
            BLOCK_COKE,
            BLOCK_SEMTEX,
            BLOCK_C4,
            BLOCK_POLYMER,
            BLOCK_BAKELITE,
            BLOCK_RUBBER,
            BLOCK_CADMIUM,
            BLOCK_TCALLOY,
            BLOCK_CDALLOY,
            BLOCK_NIOBIUM,
            BLOCK_BISMUTH,
            BLOCK_TANTALIUM,
            BLOCK_ZIRCONIUM,
            BLOCK_DINEUTRONIUM,
            BLOCK_WASTE_VITRIFIED,
            BLOCK_PU_MIX,
            BLOCK_COPPER,
            BLOCK_FLUORITE,
            BLOCK_NITER,
            BLOCK_RED_COPPER,
            BLOCK_STEEL,
            BLOCK_SULFUR,
            BLOCK_TITANIUM,
            BLOCK_TUNGSTEN,
            BLOCK_URANIUM,
            BLOCK_THORIUM,
            BLOCK_LEAD,
            BLOCK_TRINITITE,
            BLOCK_WASTE,
            BLOCK_WASTE_PAINTED,
            BLOCK_SCRAP,
            BLOCK_BERYLLIUM,
            BLOCK_SCHRABIDIUM,
            BLOCK_SCHRABIDIUM_CLUSTER,
            BLOCK_EUPHEMIUM,
            BLOCK_ADVANCED_ALLOY,
            BLOCK_MAGNETIZED_TUNGSTEN,
            BLOCK_COMBINE_STEEL,
            BLOCK_AUSTRALIUM,
            BLOCK_DESH,
            BLOCK_DURA_STEEL,
            BLOCK_YELLOWCAKE,
            BLOCK_STARMETAL,
            BLOCK_U233,
            BLOCK_U235,
            BLOCK_U238,
            BLOCK_URANIUM_FUEL,
            BLOCK_NEPTUNIUM,
            BLOCK_POLONIUM,
            BLOCK_PLUTONIUM,
            BLOCK_PU238,
            BLOCK_PU239,
            BLOCK_PU240,
            BLOCK_MOX_FUEL,
            BLOCK_PLUTONIUM_FUEL,
            BLOCK_THORIUM_FUEL,
            BLOCK_SOLINIUM,
            BLOCK_SCHRABIDIUM_FUEL,
            BLOCK_LITHIUM,
            BLOCK_WHITE_PHOSPHORUS,
            BLOCK_RED_PHOSPHORUS,
            BLOCK_INSULATOR,
            BLOCK_ASBESTOS,
            BLOCK_FIBERGLASS,
            BLOCK_COBALT
            , BLOCK_AU198,
            BLOCK_ELECTRICAL_SCRAP,
            BLOCK_EUPHEMIUM_CLUSTER,
            BLOCK_TRITIUM
    );

    private HbmBlocks() {
    }

    private static BlockBehaviour.Properties rock() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties concrete() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private static DeferredBlock<Block> ore(String name, float hardness, float resistance) {
        return registerBlock(name, () -> hazardAwareBlock(name, rock().strength(hardness, resistance)));
    }

    private static DeferredBlock<Block> depthBlock(String name) {
        return registerBlock(name, () -> new DepthRockBlock(rock().strength(100.0F, 1_000.0F)));
    }

    private static DeferredBlock<Block> depthOre(String name, String dropId, int minimum, int range) {
        return registerBlock(name, () -> new DepthOreBlock(rock().strength(100.0F, 1_000.0F), dropId, minimum, range));
    }

    private static DeferredBlock<Block> oreWithLight(String name, float hardness, float resistance, int light) {
        return registerBlock(name, () -> hazardAwareBlock(name, rock()
                .strength(hardness, resistance)
                .lightLevel(state -> light)));
    }

    private static DeferredBlock<Block> meteorBlock(String name) {
        return registerBlock(name, () -> hazardAwareBlock(name, rock().strength(15.0F, 360.0F)));
    }

    private static DeferredBlock<Block> meteorBattery(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new net.minecraft.world.level.block.RotatedPillarBlock(rock().strength(15.0F, 360.0F)));
        HbmItems.ITEMS.register(name, () -> new MeteorBatteryBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> meteorOre(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new MeteorOreBlock(rock().strength(15.0F, 360.0F)));
        HbmItems.ITEMS.register(name, () -> new MeteorOreBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> materialBlock(String name) {
        return registerBlock(name, () -> hazardAwareBlock(name, metal().strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> hadronCoil(String name, int coilStrength) {
        return registerBlock(name, () -> new HadronCoilBlock(metal().strength(5.0F, 10.0F), coilStrength));
    }

    private static DeferredBlock<Block> cokeBlock(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new CokeBlock(metal().strength(5.0F, 10.0F)));
        HbmItems.ITEMS.register(name, () -> new CokeBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> capBlock(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new CapBlock(metal().strength(5.0F, 10.0F)));
        HbmItems.ITEMS.register(name, () -> new CapBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> oilDeposit(String name) {
        return registerBlock(name, () -> new OilDepositBlock(rock().strength(5.0F, 10.0F), ORE_OIL_EMPTY::get));
    }

    private static DeferredBlock<Block> deepslateOilDeposit(String name) {
        return registerBlock(name, () -> new OilDepositBlock(rock().strength(5.0F, 10.0F), ORE_DEEPSLATE_OIL_EMPTY::get));
    }

    private static DeferredBlock<Block> oilSand(String name) {
        return registerBlock(name, () -> new OilSandBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .strength(0.5F, 1.0F)
                .sound(SoundType.SAND)));
    }

    private static DeferredBlock<Block> bedrockOil(String name) {
        return registerBlock(name, () -> new Block(rock()
                .strength(-1.0F, 3_600_000.0F)));
    }

    private static DeferredBlock<Block> guideTerminal(String name) {
        return registerBlock(name, () -> new GuideTerminalBlock(metal().strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> hevBattery(String name) {
        return registerBlock(name, () -> new HevBatteryBlock(metal()
                .strength(0.5F, 0.25F)
                .lightLevel(state -> 10)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> bedrockOre(String name) {
        return registerBlock(name, () -> new BedrockOreBlock(rock()
                .strength(-1.0F, 3_600_000.0F)));
    }

    private static Block hazardAwareBlock(String name, BlockBehaviour.Properties properties) {
        double radiation = HbmHazardSystem.rawRadiationForId(name);
        if (radiation > 0.0D) {
            return new RadiatingBlock(properties.randomTicks(), radiation);
        }
        return new Block(properties);
    }

    private static DeferredBlock<Block> fallingSoil(String name, MapColor mapColor, SoundType soundType) {
        return registerBlock(name, () -> new HbmFallingBlock(BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .strength(0.5F, 1.0F)
                .sound(soundType)));
    }

    private static DeferredBlock<Block> anvil(String name, int tier) {
        return registerBlock(name, () -> new HbmAnvilBlock(metal()
                .strength(5.0F, 100.0F)
                .sound(SoundType.ANVIL)
                .noOcclusion(), tier));
    }

    private static DeferredBlock<Block> steelGrate(String name, boolean wide) {
        return registerBlock(name, () -> new SteelGrateBlock(metal()
                .strength(2.0F, 5.0F)
                .noOcclusion(), wide));
    }

    private static DeferredBlock<Block> steelWall(String name, SteelWallBlock.Kind kind, float hardness) {
        return registerBlock(name, () -> new SteelWallBlock(metal()
                .strength(hardness, 15.0F)
                .noOcclusion(), kind));
    }

    private static DeferredBlock<Block> barbedWire(String name, BarbedWireBlock.Kind kind) {
        return registerBlock(name, () -> new BarbedWireBlock(metal()
                .strength(5.0F, 10.0F)
                .noCollission()
                .noOcclusion(), kind));
    }

    private static DeferredBlock<Block> metalFence(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new MetalFenceBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new MetalFenceBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> dungeonChain(String name) {
        return registerBlock(name, () -> new DungeonChainBlock(metal()
                .strength(0.25F, 2.0F)
                .noCollission()
                .noOcclusion()));
    }

    private static DeferredBlock<Block> concreteColored(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new ConcreteColoredBlock(concrete()
                .strength(15.0F, 140.0F)));
        HbmItems.ITEMS.register(name, () -> new ConcreteColoredBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> concreteColoredExt(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new ConcreteColoredBlock(concrete()
                .strength(15.0F, 140.0F), 7));
        HbmItems.ITEMS.register(name, () -> ConcreteColoredBlockItem.ext(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> energyCable(String name, double radius) {
        return registerBlock(name, () -> new EnergyCableBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), radius));
    }

    private static DeferredBlock<Block> powerCableBox(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new PowerCableBoxBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new PowerCableBoxBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> coatedEnergyCable(String name) {
        return registerBlock(name, () -> new CoatedEnergyCableBlock(metal()
                .strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> powerPylon(String name, PowerPylonBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new PowerPylonBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind));
        HbmItems.ITEMS.register(name, () -> new PowerPylonBlockItem(block.get(), new Item.Properties(), kind));
        return block;
    }

    private static DeferredBlock<Block> powerPylonWood(String name, PowerPylonBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new PowerPylonBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(5.0F, 10.0F)
                .sound(SoundType.WOOD)
                .noOcclusion(), kind));
        HbmItems.ITEMS.register(name, () -> new PowerPylonBlockItem(block.get(), new Item.Properties(), kind));
        return block;
    }

    private static DeferredBlock<Block> fluidDuct(String name, FluidDuctBlock.Kind kind) {
        return registerBlock(name, () -> new FluidDuctBlock(metal()
                .strength(5.0F, 10.0F)
                .sound(HbmSoundTypes.PIPE)
                .noOcclusion(), kind));
    }

    private static DeferredBlock<Block> fluidPump(String name) {
        return registerBlock(name, () -> new FluidPumpBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> drain(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new DrainBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new DrainBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> oilPipe(String name) {
        return registerBlock(name, () -> new OilPipeBlock(metal()
                .strength(5.0F, 10.0F)
                .sound(HbmSoundTypes.PIPE)));
    }

    private static DeferredBlock<Block> decorativePipe(String name) {
        return registerBlock(name, () -> new RotatedPillarBlock(metal()
                .strength(2.0F, 5.0F)
                .sound(HbmSoundTypes.PIPE)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> fluidBarrel(String name, FluidBarrelBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new FluidBarrelBlock(metal()
                .strength(2.0F, 5.0F)
                .sound(kind == FluidBarrelBlock.Kind.PLASTIC ? SoundType.STONE : SoundType.METAL)
                .noOcclusion(), kind));
        HbmItems.ITEMS.register(name, () -> new FluidBarrelBlockItem((FluidBarrelBlock) block.get(), new Item.Properties(), kind));
        return block;
    }

    private static DeferredBlock<Block> legacyBarrel(String name, LegacyBarrelBlock.Kind kind) {
        return registerBlockWithoutItem(name, () -> new LegacyBarrelBlock(metal()
                .strength(0.5F, 2.5F)
                .noOcclusion()
                .randomTicks(), kind));
    }

    private static DeferredBlock<Block> fluidTank(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new FluidTankBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(2, 3, 1), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new FluidTankBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 1.7.10's two renderer-only hexafluoride tanks: no GUI or fluid handler. */
    private static DeferredBlock<Block> hexafluorideTank(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new HexafluorideTankBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new HexafluorideTankBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> bigAssTank(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new BigAssTankBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), BigAssTankBlock.legacyFootprint(), MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new LegacyTankBlockItem(block.get(), new Item.Properties(),
                com.reinhardt.hbm.client.render.LegacyTankItemRenderer.Kind.BAT9000));
        return block;
    }

    private static DeferredBlock<Block> largeFluidTank(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LargeFluidTankBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new LegacyTankBlockItem(block.get(), new Item.Properties(),
                com.reinhardt.hbm.client.render.LegacyTankItemRenderer.Kind.BIG_ASS_TANK));
        return block;
    }

    private static DeferredBlock<Block> batteryRedd(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new BatteryReddBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
        // The world block is rendered by BatteryReddBlockEntityRenderer.  Its
        // inventory item must use the same three OBJ assemblies instead of the
        // legacy/generated flat item model.
        HbmItems.ITEMS.register(name, () -> new ObjMachineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> batterySocket(String name) {
        return registerObjBlock(name, () -> new BatterySocketBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> storageCrate(String name, StorageCrateBlockEntity.Kind kind, float hardness, float resistance) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new StorageCrateBlock(metal()
                .strength(hardness, resistance), kind));
        HbmItems.ITEMS.register(name, () -> new StorageCrateBlockItem((StorageCrateBlock) block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> droneWaypoint(String name) {
        return registerBlock(name, () -> new DroneWaypointBlock(metal().strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> droneRequestWaypoint(String name) {
        return registerBlock(name, () -> new DroneWaypointBlock(metal().strength(0.1F, 10.0F), DroneWaypointBlock.Kind.REQUEST));
    }

    private static DeferredBlock<Block> droneContainer(String name, DroneNetworkContainerBlock.Kind kind) {
        return registerBlock(name, () -> new DroneNetworkContainerBlock(metal().strength(0.1F, 10.0F), kind));
    }

    private static DeferredBlock<Block> massStorage(String name, MassStorageBlock.Kind kind) {
        return registerBlock(name, () -> new MassStorageBlock(metal().strength(5.0F, 10.0F), kind));
    }

    private static DeferredBlock<Block> lootCrate(String name, SoundType sound, float hardness, float resistance, LootCrateBlock.Kind kind) {
        return registerBlock(name, () -> new LootCrateBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(hardness, resistance)
                .sound(sound), kind));
    }

    private static DeferredBlock<Block> powerMachine(String name, PowerMachineBlock.MachineType type) {
        return registerBlock(name, () -> new PowerMachineBlock(metal()
                .strength(5.0F, 10.0F), type));
    }

    private static DeferredBlock<Block> woodBurner(String name) {
        return registerBlock(name, () -> new WoodBurnerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.box(0, 1, 0, 1, 0, 1), Shapes.block()));
    }

    private static DeferredBlock<Block> stirlingGenerator(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new StirlingGeneratorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new StirlingGeneratorBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> dieselGenerator(String name) {
        return registerBlock(name, () -> new DieselGeneratorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> combustionEngine(String name) {
        return registerObjBlock(name, () -> new CombustionEngineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> gasFlare(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new GasFlareBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new GasFlareBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> steamEngine(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new SteamEngineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new SteamEngineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> heatBoiler(String name) {
        return registerObjBlock(name, () -> new HeatBoilerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(1, 4, 1), Shapes.block()));
    }

    private static DeferredBlock<Block> industrialBoiler(String name) {
        return registerObjBlock(name, () -> new IndustrialBoilerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(1, 5, 1), Shapes.block()));
    }

    private static DeferredBlock<Block> solarBoiler(String name) {
        return registerObjBlock(name, () -> new SolarBoilerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> airCompressor(String name) {
        return registerBlock(name, () -> new AirCompressorBlock(metal()
                .strength(10.0F, 20.0F)
                .noOcclusion(), Shapes.block()));
    }

    private static DeferredBlock<Block> solarMirror(String name) {
        return registerObjBlock(name, () -> new SolarMirrorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> smallBoiler(String name, SmallBoilerBlock.Kind kind) {
        return registerBlock(name, () -> new SmallBoilerBlock(metal()
                .strength(5.0F, 10.0F), kind));
    }

    private static DeferredBlock<Block> steamTurbine(String name) {
        return registerBlock(name, () -> new SteamTurbineBlock(metal()
                .strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> industrialTurbine(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new IndustrialTurbineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new IndustrialTurbineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> largeTurbine(String name) {
        return registerBlock(name, () -> new LargeTurbineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> leviathanTurbine(String name) {
        return registerObjBlock(name, () -> new LeviathanTurbineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
    }

    private static DeferredBlock<Block> gasTurbine(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new GasTurbineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new GasTurbineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> steamCondenser(String name) {
        return registerObjBlock(name, () -> new SteamCondenserBlock(metal()
                .strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> poweredSteamCondenser(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new PoweredSteamCondenserBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new ObjMachineLegacyOffsetBlockItem(block.get(), new Item.Properties(), 1, true));
        return block;
    }

    private static DeferredBlock<Block> coolingTower(String name, CoolingTowerBlock.Kind kind) {
        return registerObjBlock(name, () -> new CoolingTowerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(kind.radiusX(), kind.height(), kind.radiusZ()), Shapes.block(), kind));
    }

    private static DeferredBlock<Block> poleTop(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new PoleTopBlock(metal()
                .strength(5.0F, 15.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new PoleBlockItem(block.get(), new Item.Properties(), false));
        return block;
    }

    private static DeferredBlock<Block> poleSatelliteReceiver(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new PoleSatelliteReceiverBlock(metal()
                .strength(5.0F, 15.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new PoleBlockItem(block.get(), new Item.Properties(), true));
        return block;
    }

    private static DeferredBlock<Block> groundwaterPump(String name, GroundwaterPumpBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new GroundwaterPumpBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind, MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new GroundwaterPumpBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> oilDerrick(String name) {
        return registerObjBlock(name, () -> new OilDerrickBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> oilPumpjack(String name) {
        return registerObjBlock(name, () -> new OilPumpjackBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> frackingTower(String name) {
        return registerObjBlock(name, () -> new FrackingTowerBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> refinery(String name) {
        return registerObjBlock(name, () -> new RefineryBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> vacuumDistill(String name) {
        return registerObjBlock(name, () -> new VacuumDistillBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> coker(String name) {
        return registerObjBlock(name, () -> new MachineCokerBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> furnaceCombination(String name) {
        return registerObjBlock(name, () -> new FurnaceCombinationBlock(rock()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> rotaryFurnace(String name) {
        return registerObjBlock(name, () -> new RotaryFurnaceBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
    }

    private static DeferredBlock<Block> solidifier(String name) {
        return registerObjBlock(name, () -> new SolidifierBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), Shapes.block()));
    }

    private static DeferredBlock<Block> liquefactor(String name) {
        return registerObjBlock(name, () -> new LiquefactorBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), Shapes.block()));
    }

    private static DeferredBlock<Block> electrolyzer(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new ElectrolyzerBlock(metal()
                .strength(10.0F, 20.0F)
                .noOcclusion(), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new ElectrolyzerBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> fractionTower(String name) {
        return registerObjBlock(name, () -> new FractionTowerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> fractionSpacer(String name) {
        return registerObjBlock(name, () -> new FractionSpacerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> catalyticCracker(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new CatalyticCrackerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new ObjMachineLegacyOffsetBlockItem(block.get(), new Item.Properties(), 3, true));
        return block;
    }

    private static DeferredBlock<Block> catalyticReformer(String name) {
        return registerObjBlock(name, () -> new CatalyticReformerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> hydrotreater(String name) {
        return registerObjBlock(name, () -> new HydrotreaterBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> breederReactor(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new BreederReactorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new LegacyMachineRendererBlockItem(block.get(), new Item.Properties(), 0));
        return block;
    }

    private static DeferredBlock<Block> watzStruct(String name) {
        return registerBlock(name, () -> new WatzStructBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> watz(String name) {
        return registerObjBlock(name, () -> new WatzBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> watzPump(String name) {
        return registerObjBlock(name, () -> new WatzPumpBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> wasteDrum(String name) {
        return registerBlock(name, () -> new WasteDrumBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> purex(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new PurexBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new PurexBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> icfPress(String name) {
        return registerBlock(name, () -> new IcfPressBlock(metal().strength(5.0F, 60.0F)));
    }

    private static DeferredBlock<Block> icfStruct(String name) {
        return registerBlock(name, () -> new IcfStructBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
    }

    private static DeferredBlock<Block> icfCore(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new IcfCoreBlock(metal()
                .strength(5.0F, 60.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new ObjMachineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> icfComponent(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new LegacyVariantBlock(metal().strength(5.0F, 30.0F), 4));
        HbmItems.ITEMS.register(name, () -> new LegacyVariantBlockItem(block.get(), new Item.Properties(),
                LegacyVariantBlock.VARIANT, "block.reinhardtshbm.icf_component",
                "scaffold", "vessel", "vessel_welded", "structure", "structure_bolted"));
        return block;
    }

    private static DeferredBlock<Block> icfController(String name) {
        return registerBlock(name, () -> new IcfControllerBlock(metal().strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> icfLaserComponent(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new IcfLaserComponentBlock(metal().strength(5.0F, 10.0F)));
        HbmItems.ITEMS.register(name, () -> new LegacyVariantBlockItem(block.get(), new Item.Properties(),
                LegacyVariantBlock.VARIANT, "block.reinhardtshbm.icf_laser_component",
                "casing", "port", "cell", "emitter", "capacitor", "turbo"));
        return block;
    }

    private static DeferredBlock<Block> icfAssembledLaser(String name) {
        return registerBlock(name, () -> new IcfAssembledLaserBlock(metal().strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> researchReactor(String name) {
        return registerObjBlock(name, () -> new ResearchReactorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
    }

    private static DeferredBlock<Block> reactorControl(String name) {
        return registerBlock(name, () -> new ReactorControlBlock(metal()
                .strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> volcanoCore(String name, boolean radioactive) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new VolcanoCoreBlock(
                metal().strength(-1.0F, 10_000.0F), radioactive));
        HbmItems.ITEMS.register(name, () -> new VolcanoCoreBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> zirnoxReactor(String name) {
        return registerBlock(name, () -> new ZirnoxReactorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
    }

    private static DeferredBlock<Block> nukeBoy(String name) {
        return registerBlock(name, () -> new NukeBoyBlock(metal()
                .strength(5.0F, 200.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> legacyNuke(String name, LegacyNukeDefinition definition) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LegacyNukeBlock(metal()
                .strength(5.0F, 200.0F)
                .noOcclusion(), definition));
        HbmItems.ITEMS.register(name, () -> new ObjMachineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> bombMulti(String name) {
        return registerObjBlock(name, () -> new BombMultiBlock(metal()
                .strength(5.0F, 200.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> vendingMachine(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new VendingMachineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new VendingMachineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> crashedBomb(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new CrashedBombBlock(metal()
                .strength(-1.0F, 6000.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new CrashedBombBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> landmine(String name, LandmineBlock.LandmineType type) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LandmineBlock(metal()
                .strength(1.0F, 1.0F)
                .noOcclusion(), type));
        HbmItems.ITEMS.register(name, () -> new LandmineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> turretJeremy(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new TurretJeremyBlock(metal()
                .strength(5.0F, 600.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new TurretJeremyBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> turretChekhov(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new TurretChekhovBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new TurretJeremyBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> legacyTurret(String name, LegacyTurretType type) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LegacyTurretBlock(metal()
                .strength(5.0F, legacyTurretResistance(type))
                .noOcclusion(), type));
        HbmItems.ITEMS.register(name, () -> new LegacyTurretBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static float legacyTurretResistance(LegacyTurretType type) {
        return switch (type) {
            case FRIENDLY, FRITZ -> 10.0F;
            case HOWARD, MAXWELL, TAUON -> 60.0F;
            case HOWARD_DAMAGED, RICHARD, ARTY, HIMARS -> 600.0F;
            case SENTRY, SENTRY_DAMAGED -> 5.0F;
        };
    }

    private static DeferredBlock<Block> launcherStructCore(String name, boolean large) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LauncherStructCoreBlock(metal()
                .strength(5.0F, 10.0F), large));
        HbmItems.ITEMS.register(name, () -> new LegacyOffsetBlockItem(
                block.get(),
                new Item.Properties(),
                0,
                true
        ));
        return block;
    }

    private static DeferredBlock<Block> structSoyuzCore(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new SoyuzStructCoreBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> compactLauncher(String name) {
        return registerBlock(name, () -> new CompactLauncherBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> launchTable(String name) {
        return registerBlock(name, () -> new LaunchTableBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> launchPad(String name, LaunchPadBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LaunchPadBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind));
        HbmItems.ITEMS.register(name, () -> new LegacyOffsetBlockItem(
                block.get(),
                new Item.Properties(),
                kind == LaunchPadBlock.Kind.LARGE ? 4 : 1,
                true
        ));
        return block;
    }

    private static DeferredBlock<Block> soyuzLauncher(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new SoyuzLauncherBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new SoyuzLauncherBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> soyuzCapsule(String name) {
        return registerBlock(name, () -> new SoyuzCapsuleBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> launcherDummy(String name, VoxelShape shape, boolean port) {
        return registerBlockWithoutItem(name, () -> new LauncherDummyBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), shape, port));
    }

    private static DeferredBlock<Block> pwrBlock(String name, PwrBlock.Kind kind) {
        Supplier<Block> supplier = () -> new PwrBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind);
        return registerBlock(name, supplier);
    }

    private static DeferredBlock<Block> zirnoxDestroyed(String name) {
        return registerBlock(name, () -> new ZirnoxDestroyedBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
    }

    private static DeferredBlock<Block> machineBlastFurnace(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new MachineBlastFurnaceBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new ObjMachineBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> crucible(String name) {
        return registerObjBlock(name, () -> new CrucibleBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> foundryCasting(String name, FoundryCastingBlock.Kind kind) {
        return registerBlock(name, () -> new FoundryCastingBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind));
    }

    private static DeferredBlock<Block> foundryChannel(String name) {
        return registerBlock(name, () -> new FoundryChannelBlock(rock()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> foundryTank(String name) {
        return registerBlock(name, () -> new FoundryTankBlock(rock()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> foundryOutlet(String name, FoundryOutletBlock.Kind kind) {
        return registerBlock(name, () -> new FoundryOutletBlock(rock()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind));
    }

    private static DeferredBlock<Block> foundrySlag(String name) {
        return registerBlock(name, () -> new FoundrySlagBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> strandCaster(String name) {
        return registerObjBlock(name, () -> new StrandCasterBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> legacyFurnace(String name, LegacyFurnaceBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LegacyFurnaceBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind, MACHINE_CORE_SHAPE));
        if (kind == LegacyFurnaceBlock.Kind.STEEL) {
            HbmItems.ITEMS.register(name, () -> new ObjMachineLegacyOffsetBlockItem(block.get(), new Item.Properties(), 1, true));
        } else {
            HbmItems.ITEMS.register(name, () -> new ObjMachineBlockItem(block.get(), new Item.Properties()));
        }
        return block;
    }

    private static DeferredBlock<Block> brickFurnace(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new BrickFurnaceBlock(metal()
                .strength(5.0F, 10.0F)
                .lightLevel(state -> state.getValue(BrickFurnaceBlock.LIT) ? 15 : 0)));
        HbmItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> vanillaDoor(String name) {
        return registerBlock(name, () -> new HbmLegacyDoorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> vanillaTrapdoor(String name) {
        return registerBlock(name, () -> new HbmLegacyTrapDoorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> heavyDoor(com.reinhardt.hbm.door.HbmDoorDecl decl) {
        DeferredBlock<Block> block = registerBlockWithoutItem(decl.id(), () -> new HbmHeavyDoorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), decl));
        HbmItems.ITEMS.register(decl.id(), () -> new HbmHeavyDoorBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> assemblyMachine(String name) {
        return registerObjBlock(name, () -> new AssemblyMachineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(1, 3, 1), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> assemblyFactory(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new AssemblyFactoryBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(2, 3, 2), MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new LargeFactoryBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> chemicalPlant(String name) {
        return registerBlock(name, () -> new ChemicalPlantBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(1, 3, 1), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> chemicalFactory(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new ChemicalFactoryBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(2, 3, 2), MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new LargeFactoryBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> solderingStation(String name) {
        return registerObjBlock(name, () -> new SolderingStationBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), solderingStationFootprint(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> arcWelder(String name) {
        return registerObjBlock(name, () -> new ArcWelderBlock(metal()
                .strength(5.0F, 10.0F)
                // HBM 1.7.10/1.12 MachineArcWelder#getDimensions(): {1, 0, 1, 0, 1, 1}.
                .noOcclusion(), LargeMachineBlock.Footprint.box(-1, 1, 0, 1, 0, 1), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> arcFurnace(String name) {
        return registerObjBlock(name, () -> new ArcFurnaceBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> storageDrum(String name) {
        return registerBlock(name, () -> new StorageDrumBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> compressor(String name, CompressorBlockEntity.Kind kind) {
        return registerObjBlock(name, () -> new CompressorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE, kind));
    }

    private static DeferredBlock<Block> mixer(String name) {
        return registerObjBlock(name, () -> new MixerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> pressMachine(String name) {
        return registerBlock(name, () -> new PressMachineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(0, 3, 0), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> ammoPress(String name) {
        // MachineAmmoPress has its own OBJ item model in 1.7.10.  Keeping a
        // plain BlockItem here silently falls back to the flat block icon.
        return registerObjBlock(name, () -> new AmmoPressBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.legacySouthBox(1, 0, 0, 0, 1, 1), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> microwave(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new MicrowaveBlock(metal().strength(5.0F, 10.0F).noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new MicrowaveBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> centrifuge(String name, CentrifugeBlock.Kind kind) {
        return registerBlock(name, () -> new CentrifugeBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind, CENTRIFUGE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> crystallizer(String name) {
        return registerObjBlock(name, () -> new CrystallizerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(1, 7, 1), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> cyclotron(String name) {
        return registerObjBlock(name, () -> new CyclotronBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> exposureChamber(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new ExposureChamberBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new ExposureChamberBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> deuteriumExtractor(String name) {
        return registerBlock(name, () -> new DeuteriumExtractorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> deuteriumTower(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new DeuteriumTowerBlock(metal()
                .strength(10.0F, 20.0F)
                .noOcclusion(), Shapes.block()));
        HbmItems.ITEMS.register(name, () -> new DeuteriumTowerBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> demonLamp(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new DemonLampBlock(metal()
                .strength(3.0F, 10.0F)
                .noOcclusion()
                .lightLevel(state -> 15)));
        HbmItems.ITEMS.register(name, () -> new DemonLampBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> geothermalHeatExchanger(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new GeothermalHeatExchangerBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new GeothermalHeatExchangerBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> particleAccelerator(String name, ParticleAcceleratorBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new ParticleAcceleratorBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE, kind));
        HbmItems.ITEMS.register(name, () -> new ParticleAcceleratorBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> silex(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new SilexBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new ObjMachineLegacyOffsetBlockItem(block.get(), new Item.Properties(), 1, true));
        return block;
    }

    private static DeferredBlock<Block> fel(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new FelBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new ObjMachineLegacyOffsetBlockItem(block.get(), new Item.Properties(), 2, true));
        return block;
    }

    private static DeferredBlock<Block> miningLaser(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new MiningLaserBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new MiningLaserBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> oreSlopper(String name) {
        return registerObjBlock(name, () -> new OreSlopperBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
    }

    private static DeferredBlock<Block> oreBasalt(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new OreBasaltBlock(rock()
                .strength(5.0F, 10.0F)
                .requiresCorrectToolForDrops()));
        HbmItems.ITEMS.register(name, () -> new OreBasaltBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> sellafield(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new SellafieldBlock(rock()
                .strength(5.0F, 10.0F)
                .randomTicks()));
        HbmItems.ITEMS.register(name, () -> new SellafieldBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> sellafieldOre(String name, SellafieldOreBlock.Drop drop) {
        return registerBlock(name, () -> new SellafieldOreBlock(rock()
                .strength(5.0F, 10.0F)
                .requiresCorrectToolForDrops(), drop));
    }

    private static DeferredBlock<Block> excavator(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new ExcavatorBlock(metal()
                .strength(5.0F, 100.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE));
        HbmItems.ITEMS.register(name, () -> new ExcavatorBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> fusionHatch(String name) {
        return registerBlock(name, () -> new FusionHatchBlock(metal()
                .strength(5.0F, 10.0F)));
    }

    private static DeferredBlock<Block> fusionComponent(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LegacyVariantBlock(metal()
                .strength(5.0F, 30.0F), 3));
        HbmItems.ITEMS.register(name, () -> new FusionComponentBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> fusionTorusStruct(String name) {
        return registerBlock(name, () -> new FusionTorusStructBlock(metal()
                .strength(5.0F, 10.0F)
                .lightLevel(state -> 15)));
    }

    private static DeferredBlock<Block> fusionMachine(String name, FusionMachineBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new FusionMachineBlock(metal()
                .strength(5.0F, 60.0F)
                .noOcclusion(), MACHINE_CORE_SHAPE, kind));
        HbmItems.ITEMS.register(name, () -> new ObjMachineLegacyOffsetBlockItem(block.get(), new Item.Properties(), kind.legacyOffset(), true));
        return block;
    }

    private static DeferredBlock<Block> dfcCore(String name) {
        return registerBlock(name, () -> new DfcCoreBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> dfcComponent(String name, DfcComponentBlock.Kind kind) {
        return registerObjBlock(name, () -> new DfcComponentBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind));
    }

    private static DeferredBlock<Block> machineBlock(String name) {
        return registerBlock(name, () -> new LargeMachineBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), LargeMachineBlock.Footprint.centered(1, 1, 1)));
    }

    private static DeferredBlock<Block> legacyMachine(String name, int[] dimensions, int offset) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new LegacyMachineBlock(
                metal().strength(name.equals("machine_autocrafter") ? 10.0F : 5.0F,
                        name.equals("machine_autocrafter") ? 20.0F : 10.0F).noOcclusion(),
                legacyMachineFootprint(name, dimensions),
                Shapes.block()
        ));
        // BlockDummyable placed its core at clicked - facing * getOffset().
        HbmItems.ITEMS.register(name, () -> switch (name) {
            case "machine_annihilator", "machine_autosaw", "machine_thresher", "machine_lpw2", "machine_forcefield", "machine_missile_assembly",
                 "machine_orbus", "machine_precass", "machine_pyrooven",
                 "machine_radiolysis", "machine_sawmill" ->
                    new LegacyMachineRendererBlockItem(block.get(), new Item.Properties(), offset);
            case "machine_turbofan" ->
                    new LegacyMachineRendererBlockItem(block.get(), new Item.Properties(), offset, false);
            case "machine_rtg_grey" -> new ObjMachineLegacyOffsetBlockItem(block.get(), new Item.Properties(), offset, true);
            case "machine_radar", "machine_radar_large", "machine_radgen" ->
                    new LegacyMachineRendererBlockItem(block.get(), new Item.Properties(), offset);
            default -> new LegacyOffsetBlockItem(block.get(), new Item.Properties(), offset, true);
        });
        return block;
    }

    private static DeferredBlock<Block> sawmillMachine(String name, int[] dimensions, int offset) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new com.reinhardt.hbm.block.SawmillBlock(
                metal().strength(5.0F, 10.0F).noOcclusion(), legacyMachineFootprint(name, dimensions)
        ));
        HbmItems.ITEMS.register(name, () -> new LegacyMachineRendererBlockItem(block.get(), new Item.Properties(), offset));
        return block;
    }

    /** 1.7.10 MachineConveyorPress: a dedicated three-block OBJ machine. */
    private static DeferredBlock<Block> conveyorPress(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new ConveyorPressBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new ConveyorPressBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<ConveyorBlock> conveyor(String name, ConveyorBlock.Kind kind) {
        DeferredBlock<ConveyorBlock> block = registerBlockWithoutItem(name, () -> new ConveyorBlock(kind));
        HbmItems.ITEMS.register(name, () -> new ConveyorBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> radarScreen(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new RadarScreenBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new RadarScreenBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /**
     * Direct translation of the 1.7.10 BlockDummyable fillSpace/makeExtra
     * calls. Coordinates are in the old SOUTH-forward local basis.
     */
    private static LargeMachineBlock.Footprint legacyMachineFootprint(String name, int[] dimensions) {
        Set<BlockPos> positions = new LinkedHashSet<>(LargeMachineBlock.Footprint.legacySouthBox(
                dimensions[0], dimensions[1], dimensions[2], dimensions[3], dimensions[4], dimensions[5]
        ).offsets());

        switch (name) {
            case "machine_annihilator" -> {
                // MachineAnnihilator: secondary tower is centered three blocks behind the core.
                addLegacyFootprintBox(positions, -1, 1, 2, 8, -4, -2);
                positions.add(new BlockPos(1, 0, 3));
                positions.add(new BlockPos(-1, 0, 3));
                positions.add(new BlockPos(0, 0, 4));
            }
            case "machine_pyrooven" -> {
                // MachinePyroOven: five lateral service blocks and one upper service block.
                for (int z = -2; z <= 2; z++) {
                    positions.add(new BlockPos(-2, 0, z));
                }
                positions.add(new BlockPos(1, 2, 0));
            }
            case "machine_precass" -> {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        positions.add(new BlockPos(x, 0, z));
                    }
                }
            }
            default -> {
            }
        }
        return new LargeMachineBlock.Footprint(List.copyOf(positions));
    }

    private static void addLegacyFootprintBox(
            Set<BlockPos> positions,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static DeferredBlock<Block> rbmkComponent(String name, RbmkComponentBlock.Kind kind) {
        if (kind.isColumn()) {
            DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new RbmkComponentBlock(metal()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(5.0F, 10.0F)
                    .noOcclusion(), kind));
            HbmItems.ITEMS.register(name, () -> new RbmkFuelChannelBlockItem(block.get(), new Item.Properties(), kind));
            return block;
        }
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new RbmkComponentBlock(metal()
                .mapColor(kind.isColumn() ? MapColor.COLOR_GRAY : MapColor.METAL)
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind));
        HbmItems.ITEMS.register(name, () -> new RbmkComponentBlockItem(block.get(), new Item.Properties(), kind));
        return block;
    }

    private static DeferredBlock<Block> rbmkFuelChannelComponent(String name, RbmkComponentBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new RbmkComponentBlock(metal()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(5.0F, 10.0F)
                .noOcclusion(), kind));
        HbmItems.ITEMS.register(name, () -> new RbmkFuelChannelBlockItem(block.get(), new Item.Properties(), kind));
        return block;
    }

    private static DeferredBlock<Block> wandStructure(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new WandStructureBlock(metal()
                .strength(5.0F, 10.0F)));
        HbmItems.ITEMS.register(name, () -> new WandStructureBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> rbmkDebris(String name, RbmkDebrisBlock.Kind kind) {
        return registerBlock(name, () -> new RbmkDebrisBlock(metal()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(50.0F, 600.0F)
                .noOcclusion()
                .randomTicks(), kind));
    }

    private static DeferredBlock<Block> heaterBlock(String name, HeaterBlockEntity.Kind kind, LargeMachineBlock.Footprint footprint) {
        return registerObjBlock(name, () -> new HeaterBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), footprint, MACHINE_CORE_SHAPE, kind));
    }

    private static DeferredBlock<Block> heaterBlock(String name, HeaterBlockEntity.Kind kind, LargeMachineBlock.Footprint footprint, LargeMachineBlock.Footprint cleanupFootprint) {
        return registerObjBlock(name, () -> new HeaterBlock(metal()
                .strength(5.0F, 10.0F)
                .noOcclusion(), footprint, cleanupFootprint, MACHINE_CORE_SHAPE, kind));
    }

    private static LargeMachineBlock.Footprint oilburnerFootprint() {
        return LargeMachineBlock.Footprint.box(-1, 1, 0, 1, -1, 1);
    }

    private static LargeMachineBlock.Footprint electricHeaterFootprint() {
        return LargeMachineBlock.Footprint.box(-1, 1, 0, 0, -2, 1);
    }

    private static LargeMachineBlock.Footprint legacyElectricHeaterFootprint() {
        return LargeMachineBlock.Footprint.box(-1, 1, 0, 0, -1, 2);
    }

    private static LargeMachineBlock.Footprint heatexFootprint() {
        return LargeMachineBlock.Footprint.box(-1, 1, 0, 0, -1, 1);
    }

    private static LargeMachineBlock.Footprint solderingStationFootprint() {
        return LargeMachineBlock.Footprint.fromOffsets(
                BlockPos.ZERO,
                new BlockPos(1, 0, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(1, 0, 1)
        );
    }

    private static final VoxelShape MACHINE_CORE_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.95D, 1.0D);
    private static final VoxelShape CENTRIFUGE_CORE_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.999D, 1.0D);

    private static DeferredBlock<Block> glass(String name, float hardness, float resistance, int light, boolean dropsSelf) {
        return registerBlock(name, () -> new LegacyGlassBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(hardness, resistance)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .lightLevel(state -> light), dropsSelf));
    }

    private static DeferredBlock<Block> glassPane(String name, float hardness, float resistance, boolean dropsSelf) {
        return registerBlock(name, () -> new LegacyGlassPaneBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(hardness, resistance)
                .sound(SoundType.GLASS)
                .noOcclusion(), dropsSelf));
    }

    private static DeferredBlock<Block> legacyVariantSlab(String name, LegacyVariantStrengths.Strength[] strengths) {
        return registerBlock(name, () -> new LegacyVariantSlabBlock(rock()
                .strength(LegacyVariantStrengths.maxHardness(strengths),
                        LegacyVariantStrengths.maxResistance(strengths)), strengths));
    }

    private static DeferredBlock<Block> legacyVariantBlock(String name, LegacyVariantStrengths.Strength[] strengths) {
        return registerBlock(name, () -> new LegacyVariantBlock(rock()
                .strength(LegacyVariantStrengths.maxHardness(strengths),
                        LegacyVariantStrengths.maxResistance(strengths)), strengths));
    }

    private static DeferredBlock<Block> registerVariantBlock(String name, Supplier<? extends Block> block,
                                                              net.minecraft.world.level.block.state.properties.Property<?> property,
                                                              String translationBase,
                                                              String... variants) {
        DeferredBlock<Block> deferredBlock = registerBlockWithoutItem(name, block::get);
        HbmItems.ITEMS.register(name, () -> new LegacyVariantBlockItem(
                deferredBlock.get(), new Item.Properties(), property, translationBase, variants));
        return deferredBlock;
    }

    private static DeferredBlock<Block> crane(String name, CraneMachineBlock.Kind kind) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> new CraneMachineBlock(
                metal().strength(3.0F, 10.0F), kind));
        HbmItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> cmVariant(String name, String translationBase, String[] variants,
                                                  boolean transparent) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name, () -> {
            BlockBehaviour.Properties properties = metal().strength(5.0F, 10.0F);
            if (transparent) {
                properties = properties.noOcclusion();
            }
            return new LegacyVariantBlock(properties, variants.length - 1);
        });
        HbmItems.ITEMS.register(name, () -> new LegacyVariantBlockItem(
                block.get(), new Item.Properties(), LegacyVariantBlock.VARIANT, translationBase, variants));
        return block;
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> deferredBlock = registerBlockWithoutItem(name, block);
        HbmItems.ITEMS.register(name, () -> new BlockItem(deferredBlock.get(), new Item.Properties()));
        return deferredBlock;
    }

    private static <T extends Block> DeferredBlock<T> registerObjBlock(String name, Supplier<T> block) {
        DeferredBlock<T> deferredBlock = registerBlockWithoutItem(name, block);
        HbmItems.ITEMS.register(name, () -> new ObjMachineBlockItem(deferredBlock.get(), new Item.Properties()));
        return deferredBlock;
    }

    private static DeferredBlock<Block> bobblehead(String name) {
        DeferredBlock<Block> deferredBlock = registerBlockWithoutItem(name,
                () -> new BobbleheadBlock(metal().strength(0.0F, 0.0F).noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new BobbleheadBlockItem(deferredBlock.get(), new Item.Properties()));
        return deferredBlock;
    }

    private static DeferredBlock<Block> snowglobe(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new SnowglobeBlock(BlockBehaviour.Properties.of().strength(0.0F, 0.0F)
                        .sound(SoundType.GLASS).noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new SnowglobeBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> plushie(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new PlushieBlock(BlockBehaviour.Properties.of().strength(0.0F, 0.0F)
                        .sound(SoundType.WOOL).noOcclusion()));
        HbmItems.ITEMS.register(name, () -> new PlushieBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> lantern(String name, boolean behemoth) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> behemoth
                        ? new LanternBehemothBlock(metal().strength(3.0F, 10.0F)
                                .sound(SoundType.METAL).noOcclusion())
                        : new LanternBlock(metal().strength(3.0F, 10.0F)
                                .sound(SoundType.METAL).noOcclusion().lightLevel(state -> 15)));
        if (!behemoth) {
            HbmItems.ITEMS.register(name, () -> new LanternBlockItem(block.get(), new Item.Properties()));
        }
        return block;
    }

    private static DeferredBlock<Block> stairs(String name, DeferredBlock<Block> base, BlockBehaviour.Properties properties) {
        return registerBlock(name, () -> new StairBlock(base.get().defaultBlockState(), properties));
    }

    private static DeferredBlock<Block> ladderBlock(String name, MapColor mapColor) {
        return registerBlock(name, () -> new LadderBlock(BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .strength(0.25F, 2.0F)
                .sound(SoundType.LADDER)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)));
    }

    private static DeferredBlock<Block> railBlock(String name, BlockBehaviour.Properties properties, float maxSpeed, boolean flexible, boolean slopable, boolean booster) {
        DeferredBlock<Block> deferredBlock = registerBlockWithoutItem(name, () -> new HbmRailBlock(properties, maxSpeed, flexible, slopable, booster));
        HbmItems.ITEMS.register(name, () -> new HbmRailBlockItem(deferredBlock.get(), new Item.Properties()));
        return deferredBlock;
    }

    private static DeferredBlock<Block> radioTorch(String name, RadioTorchBlock.Kind kind) {
        return registerBlock(name, () -> new RadioTorchBlock(
                metal().strength(0.1F, 10.0F), kind));
    }

    private static DeferredBlock<Block> glyphBlock(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new GlyphBlock(rock().strength(15.0F, 360.0F)));
        HbmItems.ITEMS.register(name, () -> new GlyphBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> caveSpike(String name, boolean hanging) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new CaveSpikeBlock(rock().strength(0.5F, 2.0F).noCollission().noOcclusion(), hanging, HbmItems.SULFUR));
        HbmItems.ITEMS.register(name, () -> new CaveSpikeBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static DeferredBlock<Block> vinylTile(String name) {
        DeferredBlock<Block> block = registerBlockWithoutItem(name,
                () -> new VinylTileBlock(rock().strength(10.0F, 60.0F).sound(SoundType.GLASS)));
        HbmItems.ITEMS.register(name, () -> new ConcreteColoredBlockItem(block.get(), new Item.Properties(),
                "block.reinhardtshbm.vinyl_tile", new String[]{"large", "small"}));
        return block;
    }

    private static BlockBehaviour.Properties gasProperties(MapColor mapColor) {
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .replaceable()
                .strength(0.0F, 0.0F)
                .sound(SoundType.WOOL)
                .noCollission()
                .noOcclusion()
                .noLootTable()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties legacyFluidProperties(MapColor mapColor, int light) {
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .replaceable()
                .strength(100.0F, 500.0F)
                .lightLevel(state -> light)
                .noCollission()
                .noLootTable()
                .pushReaction(PushReaction.DESTROY);
    }

    private static <T extends Block> DeferredBlock<T> registerBlockWithoutItem(String name, Supplier<T> block) {
        CORE_BLOCK_IDS.add(name);
        return BLOCKS.register(name, block);
    }

    public static boolean isCoreBlock(String id) {
        return CORE_BLOCK_IDS.contains(id);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
