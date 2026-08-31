package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
import com.reinhardt.hbm.client.model.CoatedCableCtmModel;
import com.reinhardt.hbm.client.model.BlastDoorItemModel;
import com.reinhardt.hbm.client.model.DecorativeCtmBlockModel;
import com.reinhardt.hbm.client.model.ExhaustDuctBakedModel;
import com.reinhardt.hbm.client.model.FluidDuctNeoBakedModel;
import com.reinhardt.hbm.client.model.FluidTankItemModel;
import com.reinhardt.hbm.client.model.HexafluorideTankItemModel;
import com.reinhardt.hbm.client.model.GeothermalHeatExchangerItemModel;
import com.reinhardt.hbm.client.model.GlyphidBaseBakedModel;
import com.reinhardt.hbm.client.model.IndustrialTurbineItemModel;
import com.reinhardt.hbm.client.model.LargeFactoryItemModel;
import com.reinhardt.hbm.client.model.LandmineItemModel;
import com.reinhardt.hbm.client.model.ObjItemAutoFitModel;
import com.reinhardt.hbm.client.model.DedicatedItemRendererModel;
import com.reinhardt.hbm.client.model.ObjMachineItemModel;
import com.reinhardt.hbm.client.model.PurexItemModel;
import com.reinhardt.hbm.client.model.LegacyMachineItemModel;
import com.reinhardt.hbm.client.model.RedCableNeoBakedModel;
import com.reinhardt.hbm.client.model.ClassicCableBakedModel;
import com.reinhardt.hbm.client.model.PowerCableBoxBakedModel;
import com.reinhardt.hbm.client.model.PowerGaugeBakedModel;
import com.reinhardt.hbm.client.model.PaintableCableBakedModel;
import com.reinhardt.hbm.client.model.ReedsBakedModel;
import com.reinhardt.hbm.client.model.SteamEngineItemModel;
import com.reinhardt.hbm.client.model.SteelScaffoldBakedModel;
import com.reinhardt.hbm.client.model.StirlingGeneratorItemModel;
import com.reinhardt.hbm.client.model.TurretItemModel;
import com.reinhardt.hbm.client.particle.ChimneySmokeParticle;
import com.reinhardt.hbm.client.particle.ArcFurnaceSmokeParticle;
import com.reinhardt.hbm.client.particle.ContrailParticle;
import com.reinhardt.hbm.client.particle.CoolingTowerParticle;
import com.reinhardt.hbm.client.particle.DrainSplashParticle;
import com.reinhardt.hbm.client.particle.DrainTowerParticle;
import com.reinhardt.hbm.client.particle.PyroOvenTowerParticle;
import com.reinhardt.hbm.client.particle.PartEmitterTowerParticle;
import com.reinhardt.hbm.client.particle.RotaryFurnaceTowerParticle;
import com.reinhardt.hbm.client.particle.FalloutRainParticle;
import com.reinhardt.hbm.client.particle.FlamethrowerParticle;
import com.reinhardt.hbm.client.particle.GasFlameParticle;
import com.reinhardt.hbm.client.particle.GasFlareBurnSmokeParticle;
import com.reinhardt.hbm.client.particle.GasFlareSmokeParticle;
import com.reinhardt.hbm.client.particle.HazeParticle;
import com.reinhardt.hbm.client.particle.GibletParticle;
import com.reinhardt.hbm.client.particle.LegacyExplosionCloudParticle;
import com.reinhardt.hbm.client.particle.LandmineFoamParticle;
import com.reinhardt.hbm.client.particle.LandmineSmokeParticle;
import com.reinhardt.hbm.client.particle.LegacyChemicalCloudParticle;
import com.reinhardt.hbm.client.particle.LegacyMistParticle;
import com.reinhardt.hbm.client.particle.LegacyPlasmaBlastParticle;
import com.reinhardt.hbm.client.particle.LegacySmallExplosionParticle;
import com.reinhardt.hbm.client.particle.MeteorTailParticle;
import com.reinhardt.hbm.client.particle.MukeCloudParticle;
import com.reinhardt.hbm.client.particle.MukeFlashParticle;
import com.reinhardt.hbm.client.particle.MukeWaveParticle;
import com.reinhardt.hbm.client.particle.NukeTorexParticle;
import com.reinhardt.hbm.client.particle.RadiationFogParticle;
import com.reinhardt.hbm.client.particle.RbmkFireParticle;
import com.reinhardt.hbm.client.particle.RbmkMushParticle;
import com.reinhardt.hbm.client.particle.VolcanoSmokeParticle;
import com.reinhardt.hbm.client.particle.TauHadronParticle;
import com.reinhardt.hbm.client.particle.TauSparkParticle;
import com.reinhardt.hbm.client.particle.VomitParticle;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.LegacyPipetteItem;
import com.reinhardt.hbm.item.HbmFluidDuctItem;
import com.reinhardt.hbm.item.IcfPelletItem;
import com.reinhardt.hbm.item.LegacyCrayonItem;
import com.reinhardt.hbm.item.LegacyMinecartItem;
import com.reinhardt.hbm.item.LegacyTrainItem;
import com.reinhardt.hbm.item.LegacyDroneItem;
import com.reinhardt.hbm.entity.LegacyRequestDroneEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.LegacyHbmContent;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HbmClientRenderers {
    private HbmClientRenderers() {
    }

    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(HbmItems.CART.get(), ReinhardtsHBM.id("cart_variant"),
                    (stack, level, entity, seed) -> LegacyMinecartItem.base(stack).ordinal() * 10.0F + LegacyMinecartItem.type(stack).ordinal());
            ItemProperties.register(HbmItems.TRAIN.get(), ReinhardtsHBM.id("train_variant"),
                    (stack, level, entity, seed) -> LegacyTrainItem.type(stack).ordinal());
            ItemProperties.register(HbmItems.DRONE.get(), ReinhardtsHBM.id("drone_type"),
                    (stack, level, entity, seed) -> LegacyDroneItem.Type.fromStack(stack).ordinal());
        });
        event.enqueueWork(() -> ItemProperties.register(HbmItems.GEM_ALEXANDRITE.get(), ReinhardtsHBM.id("alexandrite_light"),
                (stack, level, entity, seed) -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.level == null || minecraft.player == null) {
                        return 0.0F;
                    }
                    return minecraft.level.getBrightness(LightLayer.BLOCK, minecraft.player.blockPosition()) / 15.0F;
                }));
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LegacyRedPylonModel.LAYER, LegacyRedPylonModel::createLayer);
        PoleBlockEntityRenderer.registerLayerDefinitions(event);
        event.registerLayerDefinition(LegacyChopperModel.LAYER, LegacyChopperModel::createLayer);
        event.registerLayerDefinition(LegacyChopperMineModel.LAYER, LegacyChopperMineModel::createLayer);
        event.registerLayerDefinition(LegacyRadioboxModel.LAYER, LegacyRadioboxModel::createLayer);
        event.registerLayerDefinition(LegacyBroadcasterModel.LAYER, LegacyBroadcasterModel::createLayer);
    }

    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new TeslaBackLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HbmBlockEntities.FAN.get(), FanBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SPOTLIGHT.get(), SpotlightBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FLOODLIGHT.get(), FloodlightBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.GEIGER.get(), GeigerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CARGO_ELEVATOR.get(), CargoElevatorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SAT_DOCK.get(), SatelliteDockBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.BROADCASTER.get(), BroadcasterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DEMON_LAMP.get(), DemonLampBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.RADIO_TORCH.get(), RadioTorchBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.AUTOCAL.get(), AutocalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.RADIO_TELEX.get(), RadioTelexBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.RADIOBOX.get(), RadioboxBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.REFUELER.get(), RefuelerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.VENDING_MACHINE.get(), VendingMachineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DECO_EMITTER.get(), DecorationEmitterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.BOBBLEHEAD.get(), BobbleheadBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SNOWGLOBE.get(), SnowglobeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.PLUSHIE.get(), PlushieBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LANTERN.get(), LanternBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LANTERN_BEHEMOTH.get(), LanternBehemothBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DECO_DISPLAY.get(), DecoDisplayBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LEGACY_DISPLAY_STAND.get(), LegacyDisplayStandBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FILING_CABINET.get(), FilingCabinetBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.TAPE_RECORDER.get(), TapeRecorderBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.WOOD_BURNER.get(), WoodBurnerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DIESEL_GENERATOR.get(), DieselGeneratorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.COMBUSTION_ENGINE.get(), CombustionEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.GAS_FLARE.get(), GasFlareBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.HEATER.get(), HeaterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.STIRLING_GENERATOR.get(), StirlingGeneratorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.STEAM_ENGINE.get(), SteamEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.MICROWAVE.get(), MicrowaveBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.INDUSTRIAL_TURBINE.get(), IndustrialTurbineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LARGE_TURBINE.get(), LargeTurbineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LEVIATHAN_TURBINE.get(), LeviathanTurbineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.GAS_TURBINE.get(), GasTurbineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.POWERED_STEAM_CONDENSER.get(), PoweredSteamCondenserBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.POWER_PYLON.get(), PowerPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.POLE.get(), PoleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ASSEMBLY_MACHINE.get(), AssemblyMachineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ASSEMBLY_FACTORY.get(), AssemblyFactoryBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CHEMICAL_PLANT.get(), ChemicalPlantBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CHEMICAL_FACTORY.get(), ChemicalFactoryBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SOLDERING_STATION.get(), SolderingStationBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ARC_WELDER.get(), ArcWelderBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ARC_FURNACE.get(), ArcFurnaceBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.COMPRESSOR.get(), CompressorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.MIXER.get(), MixerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FUNNEL.get(), FunnelBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ASHPIT.get(), AshpitBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.AMMO_PRESS.get(), AmmoPressBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.PRESS.get(), PressBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.HEAT_BOILER.get(), HeatBoilerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.INDUSTRIAL_BOILER.get(), IndustrialBoilerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SOLAR_BOILER.get(), SolarBoilerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.AIR_COMPRESSOR.get(), AirCompressorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SOLAR_MIRROR.get(), SolarMirrorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.GROUNDWATER_PUMP.get(), GroundwaterPumpBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.GEOTHERMAL_HEAT_EXCHANGER.get(), GeothermalHeatExchangerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.COOLING_TOWER.get(), CoolingTowerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.OIL_DERRICK.get(), OilDerrickBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FRACKING_TOWER.get(), FrackingTowerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.REFINERY.get(), RefineryBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.VACUUM_DISTILL.get(), VacuumDistillBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.COKER.get(), CokerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FURNACE_COMBINATION.get(), FurnaceCombinationBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ROTARY_FURNACE.get(), RotaryFurnaceBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SOLIDIFIER.get(), SolidifierBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LIQUEFACTOR.get(), LiquefactorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ELECTROLYZER.get(), ElectrolyzerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FRACTION_TOWER.get(), FractionTowerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FRACTION_SPACER.get(), FractionSpacerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CATALYTIC_CRACKER.get(), CatalyticCrackerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CATALYTIC_REFORMER.get(), CatalyticReformerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.HYDROTREATER.get(), HydrotreaterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.MACHINE_BLAST_FURNACE.get(), MachineBlastFurnaceBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.IRON_FURNACE.get(), IronFurnaceBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.STEEL_FURNACE.get(), SteelFurnaceBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CRUCIBLE.get(), CrucibleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FOUNDRY_CASTING.get(), FoundryCastingBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FOUNDRY_FLOW.get(), FoundryFlowBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FOUNDRY_TANK.get(), FoundryTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FOUNDRY_SLAG.get(), FoundrySlagBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.STRAND_CASTER.get(), StrandCasterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.HEAVY_DOOR.get(), HbmHeavyDoorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.BLAST_DOOR.get(), BlastDoorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FLUID_TANK.get(), FluidTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FLUID_BARREL.get(), FluidBarrelBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.BIG_ASS_TANK.get(), BigAssTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LARGE_FLUID_TANK.get(), LargeFluidTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.BATTERY_REDD.get(), BatteryReddBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.BATTERY_SOCKET.get(), BatterySocketBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CHARGER.get(), ChargerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CENTRIFUGE.get(), CentrifugeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.GAS_CENTRIFUGE.get(), GasCentrifugeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CRYSTALLIZER.get(), CrystallizerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CYCLOTRON.get(), CyclotronBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.EXPOSURE_CHAMBER.get(), ExposureChamberBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DEUTERIUM_TOWER.get(), DeuteriumTowerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SILEX.get(), SilexBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FEL.get(), FelBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.PARTICLE_ACCELERATOR.get(), ParticleAcceleratorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.MINING_LASER.get(), MiningLaserBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.TESLA_COIL.get(), TeslaCoilBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.EXCAVATOR.get(), ExcavatorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DRAIN.get(), DrainBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ORE_SLOPPER.get(), OreSlopperBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CHIMNEY.get(), ChimneyBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.BREEDER_REACTOR.get(), BreederReactorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.TURRET_JEREMY.get(), TurretJeremyBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.TURRET_CHEKHOV.get(), TurretChekhovBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LEGACY_TURRET.get(), LegacyTurretBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.RESEARCH_REACTOR.get(), ResearchReactorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.WATZ.get(), WatzBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.PUREX.get(), PurexBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.WATZ_STRUCT.get(), WatzStructBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.WATZ_PUMP.get(), WatzPumpBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.NUKE_BOY.get(), NukeBoyBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LEGACY_NUKE.get(), LegacyNukeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.BOMB_MULTI.get(), BombMultiBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CRASHED_BOMB.get(), CrashedBombBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LANDMINE.get(), LandmineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.WALL_CHARGE.get(), WallChargeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LAUNCHER.get(), LauncherBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SOYUZ_LAUNCHER.get(), SoyuzLauncherBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.SOYUZ_CAPSULE.get(), SoyuzCapsuleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.WAND_STRUCTURE.get(), WandStructureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.RBMK_COMPONENT.get(), RbmkComponentBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FUSION_MACHINE.get(), FusionMachineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.FUSION_TORUS_STRUCT.get(), FusionTorusStructBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ICF_CORE.get(), IcfCoreBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DFC_CORE.get(), DfcCoreBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DFC_EMITTER.get(), DfcComponentBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DFC_RECEIVER.get(), DfcComponentBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DFC_INJECTOR.get(), DfcComponentBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.DFC_STABILIZER.get(), DfcComponentBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ZIRNOX_REACTOR.get(), ZirnoxReactorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.ZIRNOX_DESTROYED.get(), ZirnoxDestroyedBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.CONVEYOR_PRESS.get(), ConveyorPressBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.LEGACY_MACHINE.get(), LegacyMachineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.HEXAFLUORIDE_TANK.get(), HexafluorideTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.RADAR_SCREEN.get(), RadarScreenBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.REBAR.get(), RebarBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.PIPE_ANCHOR.get(), PipeAnchorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HbmBlockEntities.PISTON_INSERTER.get(), PistonInserterBlockEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.COG.get(), CogEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.SAWBLADE.get(), SawbladeEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.CONVEYOR_ITEM.get(), ConveyorMovingItemRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.RBMK_DEBRIS.get(), RbmkDebrisEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.MINE_RUBBLE.get(), MineRubbleEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.RUBBER_BOAT.get(), RubberBoatEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.MINER_ROCKET.get(), MinerRocketEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.DUCK.get(), LegacyDuckEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.DELIVERY_DRONE.get(), LegacyDroneEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.REQUEST_DRONE.get(), LegacyDroneEntityRenderer<LegacyRequestDroneEntity>::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_BOMBER.get(), LegacyBomberEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_UFO.get(), LegacyUfoEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_CHOPPER.get(), LegacyChopperEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_CHOPPER_MINE.get(), LegacyChopperMineEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_WORM_HEAD.get(), LegacyWormEntityRenderer.Head::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_WORM_BODY.get(), LegacyWormEntityRenderer.Body::new);
        event.registerEntityRenderer(HbmEntityTypes.GLYPHID.get(), GlyphidEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.GLYPHID_ACID_BOMB.get(), GlyphidAcidBombEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.GLYPHID_ACID_SPRAY.get(), GlyphidAcidSprayEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.GLYPHID_WAYPOINT.get(), NoopRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.PARASITE_MAGGOT.get(), ParasiteMaggotEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_BOSS_PROJECTILE.get(), LegacyBossProjectileEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_BOMBLET.get(), NoopRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.CLUSTER_SUBMUNITION.get(), NoopRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_BOXCAR.get(), NoopRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_BOBMAZON.get(), LegacyBobmazonEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_MINECART.get(), LegacyMinecartEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_TRAIN.get(), LegacyTrainEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.DIGAMMA_SPEAR.get(), DigammaSpearEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.JEREMY_SHELL.get(), JeremyShellEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.CHEKHOV_BULLET.get(), ChekhovBulletEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_BULLET.get(), LegacyProjectileEntityRenderer.Bullet::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_ARTILLERY_SHELL.get(), LegacyProjectileEntityRenderer.ArtilleryShell::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_HIMARS_ROCKET.get(), LegacyProjectileEntityRenderer.HimarsRocket::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_LINGERING_FIRE.get(), NoopRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_MIST.get(), NoopRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_SHRAPNEL.get(), LegacyShrapnelEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_VORTEX.get(), LegacyVortexEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.METEOR.get(), MeteorEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.FIREWORKS.get(), FireworksEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.TIMED_EXPLOSIVE.get(), TimedExplosiveEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.LEGACY_GRENADE.get(), LegacyGrenadeEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.UNDEAD_SOLDIER.get(), LegacyUndeadSoldierEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.UNIVERSAL_GRENADE.get(), UniversalGrenadeEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.NUKE_TOREX.get(), NukeTorexEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.SOYUZ.get(), SoyuzEntityRenderer::new);
        event.registerEntityRenderer(HbmEntityTypes.SOYUZ_CAPSULE.get(), SoyuzCapsuleEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(HbmParticleTypes.COOLING_TOWER.get(), CoolingTowerParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.GAS_FLARE_SMOKE.get(), GasFlareSmokeParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.GAS_FLARE_FLAME.get(), GasFlameParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.GAS_FLARE_BURN_SMOKE.get(), GasFlareBurnSmokeParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.CHIMNEY_SMOKE_BRICK.get(), sprites -> new ChimneySmokeParticle.Provider(sprites, 0.5F));
        event.registerSpriteSet(HbmParticleTypes.CHIMNEY_SMOKE_INDUSTRIAL.get(), sprites -> new ChimneySmokeParticle.Provider(sprites, 0.75F));
        event.registerSpriteSet(HbmParticleTypes.ARC_FURNACE_SMOKE.get(), ArcFurnaceSmokeParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.VOMIT.get(), sprites -> new VomitParticle.Provider(sprites, false));
        event.registerSpriteSet(HbmParticleTypes.BLOOD_VOMIT.get(), sprites -> new VomitParticle.Provider(sprites, true));
        event.registerSpriteSet(HbmParticleTypes.RBMK_MUSH.get(), RbmkMushParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.NUKE_TOREX.get(), NukeTorexParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.RBMK_FIRE.get(), RbmkFireParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.VOLCANO_SMOKE.get(), VolcanoSmokeParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.FALLOUT_RAIN.get(), FalloutRainParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.RADIATION_FOG.get(), RadiationFogParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.METEOR_TAIL.get(), MeteorTailParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.KEROSENE_ROCKET_FLAME.get(), ContrailParticle.Provider::new);
        event.registerSpecial(HbmParticleTypes.TAU_SPARK.get(), new TauSparkParticle.Provider());
        event.registerSpriteSet(HbmParticleTypes.TAU_HADRON.get(), TauHadronParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.MUKE_FLASH.get(), sprites -> new MukeFlashParticle.Provider(sprites, false));
        event.registerSpriteSet(HbmParticleTypes.MUKE_FLASH_BALEFIRE.get(), sprites -> new MukeFlashParticle.Provider(sprites, true));
        event.registerSpriteSet(HbmParticleTypes.MUKE_WAVE.get(), MukeWaveParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.MUKE_CLOUD.get(), MukeCloudParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.MUKE_CLOUD_BALEFIRE.get(), MukeCloudParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.HAZE.get(), HazeParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.LEGACY_EXPLOSION_CLOUD.get(), LegacyExplosionCloudParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.LEGACY_SMALL_EXPLOSION.get(), LegacySmallExplosionParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.LEGACY_MIST.get(), LegacyMistParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.LEGACY_PLASMA_BLAST.get(), LegacyPlasmaBlastParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.FLAMETHROWER_FIRE.get(), sprites -> new FlamethrowerParticle.Provider(sprites, false));
        event.registerSpriteSet(HbmParticleTypes.FLAMETHROWER_BALEFIRE.get(), sprites -> new FlamethrowerParticle.Provider(sprites, true));
        event.registerSpriteSet(HbmParticleTypes.GIBLET_MEAT.get(), sprites -> new GibletParticle.Provider(sprites, GibletParticle.MEAT));
        event.registerSpriteSet(HbmParticleTypes.GIBLET_SLIME.get(), sprites -> new GibletParticle.Provider(sprites, GibletParticle.SLIME));
        event.registerSpriteSet(HbmParticleTypes.GIBLET_METAL.get(), sprites -> new GibletParticle.Provider(sprites, GibletParticle.METAL));
        event.registerSpriteSet(HbmParticleTypes.LEGACY_CHLORINE_CLOUD.get(), sprites -> new LegacyChemicalCloudParticle.Provider(sprites, LegacyChemicalCloudParticle.Kind.CHLORINE));
        event.registerSpriteSet(HbmParticleTypes.LEGACY_CLOUD.get(), sprites -> new LegacyChemicalCloudParticle.Provider(sprites, LegacyChemicalCloudParticle.Kind.CLOUD));
        event.registerSpriteSet(HbmParticleTypes.LEGACY_PINK_CLOUD.get(), sprites -> new LegacyChemicalCloudParticle.Provider(sprites, LegacyChemicalCloudParticle.Kind.CLOUD));
        event.registerSpriteSet(HbmParticleTypes.LEGACY_ORANGE_CLOUD.get(), sprites -> new LegacyChemicalCloudParticle.Provider(sprites, LegacyChemicalCloudParticle.Kind.ORANGE));
        event.registerSpriteSet(HbmParticleTypes.GEYSER_FIRE.get(), GasFlameParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.PART_EMITTER_TOWER_SMALL.get(),
                sprites -> new PartEmitterTowerParticle.Provider(sprites, false));
        event.registerSpriteSet(HbmParticleTypes.PART_EMITTER_TOWER_LARGE.get(),
                sprites -> new PartEmitterTowerParticle.Provider(sprites, true));
        event.registerSpriteSet(HbmParticleTypes.LANDMINE_SMOKE.get(), LandmineSmokeParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.LANDMINE_FOAM.get(), LandmineFoamParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.DRAIN_TOWER.get(), DrainTowerParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.PYRO_OVEN_TOWER.get(), PyroOvenTowerParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.ROTARY_FURNACE_TOWER.get(), RotaryFurnaceTowerParticle.Provider::new);
        event.registerSpriteSet(HbmParticleTypes.DRAIN_SPLASH.get(), DrainSplashParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        ObjMachineItemRenderer.registerAdditionalModels(event);
        SpotlightBlockEntityRenderer.registerAdditionalModels(event);
        FanBlockEntityRenderer.registerAdditionalModels(event);
        FloodlightBlockEntityRenderer.registerAdditionalModels(event);
        CargoElevatorBlockEntityRenderer.registerAdditionalModels(event);
        DemonLampBlockEntityRenderer.registerAdditionalModels(event);
        RadioTorchBlockEntityRenderer.registerAdditionalModels(event);
        AutocalBlockEntityRenderer.registerAdditionalModels(event);
        RadioTelexBlockEntityRenderer.registerAdditionalModels(event);
        RefuelerBlockEntityRenderer.registerAdditionalModels(event);
        VendingMachineBlockEntityRenderer.registerAdditionalModels(event);
        SatelliteDockBlockEntityRenderer.registerAdditionalModels(event);
        BobbleheadBlockEntityRenderer.registerAdditionalModels(event);
        SnowglobeBlockEntityRenderer.registerAdditionalModels(event);
        PlushieBlockEntityRenderer.registerAdditionalModels(event);
        LanternBlockEntityRenderer.registerAdditionalModels(event);
        FilingCabinetBlockEntityRenderer.registerAdditionalModels(event);
        TapeRecorderBlockEntityRenderer.registerAdditionalModels(event);
        DecoDisplayBlockEntityRenderer.registerAdditionalModels(event);
        LegacyDisplayStandBlockEntityRenderer.registerAdditionalModels(event);
        WoodBurnerBlockEntityRenderer.registerAdditionalModels(event);
        DieselGeneratorBlockEntityRenderer.registerAdditionalModels(event);
        CombustionEngineBlockEntityRenderer.registerAdditionalModels(event);
        GasFlareBlockEntityRenderer.registerAdditionalModels(event);
        HeaterBlockEntityRenderer.registerAdditionalModels(event);
        StirlingGeneratorBlockEntityRenderer.registerAdditionalModels(event);
        SteamEngineBlockEntityRenderer.registerAdditionalModels(event);
        MicrowaveBlockEntityRenderer.registerAdditionalModels(event);
        ToasterItemRenderer.registerAdditionalModels(event);
        IndustrialTurbineBlockEntityRenderer.registerAdditionalModels(event);
        LargeTurbineBlockEntityRenderer.registerAdditionalModels(event);
        LeviathanTurbineBlockEntityRenderer.registerAdditionalModels(event);
        GasTurbineBlockEntityRenderer.registerAdditionalModels(event);
        PoweredSteamCondenserBlockEntityRenderer.registerAdditionalModels(event);
        PowerPylonBlockEntityRenderer.registerAdditionalModels(event);
        AssemblyMachineBlockEntityRenderer.registerAdditionalModels(event);
        AssemblyFactoryBlockEntityRenderer.registerAdditionalModels(event);
        LargeFactoryItemRenderer.registerAdditionalModels(event);
        ChemicalPlantBlockEntityRenderer.registerAdditionalModels(event);
        ChemicalFactoryBlockEntityRenderer.registerAdditionalModels(event);
        SolderingStationBlockEntityRenderer.registerAdditionalModels(event);
        ArcWelderBlockEntityRenderer.registerAdditionalModels(event);
        ArcFurnaceBlockEntityRenderer.registerAdditionalModels(event);
        CompressorBlockEntityRenderer.registerAdditionalModels(event);
        MixerBlockEntityRenderer.registerAdditionalModels(event);
        FunnelBlockEntityRenderer.registerAdditionalModels(event);
        AshpitBlockEntityRenderer.registerAdditionalModels(event);
        AmmoPressBlockEntityRenderer.registerAdditionalModels(event);
        PressBlockEntityRenderer.registerAdditionalModels(event);
        HeatBoilerBlockEntityRenderer.registerAdditionalModels(event);
        IndustrialBoilerBlockEntityRenderer.registerAdditionalModels(event);
        SolarBoilerBlockEntityRenderer.registerAdditionalModels(event);
        AirCompressorBlockEntityRenderer.registerAdditionalModels(event);
        SolarMirrorBlockEntityRenderer.registerAdditionalModels(event);
        GroundwaterPumpBlockEntityRenderer.registerAdditionalModels(event);
        GeothermalHeatExchangerBlockEntityRenderer.registerAdditionalModels(event);
        CoolingTowerBlockEntityRenderer.registerAdditionalModels(event);
        OilDerrickBlockEntityRenderer.registerAdditionalModels(event);
        FrackingTowerBlockEntityRenderer.registerAdditionalModels(event);
        RefineryBlockEntityRenderer.registerAdditionalModels(event);
        VacuumDistillBlockEntityRenderer.registerAdditionalModels(event);
        CokerBlockEntityRenderer.registerAdditionalModels(event);
        FurnaceCombinationBlockEntityRenderer.registerAdditionalModels(event);
        RotaryFurnaceBlockEntityRenderer.registerAdditionalModels(event);
        SolidifierBlockEntityRenderer.registerAdditionalModels(event);
        LiquefactorBlockEntityRenderer.registerAdditionalModels(event);
        ElectrolyzerBlockEntityRenderer.registerAdditionalModels(event);
        FractionTowerBlockEntityRenderer.registerAdditionalModels(event);
        FractionSpacerBlockEntityRenderer.registerAdditionalModels(event);
        CatalyticCrackerBlockEntityRenderer.registerAdditionalModels(event);
        CatalyticReformerBlockEntityRenderer.registerAdditionalModels(event);
        HydrotreaterBlockEntityRenderer.registerAdditionalModels(event);
        MachineBlastFurnaceBlockEntityRenderer.registerAdditionalModels(event);
        IronFurnaceBlockEntityRenderer.registerAdditionalModels(event);
        SteelFurnaceBlockEntityRenderer.registerAdditionalModels(event);
        CrucibleBlockEntityRenderer.registerAdditionalModels(event);
        StrandCasterBlockEntityRenderer.registerAdditionalModels(event);
        HbmHeavyDoorBlockEntityRenderer.registerAdditionalModels(event);
        BlastDoorBlockEntityRenderer.registerAdditionalModels(event);
        BigAssTankBlockEntityRenderer.registerAdditionalModels(event);
        LargeFluidTankBlockEntityRenderer.registerAdditionalModels(event);
        BatteryReddBlockEntityRenderer.registerAdditionalModels(event);
        BatterySocketBlockEntityRenderer.registerAdditionalModels(event);
        ChargerBlockEntityRenderer.registerAdditionalModels(event);
        CentrifugeBlockEntityRenderer.registerAdditionalModels(event);
        GasCentrifugeBlockEntityRenderer.registerAdditionalModels(event);
        CrystallizerBlockEntityRenderer.registerAdditionalModels(event);
        CyclotronBlockEntityRenderer.registerAdditionalModels(event);
        ExposureChamberBlockEntityRenderer.registerAdditionalModels(event);
        DeuteriumTowerBlockEntityRenderer.registerAdditionalModels(event);
        SilexBlockEntityRenderer.registerAdditionalModels(event);
        FelBlockEntityRenderer.registerAdditionalModels(event);
        ParticleAcceleratorBlockEntityRenderer.registerAdditionalModels(event);
        MiningLaserBlockEntityRenderer.registerAdditionalModels(event);
        TeslaCoilBlockEntityRenderer.registerAdditionalModels(event);
        ExcavatorBlockEntityRenderer.registerAdditionalModels(event);
        DrainBlockEntityRenderer.registerAdditionalModels(event);
        OreSlopperBlockEntityRenderer.registerAdditionalModels(event);
        ChimneyBlockEntityRenderer.registerAdditionalModels(event);
        BreederReactorBlockEntityRenderer.registerAdditionalModels(event);
        TurretJeremyBlockEntityRenderer.registerAdditionalModels(event);
        TurretChekhovBlockEntityRenderer.registerAdditionalModels(event);
        LegacyTurretBlockEntityRenderer.registerAdditionalModels(event);
        TurretItemRenderer.registerAdditionalModels(event);
        ResearchReactorBlockEntityRenderer.registerAdditionalModels(event);
        WatzBlockEntityRenderer.registerAdditionalModels(event);
        PurexBlockEntityRenderer.registerAdditionalModels(event);
        WatzPumpBlockEntityRenderer.registerAdditionalModels(event);
        NukeBoyBlockEntityRenderer.registerAdditionalModels(event);
        LegacyNukeBlockEntityRenderer.registerAdditionalModels(event);
        BombMultiBlockEntityRenderer.registerAdditionalModels(event);
        CrashedBombBlockEntityRenderer.registerAdditionalModels(event);
        LandmineBlockEntityRenderer.registerAdditionalModels(event);
        WallChargeBlockEntityRenderer.registerAdditionalModels(event);
        LauncherBlockEntityRenderer.registerAdditionalModels(event);
        SoyuzLauncherBlockEntityRenderer.registerAdditionalModels(event);
        SoyuzCapsuleBlockEntityRenderer.registerAdditionalModels(event);
        RbmkComponentBlockEntityRenderer.registerAdditionalModels(event);
        FusionMachineBlockEntityRenderer.registerAdditionalModels(event);
        IcfCoreBlockEntityRenderer.registerAdditionalModels(event);
        DfcCoreBlockEntityRenderer.registerAdditionalModels(event);
        DfcComponentBlockEntityRenderer.registerAdditionalModels(event);
        RbmkComponentItemRenderer.registerAdditionalModels(event);
        RbmkFuelChannelItemRenderer.registerAdditionalModels(event);
        ZirnoxReactorBlockEntityRenderer.registerAdditionalModels(event);
        ZirnoxDestroyedBlockEntityRenderer.registerAdditionalModels(event);
        ConveyorPressBlockEntityRenderer.registerAdditionalModels(event);
        LegacyMachineBlockEntityRenderer.registerAdditionalModels(event);
        GeigerBlockEntityRenderer.registerAdditionalModels(event);
        HexafluorideTankBlockEntityRenderer.registerAdditionalModels(event);
        RadarScreenBlockEntityRenderer.registerAdditionalModels(event);
        PipeAnchorBlockEntityRenderer.registerAdditionalModels(event);
        PistonInserterBlockEntityRenderer.registerAdditionalModels(event);
        CogEntityRenderer.registerAdditionalModels(event);
        SawbladeEntityRenderer.registerAdditionalModels(event);
        RbmkDebrisEntityRenderer.registerAdditionalModels(event);
        DigammaSpearEntityRenderer.registerAdditionalModels(event);
        LegacyProjectileEntityRenderer.registerAdditionalModels(event);
        JeremyShellEntityRenderer.registerAdditionalModels(event);
        TurretVisualClientEffects.registerAdditionalModels(event);
        MeteorEntityRenderer.registerAdditionalModels(event);
        SoyuzEntityRenderer.registerAdditionalModels(event);
        SoyuzCapsuleEntityRenderer.registerAdditionalModels(event);
        MinerRocketEntityRenderer.registerAdditionalModels(event);
        LegacyBomberEntityRenderer.registerAdditionalModels(event);
        LegacyUfoEntityRenderer.registerAdditionalModels(event);
        LegacyWormEntityRenderer.registerAdditionalModels(event);
        GlyphidEntityRenderer.registerAdditionalModels(event);
        LegacyBossProjectileEntityRenderer.registerAdditionalModels(event);
        LegacyDroneEntityRenderer.registerAdditionalModels(event);
        LegacyBobmazonEntityRenderer.registerAdditionalModels(event);
        LegacyMinecartEntityRenderer.registerAdditionalModels(event);
        LegacyTrainEntityRenderer.registerAdditionalModels(event);
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        CoatedCableCtmModel.replaceModels(event.getModels(), event.getTextureGetter());
        DecorativeCtmBlockModel.replaceModels(event.getModels(), event.getTextureGetter());
        ExhaustDuctBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        FluidDuctNeoBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        RedCableNeoBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        ClassicCableBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        PowerCableBoxBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        PowerGaugeBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        PaintableCableBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        ReedsBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        GlyphidBaseBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        SteelScaffoldBakedModel.replaceModels(event.getModels(), event.getTextureGetter());
        StirlingGeneratorItemModel.replaceModels(event.getModels());
        SteamEngineItemModel.replaceModels(event.getModels());
        IndustrialTurbineItemModel.replaceModels(event.getModels());
        FluidTankItemModel.replaceModels(event.getModels());
        HexafluorideTankItemModel.replaceModels(event.getModels());
        LargeFactoryItemModel.replaceModels(event.getModels());
        PurexItemModel.replaceModels(event.getModels());
        BlastDoorItemModel.replaceModels(event.getModels());
        TurretItemModel.replaceModels(event.getModels());
        LandmineItemModel.replaceModels(event.getModels());
        GeothermalHeatExchangerItemModel.replaceModels(event.getModels());
        ObjItemAutoFitModel.replaceModels(event.getModels());
        ObjMachineItemModel.replaceModels(event.getModels());
        LegacyMachineItemModel.replaceModels(event.getModels());
        DedicatedItemRendererModel.replaceModels(event.getModels());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                LegacyCrayonItem::tint,
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("crayon"))
        );
        event.register(
                com.reinhardt.hbm.item.LegacyBookLoreItem::tint,
                HbmItems.BOOK_LORE.get()
        );
        event.register(
                com.reinhardt.hbm.item.LegacyCustomKitItem::tint,
                HbmItems.KIT_CUSTOM.get()
        );
        event.register(
                (stack, tintIndex) -> FluidIconItem.fluid(stack).map(definition -> 0xFF000000 | definition.color()).orElse(0xFFFFFFFF),
                HbmItems.FLUID_ICON.get()
        );
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0) {
                        return 0xFFFFFFFF;
                    }
                    return 0xFF000000 | FluidIdentifierItem.primary(stack).color();
                },
                HbmItems.FLUID_IDENTIFIER_MULTI.get()
        );
        event.register(
                HbmFluidContainerItem::tint,
                HbmItems.CANISTER_FULL.get(),
                HbmItems.GAS_FULL.get(),
                HbmItems.FLUID_TANK_FULL.get(),
                HbmItems.FLUID_TANK_LEAD_FULL.get(),
                HbmItems.FLUID_BARREL_FULL.get(),
                HbmItems.FLUID_PACK_FULL.get(),
                HbmItems.DISPERSER_CANISTER.get(),
                HbmItems.GLYPHID_GLAND.get()
        );
        event.register(
                LegacyPipetteItem::tint,
                HbmItems.PIPETTE.get(),
                HbmItems.PIPETTE_BORON.get(),
                HbmItems.PIPETTE_LABORATORY.get()
        );
        event.register(
                com.reinhardt.hbm.item.LegacyChemicalDyeItem::tint,
                HbmItems.CHEMICAL_DYE.get()
        );
        event.register(
                HbmFluidDuctItem::tint,
                HbmItems.FF_FLUID_DUCT.get(),
                HbmItems.FLUID_DUCT.get()
        );
        event.register(
                com.reinhardt.hbm.item.LegacyBedrockOreStageItem::tint,
                HbmItems.LEGACY_BEDROCK_ORE.get(),
                HbmItems.ORE_BEDROCK.get(),
                HbmItems.ORE_CENTRIFUGED.get(),
                HbmItems.ORE_CLEANED.get(),
                HbmItems.ORE_SEPARATED.get(),
                HbmItems.ORE_PURIFIED.get(),
                HbmItems.ORE_NITRATED.get(),
                HbmItems.ORE_NITROCRYSTALLINE.get(),
                HbmItems.ORE_DEEPCLEANED.get(),
                HbmItems.ORE_SEARED.get(),
                HbmItems.ORE_ENRICHED.get()
        );
        event.register(
                com.reinhardt.hbm.item.LegacyByproductItem::tint,
                HbmItems.ORE_BYPRODUCT.get()
        );
        event.register(
                (stack, tintIndex) -> {
                    com.reinhardt.hbm.foundry.FoundryMaterial material = null;
                    if (stack.getItem() instanceof com.reinhardt.hbm.item.FoundryShapeItem shapeItem) {
                        material = shapeItem.material(stack);
                    }
                    return material == null ? 0xFFFFFFFF : 0xFF000000 | material.moltenColor();
                },
                HbmItems.PLATE_CAST.get(),
                HbmItems.PLATE_WELDED.get(),
                HbmItems.SHELL.get(),
                HbmItems.WIRE_FINE.get(),
                HbmItems.WIRE_DENSE.get(),
                HbmItems.PIPE.get(),
                HbmItems.BOLT.get(),
                HbmItems.PART_MECHANISM.get(),
                HbmItems.PART_BARREL_LIGHT.get(),
                HbmItems.PART_BARREL_HEAVY.get(),
                HbmItems.PART_RECEIVER_LIGHT.get(),
                HbmItems.PART_RECEIVER_HEAVY.get(),
                HbmItems.PART_STOCK.get(),
                HbmItems.PART_GRIP.get()
        );
        event.register(
                com.reinhardt.hbm.item.RawIngotItem::tint,
                HbmItems.INGOT_RAW.get()
        );
        event.register(
                IcfPelletItem::tint,
                HbmItems.ICF_PELLET.get()
        );
        event.register(
                (stack, tintIndex) -> {
                    com.reinhardt.hbm.foundry.FoundryMaterialStack contents = com.reinhardt.hbm.item.ScrapsItem.contents(stack);
                    return contents == null ? 0xFFFFFFFF : 0xFF000000 | contents.material().moltenColor();
                },
                HbmItems.SCRAPS.get()
        );
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> level == null || pos == null
                        ? 0xFFB2B2B2
                        : 0xFF000000 | net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos),
                HbmBlocks.PLANT_REEDS.get()
        );
        event.register(
                (state, level, pos, tintIndex) -> {
                    int age = state.getValue(com.reinhardt.hbm.block.BalefireBlock.AGE);
                    int shade = Math.max(0, Math.min(255, Math.round(255.0F * (1.0F - age / 30.0F))));
                    return 0xFF000000 | (shade << 16) | (shade << 8) | shade;
                },
                HbmBlocks.BALEFIRE.get()
        );
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 1 || level == null || pos == null) {
                        return 0xFFFFFFFF;
                    }
                    if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe && !pipe.type().isNone()) {
                        return 0xFF000000 | pipe.type().color();
                    }
                    return 0xFFFFFFFF;
                },
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
        );
    }
}
