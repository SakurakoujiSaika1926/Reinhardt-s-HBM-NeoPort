package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.AssemblyFactoryBlockEntity;
import com.reinhardt.hbm.blockentity.AssemblyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.AirCompressorBlockEntity;
import com.reinhardt.hbm.blockentity.AmmoPressBlockEntity;
import com.reinhardt.hbm.blockentity.ArcWelderBlockEntity;
import com.reinhardt.hbm.blockentity.ArcFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.StorageDrumBlockEntity;
import com.reinhardt.hbm.blockentity.AshpitBlockEntity;
import com.reinhardt.hbm.blockentity.BatteryReddBlockEntity;
import com.reinhardt.hbm.blockentity.BatterySocketBlockEntity;
import com.reinhardt.hbm.blockentity.CapacitorBlockEntity;
import com.reinhardt.hbm.blockentity.CapacitorBusBlockEntity;
import com.reinhardt.hbm.blockentity.ChargerBlockEntity;
import com.reinhardt.hbm.blockentity.BrickFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.BedrockOreBlockEntity;
import com.reinhardt.hbm.blockentity.BigAssTankBlockEntity;
import com.reinhardt.hbm.blockentity.BlastDoorBlockEntity;
import com.reinhardt.hbm.blockentity.BlastDoorDummyBlockEntity;
import com.reinhardt.hbm.blockentity.BlastFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.BreederReactorBlockEntity;
import com.reinhardt.hbm.blockentity.BroadcasterBlockEntity;
import com.reinhardt.hbm.blockentity.GeigerBlockEntity;
import com.reinhardt.hbm.blockentity.BobbleheadBlockEntity;
import com.reinhardt.hbm.blockentity.SnowglobeBlockEntity;
import com.reinhardt.hbm.blockentity.PlushieBlockEntity;
import com.reinhardt.hbm.blockentity.LanternBlockEntity;
import com.reinhardt.hbm.blockentity.LanternBehemothBlockEntity;
import com.reinhardt.hbm.blockentity.CatalyticCrackerBlockEntity;
import com.reinhardt.hbm.blockentity.CatalyticReformerBlockEntity;
import com.reinhardt.hbm.blockentity.HydrotreaterBlockEntity;
import com.reinhardt.hbm.blockentity.ChimneyBlockEntity;
import com.reinhardt.hbm.blockentity.CentrifugeBlockEntity;
import com.reinhardt.hbm.blockentity.ChemicalFactoryBlockEntity;
import com.reinhardt.hbm.blockentity.ChemicalPlantBlockEntity;
import com.reinhardt.hbm.blockentity.CableSwitchBlockEntity;
import com.reinhardt.hbm.blockentity.CableDiodeBlockEntity;
import com.reinhardt.hbm.blockentity.PaintableCableBlockEntity;
import com.reinhardt.hbm.blockentity.PowerGaugeBlockEntity;
import com.reinhardt.hbm.blockentity.CombustionEngineBlockEntity;
import com.reinhardt.hbm.blockentity.CompressorBlockEntity;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.CokerBlockEntity;
import com.reinhardt.hbm.blockentity.CoolingTowerBlockEntity;
import com.reinhardt.hbm.blockentity.CrystallizerBlockEntity;
import com.reinhardt.hbm.blockentity.CyclotronBlockEntity;
import com.reinhardt.hbm.blockentity.DeuteriumExtractorBlockEntity;
import com.reinhardt.hbm.blockentity.DeuteriumTowerBlockEntity;
import com.reinhardt.hbm.blockentity.DfcCoreBlockEntity;
import com.reinhardt.hbm.blockentity.DfcEmitterBlockEntity;
import com.reinhardt.hbm.blockentity.DfcInjectorBlockEntity;
import com.reinhardt.hbm.blockentity.DfcReceiverBlockEntity;
import com.reinhardt.hbm.blockentity.DfcStabilizerBlockEntity;
import com.reinhardt.hbm.blockentity.ElectricFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.ElectrolyzerBlockEntity;
import com.reinhardt.hbm.blockentity.EnergyConverterBlockEntity;
import com.reinhardt.hbm.blockentity.ExcavatorBlockEntity;
import com.reinhardt.hbm.blockentity.ExposureChamberBlockEntity;
import com.reinhardt.hbm.blockentity.FelBlockEntity;
import com.reinhardt.hbm.blockentity.DieselGeneratorBlockEntity;
import com.reinhardt.hbm.blockentity.DeconBlockEntity;
import com.reinhardt.hbm.blockentity.DrainBlockEntity;
import com.reinhardt.hbm.blockentity.DroneWaypointBlockEntity;
import com.reinhardt.hbm.blockentity.DroneRequestWaypointBlockEntity;
import com.reinhardt.hbm.blockentity.DroneCrateBlockEntity;
import com.reinhardt.hbm.blockentity.DroneDockBlockEntity;
import com.reinhardt.hbm.blockentity.DroneProviderBlockEntity;
import com.reinhardt.hbm.blockentity.DroneRequesterBlockEntity;
import com.reinhardt.hbm.blockentity.FluidBarrelBlockEntity;
import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
import com.reinhardt.hbm.blockentity.PipeAnchorBlockEntity;
import com.reinhardt.hbm.blockentity.PistonInserterBlockEntity;
import com.reinhardt.hbm.blockentity.FluidPumpBlockEntity;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.blockentity.FractionSpacerBlockEntity;
import com.reinhardt.hbm.blockentity.FractionTowerBlockEntity;
import com.reinhardt.hbm.blockentity.FunnelBlockEntity;
import com.reinhardt.hbm.blockentity.SirenBlockEntity;
import com.reinhardt.hbm.blockentity.FoundryCastingBlockEntity;
import com.reinhardt.hbm.blockentity.FoundryFlowBlockEntity;
import com.reinhardt.hbm.blockentity.FoundrySlagBlockEntity;
import com.reinhardt.hbm.blockentity.FoundryTankBlockEntity;
import com.reinhardt.hbm.blockentity.FurnaceCombinationBlockEntity;
import com.reinhardt.hbm.blockentity.FrackingTowerBlockEntity;
import com.reinhardt.hbm.blockentity.FissureBlockEntity;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.blockentity.FusionTorusStructBlockEntity;
import com.reinhardt.hbm.blockentity.GasCentrifugeBlockEntity;
import com.reinhardt.hbm.blockentity.GasFlareBlockEntity;
import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.GroundwaterPumpBlockEntity;
import com.reinhardt.hbm.blockentity.GeothermalHeatExchangerBlockEntity;
import com.reinhardt.hbm.blockentity.HeatBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.blockentity.HexafluorideTankBlockEntity;
import com.reinhardt.hbm.blockentity.CrucibleBlockEntity;
import com.reinhardt.hbm.blockentity.IndustrialTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.IcfPressBlockEntity;
import com.reinhardt.hbm.blockentity.IcfCoreBlockEntity;
import com.reinhardt.hbm.blockentity.IcfStructBlockEntity;
import com.reinhardt.hbm.blockentity.IcfControllerBlockEntity;
import com.reinhardt.hbm.blockentity.IcfAssembledLaserBlockEntity;
import com.reinhardt.hbm.blockentity.IronFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.IndustrialBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.LargeTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.LargeFluidTankBlockEntity;
import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import com.reinhardt.hbm.blockentity.LauncherStructCoreBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import com.reinhardt.hbm.blockentity.LeviathanTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.LiquefactorBlockEntity;
import com.reinhardt.hbm.blockentity.LandmineBlockEntity;
import com.reinhardt.hbm.blockentity.WallChargeBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyEmitterBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineBlastFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineKeyForgeBlockEntity;
import com.reinhardt.hbm.blockentity.SatelliteLinkerBlockEntity;
import com.reinhardt.hbm.blockentity.SatelliteDockBlockEntity;
import com.reinhardt.hbm.blockentity.SatelliteDockDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MiningLaserBlockEntity;
import com.reinhardt.hbm.blockentity.MixerBlockEntity;
import com.reinhardt.hbm.blockentity.MicrowaveBlockEntity;
import com.reinhardt.hbm.blockentity.NukeBoyBlockEntity;
import com.reinhardt.hbm.blockentity.BombMultiBlockEntity;
import com.reinhardt.hbm.blockentity.CrashedBombBlockEntity;
import com.reinhardt.hbm.blockentity.OilDerrickBlockEntity;
import com.reinhardt.hbm.blockentity.OreSlopperBlockEntity;
import com.reinhardt.hbm.blockentity.ParticleAcceleratorBlockEntity;
import com.reinhardt.hbm.blockentity.PileGraphiteBlockEntity;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import com.reinhardt.hbm.blockentity.PoleBlockEntity;
import com.reinhardt.hbm.blockentity.PoweredSteamCondenserBlockEntity;
import com.reinhardt.hbm.blockentity.PressBlockEntity;
import com.reinhardt.hbm.blockentity.PowerDetectorBlockEntity;
import com.reinhardt.hbm.blockentity.PurexBlockEntity;
import com.reinhardt.hbm.blockentity.PwrBlockEntity;
import com.reinhardt.hbm.blockentity.PwrControllerBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.blockentity.RadarScreenBlockEntity;
import com.reinhardt.hbm.blockentity.RefineryBlockEntity;
import com.reinhardt.hbm.blockentity.RebarBlockEntity;
import com.reinhardt.hbm.blockentity.ReactorControlBlockEntity;
import com.reinhardt.hbm.blockentity.RotaryFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.VacuumDistillBlockEntity;
import com.reinhardt.hbm.blockentity.VolcanoCoreBlockEntity;
import com.reinhardt.hbm.blockentity.WasteDrumBlockEntity;
import com.reinhardt.hbm.blockentity.DecoLootBlockEntity;
import com.reinhardt.hbm.blockentity.DecoDisplayBlockEntity;
import com.reinhardt.hbm.blockentity.FilingCabinetBlockEntity;
import com.reinhardt.hbm.blockentity.LogicBlockEntity;
import com.reinhardt.hbm.blockentity.WandJigsawBlockEntity;
import com.reinhardt.hbm.blockentity.WandLogicBlockEntity;
import com.reinhardt.hbm.blockentity.WandLootBlockEntity;
import com.reinhardt.hbm.blockentity.WandStructureBlockEntity;
import com.reinhardt.hbm.blockentity.WandTandemBlockEntity;
import com.reinhardt.hbm.blockentity.ResearchReactorBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.blockentity.SolderingStationBlockEntity;
import com.reinhardt.hbm.blockentity.SilexBlockEntity;
import com.reinhardt.hbm.blockentity.SmallBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.SmallElectricBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.SolidifierBlockEntity;
import com.reinhardt.hbm.blockentity.SolarBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.SolarMirrorBlockEntity;
import com.reinhardt.hbm.blockentity.SoyuzCapsuleBlockEntity;
import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import com.reinhardt.hbm.blockentity.SoyuzStructCoreBlockEntity;
import com.reinhardt.hbm.blockentity.SteamCondenserBlockEntity;
import com.reinhardt.hbm.blockentity.SteamEngineBlockEntity;
import com.reinhardt.hbm.blockentity.SteamTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.SteelFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import com.reinhardt.hbm.blockentity.TapeRecorderBlockEntity;
import com.reinhardt.hbm.blockentity.TeslaCoilBlockEntity;
import com.reinhardt.hbm.blockentity.SafeBlockEntity;
import com.reinhardt.hbm.blockentity.MassStorageBlockEntity;
import com.reinhardt.hbm.blockentity.StrandCasterBlockEntity;
import com.reinhardt.hbm.blockentity.StirlingGeneratorBlockEntity;
import com.reinhardt.hbm.blockentity.TurretChekhovBlockEntity;
import com.reinhardt.hbm.blockentity.TurretJeremyBlockEntity;
import com.reinhardt.hbm.blockentity.WatzBlockEntity;
import com.reinhardt.hbm.blockentity.WatzPumpBlockEntity;
import com.reinhardt.hbm.blockentity.WatzStructBlockEntity;
import com.reinhardt.hbm.blockentity.RadioTorchBlockEntity;
import com.reinhardt.hbm.blockentity.WoodBurnerBlockEntity;
import com.reinhardt.hbm.blockentity.ZirnoxDestroyedBlockEntity;
import com.reinhardt.hbm.blockentity.ZirnoxReactorBlockEntity;
import com.reinhardt.hbm.blockentity.SpotlightBeamBlockEntity;
import com.reinhardt.hbm.blockentity.SpotlightBlockEntity;
import com.reinhardt.hbm.blockentity.FanBlockEntity;
import com.reinhardt.hbm.blockentity.FloodlightBeamBlockEntity;
import com.reinhardt.hbm.blockentity.FloodlightBlockEntity;
import com.reinhardt.hbm.blockentity.CargoElevatorBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyDisplayStandBlockEntity;
import com.reinhardt.hbm.blockentity.SealHatchBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodBurnerBlockEntity>> WOOD_BURNER =
            BLOCK_ENTITIES.register(
                    "machine_wood_burner",
                    () -> BlockEntityType.Builder.of(
                            WoodBurnerBlockEntity::new,
                            HbmBlocks.MACHINE_WOOD_BURNER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeAnchorBlockEntity>> PIPE_ANCHOR =
            BLOCK_ENTITIES.register(
                    "pipe_anchor",
                    () -> BlockEntityType.Builder.of(
                            PipeAnchorBlockEntity::new,
                            HbmBlocks.PIPE_ANCHOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PistonInserterBlockEntity>> PISTON_INSERTER =
            BLOCK_ENTITIES.register(
                    "piston_inserter",
                    () -> BlockEntityType.Builder.of(
                            PistonInserterBlockEntity::new,
                            HbmBlocks.PISTON_INSERTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PileGraphiteBlockEntity>> PILE_GRAPHITE =
            BLOCK_ENTITIES.register(
                    "pile_graphite",
                    () -> BlockEntityType.Builder.of(
                            PileGraphiteBlockEntity::new,
                            HbmBlocks.BLOCK_GRAPHITE_FUEL.get(),
                            HbmBlocks.BLOCK_GRAPHITE_PLUTONIUM.get(),
                            HbmBlocks.BLOCK_GRAPHITE_SOURCE.get(),
                            HbmBlocks.BLOCK_GRAPHITE_LITHIUM.get(),
                            HbmBlocks.BLOCK_GRAPHITE_DETECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioTorchBlockEntity>> RADIO_TORCH =
            BLOCK_ENTITIES.register(
                    "radio_torch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new RadioTorchBlockEntity(
                                    pos,
                                    state,
                                    state.getBlock() instanceof com.reinhardt.hbm.block.RadioTorchBlock radio
                                            ? radio.kind()
                                            : com.reinhardt.hbm.block.RadioTorchBlock.Kind.RECEIVER),
                            HbmBlocks.RADIO_TORCH_SENDER.get(),
                            HbmBlocks.RADIO_TORCH_RECEIVER.get(),
                            HbmBlocks.RADIO_TORCH_COUNTER.get(),
                            HbmBlocks.RADIO_TORCH_LOGIC.get(),
                            HbmBlocks.RADIO_TORCH_READER.get(),
                            HbmBlocks.RADIO_TORCH_CONTROLLER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpotlightBeamBlockEntity>> SPOTLIGHT_BEAM =
            BLOCK_ENTITIES.register(
                    "spotlight_beam",
                    () -> BlockEntityType.Builder.of(
                            SpotlightBeamBlockEntity::new,
                            HbmBlocks.SPOTLIGHT_BEAM.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpotlightBlockEntity>> SPOTLIGHT =
            BLOCK_ENTITIES.register(
                    "spotlight",
                    () -> BlockEntityType.Builder.of(
                            SpotlightBlockEntity::new,
                            HbmBlocks.SPOTLIGHT_INCANDESCENT.get(),
                            HbmBlocks.SPOTLIGHT_INCANDESCENT_OFF.get(),
                            HbmBlocks.SPOTLIGHT_FLUORO.get(),
                            HbmBlocks.SPOTLIGHT_FLUORO_OFF.get(),
                            HbmBlocks.SPOTLIGHT_HALOGEN.get(),
                            HbmBlocks.SPOTLIGHT_HALOGEN_OFF.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FanBlockEntity>> FAN =
            BLOCK_ENTITIES.register(
                    "fan",
                    () -> BlockEntityType.Builder.of(FanBlockEntity::new, HbmBlocks.FAN.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloodlightBlockEntity>> FLOODLIGHT =
            BLOCK_ENTITIES.register(
                    "floodlight",
                    () -> BlockEntityType.Builder.of(FloodlightBlockEntity::new, HbmBlocks.FLOODLIGHT.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloodlightBeamBlockEntity>> FLOODLIGHT_BEAM =
            BLOCK_ENTITIES.register(
                    "floodlight_beam",
                    () -> BlockEntityType.Builder.of(FloodlightBeamBlockEntity::new, HbmBlocks.FLOODLIGHT_BEAM.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CargoElevatorBlockEntity>> CARGO_ELEVATOR =
            BLOCK_ENTITIES.register(
                    "cargo_elevator",
                    () -> BlockEntityType.Builder.of(CargoElevatorBlockEntity::new, HbmBlocks.CARGO_ELEVATOR.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LandmineBlockEntity>> LANDMINE =
            BLOCK_ENTITIES.register(
                    "landmine",
                    () -> BlockEntityType.Builder.of(
                            LandmineBlockEntity::new,
                            HbmBlocks.MINE_AP.get(),
                            HbmBlocks.MINE_HE.get(),
                            HbmBlocks.MINE_SHRAP.get(),
                            HbmBlocks.MINE_FAT.get(),
                            HbmBlocks.MINE_NAVAL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallChargeBlockEntity>> WALL_CHARGE =
            BLOCK_ENTITIES.register(
                    "wall_charge",
                    () -> BlockEntityType.Builder.of(
                            WallChargeBlockEntity::new,
                            HbmBlocks.CHARGE_DYNAMITE.get(),
                            HbmBlocks.CHARGE_MINER.get(),
                            HbmBlocks.CHARGE_C4.get(),
                            HbmBlocks.CHARGE_SEMTEX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_electric_furnace",
                    () -> BlockEntityType.Builder.of(
                            ElectricFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_ELECTRIC_FURNACE_OFF.get(),
                            HbmBlocks.MACHINE_ELECTRIC_FURNACE_ON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShredderBlockEntity>> SHREDDER =
            BLOCK_ENTITIES.register(
                    "machine_shredder",
                    () -> BlockEntityType.Builder.of(
                            ShredderBlockEntity::new,
                            HbmBlocks.MACHINE_SHREDDER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MicrowaveBlockEntity>> MICROWAVE =
            BLOCK_ENTITIES.register(
                    "machine_microwave",
                    () -> BlockEntityType.Builder.of(
                            MicrowaveBlockEntity::new,
                            HbmBlocks.MACHINE_MICROWAVE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BroadcasterBlockEntity>> BROADCASTER =
            BLOCK_ENTITIES.register(
                    "broadcaster_pc",
                    () -> BlockEntityType.Builder.of(BroadcasterBlockEntity::new, HbmBlocks.BROADCASTER_PC.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeigerBlockEntity>> GEIGER =
            BLOCK_ENTITIES.register(
                    "geiger",
                    () -> BlockEntityType.Builder.of(GeigerBlockEntity::new, HbmBlocks.GEIGER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BobbleheadBlockEntity>> BOBBLEHEAD =
            BLOCK_ENTITIES.register(
                    "bobblehead",
                    () -> BlockEntityType.Builder.of(BobbleheadBlockEntity::new, HbmBlocks.BOBBLEHEAD.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SnowglobeBlockEntity>> SNOWGLOBE =
            BLOCK_ENTITIES.register(
                    "snowglobe",
                    () -> BlockEntityType.Builder.of(SnowglobeBlockEntity::new, HbmBlocks.SNOWGLOBE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlushieBlockEntity>> PLUSHIE =
            BLOCK_ENTITIES.register(
                    "plushie",
                    () -> BlockEntityType.Builder.of(PlushieBlockEntity::new, HbmBlocks.PLUSHIE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LanternBlockEntity>> LANTERN =
            BLOCK_ENTITIES.register(
                    "lantern",
                    () -> BlockEntityType.Builder.of(LanternBlockEntity::new, HbmBlocks.LANTERN.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LanternBehemothBlockEntity>> LANTERN_BEHEMOTH =
            BLOCK_ENTITIES.register(
                    "lantern_behemoth",
                    () -> BlockEntityType.Builder.of(LanternBehemothBlockEntity::new,
                            HbmBlocks.LANTERN_BEHEMOTH.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineKeyForgeBlockEntity>> MACHINE_KEYFORGE =
            BLOCK_ENTITIES.register(
                    "machine_keyforge",
                    () -> BlockEntityType.Builder.of(
                            MachineKeyForgeBlockEntity::new,
                            HbmBlocks.MACHINE_KEYFORGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatelliteLinkerBlockEntity>> SATELLITE_LINKER =
            BLOCK_ENTITIES.register(
                    "machine_satlinker",
                    () -> BlockEntityType.Builder.of(
                            SatelliteLinkerBlockEntity::new,
                            HbmBlocks.MACHINE_SATLINKER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatelliteDockBlockEntity>> SAT_DOCK =
            BLOCK_ENTITIES.register(
                    "sat_dock",
                    () -> BlockEntityType.Builder.of(SatelliteDockBlockEntity::new, HbmBlocks.SAT_DOCK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatelliteDockDummyBlockEntity>> SAT_DOCK_DUMMY =
            BLOCK_ENTITIES.register(
                    "dummy_plate_cargo",
                    () -> BlockEntityType.Builder.of(SatelliteDockDummyBlockEntity::new, HbmBlocks.DUMMY_PLATE_CARGO.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyMachineBlockEntity>> ASSEMBLY_MACHINE =
            BLOCK_ENTITIES.register(
                    "machine_assembly_machine",
                    () -> BlockEntityType.Builder.of(
                            AssemblyMachineBlockEntity::new,
                            HbmBlocks.MACHINE_ASSEMBLY_MACHINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyFactoryBlockEntity>> ASSEMBLY_FACTORY =
            BLOCK_ENTITIES.register(
                    "machine_assembly_factory",
                    () -> BlockEntityType.Builder.of(
                            AssemblyFactoryBlockEntity::new,
                            HbmBlocks.MACHINE_ASSEMBLY_FACTORY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalPlantBlockEntity>> CHEMICAL_PLANT =
            BLOCK_ENTITIES.register(
                    "machine_chemical_plant",
                    () -> BlockEntityType.Builder.of(
                            ChemicalPlantBlockEntity::new,
                            HbmBlocks.MACHINE_CHEMICAL_PLANT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalFactoryBlockEntity>> CHEMICAL_FACTORY =
            BLOCK_ENTITIES.register(
                    "machine_chemical_factory",
                    () -> BlockEntityType.Builder.of(
                            ChemicalFactoryBlockEntity::new,
                            HbmBlocks.MACHINE_CHEMICAL_FACTORY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolderingStationBlockEntity>> SOLDERING_STATION =
            BLOCK_ENTITIES.register(
                    "machine_soldering_station",
                    () -> BlockEntityType.Builder.of(
                            SolderingStationBlockEntity::new,
                            HbmBlocks.MACHINE_SOLDERING_STATION.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcWelderBlockEntity>> ARC_WELDER =
            BLOCK_ENTITIES.register(
                    "machine_arc_welder",
                    () -> BlockEntityType.Builder.of(
                            ArcWelderBlockEntity::new,
                            HbmBlocks.MACHINE_ARC_WELDER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcFurnaceBlockEntity>> ARC_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_arc_furnace",
                    () -> BlockEntityType.Builder.of(
                            ArcFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_ARC_FURNACE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageDrumBlockEntity>> STORAGE_DRUM =
            BLOCK_ENTITIES.register(
                    "machine_storage_drum",
                    () -> BlockEntityType.Builder.of(
                            StorageDrumBlockEntity::new,
                            HbmBlocks.MACHINE_STORAGE_DRUM.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorBlockEntity>> COMPRESSOR =
            BLOCK_ENTITIES.register(
                    "machine_compressor",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> {
                                if (state.getBlock() instanceof com.reinhardt.hbm.block.CompressorBlock compressorBlock) {
                                    return new CompressorBlockEntity(pos, state, compressorBlock.kind());
                                }
                                return new CompressorBlockEntity(pos, state, CompressorBlockEntity.Kind.NORMAL);
                            },
                            HbmBlocks.MACHINE_COMPRESSOR.get(),
                            HbmBlocks.MACHINE_COMPRESSOR_COMPACT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerBlockEntity>> MIXER =
            BLOCK_ENTITIES.register(
                    "machine_mixer",
                    () -> BlockEntityType.Builder.of(
                            MixerBlockEntity::new,
                            HbmBlocks.MACHINE_MIXER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoPressBlockEntity>> AMMO_PRESS =
            BLOCK_ENTITIES.register(
                    "machine_ammo_press",
                    () -> BlockEntityType.Builder.of(
                            AmmoPressBlockEntity::new,
                            HbmBlocks.MACHINE_AMMO_PRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressBlockEntity>> PRESS =
            BLOCK_ENTITIES.register(
                    "press",
                    () -> BlockEntityType.Builder.of(
                            PressBlockEntity::new,
                            HbmBlocks.MACHINE_PRESS.get(),
                            HbmBlocks.MACHINE_EPRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE =
            BLOCK_ENTITIES.register(
                    "machine_centrifuge",
                    () -> BlockEntityType.Builder.of(
                            CentrifugeBlockEntity::new,
                            HbmBlocks.MACHINE_CENTRIFUGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasCentrifugeBlockEntity>> GAS_CENTRIFUGE =
            BLOCK_ENTITIES.register(
                    "machine_gascent",
                    () -> BlockEntityType.Builder.of(
                            GasCentrifugeBlockEntity::new,
                            HbmBlocks.MACHINE_GASCENT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystallizerBlockEntity>> CRYSTALLIZER =
            BLOCK_ENTITIES.register(
                    "machine_crystallizer",
                    () -> BlockEntityType.Builder.of(
                            CrystallizerBlockEntity::new,
                            HbmBlocks.MACHINE_CRYSTALLIZER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CyclotronBlockEntity>> CYCLOTRON =
            BLOCK_ENTITIES.register(
                    "machine_cyclotron",
                    () -> BlockEntityType.Builder.of(
                            CyclotronBlockEntity::new,
                            HbmBlocks.MACHINE_CYCLOTRON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExposureChamberBlockEntity>> EXPOSURE_CHAMBER =
            BLOCK_ENTITIES.register(
                    "machine_exposure_chamber",
                    () -> BlockEntityType.Builder.of(
                            ExposureChamberBlockEntity::new,
                            HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeuteriumExtractorBlockEntity>> DEUTERIUM_EXTRACTOR =
            BLOCK_ENTITIES.register(
                    "machine_deuterium_extractor",
                    () -> BlockEntityType.Builder.of(
                            DeuteriumExtractorBlockEntity::new,
                            HbmBlocks.MACHINE_DEUTERIUM_EXTRACTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeuteriumTowerBlockEntity>> DEUTERIUM_TOWER =
            BLOCK_ENTITIES.register(
                    "machine_deuterium_tower",
                    () -> BlockEntityType.Builder.of(
                            DeuteriumTowerBlockEntity::new,
                            HbmBlocks.MACHINE_DEUTERIUM_TOWER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParticleAcceleratorBlockEntity>> PARTICLE_ACCELERATOR =
            BLOCK_ENTITIES.register(
                    "particle_accelerator",
                    () -> BlockEntityType.Builder.of(
                            ParticleAcceleratorBlockEntity::new,
                            HbmBlocks.PA_SOURCE.get(),
                            HbmBlocks.PA_BEAMLINE.get(),
                            HbmBlocks.PA_RFC.get(),
                            HbmBlocks.PA_QUADRUPOLE.get(),
                            HbmBlocks.PA_DIPOLE.get(),
                            HbmBlocks.PA_DETECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SilexBlockEntity>> SILEX =
            BLOCK_ENTITIES.register(
                    "machine_silex",
                    () -> BlockEntityType.Builder.of(
                            SilexBlockEntity::new,
                            HbmBlocks.MACHINE_SILEX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FelBlockEntity>> FEL =
            BLOCK_ENTITIES.register(
                    "machine_fel",
                    () -> BlockEntityType.Builder.of(
                            FelBlockEntity::new,
                            HbmBlocks.MACHINE_FEL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiningLaserBlockEntity>> MINING_LASER =
            BLOCK_ENTITIES.register(
                    "machine_mining_laser",
                    () -> BlockEntityType.Builder.of(
                            MiningLaserBlockEntity::new,
                            HbmBlocks.MACHINE_MINING_LASER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeslaCoilBlockEntity>> TESLA_COIL =
            BLOCK_ENTITIES.register(
                    "tesla",
                    () -> BlockEntityType.Builder.of(
                            TeslaCoilBlockEntity::new,
                            HbmBlocks.TESLA.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OreSlopperBlockEntity>> ORE_SLOPPER =
            BLOCK_ENTITIES.register(
                    "machine_ore_slopper",
                    () -> BlockEntityType.Builder.of(
                            OreSlopperBlockEntity::new,
                            HbmBlocks.MACHINE_ORE_SLOPPER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExcavatorBlockEntity>> EXCAVATOR =
            BLOCK_ENTITIES.register(
                    "machine_excavator",
                    () -> BlockEntityType.Builder.of(
                            ExcavatorBlockEntity::new,
                            HbmBlocks.MACHINE_EXCAVATOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BedrockOreBlockEntity>> BEDROCK_ORE =
            BLOCK_ENTITIES.register(
                    "ore_bedrock_block",
                    () -> BlockEntityType.Builder.of(
                            BedrockOreBlockEntity::new,
                            HbmBlocks.ORE_BEDROCK_BLOCK.get(),
                            HbmBlocks.ORE_BEDROCK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreederReactorBlockEntity>> BREEDER_REACTOR =
            BLOCK_ENTITIES.register(
                    "machine_reactor_breeding",
                    () -> BlockEntityType.Builder.of(
                            BreederReactorBlockEntity::new,
                            HbmBlocks.MACHINE_REACTOR_BREEDING.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurretJeremyBlockEntity>> TURRET_JEREMY =
            BLOCK_ENTITIES.register(
                    "turret_jeremy",
                    () -> BlockEntityType.Builder.of(
                            TurretJeremyBlockEntity::new,
                            HbmBlocks.TURRET_JEREMY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurretChekhovBlockEntity>> TURRET_CHEKHOV =
            BLOCK_ENTITIES.register(
                    "turret_chekhov",
                    () -> BlockEntityType.Builder.of(
                            TurretChekhovBlockEntity::new,
                            HbmBlocks.TURRET_CHEKHOV.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyTurretBlockEntity>> LEGACY_TURRET =
            BLOCK_ENTITIES.register(
                    "legacy_turret",
                    () -> BlockEntityType.Builder.of(
                            LegacyTurretBlockEntity::new,
                            HbmBlocks.TURRET_FRIENDLY.get(),
                            HbmBlocks.TURRET_FRITZ.get(),
                            HbmBlocks.TURRET_HOWARD.get(),
                            HbmBlocks.TURRET_HOWARD_DAMAGED.get(),
                            HbmBlocks.TURRET_MAXWELL.get(),
                            HbmBlocks.TURRET_RICHARD.get(),
                            HbmBlocks.TURRET_TAUON.get(),
                            HbmBlocks.TURRET_ARTY.get(),
                            HbmBlocks.TURRET_HIMARS.get(),
                            HbmBlocks.TURRET_SENTRY.get(),
                            HbmBlocks.TURRET_SENTRY_DAMAGED.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NukeBoyBlockEntity>> NUKE_BOY =
            BLOCK_ENTITIES.register(
                    "nuke_boy",
                    () -> BlockEntityType.Builder.of(
                            NukeBoyBlockEntity::new,
                            HbmBlocks.NUKE_BOY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BombMultiBlockEntity>> BOMB_MULTI =
            BLOCK_ENTITIES.register(
                    "bomb_multi",
                    () -> BlockEntityType.Builder.of(
                            BombMultiBlockEntity::new,
                            HbmBlocks.BOMB_MULTI.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrashedBombBlockEntity>> CRASHED_BOMB =
            BLOCK_ENTITIES.register(
                    "crashed_bomb",
                    () -> BlockEntityType.Builder.of(
                            CrashedBombBlockEntity::new,
                            HbmBlocks.CRASHED_BOMB.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LauncherBlockEntity>> LAUNCHER =
            BLOCK_ENTITIES.register(
                    "launcher",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> {
                                if (state.is(HbmBlocks.LAUNCH_PAD_LARGE.get())) {
                                    return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.PAD_LARGE);
                                }
                                if (state.is(HbmBlocks.LAUNCH_PAD_RUSTED.get())) {
                                    return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.PAD_RUSTED);
                                }
                                if (state.is(HbmBlocks.COMPACT_LAUNCHER.get())) {
                                    return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.COMPACT);
                                }
                                if (state.is(HbmBlocks.LAUNCH_TABLE.get())) {
                                    return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.TABLE);
                                }
                                return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.PAD_SMALL);
                            },
                            HbmBlocks.LAUNCH_PAD.get(),
                            HbmBlocks.LAUNCH_PAD_LARGE.get(),
                            HbmBlocks.LAUNCH_PAD_RUSTED.get(),
                            HbmBlocks.COMPACT_LAUNCHER.get(),
                            HbmBlocks.LAUNCH_TABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoyuzLauncherBlockEntity>> SOYUZ_LAUNCHER =
            BLOCK_ENTITIES.register(
                    "soyuz_launcher",
                    () -> BlockEntityType.Builder.of(
                            SoyuzLauncherBlockEntity::new,
                            HbmBlocks.SOYUZ_LAUNCHER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoyuzCapsuleBlockEntity>> SOYUZ_CAPSULE =
            BLOCK_ENTITIES.register(
                    "soyuz_capsule",
                    () -> BlockEntityType.Builder.of(
                            SoyuzCapsuleBlockEntity::new,
                            HbmBlocks.SOYUZ_CAPSULE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandStructureBlockEntity>> WAND_STRUCTURE =
            BLOCK_ENTITIES.register(
                    "wand_structure",
                    () -> BlockEntityType.Builder.of(
                            WandStructureBlockEntity::new,
                            HbmBlocks.WAND_STRUCTURE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandJigsawBlockEntity>> WAND_JIGSAW =
            BLOCK_ENTITIES.register(
                    "wand_jigsaw",
                    () -> BlockEntityType.Builder.of(
                            WandJigsawBlockEntity::new,
                            HbmBlocks.WAND_JIGSAW.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandTandemBlockEntity>> WAND_TANDEM =
            BLOCK_ENTITIES.register(
                    "wand_tandem",
                    () -> BlockEntityType.Builder.of(
                            WandTandemBlockEntity::new,
                            HbmBlocks.WAND_TANDEM.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandLootBlockEntity>> WAND_LOOT =
            BLOCK_ENTITIES.register(
                    "wand_loot",
                    () -> BlockEntityType.Builder.of(
                            WandLootBlockEntity::new,
                            HbmBlocks.WAND_LOOT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WandLogicBlockEntity>> WAND_LOGIC =
            BLOCK_ENTITIES.register(
                    "wand_logic",
                    () -> BlockEntityType.Builder.of(
                            WandLogicBlockEntity::new,
                            HbmBlocks.WAND_LOGIC.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecoLootBlockEntity>> DECO_LOOT =
            BLOCK_ENTITIES.register(
                    "deco_loot",
                    () -> BlockEntityType.Builder.of(
                            DecoLootBlockEntity::new,
                            HbmBlocks.DECO_LOOT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecoDisplayBlockEntity>> DECO_DISPLAY =
            BLOCK_ENTITIES.register(
                    "deco_display",
                    () -> BlockEntityType.Builder.of(
                            DecoDisplayBlockEntity::new,
                            HbmBlocks.DECO_COMPUTER.get(),
                            HbmBlocks.DECO_CRT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilingCabinetBlockEntity>> FILING_CABINET =
            BLOCK_ENTITIES.register(
                    "filing_cabinet",
                    () -> BlockEntityType.Builder.of(
                            FilingCabinetBlockEntity::new,
                            HbmBlocks.FILING_CABINET.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TapeRecorderBlockEntity>> TAPE_RECORDER =
            BLOCK_ENTITIES.register(
                    "tape_recorder",
                    () -> BlockEntityType.Builder.of(
                            TapeRecorderBlockEntity::new,
                            HbmBlocks.TAPE_RECORDER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogicBlockEntity>> LOGIC_BLOCK =
            BLOCK_ENTITIES.register(
                    "logic_block",
                    () -> BlockEntityType.Builder.of(
                            LogicBlockEntity::new,
                            HbmBlocks.LOGIC_BLOCK.get(),
                            HbmBlocks.LOGIC_BLOCK_INVIS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LauncherStructCoreBlockEntity>> LAUNCHER_STRUCT_CORE =
            BLOCK_ENTITIES.register(
                    "launcher_struct_core",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new LauncherStructCoreBlockEntity(
                                    pos,
                                    state,
                                    state.is(HbmBlocks.STRUCT_LAUNCHER_CORE_LARGE.get())
                            ),
                            HbmBlocks.STRUCT_LAUNCHER_CORE.get(),
                            HbmBlocks.STRUCT_LAUNCHER_CORE_LARGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoyuzStructCoreBlockEntity>> SOYUZ_STRUCT_CORE =
            BLOCK_ENTITIES.register(
                    "soyuz_struct_core",
                    () -> BlockEntityType.Builder.of(
                            SoyuzStructCoreBlockEntity::new,
                            HbmBlocks.STRUCT_SOYUZ_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteDrumBlockEntity>> WASTE_DRUM =
            BLOCK_ENTITIES.register(
                    "machine_waste_drum",
                    () -> BlockEntityType.Builder.of(
                            WasteDrumBlockEntity::new,
                            HbmBlocks.MACHINE_WASTE_DRUM.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurexBlockEntity>> PUREX =
            BLOCK_ENTITIES.register(
                    "machine_purex",
                    () -> BlockEntityType.Builder.of(
                            PurexBlockEntity::new,
                            HbmBlocks.MACHINE_PUREX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfPressBlockEntity>> ICF_PRESS =
            BLOCK_ENTITIES.register(
                    "machine_icf_press",
                    () -> BlockEntityType.Builder.of(
                            IcfPressBlockEntity::new,
                            HbmBlocks.MACHINE_ICF_PRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfCoreBlockEntity>> ICF_CORE =
            BLOCK_ENTITIES.register(
                    "icf",
                    () -> BlockEntityType.Builder.of(IcfCoreBlockEntity::new, HbmBlocks.ICF.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfStructBlockEntity>> ICF_STRUCT =
            BLOCK_ENTITIES.register(
                    "struct_icf_core",
                    () -> BlockEntityType.Builder.of(IcfStructBlockEntity::new, HbmBlocks.STRUCT_ICF_CORE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfControllerBlockEntity>> ICF_CONTROLLER =
            BLOCK_ENTITIES.register(
                    "icf_controller",
                    () -> BlockEntityType.Builder.of(IcfControllerBlockEntity::new, HbmBlocks.ICF_CONTROLLER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcfAssembledLaserBlockEntity>> ICF_ASSEMBLED_LASER =
            BLOCK_ENTITIES.register(
                    "icf_block",
                    () -> BlockEntityType.Builder.of(IcfAssembledLaserBlockEntity::new, HbmBlocks.ICF_BLOCK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchReactorBlockEntity>> RESEARCH_REACTOR =
            BLOCK_ENTITIES.register(
                    "machine_reactor_small_new",
                    () -> BlockEntityType.Builder.of(
                            ResearchReactorBlockEntity::new,
                            HbmBlocks.MACHINE_REACTOR_SMALL.get(),
                            HbmBlocks.MACHINE_REACTOR_SMALL_LEGACY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorControlBlockEntity>> REACTOR_CONTROL =
            BLOCK_ENTITIES.register(
                    "machine_controller",
                    () -> BlockEntityType.Builder.of(
                            ReactorControlBlockEntity::new,
                            HbmBlocks.MACHINE_CONTROLLER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WatzBlockEntity>> WATZ =
            BLOCK_ENTITIES.register(
                    "watz",
                    () -> BlockEntityType.Builder.of(
                            WatzBlockEntity::new,
                            HbmBlocks.WATZ.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WatzStructBlockEntity>> WATZ_STRUCT =
            BLOCK_ENTITIES.register(
                    "struct_watz_core",
                    () -> BlockEntityType.Builder.of(
                            WatzStructBlockEntity::new,
                            HbmBlocks.STRUCT_WATZ_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WatzPumpBlockEntity>> WATZ_PUMP =
            BLOCK_ENTITIES.register(
                    "watz_pump",
                    () -> BlockEntityType.Builder.of(
                            WatzPumpBlockEntity::new,
                            HbmBlocks.WATZ_PUMP.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZirnoxReactorBlockEntity>> ZIRNOX_REACTOR =
            BLOCK_ENTITIES.register(
                    "machine_zirnox",
                    () -> BlockEntityType.Builder.of(
                            ZirnoxReactorBlockEntity::new,
                            HbmBlocks.MACHINE_ZIRNOX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PwrControllerBlockEntity>> PWR_CONTROLLER =
            BLOCK_ENTITIES.register(
                    "pwr_controller",
                    () -> BlockEntityType.Builder.of(
                            PwrControllerBlockEntity::new,
                            HbmBlocks.PWR_CONTROLLER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PwrBlockEntity>> PWR_PART =
            BLOCK_ENTITIES.register(
                    "pwr_part",
                    () -> BlockEntityType.Builder.of(
                            PwrBlockEntity::new,
                            HbmBlocks.PWR_BLOCK.get(),
                            HbmBlocks.PWR_CASING.get(),
                            HbmBlocks.PWR_CHANNEL.get(),
                            HbmBlocks.PWR_CONTROL.get(),
                            HbmBlocks.PWR_FUELROD.get(),
                            HbmBlocks.PWR_FUEL.get(),
                            HbmBlocks.PWR_HEATEX.get(),
                            HbmBlocks.PWR_HEATSINK.get(),
                            HbmBlocks.PWR_NEUTRON_SOURCE.get(),
                            HbmBlocks.PWR_PORT.get(),
                            HbmBlocks.PWR_REFLECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZirnoxDestroyedBlockEntity>> ZIRNOX_DESTROYED =
            BLOCK_ENTITIES.register(
                    "zirnox_destroyed",
                    () -> BlockEntityType.Builder.of(
                            ZirnoxDestroyedBlockEntity::new,
                            HbmBlocks.ZIRNOX_DESTROYED.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionMachineBlockEntity>> FUSION_MACHINE =
            BLOCK_ENTITIES.register(
                    "fusion_machine",
                    () -> BlockEntityType.Builder.of(
                            FusionMachineBlockEntity::new,
                            HbmBlocks.FUSION_TORUS.get(),
                            HbmBlocks.FUSION_KLYSTRON.get(),
                            HbmBlocks.FUSION_KLYSTRON_CREATIVE.get(),
                            HbmBlocks.FUSION_BREEDER.get(),
                            HbmBlocks.FUSION_COLLECTOR.get(),
                            HbmBlocks.FUSION_BOILER.get(),
                            HbmBlocks.FUSION_MHDT.get(),
                            HbmBlocks.FUSION_COUPLER.get(),
                            HbmBlocks.FUSION_PLASMA_FORGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcCoreBlockEntity>> DFC_CORE =
            BLOCK_ENTITIES.register(
                    "dfc_core",
                    () -> BlockEntityType.Builder.of(
                            DfcCoreBlockEntity::new,
                            HbmBlocks.DFC_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcEmitterBlockEntity>> DFC_EMITTER =
            BLOCK_ENTITIES.register(
                    "dfc_emitter",
                    () -> BlockEntityType.Builder.of(
                            DfcEmitterBlockEntity::new,
                            HbmBlocks.DFC_EMITTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcReceiverBlockEntity>> DFC_RECEIVER =
            BLOCK_ENTITIES.register(
                    "dfc_receiver",
                    () -> BlockEntityType.Builder.of(
                            DfcReceiverBlockEntity::new,
                            HbmBlocks.DFC_RECEIVER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcInjectorBlockEntity>> DFC_INJECTOR =
            BLOCK_ENTITIES.register(
                    "dfc_injector",
                    () -> BlockEntityType.Builder.of(
                            DfcInjectorBlockEntity::new,
                            HbmBlocks.DFC_INJECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DfcStabilizerBlockEntity>> DFC_STABILIZER =
            BLOCK_ENTITIES.register(
                    "dfc_stabilizer",
                    () -> BlockEntityType.Builder.of(
                            DfcStabilizerBlockEntity::new,
                            HbmBlocks.DFC_STABILIZER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionTorusStructBlockEntity>> FUSION_TORUS_STRUCT =
            BLOCK_ENTITIES.register(
                    "fusion_torus_struct",
                    () -> BlockEntityType.Builder.of(
                            FusionTorusStructBlockEntity::new,
                            HbmBlocks.STRUCT_TORUS_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RbmkComponentBlockEntity>> RBMK_COMPONENT =
            BLOCK_ENTITIES.register(
                    "rbmk_component",
                    () -> BlockEntityType.Builder.of(
                            RbmkComponentBlockEntity::new,
                            HbmBlocks.RBMK_ABSORBER.get(),
                            HbmBlocks.RBMK_AUTOLOADER.get(),
                            HbmBlocks.RBMK_BLANK.get(),
                            HbmBlocks.RBMK_BOILER.get(),
                            HbmBlocks.RBMK_CONSOLE.get(),
                            HbmBlocks.RBMK_CONTROL.get(),
                            HbmBlocks.RBMK_CONTROL_AUTO.get(),
                            HbmBlocks.RBMK_CONTROL_MOD.get(),
                            HbmBlocks.RBMK_CONTROL_REASIM.get(),
                            HbmBlocks.RBMK_CONTROL_REASIM_AUTO.get(),
                            HbmBlocks.RBMK_COOLER.get(),
                            HbmBlocks.RBMK_CRANE_CONSOLE.get(),
                            HbmBlocks.RBMK_DISPLAY.get(),
                            HbmBlocks.RBMK_DISPLAY_BLANK.get(),
                            HbmBlocks.RBMK_GAUGE.get(),
                            HbmBlocks.RBMK_GRAPH.get(),
                            HbmBlocks.RBMK_HEATER.get(),
                            HbmBlocks.RBMK_INDICATOR.get(),
                            HbmBlocks.RBMK_KEY_PAD.get(),
                            HbmBlocks.RBMK_LEVER.get(),
                            HbmBlocks.RBMK_LOADER.get(),
                            HbmBlocks.RBMK_MODERATOR.get(),
                            HbmBlocks.RBMK_NUMITRON.get(),
                            HbmBlocks.RBMK_OUTGASSER.get(),
                            HbmBlocks.RBMK_REFLECTOR.get(),
                            HbmBlocks.RBMK_ROD.get(),
                            HbmBlocks.RBMK_ROD_MOD.get(),
                            HbmBlocks.RBMK_ROD_REASIM.get(),
                            HbmBlocks.RBMK_ROD_REASIM_MOD.get(),
                            HbmBlocks.RBMK_STEAM_INLET.get(),
                            HbmBlocks.RBMK_STEAM_OUTLET.get(),
                            HbmBlocks.RBMK_STORAGE.get(),
                            HbmBlocks.RBMK_TERMINAL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StirlingGeneratorBlockEntity>> STIRLING_GENERATOR =
            BLOCK_ENTITIES.register(
                    "machine_stirling",
                    () -> BlockEntityType.Builder.of(
                            StirlingGeneratorBlockEntity::new,
                            HbmBlocks.MACHINE_STIRLING.get(),
                            HbmBlocks.MACHINE_STIRLING_STEEL.get(),
                            HbmBlocks.MACHINE_STIRLING_CREATIVE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DieselGeneratorBlockEntity>> DIESEL_GENERATOR =
            BLOCK_ENTITIES.register(
                    "machine_diesel",
                    () -> BlockEntityType.Builder.of(
                            DieselGeneratorBlockEntity::new,
                            HbmBlocks.MACHINE_DIESEL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionEngineBlockEntity>> COMBUSTION_ENGINE =
            BLOCK_ENTITIES.register(
                    "machine_combustion_engine",
                    () -> BlockEntityType.Builder.of(
                            CombustionEngineBlockEntity::new,
                            HbmBlocks.MACHINE_COMBUSTION_ENGINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasFlareBlockEntity>> GAS_FLARE =
            BLOCK_ENTITIES.register(
                    "machine_flare",
                    () -> BlockEntityType.Builder.of(
                            GasFlareBlockEntity::new,
                            HbmBlocks.MACHINE_FLARE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE =
            BLOCK_ENTITIES.register(
                    "machine_steam_engine",
                    () -> BlockEntityType.Builder.of(
                            SteamEngineBlockEntity::new,
                            HbmBlocks.MACHINE_STEAM_ENGINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeaterBlockEntity>> HEATER =
            BLOCK_ENTITIES.register(
                    "heater",
                    () -> BlockEntityType.Builder.of(
                            HeaterBlockEntity::new,
                            HbmBlocks.HEATER_FIREBOX.get(),
                            HbmBlocks.HEATER_OVEN.get(),
                            HbmBlocks.HEATER_OILBURNER.get(),
                            HbmBlocks.HEATER_ELECTRIC.get(),
                            HbmBlocks.HEATER_HEATEX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatBoilerBlockEntity>> HEAT_BOILER =
            BLOCK_ENTITIES.register(
                    "heat_boiler",
                    () -> BlockEntityType.Builder.of(
                            HeatBoilerBlockEntity::new,
                            HbmBlocks.HEAT_BOILER.get(),
                            HbmBlocks.MACHINE_BOILER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialBoilerBlockEntity>> INDUSTRIAL_BOILER =
            BLOCK_ENTITIES.register(
                    "machine_industrial_boiler",
                    () -> BlockEntityType.Builder.of(
                            IndustrialBoilerBlockEntity::new,
                            HbmBlocks.MACHINE_INDUSTRIAL_BOILER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarBoilerBlockEntity>> SOLAR_BOILER =
            BLOCK_ENTITIES.register(
                    "machine_solar_boiler",
                    () -> BlockEntityType.Builder.of(
                            SolarBoilerBlockEntity::new,
                            HbmBlocks.MACHINE_SOLAR_BOILER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirCompressorBlockEntity>> AIR_COMPRESSOR =
            BLOCK_ENTITIES.register(
                    "machine_intake",
                    () -> BlockEntityType.Builder.of(
                            AirCompressorBlockEntity::new,
                            HbmBlocks.MACHINE_INTAKE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyConverterBlockEntity>> ENERGY_CONVERTER =
            BLOCK_ENTITIES.register(
                    "energy_converter",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new EnergyConverterBlockEntity(pos, state,
                                    ((com.reinhardt.hbm.block.EnergyConverterBlock) state.getBlock()).kind()),
                            HbmBlocks.MACHINE_CONVERTER_HE_RF.get(),
                            HbmBlocks.MACHINE_CONVERTER_RF_HE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerDetectorBlockEntity>> POWER_DETECTOR =
            BLOCK_ENTITIES.register(
                    "machine_detector",
                    () -> BlockEntityType.Builder.of(PowerDetectorBlockEntity::new, HbmBlocks.MACHINE_DETECTOR.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableSwitchBlockEntity>> CABLE_SWITCH =
            BLOCK_ENTITIES.register(
                    "cable_switch",
                    () -> BlockEntityType.Builder.of(
                            CableSwitchBlockEntity::new,
                            HbmBlocks.CABLE_SWITCH.get(),
                            HbmBlocks.CABLE_DETECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableDiodeBlockEntity>> CABLE_DIODE =
            BLOCK_ENTITIES.register(
                    "cable_diode",
                    () -> BlockEntityType.Builder.of(
                            CableDiodeBlockEntity::new,
                            HbmBlocks.CABLE_DIODE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintableCableBlockEntity>> PAINTABLE_CABLE =
            BLOCK_ENTITIES.register(
                    "red_cable_paintable",
                    () -> BlockEntityType.Builder.of(
                            PaintableCableBlockEntity::new,
                            HbmBlocks.RED_CABLE_PAINTABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerGaugeBlockEntity>> POWER_GAUGE =
            BLOCK_ENTITIES.register(
                    "red_cable_gauge",
                    () -> BlockEntityType.Builder.of(
                            PowerGaugeBlockEntity::new,
                            HbmBlocks.RED_CABLE_GAUGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeothermalHeatExchangerBlockEntity>> GEOTHERMAL_HEAT_EXCHANGER =
            BLOCK_ENTITIES.register(
                    "machine_hephaestus",
                    () -> BlockEntityType.Builder.of(GeothermalHeatExchangerBlockEntity::new, HbmBlocks.MACHINE_HEPHAESTUS.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FissureBlockEntity>> FISSURE =
            BLOCK_ENTITIES.register(
                    "ore_volcano",
                    () -> BlockEntityType.Builder.of(FissureBlockEntity::new, HbmBlocks.ORE_VOLCANO.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VolcanoCoreBlockEntity>> VOLCANO_CORE =
            BLOCK_ENTITIES.register(
                    "volcano_core",
                    () -> BlockEntityType.Builder.of(
                            VolcanoCoreBlockEntity::new,
                            HbmBlocks.VOLCANO_CORE.get(),
                            HbmBlocks.VOLCANO_RAD_CORE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarMirrorBlockEntity>> SOLAR_MIRROR =
            BLOCK_ENTITIES.register(
                    "solar_mirror",
                    () -> BlockEntityType.Builder.of(
                            SolarMirrorBlockEntity::new,
                            HbmBlocks.SOLAR_MIRROR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmallBoilerBlockEntity>> SMALL_BOILER =
            BLOCK_ENTITIES.register(
                    "small_boiler",
                    () -> BlockEntityType.Builder.of(
                            SmallBoilerBlockEntity::new,
                            HbmBlocks.MACHINE_BOILER_OFF.get(),
                            HbmBlocks.MACHINE_BOILER_ELECTRIC_OFF.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmallElectricBoilerBlockEntity>> SMALL_ELECTRIC_BOILER =
            BLOCK_ENTITIES.register(
                    "small_electric_boiler",
                    () -> BlockEntityType.Builder.of(
                            SmallElectricBoilerBlockEntity::new,
                            HbmBlocks.MACHINE_BOILER_ELECTRIC_OFF.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_turbine",
                    () -> BlockEntityType.Builder.of(
                            SteamTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_TURBINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialTurbineBlockEntity>> INDUSTRIAL_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_industrial_turbine",
                    () -> BlockEntityType.Builder.of(
                            IndustrialTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_INDUSTRIAL_TURBINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeTurbineBlockEntity>> LARGE_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_large_turbine",
                    () -> BlockEntityType.Builder.of(
                            LargeTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_LARGE_TURBINE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeviathanTurbineBlockEntity>> LEVIATHAN_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_chungus",
                    () -> BlockEntityType.Builder.of(
                            LeviathanTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_CHUNGUS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasTurbineBlockEntity>> GAS_TURBINE =
            BLOCK_ENTITIES.register(
                    "machine_turbine_gas",
                    () -> BlockEntityType.Builder.of(
                            GasTurbineBlockEntity::new,
                            HbmBlocks.MACHINE_TURBINE_GAS.get(),
                            HbmBlocks.MACHINE_TURBINEGAS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamCondenserBlockEntity>> STEAM_CONDENSER =
            BLOCK_ENTITIES.register(
                    "machine_condenser",
                    () -> BlockEntityType.Builder.of(
                            SteamCondenserBlockEntity::new,
                            HbmBlocks.MACHINE_CONDENSER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredSteamCondenserBlockEntity>> POWERED_STEAM_CONDENSER =
            BLOCK_ENTITIES.register(
                    "machine_condenser_powered",
                    () -> BlockEntityType.Builder.of(
                            PoweredSteamCondenserBlockEntity::new,
                            HbmBlocks.MACHINE_CONDENSER_POWERED.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoolingTowerBlockEntity>> COOLING_TOWER =
            BLOCK_ENTITIES.register(
                    "machine_tower_small",
                    () -> BlockEntityType.Builder.of(
                            CoolingTowerBlockEntity::new,
                            HbmBlocks.MACHINE_TOWER_SMALL.get(),
                            HbmBlocks.MACHINE_TOWER_LARGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GroundwaterPumpBlockEntity>> GROUNDWATER_PUMP =
            BLOCK_ENTITIES.register(
                    "groundwater_pump",
                    () -> BlockEntityType.Builder.of(
                            GroundwaterPumpBlockEntity::new,
                            HbmBlocks.PUMP_STEAM.get(),
                            HbmBlocks.PUMP_ELECTRIC.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilDerrickBlockEntity>> OIL_DERRICK =
            BLOCK_ENTITIES.register(
                    "machine_well",
                    () -> BlockEntityType.Builder.of(
                            OilDerrickBlockEntity::new,
                            HbmBlocks.MACHINE_WELL.get(),
                            HbmBlocks.MACHINE_PUMPJACK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FrackingTowerBlockEntity>> FRACKING_TOWER =
            BLOCK_ENTITIES.register(
                    "machine_fracking_tower",
                    () -> BlockEntityType.Builder.of(
                            FrackingTowerBlockEntity::new,
                            HbmBlocks.MACHINE_FRACKING_TOWER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RefineryBlockEntity>> REFINERY =
            BLOCK_ENTITIES.register(
                    "machine_refinery",
                    () -> BlockEntityType.Builder.of(
                            RefineryBlockEntity::new,
                            HbmBlocks.MACHINE_REFINERY.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VacuumDistillBlockEntity>> VACUUM_DISTILL =
            BLOCK_ENTITIES.register(
                    "machine_vacuum_distill",
                    () -> BlockEntityType.Builder.of(
                            VacuumDistillBlockEntity::new,
                            HbmBlocks.MACHINE_VACUUM_DISTILL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CokerBlockEntity>> COKER =
            BLOCK_ENTITIES.register(
                    "machine_coker",
                    () -> BlockEntityType.Builder.of(
                            CokerBlockEntity::new,
                            HbmBlocks.MACHINE_COKER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChimneyBlockEntity>> CHIMNEY =
            BLOCK_ENTITIES.register(
                    "chimney",
                    () -> BlockEntityType.Builder.of(
                            ChimneyBlockEntity::new,
                            HbmBlocks.CHIMNEY_BRICK.get(),
                            HbmBlocks.CHIMNEY_INDUSTRIAL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FunnelBlockEntity>> FUNNEL =
            BLOCK_ENTITIES.register(
                    "machine_funnel",
                    () -> BlockEntityType.Builder.of(
                            FunnelBlockEntity::new,
                            HbmBlocks.MACHINE_FUNNEL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SirenBlockEntity>> SIREN =
            BLOCK_ENTITIES.register(
                    "machine_siren",
                    () -> BlockEntityType.Builder.of(
                            SirenBlockEntity::new,
                            HbmBlocks.MACHINE_SIREN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AshpitBlockEntity>> ASHPIT =
            BLOCK_ENTITIES.register(
                    "machine_ashpit",
                    () -> BlockEntityType.Builder.of(
                            AshpitBlockEntity::new,
                            HbmBlocks.MACHINE_ASHPIT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeconBlockEntity>> DECON =
            BLOCK_ENTITIES.register(
                    "decon",
                    () -> BlockEntityType.Builder.of(
                            DeconBlockEntity::new,
                            HbmBlocks.DECON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnaceCombinationBlockEntity>> FURNACE_COMBINATION =
            BLOCK_ENTITIES.register(
                    "furnace_combination",
                    () -> BlockEntityType.Builder.of(
                            FurnaceCombinationBlockEntity::new,
                            HbmBlocks.FURNACE_COMBINATION.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RotaryFurnaceBlockEntity>> ROTARY_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_rotary_furnace",
                    () -> BlockEntityType.Builder.of(
                            RotaryFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_ROTARY_FURNACE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolidifierBlockEntity>> SOLIDIFIER =
            BLOCK_ENTITIES.register(
                    "machine_solidifier",
                    () -> BlockEntityType.Builder.of(
                            SolidifierBlockEntity::new,
                            HbmBlocks.MACHINE_SOLIDIFIER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER =
            BLOCK_ENTITIES.register(
                    "machine_electrolyser",
                    () -> BlockEntityType.Builder.of(
                            ElectrolyzerBlockEntity::new,
                            HbmBlocks.MACHINE_ELECTROLYSER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquefactorBlockEntity>> LIQUEFACTOR =
            BLOCK_ENTITIES.register(
                    "machine_liquefactor",
                    () -> BlockEntityType.Builder.of(
                            LiquefactorBlockEntity::new,
                            HbmBlocks.MACHINE_LIQUEFACTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FractionTowerBlockEntity>> FRACTION_TOWER =
            BLOCK_ENTITIES.register(
                    "machine_fraction_tower",
                    () -> BlockEntityType.Builder.of(
                            FractionTowerBlockEntity::new,
                            HbmBlocks.MACHINE_FRACTION_TOWER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FractionSpacerBlockEntity>> FRACTION_SPACER =
            BLOCK_ENTITIES.register(
                    "fraction_spacer",
                    () -> BlockEntityType.Builder.of(
                            FractionSpacerBlockEntity::new,
                            HbmBlocks.FRACTION_SPACER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatalyticCrackerBlockEntity>> CATALYTIC_CRACKER =
            BLOCK_ENTITIES.register(
                    "machine_catalytic_cracker",
                    () -> BlockEntityType.Builder.of(
                            CatalyticCrackerBlockEntity::new,
                            HbmBlocks.MACHINE_CATALYTIC_CRACKER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatalyticReformerBlockEntity>> CATALYTIC_REFORMER =
            BLOCK_ENTITIES.register(
                    "machine_catalytic_reformer",
                    () -> BlockEntityType.Builder.of(
                            CatalyticReformerBlockEntity::new,
                            HbmBlocks.MACHINE_CATALYTIC_REFORMER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydrotreaterBlockEntity>> HYDROTREATER =
            BLOCK_ENTITIES.register(
                    "machine_hydrotreater",
                    () -> BlockEntityType.Builder.of(
                            HydrotreaterBlockEntity::new,
                            HbmBlocks.MACHINE_HYDROTREATER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_difurnace",
                    () -> BlockEntityType.Builder.of(
                            BlastFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_DIFURNACE_OFF.get(),
                            HbmBlocks.MACHINE_DIFURNACE_ON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlastFurnaceBlockEntity>> MACHINE_BLAST_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_blast_furnace",
                    () -> BlockEntityType.Builder.of(
                            MachineBlastFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_BLAST_FURNACE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrickFurnaceBlockEntity>> BRICK_FURNACE =
            BLOCK_ENTITIES.register(
                    "machine_furnace_brick",
                    () -> BlockEntityType.Builder.of(
                            BrickFurnaceBlockEntity::new,
                            HbmBlocks.MACHINE_FURNACE_BRICK_OFF.get(),
                            HbmBlocks.MACHINE_FURNACE_BRICK_ON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronFurnaceBlockEntity>> IRON_FURNACE =
            BLOCK_ENTITIES.register(
                    "furnace_iron",
                    () -> BlockEntityType.Builder.of(
                            IronFurnaceBlockEntity::new,
                            HbmBlocks.FURNACE_IRON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteelFurnaceBlockEntity>> STEEL_FURNACE =
            BLOCK_ENTITIES.register(
                    "furnace_steel",
                    () -> BlockEntityType.Builder.of(
                            SteelFurnaceBlockEntity::new,
                            HbmBlocks.FURNACE_STEEL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register(
                    "machine_crucible",
                    () -> BlockEntityType.Builder.of(
                            CrucibleBlockEntity::new,
                            HbmBlocks.MACHINE_CRUCIBLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryCastingBlockEntity>> FOUNDRY_CASTING =
            BLOCK_ENTITIES.register(
                    "foundry_casting",
                    () -> BlockEntityType.Builder.of(
                            FoundryCastingBlockEntity::new,
                            HbmBlocks.FOUNDRY_MOLD.get(),
                            HbmBlocks.FOUNDRY_BASIN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryFlowBlockEntity>> FOUNDRY_FLOW =
            BLOCK_ENTITIES.register(
                    "foundry_flow",
                    () -> BlockEntityType.Builder.of(
                            FoundryFlowBlockEntity::new,
                            HbmBlocks.FOUNDRY_CHANNEL.get(),
                            HbmBlocks.FOUNDRY_OUTLET.get(),
                            HbmBlocks.FOUNDRY_SLAGTAP.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryTankBlockEntity>> FOUNDRY_TANK =
            BLOCK_ENTITIES.register(
                    "foundry_tank",
                    () -> BlockEntityType.Builder.of(
                            FoundryTankBlockEntity::new,
                            HbmBlocks.FOUNDRY_TANK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundrySlagBlockEntity>> FOUNDRY_SLAG =
            BLOCK_ENTITIES.register(
                    "foundry_slag",
                    () -> BlockEntityType.Builder.of(
                            FoundrySlagBlockEntity::new,
                            HbmBlocks.SLAG.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StrandCasterBlockEntity>> STRAND_CASTER =
            BLOCK_ENTITIES.register(
                    "machine_strand_caster",
                    () -> BlockEntityType.Builder.of(
                            StrandCasterBlockEntity::new,
                            HbmBlocks.MACHINE_STRAND_CASTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HbmHeavyDoorBlockEntity>> HEAVY_DOOR =
            BLOCK_ENTITIES.register(
                    "heavy_door",
                    () -> BlockEntityType.Builder.of(
                            HbmHeavyDoorBlockEntity::new,
                            HbmBlocks.FIRE_DOOR.get(),
                            HbmBlocks.SLIDING_BLAST_DOOR.get(),
                            HbmBlocks.SLIDING_BLAST_DOOR_2.get(),
                            HbmBlocks.SLIDING_GATE_DOOR.get(),
                            HbmBlocks.QE_SLIDING.get(),
                            HbmBlocks.QE_CONTAINMENT.get(),
                            HbmBlocks.SLIDING_SEAL_DOOR.get(),
                            HbmBlocks.SECURE_ACCESS_DOOR.get(),
                            HbmBlocks.ROUND_AIRLOCK_DOOR.get(),
                            HbmBlocks.LARGE_VEHICLE_DOOR.get(),
                            HbmBlocks.VAULT_DOOR.get(),
                            HbmBlocks.WATER_DOOR.get(),
                            HbmBlocks.SILO_HATCH.get(),
                            HbmBlocks.SILO_HATCH_LARGE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HbmHeavyDoorPartBlockEntity>> HEAVY_DOOR_PART =
            BLOCK_ENTITIES.register(
                    "heavy_door_part",
                    () -> BlockEntityType.Builder.of(
                            HbmHeavyDoorPartBlockEntity::new,
                            HbmBlocks.HEAVY_DOOR_PART.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastDoorBlockEntity>> BLAST_DOOR =
            BLOCK_ENTITIES.register(
                    "blast_door",
                    () -> BlockEntityType.Builder.of(
                            BlastDoorBlockEntity::new,
                            HbmBlocks.BLAST_DOOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastDoorDummyBlockEntity>> BLAST_DOOR_DUMMY =
            BLOCK_ENTITIES.register(
                    "dummy_block_blast",
                    () -> BlockEntityType.Builder.of(
                            BlastDoorDummyBlockEntity::new,
                            HbmBlocks.DUMMY_BLOCK_BLAST.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerPylonBlockEntity>> POWER_PYLON =
            BLOCK_ENTITIES.register(
                    "power_pylon",
                    () -> BlockEntityType.Builder.of(
                            PowerPylonBlockEntity::new,
                            HbmBlocks.RED_CONNECTOR.get(),
                            HbmBlocks.CONNECTOR_RED_SUPER.get(),
                            HbmBlocks.RED_PYLON.get(),
                            HbmBlocks.RED_PYLON_MEDIUM_WOOD.get(),
                            HbmBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get(),
                            HbmBlocks.RED_PYLON_MEDIUM_STEEL.get(),
                            HbmBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get(),
                            HbmBlocks.RED_PYLON_LARGE.get(),
                            HbmBlocks.SUBSTATION.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoleBlockEntity>> POLE =
            BLOCK_ENTITIES.register(
                    "pole",
                    () -> BlockEntityType.Builder.of(
                            PoleBlockEntity::new,
                            HbmBlocks.POLE_TOP.get(),
                            HbmBlocks.POLE_SATELLITE_RECEIVER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITIES.register(
                    "fluid_pipe",
                    () -> BlockEntityType.Builder.of(
                            FluidPipeBlockEntity::new,
                            HbmBlocks.FLUID_DUCT_MK2.get(),
                            HbmBlocks.FLUID_DUCT_NEO.get(),
                            HbmBlocks.FLUID_DUCT_BOX.get(),
                            HbmBlocks.FLUID_DUCT_EXHAUST.get(),
                            HbmBlocks.FLUID_DUCT_GAUGE.get(),
                            HbmBlocks.FLUID_DUCT_PAINTABLE.get(),
                            HbmBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST.get(),
                            HbmBlocks.FLUID_DUCT_SOLID.get(),
                            HbmBlocks.FLUID_DUCT_SOLID_SEALED.get(),
                            HbmBlocks.FLUID_VALVE.get(),
                            HbmBlocks.FLUID_SWITCH.get(),
                            HbmBlocks.FLUID_COUNTER_VALVE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPumpBlockEntity>> FLUID_PUMP =
            BLOCK_ENTITIES.register(
                    "fluid_pump",
                    () -> BlockEntityType.Builder.of(
                            FluidPumpBlockEntity::new,
                            HbmBlocks.FLUID_PUMP.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrainBlockEntity>> DRAIN =
            BLOCK_ENTITIES.register(
                    "machine_drain",
                    () -> BlockEntityType.Builder.of(
                            DrainBlockEntity::new,
                            HbmBlocks.MACHINE_DRAIN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITIES.register(
                    "fluid_tank",
                    () -> BlockEntityType.Builder.of(
                            FluidTankBlockEntity::new,
                            HbmBlocks.MACHINE_FLUIDTANK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HexafluorideTankBlockEntity>> HEXAFLUORIDE_TANK =
            BLOCK_ENTITIES.register(
                    "hexafluoride_tank",
                    () -> BlockEntityType.Builder.of(
                            HexafluorideTankBlockEntity::new,
                            HbmBlocks.MACHINE_UF6_TANK.get(),
                            HbmBlocks.MACHINE_PUF6_TANK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidBarrelBlockEntity>> FLUID_BARREL =
            BLOCK_ENTITIES.register(
                    "fluid_barrel",
                    () -> BlockEntityType.Builder.of(
                            FluidBarrelBlockEntity::new,
                            HbmBlocks.BARREL_PLASTIC.get(),
                            HbmBlocks.BARREL_STEEL.get(),
                            HbmBlocks.BARREL_TCALLOY.get(),
                            HbmBlocks.BARREL_ANTIMATTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BigAssTankBlockEntity>> BIG_ASS_TANK =
            BLOCK_ENTITIES.register(
                    "machine_bat9000",
                    () -> BlockEntityType.Builder.of(
                            BigAssTankBlockEntity::new,
                            HbmBlocks.MACHINE_BAT9000.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeFluidTankBlockEntity>> LARGE_FLUID_TANK =
            BLOCK_ENTITIES.register(
                    "machine_bigasstank",
                    () -> BlockEntityType.Builder.of(
                            LargeFluidTankBlockEntity::new,
                            HbmBlocks.MACHINE_BIGASSTANK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryReddBlockEntity>> BATTERY_REDD =
            BLOCK_ENTITIES.register(
                    "machine_battery_redd",
                    () -> BlockEntityType.Builder.of(
                            BatteryReddBlockEntity::new,
                            HbmBlocks.MACHINE_BATTERY_REDD.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatterySocketBlockEntity>> BATTERY_SOCKET =
            BLOCK_ENTITIES.register(
                    "machine_battery_socket",
                    () -> BlockEntityType.Builder.of(
                            BatterySocketBlockEntity::new,
                            HbmBlocks.MACHINE_BATTERY_SOCKET.get()
                    ).build(null)
                         );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CapacitorBlockEntity>> CAPACITOR_COPPER =
            BLOCK_ENTITIES.register(
                    "capacitor_copper",
                    () -> BlockEntityType.Builder.of(
                            CapacitorBlockEntity::new,
                            HbmBlocks.CAPACITOR_COPPER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CapacitorBusBlockEntity>> CAPACITOR_BUS =
            BLOCK_ENTITIES.register(
                    "capacitor_bus",
                    () -> BlockEntityType.Builder.of(
                            CapacitorBusBlockEntity::new,
                            HbmBlocks.CAPACITOR_BUS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>> CHARGER =
            BLOCK_ENTITIES.register(
                    "charger",
                    () -> BlockEntityType.Builder.of(
                            ChargerBlockEntity::new,
                            HbmBlocks.CHARGER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageCrateBlockEntity>> STORAGE_CRATE =
            BLOCK_ENTITIES.register(
                    "storage_crate",
                    () -> BlockEntityType.Builder.of(
                            StorageCrateBlockEntity::new,
                            HbmBlocks.CRATE_IRON.get(),
                            HbmBlocks.CRATE_STEEL.get(),
                            HbmBlocks.CRATE_DESH.get(),
                            HbmBlocks.CRATE_TEMPLATE.get(),
                            HbmBlocks.CRATE_TUNGSTEN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SafeBlockEntity>> SAFE =
            BLOCK_ENTITIES.register(
                    "safe",
                    () -> BlockEntityType.Builder.of(
                            SafeBlockEntity::new,
                            HbmBlocks.SAFE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MassStorageBlockEntity>> MASS_STORAGE =
            BLOCK_ENTITIES.register(
                    "mass_storage",
                    () -> BlockEntityType.Builder.of(
                            MassStorageBlockEntity::new,
                            HbmBlocks.MASS_STORAGE.get(),
                            HbmBlocks.MASS_STORAGE_IRON.get(),
                            HbmBlocks.MASS_STORAGE_DESH.get(),
                            HbmBlocks.MASS_STORAGE_WOOD.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyEmitterBlockEntity>> LEGACY_EMITTER =
            BLOCK_ENTITIES.register(
                    "legacy_emitter",
                    () -> BlockEntityType.Builder.of(
                            LegacyEmitterBlockEntity::new,
                            HbmBlocks.GEYSIR_CHLORINE.get(),
                            HbmBlocks.GEYSIR_NETHER.get(),
                            HbmBlocks.VENT_CHLORINE.get(),
                            HbmBlocks.VENT_CLOUD.get(),
                            HbmBlocks.VENT_PINK_CLOUD.get(),
                            HbmBlocks.VENT_CHLORINE_SEAL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyMachineBlockEntity>> LEGACY_MACHINE =
            BLOCK_ENTITIES.register(
                    "legacy_machine",
                    () -> BlockEntityType.Builder.of(
                            LegacyMachineBlockEntity::new,
                            HbmBlocks.MACHINE_ANNIHILATOR.get(),
                            HbmBlocks.MACHINE_AUTOCRAFTER.get(),
                            HbmBlocks.MACHINE_AUTOSAW.get(),
                            HbmBlocks.MACHINE_THRESHER.get(),
                            HbmBlocks.MACHINE_LPW2.get(),
                            HbmBlocks.MACHINE_FORCEFIELD.get(),
                            HbmBlocks.MACHINE_MISSILE_ASSEMBLY.get(),
                            HbmBlocks.MACHINE_ORBUS.get(),
                            HbmBlocks.MACHINE_PRECASS.get(),
                            HbmBlocks.MACHINE_PYROOVEN.get(),
                            HbmBlocks.MACHINE_RADAR.get(),
                            HbmBlocks.MACHINE_RADAR_LARGE.get(),
                            HbmBlocks.MACHINE_RADGEN.get(),
                            HbmBlocks.MACHINE_RADIOLYSIS.get(),
                            HbmBlocks.MACHINE_RTG_GREY.get(),
                            HbmBlocks.MACHINE_SATLINKER.get(),
                            HbmBlocks.MACHINE_SAWMILL.get(),
                            HbmBlocks.MACHINE_TELEPORTER.get(),
                            HbmBlocks.MACHINE_TURBOFAN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorPressBlockEntity>> CONVEYOR_PRESS =
            BLOCK_ENTITIES.register(
                    "machine_conveyor_press",
                    () -> BlockEntityType.Builder.of(
                            ConveyorPressBlockEntity::new,
                            HbmBlocks.MACHINE_CONVEYOR_PRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarScreenBlockEntity>> RADAR_SCREEN =
            BLOCK_ENTITIES.register(
                    "radar_screen",
                    () -> BlockEntityType.Builder.of(
                            RadarScreenBlockEntity::new,
                            HbmBlocks.RADAR_SCREEN.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RebarBlockEntity>> REBAR =
            BLOCK_ENTITIES.register(
                    "rebar",
                    () -> BlockEntityType.Builder.of(RebarBlockEntity::new, HbmBlocks.REBAR.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineDummyBlockEntity>> MACHINE_DUMMY =
            BLOCK_ENTITIES.register(
                    "machine_dummy",
                    () -> BlockEntityType.Builder.of(
                            MachineDummyBlockEntity::new,
                            HbmBlocks.MACHINE_DUMMY.get(),
                            HbmBlocks.MACHINE_DIFURNACE_EXT.get(),
                            HbmBlocks.DUMMY_PLATE_COMPACT_LAUNCHER.get(),
                            HbmBlocks.DUMMY_PORT_COMPACT_LAUNCHER.get(),
                            HbmBlocks.DUMMY_PLATE_LAUNCH_TABLE.get(),
                            HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneWaypointBlockEntity>> DRONE_WAYPOINT =
            BLOCK_ENTITIES.register(
                    "drone_waypoint",
                    () -> BlockEntityType.Builder.of(DroneWaypointBlockEntity::new, HbmBlocks.DRONE_WAYPOINT.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneRequestWaypointBlockEntity>> DRONE_WAYPOINT_REQUEST =
            BLOCK_ENTITIES.register(
                    "drone_waypoint_request",
                    () -> BlockEntityType.Builder.of(DroneRequestWaypointBlockEntity::new, HbmBlocks.DRONE_WAYPOINT_REQUEST.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneCrateBlockEntity>> DRONE_CRATE =
            BLOCK_ENTITIES.register(
                    "drone_crate",
                    () -> BlockEntityType.Builder.of(DroneCrateBlockEntity::new, HbmBlocks.DRONE_CRATE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneDockBlockEntity>> DRONE_DOCK =
            BLOCK_ENTITIES.register(
                    "drone_dock",
                    () -> BlockEntityType.Builder.of(DroneDockBlockEntity::new, HbmBlocks.DRONE_DOCK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneProviderBlockEntity>> DRONE_PROVIDER =
            BLOCK_ENTITIES.register(
                    "drone_crate_provider",
                    () -> BlockEntityType.Builder.of(DroneProviderBlockEntity::new, HbmBlocks.DRONE_CRATE_PROVIDER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneRequesterBlockEntity>> DRONE_REQUESTER =
            BLOCK_ENTITIES.register(
                    "drone_crate_requester",
                    () -> BlockEntityType.Builder.of(DroneRequesterBlockEntity::new, HbmBlocks.DRONE_CRATE_REQUESTER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyDisplayStandBlockEntity>> LEGACY_DISPLAY_STAND =
            BLOCK_ENTITIES.register(
                    "legacy_display_stand",
                    () -> BlockEntityType.Builder.of(
                            LegacyDisplayStandBlockEntity::new,
                            HbmBlocks.PEDESTAL.get(),
                            HbmBlocks.SKELETON_HOLDER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealHatchBlockEntity>> SEAL_HATCH =
            BLOCK_ENTITIES.register(
                    "seal_hatch",
                    () -> BlockEntityType.Builder.of(SealHatchBlockEntity::new, HbmBlocks.SEAL_HATCH.get()).build(null)
            );

    private HbmBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
