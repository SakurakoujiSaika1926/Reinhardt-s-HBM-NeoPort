package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.capability.NativeEnergyStorage;
import com.reinhardt.hbm.block.PileGraphiteBlock;
import com.reinhardt.hbm.blockentity.AssemblyFactoryBlockEntity;
import com.reinhardt.hbm.blockentity.AssemblyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.AirCompressorBlockEntity;
import com.reinhardt.hbm.blockentity.AmmoPressBlockEntity;
import com.reinhardt.hbm.blockentity.ArcWelderBlockEntity;
import com.reinhardt.hbm.blockentity.ArcFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.AshpitBlockEntity;
import com.reinhardt.hbm.blockentity.BatteryReddBlockEntity;
import com.reinhardt.hbm.blockentity.BatterySocketBlockEntity;
import com.reinhardt.hbm.blockentity.BrickFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.BigAssTankBlockEntity;
import com.reinhardt.hbm.blockentity.CatalyticCrackerBlockEntity;
import com.reinhardt.hbm.blockentity.CatalyticReformerBlockEntity;
import com.reinhardt.hbm.blockentity.HydrotreaterBlockEntity;
import com.reinhardt.hbm.blockentity.ChimneyBlockEntity;
import com.reinhardt.hbm.blockentity.CentrifugeBlockEntity;
import com.reinhardt.hbm.blockentity.ChemicalFactoryBlockEntity;
import com.reinhardt.hbm.blockentity.ChemicalPlantBlockEntity;
import com.reinhardt.hbm.blockentity.CombustionEngineBlockEntity;
import com.reinhardt.hbm.blockentity.CompressorBlockEntity;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.CokerBlockEntity;
import com.reinhardt.hbm.blockentity.CoolingTowerBlockEntity;
import com.reinhardt.hbm.blockentity.CrystallizerBlockEntity;
import com.reinhardt.hbm.blockentity.CyclotronBlockEntity;
import com.reinhardt.hbm.blockentity.CrucibleBlockEntity;
import com.reinhardt.hbm.blockentity.DfcCoreBlockEntity;
import com.reinhardt.hbm.blockentity.DfcEmitterBlockEntity;
import com.reinhardt.hbm.blockentity.DfcInjectorBlockEntity;
import com.reinhardt.hbm.blockentity.DfcReceiverBlockEntity;
import com.reinhardt.hbm.blockentity.DfcStabilizerBlockEntity;
import com.reinhardt.hbm.blockentity.DeuteriumExtractorBlockEntity;
import com.reinhardt.hbm.blockentity.DieselGeneratorBlockEntity;
import com.reinhardt.hbm.blockentity.DrainBlockEntity;
import com.reinhardt.hbm.blockentity.DroneCrateBlockEntity;
import com.reinhardt.hbm.blockentity.DroneDockBlockEntity;
import com.reinhardt.hbm.blockentity.DroneProviderBlockEntity;
import com.reinhardt.hbm.blockentity.DroneRequesterBlockEntity;
import com.reinhardt.hbm.blockentity.ElectricFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.ElectrolyzerBlockEntity;
import com.reinhardt.hbm.blockentity.ExcavatorBlockEntity;
import com.reinhardt.hbm.blockentity.ExposureChamberBlockEntity;
import com.reinhardt.hbm.blockentity.FelBlockEntity;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.blockentity.FluidBarrelBlockEntity;
import com.reinhardt.hbm.blockentity.FoundryCastingBlockEntity;
import com.reinhardt.hbm.blockentity.FractionTowerBlockEntity;
import com.reinhardt.hbm.blockentity.FurnaceCombinationBlockEntity;
import com.reinhardt.hbm.blockentity.FrackingTowerBlockEntity;
import com.reinhardt.hbm.blockentity.FissureBlockEntity;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.blockentity.GasCentrifugeBlockEntity;
import com.reinhardt.hbm.blockentity.GasFlareBlockEntity;
import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.GroundwaterPumpBlockEntity;
import com.reinhardt.hbm.blockentity.GeothermalHeatExchangerBlockEntity;
import com.reinhardt.hbm.blockentity.HeatBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.blockentity.IndustrialBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.IndustrialTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.IcfPressBlockEntity;
import com.reinhardt.hbm.blockentity.IcfCoreBlockEntity;
import com.reinhardt.hbm.blockentity.IronFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.LargeTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.LargeFluidTankBlockEntity;
import com.reinhardt.hbm.blockentity.LeviathanTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.LiquefactorBlockEntity;
import com.reinhardt.hbm.blockentity.MachineBlastFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineKeyForgeBlockEntity;
import com.reinhardt.hbm.blockentity.MixerBlockEntity;
import com.reinhardt.hbm.blockentity.OilDerrickBlockEntity;
import com.reinhardt.hbm.blockentity.OreSlopperBlockEntity;
import com.reinhardt.hbm.blockentity.ParticleAcceleratorBlockEntity;
import com.reinhardt.hbm.blockentity.PoweredSteamCondenserBlockEntity;
import com.reinhardt.hbm.blockentity.PressBlockEntity;
import com.reinhardt.hbm.blockentity.PurexBlockEntity;
import com.reinhardt.hbm.blockentity.PwrBlockEntity;
import com.reinhardt.hbm.blockentity.PwrControllerBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.blockentity.RefineryBlockEntity;
import com.reinhardt.hbm.blockentity.RebarBlockEntity;
import com.reinhardt.hbm.blockentity.RefuelerBlockEntity;
import com.reinhardt.hbm.blockentity.RotaryFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.VacuumDistillBlockEntity;
import com.reinhardt.hbm.blockentity.WasteDrumBlockEntity;
import com.reinhardt.hbm.blockentity.StorageDrumBlockEntity;
import com.reinhardt.hbm.blockentity.WatzBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.blockentity.SilexBlockEntity;
import com.reinhardt.hbm.blockentity.SolidifierBlockEntity;
import com.reinhardt.hbm.blockentity.SolderingStationBlockEntity;
import com.reinhardt.hbm.blockentity.SoyuzCapsuleBlockEntity;
import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import com.reinhardt.hbm.blockentity.SmallBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.SolarBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.SteamCondenserBlockEntity;
import com.reinhardt.hbm.blockentity.SteamEngineBlockEntity;
import com.reinhardt.hbm.blockentity.SteamTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.SteelFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import com.reinhardt.hbm.blockentity.StrandCasterBlockEntity;
import com.reinhardt.hbm.blockentity.TurretChekhovBlockEntity;
import com.reinhardt.hbm.blockentity.TurretJeremyBlockEntity;
import com.reinhardt.hbm.blockentity.WoodBurnerBlockEntity;
import com.reinhardt.hbm.blockentity.ZirnoxReactorBlockEntity;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.FueledArmorFSBItem;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.item.FixedFluidBarrelBlockItem;
import com.reinhardt.hbm.item.TankSteelItem;
import com.reinhardt.hbm.item.LegacyPipetteItem;
import com.reinhardt.hbm.item.BlowtorchItem;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerGraphNode;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class HbmCapabilities {
    private HbmCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                HbmCapabilities::pileGraphiteItemHandler,
                HbmBlocks.BLOCK_GRAPHITE_DRILLED.get(),
                HbmBlocks.BLOCK_GRAPHITE_FUEL.get(),
                HbmBlocks.BLOCK_GRAPHITE_PLUTONIUM.get(),
                HbmBlocks.BLOCK_GRAPHITE_ROD.get(),
                HbmBlocks.BLOCK_GRAPHITE_SOURCE.get(),
                HbmBlocks.BLOCK_GRAPHITE_LITHIUM.get(),
                HbmBlocks.BLOCK_GRAPHITE_TRITIUM.get(),
                HbmBlocks.BLOCK_GRAPHITE_DETECTOR.get()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.HEATER.get(),
                HbmCapabilities::fuelInputHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ASHPIT.get(),
                HbmCapabilities::ashpitItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.MACHINE_DUMMY.get(),
                HbmCapabilities::dummyFuelInputHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.CONVEYOR_PRESS.get(),
                (press, side) -> new SidedInvWrapper(press, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.DRONE_CRATE.get(),
                (crate, side) -> new SidedInvWrapper(crate, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.DRONE_DOCK.get(),
                (dock, side) -> new SidedInvWrapper(dock, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.DRONE_PROVIDER.get(),
                (provider, side) -> new SidedInvWrapper(provider, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.DRONE_REQUESTER.get(),
                (requester, side) -> new SidedInvWrapper(requester, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.SHREDDER.get(),
                HbmCapabilities::shredderItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ARC_FURNACE.get(),
                (furnace, side) -> new SidedInvWrapper(furnace, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.STORAGE_DRUM.get(),
                (drum, side) -> new SidedInvWrapper(drum, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.MACHINE_KEYFORGE.get(),
                HbmCapabilities::machineKeyForgeItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ASSEMBLY_MACHINE.get(),
                HbmCapabilities::assemblyMachineItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ASSEMBLY_FACTORY.get(),
                HbmCapabilities::assemblyFactoryItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.CHEMICAL_PLANT.get(),
                HbmCapabilities::chemicalPlantItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.CHEMICAL_FACTORY.get(),
                HbmCapabilities::chemicalFactoryItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.SOLDERING_STATION.get(),
                HbmCapabilities::solderingStationItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ARC_WELDER.get(),
                HbmCapabilities::arcWelderItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.COMPRESSOR.get(),
                (compressor, side) -> new SidedInvWrapper(compressor, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.MIXER.get(),
                (mixer, side) -> new SidedInvWrapper(mixer, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ELECTRIC_FURNACE.get(),
                HbmCapabilities::electricFurnaceItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.MACHINE_BLAST_FURNACE.get(),
                HbmCapabilities::machineBlastFurnaceItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.IRON_FURNACE.get(),
                HbmCapabilities::ironFurnaceItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.BRICK_FURNACE.get(),
                (furnace, side) -> new SidedInvWrapper(furnace, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.STEEL_FURNACE.get(),
                HbmCapabilities::steelFurnaceItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.WOOD_BURNER.get(),
                HbmCapabilities::woodBurnerItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.SOYUZ_LAUNCHER.get(),
                HbmCapabilities::soyuzLauncherItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.LAUNCHER.get(),
                HbmCapabilities::launcherItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.SOYUZ_CAPSULE.get(),
                HbmCapabilities::soyuzCapsuleItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.DIESEL_GENERATOR.get(),
                HbmCapabilities::dieselGeneratorItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.COMBUSTION_ENGINE.get(),
                HbmCapabilities::combustionEngineItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.GAS_FLARE.get(),
                HbmCapabilities::gasFlareItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.PRESS.get(),
                HbmCapabilities::pressItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.AMMO_PRESS.get(),
                HbmCapabilities::ammoPressItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.STEAM_TURBINE.get(),
                HbmCapabilities::steamTurbineItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.TURRET_JEREMY.get(),
                HbmCapabilities::turretJeremyItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.TURRET_CHEKHOV.get(),
                HbmCapabilities::turretChekhovItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.LEGACY_TURRET.get(),
                HbmCapabilities::legacyTurretItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.LEGACY_MACHINE.get(),
                (machine, side) -> new SidedInvWrapper(machine, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.LARGE_TURBINE.get(),
                HbmCapabilities::largeTurbineItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.GAS_TURBINE.get(),
                HbmCapabilities::gasTurbineItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.SMALL_BOILER.get(),
                HbmCapabilities::smallBoilerItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.SMALL_ELECTRIC_BOILER.get(),
                HbmCapabilities::smallBoilerItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.OIL_DERRICK.get(),
                HbmCapabilities::oilDerrickItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.CATALYTIC_REFORMER.get(),
                HbmCapabilities::catalyticReformerItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.HYDROTREATER.get(),
                HbmCapabilities::hydrotreaterItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.FRACKING_TOWER.get(),
                HbmCapabilities::frackingTowerItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.REFINERY.get(),
                HbmCapabilities::refineryItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.VACUUM_DISTILL.get(),
                HbmCapabilities::vacuumDistillItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.COKER.get(),
                HbmCapabilities::cokerItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.FURNACE_COMBINATION.get(),
                HbmCapabilities::furnaceCombinationItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ROTARY_FURNACE.get(),
                HbmCapabilities::rotaryFurnaceItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.SOLIDIFIER.get(),
                HbmCapabilities::solidifierItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.LIQUEFACTOR.get(),
                HbmCapabilities::liquefactorItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.CRUCIBLE.get(),
                HbmCapabilities::crucibleItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.FOUNDRY_CASTING.get(),
                HbmCapabilities::foundryCastingItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.STRAND_CASTER.get(),
                HbmCapabilities::strandCasterItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.FLUID_TANK.get(),
                HbmCapabilities::fluidTankItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.FLUID_BARREL.get(),
                HbmCapabilities::fluidTankItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.BIG_ASS_TANK.get(),
                HbmCapabilities::fluidTankItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.LARGE_FLUID_TANK.get(),
                HbmCapabilities::fluidTankItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.STORAGE_CRATE.get(),
                HbmCapabilities::storageCrateItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.BATTERY_REDD.get(),
                HbmCapabilities::batteryReddItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.BATTERY_SOCKET.get(),
                HbmCapabilities::batterySocketItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.CENTRIFUGE.get(),
                HbmCapabilities::centrifugeItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.GAS_CENTRIFUGE.get(),
                HbmCapabilities::gasCentrifugeItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.CRYSTALLIZER.get(),
                HbmCapabilities::crystallizerItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.CYCLOTRON.get(),
                HbmCapabilities::cyclotronItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.EXPOSURE_CHAMBER.get(),
                HbmCapabilities::exposureChamberItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.SILEX.get(),
                (silex, side) -> new SidedInvWrapper(silex, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.FEL.get(),
                (fel, side) -> new SidedInvWrapper(fel, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.PARTICLE_ACCELERATOR.get(),
                HbmCapabilities::particleAcceleratorItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ELECTROLYZER.get(),
                HbmCapabilities::electrolyzerItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ORE_SLOPPER.get(),
                HbmCapabilities::oreSlopperItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.WASTE_DRUM.get(),
                HbmCapabilities::wasteDrumItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.PUREX.get(),
                HbmCapabilities::purexItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ICF_PRESS.get(),
                (press, side) -> new SidedInvWrapper(press, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.WATZ.get(),
                (watz, side) -> new SidedInvWrapper(watz, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.ZIRNOX_REACTOR.get(),
                HbmCapabilities::zirnoxItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.PWR_CONTROLLER.get(),
                (controller, side) -> new SidedInvWrapper(controller, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.PWR_PART.get(),
                HbmCapabilities::pwrPartItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.RBMK_COMPONENT.get(),
                HbmCapabilities::rbmkItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.DFC_CORE.get(),
                (core, side) -> new SidedInvWrapper(core, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.DFC_INJECTOR.get(),
                (injector, side) -> new SidedInvWrapper(injector, side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.DFC_STABILIZER.get(),
                (stabilizer, side) -> new SidedInvWrapper(stabilizer, side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ASSEMBLY_MACHINE.get(),
                HbmCapabilities::assemblyMachineFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ASSEMBLY_FACTORY.get(),
                HbmCapabilities::assemblyFactoryFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ARC_WELDER.get(),
                HbmCapabilities::arcWelderFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.STORAGE_DRUM.get(),
                StorageDrumBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.COMPRESSOR.get(),
                (compressor, side) -> compressor.fluidHandler(compressor.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.MIXER.get(),
                (mixer, side) -> mixer.fluidHandler(mixer.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.FLUID_PIPE.get(),
                (pipe, side) -> pipe.fluidHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.REFUELER.get(),
                RefuelerBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.PIPE_ANCHOR.get(),
                (anchor, side) -> anchor.fluidHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.PISTON_INSERTER.get(),
                (piston, side) -> new SidedInvWrapper(piston, side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DRONE_CRATE.get(),
                DroneCrateBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.LEGACY_TURRET.get(),
                (turret, side) -> turret.fluidHandler(turret.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.LEGACY_MACHINE.get(),
                (machine, side) -> machine.fluidHandler(machine.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DRAIN.get(),
                DrainBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.SOYUZ_LAUNCHER.get(),
                (launcher, side) -> launcher.fluidHandler(launcher.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.LAUNCHER.get(),
                (launcher, side) -> launcher.fluidHandler(launcher.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.CYCLOTRON.get(),
                (cyclotron, side) -> cyclotron.fluidHandler(cyclotron.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.REBAR.get(),
                (rebar, side) -> rebar.fluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DEUTERIUM_EXTRACTOR.get(),
                (extractor, side) -> extractor.fluidHandler(extractor.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DEUTERIUM_TOWER.get(),
                (tower, side) -> tower.fluidHandler(tower.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.PARTICLE_ACCELERATOR.get(),
                (accelerator, side) -> accelerator.fluidHandler(accelerator.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.CHIMNEY.get(),
                ChimneyBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.MACHINE_DUMMY.get(),
                HbmCapabilities::dummyFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.GEOTHERMAL_HEAT_EXCHANGER.get(),
                (exchanger, side) -> exchanger.fluidHandler(exchanger.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.FISSURE.get(),
                FissureBlockEntity::fluidHandler
        );
        // Register one guarded provider for every HBM block. The provider only
        // returns a storage for native PowerEndpoint instances (or their
        // multiblock dummies), so existing item/fluid capabilities remain unaffected.
        event.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                HbmCapabilities::nativeEnergyStorage,
                HbmBlocks.BLOCKS.getEntries().stream()
                        .map(holder -> holder.get())
                        .toArray(Block[]::new)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.AIR_COMPRESSOR.get(),
                HbmCapabilities::airCompressorFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.HEATER.get(),
                HbmCapabilities::heaterFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.WOOD_BURNER.get(),
                WoodBurnerBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DIESEL_GENERATOR.get(),
                DieselGeneratorBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.COMBUSTION_ENGINE.get(),
                CombustionEngineBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.GAS_FLARE.get(),
                GasFlareBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.CHEMICAL_PLANT.get(),
                HbmCapabilities::chemicalPlantFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.CHEMICAL_FACTORY.get(),
                HbmCapabilities::chemicalFactoryFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.MACHINE_BLAST_FURNACE.get(),
                HbmCapabilities::machineBlastFurnaceFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.SOLDERING_STATION.get(),
                SolderingStationBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.FLUID_TANK.get(),
                FluidTankBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.FLUID_BARREL.get(),
                FluidBarrelBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.BIG_ASS_TANK.get(),
                BigAssTankBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.LARGE_FLUID_TANK.get(),
                LargeFluidTankBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.HEAT_BOILER.get(),
                (boiler, side) -> boiler.fluidHandler(boiler.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.INDUSTRIAL_BOILER.get(),
                (boiler, side) -> boiler.fluidHandler(boiler.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.SOLAR_BOILER.get(),
                (boiler, side) -> boiler.fluidHandler(boiler.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.STEAM_TURBINE.get(),
                SteamTurbineBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.STEAM_ENGINE.get(),
                SteamEngineBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.INDUSTRIAL_TURBINE.get(),
                IndustrialTurbineBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.LARGE_TURBINE.get(),
                LargeTurbineBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.LEVIATHAN_TURBINE.get(),
                LeviathanTurbineBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.GAS_TURBINE.get(),
                GasTurbineBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.SMALL_BOILER.get(),
                SmallBoilerBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.SMALL_ELECTRIC_BOILER.get(),
                SmallBoilerBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.STEAM_CONDENSER.get(),
                SteamCondenserBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.POWERED_STEAM_CONDENSER.get(),
                (condenser, side) -> condenser.fluidHandler(condenser.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.COOLING_TOWER.get(),
                (tower, side) -> tower.fluidHandler(tower.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.GROUNDWATER_PUMP.get(),
                (pump, side) -> pump.fluidHandler(pump.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.OIL_DERRICK.get(),
                (derrick, side) -> derrick.fluidHandler(derrick.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.FRACKING_TOWER.get(),
                (tower, side) -> tower.fluidHandler(tower.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.REFINERY.get(),
                (refinery, side) -> refinery.fluidHandler(refinery.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.VACUUM_DISTILL.get(),
                (distill, side) -> distill.fluidHandler(distill.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.COKER.get(),
                (coker, side) -> coker.fluidHandler(coker.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.FURNACE_COMBINATION.get(),
                (furnace, side) -> furnace.fluidHandler(furnace.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ROTARY_FURNACE.get(),
                (furnace, side) -> furnace.fluidHandler(furnace.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.SOLIDIFIER.get(),
                (solidifier, side) -> solidifier.fluidHandler(solidifier.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.STRAND_CASTER.get(),
                (caster, side) -> caster.fluidHandler(caster.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ELECTROLYZER.get(),
                (electrolyzer, side) -> electrolyzer.fluidHandler(electrolyzer.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.LIQUEFACTOR.get(),
                (liquefactor, side) -> liquefactor.fluidHandler(liquefactor.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.CATALYTIC_CRACKER.get(),
                (cracker, side) -> cracker.fluidHandler(cracker.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.CATALYTIC_REFORMER.get(),
                (reformer, side) -> reformer.fluidHandler(reformer.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.HYDROTREATER.get(),
                (hydrotreater, side) -> hydrotreater.fluidHandler(hydrotreater.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.FRACTION_TOWER.get(),
                (tower, side) -> tower.fluidHandler(tower.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.GAS_CENTRIFUGE.get(),
                (centrifuge, side) -> centrifuge.fluidHandler(centrifuge.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.EXCAVATOR.get(),
                (excavator, side) -> excavator.fluidHandler(excavator.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ORE_SLOPPER.get(),
                (slopper, side) -> slopper.fluidHandler(slopper.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.CRYSTALLIZER.get(),
                HbmCapabilities::crystallizerFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.SILEX.get(),
                SilexBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.WATZ.get(),
                (watz, side) -> watz.fluidHandler(watz.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ZIRNOX_REACTOR.get(),
                (reactor, side) -> reactor.fluidHandler(reactor.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.PUREX.get(),
                PurexBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ICF_PRESS.get(),
                IcfPressBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.ICF_CORE.get(),
                (core, side) -> core.fluidHandler(core.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.PWR_CONTROLLER.get(),
                (controller, side) -> controller.fluidHandler(controller.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.PWR_PART.get(),
                HbmCapabilities::pwrPartFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.RBMK_COMPONENT.get(),
                RbmkComponentBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.FUSION_MACHINE.get(),
                (machine, side) -> machine.fluidHandler(machine.getBlockPos(), side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DFC_CORE.get(),
                DfcCoreBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DFC_EMITTER.get(),
                DfcEmitterBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DFC_RECEIVER.get(),
                DfcReceiverBlockEntity::fluidHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HbmBlockEntities.DFC_INJECTOR.get(),
                DfcInjectorBlockEntity::fluidHandler
        );
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> {
                    if (stack.getItem() instanceof HbmFluidContainerItem container) {
                        return container.createFluidHandler(stack);
                    }
                    if (stack.getItem() instanceof FueledArmorFSBItem armor) {
                        return armor.createFluidHandler(stack);
                    }
                    if (stack.getItem() instanceof InfiniteFluidContainerItem infinite) {
                        return infinite.createFluidHandler(stack);
                    }
                    if (stack.getItem() instanceof TankSteelItem tank) {
                        return tank.createFluidHandler(stack);
                    }
                    if (stack.getItem() instanceof FixedFluidBarrelBlockItem barrel) {
                        return barrel.createFluidHandler(stack);
                    }
                    if (stack.getItem() instanceof com.reinhardt.hbm.item.LegacyChainsawItem chainsaw) {
                        return chainsaw.createFluidHandler(stack);
                    }
                    if (stack.getItem() instanceof BlowtorchItem blowtorch) {
                        return blowtorch.createFluidHandler(stack);
                    }
                    if (stack.getItem() instanceof LegacyPipetteItem pipette) {
                        return pipette.createFluidHandler(stack);
                    }
                    return null;
                },
                fluidCapabilityItems()
        );
        net.minecraft.world.level.ItemLike[] legacyFluidCells = HbmItems.LEGACY_FLUID_CELL_ITEMS.stream()
                .map(DeferredItem::get)
                .toArray(net.minecraft.world.level.ItemLike[]::new);
        if (legacyFluidCells.length > 0) {
            event.registerItem(
                    Capabilities.FluidHandler.ITEM,
                    (stack, context) -> stack.getItem() instanceof HbmFluidContainerItem container
                            ? container.createFluidHandler(stack) : null,
                    legacyFluidCells
            );
        }
        net.minecraft.world.level.ItemLike[] legacyPipettes = LegacyHbmContent.LEGACY_ITEMS.stream()
                .map(DeferredItem::get)
                .toArray(net.minecraft.world.level.ItemLike[]::new);
        if (legacyPipettes.length > 0) {
            event.registerItem(
                    Capabilities.FluidHandler.ITEM,
                    (stack, context) -> stack.getItem() instanceof LegacyPipetteItem pipette
                            ? pipette.createFluidHandler(stack) : null,
                    legacyPipettes
            );
        }
    }

    private static net.minecraft.world.level.ItemLike[] fluidCapabilityItems() {
        List<net.minecraft.world.level.ItemLike> items = new ArrayList<>();
        if (HbmItems.INF_WATER.isBound()) {
            items.add(HbmItems.INF_WATER.get());
        }
        if (HbmItems.INF_WATER_MK2.isBound()) {
            items.add(HbmItems.INF_WATER_MK2.get());
        }
        items.add(HbmItems.FLUID_BARREL_INFINITE.get());
        items.add(HbmItems.CHLORINE_PINWHEEL.get());
        items.add(HbmItems.CANISTER_EMPTY.get());
        items.add(HbmItems.CANISTER_FULL.get());
        items.add(HbmItems.GAS_EMPTY.get());
        items.add(HbmItems.GAS_FULL.get());
        items.add(HbmItems.FLUID_TANK_EMPTY.get());
        items.add(HbmItems.FLUID_TANK_FULL.get());
        items.add(HbmItems.FLUID_TANK_LEAD_EMPTY.get());
        items.add(HbmItems.FLUID_TANK_LEAD_FULL.get());
        items.add(HbmItems.FLUID_BARREL_EMPTY.get());
        items.add(HbmItems.FLUID_BARREL_FULL.get());
        items.add(HbmItems.FLUID_PACK_EMPTY.get());
        items.add(HbmItems.FLUID_PACK_FULL.get());
        items.add(HbmItems.DISPERSER_CANISTER_EMPTY.get());
        items.add(HbmItems.DISPERSER_CANISTER.get());
        items.add(HbmItems.GLYPHID_GLAND_EMPTY.get());
        items.add(HbmItems.GLYPHID_GLAND.get());
        items.add(HbmItems.CELL_EMPTY.get());
        items.add(HbmItems.CELL_TRITIUM.get());
        items.add(HbmItems.TANK_STEEL.get());
        items.add(HbmItems.RED_BARREL_ITEM.get());
        items.add(HbmItems.PINK_BARREL_ITEM.get());
        items.add(HbmItems.LOX_BARREL_ITEM.get());
        items.add(HbmItems.CHAINSAW.get());
        items.add(HbmItems.BLOWTORCH.get());
        items.add(HbmItems.ACETYLENE_TORCH.get());
        return items.toArray(net.minecraft.world.level.ItemLike[]::new);
    }

    @Nullable
    private static IEnergyStorage nativeEnergyStorage(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                                                       @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        if (PowerNetworkManager.endpointAt(level, pos) == null) {
            return null;
        }
        return new NativeEnergyStorage(level, pos, side);
    }

    private static IItemHandler ashpitItemHandler(AshpitBlockEntity ashpit, @Nullable Direction side) {
        return new SidedInvWrapper(ashpit, side);
    }

    @Nullable
    private static IItemHandler pileGraphiteItemHandler(
            Level level,
            BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,
            @Nullable BlockEntity blockEntity,
            @Nullable Direction side
    ) {
        if (side == null || !(state.getBlock() instanceof PileGraphiteBlock)) {
            return null;
        }
        return new PileGraphiteItemHandler(level, pos, side);
    }

    private static IItemHandler fuelInputHandler(HeaterBlockEntity heater, @Nullable Direction side) {
        if (heater.kind() == HeaterBlockEntity.Kind.OILBURNER) {
            return new SidedInvWrapper(heater, side);
        }
        return new InsertOnlySidedInvWrapper(heater, side);
    }

    @Nullable
    private static IItemHandler dummyFuelInputHandler(MachineDummyBlockEntity dummy, @Nullable Direction side) {
        BlockEntity core = dummyCore(dummy);
        if (core instanceof ConveyorPressBlockEntity) {
            // The original vertical dummy parts expose the single stamp slot.
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof FelBlockEntity) {
            return null;
        }
        if (core instanceof CentrifugeBlockEntity || core instanceof GasCentrifugeBlockEntity) {
            return null;
        }
        if (core instanceof GasFlareBlockEntity) {
            return null;
        }
        if (core instanceof ExcavatorBlockEntity excavator) {
            return excavator.isAutomationPort(dummy.getBlockPos()) ? new SidedInvWrapper(dummy, side) : null;
        }
        if (core instanceof CombustionEngineBlockEntity engine) {
            return engine.allowsAutomationPort(dummy.getBlockPos(), side) ? new SidedInvWrapper(dummy, side) : null;
        }
        if (core instanceof WoodBurnerBlockEntity burner) {
            return burner.isAutomationPort(dummy.getBlockPos()) ? new SidedInvWrapper(dummy, side) : null;
        }
        if (core instanceof SoyuzLauncherBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof com.reinhardt.hbm.blockentity.LauncherBlockEntity launcher) {
            return launcher.kind() == com.reinhardt.hbm.blockentity.LauncherBlockEntity.Kind.PAD_RUSTED
                    ? null : new SidedInvWrapper(dummy, side);
        }
        if (core instanceof TurretJeremyBlockEntity || core instanceof TurretChekhovBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof LegacyTurretBlockEntity turret
                && turret.type() != com.reinhardt.hbm.blockentity.LegacyTurretType.HOWARD_DAMAGED) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof AssemblyFactoryBlockEntity factory) {
            return factory.allowsAutomationPort(dummy.getBlockPos(), side) ? new SidedInvWrapper(dummy, side) : null;
        }
        if (core instanceof ChemicalFactoryBlockEntity factory) {
            return factory.allowsAutomationPort(dummy.getBlockPos(), side) ? new SidedInvWrapper(dummy, side) : null;
        }
        if (core instanceof ZirnoxReactorBlockEntity reactor) {
            return reactor.allowsAutomationPort(dummy.getBlockPos(), side) ? new SidedInvWrapper(dummy, side) : null;
        }
        if (core instanceof WatzBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof PurexBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof MachineBlastFurnaceBlockEntity furnace) {
            return furnace.allowsAutomationPort(dummy.getBlockPos(), side) ? new SidedInvWrapper(dummy, side) : null;
        }
        if (core instanceof LegacyMachineBlockEntity machine) {
            return machine.slotCount() == 0 || !machine.allowsItemAutomationPort(dummy.getBlockPos())
                    ? null
                    : new SidedInvWrapper(dummy, side);
        }
        if (!allowsDummyAutomationPort(dummy, core)) {
            return null;
        }
        if (core instanceof AssemblyMachineBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof ChemicalPlantBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof CrystallizerBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof SilexBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof ElectrolyzerBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof ArcWelderBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof ArcFurnaceBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof ParticleAcceleratorBlockEntity accelerator) {
            return accelerator.getSlotsForAccessor(dummy.getBlockPos(), side).length == 0 ? null : new SidedInvWrapper(dummy, side);
        }
        if (core instanceof RotaryFurnaceBlockEntity furnace) {
            return furnace.getSlotsForAccessor(dummy.getBlockPos(), side).length == 0 ? null : new SidedInvWrapper(dummy, side);
        }
        if (core instanceof LargeFluidTankBlockEntity) {
            return null;
        }
        if (core instanceof FluidTankBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof OilDerrickBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof FrackingTowerBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof RefineryBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof VacuumDistillBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof CokerBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof FurnaceCombinationBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof RotaryFurnaceBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof SolidifierBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof StrandCasterBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof LiquefactorBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof CatalyticReformerBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof HydrotreaterBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof CrucibleBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof IronFurnaceBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof SteelFurnaceBlockEntity) {
            return new SidedInvWrapper(dummy, side);
        }
        if (core instanceof HeaterBlockEntity heater && heater.kind() == HeaterBlockEntity.Kind.OILBURNER) {
            return new SidedInvWrapper(dummy, side);
        }
        return new InsertOnlySidedInvWrapper(dummy, side);
    }

    private static IItemHandler shredderItemHandler(ShredderBlockEntity shredder, @Nullable Direction side) {
        return new SidedInvWrapper(shredder, side);
    }

    private static IItemHandler machineKeyForgeItemHandler(MachineKeyForgeBlockEntity keyForge, @Nullable Direction side) {
        return new SidedInvWrapper(keyForge, side);
    }

    @Nullable
    private static IItemHandler assemblyMachineItemHandler(AssemblyMachineBlockEntity assemblyMachine, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(assemblyMachine, side)) {
            return null;
        }
        return new SidedInvWrapper(assemblyMachine, side);
    }

    @Nullable
    private static IItemHandler assemblyFactoryItemHandler(AssemblyFactoryBlockEntity factory, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(factory, side)) {
            return null;
        }
        return new SidedInvWrapper(factory, side);
    }

    @Nullable
    private static IItemHandler chemicalPlantItemHandler(ChemicalPlantBlockEntity chemicalPlant, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(chemicalPlant, side)) {
            return null;
        }
        return new SidedInvWrapper(chemicalPlant, side);
    }

    @Nullable
    private static IItemHandler chemicalFactoryItemHandler(ChemicalFactoryBlockEntity factory, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(factory, side)) {
            return null;
        }
        return new SidedInvWrapper(factory, side);
    }

    private static IItemHandler solderingStationItemHandler(SolderingStationBlockEntity solderingStation, @Nullable Direction side) {
        return new SidedInvWrapper(solderingStation, side);
    }

    private static IItemHandler arcWelderItemHandler(ArcWelderBlockEntity arcWelder, @Nullable Direction side) {
        return new SidedInvWrapper(arcWelder, side);
    }

    private static IItemHandler electricFurnaceItemHandler(ElectricFurnaceBlockEntity furnace, @Nullable Direction side) {
        return new SidedInvWrapper(furnace, side);
    }

    @Nullable
    private static IItemHandler machineBlastFurnaceItemHandler(MachineBlastFurnaceBlockEntity furnace, @Nullable Direction side) {
        return side == null ? new SidedInvWrapper(furnace, null) : null;
    }

    @Nullable
    private static IFluidHandler machineBlastFurnaceFluidHandler(MachineBlastFurnaceBlockEntity furnace, @Nullable Direction side) {
        return furnace.fluidHandler(furnace.getBlockPos(), side);
    }

    private static IItemHandler ironFurnaceItemHandler(IronFurnaceBlockEntity furnace, @Nullable Direction side) {
        return new SidedInvWrapper(furnace, side);
    }

    private static IItemHandler steelFurnaceItemHandler(SteelFurnaceBlockEntity furnace, @Nullable Direction side) {
        return new SidedInvWrapper(furnace, side);
    }

    @Nullable
    private static IItemHandler woodBurnerItemHandler(WoodBurnerBlockEntity burner, @Nullable Direction side) {
        return burner.allowsAutomationPort(burner.getBlockPos(), side) ? new SidedInvWrapper(burner, side) : null;
    }

    private static IItemHandler soyuzLauncherItemHandler(SoyuzLauncherBlockEntity launcher, @Nullable Direction side) {
        return new SidedInvWrapper(launcher, side);
    }

    private static IItemHandler launcherItemHandler(com.reinhardt.hbm.blockentity.LauncherBlockEntity launcher, @Nullable Direction side) {
        return launcher.kind() == com.reinhardt.hbm.blockentity.LauncherBlockEntity.Kind.PAD_RUSTED
                ? null : new SidedInvWrapper(launcher, side);
    }

    private static IItemHandler soyuzCapsuleItemHandler(SoyuzCapsuleBlockEntity capsule, @Nullable Direction side) {
        return new SidedInvWrapper(capsule, side);
    }

    private static IItemHandler dieselGeneratorItemHandler(DieselGeneratorBlockEntity generator, @Nullable Direction side) {
        return new SidedInvWrapper(generator, side);
    }

    private static IItemHandler combustionEngineItemHandler(CombustionEngineBlockEntity engine, @Nullable Direction side) {
        return engine.allowsAutomationPort(engine.getBlockPos(), side) ? new SidedInvWrapper(engine, side) : null;
    }

    private static IItemHandler gasFlareItemHandler(GasFlareBlockEntity flare, @Nullable Direction side) {
        return new SidedInvWrapper(flare, side);
    }

    private static IItemHandler steamTurbineItemHandler(SteamTurbineBlockEntity turbine, @Nullable Direction side) {
        return new SidedInvWrapper(turbine, side);
    }

    private static IItemHandler turretJeremyItemHandler(TurretJeremyBlockEntity turret, @Nullable Direction side) {
        return new SidedInvWrapper(turret, side);
    }

    private static IItemHandler turretChekhovItemHandler(TurretChekhovBlockEntity turret, @Nullable Direction side) {
        return new SidedInvWrapper(turret, side);
    }

    private static IItemHandler legacyTurretItemHandler(LegacyTurretBlockEntity turret, @Nullable Direction side) {
        return new SidedInvWrapper(turret, side);
    }

    private static IItemHandler largeTurbineItemHandler(LargeTurbineBlockEntity turbine, @Nullable Direction side) {
        return new SidedInvWrapper(turbine, side);
    }

    private static IItemHandler gasTurbineItemHandler(GasTurbineBlockEntity turbine, @Nullable Direction side) {
        return new SidedInvWrapper(turbine, side);
    }

    private static IItemHandler smallBoilerItemHandler(SmallBoilerBlockEntity boiler, @Nullable Direction side) {
        return new SidedInvWrapper(boiler, side);
    }

    @Nullable
    private static IItemHandler oilDerrickItemHandler(OilDerrickBlockEntity derrick, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(derrick, side)) {
            return null;
        }
        return new SidedInvWrapper(derrick, side);
    }

    @Nullable
    private static IItemHandler frackingTowerItemHandler(FrackingTowerBlockEntity tower, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(tower, side)) {
            return null;
        }
        return new SidedInvWrapper(tower, side);
    }

    private static IItemHandler refineryItemHandler(RefineryBlockEntity refinery, @Nullable Direction side) {
        return new SidedInvWrapper(refinery, side);
    }

    private static IItemHandler vacuumDistillItemHandler(VacuumDistillBlockEntity distill, @Nullable Direction side) {
        return new SidedInvWrapper(distill, side);
    }

    private static IItemHandler cokerItemHandler(CokerBlockEntity coker, @Nullable Direction side) {
        return new SidedInvWrapper(coker, side);
    }

    private static IItemHandler furnaceCombinationItemHandler(FurnaceCombinationBlockEntity furnace, @Nullable Direction side) {
        return new SidedInvWrapper(furnace, side);
    }

    private static IItemHandler rotaryFurnaceItemHandler(RotaryFurnaceBlockEntity furnace, @Nullable Direction side) {
        return new SidedInvWrapper(furnace, side);
    }

    private static IItemHandler solidifierItemHandler(SolidifierBlockEntity solidifier, @Nullable Direction side) {
        return new SidedInvWrapper(solidifier, side);
    }

    private static IItemHandler liquefactorItemHandler(LiquefactorBlockEntity liquefactor, @Nullable Direction side) {
        return new SidedInvWrapper(liquefactor, side);
    }

    @Nullable
    private static IItemHandler catalyticReformerItemHandler(CatalyticReformerBlockEntity reformer, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(reformer, side)) {
            return null;
        }
        return new SidedInvWrapper(reformer, side);
    }

    @Nullable
    private static IItemHandler hydrotreaterItemHandler(HydrotreaterBlockEntity hydrotreater, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(hydrotreater, side)) {
            return null;
        }
        return new SidedInvWrapper(hydrotreater, side);
    }

    private static IItemHandler fluidTankItemHandler(FluidTankBlockEntity tank, @Nullable Direction side) {
        return new SidedInvWrapper(tank, side);
    }

    private static IItemHandler storageCrateItemHandler(StorageCrateBlockEntity crate, @Nullable Direction side) {
        return new SidedInvWrapper(crate, side);
    }

    private static IItemHandler batteryReddItemHandler(BatteryReddBlockEntity battery, @Nullable Direction side) {
        return new SidedInvWrapper(battery, side);
    }

    private static IItemHandler batterySocketItemHandler(BatterySocketBlockEntity socket, @Nullable Direction side) {
        return new SidedInvWrapper(socket, side);
    }

    private static IItemHandler centrifugeItemHandler(CentrifugeBlockEntity centrifuge, @Nullable Direction side) {
        return new SidedInvWrapper(centrifuge, side);
    }

    private static IItemHandler gasCentrifugeItemHandler(GasCentrifugeBlockEntity centrifuge, @Nullable Direction side) {
        return new SidedInvWrapper(centrifuge, side);
    }

    private static IItemHandler cyclotronItemHandler(CyclotronBlockEntity cyclotron, @Nullable Direction side) {
        return new SidedInvWrapper(cyclotron, side);
    }

    private static IItemHandler exposureChamberItemHandler(ExposureChamberBlockEntity chamber, @Nullable Direction side) {
        return new SidedInvWrapper(chamber, side);
    }

    @Nullable
    private static IItemHandler particleAcceleratorItemHandler(ParticleAcceleratorBlockEntity accelerator, @Nullable Direction side) {
        return accelerator.getSlotsForAccessor(accelerator.getBlockPos(), side).length == 0 ? null : new SidedInvWrapper(accelerator, side);
    }

    @Nullable
    private static IItemHandler electrolyzerItemHandler(ElectrolyzerBlockEntity electrolyzer, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(electrolyzer, side)) {
            return null;
        }
        return new SidedInvWrapper(electrolyzer, side);
    }

    private static IItemHandler oreSlopperItemHandler(OreSlopperBlockEntity slopper, @Nullable Direction side) {
        return new SidedInvWrapper(slopper, side);
    }

    private static IItemHandler wasteDrumItemHandler(WasteDrumBlockEntity drum, @Nullable Direction side) {
        return new SidedInvWrapper(drum, side);
    }

    private static IItemHandler purexItemHandler(PurexBlockEntity purex, @Nullable Direction side) {
        return new SidedInvWrapper(purex, side);
    }

    @Nullable
    private static IItemHandler zirnoxItemHandler(ZirnoxReactorBlockEntity reactor, @Nullable Direction side) {
        return side == null ? new SidedInvWrapper(reactor, null) : null;
    }

    private static IItemHandler rbmkItemHandler(RbmkComponentBlockEntity rbmk, @Nullable Direction side) {
        return new SidedInvWrapper(rbmk, side);
    }

    @Nullable
    private static IItemHandler pwrPartItemHandler(PwrBlockEntity part, @Nullable Direction side) {
        PwrControllerBlockEntity controller = part.controller();
        if (controller == null || !part.isPort()) {
            return null;
        }
        return new SidedInvWrapper(controller, side);
    }

    @Nullable
    private static IItemHandler crystallizerItemHandler(CrystallizerBlockEntity crystallizer, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(crystallizer, side)) {
            return null;
        }
        return new SidedInvWrapper(crystallizer, side);
    }

    private static IItemHandler crucibleItemHandler(CrucibleBlockEntity crucible, @Nullable Direction side) {
        return new InsertOnlySidedInvWrapper(crucible, side);
    }

    private static IItemHandler foundryCastingItemHandler(FoundryCastingBlockEntity casting, @Nullable Direction side) {
        return new SidedInvWrapper(casting, side);
    }

    private static IItemHandler strandCasterItemHandler(StrandCasterBlockEntity caster, @Nullable Direction side) {
        return new SidedInvWrapper(caster, side);
    }

    @Nullable
    private static IItemHandler pressItemHandler(PressBlockEntity press, @Nullable Direction side) {
        if (press.kind() == PressBlockEntity.Kind.ELECTRIC && !allowsCoreAutomationPort(press, side)) {
            return null;
        }
        return new SidedInvWrapper(press, side);
    }

    private static IItemHandler ammoPressItemHandler(AmmoPressBlockEntity ammoPress, @Nullable Direction side) {
        return new SidedInvWrapper(ammoPress, side);
    }

    private static boolean allowsDummyAutomationPort(MachineDummyBlockEntity dummy, @Nullable BlockEntity core) {
        if (core instanceof FelBlockEntity) {
            return false;
        }
        if (core instanceof ArcFurnaceBlockEntity furnace) {
            return furnace.isAutomationPort(dummy.getBlockPos());
        }
        if (core instanceof SilexBlockEntity silex) {
            return allowsSilexDummyAutomationPort(dummy, silex);
        }

        LevelAccessor level = dummy.getLevel();
        if (level == null) {
            return true;
        }

        List<BlockPos> connectors = powerConnectorPositions(level, core);
        if (connectors.isEmpty()) {
            return true;
        }

        BlockPos dummyPos = dummy.getBlockPos();
        for (BlockPos connector : connectors) {
            if (connector.equals(dummyPos) || manhattanDistance(connector, dummyPos) == 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean allowsSilexDummyAutomationPort(MachineDummyBlockEntity dummy, SilexBlockEntity silex) {
        Direction facing = silex.getBlockState().hasProperty(com.reinhardt.hbm.block.LargeMachineBlock.FACING)
                ? silex.getBlockState().getValue(com.reinhardt.hbm.block.LargeMachineBlock.FACING)
                : Direction.SOUTH;
        BlockPos corePos = silex.getBlockPos();
        Direction side = LegacyMachineGeometry.forgeRotateUp(facing);
        BlockPos northTop = corePos.relative(side).above();
        BlockPos southTop = corePos.relative(side.getOpposite()).above();
        BlockPos dummyPos = dummy.getBlockPos();
        return dummyPos.equals(northTop) || dummyPos.equals(southTop);
    }

    @Nullable
    private static IFluidHandler dummyFluidHandler(MachineDummyBlockEntity dummy, @Nullable Direction side) {
        BlockEntity core = dummyCore(dummy);
        if (core instanceof RbmkComponentBlockEntity rbmk) {
            // A dummy segment is the actual capability position queried by a
            // fluid pipe.  RBMK's old trySubscribe/tryProvide coordinates are
            // port-specific, so do not collapse it to the core position.
            return rbmk.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof FusionMachineBlockEntity fusion) {
            return fusion.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof IcfCoreBlockEntity icf) {
            return icf.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof WatzBlockEntity watz) {
            return watz.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof PurexBlockEntity purex) {
            return purex.fluidHandler(side);
        }
        if (core instanceof CombustionEngineBlockEntity engine) {
            return engine.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof GasFlareBlockEntity flare) {
            return flare.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof GasCentrifugeBlockEntity centrifuge) {
            return centrifuge.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof IndustrialBoilerBlockEntity boiler) {
            return boiler.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof HeatBoilerBlockEntity boiler) {
            return boiler.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof SolarBoilerBlockEntity boiler) {
            return boiler.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof GroundwaterPumpBlockEntity pump) {
            return pump.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof AirCompressorBlockEntity compressor) {
            return compressor.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof GeothermalHeatExchangerBlockEntity exchanger) {
            return exchanger.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof DeuteriumExtractorBlockEntity extractor) {
            return extractor.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof CompressorBlockEntity compressor) {
            return compressor.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof MixerBlockEntity mixer) {
            return mixer.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof OilDerrickBlockEntity derrick) {
            return derrick.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof FrackingTowerBlockEntity tower) {
            return tower.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof RefineryBlockEntity refinery) {
            return refinery.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof VacuumDistillBlockEntity distill) {
            return distill.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof CokerBlockEntity coker) {
            return coker.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof FurnaceCombinationBlockEntity furnace) {
            return furnace.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof RotaryFurnaceBlockEntity furnace) {
            return furnace.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof SolidifierBlockEntity solidifier) {
            return solidifier.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof StrandCasterBlockEntity caster) {
            return caster.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof ElectrolyzerBlockEntity electrolyzer) {
            return electrolyzer.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof LiquefactorBlockEntity liquefactor) {
            return liquefactor.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof CatalyticCrackerBlockEntity cracker) {
            return cracker.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof CatalyticReformerBlockEntity reformer) {
            return reformer.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof HydrotreaterBlockEntity hydrotreater) {
            return hydrotreater.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof FractionTowerBlockEntity tower) {
            return tower.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof FluidTankBlockEntity tank) {
            return tank.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof SteamEngineBlockEntity engine) {
            return engine.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof IndustrialTurbineBlockEntity turbine) {
            return turbine.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof LargeTurbineBlockEntity turbine) {
            return turbine.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof LeviathanTurbineBlockEntity turbine) {
            return turbine.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof GasTurbineBlockEntity turbine) {
            return turbine.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof CoolingTowerBlockEntity tower) {
            return tower.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof ZirnoxReactorBlockEntity reactor) {
            return reactor.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof PwrControllerBlockEntity controller) {
            return controller.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof WoodBurnerBlockEntity burner) {
            return burner.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof SoyuzLauncherBlockEntity launcher) {
            return launcher.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof com.reinhardt.hbm.blockentity.LauncherBlockEntity launcher) {
            return launcher.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof MachineBlastFurnaceBlockEntity furnace) {
            return furnace.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof ArcWelderBlockEntity arcWelder) {
            return arcWelder.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof ExcavatorBlockEntity excavator) {
            return excavator.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof OreSlopperBlockEntity slopper) {
            return slopper.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof CyclotronBlockEntity cyclotron) {
            return cyclotron.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof ParticleAcceleratorBlockEntity accelerator) {
            return accelerator.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof PoweredSteamCondenserBlockEntity condenser) {
            return condenser.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof ChimneyBlockEntity chimney) {
            return chimney.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof LegacyTurretBlockEntity turret) {
            return turret.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof LegacyMachineBlockEntity machine) {
            return machine.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof ChemicalPlantBlockEntity chemicalPlant && allowsDummyAutomationPort(dummy, core)) {
            return chemicalPlant.fluidHandler(side);
        }
        if (core instanceof CrystallizerBlockEntity crystallizer && allowsDummyAutomationPort(dummy, core)) {
            return crystallizer.fluidHandler(side);
        }
        if (core instanceof SilexBlockEntity silex && allowsDummyAutomationPort(dummy, core)) {
            return silex.fluidHandler(side);
        }
        if (core instanceof AssemblyMachineBlockEntity assemblyMachine && allowsDummyAutomationPort(dummy, core)) {
            return assemblyMachine.fluidHandler(side);
        }
        if (core instanceof AssemblyFactoryBlockEntity factory && factory.allowsAutomationPort(dummy.getBlockPos(), side)) {
            return factory.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof ChemicalFactoryBlockEntity factory && factory.allowsAutomationPort(dummy.getBlockPos(), side)) {
            return factory.fluidHandler(dummy.getBlockPos(), side);
        }
        if (core instanceof SolderingStationBlockEntity solderingStation) {
            return solderingStation.fluidHandler(side);
        }
        if (core instanceof HeaterBlockEntity heater) {
            return heater.fluidHandler(dummy.getBlockPos(), side);
        }
        return null;
    }

    @Nullable
    private static IFluidHandler heaterFluidHandler(HeaterBlockEntity heater, @Nullable Direction side) {
        return heater.fluidHandler(heater.getBlockPos(), side);
    }

    @Nullable
    private static IFluidHandler pwrPartFluidHandler(PwrBlockEntity part, @Nullable Direction side) {
        PwrControllerBlockEntity controller = part.controller();
        if (controller == null || !part.isPort()) {
            return null;
        }
        return controller.fluidHandler(part.getBlockPos(), side);
    }

    @Nullable
    private static IFluidHandler airCompressorFluidHandler(AirCompressorBlockEntity compressor, @Nullable Direction side) {
        return compressor.fluidHandler(compressor.getBlockPos(), side);
    }

    @Nullable
    private static IFluidHandler chemicalPlantFluidHandler(ChemicalPlantBlockEntity chemicalPlant, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(chemicalPlant, side)) {
            return null;
        }
        return chemicalPlant.fluidHandler(side);
    }

    @Nullable
    private static IFluidHandler assemblyMachineFluidHandler(AssemblyMachineBlockEntity assemblyMachine, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(assemblyMachine, side)) {
            return null;
        }
        return assemblyMachine.fluidHandler(side);
    }

    @Nullable
    private static IFluidHandler arcWelderFluidHandler(ArcWelderBlockEntity arcWelder, @Nullable Direction side) {
        if (side == null) {
            return arcWelder.fluidHandler(side);
        }
        return arcWelder.fluidHandler(arcWelder.getBlockPos().relative(side), side);
    }

    @Nullable
    private static IFluidHandler crystallizerFluidHandler(CrystallizerBlockEntity crystallizer, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(crystallizer, side)) {
            return null;
        }
        return crystallizer.fluidHandler(side);
    }

    @Nullable
    private static IFluidHandler assemblyFactoryFluidHandler(AssemblyFactoryBlockEntity factory, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(factory, side)) {
            return null;
        }
        return factory.fluidHandler(side);
    }

    @Nullable
    private static IFluidHandler chemicalFactoryFluidHandler(ChemicalFactoryBlockEntity factory, @Nullable Direction side) {
        if (!allowsCoreAutomationPort(factory, side)) {
            return null;
        }
        return factory.fluidHandler(side);
    }

    @Nullable
    private static BlockEntity dummyCore(MachineDummyBlockEntity dummy) {
        LevelAccessor level = dummy.getLevel();
        return level == null ? null : level.getBlockEntity(dummy.getCorePos());
    }

    private static boolean allowsCoreAutomationPort(BlockEntity core, @Nullable Direction side) {
        if (side == null || core.getLevel() == null) {
            return true;
        }

        List<BlockPos> connectors = powerConnectorPositions(core.getLevel(), core);
        return connectors.isEmpty() || connectors.contains(core.getBlockPos().relative(side));
    }

    private static List<BlockPos> powerConnectorPositions(LevelAccessor level, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof PowerEndpoint endpoint) {
            return endpoint.getPowerConnectorPositions(level);
        }
        if (blockEntity instanceof PowerGraphNode graphNode) {
            return graphNode.getPowerConnectorPositions(level);
        }
        return List.of();
    }

    private static int manhattanDistance(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX())
                + Math.abs(first.getY() - second.getY())
                + Math.abs(first.getZ() - second.getZ());
    }

    private static final class InsertOnlySidedInvWrapper extends SidedInvWrapper {
        private InsertOnlySidedInvWrapper(WorldlyContainer inv, @Nullable Direction side) {
            super(inv, side);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }

    /** Stateless axial inserter for the CP-1 graphite pile. */
    private static final class PileGraphiteItemHandler implements IItemHandler {
        private final Level level;
        private final BlockPos pos;
        private final Direction side;

        private PileGraphiteItemHandler(Level level, BlockPos pos, Direction side) {
            this.level = level;
            this.pos = pos.immutable();
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !PileGraphiteBlock.insertAutomatedRod(this.level, this.pos, this.side, stack, simulate)) {
                return stack;
            }
            return stack.getCount() == 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && PileGraphiteBlock.insertAutomatedRod(this.level, this.pos, this.side, stack, true);
        }
    }
}
